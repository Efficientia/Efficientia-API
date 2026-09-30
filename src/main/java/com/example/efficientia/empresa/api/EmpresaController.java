package com.example.efficientia.empresa.api;

import com.example.efficientia.empresa.api.EmpresaContracts.AtualizarDadosEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.CriarEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.EmpresaResponse;
import com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaResponse;
import com.example.efficientia.empresa.api.EmpresaContracts.UploadLogoResponse;
import com.example.efficientia.empresa.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PutMapping({"/api/v1/empresas/{id}", "/api/v1/empresas/{id}/dados-complementares", "/api/v1/empresas/{id}/etapa-1"})
    @Operation(
            summary = "Atualiza dados cadastrais da empresa (Etapa 1 de 3 - Onboarding)",
            description = "Insere ou atualiza informações complementares após o cadastro corporativo e login do primeiro administrador: " +
                    "CNPJ, Razão Social, Nome Fantasia, E-mail Corporativo, Telefone, Endereço completo (CEP, Logradouro, Número, Cidade, UF) e URL de logotipo. " +
                    "Avança o progresso cadastral para a etapa 2."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados da empresa atualizados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada"),
            @ApiResponse(responseCode = "409", description = "CNPJ ou e-mail corporativo em duplicidade")
    })
    public EmpresaResponse atualizarDadosComplementares(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarDadosEmpresaRequest request
    ) {
        return service.atualizarDadosComplementares(id, request);
    }

    @PatchMapping({"/api/v1/empresas/{id}", "/api/v1/empresas/{id}/dados-complementares", "/api/v1/empresas/{id}/etapa-1"})
    @Operation(summary = "Atualização parcial de dados cadastrais da empresa")
    public EmpresaResponse atualizarParcialDados(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarDadosEmpresaRequest request
    ) {
        return service.atualizarDadosComplementares(id, request);
    }

    @PutMapping({"/api/v1/empresas/codigo/{codigo}", "/api/v1/empresas/codigo/{codigo}/dados-complementares"})
    @Operation(
            summary = "Atualiza dados cadastrais da empresa através do código corporativo",
            description = "Permite salvar os dados da Etapa 1 utilizando diretamente o código corporativo de 8 dígitos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados atualizados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Código inválido"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    public EmpresaResponse atualizarDadosPorCodigo(
            @PathVariable String codigo,
            @Valid @RequestBody AtualizarDadosEmpresaRequest request
    ) {
        return service.atualizarDadosComplementaresPorCodigo(codigo, request);
    }

    @PostMapping(
            value = {"/api/v1/empresas/{id}/logo", "/api/v1/empresas/{id}/logo-upload"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Upload de logotipo da empresa (PNG ou SVG, até 5 MB)",
            description = "Recebe o arquivo de logotipo da empresa nos formatos PNG ou SVG, com tamanho máximo de até 5 MB, " +
                    "armazenando o arquivo e vinculando a URL pública correspondente aos dados da empresa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logotipo enviado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido, formato incompatível ou acima de 5 MB"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    public UploadLogoResponse uploadLogo(
            @PathVariable Long id,
            @Parameter(description = "Arquivo de imagem PNG ou SVG de até 5 MB")
            @RequestParam(value = "arquivo", required = false) MultipartFile arquivo,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "logo", required = false) MultipartFile logo
    ) {
        MultipartFile upload = arquivo != null ? arquivo : (file != null ? file : logo);
        return service.atualizarLogo(id, upload);
    }

    @GetMapping("/api/v1/empresas/{id}/logo/conteudo")
    @Operation(
            summary = "Download / Visualização direta do logotipo da empresa",
            description = "Retorna os bytes binários da logo com o cabeçalho Content-Type correspondente (image/png ou image/svg+xml) para exibição direta em tags <img> ou aplicativos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Empresa ou logotipo não encontrado")
    })
    public ResponseEntity<byte[]> obterLogo(@PathVariable Long id) {
        byte[] bytes = service.obterConteudoLogo(id);
        String mimeType = service.obterMimeTypeLogo(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(bytes);
    }
}
