package com.hotel.pms.salud;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint {@code /health} exigido por el criterio de done del Sprint 0.
 *
 * <p>No sustituye a {@code /actuator/health}: es la sonda de negocio que usa la
 * consola web y que confirma que la cadena API → base de datos funciona.</p>
 */
@RestController
public class SaludController {

    private final ObjectProvider<JdbcTemplate> jdbc;

    public SaludController(ObjectProvider<JdbcTemplate> jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public Map<String, Object> salud() {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("estado", "UP");
        respuesta.put("aplicacion", "pms-backend");
        respuesta.put("version", "0.1.0-SNAPSHOT");
        respuesta.put("momento", OffsetDateTime.now().toString());

        JdbcTemplate plantilla = jdbc.getIfAvailable();
        if (plantilla == null) {
            respuesta.put("baseDatos", Map.of("estado", "NO_CONFIGURADA"));
            return respuesta;
        }

        try {
            Integer uno = plantilla.queryForObject("select 1", Integer.class);
            Integer migraciones = plantilla.queryForObject(
                    "select count(*) from flyway_schema_history where success", Integer.class);
            respuesta.put("baseDatos", Map.of(
                    "estado", Integer.valueOf(1).equals(uno) ? "UP" : "DOWN",
                    "migraciones", migraciones == null ? 0 : migraciones));
        } catch (RuntimeException e) {
            respuesta.put("estado", "DEGRADADO");
            respuesta.put("baseDatos", Map.of("estado", "DOWN", "detalle", e.getMessage()));
        }
        return respuesta;
    }
}
