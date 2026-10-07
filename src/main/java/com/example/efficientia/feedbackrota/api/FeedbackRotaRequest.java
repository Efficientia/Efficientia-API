package com.example.efficientia.feedbackrota.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record FeedbackRotaRequest(
        @NotNull(message = "rotaId é obrigatório")
        UUID rotaId,
        @NotBlank(message = "avaliacao é obrigatória")
        String avaliacao,
        List<String> motivos,
        String comentario
) {}
