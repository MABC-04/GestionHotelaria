package com.hotel.pms.modelo.validacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Impide la deriva entre la especificación OCL y su implementación Java.
 *
 * <p>El catálogo {@code reservas.ocl} es normativo y el código es su
 * ejecución. Si alguien añade un invariante al documento sin implementarlo (o
 * al revés), este test falla.</p>
 */
class CatalogoOclTest {

    private static final Pattern INVARIANTE =
            Pattern.compile("context\\s+\\w+\\s+inv\\s+(\\w+)\\s*:");

    @Test
    @DisplayName("Cada invariante del documento OCL tiene implementación ejecutable")
    void noHayInvariantesSinImplementar() throws IOException {
        Set<String> documentadas = invariantesDocumentadas();

        Set<String> implementadas = CatalogoInvariantes.todos().stream()
                .map(Invariante::nombre)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> sinImplementar = new LinkedHashSet<>(documentadas);
        sinImplementar.removeAll(implementadas);

        Set<String> sinDocumentar = new LinkedHashSet<>(implementadas);
        sinDocumentar.removeAll(documentadas);

        assertTrue(sinImplementar.isEmpty(), () -> "Invariantes OCL sin implementación Java: " + sinImplementar);
        assertTrue(sinDocumentar.isEmpty(), () -> "Invariantes Java sin respaldo en el OCL: " + sinDocumentar);
    }

    @Test
    @DisplayName("El catálogo cubre las 45 invariantes del Sprint 0")
    void tamanoDelCatalogo() throws IOException {
        assertEquals(45, invariantesDocumentadas().size());
        assertEquals(45, CatalogoInvariantes.todos().size());
    }

    @Test
    @DisplayName("No hay dos invariantes con el mismo nombre")
    void nombresUnicos() throws IOException {
        long distintos = CatalogoInvariantes.todos().stream().map(Invariante::nombre).distinct().count();
        assertEquals(CatalogoInvariantes.todos().size(), distintos);
        assertEquals(invariantesDocumentadas().size(), distintos);
    }

    private Set<String> invariantesDocumentadas() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/model/reservas.ocl")) {
            assertTrue(in != null, "No se encontró /model/reservas.ocl");
            String texto = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = INVARIANTE.matcher(texto);
            Set<String> nombres = new LinkedHashSet<>();
            while (matcher.find()) {
                assertTrue(nombres.add(matcher.group(1)), "Invariante duplicado en el OCL: " + matcher.group(1));
            }
            return nombres;
        }
    }
}
