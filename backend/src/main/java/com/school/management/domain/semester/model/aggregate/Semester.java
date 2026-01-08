package com.school.management.domain.semester.model.aggregate;

import com.school.management.domain.semester.event.SemesterActivatedEvent;
import com.school.management.domain.semester.event.SemesterCreatedEvent;
import com.school.management.domain.semester.event.SemesterEndedEvent;
import com.school.management.domain.semester.event.SemesterUpdatedEvent;
import com.school.management.domain.semester.model.valueobject.SemesterStatus;
import com.school.management.domain.semester.model.valueobject.SemesterType;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学期聚合根
 *
 * 职责：
 * 1. 管理学期的基本信息（名称、编码、日期范围）
 * 2. 控制学期状态（正常/已结束）
 * 3. 管理当前学期标识
 * 4. 维护学期的不变量约束
 */
public class Semester extends AggregateRoot<Long> {

    // ==================== 基本信息 ====================

    /**
     * 学期名称
     */
    private String semesterName;

    /**
     * 学期编码 (如: 2024-2025-2)
     */
    private String semesterCode;

    /**
     * 学期开始日期
     */
    private LocalDate startDate;

    /**
     * 学期结束日期
     */
    private LocalDate endDate;

    /**
     * 开始年份 (如: 2024)
     */
    private Integer startYear;

    /**
     * 学期类型
     */
    private SemesterType semesterType;

    /**
     * 是否当前学期
     */
    private Boolean isCurrent;

    /**
     * 状态
     */
    private SemesterStatus status;

    // ==================== 审计字段 ====================

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    // ==================== 构造函数 ====================

    protected Semester() {
        // JPA/MyBatis需要
    }

    private Semester(String semesterName, String semesterCode, LocalDate startDate,
                     LocalDate endDate, Integer startYear, SemesterType semesterType) {
        validateDates(startDate, endDate);
        validateSemesterCode(semesterCode);

        this.semesterName = semesterName;
        this.semesterCode = semesterCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startYear = startYear;
        this.semesterType = semesterType;
        this.isCurrent = false;
        this.status = SemesterStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建新学期
     */
    public static Semester create(String semesterName, String semesterCode,
                                   LocalDate startDate, LocalDate endDate,
                                   Integer startYear, SemesterType semesterType) {
        Semester semester = new Semester(semesterName, semesterCode, startDate,
                endDate, startYear, semesterType);

        // 发布创建事件
        semester.registerEvent(new SemesterCreatedEvent(
                semester.getId() != null ? semester.getId().toString() : null,
                semesterName,
                semesterCode
        ));

        return semester;
    }

    /**
     * 从持久化重建学期
     */
    public static Semester reconstruct(Long id, String semesterName, String semesterCode,
                                        LocalDate startDate, LocalDate endDate,
                                        Integer startYear, SemesterType semesterType,
                                        Boolean isCurrent, SemesterStatus status,
                                        LocalDateTime createdAt, LocalDateTime updatedAt,
                                        Long createdBy, Long updatedBy) {
        Semester semester = new Semester();
        semester.setId(id);
        semester.semesterName = semesterName;
        semester.semesterCode = semesterCode;
        semester.startDate = startDate;
        semester.endDate = endDate;
        semester.startYear = startYear;
        semester.semesterType = semesterType;
        semester.isCurrent = isCurrent;
        semester.status = status;
        semester.createdAt = createdAt;
        semester.updatedAt = updatedAt;
        semester.createdBy = createdBy;
        semester.updatedBy = updatedBy;
        return semester;
    }

    // ==================== 业务方法 ====================

    /**
     * 更新学期基本信息
     */
    public void updateBasicInfo(String semesterName, LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);

        this.semesterName = semesterName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new SemesterUpdatedEvent(
                this.getId().toString(),
                semesterName,
                this.semesterCode
        ));
    }

    /**
     * 设置为当前学期
     */
    public void setAsCurrent() {
        if (this.status != SemesterStatus.ACTIVE) {
            throw new IllegalStateException("只有正常状态的学期才能设置为当前学期");
        }

        this.isCurrent = true;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new SemesterActivatedEvent(
                this.getId().toString(),
                this.semesterName,
                this.semesterCode
        ));
    }

    /**
     * 取消当前学期标识
     */
    public void unsetAsCurrent() {
        this.isCurrent = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 结束学期
     */
    public void end() {
        if (this.status == SemesterStatus.ENDED) {
            throw new IllegalStateException("学期已经结束");
        }

        if (this.isCurrent) {
            throw new IllegalStateException("不能结束当前学期，请先设置其他学期为当前学期");
        }

        this.status = SemesterStatus.ENDED;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new SemesterEndedEvent(
                this.getId().toString(),
                this.semesterName,
                this.semesterCode
        ));
    }

    /**
     * 重新激活学期
     */
    public void reactivate() {
        if (this.status == SemesterStatus.ACTIVE) {
            throw new IllegalStateException("学期已经是正常状态");
        }

        this.status = SemesterStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查日期是否在学期范围内
     */
    public boolean containsDate(LocalDate date) {
        if (date == null || startDate == null || endDate == null) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 检查是否是当前进行中的学期（基于日期）
     */
    public boolean isOngoing() {
        LocalDate today = LocalDate.now();
        return containsDate(today) && status == SemesterStatus.ACTIVE;
    }

    /**
     * 获取学期时长（天数）
     */
    public long getDurationDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * 生成学期编码
     */
    public static String generateCode(Integer startYear, SemesterType type) {
        if (startYear == null || type == null) {
            throw new IllegalArgumentException("开始年份和学期类型不能为空");
        }
        int endYear = startYear + 1;
        return String.format("%d-%d-%d", startYear, endYear, type.getCode());
    }

    // ==================== 验证方法 ====================

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("学期开始日期不能为空");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("学期结束日期不能为空");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("学期结束日期不能早于开始日期");
        }
    }

    private void validateSemesterCode(String semesterCode) {
        if (semesterCode == null || semesterCode.trim().isEmpty()) {
            throw new IllegalArgumentException("学期编码不能为空");
        }
        // 验证格式: YYYY-YYYY-N
        if (!semesterCode.matches("\\d{4}-\\d{4}-[12]")) {
            throw new IllegalArgumentException("学期编码格式不正确，应为: YYYY-YYYY-N (如: 2024-2025-1)");
        }
    }

    // ==================== Getters ====================

    public String getSemesterName() {
        return semesterName;
    }

    public String getSemesterCode() {
        return semesterCode;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Integer getStartYear() {
        return startYear;
    }

    public SemesterType getSemesterType() {
        return semesterType;
    }

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public SemesterStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    // ==================== Setters (仅内部使用) ====================

    void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
