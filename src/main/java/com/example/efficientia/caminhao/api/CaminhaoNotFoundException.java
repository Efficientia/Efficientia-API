package com.example.efficientia.caminhao.api;

public class CaminhaoNotFoundException extends RuntimeException {
    public CaminhaoNotFoundException(String message) {
        super(message);
    }
}
