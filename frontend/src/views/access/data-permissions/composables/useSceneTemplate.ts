/**
 * 场景决策 <> 模块 scope 映射 (通用核心, 零行业概念)
 *
 * 主决策 (primary) 只用 5 个核心 scope。行业特化维度 (如教育"我的学生" BY_CLASS) 由插件通过
 * dataScopeSpecializations 注册, 以 `specs` 参数传入本 composable —— 这里不认识任何具体行业概念,
 * 只按 spec.moduleCodes 把覆盖的模块映射到 decision.specializations[spec.groupCode] 选中的 scope。
 *
 * 反推: 已有 modulePermissions 加载时自动反推出最接近的 SceneDecision, 保证模板/手工编辑回显一致。
 */
import type { ModulePermission, ScopeItem } from '@/types/access'
import type { ScopeSpecialization } from '../dataScopeSpecializations'

/** 主决策: 数据可见范围 (对应后端核心 DataScope 枚举) */
export type PrimaryScope = 'ALL' | 'DEPARTMENT_AND_BELOW' | 'DEPARTMENT' | 'SELF' | 'CUSTOM'

/**
 * 场景决策。
 *  - primary: 基础可见范围 (必填)
 *  - specializations: 行业特化 groupCode(如 'student') -> 选中 scope(如 'BY_CLASS'), 由插件 spec 定义
 *  - bizAutoFollow: 业务数据是否跟随主决策
 *  - customOrgIds: CUSTOM 主决策时选中的组织单元 id (所有自定义项统一为 ORG_UNIT 范围项)
 */
export interface SceneDecision {
  primary: PrimaryScope
  specializations?: Record<string, string>
  bizAutoFollow: boolean
  customOrgIds?: (number | string)[]
}

/** 模块最小接口 */
export interface SimpleModule {
  code: string
  industry: string
  /** 本模块支持的 scope 代码数组; null/undefined 表示默认全集 */
  allowedScopes?: string[] | null
}

/** 核心 scope 的降级阶梯; 行业 scope 的降级由各 spec.fallbackChain 在运行时合并进来。 */
const CORE_FALLBACK_CHAIN: Record<string, string[]> = {
  ALL: ['DEPARTMENT_AND_BELOW', 'DEPARTMENT', 'SELF'],
  DEPARTMENT_AND_BELOW: ['DEPARTMENT', 'ALL', 'SELF'],
  DEPARTMENT: ['DEPARTMENT_AND_BELOW', 'ALL', 'SELF'],
  SELF: [],
  CUSTOM: ['SELF'],
}

/** 从启用的 specs 构建 module->spec 索引 + 合并的 fallback 链。 */
function buildSpecIndex(specs: ScopeSpecialization[]): {
  moduleToSpec: Map<string, ScopeSpecialization>
  fallback: Record<string, string[]>
} {
  const moduleToSpec = new Map<string, ScopeSpecialization>()
  const fallback: Record<string, string[]> = { ...CORE_FALLBACK_CHAIN }
  for (const spec of specs) {
    for (const code of spec.moduleCodes) moduleToSpec.set(code, spec)
    if (spec.fallbackChain) Object.assign(fallback, spec.fallbackChain)
  }
  return { moduleToSpec, fallback }
}

/** 解析目标 scope 对模块是否可用, 不可用则按 fallbackChain 找首个可用替代; 都不行兜底 SELF。 */
function resolveScopeWithFallback(
  target: string,
  allowed: string[] | null | undefined,
  fallbackChain: Record<string, string[]>,
): { final: string; fallback: boolean } {
  if (!allowed || allowed.length === 0) return { final: target, fallback: false }
  if (allowed.includes(target)) return { final: target, fallback: false }
  for (const candidate of fallbackChain[target] || []) {
    if (allowed.includes(candidate)) return { final: candidate, fallback: true }
  }
  return { final: allowed.includes('SELF') ? 'SELF' : allowed[0], fallback: true }
}

