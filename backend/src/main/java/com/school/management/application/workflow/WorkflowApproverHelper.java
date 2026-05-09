package com.school.management.application.workflow;

import com.school.management.application.access.ApproverResolver;
import com.school.management.domain.access.model.entity.PendingRelationApproval;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 暴露给 BPMN 表达式的审批人查找助手. 在 .bpmn20.xml 里这样用:
 *
 * <pre>
 *   &lt;userTask id="approve" name="审批"
 *             flowable:assignee="${workflowApprover.findForRelation(execution, 'admin', 'org_unit', orgUnitId)}"/&gt;
 * </pre>
 *
 * Flowable runtime 会注入 execution context 调本类方法.
 *
 * <p>本类不直接持有业务模型 (PendingRelationApproval), 只构造一个 light view 转发给
 * {@link ApproverResolver}. 这样审批流的 SPI 能跨业务复用 (访问关系/请假/报销/调课/...).
 */
@Slf4j
@Component("workflowApprover")
@RequiredArgsConstructor
public class WorkflowApproverHelper {

    private final ApproverResolver approverResolver;

    /**
     * 通用查找:按业务类型 (relation 字符串) + 资源信息 + 申请人.
     *
     * @param relationCode  业务关系码,如 "admin"/"teaches"/"leave_apply"
     * @param resourceType  "org_unit" / "place" / "user" / 自定义业务实体
     * @param resourceId    资源 id
     * @param requesterId   申请人 user id (BPMN 里通常是 starter)
     * @return 第一个匹配的审批人 user id 字符串, 给 flowable:assignee 用; null/empty 时返回空字符串(BPMN 会卡在任务,需人工干预)
     */
    public String findFirstApprover(String relationCode, String resourceType,
                                     Long resourceId, Long requesterId) {
        List<Long> approvers = findApprovers(relationCode, resourceType, resourceId, requesterId);
        if (approvers.isEmpty()) {
            log.warn("[WorkflowApprover] 未找到审批人 — relation={} resource={}:{} requester={}",
                relationCode, resourceType, resourceId, requesterId);
            return "";
        }
        return String.valueOf(approvers.get(0));
    }

    /**
     * 多审批人(用于会签 multiInstance task,Flowable 把 collection 拆成多个 task).
     */
    public List<Long> findApprovers(String relationCode, String resourceType,
                                      Long resourceId, Long requesterId) {
        PendingRelationApproval mock = PendingRelationApproval.builder()
            .relation(relationCode)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .subjectType("user")
            .subjectId(requesterId)
            .requestedBy(requesterId)
            .status(PendingRelationApproval.Status.PENDING)
            .tenantId(1L)
            .build();
        return approverResolver.resolveApprovers(mock);
    }
}
