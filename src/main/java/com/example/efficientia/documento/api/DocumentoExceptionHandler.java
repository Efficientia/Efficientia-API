package com.example.efficientia.documento.api;

import com.example.efficientia.documento.exception.ArquivoInvalidoException;
import com.example.efficientia.documento.exception.DocumentoInvalidoException;
import com.example.efficientia.documento.storage.StorageException;
import com.example.efficientia.documento.storage.StorageValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;

@RestControllerAdvice(assignableTypes = DocumentoController.class)
public class DocumentoExceptionHandler {

    @ExceptionHandler(DocumentoInvalidoException.class)
    public ProblemDetail documentoInvalido(DocumentoInvalidoException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Documento inválido", exception.getMessage());
    }

    @ExceptionHandler(ArquivoInvalidoException.class)
    public ProblemDetail arquivoInvalido(ArquivoInvalidoException exception) {
        HttpStatus status = exception.getReason()
                == ArquivoInvalidoException.Reason.UNSUPPORTED_MEDIA_TYPE
                ? HttpStatus.UNSUPPORTED_MEDIA_TYPE
                : HttpStatus.BAD_REQUEST;
        return problem(status, "Arquivo inválido", exception.getMessage());
    }

    @ExceptionHandler(StorageValidationException.class)
    public ProblemDetail storageValidation(StorageValidationException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case SIZE_LIMIT_EXCEEDED -> HttpStatus.PAYLOAD_TOO_LARGE;
            case UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            default -> HttpStatus.BAD_REQUEST;
        };
        return problem(status, "Arquivo rejeitado", exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail uploadMuitoGrande() {
        return problem(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Arquivo muito grande",
                "A requisição excede o limite configurado para upload."
        );
    }

    @ExceptionHandler(StorageException.class)
    public ProblemDetail storageIndisponivel() {
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Storage indisponível",
                "Não foi possível concluir a operação com o arquivo."
        );
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        return problem;
    }
}
