/**
 * V4 检查会话 API
 */
import { http } from '@/utils/request'
import type {
  InspectionSession,
  CreateSessionRequest,
  ClassInspectionRecord,
  SpaceDeductionRequest,
  PersonDeductionRequest,
  BatchChecklistRequest,
  ChecklistProgress,
  RecordBonusRequest
} from '@/types/v2/inspectionSession'

const SESSION_URL = '/v2/inspection/sessions'
const SPACE_URL = '/v2/inspection/spaces'

// ==================== Session API ====================

export function createSession(data: CreateSessionRequest): Promise<InspectionSession> {
  return http.post<InspectionSession>(SESSION_URL, data)
}

export function getSession(id: number): Promise<InspectionSession> {
  return http.get<InspectionSession>(`${SESSION_URL}/${id}`)
}

export function listSessions(startDate: string, endDate: string): Promise<InspectionSession[]> {
  return http.get<InspectionSession[]>(SESSION_URL, {
    params: { startDate, endDate }
  })
}

export function getClassRecords(sessionId: number): Promise<ClassInspectionRecord[]> {
  return http.get<ClassInspectionRecord[]>(`${SESSION_URL}/${sessionId}/class-records`)
}

export function recordSpaceDeduction(sessionId: number, data: SpaceDeductionRequest): Promise<ClassInspectionRecord[]> {
  return http.post<ClassInspectionRecord[]>(`${SESSION_URL}/${sessionId}/space-deductions`, data)
}

export function recordPersonDeduction(sessionId: number, data: PersonDeductionRequest): Promise<ClassInspectionRecord[]> {
  return http.post<ClassInspectionRecord[]>(`${SESSION_URL}/${sessionId}/person-deductions`, data)
}

export function submitChecklistResponses(sessionId: number, data: BatchChecklistRequest): Promise<ClassInspectionRecord> {
  return http.post<ClassInspectionRecord>(`${SESSION_URL}/${sessionId}/checklist-responses`, data)
}

export function getChecklistProgress(sessionId: number): Promise<ChecklistProgress> {
  return http.get<ChecklistProgress>(`${SESSION_URL}/${sessionId}/checklist-progress`)
}

export function submitSession(sessionId: number): Promise<InspectionSession> {
  return http.patch<InspectionSession>(`${SESSION_URL}/${sessionId}/status`, null, {
    params: { action: 'submit' }
  })
}

export function publishSession(sessionId: number): Promise<InspectionSession> {
  return http.patch<InspectionSession>(`${SESSION_URL}/${sessionId}/status`, null, {
    params: { action: 'publish' }
  })
}

export function recordBonus(sessionId: number, data: RecordBonusRequest): Promise<ClassInspectionRecord> {
  return http.post<ClassInspectionRecord>(`${SESSION_URL}/${sessionId}/bonuses`, data)
}

// ==================== Space API ====================

export interface BuildingInfo {
  id: number
  buildingName: string
  buildingCode: string
  floorCount: number
  buildingType: string
}

export interface RoomInfo {
  id: number
  roomNo: string
  buildingId: number
  floor: number
  capacity: number
  currentOccupancy: number
}

export function getBuildings(): Promise<BuildingInfo[]> {
  return http.get<BuildingInfo[]>(`${SPACE_URL}/buildings`)
}

export function getRooms(params?: { buildingId?: number; floor?: number }): Promise<RoomInfo[]> {
  return http.get<RoomInfo[]>(`${SPACE_URL}/rooms`, { params })
}

// ==================== API 对象封装 ====================

export const inspectionSessionApi = {
  create: createSession,
  get: getSession,
  list: listSessions,
  getClassRecords,
  recordSpaceDeduction,
  recordPersonDeduction,
  submitChecklistResponses,
  getChecklistProgress,
  submit: submitSession,
  publish: publishSession,
  recordBonus,
  getBuildings,
  getRooms
}
