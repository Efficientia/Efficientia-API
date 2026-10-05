package com.example.efficientia.relatorioviagem.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record CriarRelatorioViagemRequest(
        @NotNull @Positive Integer fazendaId,
        @NotNull @Positive Integer motoristaId,
        @NotNull @Positive Integer manobristaId,
        @NotNull @Positive Integer curraleiroId,
        @NotNull @Positive Integer cavaloId,
        @NotNull @Positive Integer carretaId,
        @NotBlank @Size(max = 50) String numeroGta,
        @NotBlank @Size(max = 50) String numeroNotaFiscal,
        @NotNull LocalDate dataEmbarque,
        @NotNull LocalTime horarioEmbarque,
        @NotNull LocalTime horarioSaidaPropriedade,
        @NotNull @PositiveOrZero Integer kmSaidaEmbarcadouro,
        @NotNull LocalDate dataChegadaUnidade,
        @NotNull LocalTime horarioChegadaUnidade,
        @NotNull LocalTime horarioDesembarque,
        @NotNull @PositiveOrZero Integer kmChegadaDesembarcadouro,
        @NotBlank @Size(max = 20) String numeroCurral,
        @NotNull Boolean sireneReFuncionou,
        @NotNull @PositiveOrZero Integer quantidadeMachos,
        @NotNull @PositiveOrZero Integer quantidadeFemeas,
        @NotNull @PositiveOrZero Integer quantidadeMarrucos,
        @NotNull @PositiveOrZero Integer quantidadeEmPe,
        @NotNull @PositiveOrZero Integer quantidadeDeitado,
        @NotNull @PositiveOrZero Integer quantidadeMorto,
        @NotNull @PositiveOrZero Integer quantidadeEmergencia,
        @Size(max = 255) String motivoEmergencia,
        String comentarios,
        String urlAssinaturaPecuarista,
        String urlAssinaturaMotorista,
        String urlAssinaturaManobrista,
        String urlAssinaturaCurraleiro,
        Integer capacidadeCargaUtilizada,
        String urlLaudoMortalidade
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
        this(fazendaId, motoristaId, manobristaId, curraleiroId, cavaloId, carretaId,
                numeroGta, numeroNotaFiscal, dataEmbarque, horarioEmbarque, horarioSaidaPropriedade,
                kmSaidaEmbarcadouro, dataChegadaUnidade, horarioChegadaUnidade, horarioDesembarque,
                kmChegadaDesembarcadouro, numeroCurral, sireneReFuncionou, quantidadeMachos,
                quantidadeFemeas, quantidadeMarrucos, quantidadeEmPe, quantidadeDeitado,
                quantidadeMorto, quantidadeEmergencia, motivoEmergencia, comentarios,
                urlAssinaturaPecuarista, urlAssinaturaMotorista, urlAssinaturaManobrista,
                urlAssinaturaCurraleiro, null, null);
    }
}
