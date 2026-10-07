package com.example.efficientia.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EfficientiaMetricsServiceTest {

    private SimpleMeterRegistry registry;
    private EfficientiaMetricsService service;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        service = new EfficientiaMetricsService(registry);
    }

    @Test
    void deveRegistrarTodasAsMetricasComNomesDescricoesEUnidadesEsperadas() {
        Set<String> nomes = registry.getMeters().stream()
                .map(meter -> meter.getId().getName())
                .collect(Collectors.toSet());

        assertThat(nomes).containsExactlyInAnyOrder(
                "exportacoes.solicitadas.total",
                "exportacoes.processadas.total",
                "documentos.criados.total",
                "documentos.tamanho.bytes",
                "exportacoes.processamento.tempo");

        assertThat(registry.getMeters()).hasSize(6);
        assertMeterMetadata(counter("exportacoes.solicitadas.total"),
                "Total de solicitações de exportação criadas", null);
        assertMeterMetadata(counter("exportacoes.processadas.total", "CONCLUIDA"),
                "Total de exportações concluídas com sucesso", null);
        assertMeterMetadata(counter("exportacoes.processadas.total", "FALHA"),
                "Total de exportações que falharam", null);
        assertMeterMetadata(counter("documentos.criados.total"),
                "Total de documentos cadastrados", null);
        assertMeterMetadata(summary(),
                "Distribuição de tamanhos de arquivos de documentos armazenados", "bytes");
        assertMeterMetadata(timer(),
                "Tempo de processamento de exportações em segundo plano", "seconds");

        assertThat(counter("exportacoes.solicitadas.total").count()).isZero();
        assertThat(counter("exportacoes.processadas.total", "CONCLUIDA").count()).isZero();
        assertThat(counter("exportacoes.processadas.total", "FALHA").count()).isZero();
        assertThat(counter("documentos.criados.total").count()).isZero();
        assertThat(summary().count()).isZero();
        assertThat(timer().count()).isZero();
    }

    @Test
    void deveContabilizarCadaSolicitacaoDeExportacao() {
        service.registrarExportacaoSolicitada();
        service.registrarExportacaoSolicitada();

        assertThat(counter("exportacoes.solicitadas.total").count()).isEqualTo(2.0);
    }

    @Test
    void deveManterSeparadosOsTotaisDeExportacoesConcluidasEComFalha() {
        service.registrarExportacaoConcluida();
        service.registrarExportacaoConcluida();
        service.registrarExportacaoFalha();

        assertThat(counter("exportacoes.processadas.total", "CONCLUIDA").count()).isEqualTo(2.0);
        assertThat(counter("exportacoes.processadas.total", "FALHA").count()).isEqualTo(1.0);
    }

    @Test
    void deveContabilizarDocumentosERegistrarSeusTamanhosIncluindoArquivoVazio() {
        service.registrarDocumentoCriado(0);
        service.registrarDocumentoCriado(512);
        service.registrarDocumentoCriado(1024);

        Counter documentos = counter("documentos.criados.total");
        DistributionSummary tamanhos = summary();

        assertThat(documentos.count()).isEqualTo(3.0);
        assertThat(tamanhos.count()).isEqualTo(3L);
        assertThat(tamanhos.totalAmount()).isEqualTo(1536.0);
        assertThat(tamanhos.max()).isEqualTo(1024.0);
    }

    @Test
    void deveRegistrarDuracaoDeProcessamentoEmMilissegundos() {
        service.registrarTempoProcessamento(1250);

        Timer tempo = timer();

        assertThat(tempo.count()).isEqualTo(1L);
        assertThat(tempo.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(1250.0);
        assertThat(tempo.max(TimeUnit.MILLISECONDS)).isEqualTo(1250.0);
    }

    private Counter counter(String nome) {
        return registry.get(nome).counter();
    }

    private Counter counter(String nome, String status) {
        return registry.get(nome).tag("status", status).counter();
    }

    private DistributionSummary summary() {
        return registry.get("documentos.tamanho.bytes").summary();
    }

    private Timer timer() {
        return registry.get("exportacoes.processamento.tempo").timer();
    }

    private void assertMeterMetadata(Meter meter, String description, String baseUnit) {
        assertThat(meter.getId().getDescription()).isEqualTo(description);
        assertThat(meter.getId().getBaseUnit()).isEqualTo(baseUnit);
    }
}
