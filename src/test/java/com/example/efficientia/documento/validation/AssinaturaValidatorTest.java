package com.example.efficientia.documento.validation;

import com.example.efficientia.documento.api.AssinaturaTextoRequest;
import com.example.efficientia.documento.api.DocumentoMetadataRequest;
import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.PapelAssinante;
import com.example.efficientia.documento.domain.TipoDocumento;
import com.example.efficientia.documento.exception.DocumentoInvalidoException;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssinaturaValidatorTest {

    private final AssinaturaValidator validator = new AssinaturaValidator();

    @Test
    void deveNormalizarTextoEmNfcPreservandoCapitalizacaoEAcentos() {
        String texto = validator.validarTexto(requestTexto("  Joa\u0303o da Silva  "));

        assertThat(texto).isEqualTo("João da Silva");
        assertThat(Normalizer.isNormalized(texto, Normalizer.Form.NFC)).isTrue();
    }

    @Test
    void deveRejeitarHtml() {
        assertThatThrownBy(() -> validator.validarTexto(requestTexto("<b>João</b>")))
                .isInstanceOf(DocumentoInvalidoException.class);
    }

    @Test
    void deveRejeitarCaracteresDeControle() {
        assertThatThrownBy(() -> validator.validarTexto(requestTexto("João\nSilva")))
                .isInstanceOf(DocumentoInvalidoException.class);
    }

    @Test
    void deveRejeitarTextoAcimaDeCentoECinquentaCaracteres() {
        assertThatThrownBy(() -> validator.validarTexto(requestTexto("a".repeat(151))))
                .isInstanceOf(DocumentoInvalidoException.class);
    }

    @Test
    void deveAceitarFotoEmPngComOrigemCamera() {
        assertThatCode(() -> validator.validarArquivo(
                requestArquivo(OrigemDocumento.CAMERA, ModalidadeAssinatura.FOTO),
                "image/png"
        )).doesNotThrowAnyException();
    }

    @Test
    void deveAceitarDesenhoEmPngComOrigemDesenho() {
        assertThatCode(() -> validator.validarArquivo(
                requestArquivo(OrigemDocumento.DESENHO, ModalidadeAssinatura.DESENHO),
                "image/png"
        )).doesNotThrowAnyException();
    }

    @Test
    void deveRejeitarFotoComOrigemOuMimeIncompativel() {
        assertThatThrownBy(() -> validator.validarArquivo(
                requestArquivo(OrigemDocumento.DESENHO, ModalidadeAssinatura.FOTO),
                "image/png"
        )).isInstanceOf(DocumentoInvalidoException.class);

        assertThatThrownBy(() -> validator.validarArquivo(
                requestArquivo(OrigemDocumento.CAMERA, ModalidadeAssinatura.FOTO),
                "application/pdf"
        )).isInstanceOf(DocumentoInvalidoException.class);
    }

    private AssinaturaTextoRequest requestTexto(String texto) {
        return new AssinaturaTextoRequest(
                1,
                TipoDocumento.ASSINATURA,
                OrigemDocumento.TEXTO,
                2,
                PapelAssinante.MOTORISTA,
                ModalidadeAssinatura.TEXTO,
                texto,
                "Assinatura acessível"
        );
    }

    private DocumentoMetadataRequest requestArquivo(
            OrigemDocumento origem,
            ModalidadeAssinatura modalidade
    ) {
        return new DocumentoMetadataRequest(
                1,
                TipoDocumento.ASSINATURA,
                origem,
                2,
                PapelAssinante.MOTORISTA,
                modalidade,
                "Assinatura binária"
        );
    }
}
