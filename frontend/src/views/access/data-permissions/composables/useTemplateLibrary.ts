/**
 * 内置角色模板库 — 6 个覆盖 80% 场景的预置模板
 * 用户点击 "应用" > sceneToModuleScopes 展开为 28 模块 scope > PUT 保存
 */
import { reactive } from 'vue'
import type { SceneDecision } from './useSceneTemplate'

export interface RoleTemplate {
  id: string
  name: string
  /** 展示图标 (lucide icon 名) */
  icon: string
  /** 所属行业 (仅展示过滤用) */
  industry: 'CORE' | 'EDU' | 'HEALTH' | 'CARE' | 'CUSTOM'
  /** 一句话描述 */
  description: string
  /** 典型使用场景 */
  scenario?: string
  scene: SceneDecision
}

export const BUILTIN_TEMPLATES: RoleTemplate[] = [
  {
    id: 'super-admin',
    name: '超级管理员',
    icon: 'Crown',
    industry: 'CORE',
    description: '全部数据可见, 所有操作权限',
    scenario: '系统管理员、CTO、创始人',
    scene: { primary: 'ALL', bizAutoFollow: true },
  },
  {
    id: 'dept-manager',
    name: '部门经理',
    icon: 'Users',
    industry: 'CORE',
    description: '本部门及以下数据, 组织管理权限',
    scenario: '行政主管、部门负责人',
    scene: { primary: 'DEPARTMENT_AND_BELOW', bizAutoFollow: true },
  },
  {
    id: 'personal-only',
    name: '访客 / 个人',
    icon: 'User',
    industry: 'CORE',
    description: '仅看自己创建的数据',
    scenario: '外部访客、审计人员、临时账户',
    scene: { primary: 'SELF', bizAutoFollow: true },
  },
]

/** 行业插件贡献的模板 (按插件码登记, 仅该插件启用时出现) — 与 relationScenes/scopeSpecializations 同模式 */
const pluginTemplates = reactive<Record<string, RoleTemplate[]>>({})

export function registerRoleTemplates(pluginCode: string, templates: RoleTemplate[]): void {
  pluginTemplates[pluginCode] = templates
}

/** 核心模板 + 已启用插件贡献的模板 */
export function allTemplates(enabledCodes: readonly string[]): RoleTemplate[] {
  return [...BUILTIN_TEMPLATES, ...enabledCodes.flatMap(c => pluginTemplates[c] ?? [])]
}

export function findTemplate(id: string, enabledCodes: readonly string[] = []): RoleTemplate | undefined {
  return allTemplates(enabledCodes).find(t => t.id === id)
}
