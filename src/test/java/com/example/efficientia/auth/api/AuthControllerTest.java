package com.example.efficientia.auth.api;

import com.example.efficientia.auth.api.AuthContracts.LoginResponse;
import com.example.efficientia.auth.service.AuthService;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.UsuarioResponse;
import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({AuthControllerTest.TestDependencies.class, AuthExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Test
    void deveAutenticarEDevolverTokenEUsuario() throws Exception {
        UsuarioResponse usuarioResponse = new UsuarioResponse(
                1,
                TipoUsuario.motorista,
                "12345678901",
                "EMP-100",
                "Motorista Teste",
                LocalDate.of(1990, 1, 1),
                "motorista@test.com",
                "11999998888",
                true
        );
        LoginResponse response = new LoginResponse("mocked.jwt.token", usuarioResponse);
        when(authService.autenticar(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf": "12345678901",
                                  "email": "motorista@test.com",
                                  "senha": "senha123",
                                  "codigoEmpresa": "EMP-100"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.cpf").value("12345678901"))
                .andExpect(jsonPath("$.usuario.nome").value("Motorista Teste"));
    }

    @Test
    void deveRetornar401QuandoCredenciaisForemInvalidas() throws Exception {
        when(authService.autenticar(any()))
                .thenThrow(new AutenticacaoInvalidaException("Credenciais inválidas: senha incorreta."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf": "12345678901",
                                  "email": "motorista@test.com",
                                  "senha": "senhaIncorreta",
                                  "codigoEmpresa": "EMP-100"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Falha na autenticação"))
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas: senha incorreta."));
    }

    @Test
    void deveRetornar400QuandoDadosEstiveremIncompletos() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean
        AuthService authService() {
            return Mockito.mock(AuthService.class);
        }
    }
}
