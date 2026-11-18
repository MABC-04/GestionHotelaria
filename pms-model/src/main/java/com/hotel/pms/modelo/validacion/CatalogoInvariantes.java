package com.hotel.pms.modelo.validacion;

import static com.hotel.pms.modelo.validacion.V.bool;
import static com.hotel.pms.modelo.validacion.V.decimal;
import static com.hotel.pms.modelo.validacion.V.entero;
import static com.hotel.pms.modelo.validacion.V.enumNombre;
import static com.hotel.pms.modelo.validacion.V.enumNombres;
import static com.hotel.pms.modelo.validacion.V.fecha;
import static com.hotel.pms.modelo.validacion.V.fechaHora;
import static com.hotel.pms.modelo.validacion.V.hora;
import static com.hotel.pms.modelo.validacion.V.id;
import static com.hotel.pms.modelo.validacion.V.referencia;
import static com.hotel.pms.modelo.validacion.V.referencias;
import static com.hotel.pms.modelo.validacion.V.texto;
import static com.hotel.pms.modelo.validacion.V.uuid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.eclipse.emf.ecore.EObject;

import com.hotel.pms.modelo.ModeloReservas;

/**
 * Implementación ejecutable del catálogo {@code reservas.ocl}.
 *
 * <p>Cada entrada se corresponde uno a uno con un invariante del documento OCL.
 * El test {@code CatalogoOclTest} verifica la correspondencia en ambos
 * sentidos, de modo que no pueda existir deriva entre especificación y
 * validación.</p>
 */
public final class CatalogoInvariantes {

    private static final List<Invariante> INVARIANTES = new ArrayList<>();

    private CatalogoInvariantes() {
    }

    public static List<Invariante> todos() {
        return List.copyOf(INVARIANTES);
    }

    private static void inv(String regla, String nombre, BiConsumer<ModeloReservas, Reporte> chequeo) {
        INVARIANTES.add(new Invariante(regla, nombre, chequeo));
    }

    static {
        // ---------------------------------------------------------------- RN-01
        inv("RN-01", "sinSolapamientoEnHabitacion", CatalogoInvariantes::sinSolapamientoEnHabitacion);
        inv("RN-01b", "bloqueoNoSolapaReserva", CatalogoInvariantes::bloqueoNoSolapaReserva);
        // ---------------------------------------------------------------- RN-02
        inv("RN-02", "horasEstandar", CatalogoInvariantes::horasEstandar);
        inv("RN-02", "lateCheckOutRequiereAutorizacion", CatalogoInvariantes::lateCheckOutRequiereAutorizacion);
        inv("RN-02", "earlyCheckInRequiereAutorizacion", CatalogoInvariantes::earlyCheckInRequiereAutorizacion);
        // ---------------------------------------------------------------- RN-03
        inv("RN-03", "datosMinimos", CatalogoInvariantes::datosMinimos);
        // ---------------------------------------------------------------- RN-04
        inv("RN-04", "todasLasNochesTienenPrecio", CatalogoInvariantes::todasLasNochesTienenPrecio);
        inv("RN-04", "tarifaVigente", CatalogoInvariantes::tarifaVigente);
        inv("RN-04", "estanciaMinimaCumplida", CatalogoInvariantes::estanciaMinimaCumplida);
        inv("RN-04", "estanciaMaximaCumplida", CatalogoInvariantes::estanciaMaximaCumplida);
        inv("RN-04", "cierreDeLlegada", CatalogoInvariantes::cierreDeLlegada);
        // ---------------------------------------------------------------- RN-05
        inv("RN-05", "flexible", (m, r) -> politicaFlexible(m, r, "flexible", "FLEXIBLE"));
        inv("RN-05", "moderada", CatalogoInvariantes::politicaModerada);
        inv("RN-05", "noReembolsable", CatalogoInvariantes::politicaNoReembolsable);
        inv("RN-05", "cargoCancelacionConforme", CatalogoInvariantes::cargoCancelacionConforme);
        // ---------------------------------------------------------------- RN-06
        inv("RN-06", "sinGarantiaTieneHoraLimite", CatalogoInvariantes::sinGarantiaTieneHoraLimite);
        inv("RN-06", "expiradaSoloSinGarantia", CatalogoInvariantes::expiradaSoloSinGarantia);
        inv("RN-06", "confirmadaRequiereGarantia", CatalogoInvariantes::confirmadaRequiereGarantia);
        // ---------------------------------------------------------------- RN-07
        inv("RN-07", "habitacionLista", CatalogoInvariantes::habitacionLista);
        inv("RN-07", "habitacionNoBloqueada", CatalogoInvariantes::habitacionNoBloqueada);
        // ---------------------------------------------------------------- RN-08
        inv("RN-08", "noShowTrasHoraLimite", CatalogoInvariantes::noShowTrasHoraLimite);
        inv("RN-08", "noShowConCargo", CatalogoInvariantes::noShowConCargo);
        // ---------------------------------------------------------------- RN-09
        inv("RN-09", "descuentoAprobado", CatalogoInvariantes::descuentoAprobado);
        inv("RN-09", "descuentoEnRango", CatalogoInvariantes::descuentoEnRango);
        // ---------------------------------------------------------------- RN-10
        inv("RN-10", "minimoHabitaciones", CatalogoInvariantes::minimoHabitaciones);
        inv("RN-10", "cutoffValido", CatalogoInvariantes::cutoffValido);
        inv("RN-10", "vencidoLiberaInventario", CatalogoInvariantes::vencidoLiberaInventario);
        // ---------------------------------------------------------------- RN-11
        inv("RN-11", "cierreNoRetrocede", CatalogoInvariantes::cierreNoRetrocede);
        inv("RN-11", "noModificableTrasCierre", CatalogoInvariantes::noModificableTrasCierre);
        // ---------------------------------------------------------------- RN-12
        inv("RN-12", "entradaCompleta", CatalogoInvariantes::entradaCompleta);
        inv("RN-12", "modificacionesAuditadas", CatalogoInvariantes::modificacionesAuditadas);
        // ---------------------------------------------------------------- RN-13
        inv("RN-13", "fueraDeServicioSinReservasActivas", CatalogoInvariantes::fueraDeServicioSinReservasActivas);
        // ---------------------------------------------------------------- RN-14
        inv("RN-14", "overbookingControlado", CatalogoInvariantes::overbookingControlado);
        inv("RN-14", "overbookingDesactivadoPorDefecto", CatalogoInvariantes::overbookingDesactivadoPorDefecto);
        // ---------------------------------------------------------------- RN-15
        inv("RN-15", "consentimientoAntesDeTratar", CatalogoInvariantes::consentimientoAntesDeTratar);
        inv("RN-15", "derechoDeSupresion", CatalogoInvariantes::derechoDeSupresion);
        inv("RN-15", "correoValido", CatalogoInvariantes::correoValido);
        // ---------------------------------------------------------------- §4.2
        inv("—", "fechasCoherentes", CatalogoInvariantes::fechasCoherentes);
        inv("—", "codigoConfirmacionUnico", CatalogoInvariantes::codigoConfirmacionUnico);
        inv("—", "fechasDentroDeReserva", CatalogoInvariantes::fechasDentroDeReserva);
        inv("—", "tipoHabitacionCoherente", CatalogoInvariantes::tipoHabitacionCoherente);
        inv("—", "ttlDiezMinutos", CatalogoInvariantes::ttlDiezMinutos);
        inv("—", "expiradaNoConsumible", CatalogoInvariantes::expiradaNoConsumible);
        inv("—", "totalConsistente", CatalogoInvariantes::totalConsistente);
        inv("—", "capacidadRespetada", CatalogoInvariantes::capacidadRespetada);
    }

