/**
 * V7 检查平台 - 离线同步 API
 */
import { http } from '@/utils/request'
import type { InspSubmission, SubmissionDetail } from '@/types/insp/project'

const BASE = '/v7/insp/sync'

// ==================== Types ====================

export interface SyncPullRequest {
  taskId: number
  lastSyncAt?: string | null
}

export interface SyncPullResponse {
  submissions: InspSubmission[]
  details: SubmissionDetail[]
  serverTime: string
}

export interface SyncPushItem {
  submissionId: number
  formData: string
  clientSyncVersion: number
}

export interface SyncPushResult {
  submissionId: number
  status: 'SYNCED' | 'CONFLICT' | 'NOT_FOUND' | 'REJECTED'
  serverSyncVersion: number | null
  serverFormData: string | null
  serverUpdatedAt: string | null
}

export interface SyncPushResponse {
  results: SyncPushResult[]
  serverTime: string
}

// ==================== API ====================

export function syncPull(data: SyncPullRequest): Promise<SyncPullResponse> {
  return http.post<SyncPullResponse>(`${BASE}/pull`, data)
}

export function syncPush(items: SyncPushItem[]): Promise<SyncPushResponse> {
  return http.post<SyncPushResponse>(`${BASE}/push`, { items })
}

export const inspSyncApi = { syncPull, syncPush }
