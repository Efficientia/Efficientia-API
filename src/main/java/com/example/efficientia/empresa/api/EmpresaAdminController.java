package com.example.efficientia.empresa.api;

import com.example.efficientia.empresa.api.EmpresaAdminContracts.AdminResponse;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.CriarAdminRequest;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.CriarFuncionarioEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.CriarPrimeiroAdminRequest;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.FuncionarioEmpresaResponse;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.LoginAdminRequest;
import com.example.efficientia.empresa.api.EmpresaAdminContracts.LoginAdminResponse;
import com.example.efficientia.empresa.service.EmpresaAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Administradores e Gestão Corporativa", description = "Autenticação, adesão de administradores e gestão de funcionários por empresa")
public class EmpresaAdminController {

    private final EmpresaAdminService service;

    public EmpresaAdminController(EmpresaAdminService service) {
        this.service = service;
    }

    @PostMapping({"/api/v1/auth/adm/login", "/api/v1/auth/login/adm", "/api/v1/adm/login"})
    @Operation(
            summary = "Login do Administrador da Empresa",
            description = "Autentica o administrador por e-mail (ou CPF) e senha, retornando o token JWT Bearer com perfil de ADMIN e os dados corporativos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros obrigatórios ausentes"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou administrador inativo")
    })
    public LoginAdminResponse login(@Valid @RequestBody LoginAdminRequest request) {
        return service.autenticarAdmin(request);
    }

    @PostMapping({"/api/v1/auth/adm/primeiro-acesso", "/api/v1/adm/primeiro-acesso", "/api/v1/empresas/{empresaId}/primeiro-admin"})
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Primeiro acesso do Administrador (onboarding obrigatório)",
            description = "Cadastra o primeiro administrador da empresa logo após a inserção dos dados corporativos (nome, razão social, e-mail empresarial e CNPJ). " +
                    "É de total obrigatoriedade para desbloquear a empresa e liberar o acesso com emissão imediata de token JWT. Bloqueado se a empresa já possuir administradores."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Primeiro administrador criado com acesso liberado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "A empresa já possui administradores cadastrados"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada"),
            @ApiResponse(responseCode = "409", description = "E-mail ou CPF já cadastrado")
    })
    public LoginAdminResponse cadastrarPrimeiroAdmin(@Valid @RequestBody CriarPrimeiroAdminRequest request) {
        return service.cadastrarPrimeiroAdmin(request);
    }

    @PostMapping("/api/v1/empresas/{empresaId}/adms")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Adesão de novos administradores à empresa",
            description = "Cadastra um novo administrador vinculado à empresa. Operação protegida, restrita exclusivamente a administradores autenticados da mesma empresa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Novo administrador cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: apenas administradores da própria empresa podem aderir novos administradores"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada"),
            @ApiResponse(responseCode = "409", description = "E-mail ou CPF já cadastrado")
    })
    public AdminResponse aderirNovoAdmin(
            @PathVariable Long empresaId,
            @Valid @RequestBody CriarAdminRequest request,
            Authentication authentication
    ) {
        Long adminLogadoId = extrairAdminId(authentication);
        return service.aderirNovoAdmin(empresaId, request, adminLogadoId);
    }

    @GetMapping("/api/v1/empresas/{empresaId}/adms")
    @Operation(
            summary = "Listar administradores da empresa",
            description = "Retorna todos os administradores vinculados à empresa para gestão de RH e organograma corporativo. Restrito a administradores da mesma empresa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de administradores recuperada"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    public List<AdminResponse> listarAdminsPorEmpresa(
            @PathVariable Long empresaId,
            Authentication authentication
    ) {
        Long adminLogadoId = extrairAdminId(authentication);
        return service.listarAdminsPorEmpresa(empresaId, adminLogadoId);
    }

    @PostMapping("/api/v1/empresas/{empresaId}/funcionarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastro de funcionários pela empresa",
            description = "Cadastra motoristas, manobristas, analistas, curraleiros ou pecuaristas vinculados à empresa parceira. " +
                    "Associa automaticamente o código da empresa para que o funcionário possa realizar o login no app mobile."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Funcionário cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: apenas administradores da empresa podem cadastrar seus funcionários"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada"),
            @ApiResponse(responseCode = "409", description = "CPF ou e-mail já cadastrado")
    })
    public FuncionarioEmpresaResponse cadastrarFuncionario(
            @PathVariable Long empresaId,
            @Valid @RequestBody CriarFuncionarioEmpresaRequest request,
            Authentication authentication
    ) {
        Long adminLogadoId = extrairAdminId(authentication);
        return service.cadastrarFuncionario(empresaId, request, adminLogadoId);
    }

    @GetMapping("/api/v1/empresas/{empresaId}/funcionarios")
    @Operation(
            summary = "Listar funcionários da empresa",
            description = "Retorna todos os funcionários vinculados à empresa parceira. Restrito a administradores autenticados da mesma empresa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de funcionários recuperada"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    public List<FuncionarioEmpresaResponse> listarFuncionariosPorEmpresa(
            @PathVariable Long empresaId,
            Authentication authentication
    ) {
        Long adminLogadoId = extrairAdminId(authentication);
        return service.listarFuncionariosPorEmpresa(empresaId, adminLogadoId);
    }

    private Long extrairAdminId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            Object adminIdClaim = jwt.getClaim("admin_id");
            if (adminIdClaim instanceof Number n) {
                return n.longValue();
            }
            if (adminIdClaim instanceof String s) {
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException ignored) {
                }
            }
            try {
                return Long.parseLong(jwt.getSubject());
            } catch (NumberFormatException ignored) {
            }
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
