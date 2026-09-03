package com.example.efficientia.exportacao.service;

import com.example.efficientia.documento.persistence.DocumentoEntity;
import com.example.efficientia.documento.persistence.DocumentoRepository;
import com.example.efficientia.exportacao.api.SolicitarExportacaoRequest;
import com.example.efficientia.exportacao.domain.EstadoExportacao;
import com.example.efficientia.exportacao.exception.ExportacaoConflitoException;
import com.example.efficientia.exportacao.exception.ExportacaoInvalidaException;
import com.example.efficientia.exportacao.exception.ExportacaoNaoEncontradaException;
import com.example.efficientia.exportacao.persistence.ExportacaoEntity;
import com.example.efficientia.exportacao.persistence.ExportacaoRepository;
import com.example.efficientia.security.DocumentoAccessPolicy;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExportacaoServiceTest {

    private static final long QUINHENTOS_MIB = 500L * 1024L * 1024L;

    private ExportacaoRepository repository;
    private ExportacaoPersistenceService persistenceService;
    private DocumentoRepository documentoRepository;
    private DocumentoAccessPolicy accessPolicy;
    private ExportacaoService service;

    @BeforeEach
    void setUp() {
        repository = mock(ExportacaoRepository.class);
        persistenceService = mock(ExportacaoPersistenceService.class);
        documentoRepository = mock(DocumentoRepository.class);
        accessPolicy = mock(DocumentoAccessPolicy.class);
        service = new ExportacaoService(repository, persistenceService, documentoRepository, accessPolicy);
        when(accessPolicy.usuarioAtualId()).thenReturn(Optional.of(42));
        when(persistenceService.criar(any(ExportacaoEntity.class))).thenAnswer(invocation -> {
            ExportacaoEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCriadoEm(Instant.parse("2026-09-03T10:00:00Z"));
            entity.setAtualizadoEm(Instant.parse("2026-09-03T10:00:00Z"));
            return entity;
        });
    }

    @Test
    void deveAceitarUmDocumentoEPersistirSomenteNaFilaComRespostaPublicaSegura() {
        UUID documentoId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(documentoRepository.findById(documentoId))
                .thenReturn(Optional.of(documentoComArquivo(1024L)));

        var response = service.solicitar(request(documentoId), idempotencyKey);

        ArgumentCaptor<ExportacaoEntity> captor = ArgumentCaptor.forClass(ExportacaoEntity.class);
        verify(persistenceService).criar(captor.capture());
        ExportacaoEntity persistida = captor.getValue();
        assertThat(persistida.getDocumentoIds()).containsExactly(documentoId);
        assertThat(persistida.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(persistida.getEstado()).isEqualTo(EstadoExportacao.NA_FILA);
        assertThat(persistida.getSolicitadoPor()).isEqualTo(42);
        assertThat(persistida.getTamanhoOrigemBytes()).isEqualTo(1024L);
        assertThat(persistida.getTamanhoZipBytes()).isNull();
        assertThat(persistida.getStorageKey()).isNull();
        assertThat(persistida.getNomeArquivo()).isNull();
        assertThat(persistida.getErroCodigo()).isNull();
        assertThat(response.estado()).isEqualTo(EstadoExportacao.NA_FILA);
        assertThat(response.quantidadeDocumentos()).isEqualTo(1);
        assertThat(response.tamanhoOrigemBytes()).isEqualTo(1024L);
        assertThat(response.tamanhoZipBytes()).isNull();
        assertThat(response.conteudoUrl()).isNull();
    }

    @Test
    void deveAceitarExatamenteQuinhentosDocumentos() {
        List<UUID> ids = IntStream.range(0, 500)
                .mapToObj(ignored -> UUID.randomUUID())
                .toList();
        UUID idempotencyKey = UUID.randomUUID();
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(documentoRepository.findById(any(UUID.class)))
                .thenReturn(Optional.of(documentoComArquivo(1L)));

        var response = service.solicitar(new SolicitarExportacaoRequest(ids), idempotencyKey);

        ArgumentCaptor<ExportacaoEntity> captor = ArgumentCaptor.forClass(ExportacaoEntity.class);
        verify(persistenceService).criar(captor.capture());
        assertThat(captor.getValue().getDocumentoIds()).containsExactlyElementsOf(ids);
        assertThat(response.quantidadeDocumentos()).isEqualTo(500);
        assertThat(response.tamanhoOrigemBytes()).isEqualTo(500L);
    }

    @Test
    void deveRejeitarZeroEOuMaisDeQuinhentosDocumentos() {
        List<UUID> idsDemais = IntStream.range(0, 501)
                .mapToObj(ignored -> UUID.randomUUID())
                .toList();

        assertThatThrownBy(() -> service.solicitar(
                new SolicitarExportacaoRequest(List.of()),
                UUID.randomUUID()
        )).isInstanceOf(ExportacaoInvalidaException.class);
        assertThatThrownBy(() -> service.solicitar(
                new SolicitarExportacaoRequest(idsDemais),
                UUID.randomUUID()
        )).isInstanceOf(ExportacaoInvalidaException.class);

        verifyNoInteractions(documentoRepository);
        verify(persistenceService, never()).criar(any());
    }

    @Test
    void deveAceitarTotalExatamenteIgualAQuinhentosMiB() {
        UUID documentoId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(documentoRepository.findById(documentoId))
                .thenReturn(Optional.of(documentoComArquivo(QUINHENTOS_MIB)));

        var response = service.solicitar(request(documentoId), idempotencyKey);

        assertThat(response.tamanhoOrigemBytes()).isEqualTo(QUINHENTOS_MIB);
        verify(persistenceService).criar(any(ExportacaoEntity.class));
    }

    @Test
    void deveRejeitarTotalUmByteAcimaDeQuinhentosMiB() {
        UUID documentoId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(documentoRepository.findById(documentoId))
                .thenReturn(Optional.of(documentoComArquivo(QUINHENTOS_MIB + 1L)));

        assertThatThrownBy(() -> service.solicitar(request(documentoId), idempotencyKey))
                .isInstanceOf(ExportacaoInvalidaException.class)
                .hasMessageContaining("500 MiB");

        verify(persistenceService, never()).criar(any());
    }

    @Test
    void deveRejeitarIdsDuplicadosAntesDeConsultarDocumentos() {
        UUID documentoId = UUID.randomUUID();

        assertThatThrownBy(() -> service.solicitar(
                new SolicitarExportacaoRequest(List.of(documentoId, documentoId)),
                UUID.randomUUID()
        )).isInstanceOf(ExportacaoInvalidaException.class);

        verifyNoInteractions(documentoRepository);
        verify(persistenceService, never()).criar(any());
    }

    @Test
    void deveRejeitarDocumentoInexistente() {
        UUID documentoId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(documentoRepository.findById(documentoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.solicitar(request(documentoId), idempotencyKey))
                .isInstanceOf(ExportacaoInvalidaException.class)
                .hasMessageContaining(documentoId.toString());

        verify(persistenceService, never()).criar(any());
    }

    @Test
    void deveRejeitarDocumentoSomenteTextual() {
        UUID documentoId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        DocumentoEntity somenteTexto = new DocumentoEntity();
        somenteTexto.setTamanhoBytes(null);
        somenteTexto.setStorageKey(null);
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(somenteTexto));

        assertThatThrownBy(() -> service.solicitar(request(documentoId), idempotencyKey))
                .isInstanceOf(ExportacaoInvalidaException.class)
                .hasMessageContaining("conteúdo binário");

        verify(persistenceService, never()).criar(any());
    }

    @Test
    void deveRejeitarDocumentoForaDoEscopoDoUsuario() {
        UUID documentoId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        DocumentoEntity documento = documentoComArquivo(1024L);
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documento));
        doThrow(new AccessDeniedException("Documento fora do escopo do usuário."))
                .when(accessPolicy).verificarDocumento(documento);

        assertThatThrownBy(() -> service.solicitar(request(documentoId), idempotencyKey))
                .isInstanceOf(AccessDeniedException.class);

        verify(persistenceService, never()).criar(any());
    }

    @Test
    void deveRetornarMesmaExportacaoParaMesmaChaveEIdsNaMesmaOrdem() {
        UUID primeiroId = UUID.randomUUID();
        UUID segundoId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        ExportacaoEntity existente = exportacaoExistente(
                idempotencyKey,
                List.of(primeiroId, segundoId),
                EstadoExportacao.NA_FILA
        );
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existente));

        var response = service.solicitar(
                new SolicitarExportacaoRequest(List.of(primeiroId, segundoId)),
                idempotencyKey
        );

        assertThat(response.id()).isEqualTo(existente.getId());
        assertThat(response.quantidadeDocumentos()).isEqualTo(2);
        verifyNoInteractions(documentoRepository);
        verify(persistenceService, never()).criar(any());
    }

    @Test
    void deveConflitarQuandoMesmaChaveTemIdsEmOrdemDiferente() {
        UUID primeiroId = UUID.randomUUID();
        UUID segundoId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(
                exportacaoExistente(
                        idempotencyKey,
                        List.of(primeiroId, segundoId),
                        EstadoExportacao.NA_FILA
                )
        ));

        assertThatThrownBy(() -> service.solicitar(
                new SolicitarExportacaoRequest(List.of(segundoId, primeiroId)),
                idempotencyKey
        )).isInstanceOf(ExportacaoConflitoException.class);

        verifyNoInteractions(documentoRepository);
        verify(persistenceService, never()).criar(any());
    }
    @Test
    void deveImpedirReplayDeOutroUsuario() {
        UUID primeiroId = UUID.randomUUID();
        UUID segundoId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        ExportacaoEntity existente = exportacaoExistente(
                idempotencyKey,
                List.of(primeiroId, segundoId),
                EstadoExportacao.NA_FILA
        );
        existente.setSolicitadoPor(7);
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existente));
        when(accessPolicy.usuarioAtualId()).thenReturn(Optional.of(42));

        assertThatThrownBy(() -> service.solicitar(
                new SolicitarExportacaoRequest(List.of(primeiroId, segundoId)),
                idempotencyKey
        )).isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(documentoRepository);
        verify(persistenceService, never()).criar(any());
    }

    @Test
    void deveRecuperarLinhaVencedoraAposFalhaDeConcorrencia() {
        UUID documentoId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        ExportacaoEntity vencedora = exportacaoExistente(
                idempotencyKey,
                List.of(documentoId),
                EstadoExportacao.NA_FILA
        );

        when(repository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(vencedora));
        when(documentoRepository.findById(documentoId))
                .thenReturn(Optional.of(documentoComArquivo(1024L)));
        doThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"))
                .when(persistenceService).criar(any(ExportacaoEntity.class));

        var response = service.solicitar(request(documentoId), idempotencyKey);

        assertThat(response.id()).isEqualTo(vencedora.getId());
        verify(persistenceService).criar(any(ExportacaoEntity.class));
    }

    @Test
    void deveConflitarQuandoLinhaVencedoraTemCorpoDiferente() {
        UUID documentoId = UUID.randomUUID();
        UUID outroId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        ExportacaoEntity vencedora = exportacaoExistente(
                idempotencyKey,
                List.of(outroId),
                EstadoExportacao.NA_FILA
        );

        when(repository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(vencedora));
        when(documentoRepository.findById(documentoId))
                .thenReturn(Optional.of(documentoComArquivo(1024L)));
        doThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"))
                .when(persistenceService).criar(any(ExportacaoEntity.class));

        assertThatThrownBy(() -> service.solicitar(request(documentoId), idempotencyKey))
                .isInstanceOf(ExportacaoConflitoException.class);

        verify(persistenceService).criar(any(ExportacaoEntity.class));
    }

    @Test
    void devePropagarFalhaDeConstraintNaoRelacionada() {
        UUID documentoId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        DataIntegrityViolationException exception = new DataIntegrityViolationException("some other constraint");

        when(repository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(documentoRepository.findById(documentoId))
                .thenReturn(Optional.of(documentoComArquivo(1024L)));
        doThrow(exception)
                .when(persistenceService).criar(any(ExportacaoEntity.class));

        assertThatThrownBy(() -> service.solicitar(request(documentoId), idempotencyKey))
                .isEqualTo(exception);

        verify(persistenceService).criar(any(ExportacaoEntity.class));
    }

    @Test
    void deveInformarExportacaoNaoEncontradaNaConsultaDeStatus() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(id))
                .isInstanceOf(ExportacaoNaoEncontradaException.class);
    }

    @Test
    void deveImpedirConsultaDeExportacaoDeOutroUsuario() {
        UUID id = UUID.randomUUID();
        ExportacaoEntity exportacao = exportacaoExistente(
                UUID.randomUUID(),
                List.of(UUID.randomUUID()),
                EstadoExportacao.NA_FILA
        );
        exportacao.setSolicitadoPor(7);
        when(repository.findById(id)).thenReturn(Optional.of(exportacao));
        when(accessPolicy.usuarioAtualId()).thenReturn(Optional.of(42));

        assertThatThrownBy(() -> service.buscar(id))
                .isInstanceOf(AccessDeniedException.class);
    }

    private SolicitarExportacaoRequest request(UUID... documentoIds) {
        return new SolicitarExportacaoRequest(List.of(documentoIds));
    }

    private DocumentoEntity documentoComArquivo(long tamanhoBytes) {
        DocumentoEntity entity = new DocumentoEntity();
        entity.setStorageKey("documentos/" + UUID.randomUUID() + ".pdf");
        entity.setTamanhoBytes(tamanhoBytes);
        return entity;
    }

    private ExportacaoEntity exportacaoExistente(
            UUID idempotencyKey,
            List<UUID> documentoIds,
            EstadoExportacao estado
    ) {
        Instant agora = Instant.parse("2026-09-03T10:00:00Z");
        ExportacaoEntity entity = new ExportacaoEntity();
        entity.setId(UUID.randomUUID());
        entity.setIdempotencyKey(idempotencyKey);
        entity.setDocumentoIds(documentoIds);
        entity.setEstado(estado);
        entity.setSolicitadoPor(42);
        entity.setTamanhoOrigemBytes(2048L);
        entity.setCriadoEm(agora);
        entity.setAtualizadoEm(agora);
        return entity;
    }
}
