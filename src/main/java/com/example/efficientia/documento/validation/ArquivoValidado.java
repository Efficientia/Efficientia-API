package com.example.efficientia.documento.validation;

import java.io.InputStream;

public record ArquivoValidado(
        String nomeOriginal,
        String mimeType,
        InputStream conteudo
) {
}
