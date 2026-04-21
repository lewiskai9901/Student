/**
 * 插件平台共享工具 — 行业色板 / 标签映射 / 数据解析
 */

export const INDUSTRY_COLORS: Record<string, string> = {
  CORE: '#2563eb',
  EDU: '#d97706',
  HEALTH: '#be185d',
  CARE: '#059669',
  CUSTOM: '#6b7280'
}

export function industryColor(code?: string): string {
  if (!code) return '#6b7280'
  return INDUSTRY_COLORS[code] || '#6b7280'
}

export function industryChipStyle(code?: string): Record<string, string> {
  if (!code) return { color: '#94a3b8', borderColor: '#e2e8f0', background: '#f8fafc' }
  const c = industryColor(code)
  return { color: c, borderColor: c + '60', background: c + '12' }
}

export function industryLabel(code?: string): string {
  if (!code) return ''
  return ({
    CORE: '通用核心',
    EDU: '教育行业',
    HEALTH: '医疗行业',
    CARE: '养老行业',
    CUSTOM: '自定义'
  } as Record<string, string>)[code] || code
}

export function subjectTypeLabel(code?: string): string {
  if (!code) return ''
  return ({
    USER: '用户',
    ORG_UNIT: '组织',
    ORG: '组织',
    PLACE: '场所',
    OTHER: '其他'
  } as Record<string, string>)[code] || code
}

export function shortClass(fqcn?: string): string {
  if (!fqcn) return ''
  return fqcn.split('.').pop() || fqcn
}

export function parseOrigin(origin?: string): { kind: 'PLUGIN' | 'TENANT' | 'UNKNOWN'; code: string; version?: string } {
  if (!origin) return { kind: 'UNKNOWN', code: '' }
  const m1 = origin.match(/^PLUGIN:([A-Z_]+)@([\w\.\-]+)(?::.*)?$/)
  if (m1) return { kind: 'PLUGIN', code: m1[1], version: m1[2] }
  if (origin.startsWith('TENANT:CUSTOM')) return { kind: 'TENANT', code: 'CUSTOM' }
  return { kind: 'UNKNOWN', code: origin }
}

export function inferIndustry(pluginClass?: string): string {
  if (!pluginClass) return ''
  if (pluginClass.includes('.core.') || pluginClass.endsWith('CorePlugin')) return 'CORE'
  if (pluginClass.includes('.education.')) return 'EDU'
  if (pluginClass.includes('.healthcare.')) return 'HEALTH'
  if (pluginClass.includes('.eldercare.')) return 'CARE'
  return ''
}

export function inferIndustryFromRegisteredBy(registeredBy?: string): string {
  if (!registeredBy) return 'CUSTOM'
  if (registeredBy === 'CORE') return 'CORE'
  if (registeredBy.toLowerCase().includes('education')) return 'EDU'
  if (registeredBy.toLowerCase().includes('health')) return 'HEALTH'
  if (registeredBy.toLowerCase().includes('care')) return 'CARE'
  return 'CUSTOM'
}

export function resolveIndustry(x: any, from?: string): string {
  if (x?.origin) {
    const parsed = parseOrigin(x.origin)
    if (parsed.kind === 'PLUGIN') return parsed.code
    if (parsed.kind === 'TENANT') return 'CUSTOM'
  }
  if (x?.industry) return x.industry
  if (from === 'registeredBy') {
    return x?.registeredBy === 'CORE' ? 'CORE' : inferIndustryFromRegisteredBy(x?.registeredBy)
  }
  if (x?.pluginClass) return inferIndustry(x.pluginClass) || ''
  return 'CUSTOM'
}

export function relationIndustry(r: any): string {
  if (r?.origin) {
    const parsed = parseOrigin(r.origin)
    if (parsed.kind === 'PLUGIN') return parsed.code
    if (parsed.kind === 'TENANT') return 'CUSTOM'
  }
  if (r?.industry) return r.industry
  if (!r?.registeredBy) return 'CUSTOM'
  if (r.registeredBy === 'CORE') return 'CORE'
  if (r.registeredBy === 'admin' || r.registeredBy === 'CUSTOM') return 'CUSTOM'
  const rb = String(r.registeredBy || '').toLowerCase()
  if (rb.includes('education') || rb.includes('edu')) return 'EDU'
  if (rb.includes('health')) return 'HEALTH'
  if (rb.includes('care')) return 'CARE'
  const code = String(r.relationCode || '').toLowerCase()
  if (/student|class|dorm|teacher|parent|academic|counselor|grade/.test(code)) return 'EDU'
  return 'CORE'
}

export function parseImplied(raw: any): any[] {
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  try { return JSON.parse(raw) } catch { return [] }
}

export function parseSubjects(raw: any): string[] {
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  try { return JSON.parse(raw) } catch { return [String(raw)] }
}

export function parseSchema(raw: any): Record<string, string> {
  if (!raw) return {}
  try {
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (!obj || typeof obj !== 'object') return {}
    const out: Record<string, string> = {}
    for (const k of Object.keys(obj)) {
      const v: any = (obj as any)[k]
      out[k] = (v && typeof v === 'object' && v.type) ? String(v.type) : String(v)
    }
    return out
  } catch { return {} }
}

export function countFields(t: any): number {
  try {
    const schema = typeof t.metadataSchema === 'string' ? JSON.parse(t.metadataSchema) : t.metadataSchema
    return schema?.fields?.length || 0
  } catch { return 0 }
}

