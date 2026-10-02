package com.example.efficientia.cadastrobase.api;

import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CavaloResponse;
import com.example.efficientia.cadastrobase.service.CadastroBaseService;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CavaloDetalhadoResponse;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CarretaDetalhadaResponse;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import static org.mockito.ArgumentMatchers.eq;
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
    void deveListarCavalosMecanicos() throws Exception {
        when(service.listarCavalos()).thenReturn(List.of(
                new CavaloDetalhadoResponse(1, "TST1A23", true, null, null, 1000, "Scania", "R450", 2022)
        ));

        mockMvc.perform(get("/api/v1/veiculos/cavalos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placa").value("TST1A23"));
    }

    @Test
    void deveAtualizarCavaloMecanico() throws Exception {
        when(service.atualizarCavalo(eq(1), any())).thenReturn(
                new CavaloDetalhadoResponse(1, "TST1A23", true, null, null, 1500, "Scania", "R450", 2022)
        );

        mockMvc.perform(put("/api/v1/veiculos/cavalos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kmAcumulado\": 1500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kmAcumulado").value(1500));
    }

    @Test
    void deveRemoverCavaloMecanico() throws Exception {
        mockMvc.perform(delete("/api/v1/veiculos/cavalos/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveListarCarretas() throws Exception {
        when(service.listarCarretas()).thenReturn(List.of(
                new CarretaDetalhadaResponse(1, "CAR1A23", 45, true, null, null, "Randon", "Boiadeira", "Gaiola")
        ));

        mockMvc.perform(get("/api/v1/veiculos/carretas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placa").value("CAR1A23"));
    }

    @Test
    void deveRemoverCarreta() throws Exception {
        mockMvc.perform(delete("/api/v1/veiculos/carretas/1"))
                .andExpect(status().isNoContent());
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
