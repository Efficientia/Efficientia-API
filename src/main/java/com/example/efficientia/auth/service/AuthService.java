package com.example.efficientia.auth.service;

import com.example.efficientia.auth.api.AuthContracts.LoginRequest;
import com.example.efficientia.auth.api.AuthContracts.LoginResponse;
import com.example.efficientia.auth.api.AuthContracts.SignupRequest;
import com.example.efficientia.auth.api.AutenticacaoInvalidaException;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarUsuarioRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.UsuarioResponse;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import com.example.efficientia.cadastrobase.service.CadastroBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final CadastroBaseService cadastroBaseService;
    private final JwtTokenService jwtTokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            UsuarioRepository usuarioRepository,
            CadastroBaseService cadastroBaseService,
            JwtTokenService jwtTokenService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.cadastroBaseService = cadastroBaseService;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public UsuarioResponse cadastrar(SignupRequest request) {
        CriarUsuarioRequest criarRequest = new CriarUsuarioRequest(
                request.tipo(),
                request.cpf(),
                request.codigoInterno(),
                request.nome(),
                request.dataNascimento(),
                request.email(),
                request.telefone(),
                request.senha()
        );
        return cadastroBaseService.criarUsuario(criarRequest);
    }

    @Transactional(readOnly = true)
    public LoginResponse autenticar(LoginRequest request) {
        String cpfLimpo = request.cpf().replaceAll("[^0-9]", "").trim();
        String emailLimpo = request.email().trim().toLowerCase(Locale.ROOT);
        String codigoEmpresaLimpo = request.codigoEmpresa().trim();

        Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findByCpf(cpfLimpo);
        if (usuarioOpt.isEmpty()) {
            usuarioOpt = usuarioRepository.findByEmail(emailLimpo);
        }

        if (usuarioOpt.isEmpty()) {
            log.warn("Falha de autenticação: Usuário não encontrado para CPF/Email fornecido.");
            throw new AutenticacaoInvalidaException("Credenciais inválidas.");
        }

        UsuarioEntity usuario = usuarioOpt.get();

        if (!usuario.getCpf().equals(cpfLimpo)) {
            log.warn("Falha de autenticação: CPF diverge para o usuário ID {}", usuario.getId());
            throw new AutenticacaoInvalidaException("Credenciais inválidas.");
        }

        if (!usuario.getEmail().equalsIgnoreCase(emailLimpo)) {
            log.warn("Falha de autenticação: E-mail diverge para o usuário ID {}", usuario.getId());
            throw new AutenticacaoInvalidaException("Credenciais inválidas.");
        }

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            log.warn("Falha de autenticação: Usuário ID {} está inativo.", usuario.getId());
            throw new AutenticacaoInvalidaException("Credenciais inválidas.");
        }

        if (usuario.getCodigoInterno() != null && !usuario.getCodigoInterno().isBlank()) {
            if (!usuario.getCodigoInterno().equalsIgnoreCase(codigoEmpresaLimpo)) {
                log.warn("Falha de autenticação: Código da empresa incorreto para usuário ID {}", usuario.getId());
                throw new AutenticacaoInvalidaException("Credenciais inválidas.");
            }
        }

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            log.warn("Falha de autenticação: Senha incorreta para usuário ID {}", usuario.getId());
            throw new AutenticacaoInvalidaException("Credenciais inválidas.");
        }

        String token = jwtTokenService.gerarToken(usuario);
        return new LoginResponse(token, UsuarioResponse.from(usuario));
    }
}
