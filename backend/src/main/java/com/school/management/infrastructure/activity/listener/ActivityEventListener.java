package com.school.management.infrastructure.activity.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.infrastructure.activity.ActivityEvent;
import com.school.management.infrastructure.activity.impl.ActivityEventMapper;
import com.school.management.infrastructure.activity.impl.ActivityEventPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * 活动事件持久化监听器
 * 异步 + 事务提交后执行，确保业务操作不受审计日志写入影响
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventListener {

    private final ActivityEventMapper activityEventMapper;
    private final ObjectMapper objectMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleActivityEvent(ActivityEvent event) {
        try {
            ActivityEventPO po = toPO(event);
            activityEventMapper.insert(po);
            log.debug("活动事件已持久化: module={}, action={}, resourceType={}, resourceId={}",
                    event.getModule(), event.getAction(), event.getResourceType(), event.getResourceId());
        } catch (Exception e) {
            log.error("持久化活动事件失败: module={}, action={}, resourceType={}, resourceId={}, error={}",
                    event.getModule(), event.getAction(), event.getResourceType(), event.getResourceId(),
                    e.getMessage(), e);
        }
    }

    private ActivityEventPO toPO(ActivityEvent event) {
        ActivityEventPO po = new ActivityEventPO();
        po.setRequestId(event.getRequestId());
        po.setResourceType(event.getResourceType());
        po.setResourceId(event.getResourceId());
        po.setResourceName(event.getResourceName());
        po.setAction(event.getAction());
        po.setActionLabel(event.getActionLabel());
        po.setResult(event.getResult() != null ? event.getResult() : "SUCCESS");
        po.setErrorMessage(event.getErrorMessage());
        po.setUserId(event.getUserId());
        po.setUserName(event.getUserName());
        po.setSourceIp(event.getSourceIp());
        po.setUserAgent(event.getUserAgent());
        po.setApiEndpoint(event.getApiEndpoint());
        po.setHttpMethod(event.getHttpMethod());
        po.setReason(event.getReason());
        po.setModule(event.getModule());
        po.setOccurredAt(event.getOccurredAt() != null ? event.getOccurredAt() : LocalDateTime.now());
        po.setCreatedAt(LocalDateTime.now());

        // JSON fields
        if (event.getChangedFields() != null && !event.getChangedFields().isEmpty()) {
            try {
                po.setChangedFields(objectMapper.writeValueAsString(event.getChangedFields()));
            } catch (JsonProcessingException e) {
                log.warn("序列化 changedFields 失败", e);
            }
        }
        po.setBeforeSnapshot(event.getBeforeSnapshot());
        po.setAfterSnapshot(event.getAfterSnapshot());

        if (event.getTags() != null && !event.getTags().isEmpty()) {
            try {
                po.setTags(objectMapper.writeValueAsString(event.getTags()));
            } catch (JsonProcessingException e) {
                log.warn("序列化 tags 失败", e);
            }
        }

        return po;
    }
}
