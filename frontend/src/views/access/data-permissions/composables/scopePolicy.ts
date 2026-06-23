/**
 * scopePolicy — 数据范围"默认 + 例外"加载推断 / 保存展开 纯函数核心 (UI 重设计 P3).
 *
 * 新 UI 把一个角色的数据权限建模为:
 *   一条「默认范围」(仅轴① 组织锚点, 适用所有资源) + 若干「资源例外」(完整三轴).
 * 后端仍是 per-resource (`role_data_scopes` 行) 的扁平列表 (ModulePermission[]).
 *
 * 本模块是两者之间的纯函数桥:
 *   - LOAD  = inferDefaultAndExceptions: 从 per-resource 列表推断 (众数默认 + 例外).
 *   - SAVE  = expandToCommands: 把 (默认 + 例外) 展开回 per-resource 列表.
 *
 * 见 docs/plans/2026-06-16-data-permission-ui-redesign-design.md §3, §7.
 * 关键不变量: infer(expand(def, exc, codes)) 语义上还原 {def, exc} (round-trip 稳定).
 */
import type { ModulePermission, OrgAnchor } from '@/types/access'
import type { ScopeSpecVM } from '../components/ScopeBuilder.vue'

/** 一个资源例外: 模块码 + 其完整三轴 spec. */
export interface ResourceException {
  moduleCode: string
  spec: ScopeSpecVM
}

// ──────────────────────────────────────────────────────────────────────────
// 提取: ModulePermission → ScopeSpecVM (只取轴字段). 无 orgAnchor 视作 SELF.
// ──────────────────────────────────────────────────────────────────────────
export function toSpec(m: ModulePermission): ScopeSpecVM {
  return {
    orgAnchor: m.orgAnchor || 'SELF',
    anchorParam: m.anchorParam,
    includeSubtree: m.includeSubtree,
    customOrgIds: m.customOrgIds,
    subjectRelInclude: m.subjectRelInclude,
    subjectRelExclude: m.subjectRelExclude,
    typeFilter: m.typeFilter,
    // R3/R4: 仅多锚点 (>1 grant) 时承载原始 grants; 单 grant 走常规三轴 (后端按 scopeCode 派生).
    relationGrants:
      m.relationGrants && m.relationGrants.length > 1 ? m.relationGrants : undefined,
  }
}

/** spec 是否为多锚点配置 (>1 条关系授予). */
function isMultiGrant(spec: ScopeSpecVM): boolean {
  return (spec.relationGrants?.length ?? 0) > 1
}

/** 该 spec 的有效组织锚点 (缺省 = SELF). */
function effectiveAnchor(spec: ScopeSpecVM): OrgAnchor {
  return spec.orgAnchor || 'SELF'
}

// ──────────────────────────────────────────────────────────────────────────
// Helpers (exported, testable)
// ──────────────────────────────────────────────────────────────────────────

/**
 * 轴① 稳定签名 = (orgAnchor | anchorParam | includeSubtree).
 * 忽略轴②③ — 用于"两个 spec 是否同一组织锚点"的比较与众数统计.
 */
export function axis1Signature(spec: ScopeSpecVM): string {
  const anchor = effectiveAnchor(spec)
  const param = spec.anchorParam || ''
  const subtree = spec.includeSubtree ? '1' : '0'
  return `${anchor}|${param}|${subtree}`
}

/**
 * 轴① 代表"宽度"等级 (众数 tie-break: 越宽越优先).
 * ALL→100, PRIMARY_ORG+subtree→80, RELATION+subtree→75, RELATION→65,
 * PRIMARY_ORG→60, CUSTOM_ORG→40, SELF / 其他→20.
 */
export function anchorLevel(spec: ScopeSpecVM): number {
  const anchor = effectiveAnchor(spec)
  const subtree = !!spec.includeSubtree
  switch (anchor) {
    case 'ALL':
      return 100
    case 'PRIMARY_ORG':
      return subtree ? 80 : 60
    case 'RELATION':
      return subtree ? 75 : 65
    case 'CUSTOM_ORG':
      return 40
    case 'SELF':
      return 20
    default:
      // PLUGIN_DIM / 未知锚点: 视作较窄的非兜底维度
      return 20
  }
}

/**
 * 轴① → 最接近的 legacy preset scopeCode (向后兼容 + 后端校验).
 * 后端有三轴时优先用三轴, scopeCode 只是"最接近的允许预设".
 *   - ALL→'ALL', SELF→'SELF'
 *   - PRIMARY_ORG → 含下级 'DEPARTMENT_AND_BELOW' 否则 'DEPARTMENT'
 *   - RELATION → 含下级 'MANAGED_ORGS_AND_BELOW' 否则 'MANAGED_ORGS' (无视 anchorParam)
 *   - CUSTOM_ORG → 'CUSTOM'
 *   - PLUGIN_DIM → anchorParam (维度码本身)
 */