    // ======================================================================
    // RN-01 — Sin solapamiento
    // ======================================================================
    private static void sinSolapamientoEnHabitacion(ModeloReservas m, Reporte r) {
        for (EObject linea : m.de("ReservaHabitacion")) {
            EObject habitacion = referencia(linea, "habitacion");
            if (habitacion == null || !activa(linea)) {
                continue;
            }
            for (EObject otra : m.de("ReservaHabitacion")) {
                if (otra == linea || referencia(otra, "habitacion") != habitacion || !activa(otra)) {
                    continue;
                }
                if (!solapan(linea, otra)) {
                    continue;
                }
                // Restricción simétrica: reportar solo desde el identificador mayor.
                if (id(linea).compareTo(id(otra)) < 0) {
                    continue;
                }
                r.agregar("RN-01", "sinSolapamientoEnHabitacion", linea,
                        "La habitación " + texto(habitacion, "numero") + " se solapa con otra reserva activa entre "
                                + fecha(linea, "fechaEntrada") + " y " + fecha(linea, "fechaSalida"));
            }
        }
    }

    private static void bloqueoNoSolapaReserva(ModeloReservas m, Reporte r) {
        for (EObject bloqueo : m.de("BloqueoHabitacion")) {
            EObject habitacion = referencia(bloqueo, "habitacion");
            LocalDate inicio = fecha(bloqueo, "fechaInicio");
            LocalDate fin = fecha(bloqueo, "fechaFin");
            for (EObject linea : m.de("ReservaHabitacion")) {
                if (referencia(linea, "habitacion") != habitacion || !activa(linea)) {
                    continue;
                }
                boolean solapa = fecha(linea, "fechaEntrada").isBefore(fin) && inicio.isBefore(fecha(linea, "fechaSalida"));
                if (solapa) {
                    r.agregar("RN-01b", "bloqueoNoSolapaReserva", bloqueo,
                            "El bloqueo de la habitación " + texto(habitacion, "numero") + " se cruza con una reserva activa");
                }
            }
        }
    }

    // ======================================================================
    // RN-02 — Horas estándar
    // ======================================================================
    private static void horasEstandar(ModeloReservas m, Reporte r) {
        for (EObject p : m.de("Propiedad")) {
            LocalTime entrada = hora(p, "horaCheckIn");
            LocalTime salida = hora(p, "horaCheckOut");
            if (entrada == null || salida == null || entrada.getHour() != 15 || salida.getHour() != 12) {
                r.agregar("RN-02", "horasEstandar", p,
                        "Las horas estándar deben ser 15:00 (check-in) y 12:00 (check-out)");
            }
        }
    }

    private static void lateCheckOutRequiereAutorizacion(ModeloReservas m, Reporte r) {
        for (EObject e : m.de("Estancia")) {
            LocalDateTime salidaReal = fechaHora(e, "salidaReal");
            EObject propiedad = referencia(e, "propiedad");
            if (salidaReal == null || propiedad == null) {
                continue;
            }
            LocalTime limite = hora(propiedad, "horaCheckOut");
            if (limite != null && salidaReal.toLocalTime().isAfter(limite) && !bool(e, "autorizacionSalidaTardia")) {
                r.agregar("RN-02", "lateCheckOutRequiereAutorizacion", e,
                        "La salida real " + salidaReal.toLocalTime() + " excede la hora estándar sin autorización");
            }
        }
    }

