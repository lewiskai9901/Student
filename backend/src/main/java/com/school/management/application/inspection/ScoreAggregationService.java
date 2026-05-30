package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.execution.*;
import com.school.management.domain.inspection.model.scoring.*;
import com.school.management.domain.inspection.model.template.TemplateSection;
import com.school.management.domain.inspection.repository.*;
import com.school.management.domain.inspection.service.NormalizationBasisResolver;
import com.school.management.domain.inspection.service.ScoreCalculationDomainService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 分数汇总服务（V62 简化版）
 * 负责：
 * 1. 按 cycleDate 维度为项目确定等级（gradeProjectScore）
 * 2. 级联重算：SubmissionDetail → Submission 总分 → Task 汇总 → ProjectScore
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ScoreAggregationService {

    private final InspSubmissionRepository submissionRepository;
    private final SubmissionDetailRepository detailRepository;
    private final InspTaskRepository taskRepository;
    private final InspProjectRepository projectRepository;
    private final InspectionPlanRepository planRepository;
    private final ProjectScoreRepository scoreRepository;
    private final ScoringProfileRepository scoringProfileRepository;
    private final ScoreDimensionRepository dimensionRepository;
    private final CalculationRuleRepository ruleRepository;
    private final GradeBandRepository gradeBandRepository;
    private final TemplateSectionRepository sectionRepository;
    private final EscalationPolicyRepository escalationPolicyRepository;
    private final SubmissionObservationRepository observationRepository;
    private final ScoreCalculationDomainService scoreCalculationService;
    private final NormalizationBasisResolver normalizationBasisResolver;
    private final OrgUnitScoreRollupService orgUnitScoreRollupService;
    private final ObjectMapper objectMapper;

    // ========== Scoring Config Resolution (评级引擎完美架构 2026-05-23) ==========

    /**
     * 评分配置解析的唯一权威方法.
     *
     * <p>评级引擎完美架构: 评分方案按 (projectId, sectionId) 自动定位 — 调度组与项目
     * 不再持有评分方案指针.
     *
     * @param projectId 项目 ID, 必填
     * @param sectionId 分区 ID, 可空 (无分区则返回 null)
     * @return 解析出的 ScoringProfile ID, 可能为 null (该 (project, section) 未配评分方案)
     */
    public Long resolveScoringProfileId(Long projectId, Long sectionId) {
        if (projectId == null || sectionId == null) return null;
        return scoringProfileRepository.findByProjectIdAndSectionId(projectId, sectionId)
                .map(ScoringProfile::getId)
                .orElse(null);
    }

    // ========== Grade Determination ==========

    /**
     * 为项目在指定周期日确定等级
     */
    @Transactional
    public ProjectScore gradeProjectScore(Long projectId, LocalDate cycleDate) {
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));

        ProjectScore score = scoreRepository.findByProjectIdAndCycleDate(projectId, cycleDate)
                .orElse(null);
        if (score == null) {
            log.debug("项目 {} 在 {} 无分数记录", projectId, cycleDate);
            return null;
        }

        // 评级引擎完美架构 (2026-05-23): 项目级周期汇总不再走 defaultScoringProfileId 定级,
        // 评级由 Indicator 跨分区聚合 (IndicatorEvaluationService, Phase 3) 负责.
        // 此处仅保留分数累计, 不再写 grade.
        return score;
    }

    // determineGrade 已删除 — 评级引擎完美架构后, 项目级评级由 Indicator 跨分区聚合负责.

    // ========== Cascade Score Recalculation ==========

    /**
     * 级联重算：从某个 submission 开始，重算整条链。
     * 调用场景：修改 SubmissionDetail 分数后、申诉通过后，以及管理员手动触发。
     *
     * 链路：SubmissionDetail → Submission 总分 → Task completedTargets 计数 → ProjectScore
     *
     * @param submissionId 需要重算的 submission ID
     * @return 重算后的 submission
     */
    @Transactional
    public InspSubmission recalculateFromSubmission(Long submissionId) {
        // 1. 加载 submission 及其关联的 task / project
        InspSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交不存在: " + submissionId));
        Long taskId = submission.getTaskId();
        InspTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalStateException("任务不存在: " + taskId));
        Long projectId = task.getProjectId();
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalStateException("项目不存在: " + projectId));

        // 2. 读取所有明细，重新计算 submission 分数
        // 评级引擎完美架构: 按 (project, section) 解析评分方案.
        Long profileId = resolveScoringProfileId(project.getId(), submission.getSectionId());
        List<SubmissionDetail> details = detailRepository.findBySubmissionId(submissionId);
        ScoreFields fields = computeScoreFields(profileId, details, submission.getSectionId(),
                mapTargetTypeToSubject(submission.getTargetType()), submission.getTargetId());

        // 3. 更新 submission 分数（recalculate 不改变状态，可在 COMPLETED 状态下调用）
        submission.recalculate(fields.baseScore, fields.finalScore,
                fields.deductionTotal, fields.bonusTotal,
                fields.scoreBreakdown, fields.grade, fields.passed);
        submission = submissionRepository.save(submission);

        // 4. 重算该 Task 的所有 Submission 汇总（completedTargets 计数）
        updateTaskCompletedCount(task);

        // 5. 找到该 Task 所属的 Project，重算该 Project 在该日期的 ProjectScore
        recomputeProjectScore(project, task.getTaskDate());

        log.info("级联重算完成: submissionId={}, taskId={}, projectId={}, date={}",
                submissionId, task.getId(), project.getId(), task.getTaskDate());

        return submission;
    }

    /**
     * 兼容旧调用 — 不应用重复违规递增 (无主体信息).
     * @deprecated 改用 5 参数重载传入 subject 信息以启用 EscalationPolicy
     */
    @Deprecated
    ScoreFields computeScoreFields(Long profileId, List<SubmissionDetail> details, Long sectionId) {
        return computeScoreFields(profileId, details, sectionId, null, null);
    }

    /**
     * 计算 submission 的分数字段（与 completeSubmission 逻辑共享）.
     *
     * <p>评分配置下沉 (2026-05-23): profileId 由调用方经
     * {@link #resolveScoringProfileId(InspTask, InspProject)} 解析后传入 —
     * 不再直接从 project 读取, 评分方案的来源 (调度组 / 项目默认) 已收敛到解析方法.
     *
     * @param profileId   解析后的评分方案 ID, 可空 (走无评分配置的简单汇总路径)
     * @param subjectType 主体类型 USER/ORG_UNIT/PLACE/ASSET — 提供时启用重复违规递增 (P0#3)
     * @param subjectId   主体 ID
     */
    public ScoreFields computeScoreFields(Long profileId, List<SubmissionDetail> details, Long sectionId,
                                    String subjectType, Long subjectId) {
        if (profileId != null) {
            ScoringProfile profile = scoringProfileRepository.findById(profileId)
                    .orElseThrow(() -> new IllegalStateException("评分配置不存在: " + profileId));
            List<ScoreDimension> dimensions = dimensionRepository.findByScoringProfileId(profileId);
            List<CalculationRule> rules = ruleRepository.findByScoringProfileIdOrderByPriority(profileId);
            List<GradeBand> gradeBands = gradeBandRepository.findByScoringProfileId(profileId);

            // 规模公平性归一化 (Stage 1): 解析 population 分母 — 引擎按 baseline/population
            // 系数把不同规模单位的扣分摊平.
            //
            // population 与 normConfig 必须由同一个门控决定, 否则会算错: 引擎归一化开关是
            // (normConfig != null && population > 0). 若只满足 normalizationMode!=NONE 却没解析到
            // population (如缺主体信息的 @Deprecated 路径), population 留 1 而 normConfig 非空,
            // 会用 baseline/1=baseline 系数把分数放大. 故只有 normalizeBy 与 mode 都启用且主体可解析时,
            // 才同时给出真实 population 与归一化用 profile (normProfile); 否则 normProfile=null 不归一.
            int population = 1;
            ScoringProfile normProfile = null;
            if (profile.getNormalizeBy() != null && profile.getNormalizeBy() != NormalizeBy.NONE
                    && profile.getNormalizationMode() != null
                    && profile.getNormalizationMode() != NormalizationMode.NONE
                    && subjectType != null && subjectId != null) {
                TargetType tt = mapSubjectToTargetType(subjectType);
                if (tt != null) {
                    population = normalizationBasisResolver.resolveDenominator(
                            tt, subjectId, profile.getNormalizeBy());
                    normProfile = profile;
                }
            }

            List<ScoreCalculationDomainService.ItemScoreInput> inputs = buildItemScoreInputs(details, normProfile);
            // 重复违规递增: 按 EscalationPolicy 放大本次扣分基数
            applyEscalationPolicies(profileId, subjectType, subjectId, inputs);
            ScoreCalculationDomainService.ScoreResult result = scoreCalculationService.calculate(
                    profile, dimensions, rules, gradeBands, inputs, population);

            String breakdown = serializeBreakdown(result);
            return new ScoreFields(
                    result.getRawScore(), result.getFinalScore(),
                    result.getDeductionTotal(), result.getBonusTotal(),
                    breakdown, result.getGrade(), result.isPassed());
        } else {
            // 无评分配置：简单汇总 + 从分区 scoringConfig 读取 gradeBands 计算等级
            BigDecimal sum = BigDecimal.ZERO;
            BigDecimal maxPossible = BigDecimal.ZERO;
            for (SubmissionDetail d : details) {
                if (d.getScore() != null) {
                    sum = sum.add(d.getScore());
                }
                // 从每个 detail 的 scoringConfig 读取 maxScore
                maxPossible = maxPossible.add(parseMaxScore(d.getScoringConfig()));
            }

            String grade = null;
            Boolean passed = null;

            if (sectionId != null) {
                // 分区级 ScoringProfile + GradeBand（等级评价由 IndicatorScoreService 负责）
                try {
                    ScoringProfile sectionProfile = scoringProfileRepository.findBySectionId(sectionId).orElse(null);
                    if (sectionProfile != null) {
                        List<GradeBand> bands = gradeBandRepository.findByScoringProfileId(sectionProfile.getId());
                        if (!bands.isEmpty()) {
                            for (GradeBand band : bands) {
                                if (band.getMinScore() == null || band.getMaxScore() == null) continue;
                                if (sum.compareTo(band.getMinScore()) >= 0 && sum.compareTo(band.getMaxScore()) <= 0) {
                                    // 用 gradeName（自定义标签如"优秀"），无则用 gradeCode
                                    grade = band.getGradeName() != null ? band.getGradeName() : band.getGradeCode();
                                    break;
                                }
                            }
                            // 系统为等级制, 无配置化及格线: 不臆造 60 分硬判定.
                            // passed 保持 null (未知), grade 仍按 gradeBand 给出.
                        }
                    }
                } catch (Exception e) {
                    log.warn("从分区 {} 的 ScoringProfile 读取 GradeBand 失败: {}", sectionId, e.getMessage());
                }

                // 回退：从分区 scoringConfig JSON 读取 gradeBands
                if (grade == null) {
                    try {
                        TemplateSection section = sectionRepository.findById(sectionId).orElse(null);
                        if (section != null && section.getScoringConfig() != null
                                && !section.getScoringConfig().isBlank()) {
                            JsonNode configNode = objectMapper.readTree(section.getScoringConfig());
                            JsonNode gradeBandsNode = configNode.get("gradeBands");
                            if (gradeBandsNode != null && gradeBandsNode.isArray() && gradeBandsNode.size() > 0
                                    && maxPossible.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal percentage = sum.multiply(BigDecimal.valueOf(100))
                                        .divide(maxPossible, 2, RoundingMode.HALF_UP);
                                for (JsonNode band : gradeBandsNode) {
                                    BigDecimal minPct = band.has("minPercent")
                                            ? new BigDecimal(band.get("minPercent").asText()) : BigDecimal.ZERO;
                                    BigDecimal maxPct = band.has("maxPercent")
                                            ? new BigDecimal(band.get("maxPercent").asText()) : BigDecimal.valueOf(100);
                                    if (percentage.compareTo(minPct) >= 0 && percentage.compareTo(maxPct) <= 0) {
                                        grade = band.has("label") ? band.get("label").asText()
                                                : (band.has("grade") ? band.get("grade").asText() : null);
                                        break;
                                    }
                                }
                                // 等级制, 无配置化及格线: 不臆造 60 分硬判定, passed 保持 null.
                            }
                        }
                    } catch (Exception e) {
                        log.warn("从分区 {} 解析 scoringConfig gradeBands 失败: {}", sectionId, e.getMessage());
                    }
                }
            }

            return new ScoreFields(sum, sum, BigDecimal.ZERO, BigDecimal.ZERO, null, grade, passed);
        }
    }

    /**
     * 从 detail 的 scoringConfig JSON 解析 maxScore 值
     */
    private BigDecimal parseMaxScore(String scoringConfig) {
        if (scoringConfig == null || scoringConfig.isBlank()) return BigDecimal.ZERO;
        try {
            var node = objectMapper.readTree(scoringConfig);
            // 1. maxScore > 0
            if (node.has("maxScore")) {
                BigDecimal ms = new BigDecimal(node.get("maxScore").asText());
                if (ms.compareTo(BigDecimal.ZERO) > 0) return ms;
            }
            // 2. CUMULATIVE: baseScore as max
            if (node.has("baseScore")) {
                BigDecimal bs = new BigDecimal(node.get("baseScore").asText());
                if (bs.compareTo(BigDecimal.ZERO) > 0) return bs;
            }
            // 3. SCORE_TABLE: highest score in table
            if (node.has("table") && node.get("table").isArray()) {
                BigDecimal maxVal = BigDecimal.ZERO;
                for (var entry : node.get("table")) {
                    if (entry.has("score")) {
                        BigDecimal s = new BigDecimal(entry.get("score").asText());
                        if (s.compareTo(maxVal) > 0) maxVal = s;
                    }
                }
                if (maxVal.compareTo(BigDecimal.ZERO) > 0) return maxVal;
            }
            // 4. Fallback: passScore
            if (node.has("passScore")) {
                return new BigDecimal(node.get("passScore").asText());
            }
        } catch (Exception e) {
            log.debug("解析 scoringConfig maxScore 失败: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    /**
     * 公开方法：按 projectId + cycleDate 重算 ProjectScore
     * 调用场景：任务发布后触发分数重算
     */
    @Transactional
    public void recomputeProjectScore(Long projectId, LocalDate cycleDate) {
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));
        recomputeProjectScore(project, cycleDate);
    }

    /** 重算 ProjectScore：所有已完成 submissions 的平均分 */
    private void recomputeProjectScore(InspProject project, LocalDate cycleDate) {
        List<InspTask> dateTasks = taskRepository.findByProjectIdAndTaskDate(project.getId(), cycleDate);
        if (dateTasks.isEmpty()) return;

        BigDecimal totalScore = BigDecimal.ZERO;
        int count = 0;

        for (InspTask t : dateTasks) {
            List<InspSubmission> submissions = submissionRepository.findByTaskId(t.getId());
            for (InspSubmission sub : submissions) {
                if (sub.getStatus() == SubmissionStatus.COMPLETED && sub.getFinalScore() != null) {
                    totalScore = totalScore.add(sub.getFinalScore());
                    count++;
                }
            }
        }

        if (count == 0) {
            log.debug("项目 {} 日期 {} 无已完成提交，跳过 ProjectScore 重算",
                    project.getProjectCode(), cycleDate);
            return;
        }

        BigDecimal avgScore = totalScore.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        // P2#12: recomputeProjectScore 并发无锁 — 两个线程同时
        // findByProjectIdAndCycleDate 都 miss → 各自 create → 两条 INSERT.
        // insp_project_scores(tenant_id, project_id, cycle_date) 唯一索引
        // (V20260522_1) 会拒绝第二条 INSERT, 保证不会出现重复汇总行.
        //
        // 失败方在此 catch — 不在同一 @Transactional 内重试 save (异常已把事务
        // 标为 rollback-only, 重试 save 也会在 commit 时失败). recompute 对同一
        // (project, cycleDate) 是幂等的: 胜出线程已写入基于相同数据的正确汇总,
        // 失败方整段 recompute 回滚即可, 数据最终一致.
        try {
            ProjectScore ps = scoreRepository.findByProjectIdAndCycleDate(project.getId(), cycleDate)
                    .orElse(ProjectScore.create(project.getId(), cycleDate));
            ps.updateScore(avgScore, ps.getGrade(), count, ps.getDetail());
            scoreRepository.save(ps);
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            log.warn("ProjectScore 并发插入冲突 (唯一索引拦截), 本次重算放弃, " +
                    "胜出线程已写入等价汇总: project={}, date={}",
                    project.getProjectCode(), cycleDate);
            return;
        }

        // Phase 3.4: 项目分重算成功后级联触发组织树 roll-up.
        // org_unit_scores 是衍生汇总, 读已 save 的 submission/projectScore + 写组织树各级得分.
        // 同一 @Transactional 内执行即可 (rollup 不注入本 service, 无循环依赖).
        //
        // 事务决定: roll-up 失败应 log.error 但不 rethrow —— 衍生汇总失败不能让主算分
        // 事务回滚导致 ProjectScore 也丢. 这与上方 DuplicateKeyException 防御式处理一致.
        // 注意: 此处吞异常后, 若 rollup 内部已把事务标 rollback-only, 主事务 commit 仍会失败;
        // 但 rollup 自身的 upsert 已对 DuplicateKeyException 单独 catch, 不会污染外层事务,
        // 其余异常 (如数据缺失) 仅记录不致命.
        try {
            orgUnitScoreRollupService.rollup(project.getId(), cycleDate);
        } catch (Exception e) {
            log.error("组织树 roll-up 失败 (不影响主算分): project={}, date={}",
                    project.getProjectCode(), cycleDate, e);
        }
    }

    /** 更新 task 的 completedTargets / skippedTargets 计数（按不重复 targetId 统计） */
    private void updateTaskCompletedCount(InspTask task) {
        List<InspSubmission> submissions = submissionRepository.findByTaskId(task.getId());
        // 按 targetId 去重统计，一个目标有多个分区 submission 只算一个
        long distinctTargets = submissions.stream()
                .map(InspSubmission::getTargetId).distinct().count();
        // 一个目标的所有 submission 都 COMPLETED 才算该目标完成
        var byTarget = submissions.stream().collect(java.util.stream.Collectors.groupingBy(InspSubmission::getTargetId));
        int completed = 0;
        int skipped = 0;
        for (var entry : byTarget.entrySet()) {
            var subs = entry.getValue();
            boolean allDone = subs.stream().allMatch(s ->
                    s.getStatus() == SubmissionStatus.COMPLETED || s.getStatus() == SubmissionStatus.SKIPPED);
            boolean anySkipped = subs.stream().anyMatch(s -> s.getStatus() == SubmissionStatus.SKIPPED);
            if (allDone) {
                if (anySkipped) skipped++;
                else completed++;
            }
        }
        task.updateTargetCounts((int) distinctTargets, completed, skipped);
        taskRepository.save(task);
    }

    private List<ScoreCalculationDomainService.ItemScoreInput> buildItemScoreInputs(
            List<SubmissionDetail> details, ScoringProfile normProfile) {
        // 章节级归一化配置 (Stage 1): 仅当调用方解析到 population 时才传入 normProfile,
        // 从它一次性构建 NormalizationConfig 复用到每个 item. normProfile 为 null 时不归一化.
        ScoreCalculationDomainService.NormalizationConfig normConfig =
                normProfile != null ? buildNormalizationConfig(normProfile) : null;

        List<ScoreCalculationDomainService.ItemScoreInput> inputs = new ArrayList<>();
        for (SubmissionDetail detail : details) {
            // scoringMode 仅作记录 / EscalationPolicy 匹配 (DEDUCTION 判定), 不再参与算分.
            String scoringMode = mapScoringMode(detail.getScoringMode());
            BigDecimal configScore = parseConfigScore(detail.getScoringConfig());
            BigDecimal responseNumericValue = parseNumericValue(detail.getResponseValue());
            int quantity = parseQuantity(detail.getResponseValue(), detail.getScoringMode());
            Long dimensionId = parseDimensionId(detail.getDimensions());
            // 单项原始分以 detail.score 为权威 (提交路径已由 ItemScoreEvaluator 算好). null 当 0.
            BigDecimal itemScore = detail.getScore() != null ? detail.getScore() : BigDecimal.ZERO;

            inputs.add(new ScoreCalculationDomainService.ItemScoreInput(
                    detail.getItemCode(), dimensionId, scoringMode,
                    configScore, responseNumericValue, quantity, itemScore, normConfig));
        }
        return inputs;
    }

    /**
     * 从 profile 构建章节级归一化配置.
     * mode 为 null 或 NONE → 返回 null (引擎跳过归一化).
     * 否则 enabled=true, baselinePopulation 缺省为 1.
     * 注意 NormalizationConfig 构造器 cappedAt 在 floorAt 之前.
     */
    private ScoreCalculationDomainService.NormalizationConfig buildNormalizationConfig(ScoringProfile profile) {
        NormalizationMode mode = profile.getNormalizationMode();
        if (mode == null || mode == NormalizationMode.NONE) {
            return null;
        }
        int baseline = profile.getBaselinePopulation() != null ? profile.getBaselinePopulation() : 1;
        return new ScoreCalculationDomainService.NormalizationConfig(
                true, mode, baseline, profile.getNormCap(), profile.getNormFloor(), null);
    }

    /**
     * TargetType → ScoringObservation.subjectType 映射
     * (ORG→ORG_UNIT, 其余原样保留, COMPOSITE 视为 null)
     */
    private static String mapTargetTypeToSubject(TargetType type) {
        if (type == null) return null;
        return switch (type) {
            case ORG -> "ORG_UNIT";
            case USER -> "USER";
            case PLACE -> "PLACE";
            case ASSET -> "ASSET";
            default -> null;
        };
    }

    /**
     * subjectType(String) → TargetType 反向映射 — 给归一化分母解析用.
     * (ORG_UNIT→ORG, USER→USER, PLACE→PLACE, ASSET→ASSET, 其它→null)
     */
    private static TargetType mapSubjectToTargetType(String subjectType) {
        if (subjectType == null) return null;
        return switch (subjectType) {
            case "ORG_UNIT" -> TargetType.ORG;
            case "USER" -> TargetType.USER;
            case "PLACE" -> TargetType.PLACE;
            case "ASSET" -> TargetType.ASSET;
            default -> null;
        };
    }

    /**
     * 应用重复违规递增策略 — 在 inputs 进入 ScoreCalculationDomainService 前
     * 按 EscalationPolicy 放大 DEDUCTION 项的扣分基数 (P0#3, V42 配置接入).
     *
     * <p>规则: 对每个 DEDUCTION input, 按 policy.matchBy 找过去 lookupPeriodDays
     * 内同主体的负面观察次数 N. 当 N >= 1 时, 按 mode 计算 factor:
     * <ul>
     *   <li>MULTIPLY: factor = multiplier^N (累乘), 受 maxEscalationFactor 上限</li>
     *   <li>ADD: factor = 1 + adder * N (累加比例)</li>
     *   <li>FIXED_TABLE: 查 fixedTable 中 occurrence == N+1 的 factor</li>
     * </ul>
     * 当前主体或 itemCode 缺失时跳过, 回退到原始扣分.
     */
    private void applyEscalationPolicies(Long profileId, String subjectType, Long subjectId,
                                          List<ScoreCalculationDomainService.ItemScoreInput> inputs) {
        if (subjectType == null || subjectId == null || profileId == null) return;
        List<EscalationPolicy> policies = escalationPolicyRepository.findByProfileId(profileId);
        if (policies == null || policies.isEmpty()) return;
        // 取第一个启用的策略 (V42 一个 profile 一般一条; 多策略合并是后续优化)
        EscalationPolicy policy = policies.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsEnabled()))
                .findFirst().orElse(null);
        if (policy == null) return;
        // 只支持 ITEM_CODE 匹配 (DIMENSION/SECTION/CATEGORY 留作 P1)
        if (!"ITEM_CODE".equals(policy.getMatchBy())) {
            log.debug("EscalationPolicy matchBy={} 暂未实现, 跳过", policy.getMatchBy());
            return;
        }

        int lookbackDays = policy.getLookupPeriodDays() != null ? policy.getLookupPeriodDays() : 30;
        BigDecimal cap = policy.getMaxEscalationFactor() != null
                ? policy.getMaxEscalationFactor() : new BigDecimal("5.0");

        for (int i = 0; i < inputs.size(); i++) {
            ScoreCalculationDomainService.ItemScoreInput in = inputs.get(i);
            if (!"DEDUCTION".equals(in.getScoringMode())) continue;
            if (in.getItemCode() == null) continue;

            long pastN = observationRepository.countNegativeForSubjectInPeriod(
                    subjectType, subjectId, in.getItemCode(), lookbackDays);
            if (pastN <= 0) continue;

            BigDecimal factor = computeEscalationFactor(policy, (int) pastN);
            if (factor == null || factor.compareTo(BigDecimal.ONE) <= 0) continue;
            if (factor.compareTo(cap) > 0) factor = cap;

            // 评分引擎现以 itemScore 为项原始分, 故重复违规递增改为放大 itemScore (扣分基数).
            BigDecimal oldItemScore = in.getItemScore();
            BigDecimal newItemScore = oldItemScore != null ? oldItemScore.multiply(factor) : null;
            BigDecimal newConfigScore = in.getConfigScore() != null
                    ? in.getConfigScore().multiply(factor) : in.getConfigScore();

            inputs.set(i, new ScoreCalculationDomainService.ItemScoreInput(
                    in.getItemCode(), in.getDimensionId(), in.getScoringMode(),
                    newConfigScore, in.getResponseNumericValue(), in.getQuantity(),
                    newItemScore, null));

            log.debug("EscalationPolicy 应用: itemCode={}, subject={}/{}, pastN={}, factor={}, itemScoreAdjusted={}->{}",
                    in.getItemCode(), subjectType, subjectId, pastN, factor,
                    oldItemScore, newItemScore);
        }
    }

    private BigDecimal computeEscalationFactor(EscalationPolicy policy, int pastN) {
        String mode = policy.getEscalationMode();
        if ("ADD".equals(mode)) {
            BigDecimal adder = policy.getAdder() != null ? policy.getAdder() : BigDecimal.ZERO;
            return BigDecimal.ONE.add(adder.multiply(BigDecimal.valueOf(pastN)));
        }
        if ("FIXED_TABLE".equals(mode) && policy.getFixedTable() != null) {
            try {
                JsonNode arr = objectMapper.readTree(policy.getFixedTable());
                if (arr.isArray()) {
                    int target = pastN + 1; // 当次为第 N+1 次出现
                    for (JsonNode entry : arr) {
                        if (entry.has("occurrence") && entry.get("occurrence").asInt() == target
                                && entry.has("factor")) {
                            return new BigDecimal(entry.get("factor").asText());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析 EscalationPolicy.fixedTable 失败: {}", e.getMessage());
            }
            return BigDecimal.ONE;
        }
        // 默认 MULTIPLY: factor = multiplier^pastN
        BigDecimal multiplier = policy.getMultiplier() != null
                ? policy.getMultiplier() : new BigDecimal("2.0");
        BigDecimal factor = BigDecimal.ONE;
        for (int k = 0; k < pastN; k++) factor = factor.multiply(multiplier);
        return factor;
    }

    private String mapScoringMode(ScoringMode mode) {
        if (mode == null) return "DEDUCTION";
        switch (mode) {
            case DEDUCTION:
            case TIERED_DEDUCTION:
                return "DEDUCTION";
            case ADDITION:
            case CUMULATIVE:
                return "ADDITION";
            case DIRECT:
                return "FIXED";
            default:
                return "RESPONSE_MAPPED";
        }
    }

    private BigDecimal parseConfigScore(String scoringConfig) {
        if (scoringConfig == null || scoringConfig.isBlank()) return BigDecimal.ZERO;
        try {
            var node = objectMapper.readTree(scoringConfig);
            if (node.has("score")) return new BigDecimal(node.get("score").asText());
            if (node.has("configScore")) return new BigDecimal(node.get("configScore").asText());
            if (node.has("baseScore")) return new BigDecimal(node.get("baseScore").asText());
        } catch (Exception e) {
            log.debug("解析 scoringConfig 分值失败: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal parseNumericValue(String responseValue) {
        if (responseValue == null || responseValue.isBlank()) return null;
        try {
            return new BigDecimal(responseValue.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseQuantity(String responseValue, ScoringMode mode) {
        if (mode == ScoringMode.DEDUCTION || mode == ScoringMode.ADDITION
                || mode == ScoringMode.CUMULATIVE || mode == ScoringMode.TIERED_DEDUCTION) {
            BigDecimal val = parseNumericValue(responseValue);
            if (val != null) {
                return Math.max(val.intValue(), 0);
            }
        }
        return 1;
    }

    private Long parseDimensionId(String dimensions) {
        if (dimensions == null || dimensions.isBlank()) return null;
        try {
            var node = objectMapper.readTree(dimensions);
            if (node.isArray() && node.size() > 0) {
                return node.get(0).asLong();
            }
            if (node.has("dimensionId")) {
                return node.get("dimensionId").asLong();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String serializeBreakdown(ScoreCalculationDomainService.ScoreResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("序列化评分结果失败", e);
            return null;
        }
    }

    /** 评分字段值对象（package-private，供 InspSubmissionApplicationService 共享） */
    static class ScoreFields {
        final BigDecimal baseScore;
        final BigDecimal finalScore;
        final BigDecimal deductionTotal;
        final BigDecimal bonusTotal;
        final String scoreBreakdown;
        final String grade;
        final Boolean passed;

        ScoreFields(BigDecimal baseScore, BigDecimal finalScore,
                    BigDecimal deductionTotal, BigDecimal bonusTotal,
                    String scoreBreakdown, String grade, Boolean passed) {
            this.baseScore = baseScore;
            this.finalScore = finalScore;
            this.deductionTotal = deductionTotal;
            this.bonusTotal = bonusTotal;
            this.scoreBreakdown = scoreBreakdown;
            this.grade = grade;
            this.passed = passed;
        }
    }
}
