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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

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

        UsuarioEntity usuario = usuarioOpt.orElseThrow(() ->
                new AutenticacaoInvalidaException("Credenciais inválidas: usuário não encontrado.")
        );

        if (!usuario.getCpf().equals(cpfLimpo)) {
            throw new AutenticacaoInvalidaException("Credenciais inválidas: CPF diverge do cadastrado.");
        }

        if (!usuario.getEmail().equalsIgnoreCase(emailLimpo)) {
            throw new AutenticacaoInvalidaException("Credenciais inválidas: e-mail diverge do cadastrado.");
        }

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new AutenticacaoInvalidaException("Usuário inativo no sistema.");
        }

        if (usuario.getCodigoInterno() != null && !usuario.getCodigoInterno().isBlank()) {
            if (!usuario.getCodigoInterno().equalsIgnoreCase(codigoEmpresaLimpo)) {
                throw new AutenticacaoInvalidaException("Código da empresa incorreto.");
            }
        }

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new AutenticacaoInvalidaException("Credenciais inválidas: senha incorreta.");
        }

        String token = jwtTokenService.gerarToken(usuario);
        return new LoginResponse(token, UsuarioResponse.from(usuario));
    }
}
