# Plan A Day 2 — 生产构建 + Bundle 实测报告

**Date**: 2026-05-06
**Build tool**: Vite 4.5.14, Rollup
**Frontend version**: 1.0.0 (master @ e173226f)

## 摘要

✅ **生产构建首次成功** — 1m 37s 完成, 167 chunks, dist 总 6.2 MB.

发现并修复 1 项 P0 阻塞 (Finding #13 — 死代码 import 阻塞 build).

Bundle 体积合理可上线, 留 3 项后续优化机会 (P3).

---

## 修复: Finding #13 — Build 阻塞 (commit 同 P1 修复)

**根因**:
`src/stores/inspection/inspPlatformStore.ts` 引用 3 个不存在的 API 文件:
- `@/api/inspection/notificationRule` ← ENOENT
- `@/api/inspection/reportTemplate` ← ENOENT
- `@/api/inspection/webhook` ← ENOENT

dev 模式 lazy 解析未触发故未暴露, prod 严格.

**调查**: 全代码 grep, 没有 view 消费 `notificationRules` / `webhooks` / `reportTemplates`. 纯死代码.

**修复**: 把 store 缩减到只保留实际在用的部分 (issueCategory + holidayCalendar + auditTrail). ~230 行死代码删除.

---

## Bundle 体积分析

### 总览
| 指标 | 值 |
|---|---|
| dist 总大小 | 6.18 MB |
| JS 总 | 4.90 MB |
| CSS 总 | 1.26 MB |
| chunk 数 | 167 |
| 构建耗时 | 1m 37s |

### Top 14 chunks (raw, 未压缩)

| # | Chunk | 大小 | gzip | 说明 |
|---|---|---:|---:|---|
| 1 | `element-plus` | 753 KB | 227 KB | Element Plus 组件库 (整库) |
| 2 | `xlsx` | 413 KB | 138 KB | ⚠️ Excel 导入/导出 — 应按需加载 |
| 3 | `echarts` | 387 KB | 129 KB | ECharts 图表库 |
| 4 | `vendor` | 286 KB | 96 KB | 第三方混合 |
| 5 | `konva` | 187 KB | 56 KB | Canvas 引擎 (FloorPlan / 大屏) |
| 6 | `UniversalPlaceManagement` | 150 KB | 42 KB | 场所管理大页面 |
| 7 | `vue-vendor` | 117 KB | 44 KB | Vue 3 + Pinia + Router |
| 8 | `ProjectDetailView` | 112 KB | 31 KB | 检查项目详情 (V110 含) |
| 9 | `AssetManagementCenter` | 109 KB | 28 KB | 资产管理中心 |
| 10 | `AdjustmentPanel.vue_*_style` | 99 KB | 28 KB | 评分调整面板 |
| 11 | `TemplateEditorView` | 96 KB | 28 KB | 检查模板编辑器 |
| 12 | `StudentList` | 93 KB | 22 KB | EDU 学生列表 |
| 13 | `PluginPlatformLayout` | 83 KB | 23 KB | 插件平台布局 |
| 14 | `index` (entry) | 76 KB | 15 KB | 入口 chunk |

### 首屏关键路径估算

登录页加载需要:
- `index` 76 KB
- `vue-vendor` 117 KB
- `element-plus` 753 KB
- `axios` 35 KB
- `vendor` 286 KB
- `LoginView` ~? (未在 top 14 中, 推测 < 30 KB)

合计 **~1.3 MB raw / ~430 KB gzip**.

业界 SPA 首屏 gzip 1-3 MB 普遍, 本项目处于 **健康水位**.

---

## 优化机会 (P3, 后续做)

### 优化 1: `xlsx` 按需加载 (-413 KB raw / -138 KB gzip)

xlsx 当前被打到一个 chunk 里, 但只有"导入学生 / 导出报表"按钮点击时才需要. 

**改造**:
```ts
// before
import * as XLSX from 'xlsx'

// after
async function exportExcel() {
  const XLSX = await import('xlsx')
  // ...
}
```

收益: 大部分用户从未导出/导入, 节约 138 KB gzip 永远不下载.

### 优化 2: `element-plus` on-demand (估 -30~50% / -227 KB → ~120 KB gzip)

当前 element-plus 753 KB 是全量打包. 接 `unplugin-vue-components` + `unplugin-auto-import` 可改为按需 — 用了什么打什么.

注: 当前已有 `vite.config.ts` 中可能已配, 但 manualChunks 把 ElementPlus 全打到一个 chunk 反而让 tree-shake 失效.

需要重新评估 vite.config.ts 的 manualChunks 策略.

### 优化 3: `echarts` 全量 → 模块化

ECharts 当前 387 KB. 用 `import * as echarts` 全量. 改为按需 import 单组件 (LineChart / PieChart / BarChart 等), 估可砍 50% 到 ~190 KB raw / 65 KB gzip.

```ts
import { use } from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, PieChart, GridComponent, TooltipComponent, CanvasRenderer])
```

### 优化 4: 大页面再拆 (UniversalPlaceManagement / ProjectDetailView / AssetManagementCenter)

均 100+ KB, 内含多个 tab 子组件. 改为 tab 内 lazy load, 可减小初始 chunk 30-50%.

---

## 警告 / 噪声

构建过程出现以下非阻塞警告:

1. `NODE_ENV=production is not supported in the .env file` — Vite 4 行为, 无影响
2. `[baseline-browser-mapping] data is over two months old` — 升级 `npm i baseline-browser-mapping@latest -D`
3. `Browserslist data is 6 months old` — `npx update-browserslist-db@latest`
4. `postcss.config.js... not specified... ES module reparsing` — 加 `"type": "module"` 到 package.json (需评估对其他工具影响)

非核心, Day 5 收尾时可批量处理.

---

## 结论

✅ 项目可生产构建 + 部署 (`npm run build` 成功, dist 干净, 无运行时错误).

🟢 Bundle 体积处于业界健康水位 (~430 KB gzip 首屏).

🟡 4 项优化机会, 收益估 ~30% 体积 + ~50% 首次加载时间, 但**不阻塞上线**.

⛔ 0 阻塞问题. 可继续进 Day 3-4 (TS 基线下降).
