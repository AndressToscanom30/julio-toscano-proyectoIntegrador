package com.universidad.riesgoacademico.benchmark;

import com.universidad.riesgoacademico.domain.model.*;
import com.universidad.riesgoacademico.domain.model.PerfilAdmision.TipoColegio;
import com.universidad.riesgoacademico.domain.service.RiesgoAcademicoService;

import java.util.Random;

/**
 * Generador de datos sintéticos para benchmarks JMH.
 *
 * <p>Produce N estudiantes, M materias y registros académicos aleatorios con
 * distribución realista (cada estudiante cursa entre 5 y 40 materias).
 *
 * <p>Usa semilla fija para reproducibilidad: misma seed → mismos datos en cada ejecución.
 */
public final class DataGenerator {

    /** Número de materias en el sistema (fijo para todos los benchmarks). */
    static final int NUM_MATERIAS = 200;

    /** Mínimo de materias cursadas por estudiante. */
    private static final int MIN_MATERIAS_POR_ESTUDIANTE = 5;

    /** Máximo de materias cursadas por estudiante. */
    private static final int MAX_MATERIAS_POR_ESTUDIANTE = 40;

    /** Nota mínima generada. */
    private static final double NOTA_MIN = 0.5;

    /** Nota máxima generada. */
    private static final double NOTA_MAX = 5.0;

    /** Proporción de estudiantes con ICFES (colombianos). */
    private static final double PROPORCION_CON_ICFES = 0.85;

    private static final String[] PROGRAMAS = {
            "ING_SISTEMAS", "ING_CIVIL", "ING_INDUSTRIAL",
            "MEDICINA", "DERECHO", "PSICOLOGIA", "ADMINISTRACION"
    };

    private DataGenerator() {
        // Utility class
    }

    /**
     * Genera un servicio cargado con N estudiantes y datos sintéticos.
     *
     * @param n    número de estudiantes
     * @param seed semilla para reproducibilidad
     * @return servicio con datos cargados, listo para consultas
     */
    public static RiesgoAcademicoService generar(int n, long seed) {
        Random rng = new Random(seed);
        RiesgoAcademicoService servicio = new RiesgoAcademicoService(n);

        // Registrar materias
        for (int m = 0; m < NUM_MATERIAS; m++) {
            String codigo = "MAT" + String.format("%03d", m);
            String nombre = "Materia " + m;
            servicio.registrarMateria(new Materia(codigo, nombre));
        }

        // Registrar estudiantes y sus registros académicos
        for (int i = 1; i <= n; i++) {
            Estudiante estudiante = generarEstudiante(i, rng);
            servicio.registrarEstudiante(estudiante);

            // Generar registros académicos
            int numMaterias = MIN_MATERIAS_POR_ESTUDIANTE
                    + rng.nextInt(MAX_MATERIAS_POR_ESTUDIANTE - MIN_MATERIAS_POR_ESTUDIANTE + 1);
            numMaterias = Math.min(numMaterias, NUM_MATERIAS);

            // Seleccionar materias aleatorias (sin repetición)
            boolean[] seleccionadas = new boolean[NUM_MATERIAS];
            int cursadas = 0;
            while (cursadas < numMaterias) {
                int idx = rng.nextInt(NUM_MATERIAS);
                if (!seleccionadas[idx]) {
                    seleccionadas[idx] = true;
                    String codigoMateria = "MAT" + String.format("%03d", idx);
                    double nota = generarNota(rng);
                    servicio.agregarRegistroAcademico(
                            new RegistroAcademico(i, codigoMateria, nota));
                    cursadas++;
                }
            }
        }

        return servicio;
    }

    private static Estudiante generarEstudiante(int id, Random rng) {
        String nombre = "Estudiante_" + id;
        String programa = PROGRAMAS[rng.nextInt(PROGRAMAS.length)];
        TipoColegio tipo = rng.nextBoolean() ? TipoColegio.PUBLICO : TipoColegio.PRIVADO;
        String pais = rng.nextDouble() < PROPORCION_CON_ICFES ? "CO" : "VE";

        PerfilAdmision perfil = new PerfilAdmision(programa, tipo, pais);

        PuntajeICFES icfes = null;
        if ("CO".equals(pais)) {
            icfes = new PuntajeICFES(
                    generarPuntajeIcfes(rng),
                    generarPuntajeIcfes(rng),
                    generarPuntajeIcfes(rng),
                    generarPuntajeIcfes(rng),
                    generarPuntajeIcfes(rng)
            );
        }

        return new Estudiante(id, nombre, icfes, perfil);
    }

    private static double generarNota(Random rng) {
        // Distribución gaussiana centrada en 3.5, truncada a [0.5, 5.0]
        double nota = 3.5 + rng.nextGaussian() * 0.8;
        nota = Math.max(NOTA_MIN, Math.min(NOTA_MAX, nota));
        return Math.round(nota * 100.0) / 100.0;
    }

    private static int generarPuntajeIcfes(Random rng) {
        // Distribución gaussiana centrada en 60, truncada a [1, 100]
        int puntaje = (int) (60 + rng.nextGaussian() * 15);
        return Math.max(1, Math.min(100, puntaje));
    }
}
