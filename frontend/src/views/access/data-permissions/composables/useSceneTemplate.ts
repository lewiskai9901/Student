/**
 * 场景模板 ↔ 模块 scope 映射
 * 核心创新: 让 "3 决策" 自动映射到 28 模块的具体 scope 配置
 *
 * 反推: 已有 modulePermissions 数据加载时自动反推出最接近的 SceneDecision,
 *       使用户不管是模板套用还是高级手工编辑, 回显都能一致
 */
import type { ModulePermission, ScopeItem } from '@/types/access'

/** 主决策: 数据可见范围 (对应后端 DataScope 枚举) */
export type PrimaryScope = 'ALL' | 'DEPARTMENT_AND_BELOW' | 'DEPARTMENT' | 'SELF' | 'CUSTOM'

/** 学生特化维度 (仅 EDU 插件启用时生效) */
export type StudentScope = 'ALL' | 'BY_CLASS' | 'BY_GRADE' | 'BY_MAJOR' | 'SELF'

/**
 * 场景决策: 3 个最核心的维度, 覆盖 99% 使用场景
 */
export interface SceneDecision {
  /** 决策 1: 基础可见范围 (必填) */
  primary: PrimaryScope
  /** 决策 2: 学生特化 (仅 EDU 启用时显示) */
  studentScope?: StudentScope
  /** 决策 3: 业务数据是否跟随主决策 (默认 true) */
  bizAutoFollow: boolean
  /** 决策 1 为 CUSTOM 时选中的组织单元 id 列表 */
  customOrgIds?: (number | string)[]
  /** 决策 1 为 CUSTOM 时选中的年级 id 列表 */
  customGradeIds?: (number | string)[]
  /** 决策 1 为 CUSTOM 时选中的班级 id 列表 */
  customClassIds?: (number | string)[]
}

/**
 * EDU 学生数据相关模块 (走 studentScope 的特化映射)
 *
 * 规则: 只要 moduleCode 匹配, 就优先用 decision.studentScope 而非 primary.
 * 因为学生数据通常希望按班级/年级而非部门切片.
 *
 * 这里的列表**故意硬编码**在 core 的对面 —— frontend/core 的 composable,
 * 列出的都是 EDU 插件贡献的典型 code; 即使 EDU 插件禁用时这些 code 不存在,
 * 映射也不会出错 (只对存在的 module 生成配置).
 */
export const EDU_STUDENT_MODULES = new Set([
  'student',
  'attendance',
  'grade_batch',
  'student_grade',
  'exam',
  'enrollment',
  'dormitory_student',
])

/** 模块最小接口 (PermissionConfigurator 里用的简化版) */
export interface SimpleModule {
  code: string
  industry: string
}

/**
 * 核心映射: SceneDecision → 每个 module 的 scope 配置
 *
 * 规则:
 *   - EDU_STUDENT_MODULES 且 decision.studentScope 有值 → 用 studentScope
 *   - 其他所有 module → 用 primary (bizAutoFollow=true 时)
 *   - CUSTOM 主决策时附带 scopeItems
 */
export function sceneToModuleScopes(
  decision: SceneDecision,
  modules: SimpleModule[],
  relevantCodes?: Set<string>
): Record<string, { scopeCode: string; scopeItems?: ScopeItem[] }> {
  const result: Record<string, { scopeCode: string; scopeItems?: ScopeItem[] }> = {}

  const customItems: ScopeItem[] = []
  if (decision.primary === 'CUSTOM') {
    decision.customOrgIds?.forEach(id =>
      customItems.push({ itemTypeCode: 'ORG_UNIT', scopeId: id, scopeName: '', includeChildren: true })
    )
    decision.customGradeIds?.forEach(id =>
      customItems.push({ itemTypeCode: 'GRADE', scopeId: id, scopeName: '', includeChildren: false })
    )
    decision.customClassIds?.forEach(id =>
      customItems.push({ itemTypeCode: 'CLASS', scopeId: id, scopeName: '', includeChildren: false })
    )
  }

  for (const mod of modules) {
    const code = mod.code

    // 智能过滤: 非相关模块 (跨行业 / 权限未匹配) 保持 SELF (最小权限默认)
    if (relevantCodes && !relevantCodes.has(code)) {
      result[code] = { scopeCode: 'SELF' }
      continue
    }

    // EDU 学生数据特化
    if (EDU_STUDENT_MODULES.has(code) && decision.studentScope) {
      // BY_CLASS / BY_GRADE / BY_MAJOR 目前映射为自定义 (由实际选择的 class/grade 决定)
      // 如果用户没选具体 class, 退回 ALL
      const mapped = mapStudentScopeToSystemScope(decision.studentScope)
      result[code] = { scopeCode: mapped }
      continue
    }

    // 业务跟随: 直接用 primary
    if (decision.bizAutoFollow) {
      result[code] = {
        scopeCode: decision.primary,
        scopeItems: decision.primary === 'CUSTOM' ? customItems : undefined,
      }
    } else {
      // bizAutoFollow=false 时不跟随 → 默认 SELF (保守)
      result[code] = { scopeCode: 'SELF' }
    }
  }

  return result
}

