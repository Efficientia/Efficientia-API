package com.example.efficientia.documento.storage;

import org.springframework.core.io.Resource;

import java.util.Objects;

public record StoredDocument(
        Resource conteudo,
        String mimeType,
        long tamanhoBytes
) {

    public StoredDocument {
        Objects.requireNonNull(conteudo, "conteudo é obrigatório.");
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("mimeType é obrigatório.");
        }
        if (tamanhoBytes <= 0) {
            throw new IllegalArgumentException("tamanhoBytes deve ser maior que zero.");
        }
    }
}
