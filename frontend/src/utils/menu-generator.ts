/**
 * 菜单生成工具
 * 从路由配置自动生成菜单数据,避免路由和菜单配置分离
 */
import type { RouteRecordRaw } from 'vue-router'
import type { MenuItem } from '@/types/common'

/**
 * 从路由配置生成菜单项
 * @param routes 路由配置数组
 * @param authCheck 权限检查函数
 * @returns 菜单项数组
 */
export function generateMenuFromRoutes(
  routes: RouteRecordRaw[],
  authCheck: (permission?: string) => boolean
): MenuItem[] {
  const menuItems: MenuItem[] = []

  for (const route of routes) {
    // 跳过隐藏的路由
    if (route.meta?.hidden) {
      continue
    }

    // 检查权限
    if (route.meta?.permission && !authCheck(route.meta.permission as string)) {
      continue
    }

    // 构建菜单项
    const menuItem: MenuItem = {
      id: route.name as string || route.path,
      name: route.name as string,
      path: route.path,
      title: route.meta?.title as string || route.path,
      icon: route.meta?.icon as string,
      order: (route.meta as any)?.order,
      permission: route.meta?.permission as string
    }

    // 递归处理子路由
    if (route.children && route.children.length > 0) {
      const children = generateMenuFromRoutes(route.children, authCheck)
      if (children.length > 0) {
        menuItem.children = children
      }
    }

    // 只添加有效的菜单项
    if (menuItem.title) {
      menuItems.push(menuItem)
    }
  }

  return menuItems
}

/**
 * 过滤菜单项(根据权限)
 * @param menus 菜单数组
 * @param authCheck 权限检查函数
 * @returns 过滤后的菜单数组
 */
export function filterMenuByPermission(
  menus: Array<MenuItem & { permission?: string }>,
  authCheck: (permission?: string) => boolean
): MenuItem[] {
  return menus
    .filter(menu => {
      // 如果没有设置权限,则显示
      if (!menu.permission) return true
      // 检查用户是否有该权限
      return authCheck(menu.permission)
    })
    .map(menu => {
      // 如果有子菜单,递归过滤
      if (menu.children && menu.children.length > 0) {
        const filteredChildren = filterMenuByPermission(
          menu.children as Array<MenuItem & { permission?: string }>,
          authCheck
        )
        // 只有当有可见的子菜单时才保留父菜单
        if (filteredChildren.length > 0) {
          return {
            ...menu,
            children: filteredChildren
          }
        }
        return null
      }
      return menu
    })
    .filter(Boolean) as MenuItem[]
}

/**
 * 从路由元信息生成面包屑
 * @param route 当前路由
 * @param routes 所有路由配置
 * @returns 面包屑数组
 */
export function generateBreadcrumb(
  currentPath: string,
  routes: RouteRecordRaw[]
): Array<{ title: string; path: string }> {
  const breadcrumbs: Array<{ title: string; path: string }> = []
  const pathSegments = currentPath.split('/').filter(Boolean)

  let currentRoutes = routes
  let accumulatedPath = ''

  for (const segment of pathSegments) {
    accumulatedPath += `/${segment}`

    // 在当前层级查找匹配的路由
    const matchedRoute = currentRoutes.find(route => {
      const routePath = route.path.startsWith('/') ? route.path : `/${route.path}`
      return routePath === accumulatedPath || route.path === segment
    })

    if (matchedRoute && matchedRoute.meta?.title) {
      breadcrumbs.push({
        title: matchedRoute.meta.title as string,
        path: accumulatedPath
      })

      // 如果有子路由,更新当前查找范围
      if (matchedRoute.children) {
        currentRoutes = matchedRoute.children
      }
    }
  }

  return breadcrumbs
}

/**
 * 排序菜单项
 * @param menus 菜单数组
 * @returns 排序后的菜单数组
 */
export function sortMenuItems(menus: MenuItem[]): MenuItem[] {
  return menus
    .sort((a, b) => {
      const orderA = (a as any).order || 999
      const orderB = (b as any).order || 999
      return orderA - orderB
    })
    .map(menu => {
      if (menu.children && menu.children.length > 0) {
        return {
          ...menu,
          children: sortMenuItems(menu.children)
        }
      }
      return menu
    })
}