    private static void earlyCheckInRequiereAutorizacion(ModeloReservas m, Reporte r) {
        for (EObject e : m.de("Estancia")) {
            LocalDateTime llegada = fechaHora(e, "llegadaReal");
            EObject propiedad = referencia(e, "propiedad");
            if (llegada == null || propiedad == null) {
                continue;
            }
            LocalTime estandar = hora(propiedad, "horaCheckIn");
            if (estandar != null && llegada.toLocalTime().isBefore(estandar) && !bool(e, "autorizacionSupervisor")) {
                r.agregar("RN-02", "earlyCheckInRequiereAutorizacion", e,
                        "La llegada real " + llegada.toLocalTime() + " es anterior a la hora estándar sin autorización");
            }
        }
    }

    // ======================================================================
    // RN-03 — Datos mínimos
    // ======================================================================
    private static void datosMinimos(ModeloReservas m, Reporte r) {
        for (EObject reserva : m.de("Reserva")) {
            EObject titular = referencia(reserva, "titular");
            List<String> faltantes = new ArrayList<>();
            if (titular == null) {
                faltantes.add("titular");
            } else {
                if (vacio(texto(titular, "nombre"))) {
                    faltantes.add("titular.nombre");
                }
                if (vacio(texto(titular, "correo")) && vacio(texto(titular, "telefono"))) {
                    faltantes.add("titular.correo|telefono");
                }
            }
            LocalDate entrada = fecha(reserva, "fechaEntrada");
            LocalDate salida = fecha(reserva, "fechaSalida");
            if (entrada == null || salida == null || !entrada.isBefore(salida)) {
                faltantes.add("fechas");
            }
            if (entero(reserva, "adultos") < 1) {
                faltantes.add("adultos");
            }
            if (entero(reserva, "ninos") < 0) {
                faltantes.add("ninos");
            }
            List<EObject> lineas = referencias(reserva, "lineas");
            if (lineas.isEmpty()) {
                faltantes.add("lineas");
            }
            for (EObject linea : lineas) {
                if (referencia(linea, "tarifa") == null) {
                    faltantes.add("linea.tarifa");
                }
            }
            if (!faltantes.isEmpty()) {
                r.agregar("RN-03", "datosMinimos", reserva, "Faltan datos obligatorios: " + faltantes);
            }
        }
    }

    // ======================================================================
    // RN-04 — Precio completo y restricciones
    // ======================================================================
    private static void todasLasNochesTienenPrecio(ModeloReservas m, Reporte r) {
        for (EObject linea : m.de("ReservaHabitacion")) {
            EObject tarifa = referencia(linea, "tarifa");
            if (tarifa == null) {
                continue;
            }
            LocalDate entrada = fecha(linea, "fechaEntrada");
            LocalDate salida = fecha(linea, "fechaSalida");
            long noches = ChronoUnit.DAYS.between(entrada, salida);
            long precios = referencias(tarifa, "precios").stream()
                    .map(p -> fecha(p, "fecha"))
                    .filter(f -> f != null && !f.isBefore(entrada) && f.isBefore(salida))
                    .count();
            if (precios != noches) {
                r.agregar("RN-04", "todasLasNochesTienenPrecio", linea,
                        "La tarifa " + texto(tarifa, "codigo") + " cubre " + precios + " de " + noches + " noches");
            }
        }
    }

    private static void tarifaVigente(ModeloReservas m, Reporte r) {
        for (EObject linea : m.de("ReservaHabitacion")) {
            EObject tarifa = referencia(linea, "tarifa");
            if (tarifa == null) {
                continue;
            }
            LocalDate entrada = fecha(linea, "fechaEntrada");
            LocalDate salida = fecha(linea, "fechaSalida");
            LocalDate inicio = fecha(tarifa, "vigenciaInicio");
            LocalDate fin = fecha(tarifa, "vigenciaFin");
            if (inicio == null || fin == null) {
                continue;
            }
            if (entrada.isBefore(inicio) || salida.isAfter(fin.plusDays(1))) {
                r.agregar("RN-04", "tarifaVigente", linea,
                        "La estancia " + entrada + "→" + salida + " sale de la vigencia de la tarifa "
                                + texto(tarifa, "codigo"));
            }
        }
    }

    private static void estanciaMinimaCumplida(ModeloReservas m, Reporte r) {
        for (EObject linea : m.de("ReservaHabitacion")) {
            EObject tarifa = referencia(linea, "tarifa");
            if (tarifa == null) {
                continue;
            }
            long noches = noches(linea);
            if (noches < entero(tarifa, "estanciaMinima")) {
                r.agregar("RN-04", "estanciaMinimaCumplida", linea,
                        "La estancia de " + noches + " noches no alcanza el mínimo de " + entero(tarifa, "estanciaMinima"));
            }
        }
    }

    private static void estanciaMaximaCumplida(ModeloReservas m, Reporte r) {
        for (EObject linea : m.de("ReservaHabitacion")) {
            EObject tarifa = referencia(linea, "tarifa");
            if (tarifa == null) {
                continue;
            }
            int maxima = entero(tarifa, "estanciaMaxima");
            long noches = noches(linea);
            if (maxima != 0 && noches > maxima) {
                r.agregar("RN-04", "estanciaMaximaCumplida", linea,
                        "La estancia de " + noches + " noches supera el máximo de " + maxima);
            }
        }
    }

