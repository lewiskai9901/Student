package com.school.management.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson ObjectMapper 配置类
 * 提供全局共享的 ObjectMapper Bean，避免重复创建实例
 */
@Configuration
public class JacksonConfig {

    /**
     * 创建全局共享的 ObjectMapper Bean
     * 配置了常用的序列化/反序列化选项
     *
     * @return 配置好的 ObjectMapper 实例
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());

        // 注册 Long 类型序列化为 String，解决 JavaScript 精度丢失问题
        SimpleModule longModule = new SimpleModule();
        longModule.addSerializer(Long.class, ToStringSerializer.instance);
        longModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        mapper.registerModule(longModule);

        // 序列化配置
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 反序列化配置
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }
}
