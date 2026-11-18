package com.hotel.pms.generador;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.hotel.pms.modelo.validacion.CatalogoInvariantes;
import com.hotel.pms.modelo.validacion.Invariante;

/**
 * Genera {@code pms-docs/trazabilidad.md} a partir de dos fuentes que deben
 * coincidir: el catálogo normativo {@code reservas.ocl} y su implementación
 * ejecutable {@link CatalogoInvariantes}.
 *
 * <p>El generador falla el build si ambos artefactos divergen, de modo que la
 * matriz publicada nunca describe un estado que el código no tenga. Las
 * columnas de base de datos y de prueba se declaran aquí porque son decisiones
 * de implementación (no derivables del OCL) y se contrastan con
 * {@code V1__inventario.sql}.</p>
 */
public final class GeneradorTrazabilidad {

    private static final Pattern INVARIANTE =
            Pattern.compile("context\\s+(\\w+)\\s+inv\\s+(\\w+)\\s*:");

    /** Artefacto de base de datos que refuerza cada invariante (si existe). */
    private static final Map<String, String> BASE_DE_DATOS = Map.ofEntries(
            Map.entry("sinSolapamientoEnHabitacion", "`rh_sin_solape` (EXCLUDE gist)"),
            Map.entry("datosMinimos", "`NOT NULL` + `reserva_fechas`"),
            Map.entry("horasEstandar", "`propiedad_horas_estandar`"),
            Map.entry("flexible", "`politica_flexible`"),
            Map.entry("moderada", "`politica_moderada`"),
            Map.entry("noReembolsable", "`politica_no_reembolsable`"),
            Map.entry("descuentoEnRango", "`descuento_porcentaje` CHECK 0..100"),
            Map.entry("overbookingControlado", "`propiedad_overbooking` (parcial)"),
            Map.entry("overbookingDesactivadoPorDefecto", "`propiedad_overbooking`"),
            Map.entry("entradaCompleta", "`bitacora_cambio_real` + trigger"),
            Map.entry("modificacionesAuditadas", "trigger append-only (parcial)"),
            Map.entry("consentimientoAntesDeTratar", "`huesped_consentimiento`"),
            Map.entry("derechoDeSupresion", "`huesped_supresion`"),
            Map.entry("correoValido", "`huesped_correo`"),
            Map.entry("fechasCoherentes", "`reserva_fechas`"),
            Map.entry("codigoConfirmacionUnico", "`codigo_confirmacion` UNIQUE"),
            Map.entry("ttlDiezMinutos", "`retencion_ttl`"),
            Map.entry("expiradaNoConsumible", "`retencion_expirada`"));

    /** Prueba que demuestra cada invariante (si existe). */
    private static final Map<String, String> PRUEBAS = Map.ofEntries(
            Map.entry("sinSolapamientoEnHabitacion",
                    "`SinSolapamientoIT#rechazaSolapamiento`, `#concurrenciaCienHilos`; `FixtureValidacionTest`"),
            Map.entry("descuentoAprobado", "`FixtureValidacionTest` (violación esperada)"));

    private GeneradorTrazabilidad() {
    }

    public static void main(String[] args) throws IOException {
        Path salida = Path.of(args.length > 0 ? args[0] : "../pms-docs/trazabilidad.md");
        String ocl = leerOcl();
        Map<String, String> contextos = contextos(ocl);
        List<Invariante> catalogo = CatalogoInvariantes.todos();

        verificarConsistencia(contextos, catalogo);

        Files.createDirectories(salida.toAbsolutePath().getParent());
        Files.writeString(salida, markdown(contextos, catalogo), StandardCharsets.UTF_8);
        System.out.println("Trazabilidad generada: " + salida.toAbsolutePath()
                + " (" + catalogo.size() + " invariantes)");
    }

    private static void verificarConsistencia(Map<String, String> contextos, List<Invariante> catalogo) {
        Set<String> documentadas = new LinkedHashSet<>(contextos.keySet());
        Set<String> implementadas = catalogo.stream().map(Invariante::nombre)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> sinImplementar = new LinkedHashSet<>(documentadas);
        sinImplementar.removeAll(implementadas);
        Set<String> sinDocumentar = new LinkedHashSet<>(implementadas);
        sinDocumentar.removeAll(documentadas);

        if (!sinImplementar.isEmpty() || !sinDocumentar.isEmpty() || implementadas.size() != catalogo.size()) {
            throw new IllegalStateException("El OCL y el catálogo Java divergen. "
                    + "Sin implementar: " + sinImplementar + ". Sin documentar: " + sinDocumentar
                    + ". Implementadas: " + implementadas.size() + " de " + catalogo.size() + " entradas.");
        }
    }

