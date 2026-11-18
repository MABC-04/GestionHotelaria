# AGENTS.md

PMS de reservas hoteleras con enfoque MDD: metamodelo Ecore + catálogo OCL como
especificación, esquema PostgreSQL que impide la sobreventa y backend Spring
Boot. Entrega 1 = Sprints 0–2 (solo `/health`: **no hay API CRUD todavía**).

Documento rector: `pms-reservas-plan-build.md` (§3 modelo, §4 invariantes, §6
sprints). Requisitos de origen: `analisis_sistema_reservas_hotel.md`. Los
detalles de verificación viven en `docs/verificacion-s0.md` y las decisiones en
`docs/adr/`.

## Comandos (PowerShell, desde la raíz)

```powershell
.\mvnw.cmd -B verify                 # reactor: modelo + generador + backend
.\mvnw.cmd -B -pl pms-model test      # solo el modelo (10 tests)
.\mvnw.cmd -B -pl pms-backend verify  # backend + IT (requiere PostgreSQL)
```

- Requiere JDK 21+ (se compila con `--release 21`). Comprueba `.\mvnw.cmd -v`;
  si no ve el JDK, define `$env:JAVA_HOME` en la sesión.
- PowerShell parte los `-D` con punto (`-Dmaven=3.9.9` → fase `.9.9`). Cita el
  argumento completo: `"-Dit.test=SinSolapamientoIT#rechazaSolapamiento"`.
- Un test suelto: `.\mvnw.cmd -B -pl pms-model test -Dtest=FixtureValidacionTest`.
  Un IT suelto: `"-Dit.test=SinSolapamientoIT#concurrenciaCienHilos"` (con
  `-pl pms-backend verify`).
- El build escribe avisos de Mockito/JDK en stderr y PowerShell puede devolver
  código ≠ 0 con `BUILD SUCCESS`. Decide por el log, no por `$LASTEXITCODE`.
- Usa `npm.cmd` en el frontend (`npm.ps1` está bloqueado por ExecutionPolicy).

## PostgreSQL y pruebas de integración

- Los IT (`*IT.java`) los ejecuta failsafe en `verify`; surefire los excluye. Sin
  PostgreSQL **se desactivan en silencio**: si no ves `Tests run: 5, Skipped: 0`,
  RN-01 no está verificado.
- Apunta `PMS_TEST_DB_URL` a la base `postgres`, no a `pms`: cada ejecución crea
  un esquema `pms_it_<hex>` y lo borra. Usuario/clave por defecto `postgres`.
- V1 ejecuta `CREATE EXTENSION IF NOT EXISTS btree_gist`: el usuario necesita
  permisos de superusuario.
- La garantía de RN-01 es la restricción `rh_sin_solape` (`EXCLUDE USING gist`,
  rango `'[)'`), no el OCL. El rechazo esperado es `SQLState 23P01`.
- Cada reserva de prueba necesita `codigo_confirmacion` único (`varchar(8)` con
  `UNIQUE`); tests que insertan dos reservas deben generar dos códigos.
- `V1__inventario.sql` y `V2__datos_iniciales.sql` ya se aplicaron en bases
  existentes: no las edites (Flyway valida checksums y rompe el arranque). Añade
  `V3__...`.
- Local sin Docker: instala PostgreSQL 16 nativo o usa los binarios portables de
  EnterpriseDB (`initdb`); `winget` requiere UAC.

## La regla de los dos artefactos (fuente habitual de fallos)

Cada invariante existe **dos veces** y debe coincidir:

1. `pms-model/src/main/resources/model/reservas.ocl` — especificación normativa.
2. `.../validacion/CatalogoInvariantes.java` — implementación ejecutable.

`CatalogoOclTest` (45 y 45) y `GeneradorTrazabilidad` fallan el build si
divergen. Además:

- `pms-docs/trazabilidad.md` es **generado** en `process-classes`; nunca lo
  edites a mano. Tras cambiar invariantes o los mapas de `GeneradorTrazabilidad`,
  regenéralo con `.\mvnw.cmd -pl pms-generator -am process-classes` y commitea el
  `.md` (CI hace `git diff --exit-code`). El generador es determinista a
  propósito (sin timestamps).
- Los conteos (24 clases, 12 enums, 5 tipos, 45 invariantes) están **asertados**
  en `MetamodeloTest` y `CatalogoOclTest`: actualizarlos es una decisión
  consciente, no un ajuste de test.
- El fixture versionado `src/test/resources/fixtures/reservas-fixture.xmi`
  produce exactamente 2 violaciones (`sinSolapamientoEnHabitacion`,
  `descuentoAprobado`). Si cambias el fixture, regenéralo con el `main` de
  `FixtureReservas` (escribe ese mismo `.xmi`).

## Trampas del backend ya pagadas

- **No añadas `repackage` sin `<classifier>`**: reemplazar el artefacto principal
  por el jar gordo hace que Flyway no encuentre `classpath:db/migration` y todos
  los IT fallen con "No migrations found". El jar ejecutable es
  `target/*-ejecutable.jar`.
- El backend no depende de `pms-model`: no hay clases Java generadas del
  metamodelo. Se trabaja con EMF dinámico (`DynamicEObjectImpl`) y se leen
  atributos por nombre con `V.feature(...)`, que lanza si el nombre no existe.
  Al añadir atributos al `.ecore` hay que tocar `FactoryReservas` (conversión de
  `UUID`, `BigDecimal`, `LocalDate`, `LocalTime`, enums) o el `.xmi` devolverá
  `String`.
- El motor OCL de Maven Central (2013) no compara fechas: el `.ocl` es
  especificación y la ejecución vive en Java (ADR-002). No intentes arreglarlo
  con `OCL.newInstance()`.

## Frontend (`pms-frontend`, Angular 22 standalone)

```powershell
cd pms-frontend
npm.cmd ci                    # lockfile en modo CI
npm.cmd test -- --watch=false # Vitest vía @angular/build:unit-test
npm.cmd run build             # presupuesto 500 kB warning / 1 MB error
npm.cmd start                 # proxy /health y /actuator → localhost:8080
```

- Builder moderno (`@angular/build:application`), sin Karma: los specs usan
  `TestBed` + `HttpTestingController` con URL relativa `/health`.
- Los tests de frontend no arrancan el backend; mockean `/health`.
- `ng` puede añadir `cli.analytics` (UUID de máquina) a `angular.json`: revierte
  ese cambio antes de commitear.

## Convenciones

- Todo en español: paquetes `com.hotel.pms`, clases, columnas SQL, mensajes de
  test y documentación. No introduzcas nombres en inglés en el dominio.
- Commits en español, con cuerpo que explique el qué y el porqué.
- Repositorio con LF forzado (`.gitattributes`), salvo `*.cmd/*.bat/*.ps1`.
- No regeneres el Maven Wrapper: la `distributionUrl` se corrigió a mano y
  `mvnw` tiene el bit ejecutable en el índice.
