package com.school.management.application.workflow;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * 示例 task lifecycle listener — 任务创建/完成/转办时记日志.
 *
 * <p>BPMN 里使用:
 * <pre>
 *   &lt;userTask id="approve" flowable:assignee="${assignee}"&gt;
 *     &lt;extensionElements&gt;
 *       &lt;flowable:taskListener event="create" delegateExpression="${leaveApprovalListener}"/&gt;
 *     &lt;/extensionElements&gt;
 *   &lt;/userTask&gt;
 * </pre>
 *
 * <p>真业务 listener 应实现具体功能,如发邮件/写审计/触发推送. 此处仅日志做契约示范.
 */
@Slf4j
@Component("leaveApprovalListener")
public class LeaveApprovalSampleListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        log.info("[Listener:{}] task {} ({}) — assignee={} processInstance={}",
            delegateTask.getEventName(),
            delegateTask.getId(),
            delegateTask.getName(),
            delegateTask.getAssignee(),
            delegateTask.getProcessInstanceId());
    }
}
