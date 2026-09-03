package com.example.efficientia.exportacao.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Documentos que devem compor a exportação, preservando a ordem informada")
public record SolicitarExportacaoRequest(
        @NotNull
        @Size(min = 1, max = 500)
        List<@NotNull UUID> documentoIds
) {
}
