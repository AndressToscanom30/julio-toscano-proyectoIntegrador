package com.universidad.riesgoacademico.domain.algorithm;

import com.universidad.riesgoacademico.domain.model.GradeEntry;
import com.universidad.riesgoacademico.domain.model.RegistroAcademico;

import java.util.*;

/**
 * Grafo bipartito ponderado: estudiantes ↔ materias.
 *
 * <p>Estructura de datos central del sistema (ADR-002). Implementa:
 * <ul>
 *   <li>Grafo directo: {@code HashMap<Long, Map<String, Double>>} — estudiante → {materia → nota}</li>
 *   <li>Índice invertido: {@code HashMap<String, List<GradeEntry>>} — materia → lista de (estudianteId, nota)</li>
 *   <li>Normas precalculadas: {@code HashMap<Long, Double>} — estudiante → norma euclidiana del vector de notas</li>
 * </ul>
 *
 * <p>Invariante Inv1: solo existen aristas de estudiantes a materias.
 * <p>Invariante Inv2: todos los pesos satisfacen {@code 0.0 <= peso <= 5.0}.
 * <p>Invariante Inv5: no hay aristas duplicadas.
 *
 * <p>Complejidad espacial: O(|E| + N) donde |E| = número de aristas y N = número de estudiantes.
 */
public final class GrafoBipartito {

    /** Grafo directo: estudiante → {materia → nota}. */
    private final Map<Long, Map<String, Double>> grafoPorEstudiante;

    /** Índice invertido: materia → lista de (estudianteId, nota). */
    private final Map<String, List<GradeEntry>> indicePorMateria;

    /** Normas precalculadas: estudiante → norma euclidiana del vector de notas. */
    private final Map<Long, Double> normas;

    /** Número total de aristas en el grafo. */
    private int totalAristas;

    /**
     * Crea un grafo bipartito vacío con capacidad inicial para N estudiantes.
     *
     * @param capacidadEstudiantes estimación del número de estudiantes (para pre-dimensionar)
     */
    public GrafoBipartito(int capacidadEstudiantes) {
        int capacidadHash = (int) (capacidadEstudiantes / 0.75) + 1;
        this.grafoPorEstudiante = new HashMap<>(capacidadHash);
        this.indicePorMateria = new HashMap<>(512); // M <= 500
        this.normas = new HashMap<>(capacidadHash);
        this.totalAristas = 0;
    }

    /**
     * Crea un grafo bipartito vacío con capacidad por defecto.
     */
    public GrafoBipartito() {
        this(1000);
    }

    /**
     * Agrega un registro académico (arista ponderada) al grafo.
     *
     * <p>Actualiza simultáneamente el grafo directo y el índice invertido.
     * Si la arista ya existe (mismo estudiante, misma materia), la nota se sobrescribe.
     *
     * <p>Complejidad: O(1) amortizado.
     *
     * @param registro registro académico a agregar (no nulo)
     * @throws NullPointerException si registro es nulo
     */
    public void agregarRegistro(RegistroAcademico registro) {
        Objects.requireNonNull(registro, "El registro académico no puede ser nulo");

        long estId = registro.estudianteId();
        String matId = registro.materiaId();
        double nota = registro.nota();

        // Grafo directo: estudiante → {materia → nota}
        Map<String, Double> materias = grafoPorEstudiante
                .computeIfAbsent(estId, k -> new HashMap<>());

        boolean yaExistia = materias.containsKey(matId);
        materias.put(matId, nota);

        // Índice invertido: materia → lista de (estudianteId, nota)
        List<GradeEntry> listaEstudiantes = indicePorMateria
                .computeIfAbsent(matId, k -> new ArrayList<>());

        if (yaExistia) {
            // Actualizar la entrada existente en el índice invertido
            listaEstudiantes.removeIf(e -> e.estudianteId() == estId);
        } else {
            totalAristas++;
        }
        listaEstudiantes.add(new GradeEntry(estId, nota));

        // Invalidar norma precalculada (se recalculará bajo demanda)
        normas.remove(estId);
    }

