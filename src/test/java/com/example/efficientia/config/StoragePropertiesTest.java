package com.example.efficientia.config;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoragePropertiesTest {

    @Test
    void deveRejeitarLimitesNaoPositivos() {
        assertThatThrownBy(() -> new StorageProperties(
                Path.of("storage"),
                DataSize.ofBytes(0),
                DataSize.ofMegabytes(10)
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new StorageProperties(
                Path.of("storage"),
                DataSize.ofMegabytes(25),
                DataSize.ofBytes(-1)
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
