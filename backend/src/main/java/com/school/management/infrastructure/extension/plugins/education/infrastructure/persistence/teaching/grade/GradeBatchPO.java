package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.grade;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("grade_batches")
public class GradeBatchPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String batchCode;
    private String batchName;
    private Long semesterId;
    private Long courseId;
    private Long orgUnitId;
    @TableField(exist = false)
    private Long examBatchId;
    private Integer gradeType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String courseName;
    @TableField(exist = false)
    private String className;
    @TableField(exist = false)
    private String createdByName;
    @TableField(exist = false)
    private java.time.LocalDateTime inputDeadline; // alias of endTime for UI

    // ==================== 业务行为 ====================
    // 状态: 0=草稿 1=已提交 2=已审核 3=已发布

    public boolean isPublished()  { return status != null && status == 3; }
    public boolean isApproved()   { return status != null && status == 2; }
    public boolean isSubmitted()  { return status != null && status == 1; }
    public boolean isDraft()      { return status == null || status == 0; }

    /** 是否可提交: 必须草稿态 + 必填齐全. */
    public String canSubmitReason() {
        if (!isDraft()) return "成绩批次状态非草稿, 不能提交";
        if (semesterId == null) return "学期不能为空";
        if (courseId == null) return "课程不能为空";
        if (orgUnitId == null) return "归属组织不能为空";
        return null;
    }

    /** 是否可审核: 必须已提交态. */
    public String canApproveReason() {
        if (isPublished()) return "成绩批次已发布, 无需再审核";
        if (isApproved())  return "成绩批次已审核";
        if (!isSubmitted()) return "成绩批次未提交, 不能审核";
        return null;
    }

    /** 是否可发布: 必须已审核态. */
    public String canPublishReason() {
        if (isPublished()) return "成绩批次已发布";
        if (!isApproved()) return "成绩批次未审核, 不能发布";
        return null;
    }

    public void submit() {
        String reason = canSubmitReason();
        if (reason != null) throw com.school.management.exception.TeachingDomainException.gradeBatchNotReady(reason);
        this.status = 1;
    }

    public void approve() {
        String reason = canApproveReason();
        if (reason != null) throw com.school.management.exception.TeachingDomainException.gradeBatchNotReady(reason);
        this.status = 2;
    }

    public void publish() {
        String reason = canPublishReason();
        if (reason != null) throw com.school.management.exception.TeachingDomainException.gradeBatchNotReady(reason);
        this.status = 3;
    }
}
