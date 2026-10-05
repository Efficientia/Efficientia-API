package com.example.efficientia.relatorioviagem.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CriarRelatorioViagemRequest(
        @JsonAlias({"fazenda_id", "propriedadeOrigemId"})
        @Positive(message = "O ID da fazenda deve ser um número positivo.")
        Integer fazendaId,

        @JsonAlias({"unidade_frigorifica_id", "destinoId"})
        @Positive(message = "O ID da unidade frigorífica deve ser positivo.")
        Integer unidadeFrigorificaId,

        @JsonAlias({"motorista_id"})
        @Positive(message = "O ID do motorista deve ser positivo.")
        Integer motoristaId,

        @JsonAlias({"manobrista_id"})
        @Positive(message = "O ID do manobrista deve ser positivo.")
        Integer manobristaId,

        @JsonAlias({"curraleiro_id"})
        @Positive(message = "O ID do curraleiro deve ser positivo.")
        Integer curraleiroId,

        @JsonAlias({"cavalo_id"})
        @Positive(message = "O ID do cavalo deve ser positivo.")
        Integer cavaloId,

        @JsonAlias({"carreta_id"})
        @Positive(message = "O ID da carreta deve ser positivo.")
        Integer carretaId,

        @JsonAlias({"placa_cavalo"})
        String placaCavalo,

        @JsonAlias({"placa_carreta"})
        String placaCarreta,

        @Size(max = 50)
        @JsonAlias({"numero_gta", "gta"})
        String numeroGta,

        @Size(max = 50)
        @JsonAlias({"numero_nota_fiscal", "notaFiscal"})
        String numeroNotaFiscal,

        @JsonAlias({"data_embarque"})
        LocalDate dataEmbarque,

        @JsonAlias({"horario_embarque"})
        LocalTime horarioEmbarque,

        @JsonAlias({"horario_saida_propriedade"})
        LocalTime horarioSaidaPropriedade,

        @JsonAlias({"km_saida_embarcadouro", "kmSaida"})
        @PositiveOrZero(message = "A quilometragem de saída não pode ser negativa.")
        Integer kmSaidaEmbarcadouro,

        @JsonAlias({"data_chegada_unidade", "dataChegada"})
        LocalDate dataChegadaUnidade,

        @JsonAlias({"horario_chegada_unidade", "horarioChegada"})
        LocalTime horarioChegadaUnidade,

        @JsonAlias({"horario_desembarque"})
        LocalTime horarioDesembarque,

        @JsonAlias({"km_chegada_desembarcadouro", "kmChegada"})
        @PositiveOrZero(message = "A quilometragem de chegada não pode ser negativa.")
        Integer kmChegadaDesembarcadouro,

        @Size(max = 20)
        @JsonAlias({"numero_curral", "curral"})
        String numeroCurral,

        @JsonAlias({"sirene_re_funcionou", "sireneRe"})
        Boolean sireneReFuncionou,

        @JsonAlias({"qtd_machos", "machos"})
        @PositiveOrZero(message = "A quantidade de machos não pode ser negativa.")
        Integer quantidadeMachos,

        @JsonAlias({"qtd_femeas", "femeas"})
        @PositiveOrZero(message = "A quantidade de fêmeas não pode ser negativa.")
        Integer quantidadeFemeas,

        @JsonAlias({"qtd_marrucos", "marrucos"})
        @PositiveOrZero(message = "A quantidade de marrucos não pode ser negativa.")
        Integer quantidadeMarrucos,

        @JsonAlias({"qtd_em_pe", "emPe"})
        @PositiveOrZero(message = "A quantidade em pé não pode ser negativa.")
        Integer quantidadeEmPe,

        @JsonAlias({"qtd_deitado", "deitado"})
        @PositiveOrZero(message = "A quantidade deitados não pode ser negativa.")
        Integer quantidadeDeitado,

        @JsonAlias({"qtd_morto", "morto"})
        @PositiveOrZero(message = "A quantidade de mortos não pode ser negativa.")
        Integer quantidadeMorto,

        @JsonAlias({"qtd_emergencia", "emergencia"})
        @PositiveOrZero(message = "A quantidade em emergência não pode ser negativa.")
        Integer quantidadeEmergencia,

        @Size(max = 255)
        @JsonAlias({"motivo_emergencia", "motivoIncidente"})
        String motivoEmergencia,

        @JsonAlias({"comentarios", "observacoes"})
        String comentarios,

        @JsonAlias({"url_assinatura_pecuarista"})
        String urlAssinaturaPecuarista,

        @JsonAlias({"url_assinatura_motorista"})
        String urlAssinaturaMotorista,

        @JsonAlias({"url_assinatura_manobrista"})
        String urlAssinaturaManobrista,

        @JsonAlias({"url_assinatura_curraleiro"})
        String urlAssinaturaCurraleiro,

        @JsonAlias({"capacidade_carga_utilizada", "capacidadeUtilizada"})
        Integer capacidadeCargaUtilizada,

        @JsonAlias({"url_laudo_mortalidade", "laudoMortalidadeUrl"})
        String urlLaudoMortalidade,

        String status,

        @JsonAlias({"paradas_imprevistas", "paradas"})
        List<@Valid ParadaImprevistaDto> paradasImprevistas,

        @JsonAlias({"anomalias_embarque"})
        List<@Valid AnomaliaItemDto> anomaliasEmbarque,

        @JsonAlias({"anomalias_desembarque"})
        List<@Valid AnomaliaItemDto> anomaliasDesembarque
) {
    public CriarRelatorioViagemRequest(
            Integer fazendaId, Integer motoristaId, Integer manobristaId, Integer curraleiroId,
            Integer cavaloId, Integer carretaId, String numeroGta, String numeroNotaFiscal,
            LocalDate dataEmbarque, LocalTime horarioEmbarque, LocalTime horarioSaidaPropriedade,
            Integer kmSaidaEmbarcadouro, LocalDate dataChegadaUnidade, LocalTime horarioChegadaUnidade,
            LocalTime horarioDesembarque, Integer kmChegadaDesembarcadouro, String numeroCurral,
            Boolean sireneReFuncionou, Integer quantidadeMachos, Integer quantidadeFemeas,
            Integer quantidadeMarrucos, Integer quantidadeEmPe, Integer quantidadeDeitado,
            Integer quantidadeMorto, Integer quantidadeEmergencia, String motivoEmergencia,
            String comentarios, String urlAssinaturaPecuarista, String urlAssinaturaMotorista,
            String urlAssinaturaManobrista, String urlAssinaturaCurraleiro
    ) {
        this(fazendaId, null, motoristaId, manobristaId, curraleiroId, cavaloId, carretaId,
                null, null, numeroGta, numeroNotaFiscal, dataEmbarque, horarioEmbarque,
                horarioSaidaPropriedade, kmSaidaEmbarcadouro, dataChegadaUnidade,
                horarioChegadaUnidade, horarioDesembarque, kmChegadaDesembarcadouro,
                numeroCurral, sireneReFuncionou, quantidadeMachos, quantidadeFemeas,
                quantidadeMarrucos, quantidadeEmPe, quantidadeDeitado, quantidadeMorto,
                quantidadeEmergencia, motivoEmergencia, comentarios, urlAssinaturaPecuarista,
                urlAssinaturaMotorista, urlAssinaturaManobrista, urlAssinaturaCurraleiro,
                null, null, "rascunho", null, null, null);
    }

    public CriarRelatorioViagemRequest(
            Integer fazendaId, Integer motoristaId, Integer manobristaId, Integer curraleiroId,
            Integer cavaloId, Integer carretaId, String numeroGta, String numeroNotaFiscal,
            LocalDate dataEmbarque, LocalTime horarioEmbarque, LocalTime horarioSaidaPropriedade,
            Integer kmSaidaEmbarcadouro, LocalDate dataChegadaUnidade, LocalTime horarioChegadaUnidade,
            LocalTime horarioDesembarque, Integer kmChegadaDesembarcadouro, String numeroCurral,
            Boolean sireneReFuncionou, Integer quantidadeMachos, Integer quantidadeFemeas,
            Integer quantidadeMarrucos, Integer quantidadeEmPe, Integer quantidadeDeitado,
            Integer quantidadeMorto, Integer quantidadeEmergencia, String motivoEmergencia,
            String comentarios, String urlAssinaturaPecuarista, String urlAssinaturaMotorista,
            String urlAssinaturaManobrista, String urlAssinaturaCurraleiro,
            Integer capacidadeCargaUtilizada, String urlLaudoMortalidade
    ) {
        this(fazendaId, null, motoristaId, manobristaId, curraleiroId, cavaloId, carretaId,
                null, null, numeroGta, numeroNotaFiscal, dataEmbarque, horarioEmbarque,
                horarioSaidaPropriedade, kmSaidaEmbarcadouro, dataChegadaUnidade,
                horarioChegadaUnidade, horarioDesembarque, kmChegadaDesembarcadouro,
                numeroCurral, sireneReFuncionou, quantidadeMachos, quantidadeFemeas,
                quantidadeMarrucos, quantidadeEmPe, quantidadeDeitado, quantidadeMorto,
                quantidadeEmergencia, motivoEmergencia, comentarios, urlAssinaturaPecuarista,
                urlAssinaturaMotorista, urlAssinaturaManobrista, urlAssinaturaCurraleiro,
                capacidadeCargaUtilizada, urlLaudoMortalidade, "rascunho", null, null, null);
    }
}
