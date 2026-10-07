package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.relatorioviagem.service.RelatorioViagemService;
import com.example.efficientia.caminhao.service.CaminhaoService;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoRelatorioResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.VincularCaminhaoRelatorioRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
        when(service.finalizar(eq(1), isNull())).thenReturn(Mockito.mock(RelatorioViagemResponse.class));

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
        when(service.finalizar(eq(1), isNull()))
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
    void deveAceitarRascunhoSemCamposObrigatorios() throws Exception {
        mockMvc.perform(post("/api/v1/relatorios-viagem")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    @Test
    void deveCriarSubmissaoFinalEncaminhandoChaveDeIdempotencia() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        when(service.criar(any(CriarRelatorioViagemRequest.class), nullable(org.springframework.security.core.Authentication.class), eq(idempotencyKey)))
                .thenReturn(respostaRelatorio());

        mockMvc.perform(post("/api/v1/relatorios-viagem")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"pendente\",\"numeroGta\":\"GTA-42\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("rascunho"));

        verify(service).criar(any(CriarRelatorioViagemRequest.class), nullable(org.springframework.security.core.Authentication.class), eq(idempotencyKey));
    }

    @Test
    void deveRetornar400ComErrosDeCampoQuandoRequestForInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/relatorios-viagem")
                        .contentType(APPLICATION_JSON)
                        .content("{\"motoristaId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Campos Inválidos"))
                .andExpect(jsonPath("$.fieldErrors.motoristaId").value("O ID do motorista deve ser positivo."));

        Mockito.verifyNoInteractions(service);
    }

    @Test
    void deveRetornar409QuandoGtaEstiverDuplicada() throws Exception {
        when(service.criar(any(CriarRelatorioViagemRequest.class), nullable(org.springframework.security.core.Authentication.class), isNull()))
                .thenThrow(new NumeroGtaDuplicadoException("GTA-42"));

        mockMvc.perform(post("/api/v1/relatorios-viagem")
                        .contentType(APPLICATION_JSON)
                        .content("{\"numeroGta\":\"GTA-42\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRetornar400ComFieldErrorsParaInconsistenciaDeNegocio() throws Exception {
        when(service.criar(any(CriarRelatorioViagemRequest.class), nullable(org.springframework.security.core.Authentication.class), isNull()))
                .thenThrow(new ValidacaoDiarioRotaException(
                        "Inconsistência nos dados do diário de rota.",
                        java.util.Map.of("kmChegadaDesembarcadouro", "O quilômetro de chegada não pode ser inferior ao quilômetro de saída.")));

        mockMvc.perform(post("/api/v1/relatorios-viagem")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Inconsistência no Diário de Rota"))
                .andExpect(jsonPath("$.fieldErrors.kmChegadaDesembarcadouro")
                        .value("O quilômetro de chegada não pode ser inferior ao quilômetro de saída."));
    }

    @Test
    void deveBuscarRelatorioPorId() throws Exception {
        when(service.buscar(42)).thenReturn(respostaRelatorio());

        mockMvc.perform(get("/api/v1/relatorios-viagem/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.numeroGta").value("GTA-42"))
                .andExpect(jsonPath("$.status").value("rascunho"));
    }

    @Test
    void deveRetornar404AoBuscarRelatorioInexistente() throws Exception {
        when(service.buscar(404)).thenThrow(new RelatorioViagemNotFoundException(404));

        mockMvc.perform(get("/api/v1/relatorios-viagem/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarRelatorioERepassarChaveDeIdempotencia() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        when(service.atualizar(eq(42), any(CriarRelatorioViagemRequest.class), nullable(org.springframework.security.core.Authentication.class), eq(idempotencyKey)))
                .thenReturn(respostaRelatorio());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/relatorios-viagem/42")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(APPLICATION_JSON)
                        .content("{\"comentarios\":\"Atualização do rascunho\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));

        verify(service).atualizar(eq(42), any(CriarRelatorioViagemRequest.class), nullable(org.springframework.security.core.Authentication.class), eq(idempotencyKey));
    }

    @Test
    void deveRetornar404AoAtualizarRelatorioInexistente() throws Exception {
        when(service.atualizar(eq(404), any(CriarRelatorioViagemRequest.class), nullable(org.springframework.security.core.Authentication.class), isNull()))
                .thenThrow(new RelatorioViagemNotFoundException(404));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/relatorios-viagem/404")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar400QuandoServiceRejeitarAtualizacao() throws Exception {
        when(service.atualizar(eq(42), any(CriarRelatorioViagemRequest.class), nullable(org.springframework.security.core.Authentication.class), isNull()))
                .thenThrow(new IllegalArgumentException("Relatórios aprovados não podem ser alterados."));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/relatorios-viagem/42")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveAtualizarStatusComChaveDeIdempotencia() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        when(service.atualizarStatus(42, "pendente", idempotencyKey)).thenReturn(respostaRelatorio());

        mockMvc.perform(patch("/api/v1/relatorios-viagem/42/status")
                        .param("status", "pendente")
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));

        verify(service).atualizarStatus(42, "pendente", idempotencyKey);
    }

    @Test
    void deveRetornar400ParaStatusInvalido() throws Exception {
        when(service.atualizarStatus(42, "desconhecido", null))
                .thenThrow(new ValidacaoDiarioRotaException("Status inválido.", java.util.Map.of("status", "Status inválido.")));

        mockMvc.perform(patch("/api/v1/relatorios-viagem/42/status").param("status", "desconhecido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").value("Status inválido."));
    }

    @Test
    void deveEnviarRelatorioParaAnaliseComChaveDeIdempotencia() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        when(service.enviarParaAnalise(42, idempotencyKey)).thenReturn(respostaRelatorio());

        mockMvc.perform(patch("/api/v1/relatorios-viagem/42/enviar")
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));

        verify(service).enviarParaAnalise(42, idempotencyKey);
    }

    @Test
    void deveRetornar400QuandoRegistroDeAssinaturasForNulo() throws Exception {
        when(service.registrarAssinaturas(eq(42), any(RegistrarAssinaturasRequest.class)))
                .thenThrow(new IllegalArgumentException("Os dados de assinatura não podem ser nulos."));

        mockMvc.perform(patch("/api/v1/relatorios-viagem/42/assinaturas")
                        .contentType(APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest());
    }

    private RelatorioViagemResponse respostaRelatorio() {
        return new RelatorioViagemResponse(
                42, 12, 18, 2, 3, 4, 5, "GTA-42", "NF-42",
                java.time.LocalDate.of(2026, 10, 7), java.time.LocalTime.of(8, 0),
                java.time.LocalTime.of(8, 30), 100,
                java.time.LocalDate.of(2026, 10, 7), java.time.LocalTime.of(11, 0),
                java.time.LocalTime.of(11, 30), 150,
                "C1", true, 10, 8, 0, 18, 0, 0, 0,
                null, "comentários", null, null, null, null,
                java.time.LocalDateTime.of(2026, 10, 7, 8, 0), "rascunho");
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
