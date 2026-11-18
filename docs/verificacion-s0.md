# Verificación del Sprint 0

Este documento registra **cómo se comprueba** cada criterio de done y qué
artefacto lo demuestra. No sustituye a los tests: los referencia.

## Resultado de la ejecución real

Ejecutado el **2026-11-17** contra PostgreSQL **16.16** (Windows, autenticación
local), no solo en CI:

| Comprobación | Resultado |
|---|---|
| `.\mvnw.cmd verify` (reactor completo) | `BUILD SUCCESS` |
| `pms-model` | 10 tests, 0 fallos |
| `pms-backend` (unitario) | 1 test, 0 fallos |
| `SinSolapamientoIT` (integración real) | 5 tests, 0 fallos, 0 omitidos |
| Concurrencia M1 | 1 confirmación · 99 rechazos `23P01` |
| Migraciones aplicadas | 2 (`V1`, `V2`) en 0,98 s |
| Seed verificado en base | 4 habitaciones · 2 tarifas · 730 precios |
| `rh_sin_solape` + índice GiST | presentes en `pg_constraint` y `pg_indexes` |
| `GET /health` con base viva | `200` · `estado: UP` · `baseDatos.estado: UP` · `migraciones: 2` |

Respuesta real de la sonda:

```json
{
  "estado": "UP",
  "aplicacion": "pms-backend",
  "version": "0.1.0-SNAPSHOT",
  "momento": "2026-11-17T21:58:00.989177400-06:00",
  "baseDatos": { "migraciones": 2, "estado": "UP" }
}
```

## Comandos

```powershell
# Todo el reactor: modelo, generador y backend (incluye IT si hay PostgreSQL)
.\mvnw.cmd verify

# Solo el modelo
.\mvnw.cmd -pl pms-model test

# Solo la integración contra PostgreSQL
$env:PMS_TEST_DB_URL = "jdbc:postgresql://localhost:5432/postgres"
.\mvnw.cmd -pl pms-backend verify

# Frontend
cd pms-frontend
npm.cmd test -- --watch=false
npm.cmd run build
```

## Criterios

### 1. `mvn verify` en verde

- **Evidencia:** reactor de 4 módulos (`padre`, `pms-model`, `pms-generator`,
  `pms-backend`) con `BUILD SUCCESS`.
- **Tests de modelo:** 10 (4 de metamodelo, 3 de catálogo, 3 de fixture).
- **Tests de backend:** 1 unitario (`SaludControllerTest`) y 5 de integración
  (`SinSolapamientoIT`, se omiten sin PostgreSQL).

### 2. El fixture produce exactamente 2 violaciones

- **Test:** `FixtureValidacionTest`.
- **Contrato:** `sinSolapamientoEnHabitacion` (RN-01) y `descuentoAprobado`
  (RN-09), ni una más.
- **Doble comprobación:** sobre el modelo en memoria y sobre el `.xmi`
  versionado, para cubrir la (de)serialización de EMF.

### 3. El esquema se aplica sin errores en PostgreSQL 16

- **Test:** `SinSolapamientoIT#migracionesAplicadas`.
- **Qué valida:** Flyway aplica `V1` y `V2` en un esquema limpio y el seed queda
  íntegro (4 habitaciones, 2 tarifas, 730 precios, 2 migraciones).
- **Aislamiento:** esquema `pms_it_<aleatorio>` creado y eliminado por
  ejecución; no toca la base de desarrollo.
- **Evidencia real:** aplicado además sobre la base `pms` al arrancar el jar
  (`Successfully applied 2 migrations ... now at version v2`).

### 4. `rh_sin_solape` rechaza un insert solapado

- **Test:** `SinSolapamientoIT#rechazaSolapamiento`.
- **Qué valida:** el segundo insert recibe `SQLState 23P01` y el mensaje nombra
  la restricción.
- **Casos complementarios:** fechas contiguas permitidas (`#permiteFechasContiguas`)
  y cancelación que libera inventario (`#cancelacionLiberaInventario`).

### 5. Hito M1 con 100 hilos

- **Test:** `SinSolapamientoIT#concurrenciaCienHilos`.
- **Qué valida:** 100 tareas esperan en una barrera, compiten por la misma
  habitación y el mismo rango; **exactamente 1** confirma y **99** reciben
  `23P01`. Cualquier otro error hace fallar el test.
- **Diseño:** pool de 24 conexiones para no agotar `max_connections`; el cuello
  de botella es la restricción de exclusión, no el servidor.

### 6. Endpoint `/health`

- **Test:** `SaludControllerTest` (MockMvc).
- **Qué valida:** responde `200` con `estado: UP` y declara
  `baseDatos.estado: NO_CONFIGURADA` cuando no hay datasource, en lugar de
  fallar.

### 7. Matriz de trazabilidad automática

- **Artefacto:** `pms-docs/trazabilidad.md`.
- **Generación:** `pms-generator` en la fase `process-classes`.
- **Contrato:** el generador falla el build si el OCL y `CatalogoInvariantes`
  divergen; CI comprueba con `git diff --exit-code` que el documento versionado
  está al día.
- **Contenido:** 45 invariantes con contexto, refuerzo en base de datos y prueba
  asociada, más las brechas declaradas de forma explícita.

## Limitaciones declaradas

| Limitación | Motivo | Mitigación |
|---|---|---|
| OCL clásico sin operaciones de fecha | única versión en Maven Central | `.ocl` normativo + Java ejecutable + test de no-deriva |
| `SinSolapamientoIT` se omite sin PostgreSQL | entorno de desarrollo sin Docker | CI con servicio `postgres:16`; local con `PMS_TEST_DB_URL` |
| Tablas de S3+ no persisten | alcance de la entrega | modeladas en Ecore y en la matriz |
| Sin API CRUD de reservas | alcance de esta entrega (esqueleto S0) | esquema, seed y garantía de concurrencia ya operativos |

## Resultado obtenido de `mvn verify`

Con `PMS_TEST_DB_URL` apuntando a PostgreSQL 16.16:

```
PMS Reservas ....................... SUCCESS
PMS — Modelo (PIM) ................. SUCCESS   Tests run: 10, Failures: 0
PMS — Generador .................... SUCCESS   Trazabilidad generada: ... (45 invariantes)
PMS — Backend ...................... SUCCESS   Tests run: 1, + IT: Tests run: 5, Failures: 0
BUILD SUCCESS
```
