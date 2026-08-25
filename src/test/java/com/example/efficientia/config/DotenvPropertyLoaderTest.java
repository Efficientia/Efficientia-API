package com.example.efficientia.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DotenvPropertyLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void deveRetornarMapaVazioQuandoArquivoNaoExistir() {
        assertThat(DotenvPropertyLoader.load(tempDir.resolve(".env"))).isEmpty();
    }

    @Test
    void deveInterpretarAspasEConverterUrlDoSupabaseParaJdbc() {
        var properties = DotenvPropertyLoader.parse(List.of(
                "# Configuração local",
                "SUPABASE_DB_URL=\"postgresql://usuario:senha@host:5432/postgres?sslmode=require\"",
                "SUPABASE_DB_USERNAME='usuario.pooler'",
                "SUPABASE_DB_PASSWORD=\"senha=com-igual\"",
                "DB_SCHEMA=public"
        ));

        assertThat(properties)
                .containsEntry("SUPABASE_DB_URL", "jdbc:postgresql://host:5432/postgres?sslmode=require")
                .containsEntry("SUPABASE_DB_USERNAME", "usuario.pooler")
                .containsEntry("SUPABASE_DB_PASSWORD", "senha=com-igual")
                .containsEntry("DB_SCHEMA", "public");
    }

    @Test
    void deveRejeitarLinhaSemSeparador() {
        assertThatThrownBy(() -> DotenvPropertyLoader.parse(List.of("SUPABASE_DB_URL")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("linha 1")
                .hasMessageContaining("CHAVE=VALOR");
    }
}
