package com.school.management.service.task;

import com.school.management.dto.task.TaskApproveRequest;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.util.List;
import java.util.Map;

/**
 * Flowable流程服务接口
 */
public interface FlowableProcessService {

    /**
     * 部署流程定义
     *
     * @param name    流程名称
     * @param bpmnXml BPMN XML内容
     * @return 流程定义ID
     */
    String deployProcess(String name, String bpmnXml);

    /**
     * 启动流程实例
     *
     * @param processDefinitionKey 流程定义Key
     * @param businessKey          业务Key
     * @param variables            流程变量
     * @return 流程实例
     */
    ProcessInstance startProcess(String processDefinitionKey, String businessKey, Map<String, Object> variables);

    /**
     * 获取用户待办任务
     *
     * @param userId 用户ID
     * @return 任务列表
     */
    List<Task> getUserTasks(String userId);

    /**
     * 获取流程实例的当前任务
     *
     * @param processInstanceId 流程实例ID
     * @return 任务列表
     */
    List<Task> getTasksByProcessInstanceId(String processInstanceId);

    /**
     * 完成任务(审批通过)
     *
     * @param taskId    Flowable任务ID
     * @param variables 流程变量
     * @param comment   审批意见
     */
    void completeTask(String taskId, Map<String, Object> variables, String comment);

    /**
     * 驳回任务到指定节点
     *
     * @param taskId       Flowable任务ID
     * @param targetNodeId 目标节点ID
     * @param comment      驳回原因
     */
    void rejectToNode(String taskId, String targetNodeId, String comment);

    /**
     * 获取流程实例
     *
     * @param processInstanceId 流程实例ID
     * @return 流程实例
     */
    ProcessInstance getProcessInstance(String processInstanceId);

    /**
     * 终止流程实例
     *
     * @param processInstanceId 流程实例ID
     * @param reason            终止原因
     */
    void terminateProcess(String processInstanceId, String reason);

    /**
     * 获取流程可回退的节点列表
     *
     * @param taskId 当前任务ID
     * @return 可回退的节点列表
     */
    List<Map<String, String>> getRejectableNodes(String taskId);

    /**
     * 转交任务
     *
     * @param taskId      Flowable任务ID
     * @param targetUserId 目标用户ID
     * @param reason      转交原因
     */
    void transferTask(String taskId, String targetUserId, String reason);

    /**
     * 检查流程是否结束
     *
     * @param processInstanceId 流程实例ID
     * @return 是否结束
     */
    boolean isProcessEnded(String processInstanceId);

    /**
     * 获取流程图(高亮当前节点)
     *
     * @param processInstanceId 流程实例ID
     * @return 流程图Base64
     */
    String getProcessDiagram(String processInstanceId);

    /**
     * 从BPMN XML解析用户任务节点
     *
     * @param bpmnXml BPMN XML内容
     * @return 用户任务节点列表 [{nodeId, nodeName, nodeOrder}]
     */
    List<Map<String, Object>> parseUserTaskNodes(String bpmnXml);

    /**
     * 从流程定义ID获取用户任务节点
     *
     * @param processDefinitionId 流程定义ID
     * @return 用户任务节点列表
     */
    List<Map<String, Object>> getUserTaskNodesByProcessDefinition(String processDefinitionId);

    /**
     * 获取流程实例的进度信息（包含节点和当前状态）
     *
     * @param processInstanceId 流程实例ID
     * @return 进度信息
     */
    Map<String, Object> getProcessProgress(String processInstanceId);

    /**
     * 根据审批人配置设置当前任务的审批人
     *
     * @param processInstanceId 流程实例ID
     * @param nodeConfig        节点配置Map
     */
    void assignApproverByConfig(String processInstanceId, Map<String, Object> nodeConfig);
}
