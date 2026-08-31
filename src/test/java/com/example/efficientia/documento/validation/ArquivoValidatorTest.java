package com.example.efficientia.documento.validation;

import com.example.efficientia.documento.exception.ArquivoInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.stream.Stream;

import static com.example.efficientia.documento.exception.ArquivoInvalidoException.Reason.UNSUPPORTED_MEDIA_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArquivoValidatorTest {

    private final ArquivoValidator validator = new ArquivoValidator();

    @ParameterizedTest
    @MethodSource("arquivosValidos")
    void deveDetectarMimePelosBytes(String nome, String mimeType, byte[] conteudo) throws IOException {
        var arquivo = new MockMultipartFile("arquivo", nome, mimeType, conteudo);

        ArquivoValidado validado = validator.validar(arquivo);

        assertThat(validado.nomeOriginal()).isEqualTo(nome);
        assertThat(validado.mimeType()).isEqualTo(mimeType);
        try (var input = validado.conteudo()) {
            assertThat(input.readAllBytes()).isEqualTo(conteudo);
        }
    }

    @Test
    void deveRejeitarHeaderQueNaoCorrespondeAosBytes() {
        var arquivo = new MockMultipartFile(
                "arquivo",
                "relatorio.pdf",
                "image/png",
                "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII)
        );

        assertThatThrownBy(() -> validator.validar(arquivo))
                .isInstanceOfSatisfying(ArquivoInvalidoException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void deveRejeitarExtensaoQueNaoCorrespondeAoConteudo() {
        var arquivo = new MockMultipartFile(
                "arquivo",
                "relatorio.png",
                "application/pdf",
                "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII)
        );

        assertThatThrownBy(() -> validator.validar(arquivo))
                .isInstanceOf(ArquivoInvalidoException.class);
    }

    @Test
    void deveManterSomenteOBasenameDoNomeOriginal() {
        var arquivo = new MockMultipartFile(
                "arquivo",
                "../../documentos/relatorio.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII)
        );

        ArquivoValidado validado = validator.validar(arquivo);

        assertThat(validado.nomeOriginal()).isEqualTo("relatorio.pdf");
    }

    @Test
    void deveRejeitarPngAcimaDeDezMibAntesDoStreaming() throws Exception {
        org.springframework.web.multipart.MultipartFile arquivo =
                mock(org.springframework.web.multipart.MultipartFile.class);
        when(arquivo.getOriginalFilename()).thenReturn("assinatura.png");
        when(arquivo.getContentType()).thenReturn("image/png");
        when(arquivo.getSize()).thenReturn(10L * 1024 * 1024 + 1);
        when(arquivo.getInputStream()).thenReturn(new ByteArrayInputStream(
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        ));

        assertThatThrownBy(() -> validator.validar(arquivo))
                .isInstanceOfSatisfying(ArquivoInvalidoException.class,
                        exception -> assertThat(exception.getReason())
                                .isEqualTo(ArquivoInvalidoException.Reason.SIZE_LIMIT_EXCEEDED));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> arquivosValidos() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        "relatorio.pdf",
                        "application/pdf",
                        "%PDF-1.7\nconteudo".getBytes(java.nio.charset.StandardCharsets.US_ASCII)
                ),
                org.junit.jupiter.params.provider.Arguments.of(
                        "assinatura.png",
                        "image/png",
                        new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1}
                )
        );
    }
}
