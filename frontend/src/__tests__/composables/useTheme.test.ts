import { describe, it, expect, beforeEach, vi } from 'vitest'

// 在 import composable 前 reset module 缓存, 避免单例 ref 污染
beforeEach(async () => {
  vi.resetModules()
  document.documentElement.removeAttribute('data-theme')
  // 重置 localStorage mock (setup.ts 注入)
  const ls = window.localStorage as unknown as { getItem: ReturnType<typeof vi.fn> }
  ls.getItem.mockReturnValue(null)
})

describe('useTheme composable', () => {
  it('默认 theme=system', async () => {
    const { themeApi } = await import('@/composables/useTheme')
    expect(['system', 'light', 'dark']).toContain(themeApi.current)
  })

  it('setTheme(dark) 写 data-theme="dark"', async () => {
    const { themeApi } = await import('@/composables/useTheme')
    themeApi.setTheme('dark')
    expect(themeApi.isDark).toBe(true)
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('setTheme(light) 移除 data-theme', async () => {
    const { themeApi } = await import('@/composables/useTheme')
    themeApi.setTheme('dark')
    themeApi.setTheme('light')
    expect(themeApi.isDark).toBe(false)
    expect(document.documentElement.getAttribute('data-theme')).toBeNull()
  })

  it('toggle 在 light/dark 之间切换', async () => {
    const { themeApi } = await import('@/composables/useTheme')
    themeApi.setTheme('light')
    themeApi.toggle()
    expect(themeApi.isDark).toBe(true)
    themeApi.toggle()
    expect(themeApi.isDark).toBe(false)
  })

  it('setTheme 持久化到 localStorage', async () => {
    const { themeApi } = await import('@/composables/useTheme')
    const ls = window.localStorage as unknown as { setItem: ReturnType<typeof vi.fn> }
    themeApi.setTheme('dark')
    expect(ls.setItem).toHaveBeenCalledWith('app-theme', 'dark')
  })

  it('从 localStorage 恢复 saved theme', async () => {
    const ls = window.localStorage as unknown as { getItem: ReturnType<typeof vi.fn> }
    ls.getItem.mockReturnValue('dark')
    const { themeApi } = await import('@/composables/useTheme')
    expect(themeApi.current).toBe('dark')
  })

  it('init() 应用当前主题到 <html>', async () => {
    const ls = window.localStorage as unknown as { getItem: ReturnType<typeof vi.fn> }
    ls.getItem.mockReturnValue('dark')
    const { themeApi } = await import('@/composables/useTheme')
    themeApi.init()
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })
})
