package com.example.efficientia.assinaturamotorista.validation;

import com.example.efficientia.assinaturamotorista.api.AssinaturaFormatoInvalidoException;
import com.example.efficientia.assinaturamotorista.api.AssinaturaTamanhoExcedidoException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PngSignatureValidator {

    public static final long LIMITE_MAXIMO_BYTES = 1024 * 1024; // 1 MB
    private static final byte[] PNG_MAGIC_BYTES = new byte[]{
            (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
            (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
    };

    private PngSignatureValidator() {
    }

    public static ValidatedPng validate(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new AssinaturaFormatoInvalidoException("O arquivo de assinatura não pode estar vazio.");
        }

        if (bytes.length > LIMITE_MAXIMO_BYTES) {
            throw new AssinaturaTamanhoExcedidoException("O arquivo de assinatura excede o limite máximo permitido de 1 MB.");
        }

        if (bytes.length < PNG_MAGIC_BYTES.length) {
            throw new AssinaturaFormatoInvalidoException("O arquivo enviado não possui o cabeçalho de uma imagem PNG válida.");
        }

        for (int i = 0; i < PNG_MAGIC_BYTES.length; i++) {
            if (bytes[i] != PNG_MAGIC_BYTES[i]) {
                throw new AssinaturaFormatoInvalidoException("Assinatura binária inválida. O arquivo enviado não é uma imagem PNG autêntica.");
            }
        }

        String sha256 = calcularSha256(bytes);
        return new ValidatedPng(bytes, (long) bytes.length, sha256);
    }

    public static String calcularSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível no ambiente", e);
        }
    }

    public record ValidatedPng(byte[] bytes, Long tamanhoBytes, String sha256) {
    }
}
