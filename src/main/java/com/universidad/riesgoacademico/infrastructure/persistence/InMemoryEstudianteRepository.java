package com.universidad.riesgoacademico.infrastructure.persistence;

import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo;

import java.util.*;

/**
 * Repositorio en memoria para almacenar y consultar resultados de análisis de riesgo.
 *
 * <p>Almacena los resultados de análisis más recientes por estudiante en un
 * {@code HashMap}, permitiendo consultas posteriores sin recalcular.
 *
 * <p>Complejidad: O(1) para guardar y consultar.
 * Complejidad espacial: O(N) donde N = número de estudiantes analizados.
 *
 * <p>Decisión ADR-003: persistencia en memoria en lugar de archivo o base de datos.
 * Suficiente para el alcance del proyecto integrador; los resultados se exportan
 * a JSON mediante {@code JsonResultadoExporter} cuando se requiere persistencia duradera.
 */
public final class InMemoryEstudianteRepository {

    private final Map<Long, ResultadoRiesgo> resultados;

    /**
     * Crea un repositorio vacío con capacidad por defecto.
     */
    public InMemoryEstudianteRepository() {
        this.resultados = new HashMap<>();
    }

    /**
     * Crea un repositorio vacío con capacidad estimada.
     *
     * @param capacidad número estimado de estudiantes
     */
    public InMemoryEstudianteRepository(int capacidad) {
        this.resultados = new HashMap<>((int) (capacidad / 0.75) + 1);
    }

    /**
     * Guarda (o reemplaza) el resultado de análisis de riesgo de un estudiante.
     *
     * @param resultado resultado del análisis (no nulo)
     * @throws NullPointerException si resultado es nulo
     */
    public void guardar(ResultadoRiesgo resultado) {
        Objects.requireNonNull(resultado, "El resultado no puede ser nulo");
        resultados.put(resultado.estudianteId(), resultado);
    }

    /**
     * Consulta el resultado de análisis más reciente de un estudiante.
     *
     * @param estudianteId ID del estudiante
     * @return Optional con el resultado, o vacío si el estudiante no ha sido analizado
     */
    public Optional<ResultadoRiesgo> consultar(long estudianteId) {
        return Optional.ofNullable(resultados.get(estudianteId));
    }

    /**
     * Verifica si un estudiante tiene un resultado almacenado.
     *
     * @param estudianteId ID del estudiante
     * @return true si el estudiante ha sido analizado previamente
     */
    public boolean existe(long estudianteId) {
        return resultados.containsKey(estudianteId);
    }

    /**
     * @return todos los resultados almacenados (copia inmutable)
     */
    public List<ResultadoRiesgo> obtenerTodos() {
        return List.copyOf(resultados.values());
    }

    /**
     * @return número de resultados almacenados
     */
    public int size() {
        return resultados.size();
    }

    /**
     * Elimina todos los resultados almacenados.
     */
    public void limpiar() {
        resultados.clear();
    }
}
