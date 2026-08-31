package com.example.efficientia.security;

public record DocumentoAccessContext(
        Integer usuarioId,
        DocumentoAccessScope scope
) {
    public static DocumentoAccessContext todos() {
        return new DocumentoAccessContext(null, DocumentoAccessScope.TODOS);
    }
}
