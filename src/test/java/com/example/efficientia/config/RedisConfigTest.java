package com.example.efficientia.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    @Test
    @DisplayName("Should return default LettuceConnectionFactory when redisUrl is null")
    void redisConnectionFactory_NullRedisUrl_ReturnsDefaultFactory() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "redisUrl", null);

        RedisConnectionFactory factory = config.redisConnectionFactory();

        assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        assertThat(lettuceFactory.getValidateConnection()).isFalse();
        assertThat(lettuceFactory.getHostName()).isEqualTo("localhost");
        assertThat(lettuceFactory.getPort()).isEqualTo(6379);
    }

    @Test
    @DisplayName("Should return default LettuceConnectionFactory when redisUrl is blank")
    void redisConnectionFactory_BlankRedisUrl_ReturnsDefaultFactory() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "redisUrl", "   ");

        RedisConnectionFactory factory = config.redisConnectionFactory();

        assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        assertThat(lettuceFactory.getValidateConnection()).isFalse();
        assertThat(lettuceFactory.getHostName()).isEqualTo("localhost");
        assertThat(lettuceFactory.getPort()).isEqualTo(6379);
    }

    @Test
    @DisplayName("Should configure factory with standard redis:// URL without authentication")
    void redisConnectionFactory_StandardRedisUrl_ConfiguresProperly() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "redisUrl", "redis://localhost:6379");

        RedisConnectionFactory factory = config.redisConnectionFactory();

        assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        assertThat(lettuceFactory.getValidateConnection()).isFalse();
        assertThat(lettuceFactory.getHostName()).isEqualTo("localhost");
        assertThat(lettuceFactory.getPort()).isEqualTo(6379);
        assertThat(lettuceFactory.getClientConfiguration().isUseSsl()).isFalse();
    }

    @Test
    @DisplayName("Should configure factory with rediss:// Upstash TLS URL including username and password")
    void redisConnectionFactory_UpstashTlsUrl_ConfiguresSslAndCredentials() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "redisUrl", "rediss://default:secret@enabling-eagle-216485.upstash.io:6379");

        RedisConnectionFactory factory = config.redisConnectionFactory();

        assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        assertThat(lettuceFactory.getValidateConnection()).isFalse();
        assertThat(lettuceFactory.getHostName()).isEqualTo("enabling-eagle-216485.upstash.io");
        assertThat(lettuceFactory.getPort()).isEqualTo(6379);
        RedisStandaloneConfiguration standaloneConfig = lettuceFactory.getStandaloneConfiguration();
        assertThat(standaloneConfig).isNotNull();
        assertThat(standaloneConfig.getUsername()).isEqualTo("default");
        assertThat(standaloneConfig.getPassword()).isEqualTo(org.springframework.data.redis.connection.RedisPassword.of("secret"));
        assertThat(lettuceFactory.getClientConfiguration().isUseSsl()).isTrue();
    }

    @Test
    @DisplayName("Should configure factory with rediss:// password only (no username)")
    void redisConnectionFactory_PasswordOnlyUrl_ConfiguresPasswordAndSsl() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "redisUrl", "rediss://:onlypassword@remotehost:6380");

        RedisConnectionFactory factory = config.redisConnectionFactory();

        assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        RedisStandaloneConfiguration standaloneConfig = lettuceFactory.getStandaloneConfiguration();
        assertThat(standaloneConfig).isNotNull();
        assertThat(standaloneConfig.getUsername()).isNull();
        assertThat(standaloneConfig.getPassword()).isEqualTo(org.springframework.data.redis.connection.RedisPassword.of("onlypassword"));
        assertThat(lettuceFactory.getHostName()).isEqualTo("remotehost");
        assertThat(lettuceFactory.getPort()).isEqualTo(6380);
        assertThat(lettuceFactory.getClientConfiguration().isUseSsl()).isTrue();
    }

    @Test
    @DisplayName("Should configure factory with user info containing single token (no colon)")
    void redisConnectionFactory_UserInfoSingleToken_ConfiguresPassword() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "redisUrl", "redis://tokenpassword@singlehost:6379");

        RedisConnectionFactory factory = config.redisConnectionFactory();

        assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        RedisStandaloneConfiguration standaloneConfig = lettuceFactory.getStandaloneConfiguration();
        assertThat(standaloneConfig).isNotNull();
        assertThat(standaloneConfig.getPassword()).isEqualTo(org.springframework.data.redis.connection.RedisPassword.of("tokenpassword"));
        assertThat(lettuceFactory.getHostName()).isEqualTo("singlehost");
    }

    @Test
    @DisplayName("Should use default port 6380 for rediss and default host localhost when omitted")
    void redisConnectionFactory_RedissDefaultPortAndHost() {
        RedisConfig config = new RedisConfig();
        // rediss:// without explicit port or host
        ReflectionTestUtils.setField(config, "redisUrl", "rediss:///");

        RedisConnectionFactory factory = config.redisConnectionFactory();

        assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        assertThat(lettuceFactory.getHostName()).isEqualTo("localhost");
        assertThat(lettuceFactory.getPort()).isEqualTo(6380);
        assertThat(lettuceFactory.getClientConfiguration().isUseSsl()).isTrue();
    }

    @Test
    @DisplayName("Should use default port 6379 for redis when omitted")
    void redisConnectionFactory_RedisDefaultPortWhenOmitted() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "redisUrl", "redis://myhost");

        RedisConnectionFactory factory = config.redisConnectionFactory();

        assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        assertThat(lettuceFactory.getHostName()).isEqualTo("myhost");
        assertThat(lettuceFactory.getPort()).isEqualTo(6379);
        assertThat(lettuceFactory.getClientConfiguration().isUseSsl()).isFalse();
    }

    @Test
    @DisplayName("Should catch exception and fallback to default localhost factory on invalid URL")
    void redisConnectionFactory_MalformedUrl_FallsBackToDefault() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "redisUrl", "http://invalid uri with spaces:not_a_port");

        RedisConnectionFactory factory = config.redisConnectionFactory();

        assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        assertThat(lettuceFactory.getValidateConnection()).isFalse();
        assertThat(lettuceFactory.getHostName()).isEqualTo("localhost");
        assertThat(lettuceFactory.getPort()).isEqualTo(6379);
    }

    @Test
    @DisplayName("Should instantiate redisTemplate with correct serializers and connection factory")
    void redisTemplate_InstantiatesWithSerializers() {
        RedisConfig config = new RedisConfig();
        RedisConnectionFactory mockConnectionFactory = mock(RedisConnectionFactory.class);

        RedisTemplate<String, Object> template = config.redisTemplate(mockConnectionFactory);

        assertThat(template).isNotNull();
        assertThat(template.getConnectionFactory()).isSameAs(mockConnectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isNotNull();
        assertThat(template.getHashValueSerializer()).isNotNull();
    }

    @Test
    @DisplayName("Should instantiate stringRedisTemplate with connection factory")
    void stringRedisTemplate_InstantiatesWithStringTemplate() {
        RedisConfig config = new RedisConfig();
        RedisConnectionFactory mockConnectionFactory = mock(RedisConnectionFactory.class);

        StringRedisTemplate template = config.stringRedisTemplate(mockConnectionFactory);

        assertThat(template).isNotNull();
        assertThat(template.getConnectionFactory()).isSameAs(mockConnectionFactory);
    }
}
