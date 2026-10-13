package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.relatorioviagem.persistence.ParadaImprevistaEntity;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ParadaImprevistaDto(
        Integer id,
        @NotBlank(message = "O motivo da parada imprevista é obrigatório.")
        String motivo,
        @JsonAlias({"data_hora_inicio", "inicio"})
        @NotNull(message = "O início da parada imprevista é obrigatório.")
        LocalDateTime dataHoraInicio,
        @JsonAlias({"data_hora_fim", "fim"})
        @NotNull(message = "O fim da parada imprevista é obrigatório.")
        LocalDateTime dataHoraFim,
        @Size(max = 255) String descricaoMotivo
) {
    public ParadaImprevistaDto(Integer id, String motivo, LocalDateTime dataHoraInicio,
                              LocalDateTime dataHoraFim) {
        this(id, motivo, dataHoraInicio, dataHoraFim, null);
    }

    public static ParadaImprevistaDto from(ParadaImprevistaEntity entity) {
        if (entity == null) return null;
        return new ParadaImprevistaDto(
                entity.getId(),
                entity.getMotivo(),
                entity.getDataHoraInicio(),
                entity.getDataHoraFim(),
                entity.getDescricaoMotivo()
        );
    }
}
