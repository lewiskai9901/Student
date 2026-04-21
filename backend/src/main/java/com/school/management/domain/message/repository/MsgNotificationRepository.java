package com.school.management.domain.message.repository;

import com.school.management.domain.message.model.MsgNotification;

import java.util.List;
import java.util.Optional;

/**
 * 站内消息仓储接口
 *
 * 多租户: 所有读/改 API 均强制传 tenantId, 由应用层从 TenantContextHolder 读取,
 * 确保同一 userId 在不同租户的消息完全隔离。
 */
public interface MsgNotificationRepository {

    MsgNotification save(MsgNotification notification);

    /**
     * 批量保存（无返回实体 —— 订阅分发场景不需要回读 id）
     * @return 成功插入的行数
     */
    int saveAll(List<MsgNotification> notifications);

    /** 按 id 读取单条消息, 需租户作用域。 */
    Optional<MsgNotification> findById(Long tenantId, Long id);

    List<MsgNotification> findByUserId(Long tenantId, Long userId, Boolean isRead, int page, int size);

    long countTotal(Long tenantId, Long userId, Boolean isRead);

    long countUnread(Long tenantId, Long userId);

    void markRead(Long tenantId, Long id);

    void markAllRead(Long tenantId, Long userId);

    void softDelete(Long tenantId, Long id, Long userId);

    List<MsgNotification> findByUserId(Long tenantId, Long userId, Boolean isRead, String msgType, int page, int size);

    long countTotal(Long tenantId, Long userId, Boolean isRead, String msgType);
}