/** Fallback 降级提示 (给 PreviewPanel / UI 使用) */
export interface ScopeFallbackInfo {
  moduleCode: string
  moduleName?: string
  from: string
  to: string
}

/**
 * 核心映射: SceneDecision -> 每个 module 的 scope 配置。
 *   - 被某 spec 覆盖 (mod.code ∈ spec.moduleCodes) 且 decision.specializations[spec.groupCode] 有值 -> 用该特化 scope
 *   - 其他 module -> 用 primary (bizAutoFollow=true 时), 否则 SELF
 *   - 模块感知: 目标 scope 不在 module.allowedScopes 时按 fallback 降级
 */
export function sceneToModuleScopes(
  decision: SceneDecision,
  modules: SimpleModule[],
  specs: ScopeSpecialization[],
  relevantCodes?: Set<string>
): {
  scopes: Record<string, { scopeCode: string; scopeItems?: ScopeItem[] }>
  fallbacks: ScopeFallbackInfo[]
} {
  const { moduleToSpec, fallback } = buildSpecIndex(specs)
  const scopes: Record<string, { scopeCode: string; scopeItems?: ScopeItem[] }> = {}
  const fallbacks: ScopeFallbackInfo[] = []

  const customItems: ScopeItem[] = []
  if (decision.primary === 'CUSTOM') {
    decision.customOrgIds?.forEach(id =>
      customItems.push({ itemTypeCode: 'ORG_UNIT', scopeId: String(id), scopeName: '', includeChildren: true })
    )
  }

  for (const mod of modules) {
    const code = mod.code

    if (relevantCodes && !relevantCodes.has(code)) {
      const { final, fallback: fb } = resolveScopeWithFallback('SELF', mod.allowedScopes, fallback)
      scopes[code] = { scopeCode: final }
      if (fb) fallbacks.push({ moduleCode: code, from: 'SELF', to: final })
      continue
    }

    const spec = moduleToSpec.get(code)
    const specScope = spec && decision.specializations ? decision.specializations[spec.groupCode] : undefined

    let target: string
    if (spec && specScope) {
      target = specScope
    } else if (decision.bizAutoFollow) {
      target = decision.primary
    } else {
      target = 'SELF'
    }

    const { final, fallback: fb } = resolveScopeWithFallback(target, mod.allowedScopes, fallback)
    const scopeItems = final === 'CUSTOM' ? customItems : undefined
    scopes[code] = { scopeCode: final, scopeItems }
    if (fb) fallbacks.push({ moduleCode: code, from: target, to: final })
  }

  return { scopes, fallbacks }
}

/**
 * 反推: 已有 modulePermissions -> 最接近的 SceneDecision (通用)。
 *   1. 非 spec 覆盖的模块里最常见 scope 作 primary
 *   2. 每个 spec: 看它覆盖的模块的 scope, 若是该 spec 的合法选项就反推为该特化值
 *   3. bizAutoFollow: 非 spec 模块 scope 是否一致
 *   4. CUSTOM 时合并所有 scopeItems
 */
