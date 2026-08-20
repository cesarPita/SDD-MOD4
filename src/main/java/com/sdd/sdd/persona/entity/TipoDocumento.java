package com.sdd.sdd.persona.entity;

/**
 * Tipos de documento de identidad aceptados en el sistema.
 * Persiste como cadena de texto en la base de datos mediante {@code @Enumerated(EnumType.STRING)}.
 */
public enum TipoDocumento {
    CI,
    PASAPORTE,
    CEX,
    NIT
}
