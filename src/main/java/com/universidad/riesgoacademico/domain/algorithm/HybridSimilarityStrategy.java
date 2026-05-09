package com.universidad.riesgoacademico.domain.algorithm;

import com.universidad.riesgoacademico.domain.model.Estudiante;
import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo.EstrategiaSimilitud;
import com.universidad.riesgoacademico.domain.service.SimilarityStrategy;

import java.util.*;

/**
 * Estrategia híbrida: combina similitud por notas (70%) con similitud por ICFES (30%).
 *
 * <p>Aplica a estudiantes que tienen tanto notas universitarias (R_e >= 2) como
 * puntaje ICFES. Produce la mayor confianza de predicción.
 *
 * <p>Complejidad temporal: O(M_e × k_avg + N) — costo de GradeSimilarity + ICFES.
 * Complejidad espacial: O(N) para los mapas de similitudes.
 */
public final class HybridSimilarityStrategy implements SimilarityStrategy {

    /** Peso del componente de notas universitarias. */
    static final double PESO_NOTAS = 0.70;

    /** Peso del componente ICFES. */
    static final double PESO_ICFES = 0.30;

    private final GradeSimilarityStrategy gradeStrategy;
    private final ICFESSimilarityStrategy icfesStrategy;

    /**
     * Crea la estrategia híbrida combinando ambas sub-estrategias.
     *
     * @param gradeStrategy estrategia de similitud por notas (no nula)
     * @param icfesStrategy estrategia de similitud por ICFES (no nula)
     */
    public HybridSimilarityStrategy(GradeSimilarityStrategy gradeStrategy,
                                     ICFESSimilarityStrategy icfesStrategy) {
        this.gradeStrategy = Objects.requireNonNull(gradeStrategy);
        this.icfesStrategy = Objects.requireNonNull(icfesStrategy);
    }

    /**
     * Calcula la similitud híbrida ponderada.
     *
     * <p>similitud_hibrida = 0.70 × similitud_notas + 0.30 × similitud_icfes
     *
     * <p>Si un co-estudiante solo aparece en una de las dos sub-estrategias,
     * el componente faltante se trata como 0.0.
     *
     * @param estudianteId ID del estudiante consultado
     * @return mapa de {coEstudianteId → similitud_hibrida} para similitud > 0
     */
    @Override
    public Map<Long, Double> calcularSimilitudes(long estudianteId) {
        Map<Long, Double> simNotas = gradeStrategy.calcularSimilitudes(estudianteId);
        Map<Long, Double> simIcfes = icfesStrategy.calcularSimilitudes(estudianteId);

        // Unión de ambos conjuntos de co-estudiantes
        Set<Long> todosCoEstudiantes = new HashSet<>(simNotas.keySet());
        todosCoEstudiantes.addAll(simIcfes.keySet());

        Map<Long, Double> resultado = new HashMap<>(todosCoEstudiantes.size());

        for (long coId : todosCoEstudiantes) {
            double componenteNotas = simNotas.getOrDefault(coId, 0.0);
            double componenteIcfes = simIcfes.getOrDefault(coId, 0.0);
            double hibrida = PESO_NOTAS * componenteNotas + PESO_ICFES * componenteIcfes;

            hibrida = Math.min(1.0, Math.max(0.0, hibrida));
            if (hibrida > 0.0) {
                resultado.put(coId, hibrida);
            }
        }

        return Collections.unmodifiableMap(resultado);
    }

    @Override
    public EstrategiaSimilitud getTipo() {
        return EstrategiaSimilitud.HYBRID;
    }
}
