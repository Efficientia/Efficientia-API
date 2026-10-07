package com.example.efficientia.feedbackrota.api;

import com.example.efficientia.feedbackrota.service.FeedbackRotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedbacks-rota")
@Tag(name = "Feedback de Rota", description = "Endpoints para envio de feedback de rotas via aplicativo móvel")
public class FeedbackRotaController {

    private final FeedbackRotaService service;

    public FeedbackRotaController(FeedbackRotaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
            summary = "Enviar feedback de rota",
            description = "Envia a avaliação e comentários de uma rota realizada pelo motorista.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> enviarFeedback(
            @Valid @RequestBody FeedbackRotaRequest request,
            Authentication authentication
    ) {
        service.enviarFeedback(request, authentication);
        return ResponseEntity.ok().build();
    }
}
