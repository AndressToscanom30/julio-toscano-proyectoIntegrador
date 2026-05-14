package com.universidad.riesgoacademico.domain.model;

import java.util.Map;
import java.util.Objects;

/**
 * Notas de colegio homologadas a escala colombiana (0.0 - 5.0).
 *
 * <p>Para estudiantes extranjeros cuyas notas de bachillerato fueron homologadas
 * por el MEN o la universidad al momento de la admisión. El vector de notas
 * permite calcular similitud coseno contra otros estudiantes que también tienen
 * notas de colegio homologadas.
 *
 * <p>Invariante: cada nota satisface {@code 0.0 <= nota <= 5.0} (escala colombiana).
 */
public final class NotasColegio {

    private final Map<String, Double> notas; // asignatura → nota homologada
    private final double norma;

    /**
     * Crea un registro de notas de colegio homologadas.
     *
     * @param notas mapa de {asignatura → nota} con notas en escala colombiana [0.0, 5.0].
     *              No nulo, no vacío.
     * @throws IllegalArgumentException si alguna nota está fuera del rango [0.0, 5.0]
     *                                  o si el mapa es nulo o vacío
     */
    public NotasColegio(Map<String, Double> notas) {
        Objects.requireNonNull(notas, "El mapa de notas de colegio no puede ser nulo");
        if (notas.isEmpty()) {
            throw new IllegalArgumentException("El mapa de notas de colegio no puede estar vacío");
        }
        double sumaCuadrados = 0.0;
        for (Map.Entry<String, Double> entry : notas.entrySet()) {
            double nota = entry.getValue();
            if (nota < RegistroAcademico.NOTA_MINIMA || nota > RegistroAcademico.NOTA_MAXIMA) {
                throw new IllegalArgumentException(
                        String.format("Nota de colegio '%s' fuera de rango [%.1f, %.1f]: %.2f",
                                entry.getKey(), RegistroAcademico.NOTA_MINIMA,
                                RegistroAcademico.NOTA_MAXIMA, nota));
            }
            sumaCuadrados += nota * nota;
        }
        this.notas = Map.copyOf(notas); // copia defensiva inmutable
        this.norma = Math.sqrt(sumaCuadrados);
    }

    /**
     * @return mapa inmutable de {asignatura → nota homologada}
     */
    public Map<String, Double> getNotas() {
        return notas;
    }

    /**
     * @return norma euclidiana del vector de notas (precalculada)
     */
    public double getNorma() {
        return norma;
    }

    /**
     * @return true si la norma es 0 (todas las notas son 0.0)
     */
    public boolean esVectorCero() {
        return norma == 0.0;
    }

    /**
     * @return número de asignaturas en el registro de colegio
     */
    public int size() {
        return notas.size();
    }
}
