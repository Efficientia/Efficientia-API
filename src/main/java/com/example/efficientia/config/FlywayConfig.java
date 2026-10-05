package com.example.efficientia.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                log.info("Executando flyway.repair() preventivo para limpar eventuais falhas anteriores e alinhar checksums...");
                flyway.repair();
            } catch (Exception ex) {
                log.warn("Aviso ao executar flyway.repair(): {}", ex.getMessage());
            }
            log.info("Executando flyway.migrate()...");
            flyway.migrate();
        };
    }
}
