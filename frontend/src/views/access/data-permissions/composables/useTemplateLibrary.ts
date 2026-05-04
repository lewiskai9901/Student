/**
 * 内置角色模板库 — 6 个覆盖 80% 场景的预置模板
 * 用户点击 "应用" > sceneToModuleScopes 展开为 28 模块 scope > PUT 保存
 */
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
    id: 'class-teacher',
    name: '班主任',
    icon: 'BookOpen',
    industry: 'EDU',
    description: '我带的班级学生 + 本部门数据',
    scenario: '中小学班主任、辅导员',
    scene: { primary: 'DEPARTMENT', studentScope: 'BY_CLASS', bizAutoFollow: true },
  },
  {
    id: 'grade-director',
    name: '年级主任',
    icon: 'GraduationCap',
    industry: 'EDU',
    description: '我管的年级全部数据 + 部门及以下',
    scenario: '年级组长、高中年级主任',
    scene: { primary: 'DEPARTMENT_AND_BELOW', studentScope: 'BY_GRADE', bizAutoFollow: true },
  },
  {
    id: 'doctor',
    name: '医师',
    icon: 'Stethoscope',
    industry: 'HEALTH',
    description: '本部门数据 (病区维度暂未支持)',
    scenario: '临床医师、住院医师',
    scene: { primary: 'DEPARTMENT', bizAutoFollow: true },
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

export function findTemplate(id: string): RoleTemplate | undefined {
  return BUILTIN_TEMPLATES.find(t => t.id === id)
}

export function templatesByIndustry(industry?: string): RoleTemplate[] {
  if (!industry) return BUILTIN_TEMPLATES
  return BUILTIN_TEMPLATES.filter(t => t.industry === industry)
}
