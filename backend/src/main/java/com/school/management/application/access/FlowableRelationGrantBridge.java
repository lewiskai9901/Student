package com.school.management.application.access;

import com.school.management.domain.access.model.valueobject.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

/**
 * Flowable serviceTask 桥接器 — 当 access-relation-approval 流程审批通过后,
 * 由此组件调用 AccessRelationService.forceGrant 真正落库.
 */
@Slf4j
@Component("flowableRelationGrantBridge")
@RequiredArgsConstructor
public class FlowableRelationGrantBridge {

    private final AccessRelationService accessRelationService;

    /**
     * BPMN serviceTask flowable:expression 调用入口.
     *
     * <p>从 BPMN execution 上读取 form variables 转 GrantRequest, 走 forceGrant 不再判审批.
     */
    public void grant(DelegateExecution execution) {
        AccessRelationService.GrantRequest r = AccessRelationService.GrantRequest.of(
            (String) execution.getVariable("subjectType"),
            ((Number) execution.getVariable("subjectId")).longValue(),
            (String) execution.getVariable("relation"),
            (String) execution.getVariable("resourceType"),
            ((Number) execution.getVariable("resourceId")).longValue()
        );
        r.accessLevel = AccessLevel.parse((String) execution.getVariable("accessLevel"));
        Long requesterId = ((Number) execution.getVariable("requesterId")).longValue();
        r.grantedBy = requesterId;
        r.tenantId = 1L;

        Long newId = accessRelationService.forceGrant(r);
        execution.setVariable("grantedRelationId", newId);
        log.info("[FlowableBridge] grant 通过流程触发完成 — relation_id={} relation={}",
            newId, r.relation);
    }
}
