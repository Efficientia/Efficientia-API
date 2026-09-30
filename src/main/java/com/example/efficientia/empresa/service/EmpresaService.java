package com.example.efficientia.empresa.service;

import com.example.efficientia.auth.api.AutenticacaoInvalidaException;
import com.example.efficientia.auth.service.JwtTokenService;
import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
import com.example.efficientia.cadastrobase.persistence.EnderecoEntity;
import com.example.efficientia.cadastrobase.persistence.EnderecoRepository;
import com.example.efficientia.documento.storage.StorageService;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.AdminResponse;
import com.example.efficientia.empresa.api.EmpresaContracts.AtualizarDadosEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.CriarEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.EmpresaResponse;
import com.example.efficientia.empresa.api.EmpresaContracts.EnderecoDto;
import com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaResponse;
import com.example.efficientia.empresa.api.EmpresaContracts.UploadLogoResponse;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
@Service
public class EmpresaService {

    private static final Logger log = LoggerFactory.getLogger(EmpresaService.class);

    private final EmpresaRepository repository;
    private final EmpresaAdminRepository adminRepository;
    private final CodigoEmpresaGenerator codigoGenerator;
    private final JwtTokenService jwtTokenService;
    private final EnderecoRepository enderecoRepository;
    private final StorageService storageService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final Map<Integer, EnderecoDto> enderecosEmMemoria = new ConcurrentHashMap<>();
    private final AtomicInteger enderecoIdSeq = new AtomicInteger(100);
    private final Map<Long, byte[]> logosEmMemoria = new ConcurrentHashMap<>();
    private final Map<Long, String> mimeTypesLogo = new ConcurrentHashMap<>();

    @Autowired
    public EmpresaService(
            EmpresaRepository repository,
            EmpresaAdminRepository adminRepository,
            CodigoEmpresaGenerator codigoGenerator,
            JwtTokenService jwtTokenService,
            @Autowired(required = false) EnderecoRepository enderecoRepository,
            @Autowired(required = false) StorageService storageService
    ) {
        this.repository = repository;
        this.adminRepository = adminRepository;
        this.codigoGenerator = codigoGenerator;
        this.jwtTokenService = jwtTokenService;
        this.enderecoRepository = enderecoRepository;
        this.storageService = storageService;
    }

    public EmpresaService(
            EmpresaRepository repository,
            EmpresaAdminRepository adminRepository,
            CodigoEmpresaGenerator codigoGenerator,
            JwtTokenService jwtTokenService
    ) {
        this(repository, adminRepository, codigoGenerator, jwtTokenService, null, null);
    }

