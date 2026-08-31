package com.example.efficientia.documento.service;

import org.springframework.core.io.Resource;

public record DocumentoConteudo(
        Resource resource,
        String mimeType,
        long tamanhoBytes,
        String nomeOriginal
) {
}
