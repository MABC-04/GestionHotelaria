# ADR-003 — PostgreSQL 16 nativo y la restricción `rh_sin_solape`

- **Estado:** aceptado
- **Fecha:** 2026-11-17
- **Contexto:** Sprint 0 (persistencia) y hito M1

## Contexto

El criterio de éxito de la entrega 1 es: **“el sistema nunca vende dos veces la
misma habitación”**, demostrable con 100 hilos compitiendo por la última
habitación: exactamente 1 reserva confirmada y 99 rechazos.

El plan descarta explícitamente que el OCL sea el mecanismo de concurrencia
(§4.4): una comprobación “leer y luego escribir” en la aplicación siempre tiene
una ventana de carrera, incluso con un lock pesimista mal tomado.

Además, el entorno de desarrollo no dispone de Docker; la decisión del equipo
fue instalar PostgreSQL 16 nativo con `winget`.

## Decisión

1. **PostgreSQL 16 nativo** en desarrollo (`winget install --id
   PostgreSQL.PostgreSQL.16`). En CI se usa el servicio `postgres:16` de GitHub
   Actions, que sí es un contenedor.
2. La regla RN-01 se implementa en la base con una **restricción de exclusión**
   sobre un rango semiabierto:

   ```sql
   CREATE EXTENSION IF NOT EXISTS btree_gist;

   ALTER TABLE reserva_habitacion
     ADD CONSTRAINT rh_sin_solape
     EXCLUDE USING gist (
       habitacion_id WITH =,
       daterange(fecha_entrada, fecha_salida, '[)') WITH &&
     ) WHERE (estado_activo AND habitacion_id IS NOT NULL);
   ```

   El rango `'[)'` permite reutilizar el día de salida como día de entrada, que
   es la semántica hotelera correcta (la noche de salida no se ocupa).
3. `estado_activo` es una columna generada en `reserva` y espejada en
   `reserva_habitacion` por trigger. PostgreSQL no admite predicados que
   consulten otra tabla en una restricción de exclusión, por eso se materializa
   el predicado “reserva viva” en la línea.
4. Las pruebas de integración usan **un esquema aleatorio por ejecución**
   (`pms_it_<hex>`) dentro de la base indicada por `PMS_TEST_DB_URL`. Así no se
   destruyen datos de desarrollo ni hace falta `DROP DATABASE`.

## Consecuencias

**Positivas**

- La invariante se cumple aunque la aplicación tenga un bug o se despliegue en
  varias instancias: la garantía está en el dato.
- El `EXCLUDE` es atómico; no hay ventana de carrera que un lock de aplicación
  pueda dejar abierta.
- El índice GiST parcial también acelera la consulta de disponibilidad.

**Negativas**

- Requiere `btree_gist` (extensión estándar, incluida en las contrib de
  PostgreSQL).
- Cancelar una reserva dispara una actualización de sus líneas; es barato pero
  hay que recordar que `estado_activo` es derivado, no un dato de negocio.
- Las pruebas de integración se marcan como *skipped* si no hay PostgreSQL
  local. En CI son obligatorias; localmente `mvn verify` sigue siendo verde.

## Alternativas descartadas

1. **Validar con `SELECT` antes de insertar.** Ventana de carrera clásica.
2. **Lock de aplicación (`SELECT ... FOR UPDATE` sobre la habitación).**
   Serializa correctamente si se toma siempre, pero es fácil de olvidar y no
   protege escrituras directas ni otras instancias que no usen el mismo código.
3. **Testcontainers en local.** El equipo no tiene Docker; además el servicio de
   CI ya cubre el aislamiento.
