package com.example.efficientia.exportacao.api;

import com.example.efficientia.exportacao.service.ExportacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import java.nio.charset.StandardCharsets;
import com.example.efficientia.exportacao.service.ExportacaoConteudo;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exportacoes")
@Tag(name = "Exportações", description = "Solicitação e acompanhamento de exportações assíncronas")
@SecurityRequirement(name = "bearerAuth")
public class ExportacaoController {

    private static final String RETRY_AFTER_SECONDS = "2";

    private final ExportacaoService service;

    public ExportacaoController(ExportacaoService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Solicita uma exportação",
            description = "Valida os documentos e registra a exportação na fila para processamento assíncrono."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Exportação aceita e registrada na fila"),
            @ApiResponse(responseCode = "400", description = "Contrato ou Idempotency-Key inválido"),
            @ApiResponse(responseCode = "409", description = "Idempotency-Key reutilizado com documentos diferentes"),
            @ApiResponse(responseCode = "422", description = "Documentos não podem compor a exportação")
    })
    public ResponseEntity<ExportacaoResponse> solicitar(
            @Valid @RequestBody SolicitarExportacaoRequest request,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey
    ) {
        ExportacaoResponse response = service.solicitar(request, idempotencyKey);
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/exportacoes/" + response.id()))
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(response);
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulta o estado de uma exportação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exportação encontrada"),
            @ApiResponse(responseCode = "400", description = "UUID inválido"),
            @ApiResponse(responseCode = "404", description = "Exportação não encontrada")
    })
    public ResponseEntity<ExportacaoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    @GetMapping(path = "/{id}/conteudo")
    @Operation(summary = "Transmite o arquivo ZIP da exportação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conteúdo transmitido por Resource"),
            @ApiResponse(responseCode = "404", description = "Exportação não encontrada"),
            @ApiResponse(responseCode = "409", description = "Exportação ainda não concluída"),
            @ApiResponse(responseCode = "410", description = "Arquivo de exportação expirado")
    })
    public ResponseEntity<Resource> buscarConteudo(@PathVariable UUID id) {
        ExportacaoConteudo conteudo = service.baixarConteudo(id);
        ContentDisposition disposition = ContentDisposition
                .builder("attachment")
                .filename(conteudo.nomeArquivo(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(conteudo.mimeType()))
                .contentLength(conteudo.tamanhoBytes())
                .body(conteudo.resource());
    }
}
