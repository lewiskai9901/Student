package com.school.management.application.message;

import com.school.management.common.PageResult;
import com.school.management.domain.message.model.MsgNotification;
import com.school.management.domain.message.repository.MsgNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户消息操作应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MsgNotificationService {

    private final MsgNotificationRepository notificationRepository;

    /**
     * 分页查询当前用户的消息
     */
    public PageResult<MsgNotification> getMyNotifications(Long userId, Boolean isRead, int page, int size) {
        List<MsgNotification> records = notificationRepository.findByUserId(userId, isRead, page, size);
        long total = notificationRepository.countTotal(userId, isRead);
        return PageResult.of(records, total, page, size);
    }

    /**
     * 获取未读消息数
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnread(userId);
    }

    /**
     * 标记单条消息已读（校验归属）
     */
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (!n.getUserId().equals(userId)) {
                throw new IllegalArgumentException("无权操作此消息");
            }
            notificationRepository.markRead(notificationId);
        });
    }

    /**
     * 全部标记已读
     */
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }
}
