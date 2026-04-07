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
    // 新增: 主体/分类/模块
    private String subjectType;
    private Long subjectId;
    private String subjectName;
    private String eventCategory;
    private String sourceModule;
    private Long eventId;

    private Integer isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    /**
     * 标记为已读
     */
    public MsgNotification markRead() {
        return MsgNotification.builder()
                .id(this.id).tenantId(this.tenantId).userId(this.userId)
                .title(this.title).content(this.content).msgType(this.msgType)
                .sourceEventType(this.sourceEventType)
                .sourceRefType(this.sourceRefType).sourceRefId(this.sourceRefId)
                .subjectType(this.subjectType).subjectId(this.subjectId).subjectName(this.subjectName)
                .eventCategory(this.eventCategory).sourceModule(this.sourceModule).eventId(this.eventId)
                .isRead(1).readAt(LocalDateTime.now()).createdAt(this.createdAt)
                .build();
    }

    /**
     * 工厂方法：从实体事件创建通知（完整版）
     */
    public static MsgNotification createFromEvent(Long tenantId, Long userId,
                                                   String title, String content,
                                                   String sourceEventType,
                                                   String sourceRefType, Long sourceRefId,
                                                   String subjectType, Long subjectId, String subjectName,
                                                   String eventCategory, String sourceModule, Long eventId) {
        return MsgNotification.builder()
                .tenantId(tenantId != null ? tenantId : 0L)
                .userId(userId)
                .title(title).content(content)
                .msgType("EVENT")
                .sourceEventType(sourceEventType)
                .sourceRefType(sourceRefType).sourceRefId(sourceRefId)
                .subjectType(subjectType).subjectId(subjectId).subjectName(subjectName)
                .eventCategory(eventCategory).sourceModule(sourceModule).eventId(eventId)
                .isRead(0).createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 工厂方法：创建手动/系统通知（简版）
     */
    public static MsgNotification create(Long tenantId, Long userId,
                                          String title, String content,
                                          String msgType, String sourceEventType,
                                          String sourceRefType, Long sourceRefId) {
        return MsgNotification.builder()
                .tenantId(tenantId != null ? tenantId : 0L)
                .userId(userId)
                .title(title).content(content)
                .msgType(msgType)
                .sourceEventType(sourceEventType)
                .sourceRefType(sourceRefType).sourceRefId(sourceRefId)
                .isRead(0).createdAt(LocalDateTime.now())
                .build();
    }
}
