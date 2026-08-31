package com.example.efficientia.documento.api;

import com.example.efficientia.documento.service.DocumentoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
import java.time.Instant;
import java.util.UUID;

import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.TipoDocumento;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/v1/documentos")
public class DocumentoController {

    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    public DocumentoResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }
}
