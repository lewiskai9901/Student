/**
 * F2: menu-generator 工具单测.
 *
 * 覆盖 generateMenuFromRoutes / filterMenuByPermission /
 * generateBreadcrumb / sortMenuItems.
 */
import { describe, it, expect } from 'vitest'
import type { RouteRecordRaw } from 'vue-router'
import {
  generateMenuFromRoutes,
  filterMenuByPermission,
  generateBreadcrumb,
  sortMenuItems,
} from '@/utils/menu-generator'

describe('generateMenuFromRoutes', () => {
  it('空数组返回 []', () => {
    expect(generateMenuFromRoutes([], () => true)).toEqual([])
  })

  it('hidden 路由被跳过', () => {
    const routes: RouteRecordRaw[] = [
      { path: '/h', name: 'H', meta: { hidden: true, title: 'H' }, component: {} as any },
      { path: '/a', name: 'A', meta: { title: 'A' }, component: {} as any },
    ]
    const menus = generateMenuFromRoutes(routes, () => true)
    expect(menus).toHaveLength(1)
    expect(menus[0].name).toBe('A')
  })

  it('无权限的路由被过滤', () => {
    const routes: RouteRecordRaw[] = [
      { path: '/a', name: 'A', meta: { title: 'A', permission: 'p:a' }, component: {} as any },
      { path: '/b', name: 'B', meta: { title: 'B', permission: 'p:b' }, component: {} as any },
    ]
    const menus = generateMenuFromRoutes(routes, (p) => p === 'p:a')
    expect(menus).toHaveLength(1)
    expect(menus[0].path).toBe('/a')
  })

  it('requiresClass 但 userInfo 无 class 时跳过', () => {
    const routes: RouteRecordRaw[] = [
      { path: '/c', name: 'C', meta: { title: 'C', requiresClass: true }, component: {} as any },
    ]
    const m1 = generateMenuFromRoutes(routes, { authCheck: () => true, userInfo: null })
    expect(m1).toEqual([])

    const m2 = generateMenuFromRoutes(routes, {
      authCheck: () => true,
      userInfo: { assignedClasses: [{ id: 1 }] } as any,
    })
    expect(m2).toHaveLength(1)
  })

  it('递归处理子路由', () => {
    const routes: RouteRecordRaw[] = [
      {
        path: '/parent',
        name: 'P',
        meta: { title: 'Parent' },
        component: {} as any,
        children: [
          { path: 'child1', name: 'C1', meta: { title: 'Child1' }, component: {} as any },
          { path: 'child2', name: 'C2', meta: { title: 'Child2', hidden: true }, component: {} as any },
        ],
      },
    ]
    const menus = generateMenuFromRoutes(routes, () => true)
    expect(menus).toHaveLength(1)
    expect(menus[0].children).toHaveLength(1)
    expect(menus[0].children![0].name).toBe('C1')
  })

  it('容器菜单子全部过滤后被删 (无 permission 的纯容器)', () => {
    const routes: RouteRecordRaw[] = [
      {
        path: '/x',
        name: 'X',
        meta: { title: 'X' },
        component: {} as any,
        children: [
          { path: 'c', name: 'XC', meta: { title: 'XC', permission: 'no' }, component: {} as any },
        ],
      },
    ]
    const menus = generateMenuFromRoutes(routes, () => false)
    expect(menus).toEqual([])
  })

  it('支持旧函数签名 (function)', () => {
    const routes: RouteRecordRaw[] = [
      { path: '/a', name: 'A', meta: { title: 'A' }, component: {} as any },
    ]
    const menus = generateMenuFromRoutes(routes, () => true)
    expect(menus).toHaveLength(1)
  })

  it('从 meta 取 title / icon / order / permission', () => {
    const routes: RouteRecordRaw[] = [
      {
        path: '/t',
        name: 'T',
        meta: { title: 'T', icon: 'home', order: 5, permission: 'p' },
        component: {} as any,
      },
    ]
    const menus = generateMenuFromRoutes(routes, () => true)
    expect(menus[0]).toMatchObject({
      title: 'T',
      icon: 'home',
      order: 5,
      permission: 'p',
    })
  })
})

