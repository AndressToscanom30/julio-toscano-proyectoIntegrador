package com.universidad.riesgoacademico.benchmark;

import com.universidad.riesgoacademico.domain.algorithm.GrafoBipartito;
import com.universidad.riesgoacademico.domain.model.GradeEntry;
import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo.EstrategiaSimilitud;
import com.universidad.riesgoacademico.domain.service.SimilarityStrategy;

import java.util.*;

/**
 * Algoritmo baseline: similitud coseno por fuerza bruta (pairwise).
 *
 * <p>Implementa la Opción A del ADR-001: para cada consulta, compara el
 * estudiante contra <b>todos</b> los demás estudiantes iterando sobre el
 * espacio completo de materias.
 *
 * <p>Complejidad temporal por consulta: O(N × M) donde N = estudiantes, M = materias.
 * Complejidad espacial: O(N) para el mapa de similitudes.
 *
 * <p><b>Solo para benchmarks.</b> No se usa en producción — el rendimiento
 * es inaceptable para N &gt; 10 000 con la restricción de latencia &lt; 500 ms.
 *
 * @see com.universidad.riesgoacademico.domain.algorithm.GradeSimilarityStrategy
 */
public final class BruteForceSimilarityStrategy implements SimilarityStrategy {

    private final GrafoBipartito grafo;

    /**
     * Crea la estrategia de fuerza bruta con referencia al grafo.
     *
     * @param grafo grafo bipartito (no nulo)
     */
    public BruteForceSimilarityStrategy(GrafoBipartito grafo) {
        this.grafo = Objects.requireNonNull(grafo, "El grafo no puede ser nulo");
    }

    /**
     * Calcula similitud coseno del estudiante contra TODOS los demás — O(N × M).
     *
     * <p>Algoritmo:
     * <ol>
     *   <li>Obtener el vector de notas del estudiante consultado.</li>
     *   <li>Para cada otro estudiante en el sistema, calcular el producto punto
     *       iterando sobre TODAS las materias del sistema.</li>
     *   <li>Dividir por normas.</li>
     * </ol>
     *
     * @param estudianteId ID del estudiante consultado
     * @return mapa de {coEstudianteId → similitud coseno} para similitud &gt; 0.0
     */
    @Override
    public Map<Long, Double> calcularSimilitudes(long estudianteId) {
        Map<String, Double> notasE = grafo.getNotasEstudiante(estudianteId);
        if (notasE.isEmpty()) {
            return Collections.emptyMap();
        }

        double normaE = grafo.getNorma(estudianteId);
        if (normaE == 0.0) {
            return Collections.emptyMap();
        }

        Map<Long, Double> similitudes = new HashMap<>();

        // Fuerza bruta: comparar contra TODOS los otros estudiantes
        for (long coId : grafo.getEstudianteIds()) {
            if (coId == estudianteId) {
                continue;
            }

            Map<String, Double> notasCo = grafo.getNotasEstudiante(coId);
            double normaCo = grafo.getNorma(coId);

            if (normaCo == 0.0) {
                continue;
            }

            // Producto punto: iterar sobre materias del estudiante consultado
            double productoPunto = 0.0;
            for (Map.Entry<String, Double> entry : notasE.entrySet()) {
                Double notaCo = notasCo.get(entry.getKey());
                if (notaCo != null) {
                    productoPunto += entry.getValue() * notaCo;
                }
            }

            if (productoPunto > 0.0) {
                double similitud = productoPunto / (normaE * normaCo);
                similitud = Math.min(1.0, Math.max(0.0, similitud));
                if (similitud > 0.0) {
                    similitudes.put(coId, similitud);
                }
            }
        }

        return similitudes;
    }

    @Override
    public EstrategiaSimilitud getTipo() {
        return EstrategiaSimilitud.GRADE_BASED;
    }
}
