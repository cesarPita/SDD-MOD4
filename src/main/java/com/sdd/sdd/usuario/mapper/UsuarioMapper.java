package com.sdd.sdd.usuario.mapper;

import com.sdd.sdd.usuario.dto.UsuarioRequest;
import com.sdd.sdd.usuario.dto.UsuarioResponse;
import com.sdd.sdd.usuario.entity.EstadoUsuario;
import com.sdd.sdd.usuario.entity.Usuario;

/**
 * Clase utilitaria para el mapeo entre la entidad {@link Usuario} y sus DTOs.
 *
 * <p>No es instanciable. Todos los métodos son estáticos.
 *
 * <p>El campo {@code password} nunca se copia hacia {@link UsuarioResponse} (RF-08-03).
 * {@code toEntity()} solo acepta el hash BCrypt ya codificado, nunca la contraseña en texto plano (RF-08-01).
 */
public final class UsuarioMapper {

    private UsuarioMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte una entidad {@link Usuario} a su DTO de respuesta.
     * El campo {@code password} se omite deliberadamente.
     *
     * @param usuario entidad persistida
     * @return DTO de respuesta sin campo {@code password}
     */
    public static UsuarioResponse toResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .username(usuario.getUsername())
                .email(usuario.getEmail())
                .estado(usuario.getEstado())
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaModificacion(usuario.getFechaModificacion())
                .build();
    }

    /**
     * Construye una nueva entidad {@link Usuario} a partir del DTO de creación.
     * El estado inicial es siempre {@link EstadoUsuario#ACTIVO}.
     * Los campos {@code id}, {@code fechaCreacion} y {@code fechaModificacion} no se asignan
     * aquí; son gestionados por JPA Auditing.
     *
     * @param request         DTO con los datos del nuevo usuario
     * @param passwordCifrado hash BCrypt ya codificado (nunca la contraseña en texto plano)
     * @return entidad lista para persistir
     */
    public static Usuario toEntity(UsuarioRequest request, String passwordCifrado) {
        return Usuario.builder()
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordCifrado)
                .estado(EstadoUsuario.ACTIVO)
                .build();
    }
}
