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
    void deveRetornarStatusDownQuandoDiretorioNaoExistir() {
        Path fakePath = Path.of("caminho_inexistente_que_nao_deve_existir_12345");
        StorageProperties properties = new StorageProperties(
                fakePath,
                DataSize.ofMegabytes(25),
                DataSize.ofMegabytes(10)
        );
        StorageHealthIndicator indicator = new StorageHealthIndicator(properties);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("reason", "Diretório do storage não existe.");
    }
}
