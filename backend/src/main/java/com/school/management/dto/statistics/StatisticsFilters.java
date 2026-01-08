package com.school.management.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计筛选条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsFilters {

    /**
     * 检查计划ID（必填）
     */
    private Long planId;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 班级ID列表（为空表示全部）
     */
    private List<Long> classIds;

    /**
     * 年级ID列表（为空表示全部）
     */
    private List<Long> gradeIds;

    /**
     * 检查类别ID列表（为空表示全部）
     */
    private List<Long> categoryIds;

    /**
     * 扣分项ID列表（为空表示全部）
     */
    private List<Long> itemIds;

    /**
     * 是否使用加权分
     */
    private Boolean useWeightedScore;

    /**
     * 排序字段：totalScore, weightedScore, checkCount, avgScore
     */
    private String sortBy;

    /**
     * 排序方向：asc, desc
     */
    private String sortOrder;

    /**
     * 趋势统计粒度：day, week, month
     */
    private String trendGranularity;

    /**
     * 排名数量限制（如TOP10）
     */
    private Integer topN;

    /**
     * 分页-页码
     */
    private Integer pageNum;

    /**
     * 分页-每页数量
     */
    private Integer pageSize;

    /**
     * 获取排序字段，默认总分
     */
    public String getSortByOrDefault() {
        return sortBy != null ? sortBy : "totalScore";
    }

    /**
     * 获取排序方向，默认升序（扣分少排前面）
     */
    public String getSortOrderOrDefault() {
        return sortOrder != null ? sortOrder : "asc";
    }

    /**
     * 获取趋势粒度，默认按天
     */
    public String getTrendGranularityOrDefault() {
        return trendGranularity != null ? trendGranularity : "day";
    }

    /**
     * 获取TopN，默认10
     */
    public Integer getTopNOrDefault() {
        return topN != null ? topN : 10;
    }

    /**
     * 是否使用加权分，默认false
     */
    public Boolean getUseWeightedScoreOrDefault() {
        return useWeightedScore != null ? useWeightedScore : false;
    }
}
