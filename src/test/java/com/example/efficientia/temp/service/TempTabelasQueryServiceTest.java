package com.example.efficientia.temp.service;

import com.example.efficientia.temp.api.GraficosResumoResponse;
import com.example.efficientia.temp.api.TabelaDadosResponse;
import com.example.efficientia.temp.api.TabelaMetadataDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TempTabelasQueryServiceTest {

    @Test
    @DisplayName("listarCatalogoTabelas deve listar catálogo de tabelas mesmo sem conexão ao banco")
    void listarCatalogoSemBancoDeveRetornarCatalogoComContagemZerada() {
        TempTabelasQueryService service = new TempTabelasQueryService(null);
        List<TabelaMetadataDto> catalogo = service.listarCatalogoTabelas();

        assertThat(catalogo).isNotEmpty();
        assertThat(catalogo).anyMatch(t -> t.alias().equals("relatorios-viagem"));
        assertThat(catalogo).anyMatch(t -> t.alias().equals("usuarios"));
        assertThat(catalogo).anyMatch(t -> t.alias().equals("veiculos-cavalo"));
    }

    @Test
    @DisplayName("listarCatalogoTabelas com conexão JDBC deve retornar metadados com contagens e colunas")
    void listarCatalogoComBancoDeveRetornarMetadadosEColunas() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class))).thenReturn(15L);

        when(jdbcTemplate.query(contains("LIMIT 0"), any(ResultSetExtractor.class)))
                .thenAnswer(inv -> {
                    ResultSetExtractor<?> extractor = inv.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    ResultSetMetaData meta = mock(ResultSetMetaData.class);
                    when(rs.getMetaData()).thenReturn(meta);
                    when(meta.getColumnCount()).thenReturn(2);
                    when(meta.getColumnLabel(1)).thenReturn("id");
                    when(meta.getColumnLabel(2)).thenReturn("nome");
                    return extractor.extractData(rs);
                });

        TempTabelasQueryService service = new TempTabelasQueryService(jdbcTemplate);
        List<TabelaMetadataDto> catalogo = service.listarCatalogoTabelas();

        assertThat(catalogo).isNotEmpty();
        TabelaMetadataDto relatorioViagem = catalogo.stream()
                .filter(t -> "relatorios-viagem".equals(t.alias()))
                .findFirst()
                .orElse(null);

        assertThat(relatorioViagem).isNotNull();
        assertThat(relatorioViagem.totalRegistros()).isEqualTo(15L);
        assertThat(relatorioViagem.colunas()).containsExactly("id", "nome");
    }

    @Test
    @DisplayName("obterDadosTabela deve rejeitar identificadores inválidos e SQL injection")
    void obterDadosTabelaDeveRejeitarSqlInjection() {
        TempTabelasQueryService service = new TempTabelasQueryService(null);

        assertThatThrownBy(() -> service.obterDadosTabela(null, 10, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.obterDadosTabela("   ", 10, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.obterDadosTabela("tabela; DROP TABLE usuarios;", 10, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.obterDadosTabela("relatorios-viagem", 10, 0, "coluna;--", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("obterDadosTabela deve rejeitar tabela fora do catálogo")
    void obterDadosTabelaDeveRejeitarTabelaForaDoCatalogo() {
        TempTabelasQueryService service = new TempTabelasQueryService(null);

        assertThatThrownBy(() -> service.obterDadosTabela("tabela_desconhecida", 10, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tabela não permitida ou desconhecida");
    }

    @Test
    @DisplayName("obterDadosTabela deve rejeitar coluna de ordenação inexistente")
    void obterDadosTabelaDeveRejeitarColunaDeOrdenacaoInexistente() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class))).thenReturn(10L);

        when(jdbcTemplate.query(contains("LIMIT 0"), any(ResultSetExtractor.class)))
                .thenAnswer(inv -> {
                    ResultSetExtractor<?> extractor = inv.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    ResultSetMetaData meta = mock(ResultSetMetaData.class);
                    when(rs.getMetaData()).thenReturn(meta);
                    when(meta.getColumnCount()).thenReturn(2);
                    when(meta.getColumnLabel(1)).thenReturn("id");
                    when(meta.getColumnLabel(2)).thenReturn("nome");
                    return extractor.extractData(rs);
                });

        TempTabelasQueryService service = new TempTabelasQueryService(jdbcTemplate);

        assertThatThrownBy(() -> service.obterDadosTabela("relatorios-viagem", 10, 0, "coluna_inexistente", "asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coluna de ordenação não existe na tabela");
    }

    @Test
    @DisplayName("obterDadosTabela deve clampar limites adequadamente")
    void obterDadosTabelaDeveClamparLimites() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);

        TempTabelasQueryService service = new TempTabelasQueryService(jdbcTemplate);

        TabelaDadosResponse respDefault = service.obterDadosTabela("relatorios-viagem", null, null, null, null);
        assertThat(respDefault.limite()).isEqualTo(1000);
        assertThat(respDefault.offset()).isEqualTo(0);

        TabelaDadosResponse respMax = service.obterDadosTabela("relatorios-viagem", 50000, -5, null, null);
        assertThat(respMax.limite()).isEqualTo(10000);
        assertThat(respMax.offset()).isEqualTo(0);
    }

    @Test
    @DisplayName("obterDadosTabela deve retornar dados sanitizados quando jdbcTemplate responde")
    void obterDadosTabelaRetornaDadosSanitizados() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(ResultSetExtractor.class)))
                .thenAnswer(invocation -> {
                    ResultSetExtractor<?> extractor = invocation.getArgument(2);
                    ResultSet rs = mock(ResultSet.class);
                    ResultSetMetaData meta = mock(ResultSetMetaData.class);
                    when(rs.getMetaData()).thenReturn(meta);
                    when(meta.getColumnCount()).thenReturn(4);
                    when(meta.getColumnLabel(1)).thenReturn("id");
                    when(meta.getColumnLabel(2)).thenReturn("nome");
                    when(meta.getColumnLabel(3)).thenReturn("senha_hash");
                    when(meta.getColumnLabel(4)).thenReturn("cpf");

                    when(rs.next()).thenReturn(true, false);
                    when(rs.getObject(1)).thenReturn(1);
                    when(rs.getObject(2)).thenReturn("Motorista Teste");
                    when(rs.getObject(3)).thenReturn("$2a$10$secretHashValue");
                    when(rs.getObject(4)).thenReturn("12345678900");

                    return extractor.extractData(rs);
                });

        TempTabelasQueryService service = new TempTabelasQueryService(jdbcTemplate);
        TabelaDadosResponse response = service.obterDadosTabela("usuarios", 10, 0, "id", "ASC");

        assertThat(response.tabela()).isEqualTo("tb_usuario");
        assertThat(response.dados()).hasSize(1);
        Map<String, Object> primeiroRegistro = response.dados().get(0);
        assertThat(primeiroRegistro.get("id")).isEqualTo(1);
        assertThat(primeiroRegistro.get("nome")).isEqualTo("Motorista Teste");
        assertThat(primeiroRegistro.get("senha_hash")).isEqualTo("[PROTEGIDO]");
    }

    @Test
    @DisplayName("obterDadosTabela com filtro de busca textual deve aplicar filtro WHERE e contar registros filtrados")
    void obterDadosTabelaComFiltroDeBusca() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM sc_operacao.tb_relatorio_viagem"), eq(Long.class)))
                .thenReturn(50L);

        when(jdbcTemplate.query(contains("LIMIT 0"), any(ResultSetExtractor.class)))
                .thenAnswer(inv -> {
                    ResultSetExtractor<?> extractor = inv.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    ResultSetMetaData meta = mock(ResultSetMetaData.class);
                    when(rs.getMetaData()).thenReturn(meta);
                    when(meta.getColumnCount()).thenReturn(2);
                    when(meta.getColumnLabel(1)).thenReturn("id");
                    when(meta.getColumnLabel(2)).thenReturn("status");
                    return extractor.extractData(rs);
                });

        when(jdbcTemplate.queryForObject(
                contains("WHERE ("),
                eq(Long.class),
                any(Object[].class)
        )).thenReturn(3L);

        when(jdbcTemplate.query(
                contains("WHERE ("),
                any(PreparedStatementSetter.class),
                any(ResultSetExtractor.class)
        )).thenAnswer(inv -> {
            ResultSetExtractor<?> extractor = inv.getArgument(2);
            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData meta = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(meta);
            when(meta.getColumnCount()).thenReturn(2);
            when(meta.getColumnLabel(1)).thenReturn("id");
            when(meta.getColumnLabel(2)).thenReturn("status");

            when(rs.next()).thenReturn(true, false);
            when(rs.getObject(1)).thenReturn(101L);
            when(rs.getObject(2)).thenReturn("CONCLUIDO_FILTRADO");

            return extractor.extractData(rs);
        });

        TempTabelasQueryService service = new TempTabelasQueryService(jdbcTemplate);
        TabelaDadosResponse response = service.obterDadosTabela("relatorios-viagem", 10, 0, null, null, "busca_teste");

        assertThat(response).isNotNull();
        assertThat(response.tabela()).isEqualTo("tb_relatorio_viagem");
        assertThat(response.totalRegistros()).isEqualTo(3L);
        assertThat(response.colunas()).containsExactly("id", "status");
        assertThat(response.dados()).hasSize(1);
        assertThat(response.dados().get(0))
                .containsEntry("id", 101L)
                .containsEntry("status", "CONCLUIDO_FILTRADO");
    }

    @Test
    @DisplayName("obterDadosTabela deve tratar DataAccessException ao consultar dados retornando lista vazia")
    void obterDadosTabelaTrataDataAccessExceptionRetornandoListaVazia() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class))).thenReturn(10L);

        when(jdbcTemplate.query(contains("LIMIT 0"), any(ResultSetExtractor.class)))
                .thenAnswer(inv -> {
                    ResultSetExtractor<?> extractor = inv.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    ResultSetMetaData meta = mock(ResultSetMetaData.class);
                    when(rs.getMetaData()).thenReturn(meta);
                    when(meta.getColumnCount()).thenReturn(2);
                    when(meta.getColumnLabel(1)).thenReturn("id");
                    when(meta.getColumnLabel(2)).thenReturn("status");
                    return extractor.extractData(rs);
                });

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(ResultSetExtractor.class)))
                .thenThrow(new DataRetrievalFailureException("Simulação de falha de consulta"));

        TempTabelasQueryService service = new TempTabelasQueryService(jdbcTemplate);
        TabelaDadosResponse response = service.obterDadosTabela("relatorios-viagem", 10, 0, null, null);

        assertThat(response).isNotNull();
        assertThat(response.dados()).isEmpty();
        assertThat(response.totalRegistros()).isEqualTo(10L);
    }

    @Test
    @DisplayName("obterDadosTabela deve sanitizar campos de senha e binários via reflexão e tipos SQL")
    void sanitizarValorDeveProtegerSenhasEDadosBinarios() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class))).thenReturn(1L);

        Array sqlArray = mock(Array.class);
        when(sqlArray.getArray()).thenReturn(new String[]{"tag1", "tag2"});

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(ResultSetExtractor.class)))
                .thenAnswer(invocation -> {
                    ResultSetExtractor<?> extractor = invocation.getArgument(2);
                    ResultSet rs = mock(ResultSet.class);
                    ResultSetMetaData meta = mock(ResultSetMetaData.class);
                    when(rs.getMetaData()).thenReturn(meta);
                    when(meta.getColumnCount()).thenReturn(4);
                    when(meta.getColumnLabel(1)).thenReturn("id");
                    when(meta.getColumnLabel(2)).thenReturn("password_hash");
                    when(meta.getColumnLabel(3)).thenReturn("foto_bytes");
                    when(meta.getColumnLabel(4)).thenReturn("tags");

                    when(rs.next()).thenReturn(true, false);
                    when(rs.getObject(1)).thenReturn(1);
                    when(rs.getObject(2)).thenReturn("raw_secret_value");
                    when(rs.getObject(3)).thenReturn(new byte[]{1, 2, 3, 4, 5});
                    when(rs.getObject(4)).thenReturn(sqlArray);

                    return extractor.extractData(rs);
                });

        TempTabelasQueryService service = new TempTabelasQueryService(jdbcTemplate);
        TabelaDadosResponse response = service.obterDadosTabela("usuarios", 10, 0, null, null);

        assertThat(response.dados()).hasSize(1);
        Map<String, Object> row = response.dados().get(0);
        assertThat(row.get("password_hash")).isEqualTo("[PROTEGIDO]");
        assertThat(row.get("foto_bytes")).isEqualTo("[BINARY DATA: 5 bytes]");
        assertThat(row.get("tags")).isEqualTo(new String[]{"tag1", "tag2"});
    }

    @Test
    @DisplayName("obterTodasAsTabelas deve consolidar todas as tabelas mapeadas")
    void obterTodasAsTabelasConsolidaTabelas() {
        TempTabelasQueryService service = new TempTabelasQueryService(null);
        Map<String, Object> todas = service.obterTodasAsTabelas(10);

        assertThat(todas).isNotEmpty();
        assertThat(todas).containsKey("relatorios-viagem");
        assertThat(todas).containsKey("usuarios");
        assertThat(todas).containsKey("veiculos-cavalo");
        assertThat(todas).containsKey("documentos");
    }

    @Test
    @DisplayName("obterTodasAsTabelas deve tratar limite nulo e clampar valores excessivos")
    void obterTodasAsTabelasDeveTratarLimiteNuloEClampar() {
        TempTabelasQueryService service = spy(new TempTabelasQueryService(null));

        Map<String, Object> resultadoNulo = service.obterTodasAsTabelas(null);
        assertThat(resultadoNulo).isNotNull().isNotEmpty();
        assertThat(resultadoNulo).containsKeys("relatorios-viagem", "usuarios", "veiculos-cavalo", "documentos");
        verify(service, atLeastOnce()).obterDadosTabela(eq("relatorios-viagem"), eq(1000), eq(0), isNull(), isNull());

        Map<String, Object> resultadoClamped = service.obterTodasAsTabelas(10000);
        assertThat(resultadoClamped).isNotNull().isNotEmpty();
        assertThat(resultadoClamped).containsKeys("relatorios-viagem", "usuarios", "veiculos-cavalo", "documentos");
        verify(service, atLeastOnce()).obterDadosTabela(eq("relatorios-viagem"), eq(5000), eq(0), isNull(), isNull());

        Map<String, Object> resultadoNegativo = service.obterTodasAsTabelas(-20);
        assertThat(resultadoNegativo).isNotNull().isNotEmpty();
        verify(service, atLeastOnce()).obterDadosTabela(eq("relatorios-viagem"), eq(1000), eq(0), isNull(), isNull());
    }

    @Test
    @DisplayName("obterResumoMetricas deve calcular KPIs de gráficos com valores seguros mesmo em falhas de banco")
    void obterResumoMetricasSemFalhas() {
        TempTabelasQueryService service = new TempTabelasQueryService(null);
        GraficosResumoResponse resumo = service.obterResumoMetricas();

        assertThat(resumo).isNotNull();
        assertThat(resumo.totalViagens()).isNotNull();
        assertThat(resumo.totalUsuarios()).isNotNull();
        assertThat(resumo.totalVeiculosCavalo()).isNotNull();
        assertThat(resumo.viagensPorStatus()).isNotNull();
        assertThat(resumo.paradasPorMotivo()).isNotNull();
        assertThat(resumo.anomaliasPorTipo()).isNotNull();
        assertThat(resumo.totaisGerais()).isNotNull();
    }

    @Test
    @DisplayName("obterResumoMetricas com mock JdbcTemplate deve agregar status de viagem e motivos de parada")
    void obterResumoMetricasComMockJdbc() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class)))
                .thenReturn(25L);

        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("st")).thenReturn("EM_TRANSITO", "CONCLUIDA");
            when(rs.getLong("qtd")).thenReturn(15L, 10L);
            handler.processRow(rs);
            handler.processRow(rs);
            return null;
        }).when(jdbcTemplate).query(contains("status"), any(RowCallbackHandler.class));

        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("mot")).thenReturn("PNEU_FURADO", "MANUTENCAO");
            when(rs.getLong("qtd")).thenReturn(7L, 3L);
            handler.processRow(rs);
            handler.processRow(rs);
            return null;
        }).when(jdbcTemplate).query(contains("motivo"), any(RowCallbackHandler.class));

        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("anom")).thenReturn("PESO_EXCEDENTE");
            when(rs.getLong("qtd")).thenReturn(4L);
            handler.processRow(rs);
            return null;
        }).when(jdbcTemplate).query(contains("anomalia"), any(RowCallbackHandler.class));

        TempTabelasQueryService service = new TempTabelasQueryService(jdbcTemplate);
        GraficosResumoResponse resumo = service.obterResumoMetricas();

        assertThat(resumo).isNotNull();
        assertThat(resumo.totalViagens()).isEqualTo(25L);
        assertThat(resumo.totalUsuarios()).isEqualTo(25L);
        assertThat(resumo.totalVeiculosCavalo()).isEqualTo(25L);
        assertThat(resumo.totalVeiculosCarreta()).isEqualTo(25L);
        assertThat(resumo.totalParadasImprevistas()).isEqualTo(25L);
        assertThat(resumo.totalAnomalias()).isEqualTo(50L);
        assertThat(resumo.viagensPorStatus()).containsEntry("EM_TRANSITO", 15L).containsEntry("CONCLUIDA", 10L);
        assertThat(resumo.paradasPorMotivo()).containsEntry("PNEU_FURADO", 7L).containsEntry("MANUTENCAO", 3L);
        assertThat(resumo.anomaliasPorTipo()).containsEntry("PESO_EXCEDENTE", 8L);
        assertThat(resumo.totaisGerais()).isNotEmpty();
        assertThat(resumo.totaisGerais().get("totalViagens")).isEqualTo(25L);
    }

    @Test
    @DisplayName("obterResumoMetricas deve acionar fallback para tabelas de domínio quando colunas motivo/anomalia não existirem")
    void obterResumoMetricasComFallbackDominioQuandoColunaMotivoOuAnomaliaFalhar() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class))).thenReturn(10L);

        doAnswer(invocation -> {
            throw new DataRetrievalFailureException("Coluna motivo não existe");
        }).when(jdbcTemplate).query(contains("GROUP BY motivo"), any(RowCallbackHandler.class));

        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("mot")).thenReturn("PARADA_FALLBACK");
            when(rs.getLong("qtd")).thenReturn(6L);
            handler.processRow(rs);
            return null;
        }).when(jdbcTemplate).query(contains("tb_dominio_motivo_parada"), any(RowCallbackHandler.class));

        TempTabelasQueryService service = new TempTabelasQueryService(jdbcTemplate);
        GraficosResumoResponse resumo = service.obterResumoMetricas();

        assertThat(resumo).isNotNull();
        assertThat(resumo.paradasPorMotivo()).containsEntry("PARADA_FALLBACK", 6L);
    }

    @Test
    @DisplayName("listarCatalogoTabelas com jdbcTemplate preenchido conta registros e colunas")
    void listarCatalogoComBancoDeveContarRegistrosEColunas() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(42L);
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class)))
                .thenReturn(List.of("id", "nome", "criado_em"));

        TempTabelasQueryService service = new TempTabelasQueryService(jdbcTemplate);
        List<TabelaMetadataDto> catalogo = service.listarCatalogoTabelas();

        assertThat(catalogo).isNotEmpty();
        assertThat(catalogo.get(0).totalRegistros()).isEqualTo(42L);
        assertThat(catalogo.get(0).colunas()).contains("id", "nome");
    }

    @Test
    @DisplayName("obterTodasAsTabelas trata exceção em tabela individual sem abortar")
    void obterTodasAsTabelasTrataExcecaoEmTabela() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenThrow(new DataRetrievalFailureException("erro banco"));

        TempTabelasQueryService service = new TempTabelasQueryService(jdbcTemplate);
        Map<String, Object> resultado = service.obterTodasAsTabelas(10);

        assertThat(resultado).isNotEmpty();
    }

    @Test
    @DisplayName("sanitizarValor trata nulos, binários e senhas")
    void sanitizarValorTrataValoresCorretamente() throws Exception {
        TempTabelasQueryService service = new TempTabelasQueryService(null);
        var method = TempTabelasQueryService.class.getDeclaredMethod("sanitizarValor", String.class, Object.class);
        method.setAccessible(true);

        assertThat(method.invoke(service, "senha", "123456")).isEqualTo("[PROTEGIDO]");
        assertThat(method.invoke(service, "password_hash", "hash")).isEqualTo("[PROTEGIDO]");
        assertThat(method.invoke(service, "foto", new byte[]{1, 2, 3})).isEqualTo("[BINARY DATA: 3 bytes]");
        assertThat(method.invoke(service, "nome", "João")).isEqualTo("João");
        assertThat(method.invoke(service, "campo", null)).isNull();
    }
}
