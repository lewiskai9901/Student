import { reactive } from 'vue'

/**
 * 资源关系 (PROVIDER / RECORD_RELATION 锚点) 的中文标签注册表。
 *
 * <p>核心数据权限编辑器只认通用锚点 (owner_org/creator) 与 user↔org 关系; 行业插件自带的资源
 * 锚点 (如教育的 {@code taught_by} = 我任课的学生, PROVIDER resolver) 关系码是英文, 直接显码不友好。
 * 由行业插件前端入口通过 {@link registerResourceRelationLabels} 登记中文名 —— 与
 * {@code dataScopeSpecializations} / {@code relationScenes} 同一套"核心零行业词汇 + 插件贡献"门控理念。
 *
 * <p>登记只在插件前端入口加载时执行 (即插件启用时), 与该资源锚点出现的前提一致。
 */
const registry = reactive<Record<string, string>>({})

/** 行业插件前端入口加载时调用: 登记 {relationCode: 中文名}。 */
export function registerResourceRelationLabels(map: Record<string, string>): void {
  Object.assign(registry, map)
}

/** 资源关系码 → 中文名; 未登记返回 undefined (调用方回退显码)。 */
export function resourceRelationLabel(code: string): string | undefined {
  return registry[code]
}
