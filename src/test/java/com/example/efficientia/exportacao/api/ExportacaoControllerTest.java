package com.example.efficientia.exportacao.api;

import com.example.efficientia.documento.api.DocumentoExceptionHandler;
import com.example.efficientia.exportacao.domain.EstadoExportacao;
import com.example.efficientia.exportacao.exception.ExportacaoConflitoException;
import com.example.efficientia.exportacao.exception.ExportacaoInvalidaException;
import com.example.efficientia.exportacao.exception.ExportacaoNaoEncontradaException;
import com.example.efficientia.exportacao.service.ExportacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExportacaoController.class)
@Import({
        ExportacaoControllerTest.TestDependencies.class,
        DocumentoExceptionHandler.class
})
class ExportacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExportacaoService service;

    @BeforeEach
    void limparMockDoService() {
        Mockito.reset(service);
    }

    @Test
    void deveAceitarSolicitacaoComLocationRetryAfterEContratoPublicoSeguro() throws Exception {
        UUID id = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();
        when(service.solicitar(any(), any())).thenReturn(response(id));

        mockMvc.perform(post("/api/v1/exportacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .content(jsonComIds(documentoId)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/v1/exportacoes/" + id))
                .andExpect(header().string("Retry-After", "2"))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.estado").value("NA_FILA"))
                .andExpect(jsonPath("$.quantidadeDocumentos").value(1))
                .andExpect(jsonPath("$.tamanhoOrigemBytes").value(1024))
                .andExpect(jsonPath("$.storageKey").doesNotExist())
                .andExpect(jsonPath("$.nomeArquivo").doesNotExist())
                .andExpect(jsonPath("$.erroCodigo").doesNotExist())
                .andExpect(jsonPath("$.documentoIds").doesNotExist())
                .andExpect(jsonPath("$.idempotencyKey").doesNotExist())
                .andExpect(jsonPath("$.solicitadoPor").doesNotExist());
    }

    @Test
    void deveConsultarStatusDaExportacao() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscar(id)).thenReturn(response(id));

        mockMvc.perform(get("/api/v1/exportacoes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.estado").value("NA_FILA"))
                .andExpect(jsonPath("$.conteudoUrl").isEmpty());
    }

    @Test
    void deveRejeitarListaVaziaNoContratoHttp() throws Exception {
        mockMvc.perform(post("/api/v1/exportacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .content("{\"documentoIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarDocumentoNuloNoContratoHttp() throws Exception {
        mockMvc.perform(post("/api/v1/exportacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .content("{\"documentoIds\":[null]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarHeaderDeIdempotenciaAusente() throws Exception {
        mockMvc.perform(post("/api/v1/exportacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonComIds(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"));
    }

    @Test
    void deveRejeitarMaisDeQuinhentosDocumentosNoContratoHttp() throws Exception {
        UUID[] ids = IntStream.range(0, 501)
                .mapToObj(ignored -> UUID.randomUUID())
                .toArray(UUID[]::new);

        mockMvc.perform(post("/api/v1/exportacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .content(jsonComIds(ids)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar404ComProblemDetailParaExportacaoInexistente() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscar(id)).thenThrow(new ExportacaoNaoEncontradaException(id));

        mockMvc.perform(get("/api/v1/exportacoes/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/problem+json")))
                .andExpect(jsonPath("$.title").value("Exportação não encontrada"))
                .andExpect(jsonPath("$.code").value("EXPORTACAO_NAO_ENCONTRADA"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void deveRetornar409ComProblemDetailParaIdempotenciaConflitante() throws Exception {
        when(service.solicitar(any(), any()))
                .thenThrow(new ExportacaoConflitoException("Chave já usada com outros documentos."));

        mockMvc.perform(post("/api/v1/exportacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .content(jsonComIds(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de exportação"))
                .andExpect(jsonPath("$.code").value("EXPORTACAO_CONFLITO"))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/problem+json")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void deveRetornar422ComProblemDetailParaSolicitacaoInvalida() throws Exception {
        when(service.solicitar(any(), any()))
                .thenThrow(new ExportacaoInvalidaException("Documento sem conteúdo binário."));

        mockMvc.perform(post("/api/v1/exportacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .content(jsonComIds(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Exportação inválida"))
                .andExpect(jsonPath("$.code").value("EXPORTACAO_INVALIDA"))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/problem+json")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.correlationId").exists());
    }

    private ExportacaoResponse response(UUID id) {
        Instant agora = Instant.parse("2026-09-03T10:00:00Z");
        return new ExportacaoResponse(
                id,
                EstadoExportacao.NA_FILA,
                1,
                1024L,
                null,
                agora,
                agora,
                null,
                null,
                null
        );
    }

    private String jsonComIds(UUID... ids) {
        String valores = java.util.Arrays.stream(ids)
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(","));
        return "{\"documentoIds\":[" + valores + "]}";
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean
        ExportacaoService exportacaoService() {
            return Mockito.mock(ExportacaoService.class);
        }
    }
}
