package com.example.efficientia.auth.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AutenticacaoInvalidaException.class)
    public ProblemDetail autenticacaoInvalida(AutenticacaoInvalidaException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage()
        );
        problem.setTitle("Falha na autenticação");
        problem.setType(URI.create("about:blank"));
        return problem;
    }
}
