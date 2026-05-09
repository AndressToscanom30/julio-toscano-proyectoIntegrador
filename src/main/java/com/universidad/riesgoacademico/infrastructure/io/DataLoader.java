package com.universidad.riesgoacademico.infrastructure.io;

import com.universidad.riesgoacademico.domain.service.RiesgoAcademicoService;

import java.io.IOException;

/**
 * Interfaz para cargar datos de entrada al sistema de riesgo académico.
 *
 * <p>Cada implementación define la fuente y formato de los datos (CSV, Excel,
 * base de datos, etc.) sin que el dominio conozca estos detalles (ADR-003).
 *
 * <p>Postcondición: después de invocar {@link #cargarDatos}, el servicio
 * contiene los estudiantes, materias y registros académicos de la fuente.
 *
 * @see CsvDataLoader
 */
public interface DataLoader {

    /**
     * Carga estudiantes, materias y registros académicos al servicio.
     *
     * @param servicio servicio de riesgo académico donde registrar los datos (no nulo)
     * @throws IOException si ocurre un error de lectura en la fuente de datos
     * @throws IllegalArgumentException si los datos contienen valores inválidos
     */
    void cargarDatos(RiesgoAcademicoService servicio) throws IOException;
}
