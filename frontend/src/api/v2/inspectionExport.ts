/**
 * 量化检查数据导出 API
 */
import { http } from '@/utils/request'
import type { ExportTask, CreateExportRequest } from '@/types/v2/analytics'

const EXPORT_URL = '/v2/inspection/export'

export function createExportTask(data: CreateExportRequest): Promise<ExportTask> {
  return http.post<ExportTask>(`${EXPORT_URL}/create`, data)
}

export function listExportTasks(): Promise<ExportTask[]> {
  return http.get<ExportTask[]>(`${EXPORT_URL}/tasks`)
}

export function getExportTask(taskCode: string): Promise<ExportTask> {
  return http.get<ExportTask>(`${EXPORT_URL}/tasks/${taskCode}`)
}

export const inspectionExportApi = {
  createTask: createExportTask,
  listTasks: listExportTasks,
  getTask: getExportTask
}
