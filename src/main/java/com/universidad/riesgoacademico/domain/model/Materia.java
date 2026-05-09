package com.universidad.riesgoacademico.domain.model;

/**
 * Representa una materia universitaria en el grafo bipartito.
 *
 * <p>Nodo del conjunto M del grafo bipartito ponderado. Cada materia tiene
 * un código único que actúa como identificador en el índice invertido.
 *
 * @param codigo código único de la materia (ej: "CAL2", "FIS1"). No nulo ni vacío.
 * @param nombre nombre descriptivo de la materia (ej: "Cálculo II"). No nulo ni vacío.
 */
public record Materia(String codigo, String nombre) {

    /**
     * Constructor compacto con validación de precondiciones.
     */
    public Materia {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código de materia no puede ser nulo ni vacío");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de materia no puede ser nulo ni vacío");
        }
    }
}
