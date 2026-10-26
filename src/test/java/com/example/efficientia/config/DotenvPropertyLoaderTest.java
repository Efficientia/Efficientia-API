package com.example.efficientia.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DotenvPropertyLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve retornar mapa vazio quando arquivo .env não existir")
    void deveRetornarMapaVazioQuandoArquivoNaoExistir() {
        assertThat(DotenvPropertyLoader.load(tempDir.resolve(".env"))).isEmpty();
    }

    @Test
    @DisplayName("Deve carregar propriedades de arquivo existente com load()")
    void deveCarregarArquivoExistenteComLoad() throws IOException {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "APP_NAME=Efficientia\nDB_PORT=5432\n");

        var properties = DotenvPropertyLoader.load(envFile);

        assertThat(properties)
                .containsEntry("APP_NAME", "Efficientia")
                .containsEntry("DB_PORT", "5432");
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando ocorrer erro de I/O ao carregar")
    void deveLancarIllegalStateExceptionQuandoFalharAoLerArquivo() {
        assertThatThrownBy(() -> DotenvPropertyLoader.load(tempDir))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Não foi possível ler o arquivo .env.");
    }

    @Test
    @DisplayName("Deve interpretar aspas e converter URL do Supabase para JDBC")
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
    @DisplayName("Deve extrair usuário e senha de URL com userInfo")
    void deveExtrairUsuarioESenhaDeUrlComUserInfo() {
        var properties = DotenvPropertyLoader.parse(List.of(
                "SUPABASE_DB_URL=postgresql://admin:segredo123@host:5432/banco"
        ));

        assertThat(properties)
                .containsEntry("SUPABASE_DB_URL", "jdbc:postgresql://host:5432/banco")
                .containsEntry("SUPABASE_DB_USERNAME", "admin")
                .containsEntry("SUPABASE_DB_PASSWORD", "segredo123");
    }

    @Test
    @DisplayName("Deve decodificar credenciais com caracteres especiais via URL")
    void deveDecodificarCredenciaisComCaracteresEspeciais() {
        var properties = DotenvPropertyLoader.parse(List.of(
                "SUPABASE_DB_URL=postgresql://meu%20user:minha%40senha@host:5432/banco"
        ));

        assertThat(properties)
                .containsEntry("SUPABASE_DB_USERNAME", "meu user")
                .containsEntry("SUPABASE_DB_PASSWORD", "minha@senha");
    }

    @Test
    @DisplayName("Deve preservar URL já com prefixo jdbc:")
    void devePreservarUrlJaComPrefixoJdbc() {
        var properties = DotenvPropertyLoader.parse(List.of(
                "SUPABASE_DB_URL=jdbc:postgresql://host:5432/db"
        ));

        assertThat(properties)
                .containsEntry("SUPABASE_DB_URL", "jdbc:postgresql://host:5432/db")
                .doesNotContainKey("SUPABASE_DB_USERNAME")
                .doesNotContainKey("SUPABASE_DB_PASSWORD");
    }

    @Test
    @DisplayName("Deve preservar URL que não seja Postgres")
    void devePreservarUrlQueNaoSejaPostgres() {
        var properties = DotenvPropertyLoader.parse(List.of(
                "DATABASE_URL=https://api.exemplo.com"
        ));

        assertThat(properties)
                .containsEntry("DATABASE_URL", "https://api.exemplo.com")
                .containsEntry("SUPABASE_DB_URL", "https://api.exemplo.com");
    }

    @Test
    @DisplayName("Deve ignorar linhas com valores em branco preservando fallbacks")
    void deveIgnorarLinhasComValoresEmBranco() {
        var properties = DotenvPropertyLoader.parse(List.of(
                "DB_POOL_MAX_SIZE=   ",
                "CHAVE=\"\"",
                "OUTRA=''",
                "VALIDA=10"
        ));

        assertThat(properties)
                .containsEntry("VALIDA", "10")
                .doesNotContainKey("DB_POOL_MAX_SIZE")
                .doesNotContainKey("CHAVE")
                .doesNotContainKey("OUTRA");
    }

    @Test
    @DisplayName("Deve popular alias Supabase quando apenas DATABASE_URL ou SUPABASE_DB_USER for fornecida")
    void devePopularAliasSupabaseQuandoDatabaseUrlFornecida() {
        var properties = DotenvPropertyLoader.parse(List.of(
                "DATABASE_URL=postgresql://host:5432/db",
                "SUPABASE_DB_USER=admin_user"
        ));

        assertThat(properties)
                .containsEntry("DATABASE_URL", "jdbc:postgresql://host:5432/db")
                .containsEntry("SUPABASE_DB_URL", "jdbc:postgresql://host:5432/db")
                .containsEntry("SUPABASE_DB_USER", "admin_user")
                .containsEntry("SUPABASE_DB_USERNAME", "admin_user");
    }

    @Test
    @DisplayName("Deve lançar exceção quando URL for inválida")
    void deveLancarExcecaoQuandoUrlForInvalida() {
        assertThatThrownBy(() -> DotenvPropertyLoader.parse(List.of(
                "SUPABASE_DB_URL=postgresql://invalid host with spaces:5432/db"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPABASE_DB_URL possui um formato inválido.");
    }

    @Test
    @DisplayName("Deve lançar exceção quando host for vazio")
    void deveLancarExcecaoQuandoHostForVazio() {
        assertThatThrownBy(() -> DotenvPropertyLoader.parse(List.of(
                "DATABASE_URL=postgresql:///banco"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DATABASE_URL deve informar um host válido.");
    }

    @Test
    @DisplayName("Deve rejeitar linha sem separador '='")
    void deveRejeitarLinhaSemSeparador() {
        assertThatThrownBy(() -> DotenvPropertyLoader.parse(List.of("SUPABASE_DB_URL")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("linha 1")
                .hasMessageContaining("CHAVE=VALOR");
    }

    @Test
    @DisplayName("Deve rejeitar linha iniciando com '='")
    void deveRejeitarLinhaIniciandoComSeparador() {
        assertThatThrownBy(() -> DotenvPropertyLoader.parse(List.of("=VALOR_SEM_CHAVE")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("linha 1")
                .hasMessageContaining("CHAVE=VALOR");
    }

    @Test
    @DisplayName("Deve rejeitar chave que não respeita o padrão válido")
    void deveRejeitarChaveInvalida() {
        assertThatThrownBy(() -> DotenvPropertyLoader.parse(List.of("123_INVALIDA=valor")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("linha 1")
                .hasMessageContaining("CHAVE=VALOR");
    }

    @Test
    @DisplayName("Deve suportar linhas com prefixo export")
    void deveSuportarLinhasComPrefixoExport() {
        var properties = DotenvPropertyLoader.parse(List.of(
                "export CHAVE=valor",
                "export   OUTRA_CHAVE='outro valor'"
        ));

        assertThat(properties)
                .containsEntry("CHAVE", "valor")
                .containsEntry("OUTRA_CHAVE", "outro valor");
    }

    @Test
    @DisplayName("Não deve sobrescrever credenciais pré-existentes com userInfo da URL")
    void deveNaoSobrescreverCredenciaisJaExistentes() {
        var properties = DotenvPropertyLoader.parse(List.of(
                "SUPABASE_DB_USERNAME=usuario_existente",
                "SUPABASE_DB_PASSWORD=senha_existente",
                "SUPABASE_DB_URL=postgresql://outro_usuario:outra_senha@host:5432/banco"
        ));

        assertThat(properties)
                .containsEntry("SUPABASE_DB_USERNAME", "usuario_existente")
                .containsEntry("SUPABASE_DB_PASSWORD", "senha_existente");
    }
}
