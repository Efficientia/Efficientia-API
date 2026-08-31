package com.example.efficientia.documento.service;

import com.example.efficientia.documento.api.DocumentoMapper;
import com.example.efficientia.documento.api.DocumentoListagemRequest;
import com.example.efficientia.documento.api.AssinaturaTextoRequest;
import com.example.efficientia.documento.api.DocumentoMetadataRequest;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.PapelAssinante;
import com.example.efficientia.documento.domain.TipoDocumento;
import com.example.efficientia.documento.persistence.DocumentoEntity;
import com.example.efficientia.documento.persistence.DocumentoFiltro;
import com.example.efficientia.documento.persistence.DocumentoRepository;
import com.example.efficientia.documento.storage.ArquivoArmazenado;
import com.example.efficientia.documento.storage.StorageService;
import com.example.efficientia.documento.validation.ArquivoValidator;
import com.example.efficientia.documento.validation.AssinaturaValidator;
import com.example.efficientia.documento.audit.DocumentoAuditRepository;
import com.example.efficientia.security.DocumentoAccessPolicy;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DocumentoServiceTest {

    private DocumentoRepository repository;
    private StorageService storage;
    private DocumentoService service;

    @BeforeEach
    void setUp() {
        repository = mock(DocumentoRepository.class);
        storage = mock(StorageService.class);
        service = new DocumentoService(
                repository,
                storage,
                new ArquivoValidator(),
                new AssinaturaValidator(),
                new DocumentoCursorCodec(),
                new DocumentoMapper(),
                mock(DocumentoAuditRepository.class),
                new DocumentoAccessPolicy(mock(RelatorioViagemRepository.class))
        );
    }

    @Test
    void deveSalvarStorageAntesDosMetadadosERetornarContratoPublico() {
        UUID idempotencyKey = UUID.randomUUID();
        var request = requestRelatorio();
        var arquivo = pdfValido();
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(storage.salvar(any(UUID.class), eq("relatorio.pdf"), eq("application/pdf"), any(InputStream.class)))
                .thenReturn(armazenado("documentos/key.pdf"));
        when(repository.saveAndFlush(any(DocumentoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.criarComArquivo(request, arquivo, idempotencyKey);

        ArgumentCaptor<DocumentoEntity> captor = ArgumentCaptor.forClass(DocumentoEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStorageKey()).isEqualTo("documentos/key.pdf");
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(response.id()).isNotNull();
        assertThat(response.conteudoUrl()).isEqualTo("/api/v1/documentos/" + response.id() + "/conteudo");
    }

    @Test
    void deveRetornarDocumentoExistenteSemTocarNoStorage() {
        UUID idempotencyKey = UUID.randomUUID();
        DocumentoEntity existente = entidadeExistente(idempotencyKey);
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existente));

        var response = service.criarComArquivo(requestRelatorio(), pdfValido(), idempotencyKey);

        assertThat(response.id()).isEqualTo(existente.getId());
        verifyNoInteractions(storage);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRemoverArquivoSePersistenciaFalhar() {
        UUID idempotencyKey = UUID.randomUUID();
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(storage.salvar(any(UUID.class), anyString(), eq("application/pdf"), any(InputStream.class)))
                .thenReturn(armazenado("documentos/orfao.pdf"));
        when(repository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("falha JPA"));

        assertThatThrownBy(() -> service.criarComArquivo(requestRelatorio(), pdfValido(), idempotencyKey))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(storage).remover("documentos/orfao.pdf");
    }

    @Test
    void devePersistirAssinaturaTextualNormalizadaSemStorage() {
        UUID idempotencyKey = UUID.randomUUID();
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(DocumentoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.criarAssinaturaTextual(requestTexto("  Joa\u0303o da Silva  "), idempotencyKey);

        ArgumentCaptor<DocumentoEntity> captor = ArgumentCaptor.forClass(DocumentoEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTextoAssinatura()).isEqualTo("João da Silva");
        assertThat(response.textoAssinatura()).isEqualTo("João da Silva");
        assertThat(response.conteudoUrl()).isNull();
        verifyNoInteractions(storage);
    }

    @Test
    void deveRejeitarHtmlSemChamarStorageOuRepositorySave() {
        UUID idempotencyKey = UUID.randomUUID();
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criarAssinaturaTextual(
                requestTexto("<script>alert(1)</script>"),
                idempotencyKey
        )).isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);

        verifyNoInteractions(storage);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveListarPaginaComFiltrosETotais() {
        DocumentoEntity entity = entidadeExistente(UUID.randomUUID());
        entity.setCriadoEm(Instant.parse("2026-08-31T10:00:00Z"));
        var pageable = PageRequest.of(0, 20);
        when(repository.buscarPagina(any(DocumentoFiltro.class), any()))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        var response = service.listar(listagem(0, null, 20));

        ArgumentCaptor<DocumentoFiltro> filtro = ArgumentCaptor.forClass(DocumentoFiltro.class);
        verify(repository).buscarPagina(filtro.capture(), any());
        assertThat(filtro.getValue().viagemId()).isEqualTo(1);
        assertThat(response.page()).isZero();
        assertThat(response.totalElementos()).isEqualTo(1);
        assertThat(response.itens()).extracting(item -> item.id()).containsExactly(entity.getId());
    }

    @Test
    void deveListarPorCursorEGerarProximoCursorDoUltimoItem() {
        DocumentoEntity primeiro = entidadeExistente(UUID.randomUUID());
        primeiro.setCriadoEm(Instant.parse("2026-08-31T10:00:00Z"));
        DocumentoEntity ultimo = entidadeExistente(UUID.randomUUID());
        ultimo.setCriadoEm(Instant.parse("2026-08-31T10:01:00Z"));
        when(repository.buscarCursor(any(DocumentoFiltro.class), eq(null), eq(2)))
                .thenReturn(new SliceImpl<>(List.of(primeiro, ultimo), PageRequest.of(0, 2), true));

        var response = service.listar(listagem(null, "INICIO", 2));

        var cursor = new DocumentoCursorCodec().decodificar(response.proximoCursor());
        assertThat(response.page()).isNull();
        assertThat(response.totalElementos()).isNull();
        assertThat(response.temMais()).isTrue();
        assertThat(cursor.criadoEm()).isEqualTo(ultimo.getCriadoEm());
        assertThat(cursor.id()).isEqualTo(ultimo.getId());
    }

    @Test
    void deveRejeitarCombinacaoDePageECursor() {
        assertThatThrownBy(() -> service.listar(listagem(0, "INICIO", 20)))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
    }

    @Test
    void deveInformarDocumentoNaoEncontradoNaConsultaIndividual() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(id))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoNaoEncontradoException.class);
    }

    private DocumentoMetadataRequest requestRelatorio() {
        return new DocumentoMetadataRequest(
                1,
                TipoDocumento.RELATORIO_VIAGEM,
                OrigemDocumento.UPLOAD,
                null,
                null,
                null,
                "Relatório final"
        );
    }

    private AssinaturaTextoRequest requestTexto(String texto) {
        return new AssinaturaTextoRequest(
                1,
                TipoDocumento.ASSINATURA,
                OrigemDocumento.TEXTO,
                2,
                PapelAssinante.MOTORISTA,
                ModalidadeAssinatura.TEXTO,
                texto,
                "Assinatura acessível"
        );
    }

    private DocumentoListagemRequest listagem(Integer page, String cursor, int size) {
        return new DocumentoListagemRequest(
                page,
                cursor,
                size,
                "criadoEm,desc",
                1,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private MockMultipartFile pdfValido() {
        return new MockMultipartFile(
                "arquivo",
                "relatorio.pdf",
                "application/pdf",
                "%PDF-1.7\nconteudo".getBytes(java.nio.charset.StandardCharsets.US_ASCII)
        );
    }

    private ArquivoArmazenado armazenado(String key) {
        return new ArquivoArmazenado(
                "relatorio.pdf",
                "application/pdf",
                18,
                "a".repeat(64),
                key
        );
    }

    private DocumentoEntity entidadeExistente(UUID idempotencyKey) {
        DocumentoEntity entity = new DocumentoEntity();
        entity.setId(UUID.randomUUID());
        entity.setViagemId(1);
        entity.setTipoDocumento(TipoDocumento.RELATORIO_VIAGEM);
        entity.setOrigem(OrigemDocumento.UPLOAD);
        entity.setNomeOriginal("relatorio.pdf");
        entity.setMimeType("application/pdf");
        entity.setTamanhoBytes(18L);
        entity.setSha256("a".repeat(64));
        entity.setStorageKey("documentos/existente.pdf");
        entity.setIdempotencyKey(idempotencyKey);
        return entity;
    }
}
