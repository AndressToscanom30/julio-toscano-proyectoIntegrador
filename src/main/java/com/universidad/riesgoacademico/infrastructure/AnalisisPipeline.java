package com.universidad.riesgoacademico.infrastructure;

import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo;
import com.universidad.riesgoacademico.domain.service.RiesgoAcademicoService;
import com.universidad.riesgoacademico.infrastructure.io.DataLoader;
import com.universidad.riesgoacademico.infrastructure.io.ResultadoExporter;
import com.universidad.riesgoacademico.infrastructure.persistence.InMemoryEstudianteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pipeline de procesamiento: Cargar → Analizar → Exportar (ADR-003, Opción B).
 *
 * <p>Orquesta las tres etapas del flujo de riesgo académico usando las
 * interfaces {@link DataLoader} y {@link ResultadoExporter}, sin depender
 * de implementaciones concretas (CSV, JSON, etc.).
 *
 * <p>Complejidad del pipeline completo (batch):
 * O(|R| + N × O(consulta_similitud)) donde |R| = registros cargados,
 * N = estudiantes analizados.
 *
 * @see DataLoader
 * @see ResultadoExporter
 */
public final class AnalisisPipeline {

    /** Umbral de similitud por defecto para detección de gemelos. */
    static final double UMBRAL_SIMILITUD_DEFAULT = 0.5;

    /** Umbral de riesgo (nota de reprobación) por defecto — escala colombiana. */
    static final double UMBRAL_RIESGO_DEFAULT = 3.0;

    private final RiesgoAcademicoService servicio;
    private final InMemoryEstudianteRepository repositorio;

    /**
     * Crea el pipeline con un servicio y repositorio de resultados.
     *
     * @param servicio     servicio de riesgo académico (no nulo)
     * @param repositorio  repositorio de resultados en memoria (no nulo)
     */
    public AnalisisPipeline(RiesgoAcademicoService servicio,
                            InMemoryEstudianteRepository repositorio) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio no puede ser nulo");
        this.repositorio = Objects.requireNonNull(repositorio, "El repositorio no puede ser nulo");
    }

    /**
     * Ejecuta el pipeline completo: carga → análisis batch → exportación.
     *
     * <p>Etapas:
     * <ol>
     *   <li>Cargar datos desde la fuente configurada en el {@code DataLoader}.</li>
     *   <li>Analizar riesgo para todos los estudiantes registrados.</li>
     *   <li>Exportar resultados al formato configurado en el {@code ResultadoExporter}.</li>
     * </ol>
     *
     * @param loader   cargador de datos (no nulo)
     * @param exporter exportador de resultados (no nulo)
     * @param umbralSimilitud umbral de similitud para gemelos (0.0 < umbral ≤ 1.0)
     * @param umbralRiesgo    nota de reprobación (0.0 < umbral ≤ 5.0)
     * @return número de estudiantes analizados
     * @throws IOException si ocurre un error de I/O en carga o exportación
     */
    public int ejecutar(DataLoader loader, ResultadoExporter exporter,
                        double umbralSimilitud, double umbralRiesgo) throws IOException {
        Objects.requireNonNull(loader, "El DataLoader no puede ser nulo");
        Objects.requireNonNull(exporter, "El ResultadoExporter no puede ser nulo");

        // Etapa 1: Cargar datos
        loader.cargarDatos(servicio);

        // Etapa 2: Analizar riesgo batch
        List<ResultadoRiesgo> resultados = analizarBatch(umbralSimilitud, umbralRiesgo);

        // Etapa 3: Exportar resultados
        exporter.exportarBatch(resultados);

        return resultados.size();
    }

    /**
     * Ejecuta el pipeline con umbrales por defecto.
     *
     * @param loader   cargador de datos
     * @param exporter exportador de resultados
     * @return número de estudiantes analizados
     * @throws IOException si ocurre un error de I/O
     */
    public int ejecutar(DataLoader loader, ResultadoExporter exporter) throws IOException {
        return ejecutar(loader, exporter, UMBRAL_SIMILITUD_DEFAULT, UMBRAL_RIESGO_DEFAULT);
    }

    /**
     * Analiza el riesgo académico de todos los estudiantes registrados.
     *
     * @param umbralSimilitud umbral de similitud
     * @param umbralRiesgo    umbral de reprobación
     * @return lista de resultados de riesgo
     */
    public List<ResultadoRiesgo> analizarBatch(double umbralSimilitud, double umbralRiesgo) {
        List<ResultadoRiesgo> resultados = new ArrayList<>();

        // Obtener los IDs de estudiantes del grafo bipartito
        for (long estudianteId : servicio.getGrafo().getEstudianteIds()) {
            try {
                ResultadoRiesgo resultado = servicio.analizarRiesgo(
                        estudianteId, umbralSimilitud, umbralRiesgo);
                repositorio.guardar(resultado);
                resultados.add(resultado);
            } catch (IllegalArgumentException e) {
                // Estudiante sin datos suficientes — continuar con el siguiente
                // Log: e.getMessage()
            }
        }

        return resultados;
    }

    /**
     * @return el servicio de riesgo académico subyacente
     */
    public RiesgoAcademicoService getServicio() {
        return servicio;
    }

    /**
     * @return el repositorio de resultados en memoria
     */
    public InMemoryEstudianteRepository getRepositorio() {
        return repositorio;
    }
}
