package com.example.efficientia.relatorioviagem.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record RelatorioViagemPageResponse(
        List<RelatorioViagemResponse> itens, int pagina, int tamanho, long total, int totalPaginas
) {
    public static RelatorioViagemPageResponse from(Page<RelatorioViagemResponse> page) {
        return new RelatorioViagemPageResponse(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages()
        );
    }
}
