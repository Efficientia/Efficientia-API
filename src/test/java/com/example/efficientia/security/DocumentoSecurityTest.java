package com.example.efficientia.security;

import com.example.efficientia.documento.api.DocumentoController;
import com.example.efficientia.documento.api.DocumentoExceptionHandler;
import com.example.efficientia.documento.api.DocumentoResponse;
import com.example.efficientia.documento.api.PaginaDocumentosResponse;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.TipoDocumento;
import com.example.efficientia.documento.service.DocumentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DocumentoController.class,
        properties = {
                "app.security.enabled=true",
                "app.security.audience=efficientia-api",
                "app.security.jwk-set-uri=http://localhost.invalid/jwks",
                "app.security.allowed-origins=http://localhost:5173"
        }
)
@Import({SecurityConfig.class, DocumentoExceptionHandler.class, DocumentoSecurityTest.Dependencies.class})
@EnableConfigurationProperties(SecurityProperties.class)
class DocumentoSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentoService service;

    @BeforeEach
    void resetService() {
        Mockito.reset(service);
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/api/v1/documentos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void devePermitirConsultaAoMotoristaAutenticado() throws Exception {
        when(service.listar(any())).thenReturn(new PaginaDocumentosResponse(
                List.of(), 0, 20, 0L, 0, null, false
        ));

        mockMvc.perform(get("/api/v1/documentos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))))
                .andExpect(status().isOk());
    }

    @Test
    void deveNegarPatchAoMotorista() throws Exception {
        mockMvc.perform(patch("/api/v1/documentos/{id}", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versao\":0,\"descricao\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void devePermitirPatchAoFuncionarioFriboi() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.atualizar(eq(id), any())).thenReturn(response(id));

        mockMvc.perform(patch("/api/v1/documentos/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_FUNCIONARIO_FRIBOI")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versao\":0,\"descricao\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deveAceitarPreflightSomenteDaOrigemConfigurada() throws Exception {
        mockMvc.perform(options("/api/v1/documentos")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173"));
    }

    @Test
    void deveConverterNegacaoDoEscopoDoServiceEm403() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscar(id)).thenThrow(new AccessDeniedException("fora do escopo"));

        mockMvc.perform(get("/api/v1/documentos/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))))
                .andExpect(status().isForbidden());
    }

    private DocumentoResponse response(UUID id) {
        Instant now = Instant.parse("2026-08-31T12:00:00Z");
        return new DocumentoResponse(
                id, 1, TipoDocumento.RELATORIO_VIAGEM, OrigemDocumento.UPLOAD,
                null, null, null, null, "x", "arquivo.pdf", "application/pdf",
                10L, "a".repeat(64), 7, now, now, 1L,
                "/api/v1/documentos/" + id + "/conteudo"
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dependencies {

        @Bean
        DocumentoService documentoService() {
            return Mockito.mock(DocumentoService.class);
        }
    }

}
