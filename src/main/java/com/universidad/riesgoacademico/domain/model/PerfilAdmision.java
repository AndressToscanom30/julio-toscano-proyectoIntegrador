package com.universidad.riesgoacademico.domain.model;

/**
 * Perfil de admisión de un estudiante — datos básicos capturados al ingresar.
 *
 * <p>Utilizado por {@code DemographicFallbackStrategy} como último recurso
 * para estudiantes sin ICFES y sin notas universitarias (cold start verdadero).
 *
 * @param programaAcademico código del programa (ej: "ING_SISTEMAS"). No nulo ni vacío.
 * @param tipoColegio tipo de colegio de origen (PUBLICO o PRIVADO).
 * @param paisOrigen código ISO del país de origen (ej: "CO", "VE", "EC"). No nulo ni vacío.
 */
public record PerfilAdmision(String programaAcademico, TipoColegio tipoColegio, String paisOrigen) {

    /**
     * Tipos de colegio de origen del estudiante.
     */
    public enum TipoColegio {
        PUBLICO,
        PRIVADO
    }

    /**
     * Constructor compacto con validación de precondiciones.
     */
    public PerfilAdmision {
        if (programaAcademico == null || programaAcademico.isBlank()) {
            throw new IllegalArgumentException("El programa académico no puede ser nulo ni vacío");
        }
        if (tipoColegio == null) {
            throw new IllegalArgumentException("El tipo de colegio no puede ser nulo");
        }
        if (paisOrigen == null || paisOrigen.isBlank()) {
            throw new IllegalArgumentException("El país de origen no puede ser nulo ni vacío");
        }
    }
}