    private static void cierreDeLlegada(ModeloReservas m, Reporte r) {
        for (EObject linea : m.de("ReservaHabitacion")) {
            EObject tarifa = referencia(linea, "tarifa");
            if (tarifa == null) {
                continue;
            }
            LocalDate fin = fecha(tarifa, "vigenciaFin");
            if (fin == null) {
                continue;
            }
            LocalDate limite = fin.minusDays(entero(tarifa, "cierreLlegadaDias"));
            if (fecha(linea, "fechaEntrada").isAfter(limite)) {
                r.agregar("RN-04", "cierreDeLlegada", linea,
                        "La llegada " + fecha(linea, "fechaEntrada") + " está cerrada (límite " + limite + ")");
            }
        }
    }

    // ======================================================================
    // RN-05 — Políticas de cancelación
    // ======================================================================
    private static void politicaFlexible(ModeloReservas m, Reporte r, String nombre, String codigo) {
        for (EObject p : m.de("PoliticaCancelacion")) {
            if (!codigo.equals(texto(p, "codigo"))) {
                continue;
            }
            if (entero(p, "horasLimite") != 48 || entero(p, "cargoNoches") != 0 || !bool(p, "reembolsable")) {
                r.agregar("RN-05", nombre, p,
                        "La política FLEXIBLE exige 48 h de límite, sin cargo y reembolsable");
            }
        }
    }

    private static void politicaModerada(ModeloReservas m, Reporte r) {
        for (EObject p : m.de("PoliticaCancelacion")) {
            if (!"MODERADA".equals(texto(p, "codigo"))) {
                continue;
            }
            if (entero(p, "cargoNoches") != 1 || !bool(p, "reembolsable")) {
                r.agregar("RN-05", "moderada", p, "La política MODERADA exige cargo de 1 noche y ser reembolsable");
            }
        }
    }

    private static void politicaNoReembolsable(ModeloReservas m, Reporte r) {
        for (EObject p : m.de("PoliticaCancelacion")) {
            if (!"NO_REEMBOLSABLE".equals(texto(p, "codigo"))) {
                continue;
            }
            if (decimal(p, "cargoPorcentaje").compareTo(BigDecimal.valueOf(100)) != 0 || bool(p, "reembolsable")) {
                r.agregar("RN-05", "noReembolsable", p, "La política NO_REEMBOLSABLE exige cargo del 100 % y no ser reembolsable");
            }
        }
    }

    private static void cargoCancelacionConforme(ModeloReservas m, Reporte r) {
        for (EObject reserva : m.de("Reserva")) {
            if (!"CANCELADA".equals(enumNombre(reserva, "estado"))) {
                continue;
            }
            LocalDateTime fechaCancelacion = fechaHora(reserva, "fechaCancelacion");
            if (fechaCancelacion == null) {
                continue;
            }
            BigDecimal esperado = cargoCancelacion(reserva, fechaCancelacion);
            if (decimal(reserva, "cargoCancelacion").compareTo(esperado) != 0) {
                r.agregar("RN-05", "cargoCancelacionConforme", reserva,
                        "El cargo de cancelación registrado (" + decimal(reserva, "cargoCancelacion")
                                + ") no coincide con el calculado (" + esperado + ")");
            }
        }
    }

    // ======================================================================
    // RN-06 — Garantías
    // ======================================================================
    private static void sinGarantiaTieneHoraLimite(ModeloReservas m, Reporte r) {
        for (EObject reserva : m.de("Reserva")) {
            EObject garantia = referencia(reserva, "garantia");
            if (garantia == null || !"SIN_GARANTIA".equals(enumNombre(garantia, "tipo"))) {
                continue;
            }
            EObject propiedad = referencia(reserva, "propiedad");
            LocalDateTime limite = fechaHora(garantia, "horaLimiteLiberacion");
            if (propiedad == null || limite == null) {
                r.agregar("RN-06", "sinGarantiaTieneHoraLimite", reserva,
                        "Una reserva sin garantía debe declarar su hora límite de liberación");
                continue;
            }
            LocalDateTime esperado = fecha(reserva, "fechaEntrada").atTime(hora(propiedad, "horaLimiteSinGarantia"));
            if (!limite.equals(esperado)) {
                r.agregar("RN-06", "sinGarantiaTieneHoraLimite", reserva,
                        "La hora límite de liberación (" + limite + ") no coincide con la configurada (" + esperado + ")");
            }
        }
    }

    private static void expiradaSoloSinGarantia(ModeloReservas m, Reporte r) {
        for (EObject reserva : m.de("Reserva")) {
            if (!"EXPIRADA".equals(enumNombre(reserva, "estado"))) {
                continue;
            }
            EObject garantia = referencia(reserva, "garantia");
            if (garantia == null || !"SIN_GARANTIA".equals(enumNombre(garantia, "tipo"))) {
                r.agregar("RN-06", "expiradaSoloSinGarantia", reserva,
                        "Solo una reserva sin garantía puede expirar");
            }
        }
    }

