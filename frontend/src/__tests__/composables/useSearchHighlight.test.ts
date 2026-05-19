/**
 * useSearchHighlight composable 单测 (J7).
 */
import { describe, it, expect } from 'vitest'
import { useSearchHighlight } from '@/composables/useSearchHighlight'

describe('useSearchHighlight', () => {
    const { highlightHtml } = useSearchHighlight()

    it('null/undefined text → 返回 ""', () => {
        expect(highlightHtml(null, 'kw')).toBe('')
        expect(highlightHtml(undefined, 'kw')).toBe('')
    })

    it('null/undefined kw → 返回 text 原样', () => {
        expect(highlightHtml('hello', null)).toBe('hello')
        expect(highlightHtml('hello', undefined)).toBe('hello')
        expect(highlightHtml('hello', '')).toBe('hello')
    })

    it('匹配 kw → 包裹 <mark class="search-mark">', () => {
        expect(highlightHtml('hello world', 'world'))
            .toBe('hello <mark class="search-mark">world</mark>')
    })

    it('大小写不敏感匹配', () => {
        expect(highlightHtml('Hello World', 'world'))
            .toBe('Hello <mark class="search-mark">World</mark>')
    })

    it('多次匹配 → 全部高亮', () => {
        expect(highlightHtml('aaa bbb aaa', 'aaa'))
            .toBe('<mark class="search-mark">aaa</mark> bbb <mark class="search-mark">aaa</mark>')
    })

    it('regex 特殊字符在 kw 中 → 转义后字面量匹配', () => {
        expect(highlightHtml('price $5.99', '$5.99'))
            .toBe('price <mark class="search-mark">$5.99</mark>')
        expect(highlightHtml('a.b.c', '.'))
            .toBe('a<mark class="search-mark">.</mark>b<mark class="search-mark">.</mark>c')
        expect(highlightHtml('(test)', '('))
            .toBe('<mark class="search-mark">(</mark>test)')
    })

    it('kw 不出现 → 返回 text 原样', () => {
        expect(highlightHtml('hello world', 'xyz')).toBe('hello world')
    })

    it('中文匹配', () => {
        expect(highlightHtml('检查模板编辑', '模板'))
            .toBe('检查<mark class="search-mark">模板</mark>编辑')
    })

    // L1 XSS escape 加固 (2026-05-19)
    describe('XSS escape', () => {
        it('text 含 <script> → escape, 无 kw 时仅返回 escape 文本', () => {
            expect(highlightHtml('<script>alert(1)</script>', null))
                .toBe('&lt;script&gt;alert(1)&lt;/script&gt;')
        })

        it('text 含 onerror img → escape, 标签字面化', () => {
            const out = highlightHtml('<img src=x onerror=alert(1)>', null)
            // 关键: 没有真正的 <img 起始标签
            expect(out).not.toContain('<img')
            expect(out).toBe('&lt;img src=x onerror=alert(1)&gt;')
        })

        it('text 含 & < > " \' → 全部 escape', () => {
            expect(highlightHtml(`a<b>c&d"e'f`, null))
                .toBe('a&lt;b&gt;c&amp;d&quot;e&#39;f')
        })

        it('text 含 XSS + kw 命中 → mark 正常工作, 危险标签字面化', () => {
            // safe text = '&lt;script&gt;x&lt;/script&gt;', kw='script' 命中
            expect(highlightHtml('<script>x</script>', 'script'))
                .toBe('&lt;<mark class="search-mark">script</mark>&gt;x&lt;/<mark class="search-mark">script</mark>&gt;')
        })

        it('kw 自身含 < > → escape 后再匹配', () => {
            // text = '<div>' 经 escape → '&lt;div&gt;', kw = '<div>' 经 escape → '&lt;div&gt;'
            expect(highlightHtml('<div>', '<div>'))
                .toBe('<mark class="search-mark">&lt;div&gt;</mark>')
        })
    })
})
