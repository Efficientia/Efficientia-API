package com.example.efficientia.documento.api;

import com.example.efficientia.documento.service.DocumentoService;
import com.example.efficientia.documento.service.DocumentoConteudo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.TipoDocumento;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/v1/documentos")
@Tag(name = "Documentos", description = "Upload, assinaturas, consulta e ciclo de vida de documentos")
@SecurityRequirement(name = "bearerAuth")
public class DocumentoController {

    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia PDF ou PNG", description = "Cria o documento de forma idempotente e compensa o storage se o banco falhar.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Documento criado"),
            @ApiResponse(responseCode = "400", description = "Contrato inválido"),
            @ApiResponse(responseCode = "413", description = "Arquivo acima do limite"),
            @ApiResponse(responseCode = "415", description = "Conteúdo não é PDF ou PNG"),
            @ApiResponse(responseCode = "422", description = "Regra de assinatura violada"),
            @ApiResponse(responseCode = "503", description = "Storage indisponível")
    })
    public ResponseEntity<DocumentoResponse> criarComArquivo(
            @Valid @RequestPart("metadados") DocumentoMetadataRequest metadados,
            @RequestPart("arquivo") MultipartFile arquivo,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey
    ) {
        DocumentoResponse response = service.criarComArquivo(metadados, arquivo, idempotencyKey);
        return ResponseEntity
                .created(URI.create("/api/v1/documentos/" + response.id()))
                .body(response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cria assinatura textual acessível")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Assinatura criada"),
            @ApiResponse(responseCode = "400", description = "Contrato inválido"),
            @ApiResponse(responseCode = "422", description = "Regra de assinatura violada")
    })
    public ResponseEntity<DocumentoResponse> criarAssinaturaTextual(
            @Valid @RequestBody AssinaturaTextoRequest request,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey
    ) {
        DocumentoResponse response = service.criarAssinaturaTextual(request, idempotencyKey);
        return ResponseEntity
                .created(URI.create("/api/v1/documentos/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "Lista documentos", description = "Suporta paginação para o site ou cursor opaco para sincronização.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página ou lote retornado"),
            @ApiResponse(responseCode = "400", description = "Filtro, paginação ou cursor inválido")
    })
    public PaginaDocumentosResponse listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "criadoEm,desc") String sort,
            @RequestParam(required = false) Integer viagemId,
            @RequestParam(required = false) Integer assinanteId,
            @RequestParam(required = false) TipoDocumento tipoDocumento,
            @RequestParam(required = false) OrigemDocumento origem,
            @RequestParam(required = false) ModalidadeAssinatura modalidadeAssinatura,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant criadoDe,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant criadoAte
    ) {
        return service.listar(new DocumentoListagemRequest(
                page,
                cursor,
                size,
                sort,
                viagemId,
                assinanteId,
                tipoDocumento,
                origem,
                modalidadeAssinatura,
                criadoDe,
                criadoAte
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta metadados públicos por UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documento encontrado"),
            @ApiResponse(responseCode = "404", description = "Documento não encontrado")
    })
    public DocumentoResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @GetMapping("/{id}/conteudo")
    @Operation(summary = "Transmite o conteúdo privado", description = "Use inline=true para pré-visualização.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conteúdo transmitido por Resource"),
            @ApiResponse(responseCode = "404", description = "Documento não encontrado"),
            @ApiResponse(responseCode = "409", description = "Assinatura textual não possui binário"),
            @ApiResponse(responseCode = "503", description = "Storage indisponível")
    })
    public ResponseEntity<Resource> buscarConteudo(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean inline
    ) {
        DocumentoConteudo conteudo = service.buscarConteudo(id);
        ContentDisposition disposition = ContentDisposition
                .builder(inline ? "inline" : "attachment")
                .filename(conteudo.nomeOriginal(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(conteudo.mimeType()))
                .contentLength(conteudo.tamanhoBytes())
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(conteudo.resource());
    }

    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualiza a descrição com versão otimista")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documento atualizado"),
            @ApiResponse(responseCode = "400", description = "Contrato inválido"),
            @ApiResponse(responseCode = "404", description = "Documento não encontrado"),
            @ApiResponse(responseCode = "409", description = "Versão concorrente")
    })
    public DocumentoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarDocumentoRequest request
    ) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui metadados e conteúdo privado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Documento excluído"),
            @ApiResponse(responseCode = "404", description = "Documento não encontrado"),
            @ApiResponse(responseCode = "503", description = "Storage indisponível")
    })
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
