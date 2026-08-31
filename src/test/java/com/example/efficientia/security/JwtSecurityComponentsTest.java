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
