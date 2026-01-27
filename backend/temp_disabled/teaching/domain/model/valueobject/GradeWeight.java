package com.school.management.domain.teaching.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 成绩权重配置值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeWeight {

    /**
     * 平时成绩权重
     */
    @Builder.Default
    private BigDecimal regularWeight = new BigDecimal("0.30");

    /**
     * 期中成绩权重
     */
    @Builder.Default
    private BigDecimal midtermWeight = BigDecimal.ZERO;

    /**
     * 期末成绩权重
     */
    @Builder.Default
    private BigDecimal finalWeight = new BigDecimal("0.70");

    /**
     * 实验成绩权重
     */
    @Builder.Default
    private BigDecimal experimentWeight = BigDecimal.ZERO;

    /**
     * 默认权重配置（平时30%，期末70%）
     */
    public static GradeWeight defaultWeight() {
        return GradeWeight.builder().build();
    }

    /**
     * 期中考试配置（平时20%，期中20%，期末60%）
     */
    public static GradeWeight withMidterm() {
        return GradeWeight.builder()
                .regularWeight(new BigDecimal("0.20"))
                .midtermWeight(new BigDecimal("0.20"))
                .finalWeight(new BigDecimal("0.60"))
                .build();
    }

    /**
     * 带实验配置（平时20%，实验20%，期末60%）
     */
    public static GradeWeight withExperiment() {
        return GradeWeight.builder()
                .regularWeight(new BigDecimal("0.20"))
                .experimentWeight(new BigDecimal("0.20"))
                .finalWeight(new BigDecimal("0.60"))
                .build();
    }

    /**
     * 完整配置（平时10%，期中20%，实验20%，期末50%）
     */
    public static GradeWeight full() {
        return GradeWeight.builder()
                .regularWeight(new BigDecimal("0.10"))
                .midtermWeight(new BigDecimal("0.20"))
                .experimentWeight(new BigDecimal("0.20"))
                .finalWeight(new BigDecimal("0.50"))
                .build();
    }

    /**
     * 验证权重总和是否为100%
     */
    public boolean isValid() {
        BigDecimal total = regularWeight
                .add(midtermWeight)
                .add(finalWeight)
                .add(experimentWeight);
        return total.compareTo(BigDecimal.ONE) == 0;
    }

    /**
     * 获取权重总和
     */
    public BigDecimal getTotalWeight() {
        return regularWeight
                .add(midtermWeight)
                .add(finalWeight)
                .add(experimentWeight);
    }

    /**
     * 计算总评成绩
     */
    public BigDecimal calculate(BigDecimal regular, BigDecimal midterm,
                                BigDecimal finalScore, BigDecimal experiment) {
        BigDecimal total = BigDecimal.ZERO;

        if (regular != null && regularWeight.compareTo(BigDecimal.ZERO) > 0) {
            total = total.add(regular.multiply(regularWeight));
        }
        if (midterm != null && midtermWeight.compareTo(BigDecimal.ZERO) > 0) {
            total = total.add(midterm.multiply(midtermWeight));
        }
        if (finalScore != null && finalWeight.compareTo(BigDecimal.ZERO) > 0) {
            total = total.add(finalScore.multiply(finalWeight));
        }
        if (experiment != null && experimentWeight.compareTo(BigDecimal.ZERO) > 0) {
            total = total.add(experiment.multiply(experimentWeight));
        }

        return total.setScale(1, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 获取描述
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        if (regularWeight.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("平时").append(regularWeight.multiply(BigDecimal.valueOf(100)).intValue()).append("%");
        }
        if (midtermWeight.compareTo(BigDecimal.ZERO) > 0) {
            if (!sb.isEmpty()) sb.append("+");
            sb.append("期中").append(midtermWeight.multiply(BigDecimal.valueOf(100)).intValue()).append("%");
        }
        if (experimentWeight.compareTo(BigDecimal.ZERO) > 0) {
            if (!sb.isEmpty()) sb.append("+");
            sb.append("实验").append(experimentWeight.multiply(BigDecimal.valueOf(100)).intValue()).append("%");
        }
        if (finalWeight.compareTo(BigDecimal.ZERO) > 0) {
            if (!sb.isEmpty()) sb.append("+");
            sb.append("期末").append(finalWeight.multiply(BigDecimal.valueOf(100)).intValue()).append("%");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GradeWeight that = (GradeWeight) o;
        return Objects.equals(regularWeight, that.regularWeight)
                && Objects.equals(midtermWeight, that.midtermWeight)
                && Objects.equals(finalWeight, that.finalWeight)
                && Objects.equals(experimentWeight, that.experimentWeight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(regularWeight, midtermWeight, finalWeight, experimentWeight);
    }
}
