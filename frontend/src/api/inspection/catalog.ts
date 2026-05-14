/**
 * 检查平台 - 模板分类 API
 */
import type { LongId } from '@/types/common'
import { http } from '@/utils/request'
import type {
  TemplateCatalog,
  TemplateCatalogTreeNode,
  CreateCatalogRequest,
  UpdateCatalogRequest,
} from '@/types/insp/template'

const BASE = '/inspection/catalogs'

export function getCatalogs(): Promise<TemplateCatalog[]> {
  return http.get<TemplateCatalog[]>(BASE)
}

export function getCatalogTree(): Promise<TemplateCatalogTreeNode[]> {
  return http.get<TemplateCatalogTreeNode[]>(`${BASE}/tree`)
}

export function createCatalog(data: CreateCatalogRequest): Promise<TemplateCatalog> {
  return http.post<TemplateCatalog>(BASE, data)
}

export function updateCatalog(id: LongId, data: UpdateCatalogRequest): Promise<TemplateCatalog> {
  return http.put<TemplateCatalog>(`${BASE}/${id}`, data)
}

export function deleteCatalog(id: LongId): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

export const catalogApi = {
  getList: getCatalogs,
  getTree: getCatalogTree,
  create: createCatalog,
  update: updateCatalog,
  delete: deleteCatalog,
}
