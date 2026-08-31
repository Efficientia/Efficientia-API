package com.example.efficientia.documento.validation;

import com.example.efficientia.documento.api.AssinaturaTextoRequest;
import com.example.efficientia.documento.api.DocumentoMetadataRequest;
import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.TipoDocumento;
import com.example.efficientia.documento.exception.DocumentoInvalidoException;
import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
public class AssinaturaValidator {

    private static final int MAX_TEXTO_LENGTH = 150;

    public void validarArquivo(DocumentoMetadataRequest request, String mimeType) {
        if (request.tipoDocumento() != TipoDocumento.ASSINATURA) {
            if (request.assinanteId() != null || request.papelAssinante() != null
                    || request.modalidadeAssinatura() != null) {
                throw invalida("Campos de assinante e modalidade são exclusivos de assinaturas.");
            }
            return;
        }

        if (request.assinanteId() == null || request.assinanteId() <= 0
                || request.papelAssinante() == null || request.modalidadeAssinatura() == null) {
            throw invalida("Assinatura com arquivo exige assinante, papel e modalidade.");
        }
        if (!"image/png".equals(mimeType)) {
            throw invalida("Assinaturas por foto ou desenho exigem arquivo PNG.");
        }

        if (request.modalidadeAssinatura() == ModalidadeAssinatura.FOTO) {
            if (request.origem() != OrigemDocumento.CAMERA) {
                throw invalida("Assinatura por FOTO exige origem CAMERA.");
            }
            return;
        }
        if (request.modalidadeAssinatura() == ModalidadeAssinatura.DESENHO) {
            if (request.origem() != OrigemDocumento.DESENHO) {
                throw invalida("Assinatura por DESENHO exige origem DESENHO.");
            }
            return;
        }
        throw invalida("Multipart aceita somente assinatura por FOTO ou DESENHO.");
    }

    public String validarTexto(AssinaturaTextoRequest request) {
        if (request == null) {
            throw invalida("Os dados da assinatura textual são obrigatórios.");
        }
        if (request.viagemId() == null || request.viagemId() <= 0
                || request.assinanteId() == null || request.assinanteId() <= 0
                || request.papelAssinante() == null) {
            throw invalida("Assinatura textual exige viagem, assinante e papel válidos.");
        }
        if (request.tipoDocumento() != TipoDocumento.ASSINATURA
                || request.origem() != OrigemDocumento.TEXTO
                || request.modalidadeAssinatura() != ModalidadeAssinatura.TEXTO) {
            throw invalida("Assinatura textual exige tipo ASSINATURA, origem TEXTO e modalidade TEXTO.");
        }
        if (request.textoAssinatura() == null) {
            throw invalida("O texto da assinatura é obrigatório.");
        }

        String texto = Normalizer.normalize(request.textoAssinatura().strip(), Normalizer.Form.NFC);
        int caracteres = texto.codePointCount(0, texto.length());
        if (caracteres < 1 || caracteres > MAX_TEXTO_LENGTH) {
            throw invalida("O texto da assinatura deve conter entre 1 e 150 caracteres.");
        }
        if (texto.indexOf('<') >= 0 || texto.indexOf('>') >= 0) {
            throw invalida("O texto da assinatura não pode conter HTML.");
        }
        if (texto.codePoints().anyMatch(Character::isISOControl)) {
            throw invalida("O texto da assinatura não pode conter caracteres de controle.");
        }
        return texto;
    }

    private DocumentoInvalidoException invalida(String message) {
        return new DocumentoInvalidoException(message);
    }
}
