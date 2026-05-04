package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("exam_batches")
public class ExamBatchPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String batchCode;
    private String batchName;
    private Long semesterId;
    private Integer examType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String name; // alias of batchName for UI compatibility
    @TableField(exist = false)
    private String semesterName;
    @TableField(exist = false)
    private String createdByName;

    // ==================== 业务行为 ====================
    // 状态: 0=草稿 1=进行中 2=已发布 3=已结束

    public boolean isPublished() {
        return status != null && status == 2;
    }

    public boolean isDraft() {
        return status == null || status == 0;
    }

    /** 是否可发布: 必须是草稿/进行中 + 必填字段齐全. 不可发布则返回原因. */
    public String canPublishReason() {
        if (isPublished()) return "考试批次已发布";
        if (status != null && status == 3) return "考试批次已结束";
        if (semesterId == null) return "学期不能为空";
        if (examType == null) return "考试类型不能为空";
        if (startDate == null || endDate == null) return "考试起止日期不能为空";
        if (endDate.isBefore(startDate)) return "结束日期不能早于开始日期";
        return null; // 可发布
    }

    /** 业务: 发布. 调用方需保证持久化. */
    public void publish() {
        String reason = canPublishReason();
        if (reason != null) {
            throw com.school.management.exception.TeachingDomainException.examBatchNotReady(reason);
        }
        this.status = 2;
    }
}
