package com.example.efficientia.empresa.api;

import com.example.efficientia.empresa.api.EmpresaContracts.CriarEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.EmpresaResponse;
import com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaResponse;
import com.example.efficientia.empresa.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Empresas", description = "Cadastro, autenticação corporativa e gerenciamento de empresas parceiras")
public class EmpresaController {

    private final EmpresaService service;

    public EmpresaController(EmpresaService service) {
        this.service = service;
    }

    @PostMapping({"/api/v1/empresas", "/api/v1/auth/empresas"})
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastra uma nova empresa",
            description = "Cadastra a empresa com nome, razão social, CNPJ e e-mail corporativo. Gera automaticamente um código de 8 dígitos (3 letras + 5 números aleatórios) para acesso e identificação dos funcionários. Logo após, é de total obrigatoriedade o cadastro de um administrador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empresa cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou formato de CNPJ incorreto"),
            @ApiResponse(responseCode = "409", description = "CNPJ ou e-mail corporativo já cadastrado")
    })
    public EmpresaResponse cadastrar(@Valid @RequestBody CriarEmpresaRequest request) {
        return service.cadastrarEmpresa(request);
    }

    @PostMapping({"/api/v1/auth/empresa/login", "/api/v1/auth/empresas/login", "/api/v1/empresas/login"})
    @Operation(
            summary = "Login / Verificação de status da Empresa",
            description = "Valida o acesso da empresa por CNPJ, e-mail corporativo ou código de 8 dígitos. Caso a empresa não possua nenhum administrador cadastrado, sinaliza a obrigatoriedade imediata do primeiro acesso para prosseguir."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa identificada / autenticada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros obrigatórios ausentes"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    public LoginEmpresaResponse loginEmpresa(@Valid @RequestBody LoginEmpresaRequest request) {
        return service.autenticarEmpresa(request);
    }

    @GetMapping("/api/v1/empresas")
    @Operation(summary = "Lista as empresas cadastradas")
    public List<EmpresaResponse> listar() {
        return service.listarEmpresas();
    }

    @GetMapping("/api/v1/empresas/{id}")
    @Operation(summary = "Busca uma empresa por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa encontrada"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    public EmpresaResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/api/v1/empresas/codigo/{codigo}")
    @Operation(summary = "Busca uma empresa pelo código de 8 dígitos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa encontrada"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    public EmpresaResponse buscarPorCodigo(@PathVariable String codigo) {
        return service.buscarPorCodigo(codigo);
    }

    @GetMapping("/api/v1/empresas/cnpj/{cnpj}")
    @Operation(summary = "Busca uma empresa pelo CNPJ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa encontrada"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    public EmpresaResponse buscarPorCnpj(@PathVariable String cnpj) {
        return service.buscarPorCnpj(cnpj);
    }
}
