package com.school.management.infrastructure.persistence.message;

import com.school.management.domain.message.model.MsgNotification;
import com.school.management.domain.message.repository.MsgNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 站内消息仓储实现
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
    public Optional<MsgNotification> findById(Long id) {
        MsgNotificationPO po = notificationMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<MsgNotification> findByUserId(Long userId, Boolean isRead, int page, int size) {
        Integer isReadInt = isRead != null ? (isRead ? 1 : 0) : null;
        int offset = (page - 1) * size;
        return notificationMapper.findByUserId(userId, isReadInt, offset, size)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countTotal(Long userId, Boolean isRead) {
        Integer isReadInt = isRead != null ? (isRead ? 1 : 0) : null;
        return notificationMapper.countByUserId(userId, isReadInt);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    public void markRead(Long id) {
        notificationMapper.markRead(id);
    }

    @Override
    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
    }

    private MsgNotification toDomain(MsgNotificationPO po) {
        return MsgNotification.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .userId(po.getUserId())
                .title(po.getTitle())
                .content(po.getContent())
                .msgType(po.getMsgType())
                .sourceEventType(po.getSourceEventType())
                .sourceRefType(po.getSourceRefType())
                .sourceRefId(po.getSourceRefId())
                .isRead(po.getIsRead())
                .readAt(po.getReadAt())
                .createdAt(po.getCreatedAt())
                .build();
    }

    private MsgNotificationPO toPO(MsgNotification notification) {
        MsgNotificationPO po = new MsgNotificationPO();
        po.setId(notification.getId());
        po.setTenantId(notification.getTenantId() != null ? notification.getTenantId() : 0L);
        po.setUserId(notification.getUserId());
        po.setTitle(notification.getTitle());
        po.setContent(notification.getContent());
        po.setMsgType(notification.getMsgType());
        po.setSourceEventType(notification.getSourceEventType());
        po.setSourceRefType(notification.getSourceRefType());
        po.setSourceRefId(notification.getSourceRefId());
        po.setIsRead(notification.getIsRead() != null ? notification.getIsRead() : 0);
        po.setReadAt(notification.getReadAt());
        po.setCreatedAt(notification.getCreatedAt());
        return po;
    }
}
