package com.sdd.sdd.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String mensaje;
    private String detalle;

    /** Presente solo en errores de validación (HTTP 400). Null en los demás casos. */
    private List<CampoError> errores;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampoError {
        private String campo;
        private String mensaje;
    }
}