    public EmpresaService(EmpresaRepository repository, CodigoEmpresaGenerator codigoGenerator) {
        this(repository, new com.example.efficientia.empresa.persistence.InMemoryEmpresaAdminRepository(), codigoGenerator, null, null, null);
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

        String nomeFantasiaLimpo = request.nomeFantasia() != null && !request.nomeFantasia().isBlank()
                ? request.nomeFantasia().trim()
                : nomeEmpresaLimpo;
        String telefoneLimpo = request.telefone() != null ? request.telefone().trim() : null;
        String logoUrl = request.logoUrl() != null ? request.logoUrl().trim() : null;

        Integer enderecoIdFinal = request.enderecoId();
        if (request.endereco() != null) {
            EnderecoDto endDto = request.endereco();
            EnderecoDto salvo = salvarOuAtualizarEndereco(
                    enderecoIdFinal,
                    endDto.cep(),
                    endDto.logradouro(),
                    endDto.numero(),
                    endDto.cidade(),
                    endDto.estado() != null ? endDto.estado() : endDto.uf()
            );
            if (salvo != null) {
                enderecoIdFinal = salvo.id();
            }
        } else if (request.cep() != null || request.logradouro() != null || request.cidade() != null) {
            EnderecoDto salvo = salvarOuAtualizarEndereco(
                    enderecoIdFinal,
                    request.cep(),
                    request.logradouro(),
                    request.numero(),
                    request.cidade(),
                    request.estado()
            );
            if (salvo != null) {
                enderecoIdFinal = salvo.id();
            }
        }

        Empresa empresa = new Empresa(
                null,
                enderecoIdFinal,
                codigo,
                nomeEmpresaLimpo,
                nomeFantasiaLimpo,
                razaoSocialLimpa,
                cnpjLimpo,
                emailLimpo,
                telefoneLimpo,
                logoUrl,
                1,
                false,
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
        String identificador = request.cnpj() != null ? request.cnpj().trim() : "";
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

    public EmpresaResponse atualizarDadosComplementares(Long empresaId, AtualizarDadosEmpresaRequest request) {
        return atualizarDadosComplementares(empresaId, request, null);
    }

    public EmpresaResponse atualizarDadosComplementares(Long empresaId, AtualizarDadosEmpresaRequest request, Long adminLogadoId) {
        Empresa empresa = repository.buscarPorId(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o ID informado."));
        return executarAtualizacaoDados(empresa, request);
    }

    public EmpresaResponse atualizarDadosComplementaresPorCodigo(String codigo, AtualizarDadosEmpresaRequest request) {
        if (codigo == null || codigo.isBlank()) {
            throw new CadastroInvalidoException("Código de empresa não informado.");
        }
        Empresa empresa = repository.buscarPorCodigo(codigo.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o código informado."));
        return executarAtualizacaoDados(empresa, request);
    }

    private EmpresaResponse executarAtualizacaoDados(Empresa empresa, AtualizarDadosEmpresaRequest request) {
        if (request == null) {
            return toResponse(empresa);
        }

        if (request.nomeFantasia() != null && !request.nomeFantasia().isBlank()) {
            String nomeLimpo = request.nomeFantasia().trim();
            empresa.setNomeFantasia(nomeLimpo);
            empresa.setNomeEmpresa(nomeLimpo);
        }

        if (request.razaoSocial() != null && !request.razaoSocial().isBlank()) {
            empresa.setRazaoSocial(request.razaoSocial().trim());
        }

        if (request.cnpj() != null && !request.cnpj().isBlank()) {
            String cnpjLimpo = sanitizarCnpj(request.cnpj());
            if (cnpjLimpo.length() != 14) {
                throw new CadastroInvalidoException("CNPJ inválido. Deve conter 14 dígitos numéricos.");
            }
            if (!cnpjLimpo.equalsIgnoreCase(empresa.getCnpj())) {
                if (repository.existePorCnpj(cnpjLimpo)) {
                    throw new CadastroDuplicadoException("Já existe uma empresa cadastrada com o CNPJ informado.");
                }
                empresa.setCnpj(cnpjLimpo);
            }
        }

        if (request.emailCorporativo() != null && !request.emailCorporativo().isBlank()) {
            String emailLimpo = request.emailCorporativo().trim().toLowerCase(Locale.ROOT);
            if (!emailLimpo.equalsIgnoreCase(empresa.getEmailCorporativo())) {
                if (repository.existePorEmail(emailLimpo)) {
                    throw new CadastroDuplicadoException("Já existe uma empresa cadastrada com o e-mail corporativo informado.");
                }
                empresa.setEmailCorporativo(emailLimpo);
            }
        }

        if (request.telefone() != null && !request.telefone().isBlank()) {
            empresa.setTelefone(request.telefone().trim());
        }

        if (request.logoUrl() != null && !request.logoUrl().isBlank()) {
            empresa.setLogoUrl(request.logoUrl().trim());
        }

        Integer enderecoIdAtual = empresa.getEnderecoId();
        if (request.endereco() != null) {
            EnderecoDto endDto = request.endereco();
            EnderecoDto salvo = salvarOuAtualizarEndereco(
                    enderecoIdAtual,
                    endDto.cep(),
                    endDto.logradouro(),
                    endDto.numero(),
                    endDto.cidade(),
                    endDto.estado() != null ? endDto.estado() : endDto.uf()
            );
            if (salvo != null) {
                empresa.setEnderecoId(salvo.id());
            }
        } else if (request.cep() != null || request.logradouro() != null || request.cidade() != null || request.estado() != null || request.numero() != null) {
            EnderecoDto salvo = salvarOuAtualizarEndereco(
                    enderecoIdAtual,
                    request.cep(),
                    request.logradouro(),
                    request.numero(),
                    request.cidade(),
                    request.estado()
            );
            if (salvo != null) {
                empresa.setEnderecoId(salvo.id());
            }
        }

        empresa.setEtapaCadastro(2);
        empresa.setAtualizadoEm(Instant.now());

        Empresa salva = repository.salvar(empresa);
        log.info("Dados cadastrais complementares atualizados com sucesso: Empresa ID={}, Código={}, Telefone={}",
                salva.getId(), salva.getCodigoEmpresa(), salva.getTelefone());

        return toResponse(salva);
    }

    public UploadLogoResponse atualizarLogo(Long empresaId, MultipartFile arquivo) {
        Empresa empresa = repository.buscarPorId(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o ID informado."));

        if (arquivo == null || arquivo.isEmpty()) {
            throw new CadastroInvalidoException("O arquivo de logotipo é obrigatório.");
        }

        long maxBytes = 5L * 1024 * 1024;
        if (arquivo.getSize() > maxBytes) {
            throw new CadastroInvalidoException("O arquivo de logotipo excede o limite máximo permitido de 5 MB.");
        }

        String contentType = arquivo.getContentType();
        String nomeOriginal = arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        boolean isPng = "image/png".equalsIgnoreCase(contentType) || nomeOriginal.endsWith(".png");
        boolean isSvg = "image/svg+xml".equalsIgnoreCase(contentType) || "image/svg".equalsIgnoreCase(contentType) || nomeOriginal.endsWith(".svg");

        if (!isPng && !isSvg) {
            throw new CadastroInvalidoException("Formato de arquivo inválido. Formatos aceitos: PNG ou SVG (até 5 MB).");
        }

        String mimeTypeFinal = isSvg ? "image/svg+xml" : "image/png";

        try {
            byte[] bytes = arquivo.getBytes();
            logosEmMemoria.put(empresaId, bytes);
            mimeTypesLogo.put(empresaId, mimeTypeFinal);

            if (storageService != null) {
                try (InputStream is = new ByteArrayInputStream(bytes)) {
                    storageService.salvar(UUID.randomUUID(), nomeOriginal, mimeTypeFinal, is);
                }
            }
        } catch (IOException e) {
            log.error("Erro ao processar bytes do arquivo de logotipo para empresa ID {}", empresaId, e);
            throw new CadastroInvalidoException("Falha ao processar o arquivo de logotipo enviado.");
        }

        String logoUrl = "/api/v1/empresas/" + empresaId + "/logo/conteudo";
        empresa.setLogoUrl(logoUrl);
        empresa.setAtualizadoEm(Instant.now());
        repository.salvar(empresa);

        log.info("Logo atualizada com sucesso para empresa ID {}: URL={}, tamanho={}", empresaId, logoUrl, arquivo.getSize());
        return new UploadLogoResponse(logoUrl, "Logo da empresa enviada com sucesso.", arquivo.getSize(), mimeTypeFinal);
    }

    public byte[] obterConteudoLogo(Long empresaId) {
        repository.buscarPorId(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada para o ID informado."));

        byte[] bytes = logosEmMemoria.get(empresaId);
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Logotipo não encontrado para a empresa informada.");
        }
        return bytes;
    }

    public String obterMimeTypeLogo(Long empresaId) {
        return mimeTypesLogo.getOrDefault(empresaId, "image/png");
    }

    private EnderecoDto salvarOuAtualizarEndereco(Integer enderecoIdExistente, String cep, String logradouro, String numero, String cidade, String estado) {
        if ((cep == null || cep.isBlank()) && (logradouro == null || logradouro.isBlank()) && (cidade == null || cidade.isBlank())) {
            return null;
        }
        String cepLimpo = cep != null ? cep.replaceAll("[^0-9]", "") : "";
        String logradouroLimpo = logradouro != null ? logradouro.trim() : "";
        String numeroLimpo = numero != null && !numero.isBlank() ? numero.trim() : "S/N";
        String cidadeLimpa = cidade != null ? cidade.trim() : "";
        String estadoLimpo = estado != null ? estado.trim().toUpperCase(Locale.ROOT) : "";

        if (enderecoRepository != null) {
            EnderecoEntity entity = null;
            if (enderecoIdExistente != null) {
                entity = enderecoRepository.findById(enderecoIdExistente).orElse(null);
            }
            if (entity == null) {
                entity = new EnderecoEntity();
            }
            entity.setCep(cepLimpo);
            entity.setLogradouro(logradouroLimpo);
            entity.setNumero(numeroLimpo);
            entity.setCidade(cidadeLimpa);
            entity.setEstado(estadoLimpo.length() > 2 ? estadoLimpo.substring(0, 2) : estadoLimpo);
            EnderecoEntity salvo = enderecoRepository.save(entity);
            EnderecoDto dto = new EnderecoDto(salvo.getId(), salvo.getCep(), salvo.getLogradouro(), salvo.getNumero(), salvo.getCidade(), salvo.getEstado());
            enderecosEmMemoria.put(salvo.getId(), dto);
            return dto;
        }

        int id = enderecoIdExistente != null ? enderecoIdExistente : enderecoIdSeq.getAndIncrement();
        EnderecoDto dto = new EnderecoDto(id, cepLimpo, logradouroLimpo, numeroLimpo, cidadeLimpa, estadoLimpo);
        enderecosEmMemoria.put(id, dto);
        return dto;
    }

    private EnderecoDto buscarEndereco(Integer enderecoId) {
        if (enderecoId == null) {
            return null;
        }
        EnderecoDto emMemoria = enderecosEmMemoria.get(enderecoId);
        if (emMemoria != null) {
            return emMemoria;
        }
        if (enderecoRepository != null) {
            return enderecoRepository.findById(enderecoId)
                    .map(e -> {
                        EnderecoDto dto = new EnderecoDto(e.getId(), e.getCep(), e.getLogradouro(), e.getNumero(), e.getCidade(), e.getEstado());
                        enderecosEmMemoria.put(e.getId(), dto);
                        return dto;
                    })
                    .orElse(null);
        }
        return null;
    }

    public EmpresaResponse toResponse(Empresa empresa) {
        long totalAdmins = adminRepository.contarPorEmpresaId(empresa.getId());
        boolean pendenteAdmin = totalAdmins == 0;

        String status = pendenteAdmin ? "PENDENTE_PRIMEIRO_ADMIN" : "ATIVO";
        String proximoPasso = pendenteAdmin ? "CADASTRO_PRIMEIRO_ADMIN" : "PAINEL_ADMINISTRATIVO";
        String mensagem = pendenteAdmin
                ? "Empresa registrada com sucesso. O cadastro do primeiro administrador é obrigatório para liberar o acesso ao sistema."
                : "Empresa ativa e operacional.";

        EnderecoDto endereco = buscarEndereco(empresa.getEnderecoId());
        String cep = endereco != null ? endereco.cep() : null;
        String logradouro = endereco != null ? endereco.logradouro() : null;
        String numero = endereco != null ? endereco.numero() : null;
        String cidade = endereco != null ? endereco.cidade() : null;
        String estado = endereco != null ? endereco.estado() : null;
        String uf = endereco != null ? endereco.uf() : null;

        return new EmpresaResponse(
                empresa.getId(),
                empresa.getCodigoEmpresa(),
                empresa.getCodigoEmpresa(),
                empresa.getCodigoEmpresa(),
                empresa.getNomeEmpresa(),
                empresa.getNomeEmpresa(),
                empresa.getNomeFantasia(),
                empresa.getRazaoSocial(),
                empresa.getCnpj(),
                empresa.getEmailCorporativo(),
                empresa.getEmailCorporativo(),
                empresa.getTelefone(),
                empresa.getEnderecoId(),
                endereco,
                cep,
                logradouro,
                numero,
                cidade,
                estado,
                uf,
                empresa.getLogoUrl(),
                empresa.getEtapaCadastro(),
                empresa.getCadastroCompleto(),
                status,
                pendenteAdmin,
                proximoPasso,
                mensagem,
                empresa.getCriadoEm(),
                empresa.getAtualizadoEm()
        );
    }
}
