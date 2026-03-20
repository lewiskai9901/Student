/**
 * 租户管理类型定义
 */

export interface Tenant {
  id: number | string
  tenantCode: string
  tenantName: string
  domain?: string
  config?: Record<string, unknown>
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateTenantRequest {
  tenantCode: string
  tenantName: string
  domain?: string
  config?: Record<string, unknown>
}

export interface UpdateTenantRequest {
  tenantName?: string
  domain?: string
  config?: Record<string, unknown>
  enabled?: boolean
}
