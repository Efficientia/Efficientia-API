package com.example.efficientia.feedbackrota.service;

import com.example.efficientia.feedbackrota.api.FeedbackRotaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class FeedbackRotaService {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackRotaService.class);

    public void enviarFeedback(FeedbackRotaRequest request, Authentication authentication) {
        String motoristaId = extrairId(authentication);
        String empresaId = extrairEmpresaId(authentication);
        String nomeMotorista = extrairNome(authentication);

        // Firebase Admin SDK saving stub
        logger.info("Enviando Feedback de Rota para o Firebase:");
        logger.info("Motorista ID: {}", motoristaId);
        logger.info("Empresa ID: {}", empresaId);
        logger.info("Nome Motorista: {}", nomeMotorista);
        logger.info("Rota ID: {}", request.rotaId());
        logger.info("Avaliacao: {}", request.avaliacao());
        logger.info("Motivos: {}", request.motivos());
        logger.info("Comentario: {}", request.comentario());
    }

    private String extrairId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Object usuarioIdClaim = jwtAuth.getToken().getClaim("usuario_id");
            if (usuarioIdClaim != null) return String.valueOf(usuarioIdClaim);
            return jwtAuth.getToken().getSubject();
        }
        return authentication != null ? authentication.getName() : null;
    }

    private String extrairEmpresaId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Object empresaIdClaim = jwtAuth.getToken().getClaim("empresa_id");
            if (empresaIdClaim != null) return String.valueOf(empresaIdClaim);
        }
        return null;
    }

    private String extrairNome(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Object nomeClaim = jwtAuth.getToken().getClaim("nome");
            if (nomeClaim != null) return String.valueOf(nomeClaim);
            Object nameClaim = jwtAuth.getToken().getClaim("name");
            if (nameClaim != null) return String.valueOf(nameClaim);
            Object preferredNameClaim = jwtAuth.getToken().getClaim("preferred_username");
            if (preferredNameClaim != null) return String.valueOf(preferredNameClaim);
        }
        return null;
    }
}
