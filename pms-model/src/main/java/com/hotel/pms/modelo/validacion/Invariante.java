package com.hotel.pms.modelo.validacion;

import java.util.function.BiConsumer;

import com.hotel.pms.modelo.ModeloReservas;

/**
 * Una invariante del catálogo OCL y su implementación ejecutable.
 *
 * @param regla    identificador de la regla de negocio ({@code RN-01})
 * @param nombre   nombre del invariante, idéntico al de {@code reservas.ocl}
 * @param chequeo  verificación sobre el modelo
 */
public record Invariante(String regla, String nombre, BiConsumer<ModeloReservas, Reporte> chequeo) {
}
