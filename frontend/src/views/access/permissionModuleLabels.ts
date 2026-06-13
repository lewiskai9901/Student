/**
 * 权限模块码 → 中文标签 (权限目录 / 功能权限分配 共用唯一真相源)。
 *
 * 权限按 code 前缀 (冒号前第一段) 分组展示, 这里给每个模块前缀一个中文名。
 * 缺失则回退显示原始英文码 —— 新增模块码时务必在此登记, 否则导航/目录会露英文。
 */
export const MODULE_LABELS: Record<string, string> = {
  // ── 通用核心 ──
  access: '访问控制',
  admin: '管理员',
  analytics: '数据分析',
  asset: '资产管理',
  calendar: '日历管理',
  check: '数据导出',
  dashboard: '仪表盘',
  data_module: '数据模块',
  'entity-event': '实体事件',
  'entity-event-type': '事件类型',
  'entity-type-config': '类型配置',
  'event-trigger': '事件触发器',
  msg: '消息',
  'msg-config': '消息配置',
  'msg-notification': '消息通知',
  place: '场所管理',
  plugin: '插件管理',
  'plugin-platform': '插件平台',
  role: '角色管理',
  schedule: '排班管理',
  system: '系统管理',
  task: '任务管理',
  tenant: '租户管理',
  user: '用户管理',
  wechat: '微信',
  workflow: '工作流',
  my: '我的',
  // ── 检查平台 ──
  insp: '检查平台',
  inspection: '检查通用',
  inspection_appeal: '检查申诉',
  inspection_record: '检查记录',
  quantification: '量化检查',
  // ── 教育行业 ──
  academic: '学术管理',
  enrollment: '招生管理',
  student: '学生管理',
  teacher: '教师档案',
  teaching: '教学管理',
  dormitory: '宿舍管理',
  // ── 医疗行业 ──
  patient: '患者管理',
  ward: '病房管理',
}

/**
 * 某些模块码在不同行业下含义不同, 按 industry 覆盖。
 * 例: EDU 下 system:* 实际是 building / dormitory_building / semester, 非通用系统管理。
 */
export const MODULE_LABELS_BY_INDUSTRY: Record<string, Record<string, string>> = {
  EDU: {
    system: '学校设施/学期',
  },
}

/** 取模块中文标签; 缺失回退原始码。industry 可选, 用于上下文化覆盖。 */
export function getModuleLabel(moduleCode: string, industry?: string): string {
  return (
    (industry && MODULE_LABELS_BY_INDUSTRY[industry]?.[moduleCode]) ||
    MODULE_LABELS[moduleCode] ||
    moduleCode
  )
}
