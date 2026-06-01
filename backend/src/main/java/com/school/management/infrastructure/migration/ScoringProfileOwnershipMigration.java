package com.school.management.infrastructure.migration;

import com.school.management.application.inspection.ScoringProfileApplicationService;
import com.school.management.domain.inspection.model.scoring.ScoringProfile;
import com.school.management.domain.inspection.repository.ScoringProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 一次性数据迁移: ScoringProfile 改为项目-owned (2026-05-23, Phase 2).
 *
 * <p>背景: 重构前 ScoringProfile 按 section_id 跨项目共享, 重构后必须每个项目
 * 各自持有一套. 启动时本组件扫描 (insp_projects.default_scoring_profile_id,
 * insp_inspection_plans.scoring_profile_id) 两处引用, 为每个 (project, profile)
 * 对生成 P-owned 新副本, 把引用切到新副本, 然后删除所有 project_id IS NULL 的
 * 旧 profile + 关联子实体 + 历史版本快照行.
 *
 * <p>幂等: 通过 migration_locks 表的 lock_key 行标记完成 — 第二次启动直接跳过.
 *
 * <p>失败回滚: 整段在事务里, 中途异常会回滚所有 INSERT/UPDATE/DELETE,
 * migration_locks 不写, 下次启动重做.
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE - 100) // 在大部分 bean 之后, 但仍属于启动期
public class ScoringProfileOwnershipMigration implements ApplicationRunner {

    static final String LOCK_KEY = "scoring-profile-project-ownership-2026-05-23";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ScoringProfileRepository profileRepository;
    private final ScoringProfileApplicationService profileService;

    public ScoringProfileOwnershipMigration(JdbcTemplate jdbcTemplate,
                                             TransactionTemplate transactionTemplate,
                                             ScoringProfileRepository profileRepository,
                                             ScoringProfileApplicationService profileService) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.profileRepository = profileRepository;
        this.profileService = profileService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (isLocked()) {
            log.info("[ScoringProfileOwnershipMigration] 已完成 (lock_key={}), 跳过", LOCK_KEY);
            return;
        }
        log.info("[ScoringProfileOwnershipMigration] 开始: 把 ScoringProfile 迁移到项目-owned ...");

