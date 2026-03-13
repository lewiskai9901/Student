/**
 * V7 检查平台 - 模板模块引用 API
 */
import { http } from '@/utils/request'
import type {
  TemplateModuleRef,
  AddModuleRefRequest,
  UpdateModuleRefRequest,
} from '@/types/insp/template'

const BASE = '/v7/insp/templates'

export const templateModuleRefApi = {
  list(templateId: number) {
    return http.get<TemplateModuleRef[]>(`${BASE}/${templateId}/module-refs`)
  },

  add(templateId: number, data: AddModuleRefRequest) {
    return http.post<TemplateModuleRef>(`${BASE}/${templateId}/module-refs`, data)
  },

  update(templateId: number, refId: number, data: UpdateModuleRefRequest) {
    return http.put<TemplateModuleRef>(`${BASE}/${templateId}/module-refs/${refId}`, data)
  },

  remove(templateId: number, refId: number) {
    return http.delete(`${BASE}/${templateId}/module-refs/${refId}`)
  },

  reorder(templateId: number, refIds: number[]) {
    return http.post(`${BASE}/${templateId}/module-refs/reorder`, { refIds })
  },
}
