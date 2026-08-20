package com.sdd.sdd.persona.entity;

/**
 * Géneros de una persona natural en el sistema.
 * Persiste como cadena de texto en la base de datos mediante {@code @Enumerated(EnumType.STRING)}.
 */
public enum Genero {
    MASCULINO,
    FEMENINO,
    OTRO
}
