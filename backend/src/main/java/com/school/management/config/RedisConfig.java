package com.school.management.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
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
 * Redis配置类
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 使用配置了JavaTimeModule的GenericJackson2JsonRedisSerializer来序列化和反序列化redis的value值
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(createRedisObjectMapper());

        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jsonRedisSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 缓存管理器配置 - 分层TTL策略
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // ===== 分层TTL策略配置 =====
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 静态数据 - 长TTL (24小时) - 部门、年级、专业等很少变动的数据
        cacheConfigurations.put("departments", buildCacheConfig(Duration.ofHours(24)));
        cacheConfigurations.put("department", buildCacheConfig(Duration.ofHours(24)));
        cacheConfigurations.put("grades", buildCacheConfig(Duration.ofHours(24)));
        cacheConfigurations.put("grade", buildCacheConfig(Duration.ofHours(24)));
        cacheConfigurations.put("majors", buildCacheConfig(Duration.ofHours(24)));
        cacheConfigurations.put("major", buildCacheConfig(Duration.ofHours(24)));
        cacheConfigurations.put("dicts", buildCacheConfig(Duration.ofDays(1)));  // 字典数据

        // 半静态数据 - 中TTL (1小时) - 班级、模板等偶尔变动的数据
        cacheConfigurations.put("classes", buildCacheConfig(Duration.ofHours(1)));
        cacheConfigurations.put("class", buildCacheConfig(Duration.ofHours(1)));
        cacheConfigurations.put("checkTemplates", buildCacheConfig(Duration.ofMinutes(30)));

        // 动态数据 - 短TTL (5-10分钟) - 学生、检查记录等频繁变动的数据
        cacheConfigurations.put("students", buildCacheConfig(Duration.ofMinutes(5)));
        cacheConfigurations.put("student", buildCacheConfig(Duration.ofMinutes(5)));
        cacheConfigurations.put("checkRecords", buildCacheConfig(Duration.ofMinutes(10)));

        // 默认配置 (5分钟)
        RedisCacheConfiguration defaultConfig = buildCacheConfig(Duration.ofMinutes(5));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * 构建缓存配置
     *
     * @param ttl 缓存过期时间
     * @return RedisCacheConfiguration
     */
    private RedisCacheConfiguration buildCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(createRedisObjectMapper())))
                .disableCachingNullValues();  // 不缓存null值,防止缓存穿透
    }

    /**
     * 创建配置了JavaTimeModule的ObjectMapper
     * 解决LocalDateTime等Java 8时间类型的序列化问题
     *
     * @return 配置好的ObjectMapper
     */
    private ObjectMapper createRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册JavaTimeModule以支持Java 8时间类型
        mapper.registerModule(new JavaTimeModule());
        // 激活默认类型,用于多态类型的序列化/反序列化
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }
}