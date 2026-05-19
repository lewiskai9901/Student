/**
 * loginTextFormat XSS escape 单测 (L1).
 */
import { describe, it, expect } from 'vitest'
import {
  escapeHtml,
  formatLoginTitle,
  formatLoginSubtitle,
} from '@/utils/loginTextFormat'

describe('escapeHtml', () => {
  it('escape 全部 5 个危险字符', () => {
    expect(escapeHtml(`<>&"'`)).toBe('&lt;&gt;&amp;&quot;&#39;')
  })

  it('空串原样', () => {
    expect(escapeHtml('')).toBe('')
  })

  it('普通文本原样', () => {
    expect(escapeHtml('Hello World 你好')).toBe('Hello World 你好')
  })

  it('& 先处理 (防止 &amp; 再被 &amp; 转义)', () => {
    expect(escapeHtml('&lt;')).toBe('&amp;lt;')
  })
})

describe('formatLoginTitle', () => {
  it('null / undefined → 空串', () => {
    expect(formatLoginTitle(null)).toBe('')
    expect(formatLoginTitle(undefined)).toBe('')
    expect(formatLoginTitle('')).toBe('')
  })

  it('普通文本无变化', () => {
    expect(formatLoginTitle('我的学校')).toBe('我的学校')
  })

  it('\\n 转换 <br>', () => {
    expect(formatLoginTitle('Line1\\nLine2')).toBe('Line1<br>Line2')
  })

  it('XSS 注入被中和 — script 标签', () => {
    expect(formatLoginTitle('<script>alert(1)</script>'))
      .toBe('&lt;script&gt;alert(1)&lt;/script&gt;')
  })

  it('XSS 注入被中和 — img onerror', () => {
    const out = formatLoginTitle('<img src=x onerror=alert(1)>')
    // 关键: 没有真正的 <img 起始标签 (已 escape 为 &lt;img)
    expect(out).not.toContain('<img')
    expect(out).toBe('&lt;img src=x onerror=alert(1)&gt;')
  })

  it('XSS + \\n 混合 — 仍 escape 标签, 仅 \\n→br', () => {
    expect(formatLoginTitle('Hello\\n<script>x</script>'))
      .toBe('Hello<br>&lt;script&gt;x&lt;/script&gt;')
  })
})

describe('formatLoginSubtitle', () => {
  it('null / undefined → 空串', () => {
    expect(formatLoginSubtitle(null)).toBe('')
    expect(formatLoginSubtitle(undefined)).toBe('')
  })

  it('普通文本无变化', () => {
    expect(formatLoginSubtitle('欢迎使用')).toBe('欢迎使用')
  })

  it('**xxx** 转 span (默认 class 模式)', () => {
    expect(formatLoginSubtitle('登录 **系统**'))
      .toBe('登录 <span class="text-blue-400 font-semibold">系统</span>')
  })

  it('自定义 spanAttr (style 模式, LoginPreview 用)', () => {
    expect(formatLoginSubtitle('**高亮**', 'style="color: #60a5fa;"'))
      .toBe('<span style="color: #60a5fa;">高亮</span>')
  })

  it('XSS 注入被中和 — script 标签', () => {
    expect(formatLoginSubtitle('<script>alert(1)</script>'))
      .toBe('&lt;script&gt;alert(1)&lt;/script&gt;')
  })

  it('XSS 注入被中和 — 含 ** 高亮的 payload', () => {
    const out = formatLoginSubtitle('**<img src=x onerror=alert(1)>**')
    // 关键: 没有真正的 <img 起始标签
    expect(out).not.toContain('<img')
    // 高亮 span 仍工作, 但里面的 < > 已 escape
    expect(out).toContain('<span class="text-blue-400 font-semibold">')
    expect(out).toContain('&lt;img src=x onerror=alert(1)&gt;')
  })

  it('多对 ** ** — 都被替换', () => {
    expect(formatLoginSubtitle('**A** 和 **B**'))
      .toBe(
        '<span class="text-blue-400 font-semibold">A</span>' +
        ' 和 ' +
        '<span class="text-blue-400 font-semibold">B</span>',
      )
  })
})
