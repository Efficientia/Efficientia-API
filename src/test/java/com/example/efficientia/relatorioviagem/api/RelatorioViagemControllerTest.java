package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.relatorioviagem.service.RelatorioViagemService;
import com.example.efficientia.caminhao.service.CaminhaoService;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoRelatorioResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.VincularCaminhaoRelatorioRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    @Autowired
    private CaminhaoService caminhaoService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        Mockito.reset(service, caminhaoService);
    }

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
    void deveBuscarCaminhaoDoRelatorio() throws Exception {
        CaminhaoRelatorioResponse relResponse = new CaminhaoRelatorioResponse(
                1, "rascunho", 10, "Motorista Teste", null, null, "ABC1D23", "XYZ9W87", true
        );
        when(caminhaoService.buscarCaminhaoDoRelatorio(1)).thenReturn(relResponse);

        mockMvc.perform(get("/api/v1/relatorios-viagem/1/caminhao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatorioId").value(1))
                .andExpect(jsonPath("$.placaCavalo").value("ABC1D23"))
                .andExpect(jsonPath("$.placaCarreta").value("XYZ9W87"));
    }

    @Test
    void deveVincularCaminhaoAoRelatorio() throws Exception {
        CaminhaoRelatorioResponse relResponse = new CaminhaoRelatorioResponse(
                1, "rascunho", 10, "Motorista Teste", null, null, "ABC1D23", "XYZ9W87", true
        );
        when(caminhaoService.vincularCaminhaoAoRelatorio(eq(1), any(VincularCaminhaoRelatorioRequest.class)))
                .thenReturn(relResponse);

        mockMvc.perform(post("/api/v1/relatorios-viagem/1/vincular-caminhao")
                        .contentType(APPLICATION_JSON)
                        .content("{\"placaCavalo\": \"ABC1D23\", \"placaCarreta\": \"XYZ9W87\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placaCavalo").value("ABC1D23"));
    }

    @Test
    void deveFinalizarRelatorioViagem() throws Exception {
        when(service.finalizar(1)).thenReturn(Mockito.mock(RelatorioViagemResponse.class));

        mockMvc.perform(patch("/api/v1/relatorios-viagem/1/finalizar"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRegistrarAssinaturasComSucesso() throws Exception {
        when(service.registrarAssinaturas(eq(1), any(RegistrarAssinaturasRequest.class)))
                .thenReturn(Mockito.mock(RelatorioViagemResponse.class));

        mockMvc.perform(patch("/api/v1/relatorios-viagem/1/assinaturas")
                        .contentType(APPLICATION_JSON)
                        .content("{\"urlAssinaturaPecuarista\": \"https://storage/pecuarista.png\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRegistrarAssinaturaPapelComSucesso() throws Exception {
        when(service.registrarAssinaturaPapel(eq(1), eq("MOTORISTA"), any()))
                .thenReturn(Mockito.mock(RelatorioViagemResponse.class));

        mockMvc.perform(post("/api/v1/relatorios-viagem/1/assinar-papel")
                        .param("papel", "MOTORISTA")
                        .param("urlAssinatura", "https://storage/motorista.png"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar422QuandoAssinaturasIncompletasAoFinalizar() throws Exception {
        when(service.finalizar(1))
                .thenThrow(new AssinaturasIncompletasException(1, 4, 2, List.of("Manobrista", "Curraleiro")));

        mockMvc.perform(patch("/api/v1/relatorios-viagem/1/finalizar"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Assinaturas Obrigatórias Incompletas"))
                .andExpect(jsonPath("$.relatorioId").value(1))
                .andExpect(jsonPath("$.qtdObrigatoria").value(4))
                .andExpect(jsonPath("$.qtdColetadas").value(2))
                .andExpect(jsonPath("$.papeisFaltantes[0]").value("Manobrista"))
                .andExpect(jsonPath("$.papeisFaltantes[1]").value("Curraleiro"));
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

        @Bean
        CaminhaoService caminhaoService() {
            return Mockito.mock(CaminhaoService.class);
        }
    }
}
