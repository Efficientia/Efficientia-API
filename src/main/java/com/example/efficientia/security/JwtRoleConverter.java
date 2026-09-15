package com.example.efficientia.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Set<String> SUPPORTED_ROLES = Set.of(
            "MOTORISTA", "MANOBRISTA", "ANALISTA", "PECUARISTA", "CURRALEIRO", "FUNCIONARIO_FRIBOI", "ADMIN"
    );

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        adicionar(roles, jwt.getClaim("roles"));
        adicionar(roles, jwt.getClaim("role"));

        Object appMetadata = jwt.getClaim("app_metadata");
        if (appMetadata instanceof Map<?, ?> metadata) {
            adicionar(roles, metadata.get("roles"));
            adicionar(roles, metadata.get("role"));
        }

        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .map(GrantedAuthority.class::cast)
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private void adicionar(Set<String> roles, Object value) {
        Collection<?> values = value instanceof Collection<?> collection
                ? collection
                : value == null ? List.of() : List.of(value);
        for (Object item : values) {
            String role = String.valueOf(item).strip().toUpperCase(Locale.ROOT);
            if (role.startsWith("ROLE_")) {
                role = role.substring(5);
            }
            if (SUPPORTED_ROLES.contains(role)) {
                roles.add(role);
            }
        }
    }
}
