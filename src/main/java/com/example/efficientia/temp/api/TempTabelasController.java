package com.example.efficientia.temp.api;

import com.example.efficientia.temp.service.TempTabelasQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/temp")
@CrossOrigin
@Tag(name = "Testes Temporários - Frontend / Gráficos", description = "Endpoints temporários de suporte à geração de gráficos no frontend enquanto o Qlik não está disponível.")
public class TempTabelasController {

    private final TempTabelasQueryService queryService;

    public TempTabelasController(TempTabelasQueryService queryService) {
        this.queryService = queryService;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage()));
    }

    @Operation(summary = "Catálogo de todas as tabelas", description = "Retorna os metadados de todas as tabelas do banco com contagens de registros e lista de colunas.")
    @GetMapping("/tabelas")
    public ResponseEntity<List<TabelaMetadataDto>> listarTabelas() {
        return ResponseEntity.ok(queryService.listarCatalogoTabelas());
    }

    @Operation(summary = "Obter dados de uma tabela específica", description = "Consulta paginada e ordenada de registros de uma tabela pelo seu nome ou alias semântico.")
    @GetMapping("/tabelas/{tabela}")
    public ResponseEntity<TabelaDadosResponse> obterDadosTabela(
            @Parameter(description = "Nome real da tabela ou alias amigável")
            @PathVariable("tabela") String tabela,
            @Parameter(description = "Limite de registros a retornar (padrão 100, máx 5000)")
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @Parameter(description = "Deslocamento inicial para paginação (padrão 0)")
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @Parameter(description = "Nome da coluna para ordenação")
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @Parameter(description = "Direção da ordenação (asc ou desc)")
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela(tabela, limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Obter todas as tabelas consolidadas", description = "Retorna um único JSON contendo registros de todas as tabelas do sistema.")
    @GetMapping({"/todas", "/dados-graficos"})
    public ResponseEntity<Map<String, Object>> obterTodasTabelas(
            @Parameter(description = "Limite máximo de registros por tabela (padrão 1000, máx 5000)")
            @RequestParam(name = "limite", required = false, defaultValue = "1000") Integer limite) {
        return ResponseEntity.ok(queryService.obterTodasAsTabelas(limite));
    }

    @Operation(summary = "Resumo analítico consolidado para gráficos", description = "Retorna totais e distribuições prontas para componentes de gráficos e cartões de KPI.")
    @GetMapping("/graficos/metricas-resumo")
    public ResponseEntity<GraficosResumoResponse> obterResumoMetricas() {
        return ResponseEntity.ok(queryService.obterResumoMetricas());
    }

    // --- Endpoints semânticos dedicados ---

    @Operation(summary = "Dados de Relatórios de Viagem", description = "Retorna registros da tabela relatorio_viagem.")
    @GetMapping("/relatorios-viagem")
    public ResponseEntity<TabelaDadosResponse> relatoriosViagem(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("relatorios-viagem", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Paradas Imprevistas", description = "Retorna registros da tabela parada_imprevista.")
    @GetMapping("/paradas-imprevistas")
    public ResponseEntity<TabelaDadosResponse> paradasImprevistas(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("paradas-imprevistas", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Anomalias de Embarque", description = "Retorna registros da tabela anomalia_embarque.")
    @GetMapping("/anomalias-embarque")
    public ResponseEntity<TabelaDadosResponse> anomaliasEmbarque(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("anomalias-embarque", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Anomalias de Desembarque", description = "Retorna registros da tabela anomalia_desembarque.")
    @GetMapping("/anomalias-desembarque")
    public ResponseEntity<TabelaDadosResponse> anomaliasDesembarque(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("anomalias-desembarque", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Usuários", description = "Retorna registros da tabela usuario com senhas sanitizadas.")
    @GetMapping("/usuarios")
    public ResponseEntity<TabelaDadosResponse> usuarios(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("usuarios", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Endereços", description = "Retorna registros da tabela endereco.")
    @GetMapping("/enderecos")
    public ResponseEntity<TabelaDadosResponse> enderecos(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("enderecos", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Fazendas", description = "Retorna registros da tabela fazenda.")
    @GetMapping("/fazendas")
    public ResponseEntity<TabelaDadosResponse> fazendas(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("fazendas", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Unidades Frigoríficas", description = "Retorna registros da tabela unidade_frigorifica.")
    @GetMapping("/unidades-frigorificas")
    public ResponseEntity<TabelaDadosResponse> unidadesFrigorificas(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("unidades-frigorificas", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Veículos Cavalo", description = "Retorna registros da tabela veiculo_cavalo.")
    @GetMapping("/veiculos-cavalo")
    public ResponseEntity<TabelaDadosResponse> veiculosCavalo(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("veiculos-cavalo", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Veículos Carreta", description = "Retorna registros da tabela veiculo_carreta.")
    @GetMapping("/veiculos-carreta")
    public ResponseEntity<TabelaDadosResponse> veiculosCarreta(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("veiculos-carreta", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Documentos", description = "Retorna registros da tabela documento.")
    @GetMapping("/documentos")
    public ResponseEntity<TabelaDadosResponse> documentos(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("documentos", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Exportações", description = "Retorna registros da tabela exportacao.")
    @GetMapping("/exportacoes")
    public ResponseEntity<TabelaDadosResponse> exportacoes(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("exportacoes", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Assinaturas de Motoristas", description = "Retorna registros da tabela assinatura_motorista.")
    @GetMapping("/assinaturas-motorista")
    public ResponseEntity<TabelaDadosResponse> assinaturasMotorista(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("assinaturas-motorista", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Empresas", description = "Retorna registros da tabela empresa.")
    @GetMapping("/empresas")
    public ResponseEntity<TabelaDadosResponse> empresas(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("empresas", limite, offset, ordenarPor, direcao));
    }

    @Operation(summary = "Dados de Eventos do Outbox", description = "Retorna registros da tabela outbox_eventos.")
    @GetMapping("/outbox-eventos")
    public ResponseEntity<TabelaDadosResponse> outboxEventos(
            @RequestParam(name = "limite", required = false, defaultValue = "100") Integer limite,
            @RequestParam(name = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(name = "ordenarPor", required = false) String ordenarPor,
            @RequestParam(name = "direcao", required = false, defaultValue = "asc") String direcao) {
        return ResponseEntity.ok(queryService.obterDadosTabela("outbox-eventos", limite, offset, ordenarPor, direcao));
    }
}
