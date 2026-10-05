package com.example.efficientia.relatorioviagem.api;

import java.util.Map;
import java.util.Collections;
import java.util.TreeMap;

public class ValidacaoDiarioRotaException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public ValidacaoDiarioRotaException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null
                ? Map.of()
                : Collections.unmodifiableMap(new TreeMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
