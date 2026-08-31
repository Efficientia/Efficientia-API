package com.example.efficientia.documento.service;

import com.example.efficientia.documento.api.DocumentoListagemRequest;
import com.example.efficientia.documento.api.DocumentoMapper;
import com.example.efficientia.documento.audit.DocumentoAuditRepository;
import com.example.efficientia.documento.persistence.DocumentoFiltro;
import com.example.efficientia.documento.persistence.DocumentoRepository;
import com.example.efficientia.documento.storage.StorageService;
import com.example.efficientia.documento.validation.ArquivoValidator;
import com.example.efficientia.documento.validation.AssinaturaValidator;
import com.example.efficientia.security.DocumentoAccessContext;
import com.example.efficientia.security.DocumentoAccessPolicy;
import com.example.efficientia.security.DocumentoAccessScope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentoSecurityScopeServiceTest {

    @Test
    void deveLevarEscopoDoMotoristaAConsultaNoBanco() {
        DocumentoRepository repository = mock(DocumentoRepository.class);
        DocumentoAccessPolicy accessPolicy = mock(DocumentoAccessPolicy.class);
        when(accessPolicy.contextoAtual())
                .thenReturn(new DocumentoAccessContext(12, DocumentoAccessScope.MOTORISTA));
        when(repository.buscarPagina(any(), any())).thenReturn(new PageImpl<>(List.of()));
        DocumentoService service = new DocumentoService(
                repository,
                mock(StorageService.class),
                mock(ArquivoValidator.class),
                mock(AssinaturaValidator.class),
                mock(DocumentoCursorCodec.class),
                mock(DocumentoMapper.class),
                mock(DocumentoAuditRepository.class),
                accessPolicy
        );

        service.listar(new DocumentoListagemRequest(
                0, null, 20, "criadoEm,desc", null, null,
                null, null, null, null, null
        ));

        ArgumentCaptor<DocumentoFiltro> captor = ArgumentCaptor.forClass(DocumentoFiltro.class);
        verify(repository).buscarPagina(captor.capture(), any());
        assertThat(captor.getValue().usuarioEscopoId()).isEqualTo(12);
        assertThat(captor.getValue().accessScope()).isEqualTo(DocumentoAccessScope.MOTORISTA);
    }
}
