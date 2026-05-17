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
})
