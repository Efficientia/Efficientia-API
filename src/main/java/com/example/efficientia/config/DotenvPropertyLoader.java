package com.example.efficientia.config;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class DotenvPropertyLoader {

    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private DotenvPropertyLoader() {
    }

    public static Map<String, Object> load(Path dotenvPath) {
        if (Files.notExists(dotenvPath)) {
            return Map.of();
        }

        try {
            return parse(Files.readAllLines(dotenvPath, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível ler o arquivo .env.", exception);
        }
    }

    static Map<String, Object> parse(List<String> lines) {
        Map<String, Object> properties = new LinkedHashMap<>();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).strip();
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).stripLeading();
            }

            int separator = line.indexOf('=');
            if (separator <= 0) {
                throw invalidLine(index);
            }

            String key = line.substring(0, separator).strip();
            if (!VALID_KEY.matcher(key).matches()) {
                throw invalidLine(index);
            }

            String value = stripMatchingQuotes(line.substring(separator + 1).strip());
            if ("SUPABASE_DB_URL".equals(key)) {
                value = normalizePostgresUrl(value);
            }

            properties.put(key, value);
        }

        return properties;
    }

    private static String stripMatchingQuotes(String value) {
        if (value.length() < 2) {
            return value;
        }

        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String normalizePostgresUrl(String value) {
        String uriValue = value.startsWith("jdbc:") ? value.substring("jdbc:".length()) : value;
        if (!uriValue.startsWith("postgresql://")) {
            return value;
        }

        URI uri;
        try {
            uri = URI.create(uriValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("SUPABASE_DB_URL possui um formato inválido.", exception);
        }

        if (uri.getHost() == null) {
            throw new IllegalArgumentException("SUPABASE_DB_URL deve informar um host válido.");
        }

        String host = uri.getHost().contains(":") ? "[" + uri.getHost() + "]" : uri.getHost();
        String port = uri.getPort() >= 0 ? ":" + uri.getPort() : "";
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        return "jdbc:postgresql://" + host + port + path + query;
    }

    private static IllegalArgumentException invalidLine(int zeroBasedIndex) {
        return new IllegalArgumentException(
                "Formato inválido no arquivo .env, linha " + (zeroBasedIndex + 1) + ". Use CHAVE=VALOR."
        );
    }
}
