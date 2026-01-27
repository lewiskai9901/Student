package com.school.management.domain.teaching.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.teaching.model.entity.TeachingWeek;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 学期聚合根
 * 管理学期信息及其教学周
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Semester extends AggregateRoot<Long> {

    private Long id;

    /**
     * 所属学年ID
     */
    private Long academicYearId;

    /**
     * 学期代码，如2025-2026-1
     */
    private String semesterCode;

    /**
     * 学期名称
     */
    private String semesterName;

    /**
     * 学期类型：1秋季/第一学期 2春季/第二学期
     */
    private Integer semesterType;

    /**
     * 学期开始日期
     */
    private LocalDate startDate;

    /**
     * 学期结束日期
     */
    private LocalDate endDate;

    /**
     * 教学开始日期
     */
    private LocalDate teachingStartDate;

    /**
     * 教学结束日期
     */
    private LocalDate teachingEndDate;

    /**
     * 考试周开始日期
     */
    private LocalDate examStartDate;

    /**
     * 考试周结束日期
     */
    private LocalDate examEndDate;

    /**
     * 教学周数
     */
    @Builder.Default
    private Integer totalTeachingWeeks = 18;

    /**
     * 是否当前学期
     */
    private Boolean isCurrent;

    /**
     * 状态：1正常 0停用
     */
    private Integer status;

    /**
     * 教学周列表
     */
    @Builder.Default
    private List<TeachingWeek> teachingWeeks = new ArrayList<>();

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @Override
    public Long getId() {
        return id;
    }

    /**
     * 设为当前学期
     */
    public void setAsCurrent() {
        this.isCurrent = true;
    }

    /**
     * 取消当前学期
     */
    public void unsetAsCurrent() {
        this.isCurrent = false;
    }

    /**
     * 自动生成教学周
     */
    public List<TeachingWeek> generateTeachingWeeks() {
        if (teachingStartDate == null || totalTeachingWeeks == null) {
            throw new IllegalStateException("教学开始日期和教学周数不能为空");
        }

        teachingWeeks = new ArrayList<>();
        LocalDate currentMonday = teachingStartDate;

        // 调整到周一
        int dayOfWeek = currentMonday.getDayOfWeek().getValue();
        if (dayOfWeek != 1) {
            currentMonday = currentMonday.minusDays(dayOfWeek - 1);
        }

        for (int i = 1; i <= totalTeachingWeeks; i++) {
            TeachingWeek week = TeachingWeek.builder()
                    .semesterId(this.id)
                    .weekNumber(i)
                    .startDate(currentMonday)
                    .endDate(currentMonday.plusDays(6))
                    .weekType(determineWeekType(i))
                    .isActive(true)
                    .build();
            teachingWeeks.add(week);
            currentMonday = currentMonday.plusDays(7);
        }

        return teachingWeeks;
    }

    /**
     * 确定周类型
     */
    private Integer determineWeekType(int weekNumber) {
        // 默认为正常教学周
        // 如果有考试周配置，可以根据日期判断
        if (examStartDate != null && examEndDate != null) {
            LocalDate weekStart = teachingStartDate.plusWeeks(weekNumber - 1);
            if (!weekStart.isBefore(examStartDate)) {
                return 2; // 考试周
            }
        }
        return 1; // 正常教学周
    }

    /**
     * 获取指定日期所在的教学周
     */
    public TeachingWeek getWeekByDate(LocalDate date) {
        for (TeachingWeek week : teachingWeeks) {
            if (!date.isBefore(week.getStartDate()) && !date.isAfter(week.getEndDate())) {
                return week;
            }
        }
        return null;
    }

    /**
     * 获取当前教学周
     */
    public TeachingWeek getCurrentWeek() {
        return getWeekByDate(LocalDate.now());
    }

    /**
     * 计算学期总天数
     */
    public long getTotalDays() {
        if (startDate != null && endDate != null) {
            return ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }
        return 0;
    }

    /**
     * 是否在教学期间
     */
    public boolean isInTeachingPeriod(LocalDate date) {
        return teachingStartDate != null && teachingEndDate != null
                && !date.isBefore(teachingStartDate) && !date.isAfter(teachingEndDate);
    }

    /**
     * 是否在考试期间
     */
    public boolean isInExamPeriod(LocalDate date) {
        return examStartDate != null && examEndDate != null
                && !date.isBefore(examStartDate) && !date.isAfter(examEndDate);
    }
}