export function scopeCodeFromAxis1(spec: ScopeSpecVM): string {
  const anchor = effectiveAnchor(spec)
  const subtree = !!spec.includeSubtree
  switch (anchor) {
    case 'ALL':
      return 'ALL'
    case 'SELF':
      return 'SELF'
    case 'PRIMARY_ORG':
      return subtree ? 'DEPARTMENT_AND_BELOW' : 'DEPARTMENT'
    case 'RELATION':
      return subtree ? 'MANAGED_ORGS_AND_BELOW' : 'MANAGED_ORGS'
    case 'CUSTOM_ORG':
      return 'CUSTOM'
    case 'PLUGIN_DIM':
      return spec.anchorParam || 'SELF'
    default:
      return 'SELF'
  }
}

/**
 * preset scopeCode → 完整轴① spec (scopeCodeFromAxis1 的逆).
 * 七个核心预设各自的轴① 投影 (②③ 留空):
 *   ALL→{ALL}, SELF→{SELF},
 *   DEPARTMENT→{PRIMARY_ORG}, DEPARTMENT_AND_BELOW→{PRIMARY_ORG,subtree},
 *   MANAGED_ORGS→{RELATION,admin}, MANAGED_ORGS_AND_BELOW→{RELATION,admin,subtree},
 *   CUSTOM→{CUSTOM_ORG}.
 * 未知码 (如 PLUGIN_DIM 维度码) → 兜底 {SELF}.
 */
export function presetCodeToSpec(code: string): ScopeSpecVM {
  switch (code) {
    case 'ALL':
      return { orgAnchor: 'ALL' }
    case 'DEPARTMENT':
      return { orgAnchor: 'PRIMARY_ORG' }
    case 'DEPARTMENT_AND_BELOW':
      return { orgAnchor: 'PRIMARY_ORG', includeSubtree: true }
    case 'MANAGED_ORGS':
      return { orgAnchor: 'RELATION', anchorParam: 'admin' }
    case 'MANAGED_ORGS_AND_BELOW':
      return { orgAnchor: 'RELATION', anchorParam: 'admin', includeSubtree: true }
    case 'CUSTOM':
      return { orgAnchor: 'CUSTOM_ORG', customOrgIds: [] }
    case 'SELF':
    default:
      return { orgAnchor: 'SELF' }
  }
}

/**
 * 把一个轴① spec 钳制到资源 `allowedScopes` 允许的范围内.
 *
 * 安全属性: 返回 spec 的 preset 码恒 ∈ allowed (当 allowed 非空), 且
 * "永不放宽" — 选出的范围 anchorLevel 永不超过原默认的 anchorLevel.
 *
 * 规则:
 *  - allowed 为 null/undefined/空 → 无限制, 原样返回 spec.
 *  - 默认码 ∈ allowed → 原样返回 spec.
 *  - 否则回落到"最宽但不宽于默认"的允许码; 若全部都比默认宽, 取最窄的允许码;
 *    最终兜底 = 第一个允许码.
 */
export function clampSpecToAllowed(
  spec: ScopeSpecVM,
  allowed?: string[] | null
): ScopeSpecVM {
  if (!allowed || !allowed.length) return spec

  const code = scopeCodeFromAxis1(spec)
  if (allowed.includes(code)) return spec

  const defLevel = anchorLevel(spec)
  // 每个允许码 → 其 spec + level, 用于挑选.
  const candidates = allowed.map(c => {
    const s = presetCodeToSpec(c)
    return { code: c, level: anchorLevel(s) }
  })

  // 1) 最宽但不宽于默认 (level ≤ defLevel, level 最大).
  let pick: { code: string; level: number } | null = null
  for (const c of candidates) {
    if (c.level <= defLevel && (!pick || c.level > pick.level)) pick = c
  }
  // 2) 全部比默认宽 → 取最窄 (level 最小).
  if (!pick) {
    for (const c of candidates) {
      if (!pick || c.level < pick.level) pick = c
    }
  }
  // 3) 兜底 (理论不可达, candidates 非空).
  const chosen = pick ? pick.code : allowed[0]
  return presetCodeToSpec(chosen)
}

/** spec 是否带有轴②③ (关系过滤 / 类型过滤). */
function hasAxis23(spec: ScopeSpecVM): boolean {
  return (
    !!spec.subjectRelInclude?.length ||
    !!spec.subjectRelExclude?.length ||
    !!spec.typeFilter?.length
  )
}

/** 仅取轴① 的 spec (默认范围下发用; ②③ 留空). */
function axis1Only(spec: ScopeSpecVM): ScopeSpecVM {
  const anchor = effectiveAnchor(spec)
  return {
    orgAnchor: anchor,
    anchorParam: spec.anchorParam,
    includeSubtree: !!spec.includeSubtree,
    customOrgIds: anchor === 'CUSTOM_ORG' ? spec.customOrgIds || [] : [],
  }
}

// ──────────────────────────────────────────────────────────────────────────
// LOAD: inferDefaultAndExceptions
// ──────────────────────────────────────────────────────────────────────────

