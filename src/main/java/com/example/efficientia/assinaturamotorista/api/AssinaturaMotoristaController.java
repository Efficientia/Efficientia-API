package com.example.efficientia.assinaturamotorista.api;

import com.example.efficientia.assinaturamotorista.api.AssinaturaMotoristaContracts.AssinaturaMetadadosRequest;
import com.example.efficientia.assinaturamotorista.api.AssinaturaMotoristaContracts.AssinaturaMotoristaResponse;
import com.example.efficientia.assinaturamotorista.service.AssinaturaMotoristaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

@Tag(name = "Assinatura Motorista", description = "Endpoints de gestão da assinatura fixa do motorista (PNG imutável com versionamento e auditoria)")
@RestController
@RequestMapping("/api/v1/usuarios")
public class AssinaturaMotoristaController {

    private final AssinaturaMotoristaService service;

    public AssinaturaMotoristaController(AssinaturaMotoristaService service) {
        this.service = service;
    }

    // =========================================================================
    // 1. ROTAS DO PRÓPRIO MOTORISTA (/me)
    // =========================================================================

    @Operation(
            summary = "Cadastro ou atualização da própria assinatura fixa pelo motorista autenticado",
            description = "Recebe arquivo PNG de até 1 MB com metadados (DESENHO ou NOME_DIGITADO). " +
                    "Substitui transacionalmente a assinatura ativa anterior mantendo o versionamento imutável."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assinatura cadastrada ou atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Metadados ou cabeçalho Idempotency-Key ausente/inválido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado ou token expirado"),
            @ApiResponse(responseCode = "403", description = "Usuário autenticado não possui o papel de motorista"),
            @ApiResponse(responseCode = "413", description = "Arquivo de assinatura excede o limite máximo de 1 MB"),
            @ApiResponse(responseCode = "415", description = "Arquivo não possui formato de imagem PNG válido (magic bytes)")
    })
    @PutMapping(value = "/me/assinatura", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssinaturaMotoristaResponse> cadastrarMinhaAssinatura(
            @RequestHeader(name = "Idempotency-Key") UUID idempotencyKey,
            @RequestPart("arquivo") MultipartFile arquivo,
            @RequestPart("metadados") @Valid AssinaturaMetadadosRequest metadados,
            Authentication authentication
    ) {
        validarPerfilMotorista(authentication);
        Integer usuarioId = extrairUsuarioId(authentication);

        byte[] bytes = extrairBytes(arquivo);
        AssinaturaMotoristaResponse response = service.salvarOuAtualizarAssinatura(
                usuarioId,
                usuarioId,
                idempotencyKey,
                metadados,
                bytes,
                true
        );

        return ResponseEntity.ok(response);
    }
    @Operation(
            summary = "Consulta metadados da assinatura ativa do motorista autenticado",
            description = "Retorna os metadados da assinatura ativa do próprio motorista obtido a partir do JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metadados da assinatura ativa retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Usuário autenticado não é motorista"),
            @ApiResponse(responseCode = "404", description = "Motorista ainda não possui assinatura fixa cadastrada")
    })
    @GetMapping("/me/assinatura")
    public ResponseEntity<AssinaturaMotoristaResponse> buscarMinhaAssinatura(Authentication authentication) {
        validarPerfilMotorista(authentication);
        Integer usuarioId = extrairUsuarioId(authentication);
        return ResponseEntity.ok(service.buscarAssinaturaAtiva(usuarioId, true));
    }
    @Operation(
            summary = "Transmite o arquivo PNG da assinatura do motorista autenticado",
            description = "Retorna os bytes da imagem PNG com Content-Disposition inline e Cache-Control private, no-store."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem PNG transmitida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Usuário autenticado não é motorista"),
            @ApiResponse(responseCode = "404", description = "Assinatura ativa não encontrada")
    })
    @GetMapping("/me/assinatura/conteudo")
    public ResponseEntity<byte[]> buscarMeuConteudoAssinatura(Authentication authentication) {
        validarPerfilMotorista(authentication);
        Integer usuarioId = extrairUsuarioId(authentication);
        byte[] conteudo = service.buscarConteudoAssinaturaAtiva(usuarioId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/png")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"assinatura.png\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(conteudo);
    }

    // =========================================================================
    // 2. ROTAS ADMINISTRATIVAS ({usuarioId})
    // =========================================================================

    @Operation(
            summary = "Cadastro administrativo da assinatura fixa no perfil de um motorista",
            description = "Permite que administradores cadastrem a assinatura de um motorista antes do primeiro login."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assinatura cadastrada ou atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Metadados ou cabeçalho Idempotency-Key inválido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores"),
            @ApiResponse(responseCode = "413", description = "Arquivo excede o limite máximo de 1 MB"),
            @ApiResponse(responseCode = "415", description = "Arquivo não é PNG válido"),
            @ApiResponse(responseCode = "422", description = "O usuário de destino não possui perfil de motorista")
    })
    @PutMapping(value = "/{motoristaId}/assinatura", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssinaturaMotoristaResponse> cadastrarAssinaturaAdministrativa(
            @PathVariable Integer motoristaId,
            @RequestHeader(name = "Idempotency-Key") UUID idempotencyKey,
            @RequestPart("arquivo") MultipartFile arquivo,
            @RequestPart("metadados") @Valid AssinaturaMetadadosRequest metadados,
            Authentication authentication
    ) {
        validarPerfilAdministrador(authentication);
        Integer criadoPorId = extrairUsuarioId(authentication);

        byte[] bytes = extrairBytes(arquivo);
        AssinaturaMotoristaResponse response = service.salvarOuAtualizarAssinatura(
                motoristaId,
                criadoPorId,
                idempotencyKey,
                metadados,
                bytes,
                false
        );

        return ResponseEntity.ok(response);
    }
    @Operation(
            summary = "Consulta metadados da assinatura de um motorista por administradores/analistas",
            description = "Permite consultar metadados da assinatura fixa para visualização em relatórios web."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metadados da assinatura encontrados"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a administradores e analistas"),
            @ApiResponse(responseCode = "404", description = "Assinatura ativa não encontrada para o usuário")
    })
    @GetMapping("/{usuarioId}/assinatura")
    public ResponseEntity<AssinaturaMotoristaResponse> buscarAssinaturaPorId(
            @PathVariable Integer usuarioId,
            Authentication authentication
    ) {
        validarAcessoAssinaturaTerceiro(authentication);
        return ResponseEntity.ok(service.buscarAssinaturaAtiva(usuarioId, false));
    }
    @Operation(
            summary = "Transmite o arquivo PNG da assinatura de um motorista para administradores/analistas",
            description = "Transmite o binário PNG com headers inline para exibição no formulário web."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem PNG transmitida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito"),
            @ApiResponse(responseCode = "404", description = "Assinatura ativa não encontrada")
    })
    @GetMapping("/{usuarioId}/assinatura/conteudo")
    public ResponseEntity<byte[]> buscarConteudoAssinaturaPorId(
            @PathVariable Integer usuarioId,
            Authentication authentication
    ) {
        validarAcessoAssinaturaTerceiro(authentication);
        byte[] conteudo = service.buscarConteudoAssinaturaAtiva(usuarioId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/png")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"assinatura.png\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(conteudo);
    }

    // =========================================================================
    // 3. HELPERS DE SEGURANÇA E PARSING
    // =========================================================================

    private Authentication obterAutenticacao(Authentication authentication) {
        if (authentication != null) {
            return authentication;
        }
        Authentication securityAuth = SecurityContextHolder.getContext().getAuthentication();
        if (securityAuth != null && securityAuth.isAuthenticated() && !"anonymousUser".equals(securityAuth.getPrincipal())) {
            return securityAuth;
        }
        return null;
    }

    private Integer extrairUsuarioId(Authentication authentication) {
        Authentication auth = obterAutenticacao(authentication);
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Object usuarioIdClaim = jwtAuth.getToken().getClaim("usuario_id");
            if (usuarioIdClaim instanceof Number n) {
                return n.intValue();
            }
            if (usuarioIdClaim instanceof String s && !s.isBlank()) {
                try {
                    return Integer.parseInt(s.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            Object adminIdClaim = jwtAuth.getToken().getClaim("admin_id");
            if (adminIdClaim instanceof Number n) {
                return n.intValue();
            }

            String sub = jwtAuth.getToken().getSubject();
            if (sub != null && !sub.isBlank()) {
                try {
                    return Integer.parseInt(sub.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não foi possível extrair o identificador do usuário do token.");
    }

    private void validarPerfilMotorista(Authentication authentication) {
        Authentication auth = obterAutenticacao(authentication);
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado.");
        }
        boolean isMotorista = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_MOTORISTA"));

        if (!isMotorista) {
            throw new AccessDeniedException("Acesso restrito ao perfil de motorista.");
        }
    }

    private void validarPerfilAdministrador(Authentication authentication) {
        Authentication auth = obterAutenticacao(authentication);
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado.");
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                        || a.getAuthority().equalsIgnoreCase("ROLE_ADMINISTRADOR"));

        if (!isAdmin) {
            throw new AccessDeniedException("Acesso restrito a administradores.");
        }
    }

    private void validarAcessoAssinaturaTerceiro(Authentication authentication) {
        Authentication auth = obterAutenticacao(authentication);
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado.");
        }
        boolean temAcesso = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                        || a.getAuthority().equalsIgnoreCase("ROLE_ADMINISTRADOR")
                        || a.getAuthority().equalsIgnoreCase("ROLE_ANALISTA")
                        || a.getAuthority().equalsIgnoreCase("ROLE_FUNCIONARIO_FRIBOI"));

        if (!temAcesso) {
            throw new AccessDeniedException("Usuários comuns não possuem permissão para consultar assinaturas de terceiros.");
        }
    }

    private byte[] extrairBytes(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new AssinaturaFormatoInvalidoException("O arquivo de assinatura é obrigatório e não pode estar vazio.");
        }
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new AssinaturaFormatoInvalidoException("Falha ao ler os bytes do arquivo de assinatura.");
        }
    }
}
