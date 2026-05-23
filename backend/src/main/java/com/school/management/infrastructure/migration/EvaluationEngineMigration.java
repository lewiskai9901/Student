package com.school.management.infrastructure.migration;

import com.school.management.domain.inspection.model.scoring.GradeScheme;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.model.scoring.MissingPolicy;
import com.school.management.domain.inspection.model.scoring.ScoringProfile;
import com.school.management.domain.inspection.model.scoring.TriggerMode;
import com.school.management.domain.inspection.repository.GradeBandRepository;
import com.school.management.domain.inspection.repository.GradeSchemeRepository;
import com.school.management.domain.inspection.repository.IndicatorRepository;
import com.school.management.domain.inspection.repository.ScoringProfileRepository;
import com.school.management.domain.inspection.model.scoring.GradeBand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 评级引擎完美架构 — 一次性数据迁移 (2026-05-23, Phase 2).
 *
 * <p>背景: 评级权威从 ScoringProfile.GradeBand 收敛到 Indicator + GradeScheme.
 * 启动时扫描所有 ScoringProfile, 对每个含 GradeBand 配置的 profile,
 * 派生一个 GradeScheme + 一个对应的 TIME_WINDOW/PER_TASK Indicator,
 * 关联到该 profile 的 (projectId, sectionId).
 *
 * <p>幂等: 通过 migration_locks 表的 lock_key 行标记完成 — 第二次启动直接跳过.
 *
 * <p>保留: 不删 GradeBand 数据 (Phase 6 后才清理, 留作回退余地).
 */
@Slf4j
@Component
@Order(Integer.MAX_VALUE - 100)
public class EvaluationEngineMigration implements ApplicationRunner {

    static final String LOCK_KEY = "evaluation-engine-perfect-2026-05-23";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ScoringProfileRepository profileRepository;
    private final GradeBandRepository gradeBandRepository;
    private final GradeSchemeRepository gradeSchemeRepository;
    private final IndicatorRepository indicatorRepository;

    public EvaluationEngineMigration(JdbcTemplate jdbcTemplate,
                                      TransactionTemplate transactionTemplate,
                                      ScoringProfileRepository profileRepository,
                                      GradeBandRepository gradeBandRepository,
                                      GradeSchemeRepository gradeSchemeRepository,
                                      IndicatorRepository indicatorRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.profileRepository = profileRepository;
        this.gradeBandRepository = gradeBandRepository;
        this.gradeSchemeRepository = gradeSchemeRepository;
        this.indicatorRepository = indicatorRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (isLocked()) {
            log.info("[EvaluationEngineMigration] 已完成 (lock_key={}), 跳过", LOCK_KEY);
            return;
        }
        log.info("[EvaluationEngineMigration] 开始: GradeBand → Indicator+GradeScheme 派生 ...");

        transactionTemplate.executeWithoutResult(status -> {
            try {
                doMigrate();
                writeLock();
                log.info("[EvaluationEngineMigration] 完成, lock 已写入");
            } catch (RuntimeException e) {
                log.error("[EvaluationEngineMigration] 失败, 已回滚: {}", e.getMessage(), e);
                throw e;
            }
        });
    }

    private void doMigrate() {
        List<ScoringProfile> profiles = profileRepository.findAll();
        log.info("[EvaluationEngineMigration] 扫描到 {} 个 ScoringProfile", profiles.size());

        int derived = 0;
        for (ScoringProfile profile : profiles) {
            Long projectId = profile.getProjectId();
            Long sectionId = profile.getSectionId();
            if (projectId == null || sectionId == null) {
                continue; // 历史脏数据, 跳过
            }
            List<GradeBand> bands = gradeBandRepository.findByScoringProfileId(profile.getId());
            if (bands == null || bands.isEmpty()) {
                continue; // 无 GradeBand 的 profile 不派生
            }
            // 避免重复派生: 同 (project, section) 若已有 Indicator (任意名字), 跳过
            boolean alreadyExists = indicatorRepository.findByProjectId(projectId).stream()
                    .anyMatch(i -> i.getSourceSectionIds() != null
                            && i.getSourceSectionIds().contains(sectionId));
            if (alreadyExists) {
                continue;
            }

            // 1. 派生 GradeScheme (项目级名字: profile-{id}-derived)
            GradeScheme scheme = GradeScheme.create(
                    profile.getTenantId() != null ? profile.getTenantId() : 0L,
                    "profile-" + profile.getId() + "-derived",
                    "迁移派生: 评分方案 " + profile.getId() + " 的 GradeBand → Indicator 等级方案",
                    "PER_PROJECT",
                    profile.getCreatedBy());
            GradeScheme savedScheme = gradeSchemeRepository.save(scheme);

            // 2. 派生 Indicator
            List<Long> sectionIds = new ArrayList<>();
            sectionIds.add(sectionId);
            Indicator indicator = Indicator.reconstruct(Indicator.builder()
                    .tenantId(profile.getTenantId())
                    .projectId(projectId)
                    .name("评级-" + profile.getId())
                    .indicatorType("LEAF")
                    .sourceSectionId(sectionId)
                    .sourceSectionIds(sectionIds)
                    .sourceAggregation("AVG")
                    .triggerMode(TriggerMode.TIME_WINDOW)
                    .missingPolicy(MissingPolicy.IGNORE)
                    .evaluationPeriod("PER_TASK")
                    .gradeSchemeId(savedScheme.getId())
                    .sortOrder(0));
            indicatorRepository.save(indicator);
            derived++;

            log.info("[EvaluationEngineMigration] 派生 Indicator (project={}, section={}) profile={} bands={}",
                    projectId, sectionId, profile.getId(), bands.size());
        }
        log.info("[EvaluationEngineMigration] 派生完成, 新增 Indicator: {}", derived);
    }

    private boolean isLocked() {
        if (!hasTable("migration_locks")) {
            log.warn("[EvaluationEngineMigration] migration_locks 表不存在, 跳过迁移");
            return true;
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
                "评级引擎完美架构: GradeBand → Indicator+GradeScheme 派生");
    }

    private boolean hasTable(String table) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table);
        return cnt != null && cnt > 0;
    }
}
