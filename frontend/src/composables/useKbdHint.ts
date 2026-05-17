import { ref, type Ref } from 'vue'

/**
 * 键盘快捷键提示 dismiss 状态 composable (J7).
 *
 * <p>之前 7 个 inspection view 各自 copy-paste 同一份 dismissKbdHint + showKbdHint,
 * 只是 localStorage key 不同. 集中后唯一一处, 顺手把 localStorage 读写包了 try/catch
 * (Safari 隐私模式 / 配额耗尽不会 throw QuotaExceededError 崩 setup).
 *
 * 用法:
 *   const { showKbdHint, dismissKbdHint } = useKbdHint('insp_prj_kbd_hint_dismissed')
 *
 * @param storageKey localStorage 的唯一 key
 */
export function useKbdHint(storageKey: string): {
    showKbdHint: Ref<boolean>
    dismissKbdHint: () => void
} {
    // 已 dismiss → showKbdHint=false; 未 dismiss / localStorage 读异常 → 兜底不打扰用户 (false)
    const showKbdHint = ref<boolean>(!isDismissed(storageKey))

    function dismissKbdHint(): void {
        showKbdHint.value = false
        try {
            localStorage.setItem(storageKey, '1')
        } catch {
            /* Safari 隐私模式 / 配额耗尽 — 静默忽略, 不影响内存中状态 */
        }
    }

    return { showKbdHint, dismissKbdHint }
}

/**
 * 判断指定 key 是否已被 dismiss.
 * 异常路径返回 true (视为已 dismiss), 这样隐私模式 / 配额异常时不会反复显示 hint 打扰用户.
 */
function isDismissed(storageKey: string): boolean {
    try {
        return localStorage.getItem(storageKey) === '1'
    } catch {
        return true
    }
}
