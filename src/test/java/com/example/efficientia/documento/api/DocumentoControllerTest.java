package com.example.efficientia.documento.api;

import com.example.efficientia.cadastrobase.api.CadastroBaseExceptionHandler;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.PapelAssinante;
import com.example.efficientia.documento.domain.TipoDocumento;
import com.example.efficientia.documento.service.DocumentoService;
import com.example.efficientia.documento.service.DocumentoConteudo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentoController.class)
@Import({
        DocumentoControllerTest.TestDependencies.class,
        DocumentoExceptionHandler.class,
        CadastroBaseExceptionHandler.class
})
class DocumentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentoService service;

    @Test
    void deveCriarDocumentoMultipartComLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.criarComArquivo(any(), any(), any())).thenReturn(response(id));

        mockMvc.perform(multipart("/api/v1/documentos")
                        .file(metadadosValidos())
                        .file(pdfValido())
                        .header("Idempotency-Key", UUID.randomUUID()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/documentos/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.conteudoUrl").value("/api/v1/documentos/" + id + "/conteudo"))
                .andExpect(jsonPath("$.storageKey").doesNotExist());
    }

    @Test
    void deveRejeitarMetadadosSemCamposObrigatorios() throws Exception {
        var metadados = new MockMultipartFile(
                "metadados",
                "metadados.json",
                "application/json",
                "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/documentos")
                        .file(metadados)
                        .file(pdfValido())
                        .header("Idempotency-Key", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveCriarAssinaturaTextualSemConteudoUrl() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.criarAssinaturaTextual(any(), any())).thenReturn(responseTexto(id));

        mockMvc.perform(post("/api/v1/documentos")
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .content(jsonAssinatura("João da Silva")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/documentos/" + id))
                .andExpect(jsonPath("$.textoAssinatura").value("João da Silva"))
                .andExpect(jsonPath("$.conteudoUrl").isEmpty())
                .andExpect(jsonPath("$.storageKey").doesNotExist());
    }

    @Test
    void deveRejeitarAssinaturaTextualVaziaNoContrato() throws Exception {
        mockMvc.perform(post("/api/v1/documentos")
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .content(jsonAssinatura("   ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarDocumentosComContratoDePagina() throws Exception {
        when(service.listar(any())).thenReturn(new PaginaDocumentosResponse(
                List.of(),
                0,
                20,
                0L,
                0,
                null,
                false
        ));

        mockMvc.perform(get("/api/v1/documentos")
                        .param("page", "0")
                        .param("size", "20")
                        .param("viagemId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void deveRetornar404NaConsultaDeDocumentoInexistente() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscar(id)).thenThrow(
                new com.example.efficientia.documento.exception.DocumentoNaoEncontradoException(id)
        );

        mockMvc.perform(get("/api/v1/documentos/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Documento não encontrado"));
    }

    @Test
    void deveTransmitirConteudoInlineComHeadersPrivados() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] bytes = "%PDF-stream".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(service.buscarConteudo(id)).thenReturn(new DocumentoConteudo(
                new ByteArrayResource(bytes),
                "application/pdf",
                bytes.length,
                "relatório final.pdf"
        ));

        mockMvc.perform(get("/api/v1/documentos/{id}/conteudo", id).param("inline", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Length", String.valueOf(bytes.length)))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("inline")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(bytes));
    }

    @Test
    void deveRetornarConflitoParaAssinaturaTextualSemBinario() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarConteudo(id)).thenThrow(
                new com.example.efficientia.documento.exception.DocumentoSemConteudoException(id)
        );

        mockMvc.perform(get("/api/v1/documentos/{id}/conteudo", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Documento sem conteúdo"));
    }

    @Test
    void deveAtualizarDescricaoPorPatch() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.atualizar(org.mockito.ArgumentMatchers.eq(id), any())).thenReturn(response(id));

        mockMvc.perform(patch("/api/v1/documentos/{id}", id)
                        .contentType(APPLICATION_JSON)
                        .content("{\"versao\":0,\"descricao\":\"Revisado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void deveExcluirDocumentoCom204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/documentos/{id}", id))
                .andExpect(status().isNoContent());
        Mockito.verify(service).excluir(id);
    }

    private MockMultipartFile metadadosValidos() {
        return new MockMultipartFile(
                "metadados",
                "metadados.json",
                "application/json",
                """
                        {
                          "viagemId": 1,
                          "tipoDocumento": "RELATORIO_VIAGEM",
                          "origem": "UPLOAD",
                          "descricao": "Relatório final"
                        }
                        """.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    private MockMultipartFile pdfValido() {
        return new MockMultipartFile(
                "arquivo",
                "relatorio.pdf",
                "application/pdf",
                "%PDF-1.7\nconteudo".getBytes(java.nio.charset.StandardCharsets.US_ASCII)
        );
    }

    private DocumentoResponse response(UUID id) {
        Instant agora = Instant.parse("2026-08-31T12:00:00Z");
        return new DocumentoResponse(
                id,
                1,
                TipoDocumento.RELATORIO_VIAGEM,
                OrigemDocumento.UPLOAD,
                null,
                null,
                null,
                null,
                "Relatório final",
                "relatorio.pdf",
                "application/pdf",
                18L,
                "a".repeat(64),
                null,
                agora,
                agora,
                0L,
                "/api/v1/documentos/" + id + "/conteudo"
        );
    }

    private DocumentoResponse responseTexto(UUID id) {
        Instant agora = Instant.parse("2026-08-31T12:00:00Z");
        return new DocumentoResponse(
                id,
                1,
                TipoDocumento.ASSINATURA,
                OrigemDocumento.TEXTO,
                2,
                PapelAssinante.MOTORISTA,
                ModalidadeAssinatura.TEXTO,
                "João da Silva",
                "Assinatura acessível",
                null,
                null,
                null,
                null,
                null,
                agora,
                agora,
                0L,
                null
        );
    }

    private String jsonAssinatura(String texto) {
        return """
                {
                  "viagemId": 1,
                  "tipoDocumento": "ASSINATURA",
                  "origem": "TEXTO",
                  "assinanteId": 2,
                  "papelAssinante": "MOTORISTA",
                  "modalidadeAssinatura": "TEXTO",
                  "textoAssinatura": "%s",
                  "descricao": "Assinatura acessível"
                }
                """.formatted(texto);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean
        DocumentoService documentoService() {
            return Mockito.mock(DocumentoService.class);
        }
    }
}
