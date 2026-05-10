/**
 * F6: 路由 meta.permissions 统一守护单测
 *
 * 测试 checkRoutePermissions 纯函数 — 它从 router/index.ts 导出,
 * 在 router.beforeEach 中被调用. 这里不实例化 router (因 modules 大量
 * dynamic import + Element Plus 依赖, happy-dom 环境会拖慢 / 异常),
 * 而是直接验证守门逻辑.
 */
import { describe, it, expect, vi } from 'vitest'
import { checkRoutePermissions } from '@/router/index'

describe('checkRoutePermissions (F6 路由权限守护)', () => {
  // ---- 场景 1: 有权 → pass ----
  it('用户拥有所有 required 权限 → pass', () => {
    const hasPerm = vi.fn((p: string) =>
      ['workflow:definition:view', 'workflow:definition:edit'].includes(p)
    )
    const result = checkRoutePermissions(
      { permissions: ['workflow:definition:view', 'workflow:definition:edit'] },
      hasPerm
    )
    expect(result).toBe('pass')
    expect(hasPerm).toHaveBeenCalledTimes(2)
  })

  it('单 permission 数组用户拥有 → pass', () => {
    const hasPerm = vi.fn((p: string) => p === 'event:timeline:view')
    expect(
      checkRoutePermissions({ permissions: ['event:timeline:view'] }, hasPerm)
    ).toBe('pass')
  })

  it('通配 * 权限 (super admin) → pass', () => {
    // hasPermission 已在 auth store 内部把 '*' 处理为 always-true,
    // 这里 mock 行为直接复刻
    const hasPerm = vi.fn(() => true)
    expect(
      checkRoutePermissions(
        { permissions: ['system:tenant:view', 'system:config:view'] },
        hasPerm
      )
    ).toBe('pass')
  })

  // ---- 场景 2: 无权 → redirect ----
  it('缺一个权限 → redirect (AND 语义)', () => {
    const hasPerm = vi.fn((p: string) => p === 'system:tenant:view')
    // 缺 system:config:view
    const result = checkRoutePermissions(
      { permissions: ['system:tenant:view', 'system:config:view'] },
      hasPerm
    )
    expect(result).toBe('redirect')
  })

  it('完全无权 → redirect', () => {
    const hasPerm = vi.fn(() => false)
    expect(
      checkRoutePermissions({ permissions: ['workflow:definition:view'] }, hasPerm)
    ).toBe('redirect')
  })

  // ---- 场景 3: 无 meta.permissions → 直接通过 ----
  it('meta 没有 permissions 字段 → pass (不调用 hasPermission)', () => {
    const hasPerm = vi.fn(() => false)
    expect(checkRoutePermissions({}, hasPerm)).toBe('pass')
    expect(hasPerm).not.toHaveBeenCalled()
  })

  it('meta.permissions = [] (空数组) → pass', () => {
    const hasPerm = vi.fn(() => false)
    expect(checkRoutePermissions({ permissions: [] }, hasPerm)).toBe('pass')
    expect(hasPerm).not.toHaveBeenCalled()
  })

  it('meta = undefined → pass', () => {
    const hasPerm = vi.fn(() => false)
    expect(checkRoutePermissions(undefined, hasPerm)).toBe('pass')
    expect(hasPerm).not.toHaveBeenCalled()
  })

  // ---- 场景 4: 短路求值 ----
  it('AND 语义短路: 第一个失败即停止 (every 语义)', () => {
    const hasPerm = vi.fn(() => false)
    checkRoutePermissions(
      { permissions: ['a', 'b', 'c', 'd'] },
      hasPerm
    )
    // every 在第一个 false 即停, 只调用 1 次
    expect(hasPerm).toHaveBeenCalledTimes(1)
    expect(hasPerm).toHaveBeenCalledWith('a')
  })
})
