package com.example.efficientia.documento.api;

import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.PapelAssinante;
import com.example.efficientia.documento.domain.TipoDocumento;

import java.time.Instant;
import java.util.UUID;

public record DocumentoResponse(
        UUID id,
        Integer viagemId,
        TipoDocumento tipoDocumento,
        OrigemDocumento origem,
        Integer assinanteId,
        PapelAssinante papelAssinante,
        ModalidadeAssinatura modalidadeAssinatura,
        String textoAssinatura,
        String descricao,
        String nomeOriginal,
        String mimeType,
        Long tamanhoBytes,
        String sha256,
        Integer criadoPor,
        Instant criadoEm,
        Instant atualizadoEm,
        Long versao,
        String conteudoUrl
) {
}
