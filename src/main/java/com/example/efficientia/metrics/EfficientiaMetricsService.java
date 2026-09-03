package com.example.efficientia.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class EfficientiaMetricsService {

    private final Counter exportacoesSolicitadas;
    private final Counter exportacoesConcluidas;
    private final Counter exportacoesFalhas;
    private final Counter documentosCriados;
    private final DistributionSummary tamanhoDocumentosBytes;
    private final Timer tempoProcessamentoExportacao;

    public EfficientiaMetricsService(MeterRegistry registry) {
        this.exportacoesSolicitadas = Counter.builder("exportacoes.solicitadas.total")
                .description("Total de solicitações de exportação criadas")
                .register(registry);

        this.exportacoesConcluidas = Counter.builder("exportacoes.processadas.total")
                .tag("status", "CONCLUIDA")
                .description("Total de exportações concluídas com sucesso")
                .register(registry);

        this.exportacoesFalhas = Counter.builder("exportacoes.processadas.total")
                .tag("status", "FALHA")
                .description("Total de exportações que falharam")
                .register(registry);

        this.documentosCriados = Counter.builder("documentos.criados.total")
                .description("Total de documentos cadastrados")
                .register(registry);

        this.tamanhoDocumentosBytes = DistributionSummary.builder("documentos.tamanho.bytes")
                .description("Distribuição de tamanhos de arquivos de documentos armazenados")
                .baseUnit("bytes")
                .register(registry);

        this.tempoProcessamentoExportacao = Timer.builder("exportacoes.processamento.tempo")
                .description("Tempo de processamento de exportações em segundo plano")
                .register(registry);
    }

    public void registrarExportacaoSolicitada() {
        exportacoesSolicitadas.increment();
    }

    public void registrarExportacaoConcluida() {
        exportacoesConcluidas.increment();
    }

    public void registrarExportacaoFalha() {
        exportacoesFalhas.increment();
    }

    public void registrarDocumentoCriado(long tamanhoBytes) {
        documentosCriados.increment();
        tamanhoDocumentosBytes.record(tamanhoBytes);
    }

    public void registrarTempoProcessamento(long duracaoMs) {
        tempoProcessamentoExportacao.record(duracaoMs, TimeUnit.MILLISECONDS);
    }
}