    private static void confirmadaRequiereGarantia(ModeloReservas m, Reporte r) {
        for (EObject reserva : m.de("Reserva")) {
            if ("CONFIRMADA".equals(enumNombre(reserva, "estado")) && referencia(reserva, "garantia") == null) {
                r.agregar("RN-06", "confirmadaRequiereGarantia", reserva,
                        "Una reserva confirmada debe tener garantía");
            }
        }
    }

    // ======================================================================
    // RN-07 — Habitación lista
    // ======================================================================
    private static void habitacionLista(ModeloReservas m, Reporte r) {
        for (EObject estancia : m.de("Estancia")) {
            EObject habitacion = referencia(estancia, "habitacion");
            if (habitacion == null) {
                continue;
            }
            String estado = enumNombre(habitacion, "estadoLimpieza");
            boolean lista = "LIMPIA".equals(estado) || "INSPECCIONADA".equals(estado);
            if (!lista && !bool(estancia, "autorizacionSupervisor")) {
                r.agregar("RN-07", "habitacionLista", estancia,
                        "La habitación " + texto(habitacion, "numero") + " está " + estado + " y sin autorización");
            }
        }
    }

    private static void habitacionNoBloqueada(ModeloReservas m, Reporte r) {
        for (EObject estancia : m.de("Estancia")) {
            LocalDateTime llegada = fechaHora(estancia, "llegadaReal");
            EObject habitacion = referencia(estancia, "habitacion");
            if (llegada == null || habitacion == null) {
                continue;
            }
            LocalDate dia = llegada.toLocalDate();
            for (EObject bloqueo : referencias(habitacion, "bloqueos")) {
                if (!dia.isBefore(fecha(bloqueo, "fechaInicio")) && dia.isBefore(fecha(bloqueo, "fechaFin"))) {
                    r.agregar("RN-07", "habitacionNoBloqueada", estancia,
                            "La habitación " + texto(habitacion, "numero") + " está bloqueada el " + dia);
                }
            }
        }
    }

    // ======================================================================
    // RN-08 — No-show
    // ======================================================================
    private static void noShowTrasHoraLimite(ModeloReservas m, Reporte r) {
        for (EObject reserva : m.de("Reserva")) {
            if (!"NO_SHOW".equals(enumNombre(reserva, "estado"))) {
                continue;
            }
            LocalDateTime noShow = fechaHora(reserva, "fechaNoShow");
            LocalTime limite = hora(reserva, "horaLimiteLlegada");
            if (noShow == null || limite == null || !noShow.toLocalDate().equals(fecha(reserva, "fechaEntrada"))
                    || noShow.toLocalTime().isBefore(limite)) {
                r.agregar("RN-08", "noShowTrasHoraLimite", reserva,
                        "El no-show debe registrarse el día de llegada y después de la hora límite");
            }
        }
    }

    private static void noShowConCargo(ModeloReservas m, Reporte r) {
        for (EObject reserva : m.de("Reserva")) {
            if (!"NO_SHOW".equals(enumNombre(reserva, "estado"))) {
                continue;
            }
            BigDecimal esperado = cargoNoShow(reserva);
            if (decimal(reserva, "cargoNoShow").compareTo(esperado) != 0) {
                r.agregar("RN-08", "noShowConCargo", reserva,
                        "El cargo por no-show (" + decimal(reserva, "cargoNoShow") + ") no coincide con el calculado ("
                                + esperado + ")");
            }
        }
    }

    // ======================================================================
    // RN-09 — Descuentos
    // ======================================================================
    private static void descuentoAprobado(ModeloReservas m, Reporte r) {
        for (EObject reserva : m.de("Reserva")) {
            BigDecimal descuento = decimal(reserva, "descuentoPorcentaje");
            if (descuento.compareTo(BigDecimal.TEN) <= 0) {
                continue;
            }
            EObject aprobacion = referencia(reserva, "aprobacionDescuento");
            String motivo = null;
            if (aprobacion == null) {
                motivo = "no existe aprobación";
            } else if (decimal(aprobacion, "porcentajeAprobado").compareTo(descuento) < 0) {
                motivo = "la aprobación (" + decimal(aprobacion, "porcentajeAprobado") + " %) es menor que el descuento";
            } else if (!tienePermiso(referencia(aprobacion, "aprobadoPor"), "DESCUENTO_APROBAR")) {
                motivo = "quien aprueba no tiene el permiso DESCUENTO_APROBAR";
            }
            if (motivo != null) {
                r.agregar("RN-09", "descuentoAprobado", reserva,
                        "Descuento del " + descuento + " % sin aprobación válida: " + motivo);
            }
        }
    }

    private static void descuentoEnRango(ModeloReservas m, Reporte r) {
        for (EObject reserva : m.de("Reserva")) {
            BigDecimal descuento = decimal(reserva, "descuentoPorcentaje");
            if (descuento.signum() < 0 || descuento.compareTo(BigDecimal.valueOf(100)) > 0) {
                r.agregar("RN-09", "descuentoEnRango", reserva, "El descuento " + descuento + " % está fuera de 0..100");
            }
        }
    }

    // ======================================================================
    // RN-10 — Grupos
    // ======================================================================
    private static void minimoHabitaciones(ModeloReservas m, Reporte r) {
        for (EObject bloqueo : m.de("BloqueoGrupo")) {
            if (entero(bloqueo, "habitacionesBloqueadas") < 5) {
                r.agregar("RN-10", "minimoHabitaciones", bloqueo,
                        "Un bloqueo de grupo exige 5 habitaciones o más");
            }
        }
    }

