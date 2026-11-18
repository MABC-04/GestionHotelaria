package com.hotel.pms.modelo.validacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * Acceso tipado a instancias EMF dinámicas.
 *
 * <p>El Sprint 0 no genera clases Java del modelo, así que los validadores leen
 * las características por nombre. Un nombre mal escrito falla de inmediato
 * (fail-fast) en lugar de devolver {@code null} silencioso.</p>
 */
public final class V {

    private V() {
    }

    public static EStructuralFeature feature(EObject objeto, String nombre) {
        EStructuralFeature f = objeto.eClass().getEStructuralFeature(nombre);
        if (f == null) {
            throw new IllegalStateException(
                    "Feature inexistente: " + objeto.eClass().getName() + "." + nombre);
        }
        return f;
    }

    public static Object crudo(EObject objeto, String nombre) {
        return objeto.eGet(feature(objeto, nombre));
    }

    public static String texto(EObject objeto, String nombre) {
        Object valor = crudo(objeto, nombre);
        return valor == null ? null : valor.toString();
    }

    public static boolean bool(EObject objeto, String nombre) {
        Object valor = crudo(objeto, nombre);
        return Boolean.TRUE.equals(valor);
    }

    public static int entero(EObject objeto, String nombre) {
        Object valor = crudo(objeto, nombre);
        return valor == null ? 0 : ((Number) valor).intValue();
    }

    public static BigDecimal decimal(EObject objeto, String nombre) {
        Object valor = crudo(objeto, nombre);
        return valor == null ? BigDecimal.ZERO : (BigDecimal) valor;
    }

    public static LocalDate fecha(EObject objeto, String nombre) {
        return (LocalDate) crudo(objeto, nombre);
    }

    public static LocalDateTime fechaHora(EObject objeto, String nombre) {
        return (LocalDateTime) crudo(objeto, nombre);
    }

    public static LocalTime hora(EObject objeto, String nombre) {
        return (LocalTime) crudo(objeto, nombre);
    }

    public static UUID uuid(EObject objeto, String nombre) {
        return (UUID) crudo(objeto, nombre);
    }

    /** Identificador estable para reportes de violación. */
    public static String id(EObject objeto) {
        EStructuralFeature f = objeto.eClass().getEStructuralFeature("id");
        if (f == null) {
            return objeto.eClass().getName();
        }
        return String.valueOf(objeto.eGet(f));
    }

    /** Nombre del literal de una enumeración, sea dinámica o generada. */
    public static String enumNombre(EObject objeto, String nombre) {
        return nombreDeEnum(crudo(objeto, nombre));
    }

    public static List<String> enumNombres(EObject objeto, String nombre) {
        return lista(objeto, nombre).stream().map(V::nombreDeEnum).toList();
    }

    private static String nombreDeEnum(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof org.eclipse.emf.ecore.EEnumLiteral literal) {
            return literal.getName();
        }
        if (valor instanceof Enumerator enumerator) {
            return enumerator.getName();
        }
        return valor.toString();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> lista(EObject objeto, String nombre) {
        Object valor = crudo(objeto, nombre);
        return valor == null ? List.of() : (List<Object>) valor;
    }

    public static boolean esEnum(EObject objeto, String nombre, String literal) {
        return literal.equals(enumNombre(objeto, nombre));
    }

    @SuppressWarnings("unchecked")
    public static EObject referencia(EObject objeto, String nombre) {
        return (EObject) crudo(objeto, nombre);
    }

    @SuppressWarnings("unchecked")
    public static List<EObject> referencias(EObject objeto, String nombre) {
        return (List<EObject>) crudo(objeto, nombre);
    }

    public static void poner(EObject objeto, String nombre, Object valor) {
        objeto.eSet(feature(objeto, nombre), valor);
    }
}
