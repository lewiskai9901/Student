package com.school.management.service.evaluation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.entity.Student;
import com.school.management.entity.evaluation.*;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.StudentMapper;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.evaluation.*;
import com.school.management.service.evaluation.EvaluationCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 综测计算引擎服务实现类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationCalculationServiceImpl implements EvaluationCalculationService {

    private final EvaluationPeriodMapper periodMapper;
    private final EvaluationDimensionMapper dimensionMapper;
    private final StudentEvaluationResultMapper resultMapper;
    private final StudentEvaluationDetailMapper detailMapper;
    private final CheckRecordItemStudentMapper checkItemStudentMapper;
    private final RatingResultStudentEffectMapper ratingEffectMapper;
    private final StudentHonorApplicationMapper honorMapper;
    private final StudentPunishmentMapper punishmentMapper;
    private final StudentScoreMapper scoreMapper;
    private final StudentMapper studentMapper;
    private final ClassMapper classMapper;

    // 维度权重缓存
    private Map<String, BigDecimal> dimensionWeights;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentEvaluationResult calculateStudent(Long periodId, Long studentId) {
        log.info("开始计算学生综测: periodId={}, studentId={}", periodId, studentId);

        // 验证周期
        EvaluationPeriod period = periodMapper.selectById(periodId);
        if (period == null) {
            throw new BusinessException("综测周期不存在");
        }

        // 加载维度权重
        loadDimensionWeights();

        // 获取或创建结果记录
        StudentEvaluationResult result = resultMapper.selectByPeriodAndStudent(periodId, studentId);
        boolean isNew = (result == null);
        if (isNew) {
            result = createNewResult(periodId, studentId, period);
        }

        // 清除旧的明细
        if (!isNew) {
            detailMapper.deleteByResultId(result.getId());
        }

        // 计算各维度分数
        List<StudentEvaluationDetail> allDetails = new ArrayList<>();

        // 德育
        Map<String, Object> moralResult = calculateMoralScore(periodId, studentId);
        applyDimensionScore(result, "MORAL", moralResult);
        allDetails.addAll((List<StudentEvaluationDetail>) moralResult.get("details"));

        // 智育
        Map<String, Object> intellectualResult = calculateIntellectualScore(periodId, studentId);
        applyDimensionScore(result, "INTELLECTUAL", intellectualResult);
        allDetails.addAll((List<StudentEvaluationDetail>) intellectualResult.get("details"));

        // 体育
        Map<String, Object> physicalResult = calculatePhysicalScore(periodId, studentId);
        applyDimensionScore(result, "PHYSICAL", physicalResult);
        allDetails.addAll((List<StudentEvaluationDetail>) physicalResult.get("details"));

        // 美育
        Map<String, Object> aestheticResult = calculateAestheticScore(periodId, studentId);
        applyDimensionScore(result, "AESTHETIC", aestheticResult);
        allDetails.addAll((List<StudentEvaluationDetail>) aestheticResult.get("details"));

        // 劳育
        Map<String, Object> laborResult = calculateLaborScore(periodId, studentId);
        applyDimensionScore(result, "LABOR", laborResult);
        allDetails.addAll((List<StudentEvaluationDetail>) laborResult.get("details"));

        // 发展素质
        Map<String, Object> developmentResult = calculateDevelopmentScore(periodId, studentId);
        applyDimensionScore(result, "DEVELOPMENT", developmentResult);
        allDetails.addAll((List<StudentEvaluationDetail>) developmentResult.get("details"));

        // 处理处分影响
        Map<String, Object> punishmentEffect = getPunishmentEffect(studentId, periodId);
        applyPunishmentEffect(result, punishmentEffect);

        // 计算总分
        result.calculateTotalScore();
        result.setStatus(StudentEvaluationResult.STATUS_CALCULATED);
        result.setCalculatedAt(LocalDateTime.now());
        result.setVersion(isNew ? 1 : result.getVersion() + 1);

        // 保存结果
        if (isNew) {
            resultMapper.insert(result);
        } else {
            resultMapper.updateById(result);
        }

        // 保存明细
        for (StudentEvaluationDetail detail : allDetails) {
            detail.setResultId(result.getId());
            detail.setEvaluationPeriodId(periodId);
            detail.setStudentId(studentId);
        }
        if (!allDetails.isEmpty()) {
            detailMapper.batchInsert(allDetails);
        }

        log.info("学生综测计算完成: studentId={}, totalScore={}", studentId, result.getTotalScore());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StudentEvaluationResult> calculateClass(Long periodId, Long classId) {
        log.info("开始计算班级综测: periodId={}, classId={}", periodId, classId);

        // 获取班级学生列表
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getClassId, classId)
                      .eq(Student::getStudentStatus, 0)  // 只计算在读学生(0=在读)
                      .eq(Student::getDeleted, 0);
        List<Student> students = studentMapper.selectList(studentWrapper);

        if (students.isEmpty()) {
            log.warn("班级无学生: classId={}", classId);
            return new ArrayList<>();
        }

        log.info("班级学生数: classId={}, count={}", classId, students.size());

        // 逐个计算学生综测
        List<StudentEvaluationResult> results = new ArrayList<>();
        for (Student student : students) {
            try {
                StudentEvaluationResult result = calculateStudent(periodId, student.getId());
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                log.error("学生综测计算失败: studentId={}", student.getId(), e);
            }
        }

        // 计算完成后计算班级排名
        if (!results.isEmpty()) {
            resultMapper.calculateClassRank(periodId, classId);
        }

        log.info("班级综测计算完成: classId={}, 成功数={}", classId, results.size());
        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int calculateGrade(Long periodId, Long gradeId) {
        log.info("开始计算年级综测: periodId={}, gradeId={}", periodId, gradeId);

        // 获取年级下所有班级
        LambdaQueryWrapper<com.school.management.entity.Class> classWrapper = new LambdaQueryWrapper<>();
        classWrapper.eq(com.school.management.entity.Class::getGradeId, gradeId)
                    .eq(com.school.management.entity.Class::getStatus, 1)
                    .eq(com.school.management.entity.Class::getDeleted, 0);
        List<com.school.management.entity.Class> classes = classMapper.selectList(classWrapper);

        if (classes.isEmpty()) {
            log.warn("年级无班级: gradeId={}", gradeId);
            return 0;
        }

        log.info("年级班级数: gradeId={}, count={}", gradeId, classes.size());

        // 逐个班级计算
        int totalStudents = 0;
        for (com.school.management.entity.Class clazz : classes) {
            try {
                List<StudentEvaluationResult> results = calculateClass(periodId, clazz.getId());
                totalStudents += results.size();
            } catch (Exception e) {
                log.error("班级综测计算失败: classId={}", clazz.getId(), e);
            }
        }

        // 计算年级排名
        if (totalStudents > 0) {
            resultMapper.calculateGradeRank(periodId, gradeId);
        }

        log.info("年级综测计算完成: gradeId={}, 总学生数={}", gradeId, totalStudents);
        return totalStudents;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentEvaluationResult recalculate(Long resultId) {
        StudentEvaluationResult existing = resultMapper.selectById(resultId);
        if (existing == null) {
            throw new BusinessException("综测结果不存在");
        }
        return calculateStudent(existing.getEvaluationPeriodId(), existing.getStudentId());
    }

    @Override
    public Map<String, Object> calculateMoralScore(Long periodId, Long studentId) {
        Map<String, Object> result = new HashMap<>();
        List<StudentEvaluationDetail> details = new ArrayList<>();
        BigDecimal baseScore = new BigDecimal("60");
        BigDecimal bonusScore = BigDecimal.ZERO;
        BigDecimal deductScore = BigDecimal.ZERO;

        EvaluationPeriod period = periodMapper.selectById(periodId);

        // 1. 获取量化扣分数据
        List<CheckRecordItemStudent> checkItems = checkItemStudentMapper.selectByStudentAndDateRange(
                studentId, period.getDataStartDate(), period.getDataEndDate());

        for (CheckRecordItemStudent item : checkItems) {
            if (item.getConfirmed() != CheckRecordItemStudent.CONFIRMED_YES) continue;
            if (item.getMoralScore() != null && item.getMoralScore().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal score = item.getMoralScore();
                if (score.compareTo(BigDecimal.ZERO) < 0) {
                    deductScore = deductScore.add(score.abs());
                } else {
                    bonusScore = bonusScore.add(score);
                }

                StudentEvaluationDetail detail = new StudentEvaluationDetail();
                detail.setDetailType(StudentEvaluationDetail.TYPE_QUANTIFICATION);
                detail.setEvaluationDimension(EvaluationDimension.MORAL);
                detail.setScoreCategory(score.compareTo(BigDecimal.ZERO) < 0 ?
                        StudentEvaluationDetail.CATEGORY_DEDUCT : StudentEvaluationDetail.CATEGORY_BONUS);
                detail.setSourceType(StudentEvaluationDetail.SOURCE_CHECK_ITEM);
                detail.setSourceId(item.getRecordItemId());
                detail.setSourceName(item.getBehaviorTypeName());
                detail.setSourceDate(item.getCheckDate());
                detail.setScore(score);
                detail.setBehaviorTypeId(item.getBehaviorTypeId());
                detail.setBehaviorTypeCode(item.getBehaviorTypeCode());
                details.add(detail);
            }
        }

        // 2. 获取评级加分数据
        List<RatingResultStudentEffect> ratingEffects = getRatingEffectsForDimension(
                studentId, period.getDataStartDate(), period.getDataEndDate(), EvaluationDimension.MORAL);

        for (RatingResultStudentEffect effect : ratingEffects) {
            if (effect.getEvaluationTotalScore() != null && effect.getEvaluationTotalScore().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal score = effect.getEvaluationTotalScore();
                if (effect.getEvaluationEffectType() == RatingResultStudentEffect.EFFECT_BONUS) {
                    bonusScore = bonusScore.add(score);
                } else if (effect.getEvaluationEffectType() == RatingResultStudentEffect.EFFECT_DEDUCT) {
                    deductScore = deductScore.add(score.abs());
                }

                StudentEvaluationDetail detail = new StudentEvaluationDetail();
                detail.setDetailType(StudentEvaluationDetail.TYPE_RATING);
                detail.setEvaluationDimension(EvaluationDimension.MORAL);
                detail.setScoreCategory(effect.getEvaluationEffectType() == RatingResultStudentEffect.EFFECT_BONUS ?
                        StudentEvaluationDetail.CATEGORY_BONUS : StudentEvaluationDetail.CATEGORY_DEDUCT);
                detail.setSourceType(StudentEvaluationDetail.SOURCE_RATING_RESULT);
                detail.setSourceId(effect.getRatingResultId());
                detail.setSourceName(effect.getLevelName());
                detail.setSourceDate(effect.getCheckDate());
                detail.setScore(effect.getEvaluationEffectType() == RatingResultStudentEffect.EFFECT_BONUS ? score : score.negate());
                details.add(detail);
            }
        }

        // 3. 获取荣誉加分(德育相关)
        List<StudentHonorApplication> honors = getApprovedHonorsForDimension(studentId, periodId, EvaluationDimension.MORAL);
        for (StudentHonorApplication honor : honors) {
            if (honor.getActualScore() != null && honor.getActualScore().compareTo(BigDecimal.ZERO) > 0) {
                bonusScore = bonusScore.add(honor.getActualScore());

                StudentEvaluationDetail detail = new StudentEvaluationDetail();
                detail.setDetailType(StudentEvaluationDetail.TYPE_HONOR);
                detail.setEvaluationDimension(EvaluationDimension.MORAL);
                detail.setScoreCategory(StudentEvaluationDetail.CATEGORY_BONUS);
                detail.setSourceType(StudentEvaluationDetail.SOURCE_HONOR_APPLICATION);
                detail.setSourceId(honor.getId());
                detail.setSourceCode(honor.getApplicationCode());
                detail.setSourceName(honor.getHonorName());
                detail.setSourceDate(honor.getHonorDate());
                detail.setScore(honor.getActualScore());
                details.add(detail);
            }
        }

        // 4. 奖励分封顶40分
        BigDecimal maxBonus = new BigDecimal("40");
        if (bonusScore.compareTo(maxBonus) > 0) {
            bonusScore = maxBonus;
        }

        // 5. 计算总分
        BigDecimal totalScore = baseScore.add(bonusScore).subtract(deductScore);
        totalScore = totalScore.max(BigDecimal.ZERO).min(new BigDecimal("100"));

        // 6. 计算加权分
        BigDecimal weight = dimensionWeights.getOrDefault(EvaluationDimension.MORAL, new BigDecimal("0.25"));
        BigDecimal weightedScore = totalScore.multiply(weight).setScale(2, RoundingMode.HALF_UP);

        result.put("baseScore", baseScore);
        result.put("bonusScore", bonusScore);
        result.put("deductScore", deductScore);
        result.put("totalScore", totalScore);
        result.put("weightedScore", weightedScore);
        result.put("details", details);

        return result;
    }

    @Override
    public Map<String, Object> calculateIntellectualScore(Long periodId, Long studentId) {
        Map<String, Object> result = new HashMap<>();
        List<StudentEvaluationDetail> details = new ArrayList<>();

        EvaluationPeriod period = periodMapper.selectById(periodId);

        // 从成绩汇总获取智育基础分
        Map<String, Object> scoreSummary = scoreMapper.calculateSemesterSummary(studentId, period.getSemesterId());
        BigDecimal baseScore = new BigDecimal("60");
        BigDecimal bonusScore = BigDecimal.ZERO;
        BigDecimal deductScore = BigDecimal.ZERO;

        if (scoreSummary != null && scoreSummary.get("weightedAverageScore") != null) {
            BigDecimal avgScore = new BigDecimal(scoreSummary.get("weightedAverageScore").toString());
            // 将平均分转换为智育基础分（按比例映射）
            baseScore = convertAcademicScoreToBase(avgScore);

            StudentEvaluationDetail detail = new StudentEvaluationDetail();
            detail.setDetailType(StudentEvaluationDetail.TYPE_SCORE);
            detail.setEvaluationDimension(EvaluationDimension.INTELLECTUAL);
            detail.setScoreCategory(StudentEvaluationDetail.CATEGORY_BASE);
            detail.setSourceType(StudentEvaluationDetail.SOURCE_ACADEMIC_SCORE);
            detail.setSourceName("学业成绩加权平均分: " + avgScore);
            detail.setScore(baseScore);
            details.add(detail);
        }

        // 获取智育相关荣誉加分
        List<StudentHonorApplication> honors = getApprovedHonorsForDimension(studentId, periodId, EvaluationDimension.INTELLECTUAL);
        for (StudentHonorApplication honor : honors) {
            if (honor.getActualScore() != null && honor.getActualScore().compareTo(BigDecimal.ZERO) > 0) {
                bonusScore = bonusScore.add(honor.getActualScore());

                StudentEvaluationDetail detail = new StudentEvaluationDetail();
                detail.setDetailType(StudentEvaluationDetail.TYPE_HONOR);
                detail.setEvaluationDimension(EvaluationDimension.INTELLECTUAL);
                detail.setScoreCategory(StudentEvaluationDetail.CATEGORY_BONUS);
                detail.setSourceType(StudentEvaluationDetail.SOURCE_HONOR_APPLICATION);
                detail.setSourceId(honor.getId());
                detail.setSourceCode(honor.getApplicationCode());
                detail.setSourceName(honor.getHonorName());
                detail.setSourceDate(honor.getHonorDate());
                detail.setScore(honor.getActualScore());
                details.add(detail);
            }
        }

        // 奖励分封顶
        BigDecimal maxBonus = new BigDecimal("40");
        if (bonusScore.compareTo(maxBonus) > 0) {
            bonusScore = maxBonus;
        }

        BigDecimal totalScore = baseScore.add(bonusScore).subtract(deductScore);
        totalScore = totalScore.max(BigDecimal.ZERO).min(new BigDecimal("100"));

        BigDecimal weight = dimensionWeights.getOrDefault(EvaluationDimension.INTELLECTUAL, new BigDecimal("0.40"));
        BigDecimal weightedScore = totalScore.multiply(weight).setScale(2, RoundingMode.HALF_UP);

        result.put("baseScore", baseScore);
        result.put("bonusScore", bonusScore);
        result.put("deductScore", deductScore);
        result.put("totalScore", totalScore);
        result.put("weightedScore", weightedScore);
        result.put("details", details);

        return result;
    }

    @Override
    public Map<String, Object> calculatePhysicalScore(Long periodId, Long studentId) {
        return calculateGenericDimensionScore(periodId, studentId, EvaluationDimension.PHYSICAL, new BigDecimal("0.10"));
    }

    @Override
    public Map<String, Object> calculateAestheticScore(Long periodId, Long studentId) {
        return calculateGenericDimensionScore(periodId, studentId, EvaluationDimension.AESTHETIC, new BigDecimal("0.10"));
    }

    @Override
    public Map<String, Object> calculateLaborScore(Long periodId, Long studentId) {
        Map<String, Object> result = new HashMap<>();
        List<StudentEvaluationDetail> details = new ArrayList<>();
        BigDecimal baseScore = new BigDecimal("60");
        BigDecimal bonusScore = BigDecimal.ZERO;
        BigDecimal deductScore = BigDecimal.ZERO;

        EvaluationPeriod period = periodMapper.selectById(periodId);

        // 获取卫生相关的量化扣分
        List<CheckRecordItemStudent> checkItems = checkItemStudentMapper.selectByStudentAndDateRange(
                studentId, period.getDataStartDate(), period.getDataEndDate());

        for (CheckRecordItemStudent item : checkItems) {
            if (item.getConfirmed() != CheckRecordItemStudent.CONFIRMED_YES) continue;
            if (item.getLaborScore() != null && item.getLaborScore().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal score = item.getLaborScore();
                if (score.compareTo(BigDecimal.ZERO) < 0) {
                    deductScore = deductScore.add(score.abs());
                } else {
                    bonusScore = bonusScore.add(score);
                }

                StudentEvaluationDetail detail = new StudentEvaluationDetail();
                detail.setDetailType(StudentEvaluationDetail.TYPE_QUANTIFICATION);
                detail.setEvaluationDimension(EvaluationDimension.LABOR);
                detail.setScoreCategory(score.compareTo(BigDecimal.ZERO) < 0 ?
                        StudentEvaluationDetail.CATEGORY_DEDUCT : StudentEvaluationDetail.CATEGORY_BONUS);
                detail.setSourceType(StudentEvaluationDetail.SOURCE_CHECK_ITEM);
                detail.setSourceId(item.getRecordItemId());
                detail.setSourceName(item.getBehaviorTypeName());
                detail.setSourceDate(item.getCheckDate());
                detail.setScore(score);
                details.add(detail);
            }
        }

        // 获取劳育相关荣誉(如志愿服务)
        List<StudentHonorApplication> honors = getApprovedHonorsForDimension(studentId, periodId, EvaluationDimension.LABOR);
        for (StudentHonorApplication honor : honors) {
            if (honor.getActualScore() != null && honor.getActualScore().compareTo(BigDecimal.ZERO) > 0) {
                bonusScore = bonusScore.add(honor.getActualScore());

                StudentEvaluationDetail detail = new StudentEvaluationDetail();
                detail.setDetailType(StudentEvaluationDetail.TYPE_HONOR);
                detail.setEvaluationDimension(EvaluationDimension.LABOR);
                detail.setScoreCategory(StudentEvaluationDetail.CATEGORY_BONUS);
                detail.setSourceType(StudentEvaluationDetail.SOURCE_HONOR_APPLICATION);
                detail.setSourceId(honor.getId());
                detail.setSourceName(honor.getHonorName());
                detail.setScore(honor.getActualScore());
                details.add(detail);
            }
        }

        // 封顶处理
        BigDecimal maxBonus = new BigDecimal("40");
        if (bonusScore.compareTo(maxBonus) > 0) {
            bonusScore = maxBonus;
        }

        BigDecimal totalScore = baseScore.add(bonusScore).subtract(deductScore);
        totalScore = totalScore.max(BigDecimal.ZERO).min(new BigDecimal("100"));

        BigDecimal weight = dimensionWeights.getOrDefault(EvaluationDimension.LABOR, new BigDecimal("0.10"));
        BigDecimal weightedScore = totalScore.multiply(weight).setScale(2, RoundingMode.HALF_UP);

        result.put("baseScore", baseScore);
        result.put("bonusScore", bonusScore);
        result.put("deductScore", deductScore);
        result.put("totalScore", totalScore);
        result.put("weightedScore", weightedScore);
        result.put("details", details);

        return result;
    }

    @Override
    public Map<String, Object> calculateDevelopmentScore(Long periodId, Long studentId) {
        return calculateGenericDimensionScore(periodId, studentId, EvaluationDimension.DEVELOPMENT, new BigDecimal("0.05"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateRankings(Long periodId) {
        log.info("开始计算排名: periodId={}", periodId);

        // 获取该周期所有综测结果，按班级分组
        LambdaQueryWrapper<StudentEvaluationResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentEvaluationResult::getEvaluationPeriodId, periodId);
        List<StudentEvaluationResult> allResults = resultMapper.selectList(wrapper);

        if (allResults.isEmpty()) {
            log.warn("无综测结果可排名: periodId={}", periodId);
            return;
        }

        // 按班级分组
        Map<Long, List<StudentEvaluationResult>> classResultsMap = allResults.stream()
                .filter(r -> r.getClassId() != null)
                .collect(Collectors.groupingBy(StudentEvaluationResult::getClassId));

        // 计算每个班级的排名
        for (Map.Entry<Long, List<StudentEvaluationResult>> entry : classResultsMap.entrySet()) {
            Long classId = entry.getKey();
            resultMapper.calculateClassRank(periodId, classId);
            log.debug("班级排名计算完成: classId={}", classId);
        }

        // 按年级分组计算年级排名
        Map<Long, List<StudentEvaluationResult>> gradeResultsMap = allResults.stream()
                .filter(r -> r.getGradeId() != null)
                .collect(Collectors.groupingBy(StudentEvaluationResult::getGradeId));

        for (Long gradeId : gradeResultsMap.keySet()) {
            resultMapper.calculateGradeRank(periodId, gradeId);
            log.debug("年级排名计算完成: gradeId={}", gradeId);
        }

        log.info("排名计算完成: periodId={}, 班级数={}, 年级数={}",
                periodId, classResultsMap.size(), gradeResultsMap.size());
    }

    @Override
    public Map<String, Object> getPunishmentEffect(Long studentId, Long periodId) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasPunishment", false);

        EvaluationPeriod period = periodMapper.selectById(periodId);
        if (period == null) return result;

        // 查询学生在该周期内有效的处分
        LambdaQueryWrapper<StudentPunishment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentPunishment::getStudentId, studentId)
                .eq(StudentPunishment::getStatus, 1)
                .eq(StudentPunishment::getRevoked, 0)
                .le(StudentPunishment::getEffectiveDate, period.getDataEndDate());

        List<StudentPunishment> punishments = punishmentMapper.selectList(wrapper);
        if (punishments.isEmpty()) return result;

        // 取最严重的处分
        StudentPunishment severest = punishments.stream()
                .max(Comparator.comparing(StudentPunishment::getPunishmentType))
                .orElse(null);

        if (severest != null) {
            result.put("hasPunishment", true);
            result.put("punishmentType", severest.getPunishmentType());
            result.put("punishmentName", severest.getPunishmentName());
            result.put("moralScoreCap", severest.getMoralScoreCap() != null ?
                    severest.getMoralScoreCap() : severest.getDefaultMoralScoreCap());
            result.put("effect", "德育分数上限为" + result.get("moralScoreCap") + "分");
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncQuantificationData(Long periodId) {
        log.info("同步量化数据到综测: periodId={}", periodId);

        EvaluationPeriod period = periodMapper.selectById(periodId);
        if (period == null) {
            log.warn("综测周期不存在: periodId={}", periodId);
            return 0;
        }

        // 查询周期内的量化检查记录（学生维度）
        List<CheckRecordItemStudent> checkItems = checkItemStudentMapper.selectByDateRange(
                period.getDataStartDate(), period.getDataEndDate());

        if (checkItems.isEmpty()) {
            log.info("无量化数据需要同步: periodId={}", periodId);
            return 0;
        }

        // 按学生分组统计
        Map<Long, List<CheckRecordItemStudent>> studentItemsMap = checkItems.stream()
                .filter(item -> item.getStudentId() != null && item.getConfirmed() == CheckRecordItemStudent.CONFIRMED_YES)
                .collect(Collectors.groupingBy(CheckRecordItemStudent::getStudentId));

        int syncCount = 0;
        for (Map.Entry<Long, List<CheckRecordItemStudent>> entry : studentItemsMap.entrySet()) {
            Long studentId = entry.getKey();
            // 触发学生综测重新计算，会自动包含最新的量化数据
            try {
                calculateStudent(periodId, studentId);
                syncCount++;
            } catch (Exception e) {
                log.error("同步量化数据失败: studentId={}", studentId, e);
            }
        }

        log.info("量化数据同步完成: periodId={}, 同步学生数={}", periodId, syncCount);
        return syncCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncHonorData(Long periodId) {
        log.info("同步荣誉数据到综测: periodId={}", periodId);

        // 查询周期内已审批通过的荣誉申报
        LambdaQueryWrapper<StudentHonorApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentHonorApplication::getEvaluationPeriodId, periodId)
               .eq(StudentHonorApplication::getStatus, StudentHonorApplication.STATUS_APPROVED);
        List<StudentHonorApplication> honors = honorMapper.selectList(wrapper);

        if (honors.isEmpty()) {
            log.info("无荣誉数据需要同步: periodId={}", periodId);
            return 0;
        }

        // 获取需要更新的学生列表
        Set<Long> studentIds = honors.stream()
                .map(StudentHonorApplication::getStudentId)
                .collect(Collectors.toSet());

        int syncCount = 0;
        for (Long studentId : studentIds) {
            // 触发学生综测重新计算，会自动包含最新的荣誉数据
            try {
                calculateStudent(periodId, studentId);
                syncCount++;
            } catch (Exception e) {
                log.error("同步荣誉数据失败: studentId={}", studentId, e);
            }
        }

        log.info("荣誉数据同步完成: periodId={}, 同步学生数={}", periodId, syncCount);
        return syncCount;
    }

    // ==================== 私有方法 ====================

    private void loadDimensionWeights() {
        if (dimensionWeights == null) {
            dimensionWeights = new HashMap<>();
            List<EvaluationDimension> dimensions = dimensionMapper.selectAllEnabled();
            for (EvaluationDimension dim : dimensions) {
                dimensionWeights.put(dim.getDimensionCode(), dim.getWeight().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            }
        }
    }

    private StudentEvaluationResult createNewResult(Long periodId, Long studentId, EvaluationPeriod period) {
        StudentEvaluationResult result = new StudentEvaluationResult();
        result.setEvaluationPeriodId(periodId);
        result.setSemesterId(period.getSemesterId());
        result.setStudentId(studentId);

        // 填充学生信息快照
        Student student = studentMapper.selectById(studentId);
        if (student != null) {
            result.setStudentNo(student.getStudentNo());
            result.setStudentName(student.getRealName());
            result.setClassId(student.getClassId());
            result.setGradeId(student.getGradeId());

            // 获取班级名称和部门ID
            if (student.getClassId() != null) {
                com.school.management.entity.Class clazz = classMapper.selectById(student.getClassId());
                if (clazz != null) {
                    result.setClassName(clazz.getClassName());
                    result.setDepartmentId(clazz.getDepartmentId());
                }
            }
        }

        return result;
    }

    private void applyDimensionScore(StudentEvaluationResult result, String dimension, Map<String, Object> scoreResult) {
        BigDecimal baseScore = (BigDecimal) scoreResult.get("baseScore");
        BigDecimal bonusScore = (BigDecimal) scoreResult.get("bonusScore");
        BigDecimal deductScore = (BigDecimal) scoreResult.get("deductScore");
        BigDecimal totalScore = (BigDecimal) scoreResult.get("totalScore");
        BigDecimal weightedScore = (BigDecimal) scoreResult.get("weightedScore");

        switch (dimension) {
            case "MORAL":
                result.setMoralBaseScore(baseScore);
                result.setMoralBonusScore(bonusScore);
                result.setMoralDeductScore(deductScore);
                result.setMoralTotalScore(totalScore);
                result.setMoralWeightedScore(weightedScore);
                break;
            case "INTELLECTUAL":
                result.setIntellectualBaseScore(baseScore);
                result.setIntellectualBonusScore(bonusScore);
                result.setIntellectualDeductScore(deductScore);
                result.setIntellectualTotalScore(totalScore);
                result.setIntellectualWeightedScore(weightedScore);
                break;
            case "PHYSICAL":
                result.setPhysicalBaseScore(baseScore);
                result.setPhysicalBonusScore(bonusScore);
                result.setPhysicalDeductScore(deductScore);
                result.setPhysicalTotalScore(totalScore);
                result.setPhysicalWeightedScore(weightedScore);
                break;
            case "AESTHETIC":
                result.setAestheticBaseScore(baseScore);
                result.setAestheticBonusScore(bonusScore);
                result.setAestheticDeductScore(deductScore);
                result.setAestheticTotalScore(totalScore);
                result.setAestheticWeightedScore(weightedScore);
                break;
            case "LABOR":
                result.setLaborBaseScore(baseScore);
                result.setLaborBonusScore(bonusScore);
                result.setLaborDeductScore(deductScore);
                result.setLaborTotalScore(totalScore);
                result.setLaborWeightedScore(weightedScore);
                break;
            case "DEVELOPMENT":
                result.setDevelopmentBaseScore(baseScore);
                result.setDevelopmentBonusScore(bonusScore);
                result.setDevelopmentDeductScore(deductScore);
                result.setDevelopmentTotalScore(totalScore);
                result.setDevelopmentWeightedScore(weightedScore);
                break;
        }
    }

    private void applyPunishmentEffect(StudentEvaluationResult result, Map<String, Object> punishmentEffect) {
        boolean hasPunishment = (boolean) punishmentEffect.get("hasPunishment");
        result.setHasPunishment(hasPunishment ? 1 : 0);

        if (hasPunishment) {
            result.setPunishmentType((Integer) punishmentEffect.get("punishmentType"));
            result.setPunishmentEffect((String) punishmentEffect.get("effect"));

            // 应用德育分数上限
            BigDecimal cap = (BigDecimal) punishmentEffect.get("moralScoreCap");
            if (cap != null && result.getMoralTotalScore() != null && result.getMoralTotalScore().compareTo(cap) > 0) {
                result.setMoralTotalScore(cap);
                // 重新计算加权分
                BigDecimal weight = dimensionWeights.getOrDefault(EvaluationDimension.MORAL, new BigDecimal("0.25"));
                result.setMoralWeightedScore(cap.multiply(weight).setScale(2, RoundingMode.HALF_UP));
            }
        }
    }

    private BigDecimal convertAcademicScoreToBase(BigDecimal avgScore) {
        // 将学业平均分转换为智育基础分
        // 例如: 90分以上 -> 100分基础, 60分 -> 60分基础, 线性映射
        if (avgScore.compareTo(new BigDecimal("90")) >= 0) {
            return new BigDecimal("100");
        } else if (avgScore.compareTo(new BigDecimal("60")) >= 0) {
            // 60-90 映射到 60-100
            return avgScore.subtract(new BigDecimal("60"))
                    .multiply(new BigDecimal("40"))
                    .divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP)
                    .add(new BigDecimal("60"));
        } else {
            // 低于60分按比例计算
            return avgScore;
        }
    }

    private Map<String, Object> calculateGenericDimensionScore(Long periodId, Long studentId, String dimension, BigDecimal weight) {
        Map<String, Object> result = new HashMap<>();
        List<StudentEvaluationDetail> details = new ArrayList<>();
        BigDecimal baseScore = new BigDecimal("60");
        BigDecimal bonusScore = BigDecimal.ZERO;
        BigDecimal deductScore = BigDecimal.ZERO;

        // 获取该维度的荣誉加分
        List<StudentHonorApplication> honors = getApprovedHonorsForDimension(studentId, periodId, dimension);
        for (StudentHonorApplication honor : honors) {
            if (honor.getActualScore() != null && honor.getActualScore().compareTo(BigDecimal.ZERO) > 0) {
                bonusScore = bonusScore.add(honor.getActualScore());

                StudentEvaluationDetail detail = new StudentEvaluationDetail();
                detail.setDetailType(StudentEvaluationDetail.TYPE_HONOR);
                detail.setEvaluationDimension(dimension);
                detail.setScoreCategory(StudentEvaluationDetail.CATEGORY_BONUS);
                detail.setSourceType(StudentEvaluationDetail.SOURCE_HONOR_APPLICATION);
                detail.setSourceId(honor.getId());
                detail.setSourceName(honor.getHonorName());
                detail.setScore(honor.getActualScore());
                details.add(detail);
            }
        }

        // 封顶处理
        BigDecimal maxBonus = new BigDecimal("40");
        if (bonusScore.compareTo(maxBonus) > 0) {
            bonusScore = maxBonus;
        }

        BigDecimal totalScore = baseScore.add(bonusScore).subtract(deductScore);
        totalScore = totalScore.max(BigDecimal.ZERO).min(new BigDecimal("100"));

        BigDecimal actualWeight = dimensionWeights.getOrDefault(dimension, weight);
        BigDecimal weightedScore = totalScore.multiply(actualWeight).setScale(2, RoundingMode.HALF_UP);

        result.put("baseScore", baseScore);
        result.put("bonusScore", bonusScore);
        result.put("deductScore", deductScore);
        result.put("totalScore", totalScore);
        result.put("weightedScore", weightedScore);
        result.put("details", details);

        return result;
    }

    private List<RatingResultStudentEffect> getRatingEffectsForDimension(Long studentId, java.time.LocalDate startDate,
                                                                          java.time.LocalDate endDate, String dimension) {
        // 查询学生在指定时间范围内、指定维度的评级影响记录
        LambdaQueryWrapper<RatingResultStudentEffect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RatingResultStudentEffect::getStudentId, studentId)
               .eq(RatingResultStudentEffect::getEvaluationDimension, dimension)
               .ge(RatingResultStudentEffect::getCheckDate, startDate)
               .le(RatingResultStudentEffect::getCheckDate, endDate);
        return ratingEffectMapper.selectList(wrapper);
    }

    private List<StudentHonorApplication> getApprovedHonorsForDimension(Long studentId, Long periodId, String dimension) {
        LambdaQueryWrapper<StudentHonorApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentHonorApplication::getStudentId, studentId)
                .eq(StudentHonorApplication::getEvaluationPeriodId, periodId)
                .eq(StudentHonorApplication::getStatus, StudentHonorApplication.STATUS_APPROVED)
                .eq(StudentHonorApplication::getEvaluationDimension, dimension)
                .eq(StudentHonorApplication::getIsHighestLevel, 1);
        return honorMapper.selectList(wrapper);
    }
}
