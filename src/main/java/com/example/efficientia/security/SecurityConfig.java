package com.example.efficientia.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "false", matchIfMissing = true)
    SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
    SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/documentos/**")
                        .hasAnyRole("MOTORISTA", "FUNCIONARIO_FRIBOI", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/documentos/**")
                        .hasAnyRole("MOTORISTA", "FUNCIONARIO_FRIBOI", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/documentos/**")
                        .hasAnyRole("FUNCIONARIO_FRIBOI", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/documentos/**")
                        .hasAnyRole("FUNCIONARIO_FRIBOI", "ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(new JwtRoleConverter())))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder(SecurityProperties properties) {
        if (properties.jwkSetUri().isBlank()) {
            throw new IllegalStateException(
                    "JWT_JWK_SET_URI é obrigatório quando SECURITY_ENABLED=true."
            );
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(properties.issuer().isBlank()
                ? JwtValidators.createDefault()
                : JwtValidators.createDefaultWithIssuer(properties.issuer()));
        validators.add(new AudienceValidator(properties.audience()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Correlation-Id"));
        configuration.setExposedHeaders(List.of("Location", "Content-Disposition", "X-Correlation-Id"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
