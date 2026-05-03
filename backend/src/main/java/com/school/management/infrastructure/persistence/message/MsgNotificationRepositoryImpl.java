package com.school.management.infrastructure.persistence.message;

import com.school.management.domain.message.model.MsgNotification;
import com.school.management.domain.message.repository.MsgNotificationRepository;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 站内消息仓储实现
 *
 * tenant_id 写入策略: 实体自身若携带 tenantId 则优先使用 (分发器已从事件取值);
 * 否则回退到 TenantContextHolder 当前租户。最后仍为 null 会由 NOT NULL 约束拦截,
 * 不再悄悄落默认 0。
 */
@Repository
@RequiredArgsConstructor
public class MsgNotificationRepositoryImpl implements MsgNotificationRepository {

    private final MsgNotificationMapper notificationMapper;

    @Override
    public MsgNotification save(MsgNotification notification) {
        MsgNotificationPO po = toPO(notification);
        if (notification.getId() == null) {
            po.setCreatedAt(notification.getCreatedAt() != null ? notification.getCreatedAt() : LocalDateTime.now());
            notificationMapper.insert(po);
        } else {
            notificationMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public int saveAll(List<MsgNotification> notifications) {
        if (notifications == null || notifications.isEmpty()) return 0;
        LocalDateTime now = LocalDateTime.now();
        List<MsgNotificationPO> pos = notifications.stream()
                .map(n -> {
                    MsgNotificationPO po = toPO(n);
                    if (po.getCreatedAt() == null) po.setCreatedAt(now);
                    return po;
                })
                .collect(Collectors.toList());
        // 大集合分片插入（每批 500 条），避免 SQL 过长 / packet size 超限
        final int BATCH = 500;
        int total = 0;
        for (int i = 0; i < pos.size(); i += BATCH) {
            List<MsgNotificationPO> slice = pos.subList(i, Math.min(i + BATCH, pos.size()));
            total += notificationMapper.insertBatch(slice);
        }
        return total;
    }

    @Override
    public Optional<MsgNotification> findById(Long tenantId, Long id) {
        MsgNotificationPO po = notificationMapper.findByIdScoped(tenantId, id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<MsgNotification> findByUserId(Long tenantId, Long userId, Boolean isRead, int page, int size) {
        Integer isReadInt = isRead != null ? (isRead ? 1 : 0) : null;
        int offset = (page - 1) * size;
        return notificationMapper.findByUserId(tenantId, userId, isReadInt, offset, size)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countTotal(Long tenantId, Long userId, Boolean isRead) {
        Integer isReadInt = isRead != null ? (isRead ? 1 : 0) : null;
        return notificationMapper.countByUserId(tenantId, userId, isReadInt);
    }

    @Override
    public long countUnread(Long tenantId, Long userId) {
        return notificationMapper.countUnread(tenantId, userId);
    }

    @Override
    public void markRead(Long tenantId, Long id) {
        notificationMapper.markRead(tenantId, id);
    }

    @Override
    public void markAllRead(Long tenantId, Long userId) {
        notificationMapper.markAllRead(tenantId, userId);
    }

    @Override
    public void softDelete(Long tenantId, Long id, Long userId) {
        notificationMapper.softDelete(tenantId, id, userId);
    }

    @Override
    public List<MsgNotification> findByUserId(Long tenantId, Long userId, Boolean isRead, String msgType, int page, int size) {
        Integer isReadInt = isRead != null ? (isRead ? 1 : 0) : null;
        int offset = (page - 1) * size;
        return notificationMapper.findByUserIdWithType(tenantId, userId, isReadInt, msgType, offset, size)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countTotal(Long tenantId, Long userId, Boolean isRead, String msgType) {
        Integer isReadInt = isRead != null ? (isRead ? 1 : 0) : null;
        return notificationMapper.countByUserIdWithType(tenantId, userId, isReadInt, msgType);
    }

    private MsgNotification toDomain(MsgNotificationPO po) {
        return MsgNotification.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .receiverType(po.getReceiverType())
                .receiverId(po.getReceiverId())
                .userId(po.getUserId())
                .title(po.getTitle())
                .content(po.getContent())
                .msgType(po.getMsgType())
                .sourceEventType(po.getSourceEventType())
                .sourceRefType(po.getSourceRefType())
                .sourceRefId(po.getSourceRefId())
                .subjectType(po.getSubjectType())
                .subjectId(po.getSubjectId())
                .subjectName(po.getSubjectName())
                .eventCategory(po.getEventCategory())
                .sourceModule(po.getSourceModule())
                .eventId(po.getEventId())
                .isRead(po.getIsRead())
                .readAt(po.getReadAt())
                .createdAt(po.getCreatedAt())
                .sendStatus(po.getSendStatus())
                .retryCount(po.getRetryCount())
                .lastError(po.getLastError())
                .sentAt(po.getSentAt())
                .build();
    }

    private MsgNotificationPO toPO(MsgNotification notification) {
        MsgNotificationPO po = new MsgNotificationPO();
        po.setId(notification.getId());
        // tenant 填充优先级: 实体自带 → 当前线程上下文 → 默认 0 (多租户禁用时的回退)
        Long tenantId = notification.getTenantId();
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
        po.setTenantId(tenantId != null ? tenantId : 0L);
        // S-1: 接收人多态. 兼容旧调用 (只设 userId 不设 receiverType) 时, 默认 USER
        String rt = notification.getReceiverType();
        Long rid = notification.getReceiverId();
        if (rt == null) {
            rt = "USER";
            rid = notification.getUserId();
        }
        po.setReceiverType(rt);
        po.setReceiverId(rid != null ? rid : 0L);
        po.setUserId("USER".equals(rt) ? rid : notification.getUserId());
        po.setTitle(notification.getTitle());
        po.setContent(notification.getContent());
        po.setMsgType(notification.getMsgType());
        po.setSourceEventType(notification.getSourceEventType());
        po.setSourceRefType(notification.getSourceRefType());
        po.setSourceRefId(notification.getSourceRefId());
        po.setSubjectType(notification.getSubjectType());
        po.setSubjectId(notification.getSubjectId());
        po.setSubjectName(notification.getSubjectName());
        po.setEventCategory(notification.getEventCategory());
        po.setSourceModule(notification.getSourceModule());
        po.setEventId(notification.getEventId());
        po.setIsRead(notification.getIsRead() != null ? notification.getIsRead() : 0);
        po.setReadAt(notification.getReadAt());
        po.setCreatedAt(notification.getCreatedAt());
        // 发送状态字段；默认 SENT 兼容老调用路径
        po.setSendStatus(notification.getSendStatus() != null ? notification.getSendStatus() : MsgNotification.STATUS_SENT);
        po.setRetryCount(notification.getRetryCount() != null ? notification.getRetryCount() : 0);
        po.setLastError(notification.getLastError());
        po.setSentAt(notification.getSentAt());
        return po;
    }
}
