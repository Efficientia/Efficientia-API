package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.relatorioviagem.service.RelatorioViagemService;
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

    public RelatorioViagemController(RelatorioViagemService service) {
        this.service = service;
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
}