    private static void cutoffValido(ModeloReservas m, Reporte r) {
        for (EObject grupo : m.de("GrupoReserva")) {
            LocalDate cutoff = fecha(grupo, "cutoff");
            LocalDate creado = fechaHora(grupo, "creadoEn") == null ? null : fechaHora(grupo, "creadoEn").toLocalDate();
            LocalDate primeraEntrada = referencias(grupo, "bloqueos").stream()
                    .map(b -> fecha(b, "fechaEntrada"))
                    .filter(java.util.Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(null);
            if (cutoff == null || creado == null || primeraEntrada == null
                    || !cutoff.isAfter(creado) || cutoff.isAfter(primeraEntrada)) {
                r.agregar("RN-10", "cutoffValido", grupo,
                        "El cut-off debe ser posterior a la creación y no superar la primera fecha de entrada");
            }
        }
    }

    private static void vencidoLiberaInventario(ModeloReservas m, Reporte r) {
        for (EObject grupo : m.de("GrupoReserva")) {
            if (!"VENCIDO".equals(enumNombre(grupo, "estado"))) {
                continue;
            }
            EObject propiedad = referencia(grupo, "propiedad");
            if (propiedad == null || fecha(grupo, "cutoff").isAfter(fecha(propiedad, "fechaOperativa"))) {
                r.agregar("RN-10", "vencidoLiberaInventario", grupo,
                        "Un grupo vencido debe tener el cut-off antes de la fecha operativa");
            }
        }
    }

    // ======================================================================
    // RN-11 — Cierre del día
    // ======================================================================
    private static void cierreNoRetrocede(ModeloReservas m, Reporte r) {
        for (EObject p : m.de("Propiedad")) {
            EObject ultimo = referencia(p, "ultimoCierre");
            if (ultimo != null && fecha(p, "fechaOperativa").isBefore(fecha(ultimo, "fechaOperativa"))) {
                r.agregar("RN-11", "cierreNoRetrocede", p,
                        "La fecha operativa no puede ser anterior al último cierre");
            }
        }
    }

    private static void noModificableTrasCierre(ModeloReservas m, Reporte r) {
        for (EObject reserva : m.de("Reserva")) {
            LocalDateTime modificada = fechaHora(reserva, "modificadaEn");
            EObject propiedad = referencia(reserva, "propiedad");
            if (modificada == null || propiedad == null) {
                continue;
            }
            EObject ultimo = referencia(propiedad, "ultimoCierre");
            if (ultimo == null || modificada.toLocalDate().isAfter(fecha(ultimo, "fechaOperativa"))) {
                continue;
            }
            EObject usuario = referencia(reserva, "modificadaPor");
            if (usuario == null || !tienePermiso(usuario, "EXCEPCION_CIERRE")) {
                r.agregar("RN-11", "noModificableTrasCierre", reserva,
                        "Modificación de un día ya cerrado sin permiso EXCEPCION_CIERRE");
            }
        }
    }

    // ======================================================================
    // RN-12 — Bitácora
    // ======================================================================
    private static void entradaCompleta(ModeloReservas m, Reporte r) {
        for (EObject b : m.de("Bitacora")) {
            List<String> problemas = new ArrayList<>();
            if (vacio(texto(b, "entidad"))) {
                problemas.add("entidad");
            }
            if (uuid(b, "entidadId") == null) {
                problemas.add("entidadId");
            }
            if (referencia(b, "usuario") == null) {
                problemas.add("usuario");
            }
            if (fechaHora(b, "fecha") == null) {
                problemas.add("fecha");
            }
            if (java.util.Objects.equals(texto(b, "valorAnterior"), texto(b, "valorNuevo"))) {
                problemas.add("valorAnterior=valorNuevo");
            }
            if (!problemas.isEmpty()) {
                r.agregar("RN-12", "entradaCompleta", b, "Entrada de bitácora incompleta: " + problemas);
            }
        }
    }

    private static void modificacionesAuditadas(ModeloReservas m, Reporte r) {
        for (EObject reserva : m.de("Reserva")) {
            LocalDateTime modificada = fechaHora(reserva, "modificadaEn");
            LocalDateTime creada = fechaHora(reserva, "creadaEn");
            if (modificada == null || creada == null || !modificada.isAfter(creada)) {
                continue;
            }
            boolean auditada = m.de("Bitacora").stream().anyMatch(b ->
                    "Reserva".equals(texto(b, "entidad"))
                            && uuid(b, "entidadId") != null
                            && uuid(b, "entidadId").equals(uuid(reserva, "id"))
                            && "MODIFICAR".equals(texto(b, "accion"))
                            && modificada.equals(fechaHora(b, "fecha")));
            if (!auditada) {
                r.agregar("RN-12", "modificacionesAuditadas", reserva,
                        "La reserva se modificó en " + modificada + " sin entrada de bitácora");
            }
        }
    }

    // ======================================================================
    // RN-13 — Fuera de servicio
    // ======================================================================
    private static void fueraDeServicioSinReservasActivas(ModeloReservas m, Reporte r) {
        for (EObject habitacion : m.de("Habitacion")) {
            if (!bool(habitacion, "fueraDeServicio")) {
                continue;
            }
            for (EObject linea : m.de("ReservaHabitacion")) {
                if (referencia(linea, "habitacion") == habitacion && activa(linea)) {
                    r.agregar("RN-13", "fueraDeServicioSinReservasActivas", habitacion,
                            "La habitación " + texto(habitacion, "numero") + " está fuera de servicio con reservas activas");
                }
            }
        }
    }

    // ======================================================================
    // RN-14 — Overbooking
    // ======================================================================
    private static void overbookingControlado(ModeloReservas m, Reporte r) {
        for (EObject p : m.de("Propiedad")) {
            if (!bool(p, "overbookingHabilitado")) {
                continue;
            }
            EObject autorizador = referencia(p, "overbookingHabilitadoPor");
            if (entero(p, "limiteOverbooking") <= 0 || autorizador == null
                    || !tienePermiso(autorizador, "OVERBOOKING_HABILITAR")) {
                r.agregar("RN-14", "overbookingControlado", p,
                        "El overbooking requiere límite positivo y autorización con permiso OVERBOOKING_HABILITAR");
            }
        }
    }

    private static void overbookingDesactivadoPorDefecto(ModeloReservas m, Reporte r) {
        for (EObject p : m.de("Propiedad")) {
            if (referencia(p, "overbookingHabilitadoPor") == null
                    && (bool(p, "overbookingHabilitado") || entero(p, "limiteOverbooking") != 0)) {
                r.agregar("RN-14", "overbookingDesactivadoPorDefecto", p,
                        "Sin autorizador, el overbooking debe estar desactivado y con límite 0");
            }
        }
    }

    // ======================================================================
    // RN-15 — Protección de datos
    // ======================================================================
    private static void consentimientoAntesDeTratar(ModeloReservas m, Reporte r) {
        for (EObject h : m.de("Huesped")) {
            if (bool(h, "datosPersonales") && (!bool(h, "consentimientoDatos") || fecha(h, "fechaConsentimiento") == null)) {
                r.agregar("RN-15", "consentimientoAntesDeTratar", h,
                        "Se tratan datos personales sin consentimiento registrado");
            }
        }
    }

    private static void derechoDeSupresion(ModeloReservas m, Reporte r) {
        for (EObject h : m.de("Huesped")) {
            if (bool(h, "solicitudSupresion") && (!bool(h, "datosAnonimizados") || !vacio(texto(h, "notas")))) {
                r.agregar("RN-15", "derechoDeSupresion", h,
                        "La solicitud de supresión exige anonimizar y borrar las notas");
            }
        }
    }

    private static void correoValido(ModeloReservas m, Reporte r) {
        java.util.regex.Pattern patron = java.util.regex.Pattern
                .compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
        for (EObject h : m.de("Huesped")) {
            String correo = texto(h, "correo");
            if (!vacio(correo) && !patron.matcher(correo).matches()) {
                r.agregar("RN-15", "correoValido", h, "Correo inválido: " + correo);
            }
        }
    }

    // ======================================================================
    // §4.2 — Invariantes del núcleo
    // ======================================================================
    private static void fechasCoherentes(ModeloReservas m, Reporte r) {
        for (EObject reserva : m.de("Reserva")) {
            LocalDate entrada = fecha(reserva, "fechaEntrada");
            LocalDate salida = fecha(reserva, "fechaSalida");
            if (entrada == null || salida == null || !entrada.isBefore(salida)) {
                r.agregar("—", "fechasCoherentes", reserva, "La fecha de entrada debe ser anterior a la de salida");
            }
        }
    }

    private static void codigoConfirmacionUnico(ModeloReservas m, Reporte r) {
        Set<String> vistos = new LinkedHashSet<>();
        for (EObject reserva : m.de("Reserva")) {
            String codigo = texto(reserva, "codigoConfirmacion");
            if (codigo != null && !vistos.add(codigo)) {
                r.agregar("—", "codigoConfirmacionUnico", reserva, "Código de confirmación duplicado: " + codigo);
            }
        }
    }

    private static void fechasDentroDeReserva(ModeloReservas m, Reporte r) {
        for (EObject linea : m.de("ReservaHabitacion")) {
            EObject reserva = referencia(linea, "reserva");
            if (reserva == null) {
                continue;
            }
            if (fecha(linea, "fechaEntrada").isBefore(fecha(reserva, "fechaEntrada"))
                    || fecha(linea, "fechaSalida").isAfter(fecha(reserva, "fechaSalida"))) {
                r.agregar("—", "fechasDentroDeReserva", linea,
                        "Las fechas de la línea salen del rango de la reserva");
            }
        }
    }

    private static void tipoHabitacionCoherente(ModeloReservas m, Reporte r) {
        for (EObject linea : m.de("ReservaHabitacion")) {
            EObject habitacion = referencia(linea, "habitacion");
            EObject tipo = referencia(linea, "tipoHabitacion");
            if (habitacion != null && tipo != null && referencia(habitacion, "tipo") != tipo) {
                r.agregar("—", "tipoHabitacionCoherente", linea,
                        "La habitación asignada no es del tipo contratado");
            }
        }
    }

    private static void ttlDiezMinutos(ModeloReservas m, Reporte r) {
        for (EObject ret : m.de("RetencionInventario")) {
            LocalDateTime creada = fechaHora(ret, "creadaEn");
            LocalDateTime expira = fechaHora(ret, "expiraEn");
            if (creada == null || expira == null || !expira.equals(creada.plusMinutes(10))) {
                r.agregar("—", "ttlDiezMinutos", ret, "La retención debe expirar exactamente 10 minutos después de crearse");
            }
        }
    }

    private static void expiradaNoConsumible(ModeloReservas m, Reporte r) {
        for (EObject ret : m.de("RetencionInventario")) {
            if ("EXPIRADA".equals(enumNombre(ret, "estado")) && fechaHora(ret, "consumidaEn") != null) {
                r.agregar("—", "expiradaNoConsumible", ret, "Una retención expirada no puede estar consumida");
            }
        }
    }

    private static void totalConsistente(ModeloReservas m, Reporte r) {
        for (EObject reserva : m.de("Reserva")) {
            BigDecimal esperado = totalEsperado(reserva);
            if (decimal(reserva, "total").compareTo(esperado) != 0) {
                r.agregar("—", "totalConsistente", reserva,
                        "El total (" + decimal(reserva, "total") + ") no coincide con las líneas menos descuento ("
                                + esperado + ")");
            }
        }
    }

    private static void capacidadRespetada(ModeloReservas m, Reporte r) {
        for (EObject linea : m.de("ReservaHabitacion")) {
            EObject reserva = referencia(linea, "reserva");
            EObject tipo = referencia(linea, "tipoHabitacion");
            if (reserva == null || tipo == null) {
                continue;
            }
            int lineas = referencias(reserva, "lineas").size();
            int maxAdultos = entero(tipo, "capacidadAdultos") * lineas;
            int maxNinos = entero(tipo, "capacidadNinos") * lineas;
            if (entero(reserva, "adultos") > maxAdultos || entero(reserva, "ninos") > maxNinos) {
                r.agregar("—", "capacidadRespetada", linea,
                        "La ocupación (" + entero(reserva, "adultos") + " adultos, " + entero(reserva, "ninos")
                                + " niños) excede la capacidad del tipo " + texto(tipo, "codigo"));
            }
        }
    }

    // ======================================================================
    // Utilidades
    // ======================================================================
    private static boolean activa(EObject linea) {
        EObject reserva = referencia(linea, "reserva");
        if (reserva == null) {
            return false;
        }
        String estado = enumNombre(reserva, "estado");
        return "PENDIENTE".equals(estado) || "CONFIRMADA".equals(estado) || "EN_CASA".equals(estado);
    }

    private static boolean solapan(EObject a, EObject b) {
        LocalDate a1 = fecha(a, "fechaEntrada");
        LocalDate a2 = fecha(a, "fechaSalida");
        LocalDate b1 = fecha(b, "fechaEntrada");
        LocalDate b2 = fecha(b, "fechaSalida");
        return a1.isBefore(b2) && b1.isBefore(a2);
    }

    private static long noches(EObject linea) {
        return ChronoUnit.DAYS.between(fecha(linea, "fechaEntrada"), fecha(linea, "fechaSalida"));
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static boolean tienePermiso(EObject usuario, String permiso) {
        if (usuario == null) {
            return false;
        }
        EObject rol = referencia(usuario, "rol");
        return rol != null && enumNombres(rol, "permisos").contains(permiso);
    }

    private static BigDecimal precioNoche(EObject reserva, LocalDate fechaBuscada) {
        for (EObject linea : referencias(reserva, "lineas")) {
            LocalDate entrada = fecha(linea, "fechaEntrada");
            LocalDate salida = fecha(linea, "fechaSalida");
            if (fechaBuscada.isBefore(entrada) || !fechaBuscada.isBefore(salida)) {
                continue;
            }
            EObject tarifa = referencia(linea, "tarifa");
            if (tarifa == null) {
                continue;
            }
            for (EObject precio : referencias(tarifa, "precios")) {
                if (fechaBuscada.equals(fecha(precio, "fecha"))) {
                    return decimal(precio, "monto");
                }
            }
        }
        return null;
    }

    private static BigDecimal cargoCancelacion(EObject reserva, LocalDateTime fechaCancelacion) {
        EObject politica = referencia(reserva, "politicaCancelacion");
        if (politica == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = decimal(reserva, "total");
        if ("NO_REEMBOLSABLE".equals(texto(politica, "codigo"))) {
            return total.multiply(decimal(politica, "cargoPorcentaje"))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        long horas = ChronoUnit.HOURS.between(fechaCancelacion, fecha(reserva, "fechaEntrada").atStartOfDay());
        if (horas >= entero(politica, "horasLimite")) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal precio = precioNoche(reserva, fecha(reserva, "fechaEntrada"));
        if (precio == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return precio.multiply(BigDecimal.valueOf(entero(politica, "cargoNoches"))).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal cargoNoShow(EObject reserva) {
        EObject politica = referencia(reserva, "politicaCancelacion");
        if (politica == null) {
            return BigDecimal.ZERO;
        }
        if (entero(politica, "cargoNoches") > 0) {
            BigDecimal precio = precioNoche(reserva, fecha(reserva, "fechaEntrada"));
            if (precio != null) {
                return precio.multiply(BigDecimal.valueOf(entero(politica, "cargoNoches")))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }
        return decimal(reserva, "total").multiply(decimal(politica, "cargoPorcentaje"))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal totalEsperado(EObject reserva) {
        BigDecimal suma = BigDecimal.ZERO;
        for (EObject linea : referencias(reserva, "lineas")) {
            suma = suma.add(decimal(linea, "precioTotal"));
        }
        BigDecimal descuento = decimal(reserva, "descuentoPorcentaje");
        BigDecimal rebaja = suma.multiply(descuento).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return suma.subtract(rebaja).setScale(2, RoundingMode.HALF_UP);
    }
}
