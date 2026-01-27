/**
 * 学生行为 API
 */
import { http } from '@/utils/request'
import type { BehaviorRecord, CreateBehaviorRecordRequest, BehaviorProfile, BehaviorAlert } from '@/types/behavior'

const RECORD_URL = '/behavior-records'
const ALERT_URL = '/behavior-alerts'

// Records
export function createBehaviorRecord(data: CreateBehaviorRecordRequest): Promise<BehaviorRecord> {
  return http.post<BehaviorRecord>(RECORD_URL, data)
}

export function listByClass(classId: number): Promise<BehaviorRecord[]> {
  return http.get<BehaviorRecord[]>(`${RECORD_URL}/class/${classId}`)
}

export function listByStudent(studentId: number): Promise<BehaviorRecord[]> {
  return http.get<BehaviorRecord[]>(`${RECORD_URL}/student/${studentId}`)
}

export function getStudentProfile(studentId: number): Promise<BehaviorProfile> {
  return http.get<BehaviorProfile>(`${RECORD_URL}/student/${studentId}/profile`)
}

export function acknowledgeBehaviorRecord(id: number): Promise<BehaviorRecord> {
  return http.put<BehaviorRecord>(`${RECORD_URL}/${id}/acknowledge`)
}

export function resolveBehaviorRecord(id: number, data: { resolutionNote: string }): Promise<BehaviorRecord> {
  return http.put<BehaviorRecord>(`${RECORD_URL}/${id}/resolve`, data)
}

// Alerts
export function listAlertsByClass(classId: number): Promise<BehaviorAlert[]> {
  return http.get<BehaviorAlert[]>(`${ALERT_URL}/class/${classId}`)
}

export function listAlertsByStudent(studentId: number): Promise<BehaviorAlert[]> {
  return http.get<BehaviorAlert[]>(`${ALERT_URL}/student/${studentId}`)
}

export function getUnhandledAlertCount(classId: number): Promise<number> {
  return http.get<number>(`${ALERT_URL}/class/${classId}/unhandled-count`)
}

export function markAlertRead(id: number): Promise<BehaviorAlert> {
  return http.put<BehaviorAlert>(`${ALERT_URL}/${id}/read`)
}

export function handleAlert(id: number, data: { note: string }): Promise<BehaviorAlert> {
  return http.put<BehaviorAlert>(`${ALERT_URL}/${id}/handle`, data)
}

export const behaviorRecordApi = {
  create: createBehaviorRecord,
  listByClass,
  listByStudent,
  getProfile: getStudentProfile,
  acknowledge: acknowledgeBehaviorRecord,
  resolve: resolveBehaviorRecord
}

export const behaviorAlertApi = {
  listByClass: listAlertsByClass,
  listByStudent: listAlertsByStudent,
  unhandledCount: getUnhandledAlertCount,
  markRead: markAlertRead,
  handle: handleAlert
}
