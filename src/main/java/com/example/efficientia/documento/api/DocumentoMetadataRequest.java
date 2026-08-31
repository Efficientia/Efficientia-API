package com.example.efficientia.documento.api;

import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.PapelAssinante;
import com.example.efficientia.documento.domain.TipoDocumento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DocumentoMetadataRequest(
        @NotNull @Positive Integer viagemId,
        @NotNull TipoDocumento tipoDocumento,
        @NotNull OrigemDocumento origem,
        @Positive Integer assinanteId,
        PapelAssinante papelAssinante,
        ModalidadeAssinatura modalidadeAssinatura,
        @Size(max = 300) String descricao
) {
}
