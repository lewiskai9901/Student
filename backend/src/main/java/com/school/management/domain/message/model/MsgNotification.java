package com.school.management.domain.message.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 站内消息领域模型
 */
@Getter
@Builder
public class MsgNotification {

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

    /**
     * 标记为已读
     */
    public MsgNotification markRead() {
        return MsgNotification.builder()
                .id(this.id)
                .tenantId(this.tenantId)
                .userId(this.userId)
                .title(this.title)
                .content(this.content)
                .msgType(this.msgType)
                .sourceEventType(this.sourceEventType)
                .sourceRefType(this.sourceRefType)
                .sourceRefId(this.sourceRefId)
                .isRead(1)
                .readAt(LocalDateTime.now())
                .createdAt(this.createdAt)
                .build();
    }

    /**
     * 工厂方法：创建新通知
     */
    public static MsgNotification create(Long tenantId, Long userId, String title, String content,
                                          String msgType, String sourceEventType,
                                          String sourceRefType, Long sourceRefId) {
        return MsgNotification.builder()
                .tenantId(tenantId != null ? tenantId : 0L)
                .userId(userId)
                .title(title)
                .content(content)
                .msgType(msgType)
                .sourceEventType(sourceEventType)
                .sourceRefType(sourceRefType)
                .sourceRefId(sourceRefId)
                .isRead(0)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
