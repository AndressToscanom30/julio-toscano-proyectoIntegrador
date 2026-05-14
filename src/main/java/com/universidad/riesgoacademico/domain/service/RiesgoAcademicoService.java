package com.universidad.riesgoacademico.domain.service;

import com.universidad.riesgoacademico.domain.algorithm.*;
import com.universidad.riesgoacademico.domain.model.*;
import com.universidad.riesgoacademico.domain.model.ResultadoRiesgo.EstrategiaSimilitud;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio principal de detección de riesgo académico.
 *
 * <p>Orquesta el grafo bipartito, la selección automática de estrategia
 * de similitud (patrón Strategy) y la generación de alertas de materias en riesgo.
 *
 * <p>Complejidad de consulta: O(M_e × k_avg) para GradeSimilarityStrategy (caso principal).
 * Latencia objetivo: < 500 ms para N = 100 000 (restricción de escala de PROBLEMA.md).
 */
public final class RiesgoAcademicoService {

    /** Mínimo de registros universitarios para usar GradeSimilarityStrategy. */
    static final int MIN_REGISTROS_PARA_NOTAS = 2;

    private final GrafoBipartito grafo;
    private final Map<Long, Estudiante> estudiantes;
    private final Map<String, Materia> materias;

    // Estrategias (lazy-initialized cuando se necesitan)
    private GradeSimilarityStrategy gradeStrategy;
    private ICFESSimilarityStrategy icfesStrategy;
    private HybridSimilarityStrategy hybridStrategy;
    private HighSchoolSimilarityStrategy highSchoolStrategy;
    private DemographicFallbackStrategy demographicStrategy;

    /**
     * Crea el servicio de riesgo académico.
     *
     * @param capacidadEstudiantes estimación del número de estudiantes
     */
    public RiesgoAcademicoService(int capacidadEstudiantes) {
        this.grafo = new GrafoBipartito(capacidadEstudiantes);
        int cap = (int) (capacidadEstudiantes / 0.75) + 1;
        this.estudiantes = new HashMap<>(cap);
        this.materias = new HashMap<>(512);
    }

    /**
     * Crea el servicio con capacidad por defecto.
     */
    public RiesgoAcademicoService() {
        this(1000);
    }

    /**
     * Registra un estudiante en el sistema.
     *
     * @param estudiante estudiante a registrar (no nulo, ID único)
     * @throws NullPointerException si estudiante es nulo
     * @throws IllegalArgumentException si ya existe un estudiante con el mismo ID
     */
    public void registrarEstudiante(Estudiante estudiante) {
        Objects.requireNonNull(estudiante, "El estudiante no puede ser nulo");
        if (estudiantes.containsKey(estudiante.getId())) {
            throw new IllegalArgumentException(
                    "Ya existe un estudiante con ID: " + estudiante.getId());
        }
        estudiantes.put(estudiante.getId(), estudiante);
        invalidarEstrategias();
    }

    /**
     * Registra una materia en el sistema.
     *
     * @param materia materia a registrar (no nula, código único)
     * @throws NullPointerException si materia es nula
     * @throws IllegalArgumentException si ya existe una materia con el mismo código
     */
    public void registrarMateria(Materia materia) {
        Objects.requireNonNull(materia, "La materia no puede ser nula");
        if (materias.containsKey(materia.codigo())) {
            throw new IllegalArgumentException(
                    "Ya existe una materia con código: " + materia.codigo());
        }
        materias.put(materia.codigo(), materia);
    }

    /**
     * Agrega un registro académico (arista ponderada al grafo bipartito).
     *
     * @param registro registro académico (no nulo)
     * @throws NullPointerException si registro es nulo
     * @throws IllegalArgumentException si el estudiante o materia no están registrados (Pre3)
     */
    public void agregarRegistroAcademico(RegistroAcademico registro) {
        Objects.requireNonNull(registro, "El registro no puede ser nulo");
        if (!estudiantes.containsKey(registro.estudianteId())) {
            throw new IllegalArgumentException(
                    "Estudiante no registrado: " + registro.estudianteId());
        }
        if (!materias.containsKey(registro.materiaId())) {
            throw new IllegalArgumentException(
                    "Materia no registrada: " + registro.materiaId());
        }
        grafo.agregarRegistro(registro);
        invalidarEstrategias();
    }

