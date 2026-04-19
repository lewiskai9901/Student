package com.school.management.application.myclass.query;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 班级概览数据 DTO
 */
@Data
@Builder
public class MyClassOverviewDTO {
    private Long orgUnitId;
    private String className;

    // 统计数据
    private Integer studentCount;
    private Integer maleCount;
    private Integer femaleCount;
    private Integer classRank;
    private Integer totalClasses;
    private Double averageScore;
    private Double scoreTrend; // 较上周变化
    private Integer pendingAppeals;

    // 近30天趋势数据
    private List<ScoreTrendItem> scoreTrendList;

    // 最近检查记录
    private List<RecentCheckRecord> recentRecords;

    @Data
    @Builder
    public static class ScoreTrendItem {
        private String date;
        private Double score;
    }

    @Data
    @Builder
    public static class RecentCheckRecord {
        private Long id;
        private String checkDate;
        private String checkType;
        private Double score;
        private Integer rank;
    }
}
