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

    // P1-4 发送状态（为多通道 + 失败重试做准备；站内信默认 SENT）
    private String sendStatus;      // PENDING / SENT / FAILED
    private Integer retryCount;
    private String lastError;
    private LocalDateTime sentAt;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";

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
                .sendStatus(this.sendStatus).retryCount(this.retryCount)
                .lastError(this.lastError).sentAt(this.sentAt)
                .build();
    }

    /**
     * 工厂方法：从实体事件创建通知（完整版）
     *
     * tenantId 必填：调用方必须从安全上下文或事件自身携带的租户信息传入，
     * 避免通知被错误地归入默认租户（0）而跨租户泄露。
     *
     * TODO 多租户：当前系统仍运行于单租户模式（tenant_id 默认 0L），
     * 但此处强制非空契约已就位，后续启用多租户时无需再改签名。
     */
    public static MsgNotification createFromEvent(Long tenantId, Long userId,
                                                   String title, String content,
                                                   String sourceEventType,
                                                   String sourceRefType, Long sourceRefId,
                                                   String subjectType, Long subjectId, String subjectName,
                                                   String eventCategory, String sourceModule, Long eventId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId 不能为空，必须从安全上下文或事件租户信息获取");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        return MsgNotification.builder()
                .tenantId(tenantId)
                .userId(userId)
                .title(title).content(content)
                .msgType("EVENT")
                .sourceEventType(sourceEventType)
                .sourceRefType(sourceRefType).sourceRefId(sourceRefId)
                .subjectType(subjectType).subjectId(subjectId).subjectName(subjectName)
                .eventCategory(eventCategory).sourceModule(sourceModule).eventId(eventId)
                .isRead(0).createdAt(now)
                // 站内信落库即送达；后续多通道场景由 ChannelDispatcher 覆盖为 PENDING→SENT
                .sendStatus(STATUS_SENT).retryCount(0).sentAt(now)
                .build();
    }

    /**
     * 工厂方法：创建手动/系统通知（简版）
     *
     * tenantId 必填：同 {@link #createFromEvent}。
     */
    public static MsgNotification create(Long tenantId, Long userId,
                                          String title, String content,
                                          String msgType, String sourceEventType,
                                          String sourceRefType, Long sourceRefId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId 不能为空，必须从安全上下文获取");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        return MsgNotification.builder()
                .tenantId(tenantId)
                .userId(userId)
                .title(title).content(content)
                .msgType(msgType)
                .sourceEventType(sourceEventType)
                .sourceRefType(sourceRefType).sourceRefId(sourceRefId)
                .isRead(0).createdAt(now)
                .sendStatus(STATUS_SENT).retryCount(0).sentAt(now)
                .build();
    }
}
