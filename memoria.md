# Memoria del proyecto — estado actual

- **Fecha:** 2026-11-17
- **Rama:** `master` (2 commits: `31368a9` entrega 1, `661b7b4` AGENTS.md)
- **Alcance:** entrega 1 = Sprints 0–2 del plan `pms-reservas-plan-build.md`
- **Hito M1:** *el sistema nunca vende dos veces la misma habitación* — garantizado
  por PostgreSQL y demostrado con 100 hilos.

## En una frase

El núcleo está construido y **verificado contra PostgreSQL 16.16 real**: modelo
Ecore con 45 invariantes, esquema con `EXCLUDE` que impide el solapamiento,
prueba de concurrencia como gate de release, `/health` operativo y consola
Angular. **No existe todavía API de negocio** (ni disponibilidad ni CRUD de
reservas): el M1 está garantizado en el dato, no expuesto por REST.

## Qué está terminado y con qué evidencia

| Elemento | Estado | Evidencia |
|---|---|---|
| Metamodelo `reservas.ecore` (24 clases, 12 enums, 5 tipos) | ✅ | `MetamodeloTest` (4 tests) |
| Catálogo OCL (45 invariantes) + implementación Java | ✅ | `CatalogoOclTest` (3 tests, sin deriva) |
| Fixture `.xmi` con exactamente 2 violaciones | ✅ | `FixtureValidacionTest` (3 tests) |
| Esquema `V1` (inventario, reservas, garantías, retenciones, bitácora) | ✅ | aplicado en PG 16.16 |
| Seed `V2` (4 habitaciones, 2 tarifas, 730 precios, roles, huéspedes) | ✅ | `pms` con 730 filas de `precio_noche` |
| `rh_sin_solape` (`EXCLUDE USING gist` + `btree_gist`) | ✅ | `SinSolapamientoIT` 5/5, `SQLState 23P01` |
| M1 con 100 hilos → 1 confirmación, 99 rechazos | ✅ | `#concurrenciaCienHilos` |
| `GET /health` (sonda JDBC + nº de migraciones) | ✅ | `SaludControllerTest` + smoke real |
| Matriz de trazabilidad generada (45 filas + brechas) | ✅ | `pms-generator` en `process-classes` |
| Consola Angular 22 (estados operativo/degradado/sin conexión) | ✅ | 5 tests, build 234 kB |
| CI (PostgreSQL 16 como servicio + diff de matriz + job Angular) | ✅ escrito | `.github/workflows/ci.yml` (aún no ejecutado en GitHub) |

Build de referencia: `.\mvnw.cmd clean verify` → `BUILD SUCCESS` con 10 tests de
modelo, 1 de backend y 5 de integración (0 omitidos) contra PostgreSQL 16.16.

## Entorno local en el que se verificó

- JDK 23 compilando con `--release 21`; Maven 3.9.9 vía wrapper del repo.
- Node 26 / npm 11.6.2; Angular 22 con Vitest (`@angular/build:unit-test`).
- **PostgreSQL 16.16 portable** (no hay Docker; `winget` pide UAC y quedó
  pendiente la instalación permanente):
  - binarios: `%TEMP%\opencode\pg16\pgsql\bin`
  - datos: `%TEMP%\opencode\pg16\data` (auth `trust`, usuario `postgres`)
  - arrancar: `& "$env:TEMP\opencode\pg16\pgsql\bin\pg_ctl.exe" -D "$env:TEMP\opencode\pg16\data" -l "$env:TEMP\opencode\pg16\pg.log" start`
  - parar: `& "$env:TEMP\opencode\pg16\pgsql\bin\pg_ctl.exe" -D "$env:TEMP\opencode\pg16\data" stop`
- Base `pms` creada y migrada (2 migraciones). Los IT usan la base `postgres`
  con esquemas desechables `pms_it_<hex>`.

## Cómo reproducir la verificación

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-23'      # o cualquier JDK 21+
$env:PMS_TEST_DB_URL='jdbc:postgresql://localhost:5432/postgres'
.\mvnw.cmd -B clean verify

