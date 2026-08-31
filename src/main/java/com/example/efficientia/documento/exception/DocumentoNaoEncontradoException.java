package com.example.efficientia.documento.exception;

import java.util.UUID;

public class DocumentoNaoEncontradoException extends RuntimeException {

    public DocumentoNaoEncontradoException(UUID id) {
        super("Documento não encontrado: " + id);
    }
}
