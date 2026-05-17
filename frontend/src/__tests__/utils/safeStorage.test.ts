/**
 * safeLocalStorage / safeSessionStorage 单测 (K1).
 */
import { describe, it, expect, vi } from 'vitest'
import { safeLocalStorage, safeSessionStorage } from '@/utils/safeStorage'

describe('safeLocalStorage', () => {
    describe('happy path', () => {
        it('getItem 委托 localStorage.getItem', () => {
            ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockReturnValue('val')
            expect(safeLocalStorage.getItem('key')).toBe('val')
        })

        it('setItem 返回 true 表示成功', () => {
            expect(safeLocalStorage.setItem('key', 'val')).toBe(true)
            expect(localStorage.setItem).toHaveBeenCalledWith('key', 'val')
        })

        it('removeItem 返回 true 表示成功', () => {
            expect(safeLocalStorage.removeItem('key')).toBe(true)
            expect(localStorage.removeItem).toHaveBeenCalledWith('key')
        })

        it('getJSON 反序列化', () => {
            ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockReturnValue('{"a":1,"b":"x"}')
            expect(safeLocalStorage.getJSON<{ a: number; b: string }>('key'))
                .toEqual({ a: 1, b: 'x' })
        })

        it('setJSON 序列化', () => {
            expect(safeLocalStorage.setJSON('key', { foo: 'bar' })).toBe(true)
            expect(localStorage.setItem).toHaveBeenCalledWith('key', '{"foo":"bar"}')
        })
    })

    describe('Safari 隐私模式 / 配额异常 — 静默兜底', () => {
        it('getItem 异常 → null', () => {
            ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockImplementation(() => {
                throw new Error('SecurityError')
            })
            expect(safeLocalStorage.getItem('key')).toBeNull()
        })

        it('setItem 异常 → false (不抛)', () => {
            ;(localStorage.setItem as ReturnType<typeof vi.fn>).mockImplementation(() => {
                throw new Error('QuotaExceededError')
            })
            expect(() => safeLocalStorage.setItem('key', 'val')).not.toThrow()
            expect(safeLocalStorage.setItem('key', 'val')).toBe(false)
        })

        it('removeItem 异常 → false', () => {
            ;(localStorage.removeItem as ReturnType<typeof vi.fn>).mockImplementation(() => {
                throw new Error('SecurityError')
            })
            expect(safeLocalStorage.removeItem('key')).toBe(false)
        })

        it('getJSON 异常 → null', () => {
            ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockImplementation(() => {
                throw new Error('SecurityError')
            })
            expect(safeLocalStorage.getJSON('key')).toBeNull()
        })

        it('getJSON fallback 参数生效', () => {
            ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockReturnValue(null)
            expect(safeLocalStorage.getJSON('key', { default: true }))
                .toEqual({ default: true })
        })

        it('getJSON 解析失败 → null', () => {
            ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockReturnValue('{ invalid json')
            expect(safeLocalStorage.getJSON('key')).toBeNull()
        })

        it('setJSON 异常 → false', () => {
            ;(localStorage.setItem as ReturnType<typeof vi.fn>).mockImplementation(() => {
                throw new Error('QuotaExceededError')
            })
            expect(safeLocalStorage.setJSON('key', { foo: 'bar' })).toBe(false)
        })
    })

    describe('safeSessionStorage', () => {
        it('独立 storage backend', () => {
            ;(sessionStorage.getItem as ReturnType<typeof vi.fn>).mockReturnValue('session-val')
            expect(safeSessionStorage.getItem('key')).toBe('session-val')
            // 没调 localStorage
            expect(localStorage.getItem).not.toHaveBeenCalledWith('key')
        })

        it('sessionStorage 异常 → null', () => {
            ;(sessionStorage.getItem as ReturnType<typeof vi.fn>).mockImplementation(() => {
                throw new Error('SecurityError')
            })
            expect(safeSessionStorage.getItem('key')).toBeNull()
        })
    })
})
