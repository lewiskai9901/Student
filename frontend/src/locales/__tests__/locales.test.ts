/**
 * F7 i18n 骨架 — locales key 对齐守护测试.
 *
 * 防止两份 locale 文件 drift: zh-CN 加 key 但忘记同步 en-US (或反之).
 * 缺翻译时 vue-i18n 会回退到 fallbackLocale, 但严格对齐能更早暴露问题.
 */
import { describe, it, expect } from 'vitest'
import zhCN from '@/locales/zh-CN'
import enUS from '@/locales/en-US'

describe('i18n locales', () => {
  it('zhCN 和 enUS top-level keys 一致', () => {
    expect(Object.keys(zhCN).sort()).toEqual(Object.keys(enUS).sort())
  })

  it('common 子键一致', () => {
    expect(Object.keys(zhCN.common).sort()).toEqual(Object.keys(enUS.common).sort())
  })

  it('nav 子键一致', () => {
    expect(Object.keys(zhCN.nav).sort()).toEqual(Object.keys(enUS.nav).sort())
  })
})
