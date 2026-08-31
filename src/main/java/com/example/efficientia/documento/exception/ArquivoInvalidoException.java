package com.example.efficientia.documento.exception;

import java.util.Objects;

public class ArquivoInvalidoException extends RuntimeException {

    private final Reason reason;

    public ArquivoInvalidoException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason é obrigatório");
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        INVALID_FILE,
        SIZE_LIMIT_EXCEEDED,
        UNSUPPORTED_MEDIA_TYPE
    }
}
