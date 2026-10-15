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
    @org.springframework.transaction.annotation.Transactional
    void deveAplicarV16ECriarOutboxNoPostgresReal() {
        assertThat(postgres.isRunning()).isTrue();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'sc_operacao' AND table_name = 'tb_outbox_evento'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);

        Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'sc_operacao' AND indexname = 'idx_outbox_pendentes'",
                Integer.class
        );
        assertThat(indexCount).isEqualTo(1);
    }

    @Test
    void deveExecutarMigracoesEVersaoNoPostgresReal() {
        assertThat(postgres.isRunning()).isTrue();

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
}
