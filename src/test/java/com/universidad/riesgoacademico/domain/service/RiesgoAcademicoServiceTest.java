package com.universidad.riesgoacademico.domain.service;

import com.universidad.riesgoacademico.domain.model.*;
import com.universidad.riesgoacademico.domain.model.PerfilAdmision.TipoColegio;
import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo.EstrategiaSimilitud;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas unitarias para RiesgoAcademicoService.
 * Cubre: selección de estrategia, análisis de riesgo, postcondiciones, casos borde.
 */
@DisplayName("RiesgoAcademicoService — Servicio Principal")
class RiesgoAcademicoServiceTest {

    private RiesgoAcademicoService service;

    @BeforeEach
    void setUp() {
        service = new RiesgoAcademicoService(100);
        // Registrar materias comunes
        service.registrarMateria(new Materia("CAL1", "Cálculo I"));
        service.registrarMateria(new Materia("CAL2", "Cálculo II"));
        service.registrarMateria(new Materia("FIS1", "Física I"));
        service.registrarMateria(new Materia("PROG", "Programación"));
        service.registrarMateria(new Materia("ALG", "Álgebra Lineal"));
    }

    // ========== Selección de Estrategia ==========

    @Test
    @DisplayName("Selecciona GRADE_BASED cuando tiene ≥2 notas y no tiene ICFES")
    void seleccionaGradeBased_conNotasSinIcfes() {
        Estudiante e = new Estudiante(1L, "Juan");
        service.registrarEstudiante(e);
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "CAL1", 4.0));
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "FIS1", 3.5));

        SimilarityStrategy estrategia = service.seleccionarEstrategia(e);
        assertThat(estrategia.getTipo()).isEqualTo(EstrategiaSimilitud.GRADE_BASED);
    }

    @Test
    @DisplayName("Selecciona ICFES_BASED cuando tiene ICFES pero <2 notas")
    void seleccionaIcfesBased_conIcfesSinNotas() {
        PuntajeICFES icfes = new PuntajeICFES(80, 75, 70, 65, 90);
        Estudiante e = new Estudiante(1L, "Ana", icfes, null);
        service.registrarEstudiante(e);

        SimilarityStrategy estrategia = service.seleccionarEstrategia(e);
        assertThat(estrategia.getTipo()).isEqualTo(EstrategiaSimilitud.ICFES_BASED);
    }

    @Test
    @DisplayName("Selecciona HYBRID cuando tiene ≥2 notas Y tiene ICFES")
    void seleccionaHybrid_conNotasYIcfes() {
        PuntajeICFES icfes = new PuntajeICFES(80, 75, 70, 65, 90);
        Estudiante e = new Estudiante(1L, "Carlos", icfes, null);
        service.registrarEstudiante(e);
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "CAL1", 4.0));
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "FIS1", 3.5));

        SimilarityStrategy estrategia = service.seleccionarEstrategia(e);
        assertThat(estrategia.getTipo()).isEqualTo(EstrategiaSimilitud.HYBRID);
    }

    @Test
    @DisplayName("Selecciona DEMOGRAPHIC_FALLBACK sin ICFES y sin notas (CB-03)")
    void seleccionaDemographic_sinIcfesSinNotas() {
        PerfilAdmision perfil = new PerfilAdmision("ING_SISTEMAS", TipoColegio.PUBLICO, "VE");
        Estudiante e = new Estudiante(1L, "María", null, perfil);
        service.registrarEstudiante(e);

        SimilarityStrategy estrategia = service.seleccionarEstrategia(e);
        assertThat(estrategia.getTipo()).isEqualTo(EstrategiaSimilitud.DEMOGRAPHIC_FALLBACK);
    }

    // ========== Análisis de Riesgo ==========

    @Test
    @DisplayName("Análisis detecta materia en riesgo cuando gemelo reprobó (Post2)")
    void analizarRiesgo_detectaMateriaEnRiesgo() {
        // Estudiante 1: aprobó CAL1 y FIS1, no ha cursado CAL2
        Estudiante e1 = new Estudiante(1L, "Juan");
        service.registrarEstudiante(e1);
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "CAL1", 4.0));
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "FIS1", 4.5));

        // Estudiante 2 (gemelo): notas similares en CAL1 y FIS1, pero reprobó CAL2
        Estudiante e2 = new Estudiante(2L, "Pedro");
        service.registrarEstudiante(e2);
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "CAL1", 4.2));
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "FIS1", 4.3));
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "CAL2", 2.0));

        ResultadoRiesgo resultado = service.analizarRiesgo(1L, 0.5, 3.0);

        assertThat(resultado.estudianteId()).isEqualTo(1L);
        assertThat(resultado.gemelosIdentificados()).isGreaterThanOrEqualTo(1);
        assertThat(resultado.materiasEnRiesgo())
                .anyMatch(m -> m.codigoMateria().equals("CAL2"));
    }

    @Test
    @DisplayName("Análisis no incluye materia ya aprobada por el estudiante")
    void analizarRiesgo_excluyeMateriaAprobada() {
        Estudiante e1 = new Estudiante(1L, "Juan");
        service.registrarEstudiante(e1);
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "CAL1", 4.0));
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "CAL2", 4.0)); // ya aprobó

        Estudiante e2 = new Estudiante(2L, "Pedro");
        service.registrarEstudiante(e2);
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "CAL1", 4.2));
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "CAL2", 2.0));

        ResultadoRiesgo resultado = service.analizarRiesgo(1L, 0.5, 3.0);

        // CAL2 no debería estar en riesgo porque e1 ya la aprobó con 4.0
        assertThat(resultado.materiasEnRiesgo())
                .noneMatch(m -> m.codigoMateria().equals("CAL2"));
    }

    @Test
    @DisplayName("Nota igual al umbral no se considera reprobación (CB-11)")
    void analizarRiesgo_notaIgualAlUmbral_noEsReprobacion() {
        Estudiante e1 = new Estudiante(1L, "Juan");
        service.registrarEstudiante(e1);
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "CAL1", 4.0));
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "FIS1", 4.0));

        Estudiante e2 = new Estudiante(2L, "Pedro");
        service.registrarEstudiante(e2);
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "CAL1", 4.0));
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "FIS1", 4.0));
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "CAL2", 3.0)); // exactamente 3.0

        ResultadoRiesgo resultado = service.analizarRiesgo(1L, 0.5, 3.0);

        // nota = 3.0 con umbral = 3.0 → NO es reprobación (estrictamente menor)
        assertThat(resultado.materiasEnRiesgo())
                .noneMatch(m -> m.codigoMateria().equals("CAL2"));
    }

    @Test
    @DisplayName("Resultado es reproducible (Post5)")
    void analizarRiesgo_esReproducible() {
        Estudiante e1 = new Estudiante(1L, "Juan");
        service.registrarEstudiante(e1);
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "CAL1", 4.0));
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "FIS1", 4.5));

        Estudiante e2 = new Estudiante(2L, "Pedro");
        service.registrarEstudiante(e2);
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "CAL1", 4.2));
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "FIS1", 4.3));
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "CAL2", 2.0));

        ResultadoRiesgo r1 = service.analizarRiesgo(1L, 0.5, 3.0);
        ResultadoRiesgo r2 = service.analizarRiesgo(1L, 0.5, 3.0);

        assertThat(r1.materiasEnRiesgo()).isEqualTo(r2.materiasEnRiesgo());
        assertThat(r1.gemelosIdentificados()).isEqualTo(r2.gemelosIdentificados());
    }

    @Test
    @DisplayName("Sin gemelos retorna lista vacía de materias en riesgo (Post4/CB-07)")
    void analizarRiesgo_sinGemelos_listaVacia() {
        Estudiante e1 = new Estudiante(1L, "Juan");
        service.registrarEstudiante(e1);
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "CAL1", 4.0));
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "FIS1", 4.5));

        // Estudiante 2 cursa materias totalmente diferentes → similitud = 0
        Estudiante e2 = new Estudiante(2L, "Pedro");
        service.registrarEstudiante(e2);
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "PROG", 4.0));
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "ALG", 4.0));

        ResultadoRiesgo resultado = service.analizarRiesgo(1L, 0.5, 3.0);

        assertThat(resultado.materiasEnRiesgo()).isEmpty();
        assertThat(resultado.gemelosIdentificados()).isZero();
    }

    // ========== Validación de Precondiciones ==========

    @Test
    @DisplayName("analizarRiesgo con estudianteId inexistente lanza excepción (CB-12)")
    void analizarRiesgo_estudianteNoExiste_lanzaExcepcion() {
        assertThatThrownBy(() -> service.analizarRiesgo(999L, 0.5, 3.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("analizarRiesgo con umbralSimilitud = 0 lanza excepción (Pre6)")
    void analizarRiesgo_umbralCero_lanzaExcepcion() {
        service.registrarEstudiante(new Estudiante(1L, "Juan"));
        assertThatThrownBy(() -> service.analizarRiesgo(1L, 0.0, 3.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("registrarEstudiante duplicado lanza excepción")
    void registrarEstudiante_duplicado_lanzaExcepcion() {
        service.registrarEstudiante(new Estudiante(1L, "Juan"));
        assertThatThrownBy(() -> service.registrarEstudiante(new Estudiante(1L, "Otro")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("agregarRegistro con estudiante no registrado lanza excepción (Pre3)")
    void agregarRegistro_estudianteNoRegistrado_lanzaExcepcion() {
        assertThatThrownBy(() ->
                service.agregarRegistroAcademico(new RegistroAcademico(999L, "CAL1", 4.0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("agregarRegistro con materia no registrada lanza excepción (Pre3)")
    void agregarRegistro_materiaNoRegistrada_lanzaExcepcion() {
        service.registrarEstudiante(new Estudiante(1L, "Juan"));
        assertThatThrownBy(() ->
                service.agregarRegistroAcademico(new RegistroAcademico(1L, "NOEXISTE", 4.0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== Materias en riesgo ordenadas (Post3) ==========

    @Test
    @DisplayName("Materias en riesgo ordenadas de mayor a menor nivel (Post3)")
    void analizarRiesgo_materiasOrdenadas_porNivelDescendente() {
        Estudiante e1 = new Estudiante(1L, "Juan");
        service.registrarEstudiante(e1);
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "CAL1", 4.0));
        service.agregarRegistroAcademico(new RegistroAcademico(1L, "FIS1", 4.5));

        Estudiante e2 = new Estudiante(2L, "Pedro");
        service.registrarEstudiante(e2);
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "CAL1", 4.2));
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "FIS1", 4.3));
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "CAL2", 1.0)); // muy bajo
        service.agregarRegistroAcademico(new RegistroAcademico(2L, "PROG", 2.5)); // bajo

        ResultadoRiesgo resultado = service.analizarRiesgo(1L, 0.5, 3.0);

        if (resultado.materiasEnRiesgo().size() >= 2) {
            for (int i = 0; i < resultado.materiasEnRiesgo().size() - 1; i++) {
                assertThat(resultado.materiasEnRiesgo().get(i).nivelRiesgo())
                        .isGreaterThanOrEqualTo(resultado.materiasEnRiesgo().get(i + 1).nivelRiesgo());
            }
        }
    }
}
