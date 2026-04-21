package com.school.management.application.message;

import com.school.management.common.PageResult;
import com.school.management.domain.message.model.MsgNotification;
import com.school.management.domain.message.repository.MsgNotificationRepository;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户消息操作应用服务
 *
 * 多租户: 所有读改均强制带当前 tenantId (从 TenantContextHolder 读取),
 * 确保不同租户下的同名 userId 不会相互看到对方的消息。
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
        Long tenantId = TenantContextHolder.getTenantId();
        List<MsgNotification> records = notificationRepository.findByUserId(tenantId, userId, isRead, page, size);
        long total = notificationRepository.countTotal(tenantId, userId, isRead);
        return PageResult.of(records, total, page, size);
    }

    /**
     * 分页查询当前用户的消息（带消息类型过滤）
     */
    public PageResult<MsgNotification> getMyNotifications(Long userId, Boolean isRead, String msgType, int page, int size) {
        Long tenantId = TenantContextHolder.getTenantId();
        List<MsgNotification> records = notificationRepository.findByUserId(tenantId, userId, isRead, msgType, page, size);
        long total = notificationRepository.countTotal(tenantId, userId, isRead, msgType);
        return PageResult.of(records, total, page, size);
    }

    /**
     * 获取未读消息数
     */
    public long getUnreadCount(Long userId) {
        Long tenantId = TenantContextHolder.getTenantId();
        return notificationRepository.countUnread(tenantId, userId);
    }

    /**
     * 标记单条消息已读（校验归属）
     */
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Long tenantId = TenantContextHolder.getTenantId();
        notificationRepository.findById(tenantId, notificationId).ifPresent(n -> {
            if (!n.getUserId().equals(userId)) {
                throw new IllegalArgumentException("无权操作此消息");
            }
            notificationRepository.markRead(tenantId, notificationId);
        });
    }

    /**
     * 全部标记已读
     */
    @Transactional
    public void markAllRead(Long userId) {
        Long tenantId = TenantContextHolder.getTenantId();
        notificationRepository.markAllRead(tenantId, userId);
    }

    /**
     * 软删除消息（校验归属）
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Long tenantId = TenantContextHolder.getTenantId();
        notificationRepository.findById(tenantId, messageId).ifPresent(n -> {
            if (!n.getUserId().equals(userId)) {
                throw new IllegalArgumentException("无权操作此消息");
            }
            notificationRepository.softDelete(tenantId, messageId, userId);
        });
    }
}
