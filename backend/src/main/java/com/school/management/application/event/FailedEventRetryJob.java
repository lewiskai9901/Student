package com.school.management.application.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.application.message.MessageDispatcher;
import com.school.management.domain.event.repository.EntityEventRepository;
import com.school.management.infrastructure.persistence.event.FailedEventNotificationMapper;
import com.school.management.infrastructure.persistence.event.FailedEventNotificationPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件通知死信队列重试任务 (区块3-B)。
 *
 * <p>{@link EntityEventDispatchListener} 分发失败时写 failed_event_notifications (PENDING)。本任务定时
 * 捞 PENDING 且 retry_count &lt; MAX 的行, 按 event_id 重载 {@code EntityEvent} 重新 dispatch:
 * 成功→RESOLVED; 失败→retry_count+1, 满 MAX→EXHAUSTED; 事件已不存在→EXHAUSTED。
 *
 * <p>跑在后台线程 (无 UserContext) → 数据权限拦截器 fail-open 全量, 与其它 @Scheduled 任务一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailedEventRetryJob {

    private static final int MAX_RETRY = 5;
    private static final int BATCH = 50;

    private final FailedEventNotificationMapper failedEventMapper;
    private final EntityEventRepository entityEventRepository;
    private final MessageDispatcher messageDispatcher;

    /** 默认每 5 分钟一轮; 启动后延迟 1 分钟首跑。 */
    @Scheduled(fixedDelayString = "${event.dlq.retry-interval-ms:300000}", initialDelayString = "60000")
    public void retryFailedNotifications() {
        List<FailedEventNotificationPO> pending = failedEventMapper.selectList(
                new LambdaQueryWrapper<FailedEventNotificationPO>()
                        .eq(FailedEventNotificationPO::getStatus, "PENDING")
                        .lt(FailedEventNotificationPO::getRetryCount, MAX_RETRY)
                        .orderByAsc(FailedEventNotificationPO::getCreatedAt)
                        .last("LIMIT " + BATCH));
        if (pending.isEmpty()) {
            return;
        }
        log.info("[死信重试] 本轮处理 {} 条待重试事件通知", pending.size());
        int resolved = 0, exhausted = 0, retried = 0;
        for (FailedEventNotificationPO po : pending) {
            var eventOpt = entityEventRepository.findById(po.getEventId());
            if (eventOpt.isEmpty()) {
                po.setStatus("EXHAUSTED");
                po.setErrorMessage("源事件已不存在 (eventId=" + po.getEventId() + ")");
                po.setLastRetryAt(LocalDateTime.now());
                failedEventMapper.updateById(po);
                exhausted++;
                continue;
            }
            try {
                messageDispatcher.dispatch(eventOpt.get());
                po.setStatus("RESOLVED");
                resolved++;
            } catch (Exception e) {
                po.setRetryCount(po.getRetryCount() + 1);
                String msg = e.getMessage();
                po.setErrorMessage(msg != null && msg.length() > 2000 ? msg.substring(0, 2000) : msg);
                if (po.getRetryCount() >= MAX_RETRY) {
                    po.setStatus("EXHAUSTED");
                    exhausted++;
                    log.error("[死信重试] 事件 {} 重试达上限 {} 次, 标记 EXHAUSTED", po.getEventId(), MAX_RETRY, e);
                } else {
                    retried++;
                }
            }
            po.setLastRetryAt(LocalDateTime.now());
            failedEventMapper.updateById(po);
        }
        log.info("[死信重试] 完成: 成功 {} / 仍待重试 {} / 放弃 {}", resolved, retried, exhausted);
    }
}
