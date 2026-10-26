package com.example.efficientia.temp.api;

import com.example.efficientia.temp.service.TempTabelasQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TempTabelasControllerTest {

    private MockMvc mockMvc;
    private TempTabelasQueryService queryService;
    private TempTabelasController controller;

    @BeforeEach
    void setUp() {
        queryService = Mockito.mock(TempTabelasQueryService.class);
        controller = new TempTabelasController(queryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private TabelaDadosResponse mockResponse(String tabela, String schema) {
        return new TabelaDadosResponse(
                tabela,
                schema,
                1L,
                100,
                0,
                List.of("id"),
                List.of(Map.of("id", 1))
        );
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
        when(queryService.obterDadosTabela(eq("relatorios-viagem"), eq(100), eq(0), eq("id"), eq("desc"), isNull()))
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
    @DisplayName("GET /api/v1/temp/tabelas/{tabela} com parâmetro q deve repassar filtro de busca ao service")
    void obterDadosTabelaComFiltroDeBusca() throws Exception {
        TabelaDadosResponse response = new TabelaDadosResponse(
                "tb_relatorio_viagem",
                "sc_operacao",
                1L,
                50,
                0,
                List.of("id", "status"),
                List.of(Map.of("id", 1, "status", "em_andamento"))
        );
        when(queryService.obterDadosTabela(eq("relatorios-viagem"), eq(50), eq(0), eq("id"), eq("asc"), eq("busca_termo")))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/temp/tabelas/relatorios-viagem")
                        .param("limite", "50")
                        .param("offset", "0")
                        .param("ordenarPor", "id")
                        .param("direcao", "asc")
                        .param("q", "busca_termo")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_relatorio_viagem")))
                .andExpect(jsonPath("$.totalRegistros", is(1)))
                .andExpect(jsonPath("$.dados[0].status", is("em_andamento")));

        verify(queryService).obterDadosTabela("relatorios-viagem", 50, 0, "id", "asc", "busca_termo");
    }

    @Test
    @DisplayName("GET /api/v1/temp/tabelas/{tabela} com identificador inválido deve retornar 400 Bad Request")
    void obterDadosTabelaInvalidaDeveRetornar400() throws Exception {
        when(queryService.obterDadosTabela(eq("tabela_desconhecida"), any(), any(), any(), any(), isNull()))
                .thenThrow(new IllegalArgumentException("Tabela não permitida ou desconhecida"));

        mockMvc.perform(get("/api/v1/temp/tabelas/tabela_desconhecida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("Tabela não permitida ou desconhecida")));
    }

    @Test
    @DisplayName("handleIllegalArgumentException deve devolver HTTP 400 com payload { 'erro': '...' }")
    void handleIllegalArgumentExceptionDeveRetornar400ComPayloadErro() throws Exception {
        // Teste direto do método do controller com asserções AssertJ
        ResponseEntity<Map<String, String>> directResponse = controller.handleIllegalArgumentException(
                new IllegalArgumentException("Coluna de ordenação não existe na tabela: coluna_invalida")
        );
        assertThat(directResponse.getStatusCode().value()).isEqualTo(400);
        assertThat(directResponse.getBody())
                .isNotNull()
                .containsEntry("erro", "Coluna de ordenação não existe na tabela: coluna_invalida");

        // Teste integrado via MockMvc
        when(queryService.obterDadosTabela(eq("relatorios-viagem"), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Coluna de ordenação não existe na tabela: coluna_invalida"));

        mockMvc.perform(get("/api/v1/temp/tabelas/relatorios-viagem")
                        .param("ordenarPor", "coluna_invalida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("Coluna de ordenação não existe na tabela: coluna_invalida")));
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
    @DisplayName("GET /api/v1/temp/todas sem parâmetro limite deve assumir padrão 1000")
    void obterTodasTabelasComLimitePadrao() throws Exception {
        Map<String, Object> todas = Map.of("relatorios-viagem", List.of());
        when(queryService.obterTodasAsTabelas(1000)).thenReturn(todas);

        mockMvc.perform(get("/api/v1/temp/todas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatorios-viagem").isArray());

        verify(queryService).obterTodasAsTabelas(1000);
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
                .andExpect(jsonPath("$.totalParadasImprevistas", is(5)))
                .andExpect(jsonPath("$.totalAnomalias", is(3)))
                .andExpect(jsonPath("$.viagensPorStatus.concluido", is(40)))
                .andExpect(jsonPath("$.viagensPorStatus.rascunho", is(10)))
                .andExpect(jsonPath("$.paradasPorMotivo.pneu_furado", is(2)))
                .andExpect(jsonPath("$.anomaliasPorTipo.animal_machucado", is(3)))
                .andExpect(jsonPath("$.totaisGerais.totalViagens", is(50)));

        verify(queryService).obterResumoMetricas();
    }

    @Test
    @DisplayName("Endpoints semânticos de Operação devem delegar corretamente ao service")
    void endpointsSemanticosOperacao() throws Exception {
        when(queryService.obterDadosTabela(eq("relatorios-viagem"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("tb_relatorio_viagem", "sc_operacao"));
        when(queryService.obterDadosTabela(eq("paradas-imprevistas"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("tb_parada_imprevista", "sc_operacao"));
        when(queryService.obterDadosTabela(eq("anomalias-embarque"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("tb_anomalia_embarque", "sc_operacao"));
        when(queryService.obterDadosTabela(eq("anomalias-desembarque"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("tb_anomalia_desembarque", "sc_operacao"));

        mockMvc.perform(get("/api/v1/temp/relatorios-viagem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_relatorio_viagem")));
        verify(queryService).obterDadosTabela("relatorios-viagem", 100, 0, null, "asc");

        mockMvc.perform(get("/api/v1/temp/paradas-imprevistas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_parada_imprevista")));
        verify(queryService).obterDadosTabela("paradas-imprevistas", 100, 0, null, "asc");

        mockMvc.perform(get("/api/v1/temp/anomalias-embarque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_anomalia_embarque")));
        verify(queryService).obterDadosTabela("anomalias-embarque", 100, 0, null, "asc");

        mockMvc.perform(get("/api/v1/temp/anomalias-desembarque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_anomalia_desembarque")));
        verify(queryService).obterDadosTabela("anomalias-desembarque", 100, 0, null, "asc");
    }

    @Test
    @DisplayName("Endpoints semânticos Corporativo e Frota devem delegar corretamente ao service")
    void endpointsSemanticosCorporativoEFrota() throws Exception {
        when(queryService.obterDadosTabela(eq("usuarios"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("tb_usuario", "sc_corporativo"));
        when(queryService.obterDadosTabela(eq("enderecos"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("tb_endereco", "sc_corporativo"));
        when(queryService.obterDadosTabela(eq("fazendas"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("tb_fazenda", "sc_corporativo"));
        when(queryService.obterDadosTabela(eq("unidades-frigorificas"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("tb_unidade_frigorifica", "sc_corporativo"));
        when(queryService.obterDadosTabela(eq("veiculos-cavalo"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("tb_veiculo_cavalo", "sc_frota"));
        when(queryService.obterDadosTabela(eq("veiculos-carreta"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("tb_veiculo_carreta", "sc_frota"));

        mockMvc.perform(get("/api/v1/temp/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_usuario")));
        verify(queryService).obterDadosTabela("usuarios", 100, 0, null, "asc");

        mockMvc.perform(get("/api/v1/temp/enderecos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_endereco")));
        verify(queryService).obterDadosTabela("enderecos", 100, 0, null, "asc");

        mockMvc.perform(get("/api/v1/temp/fazendas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_fazenda")));
        verify(queryService).obterDadosTabela("fazendas", 100, 0, null, "asc");

        mockMvc.perform(get("/api/v1/temp/unidades-frigorificas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_unidade_frigorifica")));
        verify(queryService).obterDadosTabela("unidades-frigorificas", 100, 0, null, "asc");

        mockMvc.perform(get("/api/v1/temp/veiculos-cavalo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_veiculo_cavalo")));
        verify(queryService).obterDadosTabela("veiculos-cavalo", 100, 0, null, "asc");

        mockMvc.perform(get("/api/v1/temp/veiculos-carreta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_veiculo_carreta")));
        verify(queryService).obterDadosTabela("veiculos-carreta", 100, 0, null, "asc");
    }

    @Test
    @DisplayName("Endpoints semânticos de Documentos, Empresas e Outbox devem delegar corretamente ao service")
    void endpointsSemanticosDocumentosEEmpresas() throws Exception {
        when(queryService.obterDadosTabela(eq("documentos"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("documento", "public"));
        when(queryService.obterDadosTabela(eq("exportacoes"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("exportacao", "public"));
        when(queryService.obterDadosTabela(eq("assinaturas-motorista"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("assinatura_motorista", "public"));
        when(queryService.obterDadosTabela(eq("empresas"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("empresa", "public"));
        when(queryService.obterDadosTabela(eq("outbox-eventos"), anyInt(), anyInt(), any(), any()))
                .thenReturn(mockResponse("tb_outbox_evento", "sc_operacao"));

        mockMvc.perform(get("/api/v1/temp/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("documento")));
        verify(queryService).obterDadosTabela("documentos", 100, 0, null, "asc");

        mockMvc.perform(get("/api/v1/temp/exportacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("exportacao")));
        verify(queryService).obterDadosTabela("exportacoes", 100, 0, null, "asc");

        mockMvc.perform(get("/api/v1/temp/assinaturas-motorista"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("assinatura_motorista")));
        verify(queryService).obterDadosTabela("assinaturas-motorista", 100, 0, null, "asc");

        mockMvc.perform(get("/api/v1/temp/empresas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("empresa")));
        verify(queryService).obterDadosTabela("empresas", 100, 0, null, "asc");

        mockMvc.perform(get("/api/v1/temp/outbox-eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_outbox_evento")));
        verify(queryService).obterDadosTabela("outbox-eventos", 100, 0, null, "asc");
    }

    @Test
    @DisplayName("Endpoints semânticos de Auditoria e Camada BI com busca devem delegar corretamente ao service")
    void endpointsSemanticosAuditoriaEBIComBusca() throws Exception {
        when(queryService.obterDadosTabela(eq("auditorias-analise"), anyInt(), anyInt(), any(), any(), eq("alerta")))
                .thenReturn(mockResponse("tb_auditoria_analise", "sc_operacao"));
        when(queryService.obterDadosTabela(eq("catalogo-dados"), anyInt(), anyInt(), any(), any(), eq("catalogo_busca")))
                .thenReturn(mockResponse("tb_catalogo_dados", "sc_auditoria"));
        when(queryService.obterDadosTabela(eq("monitoramento-dau"), anyInt(), anyInt(), any(), any(), eq("filtro_dau")))
                .thenReturn(mockResponse("tb_monitoramento_dau", "sc_auditoria"));
        when(queryService.obterDadosTabela(eq("vw-fato-transporte-animal"), anyInt(), anyInt(), any(), any(), eq("fato_busca")))
                .thenReturn(mockResponse("vw_fato_transporte_animal", "sc_operacao"));

        mockMvc.perform(get("/api/v1/temp/auditorias-analise").param("q", "alerta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_auditoria_analise")));
        verify(queryService).obterDadosTabela("auditorias-analise", 100, 0, null, "asc", "alerta");

        mockMvc.perform(get("/api/v1/temp/catalogo-dados").param("q", "catalogo_busca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_catalogo_dados")));
        verify(queryService).obterDadosTabela("catalogo-dados", 100, 0, null, "asc", "catalogo_busca");

        mockMvc.perform(get("/api/v1/temp/monitoramento-dau").param("q", "filtro_dau"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("tb_monitoramento_dau")));
        verify(queryService).obterDadosTabela("monitoramento-dau", 100, 0, null, "asc", "filtro_dau");

        mockMvc.perform(get("/api/v1/temp/bi/fato-transporte").param("q", "fato_busca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tabela", is("vw_fato_transporte_animal")));
        verify(queryService).obterDadosTabela("vw-fato-transporte-animal", 100, 0, null, "asc", "fato_busca");
    }
}
