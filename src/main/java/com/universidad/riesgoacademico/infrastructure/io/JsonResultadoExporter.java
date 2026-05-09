package com.universidad.riesgoacademico.infrastructure.io;

import com.universidad.riesgoacademico.domain.model.MateriaEnRiesgo;
import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Exporta resultados de análisis de riesgo en formato JSON.
 *
 * <p>Serialización manual sin dependencias externas (sin Jackson/Gson).
 * Aceptable para la estructura simple de {@link ResultadoRiesgo} (ADR-003).
 *
 * <p>Formato de salida: JSON con indentación de 2 espacios.
 *
 * <p>Dependencia: solo usa clases de {@code domain.model}.
 * No importa clases de {@code domain.algorithm} (ADR-003).
 */
public final class JsonResultadoExporter implements ResultadoExporter {

    /** Indentación por nivel de anidación. */
    private static final String INDENT = "  ";

    private final Path archivoSalida;

    /**
     * Crea un exportador JSON con la ruta de salida.
     *
     * @param archivoSalida ruta del archivo de salida (no nula)
     */
    public JsonResultadoExporter(Path archivoSalida) {
        this.archivoSalida = Objects.requireNonNull(archivoSalida,
                "La ruta de salida no puede ser nula");
    }

    @Override
    public void exportar(ResultadoRiesgo resultado) throws IOException {
        Objects.requireNonNull(resultado, "El resultado no puede ser nulo");
        String json = resultadoToJson(resultado, INDENT);
        escribirArchivo(json);
    }

    @Override
    public void exportarBatch(List<ResultadoRiesgo> resultados) throws IOException {
        Objects.requireNonNull(resultados, "La lista de resultados no puede ser nula");

        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < resultados.size(); i++) {
            sb.append(resultadoToJson(resultados.get(i), INDENT));
            if (i < resultados.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        escribirArchivo(sb.toString());
    }

    /**
     * Convierte un {@link ResultadoRiesgo} a su representación JSON.
     *
     * @param resultado resultado a serializar
     * @param indent    indentación base
     * @return cadena JSON
     */
    static String resultadoToJson(ResultadoRiesgo resultado, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("{\n");
        sb.append(indent).append(INDENT).append("\"estudianteId\": ")
                .append(resultado.estudianteId()).append(",\n");
        sb.append(indent).append(INDENT).append("\"estrategiaUsada\": \"")
                .append(resultado.estrategiaUsada().name()).append("\",\n");
        sb.append(indent).append(INDENT).append("\"gemelosIdentificados\": ")
                .append(resultado.gemelosIdentificados()).append(",\n");
        sb.append(indent).append(INDENT).append("\"materiasEnRiesgo\": [\n");

        List<MateriaEnRiesgo> materias = resultado.materiasEnRiesgo();
        for (int i = 0; i < materias.size(); i++) {
            MateriaEnRiesgo m = materias.get(i);
            sb.append(indent).append(INDENT).append(INDENT).append("{\n");
            sb.append(indent).append(INDENT).append(INDENT).append(INDENT)
                    .append("\"codigoMateria\": \"").append(escapeJson(m.codigoMateria())).append("\",\n");
            sb.append(indent).append(INDENT).append(INDENT).append(INDENT)
                    .append("\"nombreMateria\": \"").append(escapeJson(m.nombreMateria())).append("\",\n");
            sb.append(indent).append(INDENT).append(INDENT).append(INDENT)
                    .append("\"nivelRiesgo\": ").append(String.format("%.4f", m.nivelRiesgo())).append(",\n");
            sb.append(indent).append(INDENT).append(INDENT).append(INDENT)
                    .append("\"gemelosReprobaron\": ").append(m.gemelosReprobaron()).append("\n");
            sb.append(indent).append(INDENT).append(INDENT).append("}");
            if (i < materias.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append(indent).append(INDENT).append("]\n");
        sb.append(indent).append("}");
        return sb.toString();
    }

    private void escribirArchivo(String contenido) throws IOException {
        // Crear directorio padre si no existe
        Path parent = archivoSalida.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(archivoSalida, StandardCharsets.UTF_8)) {
            writer.write(contenido);
        }
    }

    /**
     * Escapa caracteres especiales para JSON.
     *
     * @param s cadena a escapar
     * @return cadena con caracteres especiales escapados
     */
    static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
