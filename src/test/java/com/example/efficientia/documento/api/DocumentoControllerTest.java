package com.example.efficientia.documento.api;

import com.example.efficientia.cadastrobase.api.CadastroBaseExceptionHandler;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.TipoDocumento;
import com.example.efficientia.documento.service.DocumentoService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

    @TestConfiguration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean
        DocumentoService documentoService() {
            return Mockito.mock(DocumentoService.class);
        }
    }
}
