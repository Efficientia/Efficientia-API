package com.example.efficientia.exportacao.exception;

import java.util.UUID;

public class ExportacaoNaoEncontradaException extends RuntimeException {

    public ExportacaoNaoEncontradaException(UUID id) {
        super("Exportação não encontrada: " + id);
    }
}
