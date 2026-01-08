import { http } from '@/utils/request'

// ==================== 类型定义 ====================

// 流程模板DTO
export interface WorkflowTemplateDTO {
  id: string  // 使用string避免JavaScript大数字精度丢失
  templateName: string
  templateCode: string
  templateType: string
  description?: string
  processDefinitionId?: string
  processDefinitionKey?: string
  bpmnXml?: string
  formConfig?: Record<string, unknown> | string  // 表单配置
  nodeConfig?: Record<string, unknown> | string  // 审批节点配置
  isDefault: number
  status: number
  version: number
  sortOrder: number
  createdByName?: string
  createdAt: string
  updatedAt: string
}

// 创建/更新流程模板请求
export interface WorkflowTemplateRequest {
  id?: string
  templateName: string
  templateCode: string
  templateType?: string
  description?: string
  bpmnXml?: string
  formConfig?: Record<string, unknown>  // 表单配置对象
  nodeConfig?: Record<string, unknown>  // 审批节点配置对象
  isDefault?: number
  status?: number
  sortOrder?: number
}

// 可回退节点
export interface RejectableNode {
  nodeId: string
  nodeName: string
}

// ==================== API方法 ====================

// 分页查询流程模板
export function getWorkflowTemplateList(params: {
  pageNum?: number
  pageSize?: number
  keyword?: string
  templateType?: string
  status?: number
}) {
  return http.get<{ records: WorkflowTemplateDTO[]; total: number }>('/task/workflow-templates', { params })
}

// 获取启用的流程模板列表
export function getEnabledTemplates(templateType?: string) {
  return http.get<WorkflowTemplateDTO[]>('/task/workflow-templates/enabled', {
    params: { templateType }
  })
}

// 获取流程模板详情
export function getWorkflowTemplateDetail(id: string | number) {
  return http.get<WorkflowTemplateDTO>(`/task/workflow-templates/${id}`)
}

// 创建流程模板
export function createWorkflowTemplate(data: WorkflowTemplateRequest) {
  return http.post<WorkflowTemplateDTO>('/task/workflow-templates', data)
}

// 更新流程模板
export function updateWorkflowTemplate(id: string | number, data: WorkflowTemplateRequest) {
  return http.put<WorkflowTemplateDTO>(`/task/workflow-templates/${id}`, data)
}

// 删除流程模板
export function deleteWorkflowTemplate(id: string | number) {
  return http.delete<boolean>(`/task/workflow-templates/${id}`)
}

// 部署流程定义
export function deployWorkflowTemplate(id: string | number) {
  return http.post<WorkflowTemplateDTO>(`/task/workflow-templates/${id}/deploy`)
}

// 设为默认模板
export function setDefaultTemplate(id: string | number, templateType: string) {
  return http.post<boolean>(`/task/workflow-templates/${id}/set-default`, null, {
    params: { templateType }
  })
}

// 获取流程图
export function getProcessDiagram(processInstanceId: string) {
  return http.get<string>(`/task/workflow-templates/process-diagram/${processInstanceId}`)
}

// 获取可回退节点
export function getRejectableNodes(taskId: string) {
  return http.get<RejectableNode[]>(`/task/workflow-templates/rejectable-nodes/${taskId}`)
}

// 用户任务节点（从BPMN解析）
export interface UserTaskNode {
  nodeId: string
  nodeName: string
  nodeOrder: number
  assignee?: string
  candidateUsers?: string[]
  candidateGroups?: string[]
}

// 获取流程模板的用户任务节点
export function getUserTaskNodes(templateId: string | number) {
  return http.get<UserTaskNode[]>(`/task/workflow-templates/${templateId}/user-task-nodes`)
}

// 流程进度信息
export interface ProcessProgress {
  processInstanceId: string
  isEnded: boolean
  nodes: Array<{
    nodeId: string
    nodeName: string
    nodeOrder: number
    status: 'completed' | 'active' | 'pending'
    assignee?: string
  }>
  completedNodeIds: string[]
  activeNodeIds: string[]
  error?: string
}

// 获取流程进度
export function getProcessProgress(processInstanceId: string) {
  return http.get<ProcessProgress>(`/task/workflow-templates/process-progress/${processInstanceId}`)
}
