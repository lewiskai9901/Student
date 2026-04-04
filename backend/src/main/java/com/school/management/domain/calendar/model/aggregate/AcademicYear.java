package com.school.management.domain.calendar.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AcademicYear extends AggregateRoot<Long> {
    private String yearCode;
    private String yearName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
    private Integer status; // 1=active, 0=inactive
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected AcademicYear() {}

    private AcademicYear(String yearCode, String yearName, LocalDate startDate, LocalDate endDate) {
        if (yearName == null || yearName.trim().isEmpty()) throw new IllegalArgumentException("学年名称不能为空");
        validateDates(startDate, endDate);
        this.yearCode = yearCode;
        this.yearName = yearName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isCurrent = false;
        this.status = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static AcademicYear create(String yearCode, String yearName, LocalDate startDate, LocalDate endDate) {
        if (yearCode == null || yearCode.trim().isEmpty()) {
            yearCode = startDate.getYear() + "-" + endDate.getYear();
        }
        return new AcademicYear(yearCode, yearName, startDate, endDate);
    }

    public static AcademicYear reconstruct(Long id, String yearCode, String yearName,
                                            LocalDate startDate, LocalDate endDate,
                                            Boolean isCurrent, Integer status,
                                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        AcademicYear year = new AcademicYear();
        year.setId(id);
        year.yearCode = yearCode;
        year.yearName = yearName;
        year.startDate = startDate;
        year.endDate = endDate;
        year.isCurrent = isCurrent;
        year.status = status;
        year.createdAt = createdAt;
        year.updatedAt = updatedAt;
        return year;
    }

    public void updateBasicInfo(String yearName, LocalDate startDate, LocalDate endDate) {
        if (yearName == null || yearName.trim().isEmpty()) throw new IllegalArgumentException("学年名称不能为空");
        validateDates(startDate, endDate);
        this.yearName = yearName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.updatedAt = LocalDateTime.now();
    }

    public void setAsCurrent() {
        this.isCurrent = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void unsetAsCurrent() {
        this.isCurrent = false;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) throw new IllegalArgumentException("学年开始日期不能为空");
        if (endDate == null) throw new IllegalArgumentException("学年结束日期不能为空");
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("学年结束日期不能早于开始日期");
    }

    public String getYearCode() { return yearCode; }
    public String getYearName() { return yearName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Boolean getIsCurrent() { return isCurrent; }
    public Integer getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
