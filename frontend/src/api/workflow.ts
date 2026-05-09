/**
 * 工作流引擎 API (Phase 3)
 *
 * 注意: 响应拦截器已解包 ApiResponse，API 直接返回 data 内容
 * 后端基础路径: /api/workflow/**
 */
import { http } from '@/utils/request'

export interface ProcessDefinition {
  id: string
  key: string
  name: string
  version: number
  deploymentId: string
  suspended: boolean
}

export interface ProcessInstance {
  id: string
  processDefinitionId: string
  businessKey: string
  ended: boolean
  suspended?: boolean
  startTime?: string
}

export interface WorkflowTask {
  id: string
  name: string
  assignee: string
  processInstanceId: string
  processDefinitionId: string
  createTime?: string
  dueDate?: string
}

export interface DeployResponse {
  id: string
  name: string
  deploymentTime: string
}

export const workflowApi = {
  /** 流程定义 */
  listDefinitions: (): Promise<ProcessDefinition[]> =>
    http.get<ProcessDefinition[]>('/workflow/process-definitions'),

  deploy: (file: File, name?: string): Promise<DeployResponse> => {
    const formData = new FormData()
    formData.append('file', file)
    if (name) formData.append('name', name)
    return http.post<DeployResponse>(
      '/workflow/process-definitions/deploy',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    )
  },

  deleteDeployment: (deploymentId: string, cascade = false): Promise<void> =>
    http.delete<void>(`/workflow/process-definitions/deployments/${deploymentId}?cascade=${cascade}`),

  suspendDefinition: (id: string): Promise<void> =>
    http.post<void>(`/workflow/process-definitions/${id}/suspend`),

  activateDefinition: (id: string): Promise<void> =>
    http.post<void>(`/workflow/process-definitions/${id}/activate`),

  /** 流程实例 */
  listInstances: (params?: { processKey?: string; businessKey?: string; startedBy?: string }): Promise<ProcessInstance[]> =>
    http.get<ProcessInstance[]>('/workflow/process-instances', { params }),

  startInstance: (body: {
    processKey?: string
    processDefinitionId?: string
    businessKey?: string
    variables?: Record<string, unknown>
  }): Promise<ProcessInstance> =>
    http.post<ProcessInstance>('/workflow/process-instances/start', body),

  cancelInstance: (id: string, reason: string): Promise<void> =>
    http.post<void>(`/workflow/process-instances/${id}/cancel`, { reason }),

  /** 我的待办 */
  myTasks: (): Promise<WorkflowTask[]> =>
    http.get<WorkflowTask[]>('/workflow/tasks/mine'),

  claimTask: (id: string): Promise<void> =>
    http.post<void>(`/workflow/tasks/${id}/claim`),

  unclaimTask: (id: string): Promise<void> =>
    http.post<void>(`/workflow/tasks/${id}/unclaim`),

  completeTask: (id: string, variables?: Record<string, unknown>): Promise<void> =>
    http.post<void>(`/workflow/tasks/${id}/complete`, { variables }),

  delegateTask: (id: string, assignee: string): Promise<void> =>
    http.post<void>(`/workflow/tasks/${id}/delegate`, { assignee }),

  /** 历史 */
  historicalInstances: (params?: {
    processKey?: string
    businessKey?: string
    startedBy?: string
    limit?: number
  }): Promise<unknown[]> =>
    http.get<unknown[]>('/workflow/history/instances', { params }),

  instanceTaskHistory: (id: string): Promise<unknown[]> =>
    http.get<unknown[]>(`/workflow/history/instances/${id}/tasks`),

  myHistoricalTasks: (limit = 50): Promise<unknown[]> =>
    http.get<unknown[]>(`/workflow/history/my-tasks?limit=${limit}`)
}
