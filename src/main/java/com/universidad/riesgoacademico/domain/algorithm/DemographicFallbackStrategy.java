package com.universidad.riesgoacademico.domain.algorithm;

import com.universidad.riesgoacademico.domain.model.Estudiante;
import com.universidad.riesgoacademico.domain.model.PerfilAdmision;
import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo.EstrategiaSimilitud;
import com.universidad.riesgoacademico.domain.service.SimilarityStrategy;

import java.util.*;

/**
 * Estrategia de fallback demográfico para cold start verdadero.
 *
 * <p>Aplica a estudiantes sin ICFES y sin notas universitarias (ej: extranjeros
 * en primer semestre). Agrupa por programa académico + tipo de colegio de origen
 * y asigna similitud fija a todos los miembros del mismo grupo.
 *
 * <p>Confianza: BAJA. El sistema emite advertencia COLD_START_VERDADERO.
 *
 * <p>Complejidad temporal: O(N) — recorre todos los estudiantes buscando coincidencias.
 * Complejidad espacial: O(k) donde k = estudiantes del mismo grupo demográfico.
 */
public final class DemographicFallbackStrategy implements SimilarityStrategy {

    /**
     * Similitud fija asignada a estudiantes del mismo grupo demográfico.
     * Valor bajo (0.3) que refleja la baja confianza de esta estrategia.
     */
    static final double SIMILITUD_DEMOGRAFICA = 0.3;

    private final Map<Long, Estudiante> estudiantes;

    /**
     * Crea la estrategia con el mapa de estudiantes del sistema.
     *
     * @param estudiantes mapa de {id → Estudiante} del sistema (no nulo)
     */
    public DemographicFallbackStrategy(Map<Long, Estudiante> estudiantes) {
        this.estudiantes = Objects.requireNonNull(estudiantes, "El mapa de estudiantes no puede ser nulo");
    }

    /**
     * Calcula similitud fija para estudiantes del mismo grupo demográfico.
     *
     * <p>Un grupo demográfico se define por:
     * <ul>
     *   <li>Mismo programa académico</li>
     *   <li>Mismo tipo de colegio de origen (público/privado)</li>
     * </ul>
     *
     * @param estudianteId ID del estudiante consultado
     * @return mapa de {coEstudianteId → 0.3} para los del mismo grupo demográfico
     * @throws IllegalArgumentException si el estudiante no tiene perfil de admisión
     */
    @Override
    public Map<Long, Double> calcularSimilitudes(long estudianteId) {
        Estudiante estudiante = estudiantes.get(estudianteId);
        if (estudiante == null) {
            throw new IllegalArgumentException("Estudiante no encontrado: " + estudianteId);
        }

        PerfilAdmision perfil = estudiante.getPerfilAdmision();
        if (perfil == null) {
            return Collections.emptyMap();
        }

        Map<Long, Double> similitudes = new HashMap<>();

        for (Map.Entry<Long, Estudiante> entry : estudiantes.entrySet()) {
            long coId = entry.getKey();
            Estudiante co = entry.getValue();

            if (coId == estudianteId) {
                continue;
            }

            PerfilAdmision perfilCo = co.getPerfilAdmision();
            if (perfilCo != null && coincideGrupo(perfil, perfilCo)) {
                similitudes.put(coId, SIMILITUD_DEMOGRAFICA);
            }
        }

        return Collections.unmodifiableMap(similitudes);
    }

    @Override
    public EstrategiaSimilitud getTipo() {
        return EstrategiaSimilitud.DEMOGRAPHIC_FALLBACK;
    }

    private static boolean coincideGrupo(PerfilAdmision a, PerfilAdmision b) {
        return a.programaAcademico().equals(b.programaAcademico())
                && a.tipoColegio() == b.tipoColegio();
    }
}
