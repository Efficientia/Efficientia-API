package com.example.efficientia.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("app.security")
public record SecurityProperties(
        boolean enabled,
        String issuer,
        String jwkSetUri,
        String audience,
        List<String> allowedOrigins
) {
    public SecurityProperties {
        issuer = issuer == null ? "" : issuer.strip();
        jwkSetUri = jwkSetUri == null ? "" : jwkSetUri.strip();
        audience = audience == null || audience.isBlank() ? "efficientia-api" : audience.strip();
        allowedOrigins = allowedOrigins == null || allowedOrigins.isEmpty()
                ? List.of("http://localhost:5173", "http://localhost:3000")
                : allowedOrigins.stream().map(String::strip).filter(value -> !value.isEmpty()).toList();
    }
}
