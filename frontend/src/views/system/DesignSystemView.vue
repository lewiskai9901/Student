<template>
  <div class="ds-page">
    <header class="ds-head">
      <div>
        <div class="ds-eyebrow">Design System · Living Showcase</div>
        <h1 class="ds-title">设计系统</h1>
        <p class="ds-sub">
          Phase 4 P4.2 — 一站式查看通用核心的设计 token、chip、状态、印章 等组件.
          替代 Storybook (~200MB 依赖) 的轻量展示, 集成到应用内, 修改 token 实时反映.
        </p>
      </div>
    </header>

    <!-- ─── 设计 token ─── -->
    <section class="ds-section">
      <h2>Design Tokens</h2>
      <div class="ds-grid">
        <div v-for="t in tokens" :key="t.name" class="ds-token">
          <div class="ds-token-swatch" :style="{ background: t.value }" />
          <div class="ds-token-meta">
            <code class="ds-token-name">{{ t.name }}</code>
            <code class="ds-token-value">{{ t.value }}</code>
          </div>
        </div>
      </div>
    </section>

    <!-- ─── Chip ─── -->
    <section class="ds-section">
      <h2>Chip — 状态/优先级标识</h2>
      <p class="ds-section-sub">用于列表中标识状态、优先级、分类等. 7 种 variant.</p>
      <div class="ds-row">
        <span v-for="v in chipVariants" :key="v" class="insp-chip" :class="`insp-chip--${v}`">
          {{ v }}
        </span>
      </div>
    </section>

    <!-- ─── Stamp ─── -->
    <section class="ds-section">
      <h2>Stamp — 编辑设计样板印章</h2>
      <p class="ds-section-sub">类似纸质表单的印章, 比 chip 更"权威"的视觉.</p>
      <div class="ds-row">
        <span class="insp-stamp">已审核</span>
        <span class="insp-stamp" style="color: var(--insp-fail)">已逾期</span>
        <span class="insp-stamp" style="color: var(--insp-pass)">已完成</span>
      </div>
    </section>

    <!-- ─── Severity (V110) ─── -->
    <section class="ds-section">
      <h2>V110 整改严重度</h2>
      <p class="ds-section-sub">引擎判定 + 候选确认对话框使用. 4 等级.</p>
      <div class="ds-row">
        <span class="sev sev-high">HIGH 高</span>
        <span class="sev sev-medium">MEDIUM 中</span>
        <span class="sev sev-low">LOW 低</span>
        <span class="sev sev-none">NONE 不触发</span>
      </div>
    </section>

    <!-- ─── 来源标识 (V110) ─── -->
    <section class="ds-section">
      <h2>V110 整改单来源</h2>
      <p class="ds-section-sub">区分系统建议 vs 人工建单.</p>
      <div class="ds-row">
        <span class="src-chip src-engine">
          系统建议 <span class="src-score">85%</span>
        </span>
        <span class="src-chip src-manual">人工</span>
      </div>
    </section>

    <!-- ─── 项目策略 (V110) ─── -->
    <section class="ds-section">
      <h2>V110 项目策略</h2>
      <p class="ds-section-sub">项目列表显示当前整改严格度.</p>
      <div class="ds-row">
        <span class="strict-chip strict-strict">严格</span>
        <span class="strict-chip strict-normal">标准</span>
        <span class="strict-chip strict-lenient">宽松</span>
        <span class="strict-chip strict-off">关闭</span>
      </div>
    </section>

    <!-- ─── 复发警示 (V110) ─── -->
    <section class="ds-section">
      <h2>V110 复发警示</h2>
      <p class="ds-section-sub">打分时显示该项过去 30 天复发次数.</p>
      <div class="ds-row">
        <span class="recur-chip">复发 4 次</span>
        <span class="recur-chip">复发 1 次</span>
      </div>
    </section>

    <!-- ─── Eyebrow + Title 组合 ─── -->
    <section class="ds-section">
      <h2>编辑设计样板 — Eyebrow + Title</h2>
      <p class="ds-section-sub">受报刊排版启发的页面头部样式 (insp-eyebrow + insp-display).</p>
      <div class="ds-demo-card">
        <div class="insp-eyebrow">检查项目 · Inspection Campaigns</div>
        <h1 class="insp-display">检查项目</h1>
      </div>
    </section>

    <!-- ─── 等宽数字 ─── -->
    <section class="ds-section">
      <h2>insp-num — 表格等宽数字</h2>
      <p class="ds-section-sub">列表中的数字应统一等宽对齐.</p>
      <div class="ds-row">
        <span class="insp-num">42.5</span>
        <span class="insp-num">100,000</span>
        <span class="insp-num">2026-05-04</span>
        <span class="insp-num" style="color: var(--insp-fail)">-7</span>
      </div>
    </section>

    <!-- ─── 实体标签 (Phase 2) ─── -->
    <section class="ds-section">
      <h2>Phase 2 — Entity Label 运行时标签</h2>
      <p class="ds-section-sub">
        当前激活: <strong>{{ activeIndustry }}</strong> ·
        切换插件 (EDU/HEALTH 启停) 全前端字面量同步切换.
      </p>
      <table class="ds-table">
        <thead><tr><th>字段</th><th>当前值</th></tr></thead>
        <tbody>
          <tr><td>subject</td><td>{{ labels.subject }}</td></tr>
          <tr><td>group</td><td>{{ labels.group }}</td></tr>
          <tr><td>parent</td><td>{{ labels.parent }}</td></tr>
          <tr><td>place</td><td>{{ labels.place }}</td></tr>
          <tr><td>campus</td><td>{{ labels.campus }}</td></tr>
          <tr><td>organizer</td><td>{{ labels.organizer }}</td></tr>
          <tr><td>evaluator</td><td>{{ labels.evaluator }}</td></tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useEntityLabel } from '@/composables/useEntityLabel'