    /**
     * Analiza el riesgo académico de un estudiante.
     *
     * <p>Selecciona automáticamente la estrategia de similitud según los datos
     * disponibles del estudiante (ver árbol de decisión en PROBLEMA.md),
     * encuentra los gemelos académicos con similitud >= umbral, y genera
     * la lista de materias en riesgo.
     *
     * <p>Postcondiciones: Post1 a Post6 de PROBLEMA.md.
     *
     * @param estudianteId    ID del estudiante a evaluar (Pre8)
     * @param umbralSimilitud similitud mínima para considerar gemelo (Pre6: 0.0 < umbral <= 1.0)
     * @param umbralRiesgo    nota por debajo de la cual se considera reprobación (por defecto 3.0)
     * @return resultado del análisis con materias en riesgo y metadatos
     * @throws IllegalArgumentException si el estudiante no existe o los umbrales son inválidos
     */
    public ResultadoRiesgo analizarRiesgo(long estudianteId, double umbralSimilitud,
                                           double umbralRiesgo) {
        // Validar precondiciones
        validarPrecondiciones(estudianteId, umbralSimilitud, umbralRiesgo);

        Estudiante estudiante = estudiantes.get(estudianteId);

        // Seleccionar estrategia según datos disponibles
        SimilarityStrategy estrategia = seleccionarEstrategia(estudiante);

        // Calcular similitudes con todos los co-estudiantes
        Map<Long, Double> similitudes = estrategia.calcularSimilitudes(estudianteId);

        // Filtrar gemelos con similitud >= umbral (Post1)
        Map<Long, Double> gemelos = similitudes.entrySet().stream()
                .filter(e -> e.getValue() >= umbralSimilitud)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Generar lista de materias en riesgo (Post2, Post3)
        List<MateriaEnRiesgo> materiasEnRiesgo = generarAlertasRiesgo(gemelos, estudianteId,
                umbralRiesgo);

        return new ResultadoRiesgo(
                estudianteId,
                materiasEnRiesgo,
                estrategia.getTipo(),
                gemelos.size()
        );
    }

    /**
     * Selecciona la estrategia de similitud apropiada según el árbol de decisión
     * documentado en PROBLEMA.md.
     *
     * @param estudiante el estudiante a evaluar
     * @return la estrategia seleccionada (Inv6: determinista)
     */
    SimilarityStrategy seleccionarEstrategia(Estudiante estudiante) {
        boolean tieneNotas = grafo.getNumRegistros(estudiante.getId()) >= MIN_REGISTROS_PARA_NOTAS;
        boolean tieneIcfes = estudiante.tieneIcfesValido();
        boolean tieneNotasColegio = estudiante.tieneNotasColegioValidas();

        if (tieneNotas && tieneIcfes) {
            return getHybridStrategy();
        } else if (tieneNotas) {
            return getGradeStrategy();
        } else if (tieneIcfes) {
            return getIcfesStrategy();
        } else if (tieneNotasColegio) {
            return getHighSchoolStrategy();
        } else {
            return getDemographicStrategy();
        }
    }

