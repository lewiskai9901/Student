package com.school.management.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 审批人分配监听器
 * 在任务创建时根据流程变量中的审批人配置动态分配审批人
 */
@Slf4j
@Component("approverAssignmentListener")
public class ApproverAssignmentListener implements TaskListener {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RuntimeService runtimeService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        String processInstanceId = delegateTask.getProcessInstanceId();

        log.info("任务创建事件: taskId={}, taskKey={}, processInstanceId={}",
                delegateTask.getId(), taskDefinitionKey, processInstanceId);

        try {
            // 从流程变量中获取审批人配置
            Object nodeConfigObj = delegateTask.getVariable("nodeConfig");
            if (nodeConfigObj == null) {
                log.debug("未找到nodeConfig流程变量，跳过审批人分配");
                return;
            }

            String nodeConfigJson;
            if (nodeConfigObj instanceof String) {
                nodeConfigJson = (String) nodeConfigObj;
            } else {
                nodeConfigJson = objectMapper.writeValueAsString(nodeConfigObj);
            }

            JsonNode nodeConfig = objectMapper.readTree(nodeConfigJson);
            JsonNode nodes = nodeConfig.get("nodes");

            if (nodes == null || !nodes.isArray()) {
                log.debug("nodeConfig中没有nodes配置");
                return;
            }

            // 查找当前任务节点的配置
            for (JsonNode node : nodes) {
                String nodeId = node.get("nodeId").asText();
                if (taskDefinitionKey.equals(nodeId)) {
                    assignApprover(delegateTask, node);
                    return;
                }
            }

            log.debug("未找到节点{}的审批人配置", taskDefinitionKey);

        } catch (Exception e) {
            log.error("分配审批人失败: taskId={}", delegateTask.getId(), e);
        }
    }

    /**
     * 根据节点配置分配审批人
     */
    private void assignApprover(DelegateTask delegateTask, JsonNode node) {
        String approverType = node.has("approverType") ? node.get("approverType").asText() : "USER";
        String nodeName = node.has("nodeName") ? node.get("nodeName").asText() : "";

        log.info("分配审批人: 节点={}, 类型={}", nodeName, approverType);

        switch (approverType) {
            case "USER":
                // 指定用户
                if (node.has("approverUserId") && !node.get("approverUserId").isNull()) {
                    String userId = node.get("approverUserId").asText();
                    delegateTask.setAssignee(userId);
                    log.info("设置审批人为指定用户: userId={}", userId);
                }
                break;

            case "ROLE":
                // 指定角色 - 设置候选组
                if (node.has("approverRoleCode") && !node.get("approverRoleCode").isNull()) {
                    String roleCode = node.get("approverRoleCode").asText();
                    delegateTask.addCandidateGroup(roleCode);
                    log.info("设置审批人为角色候选组: roleCode={}", roleCode);
                }
                break;

            case "DEPARTMENT_LEADER":
                // 部门负责人 - 需要根据任务创建人的部门查找负责人
                Integer departmentLevel = node.has("departmentLevel") ? node.get("departmentLevel").asInt(1) : 1;
                Object departmentLeaderId = delegateTask.getVariable("departmentLeader_" + departmentLevel);
                if (departmentLeaderId != null) {
                    delegateTask.setAssignee(departmentLeaderId.toString());
                    log.info("设置审批人为部门负责人: level={}, userId={}", departmentLevel, departmentLeaderId);
                }
                break;

            case "TASK_CREATOR":
                // 任务创建人
                Object assignerId = delegateTask.getVariable("assignerId");
                if (assignerId != null) {
                    delegateTask.setAssignee(assignerId.toString());
                    log.info("设置审批人为任务创建人: userId={}", assignerId);
                }
                break;

            default:
                log.warn("未知的审批人类型: {}", approverType);
        }
    }
}
