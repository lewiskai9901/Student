package com.school.management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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
public class WebMvcConfig implements WebMvcConfigurer {

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

                        // 如果是API请求,不处理,返回null让Spring Security和Controller处理
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        // 如果是uploads请求,不处理,返回null让Controller处理
                        if (resourcePath.startsWith("uploads/")) {
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
