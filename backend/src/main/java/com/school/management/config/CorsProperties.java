package com.school.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS配置属性
 *
 * @author system
 * @since 2.0.1
 */
@Data
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /**
     * 允许的源（支持通配符）
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * 允许的HTTP方法
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");

    /**
     * 允许的请求头
     */
    private List<String> allowedHeaders = List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Cache-Control");

    /**
     * 是否允许携带凭证
     */
    private Boolean allowCredentials = true;

    /**
     * 预检请求缓存时间（秒）
     */
    private Long maxAge = 3600L;
}
