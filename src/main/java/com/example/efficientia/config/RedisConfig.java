package com.example.efficientia.config;

import java.net.URI;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${REDIS_URL:${spring.data.redis.url:}}")
    private String redisUrl;

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        if (redisUrl == null || redisUrl.isBlank()) {
            log.info("REDIS_URL not configured. Initializing fallback standalone LettuceConnectionFactory without eager validation.");
            RedisStandaloneConfiguration defaultConfig = new RedisStandaloneConfiguration("localhost", 6379);
            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(2))
                    .build();
            LettuceConnectionFactory factory = new LettuceConnectionFactory(defaultConfig, clientConfig);
            factory.setValidateConnection(false);
            return factory;
        }

        try {
            URI uri = URI.create(redisUrl.trim());
            String scheme = uri.getScheme();
            boolean isSsl = "rediss".equalsIgnoreCase(scheme);

            String host = uri.getHost() != null ? uri.getHost() : "localhost";
            int port = uri.getPort() != -1 ? uri.getPort() : (isSsl ? 6380 : 6379);

            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);

            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isEmpty()) {
                String[] parts = userInfo.split(":", 2);
                if (parts.length == 2) {
                    if (!parts[0].isEmpty()) {
                        config.setUsername(parts[0]);
                    }
                    config.setPassword(RedisPassword.of(parts[1]));
                } else {
                    config.setPassword(RedisPassword.of(parts[0]));
                }
            }

            LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
                    LettuceClientConfiguration.builder()
                            .commandTimeout(Duration.ofSeconds(5));

            if (isSsl) {
                clientConfigBuilder.useSsl();
            }

            LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfigBuilder.build());
            factory.setValidateConnection(false);
            log.info("Configured LettuceConnectionFactory with host: {}, port: {}, ssl: {}", host, port, isSsl);
            return factory;
        } catch (Exception ex) {
            log.warn("Failed to parse REDIS_URL [{}]. Falling back to default configuration. Error: {}", redisUrl, ex.getMessage());
            RedisStandaloneConfiguration defaultConfig = new RedisStandaloneConfiguration("localhost", 6379);
            LettuceConnectionFactory factory = new LettuceConnectionFactory(defaultConfig);
            factory.setValidateConnection(false);
            return factory;
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
