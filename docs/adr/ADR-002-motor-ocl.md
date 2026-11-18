# ADR-002 — OCL normativo y validadores Java ejecutables

- **Estado:** aceptado
- **Fecha:** 2026-11-17
- **Contexto:** Sprint 0 (catálogo de invariantes)

## Contexto

El plan (§4) pide 45 invariantes OCL y un validador que las ejecute con
`OCL.newInstance()` sobre el fixture. Antes de escribir el catálogo se hizo un
*spike* con la única versión de Eclipse OCL publicada en Maven Central:

| Coordenada | Versión |
|---|---|
| `org.eclipse.ocl:ecore` | `3.3.0-v20130520-1222` |
| `org.eclipse:ocl` | `3.4.0-v20140524-1358` |
| `org.eclipse.ocl:common` | `1.2.0-v20140528-1458` |

Resultados del spike (JDK 23, `--release 21`):

| Expresión | Resultado |
|---|---|
| `self.estado = EstadoReserva::CONFIRMADA` | ✅ |
| `self.codigo.size() = 8` | ✅ |
| `self.descuento > 10` | ✅ |
| `self.entrada < self.salida` (`LocalDate`) | ❌ `Cannot find operation (<(EDate))` |
| `self.fechaSalida.daysBetween(self.fechaEntrada)` | ❌ operación inexistente |

El motor de 2013 no define el orden sobre `EDate`/`EDateTime` ni `daysBetween`;
los operadores `<`, `<=`, `>`, `>=`, `plusDays`, `minusDays`, `toLocalDate` y
`toLocalTime` no existen para esos tipos. Eso afecta a unas 20 invariantes del
catálogo, entre ellas RN-01 (solapamiento), RN-04 (noches), RN-08 (no-show) y
RN-11 (cierre).

Las versiones modernas de OCL (6.x) se distribuyen por repositorios p2 de
Eclipse, no por Maven Central, y exigirían Tycho o un mirror p2 en el build.

## Decisión

1. `pms-model/src/main/resources/model/reservas.ocl` se mantiene como
   **especificación normativa** revisable, escrita en Complete OCL.
2. Su ejecución se implementa en **Java** en `CatalogoInvariantes`, con el mismo
   nombre y la misma regla para cada invariante.
3. `CatalogoOclTest` verifica la correspondencia **en ambos sentidos** (nada
   documentado sin implementar, nada implementado sin documentar).
4. `GeneradorTrazabilidad` vuelve a comprobar la correspondencia y **falla el
   build** si hay deriva.

## Consecuencias

**Positivas**

- Las comparaciones de fechas son exactas (`LocalDate`, `ChronoUnit.DAYS`).
- No se depende de un motor abandonado para ejecutar reglas de negocio.
- La especificación sigue siendo legible por negocio y auditable.
- El catálogo Java es depurable, tipado en lo posible y cubierto por tests.

**Negativas**

- Hay dos artefactos que mantener sincronizados. Mitigado por dos tests que
  fallan el build ante cualquier deriva.
- No hay evaluación dinámica de OCL introducido por el usuario.

**Revisión futura**

Si se adopta OCL 6.x (Eclipse 2024+), se puede sustituir el catálogo Java por
un intérprete OCL con operaciones registradas para `EDate`. La especificación
`.ocl` ya está preparada: solo habría que añadir el *environment* con las
operaciones de fecha y comparar los resultados contra el catálogo actual
(prueba diferencial).

## Alternativas descartadas

1. **Fechas como `EString` ISO.** Permite comparar lexicográficamente, pero
   pierde la semántica de fecha y sigue sin `daysBetween`/`plusDays`; habría que
   implementar esas operaciones como `def` con aritmética de cadenas.
2. **Registrar operaciones de fecha en el motor clásico.** Depende de APIs
   internas no documentadas, no cubre `EDateTime` y no es portable.
3. **Adoptar OCL 6.x vía p2/Tycho.** Añade un repositorio y un ciclo de build
   distintos por un beneficio que el catálogo Java ya cubre.
