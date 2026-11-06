package com.example.efficientia.auth.service;

import com.example.efficientia.assinaturamotorista.persistence.AssinaturaMotoristaEntity;
import com.example.efficientia.assinaturamotorista.persistence.AssinaturaMotoristaRepository;
import com.example.efficientia.auth.api.AuthContracts.LoginRequest;
import com.example.efficientia.auth.api.AuthContracts.LoginResponse;
import com.example.efficientia.auth.api.AuthContracts.SignupRequest;
import com.example.efficientia.auth.api.AutenticacaoInvalidaException;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.UsuarioResponse;
import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import com.example.efficientia.cadastrobase.service.CadastroBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CadastroBaseService cadastroBaseService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private AssinaturaMotoristaRepository assinaturaMotoristaRepository;

    private AuthService authService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioRepository, cadastroBaseService, jwtTokenService, assinaturaMotoristaRepository);
    }

    @Test
    void deveAutenticarComSucesso() {
        UsuarioEntity usuario = criarUsuarioMock();
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(usuario));
        when(jwtTokenService.gerarToken(usuario)).thenReturn("mocked.jwt.token");

        LoginRequest request = new LoginRequest(
                "12345678901",
                "motorista@test.com",
                "senha123",
                "EMP-100"
        );

        LoginResponse response = authService.autenticar(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals("12345678901", response.usuario().cpf());
        assertEquals(false, response.usuario().assinaturaFixaCadastrada());
    }

    @Test
    void deveAutenticarMotoristasDiferentesDaMesmaEmpresaComMesmoCodigoInterno() {
        UsuarioEntity primeiroMotorista = criarUsuarioMock();
        primeiroMotorista.setCpf("11111111111");
        primeiroMotorista.setEmail("primeiro.motorista@test.com");
        primeiroMotorista.setEmpresaId(42);

        UsuarioEntity segundoMotorista = criarUsuarioMock();
        segundoMotorista.setCpf("22222222222");
        segundoMotorista.setEmail("segundo.motorista@test.com");
        segundoMotorista.setEmpresaId(42);

        when(usuarioRepository.findByCpf("11111111111")).thenReturn(Optional.of(primeiroMotorista));
        when(usuarioRepository.findByCpf("22222222222")).thenReturn(Optional.of(segundoMotorista));
        when(jwtTokenService.gerarToken(primeiroMotorista)).thenReturn("primeiro.jwt.token");
        when(jwtTokenService.gerarToken(segundoMotorista)).thenReturn("segundo.jwt.token");

        LoginResponse primeiraResposta = authService.autenticar(new LoginRequest(
                "11111111111",
                "primeiro.motorista@test.com",
                "senha123",
                "EMP-100"
        ));
        LoginResponse segundaResposta = authService.autenticar(new LoginRequest(
                "22222222222",
                "segundo.motorista@test.com",
                "senha123",
                "EMP-100"
        ));

        assertNotNull(primeiraResposta);
        assertNotNull(segundaResposta);
        assertEquals("primeiro.jwt.token", primeiraResposta.token());
        assertEquals("segundo.jwt.token", segundaResposta.token());
        assertEquals("11111111111", primeiraResposta.usuario().cpf());
        assertEquals("22222222222", segundaResposta.usuario().cpf());
    }

    @Test
    void deveAutenticarMotoristaComAssinaturaFixaCadastrada() {
        UsuarioEntity usuario = criarUsuarioMock();
        UUID assinaturaId = UUID.randomUUID();
        AssinaturaMotoristaEntity assinatura = new AssinaturaMotoristaEntity();
        assinatura.setId(assinaturaId);
        assinatura.setMotoristaId(1);
        assinatura.setAtiva(true);

        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(usuario));
        when(jwtTokenService.gerarToken(usuario)).thenReturn("mocked.jwt.token");
        when(assinaturaMotoristaRepository.findByMotoristaIdAndAtivaTrue(usuario.getId())).thenReturn(Optional.of(assinatura));

        LoginRequest request = new LoginRequest(
                "12345678901",
                "motorista@test.com",
                "senha123",
                "EMP-100"
        );

        LoginResponse response = authService.autenticar(request);

        assertNotNull(response);
        assertEquals(true, response.usuario().assinaturaFixaCadastrada());
        assertEquals(assinaturaId, response.usuario().assinaturaFixaId());
    }

    @Test
    void deveRejeitarSenhaIncorreta() {
        UsuarioEntity usuario = criarUsuarioMock();
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(usuario));

        LoginRequest request = new LoginRequest(
                "12345678901",
                "motorista@test.com",
                "senhaErrada",
                "EMP-100"
        );

        AutenticacaoInvalidaException ex = assertThrows(
                AutenticacaoInvalidaException.class,
                () -> authService.autenticar(request)
        );

        assertEquals("Credenciais inválidas.", ex.getMessage());
    }

    @Test
    void deveRejeitarCodigoEmpresaDivergente() {
        UsuarioEntity usuario = criarUsuarioMock();
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(usuario));

        LoginRequest request = new LoginRequest(
                "12345678901",
                "motorista@test.com",
                "senha123",
                "EMP-OUTRA"
        );

        AutenticacaoInvalidaException ex = assertThrows(
                AutenticacaoInvalidaException.class,
                () -> authService.autenticar(request)
        );

        assertEquals("Credenciais inválidas.", ex.getMessage());
    }

    @Test
    void deveRejeitarUsuarioInativo() {
        UsuarioEntity usuario = criarUsuarioMock();
        usuario.setAtivo(false);
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(usuario));

        LoginRequest request = new LoginRequest(
                "12345678901",
                "motorista@test.com",
                "senha123",
                "EMP-100"
        );

        AutenticacaoInvalidaException ex = assertThrows(
                AutenticacaoInvalidaException.class,
                () -> authService.autenticar(request)
        );

        assertEquals("Credenciais inválidas.", ex.getMessage());
    }

    @Test
    void deveCadastrarUsuarioComSucesso() {
        UsuarioResponse mockResponse = new UsuarioResponse(
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
        when(cadastroBaseService.criarUsuario(any())).thenReturn(mockResponse);

        SignupRequest signupRequest = new SignupRequest(
                TipoUsuario.motorista,
                "12345678901",
                "EMP-100",
                "Motorista Teste",
                LocalDate.of(1990, 1, 1),
                "motorista@test.com",
                "11999998888",
                "senha123"
        );

        UsuarioResponse result = authService.cadastrar(signupRequest);

        assertNotNull(result);
        assertEquals("Motorista Teste", result.nome());
    }

    private UsuarioEntity criarUsuarioMock() {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setTipo(TipoUsuario.motorista);
        entity.setCpf("12345678901");
        entity.setCodigoInterno("EMP-100");
        entity.setNome("Motorista Teste");
        entity.setEmail("motorista@test.com");
        entity.setTelefone("11999998888");
        entity.setSenhaHash(passwordEncoder.encode("senha123"));
        entity.setAtivo(true);
        return entity;
    }
}
