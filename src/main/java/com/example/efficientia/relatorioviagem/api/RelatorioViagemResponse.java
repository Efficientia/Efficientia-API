package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record RelatorioViagemResponse(
        Integer id, Integer fazendaId, Integer unidadeFrigorificaId,
        Integer motoristaId, Integer manobristaId, Integer curraleiroId,
        Integer cavaloId, Integer carretaId, Integer empresaId,
        String numeroGta, String numeroNotaFiscal,
        LocalDate dataEmbarque, LocalTime horarioEmbarque,
        LocalTime horarioSaidaPropriedade, Integer kmSaidaEmbarcadouro,
        LocalDate dataChegadaUnidade, LocalTime horarioChegadaUnidade,
        LocalTime horarioDesembarque, Integer kmChegadaDesembarcadouro,
        String numeroCurral, Boolean sireneReFuncionou,
        Integer quantidadeMachos, Integer quantidadeFemeas, Integer quantidadeMarrucos,
        Integer totalAnimais,
        Integer quantidadeEmPe, Integer quantidadeDeitado, Integer quantidadeMorto, Integer quantidadeEmergencia,
        String motivoEmergencia, String comentarios,
        String urlAssinaturaPecuarista, String urlAssinaturaMotorista, String urlAssinaturaManobrista, String urlAssinaturaCurraleiro,
        LocalDateTime criadoEm, String status, LocalDateTime atualizadoEm,
        LocalDateTime enviadoEm, LocalDateTime finalizadoEm,
        Integer capacidadeCargaUtilizada, String urlLaudoMortalidade,
        Integer totalAssinaturasColetadas, Boolean assinaturasCompletas,
        Long duracaoViagemMinutos, Integer distanciaPercorridaKm,
        UUID idempotencyKey,
        List<ParadaImprevistaDto> paradasImprevistas,
        List<AnomaliaItemDto> anomaliasEmbarque,
        List<AnomaliaItemDto> anomaliasDesembarque
) {

    public RelatorioViagemResponse(
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
            String urlAssinaturaCurraleiro, LocalDateTime criadoEm,
            String status
    ) {
        this(id, fazendaId, null, motoristaId, manobristaId, curraleiroId, cavaloId, carretaId, null,
                numeroGta, numeroNotaFiscal, dataEmbarque, horarioEmbarque, horarioSaidaPropriedade,
                kmSaidaEmbarcadouro, dataChegadaUnidade, horarioChegadaUnidade, horarioDesembarque,
                kmChegadaDesembarcadouro, numeroCurral, sireneReFuncionou, quantidadeMachos,
                quantidadeFemeas, quantidadeMarrucos,
                (quantidadeMachos != null ? quantidadeMachos : 0) + (quantidadeFemeas != null ? quantidadeFemeas : 0) + (quantidadeMarrucos != null ? quantidadeMarrucos : 0),
                quantidadeEmPe, quantidadeDeitado, quantidadeMorto,
                quantidadeEmergencia, motivoEmergencia, comentarios, urlAssinaturaPecuarista,
                urlAssinaturaMotorista, urlAssinaturaManobrista, urlAssinaturaCurraleiro,
                criadoEm, status, null, null, null, null, null, 0, false, null, null, null,
                List.of(), List.of(), List.of());
    }

    public RelatorioViagemResponse(
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
        this(id, fazendaId, motoristaId, manobristaId, curraleiroId, cavaloId, carretaId,
                numeroGta, numeroNotaFiscal, dataEmbarque, horarioEmbarque, horarioSaidaPropriedade,
                kmSaidaEmbarcadouro, dataChegadaUnidade, horarioChegadaUnidade, horarioDesembarque,
                kmChegadaDesembarcadouro, numeroCurral, sireneReFuncionou, quantidadeMachos,
                quantidadeFemeas, quantidadeMarrucos, quantidadeEmPe, quantidadeDeitado,
                quantidadeMorto, quantidadeEmergencia, motivoEmergencia, comentarios,
                urlAssinaturaPecuarista, urlAssinaturaMotorista, urlAssinaturaManobrista,
                urlAssinaturaCurraleiro, criadoEm, "rascunho");
    }

    public RelatorioViagemResponse(
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
            String urlAssinaturaCurraleiro, LocalDateTime criadoEm,
            String status, LocalDateTime enviadoEm, LocalDateTime finalizadoEm,
            Integer capacidadeCargaUtilizada, String urlLaudoMortalidade,
            Integer totalAssinaturasColetadas, Boolean assinaturasCompletas
    ) {
        this(id, fazendaId, null, motoristaId, manobristaId, curraleiroId, cavaloId, carretaId, null,
                numeroGta, numeroNotaFiscal, dataEmbarque, horarioEmbarque, horarioSaidaPropriedade,
                kmSaidaEmbarcadouro, dataChegadaUnidade, horarioChegadaUnidade, horarioDesembarque,
                kmChegadaDesembarcadouro, numeroCurral, sireneReFuncionou, quantidadeMachos,
                quantidadeFemeas, quantidadeMarrucos,
                (quantidadeMachos != null ? quantidadeMachos : 0) + (quantidadeFemeas != null ? quantidadeFemeas : 0) + (quantidadeMarrucos != null ? quantidadeMarrucos : 0),
                quantidadeEmPe, quantidadeDeitado, quantidadeMorto,
                quantidadeEmergencia, motivoEmergencia, comentarios, urlAssinaturaPecuarista,
                urlAssinaturaMotorista, urlAssinaturaManobrista, urlAssinaturaCurraleiro,
                criadoEm, status, null, enviadoEm, finalizadoEm, capacidadeCargaUtilizada, urlLaudoMortalidade,
                totalAssinaturasColetadas, assinaturasCompletas, null, null, null,
                List.of(), List.of(), List.of());
    }

    public static RelatorioViagemResponse from(RelatorioViagemEntity entity) {
        return from(entity, List.of(), List.of(), List.of());
    }

    public static RelatorioViagemResponse from(
            RelatorioViagemEntity entity,
            List<ParadaImprevistaDto> paradas,
            List<AnomaliaItemDto> anomaliasEmbarque,
            List<AnomaliaItemDto> anomaliasDesembarque
    ) {
        int totalAssinaturas = entity.contarAssinaturasPreenchidas();
        boolean completas = entity.possuiAssinaturasCompletas(4);
        return new RelatorioViagemResponse(
                entity.getId(), entity.getFazendaId(), entity.getUnidadeFrigorificaId(),
                entity.getMotoristaId(), entity.getManobristaId(), entity.getCurraleiroId(),
                entity.getCavaloId(), entity.getCarretaId(), entity.getEmpresaId(),
                entity.getNumeroGta(), entity.getNumeroNotaFiscal(),
                entity.getDataEmbarque(), entity.getHorarioEmbarque(),
                entity.getHorarioSaidaPropriedade(), entity.getKmSaidaEmbarcadouro(),
                entity.getDataChegadaUnidade(), entity.getHorarioChegadaUnidade(),
                entity.getHorarioDesembarque(), entity.getKmChegadaDesembarcadouro(),
                entity.getNumeroCurral(), entity.getSireneReFuncionou(),
                entity.getQuantidadeMachos(), entity.getQuantidadeFemeas(), entity.getQuantidadeMarrucos(),
                entity.getTotalAnimais(),
                entity.getQuantidadeEmPe(), entity.getQuantidadeDeitado(), entity.getQuantidadeMorto(),
                entity.getQuantidadeEmergencia(), entity.getMotivoEmergencia(),
                entity.getComentarios(), entity.getUrlAssinaturaPecuarista(),
                entity.getUrlAssinaturaMotorista(), entity.getUrlAssinaturaManobrista(),
                entity.getUrlAssinaturaCurraleiro(), entity.getCriadoEm(),
                entity.getStatus() != null ? entity.getStatus() : "rascunho",
                entity.getAtualizadoEm(),
                entity.getEnviadoEm(), entity.getFinalizadoEm(),
                entity.getCapacidadeCargaUtilizada(), entity.getUrlLaudoMortalidade(),
                totalAssinaturas, completas,
                entity.getDuracaoViagemMinutos(), entity.getDistanciaPercorridaKm(),
                entity.getIdempotencyKey(),
                paradas != null ? paradas : List.of(),
                anomaliasEmbarque != null ? anomaliasEmbarque : List.of(),
                anomaliasDesembarque != null ? anomaliasDesembarque : List.of()
        );
    }
}
