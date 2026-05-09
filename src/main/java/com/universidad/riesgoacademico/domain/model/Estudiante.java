package com.universidad.riesgoacademico.domain.model;

import java.util.Objects;

/**
 * Representa un estudiante en el grafo bipartito.
 *
 * <p>Nodo del conjunto S del grafo bipartito ponderado. Cada estudiante tiene
 * un ID único, un nombre, y opcionalmente un puntaje ICFES y un perfil de admisión
 * (utilizados para resolver el cold start multinivel).
 *
 * <p>Invariante: {@code id >= 1} (Pre8 de PROBLEMA.md).
 */
public final class Estudiante {

    /** ID mínimo válido para un estudiante. */
    public static final long ID_MINIMO = 1L;

    private final long id;
    private final String nombre;
    private final PuntajeICFES icfes;           // nullable — no todos tienen ICFES
    private final PerfilAdmision perfilAdmision; // nullable — datos opcionales

    /**
     * Crea un estudiante con todos los datos disponibles.
     *
     * @param id             identificador único (>= 1)
     * @param nombre         nombre completo del estudiante (no nulo ni vacío)
     * @param icfes          puntaje ICFES (puede ser null si es extranjero o no disponible)
     * @param perfilAdmision perfil de admisión (puede ser null si no se capturó)
     * @throws IllegalArgumentException si id < 1 o nombre es nulo/vacío
     */
    public Estudiante(long id, String nombre, PuntajeICFES icfes, PerfilAdmision perfilAdmision) {
        if (id < ID_MINIMO) {
            throw new IllegalArgumentException("El ID del estudiante debe ser >= " + ID_MINIMO + ", recibido: " + id);
        }
        Objects.requireNonNull(nombre, "El nombre del estudiante no puede ser nulo");
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del estudiante no puede estar vacío");
        }
        this.id = id;
        this.nombre = nombre;
        this.icfes = icfes;
        this.perfilAdmision = perfilAdmision;
    }

    /**
     * Constructor simplificado sin ICFES ni perfil (para pruebas o datos mínimos).
     *
     * @param id     identificador único (>= 1)
     * @param nombre nombre completo del estudiante (no nulo ni vacío)
     */
    public Estudiante(long id, String nombre) {
        this(id, nombre, null, null);
    }

    public long getId() { return id; }

    public String getNombre() { return nombre; }

    /**
     * @return puntaje ICFES o null si no disponible (estudiante extranjero o sin datos)
     */
    public PuntajeICFES getIcfes() { return icfes; }

    /**
     * @return true si el estudiante tiene puntaje ICFES registrado y con norma > 0 (CB-13)
     */
    public boolean tieneIcfesValido() {
        return icfes != null && !icfes.esVectorCero();
    }

    /**
     * @return perfil de admisión o null si no disponible
     */
    public PerfilAdmision getPerfilAdmision() { return perfilAdmision; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Estudiante that = (Estudiante) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Estudiante{id=" + id + ", nombre='" + nombre + "'}";
    }
}
