package com.universidad.riesgoacademico.domain.model;

import java.util.List;

/**
 * Resultado del análisis de riesgo académico para un estudiante.
 *
 * <p>Contiene la lista de materias en riesgo ordenada de mayor a menor nivel
 * de riesgo, la estrategia utilizada para el cálculo y los gemelos académicos
 * identificados.
 *
 * @param estudianteId       ID del estudiante consultado
 * @param materiasEnRiesgo   lista de materias en riesgo, ordenada de mayor a menor riesgo (Post3)
 * @param estrategiaUsada    estrategia de similitud utilizada (Post4)
 * @param gemelosIdentificados número de gemelos académicos encontrados con similitud >= umbral
 */
public record ResultadoRiesgo(
        long estudianteId,
        List<MateriaEnRiesgo> materiasEnRiesgo,
        EstrategiaSimilitud estrategiaUsada,
        int gemelosIdentificados
) {

    /**
     * Estrategias de similitud disponibles en el sistema (Post4).
     */
    public enum EstrategiaSimilitud {
        /** Similitud coseno sobre vector de notas universitarias. */
        GRADE_BASED,
        /** Similitud coseno sobre vector ICFES de 5 componentes. */
        ICFES_BASED,
        /** Combinación ponderada: 70% notas + 30% ICFES normalizado. */
        HYBRID,
        /** Agrupación demográfica por programa + tipo de colegio. Confianza baja. */
        DEMOGRAPHIC_FALLBACK
    }

    /**
     * Constructor compacto con validación.
     */
    public ResultadoRiesgo {
        if (estudianteId < Estudiante.ID_MINIMO) {
            throw new IllegalArgumentException("estudianteId inválido: " + estudianteId);
        }
        if (materiasEnRiesgo == null) {
            throw new IllegalArgumentException("materiasEnRiesgo no puede ser nulo");
        }
        if (estrategiaUsada == null) {
            throw new IllegalArgumentException("estrategiaUsada no puede ser nulo");
        }
        if (gemelosIdentificados < 0) {
            throw new IllegalArgumentException("gemelosIdentificados no puede ser negativo");
        }
        // Inmutabilidad: copia defensiva de la lista
        materiasEnRiesgo = List.copyOf(materiasEnRiesgo);
    }
}
