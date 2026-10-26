package com.example.efficientia.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    @DisplayName("normalizeToJdbc deve converter postgres:// e extrair credenciais")
    void deveConverterPostgresSchemeEExtrairCredenciais() {
        Map<String, Object> creds = new HashMap<>();
        String result = DatabaseUrlEnvironmentPostProcessor.normalizeToJdbc(
                "postgres://admin:segredo123@db.render.com:5432/meubanco", creds);

        assertThat(result).isEqualTo("jdbc:postgresql://db.render.com:5432/meubanco");
        assertThat(creds).containsEntry("spring.datasource.username", "admin");
        assertThat(creds).containsEntry("spring.datasource.password", "segredo123");
    }

    @Test
    @DisplayName("normalizeToJdbc deve decodificar caracteres especiais no userInfo")
    void deveDecodificarCaracteresEspeciais() {
        Map<String, Object> creds = new HashMap<>();
        String result = DatabaseUrlEnvironmentPostProcessor.normalizeToJdbc(
                "postgresql://usuario%20pool:senha%40forte@host.com:5432/db", creds);

        assertThat(result).isEqualTo("jdbc:postgresql://host.com:5432/db");
        assertThat(creds).containsEntry("spring.datasource.username", "usuario pool");
        assertThat(creds).containsEntry("spring.datasource.password", "senha@forte");
    }

    @Test
    @DisplayName("normalizeToJdbc sem credenciais preserva host e porta")
    void deveNormalizarSemCredenciais() {
        Map<String, Object> creds = new HashMap<>();
        String result = DatabaseUrlEnvironmentPostProcessor.normalizeToJdbc(
                "postgresql://localhost:5432/efficientia", creds);

        assertThat(result).isEqualTo("jdbc:postgresql://localhost:5432/efficientia");
        assertThat(creds).doesNotContainKey("spring.datasource.username");
        assertThat(creds).doesNotContainKey("spring.datasource.password");
    }

    @Test
    @DisplayName("normalizeToJdbc com IPv6 coloca colchetes no host")
    void deveTratarIpv6() {
        Map<String, Object> creds = new HashMap<>();
        String result = DatabaseUrlEnvironmentPostProcessor.normalizeToJdbc(
                "postgresql://user:pass@[2001:db8::1]:5432/db", creds);

        assertThat(result).isEqualTo("jdbc:postgresql://[2001:db8::1]:5432/db");
        assertThat(creds).containsEntry("spring.datasource.username", "user");
    }

    @Test
    @DisplayName("postProcessEnvironment deve injetar PropertySource prioritário quando DATABASE_URL estiver presente")
    void deveInjetarPropertySourcePrioritario() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgresql://render_user:render_pass@dpg-xxx:5432/render_db");

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://dpg-xxx:5432/render_db");
        assertThat(env.getProperty("spring.datasource.username"))
                .isEqualTo("render_user");
        assertThat(env.getProperty("spring.datasource.password"))
                .isEqualTo("render_pass");
    }

    @Test
    @DisplayName("postProcessEnvironment deve ignorar quando DATABASE_URL e SUPABASE_DB_URL forem nulos")
    void deveIgnorarQuandoUrlsAusentes() {
        MockEnvironment env = new MockEnvironment();
        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url")).isNull();
    }

    @Test
    @DisplayName("postProcessEnvironment deve consumir SUPABASE_DB_URL como fallback quando DATABASE_URL ausente")
    void deveConsumirSupabaseDbUrlComoFallback() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("SUPABASE_DB_URL", "postgresql://supa_user:supa_pass@supa_host:6543/postgres");

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://supa_host:6543/postgres");
        assertThat(env.getProperty("spring.datasource.username"))
                .isEqualTo("supa_user");
        assertThat(env.getProperty("spring.datasource.password"))
                .isEqualTo("supa_pass");
    }
}
