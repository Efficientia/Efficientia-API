package com.example.efficientia.documento.exception;

import java.util.UUID;

public class DocumentoSemConteudoException extends RuntimeException {

    public DocumentoSemConteudoException(UUID id) {
        super("O documento " + id + " é uma assinatura textual e não possui conteúdo binário.");
    }
}
