package com.hotel.pms.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.impl.EcoreFactoryImpl;

/**
 * Fábrica del paquete {@code reservas} que enseña a EMF a convertir los tipos
 * de dato del dominio desde y hacia texto.
 *
 * <p>Sin esta fábrica, EMF serializa {@code java.time.LocalDate} con
 * {@code toString()} pero al deserializar devuelve {@link String}, lo que rompe
 * cualquier validador. El mismo problema afecta a {@link UUID} y
 * {@link BigDecimal}.</p>
 */
public class FactoryReservas extends EcoreFactoryImpl {

    @Override
    public EObject create(EClass eClass) {
        // Instancias dinámicas: en el Sprint 0 no hay clases Java generadas.
        return new org.eclipse.emf.ecore.impl.DynamicEObjectImpl(eClass);
    }

    @Override
    public Object createFromString(EDataType eDataType, String literal) {
        if (literal == null) {
            return null;
        }
        if (eDataType instanceof org.eclipse.emf.ecore.EEnum eEnum) {
            org.eclipse.emf.ecore.EEnumLiteral valor = eEnum.getEEnumLiteral(literal);
            if (valor == null) {
                throw new IllegalArgumentException(
                        "Literal '" + literal + "' inexistente en " + eEnum.getName());
            }
            return valor;
        }
        return switch (eDataType.getName()) {
            case "UUID" -> UUID.fromString(literal);
            case "Decimal" -> new BigDecimal(literal);
            case "EDate" -> LocalDate.parse(literal);
            case "EDateTime" -> LocalDateTime.parse(literal);
            case "Hora" -> LocalTime.parse(literal);
            default -> super.createFromString(eDataType, literal);
        };
    }

    @Override
    public String convertToString(EDataType eDataType, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof org.eclipse.emf.ecore.EEnumLiteral literal) {
            return literal.getName();
        }
        if (value instanceof org.eclipse.emf.common.util.Enumerator enumerator) {
            return enumerator.getName();
        }
        if (value instanceof UUID
                || value instanceof BigDecimal
                || value instanceof LocalDate
                || value instanceof LocalDateTime
                || value instanceof LocalTime) {
            return value.toString();
        }
        return super.convertToString(eDataType, value);
    }
}
