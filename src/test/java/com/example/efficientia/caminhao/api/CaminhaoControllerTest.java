package com.example.efficientia.caminhao.api;

import com.example.efficientia.caminhao.api.CaminhaoContracts.AtualizarCaminhaoRequest;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoAppResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoRelatorioResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CriarCaminhaoRequest;
import com.example.efficientia.caminhao.api.CaminhaoContracts.TipoVeiculo;
import com.example.efficientia.caminhao.api.CaminhaoContracts.VincularCaminhaoRelatorioRequest;
import com.example.efficientia.caminhao.service.CaminhaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaminhaoController.class)
@Import(CaminhaoControllerTest.TestDependencies.class)
class CaminhaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaminhaoService service;

    @Test
    @DisplayName("POST /api/v1/caminhoes deve criar caminhão com sucesso")
    void deveCriarCaminhao() throws Exception {
        CaminhaoResponse response = new CaminhaoResponse(
                1, "CAVALO", "ABC1D23", 1, true, LocalDate.now().plusMonths(6),
                10000, null, "Volvo", "FH540", 2022, null, "DISPONIVEL", null, null, null
        );

        when(service.criarCaminhao(any(CriarCaminhaoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/caminhoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "tipo": "CAVALO",
                            "placa": "ABC1D23",
                            "empresaId": 1,
                            "kmAcumulado": 10000,
                            "marca": "Volvo",
                            "modelo": "FH540",
                            "anoFabricacao": 2022,
                            "ativo": true
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tipo").value("CAVALO"))
                .andExpect(jsonPath("$.placa").value("ABC1D23"))
                .andExpect(jsonPath("$.statusUso").value("DISPONIVEL"));
    }

    @Test
    @DisplayName("GET /api/v1/caminhoes deve retornar lista de caminhões")
    void deveListarCaminhoes() throws Exception {
        CaminhaoResponse r1 = new CaminhaoResponse(
                1, "CAVALO", "ABC1D23", 1, true, LocalDate.now().plusMonths(6),
                10000, null, "Volvo", "FH540", 2022, null, "DISPONIVEL", null, null, null
        );
        when(service.listarCaminhoes(eq(1), eq(true), eq("TODOS"))).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/v1/caminhoes?empresaId=1&ativo=true&tipo=TODOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].placa").value("ABC1D23"));
    }

    @Test
    @DisplayName("GET /api/v1/caminhoes/{id} deve buscar por ID")
    void deveBuscarPorId() throws Exception {
        CaminhaoResponse response = new CaminhaoResponse(
                10, "CAVALO", "ABC1D23", 1, true, LocalDate.now().plusMonths(6),
                10000, null, "Volvo", "FH540", 2022, null, "DISPONIVEL", null, null, null
        );
        when(service.buscarCaminhaoPorId(eq("CAVALO"), eq(10))).thenReturn(response);

        mockMvc.perform(get("/api/v1/caminhoes/10?tipo=CAVALO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.placa").value("ABC1D23"));
    }

    @Test
    @DisplayName("GET /api/v1/caminhoes/placa/{placa} deve buscar por placa")
    void deveBuscarPorPlaca() throws Exception {
        CaminhaoResponse response = new CaminhaoResponse(
                10, "CARRETA", "XYZ9W87", 1, true, LocalDate.now().plusMonths(6),
                null, 45, "Randon", "Boiadeira", null, "Boiadeira", "DISPONIVEL", null, null, null
        );
        when(service.buscarCaminhaoPorPlaca("XYZ9W87")).thenReturn(response);

        mockMvc.perform(get("/api/v1/caminhoes/placa/XYZ9W87"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placa").value("XYZ9W87"))
                .andExpect(jsonPath("$.tipo").value("CARRETA"));
    }

    @Test
    @DisplayName("PUT /api/v1/caminhoes/{id} deve atualizar caminhão")
    void deveAtualizarCaminhao() throws Exception {
        CaminhaoResponse response = new CaminhaoResponse(
                10, "CAVALO", "ABC1D23", 1, true, LocalDate.now().plusMonths(6),
                15000, null, "Volvo", "FH540", 2022, null, "DISPONIVEL", null, null, null
        );
        when(service.atualizarCaminhao(eq("CAVALO"), eq(10), any(AtualizarCaminhaoRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/caminhoes/10?tipo=CAVALO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "kmAcumulado": 15000,
                            "ativo": true
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kmAcumulado").value(15000));
    }

    @Test
    @DisplayName("DELETE /api/v1/caminhoes/{id} deve remover caminhão")
    void deveRemoverCaminhao() throws Exception {
        mockMvc.perform(delete("/api/v1/caminhoes/10?tipo=CAVALO"))
                .andExpect(status().isNoContent());

        verify(service).removerCaminhao("CAVALO", 10);
    }

    @Test
    @DisplayName("GET /api/v1/caminhoes/app deve retornar dados enriquecidos para o aplicativo")
    void deveListarParaApp() throws Exception {
        CaminhaoAppResponse appRes = new CaminhaoAppResponse(
                1, "CAVALO", "ABC1D23", null, 12000, "Volvo", "FH540", true,
                true, 25L, "DISPONIVEL", null, null
        );
        when(service.listarCaminhoesApp(eq(1))).thenReturn(List.of(appRes));

        mockMvc.perform(get("/api/v1/caminhoes/app?empresaId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placa").value("ABC1D23"))
                .andExpect(jsonPath("$[0].inspecaoValida").value(true))
                .andExpect(jsonPath("$[0].diasParaVencerInspecao").value(25))
                .andExpect(jsonPath("$[0].statusUso").value("DISPONIVEL"));
    }

    @Test
    @DisplayName("GET /api/v1/caminhoes/disponiveis deve listar apenas disponíveis")
    void deveListarDisponiveis() throws Exception {
        CaminhaoResponse res = new CaminhaoResponse(
                1, "CAVALO", "ABC1D23", 1, true, LocalDate.now().plusMonths(6),
                10000, null, "Volvo", "FH540", 2022, null, "DISPONIVEL", null, null, null
        );
        when(service.listarCaminhoesDisponiveis(eq(1), eq("CAVALO"))).thenReturn(List.of(res));

        mockMvc.perform(get("/api/v1/caminhoes/disponiveis?empresaId=1&tipo=CAVALO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statusUso").value("DISPONIVEL"));
    }

    @Test
    @DisplayName("GET /api/v1/caminhoes/relatorio/{relatorioId} deve retornar caminhão do relatório")
    void deveBuscarCaminhaoDoRelatorio() throws Exception {
        CaminhaoResponse cavalo = new CaminhaoResponse(
                1, "CAVALO", "ABC1D23", 1, true, null, 10000, null, null, null, null, null, "EM_USO", 100, 50, "Joao"
        );
        CaminhaoResponse carreta = new CaminhaoResponse(
                2, "CARRETA", "XYZ9W87", 1, true, null, null, 48, null, null, null, null, "EM_USO", 100, 50, "Joao"
        );
        CaminhaoRelatorioResponse relResponse = new CaminhaoRelatorioResponse(
                100, "pendente", 50, "Joao", cavalo, carreta, "ABC1D23", "XYZ9W87", true
        );

        when(service.buscarCaminhaoDoRelatorio(100)).thenReturn(relResponse);

        mockMvc.perform(get("/api/v1/caminhoes/relatorio/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatorioId").value(100))
                .andExpect(jsonPath("$.placaCavalo").value("ABC1D23"))
                .andExpect(jsonPath("$.placaCarreta").value("XYZ9W87"))
                .andExpect(jsonPath("$.motoristaNome").value("Joao"))
                .andExpect(jsonPath("$.emUso").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/caminhoes/motorista/{motoristaId} deve retornar caminhão ativo do motorista")
    void deveBuscarCaminhaoDoMotorista() throws Exception {
        CaminhaoResponse cavalo = new CaminhaoResponse(
                1, "CAVALO", "ABC1D23", 1, true, null, 10000, null, null, null, null, null, "EM_USO", 100, 50, "Joao"
        );
        CaminhaoResponse carreta = new CaminhaoResponse(
                2, "CARRETA", "XYZ9W87", 1, true, null, null, 48, null, null, null, null, "EM_USO", 100, 50, "Joao"
        );
        CaminhaoRelatorioResponse relResponse = new CaminhaoRelatorioResponse(
                100, "pendente", 50, "Joao", cavalo, carreta, "ABC1D23", "XYZ9W87", true
        );

        when(service.buscarCaminhaoAtivoDoMotorista(50)).thenReturn(relResponse);

        mockMvc.perform(get("/api/v1/caminhoes/motorista/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatorioId").value(100))
                .andExpect(jsonPath("$.placaCavalo").value("ABC1D23"));
    }

    @Test
    @DisplayName("POST /api/v1/caminhoes/relatorio/{relatorioId}/vincular deve vincular caminhão por placas")
    void deveVincularCaminhaoAoRelatorio() throws Exception {
        CaminhaoResponse cavalo = new CaminhaoResponse(
                1, "CAVALO", "ABC1D23", 1, true, null, 10000, null, null, null, null, null, "EM_USO", 100, 50, "Joao"
        );
        CaminhaoResponse carreta = new CaminhaoResponse(
                2, "CARRETA", "XYZ9W87", 1, true, null, null, 48, null, null, null, null, "EM_USO", 100, 50, "Joao"
        );
        CaminhaoRelatorioResponse relResponse = new CaminhaoRelatorioResponse(
                100, "pendente", 50, "Joao", cavalo, carreta, "ABC1D23", "XYZ9W87", true
        );

        when(service.vincularCaminhaoAoRelatorio(eq(100), any(VincularCaminhaoRelatorioRequest.class)))
                .thenReturn(relResponse);

        mockMvc.perform(post("/api/v1/caminhoes/relatorio/100/vincular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "placaCavalo": "ABC1D23",
                            "placaCarreta": "XYZ9W87"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placaCavalo").value("ABC1D23"))
                .andExpect(jsonPath("$.placaCarreta").value("XYZ9W87"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestDependencies {
        @Bean
        CaminhaoService caminhaoService() {
            return Mockito.mock(CaminhaoService.class);
        }
    }
}
