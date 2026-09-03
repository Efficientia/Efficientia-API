package com.example.efficientia.actuator;

import com.example.efficientia.config.StorageProperties;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component("privateStorage")
public class StorageHealthIndicator implements HealthIndicator {

    private final Path rootPath;

    public StorageHealthIndicator(StorageProperties storageProperties) {
        this.rootPath = storageProperties.path().toAbsolutePath().normalize();
    }

    @Override
    public Health health() {
        if (!Files.exists(rootPath)) {
            return Health.down()
                    .withDetail("storagePath", rootPath.toString())
                    .withDetail("reason", "Diretório do storage não existe.")
                    .build();
        }
        if (!Files.isDirectory(rootPath)) {
            return Health.down()
                    .withDetail("storagePath", rootPath.toString())
                    .withDetail("reason", "O caminho configurado não é um diretório.")
                    .build();
        }
        if (!Files.isWritable(rootPath)) {
            return Health.down()
                    .withDetail("storagePath", rootPath.toString())
                    .withDetail("reason", "Sem permissão de escrita no diretório do storage.")
                    .build();
        }
        return Health.up()
                .withDetail("storagePath", rootPath.toString())
                .build();
    }
}
