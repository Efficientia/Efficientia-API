package com.example.efficientia.documento.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AtualizarDocumentoRequest(
        @NotNull @PositiveOrZero Long versao,
        @Size(max = 300) String descricao
) {
}
