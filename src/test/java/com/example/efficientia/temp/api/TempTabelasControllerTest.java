package com.example.efficientia.temp.api;

import com.example.efficientia.temp.service.TempTabelasQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TempTabelasControllerTest {

    private MockMvc mockMvc;
    private TempTabelasQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = Mockito.mock(TempTabelasQueryService.class);
        TempTabelasController controller = new TempTabelasController(queryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/temp/tabelas deve retornar lista de metadados do catálogo")
    void listarTabelasDeveRetornarCatalogo() throws Exception {
        TabelaMetadataDto dto = new TabelaMetadataDto(
                "tb_relatorio_viagem",
                "sc_operacao",
                "relatorios-viagem",
                42L,
                List.of("id", "status", "motorista_id")
        );
        when(queryService.listarCatalogoTabelas()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/temp/tabelas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome", is("tb_relatorio_viagem")))
                .andExpect(jsonPath("$[0].schema", is("sc_operacao")))
                .andExpect(jsonPath("$[0].alias", is("relatorios-viagem")))
                .andExpect(jsonPath("$[0].totalRegistros", is(42)))
                .andExpect(jsonPath("$[0].colunas", hasSize(3)));

        verify(queryService).listarCatalogoTabelas();
    }

    @Test
    @DisplayName("GET /api/v1/temp/tabelas/{tabela} com nome válido deve retornar registros paginados")
    void obterDadosTabelaValida() throws Exception {
        TabelaDadosResponse response = new TabelaDadosResponse(
                "tb_relatorio_viagem",
                "sc_operacao",
                1L,
                100,
                0,
                List.of("id", "status"),
                List.of(Map.of("id", 1, "status", "concluido"))
        );
        when(queryService.obterDadosTabela(eq("relatorios-viagem"), eq(100), eq(0), eq("id"), eq("desc")))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/temp/tabelas/relatorios-viagem")
                        .param("limite", "100")
                        .param("offset", "0")
                        .param("ordenarPor", "id")
                        .param("direcao", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_relatorio_viagem")))
                .andExpect(jsonPath("$.schema", is("sc_operacao")))
                .andExpect(jsonPath("$.totalRegistros", is(1)))
                .andExpect(jsonPath("$.dados", hasSize(1)))
                .andExpect(jsonPath("$.dados[0].status", is("concluido")));
    }

    @Test
    @DisplayName("GET /api/v1/temp/tabelas/{tabela} com identificador inválido deve retornar 400 Bad Request")
    void obterDadosTabelaInvalidaDeveRetornar400() throws Exception {
        when(queryService.obterDadosTabela(eq("tabela_desconhecida"), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Tabela não permitida ou desconhecida"));

        mockMvc.perform(get("/api/v1/temp/tabelas/tabela_desconhecida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("Tabela não permitida ou desconhecida")));
    }

    @Test
    @DisplayName("GET /api/v1/temp/todas e /dados-graficos deve retornar todas as tabelas em um único mapa")
    void obterTodasTabelasConsolidadas() throws Exception {
        Map<String, Object> todas = Map.of(
                "relatorios-viagem", List.of(Map.of("id", 10)),
                "usuarios", List.of(Map.of("id", 1, "nome", "Carlos"))
        );
        when(queryService.obterTodasAsTabelas(500)).thenReturn(todas);

        mockMvc.perform(get("/api/v1/temp/todas").param("limite", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatorios-viagem", hasSize(1)))
                .andExpect(jsonPath("$.usuarios", hasSize(1)));

        mockMvc.perform(get("/api/v1/temp/dados-graficos").param("limite", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatorios-viagem", hasSize(1)))
                .andExpect(jsonPath("$.usuarios", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/temp/graficos/metricas-resumo deve retornar indicadores consolidados")
    void obterResumoMetricas() throws Exception {
        GraficosResumoResponse resumo = new GraficosResumoResponse(
                50L,
                15L,
                8L,
                12L,
                5L,
                3L,
                Map.of("concluido", 40L, "rascunho", 10L),
                Map.of("pneu_furado", 2L),
                Map.of("animal_machucado", 3L),
                Map.of("totalViagens", 50L)
        );
        when(queryService.obterResumoMetricas()).thenReturn(resumo);

        mockMvc.perform(get("/api/v1/temp/graficos/metricas-resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalViagens", is(50)))
                .andExpect(jsonPath("$.totalUsuarios", is(15)))
                .andExpect(jsonPath("$.totalVeiculosCavalo", is(8)))
                .andExpect(jsonPath("$.totalVeiculosCarreta", is(12)))
                .andExpect(jsonPath("$.viagensPorStatus.concluido", is(40)))
                .andExpect(jsonPath("$.paradasPorMotivo.pneu_furado", is(2)));
    }

    @Test
    @DisplayName("Endpoints semânticos dedicados (ex: /relatorios-viagem, /usuarios) devem delegar corretamente ao service")
    void endpointsSemanticosDedicados() throws Exception {
        TabelaDadosResponse resp = new TabelaDadosResponse(
                "tb_relatorio_viagem", "sc_operacao", 2L, 100, 0, List.of("id"), List.of()
        );
        when(queryService.obterDadosTabela(eq("relatorios-viagem"), anyInt(), anyInt(), any(), any()))
                .thenReturn(resp);

        mockMvc.perform(get("/api/v1/temp/relatorios-viagem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_relatorio_viagem")));

        TabelaDadosResponse respUsuarios = new TabelaDadosResponse(
                "tb_usuario", "sc_corporativo", 10L, 100, 0, List.of("id", "nome"), List.of()
        );
        when(queryService.obterDadosTabela(eq("usuarios"), anyInt(), anyInt(), any(), any()))
                .thenReturn(respUsuarios);

        mockMvc.perform(get("/api/v1/temp/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_usuario")));
    }
}
