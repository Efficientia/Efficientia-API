package com.example.efficientia.caminhao.api;

import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackages = "com.example.efficientia.caminhao")
public class CaminhaoExceptionHandler {

    @ExceptionHandler(CaminhaoNotFoundException.class)
    public ProblemDetail handleNotFound(CaminhaoNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Caminhão/Veículo Não Encontrado");
        problem.setType(URI.create("https://efficientia.com/errors/caminhao-nao-encontrado"));
        return problem;
    }

    @ExceptionHandler(InspecaoVencidaException.class)
    public ProblemDetail handleInspecaoVencida(InspecaoVencidaException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Inspeção de Veículo Vencida");
        problem.setType(URI.create("https://efficientia.com/errors/inspecao-vencida"));
        return problem;
    }

    @ExceptionHandler(CaminhaoEmUsoException.class)
    public ProblemDetail handleEmUso(CaminhaoEmUsoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Caminhão Já Alocado em Viagem");
        problem.setType(URI.create("https://efficientia.com/errors/caminhao-em-uso"));
        return problem;
    }

    @ExceptionHandler(CadastroDuplicadoException.class)
    public ProblemDetail handleDuplicado(CadastroDuplicadoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Placa de Veículo Duplicada");
        problem.setType(URI.create("https://efficientia.com/errors/placa-duplicada"));
        return problem;
    }

    @ExceptionHandler(CadastroInvalidoException.class)
    public ProblemDetail handleInvalido(CadastroInvalidoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Dados de Caminhão Inválidos");
        problem.setType(URI.create("https://efficientia.com/errors/dados-invalidos"));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Argumento Inválido");
        problem.setType(URI.create("https://efficientia.com/errors/argumento-invalido"));
        return problem;
    }
}
