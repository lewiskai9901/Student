package com.school.management.config;

import com.school.management.security.CustomUserDetailsService;
import com.school.management.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security配置
 *
 * @author system
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 认证提供者
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF保护
                .csrf(AbstractHttpConfigurer::disable)

                // 配置CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 配置会话管理
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置请求授权
                .authorizeHttpRequests(authz -> authz
                        // 公开接口 - 使用AntPathRequestMatcher避免多Servlet冲突
                        .requestMatchers(
                                new AntPathRequestMatcher("/auth/**"),
                                new AntPathRequestMatcher("/miniapp/auth/**"),           // 小程序登录接口
                                new AntPathRequestMatcher("/wechat/**"),                 // 微信分享接口（公开访问）
                                new AntPathRequestMatcher("/public/**"),                 // 公开接口（通报查看等）
                                // 已移除: /dev/** 现通过@Profile("dev")控制
                                // 已移除: /test/** 测试接口不应公开
                                new AntPathRequestMatcher("/files/download/**"),
                                new AntPathRequestMatcher("/uploads/**"),
                                new AntPathRequestMatcher("/swagger-ui/**"),
                                new AntPathRequestMatcher("/v3/api-docs/**"),
                                new AntPathRequestMatcher("/doc.html"),
                                new AntPathRequestMatcher("/webjars/**"),
                                new AntPathRequestMatcher("/favicon.ico"),
                                new AntPathRequestMatcher("/actuator/health"),
                                new AntPathRequestMatcher("/system/configs/public/**"),  // 公开的系统配置接口
                                // 已移除: /druid/** Druid监控需要认证，配置在DruidConfig中
                                new AntPathRequestMatcher("/dmn-api/**")                 // Flowable DMN API
                        ).permitAll()

                        // 所有其他请求需要认证
                        .anyRequest().authenticated()
                )

                // 设置认证提供者
                .authenticationProvider(authenticationProvider())

                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS配置 - 限制允许的来源，提升安全性
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源 - 根据环境动态配置
        // 生产环境应通过环境变量CORS_ALLOWED_ORIGINS配置实际域名
        String corsOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
        List<String> allowedOrigins;

        if (corsOriginsEnv != null && !corsOriginsEnv.isEmpty()) {
            // 生产环境: 从环境变量读取（逗号分隔）
            allowedOrigins = Arrays.asList(corsOriginsEnv.split(","));
        } else {
            // 开发环境: 仅允许必要的开发端口
            // ⚠️ 安全加固: 精简为实际使用的端口
            allowedOrigins = Arrays.asList(
                    "http://localhost:3000",      // 前端开发服务器
                    "http://localhost:3001",      // 前端备用端口
                    "http://localhost:3002",      // 前端备用端口2
                    "http://localhost:5173",      // Vite默认端口
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:3001",
                    "http://127.0.0.1:3002",
                    "http://127.0.0.1:5173"
            );
        }

        configuration.setAllowedOriginPatterns(allowedOrigins);

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 允许的请求头 - 限制为必要的头部
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Cache-Control"
        ));

        // 允许携带凭证（Cookie、Authorization header）
        configuration.setAllowCredentials(true);

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));

        // 预检请求的缓存时间（1小时）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}