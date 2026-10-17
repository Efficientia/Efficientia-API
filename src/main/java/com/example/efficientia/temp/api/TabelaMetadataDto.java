package com.example.efficientia.temp.api;

import java.util.List;

public record TabelaMetadataDto(
        String nome,
        String schema,
        String alias,
        Long totalRegistros,
        List<String> colunas
) {
}
