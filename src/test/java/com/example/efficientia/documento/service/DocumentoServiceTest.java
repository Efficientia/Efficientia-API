package com.example.efficientia.documento.service;

import com.example.efficientia.documento.api.DocumentoMapper;
import com.example.efficientia.documento.api.DocumentoListagemRequest;
import com.example.efficientia.documento.api.AssinaturaTextoRequest;
import com.example.efficientia.documento.api.DocumentoMetadataRequest;
import com.example.efficientia.documento.api.AtualizarDocumentoRequest;
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
import com.example.efficientia.documento.audit.DocumentoAuditEntity;
import com.example.efficientia.documento.audit.DocumentoAuditOperation;
import com.example.efficientia.security.DocumentoAccessPolicy;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.InputStream;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.nio.charset.StandardCharsets;

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
    private DocumentoAuditRepository auditRepository;
    private RelatorioViagemRepository relatorioViagemRepository;
    private DocumentoService service;

    @BeforeEach
    void setUp() {
        repository = mock(DocumentoRepository.class);
        storage = mock(StorageService.class);
        auditRepository = mock(DocumentoAuditRepository.class);
        relatorioViagemRepository = mock(RelatorioViagemRepository.class);
        service = new DocumentoService(
                repository,
                storage,
                new ArquivoValidator(),
                new AssinaturaValidator(),
                new DocumentoCursorCodec(),
                new DocumentoMapper(),
                auditRepository,
                new DocumentoAccessPolicy(relatorioViagemRepository),
                relatorioViagemRepository
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
    void deveRetornarAssinaturaTextualExistenteSemCriarOutra() {
        UUID idempotencyKey = UUID.randomUUID();
        DocumentoEntity existente = entidadeExistente(idempotencyKey);
        existente.setTipoDocumento(TipoDocumento.ASSINATURA);
        existente.setOrigem(OrigemDocumento.TEXTO);
        existente.setModalidadeAssinatura(ModalidadeAssinatura.TEXTO);
        existente.setPapelAssinante(PapelAssinante.MOTORISTA);
        existente.setTextoAssinatura("Já salva");
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existente));

        var response = service.criarAssinaturaTextual(requestTexto("Uma nova assinatura"), idempotencyKey);

        assertThat(response.id()).isEqualTo(existente.getId());
        assertThat(response.textoAssinatura()).isEqualTo("Já salva");
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(storage, relatorioViagemRepository);
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
    void devePreservarFalhaDePersistenciaQuandoCompensacaoDoStorageTambemFalhar() {
        UUID idempotencyKey = UUID.randomUUID();
        var falhaPersistencia = new DataIntegrityViolationException("falha JPA");
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(storage.salvar(any(UUID.class), anyString(), eq("application/pdf"), any(InputStream.class)))
                .thenReturn(armazenado("documentos/orfao.pdf"));
        when(repository.saveAndFlush(any())).thenThrow(falhaPersistencia);
        org.mockito.Mockito.doThrow(new IllegalStateException("storage indisponível"))
                .when(storage).remover("documentos/orfao.pdf");

        assertThatThrownBy(() -> service.criarComArquivo(requestRelatorio(), pdfValido(), idempotencyKey))
                .isSameAs(falhaPersistencia)
                .satisfies(failure -> {
                    assertThat(failure.getSuppressed()).hasSize(1);
                    assertThat(failure.getSuppressed()[0]).hasMessage("storage indisponível");
                });
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

    @Test
    void deveValidarMetadadosChaveEIdentificadorAntesDeAcessarPersistencia() {
        assertThatThrownBy(() -> service.criarComArquivo(null, pdfValido(), UUID.randomUUID()))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.criarComArquivo(
                new DocumentoMetadataRequest(null, TipoDocumento.RELATORIO_VIAGEM, OrigemDocumento.UPLOAD,
                        null, null, null, "descrição"), pdfValido(), UUID.randomUUID()))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.criarComArquivo(requestRelatorio(), pdfValido(), null))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.criarAssinaturaTextual(requestTexto("Assinatura"), null))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.criarAssinaturaTextual(null, UUID.randomUUID()))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);

        verify(storage, never()).salvar(any(), anyString(), anyString(), any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveBuscarConteudoComMetadadosDoDocumento() {
        UUID id = UUID.randomUUID();
        DocumentoEntity entity = entidadeExistente(UUID.randomUUID());
        entity.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        var resource = new ByteArrayResource("pdf bytes".getBytes(StandardCharsets.UTF_8));
        when(storage.abrir(entity.getStorageKey())).thenReturn(new com.example.efficientia.documento.storage.StoredDocument(
                resource, "application/pdf", 9));

        var response = service.buscarConteudo(id);

        assertThat(response.resource()).isSameAs(resource);
        assertThat(response.mimeType()).isEqualTo("application/pdf");
        assertThat(response.tamanhoBytes()).isEqualTo(9);
        assertThat(response.nomeOriginal()).isEqualTo("relatorio.pdf");
    }

    @Test
    void deveBuscarDocumentoExistenteERejeitarConteudoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        DocumentoEntity entity = entidadeExistente(UUID.randomUUID());
        entity.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        var response = service.buscar(id);
        assertThat(response.id()).isEqualTo(id);

        UUID ausente = UUID.randomUUID();
        when(repository.findById(ausente)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarConteudo(ausente))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoNaoEncontradoException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void deveRejeitarConteudoSemArquivoEIdsNulos() {
        UUID id = UUID.randomUUID();
        DocumentoEntity assinatura = entidadeExistente(UUID.randomUUID());
        assinatura.setId(id);
        assinatura.setStorageKey(null);
        when(repository.findById(id)).thenReturn(Optional.of(assinatura));

        assertThatThrownBy(() -> service.buscar(null))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.buscarConteudo(null))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.buscarConteudo(id))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoSemConteudoException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void deveAtualizarDescricaoNormalizadaEAuditar() {
        UUID id = UUID.randomUUID();
        DocumentoEntity entity = entidadeExistente(UUID.randomUUID());
        entity.setId(id);
        entity.setVersao(3L);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        var response = service.atualizar(id, new AtualizarDocumentoRequest(3L, "  documento revisado  "));

        assertThat(entity.getDescricao()).isEqualTo("documento revisado");
        assertThat(response.descricao()).isEqualTo("documento revisado");
        ArgumentCaptor<DocumentoAuditEntity> audit = ArgumentCaptor.forClass(DocumentoAuditEntity.class);
        verify(auditRepository).save(audit.capture());
        assertThat(audit.getValue().getDocumentoId()).isEqualTo(id);
        assertThat(audit.getValue().getOperacao()).isEqualTo(DocumentoAuditOperation.ATUALIZACAO);
        assertThat(audit.getValue().getDetalhes()).contains("versão 3");
    }

    @Test
    void deveNormalizarDescricaoVaziaParaNulaEAceitarVersaoNulaSomenteComoEntradaInvalida() {
        UUID id = UUID.randomUUID();
        DocumentoEntity entity = entidadeExistente(UUID.randomUUID());
        entity.setId(id);
        entity.setVersao(0L);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(entity)).thenReturn(entity);

        service.atualizar(id, new AtualizarDocumentoRequest(0L, "   "));

        assertThat(entity.getDescricao()).isNull();
        service.atualizar(id, new AtualizarDocumentoRequest(0L, null));
        assertThat(entity.getDescricao()).isNull();
        assertThatThrownBy(() -> service.atualizar(id, null))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.atualizar(id, new AtualizarDocumentoRequest(null, "x")))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.atualizar(null, new AtualizarDocumentoRequest(0L, "x")))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
    }

    @Test
    void deveRejeitarVersaoDesatualizadaEConverterFalhaOtimista() {
        UUID id = UUID.randomUUID();
        DocumentoEntity entity = entidadeExistente(UUID.randomUUID());
        entity.setId(id);
        entity.setVersao(2L);
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.atualizar(id, new AtualizarDocumentoRequest(1L, "alterada")))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoConcorrenteException.class);
        verify(repository, never()).saveAndFlush(any());

        when(repository.saveAndFlush(entity)).thenThrow(new ObjectOptimisticLockingFailureException(
                DocumentoEntity.class, id));
        assertThatThrownBy(() -> service.atualizar(id, new AtualizarDocumentoRequest(2L, "alterada")))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoConcorrenteException.class);
    }

    @Test
    void deveInformarDocumentoAusenteAoAtualizarOuExcluir() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(id, new AtualizarDocumentoRequest(0L, "descrição")))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.excluir(null))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        verify(repository, never()).delete(any());
        verifyNoInteractions(auditRepository, storage);
    }

    @Test
    void deveExcluirDocumentoERemoverArquivoImediatamenteSemTransacaoSincronizada() {
        UUID id = UUID.randomUUID();
        DocumentoEntity entity = entidadeExistente(UUID.randomUUID());
        entity.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        service.excluir(id);

        verify(auditRepository).save(any(DocumentoAuditEntity.class));
        verify(repository).delete(entity);
        verify(repository).flush();
        verify(storage).remover(entity.getStorageKey());
    }

    @Test
    void deveExcluirAssinaturaTextualSemStorageEAgendarRemocaoDoArquivoAposCommit() {
        DocumentoEntity textual = entidadeExistente(UUID.randomUUID());
        textual.setId(UUID.randomUUID());
        textual.setTipoDocumento(TipoDocumento.ASSINATURA);
        textual.setStorageKey(null);
        when(repository.findById(textual.getId())).thenReturn(Optional.of(textual));

        service.excluir(textual.getId());

        ArgumentCaptor<DocumentoAuditEntity> evento = ArgumentCaptor.forClass(DocumentoAuditEntity.class);
        verify(auditRepository).save(evento.capture());
        assertThat(evento.getValue().getDetalhes()).contains("sem acesso ao storage");
        verifyNoInteractions(storage);

        UUID arquivoId = UUID.randomUUID();
        DocumentoEntity arquivo = entidadeExistente(UUID.randomUUID());
        arquivo.setId(arquivoId);
        when(repository.findById(arquivoId)).thenReturn(Optional.of(arquivo));
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.excluir(arquivoId);
            verify(storage, never()).remover(arquivo.getStorageKey());
            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
            synchronizations.get(0).afterCommit();
            verify(storage).remover(arquivo.getStorageKey());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deveVincularAssinaturaTextualAoRelatorioDoPapelCorrespondente() {
        UUID idempotencyKey = UUID.randomUUID();
        RelatorioViagemEntity relatorio = new RelatorioViagemEntity();
        relatorio.setId(1);
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(DocumentoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(relatorioViagemRepository.findById(1)).thenReturn(Optional.of(relatorio));

        var response = service.criarAssinaturaTextual(requestTexto("Assinatura do motorista"), idempotencyKey);

        assertThat(relatorio.getUrlAssinaturaMotorista())
                .isEqualTo("/api/v1/documentos/" + response.id() + "/conteudo");
        verify(relatorioViagemRepository).save(relatorio);
    }

    @Test
    void deveVincularAssinaturaFotograficaDoManobristaAoRelatorio() {
        UUID idempotencyKey = UUID.randomUUID();
        RelatorioViagemEntity relatorio = new RelatorioViagemEntity();
        relatorio.setId(1);
        var request = new DocumentoMetadataRequest(
                1, TipoDocumento.ASSINATURA, OrigemDocumento.CAMERA, 3,
                PapelAssinante.MANOBRISTA, ModalidadeAssinatura.FOTO, "Foto da assinatura"
        );
        byte[] png = pngBytes();
        var arquivo = new MockMultipartFile("arquivo", "assinatura.png", "image/png", png);
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(storage.salvar(any(UUID.class), eq("assinatura.png"), eq("image/png"), any(InputStream.class)))
                .thenReturn(new ArquivoArmazenado("assinatura.png", "image/png", png.length,
                        "b".repeat(64), "documentos/assinatura.png"));
        when(repository.saveAndFlush(any(DocumentoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(relatorioViagemRepository.findById(1)).thenReturn(Optional.of(relatorio));

        var response = service.criarComArquivo(request, arquivo, idempotencyKey);

        assertThat(relatorio.getUrlAssinaturaManobrista())
                .isEqualTo("/api/v1/documentos/" + response.id() + "/conteudo");
        verify(relatorioViagemRepository).save(relatorio);
    }

    @Test
    void deveIgnorarAtualizacaoDeRelatorioAusenteEPapelSemCampoDeAssinatura() {
        UUID idempotencyKey = UUID.randomUUID();
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(DocumentoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(relatorioViagemRepository.findById(1)).thenReturn(Optional.empty());
        var request = new AssinaturaTextoRequest(
                1, TipoDocumento.ASSINATURA, OrigemDocumento.TEXTO, 3,
                PapelAssinante.FUNCIONARIO_FRIBOI, ModalidadeAssinatura.TEXTO,
                "Assinatura", "Registro interno"
        );

        service.criarAssinaturaTextual(request, idempotencyKey);

        verify(relatorioViagemRepository).findById(1);
        verify(relatorioViagemRepository, never()).save(any());
    }

    @Test
    void deveListarComCursorSemProximaPaginaEValidarOrdenacaoEParametros() {
        DocumentoEntity entity = entidadeExistente(UUID.randomUUID());
        entity.setCriadoEm(Instant.parse("2026-08-31T10:00:00Z"));
        when(repository.buscarCursor(any(DocumentoFiltro.class), any(), eq(5)))
                .thenReturn(new SliceImpl<>(List.of(entity), PageRequest.of(0, 5), false));

        var response = service.listar(listagem(null, "INICIO", 5));
        assertThat(response.proximoCursor()).isNull();
        assertThat(response.temMais()).isFalse();

        assertThatThrownBy(() -> service.listar(listagem(-1, null, 20)))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.listar(listagem(null, null, 101)))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.listar(new DocumentoListagemRequest(0, null, 10, "id,desc",
                0, null, null, null, null, null, null)))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.listar(new DocumentoListagemRequest(0, null, 10, "id,desc",
                null, -1, null, null, null, null, null)))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.listar(new DocumentoListagemRequest(0, null, 10, "id,desc",
                null, null, null, null, null, Instant.parse("2026-08-31T10:01:00Z"),
                Instant.parse("2026-08-31T10:00:00Z"))))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.listar(new DocumentoListagemRequest(0, null, 10, "naoPermitido,desc",
                1, null, null, null, null, null, null)))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
        assertThatThrownBy(() -> service.listar(new DocumentoListagemRequest(0, null, 10, "id,ascendente",
                1, null, null, null, null, null, null)))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
    }

    @Test
    void deveUsarOrdenacaoPadraoQuandoNaoInformadaENaoAceitarListagemNula() {
        var pageable = PageRequest.of(0, 20);
        when(repository.buscarPagina(any(DocumentoFiltro.class), any()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.listar(new DocumentoListagemRequest(null, null, 20, " ", null, null,
                null, null, null, null, null));

        ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(repository).buscarPagina(any(DocumentoFiltro.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("criadoEm").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
        assertThatThrownBy(() -> service.listar(null))
                .isInstanceOf(com.example.efficientia.documento.exception.DocumentoInvalidoException.class);
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

    private byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
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
