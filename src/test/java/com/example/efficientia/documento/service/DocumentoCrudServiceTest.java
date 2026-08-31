package com.example.efficientia.documento.service;

import com.example.efficientia.documento.api.AtualizarDocumentoRequest;
import com.example.efficientia.documento.api.DocumentoMapper;
import com.example.efficientia.documento.api.DocumentoResponse;
import com.example.efficientia.documento.audit.DocumentoAuditEntity;
import com.example.efficientia.documento.audit.DocumentoAuditOperation;
import com.example.efficientia.documento.audit.DocumentoAuditRepository;
import com.example.efficientia.documento.exception.DocumentoConcorrenteException;
import com.example.efficientia.documento.persistence.DocumentoEntity;
import com.example.efficientia.documento.persistence.DocumentoRepository;
import com.example.efficientia.documento.storage.StorageService;
import com.example.efficientia.documento.validation.ArquivoValidator;
import com.example.efficientia.documento.validation.AssinaturaValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentoCrudServiceTest {

    private final DocumentoRepository repository = mock(DocumentoRepository.class);
    private final StorageService storage = mock(StorageService.class);
    private final DocumentoMapper mapper = mock(DocumentoMapper.class);
    private final DocumentoAuditRepository auditRepository = mock(DocumentoAuditRepository.class);
    private final DocumentoService service = new DocumentoService(
            repository, storage, mock(ArquivoValidator.class), mock(AssinaturaValidator.class),
            mock(DocumentoCursorCodec.class), mapper, auditRepository
    );

    @Test
    void deveAtualizarSomenteDescricaoComVersaoEsperadaEAuditar() {
        UUID id = UUID.randomUUID();
        DocumentoEntity entity = documento(id, 3L, "documentos/" + id + "/arquivo.pdf");
        DocumentoResponse response = mock(DocumentoResponse.class);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.paraResponse(entity)).thenReturn(response);

        assertThat(service.atualizar(id, new AtualizarDocumentoRequest(3L, "  nova descrição  ")))
                .isSameAs(response);
        assertThat(entity.getDescricao()).isEqualTo("nova descrição");

        ArgumentCaptor<DocumentoAuditEntity> captor = ArgumentCaptor.forClass(DocumentoAuditEntity.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getOperacao()).isEqualTo(DocumentoAuditOperation.ATUALIZACAO);
    }

    @Test
    void deveRejeitarVersaoDesatualizadaSemPersistir() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(documento(id, 4L, null)));

        assertThatThrownBy(() -> service.atualizar(id, new AtualizarDocumentoRequest(3L, "x")))
                .isInstanceOf(DocumentoConcorrenteException.class);
        verify(repository, never()).saveAndFlush(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    void deveExcluirArquivoMetadadosEAuditar() {
        UUID id = UUID.randomUUID();
        String storageKey = "documentos/" + id + "/arquivo.pdf";
        DocumentoEntity entity = documento(id, 0L, storageKey);
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        service.excluir(id);

        verify(repository).delete(entity);
        verify(repository).flush();
        verify(storage).remover(storageKey);
        verify(auditRepository).save(any(DocumentoAuditEntity.class));
    }

    @Test
    void deveExcluirAssinaturaTextualSemChamarStorage() {
        UUID id = UUID.randomUUID();
        DocumentoEntity entity = documento(id, 0L, null);
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        service.excluir(id);

        verify(storage, never()).remover(any());
        verify(auditRepository).save(any(DocumentoAuditEntity.class));
    }

    private DocumentoEntity documento(UUID id, Long versao, String storageKey) {
        DocumentoEntity entity = new DocumentoEntity();
        entity.setId(id);
        entity.setVersao(versao);
        entity.setStorageKey(storageKey);
        return entity;
    }
}
