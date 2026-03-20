package com.school.management.config;

import com.school.management.infrastructure.context.AuditContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web MVC 配置类
 * 支持Vue Router的History模式
 *
 * @author system
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuditContextInterceptor auditContextInterceptor;

    /**
     * 注册拦截器
     * 添加审计上下文拦截器，自动填充请求上下文信息
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditContextInterceptor)
                .addPathPatterns("/api/**", "/v2/**", "/v6/**", "/v9/**")  // 拦截所有API请求
                .excludePathPatterns("/api/auth/login", "/api/auth/refresh");  // 排除登录和刷新token接口
    }

    /**
     * 配置静态资源处理
     * 支持Vue Router的History模式,所有非API请求都返回index.html
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源路径
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // 配置前端资源处理,支持History模式
        // 所有非API、非静态资源、非uploads的请求都返回index.html,由前端路由处理
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "classpath:/public/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // 如果请求的资源存在,直接返回
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // 如果不是静态文件请求（没有文件扩展名），视为 API 或前端路由
                        // 静态文件都有扩展名（.js, .css, .html, .png 等），API 路径没有
                        String lastSegment = resourcePath.contains("/")
                            ? resourcePath.substring(resourcePath.lastIndexOf('/') + 1)
                            : resourcePath;
                        if (!lastSegment.contains(".")) {
                            // 无扩展名 → 可能是 API 端点，返回 null 让 Controller 处理
                            return null;
                        }

                        // 其他情况(前端路由),返回index.html,由前端路由处理
                        Resource indexHtml = new ClassPathResource("/static/index.html");
                        if (indexHtml.exists() && indexHtml.isReadable()) {
                            return indexHtml;
                        }

                        return null;
                    }
                });
    }
}
