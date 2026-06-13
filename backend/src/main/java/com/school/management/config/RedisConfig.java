package com.school.management.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
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
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    /**
     * 缓存错误处理器 — 让缓存成为"尽力而为"而非硬依赖。
     *
     * <p>背景: Redis 偶发命令超时 (冷连接首命令 / 网络抖动) 时, 默认行为是把异常
     * 抛到业务层 → 整个请求 500。但缓存只是加速层, Redis 不可用应优雅降级到数据源
     * (DB), 而不是让接口挂掉。
     *
     * <p>本处理器吞掉 get/put/evict/clear 的异常并记 WARN:
     * <ul>
     *   <li>get 失败 → Spring 视为缓存未命中 → 执行原方法 (查 DB), 请求正常返回;</li>
     *   <li>put/evict/clear 失败 → 数据已由原方法写入 DB, 缓存写失败仅影响下次命中, 无害。</li>
     * </ul>
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("[cache] GET 失败, 降级查数据源 cache={} key={}: {}",
                        cache.getName(), key, e.getMessage());
            }
            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("[cache] PUT 失败 (不影响本次结果) cache={} key={}: {}",
                        cache.getName(), key, e.getMessage());
            }
            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("[cache] EVICT 失败 cache={} key={}: {}",
                        cache.getName(), key, e.getMessage());
            }
            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("[cache] CLEAR 失败 cache={}: {}", cache.getName(), e.getMessage());
            }
        };
    }

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

        // P0-B 检查平台评分配置 (评分方案 / 等级方案 / 维度) - 30 分钟 TTL
        // 写入路径都已加 @CacheEvict, 实际生效快, 30 分钟更多是兜底防 Redis 异常情况
        cacheConfigurations.put("ratingConfig", buildCacheConfig(Duration.ofMinutes(30)));

        // 动态数据 - 短TTL (5-10分钟) - 学生、检查记录等频繁变动的数据
        cacheConfigurations.put("user_student", buildCacheConfig(Duration.ofMinutes(5)));
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