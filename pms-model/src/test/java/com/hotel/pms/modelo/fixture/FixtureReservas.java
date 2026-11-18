package com.hotel.pms.modelo.fixture;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.eclipse.emf.ecore.EObject;

import com.hotel.pms.modelo.ModeloReservas;
import com.hotel.pms.modelo.validacion.V;

/**
 * Construye el fixture de validación descrito en el §5.4 del plan:
 *
 * <ul>
 *   <li>2 tipos de habitación y 4 habitaciones.</li>
 *   <li>2 tarifas con 30 precios por noche cada una.</li>
 *   <li>3 reservas: una solapada, una con 20 % de descuento sin aprobación y
 *       una sin garantía.</li>
 * </ul>
 *
 * <p>El resultado esperado es <strong>exactamente 2 violaciones</strong>. El
 * fixture se genera una vez y se versiona en
 * {@code src/test/resources/fixtures/reservas-fixture.xmi} para que el test de
 * validación sea reproducible y revisable.</p>
 */
public final class FixtureReservas {

    static final String P = "00000000-0000-0000-0000-000000000001";
    static final String TIPO_DOBLE = "00000000-0000-0000-0000-000000000101";
    static final String TIPO_SUITE = "00000000-0000-0000-0000-000000000102";
    static final String HAB_101 = "00000000-0000-0000-0000-000000000201";
    static final String HAB_102 = "00000000-0000-0000-0000-000000000202";
    static final String HAB_201 = "00000000-0000-0000-0000-000000000203";
    static final String HAB_202 = "00000000-0000-0000-0000-000000000204";
    static final String POL_FLEXIBLE = "00000000-0000-0000-0000-000000000301";
    static final String POL_MODERADA = "00000000-0000-0000-0000-000000000302";
    static final String POL_NO_REEMB = "00000000-0000-0000-0000-000000000303";
    static final String TAR_DOBLE = "00000000-0000-0000-0000-000000000401";
    static final String TAR_SUITE = "00000000-0000-0000-0000-000000000402";
    static final String CANAL_DIRECTO = "00000000-0000-0000-0000-000000000501";
    static final String CANAL_WEB = "00000000-0000-0000-0000-000000000502";
    static final String CANAL_OTA = "00000000-0000-0000-0000-000000000503";
    static final String HUESPED_ANA = "00000000-0000-0000-0000-000000000601";
    static final String HUESPED_LUIS = "00000000-0000-0000-0000-000000000602";
    static final String HUESPED_MARTA = "00000000-0000-0000-0000-000000000603";
    static final String ROL_GERENTE = "00000000-0000-0000-0000-000000000701";
    static final String ROL_RECEPCION = "00000000-0000-0000-0000-000000000702";
    static final String USUARIO_ADMIN = "00000000-0000-0000-0000-000000000801";
    static final String USUARIO_RECEP = "00000000-0000-0000-0000-000000000802";
    static final String RESERVA_1 = "00000000-0000-0000-0000-000000000901";
    public static final String RESERVA_2 = "00000000-0000-0000-0000-000000000902";
    static final String RESERVA_3 = "00000000-0000-0000-0000-000000000903";
    static final String LINEA_1 = "00000000-0000-0000-0000-000000001001";
    static final String LINEA_2 = "00000000-0000-0000-0000-000000001002";
    static final String LINEA_3 = "00000000-0000-0000-0000-000000001003";
    static final String LINEA_4 = "00000000-0000-0000-0000-000000001004";
    static final String GARANTIA_1 = "00000000-0000-0000-0000-000000001101";
    static final String GARANTIA_2 = "00000000-0000-0000-0000-000000001102";
    static final String RETENCION_1 = "00000000-0000-0000-0000-000000001201";
    static final String BITACORA_1 = "00000000-0000-0000-0000-000000001301";

    private FixtureReservas() {
    }

