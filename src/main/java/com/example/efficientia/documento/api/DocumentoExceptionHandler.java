package com.example.efficientia.documento.api;

import com.example.efficientia.documento.exception.ArquivoInvalidoException;
import com.example.efficientia.documento.exception.DocumentoConcorrenteException;
import com.example.efficientia.documento.exception.DocumentoInvalidoException;
import com.example.efficientia.documento.exception.DocumentoNaoEncontradoException;
import com.example.efficientia.documento.exception.DocumentoSemConteudoException;
import com.example.efficientia.documento.exception.RegraDocumentoException;
import com.example.efficientia.documento.storage.StorageException;
import com.example.efficientia.documento.storage.StorageValidationException;
import com.example.efficientia.exportacao.api.ExportacaoController;
import com.example.efficientia.exportacao.exception.ExportacaoConflitoException;
import com.example.efficientia.exportacao.exception.ExportacaoInvalidaException;
import com.example.efficientia.exportacao.exception.ExportacaoNaoEncontradaException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = {DocumentoController.class, ExportacaoController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DocumentoExceptionHandler {

    @ExceptionHandler(DocumentoNaoEncontradoException.class)
    public ProblemDetail documentoNaoEncontrado(DocumentoNaoEncontradoException exception) {
        return problem(HttpStatus.NOT_FOUND, "DOCUMENTO_NAO_ENCONTRADO",
                "Documento não encontrado", exception.getMessage());
    }

    @ExceptionHandler(DocumentoSemConteudoException.class)
    public ProblemDetail documentoSemConteudo(DocumentoSemConteudoException exception) {
        return problem(HttpStatus.CONFLICT, "DOCUMENTO_SEM_CONTEUDO",
                "Documento sem conteúdo", exception.getMessage());
    }

    @ExceptionHandler(DocumentoConcorrenteException.class)
    public ProblemDetail documentoConcorrente(DocumentoConcorrenteException exception) {
        return problem(HttpStatus.CONFLICT, "DOCUMENTO_CONCORRENTE",
                "Atualização concorrente", exception.getMessage());
    }

    @ExceptionHandler(RegraDocumentoException.class)
    public ProblemDetail regraDeNegocio(RegraDocumentoException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_DOCUMENTO_VIOLADA",
                "Regra de documento violada", exception.getMessage());
    }

    @ExceptionHandler(DocumentoInvalidoException.class)
    public ProblemDetail documentoInvalido(DocumentoInvalidoException exception) {
        return problem(HttpStatus.BAD_REQUEST, "DOCUMENTO_INVALIDO",
                "Documento inválido", exception.getMessage());
    }

    @ExceptionHandler(ArquivoInvalidoException.class)
    public ProblemDetail arquivoInvalido(ArquivoInvalidoException exception) {
        return switch (exception.getReason()) {
            case SIZE_LIMIT_EXCEEDED -> problem(HttpStatus.PAYLOAD_TOO_LARGE,
                    "ARQUIVO_MUITO_GRANDE", "Arquivo muito grande", exception.getMessage());
            case UNSUPPORTED_MEDIA_TYPE -> problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "TIPO_ARQUIVO_NAO_SUPORTADO", "Arquivo inválido", exception.getMessage());
            case INVALID_FILE -> problem(HttpStatus.BAD_REQUEST,
                    "ARQUIVO_INVALIDO", "Arquivo inválido", exception.getMessage());
        };
    }

    @ExceptionHandler(StorageValidationException.class)
    public ProblemDetail storageValidation(StorageValidationException exception) {
        return switch (exception.getReason()) {
            case SIZE_LIMIT_EXCEEDED -> problem(HttpStatus.PAYLOAD_TOO_LARGE,
                    "ARQUIVO_MUITO_GRANDE", "Arquivo rejeitado", exception.getMessage());
            case UNSUPPORTED_MEDIA_TYPE -> problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "TIPO_ARQUIVO_NAO_SUPORTADO", "Arquivo rejeitado", exception.getMessage());
            default -> problem(HttpStatus.BAD_REQUEST,
                    "STORAGE_ENTRADA_INVALIDA", "Arquivo rejeitado", exception.getMessage());
        };
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail uploadMuitoGrande() {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "ARQUIVO_MUITO_GRANDE",
                "Arquivo muito grande", "A requisição excede o limite configurado para upload.");
    }

    @ExceptionHandler(ExportacaoInvalidaException.class)
    public ProblemDetail exportacaoInvalida(ExportacaoInvalidaException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "EXPORTACAO_INVALIDA",
                "Exportação inválida", exception.getMessage());
    }

    @ExceptionHandler(ExportacaoConflitoException.class)
    public ProblemDetail exportacaoConflito(ExportacaoConflitoException exception) {
        return problem(HttpStatus.CONFLICT, "EXPORTACAO_CONFLITO",
                "Conflito de exportação", exception.getMessage());
    }

    @ExceptionHandler(ExportacaoNaoEncontradaException.class)
    public ProblemDetail exportacaoNaoEncontrada(ExportacaoNaoEncontradaException exception) {
        return problem(HttpStatus.NOT_FOUND, "EXPORTACAO_NAO_ENCONTRADA",
                "Exportação não encontrada", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail camposInvalidos(MethodArgumentNotValidException exception) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "CAMPOS_INVALIDOS",
                "Campos inválidos", "Revise os campos informados na requisição.");
        List<Map<String, String>> erros = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("campo", error.getField());
                    item.put("mensagem", error.getDefaultMessage());
                    return item;
                })
                .toList();
        problem.setProperty("erros", erros);
        return problem;
    }

    @ExceptionHandler({
            MissingRequestHeaderException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ProblemDetail requisicaoInvalida(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA",
                "Requisição inválida", mensagemSegura(exception));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail mediaTypeDaRequisicao() {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "CONTENT_TYPE_NAO_SUPORTADO",
                "Content-Type não suportado", "Use um Content-Type aceito pelo endpoint.");
    }

    @ExceptionHandler(StorageException.class)
    public ProblemDetail storageIndisponivel() {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_INDISPONIVEL",
                "Storage indisponível", "Não foi possível concluir a operação com o arquivo.");
    }

    private ProblemDetail problem(HttpStatus status, String code, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://efficientia.local/problems/" + code.toLowerCase()));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", correlationId());
        return problem;
    }

    private String correlationId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
            if (correlationId instanceof String value) {
                return value;
            }
        }
        return UUID.randomUUID().toString();
    }

    private String mensagemSegura(Exception exception) {
        if (exception instanceof MissingRequestHeaderException missing) {
            return "O header " + missing.getHeaderName() + " é obrigatório.";
        }
        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            return "O parâmetro " + mismatch.getName() + " possui formato inválido.";
        }
        return "O corpo da requisição está ausente ou possui formato inválido.";
    }
}
