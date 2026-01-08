package com.school.management.service.evaluation.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.evaluation.StudentEvaluationResult;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.evaluation.StudentEvaluationDetailMapper;
import com.school.management.mapper.evaluation.StudentEvaluationResultMapper;
import com.school.management.service.evaluation.EvaluationResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 综测结果服务实现类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationResultServiceImpl extends ServiceImpl<StudentEvaluationResultMapper, StudentEvaluationResult>
        implements EvaluationResultService {

    private final StudentEvaluationResultMapper resultMapper;
    private final StudentEvaluationDetailMapper detailMapper;

    @Override
    public Page<Map<String, Object>> pageResults(Page<?> page, Map<String, Object> query) {
        return resultMapper.selectResultPage(page, query);
    }

    @Override
    public Map<String, Object> getResultDetail(Long id) {
        Map<String, Object> detail = resultMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException("综测结果不存在");
        }

        // 加载各维度明细
        Long resultId = id;
        detail.put("moralDetails", detailMapper.selectByResultAndDimension(resultId, "moral"));
        detail.put("intellectualDetails", detailMapper.selectByResultAndDimension(resultId, "intellectual"));
        detail.put("physicalDetails", detailMapper.selectByResultAndDimension(resultId, "physical"));
        detail.put("aestheticDetails", detailMapper.selectByResultAndDimension(resultId, "aesthetic"));
        detail.put("laborDetails", detailMapper.selectByResultAndDimension(resultId, "labor"));
        detail.put("developmentDetails", detailMapper.selectByResultAndDimension(resultId, "development"));

        return detail;
    }

    @Override
    public Map<String, Object> getStudentResult(Long studentId, Long periodId) {
        StudentEvaluationResult result = resultMapper.selectByStudentAndPeriod(studentId, periodId);
        if (result == null) {
            return null;
        }
        return getResultDetail(result.getId());
    }

    @Override
    public List<Map<String, Object>> getClassRanking(Long periodId, Long classId) {
        return resultMapper.selectClassRanking(periodId, classId);
    }

    @Override
    public List<Map<String, Object>> getGradeRanking(Long periodId, Long gradeId, Integer limit) {
        return resultMapper.selectGradeRanking(periodId, gradeId, limit);
    }

    @Override
    public Map<String, Object> getStatistics(Long periodId, Long classId, Long gradeId) {
        Map<String, Object> statistics = new HashMap<>();

        if (classId != null) {
            statistics = resultMapper.selectClassStatistics(periodId, classId);
        } else if (gradeId != null) {
            statistics = resultMapper.selectGradeStatistics(periodId, gradeId);
        } else {
            statistics = resultMapper.selectPeriodStatistics(periodId);
        }

        return statistics;
    }

    @Override
    public List<Map<String, Object>> exportResults(Long periodId, Long classId) {
        return resultMapper.selectForExport(periodId, classId);
    }

    @Override
    public Map<String, Object> getDimensionDetail(Long resultId, String dimension) {
        Map<String, Object> detail = new HashMap<>();

        // 获取该维度的明细记录
        List<Map<String, Object>> items = detailMapper.selectByResultAndDimension(resultId, dimension);
        detail.put("items", items);

        // 获取该维度的汇总分数
        StudentEvaluationResult result = resultMapper.selectById(resultId);
        if (result != null) {
            switch (dimension) {
                case "moral":
                    detail.put("baseScore", result.getMoralBaseScore());
                    detail.put("bonusScore", result.getMoralBonusScore());
                    detail.put("deductScore", result.getMoralDeductScore());
                    detail.put("totalScore", result.getMoralTotalScore());
                    detail.put("weightedScore", result.getMoralWeightedScore());
                    break;
                case "intellectual":
                    detail.put("baseScore", result.getIntellectualBaseScore());
                    detail.put("bonusScore", result.getIntellectualBonusScore());
                    detail.put("deductScore", result.getIntellectualDeductScore());
                    detail.put("totalScore", result.getIntellectualTotalScore());
                    detail.put("weightedScore", result.getIntellectualWeightedScore());
                    break;
                case "physical":
                    detail.put("baseScore", result.getPhysicalBaseScore());
                    detail.put("bonusScore", result.getPhysicalBonusScore());
                    detail.put("deductScore", result.getPhysicalDeductScore());
                    detail.put("totalScore", result.getPhysicalTotalScore());
                    detail.put("weightedScore", result.getPhysicalWeightedScore());
                    break;
                case "aesthetic":
                    detail.put("baseScore", result.getAestheticBaseScore());
                    detail.put("bonusScore", result.getAestheticBonusScore());
                    detail.put("deductScore", result.getAestheticDeductScore());
                    detail.put("totalScore", result.getAestheticTotalScore());
                    detail.put("weightedScore", result.getAestheticWeightedScore());
                    break;
                case "labor":
                    detail.put("baseScore", result.getLaborBaseScore());
                    detail.put("bonusScore", result.getLaborBonusScore());
                    detail.put("deductScore", result.getLaborDeductScore());
                    detail.put("totalScore", result.getLaborTotalScore());
                    detail.put("weightedScore", result.getLaborWeightedScore());
                    break;
                case "development":
                    detail.put("baseScore", result.getDevelopmentBaseScore());
                    detail.put("bonusScore", result.getDevelopmentBonusScore());
                    detail.put("deductScore", result.getDevelopmentDeductScore());
                    detail.put("totalScore", result.getDevelopmentTotalScore());
                    detail.put("weightedScore", result.getDevelopmentWeightedScore());
                    break;
            }
        }

        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateResult(StudentEvaluationResult result) {
        StudentEvaluationResult existing = resultMapper.selectByStudentAndPeriod(
                result.getStudentId(), result.getEvaluationPeriodId());

        if (existing != null) {
            result.setId(existing.getId());
            resultMapper.updateById(result);
            log.info("更新综测结果: id={}", result.getId());
        } else {
            resultMapper.insert(result);
            log.info("新增综测结果: id={}", result.getId());
        }
    }

    @Override
    public StudentEvaluationResult getByStudentAndPeriod(Long studentId, Long periodId) {
        return resultMapper.selectByStudentAndPeriod(studentId, periodId);
    }
}
