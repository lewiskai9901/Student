package com.school.management.domain.teaching.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教学周实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeachingWeek {

    private Long id;

    /**
     * 所属学期ID
     */
    private Long semesterId;

    /**
     * 周次（第几周）
     */
    private Integer weekNumber;

    /**
     * 本周开始日期（周一）
     */
    private LocalDate startDate;

    /**
     * 本周结束日期（周日）
     */
    private LocalDate endDate;

    /**
     * 周类型：1正常教学周 2考试周 3假期周 4实践周
     */
    @Builder.Default
    private Integer weekType = 1;

    /**
     * 周标签，如"国庆假期"
     */
    private String weekLabel;

    /**
     * 是否有效（停课则为0）
     */
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 备注
     */
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 获取周类型名称
     */
    public String getWeekTypeName() {
        return switch (weekType) {
            case 1 -> "教学周";
            case 2 -> "考试周";
            case 3 -> "假期周";
            case 4 -> "实践周";
            default -> "未知";
        };
    }

    /**
     * 是否为教学周
     */
    public boolean isTeachingWeek() {
        return weekType == 1;
    }

    /**
     * 是否为考试周
     */
    public boolean isExamWeek() {
        return weekType == 2;
    }

    /**
     * 检查日期是否在本周
     */
    public boolean containsDate(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 标记为假期
     */
    public void markAsHoliday(String label) {
        this.weekType = 3;
        this.weekLabel = label;
        this.isActive = false;
    }

    /**
     * 标记为考试周
     */
    public void markAsExamWeek() {
        this.weekType = 2;
    }

    /**
     * 恢复为正常教学周
     */
    public void restoreToNormal() {
        this.weekType = 1;
        this.weekLabel = null;
        this.isActive = true;
    }
}
