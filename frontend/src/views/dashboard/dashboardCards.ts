import { reactive } from 'vue'
import type { Component } from 'vue'

/**
 * 总览看板"行业卡片"注册表。
 *
 * <p>通用核心 DashboardView 只渲染通用卡（组织/检查/系统）。行业看板卡由对应
 * 行业插件前端入口（如 router/plugins/edu.ts）在加载时通过
 * {@link registerDashboardCards} 注册，仅当该插件启用时出现 —— 与
 * relationScenes / dataScopeSpecializations 同一套门控理念。
 *
 * <p>数据流: DashboardView 单次拉取 /dashboard/overview，按 {@link DashboardCardDef#sectionKey}
 * 把对应分区数据以 prop `data` 传给卡片组件。分区数据由后端对应插件的
 * DashboardSectionContributor 产出（插件禁用时分区与卡片同时消失）。
 */
export interface DashboardCardDef {
  /** 唯一键（跨插件不可重复） */
  code: string
  /** /dashboard/overview 数据分区键，组件经 prop `data` 收到 overview[sectionKey] */
  sectionKey: string
  /** 布局: full = 整行（统计条样式）; half = 半宽（进双列网格，与核心卡并排） */
  span: 'full' | 'half'
  /** 渲染顺序（同 span 内生效） */
  order: number
  /** 懒加载组件 — () => import('...') */
  component: () => Promise<Component | { default: Component }>
}

/** 快捷入口贡献（与卡片同门控；perm 与路由存在性过滤由 DashboardView 统一做） */
export interface DashboardShortcutDef {
  label: string
  path: string
  perm?: string
}

const pluginCards = reactive<Record<string, DashboardCardDef[]>>({})
const pluginShortcuts = reactive<Record<string, DashboardShortcutDef[]>>({})

/** 行业插件前端入口加载时调用，登记本插件的看板卡。 */
export function registerDashboardCards(pluginCode: string, cards: DashboardCardDef[]): void {
  pluginCards[pluginCode] = cards
}

/** 行业插件前端入口加载时调用，登记本插件的快捷入口。 */
export function registerDashboardShortcuts(pluginCode: string, items: DashboardShortcutDef[]): void {
  pluginShortcuts[pluginCode] = items
}

/** 已启用插件的看板卡（按 order 升序）。 */
export function enabledDashboardCards(enabledCodes: string[]): DashboardCardDef[] {
  return enabledCodes
    .flatMap((code) => pluginCards[code] ?? [])
    .sort((a, b) => a.order - b.order)
}

/** 已启用插件的快捷入口。 */
export function enabledDashboardShortcuts(enabledCodes: string[]): DashboardShortcutDef[] {
  return enabledCodes.flatMap((code) => pluginShortcuts[code] ?? [])
}
