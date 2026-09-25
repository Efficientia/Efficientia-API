package com.example.efficientia.empresa.service;

import com.example.efficientia.auth.api.AutenticacaoInvalidaException;
import com.example.efficientia.auth.service.JwtTokenService;
import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.AdminResponse;
import com.example.efficientia.empresa.api.EmpresaContracts.CriarEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.EmpresaResponse;
import com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaResponse;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class EmpresaService {

    private static final Logger log = LoggerFactory.getLogger(EmpresaService.class);

    private final EmpresaRepository repository;
    private final EmpresaAdminRepository adminRepository;
    private final CodigoEmpresaGenerator codigoGenerator;
    private final JwtTokenService jwtTokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public EmpresaService(
            EmpresaRepository repository,
            EmpresaAdminRepository adminRepository,
            CodigoEmpresaGenerator codigoGenerator,
            JwtTokenService jwtTokenService
    ) {
        this.repository = repository;
        this.adminRepository = adminRepository;
        this.codigoGenerator = codigoGenerator;
        this.jwtTokenService = jwtTokenService;
    }
    public EmpresaService(EmpresaRepository repository, CodigoEmpresaGenerator codigoGenerator) {
        this(repository, new com.example.efficientia.empresa.persistence.InMemoryEmpresaAdminRepository(), codigoGenerator, null);
    }

    public EmpresaResponse cadastrarEmpresa(CriarEmpresaRequest request) {
        String cnpjLimpo = sanitizarCnpj(request.cnpj());
        if (cnpjLimpo.length() != 14) {
            throw new CadastroInvalidoException("CNPJ inválido. Deve conter 14 dígitos numéricos.");
        }

        String emailLimpo = request.emailCorporativo().trim().toLowerCase(Locale.ROOT);
        String nomeEmpresaLimpo = request.nomeEmpresa() != null ? request.nomeEmpresa().trim() : "";
        String razaoSocialLimpa = request.razaoSocial() != null && !request.razaoSocial().isBlank()
                ? request.razaoSocial().trim()
                : nomeEmpresaLimpo;

        if (nomeEmpresaLimpo.isBlank() && !razaoSocialLimpa.isBlank()) {
            nomeEmpresaLimpo = razaoSocialLimpa;
        }

        if (nomeEmpresaLimpo.isBlank()) {
            throw new CadastroInvalidoException("O nome ou razão social da empresa é obrigatório.");
        }

        if (repository.existePorCnpj(cnpjLimpo)) {
            throw new CadastroDuplicadoException("Já existe uma empresa cadastrada com o CNPJ informado.");
        }

        if (repository.existePorEmail(emailLimpo)) {
            throw new CadastroDuplicadoException("Já existe uma empresa cadastrada com o e-mail corporativo informado.");
        }

        String codigo = gerarCodigoUnico(nomeEmpresaLimpo);
        String senhaHash = (request.senha() != null && !request.senha().isBlank())
                ? passwordEncoder.encode(request.senha())
                : null;
        Instant agora = Instant.now();

        Empresa empresa = new Empresa(
                null,
                request.enderecoId(),
                codigo,
                nomeEmpresaLimpo,
                razaoSocialLimpa,
                cnpjLimpo,
                emailLimpo,
                senhaHash,
                true,
                agora,
                agora
        );

        Empresa salva = repository.salvar(empresa);
        log.info("Empresa cadastrada com sucesso: ID={}, Código={}, CNPJ={}", salva.getId(), salva.getCodigoEmpresa(), salva.getCnpj());

        return toResponse(salva);
    }

    /**
     * Autenticação e verificação de status da Empresa.
     * Identifica a empresa por CNPJ, e-mail ou código de 8 dígitos.
     * Caso a empresa não possua nenhum administrador cadastrado, sinaliza a obrigatoriedade
     * do cadastro do primeiro administrador.
     */
    public LoginEmpresaResponse autenticarEmpresa(LoginEmpresaRequest request) {
        String identificador = request.cnpj().trim();
        Empresa empresa = localizarEmpresaPorIdentificador(identificador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o identificador informado."));

        long totalAdmins = adminRepository.contarPorEmpresaId(empresa.getId());
        EmpresaResponse empresaResponse = toResponse(empresa);

        if (totalAdmins == 0) {
            log.warn("Empresa ID {} localizada, porém requer obrigatoriamente o cadastro do primeiro administrador.", empresa.getId());
            return LoginEmpresaResponse.pendentePrimeiroAdmin(empresaResponse);
        }

        // Se a senha foi informada, valida autenticação
        if (request.senha() != null && !request.senha().isBlank()) {
            List<EmpresaAdmin> admins = adminRepository.listarPorEmpresaId(empresa.getId());

            // 1. Tenta validar contra a senha de algum administrador da empresa
            Optional<EmpresaAdmin> adminAutenticado = admins.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getAtivo()))
                    .filter(a -> passwordEncoder.matches(request.senha(), a.getSenhaHash()))
                    .findFirst();

            if (adminAutenticado.isPresent()) {
                EmpresaAdmin admin = adminAutenticado.get();
                String token = jwtTokenService.gerarTokenAdmin(admin, empresa);
                log.info("Empresa ID {} autenticada com sucesso via credencial de administrador ID {}", empresa.getId(), admin.getId());
                return LoginEmpresaResponse.autenticado(empresaResponse, token, AdminResponse.from(admin));
            }

            // 2. Tenta validar contra a senha corporativa da empresa (se cadastrada)
            if (empresa.getSenhaHash() != null && passwordEncoder.matches(request.senha(), empresa.getSenhaHash())) {
                EmpresaAdmin primeiroAdmin = admins.stream()
                        .filter(a -> Boolean.TRUE.equals(a.getAtivo()))
                        .findFirst()
                        .orElse(null);

                String token = primeiroAdmin != null
                        ? jwtTokenService.gerarTokenAdmin(primeiroAdmin, empresa)
                        : null;

                Object adminObj = primeiroAdmin != null ? AdminResponse.from(primeiroAdmin) : null;
                log.info("Empresa ID {} autenticada com sucesso via senha corporativa", empresa.getId());
                return LoginEmpresaResponse.autenticado(empresaResponse, token, adminObj);
            }

            log.warn("Falha de autenticação corporativa para empresa ID {}: Senha incorreta.", empresa.getId());
            throw new AutenticacaoInvalidaException("Senha corporativa ou de administrador inválida.");
        }

        // Senha não informada: apenas status da empresa
        return LoginEmpresaResponse.ativoRequerAdmin(empresaResponse);
    }

    public List<EmpresaResponse> listarEmpresas() {
        return repository.listarTodas().stream()
                .map(this::toResponse)
                .toList();
    }

    public EmpresaResponse buscarPorId(Long id) {
        return repository.buscarPorId(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o ID informado."));
    }

    public EmpresaResponse buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new CadastroInvalidoException("Código de empresa não informado.");
        }
        return repository.buscarPorCodigo(codigo.trim())
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o código informado."));
    }

    public EmpresaResponse buscarPorCnpj(String cnpj) {
        String cnpjLimpo = sanitizarCnpj(cnpj);
        if (cnpjLimpo.isBlank()) {
            throw new CadastroInvalidoException("CNPJ não informado.");
        }
        return repository.buscarPorCnpj(cnpjLimpo)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o CNPJ informado."));
    }

    private Optional<Empresa> localizarEmpresaPorIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return Optional.empty();
        }
        String limpo = identificador.trim();
        String apenasDigitos = limpo.replaceAll("[^0-9]", "");

        // Se contiver 14 dígitos, busca por CNPJ
        if (apenasDigitos.length() == 14) {
            Optional<Empresa> emp = repository.buscarPorCnpj(apenasDigitos);
            if (emp.isPresent()) {
                return emp;
            }
        }

        // Se contiver '@', busca por e-mail
        if (limpo.contains("@")) {
            Optional<Empresa> emp = repository.buscarPorEmail(limpo.toLowerCase(Locale.ROOT));
            if (emp.isPresent()) {
                return emp;
            }
        }

        // Busca por código (ex: FRI48291)
        Optional<Empresa> emp = repository.buscarPorCodigo(limpo);
        if (emp.isPresent()) {
            return emp;
        }

        // Fallback: busca por ID se for numérico
        try {
            Long id = Long.parseLong(limpo);
            return repository.buscarPorId(id);
        } catch (NumberFormatException ignored) {
        }

        return Optional.empty();
    }

    private String gerarCodigoUnico(String nomeEmpresa) {
        String codigo = codigoGenerator.gerarCodigo(nomeEmpresa);
        int tentativas = 0;
        while (repository.existePorCodigo(codigo) && tentativas < 20) {
            codigo = codigoGenerator.gerarCodigo(nomeEmpresa);
            tentativas++;
        }
        return codigo;
    }

    private String sanitizarCnpj(String cnpj) {
        if (cnpj == null) {
            return "";
        }
        return cnpj.replaceAll("[^0-9]", "");
    }

    public EmpresaResponse toResponse(Empresa empresa) {
        long totalAdmins = adminRepository.contarPorEmpresaId(empresa.getId());
        boolean pendenteAdmin = totalAdmins == 0;

        String status = pendenteAdmin ? "PENDENTE_PRIMEIRO_ADMIN" : "ATIVO";
        String proximoPasso = pendenteAdmin ? "CADASTRO_PRIMEIRO_ADMIN" : "PAINEL_ADMINISTRATIVO";
        String mensagem = pendenteAdmin
                ? "Empresa registrada com sucesso. O cadastro do primeiro administrador é obrigatório para liberar o acesso ao sistema."
                : "Empresa ativa e operacional.";

        return new EmpresaResponse(
                empresa.getId(),
                empresa.getCodigoEmpresa(),
                empresa.getNomeEmpresa(),
                empresa.getRazaoSocial(),
                empresa.getCnpj(),
                empresa.getEmailCorporativo(),
                empresa.getEnderecoId(),
                status,
                pendenteAdmin,
                proximoPasso,
                mensagem,
                empresa.getCriadoEm()
        );
    }
}
