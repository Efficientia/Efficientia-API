package com.example.efficientia.documento.storage;

import java.util.Objects;
import java.util.regex.Pattern;

public record ArquivoArmazenado(
        String nomeOriginal,
        String mimeType,
        long tamanhoBytes,
        String sha256,
        String storageKey
) {

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public ArquivoArmazenado {
        exigirTexto(nomeOriginal, "nomeOriginal");
        exigirTexto(mimeType, "mimeType");
        exigirTexto(storageKey, "storageKey");
        Objects.requireNonNull(sha256, "sha256 é obrigatório.");

        if (tamanhoBytes <= 0) {
            throw new IllegalArgumentException("tamanhoBytes deve ser maior que zero.");
        }
        if (!SHA_256.matcher(sha256).matches()) {
            throw new IllegalArgumentException("sha256 deve possuir 64 caracteres hexadecimais minúsculos.");
        }
    }

    private static void exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
    }
}
