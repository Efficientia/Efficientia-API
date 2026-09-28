package com.example.efficientia.auth.service;

import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.empresa.domain.Empresa;
import com.example.efficientia.empresa.domain.EmpresaAdmin;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTest {

    @Test
    @DisplayName("Deve gerar token JWT para UsuarioEntity com sucesso")
    void deveGerarTokenUsuarioComSucesso() throws ParseException {
        JwtTokenService service = new JwtTokenService("chave-secreta-de-teste-muito-segura-e-longa-32bytes!");
        
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(100);
        usuario.setNome("Joao Silva");
        usuario.setEmail("joao@teste.com");
        usuario.setCpf("12345678901");
        usuario.setCodigoInterno("COD123");
        usuario.setTipo(TipoUsuario.motorista);

        String token = service.gerarToken(usuario);
        assertNotNull(token);

        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        assertEquals("100", claims.getSubject());
        assertEquals("efficientia-api", claims.getIssuer());
        assertTrue(claims.getAudience().contains("efficientia-api"));
        assertEquals("Joao Silva", claims.getStringClaim("nome"));
        assertEquals("MOTORISTA", claims.getStringListClaim("roles").get(0));
    }

    @Test
    @DisplayName("Deve gerar token JWT para EmpresaAdmin com sucesso")
    void deveGerarTokenAdminComSucesso() throws ParseException {
        JwtTokenService service = new JwtTokenService("chave-secreta-de-teste-muito-segura-e-longa-32bytes!");

        Empresa empresa = new Empresa(1L, "FRI123", "Friboi", "12345678000199", "friboi@teste.com", null, true, Instant.now(), Instant.now());
        EmpresaAdmin admin = new EmpresaAdmin(
                50L, 1L, "FRI123", "12345678000199", "Admin Friboi",
                "admin@friboi.com", "11122233344", null, "Gerente",
                "hash", true, Instant.now(), Instant.now()
        );

        String token = service.gerarTokenAdmin(admin, empresa);
        assertNotNull(token);

        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        assertEquals("50", claims.getSubject());
        assertEquals("efficientia-api", claims.getIssuer());
        assertTrue(claims.getAudience().contains("efficientia-api"));
        assertEquals("Admin Friboi", claims.getStringClaim("nome"));
        assertEquals("ADMIN", claims.getStringListClaim("roles").get(0));
        assertEquals("ADMIN", claims.getStringClaim("role"));
        assertEquals(1L, claims.getLongClaim("empresa_id"));
    }

    @Test
    @DisplayName("Deve forçar chave default se chave configurada for muito curta")
    void deveForcarChaveDefaultSeCurta() {
        JwtTokenService service = new JwtTokenService("curta");
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(1);
        usuario.setTipo(TipoUsuario.motorista);
        
        // Se usasse "curta" ia dar erro no MACSigner por ter menos de 256 bits, mas o construtor forçou a chave longa padrão
        assertDoesNotThrow(() -> service.gerarToken(usuario));
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException se erro ocorrer ao gerar token usuário")
    void deveLancarExceptionGerarTokenUsuario() {
        JwtTokenService service = new JwtTokenService("chave-secreta-de-teste-muito-segura-e-longa-32bytes!");
        // Passando null para forçar NullPointerException interno
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.gerarToken(null));
        assertTrue(ex.getMessage().contains("Erro ao gerar token JWT"));
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException se erro ocorrer ao gerar token admin")
    void deveLancarExceptionGerarTokenAdmin() {
        JwtTokenService service = new JwtTokenService("chave-secreta-de-teste-muito-segura-e-longa-32bytes!");
        // Passando null para forçar erro interno
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.gerarTokenAdmin(null, null));
        assertTrue(ex.getMessage().contains("Erro ao gerar token JWT de administrador"));
    }
}
