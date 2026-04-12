package com.school.management.application.event;

import com.school.management.application.message.MessageDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * 实体事件分发监听器 (通用)
 *
 * 在事务提交后异步执行，将实体事件转化为站内通知。
 * 使用 @TransactionalEventListener(AFTER_COMMIT) 确保:
 *   - 事务回滚时不发通知
 *   - 事务提交后才匹配订阅规则并分发消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityEventDispatchListener {

    private final MessageDispatcher messageDispatcher;

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
            // TODO 死信队列：待引入 failed_event_notifications 表或 Redis list 后，
            //   在此把 event 快照与异常信息写入，由定时任务重试。
        }
    }
}
