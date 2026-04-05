package com.school.management.domain.teaching.model.exam;

import com.school.management.domain.shared.AggregateRoot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 考试批次聚合根
 * 状态机: DRAFT(0) → PUBLISHED(1) → ONGOING(2) → FINISHED(3)
 */
public class ExamBatch extends AggregateRoot<Long> {
    private String batchCode;
    private String batchName;
    private Long semesterId;
    private ExamType examType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer status; // 0=草稿 1=已发布 2=进行中 3=已结束
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ExamArrangement> arrangements = new ArrayList<>();

    protected ExamBatch() {}

    public static ExamBatch create(String batchName, Long semesterId, ExamType examType,
                                    LocalDate startDate, LocalDate endDate, String remark, Long createdBy) {
        if (batchName == null || batchName.trim().isEmpty()) throw new IllegalArgumentException("批次名称不能为空");
        if (semesterId == null) throw new IllegalArgumentException("学期不能为空");
        if (startDate == null || endDate == null) throw new IllegalArgumentException("考试日期不能为空");
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("结束日期不能早于开始日期");

        ExamBatch batch = new ExamBatch();
        batch.batchName = batchName;
        batch.semesterId = semesterId;
        batch.examType = examType != null ? examType : ExamType.FINAL;
        batch.startDate = startDate;
        batch.endDate = endDate;
        batch.remark = remark;
        batch.status = 0;
        batch.createdBy = createdBy;
        batch.createdAt = LocalDateTime.now();
        batch.updatedAt = LocalDateTime.now();
        return batch;
    }

    public static ExamBatch reconstruct(Long id, String batchCode, String batchName, Long semesterId,
                                         ExamType examType, LocalDate startDate, LocalDate endDate,
                                         Integer status, String remark, Long createdBy,
                                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        ExamBatch batch = new ExamBatch();
        batch.setId(id);
        batch.batchCode = batchCode;
        batch.batchName = batchName;
        batch.semesterId = semesterId;
        batch.examType = examType;
        batch.startDate = startDate;
        batch.endDate = endDate;
        batch.status = status;
        batch.remark = remark;
        batch.createdBy = createdBy;
        batch.createdAt = createdAt;
        batch.updatedAt = updatedAt;
        return batch;
    }

    public void updateInfo(String batchName, Long semesterId, ExamType examType,
                           LocalDate startDate, LocalDate endDate, String remark) {
        if (this.status != null && this.status > 1) throw new IllegalStateException("进行中或已结束的批次不能修改");
        this.batchName = batchName;
        this.semesterId = semesterId;
        this.examType = examType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.remark = remark;
        this.updatedAt = LocalDateTime.now();
    }

    public void publish() {
        if (this.status != 0) throw new IllegalStateException("只有草稿状态的批次才能发布");
        this.status = 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void start() {
        if (this.status != 1) throw new IllegalStateException("只有已发布的批次才能开始");
        this.status = 2;
        this.updatedAt = LocalDateTime.now();
    }

    public void finish() {
        if (this.status != 2) throw new IllegalStateException("只有进行中的批次才能结束");
        this.status = 3;
        this.updatedAt = LocalDateTime.now();
    }

    public void assignBatchCode(String code) { this.batchCode = code; }

    public List<ExamArrangement> getArrangements() { return Collections.unmodifiableList(arrangements); }
    public void setArrangements(List<ExamArrangement> arrangements) { this.arrangements = arrangements != null ? arrangements : new ArrayList<>(); }

    // Getters
    public String getBatchCode() { return batchCode; }
    public String getBatchName() { return batchName; }
    public Long getSemesterId() { return semesterId; }
    public ExamType getExamType() { return examType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getStatus() { return status; }
    public String getRemark() { return remark; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
