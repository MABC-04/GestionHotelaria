package com.hotel.pms.modelo.validacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hotel.pms.modelo.ModeloReservas;
import com.hotel.pms.modelo.fixture.FixtureReservas;

/**
 * Contrato del modelo (§5.4 del plan): el fixture produce exactamente dos
 * violaciones, ni una más.
 *
 * <p>Se valida dos veces: sobre el modelo construido en memoria y sobre el
 * archivo {@code .xmi} versionado del repositorio. La segunda prueba cubre la
 * serialización (fechas, horas, UUID, enums) que es donde más se rompe EMF.</p>
 */
class FixtureValidacionTest {

    private static final Set<String> ESPERADAS = Set.of("sinSolapamientoEnHabitacion", "descuentoAprobado");

    @Test
    @DisplayName("El fixture en memoria produce exactamente 2 violaciones")
    void fixtureEnMemoria() {
        ModeloReservas modelo = FixtureReservas.construir();
        verificarDosViolaciones(modelo, "memoria");
    }

    @Test
    @DisplayName("El fixture versionado en .xmi produce las mismas 2 violaciones")
    void fixtureSerializado() {
        ModeloReservas modelo = ModeloReservas.cargarDeClasspath("/fixtures/reservas-fixture.xmi");
        verificarDosViolaciones(modelo, "xmi");
    }

    @Test
    @DisplayName("Las violaciones reportan la regla de negocio correspondiente")
    void reglasAsociadas() {
        Reporte reporte = new ValidadorModelo().validar(FixtureReservas.construir());
        Set<String> reglas = reporte.todas().stream().map(Violacion::regla).collect(Collectors.toSet());
        assertEquals(Set.of("RN-01", "RN-09"), reglas);
    }

    private void verificarDosViolaciones(ModeloReservas modelo, String origen) {
        Reporte reporte = new ValidadorModelo().validar(modelo);
        Set<Violacion> violaciones = reporte.todas();

        assertEquals(2, violaciones.size(),
                () -> "El fixture (" + origen + ") debe producir 2 violaciones, produjo " + violaciones.size()
                        + ":\n" + violaciones);

        Set<String> nombres = violaciones.stream().map(Violacion::invariante).collect(Collectors.toSet());
        assertEquals(ESPERADAS, nombres, () -> "Violaciones inesperadas: " + violaciones);

        assertTrue(violaciones.stream().anyMatch(v -> v.invariante().equals("descuentoAprobado")
                && v.objeto().equals(FixtureReservas.RESERVA_2)));
    }
}
