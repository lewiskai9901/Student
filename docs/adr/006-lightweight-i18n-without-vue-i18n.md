# ADR-006: 轻量 i18n composable (不引入 vue-i18n)

**Date**: 2026-05-05
**Status**: Accepted
**Phase**: 前端冲 A+ Phase 5 (P5.3)
**Commit**: `527310cc`

## Context

前端冲 A+ 路线图要求**多语支持**作为开放性指标 (面向海外/多租户). 业界标准方案是 [vue-i18n](https://vue-i18n.intlify.dev/), 但:

- vue-i18n v9 包体 ~70KB gzip
- 复数/datetime 格式化等高级特性当前**用不到**
- 项目当前只需"中英双语 + 嵌套 key + localStorage 持久化"
- 引入新依赖增加 supply chain 风险, lock 文件膨胀

同时 Phase 2 已有 `useEntityLabel()` 成功证明"轻量 composable 比库依赖更适合本项目"模式.

## Decision

**实现轻量 `useI18n()` composable**, 同名 API 与 vue-i18n 兼容, 0 新依赖. 未来高级特性需要时, **可平滑替换**为 vue-i18n.

### API 设计 (与 vue-i18n 对齐)

```ts
const { t, locale, setLocale, availableLocales } = useI18n()
t('common.confirm')                    // → "确认"
t('greeting', { name: 'Alice' })       // 占位符 {name}
locale.value                           // ref<'zh-CN'|'en-US'>
setLocale('en-US')                     // 切换 + 持久化
```

### 实现要点

1. **locale 单例 ref**: 全前端共享, setLocale 触发响应式重渲染
2. **嵌套 key**: `'a.b.c'` 路径 split + 递归查找
3. **回退链**: 当前 locale → DEFAULT_LOCALE (zh-CN) → key 字符串本身
4. **占位符**: `{name}` 等 mustache-like 替换
5. **持久化**: localStorage('app-locale') + 启动按 navigator.language 推断
6. **locales 文件**: `src/locales/{zh-CN,en-US}.ts` 纯 TS object (不需 JSON loader)

### 文件组织

```
src/composables/useI18n.ts        # 60 行, 含 t / locale / setLocale
src/locales/zh-CN.ts              # 5 模块 (common/inspection/...)
src/locales/en-US.ts              # 同结构镜像
src/__tests__/composables/useI18n.test.ts  # 10 单测
```

## Consequences

### Positive
- 0 依赖增加, lock 文件不变
- API 与 vue-i18n 兼容, 未来无痛迁移
- 调试简单 — composable 总共 ~80 行, 一目了然
- TypeScript 友好 — 翻译表是 TS 对象, 错 key 编译期发现 (将来可加 type-safe key)
- 启动时按 navigator.language 自动选语 — 海外用户 Out-Of-Box

### Negative
- 暂无复数处理 (en: "1 task" / "2 tasks") — 当前业务不需要, 出现时再加
- 暂无 datetime/number 格式化 — 用 `toLocaleString()` 直接调
- 暂无 RTL 布局支持 — 中英双语下 LTR 即可

### When to upgrade to vue-i18n

满足任一即启动:
1. 需要复数/序数/性别等语言特性
2. 翻译团队需要 .json/.po 等标准格式 + 第三方翻译工具
3. 支持的语言数 ≥ 5 种, 翻译表已超 1000 key
4. 服务端渲染需要 i18n (vue-i18n 有 SSR mode)

## Verification

- 10/10 单测绿: 默认/切换/缺 key/回退/嵌套/持久化/恢复/响应式/availableLocales/format
- 实测 zh-CN ↔ en-US 切换实时刷新 UI
- 浏览器 localStorage 持久化跨刷新

## Trade-offs Considered

**A. 直接引入 vue-i18n** — 拒绝, 当前 ROI 负
**B. JSON 翻译表 + i18next 全功能** — 拒绝, 同上
**C. 手写 ✓** — 当前选择, 80 行 + 10 测覆盖 + 平滑升级路径

## Related

- [ADR-005 视图插件化](./005-frontend-view-plugin-isolation.md)
- 入口: `src/composables/useI18n.ts`
- 测试: `src/__tests__/composables/useI18n.test.ts`
