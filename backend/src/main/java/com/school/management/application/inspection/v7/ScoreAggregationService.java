package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.execution.InspProject;
import com.school.management.domain.inspection.model.v7.execution.ProjectScore;
import com.school.management.domain.inspection.model.v7.scoring.GradeBand;
import com.school.management.domain.inspection.model.v7.template.TemplateModuleRef;
import com.school.management.domain.inspection.repository.v7.InspProjectRepository;
import com.school.management.domain.inspection.repository.v7.ProjectScoreRepository;
import com.school.management.domain.inspection.repository.v7.TemplateModuleRefRepository;
import com.school.management.domain.inspection.repository.v7.GradeBandRepository;
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
 * 分数汇总服务
 * 子项目分数 × 权重 → 父项目加权汇总
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ScoreAggregationService {

    private final InspProjectRepository projectRepository;
    private final ProjectScoreRepository scoreRepository;
    private final TemplateModuleRefRepository moduleRefRepository;
    private final GradeBandRepository gradeBandRepository;
    private final ObjectMapper objectMapper;

    /**
     * 汇总父项目在某个周期日的分数
     * 从子项目的 ProjectScore 按权重加权平均
     */
    @Transactional
    public ProjectScore aggregateParentScore(Long parentProjectId, LocalDate cycleDate) {
        InspProject parent = projectRepository.findById(parentProjectId)
                .orElseThrow(() -> new IllegalArgumentException("父项目不存在: " + parentProjectId));

        List<InspProject> children = projectRepository.findByParentProjectId(parentProjectId);
        if (children.isEmpty()) {
            log.debug("项目 {} 没有子项目，跳过汇总", parentProjectId);
            return null;
        }

        // 获取模板模块引用关系以获取权重
        List<TemplateModuleRef> moduleRefs = moduleRefRepository.findByCompositeTemplateId(parent.getTemplateId());
        Map<Long, Integer> templateWeightMap = moduleRefs.stream()
                .collect(Collectors.toMap(TemplateModuleRef::getModuleTemplateId, TemplateModuleRef::getWeight));

        BigDecimal weightedSum = BigDecimal.ZERO;
        int totalWeight = 0;
        int totalTargetCount = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (InspProject child : children) {
            Optional<ProjectScore> childScoreOpt = scoreRepository.findByProjectIdAndCycleDate(child.getId(), cycleDate);
            Integer weight = templateWeightMap.getOrDefault(child.getTemplateId(), 100);

            Map<String, Object> childDetail = new LinkedHashMap<>();
            childDetail.put("projectId", child.getId());
            childDetail.put("projectName", child.getProjectName());
            childDetail.put("templateId", child.getTemplateId());
            childDetail.put("weight", weight);

            if (childScoreOpt.isPresent()) {
                ProjectScore childScore = childScoreOpt.get();
                if (childScore.getScore() != null) {
                    weightedSum = weightedSum.add(childScore.getScore().multiply(BigDecimal.valueOf(weight)));
                    totalWeight += weight;
                    totalTargetCount += childScore.getTargetCount() != null ? childScore.getTargetCount() : 0;
                    childDetail.put("score", childScore.getScore());
                    childDetail.put("grade", childScore.getGrade());
                }
            }
            details.add(childDetail);
        }

        BigDecimal finalScore = null;
        if (totalWeight > 0) {
            finalScore = weightedSum.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
        }

        // 确定等级
        String grade = null;
        if (finalScore != null && parent.getScoringProfileId() != null) {
            grade = determineGrade(parent.getScoringProfileId(), finalScore);
        }

        // 序列化 detail
        String detailJson = null;
        try {
            detailJson = objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            log.warn("序列化汇总明细失败", e);
        }

        // 查找或创建 ProjectScore
        ProjectScore parentScore = scoreRepository.findByProjectIdAndCycleDate(parentProjectId, cycleDate)
                .orElse(ProjectScore.create(parentProjectId, cycleDate));
        parentScore.updateScore(finalScore, grade, totalTargetCount, detailJson);
        return scoreRepository.save(parentScore);
    }

    private String determineGrade(Long scoringProfileId, BigDecimal score) {
        List<GradeBand> bands = gradeBandRepository.findByScoringProfileId(scoringProfileId);
        for (GradeBand band : bands) {
            if (score.compareTo(band.getMinScore()) >= 0 && score.compareTo(band.getMaxScore()) <= 0) {
                return band.getGradeCode();
            }
        }
        return null;
    }
}
