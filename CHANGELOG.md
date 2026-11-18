# Changelog

Todas las fechas están en ISO 8601. El proyecto sigue un versionado
`MAJOR.MINOR.PATCH` con hitos por entrega.

## [0.1.0] — 2026-11-17 — Entrega 1 (Sprints 0-2)

Primera entrega ejecutable del núcleo de reservas.

### Añadido

**Modelo (`pms-model`)**

- `reservas.ecore` con 24 clases, 12 enumeraciones y 5 tipos de dato
  (`UUID`, `Decimal`, `EDate`, `EDateTime`, `Hora`).
- `reservas.ocl` con 45 invariantes en Complete OCL, agrupadas por RN-01 a
  RN-15 y por el núcleo del §4.2.
- `CatalogoInvariantes`: implementación ejecutable de las 45 invariantes.
- `FactoryReservas`: (de)serialización de UUID, fechas, horas, decimales y
  literales de enum para EMF dinámico.
- Fixture de validación (`reservas-fixture.xmi`) con exactamente 2 violaciones
  esperadas: `sinSolapamientoEnHabitacion` y `descuentoAprobado`.

**Persistencia y backend (`pms-backend`)**

- Migración `V1__inventario.sql`: propiedad, cierre del día, roles, usuarios,
  bitácora append-only, inventario, tarifas, canales, huéspedes, reservas,
  garantías, retenciones y aprobación de descuentos.
- Restricción `rh_sin_solape` (`EXCLUDE USING gist`) que implementa RN-01 en la
  base de datos, con `btree_gist` y predicado parcial sobre `estado_activo`.
- Migración `V2__datos_iniciales.sql`: catálogo operativo de 2026 (sin
  reservas, para no interferir con las pruebas).
- Endpoint `GET /health` con sonda JDBC y contador de migraciones.
- `SinSolapamientoIT`: esquema aleatorio por ejecución, verificación de la
  restricción y prueba de concurrencia con 100 hilos.

**Generación y documentación**

- `pms-generator`: matriz de trazabilidad generada en `process-classes` a
  partir del OCL y del catálogo Java; falla el build si divergen.
- `pms-docs/trazabilidad.md`: 45 invariantes con su contexto, refuerzo en base
  de datos y prueba asociada, más las brechas declaradas.
- ADR-001 (modelo Ecore dinámico), ADR-002 (OCL normativo + validadores Java) y
  ADR-003 (PostgreSQL 16 y `rh_sin_solape`).
- README con puesta en marcha, criterios de done y alcance.

**Frontend (`pms-frontend`)**

- Aplicación Angular 22 (standalone, signals, Vitest).
- Pantalla de salud que consume `/health`, con estados operativo, degradado y
  sin conexión.
- Proxy de desarrollo hacia `http://localhost:8080`.
- 5 pruebas de componente y servicio.

**Build y CI**

- Maven Wrapper 3.9.9 (`mvnw`, `mvnw.cmd`).
- Jar ejecutable con `java -jar` (`target/*-ejecutable.jar`) además de
  `spring-boot:run`. El clasificador mantiene el jar fino como artefacto
  principal, que es el que las pruebas de integración escanean para encontrar
  `classpath:db/migration`.
- GitHub Actions: `mvn verify` contra PostgreSQL 16, comprobación de que la
  matriz está actualizada y build + tests de Angular.

### Verificado

- `.\mvnw.cmd verify` completo en verde sobre **PostgreSQL 16.16 real**:
  10 tests de modelo, 1 de backend y 5 de integración (sin omisiones).
- M1: 100 hilos, 1 reserva confirmada y 99 rechazos `23P01`.
- `GET /health` contra la base `pms` con las 2 migraciones aplicadas.
- `npm ci` + 5 tests de Angular + build de producción (234 kB).

### Limitaciones conocidas

- El motor OCL clásico de Maven Central (`3.3.0-v20130520-1222`) no soporta
  comparaciones ni `daysBetween` sobre `EDate`. El `.ocl` es la especificación
  normativa y la ejecución vive en Java, con tests que impiden la deriva
  (ADR-002).
- Las tablas de estancia, folio, cargo, pago, bloqueos y grupos se modelan pero
  no se persisten todavía (S3/S7).
- `SinSolapamientoIT` se omite sin PostgreSQL local; en CI es obligatorio.

### Pendiente (S3+)

- Estancia, folio, cargos y pagos.
- Bloqueos de habitación y grupos con cut-off.
- `DisponibilidadService`, `RetencionService` y `BitacoraAspect`.
- Tape chart arrastrable en el frontend.
- Integración con OTAs y pasarela de pago.
