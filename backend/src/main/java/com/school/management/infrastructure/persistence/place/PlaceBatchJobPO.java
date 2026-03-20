package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 场所批量操作Job持久化对象
 * 对应表: place_batch_jobs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("place_batch_jobs")
public class PlaceBatchJobPO {

    /**
     * 任务ID (UUID)
     */
    @TableId(type = IdType.INPUT)
    private String jobId;

    /**
     * 任务类型: BATCH_ASSIGN_ORG/BATCH_CHECK_IN/BATCH_UPDATE_STATUS/BATCH_SET_CAPACITY
     */
    private String jobType;

    /**
     * 任务名称（用户自定义）
     */
    private String jobName;

    /**
     * 任务状态: PENDING/RUNNING/COMPLETED/FAILED/PARTIALLY_COMPLETED/CANCELLED
     */
    @Builder.Default
    private String jobStatus = "PENDING";

    /**
     * 总项目数
     */
    private Integer totalItems;

    /**
     * 已处理项目数
     */
    @Builder.Default
    private Integer processedItems = 0;

    /**
     * 成功数量
     */
    @Builder.Default
    private Integer successCount = 0;

    /**
     * 失败数量
     */
    @Builder.Default
    private Integer failureCount = 0;

    /**
     * 跳过数量（如重复操作）
     */
    @Builder.Default
    private Integer skippedCount = 0;

    /**
     * 失败详情 (JSON): [{itemId, itemName, errorCode, errorMessage}]
     */
    private String failureDetails;

    /**
     * 请求参数（完整输入） (JSON)
     */
    private String requestParameters;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 开始执行时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 预计完成时间
     */
    private LocalDateTime estimatedCompletion;

    /**
     * 创建用户ID
     */
    private Long createdBy;

    /**
     * 创建用户名（冗余）
     */
    private String createdByName;

    /**
     * 重试次数
     */
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * 最大重试次数
     */
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * 最后一次错误信息
     */
    private String lastError;

    /**
     * 执行摘要 (JSON): {duration, avgItemTime, peakMemory}
     */
    private String resultSummary;

    /**
     * 详细结果文件URL（大批量任务）
     */
    private String resultFileUrl;

    /**
     * 进度百分比（自动计算）
     */
    @TableField(exist = false)
    private BigDecimal progressPercentage;

    /**
     * 计算进度百分比
     */
    public BigDecimal getProgressPercentage() {
        if (totalItems == null || totalItems == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(processedItems)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalItems), 2, BigDecimal.ROUND_HALF_UP);
    }
}
