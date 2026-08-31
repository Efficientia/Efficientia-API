package com.example.efficientia.documento.exception;

import java.util.UUID;

public class DocumentoConcorrenteException extends RuntimeException {

    public DocumentoConcorrenteException(UUID id) {
        super("O documento " + id + " foi alterado por outra operação. Consulte-o novamente.");
    }
}
