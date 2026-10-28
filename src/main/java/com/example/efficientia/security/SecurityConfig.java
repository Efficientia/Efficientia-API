package com.example.efficientia.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
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

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "false")
    SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/relatorios-viagem", "/api/v1/relatorios-viagem/**",
                                "/api/v1/locais-diario/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtRoleConverter()))
                        .authenticationEntryPoint(bearerTokenAuthenticationEntryPoint()))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
    SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/metrics/**",
                                "/actuator/prometheus",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/v1/status",
                                "/api/v1/auth/**",
                                "/api/v1/temp/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/v1/empresas/*/adms",
                                "/api/v1/empresas/*/adms/**",
                                "/api/v1/empresas/*/funcionarios",
                                "/api/v1/empresas/*/funcionarios/**"
                        ).hasAnyRole("ADMIN", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/empresas", "/api/v1/empresas/").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/empresas/*/primeiro-admin").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/empresas/codigo/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/empresas/cnpj/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/empresas/*/logo/conteudo", "/api/v1/empresas/*/logo").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/empresas/*/dados-complementares", "/api/v1/empresas/*/etapa-1", "/api/v1/empresas/codigo/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/empresas/*/logo", "/api/v1/empresas/*/logo-upload").permitAll()
                        .requestMatchers("/api/v1/empresas/**")
                        .hasAnyRole("MOTORISTA", "MANOBRISTA", "ANALISTA", "PECUARISTA", "CURRALEIRO", "FUNCIONARIO_FRIBOI", "ADMIN", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/exportacoes/**")
                        .hasAnyRole("MOTORISTA", "MANOBRISTA", "ANALISTA", "PECUARISTA", "CURRALEIRO", "FUNCIONARIO_FRIBOI", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/exportacoes/**")
                        .hasAnyRole("MOTORISTA", "MANOBRISTA", "ANALISTA", "PECUARISTA", "CURRALEIRO", "FUNCIONARIO_FRIBOI", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/documentos/**")
                        .hasAnyRole("MOTORISTA", "MANOBRISTA", "ANALISTA", "PECUARISTA", "CURRALEIRO", "FUNCIONARIO_FRIBOI", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/documentos/**")
                        .hasAnyRole("MOTORISTA", "MANOBRISTA", "ANALISTA", "PECUARISTA", "CURRALEIRO", "FUNCIONARIO_FRIBOI", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/documentos/**")
                        .hasAnyRole("ANALISTA", "FUNCIONARIO_FRIBOI", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/documentos/**")
                        .hasAnyRole("ANALISTA", "FUNCIONARIO_FRIBOI", "ADMIN")
                        .requestMatchers("/api/v1/relatorios-viagem/**")
                        .hasAnyRole("MOTORISTA", "MANOBRISTA", "ANALISTA", "PECUARISTA", "CURRALEIRO", "FUNCIONARIO_FRIBOI", "ADMIN", "ADMINISTRADOR")
                        .requestMatchers("/api/v1/usuarios/**", "/api/v1/enderecos/**", "/api/v1/fazendas/**", "/api/v1/veiculos/**", "/api/v1/caminhoes/**")
                        .hasAnyRole("MOTORISTA", "MANOBRISTA", "ANALISTA", "PECUARISTA", "CURRALEIRO", "FUNCIONARIO_FRIBOI", "ADMIN", "ADMINISTRADOR")
                        .requestMatchers("/api/v1/outbox/**")
                        .hasAnyRole("ANALISTA", "ADMIN", "ADMINISTRADOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtRoleConverter()))
                        .authenticationEntryPoint(bearerTokenAuthenticationEntryPoint()))
                .build();
    }

    @Bean
    public AuthenticationEntryPoint bearerTokenAuthenticationEntryPoint() {
        BearerTokenAuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();
        return (request, response, authException) -> {
            String uri = request.getRequestURI();
            String method = request.getMethod();
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            String failureReason = authException != null ? authException.getMessage() : "Desconhecida";

            if (authHeader == null || authHeader.isBlank()) {
                log.warn("Falha de autenticação [{} {}]: Cabeçalho Authorization ausente. Causa: {}",
                        method, uri, failureReason);
            } else if (!authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                log.warn("Falha de autenticação [{} {}]: Esquema de autenticação não é Bearer. Causa: {}",
                        method, uri, failureReason);
            } else {
                log.warn("Falha de autenticação JWT [{} {}]: Token inválido, expirado ou assinatura incompatível. Detalhe: {}",
                        method, uri, failureReason);
            }

            delegate.commence(request, response, authException);
        };
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder(SecurityProperties properties) {
        NimbusJwtDecoder decoder;
        if (!properties.jwkSetUri().isBlank()) {
            log.warn("ATENÇÃO: app.security.jwkSetUri configurado ({}) - a validação JWT será realizada via JWKS remoto. " +
                    "Tokens simétricos (HMAC-SHA256) gerados internamente pela API serão rejeitados!", properties.jwkSetUri());
            decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        } else {
            log.info("Inicializando decodificador JWT com chave simétrica HMAC-SHA256 (JWT_SECRET). " +
                    "Issuer esperado: '{}', Audience esperada: '{}'", properties.issuer(), properties.audience());
            byte[] secretBytes = properties.secret().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (secretBytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET deve possuir pelo menos 32 caracteres.");
            }
            javax.crypto.SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(secretBytes, "HmacSHA256");
            decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
        }

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        if (!properties.issuer().isBlank()) {
            validators.add(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        } else {
            validators.add(JwtValidators.createDefault());
        }
        if (!properties.audience().isBlank()) {
            validators.add(new AudienceValidator(properties.audience()));
        }
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Correlation-Id"));
        configuration.setExposedHeaders(List.of(
                "Location",
                "Retry-After",
                "Content-Disposition",
                "X-Correlation-Id"
        ));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
