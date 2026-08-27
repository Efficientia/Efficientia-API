package com.example.efficientia.documento.api;

import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.TipoDocumento;
import com.example.efficientia.documento.persistence.DocumentoEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentoMapperTest {

    private final DocumentoMapper mapper = new DocumentoMapper();

    @Test
    void deveMapearArquivoSemExporCamposInternos() {
        UUID id = UUID.randomUUID();
        DocumentoEntity entity = mock(DocumentoEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getViagemId()).thenReturn(1);
        when(entity.getTipoDocumento()).thenReturn(TipoDocumento.RELATORIO_VIAGEM);
        when(entity.getOrigem()).thenReturn(OrigemDocumento.UPLOAD);
        when(entity.getNomeOriginal()).thenReturn("relatorio.pdf");
        when(entity.getMimeType()).thenReturn("application/pdf");
        when(entity.getTamanhoBytes()).thenReturn(1024L);
        when(entity.getSha256()).thenReturn("a".repeat(64));
        when(entity.getCriadoEm()).thenReturn(Instant.parse("2026-08-25T12:00:00Z"));
        when(entity.getAtualizadoEm()).thenReturn(Instant.parse("2026-08-25T12:00:00Z"));
        when(entity.getVersao()).thenReturn(0L);
        when(entity.temArquivo()).thenReturn(true);

        DocumentoResponse response = mapper.paraResponse(entity);
        var camposPublicos = Arrays.stream(DocumentoResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.conteudoUrl()).isEqualTo("/api/v1/documentos/" + id + "/conteudo");
        assertThat(camposPublicos).doesNotContain("storageKey", "idempotencyKey", "caminhoFisico");
    }

    @Test
    void deveOmitirUrlDeConteudoParaAssinaturaTextual() {
        DocumentoEntity entity = mock(DocumentoEntity.class);
        when(entity.getId()).thenReturn(UUID.randomUUID());
        when(entity.getTipoDocumento()).thenReturn(TipoDocumento.ASSINATURA);
        when(entity.getOrigem()).thenReturn(OrigemDocumento.TEXTO);
        when(entity.getModalidadeAssinatura()).thenReturn(ModalidadeAssinatura.TEXTO);
        when(entity.getTextoAssinatura()).thenReturn("Assinatura acessível");
        when(entity.temArquivo()).thenReturn(false);

        DocumentoResponse response = mapper.paraResponse(entity);

        assertThat(response.textoAssinatura()).isEqualTo("Assinatura acessível");
        assertThat(response.conteudoUrl()).isNull();
    }
}
