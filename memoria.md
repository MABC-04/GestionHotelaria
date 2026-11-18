# Memoria del proyecto — estado actual

- **Fecha:** 2026-11-17
- **Rama:** `master` (3 commits: `31368a9` entrega 1, `661b7b4` AGENTS.md,
  `8f8cfbc` memoria)
- **Alcance:** entrega 1 = Sprints 0–2 del plan `pms-reservas-plan-build.md`
- **Hito M1:** *el sistema nunca vende dos veces la misma habitación* — garantizado
  por PostgreSQL y demostrado con 100 hilos.
- **Roadmap:** ver «Siguientes fases del desarrollo»; las fases 0–D no están
  implementadas todavía.

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

## Siguientes fases del desarrollo

### Por qué hoy solo se ve "Salud del sistema"

`app.routes.ts` declara una única pantalla (`'' → SaludComponent`) y el backend
expone un único endpoint (`GET /health`). La base tiene el dominio completo
(habitaciones, tarifas, precios, reservas), pero **no hay capa de aplicación que
lo sirva por HTTP ni interfaz que lo muestre**. Es la consecuencia del alcance
cerrado en la entrega 1: S0 completo + esquema y garantía de concurrencia de
S1–S2, sin API de negocio.

Las fases siguientes cubren esa brecha. Orden recomendado: **0 → A → B**, y
después C y D en paralelo con el trabajo transversal.

### Fase 0 — Fundaciones de aplicación (sin UI)

- `JdbcClient` para consultas y escrituras; DTOs como `record` en
  `com.hotel.pms.<dominio>`; nada de entidades JPA (ver decisión pendiente 1).
- `GlobalExceptionHandler`: `SQLState 23P01` → `409 Conflict` con mensaje
  utilizable; validación → `400` con detalle por campo; `404` explícito.
- Convención de API: prefijo `/api`, JSON en español (`fechaEntrada`,
  `precioTotal`), fechas ISO `YYYY-MM-DD`, importes `BigDecimal`, horas de la
  propiedad en `America/Guayaquil`.
- `spring-boot-starter-validation` y un test de contrato por endpoint.

**Terminado cuando:** existe un endpoint trivial (`GET /api/propiedad`) con su
test y el manejador de errores tiene prueba propia.

### Fase A — S1: inventario y disponibilidad (solo lectura)

Backend:

- `GET /api/propiedad` — moneda, zona horaria, horas de check-in/out y hora
  límite sin garantía.
- `GET /api/habitaciones` — número, piso, tipo, estado de limpieza y
  `fueraDeServicio`.
- `GET /api/tarifas?desde=&hasta=` — plan, política de cancelación, estancia
  mínima/máxima, cierre de llegada y vigencia.
- `GET /api/disponibilidad?desde=&hasta=&tipo=` — por habitación libre: noches,
  precio de cada noche, subtotal, impuestos y total.

Reglas que debe respetar:

- Libre = sin línea activa solapada (`daterange && daterange` sobre
  `reserva_habitacion` con `estado_activo`) y `fuera_de_servicio = false`.
- RN-04: si falta `precio_noche` para alguna noche, la respuesta lo declara
  (`sinPrecio`) en lugar de inventar una tarifa.
- Rango inválido (`desde >= hasta`) → `400`; estancia menor que
  `estancia_minima` → se informa en la respuesta, no se oculta.

Frontend:

- Rutas nuevas: `'' → TableroComponent`, `/disponibilidad`, `/salud` (la sonda
  deja de ser la portada).
- Tablero: ocupación de hoy, habitaciones por estado de limpieza, llegadas y
  salidas del día.
- Disponibilidad: buscador de fechas y tipo; tabla de habitaciones libres con
  desglose de precio y botón "Reservar" (aún inerte hasta la fase B).

**Terminado cuando:** para un rango de 3 noches la pantalla muestra las
habitaciones libres con el total correcto y un `DisponibilidadIT` cubre
solapamiento, fechas contiguas, tipo sin precio y rango inválido.

### Fase B — S2: reservas (M1 visible en pantalla)

Backend:

- `POST /api/reservas` — reserva + líneas en **una transacción**; total calculado
  desde `precio_noche`; `codigo_confirmacion` de 8 caracteres con reintento ante
  colisión.
- `GET /api/reservas?desde=&hasta=&estado=`, `GET /api/reservas/{id}`.
- `PATCH /api/reservas/{id}` — fechas o habitación; pasa otra vez por la
  restricción de exclusión.
