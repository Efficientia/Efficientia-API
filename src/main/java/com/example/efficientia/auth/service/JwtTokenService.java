package com.example.efficientia.auth.service;

import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.empresa.domain.Empresa;
import com.example.efficientia.empresa.domain.EmpresaAdmin;
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
    private final String issuer;
    private final String audience;

    @org.springframework.beans.factory.annotation.Autowired
    public JwtTokenService(
            @Value("${app.security.secret:${app.jwt.secret:${JWT_SECRET:efficientia-secret-key-must-be-at-least-32-bytes-long!}}}") String secret,
            @Value("${app.security.issuer:${JWT_ISSUER_URI:efficientia-api}}") String issuer,
            @Value("${app.security.audience:${JWT_AUDIENCE:efficientia-api}}") String audience
    ) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            this.secret = "efficientia-secret-key-must-be-at-least-32-bytes-long!";
        } else {
            this.secret = secret;
        }
        this.issuer = (issuer == null || issuer.isBlank()) ? "efficientia-api" : issuer.strip();
        this.audience = (audience == null || audience.isBlank()) ? "efficientia-api" : audience.strip();
    }

    public JwtTokenService(String secret) {
        this(secret, "efficientia-api", "efficientia-api");
    }

    public String gerarToken(UsuarioEntity usuario) {
        try {
            Instant agora = Instant.now();
            Instant expiracao = agora.plusSeconds(86400); // 24h

            String roleName = usuario.getTipo().name().toUpperCase();

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(usuario.getId()))
                    .issuer(issuer)
                    .audience(List.of(audience))
                    .issueTime(Date.from(agora))
                    .expirationTime(Date.from(expiracao))
                    .claim("usuario_id", usuario.getId())
                    .claim("nome", usuario.getNome())
                    .claim("email", usuario.getEmail())
                    .claim("cpf", usuario.getCpf())
                    .claim("codigo_interno", usuario.getCodigoInterno())
                    .claim("roles", List.of(roleName));

            if (usuario.getEmpresaId() != null) {
                claimsBuilder.claim("empresa_id", usuario.getEmpresaId());
            }

            JWTClaimsSet claimsSet = claimsBuilder.build();

            JWSSigner signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar token JWT de autenticação", e);
        }
    }

    public String gerarTokenAdmin(EmpresaAdmin admin, Empresa empresa) {
        try {
            Instant agora = Instant.now();
            Instant expiracao = agora.plusSeconds(86400); // 24h

            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(admin.getId()))
                    .issuer(issuer)
                    .audience(List.of(audience))
                    .issueTime(Date.from(agora))
                    .expirationTime(Date.from(expiracao))
                    .claim("admin_id", admin.getId())
                    .claim("usuario_id", admin.getId())
                    .claim("nome", admin.getNome())
                    .claim("email", admin.getEmail())
                    .claim("empresa_id", empresa.getId())
                    .claim("codigo_empresa", empresa.getCodigoEmpresa())
                    .claim("cnpj", empresa.getCnpj())
                    .claim("nome_empresa", empresa.getNomeEmpresa())
                    .claim("roles", List.of("ADMIN"))
                    .claim("role", "ADMIN")
                    .claim("tipo", "ADMINISTRADOR_EMPRESA");

            if (admin.getCpf() != null && !admin.getCpf().isBlank()) {
                builder.claim("cpf", admin.getCpf());
            }
            if (admin.getCargo() != null && !admin.getCargo().isBlank()) {
                builder.claim("cargo", admin.getCargo());
            }

            JWSSigner signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar token JWT de administrador", e);
        }
    }
}
