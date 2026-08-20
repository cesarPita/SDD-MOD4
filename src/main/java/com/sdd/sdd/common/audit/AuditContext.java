package com.sdd.sdd.common.audit;

/**
 * Clase de utilidad que centraliza los campos de auditoria usados en los logs.
 * El usuario se resolvera desde el SecurityContext cuando se implemente JWT.
 *
 * Arquitectura preparada para registrar:
 *   - usuario que realizo la operacion
 *   - operacion realizada
 *   - recurso afectado (tipo + id)
 *   - fecha/hora (provista por el framework via MDC/timestamp)
 *   - resultado de la operacion
 *   - correlationId (provisto automaticamente por MDC via CorrelationIdFilter)
 *
 * TODO (etapa JWT): reemplazar "anonymous" por
 *   SecurityContextHolder.getContext().getAuthentication().getName()
 */
public final class AuditContext {

    private AuditContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Devuelve el identificador del usuario autenticado.
     * Retorna "anonymous" hasta que se integre JWT.
     */
    public static String usuarioActual() {
        // TODO (JWT): return SecurityContextHolder.getContext().getAuthentication().getName();
        return "anonymous";
    }
}