/**
 * StudentScope → 系统 DataScope 代码的降级映射
 *
 * BY_CLASS / BY_GRADE / BY_MAJOR 目前在后端 DataScope 中没有一等公民,
 * 先全部映射成 ALL (走 access_relations 由运行时过滤),
 * 等后端加上对应 scope 后改成直接传递
 */
function mapStudentScopeToSystemScope(s: StudentScope): string {
  switch (s) {
    case 'ALL':
      return 'ALL'
    case 'SELF':
      return 'SELF'
    case 'BY_CLASS':
    case 'BY_GRADE':
    case 'BY_MAJOR':
      // 这三个是用户侧概念, 后端目前通过 access_relations 过滤
      // 所以 scope 给 ALL, 让 relation 做实际过滤
      return 'ALL'
    default:
      return 'SELF'
  }
}

/**
 * 反推: 已有 modulePermissions → 最接近的 SceneDecision
 *
 * round-trip 一致性:
 *   - 模板 → Scene → Modules → 存储 → 读取 → Modules → Scene (反推)
 *   - 一致性目标: 反推结果套用回去生成的 Modules 和原始 Modules 一致
 *
 * 策略:
 *   1. 排除 EDU_STUDENT_MODULES, 统计其余模块最常见的 scope 作 primary
 *   2. student 模块的 scope 反推 studentScope (若不在 EDU set 就忽略)
 *   3. bizAutoFollow: 如果 "非 student 模块的 scope 都一致" 就 true
 *   4. CUSTOM 时合并所有 scopeItems
 */
export function moduleScopesToScene(mps: ModulePermission[]): SceneDecision {
  if (!mps || mps.length === 0) {
    return { primary: 'SELF', bizAutoFollow: true }
  }

  // 分离 student-like 模块和普通模块
  const studentMps = mps.filter(m => EDU_STUDENT_MODULES.has(m.moduleCode))
  const otherMps = mps.filter(m => !EDU_STUDENT_MODULES.has(m.moduleCode))

  // primary: 普通模块最常出现的 scope
  const target = otherMps.length > 0 ? otherMps : mps
  const scopeCount: Record<string, number> = {}
  target.forEach(mp => {
    const code = mp.scopeCode || 'SELF'
    scopeCount[code] = (scopeCount[code] || 0) + 1
  })
  const primary = (Object.entries(scopeCount).sort((a, b) => b[1] - a[1])[0]?.[0] || 'SELF') as PrimaryScope

  // 业务自动跟随: 所有非 student 模块 scope 一致
  const uniqueScopes = new Set(otherMps.map(m => m.scopeCode || 'SELF'))
  const bizAutoFollow = uniqueScopes.size <= 1

  // studentScope: student 模块的典型 scope
  let studentScope: StudentScope | undefined
  if (studentMps.length > 0) {
    const studentCode = studentMps[0].scopeCode || 'SELF'
    // 目前 BY_CLASS/BY_GRADE/BY_MAJOR 都映射为 ALL, 反推时保守用 ALL
    if (['ALL', 'SELF'].includes(studentCode)) {
      studentScope = studentCode as StudentScope
    }
  }

  // CUSTOM: 合并所有 scopeItems
  const customOrgIds: (number | string)[] = []
  const customGradeIds: (number | string)[] = []
  const customClassIds: (number | string)[] = []
  if (primary === 'CUSTOM') {
    const seen = new Set<string>()
    for (const mp of mps) {
      if (mp.scopeCode !== 'CUSTOM' || !mp.scopeItems) continue
      for (const it of mp.scopeItems) {
        const key = `${it.itemTypeCode}-${it.scopeId}`
        if (seen.has(key)) continue
        seen.add(key)
        if (it.itemTypeCode === 'ORG_UNIT') customOrgIds.push(it.scopeId)
        else if (it.itemTypeCode === 'GRADE') customGradeIds.push(it.scopeId)
        else if (it.itemTypeCode === 'CLASS') customClassIds.push(it.scopeId)
      }
    }
  }

  return {
    primary,
    studentScope,
    bizAutoFollow,
    customOrgIds: customOrgIds.length ? customOrgIds : undefined,
    customGradeIds: customGradeIds.length ? customGradeIds : undefined,
    customClassIds: customClassIds.length ? customClassIds : undefined,
  }
}

/**
 * SceneDecision 合并到现有 modulePermissions (非破坏性)
 * - 没有现有配置的模块: 按 Scene 新增
 * - 已有配置的模块: 覆盖 scope + scopeItems
 */
export function applySceneToModules(
  decision: SceneDecision,
  modules: SimpleModule[],
  existing: ModulePermission[],
  relevantCodes?: Set<string>
): ModulePermission[] {
  const mapping = sceneToModuleScopes(decision, modules, relevantCodes)
  const result: ModulePermission[] = []
  const seen = new Set<string>()

  for (const mod of modules) {
    const m = mapping[mod.code]
    if (!m) continue
    seen.add(mod.code)
    result.push({
      moduleCode: mod.code,
      scopeCode: m.scopeCode,
      scopeItems: m.scopeItems,
    })
  }

  // 保留 existing 中不在 modules 列表的 (防止误删禁用插件的模块配置)
  for (const e of existing) {
    if (!seen.has(e.moduleCode)) {
      result.push(e)
    }
  }

  return result
}
