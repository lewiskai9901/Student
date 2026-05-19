/**
 * 搜索关键词高亮 composable (J7 + L1 XSS escape 加固).
 *
 * <p>之前 7 个 inspection view 各自 copy-paste 同一份 highlightHtml, 集中后
 * 唯一一处, 也方便统一审 XSS escape 逻辑.
 *
 * 用法:
 *   const { highlightHtml } = useSearchHighlight()
 *   <span v-html="highlightHtml(text, kw)" />
 *
 * 安全 (L1, 2026-05-19):
 *   - text 参数先 HTML escape, 然后才做 regex 替换 — 防用户/管理员可控数据
 *     (项目名 / 学生名 / 单项名 等) 注入 XSS.
 *   - 关键词 regex 转义后才嵌入 RegExp, 防 regex 注入.
 *   - 输出永远只含 escape 文本 + 受控 &lt;mark&gt; 标签.
 */
import { escapeHtml } from '@/utils/loginTextFormat'

export function useSearchHighlight() {
    function highlightHtml(text: string | null | undefined, kw: string | null | undefined): string {
        if (!text) return ''
        const safeText = escapeHtml(text)
        if (!kw) return safeText
        const escapedKw = escapeHtml(kw).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
        return safeText.replace(new RegExp(`(${escapedKw})`, 'gi'), '<mark class="search-mark">$1</mark>')
    }

    return { highlightHtml }
}
