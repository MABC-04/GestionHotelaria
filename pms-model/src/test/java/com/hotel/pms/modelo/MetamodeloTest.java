package com.hotel.pms.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifica la integridad estructural de {@code reservas.ecore}.
 *
 * <p>Un XMI de Ecore escrito a mano es propenso a erratas: referencias no
 * resueltas, opuestos asimétricos, tipos inexistentes. Esta batería actúa como
 * compilador del metamodelo.</p>
 */
class MetamodeloTest {

    @Test
    @DisplayName("El metamodelo declara 24 clases, 12 enumeraciones y 5 tipos de dato")
    void composicionDelMetamodelo() {
        EPackage paquete = ModeloReservas.vacio().paquete();

        long clases = paquete.getEClassifiers().stream().filter(c -> c instanceof EClass).count();
        long enums = paquete.getEClassifiers().stream().filter(c -> c instanceof EEnum).count();
        long tipos = paquete.getEClassifiers().stream()
                .filter(c -> c instanceof EDataType && !(c instanceof EEnum))
                .count();

        assertEquals(24, clases, "clases");
        assertEquals(12, enums, "enumeraciones");
        assertEquals(5, tipos, "tipos de dato");
        assertEquals("http://www.hotel.com/pms/reservas", paquete.getNsURI());
    }

    @Test
    @DisplayName("Ninguna referencia queda sin resolver (eType proxy)")
    void referenciasResueltas() {
        EPackage paquete = ModeloReservas.vacio().paquete();
        List<String> problemas = new ArrayList<>();

        for (EClassifier c : paquete.getEClassifiers()) {
            if (!(c instanceof EClass eClass)) {
                continue;
            }
            for (EStructuralFeature f : eClass.getEStructuralFeatures()) {
                assertNotNull(f.getEType(), () -> "Sin tipo: " + eClass.getName() + "." + f.getName());
                if (f.getEType().eIsProxy()) {
                    problemas.add(eClass.getName() + "." + f.getName());
                }
            }
        }

        assertTrue(problemas.isEmpty(), () -> "Referencias sin resolver: " + problemas);
    }

    @Test
    @DisplayName("Todo par de referencias opuestas declara el opuesto recíproco")
    void opuestosSimetricos() {
        EPackage paquete = ModeloReservas.vacio().paquete();
        List<String> problemas = new ArrayList<>();

        for (EClassifier c : paquete.getEClassifiers()) {
            if (!(c instanceof EClass eClass)) {
                continue;
            }
            for (EStructuralFeature f : eClass.getEStructuralFeatures()) {
                if (!(f instanceof EReference ref) || ref.getEOpposite() == null) {
                    continue;
                }
                if (ref.getEOpposite().getEOpposite() != ref) {
                    problemas.add(eClass.getName() + "." + f.getName());
                }
                assertSame(ref.getEType(), ref.getEOpposite().getEContainingClass(),
                        () -> "El opuesto de " + eClass.getName() + "." + f.getName() + " apunta a otra clase");
            }
        }

        assertTrue(problemas.isEmpty(), () -> "Opuestos asimétricos: " + problemas);
    }

    @Test
    @DisplayName("Las enumeraciones del §3.3 existen con sus literales")
    void enumeracionesDelPlan() {
        ModeloReservas m = ModeloReservas.vacio();

        assertEquals(7, m.enumeracion("EstadoReserva").getELiterals().size());
        assertEquals(6, m.enumeracion("EstadoLimpieza").getELiterals().size());
        assertEquals(3, m.enumeracion("PlanTarifario").getELiterals().size());
        assertEquals(7, m.enumeracion("TipoCanal").getELiterals().size());
        assertEquals(4, m.enumeracion("TipoGarantia").getELiterals().size());
        assertEquals(4, m.enumeracion("EstadoRetencion").getELiterals().size());
        assertEquals(4, m.enumeracion("MotivoBloqueo").getELiterals().size());
        assertEquals(3, m.enumeracion("EstadoFolio").getELiterals().size());
        assertEquals(4, m.enumeracion("MetodoPago").getELiterals().size());
        assertEquals(4, m.enumeracion("TipoDocumento").getELiterals().size());
        assertEquals(3, m.enumeracion("EstadoGrupo").getELiterals().size());
        assertEquals(15, m.enumeracion("Permiso").getELiterals().size());
    }
}
