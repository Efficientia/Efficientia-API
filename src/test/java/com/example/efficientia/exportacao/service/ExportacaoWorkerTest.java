package com.example.efficientia.exportacao.service;

import com.example.efficientia.documento.persistence.DocumentoEntity;
import com.example.efficientia.documento.persistence.DocumentoRepository;
import com.example.efficientia.documento.storage.ArquivoArmazenado;
import com.example.efficientia.documento.storage.StorageService;
import com.example.efficientia.documento.storage.StoredDocument;
import com.example.efficientia.exportacao.domain.EstadoExportacao;
import com.example.efficientia.exportacao.persistence.ExportacaoEntity;
import com.example.efficientia.exportacao.persistence.ExportacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportacaoWorkerTest {

    @Mock
    private ExportacaoRepository exportacaoRepository;

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private StorageService storageService;

    private ExportacaoWorker worker;

    @BeforeEach
    void setUp() {
        worker = new ExportacaoWorker(exportacaoRepository, documentoRepository, storageService);
    }

    @Test
    void naoDeveFazerNadaQuandoNaoHouverExportacaoNaFila() {
        when(exportacaoRepository.findFirstByEstadoOrderByCriadoEmAsc(EstadoExportacao.NA_FILA))
                .thenReturn(Optional.empty());

        worker.processarProximaExportacao();

        verify(exportacaoRepository, never()).save(any());
        verify(storageService, never()).abrir(anyString());
    }

    @Test
    void deveIgnorarQuandoHouverConflitoDeConcorrencia() {
        ExportacaoEntity exportacao = new ExportacaoEntity();
        exportacao.setId(UUID.randomUUID());

        when(exportacaoRepository.findFirstByEstadoOrderByCriadoEmAsc(EstadoExportacao.NA_FILA))
                .thenReturn(Optional.of(exportacao));
        when(exportacaoRepository.save(exportacao)).thenThrow(new ObjectOptimisticLockingFailureException(ExportacaoEntity.class, exportacao.getId()));

        worker.processarProximaExportacao();

        verify(exportacaoRepository, times(1)).save(exportacao);
        verify(storageService, never()).abrir(anyString());
    }

    @Test
    void deveProcessarExportacaoComSucesso() {
        UUID docId1 = UUID.randomUUID();
        UUID docId2 = UUID.randomUUID();

        ExportacaoEntity exportacao = new ExportacaoEntity();
        exportacao.setId(UUID.randomUUID());
        exportacao.setDocumentoIds(List.of(docId1, docId2));

        when(exportacaoRepository.findFirstByEstadoOrderByCriadoEmAsc(EstadoExportacao.NA_FILA))
                .thenReturn(Optional.of(exportacao));

        DocumentoEntity doc1 = new DocumentoEntity();
        doc1.setId(docId1);
        doc1.setStorageKey("docs/doc1.pdf");
        doc1.setNomeOriginal("doc1.pdf");

        DocumentoEntity doc2 = new DocumentoEntity();
        doc2.setId(docId2);
        doc2.setStorageKey("docs/doc2.pdf");
        doc2.setNomeOriginal("doc2.pdf");

        when(documentoRepository.findById(docId1)).thenReturn(Optional.of(doc1));
        when(documentoRepository.findById(docId2)).thenReturn(Optional.of(doc2));

        StoredDocument stored1 = new StoredDocument(new ByteArrayResource("conteudo1".getBytes()), "application/pdf", 9);
        StoredDocument stored2 = new StoredDocument(new ByteArrayResource("conteudo2".getBytes()), "application/pdf", 9);

        when(storageService.abrir("docs/doc1.pdf")).thenReturn(stored1);
        when(storageService.abrir("docs/doc2.pdf")).thenReturn(stored2);

        ArquivoArmazenado arquivoSalvo = new ArquivoArmazenado(
                "exportacao-" + exportacao.getId() + ".zip",
                "application/zip",
                1024L,
                "a".repeat(64),
                "exportacoes/" + exportacao.getId() + "/arquivo.zip"
        );
        java.util.List<EstadoExportacao> estadosSalvos = new java.util.ArrayList<>();
        when(exportacaoRepository.save(any(ExportacaoEntity.class))).thenAnswer(invocation -> {
            ExportacaoEntity entity = invocation.getArgument(0);
            estadosSalvos.add(entity.getEstado());
            return entity;
        });
        when(storageService.salvar(eq(exportacao.getId()), anyString(), eq("application/zip"), any(InputStream.class)))
                .thenReturn(arquivoSalvo);

        worker.processarProximaExportacao();

        assertThat(estadosSalvos).containsExactly(EstadoExportacao.PROCESSANDO, EstadoExportacao.CONCLUIDA);
        assertThat(exportacao.getTamanhoZipBytes()).isNotNull();
        assertThat(exportacao.getStorageKey()).isEqualTo("exportacoes/" + exportacao.getId() + "/arquivo.zip");
        assertThat(exportacao.getNomeArquivo()).isEqualTo("exportacao-" + exportacao.getId() + ".zip");
        assertThat(exportacao.getConcluidoEm()).isNotNull();
        assertThat(exportacao.getExpiraEm()).isNotNull();
    }

    @Test
    void deveMudarEstadoParaFalhaEmCasoDeErro() {
        UUID docId1 = UUID.randomUUID();

        ExportacaoEntity exportacao = new ExportacaoEntity();
        exportacao.setId(UUID.randomUUID());
        exportacao.setDocumentoIds(List.of(docId1));

        when(exportacaoRepository.findFirstByEstadoOrderByCriadoEmAsc(EstadoExportacao.NA_FILA))
                .thenReturn(Optional.of(exportacao));
        when(exportacaoRepository.save(any(ExportacaoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(documentoRepository.findById(docId1)).thenReturn(Optional.empty()); // simula erro

        worker.processarProximaExportacao();

        ArgumentCaptor<ExportacaoEntity> captor = ArgumentCaptor.forClass(ExportacaoEntity.class);
        verify(exportacaoRepository, times(2)).save(captor.capture());

        ExportacaoEntity finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getEstado()).isEqualTo(EstadoExportacao.FALHA);
        assertThat(finalSave.getErroCodigo()).isEqualTo("ERRO_PROCESSAMENTO");
    }
}
