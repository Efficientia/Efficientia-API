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
        @NotBlank @Size(max = 255) String urlAssinaturaPecuarista,
        @NotBlank @Size(max = 255) String urlAssinaturaMotorista,
        @NotBlank @Size(max = 255) String urlAssinaturaManobrista,
        @NotBlank @Size(max = 255) String urlAssinaturaCurraleiro
) {
}
