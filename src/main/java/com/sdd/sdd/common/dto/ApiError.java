package com.sdd.sdd.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Estructura estandar de error devuelta por la API")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    @Schema(description = "Fecha y hora del error", example = "2026-01-15T10:30:00-05:00")
    private OffsetDateTime timestamp;

    @Schema(description = "Codigo HTTP del error", example = "404")
    private int status;

    @Schema(description = "Descripcion HTTP del error", example = "Not Found")
    private String error;

    @Schema(description = "Mensaje descriptivo del error", example = "Usuario con id 99 no encontrado.")
    private String mensaje;

    @Schema(description = "URI del endpoint que genero el error", example = "/api/usuarios/99")
    private String detalle;

    @Schema(description = "Lista de errores de validacion de campos. Solo presente en respuestas HTTP 400.")
    private List<CampoError> errores;

    @Schema(description = "Error de validacion de un campo especifico")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampoError {

        @Schema(description = "Nombre del campo con error", example = "email")
        private String campo;

        @Schema(description = "Mensaje de validacion del campo", example = "must be a well-formed email address")
        private String mensaje;
    }
}