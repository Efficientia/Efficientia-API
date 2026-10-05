package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.relatorioviagem.service.RelatorioViagemService;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoRelatorioResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.VincularCaminhaoRelatorioRequest;
import com.example.efficientia.caminhao.service.CaminhaoService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/relatorios-viagem")
public class RelatorioViagemController {
    private final RelatorioViagemService service;
    private final CaminhaoService caminhaoService;

    public RelatorioViagemController(RelatorioViagemService service, CaminhaoService caminhaoService) {
        this.service = service;
        this.caminhaoService = caminhaoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RelatorioViagemResponse criar(@Valid @org.springframework.web.bind.annotation.RequestBody CriarRelatorioViagemRequest request) {
        return service.criar(request);
    }

    @GetMapping
    public RelatorioViagemPageResponse listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho
    ) {
        return service.listar(pagina, tamanho);
    }

    @GetMapping("/{id}")
    public RelatorioViagemResponse buscar(@PathVariable Integer id) {
        return service.buscar(id);
    }

    @GetMapping("/{id}/caminhao")
    public CaminhaoRelatorioResponse buscarCaminhao(@PathVariable Integer id) {
        return caminhaoService.buscarCaminhaoDoRelatorio(id);
    }

    @PostMapping("/{id}/vincular-caminhao")
    public CaminhaoRelatorioResponse vincularCaminhao(
            @PathVariable Integer id,
            @Valid @RequestBody VincularCaminhaoRelatorioRequest request
    ) {
        return caminhaoService.vincularCaminhaoAoRelatorio(id, request);
    }

    @PatchMapping("/{id}/finalizar")
    public RelatorioViagemResponse finalizar(@PathVariable Integer id) {
        return service.finalizar(id);
    }

    @PatchMapping("/{id}/status")
    public RelatorioViagemResponse atualizarStatus(
            @PathVariable Integer id,
            @RequestParam String status
    ) {
        return service.atualizarStatus(id, status);
    }

    @PatchMapping("/{id}/assinaturas")
    public RelatorioViagemResponse registrarAssinaturas(
            @PathVariable Integer id,
            @RequestBody RegistrarAssinaturasRequest request
    ) {
        return service.registrarAssinaturas(id, request);
    }

    @PostMapping("/{id}/assinar-papel")
    public RelatorioViagemResponse registrarAssinaturaPapel(
            @PathVariable Integer id,
            @RequestParam String papel,
            @RequestParam(required = false) String urlAssinatura
    ) {
        return service.registrarAssinaturaPapel(id, papel, urlAssinatura);
    }

    @PatchMapping("/{id}/enviar")
    public RelatorioViagemResponse enviarParaAnalise(@PathVariable Integer id) {
        return service.enviarParaAnalise(id);
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
    public org.springframework.http.ProblemDetail assinaturasIncompletas(AssinaturasIncompletasException ex) {
        org.springframework.http.ProblemDetail problem = org.springframework.http.ProblemDetail.forStatusAndDetail(
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
}
