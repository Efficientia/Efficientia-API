package com.example.efficientia.relatorioviagem.api;

public class RelatorioViagemNotFoundException extends RuntimeException {
    public RelatorioViagemNotFoundException(Integer id) {
        super("Relatório de viagem não encontrado: " + id);
    }
}
