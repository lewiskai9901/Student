/**
 * 登录页文本格式化 — XSS 防护 (L1, 2026-05-19).
 *
 * 背景:
 *   LoginView.vue / LoginPreview.vue 用 v-html 渲染 admin 在 system_configs
 *   里写的 title / subtitle. 之前只做 \n→<br> 和 **xxx**→<span> 两种语法替换,
 *   但原始文本未 escape — admin 一句 `<img src=x onerror=alert(1)>` 就能在
 *   登录页 (所有用户必经) 触发存储型 XSS.
 *
 * 修复:
 *   - 先 escape 原文 (& < > " ')
 *   - 再做 \n→<br> 和 **xxx**→<span> 两种安全 transform
 *   - 输出永远只含字面文本 + 受控标签
 */

/**
 * HTML escape 工具 — 防 XSS 注入.
 *
 * 同时 escape 单引号和双引号, 这样输出也可以安全嵌入 attribute 上下文
 * (虽然当前调用方都是元素 text 上下文).
 */
export function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * 格式化登录页标题 — 支持 \n 换行.
 *
 * 输入 `Hello\nWorld` 输出 `Hello<br>World`.
 * 输入 `<script>` 输出 `&lt;script&gt;` (无害).
 */
export function formatLoginTitle(raw: string | null | undefined): string {
  if (!raw) return ''
  return escapeHtml(raw).replace(/\\n/g, '<br>')
}

/**
 * 格式化登录页副标题 — 支持 **关键词** 蓝色高亮.
 *
 * @param raw 原始文本
 * @param spanAttr 高亮 span 的属性 (class 或 style), LoginView 用 class, LoginPreview 用 style
 */
export function formatLoginSubtitle(
  raw: string | null | undefined,
  spanAttr: string = 'class="text-blue-400 font-semibold"',
): string {
  if (!raw) return ''
  // escape 后, `**` 字面字符还在, 用 regex 替换为受控 span
  return escapeHtml(raw).replace(/\*\*(.+?)\*\*/g, `<span ${spanAttr}>$1</span>`)
}