    public static void main(String[] args) throws Exception {
        Path destino = Path.of(args.length > 0 ? args[0] : "src/test/resources/fixtures/reservas-fixture.xmi");
        ModeloReservas modelo = construir();
        Files.createDirectories(destino.toAbsolutePath().getParent());
        modelo.guardar(destino);
        System.out.println("Fixture escrito en " + destino.toAbsolutePath());
    }

    public static ModeloReservas construir() {
        ModeloReservas m = ModeloReservas.vacio();

        // ---------------------------------------------------------- Propiedad
        EObject propiedad = obj(m, "Propiedad", P);
        txt(propiedad, "nombre", "Hotel Andino");
        txt(propiedad, "moneda", "USD");
        txt(propiedad, "zonaHoraria", "America/Guayaquil");
        hora(propiedad, "horaCheckIn", "15:00");
        hora(propiedad, "horaCheckOut", "12:00");
        hora(propiedad, "horaLimiteSinGarantia", "18:00");
        hora(propiedad, "horaLimiteLlegada", "18:00");
        bool(propiedad, "overbookingHabilitado", false);
        entero(propiedad, "limiteOverbooking", 0);
        fecha(propiedad, "fechaOperativa", "2026-01-05");

        // ------------------------------------------------------------- Tipos
        EObject doble = obj(m, "TipoHabitacion", TIPO_DOBLE);
        txt(doble, "codigo", "DOBLE");
        txt(doble, "nombre", "Habitación doble");
        entero(doble, "capacidadAdultos", 2);
        entero(doble, "capacidadNinos", 1);
        txt(doble, "descripcion", "Cama queen y baño privado");
        bool(doble, "activo", true);
        ref(doble, "propiedad", propiedad);

        EObject suite = obj(m, "TipoHabitacion", TIPO_SUITE);
        txt(suite, "codigo", "SUITE");
        txt(suite, "nombre", "Suite");
        entero(suite, "capacidadAdultos", 3);
        entero(suite, "capacidadNinos", 2);
        txt(suite, "descripcion", "Dos ambientes con sala");
        bool(suite, "activo", true);
        ref(suite, "propiedad", propiedad);

        // ------------------------------------------------------- Habitaciones
        EObject h101 = habitacion(m, HAB_101, "101", 1, doble, propiedad, "LIMPIA");
        EObject h102 = habitacion(m, HAB_102, "102", 1, doble, propiedad, "LIMPIA");
        habitacion(m, HAB_201, "201", 2, suite, propiedad, "LIMPIA");
        habitacion(m, HAB_202, "202", 2, suite, propiedad, "INSPECCIONADA");

        // --------------------------------------------------------- Políticas
        EObject flexible = politica(m, POL_FLEXIBLE, "FLEXIBLE", 48, 0, "0", true);
        EObject moderada = politica(m, POL_MODERADA, "MODERADA", 48, 1, "0", true);
        politica(m, POL_NO_REEMB, "NO_REEMBOLSABLE", 0, 0, "100", false);

        // ----------------------------------------------------------- Tarifas
        EObject tarifaDoble = tarifa(m, TAR_DOBLE, "TAR-DOBLE-2026", "Doble 2026",
                doble, moderada, "SOLO_HABITACION", "100.00");
        tarifa(m, TAR_SUITE, "TAR-SUITE-2026", "Suite 2026",
                suite, flexible, "DESAYUNO", "150.00");

        // ------------------------------------------------------------ Canales
        EObject directo = canal(m, CANAL_DIRECTO, "Directo", "DIRECTO", "0");
        EObject web = canal(m, CANAL_WEB, "Sitio web", "WEB", "15");
        canal(m, CANAL_OTA, "OTA", "OTA", "18");

        // ----------------------------------------------------------- Huéspedes
        EObject ana = huesped(m, HUESPED_ANA, "Ana Torres", "ana@example.com", "+593900000001");
        EObject luis = huesped(m, HUESPED_LUIS, "Luis Pérez", "luis@example.com", "+593900000002");
        EObject marta = huesped(m, HUESPED_MARTA, "Marta Ruiz", "marta@example.com", "+593900000003");

        // --------------------------------------------------- Usuarios y roles
        EObject rolGerente = obj(m, "Rol", ROL_GERENTE);
        txt(rolGerente, "nombre", "GERENTE");
        permisos(m, rolGerente, "DESCUENTO_APROBAR", "EXCEPCION_CIERRE", "OVERBOOKING_HABILITAR",
                "REPORTE_VER", "RESERVA_CANCELAR", "RESERVA_CREAR", "RESERVA_MODIFICAR");

        EObject rolRecepcion = obj(m, "Rol", ROL_RECEPCION);
        txt(rolRecepcion, "nombre", "RECEPCION");
        permisos(m, rolRecepcion, "RESERVA_CREAR", "RESERVA_MODIFICAR", "RESERVA_CANCELAR",
                "CHECKIN_REALIZAR", "CHECKOUT_REALIZAR");

        usuario(m, USUARIO_ADMIN, "gerente", "gerente@hotel.com", rolGerente);
        EObject recep = usuario(m, USUARIO_RECEP, "recepcion", "recepcion@hotel.com", rolRecepcion);

        // -------------------------------------------------- Reserva 1 (ok)
        EObject r1 = reserva(m, RESERVA_1, "AB12CD34", ana, directo, moderada, propiedad,
                "2026-01-10", "2026-01-12", 2, 0, "CONFIRMADA", "0", "400.00",
                "2026-01-02T10:00:00");
        linea(m, LINEA_1, r1, h101, doble, tarifaDoble, "2026-01-10", "2026-01-12", "200.00");
        linea(m, LINEA_2, r1, h102, doble, tarifaDoble, "2026-01-10", "2026-01-12", "200.00");
        garantia(m, GARANTIA_1, r1, "TARJETA", "400.00", "2026-01-02T10:05:00", null);
        V.poner(r1, "modificadaEn", LocalDateTime.parse("2026-01-04T11:00:00"));
        V.poner(r1, "modificadaPor", recep);

        EObject bitacora = obj(m, "Bitacora", BITACORA_1);
        txt(bitacora, "entidad", "Reserva");
        V.poner(bitacora, "entidadId", UUID.fromString(RESERVA_1));
        txt(bitacora, "campo", "total");
        txt(bitacora, "valorAnterior", "390.00");
        txt(bitacora, "valorNuevo", "400.00");
        txt(bitacora, "accion", "MODIFICAR");
        V.poner(bitacora, "fecha", LocalDateTime.parse("2026-01-04T11:00:00"));
        ref(bitacora, "usuario", recep);

        // ------------------- Reserva 2 (20 % de descuento sin aprobación) ← V1
        EObject r2 = reserva(m, RESERVA_2, "EF56GH78", luis, web, moderada, propiedad,
                "2026-01-11", "2026-01-13", 2, 0, "CONFIRMADA", "20", "160.00",
                "2026-01-03T09:00:00");
        linea(m, LINEA_3, r2, h101, doble, tarifaDoble, "2026-01-11", "2026-01-13", "200.00");
        garantia(m, GARANTIA_2, r2, "DEPOSITO", "160.00", "2026-01-03T09:15:00", null);

        // ---------------- Reserva 3 (pendiente, sin garantía, con retención)
        EObject r3 = reserva(m, RESERVA_3, "IJ90KL12", marta, directo, flexible, propiedad,
                "2026-01-20", "2026-01-22", 2, 0, "PENDIENTE", "0", "200.00",
                "2026-01-05T08:00:00");
        linea(m, LINEA_4, r3, h102, doble, tarifaDoble, "2026-01-20", "2026-01-22", "200.00");
        EObject retencion = obj(m, "RetencionInventario", RETENCION_1);
        entero(retencion, "cantidad", 1);
        fecha(retencion, "fechaEntrada", "2026-01-20");
        fecha(retencion, "fechaSalida", "2026-01-22");
        V.poner(retencion, "creadaEn", LocalDateTime.parse("2026-01-05T08:00:00"));
        V.poner(retencion, "expiraEn", LocalDateTime.parse("2026-01-05T08:10:00"));
        txt(retencion, "tokenSesion", "sesion-marta");
        enumLit(m, retencion, "estado", "EstadoRetencion", "ACTIVA");
        ref(retencion, "tipoHabitacion", doble);
        ref(retencion, "reserva", r3);

        return m;
    }

