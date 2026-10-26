package com.example.efficientia.temp.service;

import com.example.efficientia.temp.api.GraficosResumoResponse;
import com.example.efficientia.temp.api.TabelaDadosResponse;
import com.example.efficientia.temp.api.TabelaMetadataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class TempTabelasQueryService {

    private static final Logger log = LoggerFactory.getLogger(TempTabelasQueryService.class);

    private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    public record TabelaDef(
            String nome,
            String schema,
            String aliasPrincipal,
            List<String> aliases
    ) {}

    private record ResolvedTable(
            String qualifiedSqlName,
            String tableName,
            String schemaName,
            long count
    ) {}

    private record CandidateInfo(
            String sqlName,
            String tableName,
            String schemaName
    ) {}

    private static final List<TabelaDef> CATALOGO = List.of(
            new TabelaDef("tb_relatorio_viagem", "sc_operacao", "relatorios-viagem",
                    List.of("relatorios-viagem", "relatorio_viagem", "viagens", "relatorio-viagem", "viagem", "tb_relatorio_viagem", "sc_operacao.tb_relatorio_viagem")),
            new TabelaDef("tb_parada_imprevista", "sc_operacao", "paradas-imprevistas",
                    List.of("paradas-imprevistas", "parada_imprevista", "paradas", "parada-imprevista", "parada", "tb_parada_imprevista", "sc_operacao.tb_parada_imprevista")),
            new TabelaDef("tb_anomalia_embarque", "sc_operacao", "anomalias-embarque",
                    List.of("anomalias-embarque", "anomalia_embarque", "anomalia-embarque", "tb_anomalia_embarque", "sc_operacao.tb_anomalia_embarque")),
            new TabelaDef("tb_anomalia_desembarque", "sc_operacao", "anomalias-desembarque",
                    List.of("anomalias-desembarque", "anomalia_desembarque", "anomalia-desembarque", "tb_anomalia_desembarque", "sc_operacao.tb_anomalia_desembarque")),
            new TabelaDef("tb_outbox_evento", "sc_operacao", "outbox-eventos",
                    List.of("outbox-eventos", "tb_outbox_evento", "outbox", "outbox_evento", "outbox-evento", "tb_outbox_eventos", "sc_operacao.tb_outbox_evento")),
            new TabelaDef("tb_usuario", "sc_corporativo", "usuarios",
                    List.of("usuarios", "tb_usuario", "motoristas", "usuario", "motorista", "sc_corporativo.tb_usuario")),
            new TabelaDef("tb_endereco", "sc_corporativo", "enderecos",
                    List.of("enderecos", "tb_endereco", "endereco", "sc_corporativo.tb_endereco")),
            new TabelaDef("tb_fazenda", "sc_corporativo", "fazendas",
                    List.of("fazendas", "tb_fazenda", "fazenda", "sc_corporativo.tb_fazenda")),
            new TabelaDef("tb_unidade_frigorifica", "sc_corporativo", "unidades-frigorificas",
                    List.of("unidades-frigorificas", "unidade_frigorifica", "frigorificos", "unidade-frigorifica", "frigorifico", "tb_unidade_frigorifica", "sc_corporativo.tb_unidade_frigorifica")),
            new TabelaDef("tb_veiculo_cavalo", "sc_frota", "veiculos-cavalo",
                    List.of("veiculos-cavalo", "tb_veiculo_cavalo", "caminhoes", "veiculo-cavalo", "veiculo_cavalo", "caminhao", "sc_frota.tb_veiculo_cavalo")),
            new TabelaDef("tb_veiculo_carreta", "sc_frota", "veiculos-carreta",
                    List.of("veiculos-carreta", "tb_veiculo_carreta", "carretas", "veiculo-carreta", "veiculo_carreta", "carreta", "sc_frota.tb_veiculo_carreta")),
            new TabelaDef("documento", "public", "documentos",
                    List.of("documentos", "documento", "public.documento", "tb_documento")),
            new TabelaDef("documento_auditoria", "public", "documentos-auditoria",
                    List.of("documentos-auditoria", "documento_auditoria", "documento-auditoria", "public.documento_auditoria", "tb_documento_auditoria")),
            new TabelaDef("exportacao", "public", "exportacoes",
                    List.of("exportacoes", "exportacao", "public.exportacao", "tb_exportacao")),
            new TabelaDef("assinatura_motorista", "public", "assinaturas-motorista",
                    List.of("assinaturas-motorista", "assinatura_motorista", "assinatura-motorista", "public.assinatura_motorista", "tb_assinatura_motorista")),
            new TabelaDef("empresa", "public", "empresas",
                    List.of("empresas", "empresa", "public.empresa", "tb_empresa", "sc_corporativo.tb_empresa", "sc_corporativo.empresa")),
            new TabelaDef("empresa_admin", "public", "empresas-admin",
                    List.of("empresas-admin", "empresa_admin", "empresa-admin", "public.empresa_admin", "tb_empresa_admin", "sc_corporativo.tb_empresa_admin", "sc_corporativo.empresa_admin")),
            new TabelaDef("configuracao_operacao", "public", "configuracoes-operacao",
                    List.of("configuracoes-operacao", "configuracao_operacao", "configuracao-operacao", "public.configuracao_operacao", "tb_configuracao_operacao", "sc_corporativo.tb_configuracao_operacao", "sc_corporativo.configuracao_operacao")),
            new TabelaDef("tb_auditoria_analise", "sc_operacao", "auditorias-analise",
                    List.of("auditorias-analise", "auditoria_analise", "auditoria-analise", "auditorias", "laudos", "sc_operacao.tb_auditoria_analise")),
            new TabelaDef("tb_catalogo_dados", "sc_auditoria", "catalogo-dados",
                    List.of("catalogo-dados", "catalogo_dados", "catalogo", "metadados", "sc_auditoria.tb_catalogo_dados")),
            new TabelaDef("tb_monitoramento_dau", "sc_auditoria", "monitoramento-dau",
                    List.of("monitoramento-dau", "monitoramento_dau", "dau", "metricas-dau", "sc_auditoria.tb_monitoramento_dau")),
            new TabelaDef("tb_auditoria_log", "sc_auditoria", "auditoria-log",
                    List.of("auditoria-log", "auditoria_log", "logs-auditoria", "sc_auditoria.tb_auditoria_log")),
            new TabelaDef("tb_dominio_motivo_parada", "sc_operacao", "dominios-parada",
                    List.of("dominios-parada", "dominio-motivo-parada", "dominio_parada", "sc_operacao.tb_dominio_motivo_parada")),
            new TabelaDef("tb_dominio_anomalia", "sc_operacao", "dominios-anomalia",
                    List.of("dominios-anomalia", "dominio-anomalia", "dominio_anomalia", "sc_operacao.tb_dominio_anomalia")),
            new TabelaDef("tb_veiculo_base", "sc_frota", "veiculos-base",
                    List.of("veiculos-base", "veiculo_base", "frota-base", "sc_frota.tb_veiculo_base")),
            new TabelaDef("vw_dim_empresa", "sc_corporativo", "vw-dim-empresa",
                    List.of("vw-dim-empresa", "dim_empresa", "dim-empresa", "sc_corporativo.vw_dim_empresa")),
            new TabelaDef("vw_dim_colaborador", "sc_corporativo", "vw-dim-colaborador",
                    List.of("vw-dim-colaborador", "dim_colaborador", "dim-colaborador", "sc_corporativo.vw_dim_colaborador")),
            new TabelaDef("vw_dim_frota", "sc_frota", "vw-dim-frota",
                    List.of("vw-dim-frota", "dim_frota", "dim-frota", "sc_frota.vw_dim_frota")),
            new TabelaDef("vw_dim_localidade", "sc_corporativo", "vw-dim-localidade",
                    List.of("vw-dim-localidade", "dim_localidade", "dim-localidade", "sc_corporativo.vw_dim_localidade")),
            new TabelaDef("vw_fato_transporte_animal", "sc_operacao", "vw-fato-transporte-animal",
                    List.of("vw-fato-transporte-animal", "fato_transporte_animal", "fato-transporte", "sc_operacao.vw_fato_transporte_animal"))
    );

    private static final Map<String, TabelaDef> ALIAS_MAP = new HashMap<>();

    static {
        for (TabelaDef def : CATALOGO) {
            ALIAS_MAP.put(def.aliasPrincipal().toLowerCase(Locale.ROOT), def);
            ALIAS_MAP.put(def.nome().toLowerCase(Locale.ROOT), def);
            if (def.schema() != null) {
                ALIAS_MAP.put((def.schema() + "." + def.nome()).toLowerCase(Locale.ROOT), def);
            }
            for (String alias : def.aliases()) {
                ALIAS_MAP.put(alias.toLowerCase(Locale.ROOT), def);
            }
        }
    }

    private final JdbcTemplate jdbcTemplate;

    public TempTabelasQueryService(@Autowired(required = false) JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TabelaMetadataDto> listarCatalogoTabelas() {
        List<TabelaMetadataDto> resultado = new ArrayList<>();
        for (TabelaDef def : CATALOGO) {
            try {
                ResolvedTable resolved = resolveWorkingTable(def);
                if (resolved != null) {
                    List<String> colunas = extrairColunas(resolved.qualifiedSqlName());
                    resultado.add(new TabelaMetadataDto(
                            resolved.tableName(),
                            resolved.schemaName(),
                            def.aliasPrincipal(),
                            resolved.count(),
                            colunas
                    ));
                } else {
                    resultado.add(new TabelaMetadataDto(
                            def.nome(),
                            def.schema() != null ? def.schema() : "public",
                            def.aliasPrincipal(),
                            0L,
                            List.of()
                    ));
                }
            } catch (Exception ex) {
                log.warn("Erro ao listar metadados da tabela {}: {}", def.aliasPrincipal(), ex.getMessage());
                resultado.add(new TabelaMetadataDto(
                        def.nome(),
                        def.schema() != null ? def.schema() : "public",
                        def.aliasPrincipal(),
                        0L,
                        List.of()
                ));
            }
        }
        return resultado;
    }

    public TabelaDadosResponse obterDadosTabela(String nomeOuAlias, Integer limite, Integer offset, String ordenarPor, String direcao) {
        return obterDadosTabela(nomeOuAlias, limite, offset, ordenarPor, direcao, null);
    }

    public TabelaDadosResponse obterDadosTabela(String nomeOuAlias, Integer limite, Integer offset, String ordenarPor, String direcao, String busca) {
        if (nomeOuAlias == null || nomeOuAlias.isBlank()) {
            throw new IllegalArgumentException("Nome ou alias de tabela não informado");
        }

        String trimmed = nomeOuAlias.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        TabelaDef def = ALIAS_MAP.get(normalized);

        if (def == null) {
            throw new IllegalArgumentException("Tabela não permitida ou desconhecida: " + nomeOuAlias);
        }

        int lim = (limite == null || limite <= 0) ? 1000 : Math.min(limite, 10000);
        int off = (offset == null || offset < 0) ? 0 : offset;

        ResolvedTable resolved = resolveWorkingTable(def);
        if (resolved == null) {
            if (!ALIAS_MAP.containsKey(normalized)) {
                throw new IllegalArgumentException("Tabela não permitida ou desconhecida: " + nomeOuAlias);
            }
            return new TabelaDadosResponse(
                    def.nome(),
                    def.schema() != null ? def.schema() : "public",
                    0L,
                    lim,
                    off,
                    List.of(),
                    List.of()
            );
        }

        List<String> colunasDisponiveis = extrairColunas(resolved.qualifiedSqlName());
        StringBuilder whereClause = new StringBuilder();
        List<Object> queryParams = new ArrayList<>();
        long totalRegistros = resolved.count();

        if (busca != null && !busca.isBlank() && !colunasDisponiveis.isEmpty() && jdbcTemplate != null) {
            String termoBusca = "%" + busca.trim().toLowerCase(Locale.ROOT) + "%";
            whereClause.append(" WHERE (");
            boolean first = true;
            for (String col : colunasDisponiveis) {
                if (COLUMN_NAME_PATTERN.matcher(col).matches()) {
                    if (!first) whereClause.append(" OR ");
                    whereClause.append("LOWER(CAST(").append(col).append(" AS TEXT)) LIKE ?");
                    queryParams.add(termoBusca);
                    first = false;
                }
            }
            whereClause.append(")");

            try {
                Long countFiltrado = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + resolved.qualifiedSqlName() + whereClause,
                        Long.class,
                        queryParams.toArray()
                );
                if (countFiltrado != null) {
                    totalRegistros = countFiltrado;
                }
            } catch (Exception ex) {
                log.trace("Não foi possível contar registros filtrados por busca: {}", ex.getMessage());
            }
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(resolved.qualifiedSqlName());
        sql.append(whereClause);

        if (ordenarPor != null && !ordenarPor.isBlank()) {
            String safeCol = ordenarPor.trim();
            if (!COLUMN_NAME_PATTERN.matcher(safeCol).matches()) {
                throw new IllegalArgumentException("Coluna de ordenação inválida: " + ordenarPor);
            }
            if (colunasDisponiveis != null && !colunasDisponiveis.isEmpty() && colunasDisponiveis.stream().noneMatch(c -> c.equalsIgnoreCase(safeCol))) {
                throw new IllegalArgumentException("Coluna de ordenação não existe na tabela: " + ordenarPor);
            }
            String safeDir = (direcao != null && direcao.trim().equalsIgnoreCase("desc")) ? "DESC" : "ASC";
            sql.append(" ORDER BY ").append(safeCol).append(" ").append(safeDir);
        }
        sql.append(" LIMIT ? OFFSET ?");
        queryParams.add(lim);
        queryParams.add(off);

        List<String> colunas = new ArrayList<>();
        List<Map<String, Object>> dados = null;

        if (jdbcTemplate != null) {
            try {
                dados = jdbcTemplate.query(sql.toString(), ps -> {
                    for (int i = 0; i < queryParams.size(); i++) {
                        ps.setObject(i + 1, queryParams.get(i));
                    }
                }, (ResultSet rs) -> {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    for (int i = 1; i <= colCount; i++) {
                        colunas.add(meta.getColumnLabel(i));
                    }
                    List<Map<String, Object>> list = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            String colName = meta.getColumnLabel(i);
                            Object val = rs.getObject(i);
                            row.put(colName, sanitizarValor(colName, val));
                        }
                        list.add(row);
                    }
                    return list;
                });
            } catch (DataAccessException ex) {
                log.error("Erro ao consultar dados da tabela {}: {}", resolved.qualifiedSqlName(), ex.getMessage());
                dados = List.of();
            }
        }

        if (dados == null) {
            dados = List.of();
        }
        if (colunas.isEmpty() && !dados.isEmpty()) {
            colunas.addAll(dados.get(0).keySet());
        }

        return new TabelaDadosResponse(
                resolved.tableName(),
                resolved.schemaName(),
                totalRegistros,
                lim,
                off,
                colunas,
                dados
        );
    }

    public Map<String, Object> obterTodasAsTabelas(Integer limitePorTabela) {
        int lim = (limitePorTabela == null || limitePorTabela <= 0) ? 1000 : Math.min(limitePorTabela, 5000);
        Map<String, Object> resultado = new LinkedHashMap<>();

        for (TabelaDef def : CATALOGO) {
            try {
                TabelaDadosResponse response = obterDadosTabela(def.aliasPrincipal(), lim, 0, null, null);
                resultado.put(def.aliasPrincipal(), response.dados());
            } catch (Exception ex) {
                log.warn("Erro ao obter dados consolidados de {}: {}", def.aliasPrincipal(), ex.getMessage());
                resultado.put(def.aliasPrincipal(), List.of());
            }
        }

        return resultado;
    }

    public GraficosResumoResponse obterResumoMetricas() {
        ResolvedTable resViagens = resolveByAlias("relatorios-viagem");
        long totalViagens = (resViagens != null) ? resViagens.count() : 0L;

        ResolvedTable resUsuarios = resolveByAlias("usuarios");
        long totalUsuarios = (resUsuarios != null) ? resUsuarios.count() : 0L;

        ResolvedTable resCavalo = resolveByAlias("veiculos-cavalo");
        long totalCavalo = (resCavalo != null) ? resCavalo.count() : 0L;

        ResolvedTable resCarreta = resolveByAlias("veiculos-carreta");
        long totalCarreta = (resCarreta != null) ? resCarreta.count() : 0L;

        ResolvedTable resParadas = resolveByAlias("paradas-imprevistas");
        long totalParadas = (resParadas != null) ? resParadas.count() : 0L;

        ResolvedTable resEmb = resolveByAlias("anomalias-embarque");
        long totalEmb = (resEmb != null) ? resEmb.count() : 0L;

        ResolvedTable resDes = resolveByAlias("anomalias-desembarque");
        long totalDes = (resDes != null) ? resDes.count() : 0L;

        long totalAnomalias = totalEmb + totalDes;

        Map<String, Long> viagensPorStatus = new LinkedHashMap<>();
        if (resViagens != null && jdbcTemplate != null) {
            try {
                String sql = "SELECT COALESCE(status, 'indefinido') AS st, COUNT(*) AS qtd FROM "
                        + resViagens.qualifiedSqlName() + " GROUP BY status ORDER BY qtd DESC";
                jdbcTemplate.query(sql, rs -> {
                    viagensPorStatus.put(rs.getString("st"), rs.getLong("qtd"));
                });
            } catch (Exception ex) {
                log.warn("Erro ao agregar viagensPorStatus: {}", ex.getMessage());
            }
        }

        Map<String, Long> paradasPorMotivo = new LinkedHashMap<>();
        if (resParadas != null && jdbcTemplate != null) {
            try {
                String sql = "SELECT COALESCE(motivo, 'indefinido') AS mot, COUNT(*) AS qtd FROM "
                        + resParadas.qualifiedSqlName() + " GROUP BY motivo ORDER BY qtd DESC";
                jdbcTemplate.query(sql, rs -> {
                    paradasPorMotivo.put(rs.getString("mot"), rs.getLong("qtd"));
                });
            } catch (Exception ex) {
                try {
                    String sqlFallback = "SELECT COALESCE(d.descricao, d.codigo, 'indefinido') AS mot, COUNT(*) AS qtd FROM "
                            + resParadas.qualifiedSqlName() + " p LEFT JOIN sc_operacao.tb_dominio_motivo_parada d ON d.id = p.dominio_parada_id "
                            + " GROUP BY COALESCE(d.descricao, d.codigo, 'indefinido') ORDER BY qtd DESC";
                    jdbcTemplate.query(sqlFallback, rs -> {
                        paradasPorMotivo.put(rs.getString("mot"), rs.getLong("qtd"));
                    });
                } catch (Exception ex2) {
                    log.warn("Erro ao agregar paradasPorMotivo: {}", ex2.getMessage());
                }
            }
        }

        Map<String, Long> anomaliasPorTipo = new LinkedHashMap<>();
        if (resEmb != null && jdbcTemplate != null) {
            try {
                String sql = "SELECT COALESCE(anomalia, 'indefinido') AS anom, COUNT(*) AS qtd FROM "
                        + resEmb.qualifiedSqlName() + " GROUP BY anomalia";
                jdbcTemplate.query(sql, rs -> {
                    anomaliasPorTipo.merge(rs.getString("anom"), rs.getLong("qtd"), Long::sum);
                });
            } catch (Exception ex) {
                try {
                    String sqlFallback = "SELECT COALESCE(d.descricao, d.codigo, 'indefinido') AS anom, COUNT(*) AS qtd FROM "
                            + resEmb.qualifiedSqlName() + " e LEFT JOIN sc_operacao.tb_dominio_anomalia d ON d.id = e.dominio_anomalia_id "
                            + " GROUP BY COALESCE(d.descricao, d.codigo, 'indefinido')";
                    jdbcTemplate.query(sqlFallback, rs -> {
                        anomaliasPorTipo.merge(rs.getString("anom"), rs.getLong("qtd"), Long::sum);
                    });
                } catch (Exception ex2) {
                    log.warn("Erro ao agregar anomaliasPorTipo (embarque): {}", ex2.getMessage());
                }
            }
        }
        if (resDes != null && jdbcTemplate != null) {
            try {
                String sql = "SELECT COALESCE(anomalia, 'indefinido') AS anom, COUNT(*) AS qtd FROM "
                        + resDes.qualifiedSqlName() + " GROUP BY anomalia";
                jdbcTemplate.query(sql, rs -> {
                    anomaliasPorTipo.merge(rs.getString("anom"), rs.getLong("qtd"), Long::sum);
                });
            } catch (Exception ex) {
                try {
                    String sqlFallback = "SELECT COALESCE(d.descricao, d.codigo, 'indefinido') AS anom, COUNT(*) AS qtd FROM "
                            + resDes.qualifiedSqlName() + " d_tab LEFT JOIN sc_operacao.tb_dominio_anomalia d ON d.id = d_tab.dominio_anomalia_id "
                            + " GROUP BY COALESCE(d.descricao, d.codigo, 'indefinido')";
                    jdbcTemplate.query(sqlFallback, rs -> {
                        anomaliasPorTipo.merge(rs.getString("anom"), rs.getLong("qtd"), Long::sum);
                    });
                } catch (Exception ex2) {
                    log.warn("Erro ao agregar anomaliasPorTipo (desembarque): {}", ex2.getMessage());
                }
            }
        }

        Map<String, Object> totaisGerais = new LinkedHashMap<>();
        totaisGerais.put("totalViagens", totalViagens);
        totaisGerais.put("totalUsuarios", totalUsuarios);
        totaisGerais.put("totalVeiculosCavalo", totalCavalo);
        totaisGerais.put("totalVeiculosCarreta", totalCarreta);
        totaisGerais.put("totalParadasImprevistas", totalParadas);
        totaisGerais.put("totalAnomaliasEmbarque", totalEmb);
        totaisGerais.put("totalAnomaliasDesembarque", totalDes);
        totaisGerais.put("totalAnomalias", totalAnomalias);

        ResolvedTable resDoc = resolveByAlias("documentos");
        totaisGerais.put("totalDocumentos", resDoc != null ? resDoc.count() : 0L);

        ResolvedTable resEmp = resolveByAlias("empresas");
        totaisGerais.put("totalEmpresas", resEmp != null ? resEmp.count() : 0L);

        ResolvedTable resFaz = resolveByAlias("fazendas");
        totaisGerais.put("totalFazendas", resFaz != null ? resFaz.count() : 0L);

        ResolvedTable resFri = resolveByAlias("unidades-frigorificas");
        totaisGerais.put("totalUnidadesFrigorificas", resFri != null ? resFri.count() : 0L);

        ResolvedTable resEnd = resolveByAlias("enderecos");
        totaisGerais.put("totalEnderecos", resEnd != null ? resEnd.count() : 0L);

        ResolvedTable resAss = resolveByAlias("assinaturas-motorista");
        totaisGerais.put("totalAssinaturasMotorista", resAss != null ? resAss.count() : 0L);

        ResolvedTable resOut = resolveByAlias("outbox-eventos");
        totaisGerais.put("totalOutboxEventos", resOut != null ? resOut.count() : 0L);
        ResolvedTable resAud = resolveByAlias("auditorias-analise");
        totaisGerais.put("totalAuditoriasAnalise", resAud != null ? resAud.count() : 0L);

        ResolvedTable resDau = resolveByAlias("monitoramento-dau");
        totaisGerais.put("totalMonitoramentoDau", resDau != null ? resDau.count() : 0L);

        ResolvedTable resCat = resolveByAlias("catalogo-dados");
        totaisGerais.put("totalCatalogoDados", resCat != null ? resCat.count() : 0L);

        ResolvedTable resLog = resolveByAlias("auditoria-log");
        totaisGerais.put("totalAuditoriaLog", resLog != null ? resLog.count() : 0L);

        ResolvedTable resFato = resolveByAlias("vw-fato-transporte-animal");
        totaisGerais.put("totalFatoTransporteAnimal", resFato != null ? resFato.count() : 0L);

        return new GraficosResumoResponse(
                totalViagens,
                totalUsuarios,
                totalCavalo,
                totalCarreta,
                totalParadas,
                totalAnomalias,
                viagensPorStatus,
                paradasPorMotivo,
                anomaliasPorTipo,
                totaisGerais
        );
    }

    private ResolvedTable resolveByAlias(String alias) {
        TabelaDef def = ALIAS_MAP.get(alias.toLowerCase(Locale.ROOT));
        return def != null ? resolveWorkingTable(def) : null;
    }

    private ResolvedTable resolveWorkingTable(TabelaDef def) {
        if (jdbcTemplate == null) {
            return new ResolvedTable(def.nome(), def.nome(), def.schema() != null ? def.schema() : "public", 0L);
        }

        List<CandidateInfo> candidates = buildCandidates(def);
        for (CandidateInfo candidate : candidates) {
            try {
                Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + candidate.sqlName(), Long.class);
                return new ResolvedTable(
                        candidate.sqlName(),
                        candidate.tableName(),
                        candidate.schemaName(),
                        count != null ? count : 0L
                );
            } catch (DataAccessException ex) {
                log.trace("Tentativa na tabela {} falhou: {}", candidate.sqlName(), ex.getMessage());
            }
        }
        return null;
    }

    private List<CandidateInfo> buildCandidates(TabelaDef def) {
        List<CandidateInfo> candidates = new ArrayList<>();
        String schema = def.schema();
        String nome = def.nome();

        if (schema != null && !schema.isBlank()) {
            candidates.add(new CandidateInfo(schema + "." + nome, nome, schema));
        }
        if (schema == null || !"public".equalsIgnoreCase(schema)) {
            candidates.add(new CandidateInfo("public." + nome, nome, "public"));
        }
        candidates.add(new CandidateInfo(nome, nome, schema != null ? schema : "public"));

        if (nome.startsWith("tb_")) {
            String semTb = nome.substring(3);
            if (schema != null && !schema.isBlank()) {
                candidates.add(new CandidateInfo(schema + "." + semTb, semTb, schema));
            }
            candidates.add(new CandidateInfo("public." + semTb, semTb, "public"));
            candidates.add(new CandidateInfo(semTb, semTb, "public"));
        } else {
            String comTb = "tb_" + nome;
            if (schema != null && !schema.isBlank()) {
                candidates.add(new CandidateInfo(schema + "." + comTb, comTb, schema));
            }
            candidates.add(new CandidateInfo("public." + comTb, comTb, "public"));
            candidates.add(new CandidateInfo(comTb, comTb, "public"));
        }

        if ("empresa".equalsIgnoreCase(nome) || "tb_empresa".equalsIgnoreCase(nome)) {
            candidates.add(new CandidateInfo("sc_corporativo.tb_empresa", "tb_empresa", "sc_corporativo"));
            candidates.add(new CandidateInfo("sc_corporativo.empresa", "empresa", "sc_corporativo"));
        } else if ("configuracao_operacao".equalsIgnoreCase(nome) || "tb_configuracao_operacao".equalsIgnoreCase(nome)) {
            candidates.add(new CandidateInfo("sc_corporativo.tb_configuracao_operacao", "tb_configuracao_operacao", "sc_corporativo"));
            candidates.add(new CandidateInfo("sc_corporativo.configuracao_operacao", "configuracao_operacao", "sc_corporativo"));
        } else if ("empresa_admin".equalsIgnoreCase(nome) || "tb_empresa_admin".equalsIgnoreCase(nome)) {
            candidates.add(new CandidateInfo("sc_corporativo.tb_empresa_admin", "tb_empresa_admin", "sc_corporativo"));
            candidates.add(new CandidateInfo("sc_corporativo.empresa_admin", "empresa_admin", "sc_corporativo"));
        }

        return candidates;
    }

    private List<String> extrairColunas(String sqlName) {
        if (jdbcTemplate == null) {
            return List.of();
        }
        try {
            return jdbcTemplate.query("SELECT * FROM " + sqlName + " LIMIT 0", rs -> {
                ResultSetMetaData meta = rs.getMetaData();
                int count = meta.getColumnCount();
                List<String> cols = new ArrayList<>(count);
                for (int i = 1; i <= count; i++) {
                    cols.add(meta.getColumnLabel(i));
                }
                return cols;
            });
        } catch (Exception ex) {
            log.debug("Não foi possível extrair colunas de {}: {}", sqlName, ex.getMessage());
            return List.of();
        }
    }

    private Object sanitizarValor(String colName, Object val) {
        if (colName != null) {
            String lower = colName.toLowerCase(Locale.ROOT);
            if (lower.contains("senha") || lower.contains("password")) {
                return "[PROTEGIDO]";
            }
        }
        if (val == null) {
            return null;
        }
        if (val instanceof byte[] bytes) {
            return "[BINARY DATA: " + bytes.length + " bytes]";
        }
        if (val instanceof Array sqlArray) {
            try {
                return sqlArray.getArray();
            } catch (Exception ignored) {
                return val.toString();
            }
        }
        if ("org.postgresql.util.PGobject".equals(val.getClass().getName())) {
            try {
                return val.getClass().getMethod("getValue").invoke(val);
            } catch (Exception ignored) {
                return val.toString();
            }
        }
        return val;
    }
}
