/**
 * 检查平台 - 申诉 API (P1#8)
 */
import { http } from '@/utils/request'
import type {
  InspAppeal,
  SubmitAppealRequest,
  ApproveAppealRequest,
  RejectAppealRequest,
} from '@/types/insp/appeal'

const BASE = '/inspection/appeals'

/** 提交申诉 — 权限点 inspection:appeal:create */
export function submitAppeal(data: SubmitAppealRequest): Promise<InspAppeal> {
  return http.post<InspAppeal>(BASE, data)
}

/** 审核通过 — 权限点 inspection:appeal:review */
export function approveAppeal(id: number, data: ApproveAppealRequest): Promise<InspAppeal> {
  return http.post<InspAppeal>(`${BASE}/${id}/approve`, data)
}

/** 审核驳回 — 权限点 inspection:appeal:review */
export function rejectAppeal(id: number, data: RejectAppealRequest): Promise<InspAppeal> {
  return http.post<InspAppeal>(`${BASE}/${id}/reject`, data)
}

/** 撤回申诉 — 仅提交人 */
export function withdrawAppeal(id: number): Promise<InspAppeal> {
  return http.post<InspAppeal>(`${BASE}/${id}/withdraw`)
}

/** 单条查询 */
export function getAppeal(id: number): Promise<InspAppeal> {
  return http.get<InspAppeal>(`${BASE}/${id}`)
}

/** 我的申诉 (提交人视角) */
export function getMyAppeals(): Promise<InspAppeal[]> {
  return http.get<InspAppeal[]>(`${BASE}/my`)
}

/** 待审核清单 (审核员视角) */
export function getPendingAppeals(): Promise<InspAppeal[]> {
  return http.get<InspAppeal[]>(`${BASE}/pending`)
}

/** 按项目查询 */
export function getAppealsByProject(projectId: number): Promise<InspAppeal[]> {
  return http.get<InspAppeal[]>(`${BASE}/by-project/${projectId}`)
}
