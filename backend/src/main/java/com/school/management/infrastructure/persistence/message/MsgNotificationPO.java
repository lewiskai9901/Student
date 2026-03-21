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
    private Integer isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
