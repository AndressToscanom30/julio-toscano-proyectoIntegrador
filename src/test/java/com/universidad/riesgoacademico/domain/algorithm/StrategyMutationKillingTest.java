package com.universidad.riesgoacademico.domain.algorithm;

import com.universidad.riesgoacademico.domain.model.*;
import com.universidad.riesgoacademico.domain.model.PerfilAdmision.TipoColegio;
import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo.EstrategiaSimilitud;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas dirigidas para matar mutantes sobrevivientes de PIT.
 *
 * <p>Cada test está diseñado para detectar un tipo específico de mutante:
 * AOR (cambio de operador aritmético), ROR (cambio de relacional),
 * COR (cambio de condicional), etc.
 */
@DisplayName("Mutation Killing Tests — Estrategias de Similitud")
class StrategyMutationKillingTest {

    // ==================== GradeSimilarityStrategy ====================

    @Nested
    @DisplayName("GradeSimilarityStrategy — Matar mutantes de producto punto y norma")
    class GradeSimilarityTests {

        private GrafoBipartito grafo;

        @BeforeEach
        void setUp() {
            grafo = new GrafoBipartito(10);
        }

        @Test
        @DisplayName("Similitud coseno exacta para vectores conocidos — mata AOR en producto punto")
        void similitudExacta_vectoresConocidos() {
            // Estudiante 1: (3.0, 4.0) — norma = 5.0
            grafo.agregarRegistro(new RegistroAcademico(1L, "MAT", 3.0));
            grafo.agregarRegistro(new RegistroAcademico(1L, "FIS", 4.0));
            // Estudiante 2: (3.0, 4.0) — idéntico
            grafo.agregarRegistro(new RegistroAcademico(2L, "MAT", 3.0));
            grafo.agregarRegistro(new RegistroAcademico(2L, "FIS", 4.0));

            GradeSimilarityStrategy strategy = new GradeSimilarityStrategy(grafo);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            assertThat(sim).containsKey(2L);
            // cos(identical) = 1.0
            assertThat(sim.get(2L)).isCloseTo(1.0, within(1e-10));
        }

        @Test
        @DisplayName("Similitud con vectores ortogonales = 0 — mata ROR en filtro > 0")
        void similitudCero_vectoresOrtogonales() {
            // Estudiante 1: solo MAT
            grafo.agregarRegistro(new RegistroAcademico(1L, "MAT", 4.0));
            // Estudiante 2: solo FIS (no comparten materia)
            grafo.agregarRegistro(new RegistroAcademico(2L, "FIS", 4.0));

            GradeSimilarityStrategy strategy = new GradeSimilarityStrategy(grafo);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            // No debe aparecer el estudiante 2 (similitud = 0)
            assertThat(sim).doesNotContainKey(2L);
        }

        @Test
        @DisplayName("Similitud parcial con valores exactos — mata AOR en merge/sum")
        void similitudParcial_valoresExactos() {
            // E1: MAT=4, FIS=3, PROG=0 (no cursó)
            grafo.agregarRegistro(new RegistroAcademico(1L, "MAT", 4.0));
            grafo.agregarRegistro(new RegistroAcademico(1L, "FIS", 3.0));
            // E2: MAT=2, FIS=4
            grafo.agregarRegistro(new RegistroAcademico(2L, "MAT", 2.0));
            grafo.agregarRegistro(new RegistroAcademico(2L, "FIS", 4.0));

            // dot = 4*2 + 3*4 = 8 + 12 = 20
            // normE1 = sqrt(16+9) = 5
            // normE2 = sqrt(4+16) = sqrt(20) = 4.472...
            // cos = 20 / (5 * 4.472) = 20 / 22.36 = 0.8944...

            GradeSimilarityStrategy strategy = new GradeSimilarityStrategy(grafo);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            assertThat(sim.get(2L)).isCloseTo(20.0 / (5.0 * Math.sqrt(20.0)), within(1e-10));
        }

