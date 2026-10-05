package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoRelatorioResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.VincularCaminhaoRelatorioRequest;
import com.example.efficientia.caminhao.service.CaminhaoService;
import com.example.efficientia.relatorioviagem.service.RelatorioViagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Relatórios de Viagem", description = "Endpoints do fluxo de 6 etapas do diário de rota mobile, rascunhos, submissões e auditoria")
@RestController
@RequestMapping("/api/v1/relatorios-viagem")
public class RelatorioViagemController {
    private static final String EXEMPLO_SUBMISSAO = "{\"fazendaId\":12,\"unidadeFrigorificaId\":4,\"numeroGta\":\"GTA-2026-0042\",\"numeroNotaFiscal\":\"NF-873\",\"dataEmbarque\":\"2026-10-04\",\"horarioEmbarque\":\"08:00:00\",\"horarioSaidaPropriedade\":\"08:30:00\",\"kmSaidaEmbarcadouro\":18200,\"dataChegadaUnidade\":\"2026-10-04\",\"horarioChegadaUnidade\":\"11:15:00\",\"horarioDesembarque\":\"11:40:00\",\"kmChegadaDesembarcadouro\":18325,\"numeroCurral\":\"C-07\",\"sireneReFuncionou\":true,\"quantidadeMachos\":20,\"quantidadeFemeas\":15,\"quantidadeMarrucos\":5,\"quantidadeEmPe\":38,\"quantidadeDeitado\":1,\"quantidadeMorto\":1,\"quantidadeEmergencia\":0,\"paradasImprevistas\":[],\"anomaliasEmbarque\":[],\"anomaliasDesembarque\":[],\"status\":\"pendente\"}";
    private static final String EXEMPLO_RETOMADA = "{\"comentarios\":\"Parada registrada no km 18260\",\"paradasImprevistas\":[{\"motivo\":\"problema_mecanico\",\"dataHoraInicio\":\"2026-10-04T09:10:00\",\"dataHoraFim\":\"2026-10-04T09:25:00\"}]}";
    private final RelatorioViagemService service;
    private final CaminhaoService caminhaoService;

    public RelatorioViagemController(RelatorioViagemService service, CaminhaoService caminhaoService) {
        this.service = service;
        this.caminhaoService = caminhaoService;
    }

