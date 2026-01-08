package com.school.management.dto.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 排名变化VO (申诉生效后的排名变化)
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class RankingChangeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 原始总分
     */
    private BigDecimal originalTotalScore;

    /**
     * 调整后总分
     */
    private BigDecimal adjustedTotalScore;

    /**
     * 分数变化
     */
    private BigDecimal scoreChange;

    /**
     * 原始排名(本年级)
     */
    private Integer originalGradeRank;

    /**
     * 调整后排名(本年级)
     */
    private Integer adjustedGradeRank;

    /**
     * 排名变化(本年级)
     */
    private Integer gradeRankChange;

    /**
     * 原始排名(全校)
     */
    private Integer originalSchoolRank;

    /**
     * 调整后排名(全校)
     */
    private Integer adjustedSchoolRank;

    /**
     * 排名变化(全校)
     */
    private Integer schoolRankChange;

    /**
     * 原始等级
     */
    private String originalRating;

    /**
     * 调整后等级
     */
    private String adjustedRating;

    /**
     * 等级是否变化
     */
    private Boolean ratingChanged;

    /**
     * 是否影响评优资格
     */
    private Boolean affectsEvaluation;

    /**
     * 受影响的其他班级数量
     */
    private Integer affectedClassCount;

    /**
     * 受影响的班级列表
     */
    private java.util.List<AffectedClassDTO> affectedClasses;

    /**
     * 获取年级排名变化描述
     */
    public String getGradeRankChangeDesc() {
        if (gradeRankChange == null || gradeRankChange == 0) {
            return "排名不变";
        } else if (gradeRankChange > 0) {
            return String.format("上升%d名", gradeRankChange);
        } else {
            return String.format("下降%d名", Math.abs(gradeRankChange));
        }
    }

    /**
     * 获取全校排名变化描述
     */
    public String getSchoolRankChangeDesc() {
        if (schoolRankChange == null || schoolRankChange == 0) {
            return "排名不变";
        } else if (schoolRankChange > 0) {
            return String.format("上升%d名", schoolRankChange);
        } else {
            return String.format("下降%d名", Math.abs(schoolRankChange));
        }
    }

    /**
     * 获取等级变化描述
     */
    public String getRatingChangeDesc() {
        if (!ratingChanged) {
            return "等级不变";
        }
        return String.format("从%s变为%s", originalRating, adjustedRating);
    }

    /**
     * 是否有利变化
     */
    public Boolean isFavorableChange() {
        if (gradeRankChange == null) {
            return null;
        }
        // 排名变化为正表示上升(有利)
        return gradeRankChange > 0;
    }

    // ========== 别名方法(兼容性) ==========

    /**
     * 设置旧分数(别名)
     */
    public void setOldScore(BigDecimal oldScore) {
        this.originalTotalScore = oldScore;
    }

    /**
     * 获取旧分数(别名)
     */
    public BigDecimal getOldScore() {
        return this.originalTotalScore;
    }

    /**
     * 设置新分数(别名)
     */
    public void setNewScore(BigDecimal newScore) {
        this.adjustedTotalScore = newScore;
    }

    /**
     * 获取新分数(别名)
     */
    public BigDecimal getNewScore() {
        return this.adjustedTotalScore;
    }

    /**
     * 设置旧排名(别名)
     */
    public void setOldRank(Integer oldRank) {
        this.originalGradeRank = oldRank;
    }

    /**
     * 获取旧排名(别名)
     */
    public Integer getOldRank() {
        return this.originalGradeRank;
    }

    /**
     * 设置新排名(别名)
     */
    public void setNewRank(Integer newRank) {
        this.adjustedGradeRank = newRank;
    }

    /**
     * 获取新排名(别名)
     */
    public Integer getNewRank() {
        return this.adjustedGradeRank;
    }

    /**
     * 受影响班级DTO
     */
    @Data
    public static class AffectedClassDTO implements Serializable {
        private Long classId;
        private String className;
        private Integer originalRank;
        private Integer adjustedRank;
        private Integer rankChange;
        private String changeDesc;

        public String getChangeDesc() {
            if (rankChange == null || rankChange == 0) {
                return "排名不变";
            } else if (rankChange > 0) {
                return String.format("上升%d名", rankChange);
            } else {
                return String.format("下降%d名", Math.abs(rankChange));
            }
        }
    }
}