/**
 * 从 per-resource 列表推断「默认范围 + 资源例外」.
 *
 * - 每个模块 → 其 ScopeSpecVM (无 orgAnchor 视作 SELF).
 * - 默认 = 出现最多的轴① 签名 (忽略②③); 平票 → 取 anchorLevel 最宽的.
 * - 一个模块是「例外」当: 其轴① 签名 ≠ 默认, 或 它带任意②③.
 *   匹配默认轴① 且无②③ → 继承, 不是例外.
 * - 空输入 → 默认 SELF, 例外 [].
 */
export function inferDefaultAndExceptions(modules: ModulePermission[]): {
  defaultSpec: ScopeSpecVM
  exceptions: ResourceException[]
} {
  if (!modules.length) {
    return { defaultSpec: { orgAnchor: 'SELF' }, exceptions: [] }
  }

  const specs = modules.map(m => ({ moduleCode: m.moduleCode, spec: toSpec(m) }))

  // 统计轴① 签名频次 + 留一个代表 spec (用于 tie-break level).
  const counts = new Map<string, { count: number; repr: ScopeSpecVM }>()
  for (const { spec } of specs) {
    const sig = axis1Signature(spec)
    const entry = counts.get(sig)
    if (entry) {
      entry.count++
    } else {
      counts.set(sig, { count: 1, repr: spec })
    }
  }

  // 众数: 频次最高; 平票取最宽 (anchorLevel 最大).
  let bestSig = ''
  let best: { count: number; repr: ScopeSpecVM } | null = null
  for (const [sig, entry] of counts) {
    if (
      !best ||
      entry.count > best.count ||
      (entry.count === best.count && anchorLevel(entry.repr) > anchorLevel(best.repr))
    ) {
      best = entry
      bestSig = sig
    }
  }

  const defaultSpec = axis1Only(best!.repr)

  const exceptions: ResourceException[] = []
  for (const { moduleCode, spec } of specs) {
    const differsAxis1 = axis1Signature(spec) !== bestSig
    // 多锚点 (>1 grant) 恒为例外 — 默认范围只承载单一轴①, 不能折叠多 grant.
    if (differsAxis1 || hasAxis23(spec) || isMultiGrant(spec)) {
      exceptions.push({ moduleCode, spec })
    }
  }

  return { defaultSpec, exceptions }
}

// ──────────────────────────────────────────────────────────────────────────
// SAVE: expandToCommands
// ──────────────────────────────────────────────────────────────────────────

/** spec → ModulePermission (携带派生 scopeCode + 三轴字段). */
export function specToCommand(moduleCode: string, spec: ScopeSpecVM): ModulePermission {
  return {
    moduleCode,
    scopeCode: scopeCodeFromAxis1(spec),
    orgAnchor: effectiveAnchor(spec),
    anchorParam: spec.anchorParam,
    includeSubtree: !!spec.includeSubtree,
    customOrgIds: spec.customOrgIds,
    subjectRelInclude: spec.subjectRelInclude,
    subjectRelExclude: spec.subjectRelExclude,
    typeFilter: spec.typeFilter,
    // R3/R4: 多锚点例外原样下发 relationGrants → 后端 saveRolePermission 优先采用 (跳过 scopeCode 派生).
    relationGrants: spec.relationGrants,
  }
}

/**
 * 把「默认范围 + 资源例外」展开回 per-resource 列表 (一次 PUT 下发).
 *
 * - 例外资源 → 用该例外的完整三轴 spec, **原样下发不钳制**
 *   (per-resource ScopeBuilder 已按能力门控过例外, 必然合法).
 * - 其余资源 → 取默认 spec (仅轴①), 再**按该资源 `allowedScopes` 钳制**:
 *   保证下发的 scopeCode ∈ 该资源允许集 (非空时), 且轴① 字段与之一致 ——
 *   既防后端校验 400, 又防"对无 org 字段资源套 DEPARTMENT 生成坏 SQL", 且永不放宽.
 * - 每条命令携带派生 scopeCode (legacy preset) + 三轴字段, 后端优先采用三轴.
 *
 * @param allowedScopesByCode  每资源允许的 preset 码 (来自 M1 能力声明);
 *   缺省 / null / 空 = 不限制该资源, 默认原样透传.
 */
export function expandToCommands(
  defaultSpec: ScopeSpecVM,
  exceptions: ResourceException[],
  allModuleCodes: string[],
  allowedScopesByCode?: Record<string, string[] | null | undefined>
): ModulePermission[] {
  const exMap = new Map(exceptions.map(e => [e.moduleCode, e.spec]))
  const defOnly = axis1Only(defaultSpec)

  return allModuleCodes.map(code => {
    const ex = exMap.get(code)
    if (ex) return specToCommand(code, ex)
    // 非例外资源: 默认范围按本资源 allowedScopes 钳制后下发.
    const allowed = allowedScopesByCode?.[code]
    const clamped = clampSpecToAllowed(defOnly, allowed)
    return specToCommand(code, clamped)
  })
}
