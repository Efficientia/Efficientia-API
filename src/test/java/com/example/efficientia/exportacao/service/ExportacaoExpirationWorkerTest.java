package com.example.efficientia.exportacao.service;

import com.example.efficientia.documento.storage.StorageService;
import com.example.efficientia.exportacao.domain.EstadoExportacao;
import com.example.efficientia.exportacao.persistence.ExportacaoEntity;
import com.example.efficientia.exportacao.persistence.ExportacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class ExportacaoExpirationWorkerTest {

    @Mock
    private ExportacaoRepository exportacaoRepository;

    @Mock
    private StorageService storageService;

    private ExportacaoExpirationWorker worker;

    @BeforeEach
    void setUp() {
        worker = new ExportacaoExpirationWorker(exportacaoRepository, storageService);
    }

    @Test
    void naoFazNadaQuandoNaoHaExportacoesExpiradas() {
        when(exportacaoRepository.findByEstadoInAndExpiraEmBefore(
                eq(List.of(EstadoExportacao.CONCLUIDA, EstadoExportacao.FALHA)),
                any(Instant.class)
        )).thenReturn(List.of());

        worker.expirarExportacoesAntigas();

        verify(exportacaoRepository).findByEstadoInAndExpiraEmBefore(
                eq(List.of(EstadoExportacao.CONCLUIDA, EstadoExportacao.FALHA)),
                any(Instant.class)
        );
        verify(exportacaoRepository, never()).save(any());
        verifyNoInteractions(storageService);
    }

    @Test
    void removeArquivoEMarcaExportacaoExpirada() {
        ExportacaoEntity exportacao = exportacao(EstadoExportacao.CONCLUIDA, "exportacoes/arquivo.zip");
        when(exportacaoRepository.findByEstadoInAndExpiraEmBefore(
                eq(List.of(EstadoExportacao.CONCLUIDA, EstadoExportacao.FALHA)),
                any(Instant.class)
        )).thenReturn(List.of(exportacao));

        worker.expirarExportacoesAntigas();

        verify(storageService).remover("exportacoes/arquivo.zip");
        verify(exportacaoRepository).save(exportacao);
        assertThat(exportacao.getEstado()).isEqualTo(EstadoExportacao.EXPIRADA);
    }

    @Test
    void marcaComoExpiradaSemRemoverArquivoQuandoNaoHaStorageKey() {
        ExportacaoEntity exportacao = exportacao(EstadoExportacao.FALHA, null);
        when(exportacaoRepository.findByEstadoInAndExpiraEmBefore(
                eq(List.of(EstadoExportacao.CONCLUIDA, EstadoExportacao.FALHA)),
                any(Instant.class)
        )).thenReturn(List.of(exportacao));

        worker.expirarExportacoesAntigas();

        verifyNoInteractions(storageService);
        verify(exportacaoRepository).save(exportacao);
        assertThat(exportacao.getEstado()).isEqualTo(EstadoExportacao.EXPIRADA);
    }

    @Test
    void continuaProcessandoOutrasExportacoesQuandoRemocaoDeArquivoFalha() {
        ExportacaoEntity falhaNaRemocao = exportacao(EstadoExportacao.CONCLUIDA, "exportacoes/erro.zip");
        ExportacaoEntity semArquivo = exportacao(EstadoExportacao.FALHA, null);
        doThrow(new IllegalStateException("storage indisponivel"))
                .when(storageService).remover("exportacoes/erro.zip");
        when(exportacaoRepository.findByEstadoInAndExpiraEmBefore(
                eq(List.of(EstadoExportacao.CONCLUIDA, EstadoExportacao.FALHA)),
                any(Instant.class)
        )).thenReturn(List.of(falhaNaRemocao, semArquivo));

        worker.expirarExportacoesAntigas();

        verify(exportacaoRepository, never()).save(falhaNaRemocao);
        verify(exportacaoRepository).save(semArquivo);
        assertThat(falhaNaRemocao.getEstado()).isEqualTo(EstadoExportacao.CONCLUIDA);
        assertThat(semArquivo.getEstado()).isEqualTo(EstadoExportacao.EXPIRADA);
    }

    @Test
    void consultaSomenteEstadosTerminaisEUsaInstanteAtualComoLimite() {
        ArgumentCaptor<Instant> limite = ArgumentCaptor.forClass(Instant.class);
        Instant antesDaChamada = Instant.now();
        when(exportacaoRepository.findByEstadoInAndExpiraEmBefore(
                eq(List.of(EstadoExportacao.CONCLUIDA, EstadoExportacao.FALHA)),
                any(Instant.class)
        )).thenReturn(List.of());

        worker.expirarExportacoesAntigas();

        verify(exportacaoRepository).findByEstadoInAndExpiraEmBefore(
                eq(List.of(EstadoExportacao.CONCLUIDA, EstadoExportacao.FALHA)),
                limite.capture()
        );
        assertThat(limite.getValue()).isBetween(antesDaChamada, Instant.now());
    }

    private ExportacaoEntity exportacao(EstadoExportacao estado, String storageKey) {
        ExportacaoEntity exportacao = new ExportacaoEntity();
        exportacao.setId(UUID.randomUUID());
        exportacao.setEstado(estado);
        exportacao.setStorageKey(storageKey);
        return exportacao;
    }
}
