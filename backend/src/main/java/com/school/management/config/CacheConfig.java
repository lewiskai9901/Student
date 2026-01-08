package com.school.management.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Cache configuration for performance optimization.
 *
 * <p>Provides multi-level caching with different TTLs for different data types:
 * <ul>
 *   <li>Short-lived: Session data, frequently changing data (5 minutes)</li>
 *   <li>Medium-lived: User data, permissions (30 minutes)</li>
 *   <li>Long-lived: Configuration, static data (2 hours)</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache names and their purposes.
     */
    public static final String CACHE_USER = "user";
    public static final String CACHE_PERMISSION = "permission";
    public static final String CACHE_ROLE = "role";
    public static final String CACHE_CONFIG = "config";
    public static final String CACHE_TEMPLATE = "template";
    public static final String CACHE_DEPARTMENT = "department";
    public static final String CACHE_CLASS = "class";
    public static final String CACHE_RATING_CONFIG = "ratingConfig";
    public static final String CACHE_STATISTICS = "statistics";

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure serializers
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper())))
            .disableCachingNullValues();

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Short TTL (5 minutes) - frequently changing data
        RedisCacheConfiguration shortTtl = defaultConfig.entryTtl(Duration.ofMinutes(5));
        cacheConfigs.put(CACHE_STATISTICS, shortTtl);

        // Medium TTL (30 minutes) - user and permission data
        RedisCacheConfiguration mediumTtl = defaultConfig.entryTtl(Duration.ofMinutes(30));
        cacheConfigs.put(CACHE_USER, mediumTtl);
        cacheConfigs.put(CACHE_PERMISSION, mediumTtl);
        cacheConfigs.put(CACHE_ROLE, mediumTtl);

        // Long TTL (2 hours) - configuration and template data
        RedisCacheConfiguration longTtl = defaultConfig.entryTtl(Duration.ofHours(2));
        cacheConfigs.put(CACHE_CONFIG, longTtl);
        cacheConfigs.put(CACHE_TEMPLATE, longTtl);
        cacheConfigs.put(CACHE_DEPARTMENT, longTtl);
        cacheConfigs.put(CACHE_CLASS, longTtl);
        cacheConfigs.put(CACHE_RATING_CONFIG, longTtl);

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .transactionAware()
            .build();
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }
}
