package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.model.v7.scoring.RatingDimension;
import com.school.management.domain.inspection.model.v7.scoring.RatingResult;
import com.school.management.domain.inspection.repository.v7.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评级维度应用服务
 * 管理评级维度（同一份检查数据可评出多个奖项），
 * 负责根据维度配置计算目标评分、评定等级、生成排名。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RatingDimensionApplicationService {

    private final RatingDimensionRepository dimensionRepository;
    private final RatingResultRepository resultRepository;
    private final InspProjectRepository projectRepository;
    private final InspTaskRepository taskRepository;
    private final InspSubmissionRepository submissionRepository;
    private final ObjectMapper objectMapper;

    // ========== Dimension CRUD ==========

    /**
     * 创建评级维度
     *
     * @param projectId      关联项目ID
     * @param dimensionName  维度名称
     * @param sectionIds     关联的分区ID列表（JSON）
     * @param aggregation    聚合方式: WEIGHTED_AVG / SUM / AVG / MAX / MIN
     * @param gradeBands     等级配置（JSON: [{code,name,minScore,maxScore,color}]）
     * @param awardName      奖项名称
     * @param rankingEnabled 是否启用排名
     * @param createdBy      创建人ID
     * @return 创建的评级维度
     */
    @Transactional
    public RatingDimension createDimension(Long projectId, String dimensionName, String sectionIds,
                                            String aggregation, String gradeBands, String awardName,
                                            Boolean rankingEnabled, Long createdBy) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));

        RatingDimension dimension = RatingDimension.builder()
                .projectId(projectId)
                .dimensionName(dimensionName)
                .sectionIds(sectionIds)
                .aggregation(aggregation)
                .gradeBands(gradeBands)
                .awardName(awardName)
                .rankingEnabled(rankingEnabled)
                .createdBy(createdBy)
                .build();

        RatingDimension saved = dimensionRepository.save(dimension);
        log.info("创建评级维度: projectId={}, dimensionName={}", projectId, dimensionName);
        return saved;
    }

    /**
     * 更新评级维度
     */
    @Transactional
    public RatingDimension updateDimension(Long dimensionId, String dimensionName, String sectionIds,
                                            String aggregation, String gradeBands, String awardName,
                                            Boolean rankingEnabled) {
        RatingDimension dimension = dimensionRepository.findById(dimensionId)
                .orElseThrow(() -> new IllegalArgumentException("评级维度不存在: " + dimensionId));

        dimension.update(dimensionName, sectionIds, aggregation, gradeBands, awardName, rankingEnabled);

        RatingDimension saved = dimensionRepository.save(dimension);
        log.info("更新评级维度: dimensionId={}, dimensionName={}", dimensionId, dimensionName);
        return saved;
    }

    /**
     * 删除评级维度
     */
    @Transactional
    public void deleteDimension(Long dimensionId) {
        dimensionRepository.findById(dimensionId)
                .orElseThrow(() -> new IllegalArgumentException("评级维度不存在: " + dimensionId));
        dimensionRepository.deleteById(dimensionId);
        log.info("删除评级维度: dimensionId={}", dimensionId);
    }

    /**
     * 查询项目下的所有评级维度
     */
    @Transactional(readOnly = true)
    public List<RatingDimension> listDimensions(Long projectId) {
        return dimensionRepository.findByProjectId(projectId);
    }

    /**
     * 获取单个评级维度
     */
    @Transactional(readOnly = true)
    public Optional<RatingDimension> getDimension(Long dimensionId) {
        return dimensionRepository.findById(dimensionId);
    }

    // ========== Rating Calculation ==========

    /**
     * 计算评级结果
     *
     * 流程:
     * 1. 获取维度关联的 sectionIds
     * 2. 查询项目下指定日期所有任务的已完成提交（过滤 sectionId 匹配）
     * 3. 按 targetId 聚合分数
     * 4. 根据 gradeBands 评定等级
     * 5. 按分数降序排名
     * 6. 保存 RatingResult 记录（先清除旧记录）
     *
     * @param dimensionId 维度ID
     * @param cycleDate   周期日期
     * @return 评级结果列表
     */
    @Transactional
    public List<RatingResult> calculateRatings(Long dimensionId, LocalDate cycleDate) {
        RatingDimension dimension = dimensionRepository.findById(dimensionId)
                .orElseThrow(() -> new IllegalArgumentException("评级维度不存在: " + dimensionId));

        // 解析维度关联的 sectionIds
        Set<Long> dimensionSectionIds = parseSectionIds(dimension.getSectionIds());
        if (dimensionSectionIds.isEmpty()) {
            log.warn("评级维度 {} 未关联任何分区，无法计算", dimensionId);
            return Collections.emptyList();
        }

        // 获取项目下指定日期的所有任务
        List<InspTask> tasks = taskRepository.findByProjectIdAndTaskDate(dimension.getProjectId(), cycleDate);
        if (tasks.isEmpty()) {
            log.info("项目 {} 在 {} 无任务，无评级数据", dimension.getProjectId(), cycleDate);
            return Collections.emptyList();
        }

        // 收集所有已完成提交中匹配 sectionId 的记录
        Map<Long, List<InspSubmission>> targetSubmissions = new HashMap<>();
        Map<Long, String> targetNames = new HashMap<>();
        Map<Long, String> targetTypes = new HashMap<>();

        for (InspTask task : tasks) {
            List<InspSubmission> submissions = submissionRepository.findByTaskId(task.getId());
            for (InspSubmission sub : submissions) {
                if (sub.getStatus() != SubmissionStatus.COMPLETED) {
                    continue;
                }
                if (sub.getFinalScore() == null) {
                    continue;
                }
                // 只取维度关联分区的提交
                if (sub.getSectionId() != null && !dimensionSectionIds.contains(sub.getSectionId())) {
                    continue;
                }
                targetSubmissions.computeIfAbsent(sub.getTargetId(), k -> new ArrayList<>()).add(sub);
                targetNames.putIfAbsent(sub.getTargetId(), sub.getTargetName());
                if (sub.getTargetType() != null) {
                    targetTypes.putIfAbsent(sub.getTargetId(), sub.getTargetType().name());
                }
            }
        }

        if (targetSubmissions.isEmpty()) {
            log.info("维度 {} 在 {} 无匹配的已完成提交", dimensionId, cycleDate);
            return Collections.emptyList();
        }

        // 按 targetId 聚合分数
        String aggregation = dimension.getAggregation() != null ? dimension.getAggregation() : "AVG";
        Map<Long, BigDecimal> targetScores = new HashMap<>();
        for (Map.Entry<Long, List<InspSubmission>> entry : targetSubmissions.entrySet()) {
            BigDecimal aggregated = aggregateScores(entry.getValue(), aggregation);
            targetScores.put(entry.getKey(), aggregated);
        }

        // 解析 gradeBands
        List<GradeBandConfig> gradeBands = parseGradeBands(dimension.getGradeBands());

        // 按分数降序排序
        List<Map.Entry<Long, BigDecimal>> sortedEntries = targetScores.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList());

        // 清除旧结果
        resultRepository.deleteByDimensionIdAndCycleDate(dimensionId, cycleDate);

        // 构建并保存评级结果
        List<RatingResult> results = new ArrayList<>();
        int rank = 0;
        for (Map.Entry<Long, BigDecimal> entry : sortedEntries) {
            rank++;
            Long targetId = entry.getKey();
            BigDecimal score = entry.getValue();
            String grade = determineGrade(score, gradeBands);
            String targetType = targetTypes.getOrDefault(targetId, "ORG");

            RatingResult result = RatingResult.create(
                    dimensionId, targetId,
                    targetNames.getOrDefault(targetId, ""),
                    targetType, cycleDate, score, grade,
                    Boolean.TRUE.equals(dimension.getRankingEnabled()) ? rank : null);

            results.add(resultRepository.save(result));
        }

        log.info("维度 {} 在 {} 计算了 {} 个目标的评级结果", dimensionId, cycleDate, results.size());
        return results;
    }

    /**
     * 跨周期评级计算 — 从 InspSubmission 表按时间范围聚合分数
     *
     * 流程:
     * 1. 获取维度关联的 sectionIds
     * 2. 查询指定项目在 [cycleStartDate, cycleEndDate] 内的所有任务
     * 3. 遍历任务的已完成提交（按 sectionId 过滤）
     * 4. 按 targetId 聚合分数（使用维度配置的聚合方式）
     * 5. 根据 gradeBands 评定等级
     * 6. 按分数降序排名
     * 7. 以 cycleStartDate 为周期标识，保存/更新 RatingResult
     *
     * @param dimensionId    维度ID
     * @param cycleStartDate 周期开始日期（同时作为 cycleDate 标识）
     * @param cycleEndDate   周期结束日期
     * @return 评级结果列表（按排名升序）
     */
    @Transactional
    public List<RatingResult> calculateRatingFromSubmissions(Long dimensionId,
                                                              LocalDate cycleStartDate,
                                                              LocalDate cycleEndDate) {
        RatingDimension dimension = dimensionRepository.findById(dimensionId)
                .orElseThrow(() -> new IllegalArgumentException("评级维度不存在: " + dimensionId));

        if (cycleStartDate == null || cycleEndDate == null) {
            throw new IllegalArgumentException("周期开始和结束日期不能为空");
        }
        if (cycleStartDate.isAfter(cycleEndDate)) {
            throw new IllegalArgumentException("周期开始日期不能晚于结束日期");
        }

        // 解析维度关联的 sectionIds（null 表示全部分区）
        Set<Long> dimensionSectionIds = parseSectionIds(dimension.getSectionIds());

        // 查询时间范围内的任务
        List<InspTask> tasks = taskRepository.findByProjectIdAndTaskDateBetween(
                dimension.getProjectId(), cycleStartDate, cycleEndDate);

        if (tasks.isEmpty()) {
            log.info("项目 {} 在 {} ~ {} 无任务，无评级数据",
                    dimension.getProjectId(), cycleStartDate, cycleEndDate);
            return Collections.emptyList();
        }

        // 按 targetId 收集已完成提交
        Map<Long, List<InspSubmission>> targetSubmissions = new HashMap<>();
        Map<Long, String> targetNames = new HashMap<>();
        Map<Long, String> targetTypes = new HashMap<>();

        for (InspTask task : tasks) {
            List<InspSubmission> submissions = submissionRepository.findByTaskId(task.getId());
            for (InspSubmission sub : submissions) {
                if (sub.getStatus() != SubmissionStatus.COMPLETED) {
                    continue;
                }
                if (sub.getFinalScore() == null) {
                    continue;
                }
                // 若维度配置了 sectionIds，则只取匹配的提交
                if (!dimensionSectionIds.isEmpty()
                        && sub.getSectionId() != null
                        && !dimensionSectionIds.contains(sub.getSectionId())) {
                    continue;
                }
                targetSubmissions.computeIfAbsent(sub.getTargetId(), k -> new ArrayList<>()).add(sub);
                targetNames.putIfAbsent(sub.getTargetId(), sub.getTargetName());
                if (sub.getTargetType() != null) {
                    targetTypes.putIfAbsent(sub.getTargetId(), sub.getTargetType().name());
                }
            }
        }

        if (targetSubmissions.isEmpty()) {
            log.info("维度 {} 在 {} ~ {} 无匹配的已完成提交", dimensionId, cycleStartDate, cycleEndDate);
            return Collections.emptyList();
        }

        // 聚合分数
        String aggregation = dimension.getAggregation() != null ? dimension.getAggregation() : "AVG";
        Map<Long, BigDecimal> targetScores = new HashMap<>();
        for (Map.Entry<Long, List<InspSubmission>> entry : targetSubmissions.entrySet()) {
            targetScores.put(entry.getKey(), aggregateScores(entry.getValue(), aggregation));
        }

        // 解析 gradeBands
        List<GradeBandConfig> gradeBands = parseGradeBands(dimension.getGradeBands());

        // 按分数降序排序
        List<Map.Entry<Long, BigDecimal>> sortedEntries = targetScores.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList());

        // 清除旧结果（以 cycleStartDate 为周期标识）
        resultRepository.deleteByDimensionIdAndCycleDate(dimensionId, cycleStartDate);

        // 构建并保存评级结果
        List<RatingResult> results = new ArrayList<>();
        int rank = 0;
        for (Map.Entry<Long, BigDecimal> entry : sortedEntries) {
            rank++;
            Long targetId = entry.getKey();
            BigDecimal score = entry.getValue();
            String grade = determineGrade(score, gradeBands);
            String targetType = targetTypes.getOrDefault(targetId, "ORG");

            RatingResult result = RatingResult.create(
                    dimensionId, targetId,
                    targetNames.getOrDefault(targetId, ""),
                    targetType, cycleStartDate, score, grade,
                    Boolean.TRUE.equals(dimension.getRankingEnabled()) ? rank : null);

            results.add(resultRepository.save(result));
        }

        log.info("维度 {} 在 {} ~ {} 跨周期计算了 {} 个目标的评级结果",
                dimensionId, cycleStartDate, cycleEndDate, results.size());
        return results;
    }

    /**
     * 获取评级排名结果
     */
    @Transactional(readOnly = true)
    public List<RatingResult> getRankings(Long dimensionId, LocalDate cycleDate) {
        return resultRepository.findByDimensionIdAndCycleDate(dimensionId, cycleDate);
    }

    // ========== Internal Helpers ==========

    /**
     * 解析 sectionIds JSON 字符串为 Long 集合
     */
    private Set<Long> parseSectionIds(String sectionIdsJson) {
        if (sectionIdsJson == null || sectionIdsJson.isBlank()) {
            return Collections.emptySet();
        }
        try {
            List<Long> ids = objectMapper.readValue(sectionIdsJson, new TypeReference<List<Long>>() {});
            return new HashSet<>(ids);
        } catch (Exception e) {
            log.warn("解析 sectionIds JSON 失败: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 根据聚合方式计算分数
     */
    private BigDecimal aggregateScores(List<InspSubmission> submissions, String aggregation) {
        if (submissions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> scores = submissions.stream()
                .map(InspSubmission::getFinalScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        switch (aggregation) {
            case "SUM":
                return scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            case "MAX":
                return scores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            case "MIN":
                return scores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            case "WEIGHTED_AVG":
                // 加权平均：使用 submission 的 weightRatio
                BigDecimal weightedSum = BigDecimal.ZERO;
                BigDecimal totalWeight = BigDecimal.ZERO;
                for (InspSubmission sub : submissions) {
                    BigDecimal weight = sub.getWeightRatio() != null ? sub.getWeightRatio() : BigDecimal.ONE;
                    weightedSum = weightedSum.add(sub.getFinalScore().multiply(weight));
                    totalWeight = totalWeight.add(weight);
                }
                if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
                    return BigDecimal.ZERO;
                }
                return weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);

            case "AVG":
            default:
                BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                return sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * 根据分数确定等级
     */
    private String determineGrade(BigDecimal score, List<GradeBandConfig> gradeBands) {
        if (gradeBands.isEmpty() || score == null) {
            return null;
        }
        for (GradeBandConfig band : gradeBands) {
            boolean aboveMin = band.minScore == null || score.compareTo(band.minScore) >= 0;
            boolean belowMax = band.maxScore == null || score.compareTo(band.maxScore) <= 0;
            if (aboveMin && belowMax) {
                return band.name != null ? band.name : band.code;
            }
        }
        return null;
    }

    /**
     * 解析 gradeBands JSON
     */
    private List<GradeBandConfig> parseGradeBands(String gradeBandsJson) {
        if (gradeBandsJson == null || gradeBandsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(gradeBandsJson, new TypeReference<List<GradeBandConfig>>() {});
        } catch (Exception e) {
            log.warn("解析 gradeBands JSON 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 等级配置内部类
     */
    private static class GradeBandConfig {
        public String code;
        public String name;
        public BigDecimal minScore;
        public BigDecimal maxScore;
        public String color;
    }
}
