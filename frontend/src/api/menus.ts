/**
 * 菜单 API — 对接后端 MenuContributionPlugin 体系.
 *
 * 后端 /api/menus/my 返回当前用户按权限过滤后的菜单树,
 * 前端据此渲染侧边栏,替代 router/index.ts 中的硬编码菜单顺序.
 */
import { http } from '@/utils/request'

export interface BackendMenuItem {
  path: string
  title: string
  icon?: string
  order?: number
  component?: string
  requiredPermissions?: string[]
  requiredFeature?: string
  industry?: string
  children?: BackendMenuItem[]
}

export const menusApi = {
  my: () => http.get<BackendMenuItem[]>('/menus/my'),
  all: () => http.get<BackendMenuItem[]>('/menus/all')
}
