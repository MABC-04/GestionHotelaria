# PMS de reservas hoteleras

Núcleo de un PMS (Property Management System) para reservas hoteleras,
construido con **desarrollo dirigido por modelos**: un metamodelo Ecore, un
catálogo de invariantes OCL y un esquema PostgreSQL que impide vender dos veces
la misma habitación.

Este repositorio cubre la **entrega 1 (Sprints 0-2)** del plan
[`pms-reservas-plan-build.md`](pms-reservas-plan-build.md), derivado del
[`analisis_sistema_reservas_hotel.md`](analisis_sistema_reservas_hotel.md).

> **Hito M1:** *el sistema nunca vende dos veces la misma habitación.*
> Se demuestra con 100 hilos compitiendo por la última habitación: exactamente
> 1 reserva confirmada y 99 rechazos (`SinSolapamientoIT#concurrenciaCienHilos`).

## Estado de la entrega

| Criterio de done de S0 | Estado |
|---|:---:|
| `mvn verify` en verde | ✅ |
| El fixture produce exactamente 2 violaciones | ✅ `FixtureValidacionTest` |
| El esquema se aplica sin errores en PostgreSQL 16 | ✅ verificado en PostgreSQL 16.16 local y en CI |
| `rh_sin_solape` rechaza un insert solapado | ✅ `SinSolapamientoIT` |
| Endpoint `/health` funcionando | ✅ `SaludControllerTest` |
| Matriz de trazabilidad generada automáticamente | ✅ `pms-generator` |

Además: 100 hilos → 1 confirmación y 99 `SQLState 23P01`, frontend Angular con
pantalla de salud, y matriz de trazabilidad regenerada en cada build.

El detalle de cómo se comprueba cada criterio está en
[`docs/verificacion-s0.md`](docs/verificacion-s0.md).

## Stack

| Capa | Tecnología |
|---|---|
| Modelo | Eclipse EMF (Ecore) 2.39, EMF dinámico |
| Restricciones | Complete OCL normativo + validadores Java |
| Backend | Java 21, Spring Boot 3.5, JDBC, Flyway |
| Base de datos | PostgreSQL 16 (`btree_gist`, `EXCLUDE`) |
| Frontend | Angular 22 (standalone) |
| Build | Maven 3.9 (wrapper incluido) |
| Pruebas | JUnit 5, MockMvc, Flyway programático |

## Estructura

```
pms-model/       metamodelo .ecore, catálogo .ocl, fixture .xmi y validador
pms-generator/   genera pms-docs/trazabilidad.md desde el OCL
pms-backend/     API Spring Boot, migraciones Flyway, tests de integración
pms-frontend/    consola Angular (pantalla de salud)
pms-docs/        matriz de trazabilidad generada
docs/adr/        decisiones de arquitectura
```

## Requisitos

- **JDK 21 o superior** (el proyecto compila con `--release 21`).
- **PostgreSQL 16** con la extensión `btree_gist`.
- **Node.js 22.12+ o 24** para el frontend.

### Instalar PostgreSQL en Windows

```powershell
winget install --id PostgreSQL.PostgreSQL.16 -e
```

Crea la base y el usuario (los valores por defecto del proyecto son
`postgres/postgres` en `localhost:5432`):

```powershell
& "C:\Program Files\PostgreSQL\16\bin\createdb.exe" -U postgres pms
```

> `btree_gist` viaja con PostgreSQL; no hay que instalar nada extra.

> **Nota de verificación:** este repositorio se validó contra PostgreSQL
> **16.16** real (no solo en CI). Si `winget` falla por falta de elevación,
> sirven tanto el instalador con UAC como los binarios portables de
> EnterpriseDB (`postgresql-16.x-windows-x64-binaries.zip` + `initdb`).

## Backend

```powershell
# compilar modelo, generar trazabilidad, pruebas y verificación
.\mvnw.cmd verify

# arrancar la API (aplica V1 y V2 automáticamente con Flyway)
.\mvnw.cmd -pl pms-backend spring-boot:run

# o con el jar ejecutable que produce el build
java -jar pms-backend\target\pms-backend-0.1.0-SNAPSHOT-ejecutable.jar
```

