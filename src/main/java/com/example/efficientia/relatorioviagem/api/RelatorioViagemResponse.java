package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record RelatorioViagemResponse(
        Integer id, Integer fazendaId, Integer motoristaId, Integer manobristaId,
        Integer curraleiroId, Integer cavaloId, Integer carretaId, String numeroGta,
        String numeroNotaFiscal, LocalDate dataEmbarque, LocalTime horarioEmbarque,
        LocalTime horarioSaidaPropriedade, Integer kmSaidaEmbarcadouro,
        LocalDate dataChegadaUnidade, LocalTime horarioChegadaUnidade,
        LocalTime horarioDesembarque, Integer kmChegadaDesembarcadouro,
        String numeroCurral, Boolean sireneReFuncionou, Integer quantidadeMachos,
        Integer quantidadeFemeas, Integer quantidadeMarrucos, Integer quantidadeEmPe,
        Integer quantidadeDeitado, Integer quantidadeMorto, Integer quantidadeEmergencia,
        String motivoEmergencia, String comentarios, String urlAssinaturaPecuarista,
        String urlAssinaturaMotorista, String urlAssinaturaManobrista,
        String urlAssinaturaCurraleiro, LocalDateTime criadoEm
) {

    public static RelatorioViagemResponse from(RelatorioViagemEntity entity) {
        return new RelatorioViagemResponse(
                entity.getId(), entity.getFazendaId(), entity.getMotoristaId(),
                entity.getManobristaId(), entity.getCurraleiroId(), entity.getCavaloId(),
                entity.getCarretaId(), entity.getNumeroGta(), entity.getNumeroNotaFiscal(),
                entity.getDataEmbarque(), entity.getHorarioEmbarque(),
                entity.getHorarioSaidaPropriedade(), entity.getKmSaidaEmbarcadouro(),
                entity.getDataChegadaUnidade(), entity.getHorarioChegadaUnidade(),
                entity.getHorarioDesembarque(), entity.getKmChegadaDesembarcadouro(),
                entity.getNumeroCurral(), entity.getSireneReFuncionou(),
                entity.getQuantidadeMachos(), entity.getQuantidadeFemeas(),
                entity.getQuantidadeMarrucos(), entity.getQuantidadeEmPe(),
                entity.getQuantidadeDeitado(), entity.getQuantidadeMorto(),
                entity.getQuantidadeEmergencia(), entity.getMotivoEmergencia(),
                entity.getComentarios(), entity.getUrlAssinaturaPecuarista(),
                entity.getUrlAssinaturaMotorista(), entity.getUrlAssinaturaManobrista(),
                entity.getUrlAssinaturaCurraleiro(), entity.getCriadoEm()
        );
    }
}
