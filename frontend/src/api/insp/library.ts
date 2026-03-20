/**
 * V7 检查平台 - 检查项库 API
 */
import { http } from '@/utils/request'
import type {
  LibraryItem,
  CreateLibraryItemRequest,
  UpdateLibraryItemRequest,
  TemplateItem,
} from '@/types/insp/template'

const BASE = '/v7/insp/library-items'

export function getLibraryItems(params?: {
  keyword?: string
  category?: string
}): Promise<LibraryItem[]> {
  return http.get<LibraryItem[]>(BASE, { params })
}

export function getLibraryItem(id: number): Promise<LibraryItem> {
  return http.get<LibraryItem>(`${BASE}/${id}`)
}

export function getLibraryCategories(): Promise<string[]> {
  return http.get<string[]>(`${BASE}/categories`)
}

export function createLibraryItem(data: CreateLibraryItemRequest): Promise<LibraryItem> {
  return http.post<LibraryItem>(BASE, data)
}

export function updateLibraryItem(id: number, data: UpdateLibraryItemRequest): Promise<LibraryItem> {
  return http.put<LibraryItem>(`${BASE}/${id}`, data)
}

export function deleteLibraryItem(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

export function syncLibraryItem(id: number): Promise<number> {
  return http.post<number>(`${BASE}/${id}/sync`)
}

export function createItemFromLibrary(sectionId: number, libraryItemId: number, syncWithLibrary: boolean): Promise<TemplateItem> {
  return http.post<TemplateItem>(`/v7/insp/sections/${sectionId}/items/from-library`, {
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
