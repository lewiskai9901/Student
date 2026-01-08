package com.school.management.service.evaluation.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.evaluation.Course;
import com.school.management.entity.evaluation.EvaluationPeriod;
import com.school.management.entity.evaluation.StudentScore;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.evaluation.CourseMapper;
import com.school.management.mapper.evaluation.EvaluationPeriodMapper;
import com.school.management.mapper.evaluation.StudentScoreMapper;
import com.school.management.service.evaluation.StudentScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 学生成绩服务实现类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentScoreServiceImpl extends ServiceImpl<StudentScoreMapper, StudentScore>
        implements StudentScoreService {

    private final StudentScoreMapper scoreMapper;
    private final CourseMapper courseMapper;
    private final EvaluationPeriodMapper periodMapper;

    @Override
    public Page<Map<String, Object>> pageScores(Page<?> page, Map<String, Object> query) {
        return scoreMapper.selectScorePage(page, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long inputScore(StudentScore score) {
        // 检查是否已录入
        if (existsScore(score.getStudentId(), score.getCourseId(), score.getSemesterId())) {
            throw new BusinessException("该学生该课程成绩已存在");
        }

        // 验证课程
        Course course = courseMapper.selectById(score.getCourseId());
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        // 设置课程信息
        score.setCredit(course.getCredit());

        // 计算绩点
        score.setGradePoint(calculateGPA(score.getFinalTotalScore()));

        // 设置默认状态
        if (score.getScoreStatus() == null) {
            score.setScoreStatus(StudentScore.STATUS_INPUT); // 已录入
        }

        scoreMapper.insert(score);
        log.info("录入成绩: studentId={}, courseId={}, score={}",
                score.getStudentId(), score.getCourseId(), score.getFinalScore());
        return score.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchInputScores(List<StudentScore> scores) {
        int count = 0;
        for (StudentScore score : scores) {
            try {
                if (!existsScore(score.getStudentId(), score.getCourseId(), score.getSemesterId())) {
                    inputScore(score);
                    count++;
                } else {
                    // 更新已存在的成绩
                    StudentScore existing = scoreMapper.selectByStudentAndCourse(
                            score.getStudentId(), score.getCourseId(), score.getSemesterId());
                    if (existing != null && existing.getScoreStatus() != StudentScore.STATUS_CONFIRMED) {
                        score.setId(existing.getId());
                        score.setGradePoint(calculateGPA(score.getFinalTotalScore()));
                        scoreMapper.updateById(score);
                        count++;
                    }
                }
            } catch (Exception e) {
                log.warn("录入成绩失败: studentId={}, courseId={}, error={}",
                        score.getStudentId(), score.getCourseId(), e.getMessage());
            }
        }
        log.info("批量录入成绩完成: total={}, success={}", scores.size(), count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScore(StudentScore score) {
        StudentScore existing = scoreMapper.selectById(score.getId());
        if (existing == null) {
            throw new BusinessException("成绩记录不存在");
        }

        // 检查是否已确认
        if (existing.getScoreStatus() == StudentScore.STATUS_CONFIRMED) {
            throw new BusinessException("成绩已确认，无法修改");
        }

        // 重新计算绩点
        if (score.getFinalTotalScore() != null) {
            score.setGradePoint(calculateGPA(score.getFinalTotalScore()));
        }

        scoreMapper.updateById(score);
        log.info("更新成绩: id={}", score.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScore(Long id) {
        StudentScore existing = scoreMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("成绩记录不存在");
        }

        if (existing.getScoreStatus() == StudentScore.STATUS_CONFIRMED) {
            throw new BusinessException("成绩已确认，无法删除");
        }

        scoreMapper.deleteById(id);
        log.info("删除成绩: id={}", id);
    }

    @Override
    public Map<String, Object> getScoreDetail(Long id) {
        Map<String, Object> detail = scoreMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException("成绩记录不存在");
        }
        return detail;
    }

    @Override
    public List<Map<String, Object>> getStudentScores(Long studentId, Long semesterId) {
        return scoreMapper.selectByStudentAndSemester(studentId, semesterId);
    }

    @Override
    public List<Map<String, Object>> getStudentScoresByPeriod(Long studentId, Long periodId) {
        // 获取周期对应的学期
        EvaluationPeriod period = periodMapper.selectById(periodId);
        if (period == null) {
            return Collections.emptyList();
        }
        return scoreMapper.selectByStudentAndSemester(studentId, period.getSemesterId());
    }

    @Override
    public BigDecimal calculateSemesterGPA(Long studentId, Long semesterId) {
        List<Map<String, Object>> scores = getStudentScores(studentId, semesterId);
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalPoints = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (Map<String, Object> score : scores) {
            BigDecimal credit = getBigDecimal(score.get("credit"));
            BigDecimal gpa = getBigDecimal(score.get("gpa"));

            if (credit != null && gpa != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                totalPoints = totalPoints.add(credit.multiply(gpa));
                totalCredits = totalCredits.add(credit);
            }
        }

        if (totalCredits.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalPoints.divide(totalCredits, 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateSemesterAverage(Long studentId, Long semesterId) {
        List<Map<String, Object>> scores = getStudentScores(studentId, semesterId);
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;

        for (Map<String, Object> score : scores) {
            BigDecimal finalScore = getBigDecimal(score.get("finalScore"));
            if (finalScore != null) {
                total = total.add(finalScore);
                count++;
            }
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateWeightedAverage(Long studentId, Long semesterId) {
        List<Map<String, Object>> scores = getStudentScores(studentId, semesterId);
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalWeighted = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (Map<String, Object> score : scores) {
            BigDecimal credit = getBigDecimal(score.get("credit"));
            BigDecimal finalScore = getBigDecimal(score.get("finalScore"));

            if (credit != null && finalScore != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                totalWeighted = totalWeighted.add(credit.multiply(finalScore));
                totalCredits = totalCredits.add(credit);
            }
        }

        if (totalCredits.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalWeighted.divide(totalCredits, 2, RoundingMode.HALF_UP);
    }

    @Override
    public Map<String, Object> getClassScoreStatistics(Long classId, Long courseId, Long semesterId) {
        return scoreMapper.selectClassStatistics(classId, courseId, semesterId);
    }

    @Override
    public List<Map<String, Object>> getCourseScoreRanking(Long courseId, Long semesterId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        return scoreMapper.selectCourseRanking(courseId, semesterId, limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importScores(List<StudentScore> scores) {
        return batchInputScores(scores);
    }

    @Override
    public List<Map<String, Object>> exportClassScores(Long classId, Long semesterId) {
        return scoreMapper.selectForExport(classId, semesterId);
    }

    @Override
    public boolean existsScore(Long studentId, Long courseId, Long semesterId) {
        StudentScore existing = scoreMapper.selectByStudentAndCourse(studentId, courseId, semesterId);
        return existing != null;
    }

    @Override
    public Map<String, Object> calculateIntellectualScoreForEvaluation(Long studentId, Long periodId) {
        Map<String, Object> result = new HashMap<>();

        // 获取周期对应的学期
        EvaluationPeriod period = periodMapper.selectById(periodId);
        if (period == null) {
            result.put("academicScore", BigDecimal.ZERO);
            result.put("gpa", BigDecimal.ZERO);
            result.put("weightedAverage", BigDecimal.ZERO);
            result.put("convertedScore", BigDecimal.ZERO);
            return result;
        }

        // 计算加权平均分
        BigDecimal weightedAverage = calculateWeightedAverage(studentId, period.getSemesterId());
        BigDecimal gpa = calculateSemesterGPA(studentId, period.getSemesterId());

        // 将加权平均分转换为100分制的综测智育分数
        // 转换公式: 智育分数 = 加权平均分（假设已经是百分制）
        BigDecimal convertedScore = weightedAverage;

        // 如果使用GPA转换，可以使用以下公式：
        // 智育分数 = (GPA / 4.0) * 100
        // BigDecimal convertedScore = gpa.divide(new BigDecimal("4.0"), 4, RoundingMode.HALF_UP)
        //         .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);

        result.put("academicScore", weightedAverage);
        result.put("gpa", gpa);
        result.put("weightedAverage", weightedAverage);
        result.put("convertedScore", convertedScore);
        result.put("courseCount", scoreMapper.countByStudentAndSemester(studentId, period.getSemesterId()));

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockScores(Long semesterId, Long courseId) {
        scoreMapper.lockBySemesterAndCourse(semesterId, courseId);
        log.info("锁定成绩: semesterId={}, courseId={}", semesterId, courseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockScores(Long semesterId, Long courseId) {
        scoreMapper.unlockBySemesterAndCourse(semesterId, courseId);
        log.info("解锁成绩: semesterId={}, courseId={}", semesterId, courseId);
    }

    /**
     * 计算GPA（4.0制）
     * 90-100: 4.0
     * 85-89: 3.7
     * 82-84: 3.3
     * 78-81: 3.0
     * 75-77: 2.7
     * 72-74: 2.3
     * 68-71: 2.0
     * 64-67: 1.5
     * 60-63: 1.0
     * <60: 0
     */
    private BigDecimal calculateGPA(BigDecimal score) {
        if (score == null) {
            return BigDecimal.ZERO;
        }

        double s = score.doubleValue();
        if (s >= 90) return new BigDecimal("4.0");
        if (s >= 85) return new BigDecimal("3.7");
        if (s >= 82) return new BigDecimal("3.3");
        if (s >= 78) return new BigDecimal("3.0");
        if (s >= 75) return new BigDecimal("2.7");
        if (s >= 72) return new BigDecimal("2.3");
        if (s >= 68) return new BigDecimal("2.0");
        if (s >= 64) return new BigDecimal("1.5");
        if (s >= 60) return new BigDecimal("1.0");
        return BigDecimal.ZERO;
    }

    /**
     * 安全获取BigDecimal
     */
    private BigDecimal getBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return null;
    }
}
