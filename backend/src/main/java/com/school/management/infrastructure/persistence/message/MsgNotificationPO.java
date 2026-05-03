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
    /** S-1: 接收人多态 — USER (默认) / GROUP / ROLE */
    private String receiverType;
    /** S-1: 接收人 ID — USER 时 = user_id, GROUP 时 = group_id, ROLE 时 = role_id */
    private Long receiverId;
    /** 向后兼容 — 当 receiverType=USER 时与 receiverId 同值, 其它类型时为 null */
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
