package com.example.efficientia.exportacao.service;

import org.springframework.core.io.Resource;

public record ExportacaoConteudo(
        Resource resource,
        String mimeType,
        long tamanhoBytes,
        String nomeArquivo
) {
}
