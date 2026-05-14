import type { LongId } from '@/types/common'
import { http as request } from '@/utils/request'
import type { Tenant, CreateTenantRequest, UpdateTenantRequest } from '@/types/tenant'

/**
 * 租户管理 API
 */

export function listTenants() {
  return request.get<Tenant[]>('/tenants')
}

export function getTenant(id: LongId | string) {
  return request.get<Tenant>(`/tenants/${id}`)
}

export function createTenant(data: CreateTenantRequest) {
  return request.post<Tenant>('/tenants', data)
}

export function updateTenant(id: LongId | string, data: UpdateTenantRequest) {
  return request.put<Tenant>(`/tenants/${id}`, data)
}

export function deleteTenant(id: LongId | string) {
  return request.delete(`/tenants/${id}`)
}
