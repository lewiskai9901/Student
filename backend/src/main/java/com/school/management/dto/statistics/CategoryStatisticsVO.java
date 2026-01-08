package com.school.management.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检查类别统计VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatisticsVO {

    /**
     * 类别ID
     */
    private Long categoryId;

    /**
     * 类别名称
     */
    private String categoryName;

    /**
     * 类别编码
     */
    private String categoryCode;

    /**
     * 总扣分
     */
    private BigDecimal totalScore;

    /**
     * 占总扣分百分比
     */
    private BigDecimal percentage;

    /**
     * 扣分项触发次数
     */
    private Integer itemCount;

    /**
     * 涉及班级数
     */
    private Integer classCount;

    /**
     * 涉及人次
     */
    private Integer personCount;

    /**
     * 高频扣分项TOP5
     */
    private List<ItemBriefVO> topItems;

    /**
     * 扣分项简要信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemBriefVO {
        private Long itemId;
        private String itemName;
        private Integer triggerCount;
        private BigDecimal totalScore;
    }
}
