package com.school.management.domain.teaching.model.exam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 考试安排实体 (属于 ExamBatch 聚合)
 */
public class ExamArrangement {
    private Long id;
    private Long batchId;
    private Long courseId;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer duration;
    private Integer examForm; // 1=笔试 2=机试 3=口试 4=实操
    private Integer totalStudents;
    private String remark;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;

    protected ExamArrangement() {}

    public static ExamArrangement create(Long batchId, Long courseId, LocalDate examDate,
                                          LocalTime startTime, LocalTime endTime,
                                          Integer duration, Integer examForm,
                                          Integer totalStudents, String remark, Long createdBy) {
        if (courseId == null) throw new IllegalArgumentException("课程不能为空");
        if (examDate == null) throw new IllegalArgumentException("考试日期不能为空");
        ExamArrangement a = new ExamArrangement();
        a.batchId = batchId;
        a.courseId = courseId;
        a.examDate = examDate;
        a.startTime = startTime;
        a.endTime = endTime;
        a.duration = duration;
        a.examForm = examForm != null ? examForm : 1;
        a.totalStudents = totalStudents != null ? totalStudents : 0;
        a.remark = remark;
        a.status = 1;
        a.createdBy = createdBy;
        a.createdAt = LocalDateTime.now();
        return a;
    }

    public static ExamArrangement reconstruct(Long id, Long batchId, Long courseId, LocalDate examDate,
                                               LocalTime startTime, LocalTime endTime, Integer duration,
                                               Integer examForm, Integer totalStudents, String remark,
                                               Integer status, Long createdBy, LocalDateTime createdAt) {
        ExamArrangement a = new ExamArrangement();
        a.id = id; a.batchId = batchId; a.courseId = courseId; a.examDate = examDate;
        a.startTime = startTime; a.endTime = endTime; a.duration = duration;
        a.examForm = examForm; a.totalStudents = totalStudents; a.remark = remark;
        a.status = status; a.createdBy = createdBy; a.createdAt = createdAt;
        return a;
    }

    public void update(Long courseId, LocalDate examDate, LocalTime startTime, LocalTime endTime,
                       Integer duration, Integer examForm, Integer totalStudents, String remark) {
        this.courseId = courseId;
        this.examDate = examDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        if (examForm != null) this.examForm = examForm;
        if (totalStudents != null) this.totalStudents = totalStudents;
        this.remark = remark;
    }

    // Getters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public Long getCourseId() { return courseId; }
    public LocalDate getExamDate() { return examDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Integer getDuration() { return duration; }
    public Integer getExamForm() { return examForm; }
    public Integer getTotalStudents() { return totalStudents; }
    public String getRemark() { return remark; }
    public Integer getStatus() { return status; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
