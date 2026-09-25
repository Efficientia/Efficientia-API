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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
}
