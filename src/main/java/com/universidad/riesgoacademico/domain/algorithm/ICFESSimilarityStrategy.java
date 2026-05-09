package com.universidad.riesgoacademico.domain.algorithm;

import com.universidad.riesgoacademico.domain.model.Estudiante;
import com.universidad.riesgoacademico.domain.model.PuntajeICFES;
import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo.EstrategiaSimilitud;
import com.universidad.riesgoacademico.domain.service.SimilarityStrategy;

import java.util.*;

/**
 * Calcula similitud coseno entre estudiantes usando vectores ICFES (Saber 11).
 *
 * <p>Aplica a estudiantes colombianos de primer semestre que tienen ICFES pero
 * aún no tienen notas universitarias suficientes (R_e < 2).
 *
 * <p>Complejidad temporal: O(N × 5) = O(N) donde N = número de estudiantes con ICFES.
 * Complejidad espacial: O(N) para el mapa de similitudes.
 */
public final class ICFESSimilarityStrategy implements SimilarityStrategy {

    private final Map<Long, Estudiante> estudiantes;

    /**
     * Crea la estrategia con el mapa de estudiantes del sistema.
     *
     * @param estudiantes mapa de {id → Estudiante} del sistema (no nulo)
     */
    public ICFESSimilarityStrategy(Map<Long, Estudiante> estudiantes) {
        this.estudiantes = Objects.requireNonNull(estudiantes, "El mapa de estudiantes no puede ser nulo");
    }

    /**
     * Calcula similitud coseno sobre el vector ICFES de 5 componentes.
     *
     * @param estudianteId ID del estudiante consultado
     * @return mapa de {coEstudianteId → similitud} para estudiantes con ICFES y similitud > 0
     * @throws IllegalArgumentException si el estudiante no existe o no tiene ICFES válido
     */
    @Override
    public Map<Long, Double> calcularSimilitudes(long estudianteId) {
        Estudiante estudiante = estudiantes.get(estudianteId);
        if (estudiante == null) {
            throw new IllegalArgumentException("Estudiante no encontrado: " + estudianteId);
        }
        if (!estudiante.tieneIcfesValido()) {
            throw new IllegalArgumentException(
                    "Estudiante " + estudianteId + " no tiene ICFES válido para esta estrategia");
        }

        double[] vectorE = estudiante.getIcfes().toVector();
        double normaE = estudiante.getIcfes().norma();

        Map<Long, Double> similitudes = new HashMap<>();

        for (Map.Entry<Long, Estudiante> entry : estudiantes.entrySet()) {
            long coId = entry.getKey();
            Estudiante co = entry.getValue();

            if (coId == estudianteId || !co.tieneIcfesValido()) {
                continue;
            }

            double[] vectorCo = co.getIcfes().toVector();
            double normaCo = co.getIcfes().norma();

            double productoPunto = calcularProductoPunto(vectorE, vectorCo);
            double similitud = productoPunto / (normaE * normaCo);
            similitud = Math.min(1.0, Math.max(0.0, similitud));

            if (similitud > 0.0) {
                similitudes.put(coId, similitud);
            }
        }

        return Collections.unmodifiableMap(similitudes);
    }

    @Override
    public EstrategiaSimilitud getTipo() {
        return EstrategiaSimilitud.ICFES_BASED;
    }

    private static double calcularProductoPunto(double[] a, double[] b) {
        double suma = 0.0;
        for (int i = 0; i < PuntajeICFES.NUM_COMPONENTES; i++) {
            suma += a[i] * b[i];
        }
        return suma;
    }
}
