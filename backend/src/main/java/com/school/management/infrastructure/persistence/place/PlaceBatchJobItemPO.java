package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 批量任务项明细持久化对象
 * 对应表: place_batch_job_items
 * 用于超大批量任务的断点续传
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("place_batch_job_items")
public class PlaceBatchJobItemPO {

    /**
     * 明细ID
     */
    @TableId(type = IdType.AUTO)
    private Long itemId;

    /**
     * 所属任务ID
     */
    private String jobId;

    /**
     * 项目索引（处理顺序）
     */
    private Integer itemIndex;

    /**
     * 资源类型: PLACE/OCCUPANT
     */
    private String resourceType;

    /**
     * 资源ID
     */
    private Long resourceId;

    /**
     * 资源名称（冗余）
     */
    private String resourceName;

    /**
     * 项目状态: PENDING/SUCCESS/FAILED/SKIPPED
     */
    @Builder.Default
    private String itemStatus = "PENDING";

    /**
     * 操作数据 (JSON): {orgUnitId: 123}
     */
    private String operationData;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 处理时间
     */
    private LocalDateTime processedAt;

    /**
     * 重试次数
     */
    @Builder.Default
    private Integer retryCount = 0;
}
