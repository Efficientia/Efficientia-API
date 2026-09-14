package com.example.efficientia.auth.api;

public class AutenticacaoInvalidaException extends RuntimeException {
    public AutenticacaoInvalidaException(String message) {
        super(message);
    }
}
