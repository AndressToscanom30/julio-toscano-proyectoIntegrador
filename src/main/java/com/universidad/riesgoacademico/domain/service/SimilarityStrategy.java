package com.universidad.riesgoacademico.domain.service;

import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo.EstrategiaSimilitud;

import java.util.Map;

/**
 * Interfaz del patrón Strategy para el cálculo de similitud entre estudiantes.
 *
 * <p>Cada implementación define una forma diferente de calcular la similitud
 * según los datos disponibles del estudiante (notas, ICFES, perfil demográfico).
 *
 * <p>Invariante Inv3: la similitud retornada satisface {@code 0.0 <= similitud <= 1.0}.
 *
 * @see com.universidad.riesgoacademico.domain.algorithm.GradeSimilarityStrategy
 * @see com.universidad.riesgoacademico.domain.algorithm.ICFESSimilarityStrategy
 * @see com.universidad.riesgoacademico.domain.algorithm.HybridSimilarityStrategy
 * @see com.universidad.riesgoacademico.domain.algorithm.DemographicFallbackStrategy
 */
public interface SimilarityStrategy {

    /**
     * Calcula la similitud del estudiante consultado contra todos los demás
     * estudiantes del sistema.
     *
     * <p>Postcondición: cada valor en el mapa retornado satisface
     * {@code 0.0 <= similitud <= 1.0} (Inv3).
     *
     * @param estudianteId ID del estudiante consultado
     * @return mapa de {estudianteId → similitud} para todos los estudiantes
     *         con similitud > 0.0. Estudiantes sin relación no aparecen.
     */
    Map<Long, Double> calcularSimilitudes(long estudianteId);

    /**
     * @return la estrategia de similitud que esta implementación representa
     */
    EstrategiaSimilitud getTipo();
}
