package com.example.efficientia.assinaturamotorista.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

@RestControllerAdvice(basePackages = "com.example.efficientia.assinaturamotorista")
public class AssinaturaMotoristaExceptionHandler {

    @ExceptionHandler(AssinaturaNaoEncontradaException.class)
    public ProblemDetail handleNaoEncontrada(AssinaturaNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Assinatura Não Encontrada");
        problem.setType(URI.create("https://efficientia.com/errors/assinatura-nao-encontrada"));
        return problem;
    }

    @ExceptionHandler(AssinaturaFormatoInvalidoException.class)
    public ProblemDetail handleFormatoInvalido(AssinaturaFormatoInvalidoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
        problem.setTitle("Formato de Assinatura Inválido");
        problem.setType(URI.create("https://efficientia.com/errors/formato-invalido"));
        return problem;
    }

    @ExceptionHandler(AssinaturaTamanhoExcedidoException.class)
    public ProblemDetail handleTamanhoExcedido(AssinaturaTamanhoExcedidoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
        problem.setTitle("Tamanho da Assinatura Excedido");
        problem.setType(URI.create("https://efficientia.com/errors/tamanho-excedido"));
        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo de assinatura excede o limite máximo permitido pelo servidor.");
        problem.setTitle("Tamanho de Upload Excedido");
        problem.setType(URI.create("https://efficientia.com/errors/tamanho-excedido"));
        return problem;
    }

    @ExceptionHandler(MotoristaInvalidoException.class)
    public ProblemDetail handleMotoristaInvalido(MotoristaInvalidoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Perfil de Motorista Inválido");
        problem.setType(URI.create("https://efficientia.com/errors/motorista-invalido"));
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Acesso Negado");
        problem.setType(URI.create("https://efficientia.com/errors/acesso-negado"));
        return problem;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "O cabeçalho obrigatório '" + ex.getHeaderName() + "' não foi informado.");
        problem.setTitle("Cabeçalho Obrigatório Ausente");
        problem.setType(URI.create("https://efficientia.com/errors/cabecalho-ausente"));
        return problem;
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingPart(MissingServletRequestPartException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "A parte multipart obrigatória '" + ex.getRequestPartName() + "' não foi enviada.");
        problem.setTitle("Parte Multipart Ausente");
        problem.setType(URI.create("https://efficientia.com/errors/multipart-ausente"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Erro de validação nos metadados da assinatura.");
        problem.setTitle("Metadados Inválidos");
        problem.setType(URI.create("https://efficientia.com/errors/metadados-invalidos"));
        return problem;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
        problem.setTitle("Erro na Requisição");
        problem.setType(URI.create("https://efficientia.com/errors/erro-requisicao"));
        return problem;
    }
}
