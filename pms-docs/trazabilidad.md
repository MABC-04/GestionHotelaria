# Matriz de trazabilidad

> Documento **generado automáticamente** por `pms-generator` en la fase `process-classes`. No editar a mano.
> Regenerar con `./mvnw -pl pms-generator process-classes`.

- **Versión:** 0.1.0-SNAPSHOT (entrega 1, Sprints 0-2)
- **Metamodelo:** `reservas.ecore` (24 clases, 12 enumeraciones, 5 tipos de dato)
- **Especificación:** `reservas.ocl` (45 invariantes en 12 contextos)
- **Implementación:** `CatalogoInvariantes` (45 validadores Java)
- **Persistencia:** `V1__inventario.sql` + `V2__datos_iniciales.sql`

## Resumen de enforcement

| Mecanismo | Invariantes | Cobertura |
|---|---:|---:|
| Modelo (validador Java) | 45 | 100 % |
| Base de datos (CHECK, UNIQUE, EXCLUDE, trigger) | 18 | 40 % |
| Prueba automatizada | 2 | 4 % |

> El invariante OCL **nunca** es el mecanismo de concurrencia. La carrera por la última habitación la resuelve PostgreSQL con `rh_sin_solape` y se demuestra en `SinSolapamientoIT#concurrenciaCienHilos`.

## Distribución por regla de negocio

| Regla | Invariantes |
|---|---:|
| RN-01 | 1 |
| RN-01b | 1 |
| RN-02 | 3 |
| RN-03 | 1 |
| RN-04 | 5 |
| RN-05 | 4 |
| RN-06 | 3 |
| RN-07 | 2 |
| RN-08 | 2 |
| RN-09 | 2 |
| RN-10 | 3 |
| RN-11 | 2 |
| RN-12 | 2 |
| RN-13 | 1 |
| RN-14 | 2 |
| RN-15 | 3 |
| §4.2 (núcleo) | 8 |

## Matriz completa

| Regla | Invariante | Contexto | Modelo | Base de datos | Prueba |
|---|---|---|:---:|---|:---:|
| RN-01 | `sinSolapamientoEnHabitacion` | `ReservaHabitacion` | ✅ | `rh_sin_solape` (EXCLUDE gist) | `SinSolapamientoIT#rechazaSolapamiento`, `#concurrenciaCienHilos`; `FixtureValidacionTest` |
| RN-01b | `bloqueoNoSolapaReserva` | `BloqueoHabitacion` | ✅ | — | — |
| RN-02 | `horasEstandar` | `Propiedad` | ✅ | `propiedad_horas_estandar` | — |
| RN-02 | `lateCheckOutRequiereAutorizacion` | `Estancia` | ✅ | — | — |
| RN-02 | `earlyCheckInRequiereAutorizacion` | `Estancia` | ✅ | — | — |
| RN-03 | `datosMinimos` | `Reserva` | ✅ | `NOT NULL` + `reserva_fechas` | — |
| RN-04 | `todasLasNochesTienenPrecio` | `ReservaHabitacion` | ✅ | — | — |
| RN-04 | `tarifaVigente` | `ReservaHabitacion` | ✅ | — | — |
| RN-04 | `estanciaMinimaCumplida` | `ReservaHabitacion` | ✅ | — | — |
| RN-04 | `estanciaMaximaCumplida` | `ReservaHabitacion` | ✅ | — | — |
| RN-04 | `cierreDeLlegada` | `ReservaHabitacion` | ✅ | — | — |
| RN-05 | `flexible` | `PoliticaCancelacion` | ✅ | `politica_flexible` | — |
| RN-05 | `moderada` | `PoliticaCancelacion` | ✅ | `politica_moderada` | — |
| RN-05 | `noReembolsable` | `PoliticaCancelacion` | ✅ | `politica_no_reembolsable` | — |
| RN-05 | `cargoCancelacionConforme` | `Reserva` | ✅ | — | — |
| RN-06 | `sinGarantiaTieneHoraLimite` | `Reserva` | ✅ | — | — |
| RN-06 | `expiradaSoloSinGarantia` | `Reserva` | ✅ | — | — |
| RN-06 | `confirmadaRequiereGarantia` | `Reserva` | ✅ | — | — |
| RN-07 | `habitacionLista` | `Estancia` | ✅ | — | — |
| RN-07 | `habitacionNoBloqueada` | `Estancia` | ✅ | — | — |
| RN-08 | `noShowTrasHoraLimite` | `Reserva` | ✅ | — | — |
| RN-08 | `noShowConCargo` | `Reserva` | ✅ | — | — |
| RN-09 | `descuentoAprobado` | `Reserva` | ✅ | — | `FixtureValidacionTest` (violación esperada) |
| RN-09 | `descuentoEnRango` | `Reserva` | ✅ | `descuento_porcentaje` CHECK 0..100 | — |
| RN-10 | `minimoHabitaciones` | `BloqueoGrupo` | ✅ | — | — |
| RN-10 | `cutoffValido` | `GrupoReserva` | ✅ | — | — |
| RN-10 | `vencidoLiberaInventario` | `GrupoReserva` | ✅ | — | — |
| RN-11 | `cierreNoRetrocede` | `Propiedad` | ✅ | — | — |
| RN-11 | `noModificableTrasCierre` | `Reserva` | ✅ | — | — |
| RN-12 | `entradaCompleta` | `Bitacora` | ✅ | `bitacora_cambio_real` + trigger | — |
| RN-12 | `modificacionesAuditadas` | `Reserva` | ✅ | trigger append-only (parcial) | — |
| RN-13 | `fueraDeServicioSinReservasActivas` | `Habitacion` | ✅ | — | — |
| RN-14 | `overbookingControlado` | `Propiedad` | ✅ | `propiedad_overbooking` (parcial) | — |
| RN-14 | `overbookingDesactivadoPorDefecto` | `Propiedad` | ✅ | `propiedad_overbooking` | — |
| RN-15 | `consentimientoAntesDeTratar` | `Huesped` | ✅ | `huesped_consentimiento` | — |
| RN-15 | `derechoDeSupresion` | `Huesped` | ✅ | `huesped_supresion` | — |
| RN-15 | `correoValido` | `Huesped` | ✅ | `huesped_correo` | — |
| §4.2 | `fechasCoherentes` | `Reserva` | ✅ | `reserva_fechas` | — |
| §4.2 | `codigoConfirmacionUnico` | `Reserva` | ✅ | `codigo_confirmacion` UNIQUE | — |
| §4.2 | `fechasDentroDeReserva` | `ReservaHabitacion` | ✅ | — | — |
| §4.2 | `tipoHabitacionCoherente` | `ReservaHabitacion` | ✅ | — | — |
| §4.2 | `ttlDiezMinutos` | `RetencionInventario` | ✅ | `retencion_ttl` | — |
| §4.2 | `expiradaNoConsumible` | `RetencionInventario` | ✅ | `retencion_expirada` | — |
| §4.2 | `totalConsistente` | `Reserva` | ✅ | — | — |
| §4.2 | `capacidadRespetada` | `ReservaHabitacion` | ✅ | — | — |

