package com.school.management.application.access;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 关系授权审批 Flowable 引擎适配 (Phase 6).
 *
 * <p>对偶 RelationApprovalService 的 INLINE 模式: 用 Flowable 流程引擎驱动审批.
 * 启用条件: application.yml 设 access.approval.engine=FLOWABLE
 *
 * <p>仅当 Flowable RuntimeService bean 存在时启用 (dev profile exclude 时不工作).
 */
@Slf4j
@Service
@ConditionalOnBean(RuntimeService.class)
@RequiredArgsConstructor
public class RelationApprovalWorkflowService {

    private final RuntimeService runtimeService;

    /**
     * 启动审批流, 返回 processInstance id (string).
     */
    public String startApproval(AccessRelationService.GrantRequest r) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("resourceType", r.resourceType);
        vars.put("resourceId", r.resourceId);
        vars.put("relation", r.relation);
        vars.put("subjectType", r.subjectType);
        vars.put("subjectId", r.subjectId);
        vars.put("requesterId", r.grantedBy);
        vars.put("accessLevel", r.accessLevel != null ? r.accessLevel.name() : "FULL");

        String businessKey = String.format("rel-grant-%s-%d-%s-%d",
            r.resourceType, r.resourceId, r.subjectType, r.subjectId);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey(
            "access_relation_approval", businessKey, vars);
        log.info("[ApprovalWorkflow] 启动 access_relation_approval 流程 pi={} businessKey={}",
            pi.getId(), businessKey);
        return pi.getId();
    }
}
