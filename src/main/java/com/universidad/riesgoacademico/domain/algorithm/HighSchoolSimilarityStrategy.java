package com.universidad.riesgoacademico.domain.algorithm;

import com.universidad.riesgoacademico.domain.model.Estudiante;
import com.universidad.riesgoacademico.domain.model.NotasColegio;
import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo.EstrategiaSimilitud;
import com.universidad.riesgoacademico.domain.service.SimilarityStrategy;

import java.util.*;

/**
 * Calcula similitud coseno entre estudiantes usando notas de colegio homologadas.
 *
 * <p>Aplica a estudiantes extranjeros que no tienen ICFES ni notas universitarias,
 * pero cuyas notas de bachillerato fueron homologadas a escala colombiana (0.0 - 5.0)
 * por el MEN o la universidad al momento de admisión.
 *
 * <p>La similitud se calcula sobre las asignaturas compartidas entre ambos
 * estudiantes. Si no hay asignaturas en común, la similitud es 0.0.
 *
 * <p>Confianza: MEDIA-BAJA. Superior al demográfico pero inferior al ICFES,
 * porque las notas de colegio dependen del nivel de exigencia del colegio de origen.
 *
 * <p>Complejidad temporal: O(N × k) donde k = asignaturas promedio por estudiante.
 * Complejidad espacial: O(N) para el mapa de similitudes.
 */
public final class HighSchoolSimilarityStrategy implements SimilarityStrategy {

    private final Map<Long, Estudiante> estudiantes;

    /**
     * Crea la estrategia con el mapa de estudiantes del sistema.
     *
     * @param estudiantes mapa de {id → Estudiante} del sistema (no nulo)
     */
    public HighSchoolSimilarityStrategy(Map<Long, Estudiante> estudiantes) {
        this.estudiantes = Objects.requireNonNull(estudiantes, "El mapa de estudiantes no puede ser nulo");
    }

    /**
     * Calcula similitud coseno sobre notas de colegio homologadas.
     *
     * <p>Solo considera las asignaturas compartidas entre ambos estudiantes
     * para el cálculo del producto punto. Las normas se calculan sobre
     * el vector completo de cada estudiante.
     *
     * @param estudianteId ID del estudiante consultado
     * @return mapa de {coEstudianteId → similitud} para estudiantes con notas de colegio
     * @throws IllegalArgumentException si el estudiante no existe o no tiene notas de colegio
     */
    @Override
    public Map<Long, Double> calcularSimilitudes(long estudianteId) {
        Estudiante estudiante = estudiantes.get(estudianteId);
        if (estudiante == null) {
            throw new IllegalArgumentException("Estudiante no encontrado: " + estudianteId);
        }
        if (!estudiante.tieneNotasColegioValidas()) {
            throw new IllegalArgumentException(
                    "Estudiante " + estudianteId + " no tiene notas de colegio válidas");
        }

        NotasColegio notasE = estudiante.getNotasColegio();
        Map<Long, Double> similitudes = new HashMap<>();

        for (Map.Entry<Long, Estudiante> entry : estudiantes.entrySet()) {
            long coId = entry.getKey();
            Estudiante co = entry.getValue();

            if (coId != estudianteId && co.tieneNotasColegioValidas()) {
                double sim = calcularSimilitudEntre(notasE, co.getNotasColegio());
                if (sim > 0.0) {
                    similitudes.put(coId, sim);
                }
            }
        }

        return Collections.unmodifiableMap(similitudes);
    }

    private static double calcularSimilitudEntre(NotasColegio a, NotasColegio b) {
        Map<String, Double> mapA = a.getNotas();
        Map<String, Double> mapB = b.getNotas();

        double productoPunto = 0.0;
        for (Map.Entry<String, Double> entry : mapA.entrySet()) {
            Double notaB = mapB.get(entry.getKey());
            if (notaB != null) {
                productoPunto += entry.getValue() * notaB;
            }
        }

        double normaA = a.getNorma();
        double normaB = b.getNorma();

        if (productoPunto <= 0.0 || normaA <= 0.0 || normaB <= 0.0) {
            return 0.0;
        }
        return Math.min(1.0, productoPunto / (normaA * normaB));
    }

    @Override
    public EstrategiaSimilitud getTipo() {
        return EstrategiaSimilitud.HIGH_SCHOOL_BASED;
    }
}
