package com.example.efficientia.relatorioviagem.api;

import com.fasterxml.jackson.annotation.JsonAlias;

public record RegistrarAssinaturasRequest(
        @JsonAlias({"url_assinatura_pecuarista", "assinaturaPecuarista"})
        String urlAssinaturaPecuarista,

        @JsonAlias({"url_assinatura_motorista", "assinaturaMotorista"})
        String urlAssinaturaMotorista,

        @JsonAlias({"url_assinatura_manobrista", "assinaturaManobrista"})
        String urlAssinaturaManobrista,

        @JsonAlias({"url_assinatura_curraleiro", "assinaturaCurraleiro"})
        String urlAssinaturaCurraleiro,

        @JsonAlias({"usar_assinatura_fixa_motorista", "usarAssinaturaFixa"})
        Boolean usarAssinaturaFixaMotorista
) {
}
