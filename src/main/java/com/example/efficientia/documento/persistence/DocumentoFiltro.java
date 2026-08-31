package com.example.efficientia.documento.persistence;

import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.TipoDocumento;

import java.time.Instant;

public record DocumentoFiltro(
        Integer viagemId,
        Integer assinanteId,
        TipoDocumento tipoDocumento,
        OrigemDocumento origem,
        ModalidadeAssinatura modalidadeAssinatura,
        Instant criadoDe,
        Instant criadoAte
) {
}
