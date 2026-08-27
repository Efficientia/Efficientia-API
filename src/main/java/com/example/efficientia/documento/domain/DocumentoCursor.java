package com.example.efficientia.documento.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DocumentoCursor(Instant criadoEm, UUID id) {

    public DocumentoCursor {
        Objects.requireNonNull(criadoEm, "criadoEm é obrigatório");
        Objects.requireNonNull(id, "id é obrigatório");
    }
}
