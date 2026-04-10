package com.school.management.domain.calendar.model.aggregate;

import com.school.management.domain.calendar.event.SemesterActivatedEvent;
import com.school.management.domain.calendar.event.SemesterCreatedEvent;
import com.school.management.domain.calendar.event.SemesterEndedEvent;
import com.school.management.domain.calendar.event.SemesterUpdatedEvent;
import com.school.management.domain.calendar.model.valueobject.SemesterStatus;
import com.school.management.domain.calendar.model.valueobject.SemesterType;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Semester extends AggregateRoot<Long> {
    private Long academicYearId;
    private String semesterName;
    private String semesterCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer startYear;
    private SemesterType semesterType;
    private Boolean isCurrent;
    private SemesterStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    protected Semester() {}

    private Semester(Long academicYearId, String semesterName, String semesterCode,
                     LocalDate startDate, LocalDate endDate, Integer startYear,
                     SemesterType semesterType) {
        validateDates(startDate, endDate);
        validateSemesterCode(semesterCode);
        this.academicYearId = academicYearId;
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

    public static Semester create(Long academicYearId, String semesterName, String semesterCode,
                                   LocalDate startDate, LocalDate endDate,
                                   Integer startYear, SemesterType semesterType) {
        Semester semester = new Semester(academicYearId, semesterName, semesterCode, startDate, endDate, startYear, semesterType);
        semester.registerEvent(new SemesterCreatedEvent(
                semester.getId() != null ? semester.getId().toString() : null,
                semesterName, semesterCode));
        return semester;
    }

    public static Semester reconstruct(Long id, Long academicYearId, String semesterName,
                                        String semesterCode, LocalDate startDate, LocalDate endDate,
                                        Integer startYear, SemesterType semesterType,
                                        Boolean isCurrent, SemesterStatus status,
                                        LocalDateTime createdAt, LocalDateTime updatedAt,
                                        Long createdBy, Long updatedBy) {
        Semester semester = new Semester();
        semester.setId(id);
        semester.academicYearId = academicYearId;
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

    public void updateBasicInfo(String semesterName, LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);
        this.semesterName = semesterName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.updatedAt = LocalDateTime.now();
        if (this.getId() != null) {
            registerEvent(new SemesterUpdatedEvent(this.getId().toString(), semesterName, this.semesterCode));
        }
    }

    public void setAsCurrent() {
        if (this.status != SemesterStatus.ACTIVE) {
            throw new IllegalStateException("只有正常状态的学期才能设置为当前学期");
        }
        this.isCurrent = true;
        this.updatedAt = LocalDateTime.now();
        if (this.getId() != null) {
            registerEvent(new SemesterActivatedEvent(this.getId().toString(), this.semesterName, this.semesterCode));
        }
    }

    public void unsetAsCurrent() {
        this.isCurrent = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void end() {
        if (this.status == SemesterStatus.ENDED) throw new IllegalStateException("学期已经结束");
        if (this.isCurrent) throw new IllegalStateException("不能结束当前学期，请先设置其他学期为当前学期");
        this.status = SemesterStatus.ENDED;
        this.updatedAt = LocalDateTime.now();
        if (this.getId() != null) {
            registerEvent(new SemesterEndedEvent(this.getId().toString(), this.semesterName, this.semesterCode));
        }
    }

    public void reactivate() {
        if (this.status == SemesterStatus.ACTIVE) throw new IllegalStateException("学期已经是正常状态");
        this.status = SemesterStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean containsDate(LocalDate date) {
        if (date == null || startDate == null || endDate == null) return false;
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean isOngoing() {
        return containsDate(LocalDate.now()) && status == SemesterStatus.ACTIVE;
    }

    public long getDurationDays() {
        if (startDate == null || endDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public static String generateCode(Integer startYear, SemesterType type) {
        if (startYear == null || type == null) throw new IllegalArgumentException("开始年份和学期类型不能为空");
        return String.format("%d-%d-%d", startYear, startYear + 1, type.getCode());
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) throw new IllegalArgumentException("学期开始日期不能为空");
        if (endDate == null) throw new IllegalArgumentException("学期结束日期不能为空");
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("学期结束日期不能早于开始日期");
    }

    private void validateSemesterCode(String semesterCode) {
        if (semesterCode == null || semesterCode.trim().isEmpty()) throw new IllegalArgumentException("学期编码不能为空");
        if (!semesterCode.matches("\\d{4}-\\d{4}-[12]")) throw new IllegalArgumentException("学期编码格式不正确，应为: YYYY-YYYY-N");
    }

    public Long getAcademicYearId() { return academicYearId; }
    public String getSemesterName() { return semesterName; }
    public String getSemesterCode() { return semesterCode; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getStartYear() { return startYear; }
    public SemesterType getSemesterType() { return semesterType; }
    public Boolean getIsCurrent() { return isCurrent; }
    public SemesterStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
