package com.sdd.sdd.usuario.dto;

import com.sdd.sdd.usuario.entity.EstadoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Datos permitidos para actualizar un usuario existente. El username no es modificable.")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioUpdateRequest {

    @Schema(description = "Nuevos nombres del usuario", example = "Juan Carlos Modificado", maxLength = 100)
    @NotBlank
    @Size(max = 100)
    private String nombres;

    @Schema(description = "Nuevos apellidos del usuario", example = "Perez Modificado", maxLength = 100)
    @NotBlank
    @Size(max = 100)
    private String apellidos;

    @Schema(description = "Nuevo correo electronico (debe ser unico en el sistema)", example = "nuevo@example.com", maxLength = 150)
    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    @Schema(description = "Estado del usuario", example = "ACTIVO", allowableValues = {"ACTIVO", "INACTIVO"})
    @NotNull
    private EstadoUsuario estado;
}