package com.example.efficientia.caminhao.api;

import com.example.efficientia.caminhao.api.CaminhaoContracts.AtualizarCaminhaoRequest;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoAppResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoRelatorioResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CriarCaminhaoRequest;
import com.example.efficientia.caminhao.api.CaminhaoContracts.VincularCaminhaoRelatorioRequest;
import com.example.efficientia.caminhao.service.CaminhaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/caminhoes")
public class CaminhaoController {

    private final CaminhaoService caminhaoService;

    public CaminhaoController(CaminhaoService caminhaoService) {
        this.caminhaoService = caminhaoService;
    }

    // ==========================================
    // 1. ENDPOINTS WEB - CRUD COMPLETO DE CAMINHÕES
    // ==========================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaminhaoResponse criarCaminhao(@Valid @RequestBody CriarCaminhaoRequest request) {
        return caminhaoService.criarCaminhao(request);
    }

    @GetMapping
    public List<CaminhaoResponse> listarCaminhoes(
            @RequestParam(required = false) Integer empresaId,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String tipo
    ) {
        return caminhaoService.listarCaminhoes(empresaId, ativo, tipo);
    }

    @GetMapping("/{id}")
    public CaminhaoResponse buscarPorId(
            @PathVariable Integer id,
            @RequestParam(required = false) String tipo
    ) {
        return caminhaoService.buscarCaminhaoPorId(tipo, id);
    }

    @GetMapping("/placa/{placa}")
    public CaminhaoResponse buscarPorPlaca(@PathVariable String placa) {
        return caminhaoService.buscarCaminhaoPorPlaca(placa);
    }

    @PutMapping("/{id}")
    public CaminhaoResponse atualizar(
            @PathVariable Integer id,
            @RequestParam(required = false) String tipo,
            @Valid @RequestBody AtualizarCaminhaoRequest request
    ) {
        return caminhaoService.atualizarCaminhao(tipo, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(
            @PathVariable Integer id,
            @RequestParam(required = false) String tipo
    ) {
        caminhaoService.removerCaminhao(tipo, id);
    }

    // ==========================================
    // 2. ENDPOINTS APP MOBILE - FLUXO MOTORISTA / RELATÓRIO
    // ==========================================

    @GetMapping("/app")
    public List<CaminhaoAppResponse> listarCaminhoesApp(@RequestParam(required = false) Integer empresaId) {
        return caminhaoService.listarCaminhoesApp(empresaId);
    }

    @GetMapping("/disponiveis")
    public List<CaminhaoResponse> listarDisponiveis(
            @RequestParam(required = false) Integer empresaId,
            @RequestParam(required = false) String tipo
    ) {
        return caminhaoService.listarCaminhoesDisponiveis(empresaId, tipo);
    }

    @GetMapping("/relatorio/{relatorioId}")
    public CaminhaoRelatorioResponse buscarCaminhaoDoRelatorio(@PathVariable Integer relatorioId) {
        return caminhaoService.buscarCaminhaoDoRelatorio(relatorioId);
    }

    @GetMapping("/motorista/{motoristaId}")
    public CaminhaoRelatorioResponse buscarCaminhaoDoMotorista(@PathVariable Integer motoristaId) {
        return caminhaoService.buscarCaminhaoAtivoDoMotorista(motoristaId);
    }

    @PostMapping("/relatorio/{relatorioId}/vincular")
    public CaminhaoRelatorioResponse vincularCaminhaoAoRelatorio(
            @PathVariable Integer relatorioId,
            @Valid @RequestBody VincularCaminhaoRelatorioRequest request
    ) {
        return caminhaoService.vincularCaminhaoAoRelatorio(relatorioId, request);
    }
}
