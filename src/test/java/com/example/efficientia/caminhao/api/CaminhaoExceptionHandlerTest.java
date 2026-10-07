package com.example.efficientia.caminhao.api;

import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaminhaoExceptionHandlerTest {

    private final CaminhaoExceptionHandler handler = new CaminhaoExceptionHandler();

    @Test
    @DisplayName("Caminhão não encontrado deve gerar ProblemDetail 404")
    void deveTratarCaminhaoNaoEncontrado() {
        ProblemDetail problem = handler.handleNotFound(new CaminhaoNotFoundException("Caminhão 42 não encontrado"));

        assertProblem(problem, HttpStatus.NOT_FOUND, "Caminhão/Veículo Não Encontrado",
                "https://efficientia.com/errors/caminhao-nao-encontrado", "Caminhão 42 não encontrado");
    }

    @Test
    @DisplayName("Inspeção vencida deve gerar ProblemDetail 422")
    void deveTratarInspecaoVencida() {
        ProblemDetail problem = handler.handleInspecaoVencida(new InspecaoVencidaException("Inspeção vencida"));

        assertProblem(problem, HttpStatus.UNPROCESSABLE_ENTITY, "Inspeção de Veículo Vencida",
                "https://efficientia.com/errors/inspecao-vencida", "Inspeção vencida");
    }

    @Test
    @DisplayName("Caminhão em uso deve gerar ProblemDetail 409")
    void deveTratarCaminhaoEmUso() {
        ProblemDetail problem = handler.handleEmUso(new CaminhaoEmUsoException("Veículo está alocado"));

        assertProblem(problem, HttpStatus.CONFLICT, "Caminhão Já Alocado em Viagem",
                "https://efficientia.com/errors/caminhao-em-uso", "Veículo está alocado");
    }

    @Test
    @DisplayName("Cadastro duplicado deve gerar ProblemDetail 409")
    void deveTratarCadastroDuplicado() {
        ProblemDetail problem = handler.handleDuplicado(new CadastroDuplicadoException("Placa já cadastrada"));

        assertProblem(problem, HttpStatus.CONFLICT, "Placa de Veículo Duplicada",
                "https://efficientia.com/errors/placa-duplicada", "Placa já cadastrada");
    }

    @Test
    @DisplayName("Cadastro inválido deve gerar ProblemDetail 400")
    void deveTratarCadastroInvalido() {
        ProblemDetail problem = handler.handleInvalido(new CadastroInvalidoException("Dados inválidos"));

        assertProblem(problem, HttpStatus.BAD_REQUEST, "Dados de Caminhão Inválidos",
                "https://efficientia.com/errors/dados-invalidos", "Dados inválidos");
    }

    @Test
    @DisplayName("Argumento inválido deve gerar ProblemDetail 400")
    void deveTratarArgumentoInvalido() {
        ProblemDetail problem = handler.handleArgument(new IllegalArgumentException("Parâmetro inválido"));

        assertProblem(problem, HttpStatus.BAD_REQUEST, "Argumento Inválido",
                "https://efficientia.com/errors/argumento-invalido", "Parâmetro inválido");
    }

    private void assertProblem(ProblemDetail actual, HttpStatus expectedStatus, String expectedTitle,
                              String expectedType, String expectedDetail) {
        assertEquals(expectedStatus.value(), actual.getStatus());
        assertEquals(expectedTitle, actual.getTitle());
        assertEquals(expectedType, actual.getType().toString());
        assertEquals(expectedDetail, actual.getDetail());
    }
}
