package com.example.efficientia.cadastrobase.api;

import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.AtualizarCarretaRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.AtualizarCavaloRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CarretaDetalhadaResponse;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CavaloDetalhadoResponse;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CarretaResponse;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CavaloResponse;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarCarretaRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarCavaloRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarEnderecoRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarFazendaRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarUsuarioRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.EnderecoResponse;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.FazendaResponse;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.UsuarioResponse;
import com.example.efficientia.cadastrobase.service.CadastroBaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CadastroBaseController {

    private final CadastroBaseService service;

    public CadastroBaseController(CadastroBaseService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criarUsuario(@Valid @RequestBody CriarUsuarioRequest request) {
        return service.criarUsuario(request);
    }

    @PostMapping("/api/v1/enderecos")
    @ResponseStatus(HttpStatus.CREATED)
    public EnderecoResponse criarEndereco(@Valid @RequestBody CriarEnderecoRequest request) {
        return service.criarEndereco(request);
    }

    @PostMapping("/api/v1/fazendas")
    @ResponseStatus(HttpStatus.CREATED)
    public FazendaResponse criarFazenda(@Valid @RequestBody CriarFazendaRequest request) {
        return service.criarFazenda(request);
    }

    @PostMapping("/api/v1/veiculos/cavalos")
    @ResponseStatus(HttpStatus.CREATED)
    public CavaloResponse criarCavalo(@Valid @RequestBody CriarCavaloRequest request) {
        return service.criarCavalo(request);
    }


    @GetMapping("/api/v1/veiculos/cavalos")
    public List<CavaloDetalhadoResponse> listarCavalos() {
        return service.listarCavalos();
    }

    @GetMapping("/api/v1/veiculos/cavalos/{id}")
    public CavaloDetalhadoResponse buscarCavalo(@PathVariable Integer id) {
        return service.buscarCavalo(id);
    }

    @PutMapping("/api/v1/veiculos/cavalos/{id}")
    public CavaloDetalhadoResponse atualizarCavalo(
            @PathVariable Integer id,
            @Valid @RequestBody AtualizarCavaloRequest request
    ) {
        return service.atualizarCavalo(id, request);
    }

    @DeleteMapping("/api/v1/veiculos/cavalos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerCavalo(@PathVariable Integer id) {
        service.removerCavalo(id);
    }
    @PostMapping("/api/v1/veiculos/carretas")
    @ResponseStatus(HttpStatus.CREATED)
    public CarretaResponse criarCarreta(@Valid @RequestBody CriarCarretaRequest request) {
        return service.criarCarreta(request);
    }

    @GetMapping("/api/v1/veiculos/carretas")
    public List<CarretaDetalhadaResponse> listarCarretas() {
        return service.listarCarretas();
    }

    @GetMapping("/api/v1/veiculos/carretas/{id}")
    public CarretaDetalhadaResponse buscarCarreta(@PathVariable Integer id) {
        return service.buscarCarreta(id);
    }

    @PutMapping("/api/v1/veiculos/carretas/{id}")
    public CarretaDetalhadaResponse atualizarCarreta(
            @PathVariable Integer id,
            @Valid @RequestBody AtualizarCarretaRequest request
    ) {
        return service.atualizarCarreta(id, request);
    }

    @DeleteMapping("/api/v1/veiculos/carretas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerCarreta(@PathVariable Integer id) {
        service.removerCarreta(id);
    }
}
