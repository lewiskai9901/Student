/**
 * useKbdHint composable 单测 (J7).
 *
 * <p>NOTE: setup.ts 把 localStorage 全部 mock 成 vi.fn(), 没真实存储.
 * 这里用 mockReturnValue 控制 getItem 行为.
 */
import { describe, it, expect, vi } from 'vitest'
import { useKbdHint } from '@/composables/useKbdHint'

describe('useKbdHint', () => {
    it('首次访问 (getItem=null) → showKbdHint=true', () => {
        ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockReturnValue(null)

        const { showKbdHint } = useKbdHint('test_key_a')
        expect(showKbdHint.value).toBe(true)
    })

    it('已 dismiss (getItem=\'1\') → showKbdHint=false', () => {
        ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockReturnValue('1')

        const { showKbdHint } = useKbdHint('test_key_b')
        expect(showKbdHint.value).toBe(false)
    })

    it('dismissKbdHint() → showKbdHint=false + 调 setItem', () => {
        ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockReturnValue(null)
        const setItemSpy = localStorage.setItem as ReturnType<typeof vi.fn>

        const { showKbdHint, dismissKbdHint } = useKbdHint('test_key_c')
        expect(showKbdHint.value).toBe(true)

        dismissKbdHint()

        expect(showKbdHint.value).toBe(false)
        expect(setItemSpy).toHaveBeenCalledWith('test_key_c', '1')
    })

    it('localStorage 读异常 → 兜底视为已 dismiss (不打扰用户)', () => {
        ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockImplementation(() => {
            throw new Error('SecurityError')
        })

        const { showKbdHint } = useKbdHint('test_key_d')
        // readDismissed catch 返回 true (= already dismissed), 所以 hint 不显示
        expect(showKbdHint.value).toBe(false)
    })

    it('localStorage 写异常 → dismissKbdHint 不抛, 内存状态正常切换', () => {
        ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockReturnValue(null)
        ;(localStorage.setItem as ReturnType<typeof vi.fn>).mockImplementation(() => {
            throw new Error('QuotaExceededError')
        })

        const { showKbdHint, dismissKbdHint } = useKbdHint('test_key_e')

        expect(() => dismissKbdHint()).not.toThrow()
        expect(showKbdHint.value).toBe(false)
    })

    it('不同 storageKey 独立', () => {
        ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockReturnValue(null)

        const a = useKbdHint('test_key_f')
        const b = useKbdHint('test_key_g')

        a.dismissKbdHint()

        expect(a.showKbdHint.value).toBe(false)
        expect(b.showKbdHint.value).toBe(true)
    })
})
