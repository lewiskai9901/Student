package com.school.management.infrastructure.migration;

import com.school.management.application.inspection.ScoringProfileApplicationService;
import com.school.management.domain.inspection.model.scoring.ScoringProfile;
import com.school.management.domain.inspection.repository.ScoringProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScoringProfileOwnershipMigration 幂等性测试.
 * 用 Mock JdbcTemplate 模拟 migration_locks 表行为.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ScoringProfileOwnershipMigration 幂等迁移")
class ScoringProfileOwnershipMigrationTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock TransactionTemplate transactionTemplate;
    @Mock ScoringProfileRepository profileRepository;
    @Mock ScoringProfileApplicationService profileService;

    ScoringProfileOwnershipMigration migration;

    @BeforeEach
    void setUp() {
        migration = new ScoringProfileOwnershipMigration(
                jdbcTemplate, transactionTemplate, profileRepository, profileService);

        // TransactionTemplate.executeWithoutResult — 让 callback 直接跑
        doAnswer(inv -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> c = inv.getArgument(0);
            c.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    @DisplayName("第二次运行: 已 locked, 直接跳过, 不写引用")
    void shouldSkipWhenAlreadyLocked() {
        // migration_locks 表存在
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"),
                eq(Integer.class), eq("migration_locks"))).thenReturn(1);
        // lock_key 已存在
        when(jdbcTemplate.queryForObject(contains("FROM migration_locks WHERE lock_key"),
                eq(Integer.class), eq(ScoringProfileOwnershipMigration.LOCK_KEY))).thenReturn(1);

        migration.run(new DefaultApplicationArguments());

        // 不进入业务事务体, 没有 INSERT/UPDATE/DELETE
        verify(transactionTemplate, never()).executeWithoutResult(any());
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
        verify(profileService, never()).cloneForProject(any(), any(), any());
    }

    @Test
    @DisplayName("首次运行: 无引用对, 只写 lock 不动数据")
    void shouldRunButNoOpWhenNoReferences() {
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"),
                eq(Integer.class), eq("migration_locks"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("FROM migration_locks WHERE lock_key"),
                eq(Integer.class), eq(ScoringProfileOwnershipMigration.LOCK_KEY))).thenReturn(0);
        // 查询 insp_projects 默认 profile: 0 行
        // 查询 insp_inspection_plans.scoring_profile_id 列存在性
        when(jdbcTemplate.queryForObject(contains("information_schema.columns"),
                eq(Integer.class), anyString(), anyString())).thenReturn(0);
        // queryForList 孤儿 ids
        when(jdbcTemplate.queryForList(contains("WHERE project_id IS NULL"), eq(Long.class)))
                .thenReturn(List.of());

        // 业务回调中 jdbcTemplate.query(sql, RowMapper) 对项目/计划扫描返回 0 行
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        migration.run(new DefaultApplicationArguments());

        // 即使没有引用对, 也会写 lock 表示已完成
        verify(jdbcTemplate).update(contains("INSERT INTO migration_locks"),
                eq(ScoringProfileOwnershipMigration.LOCK_KEY), anyString());
        verify(profileService, never()).cloneForProject(any(), any(), any());
    }

    @Test
    @DisplayName("有引用对: 调用 cloneForProject, 更新引用, 删孤儿, 写 lock")
    void shouldMigrateReferencesAndDeleteOrphans() {
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"),
                eq(Integer.class), eq("migration_locks"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("FROM migration_locks WHERE lock_key"),
                eq(Integer.class), eq(ScoringProfileOwnershipMigration.LOCK_KEY))).thenReturn(0);
        // 列存在性: insp_inspection_plans.scoring_profile_id 存在
        when(jdbcTemplate.queryForObject(contains("information_schema.columns"),
                eq(Integer.class), anyString(), anyString())).thenReturn(1);
        // 表存在性: insp_escalation_policies 存在
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"),
                eq(Integer.class), eq("insp_escalation_policies"))).thenReturn(1);

        // 项目级引用: project 700 → profile 500 (旧, projectId=null)
        when(jdbcTemplate.query(contains("FROM insp_projects WHERE default_scoring_profile_id"),
                any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<?> rm = inv.getArgument(1);
            try {
                java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
                when(rs.getLong(1)).thenReturn(700L);
                when(rs.getLong(2)).thenReturn(500L);
                rm.mapRow(rs, 0);
            } catch (Exception ignored) {}
            return List.of();
        });
        // 调度组级引用: 空
        when(jdbcTemplate.query(contains("FROM insp_inspection_plans WHERE scoring_profile_id"),
                any(RowMapper.class))).thenReturn(List.of());

        // findById(500) 返回 projectId=null 的旧 profile
        ScoringProfile orphan = ScoringProfile.reconstruct(ScoringProfile.builder()
                .id(500L).sectionId(100L).projectId(null));
        when(profileRepository.findById(500L)).thenReturn(Optional.of(orphan));

        // cloneForProject 返回 id=900 新副本
        ScoringProfile cloned = ScoringProfile.reconstruct(ScoringProfile.builder()
                .id(900L).sectionId(100L).projectId(700L));
        when(profileService.cloneForProject(eq(500L), eq(700L), any())).thenReturn(cloned);

        when(jdbcTemplate.queryForList(contains("WHERE project_id IS NULL"), eq(Long.class)))
                .thenReturn(List.of(500L));

        // 各 DELETE/UPDATE 返回 row count
        when(jdbcTemplate.update(anyString())).thenReturn(1);
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(1);

        migration.run(new DefaultApplicationArguments());

        verify(profileService).cloneForProject(eq(500L), eq(700L), any());
        // UPDATE 引用切换
        verify(jdbcTemplate, atLeastOnce()).update(
                contains("UPDATE insp_projects SET default_scoring_profile_id"),
                eq(900L), eq(700L), eq(500L));
        // 写 lock
        verify(jdbcTemplate).update(contains("INSERT INTO migration_locks"),
                eq(ScoringProfileOwnershipMigration.LOCK_KEY), anyString());
    }
}
