package com.sdd.sdd.usuario.entity;

/**
 * Estados posibles de un usuario en el sistema.
 * Persiste como cadena de texto en la base de datos mediante {@code @Enumerated(EnumType.STRING)}.
 */
public enum EstadoUsuario {
    ACTIVO,
    INACTIVO
}