## Brechas declaradas

Una brecha no es un olvido: es una decisión de alcance. Las invariantes sin mecanismo en base de datos se validan en el modelo y en el servicio; las que no tienen prueba todavía son deuda técnica registrada para los sprints siguientes.

- **Sin refuerzo en base de datos (27):** `bloqueoNoSolapaReserva`, `lateCheckOutRequiereAutorizacion`, `earlyCheckInRequiereAutorizacion`, `todasLasNochesTienenPrecio`, `tarifaVigente`, `estanciaMinimaCumplida`, `estanciaMaximaCumplida`, `cierreDeLlegada`, `cargoCancelacionConforme`, `sinGarantiaTieneHoraLimite`, `expiradaSoloSinGarantia`, `confirmadaRequiereGarantia`, `habitacionLista`, `habitacionNoBloqueada`, `noShowTrasHoraLimite`, `noShowConCargo`, `descuentoAprobado`, `minimoHabitaciones`, `cutoffValido`, `vencidoLiberaInventario`, `cierreNoRetrocede`, `noModificableTrasCierre`, `fueraDeServicioSinReservasActivas`, `fechasDentroDeReserva`, `tipoHabitacionCoherente`, `totalConsistente`, `capacidadRespetada`
- **Sin prueba automatizada (43):** `bloqueoNoSolapaReserva`, `horasEstandar`, `lateCheckOutRequiereAutorizacion`, `earlyCheckInRequiereAutorizacion`, `datosMinimos`, `todasLasNochesTienenPrecio`, `tarifaVigente`, `estanciaMinimaCumplida`, `estanciaMaximaCumplida`, `cierreDeLlegada`, `flexible`, `moderada`, `noReembolsable`, `cargoCancelacionConforme`, `sinGarantiaTieneHoraLimite`, `expiradaSoloSinGarantia`, `confirmadaRequiereGarantia`, `habitacionLista`, `habitacionNoBloqueada`, `noShowTrasHoraLimite`, `noShowConCargo`, `descuentoEnRango`, `minimoHabitaciones`, `cutoffValido`, `vencidoLiberaInventario`, `cierreNoRetrocede`, `noModificableTrasCierre`, `entradaCompleta`, `modificacionesAuditadas`, `fueraDeServicioSinReservasActivas`, `overbookingControlado`, `overbookingDesactivadoPorDefecto`, `consentimientoAntesDeTratar`, `derechoDeSupresion`, `correoValido`, `fechasCoherentes`, `codigoConfirmacionUnico`, `fechasDentroDeReserva`, `tipoHabitacionCoherente`, `ttlDiezMinutos`, `expiradaNoConsumible`, `totalConsistente`, `capacidadRespetada`

## Contratos que sostienen esta matriz

1. `CatalogoOclTest` — cada invariante del OCL tiene implementación y viceversa.
2. `FixtureValidacionTest` — el fixture produce exactamente 2 violaciones (`sinSolapamientoEnHabitacion`, `descuentoAprobado`).
3. `SinSolapamientoIT` — PostgreSQL aplica el esquema y rechaza solapamientos con `SQLState 23P01`, incluso con 100 hilos concurrentes.
4. `SaludControllerTest` — `/health` responde sin base de datos y reporta su estado.
