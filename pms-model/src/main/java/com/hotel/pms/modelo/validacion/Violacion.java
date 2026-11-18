package com.hotel.pms.modelo.validacion;

/** Una violación concreta de un invariante sobre un objeto del modelo. */
public record Violacion(String regla, String invariante, String objeto, String mensaje) {

    @Override
    public String toString() {
        return "[" + regla + "/" + invariante + "] " + objeto + ": " + mensaje;
    }
}
