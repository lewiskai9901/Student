package com.school.management.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 扣分项统计VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemStatisticsVO {

    /**
     * 扣分项ID
     */
    private Long itemId;

    /**
     * 扣分项名称
     */
    private String itemName;

    /**
     * 所属类别ID
     */
    private Long categoryId;

    /**
     * 所属类别名称
     */
    private String categoryName;

    /**
     * 扣分模式：1固定 2按人数 3区间
     */
    private Integer deductMode;

    /**
     * 扣分模式描述
     */
    private String deductModeDesc;

    /**
     * 触发次数
     */
    private Integer triggerCount;

    /**
     * 涉及人数
     */
    private Integer personCount;

    /**
     * 总扣分
     */
    private BigDecimal totalScore;

    /**
     * 平均每次扣分
     */
    private BigDecimal avgScore;

    /**
     * 涉及班级数
     */
    private Integer classCount;

    /**
     * 该项扣分最多的班级TOP3
     */
    private List<ClassItemVO> topClasses;

    /**
     * 班级扣分项简要信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassItemVO {
        private Long classId;
        private String className;
        private Integer triggerCount;
        private BigDecimal totalScore;
    }

    /**
     * 获取扣分模式描述
     */
    public static String getDeductModeDescription(Integer mode) {
        if (mode == null) return "未知";
        return switch (mode) {
            case 1 -> "固定扣分";
            case 2 -> "按人数扣分";
            case 3 -> "区间扣分";
            default -> "未知";
        };
    }
}
