package com.school.management.application.asset.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资产统计DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetStatisticsDTO {

    // 总数量
    private Integer totalCount;

    // 在用数量
    private Integer inUseCount;

    // 闲置数量
    private Integer idleCount;

    // 维修中数量
    private Integer repairingCount;

    // 已报废数量
    private Integer scrappedCount;

    // 总原值
    private BigDecimal totalOriginalValue;

    // 总净值
    private BigDecimal totalNetValue;

    // 按分类统计
    private List<CategoryStatistics> categoryStatistics;

    // 按位置类型统计
    private List<LocationStatistics> locationStatistics;

    /**
     * 分类统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStatistics {
        private Long categoryId;
        private String categoryName;
        private Integer count;
        private BigDecimal totalValue;
    }

    /**
     * 位置统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationStatistics {
        private String locationType;
        private String locationTypeDesc;
        private Integer count;
    }
}
