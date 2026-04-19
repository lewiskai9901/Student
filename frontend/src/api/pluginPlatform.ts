/**
 * 插件平台治理 API — 对应后端 /api/plugin-platform.
 *
 * 能力:
 *   - 全局: enable / disable / uninstall / health / metrics
 *   - 租户级: tenants/{id}/plugins 相关
 */
import { http } from '@/utils/request'

export interface PluginHealthInfo {
  package: {
    industry_code: string
    industry_name: string
    version: string
    enabled: number
    installed_at?: string
    last_started_at?: string
  }
  contributions: {
    types: number
    relations: number
    events: number
    roles: number
    permissions: number
  }
  status: 'HEALTHY' | 'NO_CONTRIBUTIONS'
  totalContributions: number
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
  tenantPlugins:        (tenantId: number | string) =>
    http.get<TenantPluginRow[]>(`/plugin-platform/tenants/${tenantId}/plugins`),
  enableForTenant:      (tenantId: number | string, code: string, notes = '') =>
    http.post(`/plugin-platform/tenants/${tenantId}/plugins/${code}/enable`, { notes }),
  disableForTenant:     (tenantId: number | string, code: string, notes = '') =>
    http.post(`/plugin-platform/tenants/${tenantId}/plugins/${code}/disable`, { notes })
}
