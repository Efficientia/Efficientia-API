package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.relatorioviagem.service.RelatorioViagemService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RelatorioViagemController.class)
@Import(RelatorioViagemControllerTest.TestDependencies.class)
class RelatorioViagemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RelatorioViagemService service;

    @Test
    void deveListarRelatoriosComPaginacao() throws Exception {
        when(service.listar(eq(0), eq(20)))
                .thenReturn(new RelatorioViagemPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/relatorios-viagem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isArray())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void deveRejeitarCriacaoSemCamposObrigatorios() throws Exception {
        mockMvc.perform(post("/api/v1/relatorios-viagem")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean
        RelatorioViagemService relatorioViagemService() {
            return Mockito.mock(RelatorioViagemService.class);
        }
    }
}
