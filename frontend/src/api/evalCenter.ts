/**
 * 评级中心 API
 *
 * 独立评选引擎，支持多条件多级别评选活动。
 * 路径前缀: /eval
 */
import { http } from '@/utils/request'
import type {
  EvalCampaign,
  EvalLevel,
  EvalBatch,
  EvalResult,
  InspProject,
  InspSection,
  GradeBand,
  EventType,
} from '@/types/evalCenter'

const BASE = '/eval'

// ==================== 评选活动 CRUD ====================

/** 获取评选活动列表 */
export function listCampaigns(params?: {
  status?: string
  keyword?: string
  page?: number
  size?: number
}) {
  return http.get<{ records: EvalCampaign[]; total: number }>(`${BASE}/campaigns`, { params })
}

/** 获取活动详情（含级别+条件） */
export function getCampaign(id: number | string) {
  return http.get<EvalCampaign>(`${BASE}/campaigns/${id}`)
}

/** 创建评选活动 */
export function createCampaign(data: Omit<EvalCampaign, 'id' | 'createdAt' | 'updatedAt'>) {
  return http.post<EvalCampaign>(`${BASE}/campaigns`, data)
}

/** 更新评选活动基本信息 */
export function updateCampaign(id: number | string, data: Partial<EvalCampaign>) {
  return http.put<EvalCampaign>(`${BASE}/campaigns/${id}`, data)
}

/** 删除评选活动 */
export function deleteCampaign(id: number | string) {
  return http.delete<void>(`${BASE}/campaigns/${id}`)
}

// ==================== 级别管理 ====================

/** 获取活动的级别列表（含条件） */
export function getCampaignLevels(campaignId: number | string) {
  return http.get<EvalLevel[]>(`${BASE}/campaigns/${campaignId}/levels`)
}

/** 批量保存级别+条件 */
export function saveCampaignLevels(campaignId: number | string, levels: EvalLevel[]) {
  return http.put<EvalLevel[]>(`${BASE}/campaigns/${campaignId}/levels`, levels)
}

// ==================== 执行 ====================

/** 执行评选 */
export function executeCampaign(
  id: number | string,
  data: { cycleStart: string; cycleEnd: string }
) {
  return http.post<EvalBatch>(`${BASE}/campaigns/${id}/execute`, data)
}

/** 获取执行历史（批次列表） */
export function listBatches(campaignId: number | string) {
  return http.get<EvalBatch[]>(`${BASE}/campaigns/${campaignId}/batches`)
}

// ==================== 结果 ====================

/** 获取批次结果列表（含条件明细） */
export function getBatchResults(
  batchId: number | string,
  params?: { page?: number; size?: number }
) {
  return http.get<{ records: EvalResult[]; total: number }>(
    `${BASE}/batches/${batchId}/results`,
    { params }
  )
}

/** 获取批次详情 */
export function getBatch(batchId: number | string) {
  return http.get<EvalBatch>(`${BASE}/batches/${batchId}`)
}

/** 获取某目标的评选历史 */
export function getTargetHistory(type: string, id: number | string) {
  return http.get<EvalResult[]>(`${BASE}/results/target/${type}/${id}`)
}

// ==================== 辅助选项（条件编辑器） ====================

/** 获取可选的检查项目列表 */
export function getOptionProjects(): Promise<InspProject[]> {
  return http.get<InspProject[]>(`${BASE}/options/projects`)
}

/** 获取项目下的分区列表 */
export function getOptionSections(projectId?: number | string): Promise<InspSection[]> {
  if (!projectId) return Promise.resolve([])
  return http.get<InspSection[]>(`${BASE}/options/sections/${projectId}`)
}

/** 获取分区的等级映射（grade bands） */
export function getOptionGradeBands(sectionId: number | string): Promise<GradeBand[]> {
  return http.get<GradeBand[]>(`${BASE}/options/grade-bands/${sectionId}`)
}

/** 获取事件类型列表 */
export function getOptionEventTypes(): Promise<EventType[]> {
  return http.get<EventType[]>(`${BASE}/options/event-types`)
}
