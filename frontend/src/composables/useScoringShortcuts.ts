/**
 * useScoringShortcuts — 任务打分页键盘快捷键 (Audit Console redesign)
 *
 * 暴露的钩子供调用方实现具体动作:
 *   onPass / onFail   — 数字 1/2 键, 当前 detail 选择 通过/不通过
 *   onPrev / onNext   — j/k 或 ←/→ 切换目标
 *   onSubmit          — s, 提交任务
 *   onSearch          — / , 聚焦搜索框
 *
 * 输入框/textarea 焦点时所有快捷键自动失效.
 */
import { onMounted, onUnmounted } from 'vue'

export interface ScoringShortcutHandlers {
  onPass?: () => void
  onFail?: () => void
  onPrev?: () => void
  onNext?: () => void
  onSubmit?: () => void
  onSearch?: () => void
  onEscape?: () => void
}

export function useScoringShortcuts(handlers: ScoringShortcutHandlers) {
  function onKey(e: KeyboardEvent) {
    const target = e.target as HTMLElement
    const tag = target?.tagName
    if (tag === 'INPUT' || tag === 'TEXTAREA' || target?.isContentEditable) return
    if (e.metaKey || e.ctrlKey || e.altKey) return

    switch (e.key) {
      case '1':
        if (handlers.onPass) { handlers.onPass(); e.preventDefault() }
        break
      case '2':
        if (handlers.onFail) { handlers.onFail(); e.preventDefault() }
        break
      case 'j':
      case 'ArrowDown':
        if (handlers.onNext) { handlers.onNext(); e.preventDefault() }
        break
      case 'k':
      case 'ArrowUp':
        if (handlers.onPrev) { handlers.onPrev(); e.preventDefault() }
        break
      case 's':
        if (handlers.onSubmit) { handlers.onSubmit(); e.preventDefault() }
        break
      case '/':
        if (handlers.onSearch) { handlers.onSearch(); e.preventDefault() }
        break
      case 'Escape':
        if (handlers.onEscape) { handlers.onEscape() }
        break
    }
  }

  onMounted(() => window.addEventListener('keydown', onKey))
  onUnmounted(() => window.removeEventListener('keydown', onKey))
}