        // 单个事务包裹全部操作 — 任一步失败回滚, 不写 migration_lock 下次重试
        transactionTemplate.executeWithoutResult(status -> {
            try {
                doMigrate();
                writeLock();
                log.info("[ScoringProfileOwnershipMigration] 完成, lock 已写入");
            } catch (RuntimeException e) {
                log.error("[ScoringProfileOwnershipMigration] 失败, 已回滚: {}", e.getMessage(), e);
                throw e;
            }
        });
    }

    /** 真正执行迁移. 必须在事务上下文中调用. */
    private void doMigrate() {
        // 1. 收集所有 (projectId, profileId) 引用对
        List<Map.Entry<Long, Long>> refs = new ArrayList<>();

        // 项目级 default_scoring_profile_id
        // 项目级 default_scoring_profile_id (检查列存在 — 该列在 V20260523_4 后被撤销,
        // 全新库无此列, 此一次性迁移在新库上无事可做)
        if (hasColumn("insp_projects", "default_scoring_profile_id")) {
            jdbcTemplate.query(
                    "SELECT id, default_scoring_profile_id FROM insp_projects " +
                            "WHERE default_scoring_profile_id IS NOT NULL",
                    (rs, n) -> {
                        refs.add(new AbstractMap.SimpleEntry<>(
                                rs.getLong(1), rs.getLong(2)));
                        return null;
                    });
        } else {
            log.info("[ScoringProfileOwnershipMigration] insp_projects.default_scoring_profile_id 不存在 (已撤销), 跳过项目级扫描");
        }

        // 调度组级 scoring_profile_id (检查列存在 — 早期库可能尚未加列)
        if (hasColumn("insp_inspection_plans", "scoring_profile_id")) {
            jdbcTemplate.query(
                    "SELECT project_id, scoring_profile_id FROM insp_inspection_plans " +
                            "WHERE scoring_profile_id IS NOT NULL",
                    (rs, n) -> {
                        refs.add(new AbstractMap.SimpleEntry<>(
                                rs.getLong(1), rs.getLong(2)));
                        return null;
                    });
        } else {
            log.info("[ScoringProfileOwnershipMigration] insp_inspection_plans.scoring_profile_id 不存在, 跳过调度组扫描");
        }

        log.info("[ScoringProfileOwnershipMigration] 发现 {} 个引用对 (含重复)", refs.size());

        // 2. 去重并克隆: (projectId, oldProfileId) → newProfileId
        Map<RefKey, Long> cloneMap = new HashMap<>();
        Set<Long> referencedOldIds = new HashSet<>();
        for (Map.Entry<Long, Long> ref : refs) {
            Long projectId = ref.getKey();
            Long oldProfileId = ref.getValue();
            referencedOldIds.add(oldProfileId);
            RefKey key = new RefKey(projectId, oldProfileId);
            if (cloneMap.containsKey(key)) continue;

            // 跳过源不存在 (脏引用) 的对
            if (profileRepository.findById(oldProfileId).isEmpty()) {
                log.warn("[ScoringProfileOwnershipMigration] 引用的源 profile {} 不存在, 跳过 project {}",
                        oldProfileId, projectId);
                continue;
            }
            // 跳过源已经是该项目 owned 的对 (从未污染过)
            ScoringProfile src = profileRepository.findById(oldProfileId).get();
            if (projectId.equals(src.getProjectId())) {
                cloneMap.put(key, oldProfileId);  // 引用本身已合法, 无需克隆
                continue;
            }
            ScoringProfile cloned = profileService.cloneForProject(oldProfileId, projectId, null);
            cloneMap.put(key, cloned.getId());
            log.info("[ScoringProfileOwnershipMigration] 克隆 profile {} → {} (project {})",
                    oldProfileId, cloned.getId(), projectId);
        }

        // 3. 把项目/调度组的引用切到新副本
        int updatedProjects = 0;
        for (Map.Entry<RefKey, Long> e : cloneMap.entrySet()) {
            Long projectId = e.getKey().projectId();
            Long oldId = e.getKey().oldProfileId();
            Long newId = e.getValue();
            if (newId.equals(oldId)) continue; // 无需切换 (本就合法)
            updatedProjects += jdbcTemplate.update(
                    "UPDATE insp_projects SET default_scoring_profile_id = ? " +
                            "WHERE id = ? AND default_scoring_profile_id = ?",
                    newId, projectId, oldId);
            if (hasColumn("insp_inspection_plans", "scoring_profile_id")) {
                jdbcTemplate.update(
                        "UPDATE insp_inspection_plans SET scoring_profile_id = ? " +
                                "WHERE project_id = ? AND scoring_profile_id = ?",
                        newId, projectId, oldId);
            }
        }
        log.info("[ScoringProfileOwnershipMigration] 引用切换完成, 项目级 update 数={}", updatedProjects);

        // 4. 删除所有 project_id IS NULL 的旧 profile (含其子实体与版本快照)
        // 4a. 收集要删的 id 列表
        List<Long> orphanIds = jdbcTemplate.queryForList(
                "SELECT id FROM insp_scoring_profiles WHERE project_id IS NULL",
                Long.class);
        if (!orphanIds.isEmpty()) {
            // 4b. 删除版本快照 (FK-less, 但语义上是 profile 的孩子)
            String inList = orphanIds.stream().map(String::valueOf)
                    .reduce((a, b) -> a + "," + b).orElse("");
            int versionDel = jdbcTemplate.update(
                    "DELETE FROM insp_scoring_profile_versions WHERE profile_id IN (" + inList + ")");
            int dimDel = jdbcTemplate.update(
                    "DELETE FROM insp_score_dimensions WHERE scoring_profile_id IN (" + inList + ")");
            int ruleDel = jdbcTemplate.update(
                    "DELETE FROM insp_calculation_rules WHERE scoring_profile_id IN (" + inList + ")");
            int bandDel = jdbcTemplate.update(
                    "DELETE FROM insp_grade_bands WHERE scoring_profile_id IN (" + inList + ")");
            // EscalationPolicy 是 profile 的孩子, 同样清掉
            int escDel = 0;
            if (hasTable("insp_escalation_policies")) {
                escDel = jdbcTemplate.update(
                        "DELETE FROM insp_escalation_policies WHERE profile_id IN (" + inList + ")");
            }
            int profileDel = jdbcTemplate.update(
                    "DELETE FROM insp_scoring_profiles WHERE id IN (" + inList + ")");
            log.info("[ScoringProfileOwnershipMigration] 清理孤儿: profile={} version={} dim={} rule={} band={} escalation={}",
                    profileDel, versionDel, dimDel, ruleDel, bandDel, escDel);
        } else {
            log.info("[ScoringProfileOwnershipMigration] 无孤儿 profile (project_id IS NULL) 需清理");
        }
    }

    private boolean isLocked() {
        if (!hasTable("migration_locks")) {
            log.warn("[ScoringProfileOwnershipMigration] migration_locks 表不存在 — 跳过迁移 " +
                    "(Phase 1 DB migration 未执行?)");
            return true; // safe-skip: 让运维先 apply schema
        }
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM migration_locks WHERE lock_key = ?",
                Integer.class, LOCK_KEY);
        return cnt != null && cnt > 0;
    }

    private void writeLock() {
        jdbcTemplate.update(
                "INSERT INTO migration_locks (lock_key, note) VALUES (?, ?)",
                LOCK_KEY,
                "ScoringProfile 项目-owned 迁移: 克隆 + 切引用 + 删孤儿");
    }

    private boolean hasColumn(String table, String column) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        return cnt != null && cnt > 0;
    }

    private boolean hasTable(String table) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table);
        return cnt != null && cnt > 0;
    }

    /** 包内可见 (供测试). */
    record RefKey(Long projectId, Long oldProfileId) {}
}
