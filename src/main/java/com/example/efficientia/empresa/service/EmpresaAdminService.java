package com.example.efficientia.empresa.service;

import com.example.efficientia.auth.api.AutenticacaoInvalidaException;
import com.example.efficientia.auth.service.JwtTokenService;
import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.AdminResponse;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.CriarAdminRequest;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.CriarFuncionarioEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.CriarPrimeiroAdminRequest;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.FuncionarioEmpresaResponse;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.LoginAdminRequest;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.LoginAdminResponse;
import com.example.efficientia.empresa.api.EmpresaContracts.EmpresaResponse;
import com.example.efficientia.empresa.domain.Empresa;
import com.example.efficientia.empresa.domain.EmpresaAdmin;
import com.example.efficientia.empresa.persistence.EmpresaAdminRepository;
import com.example.efficientia.empresa.persistence.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class EmpresaAdminService {

    private static final Logger log = LoggerFactory.getLogger(EmpresaAdminService.class);

    private final EmpresaRepository empresaRepository;
    private final EmpresaAdminRepository adminRepository;
    private final JwtTokenService jwtTokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired(required = false)
    private UsuarioRepository usuarioRepository;
    public EmpresaAdminService(
            EmpresaRepository empresaRepository,
            EmpresaAdminRepository adminRepository,
            JwtTokenService jwtTokenService
    ) {
        this(empresaRepository, adminRepository, jwtTokenService, null);
    }

    @Autowired
    public EmpresaAdminService(
            EmpresaRepository empresaRepository,
            EmpresaAdminRepository adminRepository,
            JwtTokenService jwtTokenService,
            @Autowired(required = false) UsuarioRepository usuarioRepository
    ) {
        this.empresaRepository = empresaRepository;
        this.adminRepository = adminRepository;
        this.jwtTokenService = jwtTokenService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Cadastra o primeiro administrador da empresa logo após a sua criação no portal.
     * É de total obrigatoriedade para liberar o acesso ao sistema.
     * Caso a empresa já possua administradores cadastrados, a operação é rejeitada.
     */
    public LoginAdminResponse cadastrarPrimeiroAdmin(CriarPrimeiroAdminRequest request) {
        Empresa empresa = localizarEmpresa(request.empresaId(), request.codigoEmpresa(), request.cnpj());

        long adminsExistentes = adminRepository.contarPorEmpresaId(empresa.getId());
        if (adminsExistentes > 0) {
            log.warn("Tentativa de cadastro de primeiro admin rejeitada: empresa ID {} já possui {} administradores cadastrados.",
                    empresa.getId(), adminsExistentes);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "A empresa já possui administradores cadastrados. Novos administradores devem ser adicionados por um administrador autenticado."
            );
        }

        String emailLimpo = sanitizarEmail(request.email());
        String nomeLimpo = request.nome().trim();
        String cpfLimpo = sanitizarCpf(request.cpf());

        validarDuplicidade(emailLimpo, cpfLimpo);

        String senhaHash = passwordEncoder.encode(request.senha());
        String cargoFinal = (request.cargo() != null && !request.cargo().isBlank())
                ? request.cargo().trim()
                : "Administrador Geral";

        Instant agora = Instant.now();
        EmpresaAdmin admin = new EmpresaAdmin(
                null,
                empresa.getId(),
                empresa.getCodigoEmpresa(),
                empresa.getCnpj(),
                nomeLimpo,
                emailLimpo,
                cpfLimpo,
                sanitizarTelefone(request.telefone()),
                cargoFinal,
                senhaHash,
                true,
                agora,
                agora
        );

        EmpresaAdmin salvo = adminRepository.salvar(admin);
        sincronizarUsuarioAdmin(salvo, empresa);

        log.info("Primeiro administrador cadastrado com sucesso para empresa ID {} (Código: {})", empresa.getId(), empresa.getCodigoEmpresa());

        String token = jwtTokenService.gerarTokenAdmin(salvo, empresa);
        return new LoginAdminResponse(token, AdminResponse.from(salvo), toEmpresaResponse(empresa));
    }

    /**
     * Adesão de novos administradores à empresa, realizada exclusivamente por um administrador
     * autenticado da mesma empresa.
     */
    public AdminResponse aderirNovoAdmin(Long empresaId, CriarAdminRequest request, Long adminLogadoId) {
        Empresa empresa = empresaRepository.buscarPorId(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o ID informado."));

        validarAdminPertenceEmpresa(empresaId, adminLogadoId, "Você não tem permissão para cadastrar administradores em outra empresa.");
        String emailLimpo = sanitizarEmail(request.email());
        String nomeLimpo = request.nome().trim();
        String cpfLimpo = sanitizarCpf(request.cpf());

        validarDuplicidade(emailLimpo, cpfLimpo);

        String senhaHash = passwordEncoder.encode(request.senha());
        String cargoFinal = (request.cargo() != null && !request.cargo().isBlank())
                ? request.cargo().trim()
                : "Administrador";

        Instant agora = Instant.now();
        EmpresaAdmin admin = new EmpresaAdmin(
                null,
                empresa.getId(),
                empresa.getCodigoEmpresa(),
                empresa.getCnpj(),
                nomeLimpo,
                emailLimpo,
                cpfLimpo,
                sanitizarTelefone(request.telefone()),
                cargoFinal,
                senhaHash,
                true,
                agora,
                agora
        );

        EmpresaAdmin salvo = adminRepository.salvar(admin);
        sincronizarUsuarioAdmin(salvo, empresa);

        log.info("Novo administrador aderido com sucesso (ID: {}) na empresa ID {} por admin ID {}",
                salvo.getId(), empresa.getId(), adminLogadoId);

        return AdminResponse.from(salvo);
    }

    /**
     * Lista todos os administradores vinculados a uma empresa.
     */
    public List<AdminResponse> listarAdminsPorEmpresa(Long empresaId, Long adminLogadoId) {
        empresaRepository.buscarPorId(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o ID informado."));
        validarAdminPertenceEmpresa(empresaId, adminLogadoId, "Você não tem permissão para visualizar administradores de outra empresa.");
        return adminRepository.listarPorEmpresaId(empresaId).stream()
                .map(AdminResponse::from)
                .toList();
    }

    /**
     * Autenticação de administrador com emissão de token JWT com role ADMIN.
     */
    public LoginAdminResponse autenticarAdmin(LoginAdminRequest request) {
        String loginLimpo = request.email().trim().toLowerCase(Locale.ROOT);
        String codigoEmpresaFiltro = request.codigoEmpresa() != null ? request.codigoEmpresa().trim() : null;
        String cnpjFiltro = request.cnpj() != null ? request.cnpj().replaceAll("[^0-9]", "").trim() : null;

        Optional<EmpresaAdmin> adminOpt = adminRepository.buscarPorEmail(loginLimpo);

        // Permite login via CPF se não encontrar por e-mail e contiver 11 dígitos
        if (adminOpt.isEmpty()) {
            String possivelCpf = loginLimpo.replaceAll("[^0-9]", "");
            if (possivelCpf.length() == 11) {
                adminOpt = adminRepository.buscarPorCpf(possivelCpf);
            }
        }

        if (adminOpt.isEmpty()) {
            log.warn("Falha de autenticação de admin: Administrador não encontrado para {}", loginLimpo);
            throw new AutenticacaoInvalidaException("Credenciais inválidas.");
        }

        EmpresaAdmin admin = adminOpt.get();

        if (!Boolean.TRUE.equals(admin.getAtivo())) {
            log.warn("Falha de autenticação de admin: Administrador ID {} está inativo.", admin.getId());
            throw new AutenticacaoInvalidaException("Credenciais inválidas.");
        }

        if (codigoEmpresaFiltro != null && !codigoEmpresaFiltro.isBlank()) {
            if (!admin.getCodigoEmpresa().equalsIgnoreCase(codigoEmpresaFiltro)) {
                log.warn("Falha de autenticação de admin: Código da empresa divergente para admin ID {}", admin.getId());
                throw new AutenticacaoInvalidaException("Credenciais inválidas.");
            }
        }

        if (cnpjFiltro != null && !cnpjFiltro.isBlank()) {
            if (!admin.getCnpjEmpresa().equalsIgnoreCase(cnpjFiltro)) {
                log.warn("Falha de autenticação de admin: CNPJ da empresa divergente para admin ID {}", admin.getId());
                throw new AutenticacaoInvalidaException("Credenciais inválidas.");
            }
        }

        if (!passwordEncoder.matches(request.senha(), admin.getSenhaHash())) {
            log.warn("Falha de autenticação de admin: Senha incorreta para admin ID {}", admin.getId());
            throw new AutenticacaoInvalidaException("Credenciais inválidas.");
        }

        Empresa empresa = empresaRepository.buscarPorId(admin.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Empresa vinculada ao administrador não foi encontrada."));

        String token = jwtTokenService.gerarTokenAdmin(admin, empresa);
        log.info("Administrador ID {} autenticado com sucesso para empresa ID {}", admin.getId(), empresa.getId());

        return new LoginAdminResponse(token, AdminResponse.from(admin), toEmpresaResponse(empresa));
    }

    /**
     * Cadastro de funcionários (motoristas, manobristas, analistas, curraleiros, pecuaristas)
     * vinculados à empresa, realizado exclusivamente por um administrador autenticado da empresa.
     */
    public FuncionarioEmpresaResponse cadastrarFuncionario(Long empresaId, CriarFuncionarioEmpresaRequest request, Long adminLogadoId) {
        Empresa empresa = empresaRepository.buscarPorId(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o ID informado."));

        validarAdminPertenceEmpresa(empresaId, adminLogadoId, "Você não tem permissão para cadastrar funcionários em outra empresa.");
        String cpfLimpo = sanitizarCpf(request.cpf());
        String emailLimpo = sanitizarEmail(request.email());
        String telefoneLimpo = sanitizarTelefone(request.telefone());

        if (usuarioRepository != null) {
            if (usuarioRepository.existsByCpf(cpfLimpo)) {
                throw new CadastroDuplicadoException("Já existe um funcionário cadastrado com o CPF informado.");
            }
            if (usuarioRepository.existsByEmail(emailLimpo)) {
                throw new CadastroDuplicadoException("Já existe um funcionário cadastrado com o e-mail informado.");
            }

            UsuarioEntity entity = new UsuarioEntity();
            entity.setTipo(request.tipo());
            entity.setCpf(cpfLimpo);
            entity.setNome(request.nome().trim());
            entity.setDataNascimento(request.dataNascimento());
            entity.setEmail(emailLimpo);
            entity.setTelefone(telefoneLimpo);
            entity.setCodigoInterno(empresa.getCodigoEmpresa());
            entity.setSenhaHash(passwordEncoder.encode(request.senha()));
            entity.setAtivo(true);

            UsuarioEntity salvo = usuarioRepository.save(entity);
            log.info("Funcionário tipo {} cadastrado com sucesso (ID: {}) na empresa ID {}",
                    salvo.getTipo(), salvo.getId(), empresa.getId());

            return new FuncionarioEmpresaResponse(
                    salvo.getId(),
                    empresa.getId(),
                    empresa.getCodigoEmpresa(),
                    salvo.getTipo(),
                    salvo.getNome(),
                    salvo.getCpf(),
                    salvo.getEmail(),
                    salvo.getTelefone(),
                    request.cargo() != null ? request.cargo() : request.tipo().name(),
                    salvo.getAtivo(),
                    Instant.now()
            );
        }

        // Fallback quando executado sem DataSource JPA
        return new FuncionarioEmpresaResponse(
                100,
                empresa.getId(),
                empresa.getCodigoEmpresa(),
                request.tipo(),
                request.nome().trim(),
                cpfLimpo,
                emailLimpo,
                telefoneLimpo,
                request.cargo() != null ? request.cargo() : request.tipo().name(),
                true,
                Instant.now()
        );
    }

    /**
     * Lista todos os funcionários vinculados à empresa.
     */
    public List<FuncionarioEmpresaResponse> listarFuncionariosPorEmpresa(Long empresaId, Long adminLogadoId) {
        Empresa empresa = empresaRepository.buscarPorId(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o ID informado."));
        validarAdminPertenceEmpresa(empresaId, adminLogadoId, "Você não tem permissão para visualizar funcionários de outra empresa.");

        if (usuarioRepository != null) {
            return usuarioRepository.findAll().stream()
                    .filter(u -> empresa.getCodigoEmpresa().equalsIgnoreCase(u.getCodigoInterno()))
                    .filter(u -> u.getTipo() != TipoUsuario.administrador)
                    .map(u -> new FuncionarioEmpresaResponse(
                            u.getId(),
                            empresa.getId(),
                            empresa.getCodigoEmpresa(),
                            u.getTipo(),
                            u.getNome(),
                            u.getCpf(),
                            u.getEmail(),
                            u.getTelefone(),
                            u.getTipo().name(),
                            u.getAtivo(),
                            Instant.now()
                    ))
                    .toList();
        }

        return List.of();
    }

    private void validarAdminPertenceEmpresa(Long empresaId, Long adminLogadoId, String mensagemErro) {
        if (adminLogadoId != null) {
            EmpresaAdmin adminLogado = adminRepository.buscarPorId(adminLogadoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrador autenticado não encontrado."));

            if (!empresaId.equals(adminLogado.getEmpresaId())) {
                log.warn("Acesso negado: admin ID {} pertence à empresa ID {}, operação requerida na empresa ID {}",
                        adminLogadoId, adminLogado.getEmpresaId(), empresaId);
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        mensagemErro != null ? mensagemErro : "Você não tem permissão para gerenciar recursos de outra empresa."
                );
            }
        }
    }

    private void sincronizarUsuarioAdmin(EmpresaAdmin admin, Empresa empresa) {
        if (usuarioRepository != null && !usuarioRepository.existsByEmail(admin.getEmail())) {
            try {
                UsuarioEntity usuario = new UsuarioEntity();
                usuario.setTipo(TipoUsuario.administrador);
                usuario.setCpf(admin.getCpf() != null ? admin.getCpf() : "00000000000");
                usuario.setNome(admin.getNome());
                usuario.setEmail(admin.getEmail());
                usuario.setTelefone(admin.getTelefone());
                usuario.setCodigoInterno(empresa.getCodigoEmpresa());
                usuario.setSenhaHash(admin.getSenhaHash());
                usuario.setAtivo(true);
                usuarioRepository.save(usuario);
                log.debug("Administrador ID {} sincronizado com UsuarioRepository", admin.getId());
            } catch (Exception e) {
                log.warn("Não foi possível sincronizar admin ID {} com UsuarioRepository: {}", admin.getId(), e.getMessage());
            }
        }
    }

    private Empresa localizarEmpresa(Long empresaId, String codigoEmpresa, String cnpj) {
        if (empresaId != null) {
            return empresaRepository.buscarPorId(empresaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o ID informado."));
        }
        if (codigoEmpresa != null && !codigoEmpresa.isBlank()) {
            return empresaRepository.buscarPorCodigo(codigoEmpresa.trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o código informado."));
        }
        if (cnpj != null && !cnpj.isBlank()) {
            String cnpjLimpo = cnpj.replaceAll("[^0-9]", "");
            return empresaRepository.buscarPorCnpj(cnpjLimpo)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o CNPJ informado."));
        }
        throw new CadastroInvalidoException("É necessário informar o ID, código ou CNPJ da empresa.");
    }

    private void validarDuplicidade(String email, String cpf) {
        if (adminRepository.existePorEmail(email)) {
            throw new CadastroDuplicadoException("Já existe um administrador cadastrado com o e-mail informado.");
        }
        if (cpf != null && !cpf.isBlank() && adminRepository.existePorCpf(cpf)) {
            throw new CadastroDuplicadoException("Já existe um administrador cadastrado com o CPF informado.");
        }
    }

    private String sanitizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        String limpo = cpf.replaceAll("[^0-9]", "");
        if (limpo.length() != 11) {
            throw new CadastroInvalidoException("CPF inválido. Deve conter 11 dígitos numéricos.");
        }
        return limpo;
    }

    private String sanitizarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return null;
        }
        return telefone.trim();
    }

    private EmpresaResponse toEmpresaResponse(Empresa empresa) {
        long totalAdmins = adminRepository.contarPorEmpresaId(empresa.getId());
        boolean pendenteAdmin = totalAdmins == 0;

        return new EmpresaResponse(
                empresa.getId(),
                empresa.getCodigoEmpresa(),
                empresa.getNomeEmpresa(),
                empresa.getRazaoSocial(),
                empresa.getCnpj(),
                empresa.getEmailCorporativo(),
                empresa.getEnderecoId(),
                pendenteAdmin ? "PENDENTE_PRIMEIRO_ADMIN" : "ATIVO",
                pendenteAdmin,
                pendenteAdmin ? "CADASTRO_PRIMEIRO_ADMIN" : "PAINEL_ADMINISTRATIVO",
                pendenteAdmin
                        ? "Empresa registrada. O cadastro do primeiro administrador é obrigatório para liberar o acesso ao sistema."
                        : "Empresa ativa e operacional.",
                empresa.getCriadoEm()
        );
    }
}
