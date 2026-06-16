/**
 * useScopeLabels — 数据范围自然语言渲染的共享标签字典 + compose 逻辑.
 *
 * 把原先内嵌在 PreviewPanel.vue 里的 `composeAxisLine` (三轴 → 自然语言) 抽出为可复用单元,
 * 供新统一视图 DataScopeStudio.vue 与 PreviewPanel 共用 (UI 重设计 P2).
 *
 * 标签 (关系名 / 类型名 / 组织锚点措辞) **全部来自 API** —— 关系字典 relationTypeApi、
 * 类型字典 entityTypeApi —— 核心零行业硬编码 (守 no-industry-vocab).
 */
import { ref } from 'vue'
import type { ModulePermission } from '@/types/access'
import { relationTypeApi, type RelationTypeDef } from '@/api/relationType'
import { entityTypeApi } from '@/api/entityType'

/** 一个轴① 锚点 → 通用措辞 (无行业词汇). */
function anchorPhrase(
  mp: Pick<ModulePermission, 'orgAnchor' | 'anchorParam' | 'includeSubtree' | 'scopeCode'>,
  relLabel: (c: string) => string
): string {
  const anchor = mp.orgAnchor
  switch (anchor) {
    case 'ALL':
      return '全部组织'
    case 'SELF':
      return '仅本人'
    case 'PRIMARY_ORG':
      return '本组织'
    case 'RELATION':
      return mp.anchorParam ? `我「${relLabel(mp.anchorParam)}」的组织` : '按关系派生的组织'
    case 'CUSTOM_ORG':
      return '指定组织'
    case 'PLUGIN_DIM':
      return mp.anchorParam ? `维度「${mp.anchorParam}」` : '插件维度'
    default:
      return ''
  }
}

/**
 * 把单模块的可组合三轴拼成自然语言 (通用措辞, 标签全来自传入的 lookup):
 *   "我[关系]的组织 · 含下级 · 排除[关系]关系 · 仅[类型]类型"
 *
 * @param includeAnchor 为 true 时把轴①锚点措辞也并入 (DataScopeStudio 预览用整句);
 *   PreviewPanel 旧行为把锚点交给 badge 表达, 此处传 false 保持兼容。
 */
export function composeAxisLine(
  mp: Pick<
    ModulePermission,
    | 'orgAnchor'
    | 'anchorParam'
    | 'includeSubtree'
    | 'scopeCode'
    | 'subjectRelInclude'
    | 'subjectRelExclude'
    | 'typeFilter'
  >,
  relLabel: (c: string) => string,
  typeLabel: (c: string) => string,
  includeAnchor = false
): string {
  const parts: string[] = []

  if (includeAnchor) {
    const phrase = anchorPhrase(mp, relLabel)
    if (phrase) parts.push(phrase)
  } else {
    // 旧 PreviewPanel 行为: 仅 RELATION / PLUGIN_DIM 锚点补充措辞 (其余 preset badge 已表达)
    if (mp.orgAnchor === 'RELATION' && mp.anchorParam) {
      parts.push(`我「${relLabel(mp.anchorParam)}」的组织`)
    } else if (mp.orgAnchor === 'PLUGIN_DIM' && mp.anchorParam) {
      parts.push(`维度「${mp.anchorParam}」`)
    }
  }

  if (
    mp.includeSubtree &&
    (mp.orgAnchor === 'RELATION' || mp.orgAnchor === 'PRIMARY_ORG' || mp.orgAnchor === 'CUSTOM_ORG')
  ) {
    parts.push('含下级')
  }
  // 轴② 关系过滤
  if (mp.subjectRelExclude?.length) {
    parts.push(`排除「${mp.subjectRelExclude.map(relLabel).join('、')}」关系`)
  } else if (mp.subjectRelInclude?.length) {
    parts.push(`仅「${mp.subjectRelInclude.map(relLabel).join('、')}」关系`)
  }
  // 轴③ 类型过滤
  if (mp.typeFilter?.length) {
    parts.push(`仅「${mp.typeFilter.map(typeLabel).join('、')}」类型`)
  }
  return parts.join(' · ')
}

/**
 * 加载关系 / 类型字典并暴露 label lookup + 绑定 includeAnchor 的 compose helper.
 * 失败静默回退到 code 占位.
 */
export function useScopeLabels() {
  const relationLabels = ref<Record<string, string>>({})
  const typeLabels = ref<Record<string, string>>({})

  async function loadDicts() {
    try {
      const rels = (await relationTypeApi.list()) || []
      const rmap: Record<string, string> = {}
      rels.forEach((r: RelationTypeDef) => (rmap[r.relationCode] = r.relationName || r.relationCode))
      relationLabels.value = rmap
    } catch {
      /* 失败 → 用 code 占位 */
    }
    for (const entity of ['USER', 'PLACE', 'ORG_UNIT']) {
      try {
        const list = await entityTypeApi.list(entity)
        const tmap = { ...typeLabels.value }
        ;(list || []).forEach(t => (tmap[t.typeCode] = t.typeName || t.typeCode))
        typeLabels.value = tmap
      } catch {
        /* 忽略 */
      }
    }
  }

  function relLabel(code: string): string {
    return relationLabels.value[code] || code
  }
  function typeLabel(code: string): string {
    return typeLabels.value[code] || code
  }

  /** 绑定本字典的 compose; includeAnchor 默认 true (整句预览). */
  function compose(mp: Parameters<typeof composeAxisLine>[0], includeAnchor = true): string {
    return composeAxisLine(mp, relLabel, typeLabel, includeAnchor)
  }

  return { relationLabels, typeLabels, loadDicts, relLabel, typeLabel, compose }
}
