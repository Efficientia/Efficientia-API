package com.example.efficientia.exportacao.service;

import com.example.efficientia.documento.persistence.DocumentoEntity;
import com.example.efficientia.documento.persistence.DocumentoRepository;
import com.example.efficientia.exportacao.api.ExportacaoResponse;
import com.example.efficientia.exportacao.api.SolicitarExportacaoRequest;
import com.example.efficientia.exportacao.domain.EstadoExportacao;
import com.example.efficientia.exportacao.exception.ExportacaoConflitoException;
import com.example.efficientia.exportacao.exception.ExportacaoInvalidaException;
import com.example.efficientia.exportacao.exception.ExportacaoNaoEncontradaException;
import com.example.efficientia.exportacao.persistence.ExportacaoEntity;
import com.example.efficientia.exportacao.persistence.ExportacaoRepository;
import com.example.efficientia.security.DocumentoAccessPolicy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ExportacaoService {

    private static final int MAXIMO_DOCUMENTOS = 500;
    private static final long MAXIMO_TAMANHO_ORIGEM_BYTES = 500L * 1024L * 1024L;

    private final ExportacaoRepository repository;
    private final ExportacaoPersistenceService persistenceService;
    private final DocumentoRepository documentoRepository;
    private final DocumentoAccessPolicy accessPolicy;

    public ExportacaoService(
            ExportacaoRepository repository,
            ExportacaoPersistenceService persistenceService,
            DocumentoRepository documentoRepository,
            DocumentoAccessPolicy accessPolicy
    ) {
        this.repository = repository;
        this.persistenceService = persistenceService;
        this.documentoRepository = documentoRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional
    public ExportacaoResponse solicitar(SolicitarExportacaoRequest request, UUID idempotencyKey) {
        List<UUID> documentoIds = validarComando(request, idempotencyKey);

        Optional<ExportacaoEntity> existente = repository.findByIdempotencyKey(idempotencyKey);
        if (existente.isPresent()) {
            return tratarRepeticao(existente.get(), documentoIds);
        }

        long tamanhoOrigemBytes = validarDocumentos(documentoIds);
        ExportacaoEntity exportacao = new ExportacaoEntity();
        exportacao.setIdempotencyKey(idempotencyKey);
        exportacao.setDocumentoIds(documentoIds);
        exportacao.setEstado(EstadoExportacao.NA_FILA);
        exportacao.setSolicitadoPor(accessPolicy.usuarioAtualId().orElse(null));
        exportacao.setTamanhoOrigemBytes(tamanhoOrigemBytes);

        try {
            return paraResponse(persistenceService.criar(exportacao));
        } catch (DataIntegrityViolationException exception) {
            ExportacaoEntity vencedora = repository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);
            return tratarRepeticao(vencedora, documentoIds);
        }
    }

    @Transactional(readOnly = true)
    public ExportacaoResponse buscar(UUID id) {
        if (id == null) {
            throw new ExportacaoInvalidaException("O identificador da exportação é obrigatório.");
        }

        ExportacaoEntity exportacao = repository.findById(id)
                .orElseThrow(() -> new ExportacaoNaoEncontradaException(id));
        verificarProprietario(exportacao);
        return paraResponse(exportacao);
    }

    private List<UUID> validarComando(SolicitarExportacaoRequest request, UUID idempotencyKey) {
        if (idempotencyKey == null) {
            throw new ExportacaoInvalidaException("A chave de idempotência é obrigatória.");
        }
        if (request == null || request.documentoIds() == null) {
            throw new ExportacaoInvalidaException("A lista de documentos é obrigatória.");
        }

        List<UUID> documentoIds = request.documentoIds();
        if (documentoIds.isEmpty() || documentoIds.size() > MAXIMO_DOCUMENTOS) {
            throw new ExportacaoInvalidaException("A exportação deve conter entre 1 e 500 documentos.");
        }

        Set<UUID> unicos = new HashSet<>(documentoIds.size());
        for (UUID documentoId : documentoIds) {
            if (documentoId == null) {
                throw new ExportacaoInvalidaException("Os identificadores dos documentos são obrigatórios.");
            }
            if (!unicos.add(documentoId)) {
                throw new ExportacaoInvalidaException(
                        "O documento " + documentoId + " foi informado mais de uma vez."
                );
            }
        }
        return List.copyOf(documentoIds);
    }

    private ExportacaoResponse tratarRepeticao(
            ExportacaoEntity existente,
            List<UUID> documentoIds
    ) {
        verificarProprietario(existente);
        if (!documentoIds.equals(existente.getDocumentoIds())) {
            throw new ExportacaoConflitoException(
                    "A chave de idempotência já foi usada para outra seleção de documentos."
            );
        }
        return paraResponse(existente);
    }

    private long validarDocumentos(List<UUID> documentoIds) {
        long total = 0L;
        for (UUID documentoId : documentoIds) {
            DocumentoEntity documento = documentoRepository.findById(documentoId)
                    .orElseThrow(() -> new ExportacaoInvalidaException(
                            "Documento não encontrado: " + documentoId
                    ));
            accessPolicy.verificarDocumento(documento);

            Long tamanhoBytes = documento.getTamanhoBytes();
            if (!documento.temArquivo() || tamanhoBytes == null || tamanhoBytes <= 0L) {
                throw new ExportacaoInvalidaException(
                        "O documento " + documentoId + " não possui conteúdo binário exportável."
                );
            }

            try {
                total = Math.addExact(total, tamanhoBytes);
            } catch (ArithmeticException exception) {
                throw new ExportacaoInvalidaException(
                        "O tamanho total dos documentos excede o limite permitido."
                );
            }
            if (total > MAXIMO_TAMANHO_ORIGEM_BYTES) {
                throw new ExportacaoInvalidaException(
                        "O tamanho total dos documentos excede 500 MiB."
                );
            }
        }
        return total;
    }

    private void verificarProprietario(ExportacaoEntity exportacao) {
        Optional<Integer> usuarioAtual = accessPolicy.usuarioAtualId();
        if (usuarioAtual.isPresent()
                && !Objects.equals(usuarioAtual.get(), exportacao.getSolicitadoPor())) {
            throw new AccessDeniedException("Exportação fora do escopo do usuário.");
        }
    }

    private ExportacaoResponse paraResponse(ExportacaoEntity exportacao) {
        List<UUID> documentoIds = exportacao.getDocumentoIds();
        return new ExportacaoResponse(
                exportacao.getId(),
                exportacao.getEstado(),
                documentoIds == null ? 0 : documentoIds.size(),
                exportacao.getTamanhoOrigemBytes(),
                exportacao.getTamanhoZipBytes(),
                exportacao.getCriadoEm(),
                exportacao.getAtualizadoEm(),
                exportacao.getConcluidoEm(),
                exportacao.getExpiraEm(),
                null
        );
    }
}
