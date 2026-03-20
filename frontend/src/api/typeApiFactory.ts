/**
 * Factory for creating type API modules with standard CRUD + enable/disable endpoints.
 * Eliminates duplicated API definitions across orgType.ts, userType.ts, universalPlaceType.ts.
 */
import { http } from '@/utils/request'
import type { ConfigurableType, TypeTreeNode, CategoryInfo } from '@/types/configurableType'

export function createTypeApi<
  T extends ConfigurableType,
  C = Record<string, any>,
  U = Record<string, any>
>(basePath: string) {
  return {
    getCategories: () => http.get<CategoryInfo[]>(`${basePath}/categories`),
    getAll: () => http.get<T[]>(basePath),
    getEnabled: () => http.get<T[]>(`${basePath}/enabled`),
    getTree: () => http.get<TypeTreeNode<T>[]>(`${basePath}/tree`),
    getById: (id: number | string) => http.get<T>(`${basePath}/${id}`),
    getByCode: (code: string) => http.get<T>(`${basePath}/code/${code}`),
    create: (data: C) => http.post<T>(basePath, data),
    update: (id: number | string, data: U) => http.put<T>(`${basePath}/${id}`, data),
    delete: (id: number | string) => http.delete(`${basePath}/${id}`),
    enable: (id: number | string) => http.put<T>(`${basePath}/${id}/enable`),
    disable: (id: number | string) => http.put<T>(`${basePath}/${id}/disable`),
  }
}