    @Operation(summary = "Criar relatório de viagem (rascunho ou submissão)",
            description = "Recebe dados do diário de rota. Relaciona motorista e empresa via JWT. Suporta Idempotency-Key.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Campos parciais para rascunho ou formulário completo para submissão pendente.",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CriarRelatorioViagemRequest.class),
                            examples = @ExampleObject(name = "Submissão", value = EXEMPLO_SUBMISSAO))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Relatório criado com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RelatorioViagemResponse.class),
                            examples = @ExampleObject(name = "Diário salvo",
                                    value = "{\"id\":42,\"fazendaId\":12,\"unidadeFrigorificaId\":4,\"motoristaId\":18,\"empresaId\":3,\"numeroGta\":\"GTA-2026-0042\",\"status\":\"rascunho\",\"totalAnimais\":40,\"duracaoViagemMinutos\":195,\"distanciaPercorridaKm\":125,\"paradasImprevistas\":[],\"anomaliasEmbarque\":[],\"anomaliasDesembarque\":[]}"))),
            @ApiResponse(responseCode = "400", description = "Inconsistência nos campos ou dados inválidos",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Erro de validação",
                                    value = "{\"title\":\"Inconsistência no Diário de Rota\",\"status\":400,\"detail\":\"Inconsistência nos dados do diário de rota.\",\"fieldErrors\":{\"kmChegadaDesembarcadouro\":\"O quilômetro de chegada não pode ser inferior ao quilômetro de saída.\"}}"))),
            @ApiResponse(responseCode = "409", description = "Número da GTA duplicado")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RelatorioViagemResponse criar(
            @Valid @RequestBody CriarRelatorioViagemRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
            Authentication authentication
    ) {
        return service.criar(request, authentication, idempotencyKey);
    }

    @Operation(summary = "Atualizar relatório de viagem completo (retomada de rascunho em etapas)",
            description = "Permite atualizar todas as etapas de um diário de rota em andamento (inclusive paradas e anomalias).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Campos enviados atualizam o rascunho; campos omitidos são preservados.",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CriarRelatorioViagemRequest.class),
                            examples = @ExampleObject(name = "Retomada de rascunho", value = EXEMPLO_RETOMADA))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório atualizado com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RelatorioViagemResponse.class),
                            examples = @ExampleObject(name = "Diário atualizado",
                                    value = "{\"id\":42,\"status\":\"rascunho\",\"atualizadoEm\":\"2026-10-04T11:45:00\",\"totalAnimais\":40,\"duracaoViagemMinutos\":195,\"distanciaPercorridaKm\":125}"))),
            @ApiResponse(responseCode = "400", description = "Inconsistência nos dados",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Erro de validação",
                                    value = "{\"title\":\"Campos Inválidos\",\"status\":400,\"detail\":\"Erro de validação nos campos informados.\",\"fieldErrors\":{\"numeroGta\":\"O número da GTA é obrigatório.\"}}"))),
            @ApiResponse(responseCode = "404", description = "Relatório não encontrado")
    })
    @PutMapping("/{id}")
    public RelatorioViagemResponse atualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CriarRelatorioViagemRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
            Authentication authentication
    ) {
        return service.atualizar(id, request, authentication, idempotencyKey);
    }

    @Operation(summary = "Listar relatórios de viagem com paginação")
    @GetMapping
    public RelatorioViagemPageResponse listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho
    ) {
        return service.listar(pagina, tamanho);
    }

    @Operation(summary = "Buscar relatório de viagem por ID com paradas e anomalias completas")
    @GetMapping("/{id}")
    public RelatorioViagemResponse buscar(@PathVariable Integer id) {
        return service.buscar(id);
    }

    @Operation(summary = "Buscar caminhão vinculado ao relatório")
    @GetMapping("/{id}/caminhao")
    public CaminhaoRelatorioResponse buscarCaminhao(@PathVariable Integer id) {
        return caminhaoService.buscarCaminhaoDoRelatorio(id);
    }

    @Operation(summary = "Vincular caminhão ao relatório")
    @PostMapping("/{id}/vincular-caminhao")
    public CaminhaoRelatorioResponse vincularCaminhao(
            @PathVariable Integer id,
            @Valid @RequestBody VincularCaminhaoRelatorioRequest request
    ) {
        return caminhaoService.vincularCaminhaoAoRelatorio(id, request);
    }

    @Operation(summary = "Finalizar relatório de viagem com validação de assinaturas obrigatórias")
    @PatchMapping("/{id}/finalizar")
    public RelatorioViagemResponse finalizar(
            @PathVariable Integer id,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey
    ) {
        return service.finalizar(id, idempotencyKey);
    }

    @Operation(summary = "Atualizar status do relatório de viagem")
    @PatchMapping("/{id}/status")
    public RelatorioViagemResponse atualizarStatus(
            @PathVariable Integer id,
            @RequestParam String status,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey
    ) {
        return service.atualizarStatus(id, status, idempotencyKey);
    }

    @Operation(summary = "Registrar assinaturas em lote no relatório de viagem")
    @PatchMapping("/{id}/assinaturas")
    public RelatorioViagemResponse registrarAssinaturas(
            @PathVariable Integer id,
            @RequestBody RegistrarAssinaturasRequest request
    ) {
        return service.registrarAssinaturas(id, request);
    }

    @Operation(summary = "Registrar assinatura individual por papel (embarque, desembarque, etc.)")
    @PostMapping("/{id}/assinar-papel")
    public RelatorioViagemResponse registrarAssinaturaPapel(
            @PathVariable Integer id,
            @RequestParam String papel,
            @RequestParam(required = false) String urlAssinatura
    ) {
        return service.registrarAssinaturaPapel(id, papel, urlAssinatura);
    }

    @Operation(summary = "Enviar relatório de viagem para análise/auditoria")
    @PatchMapping("/{id}/enviar")
    public RelatorioViagemResponse enviarParaAnalise(
            @PathVariable Integer id,
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey
    ) {
        return service.enviarParaAnalise(id, idempotencyKey);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(RelatorioViagemNotFoundException.class)
    public void relatorioNaoEncontrado() {
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(NumeroGtaDuplicadoException.class)
    public void numeroGtaDuplicado() {
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public void paginacaoInvalida() {
    }

    @ExceptionHandler(AssinaturasIncompletasException.class)
    public ProblemDetail assinaturasIncompletas(AssinaturasIncompletasException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problem.setTitle("Assinaturas Obrigatórias Incompletas");
        problem.setProperty("relatorioId", ex.getRelatorioId());
        problem.setProperty("qtdObrigatoria", ex.getQtdObrigatoria());
        problem.setProperty("qtdColetadas", ex.getQtdColetadas());
        problem.setProperty("papeisFaltantes", ex.getPapeisFaltantes());
        return problem;
    }

    @ExceptionHandler(ValidacaoDiarioRotaException.class)
    public ProblemDetail handleValidacaoDiarioRota(ValidacaoDiarioRotaException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Inconsistência no Diário de Rota");
        problem.setProperty("fieldErrors", ex.getFieldErrors());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new java.util.TreeMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Erro de validação nos campos informados."
        );
        problem.setTitle("Campos Inválidos");
        problem.setProperty("fieldErrors", errors);
        return problem;
    }
}
