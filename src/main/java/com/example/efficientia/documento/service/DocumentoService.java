package com.example.efficientia.documento.service;

import com.example.efficientia.documento.api.DocumentoMapper;
import com.example.efficientia.documento.api.AssinaturaTextoRequest;
import com.example.efficientia.documento.api.DocumentoMetadataRequest;
import com.example.efficientia.documento.api.DocumentoResponse;
import com.example.efficientia.documento.exception.DocumentoInvalidoException;
import com.example.efficientia.documento.persistence.DocumentoEntity;
import com.example.efficientia.documento.persistence.DocumentoRepository;
import com.example.efficientia.documento.storage.ArquivoArmazenado;
import com.example.efficientia.documento.storage.StorageService;
import com.example.efficientia.documento.validation.ArquivoValidado;
import com.example.efficientia.documento.validation.ArquivoValidator;
import com.example.efficientia.documento.validation.AssinaturaValidator;
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
    private final AssinaturaValidator assinaturaValidator;
    private final DocumentoMapper mapper;

    public DocumentoService(
            DocumentoRepository repository,
            StorageService storage,
            ArquivoValidator arquivoValidator,
            AssinaturaValidator assinaturaValidator,
            DocumentoMapper mapper
    ) {
        this.repository = repository;
        this.storage = storage;
        this.arquivoValidator = arquivoValidator;
        this.assinaturaValidator = assinaturaValidator;
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

    @Transactional
    public DocumentoResponse criarAssinaturaTextual(
            AssinaturaTextoRequest request,
            UUID idempotencyKey
    ) {
        validarIdempotencyKey(idempotencyKey);
        return repository.findByIdempotencyKey(idempotencyKey)
                .map(mapper::paraResponse)
                .orElseGet(() -> criarNovaAssinaturaTextual(request, idempotencyKey));
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
            assinaturaValidator.validarArquivo(request, validado.mimeType());
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
        if (request.viagemId() == null || request.viagemId() <= 0
                || request.tipoDocumento() == null || request.origem() == null) {
            throw new DocumentoInvalidoException("Viagem, tipo e origem válidos são obrigatórios.");
        }
        validarIdempotencyKey(idempotencyKey);
    }

    private void validarIdempotencyKey(UUID idempotencyKey) {
        if (idempotencyKey == null) {
            throw new DocumentoInvalidoException("Idempotency-Key é obrigatório.");
        }
    }

    private DocumentoResponse criarNovaAssinaturaTextual(
            AssinaturaTextoRequest request,
            UUID idempotencyKey
    ) {
        String texto = assinaturaValidator.validarTexto(request);
        DocumentoEntity entity = new DocumentoEntity();
        entity.setId(UUID.randomUUID());
        entity.setViagemId(request.viagemId());
        entity.setTipoDocumento(request.tipoDocumento());
        entity.setOrigem(request.origem());
        entity.setAssinanteId(request.assinanteId());
        entity.setPapelAssinante(request.papelAssinante());
        entity.setModalidadeAssinatura(request.modalidadeAssinatura());
        entity.setTextoAssinatura(texto);
        entity.setDescricao(request.descricao());
        entity.setIdempotencyKey(idempotencyKey);
        return mapper.paraResponse(repository.saveAndFlush(entity));
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
