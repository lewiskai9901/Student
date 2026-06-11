package com.school.management.infrastructure.extension;

import com.school.management.infrastructure.extension.event.PermissionsRefreshedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link RolePermissionBindingRegistrar} 单测 — 角色 × 功能权限默认绑定 (镜像 P5-1
 * RoleScopeBindingRegistrar 的"声明式默认 + 命令式覆盖"模式, 落 role_permissions 表)。
 *
 * <p>背景: 功能权限四层中授予层完全空白 — 14 个非超管角色 role_permissions 0 行,
 * 任何非超管访问任何 MANAGEMENT @CasbinAccess 端点都 403。building blocks
 * (PermissionContribution / RoleContribution) 都有, 唯独缺"默认 wiring"机制:
 * RolePresetDef.permissionCodes 是从未被消费的死字段。
 *
 * <p>时序约束 (为何不挂 ContributionDispatcher): dispatcher 是 @Order(60), 跑在
 * PermissionRegistrar / RolePresetRegistrar (100~500) 之前 — 全新库上彼时 roles /
 * permissions 还没注册, 绑定会全部 skip。本 Registrar 必须是独立 @Order(600)
 * ApplicationRunner, 自行迭代 PluginPackage.contribute()。
 *
 * <p>Casbin 重载: 批量 upsert 后若有新增, 发一次 {@link PermissionsRefreshedEvent}
 * 让 CasbinPolicyService.syncFromDatabase() 把新 grants 装进内存 enforcer;
 * 全部 SKIPPED_EXISTING (稳态重启) 则不发, 避免无谓全量重建。
 */
class RolePermissionBindingRegistrarTest {

    private JdbcTemplate jdbc;
    private ApplicationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        publisher = mock(ApplicationEventPublisher.class);
    }

    private RolePermissionBindingRegistrar registrar(PluginPackage... pkgs) {
        return new RolePermissionBindingRegistrar(List.of(pkgs), jdbc, publisher);
    }

    /** 默认 stub: 角色/权限都存在, INSERT IGNORE 生效 */
    private void stubHappyPath() {
        when(jdbc.queryForObject(contains("FROM roles"), eq(Long.class), any(Object[].class)))
            .thenReturn(101L);
        when(jdbc.queryForObject(contains("FROM permissions"), eq(Long.class), any(Object[].class)))
            .thenReturn(202L);
        when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(1);
    }

    // ───────────────────────── upsert 单元语义 ─────────────────────────

    @Test
    void upsert_happyPath_insertIgnoreWithResolvedIds_returnsCreated() {
        stubHappyPath();

        var result = registrar().upsert("CLASS_TEACHER", "student:info:view", 1L);

        assertThat(result).isEqualTo(RolePermissionBindingRegistrar.Result.CREATED);
        verify(jdbc).update(contains("INSERT IGNORE INTO role_permissions"),
            eq(101L), eq(202L), eq(1L));
    }

    @Test
    void upsert_roleNotFound_skipsWithoutInsert() {
        when(jdbc.queryForObject(contains("FROM roles"), eq(Long.class), any(Object[].class)))
            .thenThrow(new EmptyResultDataAccessException(1));

        var result = registrar().upsert("NO_SUCH_ROLE", "student:info:view", 1L);

        assertThat(result).isEqualTo(RolePermissionBindingRegistrar.Result.SKIPPED_NO_ROLE);
        verify(jdbc, never()).update(any(String.class), any(Object[].class));
    }

    @Test
    void upsert_permissionNotRegistered_skipsWithoutInsert() {
        when(jdbc.queryForObject(contains("FROM roles"), eq(Long.class), any(Object[].class)))
            .thenReturn(101L);
        when(jdbc.queryForObject(contains("FROM permissions"), eq(Long.class), any(Object[].class)))
            .thenThrow(new EmptyResultDataAccessException(1));

        var result = registrar().upsert("CLASS_TEACHER", "not:registered", 1L);

        assertThat(result).isEqualTo(RolePermissionBindingRegistrar.Result.SKIPPED_NO_PERMISSION);
        verify(jdbc, never()).update(any(String.class), any(Object[].class));
    }

    @Test
    void upsert_alreadyBound_skipsPreservingAdminConfig() {
        stubHappyPath();
        when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(0); // INSERT IGNORE 0 行

        var result = registrar().upsert("CLASS_TEACHER", "student:info:view", 1L);

        assertThat(result).isEqualTo(RolePermissionBindingRegistrar.Result.SKIPPED_EXISTING);
    }

    // ───────────────────────── run() 批量分发 + 事件 ─────────────────────────

    @Test
    void run_upsertsAllBindingsFromPackages_andPublishesRefreshOnceWhenCreated() throws Exception {
        stubHappyPath();
        PluginPackage pkg = mock(PluginPackage.class);
        when(pkg.contribute()).thenReturn(Stream.of(
            Contribution.RolePermissionBindingContribution.bind("CLASS_TEACHER", "student:info:view"),
            Contribution.RolePermissionBindingContribution.bind("CLASS_TEACHER", "student:attendance:view"),
            // 非本类型贡献应被无害跳过
            Contribution.RoleScopeBindingContribution.bind("CLASS_TEACHER", "student", "BY_CLASS")
        ));

        registrar(pkg).run(null);

        verify(jdbc, times(2)).update(contains("INSERT IGNORE INTO role_permissions"),
            any(), any(), any());
        verify(publisher, times(1)).publishEvent(any(PermissionsRefreshedEvent.class));
    }

    @Test
    void run_allExisting_doesNotPublishRefresh() throws Exception {
        stubHappyPath();
        when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(0);
        PluginPackage pkg = mock(PluginPackage.class);
        when(pkg.contribute()).thenReturn(Stream.of(
            Contribution.RolePermissionBindingContribution.bind("CLASS_TEACHER", "student:info:view")
        ));

        registrar(pkg).run(null);

        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void run_oneBindingFails_othersStillProcessed() throws Exception {
        stubHappyPath();
        when(jdbc.queryForObject(contains("FROM roles"), eq(Long.class), any(Object[].class)))
            .thenThrow(new RuntimeException("boom"))   // 第 1 条角色查询炸
            .thenReturn(101L);                          // 第 2 条正常
        PluginPackage pkg = mock(PluginPackage.class);
        when(pkg.contribute()).thenReturn(Stream.of(
            Contribution.RolePermissionBindingContribution.bind("ROLE_A", "x:view"),
            Contribution.RolePermissionBindingContribution.bind("ROLE_B", "y:view")
        ));

        registrar(pkg).run(null); // 不应整体抛出

        verify(jdbc, times(1)).update(contains("INSERT IGNORE INTO role_permissions"),
            any(), any(), any());
    }

    // ───────────────────────── 贡献 record 形态 ─────────────────────────

    @Test
    void bindAll_expandsToOneContributionPerPermission() {
        List<Contribution.RolePermissionBindingContribution> all =
            Contribution.RolePermissionBindingContribution.bindAll("CLASS_TEACHER",
                "student:info:view", "student:attendance:view", "insp:task:execute");

        assertThat(all).hasSize(3);
        assertThat(all.get(0).uniqueKey()).isEqualTo("role-perm:CLASS_TEACHER/student:info:view");
        assertThat(all).allMatch(c -> c.roleCode().equals("CLASS_TEACHER"));
    }
}
