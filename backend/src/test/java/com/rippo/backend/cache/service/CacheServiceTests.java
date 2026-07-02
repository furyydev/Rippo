package com.rippo.backend.cache.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rippo.backend.cache.config.CacheProperties;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class CacheServiceTests {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final CacheProperties properties = new CacheProperties();
    private final CacheService cacheService = new CacheService(
            redisTemplate,
            new ObjectMapper(),
            properties
    );

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void putsReadableJsonWithExplicitTtlAndGetsTypedValue() {
        CachedValue value = new CachedValue("rippo", 5);
        Duration ttl = Duration.ofMinutes(3);
        when(valueOperations.get("test:key"))
                .thenReturn("{\"name\":\"rippo\",\"count\":5}");

        assertThat(cacheService.put("test:key", value, ttl)).isTrue();
        assertThat(cacheService.get("test:key", CachedValue.class)).contains(value);

        verify(valueOperations).set(
                "test:key",
                "{\"name\":\"rippo\",\"count\":5}",
                ttl
        );
    }

    @Test
    void usesConfiguredDefaultTtl() {
        properties.setDefaultTtl(Duration.ofMinutes(20));

        assertThat(cacheService.put("test:key", Map.of("value", true))).isTrue();

        verify(valueOperations).set(
                "test:key",
                "{\"value\":true}",
                Duration.ofMinutes(20)
        );
    }

    @Test
    void reportsMissAndSupportsEvictionAndExistence() {
        when(valueOperations.get("missing")).thenReturn(null);
        when(redisTemplate.hasKey("existing")).thenReturn(true);

        assertThat(cacheService.get("missing")).isEmpty();
        assertThat(cacheService.exists("existing")).isTrue();
        assertThat(cacheService.evict("existing")).isTrue();

        verify(redisTemplate).delete("existing");
    }

    @Test
    void redisFailureReturnsSafeFallbacks() {
        RedisConnectionFailureException unavailable =
                new RedisConnectionFailureException("Redis is unavailable");
        when(valueOperations.get("key")).thenThrow(unavailable);
        doThrow(unavailable).when(valueOperations).set(
                "key",
                "{\"value\":\"data\"}",
                Duration.ofMinutes(10)
        );
        when(redisTemplate.hasKey("key")).thenThrow(unavailable);
        when(redisTemplate.delete("key")).thenThrow(unavailable);

        assertThat(cacheService.get("key")).isEmpty();
        assertThat(cacheService.put("key", Map.of("value", "data"))).isFalse();
        assertThat(cacheService.exists("key")).isFalse();
        assertThat(cacheService.evict("key")).isFalse();
    }

    @Test
    void verifiesRedisConnectionWithoutMakingItRequired() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(redisTemplate.getRequiredConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");

        assertThat(cacheService.isAvailable()).isTrue();
        verify(connection).close();
    }

    @Test
    void unavailableConnectionCheckFailsOpen() {
        when(redisTemplate.getRequiredConnectionFactory()).thenThrow(
                new RedisConnectionFailureException("Redis is unavailable")
        );

        assertThat(cacheService.isAvailable()).isFalse();
    }

    private record CachedValue(String name, int count) {
    }
}
