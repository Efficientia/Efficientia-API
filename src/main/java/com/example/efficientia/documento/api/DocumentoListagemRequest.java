package com.example.efficientia.documento.api;

import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.TipoDocumento;

import java.time.Instant;

public record DocumentoListagemRequest(
        Integer page,
        String cursor,
        int size,
        String sort,
        Integer viagemId,
        Integer assinanteId,
        TipoDocumento tipoDocumento,
        OrigemDocumento origem,
        ModalidadeAssinatura modalidadeAssinatura,
        Instant criadoDe,
        Instant criadoAte
) {
}
