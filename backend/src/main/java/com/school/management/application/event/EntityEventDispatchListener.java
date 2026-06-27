package com.school.management.application.event;

import com.school.management.application.message.MessageDispatcher;
import com.school.management.infrastructure.persistence.event.FailedEventNotificationMapper;
import com.school.management.infrastructure.persistence.event.FailedEventNotificationPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDateTime;

/**
 * 实体事件分发监听器 (通用)
 *
 * 在事务提交后异步执行，将实体事件转化为站内通知。
 * 使用 @TransactionalEventListener(AFTER_COMMIT) 确保:
 *   - 事务回滚时不发通知
 *   - 事务提交后才匹配订阅规则并分发消息
 *
 * 分发失败 → 写 failed_event_notifications 死信队列, 由 {@link FailedEventRetryJob} 定时重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityEventDispatchListener {

    private final MessageDispatcher messageDispatcher;
    private final FailedEventNotificationMapper failedEventMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEntityEventCreated(EntityEventCreatedNotification notification) {
        var event = notification.getEvent();
        log.info("[事件通知] 收到事件: type={}, category={}, subject={}:{}/{}",
                event.getEventType(), event.getEventCategory(),
                event.getSubjectType(), event.getSubjectId(), event.getSubjectName());

        try {
            messageDispatcher.dispatch(event);
        } catch (Exception e) {
            // 异常必须带上完整堆栈和上下文，便于排查分发失败原因。
            // AFTER_COMMIT + @Async 下抛出异常只会被 AsyncUncaughtExceptionHandler 捕获，
            // 这里改用 ERROR 级别 + 完整堆栈（传入 Throwable 而非仅 message）。
            log.error("[事件通知] 消息分发失败: eventId={}, type={}, category={}, subject={}:{}/{}, error={}",
                    event.getId(),
                    event.getEventType(),
                    event.getEventCategory(),
                    event.getSubjectType(),
                    event.getSubjectId(),
                    event.getSubjectName(),
                    e.getMessage(),
                    e);
            enqueueDeadLetter(event.getId(), event.getTenantId(), e);
        }
    }

    /** 把分发失败的事件写入死信队列 (event_id 引用, 重试时按 id 重载)。入队本身失败只记日志, 不抛。 */
    private void enqueueDeadLetter(Long eventId, Long tenantId, Exception cause) {
        if (eventId == null) {
            log.warn("[事件通知] 事件无 id, 无法入死信队列 (跳过重试): {}", cause.getMessage());
            return;
        }
        try {
            FailedEventNotificationPO po = new FailedEventNotificationPO();
            po.setEventId(eventId);
            po.setTenantId(tenantId != null ? tenantId : 1L);
            String msg = cause.getMessage();
            po.setErrorMessage(msg != null && msg.length() > 2000 ? msg.substring(0, 2000) : msg);
            po.setRetryCount(0);
            po.setStatus("PENDING");
            po.setCreatedAt(LocalDateTime.now());
            failedEventMapper.insert(po);
        } catch (Exception ex) {
            log.error("[事件通知] 写死信队列失败 (eventId={}): {}", eventId, ex.getMessage(), ex);
        }
    }
}
