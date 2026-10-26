package com.example.efficientia.auth.api;

import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
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

    @ExceptionHandler(CadastroDuplicadoException.class)
    public ProblemDetail cadastroDuplicado(CadastroDuplicadoException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
        problem.setTitle("Cadastro duplicado");
        problem.setType(URI.create("about:blank"));
        return problem;
    }

    @ExceptionHandler(CadastroInvalidoException.class)
    public ProblemDetail cadastroInvalido(CadastroInvalidoException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
        problem.setTitle("Cadastro inválido");
        problem.setType(URI.create("about:blank"));
        return problem;
    }
}
