package com.example.efficientia.documento.service;

import com.example.efficientia.documento.api.DocumentoMapper;
import com.example.efficientia.documento.api.AssinaturaTextoRequest;
import com.example.efficientia.documento.api.DocumentoMetadataRequest;
import com.example.efficientia.documento.api.DocumentoResponse;
import com.example.efficientia.documento.api.DocumentoListagemRequest;
import com.example.efficientia.documento.api.PaginaDocumentosResponse;
import com.example.efficientia.documento.domain.DocumentoCursor;
import com.example.efficientia.documento.exception.DocumentoInvalidoException;
import com.example.efficientia.documento.exception.DocumentoNaoEncontradoException;
import com.example.efficientia.documento.exception.DocumentoSemConteudoException;
import com.example.efficientia.documento.persistence.DocumentoEntity;
import com.example.efficientia.documento.persistence.DocumentoFiltro;
import com.example.efficientia.documento.persistence.DocumentoRepository;
import com.example.efficientia.documento.storage.ArquivoArmazenado;
import com.example.efficientia.documento.storage.StorageService;
import com.example.efficientia.documento.storage.StoredDocument;
import com.example.efficientia.documento.validation.ArquivoValidado;
import com.example.efficientia.documento.validation.ArquivoValidator;
import com.example.efficientia.documento.validation.AssinaturaValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.List;
import java.util.Set;

@Service
public class DocumentoService {

    private final DocumentoRepository repository;
    private final StorageService storage;
    private final ArquivoValidator arquivoValidator;
    private final AssinaturaValidator assinaturaValidator;
    private final DocumentoCursorCodec cursorCodec;
    private final DocumentoMapper mapper;

    public DocumentoService(
            DocumentoRepository repository,
            StorageService storage,
            ArquivoValidator arquivoValidator,
            AssinaturaValidator assinaturaValidator,
            DocumentoCursorCodec cursorCodec,
            DocumentoMapper mapper
    ) {
        this.repository = repository;
        this.storage = storage;
        this.arquivoValidator = arquivoValidator;
        this.assinaturaValidator = assinaturaValidator;
        this.cursorCodec = cursorCodec;
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

    @Transactional(readOnly = true)
    public PaginaDocumentosResponse listar(DocumentoListagemRequest request) {
        validarListagem(request);
        DocumentoFiltro filtro = new DocumentoFiltro(
                request.viagemId(),
                request.assinanteId(),
                request.tipoDocumento(),
                request.origem(),
                request.modalidadeAssinatura(),
                request.criadoDe(),
                request.criadoAte()
        );

        if (request.cursor() != null) {
            return listarPorCursor(request, filtro);
        }
        return listarPorPagina(request, filtro);
    }

    @Transactional(readOnly = true)
    public DocumentoResponse buscar(UUID id) {
        if (id == null) {
            throw new DocumentoInvalidoException("O identificador do documento é obrigatório.");
        }
        return repository.findById(id)
                .map(mapper::paraResponse)
                .orElseThrow(() -> new DocumentoNaoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public DocumentoConteudo buscarConteudo(UUID id) {
        if (id == null) {
            throw new DocumentoInvalidoException("O identificador do documento é obrigatório.");
        }

        DocumentoEntity documento = repository.findById(id)
                .orElseThrow(() -> new DocumentoNaoEncontradoException(id));
        if (!documento.temArquivo()) {
            throw new DocumentoSemConteudoException(id);
        }

        StoredDocument armazenado = storage.abrir(documento.getStorageKey());
        return new DocumentoConteudo(
                armazenado.conteudo(),
                armazenado.mimeType(),
                armazenado.tamanhoBytes(),
                documento.getNomeOriginal()
        );
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

    private PaginaDocumentosResponse listarPorPagina(
            DocumentoListagemRequest request,
            DocumentoFiltro filtro
    ) {
        int pagina = request.page() == null ? 0 : request.page();
        Sort sort = parseSort(request.sort());
        var resultado = repository.buscarPagina(
                filtro,
                PageRequest.of(pagina, request.size(), sort)
        );
        List<DocumentoResponse> itens = resultado.getContent().stream()
                .map(mapper::paraResponse)
                .toList();
        return new PaginaDocumentosResponse(
                itens,
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                null,
                resultado.hasNext()
        );
    }

    private PaginaDocumentosResponse listarPorCursor(
            DocumentoListagemRequest request,
            DocumentoFiltro filtro
    ) {
        DocumentoCursor cursor = cursorCodec.decodificar(request.cursor());
        var resultado = repository.buscarCursor(filtro, cursor, request.size());
        List<DocumentoResponse> itens = resultado.getContent().stream()
                .map(mapper::paraResponse)
                .toList();

        String proximoCursor = null;
        if (resultado.hasNext() && !resultado.getContent().isEmpty()) {
            DocumentoEntity ultimo = resultado.getContent().get(resultado.getContent().size() - 1);
            proximoCursor = cursorCodec.codificar(new DocumentoCursor(ultimo.getCriadoEm(), ultimo.getId()));
        }
        return new PaginaDocumentosResponse(
                itens,
                null,
                request.size(),
                null,
                null,
                proximoCursor,
                resultado.hasNext()
        );
    }

    private void validarListagem(DocumentoListagemRequest request) {
        if (request == null) {
            throw new DocumentoInvalidoException("Os parâmetros da listagem são obrigatórios.");
        }
        if (request.page() != null && request.cursor() != null) {
            throw new DocumentoInvalidoException("page e cursor não podem ser combinados.");
        }
        if (request.page() != null && request.page() < 0) {
            throw new DocumentoInvalidoException("page deve ser maior ou igual a zero.");
        }
        if (request.size() < 1 || request.size() > 100) {
            throw new DocumentoInvalidoException("size deve estar entre 1 e 100.");
        }
        if ((request.viagemId() != null && request.viagemId() <= 0)
                || (request.assinanteId() != null && request.assinanteId() <= 0)) {
            throw new DocumentoInvalidoException("Os filtros de identificador devem ser positivos.");
        }
        if (request.criadoDe() != null && request.criadoAte() != null
                && request.criadoDe().isAfter(request.criadoAte())) {
            throw new DocumentoInvalidoException("criadoDe não pode ser posterior a criadoAte.");
        }
    }

    private Sort parseSort(String valor) {
        String sort = valor == null || valor.isBlank() ? "criadoEm,desc" : valor;
        String[] partes = sort.split(",", -1);
        Set<String> permitidos = Set.of("id", "criadoEm", "atualizadoEm", "tipoDocumento", "origem");
        if (partes.length != 2 || !permitidos.contains(partes[0])) {
            throw new DocumentoInvalidoException("Ordenação não permitida.");
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(partes[1]);
        } catch (IllegalArgumentException exception) {
            throw new DocumentoInvalidoException("Direção de ordenação inválida.");
        }
        return Sort.by(direction, partes[0]);
    }
}
