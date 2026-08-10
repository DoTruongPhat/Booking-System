package com.booking.infrastructure.cache.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableCaching
@Slf4j
public class RedisCacheConfig implements CachingConfigurer {

    @Value("${app.cache.search-result-ttl-minutes:1}")
    private int searchTtlMinutes;

    @Value("${app.cache.room-availability-ttl-minutes:5}")
    private int availabilityTtlMinutes;

    /**
     * Cache names used across the application
     */
    public static final String CACHE_SEARCH_RESULTS = "search-results";
    public static final String CACHE_ROOM_AVAILABILITY = "room-availability";
    public static final String CACHE_ROOM_DETAIL = "room-detail";
    public static final String CACHE_HOTEL_DETAIL = "hotel-detail";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default config (10 min TTL)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer()))
                .disableCachingNullValues();

        // Per-cache TTL configurations
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                CACHE_SEARCH_RESULTS, defaultConfig.entryTtl(Duration.ofMinutes(searchTtlMinutes)),
                CACHE_ROOM_AVAILABILITY, defaultConfig.entryTtl(Duration.ofMinutes(availabilityTtlMinutes)),
                CACHE_ROOM_DETAIL, defaultConfig.entryTtl(Duration.ofMinutes(10)),
                CACHE_HOTEL_DETAIL, defaultConfig.entryTtl(Duration.ofMinutes(10))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer());
        template.setHashValueSerializer(jsonSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("[Cache] GET failed for cache={} key={}; falling back to source: {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("[Cache] PUT failed for cache={} key={}: {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("[Cache] EVICT failed for cache={} key={}: {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("[Cache] CLEAR failed for cache={}: {}", cache.getName(), exception.getMessage());
            }
        };
    }

    private GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
