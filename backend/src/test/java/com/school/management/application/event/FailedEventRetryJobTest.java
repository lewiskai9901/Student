package com.school.management.application.event;

import com.school.management.application.message.MessageDispatcher;
import com.school.management.domain.event.model.EntityEvent;
import com.school.management.domain.event.repository.EntityEventRepository;
import com.school.management.infrastructure.persistence.event.FailedEventNotificationMapper;
import com.school.management.infrastructure.persistence.event.FailedEventNotificationPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 区块3-B 验收: 死信队列重试逻辑 (确定性, 不依赖 @Scheduled 计时)。
 * 验证三条路径: 重载成功→RESOLVED / 源事件不存在→EXHAUSTED / 重试达上限→EXHAUSTED。
 */
class FailedEventRetryJobTest {

    private final FailedEventNotificationMapper mapper = mock(FailedEventNotificationMapper.class);
    private final EntityEventRepository eventRepo = mock(EntityEventRepository.class);
    private final MessageDispatcher dispatcher = mock(MessageDispatcher.class);
    private final FailedEventRetryJob job = new FailedEventRetryJob(mapper, eventRepo, dispatcher);

    private FailedEventNotificationPO row(long id, long eventId, int retryCount) {
        FailedEventNotificationPO po = new FailedEventNotificationPO();
        po.setId(id);
        po.setEventId(eventId);
        po.setRetryCount(retryCount);
        po.setStatus("PENDING");
        return po;
    }

    @Test
    @DisplayName("重载成功→RESOLVED; 源事件不存在→EXHAUSTED")
    void resolvesOnSuccessAndExhaustsWhenEventMissing() {
        FailedEventNotificationPO ok = row(1L, 100L, 0);
        FailedEventNotificationPO missing = row(2L, 999L, 0);
        when(mapper.selectList(any())).thenReturn(List.of(ok, missing));
        when(eventRepo.findById(100L)).thenReturn(Optional.of(EntityEvent.builder().id(100L).build()));
        when(eventRepo.findById(999L)).thenReturn(Optional.empty());

        job.retryFailedNotifications();

        assertThat(ok.getStatus()).isEqualTo("RESOLVED");
        assertThat(missing.getStatus()).isEqualTo("EXHAUSTED");
        verify(dispatcher, times(1)).dispatch(any());          // 只对存在的事件 dispatch
        verify(mapper, times(2)).updateById(any(FailedEventNotificationPO.class));
    }

    @Test
    @DisplayName("dispatch 抛错: 未达上限→retry_count+1 仍 PENDING; 达上限→EXHAUSTED")
    void incrementsThenExhaustsOnRepeatedFailure() {
        FailedEventNotificationPO young = row(1L, 100L, 0);   // 0→1, 仍 PENDING
        FailedEventNotificationPO old = row(2L, 100L, 4);     // 4→5, 达上限 MAX_RETRY=5
        when(mapper.selectList(any())).thenReturn(List.of(young, old));
        when(eventRepo.findById(100L)).thenReturn(Optional.of(EntityEvent.builder().id(100L).build()));
        doThrow(new RuntimeException("分发挂了")).when(dispatcher).dispatch(any());

        job.retryFailedNotifications();

        assertThat(young.getRetryCount()).isEqualTo(1);
        assertThat(young.getStatus()).isEqualTo("PENDING");
        assertThat(old.getRetryCount()).isEqualTo(5);
        assertThat(old.getStatus()).isEqualTo("EXHAUSTED");
    }
}
