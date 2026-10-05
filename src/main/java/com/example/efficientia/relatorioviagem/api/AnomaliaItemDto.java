package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.relatorioviagem.persistence.AnomaliaDesembarqueEntity;
import com.example.efficientia.relatorioviagem.persistence.AnomaliaEmbarqueEntity;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record AnomaliaItemDto(
        Integer id,
        String anomalia,
        @JsonAlias({"descricao_outros", "descricao"})
        @Size(max = 150, message = "A descrição da anomalia deve ter até 150 caracteres.")
        String descricaoOutros,
        @JsonAlias({"quantidade_animais", "animaisEnvolvidos", "quantidade"})
        @NotNull(message = "Informe quantos animais estão envolvidos na anomalia.")
        @Positive(message = "A quantidade de animais envolvidos na anomalia deve ser maior que zero.")
        Integer quantidadeAnimais
) {
    public static AnomaliaItemDto fromEmbarque(AnomaliaEmbarqueEntity entity) {
        if (entity == null) return null;
        return new AnomaliaItemDto(
                entity.getId(),
                entity.getAnomalia(),
                entity.getDescricaoOutros(),
                entity.getQuantidadeAnimais()
        );
    }

    public static AnomaliaItemDto fromDesembarque(AnomaliaDesembarqueEntity entity) {
        if (entity == null) return null;
        return new AnomaliaItemDto(
                entity.getId(),
                entity.getAnomalia(),
                entity.getDescricaoOutros(),
                entity.getQuantidadeAnimais()
        );
    }
}
