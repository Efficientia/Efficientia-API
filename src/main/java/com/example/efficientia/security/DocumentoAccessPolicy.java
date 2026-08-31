package com.example.efficientia.security;

import com.example.efficientia.documento.persistence.DocumentoEntity;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class DocumentoAccessPolicy {

    private final RelatorioViagemRepository viagemRepository;

    public DocumentoAccessPolicy(RelatorioViagemRepository viagemRepository) {
        this.viagemRepository = viagemRepository;
    }

    public DocumentoAccessContext contextoAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return DocumentoAccessContext.todos();
        }
        if (temPapel(authentication, "ADMIN")) {
            return DocumentoAccessContext.todos();
        }

        Integer usuarioId = usuarioId(authentication)
                .orElseThrow(() -> new AccessDeniedException("Token sem identificador de usuário."));
        if (temPapel(authentication, "MOTORISTA")) {
            return new DocumentoAccessContext(usuarioId, DocumentoAccessScope.MOTORISTA);
        }
        if (temPapel(authentication, "FUNCIONARIO_FRIBOI")) {
            return new DocumentoAccessContext(usuarioId, DocumentoAccessScope.RESPONSAVEL);
        }
        throw new AccessDeniedException("Papel sem acesso aos documentos.");
    }

    public Optional<Integer> usuarioAtualId() {
        DocumentoAccessContext contexto = contextoAtual();
        return Optional.ofNullable(contexto.usuarioId());
    }

    public void verificarViagem(Integer viagemId) {
        DocumentoAccessContext contexto = contextoAtual();
        if (contexto.scope() == DocumentoAccessScope.MOTORISTA
                && !viagemRepository.existsByIdAndMotoristaId(viagemId, contexto.usuarioId())) {
            throw new AccessDeniedException("Viagem fora do escopo do motorista.");
        }
    }

    public void verificarDocumento(DocumentoEntity documento) {
        DocumentoAccessContext contexto = contextoAtual();
        boolean permitido = switch (contexto.scope()) {
            case TODOS -> true;
            case MOTORISTA -> viagemRepository.existsByIdAndMotoristaId(
                    documento.getViagemId(), contexto.usuarioId());
            case RESPONSAVEL -> Objects.equals(documento.getCriadoPor(), contexto.usuarioId())
                    || Objects.equals(documento.getAssinanteId(), contexto.usuarioId());
        };
        if (!permitido) {
            throw new AccessDeniedException("Documento fora do escopo do usuário.");
        }
    }

    private boolean temPapel(Authentication authentication, String papel) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + papel));
    }

    private Optional<Integer> usuarioId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Object claim = jwtAuthentication.getToken().getClaim("usuario_id");
            Optional<Integer> value = converterInteiro(claim);
            if (value.isPresent()) {
                return value;
            }
            return converterInteiro(jwtAuthentication.getToken().getSubject());
        }
        return converterInteiro(authentication.getName());
    }

    private Optional<Integer> converterInteiro(Object value) {
        if (value instanceof Number number) {
            return Optional.of(number.intValue());
        }
        if (value instanceof String text && text.matches("[1-9][0-9]*")) {
            try {
                return Optional.of(Integer.parseInt(text));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