- `POST /api/reservas/{id}/cancelar` — aplica `politica_cancelacion`
  (horas límite, cargo por noches o porcentaje) y libera inventario.

Reglas que debe respetar:

- RN-01: la carrera la resuelve PostgreSQL; el servicio traduce `23P01` a `409`.
- RN-09: descuento > 10 % exige `aprobacion_descuento` con porcentaje suficiente
  y aprobador con `DESCUENTO_APROBAR`; si no, `400`.
- RN-06: `SIN_GARANTIA` fija `hora_limite_llegada` a la hora límite de la
  propiedad (18:00 por defecto).
- RN-11/RN-12: `modificada_por` obligatorio y registro en `bitacora` (append-only)
  en crear, modificar y cancelar.

Frontend:

- `/reservas`: lista con filtros por fecha y estado, y ficha de detalle.
- Formulario de alta desde disponibilidad con la habitación preseleccionada.
- Cancelación con confirmación y cargo estimado; el `409` se muestra como aviso
  claro ("esa habitación ya está vendida en esas fechas").

**Terminado cuando:** desde la UI se crea una reserva para la 101 y un segundo
intento en las mismas fechas recibe `409` visible; `ReservaIT` cubre alta feliz,
solape, descuento sin aprobación, sin garantía y cancelación con cargo.

### Fase C — S3: estancia, folio y cobros

- Check-in/check-out, cambios de estado de habitación y autorización de
  supervisor (RN-07: solo `LIMPIA`/`INSPECCIONADA`), late check-out (RN-02).
- Persistir las tablas ya modeladas: `estancia`, `folio`, `cargo`, `pago`
  (pago simulado; sin pasarela).
- UI: acciones de check-in/out desde la ficha de reserva y folio con cargos.

**Terminado cuando:** el ciclo reserva → check-in → cargos → check-out se
completa desde la interfaz y el folio cuadra con el total de la reserva.

### Fase D — S6/S7: operación

- Bloqueos de habitación (RN-01b) y grupos con cut-off (`minimoHabitaciones`).
- Cierre del día (RN-11) y bloqueo de modificaciones retroactivas.
- Retenciones de inventario con TTL de 10 minutos (CU-02) y job de liberación.
- No-show nocturno (RN-08) y cargos automáticos.
- Channel manager/OTA y pasarela de pago (integración, no construcción).
- UI: tape chart arrastrable, cierre del día y panel de ocupación/ADR/RevPAR.

### Trabajo transversal (desde el inicio de la fase B)

- **Autenticación y permisos:** `rol`/`usuario` ya existen con permisos en BD;
  falta sesión y autorización por permiso (`DESCUENTO_APROBAR`,
  `EXCEPCION_CIERRE`, etc.). Varias invariantes dependen del rol, no del dato.
- **Cobertura de invariantes:** hoy 2/45 con prueba automatizada. Objetivo ≥ 60 %
  al cerrar B (todas las de reserva) y 100 % al cerrar C; la matriz lo refleja
  sola.
- **CI real:** crear remoto en GitHub y ejecutar el workflow (hoy escrito pero
  nunca ejecutado).
- **Migraciones:** todo cambio de esquema entra como `V3__...` en adelante.

### Decisiones pendientes antes de la fase 0

1. **Acceso a datos:** `JdbcClient` (recomendado, coherente con lo verificado y
   con SQL explícito) vs JPA/Hibernate.
2. **Portada:** mover la salud a `/salud` y usar el tablero como inicio
   (recomendado) vs dejar la salud como portada.
3. **Seed de demo:** migración `V3` con reservas de los próximos días para que el
   tablero no salga vacío (recomendado) vs mantener el seed sin reservas.
4. **Autenticación en B:** Spring Security con JWT vs usuario de sesión simulado
   mientras se construye la UI.

### Riesgos a vigilar en estas fases

- Noches sin `precio_noche` fuera de 2026: el seed solo cubre ese año.
- Colisiones de `codigo_confirmacion` (8 caracteres) bajo carga: reintentar.
- Escribir directo en `reserva_habitacion` sin pasar por el trigger de
  `estado_activo` deja líneas activas fantasma.
- Cancelar una reserva `EN_CASA` con cargo: definir si se permite y con qué
  permiso antes de implementarlo.
- Los IT se desactivan en silencio sin PostgreSQL: revisar `Skipped: 0`.


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
