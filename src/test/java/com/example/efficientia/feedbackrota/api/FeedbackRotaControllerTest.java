package com.example.efficientia.feedbackrota.api;

import com.example.efficientia.feedbackrota.service.FeedbackRotaService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackRotaController.class)
@AutoConfigureMockMvc(addFilters = false) // Ignore security filters for unit test to focus on controller logic
@Import(FeedbackRotaControllerTest.TestDependencies.class)
class FeedbackRotaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FeedbackRotaService service;

    @TestConfiguration
    static class TestDependencies {
        @Bean
        public FeedbackRotaService feedbackRotaService() {
            return Mockito.mock(FeedbackRotaService.class);
        }
    }

    @Test
    @WithMockUser
    void deveEnviarFeedbackERetornarOk() throws Exception {
        String jsonPayload = """
                {
                  "rotaId": "%s",
                  "avaliacao": "OTIMA",
                  "motivos": ["ENTRADA", "ESPERA_NA_FAZENDA"],
                  "comentario": "Muito bom"
                }
                """.formatted(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/feedbacks-rota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload)
                        .with(csrf()))
                .andExpect(status().isOk());

        Mockito.verify(service, Mockito.times(1)).enviarFeedback(Mockito.any(FeedbackRotaRequest.class), Mockito.any());
    }

    @Test
    @WithMockUser
    void deveRetornarBadRequestQuandoFaltaRotaId() throws Exception {
        String jsonPayload = """
                {
                  "avaliacao": "OTIMA",
                  "motivos": ["ENTRADA"],
                  "comentario": "Faltou a rota"
                }
                """;

        mockMvc.perform(post("/api/v1/feedbacks-rota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}