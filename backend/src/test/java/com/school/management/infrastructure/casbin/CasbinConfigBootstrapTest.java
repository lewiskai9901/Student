package com.school.management.infrastructure.casbin;

import org.casbin.jcasbin.main.Enforcer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link CasbinConfig#enforcer(JdbcTemplate)} 从业务表加载策略的完整链路:
 * <ol>
 *   <li>两条 SQL 各自产生的 row map 能正确映射为 Casbin p/g 策略</li>
 *   <li>双冒号 permission_code 按最后一个冒号切分,避免与 @CasbinAccess(resource,action) 错配</li>
 *   <li>生成的 Enforcer 对 (user, tenant, resource, action) 的实际鉴权结果正确</li>
 * </ol>
 *
 * <p>不走 Spring 上下文:mock JdbcTemplate,真跑 Enforcer (加载 classpath model.conf)。
 */
@ExtendWith(MockitoExtension.class)
class CasbinConfigBootstrapTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("启动加载: p-policy + g-policy 正确写入 Enforcer 并能鉴权通过")
    void bootstrap_buildsEnforceableState() throws Exception {
        // role_permissions JOIN: TEACHER 在租户 1 获得双冒号 academic:course:view
        List<Map<String, Object>> rpRows = List.of(
                Map.of("role_code", "TEACHER", "tenant_id", 1L, "permission_code", "academic:course:view"),
                Map.of("role_code", "TEACHER", "tenant_id", 1L, "permission_code", "system:user:view")
        );
        // user_roles JOIN: user 100 绑定 TEACHER
        List<Map<String, Object>> urRows = List.of(
                Map.of("user_id", 100L, "role_code", "TEACHER", "tenant_id", 1L)
        );

        when(jdbcTemplate.queryForList(anyString()))
                .thenReturn(rpRows)   // 第一次调用 → loadPolicies
                .thenReturn(urRows);  // 第二次调用 → loadGroupings

        Enforcer enforcer = new CasbinConfig().enforcer(jdbcTemplate);

        // 双冒号权限按最后冒号切分:(academic:course, view) 应鉴权通过
        assertThat(enforcer.enforce("100", "1", "academic:course", "view"))
                .as("teacher with academic:course:view should pass for (academic:course, view)")
                .isTrue();

        // 错误切分应失败 — 保护 split(':',2) 回归
        assertThat(enforcer.enforce("100", "1", "academic", "course:view"))
                .as("wrong split into (academic, course:view) must NOT pass")
                .isFalse();

        // 单冒号权限正常工作
        assertThat(enforcer.enforce("100", "1", "system:user", "view"))
                .as("teacher with system:user:view should pass for (system:user, view)")
                .isTrue();

        // 跨租户应隔离 — role 绑定在 tenant 1,请求 tenant 2 拒绝
        assertThat(enforcer.enforce("100", "2", "academic:course", "view"))
                .as("cross-tenant access must be denied (domain isolation)")
                .isFalse();

        // 未授权动作拒绝
        assertThat(enforcer.enforce("100", "1", "academic:course", "delete"))
                .as("unauthorized action must be denied")
                .isFalse();

        // 未绑角色的用户拒绝
        assertThat(enforcer.enforce("999", "1", "academic:course", "view"))
                .as("user without role binding must be denied")
                .isFalse();
    }

    @Test
    @DisplayName("空数据: 两张表都为空时 Enforcer 正常构造但所有请求都拒绝")
    void bootstrap_withEmptyTables_producesEmptyButValidEnforcer() throws Exception {
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

        Enforcer enforcer = new CasbinConfig().enforcer(jdbcTemplate);

        assertThat(enforcer.enforce("1", "1", "any", "any"))
                .as("empty policy set must deny by default")
                .isFalse();
    }
}
