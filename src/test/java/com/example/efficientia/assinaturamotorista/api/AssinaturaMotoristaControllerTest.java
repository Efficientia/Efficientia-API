package com.example.efficientia.assinaturamotorista.api;

import com.example.efficientia.assinaturamotorista.api.AssinaturaMotoristaContracts.AssinaturaMetadadosRequest;
import com.example.efficientia.assinaturamotorista.api.AssinaturaMotoristaContracts.AssinaturaMotoristaResponse;
import com.example.efficientia.assinaturamotorista.domain.ModalidadeAssinaturaMotorista;
import com.example.efficientia.assinaturamotorista.service.AssinaturaMotoristaService;
import com.example.efficientia.security.SecurityConfig;
import com.example.efficientia.security.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AssinaturaMotoristaController.class,
        properties = {
                "app.security.enabled=true",
                "app.security.audience=efficientia-api",
                "app.security.secret=efficientia-secret-key-must-be-at-least-32-bytes-long!"
        }
)
@Import({
        SecurityConfig.class,
        AssinaturaMotoristaControllerTest.TestDependencies.class,
        AssinaturaMotoristaExceptionHandler.class
})
@EnableConfigurationProperties(SecurityProperties.class)
class AssinaturaMotoristaControllerTest {

    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssinaturaMotoristaService service;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service);
    }

    private MockMultipartFile criarArquivoValido() {
        return new MockMultipartFile(
                "arquivo",
                "assinatura.png",
                "image/png",
                VALID_PNG_BYTES
        );
    }

    private MockMultipartFile criarMetadadosValidos(ModalidadeAssinaturaMotorista modalidade, String textoOrigem) {
        String json = String.format("""
                {
                    "modalidade": "%s",
                    "textoOrigem": %s
                }
                """,
                modalidade.name(),
                textoOrigem != null ? "\"" + textoOrigem + "\"" : "null"
        );
        return new MockMultipartFile(
                "metadados",
                "metadados.json",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8)
        );
    }

    private AssinaturaMotoristaResponse criarResponsePadrao(UUID id, Integer usuarioId, boolean isMe) {
        String url = isMe ? "/api/v1/usuarios/me/assinatura/conteudo" : "/api/v1/usuarios/" + usuarioId + "/assinatura/conteudo";
        return new AssinaturaMotoristaResponse(
                id,
                usuarioId,
                ModalidadeAssinaturaMotorista.DESENHO.name(),
                "image/png",
                (long) VALID_PNG_BYTES.length,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                url,
                Instant.now(),
                1L
        );
    }

    // 1. PUT /api/v1/usuarios/me/assinatura with ROLE_MOTORISTA, multipart arquivo and metadados, and Idempotency-Key -> 200 OK
    @Test
    @DisplayName("1. PUT /api/v1/usuarios/me/assinatura com ROLE_MOTORISTA, multipart válido e Idempotency-Key: retorna 200 OK")
    void deveCadastrarMinhaAssinaturaComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        AssinaturaMotoristaResponse response = criarResponsePadrao(id, 42, true);

        when(service.salvarOuAtualizarAssinatura(
                eq(42),
                eq(42),
                eq(idempotencyKey),
                any(AssinaturaMetadadosRequest.class),
                any(byte[].class),
                eq(true)
        )).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/usuarios/me/assinatura")
                        .file(criarArquivoValido())
                        .file(criarMetadadosValidos(ModalidadeAssinaturaMotorista.DESENHO, null))
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.usuarioId").value(42))
                .andExpect(jsonPath("$.modalidade").value("DESENHO"))
                .andExpect(jsonPath("$.mimeType").value("image/png"))
                .andExpect(jsonPath("$.conteudoUrl").value("/api/v1/usuarios/me/assinatura/conteudo"))
                .andExpect(jsonPath("$.versao").value(1));
    }

    // 2. PUT /api/v1/usuarios/me/assinatura without Idempotency-Key header -> 400 Bad Request
    @Test
    @DisplayName("2. PUT /api/v1/usuarios/me/assinatura sem cabeçalho Idempotency-Key: retorna 400 Bad Request")
    void deveRetornar400QuandoFaltarIdempotencyKey() throws Exception {
        mockMvc.perform(multipart("/api/v1/usuarios/me/assinatura")
                        .file(criarArquivoValido())
                        .file(criarMetadadosValidos(ModalidadeAssinaturaMotorista.DESENHO, null))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Cabeçalho Obrigatório Ausente"));
    }

    // 3. PUT /api/v1/usuarios/me/assinatura with ROLE_ANALISTA (not a motorista) -> 403 Forbidden
    @Test
    @DisplayName("3. PUT /api/v1/usuarios/me/assinatura com ROLE_ANALISTA (não motorista): retorna 403 Forbidden")
    void deveRetornar403QuandoNaoForMotoristaEmMinhaAssinatura() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();

        mockMvc.perform(multipart("/api/v1/usuarios/me/assinatura")
                        .file(criarArquivoValido())
                        .file(criarMetadadosValidos(ModalidadeAssinaturaMotorista.DESENHO, null))
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ANALISTA"))
                                .jwt(token -> token.claim("usuario_id", 10))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso Negado"));
    }

    // 4. PUT /api/v1/usuarios/me/assinatura without auth -> 401 Unauthorized
    @Test
    @DisplayName("4. PUT /api/v1/usuarios/me/assinatura sem autenticação: retorna 401 Unauthorized")
    void deveRetornar401QuandoNaoAutenticadoEmMinhaAssinatura() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();

        mockMvc.perform(multipart("/api/v1/usuarios/me/assinatura")
                        .file(criarArquivoValido())
                        .file(criarMetadadosValidos(ModalidadeAssinaturaMotorista.DESENHO, null))
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isUnauthorized());
    }

    // 5. GET /api/v1/usuarios/me/assinatura with ROLE_MOTORISTA -> 200 OK
    @Test
    @DisplayName("5. GET /api/v1/usuarios/me/assinatura com ROLE_MOTORISTA: retorna 200 OK")
    void deveBuscarMinhaAssinaturaComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        AssinaturaMotoristaResponse response = criarResponsePadrao(id, 42, true);

        when(service.buscarAssinaturaAtiva(eq(42), eq(true))).thenReturn(response);

        mockMvc.perform(get("/api/v1/usuarios/me/assinatura")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.usuarioId").value(42))
                .andExpect(jsonPath("$.conteudoUrl").value("/api/v1/usuarios/me/assinatura/conteudo"));
    }

    // 6. GET /api/v1/usuarios/me/assinatura when service throws AssinaturaNaoEncontradaException -> 404 Not Found
    @Test
    @DisplayName("6. GET /api/v1/usuarios/me/assinatura quando assinatura não existe: retorna 404 Not Found")
    void deveRetornar404QuandoMinhaAssinaturaNaoEncontrada() throws Exception {
        when(service.buscarAssinaturaAtiva(eq(42), eq(true)))
                .thenThrow(new AssinaturaNaoEncontradaException("Assinatura ativa não encontrada para o motorista."));

        mockMvc.perform(get("/api/v1/usuarios/me/assinatura")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Assinatura Não Encontrada"));
    }

    // 7. GET /api/v1/usuarios/me/assinatura/conteudo with ROLE_MOTORISTA -> 200 OK image/png
    @Test
    @DisplayName("7. GET /api/v1/usuarios/me/assinatura/conteudo com ROLE_MOTORISTA: retorna 200 OK com imagem PNG")
    void deveBuscarMeuConteudoAssinaturaComSucesso() throws Exception {
        when(service.buscarConteudoAssinaturaAtiva(eq(42))).thenReturn(VALID_PNG_BYTES);

        mockMvc.perform(get("/api/v1/usuarios/me/assinatura/conteudo")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"assinatura.png\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(content().bytes(VALID_PNG_BYTES));
    }

    // 8. PUT /api/v1/usuarios/{motoristaId}/assinatura with ROLE_ADMIN -> 200 OK
    @Test
    @DisplayName("8. PUT /api/v1/usuarios/{motoristaId}/assinatura com ROLE_ADMIN: retorna 200 OK")
    void deveCadastrarAssinaturaAdministrativaComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        AssinaturaMotoristaResponse response = criarResponsePadrao(id, 99, false);

        when(service.salvarOuAtualizarAssinatura(
                eq(99),
                eq(1),
                eq(idempotencyKey),
                any(AssinaturaMetadadosRequest.class),
                any(byte[].class),
                eq(false)
        )).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/usuarios/{motoristaId}/assinatura", 99)
                        .file(criarArquivoValido())
                        .file(criarMetadadosValidos(ModalidadeAssinaturaMotorista.DESENHO, null))
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("usuario_id", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.usuarioId").value(99))
                .andExpect(jsonPath("$.conteudoUrl").value("/api/v1/usuarios/99/assinatura/conteudo"));
    }

    // 9. PUT /api/v1/usuarios/{motoristaId}/assinatura with non-admin (ROLE_MOTORISTA) -> 403 Forbidden
    @Test
    @DisplayName("9. PUT /api/v1/usuarios/{motoristaId}/assinatura com não-admin (ROLE_MOTORISTA): retorna 403 Forbidden")
    void deveRetornar403QuandoNaoAdminEmCadastroAdministrativo() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();

        mockMvc.perform(multipart("/api/v1/usuarios/{motoristaId}/assinatura", 99)
                        .file(criarArquivoValido())
                        .file(criarMetadadosValidos(ModalidadeAssinaturaMotorista.DESENHO, null))
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso Negado"));
    }

    // 10. PUT /api/v1/usuarios/{motoristaId}/assinatura when service throws MotoristaInvalidoException -> 422 Unprocessable Entity
    @Test
    @DisplayName("10. PUT /api/v1/usuarios/{motoristaId}/assinatura quando usuário destino não é motorista: retorna 422 Unprocessable Entity")
    void deveRetornar422QuandoMotoristaInvalido() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();

        when(service.salvarOuAtualizarAssinatura(
                eq(99),
                eq(1),
                eq(idempotencyKey),
                any(AssinaturaMetadadosRequest.class),
                any(byte[].class),
                eq(false)
        )).thenThrow(new MotoristaInvalidoException("O usuário de destino não possui perfil de motorista."));

        mockMvc.perform(multipart("/api/v1/usuarios/{motoristaId}/assinatura", 99)
                        .file(criarArquivoValido())
                        .file(criarMetadadosValidos(ModalidadeAssinaturaMotorista.DESENHO, null))
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("usuario_id", 1))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Perfil de Motorista Inválido"));
    }

    // 11. GET /api/v1/usuarios/{usuarioId}/assinatura with ROLE_ADMIN -> 200 OK
    @Test
    @DisplayName("11. GET /api/v1/usuarios/{usuarioId}/assinatura com ROLE_ADMIN: retorna 200 OK")
    void deveBuscarAssinaturaPorIdComAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        AssinaturaMotoristaResponse response = criarResponsePadrao(id, 99, false);

        when(service.buscarAssinaturaAtiva(eq(99), eq(false))).thenReturn(response);

        mockMvc.perform(get("/api/v1/usuarios/{usuarioId}/assinatura", 99)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("usuario_id", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.usuarioId").value(99))
                .andExpect(jsonPath("$.conteudoUrl").value("/api/v1/usuarios/99/assinatura/conteudo"));
    }

    // 12. GET /api/v1/usuarios/{usuarioId}/assinatura with ROLE_MOTORISTA (terceiro) -> 403 Forbidden
    @Test
    @DisplayName("12. GET /api/v1/usuarios/{usuarioId}/assinatura com ROLE_MOTORISTA (terceiro): retorna 403 Forbidden")
    void deveRetornar403QuandoMotoristaTentarConsultarAssinaturaDeTerceiro() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/{usuarioId}/assinatura", 99)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso Negado"));
    }

    // 13. GET /api/v1/usuarios/{usuarioId}/assinatura/conteudo with ROLE_ADMIN -> 200 OK with binary PNG
    @Test
    @DisplayName("13. GET /api/v1/usuarios/{usuarioId}/assinatura/conteudo com ROLE_ADMIN: retorna 200 OK com binário PNG")
    void deveBuscarConteudoAssinaturaPorIdComAdmin() throws Exception {
        when(service.buscarConteudoAssinaturaAtiva(eq(99))).thenReturn(VALID_PNG_BYTES);

        mockMvc.perform(get("/api/v1/usuarios/{usuarioId}/assinatura/conteudo", 99)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("usuario_id", 1))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"assinatura.png\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(content().bytes(VALID_PNG_BYTES));
    }

    // 14. PUT /api/v1/usuarios/me/assinatura when service throws AssinaturaFormatoInvalidoException -> 415 Unsupported Media Type
    @Test
    @DisplayName("14. PUT /api/v1/usuarios/me/assinatura quando formato inválido: retorna 415 Unsupported Media Type")
    void deveRetornar415QuandoFormatoInvalido() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();

        when(service.salvarOuAtualizarAssinatura(
                eq(42),
                eq(42),
                eq(idempotencyKey),
                any(AssinaturaMetadadosRequest.class),
                any(byte[].class),
                eq(true)
        )).thenThrow(new AssinaturaFormatoInvalidoException("O arquivo de assinatura deve ser uma imagem PNG válida."));

        mockMvc.perform(multipart("/api/v1/usuarios/me/assinatura")
                        .file(criarArquivoValido())
                        .file(criarMetadadosValidos(ModalidadeAssinaturaMotorista.DESENHO, null))
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.title").value("Formato de Assinatura Inválido"));
    }

    // 15. PUT /api/v1/usuarios/me/assinatura when service throws AssinaturaTamanhoExcedidoException -> 413 Payload Too Large
    @Test
    @DisplayName("15. PUT /api/v1/usuarios/me/assinatura quando tamanho excedido: retorna 413 Payload Too Large")
    void deveRetornar413QuandoTamanhoExcedido() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();

        when(service.salvarOuAtualizarAssinatura(
                eq(42),
                eq(42),
                eq(idempotencyKey),
                any(AssinaturaMetadadosRequest.class),
                any(byte[].class),
                eq(true)
        )).thenThrow(new AssinaturaTamanhoExcedidoException("O arquivo de assinatura não pode exceder 1 MB."));

        mockMvc.perform(multipart("/api/v1/usuarios/me/assinatura")
                        .file(criarArquivoValido())
                        .file(criarMetadadosValidos(ModalidadeAssinaturaMotorista.DESENHO, null))
                        .header("Idempotency-Key", idempotencyKey.toString())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.title").value("Tamanho da Assinatura Excedido"));
    }

    @Test
    @DisplayName("Metadados sem modalidade são rejeitados com 400")
    void deveRejeitarMetadadosSemModalidade() throws Exception {
        MockMultipartFile metadados = new MockMultipartFile(
                "metadados", "metadados.json", MediaType.APPLICATION_JSON_VALUE,
                "{\"textoOrigem\":\"tablet\"}".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/usuarios/me/assinatura")
                        .file(criarArquivoValido())
                        .file(metadados)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Metadados Inválidos"));
    }

    @Test
    @DisplayName("Texto de origem acima do limite é rejeitado com 400")
    void deveRejeitarTextoDeOrigemAcimaDoLimite() throws Exception {
        String texto = "x".repeat(151);
        MockMultipartFile metadados = criarMetadadosValidos(ModalidadeAssinaturaMotorista.NOME_DIGITADO, texto);

        mockMvc.perform(multipart("/api/v1/usuarios/me/assinatura")
                        .file(criarArquivoValido())
                        .file(metadados)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Metadados Inválidos"));
    }

    @Test
    @DisplayName("Parte de metadados ausente retorna erro multipart estável")
    void deveRetornar400QuandoFaltarParteDeMetadados() throws Exception {
        mockMvc.perform(multipart("/api/v1/usuarios/me/assinatura")
                        .file(criarArquivoValido())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parte Multipart Ausente"));
    }

    @Test
    @DisplayName("Arquivo multipart vazio é tratado como formato inválido")
    void deveRejeitarArquivoVazio() throws Exception {
        MockMultipartFile arquivoVazio = new MockMultipartFile("arquivo", "assinatura.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/v1/usuarios/me/assinatura")
                        .file(arquivoVazio)
                        .file(criarMetadadosValidos(ModalidadeAssinaturaMotorista.DESENHO, null))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.title").value("Formato de Assinatura Inválido"));
    }

    @Test
    @DisplayName("Cadastro administrativo aceita o papel ROLE_ADMINISTRADOR")
    void deveAceitarPapelAdministradorAlternativo() throws Exception {
        UUID key = UUID.randomUUID();
        when(service.salvarOuAtualizarAssinatura(eq(99), eq(1), eq(key), any(), any(byte[].class), eq(false)))
                .thenReturn(criarResponsePadrao(UUID.randomUUID(), 99, false));

        mockMvc.perform(multipart("/api/v1/usuarios/{motoristaId}/assinatura", 99)
                        .file(criarArquivoValido())
                        .file(criarMetadadosValidos(ModalidadeAssinaturaMotorista.DESENHO, null))
                        .header("Idempotency-Key", key.toString())
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                                .jwt(token -> token.claim("usuario_id", 1))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Analista pode consultar metadados da assinatura de terceiro")
    void devePermitirConsultaDeTerceiroPorAnalista() throws Exception {
        when(service.buscarAssinaturaAtiva(eq(99), eq(false)))
                .thenReturn(criarResponsePadrao(UUID.randomUUID(), 99, false));

        mockMvc.perform(get("/api/v1/usuarios/{usuarioId}/assinatura", 99)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ANALISTA"))
                                .jwt(token -> token.claim("usuario_id", 7))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Violação de integridade em chave de idempotência retorna 409 Conflict com mensagem explicativa")
    void deveRetornar409QuandoHouverViolacaoDeIntegridade() throws Exception {
        UUID key = UUID.randomUUID();
        when(service.salvarOuAtualizarAssinatura(eq(42), eq(42), eq(key), any(), any(byte[].class), eq(true)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key value violates unique constraint \"idempotency_key\""));

        mockMvc.perform(multipart("/api/v1/usuarios/me/assinatura")
                        .file(criarArquivoValido())
                        .file(criarMetadadosValidos(ModalidadeAssinaturaMotorista.DESENHO, null))
                        .header("Idempotency-Key", key.toString())
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito ao Persistir Assinatura"))
                .andExpect(jsonPath("$.detail").value("Conflito na chave de idempotência informada. Gere uma nova chave para enviar outra assinatura."));
    }

    @Test
    @DisplayName("Reenvio com mesma chave de idempotência retorna 200 OK com a assinatura existente")
    void deveRetornar200EmReenvioComMesmaChaveDeIdempotencia() throws Exception {
        UUID key = UUID.randomUUID();
        when(service.salvarOuAtualizarAssinatura(eq(42), eq(42), eq(key), any(), any(byte[].class), eq(true)))
                .thenReturn(criarResponsePadrao(UUID.randomUUID(), 42, true));

        mockMvc.perform(multipart("/api/v1/usuarios/me/assinatura")
                        .file(criarArquivoValido())
                        .file(criarMetadadosValidos(ModalidadeAssinaturaMotorista.DESENHO, null))
                        .header("Idempotency-Key", key.toString())
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", 42))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(42));
    }

    @Test
    @DisplayName("Funcionário Friboi pode consultar o conteúdo da assinatura de terceiro")
    void devePermitirConteudoDeTerceiroPorFuncionarioFriboi() throws Exception {
        when(service.buscarConteudoAssinaturaAtiva(eq(99))).thenReturn(VALID_PNG_BYTES);

        mockMvc.perform(get("/api/v1/usuarios/{usuarioId}/assinatura/conteudo", 99)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_FUNCIONARIO_FRIBOI"))
                                .jwt(token -> token.claim("usuario_id", 7))))
                .andExpect(status().isOk())
                .andExpect(content().bytes(VALID_PNG_BYTES));
    }

    @Test
    @DisplayName("JWT aceita o identificador do usuário em texto numérico")
    void deveExtrairUsuarioIdDeClaimTexto() throws Exception {
        when(service.buscarAssinaturaAtiva(eq(42), eq(true)))
                .thenReturn(criarResponsePadrao(UUID.randomUUID(), 42, true));

        mockMvc.perform(get("/api/v1/usuarios/me/assinatura")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", " 42 "))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("JWT usa admin_id numérico quando usuario_id não é numérico")
    void deveUsarAdminIdQuandoClaimPrincipalForInvalida() throws Exception {
        when(service.buscarAssinaturaAtiva(eq(12), eq(true)))
                .thenReturn(criarResponsePadrao(UUID.randomUUID(), 12, true));

        mockMvc.perform(get("/api/v1/usuarios/me/assinatura")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", "nao-numero").claim("admin_id", 12))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("JWT usa subject numérico como alternativa de identificador")
    void deveUsarSubjectQuandoClaimsDeUsuarioEstaoAusentes() throws Exception {
        when(service.buscarAssinaturaAtiva(eq(13), eq(true)))
                .thenReturn(criarResponsePadrao(UUID.randomUUID(), 13, true));

        mockMvc.perform(get("/api/v1/usuarios/me/assinatura")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.subject("13"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("JWT sem identificador numérico recebe 401")
    void deveRetornar401QuandoIdentificadorDoJwtNaoForNumerico() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me/assinatura")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.subject("driver"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Erro na Requisição"));
    }

    @Test
    @DisplayName("JWT ignora claims em branco e usa subject numérico")
    void deveIgnorarClaimsEmBranco() throws Exception {
        when(service.buscarAssinaturaAtiva(eq(14), eq(true)))
                .thenReturn(criarResponsePadrao(UUID.randomUUID(), 14, true));

        mockMvc.perform(get("/api/v1/usuarios/me/assinatura")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))
                                .jwt(token -> token.claim("usuario_id", "  ").subject("14"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Falha de leitura do arquivo é convertida em erro de formato")
    void deveConverterFalhaAoLerArquivoEmErroDeFormato() throws Exception {
        MultipartFile arquivo = mock(MultipartFile.class);
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getBytes()).thenThrow(new IOException("erro de leitura"));
        Authentication motorista = autenticacaoJwt("42", List.of(new SimpleGrantedAuthority("ROLE_MOTORISTA")));
        AssinaturaMotoristaController controller = new AssinaturaMotoristaController(service);

        assertThrows(AssinaturaFormatoInvalidoException.class,
                () -> controller.cadastrarMinhaAssinatura(UUID.randomUUID(), arquivo,
                        new AssinaturaMetadadosRequest(ModalidadeAssinaturaMotorista.DESENHO, null), motorista));
    }

    @Test
    @DisplayName("Arquivo nulo é rejeitado antes do armazenamento")
    void deveRejeitarArquivoNulo() {
        Authentication motorista = autenticacaoJwt("42", List.of(new SimpleGrantedAuthority("ROLE_MOTORISTA")));

        assertThrows(AssinaturaFormatoInvalidoException.class,
                () -> new AssinaturaMotoristaController(service).cadastrarMinhaAssinatura(
                        UUID.randomUUID(), null,
                        new AssinaturaMetadadosRequest(ModalidadeAssinaturaMotorista.DESENHO, null), motorista));
    }

    @Test
    @DisplayName("Autenticação obtida do contexto de segurança permite resolver a identidade JWT")
    void deveUsarAutenticacaoJwtDoContextoQuandoParametroNaoVier() {
        when(service.buscarAssinaturaAtiva(eq(17), eq(true)))
                .thenReturn(criarResponsePadrao(UUID.randomUUID(), 17, true));
        SecurityContextHolder.getContext().setAuthentication(autenticacaoJwt("17",
                List.of(new SimpleGrantedAuthority("ROLE_MOTORISTA"))));
        try {
            new AssinaturaMotoristaController(service).buscarMinhaAssinatura(null);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Identidade ausente no parâmetro e contexto anônimo resulta em 401")
    void deveRetornar401QuandoContextoForAnonimo() {
        Authentication anonimo = new UsernamePasswordAuthenticationToken("anonymousUser", "", List.of());
        SecurityContextHolder.getContext().setAuthentication(anonimo);
        try {
            assertThrows(org.springframework.web.server.ResponseStatusException.class,
                    () -> new AssinaturaMotoristaController(service).buscarMinhaAssinatura(null));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Contexto não autenticado não é usado como identidade")
    void deveIgnorarAutenticacaoNaoAutenticadaNoContexto() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("driver", "password"));
        try {
            assertThrows(org.springframework.web.server.ResponseStatusException.class,
                    () -> new AssinaturaMotoristaController(service).buscarMinhaAssinatura(null));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Requisição direta sem autenticação nem contexto retorna 401")
    void deveRetornar401QuandoNaoExisteAutenticacaoDisponivel() {
        SecurityContextHolder.clearContext();
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> new AssinaturaMotoristaController(service).buscarMinhaAssinatura(null));
    }

    @Test
    @DisplayName("Endpoints administrativo e de terceiro rejeitam ausência de autenticação")
    void deveRejeitarEndpointsAdminEServicosDeTerceiroSemAutenticacao() {
        AssinaturaMotoristaController controller = new AssinaturaMotoristaController(service);
        AssinaturaMetadadosRequest metadados = new AssinaturaMetadadosRequest(
                ModalidadeAssinaturaMotorista.DESENHO, null);

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.cadastrarAssinaturaAdministrativa(99, UUID.randomUUID(),
                        criarArquivoValido(), metadados, null));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.buscarAssinaturaPorId(99, null));
    }

    private JwtAuthenticationToken autenticacaoJwt(String subject, List<SimpleGrantedAuthority> authorities) {
        Instant agora = Instant.now();
        Jwt token = Jwt.withTokenValue("controller-test")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(60))
                .build();
        return new JwtAuthenticationToken(token, authorities);
    }

    @TestConfiguration
    static class TestDependencies {
        @Bean
        public AssinaturaMotoristaService assinaturaMotoristaService() {
            return Mockito.mock(AssinaturaMotoristaService.class);
        }
    }
}
