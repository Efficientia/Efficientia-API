package com.example.efficientia.relatorioviagem.api;

public class NumeroGtaDuplicadoException extends RuntimeException {

    public NumeroGtaDuplicadoException(String numeroGta) {
        super("Já existe um relatório para o número GTA: " + numeroGta);
    }
}
