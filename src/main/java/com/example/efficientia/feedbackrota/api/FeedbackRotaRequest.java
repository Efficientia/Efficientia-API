package com.example.efficientia.feedbackrota.api;

import java.util.List;
import java.util.UUID;

public record FeedbackRotaRequest(
    UUID rotaId,
    String avaliacao,
    List<String> motivos,
    String comentario
) {}
