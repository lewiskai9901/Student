package com.school.management.domain.inspection.model.v7.platform;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * V7 假日日历聚合根
 * 管理年度假日和调休工作日，用于检查排期时自动跳过非工作日。
 */
public class HolidayCalendar extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private String calendarName;
    private Integer year;
    /** JSON array of date strings, e.g. ["2026-01-01","2026-01-26"] */
    private String holidays;
    /** JSON array of make-up workday dates, e.g. ["2026-02-07"] */
    private String workdays;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected HolidayCalendar() {
    }

    private HolidayCalendar(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.calendarName = builder.calendarName;
        this.year = builder.year;
        this.holidays = builder.holidays;
        this.workdays = builder.workdays;
        this.isDefault = builder.isDefault != null ? builder.isDefault : false;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static HolidayCalendar create(Builder builder) {
        return new HolidayCalendar(builder);
    }

    public static HolidayCalendar reconstruct(Builder builder) {
        return new HolidayCalendar(builder);
    }

    public void update(String calendarName, Integer year, String holidays,
                       String workdays, Boolean isDefault) {
        this.calendarName = calendarName;
        this.year = year;
        this.holidays = holidays;
        this.workdays = workdays;
        this.isDefault = isDefault;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public String getCalendarName() { return calendarName; }
    public Integer getYear() { return year; }
    public String getHolidays() { return holidays; }
    public String getWorkdays() { return workdays; }
    public Boolean getIsDefault() { return isDefault; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public void setCalendarName(String calendarName) { this.calendarName = calendarName; }
    public void setYear(Integer year) { this.year = year; }
    public void setHolidays(String holidays) { this.holidays = holidays; }
    public void setWorkdays(String workdays) { this.workdays = workdays; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private String calendarName;
        private Integer year;
        private String holidays;
        private String workdays;
        private Boolean isDefault;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder calendarName(String calendarName) { this.calendarName = calendarName; return this; }
        public Builder year(Integer year) { this.year = year; return this; }
        public Builder holidays(String holidays) { this.holidays = holidays; return this; }
        public Builder workdays(String workdays) { this.workdays = workdays; return this; }
        public Builder isDefault(Boolean isDefault) { this.isDefault = isDefault; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public HolidayCalendar build() { return new HolidayCalendar(this); }
    }
}
