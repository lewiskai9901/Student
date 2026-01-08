package com.school.management.dto.rating.statistics;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 班级评级频次详细VO
 * 用于展示某个等级下所有班级的详细频次信息
 *
 * @author Claude Code
 * @since 2025-12-22
 */
@Data
public class ClassFrequencyDetailVO {

    private Long classId;
    private String className;
    private String departmentName;
    private String gradeName;

    // 频次统计
    private Integer frequency;          // 获得次数
    private Integer totalRatings;       // 参评总次数
    private BigDecimal rate;            // 获得率 (frequency/totalRatings * 100)
    private Integer ranking;            // 在该等级中的排名

    // 连续记录
    private Integer consecutiveCount;   // 当前连续获得次数
    private Integer bestStreak;         // 历史最佳连续记录

    // 最近获得日期
    private List<LocalDate> recentDates; // 最近5次获得日期
    private LocalDate lastRatingDate;    // 最近一次获得日期

    // 获得的徽章
    private List<String> badgeNames;     // 徽章名称列表
    private Integer badgeCount;          // 徽章总数

    // 等级信息（冗余）
    private Long levelId;
    private String levelName;
    private String levelColor;
}
