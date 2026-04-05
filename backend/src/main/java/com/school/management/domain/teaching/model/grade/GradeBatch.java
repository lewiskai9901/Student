package com.school.management.domain.teaching.model.grade;

import com.school.management.domain.shared.AggregateRoot;
import java.time.LocalDateTime;

/**
 * 成绩批次聚合根
 * 状态机: DRAFT(0) → SUBMITTED(1) → APPROVED(2) → PUBLISHED(3)
 */
public class GradeBatch extends AggregateRoot<Long> {
    private String batchCode;
    private String batchName;
    private Long semesterId;
    private GradeType gradeType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected GradeBatch() {}

    public static GradeBatch create(String batchName, Long semesterId, GradeType gradeType,
                                     LocalDateTime startTime, LocalDateTime endTime, Long createdBy) {
        if (batchName == null || batchName.trim().isEmpty()) throw new IllegalArgumentException("批次名称不能为空");
        if (semesterId == null) throw new IllegalArgumentException("学期不能为空");
        GradeBatch batch = new GradeBatch();
        batch.batchName = batchName;
        batch.semesterId = semesterId;
        batch.gradeType = gradeType != null ? gradeType : GradeType.FINAL;
        batch.startTime = startTime;
        batch.endTime = endTime;
        batch.status = 0;
        batch.createdBy = createdBy;
        batch.createdAt = LocalDateTime.now();
        batch.updatedAt = LocalDateTime.now();
        return batch;
    }

    public static GradeBatch reconstruct(Long id, String batchCode, String batchName, Long semesterId,
                                          GradeType gradeType, LocalDateTime startTime, LocalDateTime endTime,
                                          Integer status, Long createdBy,
                                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        GradeBatch b = new GradeBatch();
        b.setId(id); b.batchCode = batchCode; b.batchName = batchName;
        b.semesterId = semesterId; b.gradeType = gradeType;
        b.startTime = startTime; b.endTime = endTime; b.status = status;
        b.createdBy = createdBy; b.createdAt = createdAt; b.updatedAt = updatedAt;
        return b;
    }

    public void updateInfo(String batchName, Long semesterId, GradeType gradeType,
                           LocalDateTime startTime, LocalDateTime endTime) {
        if (this.status != null && this.status > 0) throw new IllegalStateException("已提交的批次不能修改");
        this.batchName = batchName;
        this.semesterId = semesterId;
        this.gradeType = gradeType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.updatedAt = LocalDateTime.now();
    }

    public void submit() {
        if (this.status != 0) throw new IllegalStateException("只有草稿状态的批次才能提交");
        this.status = 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void approve() {
        if (this.status != 1) throw new IllegalStateException("只有已提交的批次才能审核");
        this.status = 2;
        this.updatedAt = LocalDateTime.now();
    }

    public void publish() {
        if (this.status != 2) throw new IllegalStateException("只有已审核的批次才能发布");
        this.status = 3;
        this.updatedAt = LocalDateTime.now();
    }

    public void assignBatchCode(String code) { this.batchCode = code; }

    public String getBatchCode() { return batchCode; }
    public String getBatchName() { return batchName; }
    public Long getSemesterId() { return semesterId; }
    public GradeType getGradeType() { return gradeType; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Integer getStatus() { return status; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
