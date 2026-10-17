package com.example.efficientia.temp.service;

import com.example.efficientia.temp.api.GraficosResumoResponse;
import com.example.efficientia.temp.api.TabelaDadosResponse;
import com.example.efficientia.temp.api.TabelaMetadataDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("obterDadosTabela deve clampar limites adequadamente")
    void obterDadosTabelaDeveClamparLimites() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
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
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(ResultSetExtractor.class)))
                .thenAnswer(invocation -> {
                    ResultSetExtractor<?> extractor = invocation.getArgument(2);
                    java.sql.ResultSet rs = Mockito.mock(java.sql.ResultSet.class);
                    java.sql.ResultSetMetaData meta = Mockito.mock(java.sql.ResultSetMetaData.class);
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
        // Verifica que senha_hash nunca é exposta
        assertThat(primeiroRegistro.get("senha_hash")).isEqualTo("[PROTEGIDO]");
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
}
