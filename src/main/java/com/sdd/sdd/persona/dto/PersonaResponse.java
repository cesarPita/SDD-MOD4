package com.sdd.sdd.persona.dto;

import com.sdd.sdd.persona.entity.EstadoCivil;
import com.sdd.sdd.persona.entity.Genero;
import com.sdd.sdd.persona.entity.TipoDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO de respuesta para Persona.
 * Incluye todos los campos de {@link PersonaRequest} más los campos de identidad y auditoría.
 * No hay campos de solo escritura.
 */
@Schema(description = "Datos de la persona devueltos por la API.")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonaResponse {

    @Schema(description = "Identificador único de la persona", example = "1")
    private Long id;

    @Schema(description = "Tipo de documento de identidad", example = "CI")
    private TipoDocumento tipoDocumento;

    @Schema(description = "Número de documento de identidad", example = "12345678")
    private String numeroDocumento;

    @Schema(description = "Complemento del documento (puede ser nulo si no aplica)", example = "1A")
    private String complemento;

    @Schema(description = "Fecha de nacimiento de la persona (ISO 8601 fecha)", example = "1990-05-15")
    private LocalDate fechaNacimiento;

    @Schema(description = "Apellido paterno de la persona", example = "García")
    private String apellidoPaterno;

    @Schema(description = "Apellido materno de la persona", example = "López")
    private String apellidoMaterno;

    @Schema(description = "Apellido de esposo/a", example = "Flores")
    private String apellidoEsposo;

    @Schema(description = "Nombres completos de la persona", example = "Juan Carlos")
    private String nombres;

    @Schema(description = "Género de la persona", example = "MASCULINO")
    private Genero genero;

    @Schema(description = "Estado civil de la persona", example = "SOLTERO")
    private EstadoCivil estadoCivil;

    @Schema(description = "Fecha y hora de creación del registro (ISO 8601 con offset)", example = "2026-01-15T10:30:00-05:00")
    private OffsetDateTime fechaCreacion;

    @Schema(description = "Fecha y hora de última modificación (ISO 8601 con offset)", example = "2026-01-20T14:45:00-05:00")
    private OffsetDateTime fechaModificacion;
}
