package com.example.efficientia.config;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
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
            if (value.isBlank()) {
                continue;
            }
            if ("SUPABASE_DB_URL".equals(key) || "DATABASE_URL".equals(key)) {
                value = normalizePostgresUrl(key, value, properties);
            }

            properties.put(key, value);
        }

        if (properties.containsKey("DATABASE_URL") && !properties.containsKey("SUPABASE_DB_URL")) {
            properties.put("SUPABASE_DB_URL", properties.get("DATABASE_URL"));
        }
        if (properties.containsKey("SUPABASE_DB_USER") && !properties.containsKey("SUPABASE_DB_USERNAME")) {
            properties.put("SUPABASE_DB_USERNAME", properties.get("SUPABASE_DB_USER"));
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

    private static String normalizePostgresUrl(String key, String value, Map<String, Object> properties) {
        if (value.startsWith("jdbc:")) {
            return value;
        }
        String uriValue = value;
        if (!uriValue.startsWith("postgresql://")) {
            return value;
        }

        URI uri;
        try {
            uri = URI.create(uriValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(key + " possui um formato inválido.", exception);
        }

        if (uri.getHost() == null) {
            throw new IllegalArgumentException(key + " deve informar um host válido.");
        }

        String userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            int colon = userInfo.indexOf(':');
            String user = colon >= 0 ? userInfo.substring(0, colon) : userInfo;
            String pass = colon >= 0 ? userInfo.substring(colon + 1) : "";
            try {
                user = URLDecoder.decode(user, StandardCharsets.UTF_8);
                pass = URLDecoder.decode(pass, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
            if (!user.isBlank() && !properties.containsKey("SUPABASE_DB_USERNAME")) {
                properties.put("SUPABASE_DB_USERNAME", user);
            }
            if (!pass.isBlank() && !properties.containsKey("SUPABASE_DB_PASSWORD")) {
                properties.put("SUPABASE_DB_PASSWORD", pass);
            }
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
