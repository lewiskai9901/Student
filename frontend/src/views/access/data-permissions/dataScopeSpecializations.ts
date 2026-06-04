import { reactive } from 'vue'

/**
 * 数据权限"行业特化维度"注册表。
 *
 * <p>通用核心的数据权限向导只认 5 个核心 scope (ALL/DEPARTMENT.../SELF/CUSTOM)。行业特化
 * 维度 (如教育的"我的学生" — 按班级/年级/专业切片) 由对应行业插件前端入口通过
 * {@link registerScopeSpecializations} 注册, 仅当该插件启用时出现在向导里 —— 与关系绑定场景
 * (relationScenes) / 路由按 EDU.enabled 条件注册同一套门控理念。核心代码零行业词汇。
 */
export interface ScopeSpecialization {
  /** 贡献插件码, 如 'EDU' */
  pluginCode: string
  /** 特化组码 (作为 SceneDecision.specializations 的 key), 如 'student' */
  groupCode: string
  /** 向导里该特化分区的标题, 如 '我的学生' */
  title: string
  /** 该特化覆盖的模块 code —— 这些模块用特化 scope 而非主决策 scope */
  moduleCodes: string[]
  /** 可选 scope (code 必须是后端 scope 字典里的有效项; label/desc 是主体视角措辞) */
  options: { code: string; label: string; desc?: string }[]
  /** 各特化 scope 的降级链 (模块 allowedScopes 不含目标时按此找替代) */
  fallbackChain?: Record<string, string[]>
}

const registry = reactive<Record<string, ScopeSpecialization[]>>({})

/** 行业插件前端入口加载时调用, 登记本插件的数据权限特化维度。 */
export function registerScopeSpecializations(pluginCode: string, specs: ScopeSpecialization[]): void {
  registry[pluginCode] = specs
}

/** 取所有"已启用"插件 (enabledCodes) 登记的特化维度。 */
export function enabledScopeSpecializations(enabledCodes: readonly string[]): ScopeSpecialization[] {
  return enabledCodes.flatMap(code => registry[code] ?? [])
}
