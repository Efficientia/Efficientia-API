package com.example.efficientia.feedbackrota.service;

import com.example.efficientia.feedbackrota.api.FeedbackRotaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FeedbackRotaServiceTest {

    private FeedbackRotaService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackRotaService();
    }

    @Test
    void deveEnviarFeedbackComSucessoComJwtAuthentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("usuario_id", 42)
                .claim("empresa_id", 10)
                .claim("nome", "Joao Motorista")
                .build();
        Authentication authentication = new JwtAuthenticationToken(jwt);
        
        FeedbackRotaRequest request = new FeedbackRotaRequest(
                UUID.randomUUID(),
                "OTIMA",
                List.of("ENTRADA"),
                "Tudo certo"
        );

        assertDoesNotThrow(() -> service.enviarFeedback(request, authentication));
    }

    @Test
    void deveEnviarFeedbackComSucessoComClaimsAlternativos() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("usuario-99")
                .claim("name", "Joao Alt")
                .build();
        Authentication authentication = new JwtAuthenticationToken(jwt);
        
        FeedbackRotaRequest request = new FeedbackRotaRequest(
                UUID.randomUUID(),
                "REGULAR",
                List.of(),
                null
        );

        assertDoesNotThrow(() -> service.enviarFeedback(request, authentication));
    }

    @Test
    void deveEnviarFeedbackComSucessoComOutroTipoDeAuthentication() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("user123", "pass");
        
        FeedbackRotaRequest request = new FeedbackRotaRequest(
                UUID.randomUUID(),
                "RUIM",
                List.of("ESPERA_NA_FAZENDA"),
                "Demorou muito"
        );

        assertDoesNotThrow(() -> service.enviarFeedback(request, authentication));
    }

    @Test
    void deveEnviarFeedbackComAuthenticationNula() {
        FeedbackRotaRequest request = new FeedbackRotaRequest(
                UUID.randomUUID(),
                "PESSIMA",
                List.of("OUTROS"),
                "N/A"
        );

        assertDoesNotThrow(() -> service.enviarFeedback(request, null));
    }
}