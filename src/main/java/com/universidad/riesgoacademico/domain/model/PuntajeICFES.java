package com.universidad.riesgoacademico.domain.model;

/**
 * Puntaje ICFES (Saber 11) de un estudiante colombiano.
 *
 * <p>Vector de 5 componentes utilizado por {@code ICFESSimilarityStrategy}
 * para calcular similitud entre estudiantes de primer semestre que aún
 * no tienen notas universitarias.
 *
 * <p>Invariante: cada componente satisface {@code 0 <= componente <= 100}.
 *
 * @param matematicas    puntaje en Matemáticas [0, 100]
 * @param lecturaCritica puntaje en Lectura Crítica [0, 100]
 * @param cienciasNaturales puntaje en Ciencias Naturales [0, 100]
 * @param socialesYCiudadanas puntaje en Sociales y Ciudadanas [0, 100]
 * @param ingles         puntaje en Inglés [0, 100]
 */
public record PuntajeICFES(
        int matematicas,
        int lecturaCritica,
        int cienciasNaturales,
        int socialesYCiudadanas,
        int ingles
) {
    /** Número de componentes del vector ICFES. */
    public static final int NUM_COMPONENTES = 5;

    /** Valor mínimo permitido para cada componente. */
    public static final int MIN_PUNTAJE = 0;

    /** Valor máximo permitido para cada componente. */
    public static final int MAX_PUNTAJE = 100;

    /**
     * Constructor compacto con validación de precondiciones (Pre7).
     */
    public PuntajeICFES {
        validarComponente(matematicas, "matematicas");
        validarComponente(lecturaCritica, "lecturaCritica");
        validarComponente(cienciasNaturales, "cienciasNaturales");
        validarComponente(socialesYCiudadanas, "socialesYCiudadanas");
        validarComponente(ingles, "ingles");
    }

    /**
     * Retorna el vector ICFES como arreglo de doubles para cálculo de similitud.
     *
     * @return arreglo de 5 elementos en orden: matemáticas, lectura, ciencias, sociales, inglés
     */
    public double[] toVector() {
        return new double[]{
                matematicas, lecturaCritica, cienciasNaturales,
                socialesYCiudadanas, ingles
        };
    }

    /**
     * Calcula la norma euclidiana del vector ICFES.
     *
     * @return norma >= 0.0; retorna 0.0 si todos los componentes son 0
     */
    public double norma() {
        double suma = (double) matematicas * matematicas
                + (double) lecturaCritica * lecturaCritica
                + (double) cienciasNaturales * cienciasNaturales
                + (double) socialesYCiudadanas * socialesYCiudadanas
                + (double) ingles * ingles;
        return Math.sqrt(suma);
    }

    /**
     * Verifica si el vector tiene norma cero (todos los componentes en 0).
     * En este caso, la similitud coseno es indefinida (CB-13).
     *
     * @return true si todos los componentes son 0
     */
    public boolean esVectorCero() {
        return matematicas == 0 && lecturaCritica == 0 && cienciasNaturales == 0
                && socialesYCiudadanas == 0 && ingles == 0;
    }

    private static void validarComponente(int valor, String nombre) {
        if (valor < MIN_PUNTAJE || valor > MAX_PUNTAJE) {
            throw new IllegalArgumentException(
                    String.format("Componente ICFES '%s' fuera de rango [%d, %d]: %d",
                            nombre, MIN_PUNTAJE, MAX_PUNTAJE, valor));
        }
    }
}