    private List<MateriaEnRiesgo> generarAlertasRiesgo(Map<Long, Double> gemelos,
                                                        long estudianteId,
                                                        double umbralRiesgo) {
        if (gemelos.isEmpty()) {
            return Collections.emptyList();
        }

        // Recopilar las materias de los gemelos donde reprobaron
        Map<String, List<Double>> notasGemelosPorMateria = new HashMap<>();
        Map<String, Integer> reprobacionesPorMateria = new HashMap<>();

        for (long gemeloId : gemelos.keySet()) {
            Map<String, Double> notasGemelo = grafo.getNotasEstudiante(gemeloId);
            for (Map.Entry<String, Double> entry : notasGemelo.entrySet()) {
                String matId = entry.getKey();
                double nota = entry.getValue();

                notasGemelosPorMateria
                        .computeIfAbsent(matId, k -> new ArrayList<>())
                        .add(nota);

                if (nota < umbralRiesgo) {
                    reprobacionesPorMateria.merge(matId, 1, Integer::sum);
                }
            }
        }

        // Excluir materias que el estudiante ya aprobó
        Map<String, Double> notasEstudiante = grafo.getNotasEstudiante(estudianteId);

        // Generar alertas solo para materias con al menos un gemelo reprobado (Post2)
        List<MateriaEnRiesgo> alertas = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : reprobacionesPorMateria.entrySet()) {
            String matId = entry.getKey();
            int reprobaciones = entry.getValue();

            // Si el estudiante ya cursó la materia y aprobó, no alertar
            Double notaEstudiante = notasEstudiante.get(matId);
            if (notaEstudiante != null && notaEstudiante >= umbralRiesgo) {
                continue;
            }

            List<Double> notasGemelos = notasGemelosPorMateria.get(matId);
            double promedio = notasGemelos.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            // nivelRiesgo = 1 - (promedio / NOTA_MAXIMA) normalizado a [0,1]
            double nivelRiesgo = 1.0 - (promedio / RegistroAcademico.NOTA_MAXIMA);
            nivelRiesgo = Math.min(1.0, Math.max(0.0, nivelRiesgo));

            Materia materia = materias.get(matId);
            String nombreMateria = materia != null ? materia.nombre() : matId;

            alertas.add(new MateriaEnRiesgo(matId, nombreMateria, nivelRiesgo, reprobaciones));
        }

        // Ordenar de mayor a menor riesgo (Post3)
        alertas.sort(Comparator.comparingDouble(MateriaEnRiesgo::nivelRiesgo).reversed());

        return alertas;
    }

    private void validarPrecondiciones(long estudianteId, double umbralSimilitud,
                                       double umbralRiesgo) {
        if (!estudiantes.containsKey(estudianteId)) {
            throw new IllegalArgumentException("Estudiante no encontrado: " + estudianteId);
        }
        if (umbralSimilitud <= 0.0 || umbralSimilitud > 1.0) {
            throw new IllegalArgumentException(
                    "umbralSimilitud debe estar en (0.0, 1.0]: " + umbralSimilitud);
        }
        if (umbralRiesgo <= 0.0 || umbralRiesgo > RegistroAcademico.NOTA_MAXIMA) {
            throw new IllegalArgumentException(
                    "umbralRiesgo debe estar en (0.0, 5.0]: " + umbralRiesgo);
        }
    }

    private void invalidarEstrategias() {
        gradeStrategy = null;
        icfesStrategy = null;
        hybridStrategy = null;
        highSchoolStrategy = null;
        demographicStrategy = null;
    }

    private GradeSimilarityStrategy getGradeStrategy() {
        if (gradeStrategy == null) {
            gradeStrategy = new GradeSimilarityStrategy(grafo);
        }
        return gradeStrategy;
    }

    private ICFESSimilarityStrategy getIcfesStrategy() {
        if (icfesStrategy == null) {
            icfesStrategy = new ICFESSimilarityStrategy(estudiantes);
        }
        return icfesStrategy;
    }

    private HybridSimilarityStrategy getHybridStrategy() {
        if (hybridStrategy == null) {
            hybridStrategy = new HybridSimilarityStrategy(getGradeStrategy(), getIcfesStrategy());
        }
        return hybridStrategy;
    }

    private DemographicFallbackStrategy getDemographicStrategy() {
        if (demographicStrategy == null) {
            demographicStrategy = new DemographicFallbackStrategy(estudiantes);
        }
        return demographicStrategy;
    }

    private HighSchoolSimilarityStrategy getHighSchoolStrategy() {
        if (highSchoolStrategy == null) {
            highSchoolStrategy = new HighSchoolSimilarityStrategy(estudiantes);
        }
        return highSchoolStrategy;
    }

    /** @return grafo bipartito interno (para benchmarks y pruebas) */
    public GrafoBipartito getGrafo() { return grafo; }

    /** @return número de estudiantes registrados */
    public int getNumEstudiantes() { return estudiantes.size(); }

    /** @return número de materias registradas */
    public int getNumMaterias() { return materias.size(); }
}
