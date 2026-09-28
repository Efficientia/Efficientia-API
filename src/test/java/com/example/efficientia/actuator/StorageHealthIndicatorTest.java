package com.example.efficientia.actuator;

import com.example.efficientia.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StorageHealthIndicatorTest {

    @Test
    void deveRetornarStatusUpQuandoDiretorioExisteEForGravavel(@TempDir Path tempDir) {
        StorageProperties properties = new StorageProperties(
                tempDir,
                DataSize.ofMegabytes(25),
                DataSize.ofMegabytes(10)
        );
        StorageHealthIndicator indicator = new StorageHealthIndicator(properties);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("storagePath");
    }

    @Test
    void deveCriarDiretorioERetornarStatusUpQuandoDiretorioNaoExistir(@TempDir Path tempDir) {
        Path subFolder = tempDir.resolve("pasta_nova");
        StorageProperties properties = new StorageProperties(
                subFolder,
                DataSize.ofMegabytes(25),
                DataSize.ofMegabytes(10)
        );
        StorageHealthIndicator indicator = new StorageHealthIndicator(properties);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(subFolder).exists();
        assertThat(subFolder).isDirectory();
    }
}
