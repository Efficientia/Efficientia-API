package com.example.efficientia.auth.service;

import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenService {

    private final String secret;

    public JwtTokenService(@Value("${app.jwt.secret:efficientia-secret-key-must-be-at-least-32-bytes-long!}") String secret) {
        // Garantir que a chave tenha pelo menos 256 bits (32 bytes) para HMAC-SHA256
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            this.secret = "efficientia-secret-key-must-be-at-least-32-bytes-long!";
        } else {
            this.secret = secret;
        }
    }

    public String gerarToken(UsuarioEntity usuario) {
        try {
            Instant agora = Instant.now();
            Instant expiracao = agora.plusSeconds(86400); // 24h

            String roleName = usuario.getTipo().name().toUpperCase();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(usuario.getId()))
                    .issuer("efficientia-api")
                    .issueTime(Date.from(agora))
                    .expirationTime(Date.from(expiracao))
                    .claim("usuario_id", usuario.getId())
                    .claim("nome", usuario.getNome())
                    .claim("email", usuario.getEmail())
                    .claim("cpf", usuario.getCpf())
                    .claim("codigo_interno", usuario.getCodigoInterno())
                    .claim("roles", List.of(roleName))
                    .build();

            JWSSigner signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar token JWT de autenticação", e);
        }
    }
}
