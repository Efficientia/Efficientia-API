package com.example.efficientia.assinaturamotorista.api;

import com.example.efficientia.assinaturamotorista.domain.ModalidadeAssinaturaMotorista;
import com.example.efficientia.assinaturamotorista.persistence.AssinaturaMotoristaEntity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AssinaturaMotoristaContracts {

    private AssinaturaMotoristaContracts() {
    }

    public record AssinaturaMetadadosRequest(
            @NotNull(message = "A modalidade da assinatura é obrigatória (DESENHO ou NOME_DIGITADO).")
            ModalidadeAssinaturaMotorista modalidade,

            @Size(max = 150, message = "O texto de origem não pode exceder 150 caracteres.")
            String textoOrigem
    ) {
    }

    public record AssinaturaMotoristaResponse(
            UUID id,
            Integer usuarioId,
            String modalidade,
            String mimeType,
            Long tamanhoBytes,
            String sha256,
            String conteudoUrl,
            Instant atualizadoEm,
            Long versao
    ) {
        public static AssinaturaMotoristaResponse fromEntity(AssinaturaMotoristaEntity entity, boolean isMe) {
            String url = isMe
                    ? "/api/v1/usuarios/me/assinatura/conteudo"
                    : "/api/v1/usuarios/" + entity.getMotoristaId() + "/assinatura/conteudo";
            return new AssinaturaMotoristaResponse(
                    entity.getId(),
                    entity.getMotoristaId(),
                    entity.getModalidade(),
                    entity.getMimeType(),
                    entity.getTamanhoBytes(),
                    entity.getSha256(),
                    url,
                    entity.getAtualizadoEm(),
                    entity.getVersao() != null ? entity.getVersao() : 0L
            );
        }
    }
}