Configuración por variables de entorno:

| Variable | Valor por defecto |
|---|---|
| `PMS_DB_URL` | `jdbc:postgresql://localhost:5432/pms` |
| `PMS_DB_USER` | `postgres` |
| `PMS_DB_PASSWORD` | `postgres` |
| `PMS_PORT` | `8080` |

Endpoints:

- `GET /health` — sonda de negocio (estado de la aplicación y de la base).
- `GET /actuator/health` — sonda estándar de Spring Boot.

### Pruebas

| Test | Qué demuestra |
|---|---|
| `MetamodeloTest` | 24 clases, 12 enums, 5 tipos, referencias y opuestos resueltos |
| `CatalogoOclTest` | 45 invariantes sin deriva entre OCL y Java |
| `FixtureValidacionTest` | exactamente 2 violaciones (memoria y `.xmi`) |
| `SaludControllerTest` | `/health` responde sin base de datos |
| `SinSolapamientoIT` | esquema, `rh_sin_solape` y 100 hilos concurrentes |

`SinSolapamientoIT` se omite si no hay PostgreSQL escuchando. Para ejecutarlo
contra otra instancia:

```powershell
$env:PMS_TEST_DB_URL = "jdbc:postgresql://localhost:5432/postgres"
.\mvnw.cmd -pl pms-backend verify
```

Cada ejecución crea un esquema `pms_it_<aleatorio>` y lo elimina al terminar; no
toca la base de desarrollo.

## Frontend

```powershell
cd pms-frontend
npm.cmd install
npm.cmd start
```

Abre `http://localhost:4200`. La pantalla consulta `/health` (proxy de
desarrollo hacia `http://localhost:8080`).

## Modelo y OCL

- `pms-model/src/main/resources/model/reservas.ecore`: 24 clases, 12
  enumeraciones y 5 tipos de dato (`UUID`, `Decimal`, `EDate`, `EDateTime`,
  `Hora`).
- `pms-model/src/main/resources/model/reservas.ocl`: 45 invariantes en Complete
  OCL, agrupadas por regla de negocio (RN-01 a RN-15) y por el núcleo del §4.2.
- `pms-model/.../CatalogoInvariantes.java`: ejecución de esas 45 reglas.

El motor OCL clásico de Maven Central **no soporta comparaciones sobre
`EDate`**. La especificación se mantiene en OCL y la ejecución en Java, con un
test que impide la deriva. La decisión está razonada en
[ADR-002](docs/adr/ADR-002-motor-ocl.md).

### Regenerar la matriz de trazabilidad

Se regenera sola en cada build. Manualmente:

```powershell
.\mvnw.cmd -pl pms-model install
.\mvnw.cmd -pl pms-generator process-classes
```

Resultado: [`pms-docs/trazabilidad.md`](pms-docs/trazabilidad.md).

## Decisiones de arquitectura

- [ADR-001 — Modelo Ecore dinámico durante el Sprint 0](docs/adr/ADR-001-modelo-ecore-dinamico.md)
- [ADR-002 — OCL normativo y validadores Java ejecutables](docs/adr/ADR-002-motor-ocl.md)
- [ADR-003 — PostgreSQL 16 nativo y la restricción `rh_sin_solape`](docs/adr/ADR-003-postgresql-y-exclusion.md)

## Alcance y siguientes pasos

**Incluido (S0-S2):** modelo completo, validación, esquema de inventario,
reservas, garantías, retenciones y aprobación de descuentos, más la consola de
salud.

**Fuera de alcance (S3+):** estancia, folio, cargos y pagos; bloqueos de
habitación; grupos y cupos; integración con OTAs y pasarela de pago; tape chart
arrastrable.

El detalle de sprints, riesgos y criterios de M2-M4 está en el
[plan de build](pms-reservas-plan-build.md).
