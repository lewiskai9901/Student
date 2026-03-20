import request from '@/utils/request'

// 整改状态
export type CorrectiveActionStatus = 'PENDING' | 'SUBMITTED' | 'VERIFIED' | 'REJECTED' | 'CANCELLED'

// 整改记录
export interface CorrectiveAction {
  id: number
  detailId?: number
  targetId?: number
  taskId?: number
  projectId?: number
  actionCode: string
  issueDescription: string
  requiredAction: string
  deadline?: string
  assigneeId?: number
  assigneeName?: string
  status: CorrectiveActionStatus
  statusName: string
  correctionNote?: string
  evidenceIds?: string
  correctedAt?: string
  verifierId?: number
  verifierName?: string
  verifiedAt?: string
  verificationNote?: string
  overdue: boolean
  createdAt: string
}

// 状态信息
export interface StatusInfo {
  code: CorrectiveActionStatus
  name: string
}

// 统计信息
export interface CorrectiveStats {
  pending: number
  submitted: number
  verified: number
  rejected: number
  total: number
}

// 创建整改请求
export interface CreateActionRequest {
  detailId?: number
  targetId?: number
  taskId?: number
  projectId?: number
  issueDescription: string
  requiredAction: string
  deadline?: string
  assigneeId?: number
  assigneeName?: string
  createdBy?: number
}

// 整改 API
export const correctiveActionApi = {
  // 创建整改记录
  create(data: CreateActionRequest) {
    return request.post<CorrectiveAction>('/v6/corrective-actions', data)
  },

  // 提交整改
  submit(id: number | string, correctionNote: string, evidenceIds?: string) {
    return request.post<CorrectiveAction>(`/v6/corrective-actions/${id}/submit`, {
      correctionNote,
      evidenceIds
    })
  },

  // 验收通过
  verify(id: number | string, verifierId: number | string, verifierName: string, verificationNote?: string) {
    return request.post<CorrectiveAction>(`/v6/corrective-actions/${id}/verify`, {
      verifierId,
      verifierName,
      verificationNote
    })
  },

  // 验收驳回
  reject(id: number | string, verifierId: number | string, verifierName: string, verificationNote?: string) {
    return request.post<CorrectiveAction>(`/v6/corrective-actions/${id}/reject`, {
      verifierId,
      verifierName,
      verificationNote
    })
  },

  // 取消整改
  cancel(id: number | string) {
    return request.post(`/v6/corrective-actions/${id}/cancel`)
  },

  // 删除整改
  delete(id: number | string) {
    return request.delete(`/v6/corrective-actions/${id}`)
  },

  // 根据ID查询
  getById(id: number | string) {
    return request.get<CorrectiveAction>(`/v6/corrective-actions/${id}`)
  },

  // 根据检查明细查询
  getByDetailId(detailId: number | string) {
    return request.get<CorrectiveAction[]>(`/v6/corrective-actions/detail/${detailId}`)
  },

  // 根据检查目标查询
  getByTargetId(targetId: number | string) {
    return request.get<CorrectiveAction[]>(`/v6/corrective-actions/target/${targetId}`)
  },

  // 根据检查任务查询
  getByTaskId(taskId: number | string) {
    return request.get<CorrectiveAction[]>(`/v6/corrective-actions/task/${taskId}`)
  },

  // 根据检查项目查询
  getByProjectId(projectId: number | string) {
    return request.get<CorrectiveAction[]>(`/v6/corrective-actions/project/${projectId}`)
  },

  // 查询我的待整改
  getMyPending(assigneeId: number | string) {
    return request.get<CorrectiveAction[]>('/v6/corrective-actions/my-pending', {
      params: { assigneeId }
    })
  },

  // 查询逾期整改
  getOverdue() {
    return request.get<CorrectiveAction[]>('/v6/corrective-actions/overdue')
  },

  // 根据项目和状态查询
  getByProjectIdAndStatus(projectId: number | string, status: CorrectiveActionStatus) {
    return request.get<CorrectiveAction[]>(`/v6/corrective-actions/project/${projectId}/status/${status}`)
  },

  // 项目整改统计
  getProjectStats(projectId: number | string) {
    return request.get<CorrectiveStats>(`/v6/corrective-actions/project/${projectId}/stats`)
  },

  // 获取所有状态
  getStatuses() {
    return request.get<StatusInfo[]>('/v6/corrective-actions/statuses')
  }
}
