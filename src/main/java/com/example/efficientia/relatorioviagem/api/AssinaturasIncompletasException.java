package com.example.efficientia.relatorioviagem.api;

import java.util.List;

public class AssinaturasIncompletasException extends RuntimeException {
    private final Integer relatorioId;
    private final int qtdObrigatoria;
    private final int qtdColetadas;
    private final List<String> papeisFaltantes;

    public AssinaturasIncompletasException(Integer relatorioId, int qtdObrigatoria, int qtdColetadas, List<String> papeisFaltantes) {
        super(String.format(
                "Não é possível finalizar o relatório de viagem ID %d. São obrigatórias %d assinaturas, mas apenas %d foram coletadas. Faltam: %s.",
                relatorioId, qtdObrigatoria, qtdColetadas, String.join(", ", papeisFaltantes)
        ));
        this.relatorioId = relatorioId;
        this.qtdObrigatoria = qtdObrigatoria;
        this.qtdColetadas = qtdColetadas;
        this.papeisFaltantes = papeisFaltantes;
    }

    public Integer getRelatorioId() {
        return relatorioId;
    }

    public int getQtdObrigatoria() {
        return qtdObrigatoria;
    }

    public int getQtdColetadas() {
        return qtdColetadas;
    }

    public List<String> getPapeisFaltantes() {
        return papeisFaltantes;
    }
}
