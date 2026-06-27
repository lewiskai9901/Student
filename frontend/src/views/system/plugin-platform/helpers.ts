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
    ACCESS_RELATION: '访问关系',
    place: '场所',
    org_unit: '组织',
    user: '用户',
    role: '角色',
    access_relation: '访问关系',
    permission: '权限',
    data_scope: '数据维度'
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
  const m1 = origin.match(/^PLUGIN:([A-Z_]+)@([\w.-]+)(?::.*)?$/)
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
  return allFeatures(t).slice(0, 3)
}

/** 类型的全部启用特性 key (不截断)。 */
export function allFeatures(t: any): string[] {
  const f = t?.features
  if (!f) return []
  const obj = typeof f === 'string' ? (() => { try { return JSON.parse(f) } catch { return {} } })() : f
  return Object.entries(obj).filter(([, v]) => v === true).map(([k]) => k)
}

/**
 * 实体类型"特性"(feature flag) 的中文说明。特性是插件 getFeatures() 声明的能力开关 (布尔),
 * 不是方法名 —— 决定该类型在系统里能做什么 (如能否登录、是否教职工)。未知 key 回退原码。
 */
export interface FeatureMeta {
  /** 中文名 */
  label: string
  /** 用处 + 效果 (点击/悬停展示) */
  desc: string
  /** 归属: CORE=通用核心特性(插件可用), EDU=教育行业特性 */
  owner: 'CORE' | 'EDU'
}

/**
 * 实体类型"特性"词典: code → {中文名, 用处说明, 归属}。
 * 特性是共享能力词汇表 —— 通用特性(CORE)任何插件可用, 行业特性(EDU)归对应行业。
 * 未知 key 回退原码 (label) / 空说明。
 */
export const FEATURE_INFO: Record<string, FeatureMeta> = {
  // ── 通用 (用户/管理) ──
  canLogin: { label: '可登录系统', owner: 'CORE', desc: '该类型用户能否登录系统。与账户状态(status)共同决定:关闭后,此类用户即使有账号、密码正确也无法登录(如访客)。' },
  isStaff: { label: '教职工身份', owner: 'CORE', desc: '标记为内部教职工。影响"教职工"维度的统计、筛选与部分管理界面的可见性。' },
  isExternal: { label: '外部人员', owner: 'CORE', desc: '非本组织正式成员(访客/家长/外包)。通常不计入成员统计,默认权限更受限。' },
  profileEditableBySelf: { label: '本人可编辑资料', owner: 'CORE', desc: '允许该类型用户自行编辑个人档案字段(否则只能由管理员维护)。' },
  canBeAdminOfOrg: { label: '可任组织管理员', owner: 'CORE', desc: '该类型用户可被指派为某组织的管理员(admin 关系),从而管理该组织。' },
  canBeResponsibleForPlace: { label: '可作场所责任人', owner: 'CORE', desc: '该类型用户可被指派为场所的责任人。' },
  // ── 场所能力 (PLACE) ──
  hasCapacity: { label: '有容量上限', owner: 'CORE', desc: '该场所类型有容纳人数/工位上限,可在场所上设置并校验容量。' },
  bookable: { label: '可预订', owner: 'CORE', desc: '该场所类型可被预订占用(进入预订流程),如会议室、活动室。' },
  assignable: { label: '可分配', owner: 'CORE', desc: '该场所类型可被分配给组织/班级(归属关系),如教学楼分给某学院。' },
  occupiable: { label: '可占用', owner: 'CORE', desc: '该场所类型可登记长期占用(如住宿床位、固定工位)。' },
  hasGender: { label: '含性别属性', owner: 'CORE', desc: '该类型区分性别(如宿舍按性别),启用性别相关约束。' },
  hasOccupancy: { label: '跟踪占用', owner: 'CORE', desc: '该场所跟踪当前占用情况(已用/空闲)。' },
  // ── 组织能力 (ORG / OrgCategory) ──
  dataPermissionBoundary: { label: '数据权限边界', owner: 'CORE', desc: '该组织是数据权限的边界节点 —— "本组织及以下"等范围以它为根划分。' },
  inspectionTarget: { label: '可被检查', owner: 'CORE', desc: '该组织可作为检查/评分的对象(出现在检查任务的可选范围里)。' },
  memberManagement: { label: '管理成员', owner: 'CORE', desc: '该组织直接管理成员名册(如班级、部门),可在其下增删成员。' },
  attendance: { label: '启用考勤', owner: 'CORE', desc: '该组织启用考勤功能。' },
  scheduling: { label: '启用排课', owner: 'CORE', desc: '该组织启用排课/课表功能。' },
  // ── 教育行业 (EDU) ──
  isLearner: { label: '学习者', owner: 'EDU', desc: '标记为学生类。可被评教、记成绩、纳入班级名册等学习场景;org-impact 的"学生数"按它统计。' },
  canEnroll: { label: '可注册入学 / 选课', owner: 'EDU', desc: '参与入学注册与选课流程。' },
  canTeach: { label: '可授课', owner: 'EDU', desc: '可被排课、担任任课教师;org-impact 的"教师数"按它统计。' },
  canCounsel: { label: '可带班 / 辅导', owner: 'EDU', desc: '可担任辅导员、带学生。' },
  canApproveGrade: { label: '可审批成绩', owner: 'EDU', desc: '具备成绩审批权限。' },
  hasGuardian: { label: '有监护人', owner: 'EDU', desc: '该类型用户关联监护人(家长)信息。' },
  receivesPersonalGrade: { label: '接收个人成绩', owner: 'EDU', desc: '会产生/接收个人成绩记录。' },
  attendanceTracked: { label: '纳入考勤', owner: 'EDU', desc: '该类型用户参与考勤统计。' },
  canBeAssignedToClass: { label: '可分配到班级', owner: 'EDU', desc: '可被编入班级(班级成员)。' },
  manageableByOrgAdmin: { label: '组织管理员可管', owner: 'EDU', desc: '可由其所属组织的管理员管理。' },
  hasStudents: { label: '含学生', owner: 'EDU', desc: '该组织下挂学生。' },
  hasClasses: { label: '含班级', owner: 'EDU', desc: '该组织下设班级。' },
  hasExams: { label: '有考试', owner: 'EDU', desc: '该组织组织考试。' },
  hasTimetable: { label: '有课表', owner: 'EDU', desc: '该组织有课表。' },
  hasAttendance: { label: '有考勤', owner: 'EDU', desc: '该组织有考勤记录。' },
  hasProjector: { label: '配备投影仪', owner: 'EDU', desc: '该场所配备投影仪(教室设备)。' },
  hasAC: { label: '配备空调', owner: 'EDU', desc: '该场所配备空调。' },
}

