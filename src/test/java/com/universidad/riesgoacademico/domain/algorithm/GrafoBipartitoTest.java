package com.universidad.riesgoacademico.domain.algorithm;

import com.universidad.riesgoacademico.domain.model.GradeEntry;
import com.universidad.riesgoacademico.domain.model.RegistroAcademico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas unitarias para GrafoBipartito.
 * Cubre: inserción, lookup, índice invertido, normas, casos borde.
 */
@DisplayName("GrafoBipartito — Grafo Directo + Índice Invertido")
class GrafoBipartitoTest {

    private GrafoBipartito grafo;

    @BeforeEach
    void setUp() {
        grafo = new GrafoBipartito(100);
    }

    @Test
    @DisplayName("Grafo vacío tiene 0 estudiantes, 0 materias, 0 aristas")
    void grafoVacio_sinDatos() {
        assertThat(grafo.getNumEstudiantes()).isZero();
        assertThat(grafo.getNumMaterias()).isZero();
        assertThat(grafo.getTotalAristas()).isZero();
    }

    @Test
    @DisplayName("Agregar un registro crea arista en ambas direcciones")
    void agregarRegistro_creaAristaEnAmbasDirecciones() {
        grafo.agregarRegistro(new RegistroAcademico(1L, "CAL2", 4.0));

        // Grafo directo
        Map<String, Double> notas = grafo.getNotasEstudiante(1L);
        assertThat(notas).containsEntry("CAL2", 4.0);

        // Índice invertido
        List<GradeEntry> entries = grafo.getEstudiantesPorMateria("CAL2");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).estudianteId()).isEqualTo(1L);
        assertThat(entries.get(0).nota()).isEqualTo(4.0);

        assertThat(grafo.getTotalAristas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Agregar múltiples registros de un estudiante")
    void agregarMultiplesRegistros_mismoEstudiante() {
        grafo.agregarRegistro(new RegistroAcademico(1L, "CAL2", 4.0));
        grafo.agregarRegistro(new RegistroAcademico(1L, "FIS1", 3.5));
        grafo.agregarRegistro(new RegistroAcademico(1L, "PROG", 4.8));

        assertThat(grafo.getNotasEstudiante(1L)).hasSize(3);
        assertThat(grafo.getNumEstudiantes()).isEqualTo(1);
        assertThat(grafo.getNumMaterias()).isEqualTo(3);
        assertThat(grafo.getTotalAristas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Múltiples estudiantes en la misma materia (índice invertido)")
    void multiplesEstudiantes_mismaMateria() {
        grafo.agregarRegistro(new RegistroAcademico(1L, "CAL2", 4.0));
        grafo.agregarRegistro(new RegistroAcademico(2L, "CAL2", 3.5));
        grafo.agregarRegistro(new RegistroAcademico(3L, "CAL2", 2.0));

        List<GradeEntry> entries = grafo.getEstudiantesPorMateria("CAL2");
        assertThat(entries).hasSize(3);
        assertThat(grafo.getNumMaterias()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sobrescribir nota existente actualiza ambas estructuras (Inv5)")
    void sobrescribirNota_actualizaAmbasEstructuras() {
        grafo.agregarRegistro(new RegistroAcademico(1L, "CAL2", 2.0));
        grafo.agregarRegistro(new RegistroAcademico(1L, "CAL2", 4.5));

        // Grafo directo actualizado
        assertThat(grafo.getNotasEstudiante(1L)).containsEntry("CAL2", 4.5);

        // Índice invertido actualizado (sin duplicado)
        List<GradeEntry> entries = grafo.getEstudiantesPorMateria("CAL2");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).nota()).isEqualTo(4.5);

        // No se creó arista duplicada
        assertThat(grafo.getTotalAristas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Estudiante sin registros retorna mapa vacío")
    void estudianteSinRegistros_retornaMapaVacio() {
        assertThat(grafo.getNotasEstudiante(999L)).isEmpty();
    }

    @Test
    @DisplayName("Materia sin registros retorna lista vacía")
    void materiaSinRegistros_retornaListaVacia() {
        assertThat(grafo.getEstudiantesPorMateria("NOEXISTE")).isEmpty();
    }

    @Test
    @DisplayName("Norma calculada correctamente para vector (3, 4) = 5.0")
    void norma_calculadaCorrectamente() {
        grafo.agregarRegistro(new RegistroAcademico(1L, "MAT", 3.0));
        grafo.agregarRegistro(new RegistroAcademico(1L, "FIS", 4.0));

        assertThat(grafo.getNorma(1L)).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Norma de estudiante sin registros es 0.0")
    void norma_estudianteSinRegistros_esCero() {
        assertThat(grafo.getNorma(999L)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("tieneRegistros retorna false para estudiante nuevo")
    void tieneRegistros_retornaFalse_estudianteNuevo() {
        assertThat(grafo.tieneRegistros(1L)).isFalse();
    }

    @Test
    @DisplayName("tieneRegistros retorna true después de agregar registro")
    void tieneRegistros_retornaTrue_despuesDeAgregar() {
        grafo.agregarRegistro(new RegistroAcademico(1L, "CAL2", 4.0));
        assertThat(grafo.tieneRegistros(1L)).isTrue();
    }

    @Test
    @DisplayName("agregarRegistro con null lanza NullPointerException")
    void agregarRegistro_null_lanzaExcepcion() {
        assertThatThrownBy(() -> grafo.agregarRegistro(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Norma se invalida al agregar nuevo registro")
    void norma_seInvalidaAlAgregarRegistro() {
        grafo.agregarRegistro(new RegistroAcademico(1L, "MAT", 3.0));
        double norma1 = grafo.getNorma(1L); // 3.0

        grafo.agregarRegistro(new RegistroAcademico(1L, "FIS", 4.0));
        double norma2 = grafo.getNorma(1L); // 5.0

        assertThat(norma2).isNotEqualTo(norma1);
        assertThat(norma2).isEqualTo(5.0);
    }

    @Test
    @DisplayName("cargarRegistros en lote funciona correctamente")
    void cargarRegistros_enLote() {
        List<RegistroAcademico> registros = List.of(
                new RegistroAcademico(1L, "CAL2", 4.0),
                new RegistroAcademico(2L, "CAL2", 3.5),
                new RegistroAcademico(1L, "FIS1", 4.5)
        );
        grafo.cargarRegistros(registros);

        assertThat(grafo.getTotalAristas()).isEqualTo(3);
        assertThat(grafo.getNumEstudiantes()).isEqualTo(2);
    }
}
