package com.example.efficientia.exportacao.persistence;

import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.TipoDocumento;
import com.example.efficientia.documento.persistence.DocumentoEntity;
import com.example.efficientia.exportacao.domain.EstadoExportacao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(ExportacaoRepositoryTest.PersistenceConfig.class)
@Transactional
class ExportacaoRepositoryTest {

    private final ExportacaoRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    ExportacaoRepositoryTest(ExportacaoRepository repository) {
        this.repository = repository;
    }

    @Test
    void devePersistirEstadoEOrdemDosDocumentosEConsultarPorIdempotencia() {
        UUID primeiroId = UUID.randomUUID();
        UUID segundoId = UUID.randomUUID();
        persistirDocumento(primeiroId);
        persistirDocumento(segundoId);
        UUID idempotencyKey = UUID.randomUUID();

        ExportacaoEntity salvo = repository.saveAndFlush(
                novaExportacao(idempotencyKey, List.of(segundoId, primeiroId))
        );
        UUID exportacaoId = salvo.getId();
        entityManager.clear();

        ExportacaoEntity recarregado = repository.findById(exportacaoId).orElseThrow();
        assertThat(recarregado.getDocumentoIds()).containsExactly(segundoId, primeiroId);
        assertThat(recarregado.getEstado()).isEqualTo(EstadoExportacao.NA_FILA);
        assertThat(recarregado.getTamanhoOrigemBytes()).isEqualTo(2048L);
        assertThat(recarregado.getVersao()).isZero();
        assertThat(repository.findByIdempotencyKey(idempotencyKey))
                .map(ExportacaoEntity::getId)
                .contains(exportacaoId);
    }

    @Test
    void deveRejeitarIdempotencyKeyDuplicada() {
        UUID primeiroId = UUID.randomUUID();
        UUID segundoId = UUID.randomUUID();
        persistirDocumento(primeiroId);
        persistirDocumento(segundoId);
        UUID idempotencyKey = UUID.randomUUID();
        repository.saveAndFlush(novaExportacao(idempotencyKey, List.of(primeiroId)));

        ExportacaoEntity duplicada = novaExportacao(idempotencyKey, List.of(segundoId));

        assertThatThrownBy(() -> repository.saveAndFlush(duplicada))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private ExportacaoEntity novaExportacao(UUID idempotencyKey, List<UUID> documentoIds) {
        ExportacaoEntity entity = new ExportacaoEntity();
        entity.setIdempotencyKey(idempotencyKey);
        entity.setDocumentoIds(documentoIds);
        entity.setEstado(EstadoExportacao.NA_FILA);
        entity.setSolicitadoPor(42);
        entity.setTamanhoOrigemBytes(documentoIds.size() * 1024L);
        entity.setCriadoEm(Instant.parse("2026-09-03T10:00:00Z"));
        return entity;
    }

    private void persistirDocumento(UUID id) {
        DocumentoEntity documento = new DocumentoEntity();
        documento.setId(id);
        documento.setViagemId(1);
        documento.setTipoDocumento(TipoDocumento.RELATORIO_VIAGEM);
        documento.setOrigem(OrigemDocumento.UPLOAD);
        documento.setNomeOriginal(id + ".pdf");
        documento.setMimeType("application/pdf");
        documento.setTamanhoBytes(1024L);
        documento.setSha256("a".repeat(64));
        documento.setStorageKey("documentos/" + id + ".pdf");
        documento.setIdempotencyKey(UUID.randomUUID());
        documento.setCriadoEm(Instant.parse("2026-09-03T09:00:00Z"));
        entityManager.persist(documento);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = ExportacaoRepository.class)
    static class PersistenceConfig {

        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan(
                    ExportacaoEntity.class.getPackageName(),
                    DocumentoEntity.class.getPackageName()
            );
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of(
                    "hibernate.hbm2ddl.auto", "create-drop",
                    "hibernate.show_sql", "false"
            ));
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }
    }
}
