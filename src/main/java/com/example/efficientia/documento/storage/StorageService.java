package com.example.efficientia.documento.storage;

import java.io.InputStream;
import java.util.UUID;

public interface StorageService {

    /**
     * Consome e fecha o stream recebido. A implementação calcula tamanho e
     * SHA-256 durante a mesma cópia usada para persistir o conteúdo.
     */
    ArquivoArmazenado salvar(
            UUID documentoId,
            String nomeOriginal,
            String mimeType,
            InputStream conteudo
    );

    StoredDocument abrir(String storageKey);

    /**
     * Remove o conteúdo de forma idempotente. Uma chave válida e inexistente
     * não produz erro.
     */
    void remover(String storageKey);
}
