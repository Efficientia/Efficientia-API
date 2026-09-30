package com.example.efficientia.empresa.api;

import com.example.efficientia.cadastrobase.api.CadastroBaseExceptionHandler;
import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.empresa.api.EmpresaContracts.AtualizarDadosEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.EmpresaResponse;
import com.example.efficientia.empresa.api.EmpresaContracts.EnderecoDto;
import com.example.efficientia.empresa.api.EmpresaContracts.UploadLogoResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Test
    @DisplayName("Deve atualizar dados complementares da empresa via PUT /api/v1/empresas/{id}/dados-complementares")
    void deveAtualizarDadosComplementares() throws Exception {
        EnderecoDto endereco = new EnderecoDto(1, "79002190", "Av. Afonso Pena", "2450", "Campo Grande", "MS");
        EmpresaResponse response = new EmpresaResponse(
                1L, "EFF12345", "EFF12345", "EFF12345",
                "Efficientia Transportes", "Efficientia Transportes", "Efficientia", "Efficientia Transportes Ltda.",
                "12345678000190", "operacao@efficientia.com.br", "operacao@efficientia.com.br",
                "+55 (67) 99999-2048", 1, endereco, "79002190", "Av. Afonso Pena", "2450", "Campo Grande", "MS", "MS",
                "/api/v1/empresas/1/logo/conteudo", 2, false, "ATIVO", false, "PAINEL_ADMINISTRATIVO",
                "Dados da empresa atualizados com sucesso.", Instant.now(), Instant.now()
        );

        when(empresaService.atualizarDadosComplementares(eq(1L), any(AtualizarDadosEmpresaRequest.class)))
                .thenReturn(response);

        String json = """
                {
                  "nomeFantasia": "Efficientia",
                  "razaoSocial": "Efficientia Transportes Ltda.",
                  "cnpj": "12.345.678/0001-90",
                  "emailCorporativo": "operacao@efficientia.com.br",
                  "telefone": "+55 (67) 99999-2048",
                  "cep": "79002-190",
                  "logradouro": "Av. Afonso Pena",
                  "numero": "2450",
                  "cidade": "Campo Grande",
                  "estado": "MS"
                }
                """;

        mockMvc.perform(put("/api/v1/empresas/1/dados-complementares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeFantasia").value("Efficientia"))
                .andExpect(jsonPath("$.telefone").value("+55 (67) 99999-2048"))
                .andExpect(jsonPath("$.logradouro").value("Av. Afonso Pena"))
                .andExpect(jsonPath("$.etapaCadastro").value(2));
    }

    @Test
    @DisplayName("Deve atualizar dados complementares por código via PUT /api/v1/empresas/codigo/{codigo}")
    void deveAtualizarDadosComplementaresPorCodigo() throws Exception {
        EmpresaResponse response = new EmpresaResponse(
                1L, "EFF12345", "EFF12345", "EFF12345",
                "Efficientia", "Efficientia", "Efficientia", "Efficientia Ltda",
                "12345678000190", "operacao@efficientia.com.br", "operacao@efficientia.com.br",
                "+55 67 9999-9999", null, null, null, null, null, null, null, null,
                null, 2, false, "ATIVO", false, "PAINEL_ADMINISTRATIVO",
                "Atualizado", Instant.now(), Instant.now()
        );

        when(empresaService.atualizarDadosComplementaresPorCodigo(eq("EFF12345"), any(AtualizarDadosEmpresaRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/empresas/codigo/EFF12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\": \"+55 67 9999-9999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoEmpresa").value("EFF12345"))
                .andExpect(jsonPath("$.etapaCadastro").value(2));
    }

    @Test
    @DisplayName("Deve fazer upload de logotipo via POST /api/v1/empresas/{id}/logo")
    void deveFazerUploadLogotipo() throws Exception {
        UploadLogoResponse uploadResp = new UploadLogoResponse("/api/v1/empresas/1/logo/conteudo", "Logo enviada com sucesso.", 1024L, "image/png");
        when(empresaService.atualizarLogo(eq(1L), any())).thenReturn(uploadResp);

        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "arquivo", "logo.png", "image/png", new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/empresas/1/logo").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("/api/v1/empresas/1/logo/conteudo"))
                .andExpect(jsonPath("$.mimeType").value("image/png"));
    }

    @Test
    @DisplayName("Deve obter bytes do logotipo via GET /api/v1/empresas/{id}/logo/conteudo")
    void deveObterLogotipoConteudo() throws Exception {
        byte[] bytes = new byte[]{10, 20, 30};
        when(empresaService.obterConteudoLogo(1L)).thenReturn(bytes);
        when(empresaService.obterMimeTypeLogo(1L)).thenReturn("image/png");

        mockMvc.perform(get("/api/v1/empresas/1/logo/conteudo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", "public, max-age=86400"))
                .andExpect(content().bytes(bytes));
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean
        EmpresaService empresaService() {
            return Mockito.mock(EmpresaService.class);
        }
    }
}
