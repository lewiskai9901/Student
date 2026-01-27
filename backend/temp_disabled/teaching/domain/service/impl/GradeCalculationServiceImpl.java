package com.school.management.domain.teaching.service.impl;

import com.school.management.domain.teaching.model.aggregate.StudentGrade;
import com.school.management.domain.teaching.model.entity.GradeItem;
import com.school.management.domain.teaching.model.valueobject.GradeScale;
import com.school.management.domain.teaching.model.valueobject.GradeWeight;
import com.school.management.domain.teaching.service.GradeCalculationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 成绩计算领域服务实现
 */
@Slf4j
@Service
public class GradeCalculationServiceImpl implements GradeCalculationService {

    private static final BigDecimal DEFAULT_PASS_LINE = new BigDecimal("60");

    @Override
    public void calculateTotalScore(StudentGrade grade, GradeWeight weight) {
        if (grade == null) return;

        BigDecimal total = BigDecimal.ZERO;

        if (weight == null) {
            // 使用默认权重：平时30%，期末70%
            if (grade.getRegularScore() != null) {
                total = total.add(grade.getRegularScore().multiply(new BigDecimal("0.30")));
            }
            if (grade.getFinalScore() != null) {
                total = total.add(grade.getFinalScore().multiply(new BigDecimal("0.70")));
            }
        } else {
            if (grade.getRegularScore() != null && weight.getRegularWeight() != null) {
                total = total.add(grade.getRegularScore().multiply(weight.getRegularWeight()));
            }
            if (grade.getMidtermScore() != null && weight.getMidtermWeight() != null) {
                total = total.add(grade.getMidtermScore().multiply(weight.getMidtermWeight()));
            }
            if (grade.getFinalScore() != null && weight.getFinalWeight() != null) {
                total = total.add(grade.getFinalScore().multiply(weight.getFinalWeight()));
            }
            if (grade.getExperimentScore() != null && weight.getExperimentWeight() != null) {
                total = total.add(grade.getExperimentScore().multiply(weight.getExperimentWeight()));
            }
        }

        grade.setTotalScore(total.setScale(1, RoundingMode.HALF_UP));
        updateGradeInfo(grade);
    }

    @Override
    public void calculateFromItems(StudentGrade grade, List<GradeItem> items) {
        if (grade == null || items == null || items.isEmpty()) return;

        BigDecimal total = BigDecimal.ZERO;
        for (GradeItem item : items) {
            if (item.getWeightedScore() != null) {
                total = total.add(item.getWeightedScore());
            }
        }

        grade.setTotalScore(total.setScale(1, RoundingMode.HALF_UP));
        updateGradeInfo(grade);
    }

    @Override
    public GradeScale convertToGradeScale(BigDecimal score, Integer scaleType) {
        return GradeScale.fromScore(score, scaleType != null ? scaleType : 1);
    }

    @Override
    public BigDecimal calculateGradePoint(BigDecimal score, Integer scaleType) {
        GradeScale scale = convertToGradeScale(score, scaleType);
        return scale.getGradePoint();
    }

    @Override
    public BigDecimal calculateGPA(List<StudentGrade> grades) {
        if (grades == null || grades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalPoints = BigDecimal.ZERO;
        int count = 0;

        for (StudentGrade grade : grades) {
            if (grade.getGradePoint() != null) {
                totalPoints = totalPoints.add(grade.getGradePoint());
                count++;
            }
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return totalPoints.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateWeightedAverage(List<StudentGrade> grades) {
        if (grades == null || grades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        int count = 0;

        for (StudentGrade grade : grades) {
            if (grade.getTotalScore() != null) {
                totalScore = totalScore.add(grade.getTotalScore());
                count++;
            }
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return totalScore.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean checkPassed(BigDecimal score, BigDecimal passLine) {
        if (score == null) return false;
        BigDecimal line = passLine != null ? passLine : DEFAULT_PASS_LINE;
        return score.compareTo(line) >= 0;
    }

    @Override
    public void updateGradeScales(List<StudentGrade> grades) {
        if (grades == null) return;
        for (StudentGrade grade : grades) {
            updateGradeInfo(grade);
        }
    }

    @Override
    public ClassGradeStatistics calculateClassStatistics(List<StudentGrade> grades) {
        if (grades == null || grades.isEmpty()) {
            return new ClassGradeStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        int totalCount = grades.size();
        int passedCount = 0;
        int excellentCount = 0;
        int goodCount = 0;
        int mediumCount = 0;
        int passCount = 0;
        int failCount = 0;

        double sum = 0;
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        List<Double> scores = new ArrayList<>();

        for (StudentGrade grade : grades) {
            if (grade.getTotalScore() != null) {
                double score = grade.getTotalScore().doubleValue();
                scores.add(score);
                sum += score;
                max = Math.max(max, score);
                min = Math.min(min, score);

                if (score >= 90) excellentCount++;
                else if (score >= 80) goodCount++;
                else if (score >= 70) mediumCount++;
                else if (score >= 60) passCount++;
                else failCount++;

                if (score >= 60) passedCount++;
            }
        }

        double average = scores.isEmpty() ? 0 : sum / scores.size();
        double passRate = totalCount > 0 ? (double) passedCount / totalCount * 100 : 0;

        // 计算标准差
        double variance = 0;
        for (Double score : scores) {
            variance += Math.pow(score - average, 2);
        }
        double stdDev = scores.isEmpty() ? 0 : Math.sqrt(variance / scores.size());

        return new ClassGradeStatistics(
                totalCount,
                passedCount,
                passRate,
                average,
                scores.isEmpty() ? 0 : max,
                scores.isEmpty() ? 0 : min,
                stdDev,
                excellentCount,
                goodCount,
                mediumCount,
                passCount,
                failCount
        );
    }

    private void updateGradeInfo(StudentGrade grade) {
        if (grade == null || grade.getTotalScore() == null) return;

        GradeScale scale = convertToGradeScale(grade.getTotalScore(), grade.getGradeScaleType());
        grade.setGradeLevel(scale.getLevel());
        grade.setGradePoint(scale.getGradePoint());
        grade.setPassed(scale.isPassed());
    }
}
