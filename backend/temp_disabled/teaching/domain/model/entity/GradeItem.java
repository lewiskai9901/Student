package com.school.management.domain.teaching.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 成绩明细项实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeItem {

    private Long id;

    /**
     * 学生成绩ID
     */
    private Long gradeId;

    /**
     * 成绩组成配置ID
     */
    private Long compositionId;

    /**
     * 成绩项名称
     */
    private String itemName;

    /**
     * 得分
     */
    private BigDecimal score;

    /**
     * 满分
     */
    private BigDecimal fullScore;

    /**
     * 权重
     */
    private BigDecimal weight;

    /**
     * 加权得分
     */
    private BigDecimal weightedScore;

    /**
     * 备注
     */
    private String remark;

    /**
     * 录入时间
     */
    private LocalDateTime inputTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 计算加权得分
     */
    public void calculateWeightedScore() {
        if (score != null && weight != null) {
            this.weightedScore = score.multiply(weight)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * 获取得分率（百分比）
     */
    public BigDecimal getScoreRate() {
        if (score != null && fullScore != null && fullScore.compareTo(BigDecimal.ZERO) > 0) {
            return score.divide(fullScore, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }
        return null;
    }

    /**
     * 是否及格（默认60分及格）
     */
    public boolean isPassed() {
        return isPassed(BigDecimal.valueOf(60));
    }

    /**
     * 是否及格（自定义及格线）
     */
    public boolean isPassed(BigDecimal passLine) {
        if (score == null || fullScore == null) {
            return false;
        }
        BigDecimal rate = getScoreRate();
        return rate != null && rate.compareTo(passLine) >= 0;
    }

    /**
     * 录入成绩
     */
    public void recordScore(BigDecimal score) {
        this.score = score;
        this.inputTime = LocalDateTime.now();
        calculateWeightedScore();
    }
}