/** 特性 key → 中文名 (未知回退原码)。 */
export function featureLabel(key: string): string {
  return FEATURE_INFO[key]?.label || key
}

/** 特性 key → 用处说明 (未知回退空串)。 */
export function featureDesc(key: string): string {
  return FEATURE_INFO[key]?.desc || ''
}

/** 特性 key → 归属 (CORE/EDU; 未知按 EDU 处理=行业特性更可能未登记)。 */
export function featureOwner(key: string): 'CORE' | 'EDU' {
  return FEATURE_INFO[key]?.owner || 'EDU'
}

/** 字段类型码 → 中文。 */
export function fieldTypeLabel(type?: string): string {
  const m: Record<string, string> = {
    text: '文本', textarea: '多行文本', number: '数字', date: '日期', datetime: '日期时间',
    select: '单选', multiselect: '多选', tags: '标签', relation: '关联', boolean: '是 / 否',
    email: '邮箱', phone: '电话', file: '文件', image: '图片', json: 'JSON', richtext: '富文本',
  }
  return m[type || ''] || type || '—'
}

/** 解析类型的字段定义 (metadataSchema.fields)。 */
export function parseTypeFields(t: any): any[] {
  try {
    const schema = typeof t?.metadataSchema === 'string' ? JSON.parse(t.metadataSchema) : t?.metadataSchema
    return Array.isArray(schema?.fields) ? schema.fields : []
  } catch { return [] }
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

/**
 * 实体"类型"的分类 (entity_type_configs.category) → 中文。
 * 源自后端枚举 UserCategory / OrgCategory / BaseCategory。注意: 这与上面"关系"的 categoryLabel
 * (MEMBERSHIP/OWNERSHIP…) 是两回事 —— 类型分类是把同一实体下的类型按性质归组。
 */
export function typeCategoryLabel(cat?: string): string {
  return ({
    // 用户 (UserCategory)
    ADMIN: '管理员', STAFF: '职工', MEMBER: '成员', EXTERNAL: '外部人员',
    // 组织 (OrgCategory)
    ROOT: '根组织', BRANCH: '分支机构', FUNCTIONAL: '职能部门', GROUP: '成员组', CONTAINER: '容器',
    // 场所 (BaseCategory)
    SITE: '校区/园区', BUILDING: '楼栋', FLOOR: '楼层', ROOM: '房间', AREA: '区域', POINT: '点位', SPACE: '空间',
  } as Record<string, string>)[cat || ''] || cat || '—'
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
  dataResources: any[]
  resourceRelations: any[]
  triggerPoints: any[]
  subscriptionRules: any[]
  targetModes: any[]
  messagingHealth: { healthy: boolean; missingTables: string[] }
  metrics: { totalDurationMs?: number; registrars?: Record<string, { durationMs: number; declarationCount: number }> } | null
}

export const RESOURCE_TYPES = [
  { key: 'types', label: '类型', icon: 'LayoutGrid' },
  // 关系分两类: 主体关系(relation_types, 主体↔主体的图) / 数据关系(resource_relations, 资源如何锚定主体)
  { key: 'relations', label: '主体关系', icon: 'Link2' },
  { key: 'dataResources', label: '数据资源', icon: 'Database' },
  { key: 'resourceRelations', label: '数据关系', icon: 'Share2' },
  { key: 'events', label: '事件类型', icon: 'Bell' },
  { key: 'permissions', label: '权限', icon: 'Shield' },
  { key: 'roles', label: '角色', icon: 'UserCog' },
  { key: 'policies', label: '策略', icon: 'ShieldCheck' },
  { key: 'dataScopes', label: '数据维度', icon: 'Filter' },
  { key: 'triggerPoints', label: '触发点', icon: 'Zap' },
  { key: 'subscriptionRules', label: '订阅规则', icon: 'BellRing' }
] as const

/** 存储种类 (resource_relations.storage_kind) → 面向意图的中文 (管理员可懂; 原码进 tooltip) */
export function storageKindLabel(kind?: string): string {
  if (!kind) return '-'
  return ({
    COLUMN: '按业务字段',
    SUBJECT_GRAPH: '按成员归属',
    RECORD_RELATION: '按记录关系',
    PROVIDER: '按动态规则',
    MATERIALIZED: '物化'
  } as Record<string, string>)[kind] || kind
}

/** 数据范围/维度码 (allowed_scopes / OrgAnchor / data_scope_dims) 中文 */
export function scopeLabel(code?: string): string {
  if (!code) return '-'
  return ({
    ALL: '全部', SELF: '仅本人',
    DEPARTMENT: '本组织', DEPARTMENT_AND_BELOW: '本组织及下级',
    MANAGED_ORGS: '我管理的组织', MANAGED_ORGS_AND_BELOW: '我管理的组织及下级',
    PRIMARY_ORG: '主组织', RELATION: '按关系', CUSTOM: '自定义', CUSTOM_ORG: '指定组织',
    PLUGIN_DIM: '插件维度', BY_CLASS: '按班级', BY_GRADE: '按年级', BY_MAJOR: '按专业'
  } as Record<string, string>)[code] || code
}

/** 消息目标模式 (target_mode) 中文 */
export function targetModeLabel(code?: string): string {
  if (!code) return '-'
  return ({
    BY_SUBJECT: '主体本人', BY_ROLE: '按角色', BY_RELATION: '按关系',
    BY_FEATURE: '按能力', FIXED: '固定名单', ALL_USERS: '全部用户'
  } as Record<string, string>)[code] || code
}

/** 消息渠道 (channel) 中文 */
export function channelLabel(code?: string): string {
  if (!code) return '站内信'
  return ({ IN_APP: '站内信', EMAIL: '邮件', SMS: '短信', WECHAT: '微信', PUSH: '推送' } as Record<string, string>)[code] || code
}

/** 策略作用点 "place/BEFORE_CHECKIN" → "场所 / 入住前" */
export function hookKeyLabel(key?: string): string {
  if (!key) return '-'
  const [entity, phase] = key.split('/')
  return subjectTypeLabel(entity) + ' / ' + phaseLabel(phase)
}

export type ResourceKey = typeof RESOURCE_TYPES[number]['key']

/**
 * 资源类型分组 (按用途归类, 让"功能与关系"一目了然)。左栏按此分组 + 概念图例用。
 */
export const RESOURCE_GROUPS: { label: string; hint: string; keys: ResourceKey[] }[] = [
  { label: '实体与关系', hint: '建模主体本身, 以及主体之间的关系',
    keys: ['types', 'relations'] },
  { label: '权限与数据范围', hint: '谁能进哪些功能 (权限/角色), 以及能看哪些数据 (策略/维度)',
    keys: ['permissions', 'roles', 'policies', 'dataScopes'] },
  { label: '数据归属', hint: '业务数据受不受管控, 以及它"挂在谁名下"——决定数据权限怎么过滤',
    keys: ['dataResources', 'resourceRelations'] },
  { label: '事件与消息', hint: '系统发生了什么 (事件/触发点), 以及通知谁 (订阅规则)',
    keys: ['events', 'triggerPoints', 'subscriptionRules'] }
]

/** 每个资源类型一句人话说明 (选中时显示在表顶 + 左栏 tooltip)。 */
export const RESOURCE_TYPE_DESC: Record<ResourceKey, string> = {
  types: '用户/组织/场所的子类型 (如 学生、教师、班级), 插件可在其上扩展字段。',
  relations: '主体之间的关系图: 谁是某组织的成员/管理员、谁是谁的家属。回答"谁和谁有关系"。',
  dataResources: '受数据权限管控的业务资源, 及其可配置的"可见范围"选项。回答"哪些数据受管控"。',
  resourceRelations: '每个数据资源如何锚定到主体 (按所属组织/创建者/被检查方…) —— 直接决定数据权限怎么过滤。回答"这些数据挂在谁名下"。',
  events: '系统中可发生的事件 (如 入住、成绩发布), 用于触发通知/流程。',
  permissions: '功能权限点 (菜单/操作/接口/按钮/数据)。',
  roles: '权限的集合, 授予用户。',
  policies: '业务规则钩子 (如 入住前校验容量), 由插件挂接到实体生命周期的某个阶段。',
  dataScopes: '插件提供的数据范围切分维度 (如 按班级、按年级), 供角色配置数据权限时选用。',
  triggerPoints: '业务流程中可挂接事件的位置 (点火后产生事件/通知)。',
  subscriptionRules: '事件发生时通知谁、走什么渠道。'
}

/** 资源表各列的中文 tooltip (列头 title)。key = `${resourceKey}.${列标识}`。 */
export const COLUMN_TOOLTIPS: Record<string, string> = {
  'common.industry': '该声明由哪个插件/行业包贡献 (通用核心 / 教育行业 / 跨行业通用扩展 / 自定义)',
  'relations.direction': '关系从哪类主体指向哪类主体 (如 用户 → 组织)',
  'relations.category': '关系的语义类别: 管理/成员/关联/委托/订阅',
  'relations.chain': '传递/隐含关系: 拥有此关系会自动获得的下游关系',
  'relations.tier': '通用度层级: 通用核心 / 通用扩展 / 行业垂直',
  'dataResources.scopes': '该资源在数据权限里可配置的可见范围选项',
  'dataResources.typeField': '按"类型"再过滤时所依据的字段 (留空=不按类型过滤)',
  'dataResources.kind': '资源种类: 记录本身是否就是一个主体 (PLAIN=普通业务记录 / SUBJECT=记录即主体)',
  'resourceRelations.subject': '该锚定关系指向哪类主体 (用户/组织/场所)',
  'resourceRelations.storage': '锚定方式: 按业务字段 / 按成员归属 / 按记录关系 / 按动态规则',
  'resourceRelations.anchor': '锚定到主体所依据的具体落点 (业务表列名 / 关系码 / 解析器 bean) —— 实现细节',
  'resourceRelations.grants': '无显式授予时, 该锚定是否默认参与可见性判定',
  'events.polarity': '事件极性: 正向 (好事) / 负向 (问题) / 中性',
  'permissions.type': '权限类型: 菜单/操作/按钮/接口/数据',
  'permissions.scope': '权限作用面: 公开/本人/管理/系统',
  'roles.level': '角色级别 (数值越小越高; 仅作排序参考)',
  'policies.hook': '策略挂接的实体与阶段 (如 场所 / 入住前)',
  'policies.source': '实现该策略的 Java 类 —— 实现细节',
  'triggerPoints.context': '点火时可携带的上下文字段',
  'subscriptionRules.target': '通知目标的选取方式'
}
