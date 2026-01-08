package com.school.management.service.task.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.exception.BusinessException;
import com.school.management.service.task.FlowableProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Flowable流程服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowableProcessServiceImpl implements FlowableProcessService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final ProcessEngine processEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deployProcess(String name, String bpmnXml) {
        log.info("部署流程定义: name={}", name);

        // 将BPMN XML转换为输入流
        InputStream bpmnStream = new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8));

        // 部署流程
        Deployment deployment = repositoryService.createDeployment()
                .name(name)
                .addInputStream(name + ".bpmn20.xml", bpmnStream)
                .deploy();

        // 获取流程定义
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

        log.info("流程部署成功: deploymentId={}, processDefinitionId={}, key={}",
                deployment.getId(), processDefinition.getId(), processDefinition.getKey());

        return processDefinition.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstance startProcess(String processDefinitionKey, String businessKey, Map<String, Object> variables) {
        log.info("启动流程实例: key={}, businessKey={}", processDefinitionKey, businessKey);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                processDefinitionKey,
                businessKey,
                variables
        );

        log.info("流程实例启动成功: processInstanceId={}", processInstance.getId());
        return processInstance;
    }

    @Override
    public List<Task> getUserTasks(String userId) {
        return taskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime()
                .desc()
                .list();
    }

    @Override
    public List<Task> getTasksByProcessInstanceId(String processInstanceId) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(String taskId, Map<String, Object> variables, String comment) {
        log.info("完成任务: taskId={}", taskId);

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        // 添加审批意见
        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), "approve", comment);
        }

        // 完成任务
        if (variables != null && !variables.isEmpty()) {
            taskService.complete(taskId, variables);
        } else {
            taskService.complete(taskId);
        }

        log.info("任务完成成功: taskId={}", taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectToNode(String taskId, String targetNodeId, String comment) {
        log.info("驳回任务到节点: taskId={}, targetNodeId={}", taskId, targetNodeId);

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        // 添加驳回意见
        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), "reject", comment);
        }

        // 获取当前活动节点
        String currentActivityId = task.getTaskDefinitionKey();

        // 获取流程定义
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());

        // 获取当前节点和目标节点
        FlowNode currentFlowNode = (FlowNode) bpmnModel.getFlowElement(currentActivityId);
        FlowNode targetFlowNode = (FlowNode) bpmnModel.getFlowElement(targetNodeId);

        if (targetFlowNode == null) {
            throw new BusinessException("目标节点不存在");
        }

        // 记录原始的出口连线
        List<SequenceFlow> originalOutgoingFlows = new ArrayList<>(currentFlowNode.getOutgoingFlows());

        // 清空当前节点的出口连线
        currentFlowNode.getOutgoingFlows().clear();

        // 创建新的连线指向目标节点
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("reject_" + System.currentTimeMillis());
        newSequenceFlow.setSourceFlowElement(currentFlowNode);
        newSequenceFlow.setTargetFlowElement(targetFlowNode);
        currentFlowNode.getOutgoingFlows().add(newSequenceFlow);

        // 完成任务，触发流转
        Map<String, Object> variables = new HashMap<>();
        variables.put("rejected", true);
        variables.put("rejectToNode", targetNodeId);
        taskService.complete(taskId, variables);

        // 恢复原始连线
        currentFlowNode.getOutgoingFlows().clear();
        currentFlowNode.getOutgoingFlows().addAll(originalOutgoingFlows);

        log.info("任务驳回成功: taskId={}, targetNodeId={}", taskId, targetNodeId);
    }

    @Override
    public ProcessInstance getProcessInstance(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateProcess(String processInstanceId, String reason) {
        log.info("终止流程实例: processInstanceId={}, reason={}", processInstanceId, reason);

        runtimeService.deleteProcessInstance(processInstanceId, reason);

        log.info("流程实例终止成功: processInstanceId={}", processInstanceId);
    }

    @Override
    public List<Map<String, String>> getRejectableNodes(String taskId) {
        List<Map<String, String>> result = new ArrayList<>();

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            return result;
        }

        // 获取历史活动节点
        List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .finished()
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        // 获取BpmnModel
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());

        Set<String> addedNodes = new HashSet<>();
        for (HistoricActivityInstance activity : historicActivities) {
            String activityId = activity.getActivityId();
            if (!addedNodes.contains(activityId)) {
                FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(activityId);
                if (flowNode != null) {
                    Map<String, String> node = new HashMap<>();
                    node.put("nodeId", activityId);
                    node.put("nodeName", flowNode.getName());
                    result.add(node);
                    addedNodes.add(activityId);
                }
            }
        }

        // 添加开始节点(提交人)
        Map<String, String> startNode = new HashMap<>();
        startNode.put("nodeId", "submitTask");
        startNode.put("nodeName", "提交人");
        result.add(0, startNode);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferTask(String taskId, String targetUserId, String reason) {
        log.info("转交任务: taskId={}, targetUserId={}", taskId, targetUserId);

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        // 添加转交意见
        if (reason != null && !reason.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), "transfer", reason);
        }

        // 转交任务
        taskService.setAssignee(taskId, targetUserId);

        log.info("任务转交成功: taskId={}, targetUserId={}", taskId, targetUserId);
    }

    @Override
    public boolean isProcessEnded(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        return processInstance == null;
    }

    @Override
    public String getProcessDiagram(String processInstanceId) {
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            String processDefinitionId;
            List<String> activeActivityIds;

            if (processInstance != null) {
                processDefinitionId = processInstance.getProcessDefinitionId();
                activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
            } else {
                // 流程已结束，从历史中获取
                var historicInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                if (historicInstance == null) {
                    return null;
                }
                processDefinitionId = historicInstance.getProcessDefinitionId();
                activeActivityIds = Collections.emptyList();
            }

            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);

            ProcessDiagramGenerator diagramGenerator = processEngine.getProcessEngineConfiguration()
                    .getProcessDiagramGenerator();

            InputStream inputStream = diagramGenerator.generateDiagram(
                    bpmnModel,
                    "png",
                    activeActivityIds,
                    Collections.emptyList(),
                    "宋体",
                    "宋体",
                    "宋体",
                    null,
                    1.0,
                    true
            );

            byte[] bytes = inputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);

        } catch (Exception e) {
            log.error("获取流程图失败: processInstanceId={}", processInstanceId, e);
            return null;
        }
    }

    @Override
    public List<Map<String, Object>> parseUserTaskNodes(String bpmnXml) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (bpmnXml == null || bpmnXml.isEmpty()) {
            log.warn("BPMN XML为空");
            return result;
        }

        log.info("开始解析BPMN XML, 长度={}", bpmnXml.length());

        try {
            // 解析BPMN XML
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

            XMLStreamReader reader = factory.createXMLStreamReader(
                    new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));

            BpmnXMLConverter converter = new BpmnXMLConverter();
            BpmnModel bpmnModel = converter.convertToBpmnModel(reader);

            // 获取主流程
            org.flowable.bpmn.model.Process process = bpmnModel.getMainProcess();
            if (process == null) {
                log.warn("BPMN中没有找到主流程");
                return result;
            }

            log.info("找到主流程: id={}, 元素数量={}", process.getId(), process.getFlowElements().size());

            // 按节点顺序获取UserTask
            int order = 1;
            for (FlowElement element : process.getFlowElements()) {
                log.debug("发现元素: type={}, id={}, name={}",
                    element.getClass().getSimpleName(), element.getId(), element.getName());
                if (element instanceof UserTask) {
                    UserTask userTask = (UserTask) element;
                    Map<String, Object> node = new HashMap<>();
                    node.put("nodeId", userTask.getId());
                    node.put("nodeName", userTask.getName() != null ? userTask.getName() : userTask.getId());
                    node.put("nodeOrder", order++);
                    node.put("assignee", userTask.getAssignee());
                    node.put("candidateUsers", userTask.getCandidateUsers());
                    node.put("candidateGroups", userTask.getCandidateGroups());
                    result.add(node);
                    log.info("找到用户任务: id={}, name={}", userTask.getId(), userTask.getName());
                }
            }

            log.info("解析BPMN获取到{}个用户任务节点", result.size());

        } catch (Exception e) {
            log.error("解析BPMN XML失败: {}", e.getMessage(), e);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getUserTaskNodesByProcessDefinition(String processDefinitionId) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel == null) {
                return result;
            }

            org.flowable.bpmn.model.Process process = bpmnModel.getMainProcess();
            if (process == null) {
                return result;
            }

            int order = 1;
            for (FlowElement element : process.getFlowElements()) {
                if (element instanceof UserTask) {
                    UserTask userTask = (UserTask) element;
                    Map<String, Object> node = new HashMap<>();
                    node.put("nodeId", userTask.getId());
                    node.put("nodeName", userTask.getName() != null ? userTask.getName() : userTask.getId());
                    node.put("nodeOrder", order++);
                    node.put("assignee", userTask.getAssignee());
                    result.add(node);
                }
            }

        } catch (Exception e) {
            log.error("从流程定义获取用户任务节点失败: processDefinitionId={}", processDefinitionId, e);
        }

        return result;
    }

    @Override
    public Map<String, Object> getProcessProgress(String processInstanceId) {
        Map<String, Object> progress = new HashMap<>();
        progress.put("processInstanceId", processInstanceId);

        try {
            // 获取流程定义
            String processDefinitionId;
            boolean isEnded = false;

            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance != null) {
                processDefinitionId = processInstance.getProcessDefinitionId();
            } else {
                // 流程已结束，从历史中获取
                var historicInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                if (historicInstance == null) {
                    progress.put("error", "流程实例不存在");
                    return progress;
                }
                processDefinitionId = historicInstance.getProcessDefinitionId();
                isEnded = true;
            }

            progress.put("isEnded", isEnded);

            // 获取所有用户任务节点
            List<Map<String, Object>> allNodes = getUserTaskNodesByProcessDefinition(processDefinitionId);
            progress.put("nodes", allNodes);

            // 获取历史活动
            List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .activityType("userTask")
                    .orderByHistoricActivityInstanceStartTime()
                    .asc()
                    .list();

            // 标记已完成的节点
            Set<String> completedNodeIds = new HashSet<>();
            Set<String> activeNodeIds = new HashSet<>();

            for (HistoricActivityInstance activity : historicActivities) {
                if (activity.getEndTime() != null) {
                    completedNodeIds.add(activity.getActivityId());
                } else {
                    activeNodeIds.add(activity.getActivityId());
                }
            }

            // 获取当前活动节点
            if (!isEnded) {
                List<String> currentActiveIds = runtimeService.getActiveActivityIds(processInstanceId);
                activeNodeIds.addAll(currentActiveIds);
            }

            // 更新节点状态
            for (Map<String, Object> node : allNodes) {
                String nodeId = (String) node.get("nodeId");
                if (completedNodeIds.contains(nodeId)) {
                    node.put("status", "completed");
                } else if (activeNodeIds.contains(nodeId)) {
                    node.put("status", "active");
                } else {
                    node.put("status", "pending");
                }
            }

            progress.put("completedNodeIds", completedNodeIds);
            progress.put("activeNodeIds", activeNodeIds);

        } catch (Exception e) {
            log.error("获取流程进度失败: processInstanceId={}", processInstanceId, e);
            progress.put("error", e.getMessage());
        }

        return progress;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignApproverByConfig(String processInstanceId, Map<String, Object> nodeConfigMap) {
        if (nodeConfigMap == null || nodeConfigMap.isEmpty()) {
            log.debug("nodeConfig为空，跳过审批人分配");
            return;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode nodeConfig = objectMapper.valueToTree(nodeConfigMap);
            JsonNode nodes = nodeConfig.get("nodes");

            if (nodes == null || !nodes.isArray()) {
                log.debug("nodeConfig中没有nodes配置");
                return;
            }

            // 获取当前待办任务
            List<Task> currentTasks = getTasksByProcessInstanceId(processInstanceId);
            if (currentTasks.isEmpty()) {
                log.debug("没有找到当前待办任务");
                return;
            }

            for (Task task : currentTasks) {
                String taskDefinitionKey = task.getTaskDefinitionKey();

                // 查找匹配的节点配置
                for (JsonNode node : nodes) {
                    String nodeId = node.get("nodeId").asText();
                    if (taskDefinitionKey.equals(nodeId)) {
                        assignApproverToTask(task.getId(), node);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            log.error("根据配置分配审批人失败: processInstanceId={}", processInstanceId, e);
        }
    }

    /**
     * 为单个任务分配审批人
     */
    private void assignApproverToTask(String taskId, JsonNode node) {
        String approverType = node.has("approverType") ? node.get("approverType").asText() : "USER";
        String nodeName = node.has("nodeName") ? node.get("nodeName").asText() : "";

        log.info("分配审批人: taskId={}, 节点={}, 类型={}", taskId, nodeName, approverType);

        switch (approverType) {
            case "USER":
                // 指定用户
                if (node.has("approverUserId") && !node.get("approverUserId").isNull()) {
                    String userId = node.get("approverUserId").asText();
                    taskService.setAssignee(taskId, userId);
                    log.info("设置审批人为指定用户: userId={}", userId);
                }
                break;

            case "ROLE":
                // 指定角色 - 设置候选组
                if (node.has("approverRoleCode") && !node.get("approverRoleCode").isNull()) {
                    String roleCode = node.get("approverRoleCode").asText();
                    taskService.addCandidateGroup(taskId, roleCode);
                    log.info("设置审批人为角色候选组: roleCode={}", roleCode);
                }
                break;

            case "TASK_CREATOR":
                // 任务创建人 - 需要从流程变量获取
                Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
                if (task != null) {
                    Object assignerId = runtimeService.getVariable(task.getProcessInstanceId(), "assignerId");
                    if (assignerId != null) {
                        taskService.setAssignee(taskId, assignerId.toString());
                        log.info("设置审批人为任务创建人: userId={}", assignerId);
                    }
                }
                break;

            default:
                log.warn("未知或未支持的审批人类型: {}", approverType);
        }
    }
}
