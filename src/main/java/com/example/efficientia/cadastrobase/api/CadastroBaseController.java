package com.example.efficientia.cadastrobase.api;

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

    @PostMapping("/api/v1/veiculos/carretas")
    @ResponseStatus(HttpStatus.CREATED)
    public CarretaResponse criarCarreta(@Valid @RequestBody CriarCarretaRequest request) {
        return service.criarCarreta(request);
    }
}
