/**
 * 插件平台治理 API — 对应后端 /api/plugin-platform.
 *
 * 能力:
 *   - 全局: enable / disable / uninstall / health / metrics
 *   - 租户级: tenants/{id}/plugins 相关
 */
import type { LongId } from '@/types/common'
import { http } from '@/utils/request'

export interface PluginHealthInfo {
  code: string
  name: string
  version: string
  status: 'HEALTHY' | 'UNHEALTHY' | 'NO_CONTRIBUTIONS'
  enabled: boolean
  lastStartedAt?: string
  installedAt?: string
  manifestClass?: string

  contributions: {
    types: number
    relations: number
    events: number
    triggerPoints: number
    permissions: number
    roles: number
    menus: number
    policies: number
    dataScopes: number
  }
  totalContributions: number

  samples?: {
    types?: string[]
    relations?: string[]
    events?: string[]
    permissions?: string[]
    roles?: string[]
  }

  dependencies?: {
    code: string
    version?: string
    status: 'HEALTHY' | 'DISABLED' | 'MISSING'
    enabled: boolean
  }[]

  warnings?: string[]

  // legacy: 旧 UI 兼容字段, 新 UI 不需读
  package?: Record<string, unknown>
}

export interface RegistrarMetrics {
  registrars: Record<string, { durationMs: number; declarationCount: number }>
  totalDurationMs: number
}

export interface TenantPluginRow {
  code: string
  name: string
  version: string
  globalEnabled: number
  tenantEnabled: number
  enabledAt?: string
  notes?: string
}

export const pluginPlatformApi = {
  // ─── 全局治理 ───
  enable:    (code: string) => http.post(`/plugin-platform/${code}/enable`),
  disable:   (code: string) => http.post(`/plugin-platform/${code}/disable`),
  uninstall: (code: string) => http.post(`/plugin-platform/${code}/uninstall`),
  health:    (code: string) => http.get<PluginHealthInfo>(`/plugin-platform/${code}/health`),
  metrics:   () => http.get<RegistrarMetrics>('/plugin-platform/metrics'),

  // ─── 租户级治理 ───
  tenantPlugins:        (tenantId: LongId | string) =>
    http.get<TenantPluginRow[]>(`/plugin-platform/tenants/${tenantId}/plugins`),
  enableForTenant:      (tenantId: LongId | string, code: string, notes = '') =>
    http.post(`/plugin-platform/tenants/${tenantId}/plugins/${code}/enable`, { notes }),
  disableForTenant:     (tenantId: LongId | string, code: string, notes = '') =>
    http.post(`/plugin-platform/tenants/${tenantId}/plugins/${code}/disable`, { notes })
}
