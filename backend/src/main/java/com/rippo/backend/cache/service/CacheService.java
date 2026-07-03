package com.rippo.backend.cache.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rippo.backend.cache.config.CacheProperties;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;

    public CacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            CacheProperties cacheProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
    }

    public Optional<Object> get(String key) {
        return get(key, Object.class);
    }

    public <T> Optional<T> get(String key, Class<T> valueType) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                LOGGER.info("Cache MISS: key={}", key);
                return Optional.empty();
            }

            T value = objectMapper.readValue(json, valueType);
            LOGGER.info("Cache HIT: key={}", key);
            return Optional.ofNullable(value);
        } catch (Exception exception) {
            logFailure("GET", key, exception);
            return Optional.empty();
        }
    }

    public boolean put(String key, Object value) {
        return put(key, value, cacheProperties.getDefaultTtl());
    }

    public boolean put(String key, Object value, Duration ttl) {
        try {
            Duration effectiveTtl = validTtlOrDefault(ttl);
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, effectiveTtl);
            LOGGER.info(
                    "Cache PUT: key={} ttlSeconds={}",
                    key,
                    effectiveTtl.toSeconds()
            );
            return true;
        } catch (Exception exception) {
            logFailure("PUT", key, exception);
            return false;
        }
    }

    public boolean evict(String key) {
        try {
            redisTemplate.delete(key);
            LOGGER.info("Cache EVICT: key={}", key);
            return true;
        } catch (Exception exception) {
            logFailure("EVICT", key, exception);
            return false;
        }
    }

    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception exception) {
            logFailure("EXISTS", key, exception);
            return false;
        }
    }

    public boolean isAvailable() {
        RedisConnection connection = null;
        try {
            connection = redisTemplate.getRequiredConnectionFactory().getConnection();
            String response = connection.ping();
            boolean available = response != null && response.equalsIgnoreCase("PONG");
            LOGGER.info("Redis connection check: available={}", available);
            return available;
        } catch (Exception exception) {
            LOGGER.warn("Redis connection check failed; cache will be bypassed");
            LOGGER.debug("Redis connection failure details", exception);
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception exception) {
                    LOGGER.debug("Could not close Redis connection", exception);
                }
            }
        }
    }

    private Duration validTtlOrDefault(Duration ttl) {
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            return ttl;
        }
        Duration defaultTtl = cacheProperties.getDefaultTtl();
        if (defaultTtl == null || defaultTtl.isZero() || defaultTtl.isNegative()) {
            return Duration.ofMinutes(10);
        }
        return defaultTtl;
    }

    private void logFailure(String operation, String key, Exception exception) {
        LOGGER.warn(
                "Cache {} failed: key={}; continuing without cache",
                operation,
                key
        );
        if (!(exception instanceof JsonProcessingException)) {
            LOGGER.debug("Redis cache failure details", exception);
        } else {
            LOGGER.debug("Cache JSON serialization failure details", exception);
        }
    }
}
