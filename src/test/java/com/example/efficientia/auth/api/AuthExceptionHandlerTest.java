package com.example.efficientia.auth.api;

import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    @DisplayName("Deve retornar Conflict (409) para cadastro duplicado")
    void deveRetornarConflictParaCadastroDuplicado() {
        CadastroDuplicadoException exception = new CadastroDuplicadoException("CPF já cadastrado");

        ProblemDetail problem = handler.cadastroDuplicado(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Cadastro duplicado");
        assertThat(problem.getDetail()).isEqualTo("CPF já cadastrado");
        assertThat(problem.getType()).isEqualTo(URI.create("about:blank"));
    }

    @Test
    @DisplayName("Deve retornar BadRequest (400) para cadastro inválido")
    void deveRetornarBadRequestParaCadastroInvalido() {
        CadastroInvalidoException exception = new CadastroInvalidoException("Dados inválidos");

        ProblemDetail problem = handler.cadastroInvalido(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Cadastro inválido");
        assertThat(problem.getDetail()).isEqualTo("Dados inválidos");
        assertThat(problem.getType()).isEqualTo(URI.create("about:blank"));
    }

    @Test
    @DisplayName("Deve retornar Unauthorized (401) para autenticação inválida")
    void deveRetornarUnauthorizedParaAutenticacaoInvalida() {
        AutenticacaoInvalidaException exception = new AutenticacaoInvalidaException("Senha incorreta");

        ProblemDetail problem = handler.autenticacaoInvalida(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problem.getTitle()).isEqualTo("Falha na autenticação");
        assertThat(problem.getDetail()).isEqualTo("Senha incorreta");
        assertThat(problem.getType()).isEqualTo(URI.create("about:blank"));
    }
}
