package com.school.management.domain.message.repository;

import com.school.management.domain.message.model.MsgNotification;

import java.util.List;
import java.util.Optional;

/**
 * 站内消息仓储接口
 */
public interface MsgNotificationRepository {

    MsgNotification save(MsgNotification notification);

    Optional<MsgNotification> findById(Long id);

    List<MsgNotification> findByUserId(Long userId, Boolean isRead, int page, int size);

    long countTotal(Long userId, Boolean isRead);

    long countUnread(Long userId);

    void markRead(Long id);

    void markAllRead(Long userId);

    void softDelete(Long id, Long userId);

    List<MsgNotification> findByUserId(Long userId, Boolean isRead, String msgType, int page, int size);

    long countTotal(Long userId, Boolean isRead, String msgType);
}
