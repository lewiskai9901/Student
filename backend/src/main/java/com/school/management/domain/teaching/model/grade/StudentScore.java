package com.school.management.domain.teaching.model.grade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生成绩实体
 * 命名为 StudentScore 避免与年级 Grade 概念冲突
 */
public class StudentScore {
    private Long id;
    private Long batchId;
    private Long semesterId;
    private Long taskId;
    private Long courseId;
    private Long studentId;
    private Long classId;
    private BigDecimal totalScore;
    private String gradeLevel;
    private BigDecimal gradePoint;
    private Boolean passed;
    private BigDecimal creditsEarned;
    private Integer gradeStatus; // 0=未录入 1=已录入 2=已确认
    private Boolean isMakeup;
    private Boolean isRetake;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected StudentScore() {}

    public static StudentScore create(Long batchId, Long semesterId, Long taskId, Long courseId,
                                       Long studentId, Long classId) {
        if (studentId == null) throw new IllegalArgumentException("学生不能为空");
        if (courseId == null) throw new IllegalArgumentException("课程不能为空");
        StudentScore s = new StudentScore();
        s.batchId = batchId; s.semesterId = semesterId; s.taskId = taskId;
        s.courseId = courseId; s.studentId = studentId; s.classId = classId;
        s.gradeStatus = 0;
        s.isMakeup = false;
        s.isRetake = false;
        s.createdAt = LocalDateTime.now();
        s.updatedAt = LocalDateTime.now();
        return s;
    }

    public static StudentScore reconstruct(Long id, Long batchId, Long semesterId, Long taskId,
                                            Long courseId, Long studentId, Long classId,
                                            BigDecimal totalScore, String gradeLevel, BigDecimal gradePoint,
                                            Boolean passed, BigDecimal creditsEarned, Integer gradeStatus,
                                            Boolean isMakeup, Boolean isRetake, String remark,
                                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        StudentScore s = new StudentScore();
        s.id = id; s.batchId = batchId; s.semesterId = semesterId; s.taskId = taskId;
        s.courseId = courseId; s.studentId = studentId; s.classId = classId;
        s.totalScore = totalScore; s.gradeLevel = gradeLevel; s.gradePoint = gradePoint;
        s.passed = passed; s.creditsEarned = creditsEarned; s.gradeStatus = gradeStatus;
        s.isMakeup = isMakeup; s.isRetake = isRetake; s.remark = remark;
        s.createdAt = createdAt; s.updatedAt = updatedAt;
        return s;
    }

    /**
     * 录入成绩 — 核心业务方法
     */
    public void record(BigDecimal totalScore, BigDecimal gradePoint, BigDecimal creditsEarned, String remark) {
        this.totalScore = totalScore;
        this.gradePoint = gradePoint;
        this.creditsEarned = creditsEarned;
        this.remark = remark;
        this.gradeLevel = calculateGradeLevel(totalScore);
        this.passed = calculatePassed(totalScore);
        this.gradeStatus = 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void confirm() {
        if (this.gradeStatus != 1) throw new IllegalStateException("只有已录入的成绩才能确认");
        this.gradeStatus = 2;
        this.updatedAt = LocalDateTime.now();
    }

    private String calculateGradeLevel(BigDecimal score) {
        if (score == null) return null;
        double s = score.doubleValue();
        if (s >= 90) return "A";
        if (s >= 80) return "B";
        if (s >= 70) return "C";
        if (s >= 60) return "D";
        return "F";
    }

    private Boolean calculatePassed(BigDecimal score) {
        if (score == null) return null;
        return score.doubleValue() >= 60;
    }

    // Getters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public Long getSemesterId() { return semesterId; }
    public Long getTaskId() { return taskId; }
    public Long getCourseId() { return courseId; }
    public Long getStudentId() { return studentId; }
    public Long getClassId() { return classId; }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getGradeLevel() { return gradeLevel; }
    public BigDecimal getGradePoint() { return gradePoint; }
    public Boolean getPassed() { return passed; }
    public BigDecimal getCreditsEarned() { return creditsEarned; }
    public Integer getGradeStatus() { return gradeStatus; }
    public Boolean getIsMakeup() { return isMakeup; }
    public Boolean getIsRetake() { return isRetake; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
