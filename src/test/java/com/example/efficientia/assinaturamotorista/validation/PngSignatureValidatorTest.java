package com.example.efficientia.assinaturamotorista.validation;

import com.example.efficientia.assinaturamotorista.api.AssinaturaFormatoInvalidoException;
import com.example.efficientia.assinaturamotorista.api.AssinaturaTamanhoExcedidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PngSignatureValidator Tests")
class PngSignatureValidatorTest {

    private static final byte[] VALID_PNG_HEADER = new byte[]{
            (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
            (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
    };

    @Test
    @DisplayName("Deve validar com sucesso quando os bytes contêm cabeçalho PNG válido e tamanho dentro do limite")
    void deveValidarComSucessoBytesPngValidos() {
        byte[] payload = new byte[32];
        System.arraycopy(VALID_PNG_HEADER, 0, payload, 0, VALID_PNG_HEADER.length);
        for (int i = VALID_PNG_HEADER.length; i < payload.length; i++) {
            payload[i] = (byte) (i & 0xFF);
        }

        PngSignatureValidator.ValidatedPng resultado = PngSignatureValidator.validate(payload);

        assertNotNull(resultado, "O resultado da validação não deve ser nulo");
        assertArrayEquals(payload, resultado.bytes(), "Os bytes validados devem ser idênticos aos de entrada");
        assertEquals(32L, resultado.tamanhoBytes(), "O tamanho retornado deve ser igual ao número de bytes do array");
        assertNotNull(resultado.sha256(), "O hash SHA-256 não deve ser nulo");
        assertEquals(64, resultado.sha256().length(), "O SHA-256 deve possuir 64 caracteres hexadecimais");
        assertEquals(PngSignatureValidator.calcularSha256(payload), resultado.sha256(), "O hash SHA-256 calculado deve ser consistente");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Deve lançar AssinaturaFormatoInvalidoException quando o array de bytes for nulo ou vazio")
    void deveLancarExcecaoQuandoBytesNulosOuVazios(byte[] bytes) {
        AssinaturaFormatoInvalidoException exception = assertThrows(
                AssinaturaFormatoInvalidoException.class,
                () -> PngSignatureValidator.validate(bytes),
                "Deveria lançar AssinaturaFormatoInvalidoException para array nulo ou vazio"
        );
        assertTrue(exception.getMessage().contains("não pode estar vazio"),
                "A mensagem de erro deve indicar que o arquivo não pode estar vazio");
    }

    @Test
    @DisplayName("Deve lançar AssinaturaTamanhoExcedidoException quando o arquivo exceder 1 MB")
    void deveLancarExcecaoQuandoTamanhoExcederLimite() {
        int tamanhoExcedido = (int) PngSignatureValidator.LIMITE_MAXIMO_BYTES + 1;
        byte[] bytesGrandes = new byte[tamanhoExcedido];
        System.arraycopy(VALID_PNG_HEADER, 0, bytesGrandes, 0, VALID_PNG_HEADER.length);

        AssinaturaTamanhoExcedidoException exception = assertThrows(
                AssinaturaTamanhoExcedidoException.class,
                () -> PngSignatureValidator.validate(bytesGrandes),
                "Deveria lançar AssinaturaTamanhoExcedidoException para payload maior que 1 MB"
        );
        assertTrue(exception.getMessage().contains("1 MB"),
                "A mensagem de erro deve indicar o limite máximo de 1 MB");
    }

    @Test
    @DisplayName("Deve lançar AssinaturaFormatoInvalidoException quando o array tiver menos de 8 bytes")
    void deveLancarExcecaoQuandoMenorQueOitoBytes() {
        byte[] bytesCurtos = new byte[]{(byte) 0x89, 0x50, 0x4E};

        AssinaturaFormatoInvalidoException exception = assertThrows(
                AssinaturaFormatoInvalidoException.class,
                () -> PngSignatureValidator.validate(bytesCurtos),
                "Deveria lançar AssinaturaFormatoInvalidoException para array com menos de 8 bytes"
        );
        assertTrue(exception.getMessage().contains("cabeçalho de uma imagem PNG válida"),
                "A mensagem deve informar sobre a ausência de cabeçalho PNG válido");
    }

    @Test
    @DisplayName("Deve lançar AssinaturaFormatoInvalidoException para arquivos com outros formatos como PDF ou JPEG")
    void deveLancarExcecaoParaFormatosNaoPng() {
        byte[] pdfHeader = "%PDF-1.4 sample content".getBytes(StandardCharsets.ISO_8859_1);
        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46};

        AssinaturaFormatoInvalidoException exPdf = assertThrows(
                AssinaturaFormatoInvalidoException.class,
                () -> PngSignatureValidator.validate(pdfHeader)
        );
        assertTrue(exPdf.getMessage().contains("não é uma imagem PNG autêntica"));

        AssinaturaFormatoInvalidoException exJpeg = assertThrows(
                AssinaturaFormatoInvalidoException.class,
                () -> PngSignatureValidator.validate(jpegHeader)
        );
        assertTrue(exJpeg.getMessage().contains("não é uma imagem PNG autêntica"));
    }

    @Test
    @DisplayName("calcularSha256 deve gerar hash determinístico de 64 caracteres hexadecimais em minúsculo")
    void deveCalcularSha256Corretamente() {
        byte[] input = "assinatura-digital-teste".getBytes(StandardCharsets.UTF_8);

        String hash1 = PngSignatureValidator.calcularSha256(input);
        String hash2 = PngSignatureValidator.calcularSha256(input);

        assertNotNull(hash1);
        assertEquals(hash1, hash2, "O hash SHA-256 deve ser determinístico");
        assertEquals(64, hash1.length(), "O tamanho do hash SHA-256 deve ser de exatamente 64 caracteres");
        assertTrue(hash1.matches("^[0-9a-f]{64}$"), "O hash deve conter apenas caracteres hexadecimais minúsculos");
    }
}
