package com.sdd.sdd.usuario.dto;

import com.sdd.sdd.usuario.entity.EstadoUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para Usuario.
 * El campo {@code password} está deliberadamente ausente (RF-07-02, RF-08-03).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponse {

    private Long id;
    private String nombres;
    private String apellidos;
    private String username;
    private String email;
    private EstadoUsuario estado;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaModificacion;
}
