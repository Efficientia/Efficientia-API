package com.example.efficientia.cadastrobase.api;

import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CavaloResponse;
import com.example.efficientia.cadastrobase.service.CadastroBaseService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CadastroBaseController.class)
@Import({CadastroBaseControllerTest.TestDependencies.class, CadastroBaseExceptionHandler.class})
class CadastroBaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CadastroBaseService service;

    @Test
    void deveCriarCavaloMecanico() throws Exception {
        when(service.criarCavalo(any()))
                .thenReturn(new CavaloResponse(1, "TST1A23", true));

        mockMvc.perform(post("/api/v1/veiculos/cavalos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "placa": "TST1A23",
                                  "ativo": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.placa").value("TST1A23"));
    }

    @Test
    void deveRejeitarUsuarioSemCamposObrigatorios() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.cpf").exists())
                .andExpect(jsonPath("$.erros.senha").exists());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean
        CadastroBaseService cadastroBaseService() {
            return Mockito.mock(CadastroBaseService.class);
        }
    }
}
