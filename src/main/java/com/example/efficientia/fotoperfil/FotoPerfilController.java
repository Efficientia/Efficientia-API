package com.example.efficientia.fotoperfil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Tag(name = "Foto de Perfil", description = "Foto do motorista autenticado")
@RestController
@RequestMapping("/api/v1/usuarios/me/foto")
public class FotoPerfilController {
    private final FotoPerfilService service;

    public FotoPerfilController(FotoPerfilService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra ou substitui a foto de perfil do motorista")
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> salvar(@RequestPart("arquivo") MultipartFile arquivo,
                                      Authentication authentication) throws IOException {
        Integer usuarioId = usuarioId(authentication);
        if (arquivo.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "A foto é obrigatória.");
        if (arquivo.getSize() > FotoPerfilService.MAX_BYTES) {
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "A foto deve ter até 2 MB.");
        }
        service.salvar(usuarioId, arquivo.getBytes(), arquivo.getContentType());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtém a foto de perfil do motorista autenticado")
    @GetMapping(value = "/conteudo", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<byte[]> conteudo(Authentication authentication) {
        FotoPerfilService.FotoConteudo foto = service.buscar(usuarioId(authentication));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.mime()))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(foto.bytes());
    }

    @Operation(summary = "Remove a foto de perfil do motorista autenticado")
    @DeleteMapping
    public ResponseEntity<Void> remover(Authentication authentication) {
        service.remover(usuarioId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Integer usuarioId(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Usuário não autenticado.");
        }
        Object claim = jwt.getToken().getClaim("usuario_id");
        try {
            if (claim instanceof Number number) return number.intValue();
            if (claim instanceof String value) return Integer.parseInt(value);
        } catch (NumberFormatException ignored) { }
        throw new ResponseStatusException(UNAUTHORIZED, "Token sem identificador do usuário.");
    }
}
