package com.example.efficientia.documento.service;

import com.example.efficientia.documento.api.DocumentoMapper;
import com.example.efficientia.documento.api.DocumentoMetadataRequest;
import com.example.efficientia.documento.api.DocumentoResponse;
import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.TipoDocumento;
import com.example.efficientia.documento.exception.DocumentoInvalidoException;
import com.example.efficientia.documento.persistence.DocumentoEntity;
import com.example.efficientia.documento.persistence.DocumentoRepository;
import com.example.efficientia.documento.storage.ArquivoArmazenado;
import com.example.efficientia.documento.storage.StorageService;
import com.example.efficientia.documento.validation.ArquivoValidado;
import com.example.efficientia.documento.validation.ArquivoValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class DocumentoService {

    private final DocumentoRepository repository;
    private final StorageService storage;
    private final ArquivoValidator arquivoValidator;
    private final DocumentoMapper mapper;

    public DocumentoService(
            DocumentoRepository repository,
            StorageService storage,
            ArquivoValidator arquivoValidator,
            DocumentoMapper mapper
    ) {
        this.repository = repository;
        this.storage = storage;
        this.arquivoValidator = arquivoValidator;
        this.mapper = mapper;
    }

    @Transactional
    public DocumentoResponse criarComArquivo(
            DocumentoMetadataRequest request,
            MultipartFile arquivo,
            UUID idempotencyKey
    ) {
        validarComando(request, idempotencyKey);

        return repository.findByIdempotencyKey(idempotencyKey)
                .map(mapper::paraResponse)
                .orElseGet(() -> criarNovoComArquivo(request, arquivo, idempotencyKey));
    }

    private DocumentoResponse criarNovoComArquivo(
            DocumentoMetadataRequest request,
            MultipartFile arquivo,
            UUID idempotencyKey
    ) {
        ArquivoValidado validado = arquivoValidator.validar(arquivo);
        UUID documentoId = UUID.randomUUID();
        ArquivoArmazenado armazenado;

        try (InputStream conteudo = validado.conteudo()) {
            validarRegraDoArquivo(request, validado.mimeType());
            armazenado = storage.salvar(
                    documentoId,
                    validado.nomeOriginal(),
                    validado.mimeType(),
                    conteudo
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível fechar o conteúdo recebido.", exception);
        }

        DocumentoEntity entity = montarEntidade(documentoId, request, idempotencyKey, armazenado);
        try {
            return mapper.paraResponse(repository.saveAndFlush(entity));
        } catch (RuntimeException exception) {
            compensarStorage(armazenado.storageKey(), exception);
            throw exception;
        }
    }

    private void validarComando(DocumentoMetadataRequest request, UUID idempotencyKey) {
        if (request == null) {
            throw new DocumentoInvalidoException("Os metadados do documento são obrigatórios.");
        }
        if (idempotencyKey == null) {
            throw new DocumentoInvalidoException("Idempotency-Key é obrigatório.");
        }

        boolean assinatura = request.tipoDocumento() == TipoDocumento.ASSINATURA;
        if (assinatura) {
            if (request.assinanteId() == null || request.papelAssinante() == null
                    || request.modalidadeAssinatura() == null
                    || request.modalidadeAssinatura() == ModalidadeAssinatura.TEXTO) {
                throw new DocumentoInvalidoException(
                        "Assinatura com arquivo exige assinante, papel e modalidade FOTO ou DESENHO."
                );
            }
        } else if (request.assinanteId() != null || request.papelAssinante() != null
                || request.modalidadeAssinatura() != null) {
            throw new DocumentoInvalidoException(
                    "Campos de assinante e modalidade são exclusivos de documentos de assinatura."
            );
        }
    }

    private void validarRegraDoArquivo(DocumentoMetadataRequest request, String mimeType) {
        if (request.tipoDocumento() == TipoDocumento.ASSINATURA && !"image/png".equals(mimeType)) {
            throw new DocumentoInvalidoException("Assinaturas por foto ou desenho exigem arquivo PNG.");
        }
    }

    private DocumentoEntity montarEntidade(
            UUID documentoId,
            DocumentoMetadataRequest request,
            UUID idempotencyKey,
            ArquivoArmazenado arquivo
    ) {
        DocumentoEntity entity = new DocumentoEntity();
        entity.setId(documentoId);
        entity.setViagemId(request.viagemId());
        entity.setTipoDocumento(request.tipoDocumento());
        entity.setOrigem(request.origem());
        entity.setAssinanteId(request.assinanteId());
        entity.setPapelAssinante(request.papelAssinante());
        entity.setModalidadeAssinatura(request.modalidadeAssinatura());
        entity.setDescricao(request.descricao());
        entity.setNomeOriginal(arquivo.nomeOriginal());
        entity.setMimeType(arquivo.mimeType());
        entity.setTamanhoBytes(arquivo.tamanhoBytes());
        entity.setSha256(arquivo.sha256());
        entity.setStorageKey(arquivo.storageKey());
        entity.setIdempotencyKey(idempotencyKey);
        return entity;
    }

    private void compensarStorage(String storageKey, RuntimeException falhaOriginal) {
        try {
            storage.remover(storageKey);
        } catch (RuntimeException falhaCompensacao) {
            falhaOriginal.addSuppressed(falhaCompensacao);
        }
    }
}