    // ------------------------------------------------------------------ ayudas

    private static EObject habitacion(ModeloReservas m, String idVal, String numero, int piso,
            EObject tipo, EObject propiedad, String limpieza) {
        EObject h = obj(m, "Habitacion", idVal);
        txt(h, "numero", numero);
        entero(h, "piso", piso);
        enumLit(m, h, "estadoLimpieza", "EstadoLimpieza", limpieza);
        bool(h, "fueraDeServicio", false);
        ref(h, "tipo", tipo);
        ref(h, "propiedad", propiedad);
        return h;
    }

    private static EObject politica(ModeloReservas m, String idVal, String codigo, int horas, int noches,
            String porcentaje, boolean reembolsable) {
        EObject p = obj(m, "PoliticaCancelacion", idVal);
        txt(p, "codigo", codigo);
        entero(p, "horasLimite", horas);
        entero(p, "cargoNoches", noches);
        dec(p, "cargoPorcentaje", porcentaje);
        bool(p, "reembolsable", reembolsable);
        return p;
    }

    private static EObject tarifa(ModeloReservas m, String idVal, String codigo, String nombre, EObject tipo,
            EObject politica, String plan, String montoNoche) {
        EObject t = obj(m, "Tarifa", idVal);
        txt(t, "codigo", codigo);
        txt(t, "nombre", nombre);
        enumLit(m, t, "plan", "PlanTarifario", plan);
        fecha(t, "vigenciaInicio", "2026-01-01");
        fecha(t, "vigenciaFin", "2026-12-31");
        entero(t, "estanciaMinima", 1);
        entero(t, "estanciaMaxima", 0);
        entero(t, "cierreLlegadaDias", 0);
        bool(t, "activa", true);
        ref(t, "tipoHabitacion", tipo);
        ref(t, "politicaCancelacion", politica);

        List<Object> precios = lista(t, "precios");
        for (int dia = 1; dia <= 30; dia++) {
            UUID idPrecio = UUID.nameUUIDFromBytes((idVal + "#" + dia).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            EObject precio = obj(m, "PrecioNoche", idPrecio);
            fecha(precio, "fecha", LocalDate.of(2026, 1, dia).toString());
            dec(precio, "monto", montoNoche);
            dec(precio, "impuestoPorcentaje", "16");
            ref(precio, "tarifa", t);
            precios.add(precio);
        }
        return t;
    }

    private static EObject canal(ModeloReservas m, String idVal, String nombre, String tipo, String comision) {
        EObject c = obj(m, "Canal", idVal);
        txt(c, "nombre", nombre);
        enumLit(m, c, "tipo", "TipoCanal", tipo);
        dec(c, "comisionPorcentaje", comision);
        return c;
    }

    private static EObject huesped(ModeloReservas m, String idVal, String nombre, String correo, String telefono) {
        EObject h = obj(m, "Huesped", idVal);
        txt(h, "nombre", nombre);
        txt(h, "correo", correo);
        txt(h, "telefono", telefono);
        txt(h, "documento", "1712345678");
        enumLit(m, h, "tipoDocumento", "TipoDocumento", "DNI");
        bool(h, "consentimientoDatos", true);
        fecha(h, "fechaConsentimiento", "2025-06-01");
        bool(h, "datosPersonales", true);
        bool(h, "solicitudSupresion", false);
        bool(h, "datosAnonimizados", false);
        return h;
    }

    private static EObject usuario(ModeloReservas m, String idVal, String nombre, String correo, EObject rol) {
        EObject u = obj(m, "Usuario", idVal);
        txt(u, "nombreUsuario", nombre);
        txt(u, "correo", correo);
        bool(u, "activo", true);
        ref(u, "rol", rol);
        return u;
    }

    private static EObject reserva(ModeloReservas m, String idVal, String codigo, EObject titular, EObject canal,
            EObject politica, EObject propiedad, String entrada, String salida, int adultos, int ninos,
            String estado, String descuento, String total, String creadaEn) {
        EObject r = obj(m, "Reserva", idVal);
        txt(r, "codigoConfirmacion", codigo);
        fecha(r, "fechaEntrada", entrada);
        fecha(r, "fechaSalida", salida);
        entero(r, "adultos", adultos);
        entero(r, "ninos", ninos);
        enumLit(m, r, "estado", "EstadoReserva", estado);
        dec(r, "descuentoPorcentaje", descuento);
        dec(r, "total", total);
        V.poner(r, "creadaEn", LocalDateTime.parse(creadaEn));
        hora(r, "horaLimiteLlegada", "18:00");
        ref(r, "titular", titular);
        ref(r, "canal", canal);
        ref(r, "politicaCancelacion", politica);
        ref(r, "propiedad", propiedad);
        return r;
    }

    private static void linea(ModeloReservas m, String idVal, EObject reserva, EObject habitacion, EObject tipo,
            EObject tarifa, String entrada, String salida, String precio) {
        EObject l = obj(m, "ReservaHabitacion", idVal);
        fecha(l, "fechaEntrada", entrada);
        fecha(l, "fechaSalida", salida);
        dec(l, "precioTotal", precio);
        ref(l, "reserva", reserva);
        ref(l, "habitacion", habitacion);
        ref(l, "tipoHabitacion", tipo);
        ref(l, "tarifa", tarifa);
    }

    private static void garantia(ModeloReservas m, String idVal, EObject reserva, String tipo, String monto,
            String autorizadaEn, String horaLimite) {
        EObject g = obj(m, "Garantia", idVal);
        enumLit(m, g, "tipo", "TipoGarantia", tipo);
        dec(g, "monto", monto);
        txt(g, "referencia", "REF-" + tipo);
        V.poner(g, "autorizadaEn", LocalDateTime.parse(autorizadaEn));
        if (horaLimite != null) {
            V.poner(g, "horaLimiteLiberacion", LocalDateTime.parse(horaLimite));
        }
        ref(g, "reserva", reserva);
    }

    private static EObject obj(ModeloReservas m, String clase, String idVal) {
        return obj(m, clase, UUID.fromString(idVal));
    }

    private static EObject obj(ModeloReservas m, String clase, UUID idVal) {
        EObject o = m.nuevo(clase);
        V.poner(o, "id", idVal);
        m.agregar(o);
        return o;
    }

    private static void txt(EObject o, String f, String v) {
        V.poner(o, f, v);
    }

    private static void dec(EObject o, String f, String v) {
        V.poner(o, f, new BigDecimal(v));
    }

    private static void entero(EObject o, String f, int v) {
        V.poner(o, f, v);
    }

    private static void bool(EObject o, String f, boolean v) {
        V.poner(o, f, v);
    }

    private static void fecha(EObject o, String f, String iso) {
        V.poner(o, f, LocalDate.parse(iso));
    }

    private static void hora(EObject o, String f, String iso) {
        V.poner(o, f, LocalTime.parse(iso));
    }

    private static void ref(EObject o, String f, EObject destino) {
        V.poner(o, f, destino);
    }

    private static void enumLit(ModeloReservas m, EObject o, String f, String enumeracion, String literal) {
        V.poner(o, f, m.literal(enumeracion, literal));
    }

    private static void permisos(ModeloReservas m, EObject rol, String... valores) {
        List<Object> lista = lista(rol, "permisos");
        for (String valor : valores) {
            lista.add(m.literal("Permiso", valor));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> lista(EObject o, String f) {
        return (List<Object>) V.crudo(o, f);
    }
}
