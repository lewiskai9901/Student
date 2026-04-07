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
            log.error("[事件通知] 消息分发失败: type={}, error={}",
                    event.getEventType(), e.getMessage());
        }
    }
}
