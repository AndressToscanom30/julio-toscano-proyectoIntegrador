package com.universidad.riesgoacademico.domain.model;

/**
 * Entrada del índice invertido: un estudiante con su nota en una materia.
 *
 * <p>Utilizado internamente por el índice invertido del grafo bipartito
 * (materia → lista de {@code GradeEntry}). Almacenado en {@code ArrayList}
 * para localidad de caché en recorridos secuenciales (ADR-002).
 *
 * @param estudianteId ID del estudiante
 * @param nota         nota obtenida en la materia [0.0, 5.0]
 */
public record GradeEntry(long estudianteId, double nota) {

    /**
     * Constructor compacto con validación.
     */
    public GradeEntry {
        if (estudianteId < Estudiante.ID_MINIMO) {
            throw new IllegalArgumentException("estudianteId inválido: " + estudianteId);
        }
        if (nota < RegistroAcademico.NOTA_MINIMA || nota > RegistroAcademico.NOTA_MAXIMA) {
            throw new IllegalArgumentException("Nota fuera de rango: " + nota);
        }
    }
}
