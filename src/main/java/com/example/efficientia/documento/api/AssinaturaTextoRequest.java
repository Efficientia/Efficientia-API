package com.example.efficientia.documento.api;

import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.PapelAssinante;
import com.example.efficientia.documento.domain.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AssinaturaTextoRequest(
        @NotNull @Positive Integer viagemId,
        @NotNull TipoDocumento tipoDocumento,
        @NotNull OrigemDocumento origem,
        @NotNull @Positive Integer assinanteId,
        @NotNull PapelAssinante papelAssinante,
        @NotNull ModalidadeAssinatura modalidadeAssinatura,
        @NotBlank @Size(max = 150) String textoAssinatura,
        @Size(max = 300) String descricao
) {
}
