package com.example.efficientia.temp.api;

import java.util.List;
import java.util.Map;

public record TabelaDadosResponse(
        String tabela,
        String schema,
        Long totalRegistros,
        Integer limite,
        Integer offset,
        List<String> colunas,
        List<Map<String, Object>> dados
) {
}
