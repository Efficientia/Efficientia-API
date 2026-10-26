package com.example.efficientia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, org.springframework.boot.env.EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUrlEnvironmentPostProcessor.class);
    private static final String PROPERTY_SOURCE_NAME = "render-database-url-normalization";

    private static final Pattern POSTGRES_URL_PATTERN = Pattern.compile(
            "^(?:postgresql|postgres)://(?:([^:@/]+)(?::([^@/]*))?@)?(\\[[0-9a-fA-F:]+\\]|[^:/]+)(?::(\\d+))?(/[^?#]*)?(?:\\?(.*))?$"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String rawUrl = environment.getProperty("DATABASE_URL");
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = environment.getProperty("SUPABASE_DB_URL");
        }

        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }

        String trimmed = rawUrl.trim();
        if (!trimmed.startsWith("postgresql://") && !trimmed.startsWith("postgres://")) {
            return;
        }

        Map<String, Object> overrides = new HashMap<>();
        String normalizedUrl = normalizeToJdbc(trimmed, overrides);
        if (normalizedUrl != null) {
            overrides.put("spring.datasource.url", normalizedUrl);
            log.info("DatabaseUrlEnvironmentPostProcessor: URL de banco de dados normalizada para JDBC (Render/Cloud).");
        }

        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
        }
    }

    static String normalizeToJdbc(String rawUrl, Map<String, Object> credentialsTarget) {
        Matcher matcher = POSTGRES_URL_PATTERN.matcher(rawUrl.trim());
        if (!matcher.matches()) {
            return rawUrl.startsWith("jdbc:") ? rawUrl : "jdbc:" + rawUrl;
        }

        String username = matcher.group(1);
        String password = matcher.group(2);
        String host = matcher.group(3);
        String port = matcher.group(4);
        String path = matcher.group(5);
        String query = matcher.group(6);

        if (credentialsTarget != null) {
            if (username != null && !username.isBlank()) {
                credentialsTarget.put("spring.datasource.username", decode(username));
            }
            if (password != null && !password.isBlank()) {
                credentialsTarget.put("spring.datasource.password", decode(password));
            }
        }

        String portStr = (port != null && !port.isBlank()) ? ":" + port : "";
        String pathStr = (path != null) ? path : "";
        String queryStr = (query != null && !query.isBlank()) ? "?" + query : "";

        return "jdbc:postgresql://" + host + portStr + pathStr + queryStr;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return value;
        }
    }
}
