import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { hasFeature, isFeatureDisabled, useFeature, useAnyFeature, useAllFeatures } from '@/composables/useFeature'
import { usePluginsStore } from '@/stores/plugins'

describe('useFeature composable', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('hasFeature - 基础', () => {
    it('CORE 永远启用', () => {
      expect(hasFeature('CORE')).toBe(true)
      expect(hasFeature('core')).toBe(true)
      expect(hasFeature('Core')).toBe(true)
    })

    it('null/empty 输入返回 false', () => {
      expect(hasFeature(null)).toBe(false)
      expect(hasFeature(undefined)).toBe(false)
      expect(hasFeature('')).toBe(false)
    })

    it('未启用插件返回 false', () => {
      expect(hasFeature('EDU')).toBe(false)
      expect(hasFeature('HEALTH')).toBe(false)
    })

    it('启用 EDU 后 hasFeature 返回 true', () => {
      const store = usePluginsStore()
      store.markLoaded('EDU')
      expect(hasFeature('EDU')).toBe(true)
      expect(hasFeature('edu')).toBe(true)  // 大小写无关
    })

    it('卸载插件后 hasFeature 返回 false', () => {
      const store = usePluginsStore()
      store.markLoaded('EDU')
      expect(hasFeature('EDU')).toBe(true)
      store.markUnloaded('EDU')
      expect(hasFeature('EDU')).toBe(false)
    })
  })

  describe('hasFeature - 子特征', () => {
    it('EDU 启用时 education.dormitory 自动开', () => {
      const store = usePluginsStore()
      store.markLoaded('EDU')
      expect(hasFeature('education.dormitory')).toBe(true)
      expect(hasFeature('education.student')).toBe(true)
      expect(hasFeature('education.academic')).toBe(true)
    })

    it('EDU 禁用时所有 education.* 子特征关闭', () => {
      expect(hasFeature('education.dormitory')).toBe(false)
      expect(hasFeature('education.student')).toBe(false)
    })

    it('HEALTH 启用时 healthcare.patient 开', () => {
      const store = usePluginsStore()
      store.markLoaded('HEALTH')
      expect(hasFeature('healthcare.patient')).toBe(true)
      expect(hasFeature('healthcare.ward')).toBe(true)
    })

    it('未声明的子特征返回 false', () => {
      const store = usePluginsStore()
      store.markLoaded('EDU')
      expect(hasFeature('education.unknown')).toBe(false)
    })
  })

  describe('isFeatureDisabled', () => {
    it('启用时返回 false', () => {
      const store = usePluginsStore()
      store.markLoaded('EDU')
      expect(isFeatureDisabled('EDU')).toBe(false)
    })

    it('未启用时返回 true', () => {
      expect(isFeatureDisabled('HEALTH')).toBe(true)
    })
  })

  describe('useFeature 响应式', () => {
    it('返回 enabled/disabled computed', () => {
      const { enabled, disabled } = useFeature('EDU')
      expect(enabled.value).toBe(false)
      expect(disabled.value).toBe(true)

      const store = usePluginsStore()
      store.markLoaded('EDU')
      expect(enabled.value).toBe(true)
      expect(disabled.value).toBe(false)
    })
  })

  describe('useAnyFeature', () => {
    it('任一启用即 true', () => {
      const { enabled } = useAnyFeature(['EDU', 'HEALTH'])
      expect(enabled.value).toBe(false)

      const store = usePluginsStore()
      store.markLoaded('HEALTH')
      expect(enabled.value).toBe(true)
    })

    it('全未启用时 false', () => {
      const { enabled } = useAnyFeature(['EDU', 'HEALTH', 'CARE'])
      expect(enabled.value).toBe(false)
    })

    it('CORE 在列表中即 true (CORE 永远启用)', () => {
      const { enabled } = useAnyFeature(['CORE', 'HEALTH'])
      expect(enabled.value).toBe(true)
    })
  })

  describe('useAllFeatures', () => {
    it('全部启用才 true', () => {
      const { enabled } = useAllFeatures(['EDU', 'HEALTH'])
      const store = usePluginsStore()
      store.markLoaded('EDU')
      expect(enabled.value).toBe(false)  // HEALTH 未启用
      store.markLoaded('HEALTH')
      expect(enabled.value).toBe(true)
    })

    it('空列表返回 true (every of empty)', () => {
      const { enabled } = useAllFeatures([])
      expect(enabled.value).toBe(true)
    })
  })
})
