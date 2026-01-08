import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

/**
 * 工具函数测试
 */
describe('Utility Functions', () => {
  describe('formatDate', () => {
    // 模拟日期格式化函数
    const formatDate = (date: Date | string, format = 'YYYY-MM-DD'): string => {
      const d = typeof date === 'string' ? new Date(date) : date
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hours = String(d.getHours()).padStart(2, '0')
      const minutes = String(d.getMinutes()).padStart(2, '0')
      const seconds = String(d.getSeconds()).padStart(2, '0')

      return format
        .replace('YYYY', String(year))
        .replace('MM', month)
        .replace('DD', day)
        .replace('HH', hours)
        .replace('mm', minutes)
        .replace('ss', seconds)
    }

    it('should format date with default format', () => {
      const date = new Date('2026-01-06T10:30:00')
      expect(formatDate(date)).toBe('2026-01-06')
    })

    it('should format date with custom format', () => {
      const date = new Date('2026-01-06T10:30:45')
      expect(formatDate(date, 'YYYY-MM-DD HH:mm:ss')).toBe('2026-01-06 10:30:45')
    })

    it('should handle string input', () => {
      expect(formatDate('2026-01-06')).toBe('2026-01-06')
    })
  })

  describe('debounce', () => {
    // 模拟防抖函数
    const debounce = <T extends (...args: any[]) => any>(
      fn: T,
      delay: number
    ): ((...args: Parameters<T>) => void) => {
      let timeoutId: ReturnType<typeof setTimeout> | null = null

      return (...args: Parameters<T>) => {
        if (timeoutId) {
          clearTimeout(timeoutId)
        }
        timeoutId = setTimeout(() => {
          fn(...args)
        }, delay)
      }
    }

    beforeEach(() => {
      vi.useFakeTimers()
    })

    afterEach(() => {
      vi.restoreAllMocks()
    })

    it('should delay function execution', () => {
      const fn = vi.fn()
      const debouncedFn = debounce(fn, 100)

      debouncedFn()
      expect(fn).not.toHaveBeenCalled()

      vi.advanceTimersByTime(100)
      expect(fn).toHaveBeenCalledTimes(1)
    })

    it('should only execute once for multiple calls', () => {
      const fn = vi.fn()
      const debouncedFn = debounce(fn, 100)

      debouncedFn()
      debouncedFn()
      debouncedFn()

      vi.advanceTimersByTime(100)
      expect(fn).toHaveBeenCalledTimes(1)
    })

    it('should pass arguments to the function', () => {
      const fn = vi.fn()
      const debouncedFn = debounce(fn, 100)

      debouncedFn('arg1', 'arg2')
      vi.advanceTimersByTime(100)

      expect(fn).toHaveBeenCalledWith('arg1', 'arg2')
    })
  })

  describe('throttle', () => {
    // 模拟节流函数
    const throttle = <T extends (...args: any[]) => any>(
      fn: T,
      limit: number
    ): ((...args: Parameters<T>) => void) => {
      let lastRun = 0

      return (...args: Parameters<T>) => {
        const now = Date.now()
        if (now - lastRun >= limit) {
          fn(...args)
          lastRun = now
        }
      }
    }

    beforeEach(() => {
      vi.useFakeTimers()
    })

    afterEach(() => {
      vi.restoreAllMocks()
    })

    it('should execute immediately on first call', () => {
      const fn = vi.fn()
      const throttledFn = throttle(fn, 100)

      throttledFn()
      expect(fn).toHaveBeenCalledTimes(1)
    })

    it('should throttle subsequent calls', () => {
      const fn = vi.fn()
      const throttledFn = throttle(fn, 100)

      throttledFn()
      throttledFn()
      throttledFn()

      expect(fn).toHaveBeenCalledTimes(1)
    })

    it('should allow call after limit', () => {
      const fn = vi.fn()
      const throttledFn = throttle(fn, 100)

      throttledFn()
      vi.advanceTimersByTime(100)
      throttledFn()

      expect(fn).toHaveBeenCalledTimes(2)
    })
  })

  describe('deepClone', () => {
    // 模拟深拷贝函数
    const deepClone = <T>(obj: T): T => {
      if (obj === null || typeof obj !== 'object') {
        return obj
      }

      if (Array.isArray(obj)) {
        return obj.map(item => deepClone(item)) as T
      }

      const cloned = {} as T
      for (const key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) {
          cloned[key] = deepClone(obj[key])
        }
      }
      return cloned
    }

    it('should clone primitive values', () => {
      expect(deepClone(1)).toBe(1)
      expect(deepClone('hello')).toBe('hello')
      expect(deepClone(null)).toBe(null)
    })

    it('should clone arrays', () => {
      const arr = [1, 2, [3, 4]]
      const cloned = deepClone(arr)

      expect(cloned).toEqual(arr)
      expect(cloned).not.toBe(arr)
      expect(cloned[2]).not.toBe(arr[2])
    })

    it('should clone objects', () => {
      const obj = { a: 1, b: { c: 2 } }
      const cloned = deepClone(obj)

      expect(cloned).toEqual(obj)
      expect(cloned).not.toBe(obj)
      expect(cloned.b).not.toBe(obj.b)
    })
  })

  describe('isEmpty', () => {
    // 模拟空值检查函数
    const isEmpty = (value: any): boolean => {
      if (value === null || value === undefined) return true
      if (typeof value === 'string') return value.trim() === ''
      if (Array.isArray(value)) return value.length === 0
      if (typeof value === 'object') return Object.keys(value).length === 0
      return false
    }

    it('should return true for null/undefined', () => {
      expect(isEmpty(null)).toBe(true)
      expect(isEmpty(undefined)).toBe(true)
    })

    it('should return true for empty string', () => {
      expect(isEmpty('')).toBe(true)
      expect(isEmpty('   ')).toBe(true)
    })

    it('should return false for non-empty string', () => {
      expect(isEmpty('hello')).toBe(false)
    })

    it('should return true for empty array', () => {
      expect(isEmpty([])).toBe(true)
    })

    it('should return false for non-empty array', () => {
      expect(isEmpty([1, 2])).toBe(false)
    })

    it('should return true for empty object', () => {
      expect(isEmpty({})).toBe(true)
    })

    it('should return false for non-empty object', () => {
      expect(isEmpty({ a: 1 })).toBe(false)
    })
  })
})
