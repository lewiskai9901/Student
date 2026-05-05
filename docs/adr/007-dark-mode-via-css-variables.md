# ADR-007: Dark mode via CSS 变量 + data-theme 切换

**Date**: 2026-05-05
**Status**: Accepted
**Phase**: 前端冲 A+ Phase 5 (P5.2)
**Commit**: `527310cc`

## Context

前端冲 A+ 路线图要求**深色模式**支持. 项目设计系统已有 `--insp-*` CSS 变量 (`styles/inspection-tokens.css`), 但所有变量定义在 `:root`, 没有深色覆盖. 业界 dark mode 主流方案对比:

| 方案 | 优 | 劣 |
|---|---|---|
| **A. CSS 变量 + data-theme** | 切换瞬时, 运行时无 JS, 1 行 attr 切换 | 必须用变量, 不能 hex 字面量 |
| B. 类名切换 (.dark .my-class) | 简单 | scoped CSS 难写, 嵌套复杂 |
| C. JS 动态注入 stylesheet | 灵活 | FOUC, 性能差 |
| D. PostCSS 编译双 stylesheet | 静态 | 包体 ×2, 切换需 reload |

## Decision

**A 方案**: 在 `inspection-tokens.css` 末尾追加 `html[data-theme='dark']` 块, 反转 / 调亮 50+ 个 token. 切换通过 `document.documentElement.setAttribute('data-theme', 'dark')`, **零 JS 重渲染成本**.

### 实现

#### 1. Token 反转 (50+ 变量)

```css
:root {
  --insp-gray-50:  #f8fafc;   /* light: 最浅 */
  --insp-gray-900: #0f172a;   /* light: 最深 */
  --insp-bg-page: var(--insp-gray-50);
  --insp-ink-primary: var(--insp-gray-900);
  /* ... */
}

html[data-theme='dark'] {
  --insp-gray-50:  #0f172a;   /* dark: 最浅 = 原最深 */
  --insp-gray-900: #f8fafc;   /* dark: 最深 = 原最浅 */
  /* 表面色用专属深色, 不直接复用反转灰阶 (避免对比度过强) */
  --insp-bg-page: #0f172a;
  --insp-bg-surface: #1e293b;
  /* 语义色调亮以匹配深色背景 */
  --insp-accent: #60a5fa;
  --insp-pass: #34d399;
  --insp-fail: #f87171;
  /* 阴影深化扩散 */
  --insp-shadow-md: 0 4px 12px rgba(0, 0, 0, 0.4);
}
```

任何使用 `var(--insp-*)` 的组件**自动适配**, 无需修改业务代码.

#### 2. useTheme composable

```ts
const { theme, isDark, setTheme, toggle, initTheme } = useTheme()
// theme: 'light' | 'dark' | 'system'
// system 模式跟随 prefers-color-scheme + matchMedia 监听
// localStorage('app-theme') 持久化
// initTheme() 在 App.vue 顶部立即调用 → 防 FOUC
```

#### 3. 启动时序 (防 FOUC)

```ts
// App.vue
import { themeApi } from '@/composables/useTheme'
themeApi.init()        // 同步执行, setup 之前 → <html> 立即有 data-theme
onMounted(() => { ... }) // 后续 mounted 业务
```

如果用 `onMounted` 才 init, 用户会看到一闪而过的 light → dark.

## Consequences

### Positive
- 切换瞬时 (< 16ms, 单 attr 写入)
- 业务组件 0 修改 — 只要用了 `var(--insp-*)` 就自动适配
- WCAG AA 对比度满足: ink-primary (#f1f5f9) on bg-surface (#1e293b) = 12:1
- `prefers-color-scheme` 自动识别系统主题
- localStorage 跨刷新持久化
- system 模式 matchMedia 监听 → 系统主题切换实时跟随

### Negative
- **强制使用 CSS 变量**: 任何 hex 字面量 (`color: #dc2626`) 在 dark 下不变 → 视觉脱节. 后续需逐步迁移 (Phase 5.5 计划)
- 部分 V110 组件仍有 hex 字面量 (recur-chip / src-chip / strict-chip 等), 计划批量替换为 token
- 第三方组件 (Element Plus) 暗色支持需另接 `el-config-provider` (将来加)

### Migration Plan

1. **Phase 5 (本次)**: 引入框架 + token 反转
2. **Phase 5.5 (后续)**: hex → 变量批量替换 (~200 处, 预计 1 天)
3. **Phase 6 (可选)**: Element Plus dark theme 接入

## Verification

- 7/7 单测绿: setTheme/toggle/persist/restore/init/system 跟随
- DesignSystemView 头部加切换按钮 (浅色/深色/跟随系统)
- 实测切换无闪烁 (< 16ms)

## Trade-offs Considered

**B. class 切换 (.dark .my-comp)** — 拒绝, scoped CSS 写嵌套 selector 复杂 + 需重构所有组件
**C. JS 注入** — 拒绝, FOUC + 性能差
**D. 双编译** — 拒绝, 包体 ×2

## Related

- [ADR-006 轻量 i18n](./006-lightweight-i18n-without-vue-i18n.md)
- 入口: `src/composables/useTheme.ts`
- token 文件: `src/styles/inspection-tokens.css`
- DesignSystemView 实时验证: `/system/design-system`
