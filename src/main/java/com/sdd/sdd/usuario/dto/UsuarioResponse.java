package com.sdd.sdd.usuario.dto;

import com.sdd.sdd.usuario.entity.EstadoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para Usuario.
 * El campo {@code password} esta deliberadamente ausente (RF-07-02, RF-08-03).
 */
@Schema(description = "Datos del usuario devueltos por la API. El campo password nunca esta presente.")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponse {

    @Schema(description = "Identificador unico del usuario", example = "1")
    private Long id;

    @Schema(description = "Nombres del usuario", example = "Juan Carlos")
    private String nombres;

    @Schema(description = "Apellidos del usuario", example = "Perez Garcia")
    private String apellidos;

    @Schema(description = "Nombre de usuario unico", example = "jperez")
    private String username;

    @Schema(description = "Correo electronico del usuario", example = "juan.perez@example.com")
    private String email;

    @Schema(description = "Estado actual del usuario", example = "ACTIVO")
    private EstadoUsuario estado;

    @Schema(description = "Fecha y hora de creacion del registro (ISO 8601 con offset)", example = "2026-01-15T10:30:00-05:00")
    private OffsetDateTime fechaCreacion;

    @Schema(description = "Fecha y hora de ultima modificacion (ISO 8601 con offset)", example = "2026-01-20T14:45:00-05:00")
    private OffsetDateTime fechaModificacion;
}