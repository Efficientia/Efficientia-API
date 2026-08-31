package com.example.efficientia.documento.service;

import com.example.efficientia.documento.domain.DocumentoCursor;
import com.example.efficientia.documento.exception.DocumentoInvalidoException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentoCursorCodecTest {

    private final DocumentoCursorCodec codec = new DocumentoCursorCodec();

    @Test
    void deveCodificarEDecodificarCursorSemExporFormatoAoCliente() {
        DocumentoCursor original = new DocumentoCursor(
                Instant.parse("2026-08-31T12:00:00Z"),
                UUID.randomUUID()
        );

        String codificado = codec.codificar(original);

        assertThat(codificado).doesNotContain("|", original.criadoEm().toString());
        assertThat(codec.decodificar(codificado)).isEqualTo(original);
    }

    @Test
    void deveInterpretarInicioComoAusenciaDeCursor() {
        assertThat(codec.decodificar("INICIO")).isNull();
    }

    @Test
    void deveRejeitarCursorMalformado() {
        assertThatThrownBy(() -> codec.decodificar("nao-e-um-cursor"))
                .isInstanceOf(DocumentoInvalidoException.class);
    }
}
