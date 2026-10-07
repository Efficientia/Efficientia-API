package com.example.efficientia.empresa.service;

import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
import com.example.efficientia.cadastrobase.persistence.EnderecoEntity;
import com.example.efficientia.cadastrobase.persistence.EnderecoRepository;
import com.example.efficientia.empresa.api.EmpresaContracts.AtualizarDadosEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.CriarEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.EmpresaResponse;
import com.example.efficientia.empresa.api.EmpresaContracts.EnderecoDto;
import com.example.efficientia.empresa.api.EmpresaContracts.UploadLogoResponse;
import com.example.efficientia.empresa.persistence.InMemoryEmpresaRepository;
import com.example.efficientia.empresa.persistence.InMemoryEmpresaAdminRepository;
import com.example.efficientia.documento.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    @DisplayName("Deve cadastrar empresa com dados complementares e endereco completo")
    void deveCadastrarEmpresaComDadosComplementaresEEndereco() {
        EnderecoDto endereco = new EnderecoDto(null, "79002190", "Av. Afonso Pena", "2450", "Campo Grande", "MS");
        CriarEmpresaRequest request = new CriarEmpresaRequest(
                "Efficientia Transportes",
                "Efficientia",
                "Efficientia Transportes Ltda.",
                "12345678000190",
                "operacao@efficientia.com.br",
                "+55 (67) 99999-2048",
                "senhaForte123",
                null,
                "79002190",
                "Av. Afonso Pena",
                "2450",
                "Campo Grande",
                "MS",
                "https://cdn.efficientia.com/logo.svg",
                endereco
        );

        EmpresaResponse response = service.cadastrarEmpresa(request);

        assertNotNull(response.id());
        assertEquals("Efficientia Transportes", response.nomeEmpresa());
        assertEquals("Efficientia", response.nomeFantasia());
        assertEquals("Efficientia Transportes Ltda.", response.razaoSocial());
        assertEquals("+55 (67) 99999-2048", response.telefone());
        assertEquals("79002190", response.cep());
        assertEquals("Av. Afonso Pena", response.logradouro());
        assertEquals("2450", response.numero());
        assertEquals("Campo Grande", response.cidade());
        assertEquals("MS", response.uf());
        assertNotNull(response.enderecoId());
        assertNotNull(response.endereco());
        assertEquals("https://cdn.efficientia.com/logo.svg", response.logoUrl());
    }

    @Test
    @DisplayName("Deve atualizar dados cadastrais complementares por ID (Etapa 1 de 3)")
    void deveAtualizarDadosComplementaresPorId() {
        CriarEmpresaRequest inicial = new CriarEmpresaRequest("Empresa Beta", "88888888000188", "beta@empresa.com", "senha123");
        EmpresaResponse cadastrada = service.cadastrarEmpresa(inicial);
        assertEquals(1, cadastrada.etapaCadastro());

        AtualizarDadosEmpresaRequest update = new AtualizarDadosEmpresaRequest(
                "Beta Log",
                "Beta Logistica S.A.",
                "88888888000188",
                "contato@betalog.com.br",
                "+55 11 98888-7777",
                "01001000",
                "Praça da Sé",
                "100",
                "São Paulo",
                "SP",
                "/api/v1/empresas/" + cadastrada.id() + "/logo/conteudo",
                null
        );

        EmpresaResponse atualizada = service.atualizarDadosComplementares(cadastrada.id(), update);

        assertEquals("Beta Log", atualizada.nomeFantasia());
        assertEquals("Beta Logistica S.A.", atualizada.razaoSocial());
        assertEquals("contato@betalog.com.br", atualizada.emailCorporativo());
        assertEquals("+55 11 98888-7777", atualizada.telefone());
        assertEquals("01001000", atualizada.cep());
        assertEquals("Praça da Sé", atualizada.logradouro());
        assertEquals("100", atualizada.numero());
        assertEquals("São Paulo", atualizada.cidade());
        assertEquals("SP", atualizada.uf());
        assertEquals(2, atualizada.etapaCadastro());
    }

    @Test
    @DisplayName("Deve atualizar dados cadastrais complementares por Código corporativo")
    void deveAtualizarDadosComplementaresPorCodigo() {
        CriarEmpresaRequest inicial = new CriarEmpresaRequest("Empresa Gama", "77777777000177", "gama@empresa.com", "senha123");
        EmpresaResponse cadastrada = service.cadastrarEmpresa(inicial);

        AtualizarDadosEmpresaRequest update = new AtualizarDadosEmpresaRequest(
                "Gama Express",
                "Gama Express Transportes",
                null,
                null,
                "+55 67 91234-5678",
                "79000000",
                "Rua 14 de Julho",
                "500",
                "Campo Grande",
                "MS",
                null,
                null
        );

        EmpresaResponse atualizada = service.atualizarDadosComplementaresPorCodigo(cadastrada.codigoEmpresa(), update);
        assertEquals("Gama Express", atualizada.nomeFantasia());
        assertEquals("+55 67 91234-5678", atualizada.telefone());
        assertEquals(2, atualizada.etapaCadastro());
    }

    @Test
    @DisplayName("Deve fazer upload de logotipo PNG ou SVG válido e recuperar bytes")
    void deveFazerUploadEObterLogotipo() {
        CriarEmpresaRequest inicial = new CriarEmpresaRequest("Empresa Logo", "66666666000166", "logo@empresa.com", "senha123");
        EmpresaResponse cadastrada = service.cadastrarEmpresa(inicial);

        byte[] conteudoPng = new byte[]{1, 2, 3, 4, 5};
        MockMultipartFile file = new MockMultipartFile("arquivo", "logo.png", "image/png", conteudoPng);

        UploadLogoResponse uploadResp = service.atualizarLogo(cadastrada.id(), file);
        assertNotNull(uploadResp.logoUrl());
        assertEquals("image/png", uploadResp.mimeType());
        assertEquals(5L, uploadResp.tamanhoBytes());

        byte[] obtido = service.obterConteudoLogo(cadastrada.id());
        assertArrayEquals(conteudoPng, obtido);
        assertEquals("image/png", service.obterMimeTypeLogo(cadastrada.id()));
    }

    @Test
    @DisplayName("Deve rejeitar logotipo com formato invalido ou tamanho acima de 5 MB")
    void deveRejeitarLogoInvalida() {
        CriarEmpresaRequest inicial = new CriarEmpresaRequest("Empresa Invalida", "55555555000155", "inv@empresa.com", "senha123");
        EmpresaResponse cadastrada = service.cadastrarEmpresa(inicial);

        // Formato não aceito
        MockMultipartFile arquivoTexto = new MockMultipartFile("arquivo", "logo.txt", "text/plain", new byte[]{1, 2, 3});
        assertThrows(CadastroInvalidoException.class, () -> service.atualizarLogo(cadastrada.id(), arquivoTexto));

        // Tamanho > 5MB
        byte[] grande = new byte[6 * 1024 * 1024];
        MockMultipartFile arquivoGrande = new MockMultipartFile("arquivo", "logo.png", "image/png", grande);
        assertThrows(CadastroInvalidoException.class, () -> service.atualizarLogo(cadastrada.id(), arquivoGrande));
    }

    @Test
    @DisplayName("Deve rejeitar atualizacao com CNPJ ou email duplicado de outra empresa")
    void deveRejeitarDuplicidadeNaAtualizacao() {
        CriarEmpresaRequest empA = new CriarEmpresaRequest("Empresa A", "11111111000111", "a@empresa.com", "senha123");
        CriarEmpresaRequest empB = new CriarEmpresaRequest("Empresa B", "22222222000122", "b@empresa.com", "senha123");
        EmpresaResponse cadA = service.cadastrarEmpresa(empA);
        EmpresaResponse cadB = service.cadastrarEmpresa(empB);

        AtualizarDadosEmpresaRequest duplicarCnpj = new AtualizarDadosEmpresaRequest(null, null, cadA.cnpj(), null, null, null, null, null, null, null, null, null);
        assertThrows(CadastroDuplicadoException.class, () -> service.atualizarDadosComplementares(cadB.id(), duplicarCnpj));

        AtualizarDadosEmpresaRequest duplicarEmail = new AtualizarDadosEmpresaRequest(null, null, null, cadA.emailCorporativo(), null, null, null, null, null, null, null, null);
        assertThrows(CadastroDuplicadoException.class, () -> service.atualizarDadosComplementares(cadB.id(), duplicarEmail));
    }

    @Test
    @DisplayName("Deve repetir a geração do código quando houver colisão e persistir o próximo disponível")
    void deveTentarNovoCodigoQuandoHouverColisao() {
        AtomicInteger verificacoesDeCodigo = new AtomicInteger();
        InMemoryEmpresaRepository repositoryComColisoes = new InMemoryEmpresaRepository() {
            @Override
            public boolean existePorCodigo(String codigo) {
                return verificacoesDeCodigo.incrementAndGet() <= 2;
            }
        };
        AtomicInteger codigosGerados = new AtomicInteger();
        CodigoEmpresaGenerator generatorDeterministico = new CodigoEmpresaGenerator() {
            @Override
            public String gerarCodigo(String nomeEmpresa) {
                return "EMP0000" + codigosGerados.incrementAndGet();
            }
        };
        EmpresaService serviceComColisoes = new EmpresaService(repositoryComColisoes, generatorDeterministico);

        EmpresaResponse response = serviceComColisoes.cadastrarEmpresa(new CriarEmpresaRequest(
                "Empresa Colisao", "12345678000191", "colisao@empresa.com", "senha123"));

        assertEquals("EMP00003", response.codigoEmpresa());
        assertEquals(3, codigosGerados.get());
        assertEquals(3, verificacoesDeCodigo.get());
    }

    @Test
    @DisplayName("Deve persistir e normalizar endereço usando o repositório relacional")
    void devePersistirEnderecoNoRepositorioRelacional() {
        EnderecoRepository enderecoRepository = mock(EnderecoRepository.class);
        EnderecoEntity enderecoPersistido = mock(EnderecoEntity.class);
        when(enderecoPersistido.getId()).thenReturn(4321);
        when(enderecoPersistido.getCep()).thenReturn("12345678");
        when(enderecoPersistido.getLogradouro()).thenReturn("Rua das Flores");
        when(enderecoPersistido.getNumero()).thenReturn("S/N");
        when(enderecoPersistido.getCidade()).thenReturn("Campo Grande");
        when(enderecoPersistido.getEstado()).thenReturn("GO");
        when(enderecoRepository.findById(77)).thenReturn(Optional.empty());
        when(enderecoRepository.save(any(EnderecoEntity.class))).thenReturn(enderecoPersistido);
        EmpresaService serviceComEndereco = new EmpresaService(
                repository,
                new InMemoryEmpresaAdminRepository(),
                codigoGenerator,
                null,
                enderecoRepository,
                null
        );

        EmpresaResponse response = serviceComEndereco.cadastrarEmpresa(new CriarEmpresaRequest(
                "Empresa com endereco", "Empresa Fantasia", "Razao Social Ltda", "12345678000192",
                "endereco@empresa.com", null, "senha123", 77, null, null, null, null, null, null,
                new EnderecoDto(null, "12.345-678", " Rua das Flores ", null, " Campo Grande ", "goias")
        ));

        var captor = org.mockito.ArgumentCaptor.forClass(EnderecoEntity.class);
        verify(enderecoRepository).save(captor.capture());
        assertEquals("12345678", captor.getValue().getCep());
        assertEquals("Rua das Flores", captor.getValue().getLogradouro());
        assertEquals("S/N", captor.getValue().getNumero());
        assertEquals("Campo Grande", captor.getValue().getCidade());
        assertEquals("GO", captor.getValue().getEstado());
        assertEquals(4321, response.enderecoId());
        assertEquals("GO", response.uf());
    }

    @Test
    @DisplayName("Deve reutilizar o endereço relacional existente durante o cadastro")
    void deveAtualizarEnderecoRelacionalExistente() {
        EnderecoRepository enderecoRepository = mock(EnderecoRepository.class);
        EnderecoEntity enderecoExistente = mock(EnderecoEntity.class);
        when(enderecoExistente.getId()).thenReturn(79);
        when(enderecoExistente.getCep()).thenReturn("01001000");
        when(enderecoExistente.getLogradouro()).thenReturn("Rua Nova");
        when(enderecoExistente.getNumero()).thenReturn("15");
        when(enderecoExistente.getCidade()).thenReturn("São Paulo");
        when(enderecoExistente.getEstado()).thenReturn("SP");
        when(enderecoRepository.findById(79)).thenReturn(Optional.of(enderecoExistente));
        when(enderecoRepository.save(enderecoExistente)).thenReturn(enderecoExistente);
        EmpresaService serviceComEndereco = new EmpresaService(
                repository,
                new InMemoryEmpresaAdminRepository(),
                codigoGenerator,
                null,
                enderecoRepository,
                null
        );

        EmpresaResponse response = serviceComEndereco.cadastrarEmpresa(new CriarEmpresaRequest(
                "Empresa endereco relacional", "Empresa Relacional", "Empresa Relacional Ltda",
                "12345678000199", "endereco-relacional@empresa.com", null, "senha123", 79,
                "01.001-000", " Rua Nova ", " 15 ", " São Paulo ", "sp", null, null));

        verify(enderecoExistente).setCep("01001000");
        verify(enderecoExistente).setLogradouro("Rua Nova");
        verify(enderecoExistente).setNumero("15");
        verify(enderecoExistente).setCidade("São Paulo");
        verify(enderecoExistente).setEstado("SP");
        verify(enderecoRepository).save(enderecoExistente);
        assertEquals(79, response.enderecoId());
        assertEquals("01001000", response.cep());
    }

    @Test
    @DisplayName("Deve carregar endereço existente pelo repositório quando não estiver no cache")
    void deveBuscarEnderecoNoRepositorioRelacional() {
        EnderecoRepository enderecoRepository = mock(EnderecoRepository.class);
        EnderecoEntity enderecoExistente = mock(EnderecoEntity.class);
        when(enderecoExistente.getId()).thenReturn(88);
        when(enderecoExistente.getCep()).thenReturn("79000000");
        when(enderecoExistente.getLogradouro()).thenReturn("Rua A");
        when(enderecoExistente.getNumero()).thenReturn("10");
        when(enderecoExistente.getCidade()).thenReturn("Campo Grande");
        when(enderecoExistente.getEstado()).thenReturn("MS");
        when(enderecoRepository.findById(88)).thenReturn(Optional.of(enderecoExistente));
        EmpresaService serviceComEndereco = new EmpresaService(
                repository,
                new InMemoryEmpresaAdminRepository(),
                codigoGenerator,
                null,
                enderecoRepository,
                null
        );

        EmpresaResponse response = serviceComEndereco.cadastrarEmpresa(new CriarEmpresaRequest(
                "Empresa endereco existente", "Empresa endereco existente", "12345678000193",
                "endereco-existente@empresa.com", "senha123", 88));

        assertEquals(88, response.enderecoId());
        assertEquals("79000000", response.cep());
        assertEquals("Rua A", response.logradouro());
        assertEquals("MS", response.estado());
        verify(enderecoRepository).findById(88);
    }

    @Test
    @DisplayName("Deve preservar dados quando atualização é nula ou contém valores em branco")
    void devePreservarDadosEmAtualizacaoNulaOuEmBranco() {
        EmpresaResponse cadastrada = service.cadastrarEmpresa(new CriarEmpresaRequest(
                "Empresa Original", "12345678000194", "original@empresa.com", "senha123"));

        EmpresaResponse semAlteracao = service.atualizarDadosComplementares(cadastrada.id(), null);
        assertEquals("Empresa Original", semAlteracao.nomeEmpresa());
        assertEquals(1, semAlteracao.etapaCadastro());

        AtualizarDadosEmpresaRequest dadosEmBranco = new AtualizarDadosEmpresaRequest(
                " ", " ", cadastrada.cnpj(), cadastrada.emailCorporativo(), " ", " ", " ", " ", " ", " ", " ", null);
        EmpresaResponse atualizada = service.atualizarDadosComplementares(cadastrada.id(), dadosEmBranco);

        assertEquals("Empresa Original", atualizada.nomeFantasia());
        assertEquals(cadastrada.cnpj(), atualizada.cnpj());
        assertEquals(cadastrada.emailCorporativo(), atualizada.emailCorporativo());
        assertEquals(2, atualizada.etapaCadastro());
    }

    @Test
    @DisplayName("Deve validar CNPJ inválido e código ausente durante atualização")
    void deveValidarCnpjECodigoNaAtualizacao() {
        EmpresaResponse cadastrada = service.cadastrarEmpresa(new CriarEmpresaRequest(
                "Empresa Validacao", "12345678000195", "validacao@empresa.com", "senha123"));
        AtualizarDadosEmpresaRequest cnpjInvalido = new AtualizarDadosEmpresaRequest(
                null, null, "123", null, null, null, null, null, null, null, null, null);

        assertThrows(CadastroInvalidoException.class,
                () -> service.atualizarDadosComplementares(cadastrada.id(), cnpjInvalido));
        assertThrows(CadastroInvalidoException.class,
                () -> service.atualizarDadosComplementaresPorCodigo(" ", cnpjInvalido));
        assertThrows(ResponseStatusException.class,
                () -> service.atualizarDadosComplementaresPorCodigo("NAOEXISTE", cnpjInvalido));
        assertThrows(ResponseStatusException.class,
                () -> service.atualizarDadosComplementares(999L, cnpjInvalido));
    }

    @Test
    @DisplayName("Deve completar endereço parcial em memória durante a atualização")
    void deveAtualizarEnderecoParcialEmMemoria() {
        EmpresaResponse inicial = service.cadastrarEmpresa(new CriarEmpresaRequest(
                "Empresa endereco memoria", "Empresa endereco memoria", "12345678000196",
                "endereco-memoria@empresa.com", "senha123", 123));
        AtualizarDadosEmpresaRequest update = new AtualizarDadosEmpresaRequest(
                null, null, null, null, null, "79.001-000", " Avenida Central ", null,
                " Campo Grande ", "ms", null, null);

        EmpresaResponse atualizada = service.atualizarDadosComplementares(inicial.id(), update);

        assertEquals(123, atualizada.enderecoId());
        assertEquals("79001000", atualizada.cep());
        assertEquals("Avenida Central", atualizada.logradouro());
        assertEquals("S/N", atualizada.numero());
        assertEquals("Campo Grande", atualizada.cidade());
        assertEquals("MS", atualizada.uf());
    }

    @Test
    @DisplayName("Deve aceitar SVG pelo nome do arquivo e persistir também no serviço de storage")
    void deveAceitarSvgPorExtensaoEPersistirNoStorage() {
        CriarEmpresaRequest inicial = new CriarEmpresaRequest(
                "Empresa SVG", "12345678000197", "svg@empresa.com", "senha123");
        EmpresaResponse cadastrada = service.cadastrarEmpresa(inicial);
        StorageService storageService = mock(StorageService.class);
        EmpresaService serviceComStorage = new EmpresaService(
                repository,
                new InMemoryEmpresaAdminRepository(),
                codigoGenerator,
                null,
                null,
                storageService
        );
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "LOGO.SVG", null, "<svg/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        UploadLogoResponse response = serviceComStorage.atualizarLogo(cadastrada.id(), arquivo);

        assertEquals("image/svg+xml", response.mimeType());
        verify(storageService).salvar(any(UUID.class), eq("logo.svg"), eq("image/svg+xml"), any(InputStream.class));
    }

    @Test
    @DisplayName("Deve rejeitar arquivo de logo vazio, empresa ausente e leitura com falha")
    void deveTratarFalhasNoUploadOuConsultaDaLogo() throws IOException {
        EmpresaResponse cadastrada = service.cadastrarEmpresa(new CriarEmpresaRequest(
                "Empresa logo erros", "12345678000198", "logo-erros@empresa.com", "senha123"));

        assertEquals("image/png", service.obterMimeTypeLogo(cadastrada.id()));
        assertThrows(ResponseStatusException.class, () -> service.obterConteudoLogo(cadastrada.id()));
        assertThrows(ResponseStatusException.class, () -> service.obterConteudoLogo(999L));
        assertThrows(ResponseStatusException.class, () -> service.atualizarLogo(999L, null));
        assertThrows(CadastroInvalidoException.class, () -> service.atualizarLogo(cadastrada.id(), null));
        assertThrows(CadastroInvalidoException.class, () -> service.atualizarLogo(
                cadastrada.id(), new MockMultipartFile("arquivo", "logo.png", "image/png", new byte[0])));

        MultipartFile arquivoComFalha = mock(MultipartFile.class);
        when(arquivoComFalha.isEmpty()).thenReturn(false);
        when(arquivoComFalha.getSize()).thenReturn(1L);
        when(arquivoComFalha.getContentType()).thenReturn("image/png");
        when(arquivoComFalha.getOriginalFilename()).thenReturn("logo.png");
        when(arquivoComFalha.getBytes()).thenThrow(new IOException("falha simulada de leitura"));

        assertThrows(CadastroInvalidoException.class, () -> service.atualizarLogo(cadastrada.id(), arquivoComFalha));
    }
}