export function topFeatures(t: any): string[] {
  const f = t?.features
  if (!f) return []
  const obj = typeof f === 'string' ? (() => { try { return JSON.parse(f) } catch { return {} } })() : f
  return Object.entries(obj).filter(([, v]) => v === true).map(([k]) => k).slice(0, 3)
}

export function parseDataScopeSource(src?: string): string {
  if (!src) return 'CORE'
  if (src === 'CORE') return 'CORE'
  const m = src.match(/^PLUGIN:([A-Z_]+)/)
  return m ? m[1] : 'CUSTOM'
}

export function moduleCodeToIndustry(moduleCode?: string): string {
  if (!moduleCode) return 'CORE'
  const c = String(moduleCode).toLowerCase()
  if (/student|class|grade|exam|academic|teaching|dorm|attendance|inspection/.test(c)) return 'EDU'
  if (/patient|ward|clinic|medical|health/.test(c)) return 'HEALTH'
  if (/elder|care/.test(c)) return 'CARE'
  return 'CORE'
}

export function tierLabel(tier?: string): string {
  if (!tier) return '-'
  return ({ CORE: '通用核心', COMMON_EXT: '通用扩展', DOMAIN: '行业垂直' } as Record<string, string>)[tier] || tier
}

export function tierTagType(tier?: string): 'primary' | 'success' | 'warning' | 'info' {
  return ({ CORE: 'primary', COMMON_EXT: 'warning', DOMAIN: 'success' } as any)[tier || ''] || 'info'
}

export function categoryTagType(cat?: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  return ({
    OWNERSHIP: 'warning', MEMBERSHIP: 'primary', ASSOCIATION: 'success',
    DELEGATION: 'danger', SUBSCRIPTION: 'info'
  } as any)[cat || ''] || 'info'
}

export function categoryLabel(cat?: string): string {
  return ({
    OWNERSHIP: '管理', MEMBERSHIP: '成员', ASSOCIATION: '关联',
    DELEGATION: '委托', SUBSCRIPTION: '订阅'
  } as Record<string, string>)[cat || ''] || cat || '-'
}

export function polarityTagType(p?: string): 'success' | 'warning' | 'danger' | 'info' {
  return ({ POSITIVE: 'success', NEGATIVE: 'danger', NEUTRAL: 'info' } as any)[p || ''] || 'info'
}

export function polarityLabel(p?: string): string {
  return ({ POSITIVE: '正向', NEGATIVE: '负向', NEUTRAL: '中性' } as Record<string, string>)[p || ''] || p || '-'
}

export function permissionTypeLabel(type?: string): string {
  if (!type) return '-'
  return ({
    MENU: '菜单', OPERATION: '操作', BUTTON: '按钮', API: '接口', DATA: '数据'
  } as Record<string, string>)[type] || type
}

export function permissionScopeLabel(scope?: string): string {
  if (!scope) return '-'
  return ({
    PUBLIC: '公开', SELF: '本人', MANAGEMENT: '管理', SYSTEM: '系统'
  } as Record<string, string>)[scope] || scope
}

export function roleTypeLabel(type?: string): string {
  if (!type) return '-'
  return ({
    PRESET: '预置', CUSTOM: '自定义', SUPER_ADMIN: '超级管理',
    SYSTEM_ADMIN: '系统管理', SYSTEM: '系统'
  } as Record<string, string>)[type] || type
}

export const PERMISSION_MODULE_LABELS: Record<string, string> = {
  academic: '学术', access: '权限', asset: '资产', attendance: '考勤',
  check: '打卡', dashboard: '首页', discipline: '纪律', dormitory: '宿舍',
  evaluation: '评价', event: '事件', file: '文件', grade: '成绩',
  inspection: '检查', log: '日志', member: '成员', message: '消息',
  my: '我的', organization: '组织', permission: '权限', place: '场所',
  plugin: '插件', public: '公开', rating: '评分', relation: '关系',
  role: '角色', schedule: '课表', scoring: '评分', student: '学生',
  system: '系统', tag: '标签', task: '任务', teacher: '教师',
  teaching: '教务', template: '模板', tenant: '租户', user: '用户',
  weight: '权重', weixin: '微信', workflow: '流程'
}

export function permissionModuleLabel(module: string): string {
  return PERMISSION_MODULE_LABELS[module] || module
}

export function formatDateShort(ts: string | null | undefined): string {
  if (!ts) return '-'
  const d = new Date(ts)
  if (isNaN(d.getTime())) return String(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export interface PluginData {
  industries: any[]
  types: any[]
  relations: any[]
  events: any[]
  permissions: any[]
  roles: any[]
  policies: any[]
  hookPoints: any[]
  dataScopes: any[]
  triggerPoints: any[]
  subscriptionRules: any[]
  targetModes: any[]
  messagingHealth: { healthy: boolean; missingTables: string[] }
  metrics: { totalDurationMs?: number; registrars?: Record<string, { durationMs: number; declarationCount: number }> } | null
}

export const RESOURCE_TYPES = [
  { key: 'types', label: '类型', icon: 'LayoutGrid' },
  { key: 'relations', label: '关系', icon: 'Link2' },
  { key: 'events', label: '事件类型', icon: 'Bell' },
  { key: 'permissions', label: '权限', icon: 'Shield' },
  { key: 'roles', label: '角色', icon: 'UserCog' },
  { key: 'policies', label: '策略', icon: 'ShieldCheck' },
  { key: 'dataScopes', label: '数据维度', icon: 'Filter' },
  { key: 'triggerPoints', label: '触发点', icon: 'Zap' },
  { key: 'subscriptionRules', label: '订阅规则', icon: 'BellRing' }
] as const

export type ResourceKey = typeof RESOURCE_TYPES[number]['key']