export function moduleScopesToScene(mps: ModulePermission[], specs: ScopeSpecialization[]): SceneDecision {
  if (!mps || mps.length === 0) {
    return { primary: 'SELF', bizAutoFollow: true }
  }
  const { moduleToSpec } = buildSpecIndex(specs)

  const otherMps = mps.filter(m => !moduleToSpec.has(m.moduleCode))

  // primary: 非 spec 模块最常出现的 scope
  const target = otherMps.length > 0 ? otherMps : mps
  const scopeCount: Record<string, number> = {}
  target.forEach(mp => {
    const code = mp.scopeCode || 'SELF'
    scopeCount[code] = (scopeCount[code] || 0) + 1
  })
  const primary = (Object.entries(scopeCount).sort((a, b) => b[1] - a[1])[0]?.[0] || 'SELF') as PrimaryScope

  // 业务自动跟随: 所有非 spec 模块 scope 一致
  const uniqueScopes = new Set(otherMps.map(m => m.scopeCode || 'SELF'))
  const bizAutoFollow = uniqueScopes.size <= 1

  // specializations: 每个 spec 反推其覆盖模块的典型 scope (须是该 spec 的合法选项)
  const specializations: Record<string, string> = {}
  for (const spec of specs) {
    const covered = mps.filter(m => spec.moduleCodes.includes(m.moduleCode))
    if (covered.length === 0) continue
    const sc = covered[0].scopeCode || 'SELF'
    if (spec.options.some(o => o.code === sc)) {
      specializations[spec.groupCode] = sc
    }
  }

  // CUSTOM: 合并所有 scopeItems (自定义项都是 org_unit, 统一收集为 customOrgIds)
  const customOrgIds: (number | string)[] = []
  if (primary === 'CUSTOM') {
    const seen = new Set<string>()
    for (const mp of mps) {
      if (mp.scopeCode !== 'CUSTOM' || !mp.scopeItems) continue
      for (const it of mp.scopeItems) {
        const key = String(it.scopeId)
        if (seen.has(key)) continue
        seen.add(key)
        customOrgIds.push(it.scopeId)
      }
    }
  }

  return {
    primary,
    specializations: Object.keys(specializations).length ? specializations : undefined,
    bizAutoFollow,
    customOrgIds: customOrgIds.length ? customOrgIds : undefined,
  }
}

/**
 * SceneDecision 合并到现有 modulePermissions (非破坏性)。
 * @returns { modulePermissions, fallbacks } — fallbacks 给 UI 提示用
 */
export function applySceneToModules(
  decision: SceneDecision,
  modules: SimpleModule[],
  existing: ModulePermission[],
  specs: ScopeSpecialization[],
  relevantCodes?: Set<string>
): { modulePermissions: ModulePermission[]; fallbacks: ScopeFallbackInfo[] } {
  const { scopes, fallbacks } = sceneToModuleScopes(decision, modules, specs, relevantCodes)
  const result: ModulePermission[] = []
  const seen = new Set<string>()
  // 高级配置的轴 (类型过滤②③ + 组织锚点①) 是 scene 之外的正交维度 —
  // 场景只设主决策的组织范围, 不应抹掉已手工配置的高级轴。
  const advancedByCode = new Map<string, Partial<ModulePermission>>(
    existing.map(e => [
      e.moduleCode,
      {
        ...(e.typeFilter && e.typeFilter.length ? { typeFilter: e.typeFilter } : {}),
        ...(e.orgAnchor ? { orgAnchor: e.orgAnchor } : {}),
        ...(e.anchorParam ? { anchorParam: e.anchorParam } : {}),
        ...(e.includeSubtree != null ? { includeSubtree: e.includeSubtree } : {}),
        ...(e.customOrgIds && e.customOrgIds.length ? { customOrgIds: e.customOrgIds } : {}),
        ...(e.subjectRelInclude && e.subjectRelInclude.length
          ? { subjectRelInclude: e.subjectRelInclude }
          : {}),
        ...(e.subjectRelExclude && e.subjectRelExclude.length
          ? { subjectRelExclude: e.subjectRelExclude }
          : {}),
      },
    ])
  )

  for (const mod of modules) {
    const m = scopes[mod.code]
    if (!m) continue
    seen.add(mod.code)
    const adv = advancedByCode.get(mod.code) ?? {}
    result.push({
      moduleCode: mod.code,
      scopeCode: m.scopeCode,
      scopeItems: m.scopeItems,
      ...adv,
    })
  }

  // 保留 existing 中不在 modules 列表的 (防止误删禁用插件的模块配置)
  for (const e of existing) {
    if (!seen.has(e.moduleCode)) {
      result.push(e)
    }
  }

  return { modulePermissions: result, fallbacks }
}
