package com.universidad.riesgoacademico.domain.algorithm;

import com.universidad.riesgoacademico.domain.model.*;
import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo.EstrategiaSimilitud;
import com.universidad.riesgoacademico.domain.service.RiesgoAcademicoService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas basadas en propiedades (PBT) con jqwik.
 * Verifican invariantes algebraicas del sistema que deben cumplirse
 * para cualquier entrada válida generada aleatoriamente.
 */
class AlgorithmPropertyTest {

    // ========== Propiedad 1: Similitud coseno está acotada en [0, 1] (Inv3) ==========

    @Property(tries = 200, seed = "12345")
    @Report(Reporting.GENERATED)
    void similitudCoseno_siempreEnRangoCeroUno(
            @ForAll @IntRange(min = 2, max = 20) int numEstudiantes,
            @ForAll @IntRange(min = 2, max = 10) int numMaterias
    ) {
        GrafoBipartito grafo = new GrafoBipartito(numEstudiantes);
        Random rng = new Random(42L);

        // Generar registros aleatorios
        for (int e = 1; e <= numEstudiantes; e++) {
            int materiasACursar = 1 + rng.nextInt(Math.min(numMaterias, 5));
            Set<Integer> materiasElegidas = new HashSet<>();
            while (materiasElegidas.size() < materiasACursar) {
                materiasElegidas.add(1 + rng.nextInt(numMaterias));
            }
            for (int m : materiasElegidas) {
                double nota = Math.round(rng.nextDouble() * 5.0 * 100.0) / 100.0;
                nota = Math.min(5.0, Math.max(0.0, nota));
                grafo.agregarRegistro(new RegistroAcademico(e, "MAT" + m, nota));
            }
        }

        // Verificar similitud para el estudiante 1
        if (grafo.getNumRegistros(1) >= 1) {
            GradeSimilarityStrategy strategy = new GradeSimilarityStrategy(grafo);
            Map<Long, Double> similitudes = strategy.calcularSimilitudes(1L);

            for (Map.Entry<Long, Double> entry : similitudes.entrySet()) {
                assertThat(entry.getValue())
                        .as("Similitud con estudiante %d", entry.getKey())
                        .isBetween(0.0, 1.0);
            }
        }
    }

    // ========== Propiedad 2: Similitud es simétrica — sim(A,B) == sim(B,A) ==========

    @Property(tries = 100, seed = "67890")
    void similitudCoseno_esSimetrica(
            @ForAll @IntRange(min = 2, max = 15) int numEstudiantes,
            @ForAll @IntRange(min = 2, max = 8) int numMaterias
    ) {
        GrafoBipartito grafo = new GrafoBipartito(numEstudiantes);
        Random rng = new Random(99L);

        for (int e = 1; e <= numEstudiantes; e++) {
            int materiasACursar = 1 + rng.nextInt(Math.min(numMaterias, 4));
            Set<Integer> materiasElegidas = new HashSet<>();
            while (materiasElegidas.size() < materiasACursar) {
                materiasElegidas.add(1 + rng.nextInt(numMaterias));
            }
            for (int m : materiasElegidas) {
                double nota = Math.round(rng.nextDouble() * 5.0 * 100.0) / 100.0;
                nota = Math.min(5.0, Math.max(0.0, nota));
                grafo.agregarRegistro(new RegistroAcademico(e, "MAT" + m, nota));
            }
        }

        GradeSimilarityStrategy strategy = new GradeSimilarityStrategy(grafo);

        // Verificar simetría para todos los pares válidos
        for (int a = 1; a <= numEstudiantes; a++) {
            if (grafo.getNumRegistros(a) < 1) continue;
            Map<Long, Double> simA = strategy.calcularSimilitudes(a);

            for (int b = a + 1; b <= numEstudiantes; b++) {
                if (grafo.getNumRegistros(b) < 1) continue;
                Map<Long, Double> simB = strategy.calcularSimilitudes(b);

                double simAB = simA.getOrDefault((long) b, 0.0);
                double simBA = simB.getOrDefault((long) a, 0.0);

                assertThat(simAB)
                        .as("sim(%d,%d) == sim(%d,%d)", a, b, b, a)
                        .isCloseTo(simBA, within(1e-10));
            }
        }
    }

    // ========== Propiedad 3: Análisis de riesgo es idempotente (Post5) ==========

