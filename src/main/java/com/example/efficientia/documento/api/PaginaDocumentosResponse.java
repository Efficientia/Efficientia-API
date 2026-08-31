package com.example.efficientia.documento.api;

import java.util.List;

public record PaginaDocumentosResponse(
        List<DocumentoResponse> itens,
        Integer page,
        int size,
        Long totalElementos,
        Integer totalPaginas,
        String proximoCursor,
        boolean temMais
) {
    public PaginaDocumentosResponse {
        itens = List.copyOf(itens);
    }
}
