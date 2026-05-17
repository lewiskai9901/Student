/**
 * 搜索关键词高亮 composable (J7).
 *
 * <p>之前 7 个 inspection view (ProjectListView / CorrectiveCaseListView /
 * MyInspectionView / InspectionConfigView / ScoringProfileListView /
 * GradeSchemeListView / ItemLibraryView) 各自 copy-paste 同一份 highlightHtml,
 * 集中后唯一一处, 也方便统一审 XSS escape 逻辑.
 *
 * 用法:
 *   const { highlightHtml } = useSearchHighlight()
 *   <span v-html="highlightHtml(text, kw)" />
 *
 * 安全:
 *   - 关键词 regex 转义后才嵌入 RegExp, 防注入
 *   - text 参数自身**未做 HTML escape** — 调用方必须确保 text 来自可信源
 *     (后端 plain text, 已 escape 过的内容). 与 v-html 配对时格外注意.
 */
export function useSearchHighlight() {
    function highlightHtml(text: string | null | undefined, kw: string | null | undefined): string {
        if (!text) return ''
        if (!kw) return text
        const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
        return text.replace(new RegExp(`(${escaped})`, 'gi'), '<mark class="search-mark">$1</mark>')
    }

    return { highlightHtml }
}
