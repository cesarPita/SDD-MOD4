package com.sdd.sdd.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que registra el inicio y la finalizacion de cada peticion HTTP.
 * Registra: metodo, URI, status HTTP y duracion.
 * NO registra: passwords, tokens, cabecera Authorization ni cuerpos de peticion.
 * Se ejecuta despues de CorrelationIdFilter (@Order(2)) para que el correlationId
 * ya este en MDC cuando se generan los logs.
 */
@Component
@Order(2)
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri    = request.getRequestURI();
        long   start  = System.currentTimeMillis();

        log.info("HTTP {} {} - iniciando", method, uri);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int  status   = response.getStatus();

            if (status >= 500) {
                log.error("HTTP {} {} - completado status={} duration={}ms", method, uri, status, duration);
            } else if (status >= 400) {
                log.warn("HTTP {} {} - completado status={} duration={}ms", method, uri, status, duration);
            } else {
                log.info("HTTP {} {} - completado status={} duration={}ms", method, uri, status, duration);
            }
        }
    }
}