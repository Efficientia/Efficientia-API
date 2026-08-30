package com.example.efficientia.documento.storage;

import java.util.Objects;

public class StorageValidationException extends StorageException {

    private final Reason reason;

    public StorageValidationException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason é obrigatório.");
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        INVALID_INPUT,
        INVALID_KEY,
        UNSUPPORTED_MEDIA_TYPE,
        EMPTY_FILE,
        SIZE_LIMIT_EXCEEDED
    }
}
