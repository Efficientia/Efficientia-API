package com.example.efficientia.empresa.api;

import com.example.efficientia.cadastrobase.api.CadastroBaseExceptionHandler;
import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.empresa.api.EmpresaContracts.EmpresaResponse;
import com.example.efficientia.empresa.service.EmpresaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmpresaController.class)
@Import({EmpresaControllerTest.TestDependencies.class, CadastroBaseExceptionHandler.class})
class EmpresaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaService empresaService;

    @Test
    @DisplayName("Deve cadastrar empresa com sucesso via POST /api/v1/empresas")
    void deveCadastrarEmpresa() throws Exception {
        EmpresaResponse response = new EmpresaResponse(
                1L,
                "FRI12345",
                "Friboi Alimentos",
                "12345678000195",
                "contato@friboi.com.br",
                Instant.now()
        );
        when(empresaService.cadastrarEmpresa(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeEmpresa": "Friboi Alimentos",
                                  "cnpj": "12.345.678/0001-95",
                                  "emailCorporativo": "contato@friboi.com.br",
                                  "senha": "senhaSegura123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.codigoEmpresa").value("FRI12345"))
                .andExpect(jsonPath("$.codigo").value("FRI12345"))
                .andExpect(jsonPath("$.nomeEmpresa").value("Friboi Alimentos"))
                .andExpect(jsonPath("$.cnpj").value("12345678000195"))
                .andExpect(jsonPath("$.emailCorporativo").value("contato@friboi.com.br"));
    }

    @Test
    @DisplayName("Deve suportar aliases nos campos (nome, email, senha) no cadastro de empresa")
    void deveCadastrarEmpresaComAliases() throws Exception {
        EmpresaResponse response = new EmpresaResponse(
                2L,
                "JBS99887",
                "JBS S/A",
                "98765432000110",
                "admin@jbs.com.br",
                Instant.now()
        );
        when(empresaService.cadastrarEmpresa(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "JBS S/A",
                                  "cnpj": "98765432000110",
                                  "email": "admin@jbs.com.br",
                                  "senha": "senhaSegura123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.codigoEmpresa").value("JBS99887"));
    }

    @Test
    @DisplayName("Deve aceitar cadastro também via alias /api/v1/auth/empresas")
    void deveCadastrarViaAliasAuthEmpresas() throws Exception {
        EmpresaResponse response = new EmpresaResponse(
                3L,
                "SEA55443",
                "Seara",
                "11222333000144",
                "seara@empresa.com",
                Instant.now()
        );
        when(empresaService.cadastrarEmpresa(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeEmpresa": "Seara",
                                  "cnpj": "11222333000144",
                                  "emailCorporativo": "seara@empresa.com",
                                  "senha": "senhaSegura123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoEmpresa").value("SEA55443"));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se faltarem campos obrigatórios")
    void deveRetornar400QuandoDadosIncompletos() throws Exception {
        mockMvc.perform(post("/api/v1/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"));
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict se CNPJ ou e-mail já estiver cadastrado")
    void deveRetornar409QuandoDuplicado() throws Exception {
        when(empresaService.cadastrarEmpresa(any()))
                .thenThrow(new CadastroDuplicadoException("Já existe uma empresa cadastrada com o CNPJ informado."));

        mockMvc.perform(post("/api/v1/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeEmpresa": "Empresa Duplicada",
                                  "cnpj": "12345678000195",
                                  "emailCorporativo": "duplicada@empresa.com",
                                  "senha": "senhaSegura123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cadastro duplicado"));
    }

    @Test
    @DisplayName("Deve listar empresas e buscar por código")
    void deveListarEBuscarPorCodigo() throws Exception {
        EmpresaResponse response = new EmpresaResponse(
                1L,
                "FRI12345",
                "Friboi Alimentos",
                "12345678000195",
                "contato@friboi.com.br",
                Instant.now()
        );
        when(empresaService.listarEmpresas()).thenReturn(List.of(response));
        when(empresaService.buscarPorCodigo("FRI12345")).thenReturn(response);

        mockMvc.perform(get("/api/v1/empresas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoEmpresa").value("FRI12345"));

        mockMvc.perform(get("/api/v1/empresas/codigo/FRI12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoEmpresa").value("FRI12345"));
    }

    @Test
    @DisplayName("Deve realizar login/verificação da empresa via POST /api/v1/auth/empresa/login")
    void deveVerificarStatusEmpresaLogin() throws Exception {
        EmpresaResponse empresaResp = new EmpresaResponse(
                1L, "FRI12345", "Friboi Alimentos", "JBS S.A.", "12345678000195",
                "contato@friboi.com.br", 1, "PENDENTE_PRIMEIRO_ADMIN", true,
                "CADASTRO_PRIMEIRO_ADMIN", "Aguardando admin", Instant.now()
        );
        var loginResponse = com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaResponse.pendentePrimeiroAdmin(empresaResp);

        when(empresaService.autenticarEmpresa(any())).thenReturn(loginResponse);

        mockMvc.perform(post("/api/v1/auth/empresa/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cnpj": "12.345.678/0001-95"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE_PRIMEIRO_ADMIN"))
                .andExpect(jsonPath("$.requerPrimeiroAdmin").value(true))
                .andExpect(jsonPath("$.proximoPasso").value("CADASTRO_PRIMEIRO_ADMIN"))
                .andExpect(jsonPath("$.empresa.cnpj").value("12345678000195"));
    }

    @Test
    @DisplayName("Deve buscar empresa por CNPJ via GET /api/v1/empresas/cnpj/{cnpj}")
    void deveBuscarPorCnpj() throws Exception {
        EmpresaResponse response = new EmpresaResponse(
                1L, "FRI12345", "Friboi Alimentos", "JBS S.A.", "12345678000195",
                "contato@friboi.com.br", 1, "ATIVO", false,
                "PAINEL_ADMINISTRATIVO", "Ativo", Instant.now()
        );
        when(empresaService.buscarPorCnpj("12345678000195")).thenReturn(response);

        mockMvc.perform(get("/api/v1/empresas/cnpj/12345678000195"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value("12345678000195"))
                .andExpect(jsonPath("$.nomeEmpresa").value("Friboi Alimentos"));
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean
        EmpresaService empresaService() {
            return Mockito.mock(EmpresaService.class);
        }
    }
}
