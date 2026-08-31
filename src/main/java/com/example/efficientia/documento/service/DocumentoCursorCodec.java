package com.example.efficientia.documento.service;

import com.example.efficientia.documento.domain.DocumentoCursor;
import com.example.efficientia.documento.exception.DocumentoInvalidoException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Component
public class DocumentoCursorCodec {

    private static final String INICIO = "INICIO";
    private static final int MAX_CURSOR_LENGTH = 512;

    public String codificar(DocumentoCursor cursor) {
        String payload = cursor.criadoEm() + "|" + cursor.id();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public DocumentoCursor decodificar(String cursor) {
        if (INICIO.equals(cursor)) {
            return null;
        }
        if (cursor == null || cursor.isBlank() || cursor.length() > MAX_CURSOR_LENGTH) {
            throw invalido();
        }

        try {
            String payload = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );
            int separador = payload.lastIndexOf('|');
            if (separador <= 0 || separador == payload.length() - 1) {
                throw invalido();
            }
            return new DocumentoCursor(
                    Instant.parse(payload.substring(0, separador)),
                    UUID.fromString(payload.substring(separador + 1))
            );
        } catch (DocumentoInvalidoException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalido();
        }
    }

    private DocumentoInvalidoException invalido() {
        return new DocumentoInvalidoException("Cursor inválido.");
    }
}
