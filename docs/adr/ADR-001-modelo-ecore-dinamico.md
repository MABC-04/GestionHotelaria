# ADR-001 — Modelo Ecore dinámico durante el Sprint 0

- **Estado:** aceptado
- **Fecha:** 2026-11-17
- **Contexto:** Sprint 0 (modelo y validación)

## Contexto

El pipeline MDD del plan genera, a partir de `reservas.ecore`, entidades JPA,
DTOs, controladores y cliente TypeScript. Ese paso (G1 del §5.3) pertenece a los
sprints de implementación. En el Sprint 0 solo se necesita **validar el modelo**:
cargar el metamodelo, construir el fixture y ejecutar las invariantes.

Generar las 24 clases Java ahora tiene un coste real:

- exige configurar EMF codegen (genmodel, plantillas, `maven-plugin` de EMF) y
  regenerar en cada cambio del `.ecore`;
- introduce código generado en el repositorio que nadie ha revisado;
- las clases generadas no son entidades JPA todavía, así que el backend tendría
  que envolverlas de todos modos.

## Decisión

Cargar `reservas.ecore` en tiempo de ejecución y trabajar con **EMF dinámico**
(`DynamicEObjectImpl`). Las instancias se crean con `EcoreUtil.create(eClass)` y
se leen por nombre de característica mediante la fachada `V`, que falla
inmediatamente si el nombre no existe.

`FactoryReservas` extiende `EcoreFactoryImpl` para que EMF sepa convertir
`UUID`, `BigDecimal`, `LocalDate`, `LocalDateTime`, `LocalTime` y literales de
enum desde y hacia texto. Sin ella, un `.xmi` cargado devuelve `String` en esos
atributos y la validación se rompe en silencio.

## Consecuencias

**Positivas**

- El modelo es la única fuente de verdad; no hay código generado sin revisar.
- El fixture y los validadores se ejecutan sin compilar 24 clases.
- Los tests `MetamodeloTest` y `FixtureValidacionTest` cubren serialización y
  navegación, que es donde falla el enfoque dinámico.

**Negativas**

- No hay tipos en tiempo de compilación: un nombre de atributo mal escrito solo
  falla al ejecutar. Mitigado porque `V.feature(...)` lanza
  `IllegalStateException` en lugar de devolver `null`.
- El IDE no autocompleta las propiedades del dominio.

**Migración**

Cuando el backend necesite entidades persistentes (Sprint 1), se añade el
genmodel y se generan las clases. `CatalogoInvariantes` seguirá funcionando con
`V` si las clases generadas implementan `EObject`; el contrato de `V` (leer por
nombre) no cambia. La decisión se revisará en ese momento con un ADR nuevo.

## Alternativas descartadas

1. **Generar clases Java ya.** Más andamiaje del necesario para S0 y ruido de
   regeneración en cada ajuste del metamodelo.
2. **POJOs escritos a mano.** Duplican el metamodelo y garantizan deriva.
3. **Validar solo con el editor de Eclipse.** No es reproducible en CI.
