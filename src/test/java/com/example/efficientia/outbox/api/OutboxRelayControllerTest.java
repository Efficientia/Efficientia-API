package com.example.efficientia.outbox.api;

import com.example.efficientia.outbox.service.OutboxRelayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OutboxRelayController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(OutboxRelayControllerTest.TestDependencies.class)
class OutboxRelayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OutboxRelayService outboxRelayService;

    @BeforeEach
    void setUp() {
        clearInvocations(outboxRelayService);
    }

    @TestConfiguration
    static class TestDependencies {
        @Bean
        public OutboxRelayService outboxRelayService() {
            return Mockito.mock(OutboxRelayService.class);
        }
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/outbox/relay com parâmetro padrão retorna 200 e campos esperados")
    void deveExecutarRelayComParametroPadrao() throws Exception {
        when(outboxRelayService.processarEventosPendentes(50)).thenReturn(15);
        when(outboxRelayService.contarPendentes()).thenReturn(35L);

        mockMvc.perform(post("/api/v1/outbox/relay")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processados", is(15)))
                .andExpect(jsonPath("$.pendentesRestantes", is(35)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(outboxRelayService).processarEventosPendentes(50);
        verify(outboxRelayService).contarPendentes();
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/outbox/relay com parâmetro limite customizado repassa para o service")
    void deveExecutarRelayComParametroCustomizado() throws Exception {
        when(outboxRelayService.processarEventosPendentes(100)).thenReturn(25);
        when(outboxRelayService.contarPendentes()).thenReturn(10L);

        mockMvc.perform(post("/api/v1/outbox/relay")
                        .param("limite", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processados", is(25)))
                .andExpect(jsonPath("$.pendentesRestantes", is(10)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(outboxRelayService).processarEventosPendentes(100);
        verify(outboxRelayService).contarPendentes();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/outbox/pendentes retorna 200 com totalPendentes e timestamp")
    void deveContarEventosPendentes() throws Exception {
        when(outboxRelayService.contarPendentes()).thenReturn(7L);

        mockMvc.perform(get("/api/v1/outbox/pendentes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPendentes", is(7)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(outboxRelayService).contarPendentes();
    }
}
