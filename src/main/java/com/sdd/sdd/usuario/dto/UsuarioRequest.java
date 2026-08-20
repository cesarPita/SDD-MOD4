package com.sdd.sdd.usuario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Datos requeridos para registrar un nuevo usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioRequest {

    @Schema(description = "Nombres del usuario", example = "Juan Carlos", maxLength = 100)
    @NotBlank
    @Size(max = 100)
    private String nombres;

    @Schema(description = "Apellidos del usuario", example = "Perez Garcia", maxLength = 100)
    @NotBlank
    @Size(max = 100)
    private String apellidos;

    @Schema(description = "Nombre de usuario unico (3-50 caracteres alfanumericos, guion o guion bajo)",
            example = "jperez", minLength = 3, maxLength = 50)
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,50}$")
    private String username;

    @Schema(description = "Correo electronico unico del usuario", example = "juan.perez@example.com", maxLength = 150)
    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    @Schema(description = "Contrasena (minimo 8 caracteres). Solo escritura — nunca se devuelve en respuestas.",
            example = "Secreto123", minLength = 8, maxLength = 100, accessMode = Schema.AccessMode.WRITE_ONLY)
    @NotBlank
    @Size(min = 8, max = 100)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}