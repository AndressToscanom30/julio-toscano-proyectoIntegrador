package com.universidad.riesgoacademico.domain.algorithm;

import com.universidad.riesgoacademico.domain.model.GradeEntry;
import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo.EstrategiaSimilitud;
import com.universidad.riesgoacademico.domain.service.SimilarityStrategy;

import java.util.*;

/**
 * Calcula similitud coseno entre estudiantes usando el índice invertido del grafo.
 *
 * <p>Algoritmo: similitud coseno con índice invertido por materia (ADR-001, Opción C).
 * Complejidad temporal por consulta: O(M_e × k_avg) donde M_e = materias del estudiante
 * y k_avg = promedio de co-estudiantes por materia.
 * Complejidad espacial: O(N_co) donde N_co = número de co-estudiantes encontrados.
 *
 * <p>Invariante durante la ejecución: los acumuladores de producto punto nunca son negativos
 * (las notas son >= 0), garantizando que la similitud coseno resultante está en [0.0, 1.0].
 *
 * @see GrafoBipartito
 */
public final class GradeSimilarityStrategy implements SimilarityStrategy {

    private final GrafoBipartito grafo;

    /**
     * Crea la estrategia con referencia al grafo bipartito.
     *
     * @param grafo grafo bipartito con el índice invertido construido (no nulo)
     */
    public GradeSimilarityStrategy(GrafoBipartito grafo) {
        this.grafo = Objects.requireNonNull(grafo, "El grafo no puede ser nulo");
    }

    /**
     * Calcula la similitud coseno del estudiante contra todos sus co-estudiantes
     * usando el índice invertido.
     *
     * <p>Algoritmo:
     * <ol>
     *   <li>Obtener el vector de notas del estudiante consultado.</li>
     *   <li>Para cada materia que cursó, recorrer la lista invertida acumulando
     *       el producto punto con cada co-estudiante.</li>
     *   <li>Dividir cada producto punto acumulado por el producto de las normas.</li>
     * </ol>
     *
     * <p>Complejidad temporal: O(M_e × k_avg) — solo visita co-estudiantes.
     *
     * @param estudianteId ID del estudiante consultado
     * @return mapa de {coEstudianteId → similitud coseno} para similitud > 0.0
     * @throws IllegalArgumentException si el estudiante no tiene registros en el grafo
     */
    @Override
    public Map<Long, Double> calcularSimilitudes(long estudianteId) {
        Map<String, Double> notasEstudiante = grafo.getNotasEstudiante(estudianteId);
        if (notasEstudiante.isEmpty()) {
            throw new IllegalArgumentException(
                    "Estudiante " + estudianteId + " no tiene registros en el grafo");
        }

        double normaEstudiante = grafo.getNorma(estudianteId);
        if (normaEstudiante == 0.0) {
            return Collections.emptyMap();
        }

        // Acumulador de productos punto: coEstudianteId → sum(nota_e * nota_co)
        Map<Long, Double> productoPunto = new HashMap<>();

        // Recorrer las listas invertidas de cada materia del estudiante
        for (Map.Entry<String, Double> entry : notasEstudiante.entrySet()) {
            String materiaId = entry.getKey();
            double notaEstudiante = entry.getValue();

            List<GradeEntry> coEstudiantes = grafo.getEstudiantesPorMateria(materiaId);
            for (GradeEntry coEntry : coEstudiantes) {
                if (coEntry.estudianteId() != estudianteId) {
                    productoPunto.merge(
                            coEntry.estudianteId(),
                            notaEstudiante * coEntry.nota(),
                            Double::sum
                    );
                }
            }
        }

        // Dividir por normas para obtener similitud coseno
        Map<Long, Double> similitudes = new HashMap<>(productoPunto.size());
        for (Map.Entry<Long, Double> entry : productoPunto.entrySet()) {
            long coId = entry.getKey();
            double dotProduct = entry.getValue();
            double normaCo = grafo.getNorma(coId);

            if (normaCo > 0.0) {
                double similitud = dotProduct / (normaEstudiante * normaCo);
                // Clamp por posibles errores de punto flotante
                similitud = Math.min(1.0, Math.max(0.0, similitud));
                if (similitud > 0.0) {
                    similitudes.put(coId, similitud);
                }
            }
        }

        return Collections.unmodifiableMap(similitudes);
    }

    @Override
    public EstrategiaSimilitud getTipo() {
        return EstrategiaSimilitud.GRADE_BASED;
    }
}
