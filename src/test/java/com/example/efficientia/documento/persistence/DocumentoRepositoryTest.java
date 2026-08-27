package com.example.efficientia.documento.persistence;

import com.example.efficientia.documento.domain.DocumentoCursor;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.TipoDocumento;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
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
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(DocumentoRepositoryTest.PersistenceConfig.class)
@Transactional
class DocumentoRepositoryTest {

    private final DocumentoRepository repository;

    @Autowired
    DocumentoRepositoryTest(DocumentoRepository repository) {
        this.repository = repository;
    }

    @Test
    void deveSalvarEConsultarPorUuidEIdempotencyKey() {
        UUID idempotencyKey = UUID.randomUUID();

        DocumentoEntity salvo = repository.saveAndFlush(
                novoDocumentoArquivo(idempotencyKey, Instant.parse("2026-08-25T12:00:00Z"), 1)
        );

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getVersao()).isZero();
        assertThat(repository.findById(salvo.getId())).contains(salvo);
        assertThat(repository.findByIdempotencyKey(idempotencyKey)).contains(salvo);
        assertThat(repository.existsByIdempotencyKey(idempotencyKey)).isTrue();
    }

    @Test
    void deveRejeitarIdempotencyKeyDuplicada() {
        UUID idempotencyKey = UUID.randomUUID();
        repository.saveAndFlush(
                novoDocumentoArquivo(idempotencyKey, Instant.parse("2026-08-25T12:00:00Z"), 1)
        );

        DocumentoEntity duplicado = novoDocumentoArquivo(
                idempotencyKey,
                Instant.parse("2026-08-25T12:01:00Z"),
                2
        );

        assertThatThrownBy(() -> repository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void devePaginarPorViagem() {
        repository.save(novoDocumentoArquivo(UUID.randomUUID(), Instant.parse("2026-08-25T12:00:00Z"), 1));
        repository.save(novoDocumentoArquivo(UUID.randomUUID(), Instant.parse("2026-08-25T12:01:00Z"), 1));
        repository.save(novoDocumentoArquivo(UUID.randomUUID(), Instant.parse("2026-08-25T12:02:00Z"), 2));
        repository.flush();

        var pagina = repository.findByViagemId(1, PageRequest.of(0, 1));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).hasSize(1);
    }

    @Test
    void deveContinuarListagemDepoisDoCursor() {
        Instant inicio = Instant.parse("2026-08-25T12:00:00Z");
        repository.save(novoDocumentoArquivo(UUID.randomUUID(), inicio, 1));
        repository.save(novoDocumentoArquivo(UUID.randomUUID(), inicio.plus(1, ChronoUnit.MINUTES), 1));
        repository.save(novoDocumentoArquivo(UUID.randomUUID(), inicio.plus(2, ChronoUnit.MINUTES), 1));
        repository.flush();

        var primeiraPagina = repository.buscarProximos(null, 2);
        DocumentoEntity ultimo = primeiraPagina.getContent()
                .get(primeiraPagina.getContent().size() - 1);
        var proximaPagina = repository.buscarProximos(
                new DocumentoCursor(ultimo.getCriadoEm(), ultimo.getId()),
                2
        );

        assertThat(primeiraPagina.getContent()).hasSize(2);
        assertThat(primeiraPagina.hasNext()).isTrue();
        assertThat(proximaPagina.getContent()).hasSize(1);
        assertThat(proximaPagina.getContent().get(0).getCriadoEm())
                .isAfter(ultimo.getCriadoEm());
    }

    private DocumentoEntity novoDocumentoArquivo(UUID idempotencyKey, Instant criadoEm, int viagemId) {
        DocumentoEntity entity = new DocumentoEntity();
        entity.setViagemId(viagemId);
        entity.setTipoDocumento(TipoDocumento.RELATORIO_VIAGEM);
        entity.setOrigem(OrigemDocumento.UPLOAD);
        entity.setDescricao("Documento de teste");
        entity.setNomeOriginal("relatorio.pdf");
        entity.setMimeType("application/pdf");
        entity.setTamanhoBytes(1024L);
        entity.setSha256("a".repeat(64));
        entity.setStorageKey("documentos/" + UUID.randomUUID() + ".pdf");
        entity.setIdempotencyKey(idempotencyKey);
        entity.setCriadoEm(criadoEm);
        return entity;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = DocumentoRepository.class)
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
            factory.setPackagesToScan(DocumentoEntity.class.getPackageName());
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
