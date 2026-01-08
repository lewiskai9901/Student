package com.school.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson配置类
 * 解决Long类型精度丢失问题
 *
 * @author system
 * @since 1.0.0
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置Jackson ObjectMapper
     * 将Long类型序列化为String,避免JavaScript精度丢失
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // 创建SimpleModule,注册Long类型的序列化器
        SimpleModule simpleModule = new SimpleModule();
        // 将Long和long类型序列化为String
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        objectMapper.registerModule(simpleModule);

        return objectMapper;
    }
}
