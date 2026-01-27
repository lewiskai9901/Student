package com.school.management.dto.statistics.smart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 智能统计筛选条件
 * 支持更灵活的筛选和对比模式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartStatisticsFilters {

    // ==================== 基本筛选 ====================

    /**
     * 检查计划ID（必填）
     */
    private Long planId;

    /**
     * 指定检查记录ID列表（可选，用于精确筛选）
     */
    private List<Long> recordIds;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    // ==================== 范围筛选 ====================

    /**
     * 班级ID列表
     */
    private List<Long> classIds;

    /**
     * 年级ID列表
     */
    private List<Long> gradeIds;

    /**
     * 部门ID列表
     */
    private List<Long> orgUnitIds;

    /**
     * 检查类别ID列表
     */
    private List<Long> categoryIds;

    // ==================== 对比模式 ====================

    /**
     * 排名对比模式：
     * - total: 按总分排名
     * - average: 按每轮平均分排名（推荐）
     * - normalized: 按归一化分排名
     * - weighted: 按加权分排名
     */
    private String compareMode;

    /**
     * 是否包含部分检查的班级（缺检班级）
     * true: 包含（按实际数据计算）
     * false: 排除（不参与排名但单独展示）
     */
    private Boolean includePartial;

    /**
     * 缺检处理策略：
     * - exclude: 排除不参与排名（默认）
     * - zero: 按0分计算
     * - average: 按平均分计算
     * - penalty: 按惩罚分计算
     */
    private String missingStrategy;

    // ==================== 排序设置 ====================

    /**
     * 排序字段
     */
    private String sortBy;

    /**
     * 排序方向：asc, desc
     */
    private String sortOrder;

    // ==================== 趋势设置 ====================

    /**
     * 趋势统计粒度：day, week, month
     */
    private String trendGranularity;

    /**
     * 对比基准：
     * - previous: 与上一期对比
     * - lastWeek: 与上周对比
     * - lastMonth: 与上月对比
     * - custom: 自定义对比范围
     */
    private String compareBase;

    /**
     * 自定义对比开始日期
     */
    private LocalDate compareStartDate;

    /**
     * 自定义对比结束日期
     */
    private LocalDate compareEndDate;

    // ==================== 分页设置 ====================

    /**
     * 排名数量限制（如TOP10）
     */
    private Integer topN;

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 每页数量
     */
    private Integer pageSize;

    // ==================== 默认值方法 ====================

    public String getCompareModeOrDefault() {
        return compareMode != null ? compareMode : "average";
    }

    public Boolean getIncludePartialOrDefault() {
        return includePartial != null ? includePartial : false;
    }

    public String getMissingStrategyOrDefault() {
        return missingStrategy != null ? missingStrategy : "exclude";
    }

    public String getSortByOrDefault() {
        return sortBy != null ? sortBy : "avgScorePerRound";
    }

    public String getSortOrderOrDefault() {
        return sortOrder != null ? sortOrder : "asc";
    }

    public String getTrendGranularityOrDefault() {
        return trendGranularity != null ? trendGranularity : "day";
    }

    public String getCompareBaseOrDefault() {
        return compareBase != null ? compareBase : "previous";
    }

    public Integer getTopNOrDefault() {
        return topN != null ? topN : 10;
    }

    public Integer getPageNumOrDefault() {
        return pageNum != null ? pageNum : 1;
    }

    public Integer getPageSizeOrDefault() {
        return pageSize != null ? pageSize : 20;
    }
}
