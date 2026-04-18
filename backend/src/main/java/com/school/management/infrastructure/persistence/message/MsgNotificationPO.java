package com.school.management.infrastructure.persistence.message;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息持久化对象
 */
@Data
@TableName("msg_notifications")
public class MsgNotificationPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long userId;
    private String title;
    private String content;
    private String msgType;
    private String sourceEventType;
    private String sourceRefType;
    private Long sourceRefId;
    private String subjectType;
    private Long subjectId;
    private String subjectName;
    private String eventCategory;
    private String sourceModule;
    private Long eventId;
    private Integer isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    // P1-4 发送状态追踪（为多通道 & 失败重试预留）
    private String sendStatus;      // PENDING / SENT / FAILED
    private Integer retryCount;
    private String lastError;
    private LocalDateTime sentAt;

    @TableLogic
    private Integer deleted;
}