        @Test
        @DisplayName("Estudiante sin registros lanza excepción — mata RemoveConditional")
        void sinRegistros_lanzaExcepcion() {
            GradeSimilarityStrategy strategy = new GradeSimilarityStrategy(grafo);
            assertThatThrownBy(() -> strategy.calcularSimilitudes(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Norma cero retorna mapa vacío — mata NullReturn en empty check")
        void normaCero_retornaMapaVacio() {
            // Nota 0.0 tiene norma 0
            grafo.agregarRegistro(new RegistroAcademico(1L, "MAT", 0.0));
            grafo.agregarRegistro(new RegistroAcademico(2L, "MAT", 4.0));

            GradeSimilarityStrategy strategy = new GradeSimilarityStrategy(grafo);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            assertThat(sim).isEmpty();
        }

        @Test
        @DisplayName("Múltiples co-estudiantes con similitudes distintas — mata Math en merge")
        void multiplesCoEstudiantes_similitudesDistintas() {
            grafo.agregarRegistro(new RegistroAcademico(1L, "MAT", 4.0));
            grafo.agregarRegistro(new RegistroAcademico(1L, "FIS", 3.0));
            grafo.agregarRegistro(new RegistroAcademico(2L, "MAT", 4.0));
            grafo.agregarRegistro(new RegistroAcademico(2L, "FIS", 3.0));
            grafo.agregarRegistro(new RegistroAcademico(3L, "MAT", 1.0));
            grafo.agregarRegistro(new RegistroAcademico(3L, "FIS", 5.0));

            GradeSimilarityStrategy strategy = new GradeSimilarityStrategy(grafo);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            assertThat(sim).containsKey(2L);
            assertThat(sim).containsKey(3L);
            // E1 y E2 son idénticos
            assertThat(sim.get(2L)).isCloseTo(1.0, within(1e-10));
            // E1=(4,3) E3=(1,5): dot=4+15=19, n1=5, n3=sqrt(26)
            assertThat(sim.get(3L)).isCloseTo(19.0 / (5.0 * Math.sqrt(26.0)), within(1e-10));
            // Verify they are different (kills equality mutation)
            assertThat(sim.get(2L)).isNotEqualTo(sim.get(3L));
        }

        @Test
        @DisplayName("getTipo retorna GRADE_BASED — mata return value mutant")
        void getTipo_retornaGradeBased() {
            GradeSimilarityStrategy strategy = new GradeSimilarityStrategy(grafo);
            assertThat(strategy.getTipo()).isEqualTo(EstrategiaSimilitud.GRADE_BASED);
        }
    }

    // ==================== ICFESSimilarityStrategy ====================

    @Nested
    @DisplayName("ICFESSimilarityStrategy — Matar mutantes de coseno ICFES")
    class ICFESSimilarityTests {

        @Test
        @DisplayName("Similitud coseno ICFES exacta — mata AOR en producto punto 5D")
        void similitudIcfesExacta() {
            Map<Long, Estudiante> estudiantes = new HashMap<>();
            PuntajeICFES icfes1 = new PuntajeICFES(80, 70, 60, 50, 40);
            PuntajeICFES icfes2 = new PuntajeICFES(80, 70, 60, 50, 40);
            estudiantes.put(1L, new Estudiante(1, "E1", icfes1, null));
            estudiantes.put(2L, new Estudiante(2, "E2", icfes2, null));

            ICFESSimilarityStrategy strategy = new ICFESSimilarityStrategy(estudiantes);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            assertThat(sim.get(2L)).isCloseTo(1.0, within(1e-10));
        }

        @Test
        @DisplayName("ICFES con valores diferentes — verifica valor exacto del coseno")
        void icfesDiferentes_valorExacto() {
            Map<Long, Estudiante> estudiantes = new HashMap<>();
            PuntajeICFES icfes1 = new PuntajeICFES(100, 0, 0, 0, 0);
            PuntajeICFES icfes2 = new PuntajeICFES(0, 100, 0, 0, 0);
            estudiantes.put(1L, new Estudiante(1, "E1", icfes1, null));
            estudiantes.put(2L, new Estudiante(2, "E2", icfes2, null));

            ICFESSimilarityStrategy strategy = new ICFESSimilarityStrategy(estudiantes);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            // Ortogonales → similitud 0 → no aparece en el mapa
            assertThat(sim).doesNotContainKey(2L);
        }

        @Test
        @DisplayName("Estudiante sin ICFES lanza excepción")
        void sinIcfes_lanzaExcepcion() {
            Map<Long, Estudiante> estudiantes = new HashMap<>();
            estudiantes.put(1L, new Estudiante(1, "E1", null, null));

            ICFESSimilarityStrategy strategy = new ICFESSimilarityStrategy(estudiantes);
            assertThatThrownBy(() -> strategy.calcularSimilitudes(1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Estudiante no existente lanza excepción")
        void noExistente_lanzaExcepcion() {
            Map<Long, Estudiante> estudiantes = new HashMap<>();
            ICFESSimilarityStrategy strategy = new ICFESSimilarityStrategy(estudiantes);
            assertThatThrownBy(() -> strategy.calcularSimilitudes(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Solo cuenta co-estudiantes con ICFES válido — mata conditional")
        void soloConIcfesValido() {
            Map<Long, Estudiante> estudiantes = new HashMap<>();
            PuntajeICFES icfes1 = new PuntajeICFES(50, 50, 50, 50, 50);
            estudiantes.put(1L, new Estudiante(1, "E1", icfes1, null));
            estudiantes.put(2L, new Estudiante(2, "E2", null, null)); // sin ICFES
            estudiantes.put(3L, new Estudiante(3, "E3", icfes1, null));

            ICFESSimilarityStrategy strategy = new ICFESSimilarityStrategy(estudiantes);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            assertThat(sim).doesNotContainKey(2L);
            assertThat(sim).containsKey(3L);
        }

        @Test
        @DisplayName("getTipo retorna ICFES_BASED")
        void getTipo_retornaIcfesBased() {
            ICFESSimilarityStrategy strategy = new ICFESSimilarityStrategy(new HashMap<>());
            assertThat(strategy.getTipo()).isEqualTo(EstrategiaSimilitud.ICFES_BASED);
        }
    }

    // ==================== HybridSimilarityStrategy ====================

    @Nested
    @DisplayName("HybridSimilarityStrategy — Matar mutantes en ponderación 70/30")
    class HybridSimilarityTests {

        @Test
        @DisplayName("Ponderación híbrida exacta — mata AOR en PESO_NOTAS * componenteNotas")
        void ponderacionExacta() {
            GrafoBipartito grafo = new GrafoBipartito(10);
            Map<Long, Estudiante> estudiantes = new HashMap<>();

            PuntajeICFES icfes1 = new PuntajeICFES(80, 70, 60, 50, 40);
            PuntajeICFES icfes2 = new PuntajeICFES(80, 70, 60, 50, 40);
            estudiantes.put(1L, new Estudiante(1, "E1", icfes1, null));
            estudiantes.put(2L, new Estudiante(2, "E2", icfes2, null));

            grafo.agregarRegistro(new RegistroAcademico(1L, "MAT", 4.0));
            grafo.agregarRegistro(new RegistroAcademico(1L, "FIS", 3.0));
            grafo.agregarRegistro(new RegistroAcademico(2L, "MAT", 4.0));
            grafo.agregarRegistro(new RegistroAcademico(2L, "FIS", 3.0));

            GradeSimilarityStrategy gradeStrategy = new GradeSimilarityStrategy(grafo);
            ICFESSimilarityStrategy icfesStrategy = new ICFESSimilarityStrategy(estudiantes);
            HybridSimilarityStrategy hybrid = new HybridSimilarityStrategy(gradeStrategy, icfesStrategy);

            Map<Long, Double> sim = hybrid.calcularSimilitudes(1L);

            // Both components = 1.0, so hybrid = 0.7*1.0 + 0.3*1.0 = 1.0
            assertThat(sim.get(2L)).isCloseTo(1.0, within(1e-10));
        }

        @Test
        @DisplayName("Co-estudiante solo en grades — componente ICFES es 0")
        void soloEnGrades_icfesCero() {
            GrafoBipartito grafo = new GrafoBipartito(10);
            Map<Long, Estudiante> estudiantes = new HashMap<>();

            PuntajeICFES icfes1 = new PuntajeICFES(80, 70, 60, 50, 40);
            estudiantes.put(1L, new Estudiante(1, "E1", icfes1, null));
            estudiantes.put(2L, new Estudiante(2, "E2", null, null)); // sin ICFES

            grafo.agregarRegistro(new RegistroAcademico(1L, "MAT", 4.0));
            grafo.agregarRegistro(new RegistroAcademico(2L, "MAT", 4.0));

            GradeSimilarityStrategy gradeStrategy = new GradeSimilarityStrategy(grafo);
            ICFESSimilarityStrategy icfesStrategy = new ICFESSimilarityStrategy(estudiantes);
            HybridSimilarityStrategy hybrid = new HybridSimilarityStrategy(gradeStrategy, icfesStrategy);

            Map<Long, Double> sim = hybrid.calcularSimilitudes(1L);

            // grade=1.0, icfes=0.0 → hybrid = 0.7*1.0 + 0.3*0.0 = 0.7
            assertThat(sim.get(2L)).isCloseTo(0.7, within(1e-10));
        }

        @Test
        @DisplayName("getTipo retorna HYBRID")
        void getTipo_retornaHybrid() {
            GrafoBipartito grafo = new GrafoBipartito(10);
            GradeSimilarityStrategy g = new GradeSimilarityStrategy(grafo);
            ICFESSimilarityStrategy i = new ICFESSimilarityStrategy(new HashMap<>());
            HybridSimilarityStrategy hybrid = new HybridSimilarityStrategy(g, i);
            assertThat(hybrid.getTipo()).isEqualTo(EstrategiaSimilitud.HYBRID);
        }
    }

    // ==================== DemographicFallbackStrategy ====================

    @Nested
    @DisplayName("DemographicFallbackStrategy — Matar mutantes en matching demográfico")
    class DemographicFallbackTests {

        @Test
        @DisplayName("Mismo grupo demográfico → similitud fija 0.3 — mata Math en SIMILITUD_DEMOGRAFICA")
        void mismoGrupo_similitudFija() {
            Map<Long, Estudiante> estudiantes = new HashMap<>();
            PerfilAdmision perfil1 = new PerfilAdmision("ING_SISTEMAS", TipoColegio.PUBLICO, "VE");
            PerfilAdmision perfil2 = new PerfilAdmision("ING_SISTEMAS", TipoColegio.PUBLICO, "VE");
            estudiantes.put(1L, new Estudiante(1, "E1", null, perfil1));
            estudiantes.put(2L, new Estudiante(2, "E2", null, perfil2));

            DemographicFallbackStrategy strategy = new DemographicFallbackStrategy(estudiantes);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            assertThat(sim).containsEntry(2L, 0.3);
        }

        @Test
        @DisplayName("Diferente programa → no son similares — mata COR en coincideGrupo")
        void diferentePrograma_noSimilares() {
            Map<Long, Estudiante> estudiantes = new HashMap<>();
            PerfilAdmision perfil1 = new PerfilAdmision("ING_SISTEMAS", TipoColegio.PUBLICO, "VE");
            PerfilAdmision perfil2 = new PerfilAdmision("MEDICINA", TipoColegio.PUBLICO, "VE");
            estudiantes.put(1L, new Estudiante(1, "E1", null, perfil1));
            estudiantes.put(2L, new Estudiante(2, "E2", null, perfil2));

            DemographicFallbackStrategy strategy = new DemographicFallbackStrategy(estudiantes);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            assertThat(sim).doesNotContainKey(2L);
        }

        @Test
        @DisplayName("Diferente tipo colegio → no son similares — mata COR en tipoColegio check")
        void diferenteTipoColegio_noSimilares() {
            Map<Long, Estudiante> estudiantes = new HashMap<>();
            PerfilAdmision perfil1 = new PerfilAdmision("ING_SISTEMAS", TipoColegio.PUBLICO, "VE");
            PerfilAdmision perfil2 = new PerfilAdmision("ING_SISTEMAS", TipoColegio.PRIVADO, "VE");
            estudiantes.put(1L, new Estudiante(1, "E1", null, perfil1));
            estudiantes.put(2L, new Estudiante(2, "E2", null, perfil2));

            DemographicFallbackStrategy strategy = new DemographicFallbackStrategy(estudiantes);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            assertThat(sim).doesNotContainKey(2L);
        }

        @Test
        @DisplayName("Estudiante sin perfil → mapa vacío — mata NullReturn")
        void sinPerfil_mapaVacio() {
            Map<Long, Estudiante> estudiantes = new HashMap<>();
            estudiantes.put(1L, new Estudiante(1, "E1", null, null));

            DemographicFallbackStrategy strategy = new DemographicFallbackStrategy(estudiantes);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            assertThat(sim).isEmpty();
        }

        @Test
        @DisplayName("Co-estudiante sin perfil no incluido — mata conditional en perfilCo != null")
        void coEstudianteSinPerfil_noIncluido() {
            Map<Long, Estudiante> estudiantes = new HashMap<>();
            PerfilAdmision perfil1 = new PerfilAdmision("ING_SISTEMAS", TipoColegio.PUBLICO, "VE");
            estudiantes.put(1L, new Estudiante(1, "E1", null, perfil1));
            estudiantes.put(2L, new Estudiante(2, "E2", null, null)); // sin perfil

            DemographicFallbackStrategy strategy = new DemographicFallbackStrategy(estudiantes);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            assertThat(sim).doesNotContainKey(2L);
        }

        @Test
        @DisplayName("No se incluye a sí mismo — mata conditional coId == estudianteId")
        void noSeIncluyeASiMismo() {
            Map<Long, Estudiante> estudiantes = new HashMap<>();
            PerfilAdmision perfil = new PerfilAdmision("ING_SISTEMAS", TipoColegio.PUBLICO, "VE");
            estudiantes.put(1L, new Estudiante(1, "E1", null, perfil));

            DemographicFallbackStrategy strategy = new DemographicFallbackStrategy(estudiantes);
            Map<Long, Double> sim = strategy.calcularSimilitudes(1L);

            assertThat(sim).doesNotContainKey(1L);
        }

        @Test
        @DisplayName("Estudiante no encontrado lanza excepción")
        void noEncontrado_lanzaExcepcion() {
            DemographicFallbackStrategy strategy = new DemographicFallbackStrategy(new HashMap<>());
            assertThatThrownBy(() -> strategy.calcularSimilitudes(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getTipo retorna DEMOGRAPHIC_FALLBACK")
        void getTipo_retornaDemographic() {
            DemographicFallbackStrategy strategy = new DemographicFallbackStrategy(new HashMap<>());
            assertThat(strategy.getTipo()).isEqualTo(EstrategiaSimilitud.DEMOGRAPHIC_FALLBACK);
        }
    }

    // ==================== GrafoBipartito — Tests adicionales para matar mutantes ====================

    @Nested
    @DisplayName("GrafoBipartito — Tests adicionales para PIT")
    class GrafoBipartitoAdditionalTests {

        @Test
        @DisplayName("getNumRegistros devuelve valor exacto — mata Math en size()")
        void getNumRegistros_valorExacto() {
            GrafoBipartito grafo = new GrafoBipartito(10);
            grafo.agregarRegistro(new RegistroAcademico(1L, "MAT", 3.0));
            grafo.agregarRegistro(new RegistroAcademico(1L, "FIS", 4.0));
            grafo.agregarRegistro(new RegistroAcademico(1L, "PROG", 5.0));

            assertThat(grafo.getNumRegistros(1L)).isEqualTo(3);
            assertThat(grafo.getNumRegistros(999L)).isEqualTo(0);
        }

        @Test
        @DisplayName("getEstudianteIds y getMateriaIds retornan conjuntos correctos")
        void conjuntosDeIds() {
            GrafoBipartito grafo = new GrafoBipartito(10);
            grafo.agregarRegistro(new RegistroAcademico(1L, "MAT", 3.0));
            grafo.agregarRegistro(new RegistroAcademico(2L, "FIS", 4.0));

            assertThat(grafo.getEstudianteIds()).containsExactlyInAnyOrder(1L, 2L);
            assertThat(grafo.getMateriaIds()).containsExactlyInAnyOrder("MAT", "FIS");
        }

        @Test
        @DisplayName("Constructor por defecto funciona — mata RemoveConditional en default capacity")
        void constructorPorDefecto() {
            GrafoBipartito grafo = new GrafoBipartito();
            assertThat(grafo.getNumEstudiantes()).isZero();
            grafo.agregarRegistro(new RegistroAcademico(1L, "MAT", 3.0));
            assertThat(grafo.getNumEstudiantes()).isEqualTo(1);
        }

        @Test
        @DisplayName("cargarRegistros con null lanza NullPointerException")
        void cargarRegistrosNull_lanzaExcepcion() {
            GrafoBipartito grafo = new GrafoBipartito();
            assertThatThrownBy(() -> grafo.cargarRegistros(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
