package com.universidad.riesgoacademico.domain.model;

/**
 * Registro académico: arista del grafo bipartito ponderado.
 *
 * <p>Representa la relación (estudiante, materia, nota) — una arista ponderada
 * en el grafo bipartito donde el peso es la nota obtenida.
 *
 * <p>Invariante (Inv2): {@code 0.0 <= nota <= 5.0}.
 *
 * @param estudianteId ID del estudiante (>= 1)
 * @param materiaId    código de la materia (no nulo ni vacío)
 * @param nota         calificación obtenida [0.0, 5.0]
 */
public record RegistroAcademico(long estudianteId, String materiaId, double nota) {

    /** Nota mínima válida en escala colombiana. */
    public static final double NOTA_MINIMA = 0.0;

    /** Nota máxima válida en escala colombiana. */
    public static final double NOTA_MAXIMA = 5.0;

    /**
     * Constructor compacto con validación de precondiciones (Pre3, Pre5).
     */
    public RegistroAcademico {
        if (estudianteId < Estudiante.ID_MINIMO) {
            throw new IllegalArgumentException(
                    "estudianteId debe ser >= " + Estudiante.ID_MINIMO + ", recibido: " + estudianteId);
        }
        if (materiaId == null || materiaId.isBlank()) {
            throw new IllegalArgumentException("materiaId no puede ser nulo ni vacío");
        }
        if (nota < NOTA_MINIMA || nota > NOTA_MAXIMA) {
            throw new IllegalArgumentException(
                    String.format("Nota fuera de rango [%.1f, %.1f]: %.2f", NOTA_MINIMA, NOTA_MAXIMA, nota));
        }
    }
}
