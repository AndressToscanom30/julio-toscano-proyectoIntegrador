package com.universidad.riesgoacademico.domain.model;

/**
 * Materia identificada como riesgo para un estudiante.
 *
 * <p>Postcondición Post3: la lista de estas instancias está ordenada de
 * mayor a menor {@code nivelRiesgo}.
 *
 * @param codigoMateria  código de la materia en riesgo
 * @param nombreMateria  nombre descriptivo de la materia
 * @param nivelRiesgo    nivel de riesgo estimado en [0.0, 1.0] donde 1.0 = máximo riesgo.
 *                       Calculado como {@code 1 - promedioNotaGemelos(materia)}.
 * @param gemelosReprobaron número de gemelos académicos que reprobaron esta materia
 */
public record MateriaEnRiesgo(
        String codigoMateria,
        String nombreMateria,
        double nivelRiesgo,
        int gemelosReprobaron
) {

    /**
     * Constructor compacto con validación.
     */
    public MateriaEnRiesgo {
        if (codigoMateria == null || codigoMateria.isBlank()) {
            throw new IllegalArgumentException("codigoMateria no puede ser nulo ni vacío");
        }
        if (nombreMateria == null || nombreMateria.isBlank()) {
            throw new IllegalArgumentException("nombreMateria no puede ser nulo ni vacío");
        }
        if (nivelRiesgo < 0.0 || nivelRiesgo > 1.0) {
            throw new IllegalArgumentException("nivelRiesgo debe estar en [0.0, 1.0]: " + nivelRiesgo);
        }
        if (gemelosReprobaron < 0) {
            throw new IllegalArgumentException("gemelosReprobaron no puede ser negativo");
        }
    }
}
