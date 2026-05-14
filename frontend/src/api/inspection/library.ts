/**
 * 检查平台 - 检查项库 API
 */
import type { LongId } from '@/types/common'
import { http } from '@/utils/request'
import type {
  LibraryItem,
  CreateLibraryItemRequest,
  UpdateLibraryItemRequest,
  TemplateItem,
} from '@/types/insp/template'

const BASE = '/inspection/library-items'

export function getLibraryItems(params?: {
  keyword?: string
  category?: string
}): Promise<LibraryItem[]> {
  return http.get<LibraryItem[]>(BASE, { params })
}

export function getLibraryItem(id: LongId): Promise<LibraryItem> {
  return http.get<LibraryItem>(`${BASE}/${id}`)
}

export function getLibraryCategories(): Promise<string[]> {
  return http.get<string[]>(`${BASE}/categories`)
}

export function createLibraryItem(data: CreateLibraryItemRequest): Promise<LibraryItem> {
  return http.post<LibraryItem>(BASE, data)
}

export function updateLibraryItem(id: LongId, data: UpdateLibraryItemRequest): Promise<LibraryItem> {
  return http.put<LibraryItem>(`${BASE}/${id}`, data)
}

export function deleteLibraryItem(id: LongId): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

export function syncLibraryItem(id: LongId): Promise<number> {
  return http.post<number>(`${BASE}/${id}/sync`)
}

export function createItemFromLibrary(sectionId: LongId, libraryItemId: LongId, syncWithLibrary: boolean): Promise<TemplateItem> {
  return http.post<TemplateItem>(`/inspection/sections/${sectionId}/items/from-library`, {
    libraryItemId,
    syncWithLibrary,
  })
}

export const libraryItemApi = {
  getList: getLibraryItems,
  getById: getLibraryItem,
  getCategories: getLibraryCategories,
  create: createLibraryItem,
  update: updateLibraryItem,
  delete: deleteLibraryItem,
  sync: syncLibraryItem,
  createFromLibrary: createItemFromLibrary,
}
