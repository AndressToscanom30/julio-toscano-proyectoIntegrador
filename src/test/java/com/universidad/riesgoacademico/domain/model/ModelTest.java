package com.universidad.riesgoacademico.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas unitarias para los records del modelo de dominio.
 * Cubre: Materia, RegistroAcademico, GradeEntry, PuntajeICFES,
 * PerfilAdmision, Estudiante, MateriaEnRiesgo, ResultadoRiesgo.
 */
@DisplayName("Modelo de Dominio — Records y Validaciones")
class ModelTest {

    // ========== Materia ==========

    @Test
    @DisplayName("Materia: crear con datos válidos")
    void materia_creaCorrectamente_conDatosValidos() {
        Materia m = new Materia("CAL2", "Cálculo II");
        assertThat(m.codigo()).isEqualTo("CAL2");
        assertThat(m.nombre()).isEqualTo("Cálculo II");
    }

    @Test
    @DisplayName("Materia: código nulo lanza excepción")
    void materia_lanzaExcepcion_codigoNulo() {
        assertThatThrownBy(() -> new Materia(null, "Cálculo"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Materia: código vacío lanza excepción")
    void materia_lanzaExcepcion_codigoVacio() {
        assertThatThrownBy(() -> new Materia("  ", "Cálculo"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Materia: nombre nulo lanza excepción")
    void materia_lanzaExcepcion_nombreNulo() {
        assertThatThrownBy(() -> new Materia("CAL2", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== RegistroAcademico ==========

    @Test
    @DisplayName("RegistroAcademico: crear con datos válidos")
    void registro_creaCorrectamente_conDatosValidos() {
        RegistroAcademico r = new RegistroAcademico(1L, "CAL2", 4.5);
        assertThat(r.estudianteId()).isEqualTo(1L);
        assertThat(r.materiaId()).isEqualTo("CAL2");
        assertThat(r.nota()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("RegistroAcademico: nota negativa lanza excepción (Pre5)")
    void registro_lanzaExcepcion_notaNegativa() {
        assertThatThrownBy(() -> new RegistroAcademico(1L, "CAL2", -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RegistroAcademico: nota superior a 5.0 lanza excepción (Pre5)")
    void registro_lanzaExcepcion_notaSuperiorAlMaximo() {
        assertThatThrownBy(() -> new RegistroAcademico(1L, "CAL2", 5.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RegistroAcademico: estudianteId = 0 lanza excepción")
    void registro_lanzaExcepcion_estudianteIdCero() {
        assertThatThrownBy(() -> new RegistroAcademico(0L, "CAL2", 3.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RegistroAcademico: nota en límite inferior (0.0) es válida")
    void registro_aceptaNotaCero() {
        RegistroAcademico r = new RegistroAcademico(1L, "CAL2", 0.0);
        assertThat(r.nota()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("RegistroAcademico: nota en límite superior (5.0) es válida")
    void registro_aceptaNotaMaxima() {
        RegistroAcademico r = new RegistroAcademico(1L, "CAL2", 5.0);
        assertThat(r.nota()).isEqualTo(5.0);
    }

    // ========== PuntajeICFES ==========

    @Test
    @DisplayName("PuntajeICFES: crear con datos válidos")
    void icfes_creaCorrectamente() {
        PuntajeICFES p = new PuntajeICFES(80, 75, 70, 65, 90);
        assertThat(p.matematicas()).isEqualTo(80);
        assertThat(p.ingles()).isEqualTo(90);
    }

    @Test
    @DisplayName("PuntajeICFES: componente negativo lanza excepción (Pre7)")
    void icfes_lanzaExcepcion_componenteNegativo() {
        assertThatThrownBy(() -> new PuntajeICFES(-1, 75, 70, 65, 90))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PuntajeICFES: componente > 100 lanza excepción (Pre7)")
    void icfes_lanzaExcepcion_componenteSuperiorAlMaximo() {
        assertThatThrownBy(() -> new PuntajeICFES(80, 101, 70, 65, 90))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PuntajeICFES: vector cero detectado (CB-13)")
    void icfes_detectaVectorCero() {
        PuntajeICFES cero = new PuntajeICFES(0, 0, 0, 0, 0);
        assertThat(cero.esVectorCero()).isTrue();

        PuntajeICFES noCero = new PuntajeICFES(1, 0, 0, 0, 0);
        assertThat(noCero.esVectorCero()).isFalse();
    }

    @Test
    @DisplayName("PuntajeICFES: toVector retorna 5 componentes correctos")
    void icfes_toVectorRetornaCincoComponentes() {
        PuntajeICFES p = new PuntajeICFES(80, 75, 70, 65, 90);
        double[] vector = p.toVector();
        assertThat(vector).hasSize(5);
        assertThat(vector).containsExactly(80.0, 75.0, 70.0, 65.0, 90.0);
    }

    @Test
    @DisplayName("PuntajeICFES: norma calculada correctamente")
    void icfes_normaCalculadaCorrectamente() {
        PuntajeICFES p = new PuntajeICFES(3, 4, 0, 0, 0);
        // sqrt(9 + 16) = 5.0
        assertThat(p.norma()).isEqualTo(5.0);
    }

    // ========== PerfilAdmision ==========

    @Test
    @DisplayName("PerfilAdmision: crear con datos válidos")
    void perfilAdmision_creaCorrectamente() {
        PerfilAdmision p = new PerfilAdmision("ING_SISTEMAS",
                PerfilAdmision.TipoColegio.PUBLICO, "CO");
        assertThat(p.programaAcademico()).isEqualTo("ING_SISTEMAS");
        assertThat(p.tipoColegio()).isEqualTo(PerfilAdmision.TipoColegio.PUBLICO);
        assertThat(p.paisOrigen()).isEqualTo("CO");
    }

    @Test
    @DisplayName("PerfilAdmision: programa nulo lanza excepción")
    void perfilAdmision_lanzaExcepcion_programaNulo() {
        assertThatThrownBy(() -> new PerfilAdmision(null,
                PerfilAdmision.TipoColegio.PUBLICO, "CO"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== Estudiante ==========

    @Test
    @DisplayName("Estudiante: crear con datos mínimos")
    void estudiante_creaConDatosMinimos() {
        Estudiante e = new Estudiante(1L, "Juan Pérez");
        assertThat(e.getId()).isEqualTo(1L);
        assertThat(e.getNombre()).isEqualTo("Juan Pérez");
        assertThat(e.getIcfes()).isNull();
        assertThat(e.tieneIcfesValido()).isFalse();
    }

    @Test
    @DisplayName("Estudiante: id = 0 lanza excepción")
    void estudiante_lanzaExcepcion_idCero() {
        assertThatThrownBy(() -> new Estudiante(0L, "Juan"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Estudiante: nombre nulo lanza excepción")
    void estudiante_lanzaExcepcion_nombreNulo() {
        assertThatThrownBy(() -> new Estudiante(1L, null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, -100, Long.MIN_VALUE})
    @DisplayName("Estudiante: id negativo lanza excepción")
    void estudiante_lanzaExcepcion_idNegativo(long id) {
        assertThatThrownBy(() -> new Estudiante(id, "Juan"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Estudiante: tieneIcfesValido con ICFES vector cero retorna false (CB-13)")
    void estudiante_icfesVectorCero_retornaFalse() {
        PuntajeICFES cero = new PuntajeICFES(0, 0, 0, 0, 0);
        Estudiante e = new Estudiante(1L, "Ana", cero, null);
        assertThat(e.tieneIcfesValido()).isFalse();
    }

    @Test
    @DisplayName("Estudiante: tieneIcfesValido con ICFES válido retorna true")
    void estudiante_icfesValido_retornaTrue() {
        PuntajeICFES valido = new PuntajeICFES(80, 75, 70, 65, 90);
        Estudiante e = new Estudiante(1L, "Ana", valido, null);
        assertThat(e.tieneIcfesValido()).isTrue();
    }

    @Test
    @DisplayName("Estudiante: equals se basa en id")
    void estudiante_equalsUsaId() {
        Estudiante e1 = new Estudiante(1L, "Juan");
        Estudiante e2 = new Estudiante(1L, "Otro Nombre");
        Estudiante e3 = new Estudiante(2L, "Juan");
        assertThat(e1).isEqualTo(e2);
        assertThat(e1).isNotEqualTo(e3);
    }

    // ========== MateriaEnRiesgo ==========

    @Test
    @DisplayName("MateriaEnRiesgo: nivelRiesgo fuera de [0,1] lanza excepción")
    void materiaEnRiesgo_lanzaExcepcion_nivelFueraDeRango() {
        assertThatThrownBy(() -> new MateriaEnRiesgo("CAL2", "Cálculo II", 1.5, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("MateriaEnRiesgo: gemelosReprobaron negativo lanza excepción")
    void materiaEnRiesgo_lanzaExcepcion_gemelosNegativo() {
        assertThatThrownBy(() -> new MateriaEnRiesgo("CAL2", "Cálculo II", 0.8, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
