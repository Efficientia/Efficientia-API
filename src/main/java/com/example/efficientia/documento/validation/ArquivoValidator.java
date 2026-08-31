package com.example.efficientia.documento.validation;

import com.example.efficientia.documento.exception.ArquivoInvalidoException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static com.example.efficientia.documento.exception.ArquivoInvalidoException.Reason.INVALID_FILE;
import static com.example.efficientia.documento.exception.ArquivoInvalidoException.Reason.SIZE_LIMIT_EXCEEDED;
import static com.example.efficientia.documento.exception.ArquivoInvalidoException.Reason.UNSUPPORTED_MEDIA_TYPE;

@Component
public class ArquivoValidator {

    private static final long MAX_PDF_BYTES = 25L * 1024 * 1024;
    private static final long MAX_PNG_BYTES = 10L * 1024 * 1024;

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    public ArquivoValidado validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException(INVALID_FILE, "O arquivo é obrigatório e não pode estar vazio.");
        }

        String nomeOriginal = sanitizarNome(arquivo.getOriginalFilename());
        BufferedInputStream input = abrir(arquivo);
        try {
            input.mark(PNG_SIGNATURE.length);
            byte[] cabecalho = input.readNBytes(PNG_SIGNATURE.length);
            input.reset();

            MediaDefinition media = detectar(cabecalho);
            validarHeader(arquivo.getContentType(), media.mimeType());
            validarExtensao(nomeOriginal, media.extension());
            validarTamanho(arquivo.getSize(), media.maxBytes());
            return new ArquivoValidado(nomeOriginal, media.mimeType(), input);
        } catch (RuntimeException | IOException exception) {
            fecharSilenciosamente(input);
            if (exception instanceof ArquivoInvalidoException arquivoInvalido) {
                throw arquivoInvalido;
            }
            throw new ArquivoInvalidoException(INVALID_FILE, "Não foi possível ler o arquivo enviado.");
        }
    }

    private BufferedInputStream abrir(MultipartFile arquivo) {
        try {
            return new BufferedInputStream(arquivo.getInputStream());
        } catch (IOException exception) {
            throw new ArquivoInvalidoException(INVALID_FILE, "Não foi possível ler o arquivo enviado.");
        }
    }

    private MediaDefinition detectar(byte[] cabecalho) {
        if (comecaCom(cabecalho, PDF_SIGNATURE)) {
            return new MediaDefinition("application/pdf", ".pdf", MAX_PDF_BYTES);
        }
        if (comecaCom(cabecalho, PNG_SIGNATURE)) {
            return new MediaDefinition("image/png", ".png", MAX_PNG_BYTES);
        }
        throw new ArquivoInvalidoException(
                UNSUPPORTED_MEDIA_TYPE,
                "Os bytes do arquivo não correspondem a PDF ou PNG."
        );
    }

    private boolean comecaCom(byte[] conteudo, byte[] assinatura) {
        if (conteudo.length < assinatura.length) {
            return false;
        }
        for (int i = 0; i < assinatura.length; i++) {
            if (conteudo[i] != assinatura[i]) {
                return false;
            }
        }
        return true;
    }

    private void validarHeader(String informado, String detectado) {
        if (!detectado.equals(informado)) {
            throw new ArquivoInvalidoException(
                    UNSUPPORTED_MEDIA_TYPE,
                    "O Content-Type informado não corresponde aos bytes do arquivo."
            );
        }
    }

    private void validarExtensao(String nomeOriginal, String extensao) {
        if (!nomeOriginal.toLowerCase(Locale.ROOT).endsWith(extensao)) {
            throw new ArquivoInvalidoException(
                    UNSUPPORTED_MEDIA_TYPE,
                    "A extensão do arquivo não corresponde ao conteúdo detectado."
            );
        }
    }

    private void validarTamanho(long tamanho, long maximo) {
        if (tamanho > maximo) {
            throw new ArquivoInvalidoException(
                    SIZE_LIMIT_EXCEEDED,
                    "O arquivo excede o limite permitido para o tipo detectado."
            );
        }
    }

    private String sanitizarNome(String original) {
        if (original == null) {
            throw new ArquivoInvalidoException(INVALID_FILE, "O nome original do arquivo é obrigatório.");
        }

        String normalizado = original.replace('\\', '/');
        String nome = normalizado.substring(normalizado.lastIndexOf('/') + 1).strip();
        if (nome.isEmpty() || nome.length() > 255 || nome.equals(".") || nome.equals("..")
                || nome.codePoints().anyMatch(Character::isISOControl)) {
            throw new ArquivoInvalidoException(INVALID_FILE, "O nome original do arquivo é inválido.");
        }
        return nome;
    }

    private void fecharSilenciosamente(BufferedInputStream input) {
        try {
            input.close();
        } catch (IOException ignored) {
            // A exceção de validação original é preservada.
        }
    }

    private record MediaDefinition(String mimeType, String extension, long maxBytes) {
    }
}