describe('filterMenuByPermission', () => {
  it('无 permission 的菜单保留', () => {
    const menus = [{ id: 'a', name: 'a', path: '/a', title: 'A' }] as any
    const result = filterMenuByPermission(menus, () => false)
    expect(result).toHaveLength(1)
  })

  it('有 permission 的菜单按 authCheck 过滤', () => {
    const menus = [
      { id: 'a', name: 'a', path: '/a', title: 'A', permission: 'p:a' },
      { id: 'b', name: 'b', path: '/b', title: 'B', permission: 'p:b' },
    ] as any
    const result = filterMenuByPermission(menus, (p) => p === 'p:a')
    expect(result).toHaveLength(1)
    expect(result[0].id).toBe('a')
  })

  it('父菜单子全过滤后被删', () => {
    const menus = [
      {
        id: 'parent',
        name: 'p',
        path: '/p',
        title: 'P',
        children: [
          { id: 'c1', name: 'c1', path: '/p/c1', title: 'C1', permission: 'denied' },
        ],
      },
    ] as any
    const result = filterMenuByPermission(menus, () => false)
    expect(result).toEqual([])
  })

  it('父菜单子部分保留时, 父也保留', () => {
    const menus = [
      {
        id: 'p',
        name: 'p',
        path: '/p',
        title: 'P',
        children: [
          { id: 'c1', name: 'c1', path: '/p/c1', title: 'C1', permission: 'p:1' },
          { id: 'c2', name: 'c2', path: '/p/c2', title: 'C2', permission: 'p:2' },
        ],
      },
    ] as any
    const result = filterMenuByPermission(menus, (p) => p === 'p:1')
    expect(result).toHaveLength(1)
    expect((result[0] as any).children).toHaveLength(1)
  })
})

describe('generateBreadcrumb', () => {
  it('空路径返回 []', () => {
    expect(generateBreadcrumb('/', [])).toEqual([])
  })

  it('单层路径', () => {
    const routes: RouteRecordRaw[] = [
      { path: '/dashboard', name: 'D', meta: { title: '首页' }, component: {} as any },
    ]
    const crumbs = generateBreadcrumb('/dashboard', routes)
    expect(crumbs).toEqual([{ title: '首页', path: '/dashboard' }])
  })

  it('两层路径', () => {
    const routes: RouteRecordRaw[] = [
      {
        path: '/org',
        name: 'O',
        meta: { title: '组织' },
        component: {} as any,
        children: [
          { path: 'units', name: 'U', meta: { title: '单元' }, component: {} as any },
        ],
      },
    ]
    const crumbs = generateBreadcrumb('/org/units', routes)
    expect(crumbs).toHaveLength(2)
    expect(crumbs[1].title).toBe('单元')
  })

  it('未匹配的 segment 不入 breadcrumb', () => {
    const routes: RouteRecordRaw[] = [
      { path: '/x', name: 'X', meta: { title: 'X' }, component: {} as any },
    ]
    const crumbs = generateBreadcrumb('/y/z', routes)
    expect(crumbs).toEqual([])
  })
})

describe('sortMenuItems', () => {
  it('order 升序排序', () => {
    const menus = [
      { id: 'a', name: 'a', path: '/a', title: 'A', order: 3 },
      { id: 'b', name: 'b', path: '/b', title: 'B', order: 1 },
      { id: 'c', name: 'c', path: '/c', title: 'C', order: 2 },
    ] as any
    const sorted = sortMenuItems(menus)
    expect(sorted.map((m: any) => m.id)).toEqual(['b', 'c', 'a'])
  })

  it('未指定 order 按 999 排到最后', () => {
    const menus = [
      { id: 'a', name: 'a', path: '/a', title: 'A' },
      { id: 'b', name: 'b', path: '/b', title: 'B', order: 1 },
    ] as any
    const sorted = sortMenuItems(menus)
    expect(sorted[0].id).toBe('b')
  })

  it('order=0 仍是最前 (?? 而非 ||)', () => {
    const menus = [
      { id: 'a', name: 'a', path: '/a', title: 'A', order: 5 },
      { id: 'z', name: 'z', path: '/z', title: 'Z', order: 0 },
    ] as any
    const sorted = sortMenuItems(menus)
    expect(sorted[0].id).toBe('z')
  })

  it('递归排序子菜单', () => {
    const menus = [
      {
        id: 'p',
        name: 'p',
        path: '/p',
        title: 'P',
        order: 1,
        children: [
          { id: 'c1', name: 'c1', path: '/p/c1', title: 'C1', order: 2 },
          { id: 'c2', name: 'c2', path: '/p/c2', title: 'C2', order: 1 },
        ],
      },
    ] as any
    const sorted = sortMenuItems(menus)
    expect((sorted[0] as any).children[0].id).toBe('c2')
  })
})
