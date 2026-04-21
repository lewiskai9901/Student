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
    OTHER: '其他',
    place: '场所',
    org_unit: '组织',
    user: '用户',
    role: '角色'
  } as Record<string, string>)[code] || code
}

/** Hook phase 中文标签 */
export function phaseLabel(phase?: string): string {
  if (!phase) return ''
  return ({
    BEFORE_CREATE: '创建前',
    AFTER_CREATE: '创建后',
    BEFORE_UPDATE: '更新前',
    AFTER_UPDATE: '更新后',
    BEFORE_DELETE: '删除前',
    AFTER_DELETE: '删除后',
    BEFORE_CHECKIN: '入住前',
    AFTER_CHECKIN: '入住后',
    BEFORE_CHECKOUT: '退出前',
    AFTER_CHECKOUT: '退出后',
    BEFORE_ADD_MEMBER: '加成员前',
    AFTER_ADD_MEMBER: '加成员后',
    BEFORE_REMOVE_MEMBER: '移成员前',
    AFTER_REMOVE_MEMBER: '移成员后',
    BEFORE_GRANT: '授权前',
    AFTER_GRANT: '授权后',
    BEFORE_REVOKE: '撤销前',
    AFTER_REVOKE: '撤销后',
    BEFORE_MOVE: '移动前',
    AFTER_MOVE: '移动后'
  } as Record<string, string>)[phase] || phase
}

/**
 * Hook 点的调用位置 — 告诉开发者 core 是在哪个 Service 方法里触发这个 hook.
 * 修改 core Policy hook 接入位置时, 同步更新此映射.
 */
export function hookCallSite(entityType?: string, phase?: string): { method: string; desc: string } {
  const key = `${entityType}/${phase}`
  const sites: Record<string, { method: string; desc: string }> = {
    // Place occupancy
    'place/BEFORE_CHECKIN':           { method: 'UniversalPlaceApplicationService.checkIn', desc: '用户入住场所前检查 — BLOCK 级违规会阻止入住' },
    'place/AFTER_CHECKIN':            { method: 'UniversalPlaceApplicationService.checkIn', desc: '用户入住完成后触发 — WARN/INFO 级提示, 不阻断' },
    'place/BEFORE_CHECKOUT':          { method: 'UniversalPlaceApplicationService.checkOut', desc: '用户退宿前检查 — BLOCK 可拒绝退宿' },
    'place/AFTER_CHECKOUT':           { method: 'UniversalPlaceApplicationService.checkOut', desc: '用户退宿完成后触发' },
    // Place CRUD
    'place/BEFORE_CREATE':            { method: 'UniversalPlaceApplicationService.createPlace', desc: '场所创建前 — 可拒绝非法结构/命名' },
    'place/AFTER_CREATE':             { method: 'UniversalPlaceApplicationService.createPlace', desc: '场所创建完成后 — 审计/通知/挂载物联设备' },
    'place/BEFORE_UPDATE':            { method: 'UniversalPlaceApplicationService.updatePlace', desc: '场所属性更新前 — 例: 容量缩减前检查当前入住数' },
    'place/AFTER_UPDATE':             { method: 'UniversalPlaceApplicationService.updatePlace', desc: '场所属性更新完成后' },
    'place/BEFORE_DELETE':            { method: 'UniversalPlaceApplicationService.deletePlace', desc: '场所删除前 — 可拒绝 (例: 还有占用者/预订)' },
    // OrgUnit CRUD
    'org_unit/BEFORE_CREATE':         { method: 'OrgUnitApplicationService.createOrgUnit', desc: '创建组织节点前 — 可拒绝非法结构' },
    'org_unit/AFTER_CREATE':          { method: 'OrgUnitApplicationService.createOrgUnit', desc: '创建完成后触发' },
    'org_unit/BEFORE_UPDATE':         { method: 'OrgUnitApplicationService.updateOrgUnit', desc: '更新组织前' },
    'org_unit/AFTER_UPDATE':          { method: 'OrgUnitApplicationService.updateOrgUnit', desc: '更新完成后触发' },
    'org_unit/BEFORE_DELETE':         { method: 'OrgUnitApplicationService.deleteOrgUnit', desc: '删除组织前 — 可拒绝 (例: CLASS 删前必须无归属学生)' },
    // OrgUnit membership
    'org_unit/BEFORE_ADD_MEMBER':     { method: 'OrgMemberService.addMember', desc: '成员加入组织前' },
    'org_unit/AFTER_ADD_MEMBER':      { method: 'OrgMemberService.addMember', desc: '成员加入完成后' },
    'org_unit/BEFORE_REMOVE_MEMBER':  { method: 'OrgMemberService.removeMember', desc: '成员移除前' },
    'org_unit/AFTER_REMOVE_MEMBER':   { method: 'OrgMemberService.removeMember', desc: '成员移除完成后' },
    // User CRUD
    'user/BEFORE_CREATE':             { method: 'UserApplicationService.createUser', desc: '用户创建前 — 可拒绝无效输入 (例: 邮箱重复/禁用词/身份证校验)' },
    'user/AFTER_CREATE':              { method: 'UserApplicationService.createUser', desc: '用户创建完成后 — 可发欢迎邮件/审计' },
    'user/BEFORE_UPDATE':             { method: 'UserApplicationService.updateUser', desc: '用户更新前 — 可拒绝敏感字段修改' },
    'user/AFTER_UPDATE':              { method: 'UserApplicationService.updateUser', desc: '用户更新完成后' },
    'user/BEFORE_DELETE':             { method: 'UserApplicationService.deleteUser', desc: '用户删除前 — 禁删超管/级联处理关系' },
    // AccessRelation grant/revoke
    'access_relation/BEFORE_GRANT':   { method: 'AccessRelationApplicationService.create', desc: '授予关系前 — 例: 家属监护必须身份证校验/禁止给离职用户授权' },
    'access_relation/AFTER_GRANT':    { method: 'AccessRelationApplicationService.create', desc: '授予关系完成后 — 审计/通知被授权人' },
    'access_relation/BEFORE_REVOKE':  { method: 'AccessRelationApplicationService.delete', desc: '撤销关系前 — 可拒绝 (例: 必须由授权人本人撤销)' },
    'access_relation/AFTER_REVOKE':   { method: 'AccessRelationApplicationService.delete', desc: '撤销关系完成后 — 审计/清理派生数据' }
  }
  return sites[key] || { method: '', desc: '' }
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
