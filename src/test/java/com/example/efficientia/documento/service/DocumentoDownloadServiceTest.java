package com.example.efficientia.documento.service;

import com.example.efficientia.documento.api.DocumentoMapper;
import com.example.efficientia.documento.exception.DocumentoNaoEncontradoException;
import com.example.efficientia.documento.exception.DocumentoSemConteudoException;
import com.example.efficientia.documento.persistence.DocumentoEntity;
import com.example.efficientia.documento.persistence.DocumentoRepository;
import com.example.efficientia.documento.storage.StorageService;
import com.example.efficientia.documento.storage.StoredDocument;
import com.example.efficientia.documento.validation.ArquivoValidator;
import com.example.efficientia.documento.validation.AssinaturaValidator;
import com.example.efficientia.documento.audit.DocumentoAuditRepository;
import com.example.efficientia.security.DocumentoAccessPolicy;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentoDownloadServiceTest {

    private final DocumentoRepository repository = mock(DocumentoRepository.class);
    private final StorageService storage = mock(StorageService.class);
    private final DocumentoService service = new DocumentoService(
            repository,
            storage,
            mock(ArquivoValidator.class),
            mock(AssinaturaValidator.class),
            mock(DocumentoCursorCodec.class),
            mock(DocumentoMapper.class),
            mock(DocumentoAuditRepository.class),
            new DocumentoAccessPolicy(mock(RelatorioViagemRepository.class))
    );

    @Test
    void deveAbrirResourceSemMaterializarBytesNoService() {
        UUID id = UUID.randomUUID();
        DocumentoEntity entity = documento(id, "documentos/" + id + "/arquivo.pdf");
        ByteArrayResource resource = new ByteArrayResource("%PDF-conteudo".getBytes());
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(storage.abrir(entity.getStorageKey()))
                .thenReturn(new StoredDocument(resource, "application/pdf", 13));

        DocumentoConteudo conteudo = service.buscarConteudo(id);

        assertThat(conteudo.resource()).isSameAs(resource);
        assertThat(conteudo.mimeType()).isEqualTo("application/pdf");
        assertThat(conteudo.nomeOriginal()).isEqualTo("relatorio final.pdf");
    }

    @Test
    void deveRejeitarAssinaturaTextualSemConsultarStorage() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(documento(id, null)));

        assertThatThrownBy(() -> service.buscarConteudo(id))
                .isInstanceOf(DocumentoSemConteudoException.class);
        verify(storage, never()).abrir(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveRetornar404AntesDeConsultarStorage() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarConteudo(id))
                .isInstanceOf(DocumentoNaoEncontradoException.class);
        verify(storage, never()).abrir(org.mockito.ArgumentMatchers.any());
    }

    private DocumentoEntity documento(UUID id, String storageKey) {
        DocumentoEntity entity = new DocumentoEntity();
        entity.setId(id);
        entity.setNomeOriginal("relatorio final.pdf");
        entity.setStorageKey(storageKey);
        return entity;
    }
}