    /**
     * Carga un lote de registros académicos al grafo.
     *
     * <p>Complejidad: O(|registros|) amortizado.
     *
     * @param registros colección de registros a cargar (no nula)
     */
    public void cargarRegistros(Collection<RegistroAcademico> registros) {
        Objects.requireNonNull(registros, "La colección de registros no puede ser nula");
        for (RegistroAcademico registro : registros) {
            agregarRegistro(registro);
        }
    }

    /**
     * Obtiene las notas de un estudiante como mapa {materia → nota}.
     *
     * <p>Complejidad: O(1) lookup.
     *
     * @param estudianteId ID del estudiante
     * @return mapa inmutable de {materiaId → nota}, o mapa vacío si el estudiante no tiene registros
     */
    public Map<String, Double> getNotasEstudiante(long estudianteId) {
        Map<String, Double> notas = grafoPorEstudiante.get(estudianteId);
        return notas != null ? Collections.unmodifiableMap(notas) : Collections.emptyMap();
    }

    /**
     * Obtiene la lista de estudiantes que cursaron una materia (índice invertido).
     *
     * <p>Complejidad: O(1) lookup.
     *
     * @param materiaId código de la materia
     * @return lista inmutable de GradeEntry, o lista vacía si nadie cursó la materia
     */
    public List<GradeEntry> getEstudiantesPorMateria(String materiaId) {
        List<GradeEntry> entries = indicePorMateria.get(materiaId);
        return entries != null ? Collections.unmodifiableList(entries) : Collections.emptyList();
    }

    /**
     * Calcula y cachea la norma euclidiana del vector de notas de un estudiante.
     *
     * <p>Complejidad: O(M_e) en la primera llamada, O(1) en llamadas posteriores.
     *
     * @param estudianteId ID del estudiante
     * @return norma euclidiana >= 0.0; retorna 0.0 si el estudiante no tiene registros
     */
    public double getNorma(long estudianteId) {
        return normas.computeIfAbsent(estudianteId, id -> {
            Map<String, Double> notas = grafoPorEstudiante.get(id);
            if (notas == null || notas.isEmpty()) {
                return 0.0;
            }
            double sumaCuadrados = 0.0;
            for (double nota : notas.values()) {
                sumaCuadrados += nota * nota;
            }
            return Math.sqrt(sumaCuadrados);
        });
    }

    /**
     * Verifica si un estudiante tiene registros en el grafo.
     *
     * @param estudianteId ID del estudiante
     * @return true si el estudiante tiene al menos un registro
     */
    public boolean tieneRegistros(long estudianteId) {
        Map<String, Double> notas = grafoPorEstudiante.get(estudianteId);
        return notas != null && !notas.isEmpty();
    }

    /**
     * Obtiene el número de registros de un estudiante.
     *
     * @param estudianteId ID del estudiante
     * @return número de materias cursadas por el estudiante (0 si no tiene registros)
     */
    public int getNumRegistros(long estudianteId) {
        Map<String, Double> notas = grafoPorEstudiante.get(estudianteId);
        return notas != null ? notas.size() : 0;
    }

    /**
     * @return número total de estudiantes con al menos un registro en el grafo
     */
    public int getNumEstudiantes() {
        return grafoPorEstudiante.size();
    }

    /**
     * @return número total de materias con al menos un registro en el grafo
     */
    public int getNumMaterias() {
        return indicePorMateria.size();
    }

    /**
     * @return número total de aristas en el grafo (Inv4: |E| <= |S| × |M|)
     */
    public int getTotalAristas() {
        return totalAristas;
    }

    /**
     * @return conjunto de todos los IDs de estudiantes con registros en el grafo
     */
    public Set<Long> getEstudianteIds() {
        return Collections.unmodifiableSet(grafoPorEstudiante.keySet());
    }

    /**
     * @return conjunto de todos los códigos de materias con registros en el grafo
     */
    public Set<String> getMateriaIds() {
        return Collections.unmodifiableSet(indicePorMateria.keySet());
    }
}
