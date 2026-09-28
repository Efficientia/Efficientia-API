package com.example.efficientia.empresa.service;

import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
import com.example.efficientia.empresa.api.EmpresaContracts.CriarEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.EmpresaResponse;
import com.example.efficientia.empresa.persistence.InMemoryEmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmpresaServiceTest {

    private InMemoryEmpresaRepository repository;
    private CodigoEmpresaGenerator codigoGenerator;
    private EmpresaService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEmpresaRepository();
        codigoGenerator = new CodigoEmpresaGenerator();
        service = new EmpresaService(repository, codigoGenerator);
    }

    @Test
    @DisplayName("Deve cadastrar empresa com sucesso e gerar código de 8 dígitos")
    void deveCadastrarEmpresaComSucesso() {
        CriarEmpresaRequest request = new CriarEmpresaRequest(
                "Friboi Alimentos",
                "12.345.678/0001-95",
                "contato@friboi.com.br",
                "senhaForte123"
        );

        EmpresaResponse response = service.cadastrarEmpresa(request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertNotNull(response.codigoEmpresa());
        assertEquals(8, response.codigoEmpresa().length());
        assertTrue(response.codigoEmpresa().startsWith("FRI"));
        assertEquals("Friboi Alimentos", response.nomeEmpresa());
        assertEquals("12345678000195", response.cnpj());
        assertEquals("contato@friboi.com.br", response.emailCorporativo());
    }

    @Test
    @DisplayName("Deve rejeitar CNPJ com quantidade incorreta de dígitos")
    void deveRejeitarCnpjInvalido() {
        CriarEmpresaRequest request = new CriarEmpresaRequest(
                "Empresa Teste",
                "12345",
                "teste@empresa.com",
                "senha123"
        );

        assertThrows(CadastroInvalidoException.class, () -> service.cadastrarEmpresa(request));
    }

    @Test
    @DisplayName("Deve rejeitar cadastro com CNPJ duplicado")
    void deveRejeitarCnpjDuplicado() {
        CriarEmpresaRequest request1 = new CriarEmpresaRequest(
                "Empresa Alfa",
                "12.345.678/0001-95",
                "alfa@empresa.com",
                "senha123"
        );
        service.cadastrarEmpresa(request1);

        CriarEmpresaRequest request2 = new CriarEmpresaRequest(
                "Empresa Beta",
                "12345678000195",
                "beta@empresa.com",
                "senha456"
        );

        assertThrows(CadastroDuplicadoException.class, () -> service.cadastrarEmpresa(request2));
    }

    @Test
    @DisplayName("Deve rejeitar cadastro com e-mail corporativo duplicado")
    void deveRejeitarEmailDuplicado() {
        CriarEmpresaRequest request1 = new CriarEmpresaRequest(
                "Empresa Alfa",
                "11.111.111/0001-11",
                "mesmo_email@empresa.com",
                "senha123"
        );
        service.cadastrarEmpresa(request1);

        CriarEmpresaRequest request2 = new CriarEmpresaRequest(
                "Empresa Beta",
                "22.222.222/0001-22",
                "mesmo_email@empresa.com",
                "senha456"
        );

        assertThrows(CadastroDuplicadoException.class, () -> service.cadastrarEmpresa(request2));
    }

    @Test
    @DisplayName("Deve buscar empresa cadastrada por código ou ID")
    void deveBuscarPorCodigoEId() {
        CriarEmpresaRequest request = new CriarEmpresaRequest(
                "Seara Alimentos",
                "33.333.333/0001-33",
                "contato@seara.com",
                "senha123"
        );
        EmpresaResponse cadastrada = service.cadastrarEmpresa(request);

        EmpresaResponse porId = service.buscarPorId(cadastrada.id());
        assertEquals(cadastrada.id(), porId.id());

        EmpresaResponse porCodigo = service.buscarPorCodigo(cadastrada.codigoEmpresa());
        assertEquals(cadastrada.codigoEmpresa(), porCodigo.codigoEmpresa());

        assertThrows(ResponseStatusException.class, () -> service.buscarPorId(999L));
        assertThrows(ResponseStatusException.class, () -> service.buscarPorCodigo("INEXIST"));
    }

    @Test
    @DisplayName("Deve cadastrar empresa com razão social e marcar status como PENDENTE_PRIMEIRO_ADMIN")
    void deveCadastrarComRazaoSocialEIndicarPendentePrimeiroAdmin() {
        CriarEmpresaRequest request = new CriarEmpresaRequest(
                "JBS Alimentos",
                "JBS S.A. Participações",
                "44.444.444/0001-44",
                "corporativo@jbs.com.br",
                "senhaCorp123",
                1
        );

        EmpresaResponse response = service.cadastrarEmpresa(request);

        assertNotNull(response);
        assertEquals("JBS Alimentos", response.nomeEmpresa());
        assertEquals("JBS S.A. Participações", response.razaoSocial());
        assertEquals("44444444000144", response.cnpj());
        assertEquals("PENDENTE_PRIMEIRO_ADMIN", response.status());
        assertTrue(response.requerPrimeiroAdmin());
        assertEquals("CADASTRO_PRIMEIRO_ADMIN", response.proximoPasso());
        assertNotNull(response.mensagem());
    }

    @Test
    @DisplayName("Deve autenticar empresa sem administrador informando que requer primeiro admin")
    void deveAutenticarEmpresaSemAdminIndicandoPendentePrimeiroAdmin() {
        CriarEmpresaRequest request = new CriarEmpresaRequest(
                "Swift Carnes",
                "55.555.555/0001-55",
                "contato@swift.com.br",
                "senha123"
        );
        EmpresaResponse cadastrada = service.cadastrarEmpresa(request);

        var loginResponse = service.autenticarEmpresa(new com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaRequest(
                "55.555.555/0001-55",
                null
        ));

        assertNotNull(loginResponse);
        assertEquals("PENDENTE_PRIMEIRO_ADMIN", loginResponse.status());
        assertTrue(loginResponse.requerPrimeiroAdmin());
        assertEquals("CADASTRO_PRIMEIRO_ADMIN", loginResponse.proximoPasso());
        assertEquals(cadastrada.id(), loginResponse.empresa().id());
    }

    @Test
    @DisplayName("Deve buscar empresa pelo CNPJ com pontuação ou dígitos limpos")
    void deveBuscarPorCnpj() {
        CriarEmpresaRequest request = new CriarEmpresaRequest(
                "Maturatta",
                "66.666.666/0001-66",
                "contato@maturatta.com.br",
                "senha123"
        );
        service.cadastrarEmpresa(request);

        EmpresaResponse porCnpjComPontos = service.buscarPorCnpj("66.666.666/0001-66");
        assertEquals("66666666000166", porCnpjComPontos.cnpj());
        assertEquals("Maturatta", porCnpjComPontos.nomeEmpresa());

        EmpresaResponse porCnpjLimpo = service.buscarPorCnpj("66666666000166");
        assertEquals("66666666000166", porCnpjLimpo.cnpj());

        assertThrows(ResponseStatusException.class, () -> service.buscarPorCnpj("00000000000000"));
    }

    @Test
    @DisplayName("Deve falhar ao cadastrar se faltar nome e razao social")
    void deveFalharSeFaltarNomeERazaoSocial() {
        CriarEmpresaRequest request = new CriarEmpresaRequest("", "", "11111111111114", "teste@b.com", "senha", 1);
        assertThrows(CadastroInvalidoException.class, () -> service.cadastrarEmpresa(request));
    }

    @Test
    @DisplayName("Deve usar razao social se nome estiver em branco")
    void deveUsarRazaoSocialSeNomeEmBranco() {
        CriarEmpresaRequest request = new CriarEmpresaRequest("", "Razao Corp", "11111111111115", "teste5@b.com", "senha", 1);
        EmpresaResponse response = service.cadastrarEmpresa(request);
        assertEquals("Razao Corp", response.nomeEmpresa());
    }

    @Test
    @DisplayName("Deve falhar se codigo for nulo na busca")
    void deveFalharBuscaCodigoNulo() {
        assertThrows(CadastroInvalidoException.class, () -> service.buscarPorCodigo(""));
        assertThrows(CadastroInvalidoException.class, () -> service.buscarPorCodigo(null));
    }

    @Test
    @DisplayName("Deve falhar se cnpj for nulo na busca")
    void deveFalharBuscaCnpjNulo() {
        assertThrows(CadastroInvalidoException.class, () -> service.buscarPorCnpj(null));
        assertThrows(CadastroInvalidoException.class, () -> service.buscarPorCnpj(""));
    }

    @Test
    @DisplayName("Deve listar todas as empresas cadastradas")
    void deveListarEmpresas() {
        CriarEmpresaRequest request = new CriarEmpresaRequest("Empresa 1", "12121212121212", "emp1@a.com", "senha");
        service.cadastrarEmpresa(request);
        assertTrue(service.listarEmpresas().size() > 0);
    }

    @Test
    @DisplayName("Deve autenticar empresa via ID, email ou código (identificador genérico)")
    void deveLocalizarPorIdentificador() {
        CriarEmpresaRequest request = new CriarEmpresaRequest("Empresa Ide", "99887766554433", "ident@empresa.com", "senha123");
        EmpresaResponse cadastrada = service.cadastrarEmpresa(request);

        // Por e-mail
        var login1 = service.autenticarEmpresa(new com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaRequest("ident@empresa.com", null));
        assertEquals(cadastrada.id(), login1.empresa().id());

        // Por código
        var login2 = service.autenticarEmpresa(new com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaRequest(cadastrada.codigoEmpresa(), null));
        assertEquals(cadastrada.id(), login2.empresa().id());

        // Por ID numérico (ex. "1")
        var login3 = service.autenticarEmpresa(new com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaRequest(cadastrada.id().toString(), null));
        assertEquals(cadastrada.id(), login3.empresa().id());
    }

    @Test
    @DisplayName("Deve falhar ao autenticar com identificador nulo ou nao encontrado")
    void deveFalharIdentificadorAusente() {
        assertThrows(ResponseStatusException.class, () -> service.autenticarEmpresa(
                new com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaRequest(null, null)
        ));
        assertThrows(ResponseStatusException.class, () -> service.autenticarEmpresa(
                new com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaRequest("identificador-invalido-inexistente", null)
        ));
    }
}
