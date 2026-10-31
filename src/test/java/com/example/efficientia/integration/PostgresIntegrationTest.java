package com.example.efficientia.integration;

import com.example.efficientia.exportacao.domain.EstadoExportacao;
import com.example.efficientia.exportacao.persistence.ExportacaoEntity;
import com.example.efficientia.exportacao.persistence.ExportacaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class PostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("efficientia")
            .withUsername("efficientia")
            .withPassword("efficientia");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.autoconfigure.exclude", () -> "");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private com.example.efficientia.documento.persistence.DocumentoRepository documentoRepository;

    @Autowired
    private ExportacaoRepository exportacaoRepository;

    @Autowired
    private com.example.efficientia.relatorioviagem.service.RelatorioViagemService relatorioViagemService;
    @Autowired
    private com.example.efficientia.temp.service.TempTabelasQueryService tempTabelasQueryService;

    @Test
    @org.springframework.transaction.annotation.Transactional
    void permiteNovoDiarioAposEnvioEOutrosRascunhosDoMesmoMotorista() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '21'", Boolean.class)).isTrue();
        jdbcTemplate.update("""
                INSERT INTO sc_corporativo.tb_usuario (id, tipo, cpf, nome, senha_hash)
                VALUES (8991, 'motorista', '00000008991', 'Motorista multiplos diarios', 'hash')
                ON CONFLICT DO NOTHING
                """);
        String gta = "MULTI-" + UUID.randomUUID();
        Integer primeiroId = jdbcTemplate.queryForObject("""
                INSERT INTO sc_operacao.tb_relatorio_viagem (motorista_id, status, numero_gta)
                VALUES (8991, 'rascunho', ?) RETURNING id
                """, Integer.class, gta + "-1");
        jdbcTemplate.update("UPDATE sc_operacao.tb_relatorio_viagem SET status = 'pendente' WHERE id = ?",
                primeiroId);
        Integer segundoId = jdbcTemplate.queryForObject("""
                INSERT INTO sc_operacao.tb_relatorio_viagem (motorista_id, status, numero_gta)
                VALUES (8991, 'rascunho', ?) RETURNING id
                """, Integer.class, gta + "-2");
        Integer terceiroId = jdbcTemplate.queryForObject("""
                INSERT INTO sc_operacao.tb_relatorio_viagem (motorista_id, status, numero_gta)
                VALUES (8991, 'rascunho', ?) RETURNING id
                """, Integer.class, gta + "-3");
        assertThat(List.of(primeiroId, segundoId, terceiroId)).doesNotHaveDuplicates();
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void deveAplicarV15EAceitarRascunhoParcialComLocaisDigitadosNoPostgres() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '15'", Boolean.class)).isTrue();
        List<String> camposDasEtapas = List.of(
                "fazenda_id", "manobrista_id", "curraleiro_id", "cavalo_id", "carreta_id",
                "numero_gta", "numero_nota_fiscal", "data_embarque", "horario_embarque",
                "horario_saida_propriedade", "km_saida_embarcadouro", "data_chegada_unidade",
                "horario_chegada_unidade", "horario_desembarque", "km_chegada_desembarcadouro",
                "numero_curral", "sirene_re_funcionou", "qtd_machos", "qtd_femeas", "qtd_marrucos",
                "qtd_em_pe", "qtd_deitado", "qtd_morto", "qtd_emergencia",
                "url_assinatura_pecuarista", "url_assinatura_motorista", "url_assinatura_manobrista",
                "url_assinatura_curraleiro");
        for (String campo : camposDasEtapas) {
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT is_nullable FROM information_schema.columns
                    WHERE table_schema = 'sc_operacao' AND table_name = 'tb_relatorio_viagem'
                      AND column_name = ?
                    """, String.class, campo)).as("nulabilidade de %s após V15", campo).isEqualTo("YES");
        }

        jdbcTemplate.update("""
                INSERT INTO sc_corporativo.tb_usuario (id, tipo, cpf, nome, senha_hash)
                VALUES (18, 'motorista', '12345678918', 'Motorista rascunho', 'hash')
                ON CONFLICT DO NOTHING
                """);
        Integer id = jdbcTemplate.queryForObject("""
                INSERT INTO sc_operacao.tb_relatorio_viagem
                    (motorista_id, status, numero_gta, numero_nota_fiscal, nome_origem, nome_destino)
                VALUES (18, 'rascunho', '6767a', '56767', 'Fazenda Do Osmar', 'Fazenda Do Noé')
                RETURNING id
                """, Integer.class);
        assertThat(id).isNotNull();
        assertThat(jdbcTemplate.queryForMap("""
                SELECT status, fazenda_id, unidade_frigorifica_id, data_chegada_unidade,
                       nome_origem, nome_destino
                FROM sc_operacao.tb_relatorio_viagem WHERE id = ?
                """, id)).containsEntry("status", "rascunho")
                .containsEntry("fazenda_id", null)
                .containsEntry("unidade_frigorifica_id", null)
                .containsEntry("data_chegada_unidade", null)
                .containsEntry("nome_origem", "Fazenda Do Osmar")
                .containsEntry("nome_destino", "Fazenda Do Noé");
    }


    @Test
    void deveExecutarMigracoesEVersaoNoPostgresReal() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '18'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '19'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'sc_operacao' AND indexname = 'idx_api_relatorio_motorista_status'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT data_type FROM information_schema.columns
                WHERE table_schema = 'sc_corporativo'
                  AND table_name = 'tb_unidade_frigorifica' AND column_name = 'id'
                """, String.class)).isEqualTo("integer");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.unidade_frigorifica') IS NULL", Boolean.class)).isTrue();
        Integer outboxTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'sc_operacao' AND table_name = 'tb_outbox_evento'",
                Integer.class
        );
        assertThat(outboxTableCount).isEqualTo(1);

        Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'sc_operacao' AND indexname = 'idx_outbox_pendentes'",
                Integer.class
        );
        assertThat(indexCount).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT data_type FROM information_schema.columns
                WHERE table_schema = 'sc_auditoria' AND table_name = 'tb_monitoramento_dau'
                  AND column_name = 'usuario_id'
                """, String.class)).isEqualTo("uuid");
        UUID dauUsuarioId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO sc_auditoria.tb_monitoramento_dau (usuario_id)
                VALUES (?) ON CONFLICT (usuario_id, data_acesso) DO NOTHING
                """, dauUsuarioId);
        jdbcTemplate.update("""
                INSERT INTO sc_auditoria.tb_monitoramento_dau (usuario_id)
                VALUES (?) ON CONFLICT (usuario_id, data_acesso) DO NOTHING
                """, dauUsuarioId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sc_auditoria.tb_monitoramento_dau WHERE usuario_id = ?",
                Integer.class, dauUsuarioId)).isEqualTo(1);



        jdbcTemplate.execute("INSERT INTO sc_corporativo.tb_usuario (id, tipo, cpf, nome, senha_hash) VALUES (10, 'motorista', '12345678901', 'Motorista Teste', 'hash') ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO sc_frota.tb_veiculo_cavalo (id, placa) VALUES (10, 'ABC1234') ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO sc_frota.tb_veiculo_carreta (id, placa, capacidade_cabecas) VALUES (10, 'DEF5678', 50) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO sc_corporativo.tb_endereco (id, cep, logradouro, numero, cidade, estado) VALUES (10, '01000000', 'Logradouro Teste', '100', 'Cidade', 'SP') ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO sc_corporativo.tb_fazenda (id, pecuarista_id, endereco_id, nome) VALUES (10, 10, 10, 'Fazenda Teste') ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO sc_operacao.tb_relatorio_viagem (id, fazenda_id, motorista_id, manobrista_id, curraleiro_id, cavalo_id, carreta_id, numero_gta, numero_nota_fiscal, data_embarque, horario_embarque, horario_saida_propriedade, km_saida_embarcadouro, data_chegada_unidade, horario_chegada_unidade, horario_desembarque, km_chegada_desembarcadouro, numero_curral, sirene_re_funcionou, url_assinatura_pecuarista, url_assinatura_motorista, url_assinatura_manobrista, url_assinatura_curraleiro) VALUES (100, 10, 10, 10, 10, 10, 10, 'GTA12345', 'NF12345', '2026-09-03', '08:00:00', '09:00:00', 100, '2026-09-03', '12:00:00', '13:00:00', 200, 'C1', true, 'url1', 'url2', 'url3', 'url4') ON CONFLICT DO NOTHING");

        com.example.efficientia.documento.persistence.DocumentoEntity doc = new com.example.efficientia.documento.persistence.DocumentoEntity();
        doc.setId(UUID.randomUUID());
        doc.setViagemId(100);
        doc.setTipoDocumento(com.example.efficientia.documento.domain.TipoDocumento.RELATORIO_VIAGEM);
        doc.setOrigem(com.example.efficientia.documento.domain.OrigemDocumento.UPLOAD);
        doc.setIdempotencyKey(UUID.randomUUID());
        doc.setNomeOriginal("teste.pdf");
        doc.setMimeType("application/pdf");
        doc.setTamanhoBytes(2048L);
        doc.setSha256("a".repeat(64));
        doc.setStorageKey("documentos/" + doc.getId() + "/teste.pdf");
        doc = documentoRepository.save(doc);

        UUID exportId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        ExportacaoEntity entity = new ExportacaoEntity();
        entity.setId(exportId);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setEstado(EstadoExportacao.NA_FILA);
        entity.setTamanhoOrigemBytes(2048L);
        entity.setDocumentoIds(List.of(doc.getId()));
        entity.setCriadoEm(Instant.now());
        entity.setAtualizadoEm(Instant.now());

        ExportacaoEntity salva = exportacaoRepository.save(entity);
        assertThat(salva.getId()).isEqualTo(exportId);

        var buscada = exportacaoRepository.findByIdempotencyKey(idempotencyKey);
        assertThat(buscada).isPresent();
        assertThat(buscada.get().getEstado()).isEqualTo(EstadoExportacao.NA_FILA);
        assertThat(buscada.get().getTamanhoOrigemBytes()).isEqualTo(2048L);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void buscaDeRelatoriosCombinaStatusGtaEExcluiRelatoriosRemovidos() {
        String busca = "SRCH-" + UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO sc_corporativo.tb_usuario (id, tipo, cpf, nome, senha_hash)
                VALUES
                    (21, 'motorista', '12345678921', 'Motorista busca P', 'hash'),
                    (22, 'motorista', '12345678922', 'Motorista busca A', 'hash'),
                    (23, 'motorista', '12345678923', 'Motorista busca X', 'hash')
                ON CONFLICT DO NOTHING
                """);
        Integer pendenteId = jdbcTemplate.queryForObject("""
                INSERT INTO sc_operacao.tb_relatorio_viagem (motorista_id, status, numero_gta)
                VALUES (21, 'pendente', ?)
                RETURNING id
                """, Integer.class, "GTA-" + busca + "-P");
        Integer aprovadoId = jdbcTemplate.queryForObject("""
                INSERT INTO sc_operacao.tb_relatorio_viagem (motorista_id, status, numero_gta)
                VALUES (22, 'aprovado', ?)
                RETURNING id
                """, Integer.class, "GTA-" + busca + "-A");
        Integer excluidoId = jdbcTemplate.queryForObject("""
                INSERT INTO sc_operacao.tb_relatorio_viagem (motorista_id, status, numero_gta)
                VALUES (23, 'excluido', ?)
                RETURNING id
                """, Integer.class, "GTA-" + busca + "-X");

        var porStatusEGta = relatorioViagemService.listarComFiltros(0, 20, "PENDENTE", busca);
        assertThat(porStatusEGta.total()).isEqualTo(1);
        assertThat(porStatusEGta.itens()).extracting(item -> item.id()).containsExactly(pendenteId);

        var porGta = relatorioViagemService.listarComFiltros(0, 20, null, busca);
        assertThat(porGta.total()).isEqualTo(2);
        assertThat(porGta.itens()).extracting(item -> item.id())
                .containsExactlyInAnyOrder(pendenteId, aprovadoId)
                .doesNotContain(excluidoId);

        var genericSearch = tempTabelasQueryService.obterDadosTabela(
                "relatorios-viagem", 20, 0, null, null, busca.toLowerCase(java.util.Locale.ROOT));
        assertThat(genericSearch.totalRegistros()).isEqualTo(3);
        assertThat(genericSearch.dados()).hasSize(3);
    }
}
