package com.example.efficientia.cadastrobase.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.net.URI;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class CadastroBaseExceptionHandler {

    @ExceptionHandler(CadastroDuplicadoException.class)
    public ProblemDetail cadastroDuplicado(CadastroDuplicadoException exception) {
        return problem(HttpStatus.CONFLICT, "Cadastro duplicado", exception.getMessage());
    }

    @ExceptionHandler(CadastroInvalidoException.class)
    public ProblemDetail cadastroInvalido(CadastroInvalidoException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Cadastro inválido", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException exception) {
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST,
                "Dados inválidos",
                "Revise os campos informados."
        );
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        detail.setProperty("erros", errors);
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail jsonInvalido() {
        return problem(HttpStatus.BAD_REQUEST, "JSON inválido", "Revise os tipos e valores enviados.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridade() {
        return problem(
                HttpStatus.CONFLICT,
                "Conflito de dados",
                "O cadastro viola uma referência ou valor único existente."
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail responseStatus(ResponseStatusException exception) {
        return problem(
                HttpStatus.valueOf(exception.getStatusCode().value()),
                "Operação não concluída",
                exception.getReason()
        );
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail == null ? title : detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        return problem;
    }
}
