package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioViagemResponseTest {

    @Test
    void deveMapearCamposDaViagemAssinaturasEListasDoEntity() {
        LocalDate data = LocalDate.of(2026, 10, 7);
        LocalDateTime criadoEm = data.atTime(8, 0);
        LocalDateTime enviadoEm = data.atTime(12, 0);
        UUID chave = UUID.randomUUID();
        RelatorioViagemEntity entity = new RelatorioViagemEntity();
        entity.setId(42);
        entity.setFazendaId(12);
        entity.setUnidadeFrigorificaId(13);
        entity.setMotoristaId(14);
        entity.setManobristaId(15);
        entity.setCurraleiroId(16);
        entity.setCavaloId(17);
        entity.setCarretaId(18);
        entity.setEmpresaId(19);
        entity.setNumeroGta("GTA-42");
        entity.setNumeroNotaFiscal("NF-42");
        entity.setDataEmbarque(data);
        entity.setHorarioEmbarque(LocalTime.of(8, 0));
        entity.setHorarioSaidaPropriedade(LocalTime.of(8, 30));
        entity.setKmSaidaEmbarcadouro(100);
        entity.setDataChegadaUnidade(data);
        entity.setHorarioChegadaUnidade(LocalTime.of(11, 45));
        entity.setHorarioDesembarque(LocalTime.of(12, 0));
        entity.setKmChegadaDesembarcadouro(245);
        entity.setNumeroCurral("C-7");
        entity.setSireneReFuncionou(true);
        entity.setQuantidadeMachos(10);
        entity.setQuantidadeFemeas(8);
        entity.setQuantidadeMarrucos(2);
        entity.setQuantidadeEmPe(18);
        entity.setQuantidadeDeitado(1);
        entity.setQuantidadeMorto(1);
        entity.setQuantidadeEmergencia(2);
        entity.setMotivoEmergencia("Animal ferido");
        entity.setComentarios("Ocorrência registrada");
        entity.setUrlAssinaturaPecuarista("s3://assinaturas/pecuarista.png");
        entity.setUrlAssinaturaMotorista("s3://assinaturas/motorista.png");
        entity.setUrlAssinaturaManobrista("s3://assinaturas/manobrista.png");
        entity.setUrlAssinaturaCurraleiro("s3://assinaturas/curraleiro.png");
        entity.setCriadoEm(criadoEm);
        entity.setAtualizadoEm(criadoEm.plusMinutes(5));
        entity.setStatus("enviado");
        entity.setEnviadoEm(enviadoEm);
        entity.setFinalizadoEm(enviadoEm.plusMinutes(10));
        entity.setCapacidadeCargaUtilizada(90);
        entity.setUrlLaudoMortalidade("s3://laudos/laudo.pdf");
        entity.setIdempotencyKey(chave);

        ParadaImprevistaDto parada = new ParadaImprevistaDto(1, "Pneu", criadoEm, criadoEm.plusMinutes(15));
        AnomaliaItemDto anomalia = new AnomaliaItemDto(2, "FERIMENTO", null, 1);

        RelatorioViagemResponse response = RelatorioViagemResponse.from(
                entity, List.of(parada), List.of(anomalia), List.of(anomalia));

        assertThat(response.id()).isEqualTo(42);
        assertThat(response.fazendaId()).isEqualTo(12);
        assertThat(response.unidadeFrigorificaId()).isEqualTo(13);
        assertThat(response.motoristaId()).isEqualTo(14);
        assertThat(response.manobristaId()).isEqualTo(15);
        assertThat(response.curraleiroId()).isEqualTo(16);
        assertThat(response.cavaloId()).isEqualTo(17);
        assertThat(response.carretaId()).isEqualTo(18);
        assertThat(response.empresaId()).isEqualTo(19);
        assertThat(response.totalAnimais()).isEqualTo(20);
        assertThat(response.status()).isEqualTo("enviado");
        assertThat(response.totalAssinaturasColetadas()).isEqualTo(4);
        assertThat(response.assinaturasCompletas()).isTrue();
        assertThat(response.duracaoViagemMinutos()).isEqualTo(210);
        assertThat(response.distanciaPercorridaKm()).isEqualTo(145);
        assertThat(response.idempotencyKey()).isEqualTo(chave);
        assertThat(response.criadoEm()).isEqualTo(criadoEm);
        assertThat(response.atualizadoEm()).isEqualTo(criadoEm.plusMinutes(5));
        assertThat(response.enviadoEm()).isEqualTo(enviadoEm);
        assertThat(response.finalizadoEm()).isEqualTo(enviadoEm.plusMinutes(10));
        assertThat(response.capacidadeCargaUtilizada()).isEqualTo(90);
        assertThat(response.urlLaudoMortalidade()).isEqualTo("s3://laudos/laudo.pdf");
        assertThat(response.paradasImprevistas()).containsExactly(parada);
        assertThat(response.anomaliasEmbarque()).containsExactly(anomalia);
        assertThat(response.anomaliasDesembarque()).containsExactly(anomalia);
    }

    @Test
    void deveUsarRascunhoEListasVaziasParaCamposOpcionaisNulos() {
        RelatorioViagemEntity entity = new RelatorioViagemEntity();
        entity.setStatus(null);

        RelatorioViagemResponse comListasNulas = RelatorioViagemResponse.from(entity, null, null, null);
        RelatorioViagemResponse semListas = RelatorioViagemResponse.from(entity);

        assertThat(comListasNulas.status()).isEqualTo("rascunho");
        assertThat(comListasNulas.totalAnimais()).isZero();
        assertThat(comListasNulas.totalAssinaturasColetadas()).isZero();
        assertThat(comListasNulas.assinaturasCompletas()).isFalse();
        assertThat(comListasNulas.duracaoViagemMinutos()).isNull();
        assertThat(comListasNulas.distanciaPercorridaKm()).isNull();
        assertThat(comListasNulas.paradasImprevistas()).isEmpty();
        assertThat(comListasNulas.anomaliasEmbarque()).isEmpty();
        assertThat(comListasNulas.anomaliasDesembarque()).isEmpty();
        assertThat(semListas.paradasImprevistas()).isEmpty();
        assertThat(semListas.anomaliasEmbarque()).isEmpty();
        assertThat(semListas.anomaliasDesembarque()).isEmpty();
    }

    @Test
    void construtoresDeCompatibilidadeCalculamTotaisComCategoriasAusentes() {
        LocalDate data = LocalDate.of(2026, 10, 7);
        LocalDateTime criadoEm = data.atStartOfDay();

        RelatorioViagemResponse legado = new RelatorioViagemResponse(
                1, 2, 3, 4, 5, 6, 7, "GTA", "NF", data, null, null, null,
                data, null, null, null, "C1", null, null, 5, null,
                null, null, null, null, null, null, null, null, null, null, criadoEm, "rascunho");
        RelatorioViagemResponse semStatus = new RelatorioViagemResponse(
                1, 2, 3, 4, 5, 6, 7, "GTA", "NF", data, null, null, null,
                data, null, null, null, "C1", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, criadoEm);
        RelatorioViagemResponse comMetadados = new RelatorioViagemResponse(
                1, 2, 3, 4, 5, 6, 7, "GTA", "NF", data, null, null, null,
                data, null, null, null, "C1", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, criadoEm,
                "finalizado", criadoEm.plusHours(1), criadoEm.plusHours(2), 80,
                "s3://laudo.pdf", 1, false);

        assertThat(legado.totalAnimais()).isEqualTo(5);
        assertThat(semStatus.status()).isEqualTo("rascunho");
        assertThat(semStatus.totalAnimais()).isZero();
        assertThat(comMetadados.status()).isEqualTo("finalizado");
        assertThat(comMetadados.totalAnimais()).isZero();
        assertThat(comMetadados.enviadoEm()).isEqualTo(criadoEm.plusHours(1));
        assertThat(comMetadados.finalizadoEm()).isEqualTo(criadoEm.plusHours(2));
        assertThat(comMetadados.capacidadeCargaUtilizada()).isEqualTo(80);
        assertThat(comMetadados.urlLaudoMortalidade()).isEqualTo("s3://laudo.pdf");
    }
}