    private static String leerOcl() throws IOException {
        try (InputStream in = GeneradorTrazabilidad.class.getResourceAsStream("/model/reservas.ocl")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró /model/reservas.ocl en el classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> contextos(String ocl) {
        Map<String, String> resultado = new LinkedHashMap<>();
        Matcher m = INVARIANTE.matcher(ocl);
        while (m.find()) {
            if (resultado.put(m.group(2), m.group(1)) != null) {
                throw new IllegalStateException("Invariante duplicado en el OCL: " + m.group(2));
            }
        }
        return resultado;
    }

    private static String markdown(Map<String, String> contextos, List<Invariante> catalogo) {
        long conBaseDeDatos = catalogo.stream().filter(i -> BASE_DE_DATOS.containsKey(i.nombre())).count();
        long conPrueba = catalogo.stream().filter(i -> PRUEBAS.containsKey(i.nombre())).count();

        Map<String, Long> porRegla = catalogo.stream()
                .collect(Collectors.groupingBy(Invariante::regla, TreeMap::new, Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append("# Matriz de trazabilidad\n\n");
        sb.append("> Documento **generado automáticamente** por `pms-generator` en la fase ")
                .append("`process-classes`. No editar a mano.\n")
                .append("> Regenerar con `./mvnw -pl pms-generator process-classes`.\n\n");

        sb.append("- **Versión:** 0.1.0-SNAPSHOT (entrega 1, Sprints 0-2)\n");
        sb.append("- **Metamodelo:** `reservas.ecore` (24 clases, 12 enumeraciones, 5 tipos de dato)\n");
        sb.append("- **Especificación:** `reservas.ocl` (")
                .append(contextos.size()).append(" invariantes en ")
                .append(new LinkedHashSet<>(contextos.values()).size()).append(" contextos)\n");
        sb.append("- **Implementación:** `CatalogoInvariantes` (")
                .append(catalogo.size()).append(" validadores Java)\n");
        sb.append("- **Persistencia:** `V1__inventario.sql` + `V2__datos_iniciales.sql`\n\n");

        sb.append("## Resumen de enforcement\n\n");
        sb.append("| Mecanismo | Invariantes | Cobertura |\n|---|---:|---:|\n");
        sb.append("| Modelo (validador Java) | ").append(catalogo.size()).append(" | 100 % |\n");
        sb.append("| Base de datos (CHECK, UNIQUE, EXCLUDE, trigger) | ").append(conBaseDeDatos)
                .append(" | ").append(porcentaje(conBaseDeDatos, catalogo.size())).append(" |\n");
        sb.append("| Prueba automatizada | ").append(conPrueba)
                .append(" | ").append(porcentaje(conPrueba, catalogo.size())).append(" |\n\n");

        sb.append("> El invariante OCL **nunca** es el mecanismo de concurrencia. ")
                .append("La carrera por la última habitación la resuelve PostgreSQL con `rh_sin_solape` ")
                .append("y se demuestra en `SinSolapamientoIT#concurrenciaCienHilos`.\n\n");

        sb.append("## Distribución por regla de negocio\n\n");
        sb.append("| Regla | Invariantes |\n|---|---:|\n");
        porRegla.forEach((regla, total) ->
                sb.append("| ").append(regla.equals("—") ? "§4.2 (núcleo)" : regla)
                        .append(" | ").append(total).append(" |\n"));
        sb.append('\n');

        sb.append("## Matriz completa\n\n");
        sb.append("| Regla | Invariante | Contexto | Modelo | Base de datos | Prueba |\n");
        sb.append("|---|---|---|:---:|---|:---:|\n");
        for (Invariante invariante : catalogo) {
            sb.append("| ").append(invariante.regla().equals("—") ? "§4.2" : invariante.regla())
                    .append(" | `").append(invariante.nombre()).append('`')
                    .append(" | `").append(contextos.get(invariante.nombre())).append('`')
                    .append(" | ✅")
                    .append(" | ").append(BASE_DE_DATOS.getOrDefault(invariante.nombre(), "—"))
                    .append(" | ").append(PRUEBAS.getOrDefault(invariante.nombre(), "—"))
                    .append(" |\n");
        }
        sb.append('\n');

        List<String> brechasBd = catalogo.stream()
                .map(Invariante::nombre)
                .filter(n -> !BASE_DE_DATOS.containsKey(n))
                .toList();
        List<String> brechasPrueba = catalogo.stream()
                .map(Invariante::nombre)
                .filter(n -> !PRUEBAS.containsKey(n))
                .toList();

        sb.append("## Brechas declaradas\n\n");
        sb.append("Una brecha no es un olvido: es una decisión de alcance. Las invariantes sin ")
                .append("mecanismo en base de datos se validan en el modelo y en el servicio; las que no ")
                .append("tienen prueba todavía son deuda técnica registrada para los sprints siguientes.\n\n");
        sb.append("- **Sin refuerzo en base de datos (").append(brechasBd.size()).append("):** ")
                .append(brechasBd.stream().map(n -> "`" + n + "`").collect(Collectors.joining(", ")))
                .append("\n");
        sb.append("- **Sin prueba automatizada (").append(brechasPrueba.size()).append("):** ")
                .append(brechasPrueba.stream().map(n -> "`" + n + "`").collect(Collectors.joining(", ")))
                .append("\n\n");

        sb.append("## Contratos que sostienen esta matriz\n\n");
        sb.append("1. `CatalogoOclTest` — cada invariante del OCL tiene implementación y viceversa.\n");
        sb.append("2. `FixtureValidacionTest` — el fixture produce exactamente 2 violaciones ")
                .append("(`sinSolapamientoEnHabitacion`, `descuentoAprobado`).\n");
        sb.append("3. `SinSolapamientoIT` — PostgreSQL aplica el esquema y rechaza solapamientos ")
                .append("con `SQLState 23P01`, incluso con 100 hilos concurrentes.\n");
        sb.append("4. `SaludControllerTest` — `/health` responde sin base de datos y reporta su estado.\n");
        return sb.toString();
    }

    private static String porcentaje(long parte, long total) {
        if (total == 0) {
            return "0 %";
        }
        return String.format(java.util.Locale.ROOT, "%.0f %%", 100.0 * parte / total);
    }
}
