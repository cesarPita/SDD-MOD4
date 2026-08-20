package com.sdd.sdd.common.logging;

import com.sdd.sdd.common.dto.PageResponse;
import com.sdd.sdd.config.SecurityConfig;
import com.sdd.sdd.usuario.dto.UsuarioResponse;
import com.sdd.sdd.usuario.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integracion para {@link CorrelationIdFilter}.
 *
 * Verifica:
 *   - Generacion de correlationId cuando la peticion no lo incluye.
 *   - Reutilizacion del X-Correlation-ID recibido.
 *   - Presencia del header en la respuesta HTTP.
 *   - Limpieza del MDC al finalizar la peticion.
 *   - Unicidad de correlationId entre peticiones.
 */
@WebMvcTest
@Import(SecurityConfig.class)
class CorrelationIdFilterTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UsuarioService usuarioService;

    @BeforeEach
    void stubService() {
        PageResponse<UsuarioResponse> emptyPage =
                new PageResponse<>(Collections.emptyList(), 0, 10, 0L, 0, true);
        when(usuarioService.listar(isNull(), isNull(), isNull(), any())).thenReturn(emptyPage);
    }

    /**
     * Sin X-Correlation-ID entrante: la respuesta incluye un UUID generado.
     */
    @Test
    void sinCorrelationId_seGeneraUUID() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
                .andReturn();

        String correlationId = result.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(correlationId).isNotBlank();
        assertThat(correlationId).matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    /**
     * Con X-Correlation-ID entrante: debe reutilizarse el mismo valor.
     */
    @Test
    void conCorrelationId_seReutilizaElMismo() throws Exception {
        String idEntrante = "test-correlation-abc-123";

        MvcResult result = mockMvc.perform(get("/api/usuarios")
                        .header(CorrelationIdFilter.HEADER_NAME, idEntrante))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, idEntrante))
                .andReturn();

        String idRespuesta = result.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(idRespuesta).isEqualTo(idEntrante);
    }

    /**
     * El MDC debe estar limpio despues de que la peticion finaliza.
     */
    @Test
    void mdcLimpioAlTerminarLaPeticion() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk());

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    /**
     * Cada peticion sin correlationId genera un UUID distinto.
     */
    @Test
    void cadaPeticionGeneraCorrelationIdUnico() throws Exception {
        MvcResult r1 = mockMvc.perform(get("/api/usuarios")).andReturn();
        MvcResult r2 = mockMvc.perform(get("/api/usuarios")).andReturn();

        String id1 = r1.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);
        String id2 = r2.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);

        assertThat(id1).isNotEqualTo(id2);
    }
}