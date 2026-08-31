package com.example.efficientia.documento.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ApiProblem", description = "Falha HTTP padronizada em application/problem+json")
public record ApiProblemResponse(
        @Schema(example = "DOCUMENTO_NAO_ENCONTRADO") String code,
        @Schema(example = "2026-08-31T13:00:00Z") Instant timestamp,
        @Schema(example = "a1b2c3d4-e5f6-47a8-9012-3456789abcde") String correlationId,
        @Schema(example = "Documento não encontrado") String title,
        @Schema(example = "404") int status,
        @Schema(example = "O documento informado não existe.") String detail
) {
}
