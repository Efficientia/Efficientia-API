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

    @Test
    void deveAceitarDocumentoSemAssinaturaSemCamposDeAssinante() {
        DocumentoMetadataRequest documento = new DocumentoMetadataRequest(
                1, TipoDocumento.RELATORIO_VIAGEM, OrigemDocumento.UPLOAD, null, null, null, "Relatório"
        );

        assertThatCode(() -> validator.validarArquivo(documento, "application/pdf"))
                .doesNotThrowAnyException();
    }

    @Test
    void deveRejeitarCamposDeAssinanteEmDocumentoQueNaoSejaAssinatura() {
        assertArquivoInvalido(new DocumentoMetadataRequest(
                1, TipoDocumento.RELATORIO_VIAGEM, OrigemDocumento.UPLOAD, 2,
                null, null, "Documento"
        ), "application/pdf");
        assertArquivoInvalido(new DocumentoMetadataRequest(
                1, TipoDocumento.RELATORIO_VIAGEM, OrigemDocumento.UPLOAD, null,
                PapelAssinante.MOTORISTA, null, "Documento"
        ), "application/pdf");
        assertArquivoInvalido(new DocumentoMetadataRequest(
                1, TipoDocumento.RELATORIO_VIAGEM, OrigemDocumento.UPLOAD, null,
                null, ModalidadeAssinatura.FOTO, "Documento"
        ), "application/pdf");
    }

    @Test
    void deveRejeitarAssinaturaArquivoSemCadaCampoObrigatorio() {
        assertArquivoInvalido(new DocumentoMetadataRequest(
                1, TipoDocumento.ASSINATURA, OrigemDocumento.CAMERA, null,
                PapelAssinante.MOTORISTA, ModalidadeAssinatura.FOTO, null
        ), "image/png");
        assertArquivoInvalido(new DocumentoMetadataRequest(
                1, TipoDocumento.ASSINATURA, OrigemDocumento.CAMERA, 0,
                PapelAssinante.MOTORISTA, ModalidadeAssinatura.FOTO, null
        ), "image/png");
        assertArquivoInvalido(new DocumentoMetadataRequest(
                1, TipoDocumento.ASSINATURA, OrigemDocumento.CAMERA, 2,
                null, ModalidadeAssinatura.FOTO, null
        ), "image/png");
        assertArquivoInvalido(new DocumentoMetadataRequest(
                1, TipoDocumento.ASSINATURA, OrigemDocumento.CAMERA, 2,
                PapelAssinante.MOTORISTA, null, null
        ), "image/png");
    }

    @Test
    void deveRejeitarDesenhoComOrigemIncompativelETextoComoArquivo() {
        assertArquivoInvalido(requestArquivo(OrigemDocumento.CAMERA, ModalidadeAssinatura.DESENHO), "image/png");
        assertArquivoInvalido(new DocumentoMetadataRequest(
                1, TipoDocumento.ASSINATURA, OrigemDocumento.TEXTO, 2,
                PapelAssinante.MOTORISTA, ModalidadeAssinatura.TEXTO, "Assinatura textual"
        ), "image/png");
    }

    @Test
    void deveRejeitarAssinaturaTextualNulaOuSemIdentificadoresValidos() {
        assertThatThrownBy(() -> validator.validarTexto(null))
                .isInstanceOf(DocumentoInvalidoException.class);
        assertTextoInvalido(requestTexto(null, TipoDocumento.ASSINATURA, OrigemDocumento.TEXTO,
                2, PapelAssinante.MOTORISTA, ModalidadeAssinatura.TEXTO, "Ana"));
        assertTextoInvalido(requestTexto(0, TipoDocumento.ASSINATURA, OrigemDocumento.TEXTO,
                2, PapelAssinante.MOTORISTA, ModalidadeAssinatura.TEXTO, "Ana"));
        assertTextoInvalido(requestTexto(1, TipoDocumento.ASSINATURA, OrigemDocumento.TEXTO,
                null, PapelAssinante.MOTORISTA, ModalidadeAssinatura.TEXTO, "Ana"));
        assertTextoInvalido(requestTexto(1, TipoDocumento.ASSINATURA, OrigemDocumento.TEXTO,
                0, PapelAssinante.MOTORISTA, ModalidadeAssinatura.TEXTO, "Ana"));
        assertTextoInvalido(requestTexto(1, TipoDocumento.ASSINATURA, OrigemDocumento.TEXTO,
                2, null, ModalidadeAssinatura.TEXTO, "Ana"));
    }

    @Test
    void deveRejeitarAssinaturaTextualComTipoOrigemOuModalidadeIncompativel() {
        assertTextoInvalido(requestTexto(1, TipoDocumento.RELATORIO_VIAGEM, OrigemDocumento.TEXTO,
                2, PapelAssinante.MOTORISTA, ModalidadeAssinatura.TEXTO, "Ana"));
        assertTextoInvalido(requestTexto(1, TipoDocumento.ASSINATURA, OrigemDocumento.CAMERA,
                2, PapelAssinante.MOTORISTA, ModalidadeAssinatura.TEXTO, "Ana"));
        assertTextoInvalido(requestTexto(1, TipoDocumento.ASSINATURA, OrigemDocumento.TEXTO,
                2, PapelAssinante.MOTORISTA, ModalidadeAssinatura.FOTO, "Ana"));
        assertTextoInvalido(requestTexto(1, TipoDocumento.ASSINATURA, OrigemDocumento.TEXTO,
                2, PapelAssinante.MOTORISTA, ModalidadeAssinatura.TEXTO, null));
    }

    @Test
    void deveRejeitarTextoVazioECaracteresHtmlComAberturaOuFechamento() {
        assertTextoInvalido(requestTexto("   "));
        assertThatThrownBy(() -> validator.validarTexto(requestTexto("Assinatura>")))
                .isInstanceOf(DocumentoInvalidoException.class);
    }

    @Test
    void deveContarPontosDeCodigoParaLimiteDeCaracteres() {
        String limite = "😀".repeat(150);
        assertThat(validator.validarTexto(requestTexto(limite))).isEqualTo(limite);
        assertTextoInvalido(requestTexto("😀".repeat(151)));
    }

    private void assertArquivoInvalido(DocumentoMetadataRequest request, String mimeType) {
        assertThatThrownBy(() -> validator.validarArquivo(request, mimeType))
                .isInstanceOf(DocumentoInvalidoException.class);
    }

    private void assertTextoInvalido(AssinaturaTextoRequest request) {
        assertThatThrownBy(() -> validator.validarTexto(request))
                .isInstanceOf(DocumentoInvalidoException.class);
    }

    private AssinaturaTextoRequest requestTexto(String texto) {
        return requestTexto(1, TipoDocumento.ASSINATURA, OrigemDocumento.TEXTO,
                2, PapelAssinante.MOTORISTA, ModalidadeAssinatura.TEXTO, texto);
    }

    private AssinaturaTextoRequest requestTexto(
            Integer viagemId,
            TipoDocumento tipoDocumento,
            OrigemDocumento origem,
            Integer assinanteId,
            PapelAssinante papelAssinante,
            ModalidadeAssinatura modalidade,
            String texto
    ) {
        return new AssinaturaTextoRequest(
                viagemId, tipoDocumento, origem, assinanteId, papelAssinante, modalidade, texto, "Assinatura acessível"
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
