package com.school.management.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 配置类
 * <p>
 * 提供API文档自动生成功能
 * 访问地址：http://localhost:8080/swagger-ui/index.html
 * API文档：http://localhost:8080/v3/api-docs
 *
 * @author School Management System
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:学生管理系统}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * 配置OpenAPI基本信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // API基本信息
                .info(new Info()
                        .title(applicationName + " API 文档")
                        .version("2.0.0")
                        .description("""
                                学生管理系统后端API接口文档

                                ## ⚠️ API 版本迁移提示

                                **重要通知**: 系统正在进行 V1 → V2 API 迁移

                                ### V2 API (推荐)
                                新开发请使用 V2 API，路径前缀: `/api/v2/`
                                - `/api/v2/organization/*` - 组织管理 (org-units, classes, grades)
                                - `/api/v2/access/*` - 权限管理 (roles, permissions)
                                - `/api/v2/inspection/*` - 量化检查 (templates, records, appeals)
                                - `/api/v2/student/*` - 学生管理
                                - `/api/v2/asset/*` - 资产管理 (buildings, dormitories)
                                - `/api/v2/task/*` - 任务管理

                                ### V1 API (已弃用)
                                V1 API 将在90天后下线，请尽快迁移:
                                - `/api/classes/*` → `/api/v2/organization/classes/*`
                                - `/api/students/*` → `/api/v2/students/*`
                                - `/api/roles/*` → `/api/v2/roles/*`
                                - `/api/permissions/*` → `/api/v2/permissions/*`
                                - `/api/quantification/*` → `/api/v2/inspection/*`

                                ## 功能模块
                                - 用户管理：用户登录、注册、权限管理
                                - 学生管理：学生信息CRUD、档案管理
                                - 组织管理：部门、年级、班级、专业
                                - 资产管理：楼栋、宿舍分配
                                - 量化检查：检查模板、检查记录、申诉管理、评级配置
                                - 任务管理：任务分配、审批流程
                                - 系统管理：角色管理、权限配置

                                ## 认证说明
                                大部分接口需要JWT Token认证，请先调用登录接口获取Token
                                """)
                        .contact(new Contact()
                                .name("技术支持")
                                .email("support@example.com")
                                .url("https://github.com/yourusername/student-management"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))

                // 服务器配置
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("开发环境"),
                        new Server()
                                .url("https://api.yourdomain.com")
                                .description("生产环境")
                ))

                // 安全认证配置
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请在下方输入JWT Token (不需要Bearer前缀)")))

                // 全局应用安全认证
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }

    /**
     * 全部API（不分组）
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("0-全部接口")
                .pathsToMatch("/api/**")
                .build();
    }

    // ============================================================================
    // V2 API Groups (推荐使用)
    // ============================================================================

    /**
     * V2 组织管理模块
     */
    @Bean
    public GroupedOpenApi v2OrganizationApi() {
        return GroupedOpenApi.builder()
                .group("V2-1-组织管理")
                .displayName("V2 组织管理 (推荐)")
                .pathsToMatch("/api/v2/organization/**", "/api/v2/org-units/**", "/api/v2/grades/**")
                .build();
    }

    /**
     * V2 权限管理模块
     */
    @Bean
    public GroupedOpenApi v2AccessApi() {
        return GroupedOpenApi.builder()
                .group("V2-2-权限管理")
                .displayName("V2 权限管理 (推荐)")
                .pathsToMatch("/api/v2/roles/**", "/api/v2/permissions/**", "/api/v2/user-roles/**")
                .build();
    }

    /**
     * V2 量化检查模块
     */
    @Bean
    public GroupedOpenApi v2InspectionApi() {
        return GroupedOpenApi.builder()
                .group("V2-3-量化检查")
                .displayName("V2 量化检查 (推荐)")
                .pathsToMatch("/api/v2/inspection/**", "/api/v2/inspection-templates/**",
                        "/api/v2/inspection-records/**", "/api/v2/appeals/**")
                .build();
    }

    /**
     * V2 学生管理模块
     */
    @Bean
    public GroupedOpenApi v2StudentApi() {
        return GroupedOpenApi.builder()
                .group("V2-4-学生管理")
                .displayName("V2 学生管理 (推荐)")
                .pathsToMatch("/api/v2/students/**")
                .build();
    }

    /**
     * V2 资产管理模块
     */
    @Bean
    public GroupedOpenApi v2AssetApi() {
        return GroupedOpenApi.builder()
                .group("V2-5-资产管理")
                .displayName("V2 资产管理 (推荐)")
                .pathsToMatch("/api/v2/buildings/**", "/api/v2/dormitories/**")
                .build();
    }

    /**
     * V2 任务管理模块
     */
    @Bean
    public GroupedOpenApi v2TaskApi() {
        return GroupedOpenApi.builder()
                .group("V2-6-任务管理")
                .displayName("V2 任务管理 (推荐)")
                .pathsToMatch("/api/v2/tasks/**", "/api/v2/workflows/**")
                .build();
    }

    /**
     * V2 评比管理模块
     */
    @Bean
    public GroupedOpenApi v2RatingApi() {
        return GroupedOpenApi.builder()
                .group("V2-7-评比管理")
                .displayName("V2 评比管理 (推荐)")
                .pathsToMatch("/api/v2/ratings/**")
                .build();
    }

    /**
     * V2 系统监控模块
     */
    @Bean
    public GroupedOpenApi v2SystemApi() {
        return GroupedOpenApi.builder()
                .group("V2-8-系统监控")
                .displayName("V2 系统监控")
                .pathsToMatch("/api/v2/performance/**", "/api/v2/features/**")
                .build();
    }

    /**
     * V2 全部API
     */
    @Bean
    public GroupedOpenApi v2AllApi() {
        return GroupedOpenApi.builder()
                .group("V2-0-全部V2接口")
                .displayName("V2 全部接口 (推荐)")
                .pathsToMatch("/api/v2/**")
                .build();
    }

    // ============================================================================
    // V1 API Groups (已弃用 - Deprecated)
    // ============================================================================

    /**
     * V1 用户模块API分组 (已弃用)
     */
    @Bean
    public GroupedOpenApi v1UserApiDeprecated() {
        return GroupedOpenApi.builder()
                .group("V1-1-用户模块(已弃用)")
                .pathsToMatch("/api/users/**", "/api/auth/**")
                .build();
    }

    /**
     * V1 学生模块API分组 (已弃用)
     */
    @Bean
    public GroupedOpenApi v1StudentApiDeprecated() {
        return GroupedOpenApi.builder()
                .group("V1-2-学生模块(已弃用)")
                .pathsToMatch("/api/students/**")
                .build();
    }

    /**
     * V1 量化考核模块API分组 (已弃用)
     */
    @Bean
    public GroupedOpenApi v1QuantificationApiDeprecated() {
        return GroupedOpenApi.builder()
                .group("V1-3-量化考核(已弃用)")
                .pathsToMatch("/api/quantification/**")
                .build();
    }
}
