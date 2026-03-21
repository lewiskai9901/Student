package com.school.management.application.inspection.v7;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.application.inspection.v7.evaluation.EvaluationEngine;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.execution.InspSubmission;
import com.school.management.domain.inspection.model.v7.execution.InspTask;
import com.school.management.domain.inspection.model.v7.scoring.EvaluationCondition;
import com.school.management.domain.inspection.model.v7.scoring.EvaluationLevel;
import com.school.management.domain.inspection.model.v7.scoring.EvaluationResult;
import com.school.management.domain.inspection.model.v7.scoring.EvaluationRule;
import com.school.management.domain.inspection.repository.v7.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评选规则应用服务 — 管理 EvaluationRule、EvaluationLevel，执行评选
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationRuleApplicationService {

    private final EvaluationRuleRepository ruleRepository;
    private final EvaluationLevelRepository levelRepository;
    private final EvaluationResultRepository resultRepository;
    private final InspTaskRepository taskRepository;
    private final InspSubmissionRepository submissionRepository;
    private final EvaluationEngine evaluationEngine;
    private final ObjectMapper objectMapper;

    // ========== EvaluationRule CRUD ==========

    @Transactional(readOnly = true)
    public List<EvaluationRule> listRules(Long projectId) {
        return ruleRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<EvaluationRule> getRule(Long id) {
        return ruleRepository.findById(id);
    }

    @Transactional
    public EvaluationRule createRule(Long projectId, String ruleName, String ruleDescription,
                                     String targetType, String evaluationPeriod,
                                     String awardName, Boolean rankingEnabled, Integer sortOrder) {
        Long userId = SecurityUtils.getCurrentUserId();
        EvaluationRule rule = EvaluationRule.builder()
                .projectId(projectId)
                .ruleName(ruleName)
                .ruleDescription(ruleDescription)
                .targetType(targetType)
                .evaluationPeriod(evaluationPeriod)
                .awardName(awardName)
                .rankingEnabled(rankingEnabled != null ? rankingEnabled : true)
                .sortOrder(sortOrder)
                .createdBy(userId)
                .build();
        EvaluationRule saved = ruleRepository.save(rule);
        log.info("创建评选规则: id={}, projectId={}", saved.getId(), projectId);
        return saved;
    }

    @Transactional
    public EvaluationRule updateRule(Long id, String ruleName, String ruleDescription,
                                     String targetType, String evaluationPeriod,
                                     String awardName, Boolean rankingEnabled, Integer sortOrder) {
        Long userId = SecurityUtils.getCurrentUserId();
        EvaluationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("评选规则不存在: " + id));
        rule.update(ruleName, ruleDescription, targetType, evaluationPeriod,
                awardName, rankingEnabled, sortOrder, userId);
        EvaluationRule saved = ruleRepository.save(rule);
        log.info("更新评选规则: id={}", id);
        return saved;
    }

    @Transactional
    public void deleteRule(Long id) {
        ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("评选规则不存在: " + id));
        levelRepository.deleteByRuleId(id);
        ruleRepository.deleteById(id);
        log.info("删除评选规则: id={}", id);
    }

    // ========== EvaluationLevel ==========

    @Transactional(readOnly = true)
    public List<EvaluationLevel> getLevels(Long ruleId) {
        return levelRepository.findByRuleId(ruleId);
    }

    /**
     * 批量保存等级（全量替换）
     */
    @Transactional
    public List<EvaluationLevel> saveLevels(Long ruleId, List<EvaluationLevel> levels) {
        ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("评选规则不存在: " + ruleId));
        levelRepository.deleteByRuleId(ruleId);
        return levels.stream()
                .map(l -> {
                    EvaluationLevel newLevel = EvaluationLevel.builder()
                            .ruleId(ruleId)
                            .levelNum(l.getLevelNum())
                            .levelName(l.getLevelName())
                            .levelIcon(l.getLevelIcon())
                            .levelColor(l.getLevelColor())
                            .conditionLogic(l.getConditionLogic())
                            .conditions(l.getConditions())
                            .sortOrder(l.getSortOrder())
                            .build();
                    return levelRepository.save(newLevel);
                })
                .collect(Collectors.toList());
    }

    // ========== 执行评选 ==========

    /**
     * 执行评选：对项目中所有目标按等级条件判定，输出排名结果
     *
     * @param ruleId     评选规则 ID
     * @param cycleStart 周期开始日
     * @param cycleEnd   周期结束日
     * @return 评选结果列表（已按分数排名）
     */
    @Transactional
    public List<EvaluationResult> evaluate(Long ruleId, LocalDate cycleStart, LocalDate cycleEnd) {
        // 1. 获取规则 + 等级（按 levelNum 升序 = 最高等级优先判定）
        EvaluationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("评选规则不存在: " + ruleId));
        List<EvaluationLevel> levels = levelRepository.findByRuleId(ruleId);
        if (levels.isEmpty()) {
            log.warn("评选规则 {} 没有配置等级，跳过评选", ruleId);
            return Collections.emptyList();
        }
        // 按 levelNum 升序（1=最高等级先判）
        levels.sort(Comparator.comparingInt(l -> l.getLevelNum() != null ? l.getLevelNum() : 999));

        Long projectId = rule.getProjectId();
        String targetType = rule.getTargetType();

        // 2. 获取周期内所有任务，收集目标（targetId → targetName 去重）
        List<InspTask> tasks = taskRepository.findByProjectIdAndTaskDateBetween(
                projectId, cycleStart, cycleEnd);
        if (tasks.isEmpty()) {
            log.info("项目 {} 在周期 [{}, {}] 内没有任务，跳过评选", projectId, cycleStart, cycleEnd);
            return Collections.emptyList();
        }

        Map<Long, String> targetMap = new LinkedHashMap<>();
        for (InspTask task : tasks) {
            List<InspSubmission> submissions = submissionRepository.findByTaskId(task.getId());
            for (InspSubmission s : submissions) {
                if (s.getTargetId() != null && s.getTargetName() != null) {
                    targetMap.putIfAbsent(s.getTargetId(), s.getTargetName());
                }
            }
        }
        log.info("评选规则 {} 找到 {} 个目标", ruleId, targetMap.size());

        // 3. 对每个目标进行等级判定
        List<TargetEvalResult> rawResults = new ArrayList<>();
        for (Map.Entry<Long, String> entry : targetMap.entrySet()) {
            Long targetId = entry.getKey();
            String targetName = entry.getValue();

            // 计算该目标的平均得分（用于排名）
            BigDecimal avgScore = evaluationEngine.computeAvgScore(
                    targetId, cycleStart, cycleEnd, projectId);

            // 从最高等级开始判定
            Integer matchedLevelNum = null;
            String matchedLevelName = null;
            Map<String, Boolean> conditionDetails = new LinkedHashMap<>();

            for (EvaluationLevel level : levels) {
                List<EvaluationCondition> conditions = level.parseConditions();
                boolean levelMatched = evaluateLevelConditions(
                        conditions, level.getConditionLogic(),
                        targetId, targetType, cycleStart, cycleEnd, projectId, conditionDetails);
                if (levelMatched) {
                    matchedLevelNum = level.getLevelNum();
                    matchedLevelName = level.getLevelName();
                    break;
                }
            }

            rawResults.add(new TargetEvalResult(
                    targetId, targetName, avgScore, matchedLevelNum, matchedLevelName, conditionDetails));
        }

        // 4. 按分数降序排名
        rawResults.sort(Comparator.comparing(TargetEvalResult::score, Comparator.reverseOrder()));

        // 5. 清除旧结果，保存新结果
        resultRepository.deleteByRuleIdAndCycleDate(ruleId, cycleStart);

        List<EvaluationResult> results = new ArrayList<>();
        int rank = 1;
        for (TargetEvalResult raw : rawResults) {
            String detailsJson = serializeDetails(raw.conditionDetails());
            EvaluationResult result = EvaluationResult.builder()
                    .ruleId(ruleId)
                    .targetType(targetType)
                    .targetId(raw.targetId())
                    .targetName(raw.targetName())
                    .cycleDate(cycleStart)
                    .levelNum(raw.levelNum())
                    .levelName(raw.levelName())
                    .score(raw.score())
                    .rankNo(Boolean.TRUE.equals(rule.getRankingEnabled()) ? rank : null)
                    .details(detailsJson)
                    .build();
            results.add(resultRepository.save(result));
            rank++;
        }

        log.info("评选规则 {} 完成，共 {} 条结果", ruleId, results.size());
        return results;
    }

    // ========== 查询结果 ==========

    @Transactional(readOnly = true)
    public List<EvaluationResult> getResults(Long ruleId, LocalDate cycleDate) {
        return resultRepository.findByRuleIdAndCycleDate(ruleId, cycleDate);
    }

    // ========== 私有工具方法 ==========

    /**
     * 判定单个等级的所有条件（AND / OR）
     */
    private boolean evaluateLevelConditions(List<EvaluationCondition> conditions,
                                            String logic,
                                            Long targetId, String targetType,
                                            LocalDate cycleStart, LocalDate cycleEnd,
                                            Long projectId,
                                            Map<String, Boolean> detailsAccumulator) {
        if (conditions == null || conditions.isEmpty()) {
            // 无条件配置的等级视为"兜底等级"（始终匹配）
            return true;
        }
        boolean useAnd = !"OR".equalsIgnoreCase(logic);
        boolean result = useAnd; // AND 初始 true，OR 初始 false
        for (int i = 0; i < conditions.size(); i++) {
            EvaluationCondition cond = conditions.get(i);
            boolean matched = evaluationEngine.evaluateCondition(
                    cond, targetId, targetType, cycleStart, cycleEnd, projectId);
            String key = (cond.getDescription() != null && !cond.getDescription().isBlank())
                    ? cond.getDescription()
                    : (cond.getConditionType() + "_" + i);
            detailsAccumulator.put(key, matched);
            if (useAnd) {
                result = result && matched;
            } else {
                result = result || matched;
            }
        }
        return result;
    }

    private String serializeDetails(Map<String, Boolean> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** 内部 VO，用于排名前的临时数据 */
    private record TargetEvalResult(Long targetId, String targetName, BigDecimal score,
                                    Integer levelNum, String levelName,
                                    Map<String, Boolean> conditionDetails) {
    }
}
