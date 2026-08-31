package com.example.efficientia.documento.api;

import com.example.efficientia.documento.service.DocumentoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

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
}
