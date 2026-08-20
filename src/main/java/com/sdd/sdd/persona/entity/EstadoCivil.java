package com.sdd.sdd.persona.entity;

/**
 * Estados civiles de una persona natural en el sistema.
 * Persiste como cadena de texto en la base de datos mediante {@code @Enumerated(EnumType.STRING)}.
 */
public enum EstadoCivil {
    SOLTERO,
    CASADO,
    DIVORCIADO,
    VIUDO,
    UNION_LIBRE
}
