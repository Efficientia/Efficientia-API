package com.example.efficientia.empresa.api;

import com.example.efficientia.auth.api.AuthExceptionHandler;
import com.example.efficientia.auth.api.AutenticacaoInvalidaException;
import com.example.efficientia.cadastrobase.api.CadastroBaseExceptionHandler;
import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.AdminResponse;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.LoginAdminResponse;
import com.example.efficientia.empresa.api.EmpresaContracts.EmpresaResponse;
import com.example.efficientia.empresa.service.EmpresaAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmpresaAdminController.class)
@Import({EmpresaAdminControllerTest.TestDependencies.class, AuthExceptionHandler.class, CadastroBaseExceptionHandler.class})
class EmpresaAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaAdminService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        Mockito.reset(service);
    }

    @Test
    @DisplayName("Deve autenticar admin com sucesso via POST /api/v1/auth/adm/login")
    void deveAutenticarAdmin() throws Exception {
        AdminResponse admin = new AdminResponse(
                10L, 1L, "FRI12345", "12345678000195", "Carlos Silva",
                "carlos@friboi.com.br", "12345678901", "11988887777", "Gerente de RH", true, Instant.now()
        );
        EmpresaResponse empresa = new EmpresaResponse(
                1L, "FRI12345", "Friboi Alimentos", "12345678000195", "contato@friboi.com.br", Instant.now()
        );
        LoginAdminResponse response = new LoginAdminResponse("token.jwt.admin", admin, empresa);

        when(service.autenticarAdmin(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/adm/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "carlos@friboi.com.br",
                                  "senha": "senhaSegura123",
                                  "codigoEmpresa": "FRI12345"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token.jwt.admin"))
                .andExpect(jsonPath("$.admin.nome").value("Carlos Silva"))
                .andExpect(jsonPath("$.admin.cargo").value("Gerente de RH"))
                .andExpect(jsonPath("$.empresa.codigoEmpresa").value("FRI12345"));
    }

    @Test
    @DisplayName("Deve retornar 401 quando autenticação de admin falhar")
    void deveRetornar401QuandoCredenciaisInvalidas() throws Exception {
        when(service.autenticarAdmin(any()))
                .thenThrow(new AutenticacaoInvalidaException("Credenciais inválidas."));

        mockMvc.perform(post("/api/v1/auth/adm/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "carlos@friboi.com.br",
                                  "senha": "senhaIncorreta"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Falha na autenticação"));
    }

    @Test
    @DisplayName("Deve cadastrar primeiro admin com sucesso via POST /api/v1/auth/adm/primeiro-acesso")
    void deveCadastrarPrimeiroAdmin() throws Exception {
        AdminResponse admin = new AdminResponse(
                10L, 1L, "FRI12345", "12345678000195", "Carlos Silva",
                "carlos@friboi.com.br", "12345678901", "11988887777", "Gerente Geral", true, Instant.now()
        );
        EmpresaResponse empresa = new EmpresaResponse(
                1L, "FRI12345", "Friboi Alimentos", "12345678000195", "contato@friboi.com.br", Instant.now()
        );
        LoginAdminResponse response = new LoginAdminResponse("token.jwt.primeiro.admin", admin, empresa);

        when(service.cadastrarPrimeiroAdmin(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/adm/primeiro-acesso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "empresaId": 1,
                                  "nome": "Carlos Silva",
                                  "email": "carlos@friboi.com.br",
                                  "senha": "senhaForte123",
                                  "cpf": "12345678901"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token.jwt.primeiro.admin"))
                .andExpect(jsonPath("$.admin.id").value(10))
                .andExpect(jsonPath("$.empresa.id").value(1));
    }

    @Test
    @DisplayName("Deve retornar 403 no primeiro acesso se a empresa já possuir administradores")
    void deveRetornar403QuandoEmpresaJaTiverAdmins() throws Exception {
        when(service.cadastrarPrimeiroAdmin(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "A empresa já possui administradores cadastrados."));

        mockMvc.perform(post("/api/v1/auth/adm/primeiro-acesso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "empresaId": 1,
                                  "nome": "Segundo Admin",
                                  "email": "segundo@friboi.com.br",
                                  "senha": "senhaForte123"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar 409 se e-mail de admin já estiver cadastrado")
    void deveRetornar409QuandoEmailDuplicado() throws Exception {
        when(service.cadastrarPrimeiroAdmin(any()))
                .thenThrow(new CadastroDuplicadoException("Já existe um administrador cadastrado com o e-mail informado."));

        mockMvc.perform(post("/api/v1/auth/adm/primeiro-acesso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "empresaId": 1,
                                  "nome": "Admin Duplicado",
                                  "email": "duplicado@friboi.com.br",
                                  "senha": "senhaForte123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cadastro duplicado"));
    }

    @Test
    @DisplayName("Deve aderir novo administrador via POST /api/v1/empresas/{empresaId}/adms com token de admin")
    void deveAderirNovoAdmin() throws Exception {
        AdminResponse response = new AdminResponse(
                20L, 1L, "FRI12345", "12345678000195", "Novo Co-Admin",
                "coadmin@friboi.com.br", "22222222222", "11977776666", "RH", true, Instant.now()
        );

        when(service.aderirNovoAdmin(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/empresas/1/adms")
                        .with(jwt().jwt(jwt -> jwt.claim("admin_id", 10L).subject("10")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Novo Co-Admin",
                                  "email": "coadmin@friboi.com.br",
                                  "senha": "senhaAdmin123",
                                  "cpf": "22222222222",
                                  "cargo": "RH"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.nome").value("Novo Co-Admin"))
                .andExpect(jsonPath("$.email").value("coadmin@friboi.com.br"));
    }

    @Test
    @DisplayName("Deve listar administradores da empresa via GET /api/v1/empresas/{empresaId}/adms")
    void deveListarAdminsDaEmpresa() throws Exception {
        AdminResponse admin1 = new AdminResponse(
                10L, 1L, "FRI12345", "12345678000195", "Carlos Silva",
                "carlos@friboi.com.br", null, null, "Diretor", true, Instant.now()
        );
        AdminResponse admin2 = new AdminResponse(
                20L, 1L, "FRI12345", "12345678000195", "Mariana Souza",
                "mariana@friboi.com.br", null, null, "Gerente de RH", true, Instant.now()
        );

        when(service.listarAdminsPorEmpresa(eq(1L), any())).thenReturn(List.of(admin1, admin2));

        mockMvc.perform(get("/api/v1/empresas/1/adms")
                        .with(jwt().jwt(jwt -> jwt.claim("admin_id", 10L).subject("10"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Carlos Silva"))
                .andExpect(jsonPath("$[1].nome").value("Mariana Souza"));
    }

    @Test
    @DisplayName("Deve cadastrar funcionário pela empresa via POST /api/v1/empresas/{empresaId}/funcionarios")
    void deveCadastrarFuncionarioPelaEmpresa() throws Exception {
        var funcionarioResp = new com.example.efficientia.empresa.api.EmpresaAdminContracts.FuncionarioEmpresaResponse(
                100, 1L, "FRI12345", com.example.efficientia.cadastrobase.domain.TipoUsuario.motorista,
                "José Silva", "11122233344", "jose@friboi.com.br", "11988887777", "Motorista", true, Instant.now()
        );

        when(service.cadastrarFuncionario(eq(1L), any(), any())).thenReturn(funcionarioResp);

        mockMvc.perform(post("/api/v1/empresas/1/funcionarios")
                        .with(jwt().jwt(jwt -> jwt.claim("admin_id", 10L).subject("10")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "motorista",
                                  "cpf": "11122233344",
                                  "nome": "José Silva",
                                  "email": "jose@friboi.com.br",
                                  "telefone": "11988887777",
                                  "senha": "senhaSegura123",
                                  "cargo": "Motorista Carreteiro"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("José Silva"))
                .andExpect(jsonPath("$.codigoEmpresa").value("FRI12345"))
                .andExpect(jsonPath("$.tipo").value("motorista"));
    }

    @Test
    @DisplayName("Deve listar funcionários da empresa via GET /api/v1/empresas/{empresaId}/funcionarios")
    void deveListarFuncionariosDaEmpresa() throws Exception {
        var funcionario1 = new com.example.efficientia.empresa.api.EmpresaAdminContracts.FuncionarioEmpresaResponse(
                100, 1L, "FRI12345", com.example.efficientia.cadastrobase.domain.TipoUsuario.motorista,
                "José Silva", "11122233344", "jose@friboi.com.br", "11988887777", "Motorista", true, Instant.now()
        );

        when(service.listarFuncionariosPorEmpresa(eq(1L), any())).thenReturn(List.of(funcionario1));

        mockMvc.perform(get("/api/v1/empresas/1/funcionarios")
                        .with(jwt().jwt(jwt -> jwt.claim("admin_id", 10L).subject("10"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("José Silva"));
    }

    @Test
    @DisplayName("Deve aceitar primeiro acesso via alias POST /api/v1/empresas/{empresaId}/primeiro-admin")
    void deveAceitarPrimeiroAcessoViaAlias() throws Exception {
        AdminResponse adminResp = new AdminResponse(
                10L, 1L, "FRI12345", "12345678000195", "Carlos Silva",
                "carlos@friboi.com.br", "12345678901", "11988887777", "Gerente Geral", true, Instant.now()
        );
        EmpresaResponse empresaResp = new EmpresaResponse(
                1L, "FRI12345", "Friboi Alimentos", "12345678000195", "contato@friboi.com.br", Instant.now()
        );
        LoginAdminResponse response = new LoginAdminResponse("token.jwt.mock", adminResp, empresaResp);

        when(service.cadastrarPrimeiroAdmin(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/empresas/1/primeiro-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Carlos Silva",
                                  "email": "carlos@friboi.com.br",
                                  "senha": "senhaSegura123",
                                  "cpf": "12345678901"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token.jwt.mock"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestDependencies {
        @Bean
        EmpresaAdminService empresaAdminService() {
            return Mockito.mock(EmpresaAdminService.class);
        }
    }
}
