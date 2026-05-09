package com.universidad.riesgoacademico.infrastructure.io;

import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo;

import java.io.IOException;
import java.util.List;

/**
 * Interfaz para exportar resultados del análisis de riesgo académico.
 *
 * <p>Cada implementación define el formato de salida (JSON, CSV, XML, etc.)
 * sin que el dominio conozca estos detalles (ADR-003).
 *
 * @see JsonResultadoExporter
 */
public interface ResultadoExporter {

    /**
     * Exporta un único resultado de riesgo al destino configurado.
     *
     * @param resultado resultado del análisis de riesgo (no nulo)
     * @throws IOException si ocurre un error de escritura
     */
    void exportar(ResultadoRiesgo resultado) throws IOException;

    /**
     * Exporta una lista de resultados de riesgo al destino configurado.
     *
     * @param resultados lista de resultados (no nula)
     * @throws IOException si ocurre un error de escritura
     */
    void exportarBatch(List<ResultadoRiesgo> resultados) throws IOException;
}