    @Property(tries = 100, seed = "11111")
    void analizarRiesgo_esIdempotente(
            @ForAll @IntRange(min = 3, max = 10) int numEstudiantes
    ) {
        RiesgoAcademicoService service = new RiesgoAcademicoService(numEstudiantes);
        Random rng = new Random(42L);

        // Registrar materias fijas
        String[] codigos = {"CAL1", "FIS1", "PROG", "ALG", "CAL2"};
        String[] nombres = {"Cálculo I", "Física I", "Programación", "Álgebra", "Cálculo II"};
        for (int i = 0; i < codigos.length; i++) {
            service.registrarMateria(new Materia(codigos[i], nombres[i]));
        }

        // Registrar estudiantes y notas aleatorias
        for (int e = 1; e <= numEstudiantes; e++) {
            service.registrarEstudiante(new Estudiante(e, "Est" + e));
            int materias = 2 + rng.nextInt(3);
            Set<Integer> elegidas = new HashSet<>();
            while (elegidas.size() < materias) {
                elegidas.add(rng.nextInt(codigos.length));
            }
            for (int idx : elegidas) {
                double nota = Math.round(rng.nextDouble() * 5.0 * 100.0) / 100.0;
                nota = Math.min(5.0, Math.max(0.0, nota));
                service.agregarRegistroAcademico(new RegistroAcademico(e, codigos[idx], nota));
            }
        }

        // Ejecutar dos veces para el mismo estudiante
        ResultadoRiesgo r1 = service.analizarRiesgo(1L, 0.3, 3.0);
        ResultadoRiesgo r2 = service.analizarRiesgo(1L, 0.3, 3.0);

        // Post5: mismas entradas → mismo resultado
        assertThat(r1.materiasEnRiesgo()).isEqualTo(r2.materiasEnRiesgo());
        assertThat(r1.gemelosIdentificados()).isEqualTo(r2.gemelosIdentificados());
        assertThat(r1.estrategiaUsada()).isEqualTo(r2.estrategiaUsada());
    }

    // ========== Propiedad 4: Nivel de riesgo siempre en [0, 1] ==========

    @Property(tries = 150, seed = "22222")
    void nivelRiesgo_siempreEnRangoCeroUno(
            @ForAll @IntRange(min = 3, max = 12) int numEstudiantes
    ) {
        RiesgoAcademicoService service = new RiesgoAcademicoService(numEstudiantes);
        Random rng = new Random(77L);

        String[] codigos = {"CAL1", "FIS1", "PROG", "ALG", "CAL2"};
        String[] nombres = {"Cálculo I", "Física I", "Programación", "Álgebra", "Cálculo II"};
        for (int i = 0; i < codigos.length; i++) {
            service.registrarMateria(new Materia(codigos[i], nombres[i]));
        }

        for (int e = 1; e <= numEstudiantes; e++) {
            service.registrarEstudiante(new Estudiante(e, "Est" + e));
            int materias = 2 + rng.nextInt(3);
            Set<Integer> elegidas = new HashSet<>();
            while (elegidas.size() < materias) {
                elegidas.add(rng.nextInt(codigos.length));
            }
            for (int idx : elegidas) {
                double nota = Math.round(rng.nextDouble() * 5.0 * 100.0) / 100.0;
                nota = Math.min(5.0, Math.max(0.0, nota));
                service.agregarRegistroAcademico(new RegistroAcademico(e, codigos[idx], nota));
            }
        }

        ResultadoRiesgo resultado = service.analizarRiesgo(1L, 0.1, 3.0);

        for (MateriaEnRiesgo materia : resultado.materiasEnRiesgo()) {
            assertThat(materia.nivelRiesgo())
                    .as("Nivel de riesgo para %s", materia.codigoMateria())
                    .isBetween(0.0, 1.0);
        }
    }

    // ========== Propiedad 5: Norma del grafo siempre >= 0 ==========

    @Property(tries = 200, seed = "33333")
    void normaDelGrafo_siempreNoNegativa(
            @ForAll @IntRange(min = 1, max = 20) int numEstudiantes,
            @ForAll @IntRange(min = 1, max = 10) int numMaterias
    ) {
        GrafoBipartito grafo = new GrafoBipartito(numEstudiantes);
        Random rng = new Random(55L);

        for (int e = 1; e <= numEstudiantes; e++) {
            int materias = 1 + rng.nextInt(Math.min(numMaterias, 5));
            for (int m = 1; m <= materias; m++) {
                double nota = Math.round(rng.nextDouble() * 5.0 * 100.0) / 100.0;
                nota = Math.min(5.0, Math.max(0.0, nota));
                grafo.agregarRegistro(new RegistroAcademico(e, "M" + m, nota));
            }
        }

        for (int e = 1; e <= numEstudiantes; e++) {
            assertThat(grafo.getNorma(e))
                    .as("Norma del estudiante %d", e)
                    .isGreaterThanOrEqualTo(0.0);
        }
    }
}
