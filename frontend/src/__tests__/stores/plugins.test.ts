/**
 * F2: usePluginsStore 单测.
 *
 * 验证响应式 loadedCodes Set + loadVersion 自增, codes computed.
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePluginsStore } from '@/stores/plugins'

describe('usePluginsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('初始 loadedCodes 为空 Set', () => {
    const store = usePluginsStore()
    expect(store.loadedCodes.size).toBe(0)
    expect(store.loadVersion).toBe(0)
  })

  it('markLoaded 添加新码并自增 loadVersion', () => {
    const store = usePluginsStore()
    const ok = store.markLoaded('EDU')
    expect(ok).toBe(true)
    expect(store.loadVersion).toBe(1)
    expect(store.isLoaded('EDU')).toBe(true)
  })

  it('markLoaded 重复加同一码返回 false 不增 version', () => {
    const store = usePluginsStore()
    store.markLoaded('EDU')
    const v1 = store.loadVersion
    const ok = store.markLoaded('EDU')
    expect(ok).toBe(false)
    expect(store.loadVersion).toBe(v1)
  })

  it('markUnloaded 删除已加载的码', () => {
    const store = usePluginsStore()
    store.markLoaded('HEALTH')
    expect(store.isLoaded('HEALTH')).toBe(true)
    const ok = store.markUnloaded('HEALTH')
    expect(ok).toBe(true)
    expect(store.isLoaded('HEALTH')).toBe(false)
  })

  it('markUnloaded 不存在的码返回 false', () => {
    const store = usePluginsStore()
    expect(store.markUnloaded('XX')).toBe(false)
  })

  it('codes computed 反映 loadedCodes', () => {
    const store = usePluginsStore()
    store.markLoaded('A')
    store.markLoaded('B')
    expect(store.codes.sort()).toEqual(['A', 'B'])
  })

  it('isLoaded 区分大小写 — 直接 Set 查找', () => {
    const store = usePluginsStore()
    store.markLoaded('EDU')
    expect(store.isLoaded('EDU')).toBe(true)
    expect(store.isLoaded('edu')).toBe(false)
  })

  it('多次 markLoaded/Unloaded 后 loadVersion 累加正确', () => {
    const store = usePluginsStore()
    store.markLoaded('A') // 1
    store.markLoaded('B') // 2
    store.markUnloaded('A') // 3
    store.markUnloaded('A') // no-op
    expect(store.loadVersion).toBe(3)
  })

  it('全部卸载后 codes 为空数组', () => {
    const store = usePluginsStore()
    store.markLoaded('A')
    store.markLoaded('B')
    store.markUnloaded('A')
    store.markUnloaded('B')
    expect(store.codes).toEqual([])
  })
})
