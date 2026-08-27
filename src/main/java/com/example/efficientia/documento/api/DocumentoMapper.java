package com.example.efficientia.documento.api;

import com.example.efficientia.documento.persistence.DocumentoEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DocumentoMapper {

    public DocumentoResponse paraResponse(DocumentoEntity entity) {
        Objects.requireNonNull(entity, "entity é obrigatória");

        String conteudoUrl = entity.temArquivo() && entity.getId() != null
                ? "/api/v1/documentos/" + entity.getId() + "/conteudo"
                : null;

        return new DocumentoResponse(
                entity.getId(),
                entity.getViagemId(),
                entity.getTipoDocumento(),
                entity.getOrigem(),
                entity.getAssinanteId(),
                entity.getPapelAssinante(),
                entity.getModalidadeAssinatura(),
                entity.getTextoAssinatura(),
                entity.getDescricao(),
                entity.getNomeOriginal(),
                entity.getMimeType(),
                entity.getTamanhoBytes(),
                entity.getSha256(),
                entity.getCriadoPor(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm(),
                entity.getVersao(),
                conteudoUrl
        );
    }
}
