package com.sdd.sdd.persona.dto;

import com.sdd.sdd.persona.entity.EstadoCivil;
import com.sdd.sdd.persona.entity.Genero;
import com.sdd.sdd.persona.entity.TipoDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Schema(description = "Datos requeridos para registrar una nueva persona natural")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonaRequest {

    @Schema(description = "Tipo de documento de identidad", example = "CI")
    @NotNull
    private TipoDocumento tipoDocumento;

    @Schema(description = "Número de documento de identidad (máximo 50 caracteres)", example = "12345678", maxLength = 50)
    @NotBlank
    @Size(max = 50)
    private String numeroDocumento;

    @Schema(description = "Complemento del documento (opcional, máximo 10 caracteres)", example = "1A", maxLength = 10)
    @Size(max = 10)
    private String complemento;

    @Schema(description = "Fecha de nacimiento de la persona (debe ser una fecha pasada)", example = "1990-05-15")
    @NotNull
    @Past
    private LocalDate fechaNacimiento;

    @Schema(description = "Apellido paterno de la persona (opcional)", example = "García")
    private String apellidoPaterno;

    @Schema(description = "Apellido materno de la persona (opcional)", example = "López")
    private String apellidoMaterno;

    @Schema(description = "Apellido de esposo/a (opcional, aplica según estado civil)", example = "Flores")
    private String apellidoEsposo;

    @Schema(description = "Nombres completos de la persona (máximo 200 caracteres)", example = "Juan Carlos", maxLength = 200)
    @NotBlank
    @Size(max = 200)
    private String nombres;

    @Schema(description = "Género de la persona", example = "MASCULINO")
    @NotNull
    private Genero genero;

    @Schema(description = "Estado civil de la persona", example = "SOLTERO")
    @NotNull
    private EstadoCivil estadoCivil;
}
