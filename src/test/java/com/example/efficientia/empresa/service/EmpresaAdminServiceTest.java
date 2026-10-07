package com.example.efficientia.empresa.service;

import com.example.efficientia.auth.api.AutenticacaoInvalidaException;
import com.example.efficientia.auth.service.JwtTokenService;
import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.AdminResponse;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.CriarAdminRequest;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.CriarPrimeiroAdminRequest;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.LoginAdminRequest;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.LoginAdminResponse;
import com.example.efficientia.empresa.domain.Empresa;
import com.example.efficientia.empresa.domain.EmpresaAdmin;
import com.example.efficientia.empresa.persistence.EmpresaAdminRepository;
import com.example.efficientia.empresa.persistence.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpresaAdminServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private EmpresaAdminRepository adminRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    private EmpresaAdminService service;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private Empresa empresaMock;

    @BeforeEach
    void setUp() {
        service = new EmpresaAdminService(empresaRepository, adminRepository, jwtTokenService);

        empresaMock = new Empresa(
                1L,
                "FRI12345",
                "Friboi Alimentos",
                "12345678000195",
                "contato@friboi.com.br",
                null,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Deve cadastrar primeiro admin com sucesso e emitir token JWT")
    void deveCadastrarPrimeiroAdminComSucesso() {
        CriarPrimeiroAdminRequest request = new CriarPrimeiroAdminRequest(
                1L,
                "FRI12345",
                null,
                "Carlos Silva",
                "carlos.silva@friboi.com.br",
                "senhaForte123",
                "12345678901",
                "11988887777",
                "Gerente de RH"
        );

        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.contarPorEmpresaId(1L)).thenReturn(0L);
        when(adminRepository.existePorEmail("carlos.silva@friboi.com.br")).thenReturn(false);
        when(adminRepository.existePorCpf("12345678901")).thenReturn(false);
        when(adminRepository.salvar(any(EmpresaAdmin.class))).thenAnswer(invocation -> {
            EmpresaAdmin a = invocation.getArgument(0);
            a.setId(10L);
            return a;
        });
        when(jwtTokenService.gerarTokenAdmin(any(), any())).thenReturn("mocked.admin.jwt.token");

        LoginAdminResponse response = service.cadastrarPrimeiroAdmin(request);

        assertNotNull(response);
        assertEquals("mocked.admin.jwt.token", response.token());
        assertEquals(10L, response.admin().id());
        assertEquals("Carlos Silva", response.admin().nome());
        assertEquals("FRI12345", response.empresa().codigoEmpresa());

        ArgumentCaptor<EmpresaAdmin> captor = ArgumentCaptor.forClass(EmpresaAdmin.class);
        verify(adminRepository).salvar(captor.capture());
        EmpresaAdmin salvo = captor.getValue();
        assertTrue(passwordEncoder.matches("senhaForte123", salvo.getSenhaHash()));
        assertEquals(1L, salvo.getEmpresaId());
    }

    @Test
    @DisplayName("Deve bloquear cadastro de primeiro admin se empresa já tiver administradores cadastrados")
    void deveBloquearPrimeiroAdminSeJaExistirAdministrador() {
        CriarPrimeiroAdminRequest request = new CriarPrimeiroAdminRequest(
                1L,
                null,
                null,
                "Outro Admin",
                "outro@friboi.com.br",
                "senha123",
                null,
                null,
                null
        );

        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.contarPorEmpresaId(1L)).thenReturn(1L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.cadastrarPrimeiroAdmin(request)
        );

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("já possui administradores cadastrados"));
    }

    @Test
    @DisplayName("Deve rejeitar primeiro admin com e-mail já existente")
    void deveRejeitarPrimeiroAdminComEmailDuplicado() {
        CriarPrimeiroAdminRequest request = new CriarPrimeiroAdminRequest(
                1L,
                null,
                null,
                "Admin Duplicado",
                "existente@friboi.com.br",
                "senha123",
                null,
                null,
                null
        );

        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.contarPorEmpresaId(1L)).thenReturn(0L);
        when(adminRepository.existePorEmail("existente@friboi.com.br")).thenReturn(true);

        assertThrows(
                CadastroDuplicadoException.class,
                () -> service.cadastrarPrimeiroAdmin(request)
        );
    }

    @Test
    @DisplayName("Deve aderir novo administrador quando solicitado por admin autenticado da mesma empresa")
    void deveAderirNovoAdminPorAdminDaMesmaEmpresa() {
        EmpresaAdmin adminLogado = new EmpresaAdmin(
                10L, 1L, "FRI12345", "12345678000195", "Admin Principal",
                "principal@friboi.com.br", "11111111111", null, "Diretor",
                "hash", true, Instant.now(), Instant.now()
        );

        CriarAdminRequest request = new CriarAdminRequest(
                "Novo Co-Admin",
                "coadmin@friboi.com.br",
                "senhaAdmin123",
                "22222222222",
                "11977776666",
                "Analista de RH"
        );

        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.buscarPorId(10L)).thenReturn(Optional.of(adminLogado));
        when(adminRepository.existePorEmail("coadmin@friboi.com.br")).thenReturn(false);
        when(adminRepository.existePorCpf("22222222222")).thenReturn(false);
        when(adminRepository.salvar(any())).thenAnswer(inv -> {
            EmpresaAdmin a = inv.getArgument(0);
            a.setId(20L);
            return a;
        });

        AdminResponse novo = service.aderirNovoAdmin(1L, request, 10L);

        assertNotNull(novo);
        assertEquals(20L, novo.id());
        assertEquals("Novo Co-Admin", novo.nome());
        assertEquals("coadmin@friboi.com.br", novo.email());
        assertEquals("FRI12345", novo.codigoEmpresa());
    }

    @Test
    @DisplayName("Deve rejeitar adesão se admin logado for de outra empresa")
    void deveRejeitarAdesaoDeAdminDeOutraEmpresa() {
        EmpresaAdmin adminDeOutraEmpresa = new EmpresaAdmin(
                99L, 2L, "OUT99999", "99999999000199", "Admin Outra",
                "admin@outra.com", null, null, null,
                "hash", true, Instant.now(), Instant.now()
        );

        CriarAdminRequest request = new CriarAdminRequest(
                "Invasor", "invasor@teste.com", "senha123", null, null, null
        );

        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.buscarPorId(99L)).thenReturn(Optional.of(adminDeOutraEmpresa));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.aderirNovoAdmin(1L, request, 99L)
        );

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("permissão para cadastrar administradores em outra empresa"));
    }

    @Test
    @DisplayName("Deve autenticar admin com sucesso via e-mail e senha")
    void deveAutenticarAdminComSucesso() {
        String hash = passwordEncoder.encode("senhaCorreta123");
        EmpresaAdmin admin = new EmpresaAdmin(
                10L, 1L, "FRI12345", "12345678000195", "Carlos Silva",
                "carlos@friboi.com.br", "12345678901", null, "Administrador",
                hash, true, Instant.now(), Instant.now()
        );

        when(adminRepository.buscarPorEmail("carlos@friboi.com.br")).thenReturn(Optional.of(admin));
        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(jwtTokenService.gerarTokenAdmin(admin, empresaMock)).thenReturn("jwt.token.adm");

        LoginAdminRequest request = new LoginAdminRequest("carlos@friboi.com.br", "senhaCorreta123", "FRI12345");
        LoginAdminResponse response = service.autenticarAdmin(request);

        assertNotNull(response);
        assertEquals("jwt.token.adm", response.token());
        assertEquals("Carlos Silva", response.admin().nome());
        assertEquals("Friboi Alimentos", response.empresa().nomeEmpresa());
    }

    @Test
    @DisplayName("Deve rejeitar autenticação com senha incorreta")
    void deveRejeitarAutenticacaoComSenhaIncorreta() {
        String hash = passwordEncoder.encode("senhaCorreta123");
        EmpresaAdmin admin = new EmpresaAdmin(
                10L, 1L, "FRI12345", "12345678000195", "Carlos Silva",
                "carlos@friboi.com.br", null, null, null,
                hash, true, Instant.now(), Instant.now()
        );

        when(adminRepository.buscarPorEmail("carlos@friboi.com.br")).thenReturn(Optional.of(admin));

        LoginAdminRequest request = new LoginAdminRequest("carlos@friboi.com.br", "senhaErrada", null);

        assertThrows(
                AutenticacaoInvalidaException.class,
                () -> service.autenticarAdmin(request)
        );
    }

    @Test
    @DisplayName("Deve rejeitar autenticação se admin estiver inativo")
    void deveRejeitarAutenticacaoDeAdminInativo() {
        String hash = passwordEncoder.encode("senhaCorreta123");
        EmpresaAdmin admin = new EmpresaAdmin(
                10L, 1L, "FRI12345", "12345678000195", "Carlos Silva",
                "carlos@friboi.com.br", null, null, null,
                hash, false, Instant.now(), Instant.now() // Inativo
        );

        when(adminRepository.buscarPorEmail("carlos@friboi.com.br")).thenReturn(Optional.of(admin));

        LoginAdminRequest request = new LoginAdminRequest("carlos@friboi.com.br", "senhaCorreta123", null);

        assertThrows(
                AutenticacaoInvalidaException.class,
                () -> service.autenticarAdmin(request)
        );
    }

    @Test
    @DisplayName("Deve listar administradores da empresa para admin autorizado")
    void deveListarAdminsDaEmpresa() {
        EmpresaAdmin adminLogado = new EmpresaAdmin(
                10L, 1L, "FRI12345", "12345678000195", "Carlos Silva",
                "carlos@friboi.com.br", null, null, null,
                "hash", true, Instant.now(), Instant.now()
        );

        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.buscarPorId(10L)).thenReturn(Optional.of(adminLogado));
        when(adminRepository.listarPorEmpresaId(1L)).thenReturn(List.of(adminLogado));

        List<AdminResponse> admins = service.listarAdminsPorEmpresa(1L, 10L);

        assertEquals(1, admins.size());
        assertEquals("Carlos Silva", admins.get(0).nome());
    }

    @Test
    @DisplayName("Deve cadastrar funcionário com sucesso por admin da mesma empresa")
    void deveCadastrarFuncionarioComSucesso() {
        EmpresaAdmin adminLogado = new EmpresaAdmin(
                10L, 1L, "FRI12345", "12345678000195", "Carlos Silva",
                "carlos@friboi.com.br", null, null, null,
                "hash", true, Instant.now(), Instant.now()
        );

        var request = new com.example.efficientia.empresa.api.EmpresaAdminContracts.CriarFuncionarioEmpresaRequest(
                com.example.efficientia.cadastrobase.domain.TipoUsuario.motorista,
                "11122233344",
                "José Motorista",
                null,
                "jose@friboi.com.br",
                "11988887777",
                "senhaMot123",
                "Motorista Carreteiro",
                "12345678900",
                "e",
                null
        );

        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.buscarPorId(10L)).thenReturn(Optional.of(adminLogado));

        var response = service.cadastrarFuncionario(1L, request, 10L);

        assertNotNull(response);
        assertEquals("José Motorista", response.nome());
        assertEquals("11122233344", response.cpf());
        assertEquals(com.example.efficientia.cadastrobase.domain.TipoUsuario.motorista, response.tipo());
        assertEquals("FRI12345", response.codigoEmpresa());
    }

    @Test
    @DisplayName("Deve bloquear cadastro de funcionário se admin logado pertencer a outra empresa")
    void deveBloquearCadastroDeFuncionarioSeAdminForDeOutraEmpresa() {
        EmpresaAdmin adminOutraEmpresa = new EmpresaAdmin(
                20L, 2L, "OUT99999", "99999999000199", "Admin Outro",
                "outro@empresa.com", null, null, null,
                "hash", true, Instant.now(), Instant.now()
        );

        var request = new com.example.efficientia.empresa.api.EmpresaAdminContracts.CriarFuncionarioEmpresaRequest(
                com.example.efficientia.cadastrobase.domain.TipoUsuario.motorista,
                "11122233344",
                "José Motorista",
                null,
                "jose@friboi.com.br",
                "11988887777",
                "senhaMot123",
                null,
                null,
                null,
                null
        );

        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.buscarPorId(20L)).thenReturn(Optional.of(adminOutraEmpresa));

        assertThrows(ResponseStatusException.class, () -> service.cadastrarFuncionario(1L, request, 20L));
    }

    @Test
    @DisplayName("Deve autenticar admin com sucesso informando CNPJ corporativo")
    void deveAutenticarAdminComCnpj() {
        EmpresaAdmin admin = new EmpresaAdmin(
                10L, 1L, "FRI12345", "12345678000195", "Carlos Silva",
                "carlos@friboi.com.br", "12345678901", "11988887777", "Gerente",
                passwordEncoder.encode("senha123"), true, Instant.now(), Instant.now()
        );

        when(adminRepository.buscarPorEmail("carlos@friboi.com.br")).thenReturn(Optional.of(admin));
        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(jwtTokenService.gerarTokenAdmin(admin, empresaMock)).thenReturn("mocked.jwt.token");

        LoginAdminRequest request = new LoginAdminRequest("carlos@friboi.com.br", "senha123", null, "12.345.678/0001-95");
        LoginAdminResponse response = service.autenticarAdmin(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.token());
        assertEquals("Carlos Silva", response.admin().nome());
    }

    @Test
    @DisplayName("Deve autenticar admin com sucesso via CPF")
    void deveAutenticarAdminComCpf() {
        String hash = passwordEncoder.encode("senhaCorreta123");
        EmpresaAdmin admin = new EmpresaAdmin(
                10L, 1L, "FRI12345", "12345678000195", "Carlos Silva",
                "carlos@friboi.com.br", "12345678901", null, "Administrador",
                hash, true, Instant.now(), Instant.now()
        );

        when(adminRepository.buscarPorEmail("123.456.789-01")).thenReturn(Optional.empty());
        when(adminRepository.buscarPorCpf("12345678901")).thenReturn(Optional.of(admin));
        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(jwtTokenService.gerarTokenAdmin(admin, empresaMock)).thenReturn("jwt.token.adm");

        LoginAdminRequest request = new LoginAdminRequest("123.456.789-01", "senhaCorreta123", null);
        LoginAdminResponse response = service.autenticarAdmin(request);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Deve falhar ao autenticar admin se código da empresa divergir")
    void deveFalharAutenticacaoAdminCodigoEmpresaDivergente() {
        EmpresaAdmin admin = new EmpresaAdmin(
                10L, 1L, "FRI12345", "12345678000195", "Carlos Silva",
                "carlos@friboi.com.br", "12345678901", null, "Administrador",
                "hash", true, Instant.now(), Instant.now()
        );

        when(adminRepository.buscarPorEmail("carlos@friboi.com.br")).thenReturn(Optional.of(admin));

        LoginAdminRequest request = new LoginAdminRequest("carlos@friboi.com.br", "senhaCorreta123", "COD-ERRADO");

        assertThrows(AutenticacaoInvalidaException.class, () -> service.autenticarAdmin(request));
    }

    @Test
    @DisplayName("Deve falhar ao autenticar admin se CNPJ divergir")
    void deveFalharAutenticacaoAdminCnpjDivergente() {
        EmpresaAdmin admin = new EmpresaAdmin(
                10L, 1L, "FRI12345", "12345678000195", "Carlos Silva",
                "carlos@friboi.com.br", "12345678901", null, "Administrador",
                "hash", true, Instant.now(), Instant.now()
        );

        when(adminRepository.buscarPorEmail("carlos@friboi.com.br")).thenReturn(Optional.of(admin));

        LoginAdminRequest request = new LoginAdminRequest("carlos@friboi.com.br", "senhaCorreta123", null, "99.999.999/0001-99");

        assertThrows(AutenticacaoInvalidaException.class, () -> service.autenticarAdmin(request));
    }

    @Test
    @DisplayName("Deve falhar ao autenticar se admin não for encontrado por email nem cpf")
    void deveFalharAutenticacaoNaoEncontrado() {
        when(adminRepository.buscarPorEmail("inexistente@teste.com")).thenReturn(Optional.empty());

        LoginAdminRequest request = new LoginAdminRequest("inexistente@teste.com", "senhaCorreta123", null);

        assertThrows(AutenticacaoInvalidaException.class, () -> service.autenticarAdmin(request));
    }

    @Test
    @DisplayName("Deve buscar empresa por código ou cnpj no cadastrarPrimeiroAdmin")
    void deveBuscarEmpresaPorCodigoOuCnpj() {
        when(empresaRepository.buscarPorCodigo("FRI12345")).thenReturn(Optional.of(empresaMock));
        when(adminRepository.contarPorEmpresaId(1L)).thenReturn(0L);
        when(adminRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        CriarPrimeiroAdminRequest reqCod = new CriarPrimeiroAdminRequest(
                null, "FRI12345", null, "Nome", "teste@teste.com", "senha", "12345678901", null, null
        );
        assertNotNull(service.cadastrarPrimeiroAdmin(reqCod));

        when(empresaRepository.buscarPorCnpj("12345678000195")).thenReturn(Optional.of(empresaMock));
        CriarPrimeiroAdminRequest reqCnpj = new CriarPrimeiroAdminRequest(
                null, null, "12.345.678/0001-95", "Nome", "teste2@teste.com", "senha", "12345678902", null, null
        );
        assertNotNull(service.cadastrarPrimeiroAdmin(reqCnpj));
    }

    @Test
    @DisplayName("Deve falhar se nao encontrar empresa por id, codigo ou cnpj")
    void deveFalharLocalizarEmpresaNaoEncontrada() {
        assertThrows(CadastroInvalidoException.class, () -> {
            service.cadastrarPrimeiroAdmin(new CriarPrimeiroAdminRequest(null, null, null, "A", "b@b.com", "1", "12345678901", null, null));
        });
        
        when(empresaRepository.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> {
            service.cadastrarPrimeiroAdmin(new CriarPrimeiroAdminRequest(99L, null, null, "A", "b@b.com", "1", "12345678901", null, null));
        });
    }

    @Test
    @DisplayName("Deve usar o UsuarioRepository quando estiver presente - Sincronizacao")
    void deveUsarUsuarioRepositoryParaSincronizacaoECadastros() {
        com.example.efficientia.cadastrobase.persistence.UsuarioRepository userRepo = org.mockito.Mockito.mock(com.example.efficientia.cadastrobase.persistence.UsuarioRepository.class);
        EmpresaAdminService fullService = new EmpresaAdminService(empresaRepository, adminRepository, jwtTokenService, userRepo);
        
        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.contarPorEmpresaId(1L)).thenReturn(0L);
        when(adminRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        // Teste 1: Sincronizar admin
        CriarPrimeiroAdminRequest req = new CriarPrimeiroAdminRequest(1L, null, null, "Nome", "a@a.com", "senha", "11122233344", null, null);
        fullService.cadastrarPrimeiroAdmin(req);
        verify(userRepo).save(any());

        // Teste 2: Cadastrar funcionario duplicado CPF e Email
        EmpresaAdmin adminLogado = new EmpresaAdmin(10L, 1L, "FRI", "0", "Admin", "a@a.com", null, null, null, "hash", true, Instant.now(), Instant.now());
        when(adminRepository.buscarPorId(10L)).thenReturn(Optional.of(adminLogado));
        
        when(userRepo.existsByCpf(any())).thenReturn(true);
        var reqFuncCpf = new com.example.efficientia.empresa.api.EmpresaAdminContracts.CriarFuncionarioEmpresaRequest(
                com.example.efficientia.cadastrobase.domain.TipoUsuario.motorista, "11122233344", "Func", null, "b@b.com", null, "senha", null, null, null, null);
        assertThrows(CadastroDuplicadoException.class, () -> fullService.cadastrarFuncionario(1L, reqFuncCpf, 10L));
        
        when(userRepo.existsByCpf(any())).thenReturn(false);
        when(userRepo.existsByEmail(any())).thenReturn(true);
        var reqFuncEmail = new com.example.efficientia.empresa.api.EmpresaAdminContracts.CriarFuncionarioEmpresaRequest(
                com.example.efficientia.cadastrobase.domain.TipoUsuario.motorista, "11122233344", "Func", null, "b@b.com", null, "senha", null, null, null, null);
        assertThrows(CadastroDuplicadoException.class, () -> fullService.cadastrarFuncionario(1L, reqFuncEmail, 10L));

        // Teste 3: Listar funcionarios
        when(userRepo.findAll()).thenReturn(List.of());
        assertEquals(0, fullService.listarFuncionariosPorEmpresa(1L, 10L).size());
    }

    @Test
    @DisplayName("Deve normalizar os dados do primeiro administrador e aplicar cargo padrão")
    void deveNormalizarDadosDoPrimeiroAdminEAplicarCargoPadrao() {
        when(empresaRepository.buscarPorCodigo("FRI12345")).thenReturn(Optional.of(empresaMock));
        when(adminRepository.contarPorEmpresaId(1L)).thenReturn(0L, 1L);
        when(adminRepository.salvar(any(EmpresaAdmin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenService.gerarTokenAdmin(any(EmpresaAdmin.class), any(Empresa.class))).thenReturn("jwt.primeiro.admin");

        CriarPrimeiroAdminRequest request = new CriarPrimeiroAdminRequest(
                null, " FRI12345 ", null, "  Ana Souza  ", " ANA@EXEMPLO.COM ", "senhaSegura",
                null, "  ", "   "
        );

        LoginAdminResponse response = service.cadastrarPrimeiroAdmin(request);

        ArgumentCaptor<EmpresaAdmin> captor = ArgumentCaptor.forClass(EmpresaAdmin.class);
        verify(adminRepository).salvar(captor.capture());
        EmpresaAdmin salvo = captor.getValue();
        assertEquals("Ana Souza", salvo.getNome());
        assertEquals("ana@exemplo.com", salvo.getEmail());
        assertTrue(salvo.getCpf() == null);
        assertTrue(salvo.getTelefone() == null);
        assertEquals("Administrador Geral", salvo.getCargo());
        assertTrue(passwordEncoder.matches("senhaSegura", salvo.getSenhaHash()));
        assertEquals("ATIVO", response.empresa().status());
        assertFalse(response.empresa().requerPrimeiroAdmin());
    }

    @Test
    @DisplayName("Deve rejeitar CPF inválido e CPF duplicado no cadastro do primeiro administrador")
    void deveRejeitarCpfInvalidoOuDuplicadoNoPrimeiroAdmin() {
        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.contarPorEmpresaId(1L)).thenReturn(0L);

        CriarPrimeiroAdminRequest cpfInvalido = new CriarPrimeiroAdminRequest(
                1L, null, null, "Ana", "ana@exemplo.com", "senhaSegura", "123", null, null
        );
        assertThrows(CadastroInvalidoException.class, () -> service.cadastrarPrimeiroAdmin(cpfInvalido));

        when(adminRepository.existePorEmail("ana@exemplo.com")).thenReturn(false);
        when(adminRepository.existePorCpf("12345678901")).thenReturn(true);
        CriarPrimeiroAdminRequest cpfDuplicado = new CriarPrimeiroAdminRequest(
                1L, null, null, "Ana", "ana@exemplo.com", "senhaSegura", "123.456.789-01", null, null
        );
        assertThrows(CadastroDuplicadoException.class, () -> service.cadastrarPrimeiroAdmin(cpfDuplicado));
        verify(adminRepository, never()).salvar(any(EmpresaAdmin.class));
    }

    @Test
    @DisplayName("Deve informar empresa ausente ao aderir ou listar administradores")
    void deveRetornarNaoEncontradoQuandoEmpresaNaoExisteNasOperacoesDeAdmin() {
        when(empresaRepository.buscarPorId(99L)).thenReturn(Optional.empty());
        CriarAdminRequest request = new CriarAdminRequest("Ana", "ana@exemplo.com", "senhaSegura", null, null, null);

        ResponseStatusException adesao = assertThrows(
                ResponseStatusException.class, () -> service.aderirNovoAdmin(99L, request, null)
        );
        ResponseStatusException listagem = assertThrows(
                ResponseStatusException.class, () -> service.listarAdminsPorEmpresa(99L, null)
        );
        assertEquals(404, adesao.getStatusCode().value());
        assertEquals(404, listagem.getStatusCode().value());
    }

    @Test
    @DisplayName("Deve rejeitar operações administrativas quando o administrador autenticado não existe")
    void deveRejeitarAdminAutenticadoNaoEncontrado() {
        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.buscarPorId(77L)).thenReturn(Optional.empty());
        CriarAdminRequest request = new CriarAdminRequest("Ana", "ana@exemplo.com", "senhaSegura", null, null, null);

        ResponseStatusException adesao = assertThrows(
                ResponseStatusException.class, () -> service.aderirNovoAdmin(1L, request, 77L)
        );
        ResponseStatusException listagem = assertThrows(
                ResponseStatusException.class, () -> service.listarAdminsPorEmpresa(1L, 77L)
        );
        assertEquals(403, adesao.getStatusCode().value());
        assertEquals(403, listagem.getStatusCode().value());
    }

    @Test
    @DisplayName("Deve normalizar dados e permitir adesão sem id de administrador")
    void deveAderirAdminSemIdLogadoUsandoCnpjNormalizado() {
        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.salvar(any(EmpresaAdmin.class))).thenAnswer(invocation -> {
            EmpresaAdmin admin = invocation.getArgument(0);
            admin.setId(20L);
            return admin;
        });

        CriarAdminRequest request = new CriarAdminRequest(
                "  Bianca Lima ", " BIANCA@EXEMPLO.COM ", "senhaSegura", "123.456.789-01", "  (11) 9999-0000  ", " "
        );
        AdminResponse response = service.aderirNovoAdmin(1L, request, null);

        ArgumentCaptor<EmpresaAdmin> captor = ArgumentCaptor.forClass(EmpresaAdmin.class);
        verify(adminRepository).salvar(captor.capture());
        assertEquals("Bianca Lima", response.nome());
        assertEquals("bianca@exemplo.com", captor.getValue().getEmail());
        assertEquals("12345678901", captor.getValue().getCpf());
        assertEquals("(11) 9999-0000", captor.getValue().getTelefone());
        assertEquals("Administrador", captor.getValue().getCargo());
        verify(adminRepository, never()).buscarPorId(any());
    }

    @Test
    @DisplayName("Deve retornar erro interno quando administrador aponta para empresa inexistente")
    void deveFalharLoginQuandoEmpresaVinculadaNaoExiste() {
        EmpresaAdmin admin = new EmpresaAdmin(
                10L, 404L, "FRI12345", "12345678000195", "Carlos Silva", "carlos@friboi.com.br",
                null, null, null, passwordEncoder.encode("senhaCorreta"), true, Instant.now(), Instant.now()
        );
        when(adminRepository.buscarPorEmail("carlos@friboi.com.br")).thenReturn(Optional.of(admin));
        when(empresaRepository.buscarPorId(404L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.autenticarAdmin(new LoginAdminRequest(" CARLOS@FRIBOI.COM.BR ", "senhaCorreta", " ", " "))
        );

        assertEquals(500, exception.getStatusCode().value());
        verify(jwtTokenService, never()).gerarTokenAdmin(any(EmpresaAdmin.class), any(Empresa.class));
    }

    @Test
    @DisplayName("Deve cadastrar funcionário no repositório e usar cargo padrão quando não informado")
    void deveCadastrarFuncionarioNoUsuarioRepository() {
        com.example.efficientia.cadastrobase.persistence.UsuarioRepository userRepository =
                org.mockito.Mockito.mock(com.example.efficientia.cadastrobase.persistence.UsuarioRepository.class);
        EmpresaAdminService serviceComUsuarios = new EmpresaAdminService(
                empresaRepository, adminRepository, jwtTokenService, userRepository
        );
        EmpresaAdmin adminLogado = new EmpresaAdmin(
                10L, 1L, "FRI12345", "12345678000195", "Admin", "admin@friboi.com.br",
                null, null, null, "hash", true, Instant.now(), Instant.now()
        );
        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.buscarPorId(10L)).thenReturn(Optional.of(adminLogado));
        when(userRepository.existsByCpf("12345678901")).thenReturn(false);
        when(userRepository.existsByEmail("motorista@exemplo.com")).thenReturn(false);
        when(userRepository.save(any(com.example.efficientia.cadastrobase.persistence.UsuarioEntity.class)))
                .thenAnswer(invocation -> {
                    com.example.efficientia.cadastrobase.persistence.UsuarioEntity usuario = invocation.getArgument(0);
                    usuario.setId(42);
                    return usuario;
                });

        var request = new com.example.efficientia.empresa.api.EmpresaAdminContracts.CriarFuncionarioEmpresaRequest(
                com.example.efficientia.cadastrobase.domain.TipoUsuario.motorista, "123.456.789-01", "  Joao Silva ",
                null, " MOTORISTA@EXEMPLO.COM ", " 11999998888 ", "senhaMot123", null, null, null, null
        );
        var response = serviceComUsuarios.cadastrarFuncionario(1L, request, 10L);

        assertEquals(42, response.id());
        assertEquals("Joao Silva", response.nome());
        assertEquals("12345678901", response.cpf());
        assertEquals("motorista@exemplo.com", response.email());
        assertEquals("11999998888", response.telefone());
        assertEquals("motorista", response.cargo());
        assertTrue(response.ativo());
        ArgumentCaptor<com.example.efficientia.cadastrobase.persistence.UsuarioEntity> captor =
                ArgumentCaptor.forClass(com.example.efficientia.cadastrobase.persistence.UsuarioEntity.class);
        verify(userRepository).save(captor.capture());
        assertEquals("FRI12345", captor.getValue().getCodigoInterno());
        assertTrue(passwordEncoder.matches("senhaMot123", captor.getValue().getSenhaHash()));
    }

    @Test
    @DisplayName("Deve filtrar funcionários por empresa e excluir administradores da listagem")
    void deveFiltrarFuncionariosPorEmpresaEExcluirAdmins() {
        com.example.efficientia.cadastrobase.persistence.UsuarioRepository userRepository =
                org.mockito.Mockito.mock(com.example.efficientia.cadastrobase.persistence.UsuarioRepository.class);
        EmpresaAdminService serviceComUsuarios = new EmpresaAdminService(
                empresaRepository, adminRepository, jwtTokenService, userRepository
        );
        EmpresaAdmin adminLogado = new EmpresaAdmin(
                10L, 1L, "FRI12345", "12345678000195", "Admin", "admin@friboi.com.br",
                null, null, null, "hash", true, Instant.now(), Instant.now()
        );
        var motorista = criarUsuario(41, com.example.efficientia.cadastrobase.domain.TipoUsuario.motorista,
                "fri12345", "Motorista", "motorista@exemplo.com");
        var adminEmpresa = criarUsuario(42, com.example.efficientia.cadastrobase.domain.TipoUsuario.administrador,
                "FRI12345", "Outro admin", "outro-admin@exemplo.com");
        var motoristaDeOutraEmpresa = criarUsuario(43, com.example.efficientia.cadastrobase.domain.TipoUsuario.motorista,
                "OUTRA", "Outro motorista", "outro@exemplo.com");
        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.buscarPorId(10L)).thenReturn(Optional.of(adminLogado));
        when(userRepository.findAll()).thenReturn(List.of(motorista, adminEmpresa, motoristaDeOutraEmpresa));

        var funcionarios = serviceComUsuarios.listarFuncionariosPorEmpresa(1L, 10L);

        assertEquals(1, funcionarios.size());
        assertEquals(41, funcionarios.get(0).id());
        assertEquals("Motorista", funcionarios.get(0).nome());
        assertEquals("motorista", funcionarios.get(0).cargo());
    }

    @Test
    @DisplayName("Deve usar CPF substituto e não duplicar usuário sincronizado ao criar primeiro admin")
    void deveAplicarCpfSubstitutoEIgnorarAdminJaSincronizado() {
        com.example.efficientia.cadastrobase.persistence.UsuarioRepository userRepository =
                org.mockito.Mockito.mock(com.example.efficientia.cadastrobase.persistence.UsuarioRepository.class);
        EmpresaAdminService serviceComUsuarios = new EmpresaAdminService(
                empresaRepository, adminRepository, jwtTokenService, userRepository
        );
        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.contarPorEmpresaId(1L)).thenReturn(0L);
        when(adminRepository.salvar(any(EmpresaAdmin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.existsByEmail("semcpf@exemplo.com")).thenReturn(true);

        serviceComUsuarios.cadastrarPrimeiroAdmin(new CriarPrimeiroAdminRequest(
                1L, null, null, "Sem CPF", "SemCpf@Exemplo.com", "senhaSegura", null, null, null
        ));

        verify(userRepository).existsByEmail("semcpf@exemplo.com");
        verify(userRepository, never()).save(any(com.example.efficientia.cadastrobase.persistence.UsuarioEntity.class));
    }

    @Test
    @DisplayName("Deve ignorar falha de sincronização do admin sem interromper o cadastro")
    void deveIgnorarFalhaAoSincronizarAdmin() {
        com.example.efficientia.cadastrobase.persistence.UsuarioRepository userRepository =
                org.mockito.Mockito.mock(com.example.efficientia.cadastrobase.persistence.UsuarioRepository.class);
        EmpresaAdminService serviceComUsuarios = new EmpresaAdminService(
                empresaRepository, adminRepository, jwtTokenService, userRepository
        );
        when(empresaRepository.buscarPorId(1L)).thenReturn(Optional.of(empresaMock));
        when(adminRepository.contarPorEmpresaId(1L)).thenReturn(0L);
        when(adminRepository.salvar(any(EmpresaAdmin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(com.example.efficientia.cadastrobase.persistence.UsuarioEntity.class)))
                .thenThrow(new IllegalStateException("banco indisponível"));

        LoginAdminResponse response = serviceComUsuarios.cadastrarPrimeiroAdmin(new CriarPrimeiroAdminRequest(
                1L, null, null, "Admin", "admin@exemplo.com", "senhaSegura", null, null, null
        ));

        assertNotNull(response);
        ArgumentCaptor<com.example.efficientia.cadastrobase.persistence.UsuarioEntity> captor =
                ArgumentCaptor.forClass(com.example.efficientia.cadastrobase.persistence.UsuarioEntity.class);
        verify(userRepository).save(captor.capture());
        assertEquals("00000000000", captor.getValue().getCpf());
    }

    private com.example.efficientia.cadastrobase.persistence.UsuarioEntity criarUsuario(
            Integer id,
            com.example.efficientia.cadastrobase.domain.TipoUsuario tipo,
            String codigoEmpresa,
            String nome,
            String email
    ) {
        var usuario = new com.example.efficientia.cadastrobase.persistence.UsuarioEntity();
        usuario.setId(id);
        usuario.setTipo(tipo);
        usuario.setCodigoInterno(codigoEmpresa);
        usuario.setNome(nome);
        usuario.setCpf("12345678901");
        usuario.setEmail(email);
        usuario.setTelefone("11999990000");
        usuario.setAtivo(true);
        return usuario;
    }
}
