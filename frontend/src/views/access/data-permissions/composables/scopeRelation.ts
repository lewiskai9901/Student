/**
 * scopeRelation — 纯关系化数据范围编辑器的 UI 选项 ↔ grant 映射 (纯函数, 可测)。
 *
 * 设计: docs/plans/2026-06-22-pure-relation-scope-editor-design.md
 * 一切皆"经[关系]"选定: 创建者 / 全部 / 和我有[org 关系]的组织 / 资源关系(PROVIDER·复核) / 插件维度(保全)。
 * 多条件 OR。后端 grant 模型 ({relation, subject, subjectParam, subtree}) 已支持, 零后端改。
 */
import type { RelationGrant, ModulePermission } from '@/types/access'

export interface RelOption {
  /** 选项唯一键 (creator / all / org:<code> / res:<code> / dim:<code>) */
  key: string
  /** 人话标签 */
  label: string
  /** 类别: 特殊 creator/all; org=用户↔组织关系; res=资源关系(PROVIDER/RECORD); dim=插件维度(保全) */
  kind: 'creator' | 'all' | 'org' | 'res' | 'dim'
  /** org/res/dim 的关系码/维度码 */
  code?: string
}

/** 一条 grant → 对应 UI 选项键 (含 legacy MY_ORG→成员 / owner_org+SELF→创建者 迁移)。 */
export function grantToKey(g: RelationGrant): string {
  if (g.relation === 'creator') return 'creator'
  if (g.relation === 'owner_org') {
    switch (g.subject) {
      case 'ALL': return 'all'
      case 'MY_ORG': return 'org:member' // legacy 本组织 = 成员关系
      case 'SELF': return 'creator' // 成员主体自身 ≈ 我创建的
      case 'RELATION': return 'org:' + (g.subjectParam || 'member')
      case 'PLUGIN_DIM': return 'dim:' + (g.subjectParam || '')
      case 'CUSTOM': return 'custom' // 指定组织 (保全, 无 picker 不可重选)
    }
  }
  // 其它 relation (reviewer/inspected/taught_by…) = 资源关系, subject SELF
  return 'res:' + g.relation
}

/** UI 选项键 + 含下级 → grant。 */
export function keyToGrant(key: string, subtree: boolean): RelationGrant {
  if (key === 'creator') return { relation: 'creator', subject: 'SELF' }
  if (key === 'all') return { relation: 'owner_org', subject: 'ALL' }
  if (key.startsWith('org:')) {
    return { relation: 'owner_org', subject: 'RELATION', subjectParam: key.slice(4), subtree: !!subtree }
  }
  if (key.startsWith('dim:')) {
    return { relation: 'owner_org', subject: 'PLUGIN_DIM', subjectParam: key.slice(4) }
  }
  if (key.startsWith('res:')) {
    return { relation: key.slice(4), subject: 'SELF' }
  }
  if (key === 'custom') {
    return { relation: 'owner_org', subject: 'CUSTOM', orgIds: [] } // 重选无 picker → 空 (载入保全靠原 grant 不变)
  }
  return { relation: 'creator', subject: 'SELF' }
}

/**
 * 载入: 一条 role config (ModulePermission) → grant 数组 (一切皆关系)。
 * 后端 GET 已对每个已配模块回传 relationGrants → 直接用; 否则从 legacy 轴① 字段派生 (保全 PLUGIN_DIM/CUSTOM)。
 * 未配模块 → [创建者] (= SELF 兜底)。
 */
export function permissionToGrants(mp: ModulePermission | undefined): RelationGrant[] {
  if (!mp) return [{ relation: 'creator', subject: 'SELF' }]
  if (mp.relationGrants && mp.relationGrants.length) {
    return mp.relationGrants.map(g => ({ ...g }))
  }
  // legacy 轴① → 单 grant
  const subtree = !!mp.includeSubtree
  switch (mp.orgAnchor) {
    case 'ALL': return [{ relation: 'owner_org', subject: 'ALL' }]
    case 'PRIMARY_ORG': return [{ relation: 'owner_org', subject: 'RELATION', subjectParam: 'member', subtree }]
    case 'RELATION': return [{ relation: 'owner_org', subject: 'RELATION', subjectParam: mp.anchorParam || 'member', subtree }]
    case 'CUSTOM_ORG': return [{ relation: 'owner_org', subject: 'CUSTOM', subtree, orgIds: mp.customOrgIds }]
    case 'PLUGIN_DIM': return [{ relation: 'owner_org', subject: 'PLUGIN_DIM', subjectParam: mp.anchorParam }]
    case 'SELF':
    default: return [{ relation: 'creator', subject: 'SELF' }]
  }
}

/** 该选项是否"和我有X关系的组织" (org 关系) → 显示"含下级"。 */
export function keyIsOrg(key: string): boolean {
  return key.startsWith('org:')
}

/** 一个 spec 的 relationGrants 是否为空 (用于"未配置=创建者"兜底)。 */
export function ensureGrants(grants: RelationGrant[] | undefined): RelationGrant[] {
  return grants && grants.length ? grants : [{ relation: 'creator', subject: 'SELF' }]
}
