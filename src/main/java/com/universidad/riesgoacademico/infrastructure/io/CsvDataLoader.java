package com.universidad.riesgoacademico.infrastructure.io;

import com.universidad.riesgoacademico.domain.model.*;
import com.universidad.riesgoacademico.domain.model.PerfilAdmision.TipoColegio;
import com.universidad.riesgoacademico.domain.service.RiesgoAcademicoService;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Carga datos de archivos CSV al servicio de riesgo académico.
 *
 * <p>Formato esperado de los archivos (primera línea = encabezado, separador = coma):
 * <ul>
 *   <li>{@code estudiantes.csv}: id, nombre, programaAcademico, tipoColegio, paisOrigen,
 *       icfesMatematicas, icfesLectura, icfesCiencias, icfesSociales, icfesIngles</li>
 *   <li>{@code materias.csv}: codigo, nombre</li>
 *   <li>{@code registros.csv}: estudianteId, materiaId, nota</li>
 * </ul>
 *
 * <p>Complejidad: O(|E| + |M| + |R|) donde E = estudiantes, M = materias, R = registros.
 *
 * <p>Dependencia: solo usa interfaces del dominio ({@link RiesgoAcademicoService}).
 * No importa clases de {@code domain.algorithm} (ADR-003).
 */
public final class CsvDataLoader implements DataLoader {

    /** Separador CSV por defecto. */
    private static final String SEPARADOR = ",";

    /** Índice de columna para campos ICFES vacíos. */
    private static final int COLUMNAS_ESTUDIANTE_MINIMAS = 5;

    /** Número de columnas cuando incluye ICFES. */
    private static final int COLUMNAS_ESTUDIANTE_CON_ICFES = 10;

    private final Path archivoEstudiantes;
    private final Path archivoMaterias;
    private final Path archivoRegistros;

    /**
     * Crea un cargador CSV con las rutas de los tres archivos de datos.
     *
     * @param archivoEstudiantes ruta al archivo de estudiantes (no nula)
     * @param archivoMaterias    ruta al archivo de materias (no nula)
     * @param archivoRegistros   ruta al archivo de registros académicos (no nula)
     */
    public CsvDataLoader(Path archivoEstudiantes, Path archivoMaterias, Path archivoRegistros) {
        this.archivoEstudiantes = Objects.requireNonNull(archivoEstudiantes,
                "La ruta de estudiantes no puede ser nula");
        this.archivoMaterias = Objects.requireNonNull(archivoMaterias,
                "La ruta de materias no puede ser nula");
        this.archivoRegistros = Objects.requireNonNull(archivoRegistros,
                "La ruta de registros no puede ser nula");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Orden de carga: materias → estudiantes → registros académicos.
     * Los registros se cargan al final para garantizar que las entidades
     * referenciadas (Pre3) ya existan en el servicio.
     */
    @Override
    public void cargarDatos(RiesgoAcademicoService servicio) throws IOException {
        Objects.requireNonNull(servicio, "El servicio no puede ser nulo");
        cargarMaterias(servicio);
        cargarEstudiantes(servicio);
        cargarRegistros(servicio);
    }

    private void cargarMaterias(RiesgoAcademicoService servicio) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(archivoMaterias, StandardCharsets.UTF_8)) {
            reader.readLine(); // Saltar encabezado
            String linea;
            int numLinea = 1;
            while ((linea = reader.readLine()) != null) {
                numLinea++;
                String lineaTrimmed = linea.trim();
                if (lineaTrimmed.isEmpty()) {
                    continue;
                }
                String[] campos = lineaTrimmed.split(SEPARADOR, -1);
                if (campos.length < 2) {
                    throw new IOException("Formato inválido en materias.csv línea " + numLinea
                            + ": se esperan al menos 2 columnas");
                }
                String codigo = campos[0].trim();
                String nombre = campos[1].trim();
                servicio.registrarMateria(new Materia(codigo, nombre));
            }
        }
    }

    private void cargarEstudiantes(RiesgoAcademicoService servicio) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(archivoEstudiantes, StandardCharsets.UTF_8)) {
            reader.readLine(); // Saltar encabezado
            String linea;
            int numLinea = 1;
            while ((linea = reader.readLine()) != null) {
                numLinea++;
                String lineaTrimmed = linea.trim();
                if (lineaTrimmed.isEmpty()) {
                    continue;
                }
                String[] campos = lineaTrimmed.split(SEPARADOR, -1);
                if (campos.length < COLUMNAS_ESTUDIANTE_MINIMAS) {
                    throw new IOException("Formato inválido en estudiantes.csv línea " + numLinea
                            + ": se esperan al menos " + COLUMNAS_ESTUDIANTE_MINIMAS + " columnas");
                }
                Estudiante estudiante = parsearEstudiante(campos, numLinea);
                servicio.registrarEstudiante(estudiante);
            }
        }
    }

    private Estudiante parsearEstudiante(String[] campos, int numLinea) throws IOException {
        try {
            long id = Long.parseLong(campos[0].trim());
            String nombre = campos[1].trim();
            String programa = campos[2].trim();
            TipoColegio tipoColegio = TipoColegio.valueOf(campos[3].trim().toUpperCase());
            String pais = campos[4].trim();

            PerfilAdmision perfil = new PerfilAdmision(programa, tipoColegio, pais);
            PuntajeICFES icfes = parsearIcfes(campos);

            return new Estudiante(id, nombre, icfes, perfil);
        } catch (IllegalArgumentException e) {
            throw new IOException("Error parseando estudiante en línea " + numLinea + ": " + e.getMessage(), e);
        }
    }

    private PuntajeICFES parsearIcfes(String[] campos) {
        if (campos.length < COLUMNAS_ESTUDIANTE_CON_ICFES) {
            return null;
        }
        String matStr = campos[5].trim();
        if (matStr.isEmpty()) {
            return null;
        }
        int matematicas = Integer.parseInt(matStr);
        int lectura = Integer.parseInt(campos[6].trim());
        int ciencias = Integer.parseInt(campos[7].trim());
        int sociales = Integer.parseInt(campos[8].trim());
        int ingles = Integer.parseInt(campos[9].trim());
        return new PuntajeICFES(matematicas, lectura, ciencias, sociales, ingles);
    }

    private void cargarRegistros(RiesgoAcademicoService servicio) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(archivoRegistros, StandardCharsets.UTF_8)) {
            reader.readLine(); // Saltar encabezado
            String linea;
            int numLinea = 1;
            while ((linea = reader.readLine()) != null) {
                numLinea++;
                String lineaTrimmed = linea.trim();
                if (lineaTrimmed.isEmpty()) {
                    continue;
                }
                String[] campos = lineaTrimmed.split(SEPARADOR, -1);
                if (campos.length < 3) {
                    throw new IOException("Formato inválido en registros.csv línea " + numLinea
                            + ": se esperan al menos 3 columnas");
                }
                try {
                    long estudianteId = Long.parseLong(campos[0].trim());
                    String materiaId = campos[1].trim();
                    double nota = Double.parseDouble(campos[2].trim());
                    servicio.agregarRegistroAcademico(
                            new RegistroAcademico(estudianteId, materiaId, nota));
                } catch (NumberFormatException e) {
                    throw new IOException("Error parseando registro en línea " + numLinea
                            + ": " + e.getMessage(), e);
                }
            }
        }
    }
}
