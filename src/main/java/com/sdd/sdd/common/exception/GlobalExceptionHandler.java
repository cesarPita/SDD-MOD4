package com.sdd.sdd.common.exception;

import com.sdd.sdd.common.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Manejador centralizado de excepciones REST.
 * Genera logs apropiados con correlationId (ya en MDC via CorrelationIdFilter).
 * El stack trace solo se registra en servidor (ERROR), nunca se expone al cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 404 · Recurso no encontrado ──────────────────────────────────────────

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleRecursoNoEncontrado(
            RecursoNoEncontradoException ex, HttpServletRequest request) {

        log.warn("Recurso no encontrado uri={} mensaje={}", request.getRequestURI(), ex.getMessage());

        ApiError error = ApiError.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .mensaje(ex.getMessage())
                .detalle(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ── 409 · Duplicado ───────────────────────────────────────────────────────

    @ExceptionHandler(DuplicadoException.class)
    public ResponseEntity<ApiError> handleDuplicado(
            DuplicadoException ex, HttpServletRequest request) {

        log.warn("Conflicto de datos uri={} mensaje={}", request.getRequestURI(), ex.getMessage());

        ApiError error = ApiError.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .mensaje(ex.getMessage())
                .detalle(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ── 400 · Bean Validation ─────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> campos = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        log.warn("Validacion fallida uri={} campos={}", request.getRequestURI(), campos);

        List<ApiError.CampoError> campoErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiError.CampoError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ApiError error = ApiError.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .mensaje("Validacion fallida.")
                .detalle(request.getRequestURI())
                .errores(campoErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ── 400 · JSON malformado ─────────────────────────────────────────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMensajeNoLegible(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Cuerpo de solicitud no legible uri={}", request.getRequestURI());

        ApiError error = ApiError.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .mensaje("El cuerpo de la solicitud no es legible o esta malformado.")
                .detalle(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ── 500 · Error interno ───────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenerico(
            Exception ex, HttpServletRequest request) {

        // Stack trace solo en servidor, nunca expuesto al cliente
        log.error("Error interno inesperado uri={} tipo={}", request.getRequestURI(),
                ex.getClass().getName(), ex);

        ApiError error = ApiError.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .mensaje("Ocurrio un error interno inesperado.")
                .detalle(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}