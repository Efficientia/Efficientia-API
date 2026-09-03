package com.example.efficientia.exportacao.api;

import com.example.efficientia.exportacao.domain.EstadoExportacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Estado público de uma exportação assíncrona")
public record ExportacaoResponse(
        UUID id,
        EstadoExportacao estado,
        int quantidadeDocumentos,
        long tamanhoOrigemBytes,
        Long tamanhoZipBytes,
        Instant criadoEm,
        Instant atualizadoEm,
        Instant concluidoEm,
        Instant expiraEm,
        String conteudoUrl
) {
}
