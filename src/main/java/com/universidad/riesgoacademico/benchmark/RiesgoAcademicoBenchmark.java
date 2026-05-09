package com.universidad.riesgoacademico.benchmark;

import com.universidad.riesgoacademico.domain.algorithm.GradeSimilarityStrategy;
import com.universidad.riesgoacademico.domain.service.RiesgoAcademicoService;
import com.universidad.riesgoacademico.domain.service.SimilarityStrategy;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark JMH: compara el algoritmo principal (coseno + índice invertido)
 * contra la alternativa base (fuerza bruta pairwise) para distintos valores de N.
 *
 * <p>Configuración según la Guía de Contenido, sección 5.2:
 * <ul>
 *   <li>Modo: tiempo promedio por operación</li>
 *   <li>Unidad: milisegundos</li>
 *   <li>Warmup: 3 iteraciones de 1 segundo</li>
 *   <li>Medición: 5 iteraciones de 1 segundo</li>
 *   <li>Forks: 2</li>
 *   <li>Parámetros: N = {1000, 10000, 100000, 500000}</li>
 * </ul>
 *
 * <p>Ejecución:
 * <pre>
 * mvn clean package -DskipTests
 * java -jar target/benchmarks.jar -rf json -rff benchmark-results.json
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class RiesgoAcademicoBenchmark {

    /** Semilla fija para datos reproducibles. */
    private static final long SEED = 42L;

    /** ID del estudiante a consultar (fijo para consistencia). */
    private static final long ESTUDIANTE_CONSULTA = 1L;

    @Param({"1000", "10000", "100000", "500000"})
    int n;

    private SimilarityStrategy mainStrategy;
    private SimilarityStrategy baselineStrategy;

    /**
     * Genera los datos de prueba antes de cada iteración.
     * Usa seed fija para reproducibilidad.
     */
    @Setup(Level.Trial)
    public void setup() {
        RiesgoAcademicoService servicio = DataGenerator.generar(n, SEED);

        // Estrategia principal: coseno con índice invertido (ADR-001, Opción C)
        mainStrategy = new GradeSimilarityStrategy(servicio.getGrafo());

        // Baseline: fuerza bruta pairwise (ADR-001, Opción A)
        baselineStrategy = new BruteForceSimilarityStrategy(servicio.getGrafo());
    }

    /**
     * Benchmark del algoritmo principal: coseno con índice invertido.
     *
     * <p>Complejidad esperada: O(M_e × k_avg) por consulta.
     */
    @Benchmark
    public void mainAlgorithm(Blackhole bh) {
        Map<Long, Double> resultado = mainStrategy.calcularSimilitudes(ESTUDIANTE_CONSULTA);
        bh.consume(resultado);
    }

    /**
     * Benchmark del algoritmo baseline: fuerza bruta pairwise.
     *
     * <p>Complejidad esperada: O(N × M) por consulta.
     */
    @Benchmark
    public void baselineAlgorithm(Blackhole bh) {
        Map<Long, Double> resultado = baselineStrategy.calcularSimilitudes(ESTUDIANTE_CONSULTA);
        bh.consume(resultado);
    }
}
