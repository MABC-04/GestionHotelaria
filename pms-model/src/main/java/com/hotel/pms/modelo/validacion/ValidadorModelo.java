package com.hotel.pms.modelo.validacion;

import java.util.ArrayList;
import java.util.List;

import com.hotel.pms.modelo.ModeloReservas;

/** Ejecuta el catálogo completo de invariantes sobre un modelo. */
public final class ValidadorModelo {

    private final List<Invariante> catalogo;

    public ValidadorModelo() {
        this(CatalogoInvariantes.todos());
    }

    public ValidadorModelo(List<Invariante> catalogo) {
        this.catalogo = List.copyOf(catalogo);
    }

    public Reporte validar(ModeloReservas modelo) {
        Reporte reporte = new Reporte();
        for (Invariante invariante : catalogo) {
            invariante.chequeo().accept(modelo, reporte);
        }
        return reporte;
    }

    public List<Violacion> violacionesDe(ModeloReservas modelo, String sufijoInvariante) {
        List<Violacion> filtradas = new ArrayList<>();
        for (Violacion v : validar(modelo).todas()) {
            if (v.invariante().endsWith(sufijoInvariante)) {
                filtradas.add(v);
            }
        }
        return filtradas;
    }

    public List<Invariante> catalogo() {
        return catalogo;
    }
}
