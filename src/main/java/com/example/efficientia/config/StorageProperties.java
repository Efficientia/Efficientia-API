package com.example.efficientia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.util.Objects;

@ConfigurationProperties(prefix = "app.document-storage")
public record StorageProperties(
        Path path,
        DataSize maxPdfSize,
        DataSize maxPngSize
) {

    public StorageProperties {
        Objects.requireNonNull(path, "O diretório do storage é obrigatório.");
        validarTamanho(maxPdfSize, "O limite de PDF");
        validarTamanho(maxPngSize, "O limite de PNG");
    }

    private static void validarTamanho(DataSize tamanho, String campo) {
        Objects.requireNonNull(tamanho, campo + " é obrigatório.");
        if (tamanho.toBytes() <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero.");
        }
    }
}
