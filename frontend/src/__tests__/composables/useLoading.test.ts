import { describe, it, expect } from 'vitest'
import { useLoading, useSimpleLoading } from '@/composables/useLoading'

/**
 * useLoading Composable 测试
 */
describe('useLoading', () => {
  describe('初始状态', () => {
    it('应该初始化为空的加载状态', () => {
      const { loadingMap, isAnyLoading } = useLoading()

      expect(loadingMap.value).toEqual({})
      expect(isAnyLoading.value).toBe(false)
    })
  })

  describe('startLoading / stopLoading', () => {
    it('应该使用默认 key 开始和停止加载', () => {
      const { startLoading, stopLoading, isLoading } = useLoading()

      startLoading()
      expect(isLoading().value).toBe(true)

      stopLoading()
      expect(isLoading().value).toBe(false)
    })

    it('应该支持自定义 key', () => {
      const { startLoading, stopLoading, isLoading } = useLoading()

      startLoading('fetch')
      expect(isLoading('fetch').value).toBe(true)
      expect(isLoading('default').value).toBe(false)

      stopLoading('fetch')
      expect(isLoading('fetch').value).toBe(false)
    })

    it('应该支持多个同时加载', () => {
      const { startLoading, stopLoading, isLoading, isAnyLoading } = useLoading()

      startLoading('fetch1')
      startLoading('fetch2')

      expect(isLoading('fetch1').value).toBe(true)
      expect(isLoading('fetch2').value).toBe(true)
      expect(isAnyLoading.value).toBe(true)

      stopLoading('fetch1')
      expect(isLoading('fetch1').value).toBe(false)
      expect(isLoading('fetch2').value).toBe(true)
      expect(isAnyLoading.value).toBe(true)

      stopLoading('fetch2')
      expect(isAnyLoading.value).toBe(false)
    })
  })

  describe('isAnyLoading', () => {
    it('应该在有任何加载时返回 true', () => {
      const { startLoading, stopLoading, isAnyLoading } = useLoading()

      expect(isAnyLoading.value).toBe(false)

      startLoading('task1')
      expect(isAnyLoading.value).toBe(true)

      startLoading('task2')
      expect(isAnyLoading.value).toBe(true)

      stopLoading('task1')
      expect(isAnyLoading.value).toBe(true)

      stopLoading('task2')
      expect(isAnyLoading.value).toBe(false)
    })
  })

  describe('withLoading', () => {
    it('应该在异步函数执行期间设置加载状态', async () => {
      const { withLoading, isLoading } = useLoading()

      let resolvePromise: () => void
      const asyncFn = () =>
        new Promise<string>(resolve => {
          resolvePromise = () => resolve('result')
        })

      const promise = withLoading(asyncFn)
      expect(isLoading().value).toBe(true)

      resolvePromise!()
      const result = await promise

      expect(result).toBe('result')
      expect(isLoading().value).toBe(false)
    })

    it('应该在异步函数抛出错误后停止加载', async () => {
      const { withLoading, isLoading } = useLoading()

      const asyncFn = () => Promise.reject(new Error('test error'))

      await expect(withLoading(asyncFn)).rejects.toThrow('test error')
      expect(isLoading().value).toBe(false)
    })

    it('应该支持自定义 key', async () => {
      const { withLoading, isLoading } = useLoading()

      await withLoading(async () => {
        expect(isLoading('custom').value).toBe(true)
        return 'ok'
      }, 'custom')

      expect(isLoading('custom').value).toBe(false)
    })
  })

  describe('resetLoading', () => {
    it('应该重置所有加载状态', () => {
      const { startLoading, resetLoading, loadingMap, isAnyLoading } = useLoading()

      startLoading('task1')
      startLoading('task2')
      startLoading('task3')

      expect(isAnyLoading.value).toBe(true)

      resetLoading()

      expect(loadingMap.value).toEqual({})
      expect(isAnyLoading.value).toBe(false)
    })
  })

  describe('isLoading 返回值', () => {
    it('对不存在的 key 应该返回 false', () => {
      const { isLoading } = useLoading()

      expect(isLoading('nonexistent').value).toBe(false)
    })
  })
})

/**
 * useSimpleLoading Composable 测试
 */
describe('useSimpleLoading', () => {
  describe('初始状态', () => {
    it('应该默认初始化为 false', () => {
      const { loading } = useSimpleLoading()

      expect(loading.value).toBe(false)
    })

    it('应该支持自定义初始值', () => {
      const { loading } = useSimpleLoading(true)

      expect(loading.value).toBe(true)
    })
  })

  describe('startLoading / stopLoading', () => {
    it('应该正确切换加载状态', () => {
      const { loading, startLoading, stopLoading } = useSimpleLoading()

      startLoading()
      expect(loading.value).toBe(true)

      stopLoading()
      expect(loading.value).toBe(false)
    })
  })

  describe('withLoading', () => {
    it('应该在异步函数执行期间设置加载状态', async () => {
      const { loading, withLoading } = useSimpleLoading()

      const result = await withLoading(async () => {
        expect(loading.value).toBe(true)
        return 'success'
      })

      expect(result).toBe('success')
      expect(loading.value).toBe(false)
    })

    it('应该在异步函数抛出错误后停止加载', async () => {
      const { loading, withLoading } = useSimpleLoading()

      await expect(
        withLoading(async () => {
          throw new Error('async error')
        })
      ).rejects.toThrow('async error')

      expect(loading.value).toBe(false)
    })

    it('应该返回异步函数的结果', async () => {
      const { withLoading } = useSimpleLoading()

      const result = await withLoading(async () => {
        return { id: 1, name: 'test' }
      })

      expect(result).toEqual({ id: 1, name: 'test' })
    })
  })
})