import { hasFeature } from '@/composables/useFeature'

const { labels } = useEntityLabel()

const activeIndustry = computed(() => {
  if (hasFeature('EDU')) return 'EDU (学校场景)'
  if (hasFeature('HEALTH')) return 'HEALTH (医疗场景)'
  return 'CORE (通用兜底)'
})

const tokens = [
  { name: '--insp-accent',           value: '#2563eb' },
  { name: '--insp-pass',             value: '#15803d' },
  { name: '--insp-warn',             value: '#b45309' },
  { name: '--insp-fail',             value: '#b91c1c' },
  { name: '--insp-bg-page',          value: '#f8fafc' },
  { name: '--insp-bg-surface',       value: '#ffffff' },
  { name: '--insp-border-default',   value: '#e5e7eb' },
  { name: '--insp-border-strong',    value: '#d1d5db' },
  { name: '--insp-ink-primary',      value: '#111827' },
  { name: '--insp-ink-secondary',    value: '#4b5563' },
  { name: '--insp-ink-tertiary',     value: '#9ca3af' },
]

const chipVariants = ['default', 'pass', 'warn', 'fail', 'pending', 'info', 'neutral']
</script>

<style scoped>
.ds-page {
  padding: 24px 32px;
  max-width: 1200px;
  margin: 0 auto;
}
.ds-head {
  padding: 16px 0 24px;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 24px;
}
.ds-eyebrow { font-size: 11px; color: #9ca3af; letter-spacing: 0.05em; }
.ds-title { font-size: 26px; font-weight: 700; color: #111827; margin: 6px 0; }
.ds-sub { font-size: 13px; color: #6b7280; max-width: 700px; line-height: 1.6; }

.ds-section {
  margin-bottom: 28px;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}
.ds-section h2 {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 6px;
}
.ds-section-sub {
  font-size: 12px;
  color: #6b7280;
  margin: 0 0 14px;
}

.ds-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}
.ds-token {
  display: flex; align-items: center; gap: 10px;
  padding: 8px;
  border: 1px solid #f3f4f6;
  border-radius: 4px;
}
.ds-token-swatch {
  width: 36px; height: 36px;
  border-radius: 4px;
  border: 1px solid rgba(0,0,0,0.08);
  flex-shrink: 0;
}
.ds-token-meta { font-size: 11px; line-height: 1.4; min-width: 0; }
.ds-token-name { display: block; color: #1f2937; font-weight: 500; }
.ds-token-value { color: #6b7280; }

.ds-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }

.ds-demo-card {
  padding: 16px 20px;
  background: #f9fafb;
  border-radius: 4px;
}

.ds-table {
  width: 100%; border-collapse: collapse; font-size: 13px;
}
.ds-table th, .ds-table td {
  padding: 6px 12px; border: 1px solid #f3f4f6; text-align: left;
}
.ds-table th { background: #f9fafb; font-weight: 500; color: #6b7280; font-size: 12px; }
.ds-table td:first-child { font-family: ui-monospace, monospace; color: #4b5563; }

/* ─── 复刻 V110 chip 样式 ─── */
.sev {
  display: inline-block; padding: 2px 10px; border-radius: 3px;
  font-size: 12px; font-weight: 600;
}
.sev-high   { background: #fee2e2; color: #b91c1c; }
.sev-medium { background: #fef3c7; color: #92400e; }
.sev-low    { background: #dcfce7; color: #15803d; }
.sev-none   { background: #f3f4f6; color: #9ca3af; }

.src-chip {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 1px 7px; border-radius: 3px;
  font-size: 11px; font-weight: 500;
}
.src-engine { background: #ede9fe; color: #6d28d9; border: 1px solid #ddd6fe; }
.src-manual { background: #f3f4f6; color: #6b7280; border: 1px solid #e5e7eb; }
.src-score {
  background: #6d28d9; color: #fff;
  padding: 0 4px; border-radius: 2px; font-size: 10px;
  font-family: ui-monospace, monospace;
}

.strict-chip {
  display: inline-block; padding: 1px 7px;
  font-size: 11px; border-radius: 3px; font-weight: 500;
}
.strict-strict  { background: #fee2e2; color: #b91c1c; }
.strict-normal  { background: #ede9fe; color: #6d28d9; }
.strict-lenient { background: #d1fae5; color: #047857; }
.strict-off     { background: #f3f4f6; color: #9ca3af; }

.recur-chip {
  display: inline-block; padding: 1px 7px;
  background: #fee2e2; color: #b91c1c;
  border: 1px solid #fca5a5; border-radius: 3px;
  font-size: 11px; font-weight: 600;
  animation: recur-pulse 2.4s ease-in-out infinite;
}
@keyframes recur-pulse {
  0%, 100% { background: #fee2e2; }
  50%      { background: #fecaca; }
}
</style>
