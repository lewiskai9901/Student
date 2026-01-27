package com.school.management.domain.teaching.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 学年聚合根
 * 管理学年及其下属学期
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYear extends AggregateRoot<Long> {

    private Long id;

    /**
     * 学年代码，如2025-2026
     */
    private String yearCode;

    /**
     * 学年名称
     */
    private String yearName;

    /**
     * 学年开始日期
     */
    private LocalDate startDate;

    /**
     * 学年结束日期
     */
    private LocalDate endDate;

    /**
     * 是否当前学年
     */
    private Boolean isCurrent;

    /**
     * 状态：1正常 0停用
     */
    private Integer status;

    /**
     * 下属学期列表
     */
    @Builder.Default
    private List<Semester> semesters = new ArrayList<>();

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @Override
    public Long getId() {
        return id;
    }

    /**
     * 设为当前学年
     */
    public void setAsCurrent() {
        this.isCurrent = true;
    }

    /**
     * 取消当前学年
     */
    public void unsetAsCurrent() {
        this.isCurrent = false;
    }

    /**
     * 添加学期
     */
    public void addSemester(Semester semester) {
        if (semesters == null) {
            semesters = new ArrayList<>();
        }
        semester.setAcademicYearId(this.id);
        semesters.add(semester);
    }

    /**
     * 验证学年日期有效性
     */
    public boolean isDateValid() {
        return startDate != null && endDate != null && startDate.isBefore(endDate);
    }

    /**
     * 检查日期是否在学年范围内
     */
    public boolean containsDate(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 启用学年
     */
    public void enable() {
        this.status = 1;
    }

    /**
     * 停用学年
     */
    public void disable() {
        this.status = 0;
    }
}
