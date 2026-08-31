package com.example.efficientia.documento.persistence;

import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.TipoDocumento;

import java.time.Instant;
import com.example.efficientia.security.DocumentoAccessScope;

public record DocumentoFiltro(
        Integer viagemId,
        Integer assinanteId,
        TipoDocumento tipoDocumento,
        OrigemDocumento origem,
        ModalidadeAssinatura modalidadeAssinatura,
        Instant criadoDe,
        Instant criadoAte,
        Integer usuarioEscopoId,
        DocumentoAccessScope accessScope
) {
    public DocumentoFiltro(
            Integer viagemId,
            Integer assinanteId,
            TipoDocumento tipoDocumento,
            OrigemDocumento origem,
            ModalidadeAssinatura modalidadeAssinatura,
            Instant criadoDe,
            Instant criadoAte
    ) {
        this(viagemId, assinanteId, tipoDocumento, origem, modalidadeAssinatura,
                criadoDe, criadoAte, null, DocumentoAccessScope.TODOS);
    }
}
