package com.hotel.pms.modelo.validacion;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;

/**
 * Acumulador de violaciones.
 *
 * <p>Se deduplica por {@code (invariante, objeto)}: las invariantes simétricas
 * (por ejemplo el solapamiento entre dos líneas de reserva) se reportan una
 * sola vez y no una por cada extremo del par.</p>
 */
public final class Reporte {

    private final Map<String, Violacion> violaciones = new LinkedHashMap<>();

    public void agregar(String regla, String invariante, EObject objeto, String mensaje) {
        String clave = invariante + "#" + V.id(objeto);
        violaciones.putIfAbsent(clave, new Violacion(regla, invariante, V.id(objeto), mensaje));
    }

    public Set<Violacion> todas() {
        return new LinkedHashSet<>(violaciones.values());
    }

    public Reporte agregarTodo(Collection<Violacion> otras) {
        for (Violacion v : otras) {
            violaciones.putIfAbsent(v.invariante() + "#" + v.objeto(), v);
        }
        return this;
    }

    public int tamano() {
        return violaciones.size();
    }
}
