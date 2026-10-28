package com.example.efficientia.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;

class JwtSecurityComponentsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveConverterPapeisSemAceitarAutoridadeDesconhecida() {
        Jwt jwt = jwt(Map.of(
                "sub", "7",
                "roles", List.of("motorista", "SUPERUSER"),
                "app_metadata", Map.of("role", "FUNCIONARIO_FRIBOI")
        ), List.of("efficientia-api"));

        JwtAuthenticationToken authentication = (JwtAuthenticationToken) new JwtRoleConverter().convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactlyInAnyOrder("ROLE_MOTORISTA", "ROLE_FUNCIONARIO_FRIBOI");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Deve converter todos os papéis válidos do sistema")
    void deveConverterTodosOsPapeisValidosDoSistema() {
        Jwt jwt = jwt(Map.of(
                "sub", "10",
                "roles", List.of("manobrista", "analista", "pecuarista", "curraleiro")
        ), List.of("efficientia-api"));

        JwtAuthenticationToken authentication = (JwtAuthenticationToken) new JwtRoleConverter().convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactlyInAnyOrder("ROLE_MANOBRISTA", "ROLE_ANALISTA", "ROLE_PECUARISTA", "ROLE_CURRALEIRO");
    }

    @Test
    void deveValidarAudience() {
        AudienceValidator validator = new AudienceValidator("efficientia-api");

        assertThat(validator.validate(jwt(Map.of("sub", "7"), List.of("efficientia-api"))).hasErrors())
                .isFalse();
        assertThat(validator.validate(jwt(Map.of("sub", "7"), List.of("outra-api"))).hasErrors())
                .isTrue();
    }

    @Test
    void motoristaNaoDeveAcessarViagemDeOutroUsuario() {
        var repository = mock(com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository.class);
        var policy = new DocumentoAccessPolicy(repository);
        autenticar(12, "MOTORISTA");
        when(repository.existsByIdAndMotoristaId(99, 12)).thenReturn(false);

        assertThatThrownBy(() -> policy.verificarViagem(99))
                .isInstanceOf(AccessDeniedException.class);

        when(repository.existsByIdAndMotoristaId(99, 12)).thenReturn(true);
        assertThatCode(() -> policy.verificarViagem(99)).doesNotThrowAnyException();
    }

    @Test
    void funcionarioDeveAcessarSomenteDocumentoSobSuaResponsabilidade() {
        var policy = new DocumentoAccessPolicy(
                mock(com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository.class)
        );
        autenticar(21, "FUNCIONARIO_FRIBOI");
        var documento = new com.example.efficientia.documento.persistence.DocumentoEntity();
        documento.setCriadoPor(21);

        assertThatCode(() -> policy.verificarDocumento(documento)).doesNotThrowAnyException();

        documento.setCriadoPor(22);
        assertThatThrownBy(() -> policy.verificarDocumento(documento))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deveConfigurarJwtDecoderComChaveSimetricaValida() {
        SecurityConfig config = new SecurityConfig();
        SecurityProperties properties = new SecurityProperties(
                true, "efficientia-api", "", "uma-chave-secreta-muito-segura-e-longa-32bytes!", "efficientia-api", List.of()
        );

        var decoder = config.jwtDecoder(properties);
        assertThat(decoder).isNotNull();
    }

    @Test
    void deveLancarExcecaoQuandoChaveSimetricaForMenorQue32Bytes() {
        SecurityConfig config = new SecurityConfig();
        // Usando reflexão ou valor curto se o record não forçar fallback
        SecurityProperties properties = new SecurityProperties(
                true, "efficientia-api", "", "curta", "efficientia-api", List.of()
        );

        // SecurityProperties já aplica fallback para 32 bytes se nulo/em branco, mas se passar uma string não branca curta:
        assertThatThrownBy(() -> config.jwtDecoder(new SecurityProperties(true, "", "", "1234567890123456789012345678901", "", List.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET deve possuir pelo menos 32 caracteres.");
    }

    @Test
    void bearerTokenAuthenticationEntryPointDeveExecutarParaDiferentesCenarios() throws Exception {
        SecurityConfig config = new SecurityConfig();
        AuthenticationEntryPoint entryPoint = config.bearerTokenAuthenticationEntryPoint();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/relatorios-viagem");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new InvalidBearerTokenException("Token expirado");

        // 1. Sem cabeçalho Authorization
        assertThatCode(() -> entryPoint.commence(request, response, authException)).doesNotThrowAnyException();
        assertThat(response.getStatus()).isEqualTo(401);

        // 2. Com cabeçalho que não é Bearer
        MockHttpServletRequest requestBasic = new MockHttpServletRequest("POST", "/api/v1/relatorios-viagem");
        requestBasic.addHeader(org.springframework.http.HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");
        assertThatCode(() -> entryPoint.commence(requestBasic, new MockHttpServletResponse(), authException)).doesNotThrowAnyException();

        // 3. Com Bearer inválido
        MockHttpServletRequest requestBearer = new MockHttpServletRequest("POST", "/api/v1/relatorios-viagem");
        requestBearer.addHeader(org.springframework.http.HttpHeaders.AUTHORIZATION, "Bearer token.invalido.aqui");
        assertThatCode(() -> entryPoint.commence(requestBearer, new MockHttpServletResponse(), authException)).doesNotThrowAnyException();
    }

    private void autenticar(int usuarioId, String role) {
        Jwt token = jwt(Map.of("sub", String.valueOf(usuarioId), "usuario_id", usuarioId),
                List.of("efficientia-api"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                token,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        ));
    }

    private Jwt jwt(Map<String, Object> claims, List<String> audience) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.parse("2026-08-31T12:00:00Z"))
                .expiresAt(Instant.parse("2026-08-31T13:00:00Z"))
                .audience(audience)
                .claims(values -> values.putAll(claims))
                .build();
    }
}
