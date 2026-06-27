package com.school.management.infrastructure.persistence.event;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件通知分发失败死信队列 (区块3-B)。
 * 派发失败时写一行 (event_id + 错误), 由 FailedEventRetryJob 定时按 event_id 重载事件重试。
 */
@Data
@TableName("failed_event_notifications")
public class FailedEventNotificationPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 待重试的 entity_events.id */
    private Long eventId;

    private String errorMessage;

    private Integer retryCount;

    /** PENDING / RESOLVED / EXHAUSTED */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime lastRetryAt;

    private Long tenantId;
}
