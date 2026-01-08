package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表盘统计响应")
public class DashboardStatisticsResponse {

    @Schema(description = "学生总数")
    private Long studentCount;

    @Schema(description = "班级总数")
    private Long classCount;

    @Schema(description = "宿舍总数")
    private Long dormitoryCount;

    @Schema(description = "今日检查数")
    private Long todayCheckCount;

    @Schema(description = "宿舍入住率")
    private BigDecimal occupancyRate;

    @Schema(description = "已完成检查数")
    private Long completedChecks;

    @Schema(description = "待处理检查数")
    private Long pendingChecks;

    @Schema(description = "检查完成率")
    private BigDecimal completionRate;

    @Schema(description = "检查得分趋势数据")
    private List<ChartDataItem> chartData;

    @Schema(description = "检查分类完成情况")
    private List<CategoryItem> checkCategories;

    @Schema(description = "最近检查记录")
    private List<RecentCheckRecord> recentRecords;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataItem {
        @Schema(description = "日期")
        private String date;
        @Schema(description = "得分")
        private BigDecimal score;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryItem {
        @Schema(description = "分类名称")
        private String name;
        @Schema(description = "完成率")
        private BigDecimal value;
        @Schema(description = "颜色")
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentCheckRecord {
        @Schema(description = "记录ID")
        private Long id;
        @Schema(description = "检查类型名称")
        private String typeName;
        @Schema(description = "检查目标名称")
        private String targetName;
        @Schema(description = "总分")
        private BigDecimal totalScore;
        @Schema(description = "得分率")
        private BigDecimal scoreRate;
        @Schema(description = "创建时间")
        private LocalDateTime createdAt;
    }
}