cd pms-frontend
npm.cmd ci; npm.cmd test -- --watch=false; npm.cmd run build
```

Smoke de la API:

```powershell
java -jar pms-backend\target\pms-backend-0.1.0-SNAPSHOT-ejecutable.jar
curl http://localhost:8080/health
# {"estado":"UP",...,"baseDatos":{"migraciones":2,"estado":"UP"}}
```

## Mapa de artefactos

| Módulo | Contenido |
|---|---|
| `pms-model` | `.ecore`, `.ocl` (especificación), `CatalogoInvariantes` (ejecución), `FactoryReservas`, `V`, fixture `.xmi` |
| `pms-generator` | `GeneradorTrazabilidad` → `pms-docs/trazabilidad.md`; falla si OCL y Java divergen |
| `pms-backend` | Spring Boot 3.5.3, Flyway `V1`/`V2`, `SaludController`, `SinSolapamientoIT` |
| `pms-frontend` | Angular 22 standalone: `salud.service`, `salud.component`, proxy `/health` → 8080 |
| `pms-docs` | matriz de trazabilidad generada |
| `docs/` | `verificacion-s0.md` y ADR-001/002/003 |

## Decisiones vigentes

- **ADR-001:** EMF dinámico en S0; sin clases Java generadas del metamodelo.
- **ADR-002:** el `.ocl` es normativo y la ejecución vive en Java (el motor OCL
  clásico de Maven Central no compara `EDate` ni tiene `daysBetween`).
- **ADR-003:** PostgreSQL 16 nativo y `rh_sin_solape` como mecanismo real de
  concurrencia; OCL nunca decide la carrera.
- La entrega se limitó a S0 completo (modelo + OCL + esqueleto backend) más el
  esquema y la garantía de S1–S2.

## Deuda y brechas conocidas

- **Sin API de negocio:** faltan disponibilidad con precios (S1) y
  crear/modificar/cancelar reserva (S2), además de sus pantallas.
- La matriz declara 2/45 invariantes con prueba automatizada y 18/45 con
  refuerzo en base de datos; el resto se valida en modelo/servicio.
- Tablas de estancia, folio, cargo, pago, bloqueos y grupos: modeladas, no
  persistidas (S3/S7).
- `SinSolapamientoIT` se desactiva en silencio sin PostgreSQL: hay que mirar
  `Tests run: 5, Skipped: 0` para afirmar que M1 está verificado.
- Instalación permanente de PostgreSQL pendiente (requiere UAC); hoy se usa la
  copia portable en `%TEMP%`.
- La `distributionUrl` del wrapper y el bit ejecutable de `mvnw` se corrigieron a
  mano: no regenerar el wrapper.
- El CI está escrito pero **no se ha ejecutado en GitHub** (repo local, sin
  remoto).

## Siguiente paso propuesto

1. `DisponibilidadService` + `GET /disponibilidad` (rango, tipo, tarifa) con
   precios por noche y validación de las invariantes de RN-04.
2. `ReservaService` + endpoints de crear/modificar/cancelar; mapear `23P01` a
   `409 Conflict` y reutilizar `SinSolapamientoIT` como gate.
3. Pantallas de reserva en Angular sobre `proxy.conf.json`.
4. Sumar pruebas de invariantes a la matriz (hoy 2/45) para reducir la brecha.

## Bitácora de la última sesión

- Verificado el esquema y la concurrencia en PostgreSQL 16.16 real (antes solo
  había evidencia condicional).
- Corregido `SinSolapamientoIT#permiteFechasContiguas`: reutilizaba el mismo
  `codigo_confirmacion` y violaba el `UNIQUE`.
- Detectado que `repackage` sin `<classifier>` rompe el escaneo de
  `classpath:db/migration` y hace fallar los 5 IT; el jar ejecutable pasó a
  `*-ejecutable.jar`.
- Añadido `AGENTS.md` con comandos verificados y trampas del entorno.
- Fijado `cli.analytics: false` en `angular.json` para que la CLI no escriba
  UUIDs de máquina en el repo.
