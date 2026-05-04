<template>
  <div class="rcv-page">
    <header class="rcv-head">
      <div>
        <div class="rcv-eyebrow">受检主体 · My Inspections</div>
        <h1 class="rcv-title">我被检查的记录</h1>
      </div>
      <div class="rcv-kpi">
        <div class="kpi"><span class="kpi-num">{{ summary?.totalInspections ?? 0 }}</span><span class="kpi-label">检查次数</span></div>
        <div class="kpi-sep" />
        <div class="kpi">
          <span class="kpi-num">
            {{ summary?.avgScore != null ? summary.avgScore.toFixed(1) : '—' }}
          </span>
          <span class="kpi-label">平均分</span>
        </div>
        <div class="kpi-sep" />
        <div class="kpi">
          <span class="kpi-num" :style="{ color: (summary?.openCorrectives ?? 0) > 0 ? '#b45309' : '#0f766e' }">
            {{ summary?.openCorrectives ?? 0 }}
          </span>
          <span class="kpi-label">待整改</span>
        </div>
        <div class="kpi-sep" />
        <div class="kpi">
          <span class="kpi-num" :style="{ color: (summary?.overdueCorrectives ?? 0) > 0 ? '#b91c1c' : '#374151' }">
            {{ summary?.overdueCorrectives ?? 0 }}
          </span>
          <span class="kpi-label">已逾期</span>
        </div>
      </div>
    </header>

    <div class="rcv-toolbar">
      <span>近</span>
      <el-select v-model="days" size="small" style="width:100px" @change="reload">
        <el-option label="7 天" :value="7" />
        <el-option label="30 天" :value="30" />
        <el-option label="90 天" :value="90" />
      </el-select>
      <span class="rcv-spacer" />
      <span class="rcv-meta" v-if="summary">
        覆盖组织 {{ summary.orgUnitCount }} 个
      </span>
      <el-button size="small" @click="reload">刷新</el-button>
    </div>

    <div v-loading="loading" class="rcv-list">
      <div v-if="!loading && inspections.length === 0" class="rcv-empty">
        最近 {{ days }} 天暂无检查记录
      </div>
      <article
        v-for="r in inspections" :key="r.submissionId"
        class="rcv-row"
        :class="{ 'has-issue': r.issueCount > 0 }"
      >
        <div class="rcv-row-date">
          <div class="rcv-num">{{ fmtDate(r.inspectedAt) }}</div>
          <div class="rcv-time">{{ fmtTime(r.inspectedAt) }}</div>
        </div>
        <div class="rcv-row-meta">
          <div class="rcv-row-proj">{{ r.projectName }}</div>
          <div class="rcv-row-sub">
            <span class="rcv-target">{{ r.subjectName }}</span>
            <span class="rcv-task">· {{ r.taskCode }}</span>
          </div>
        </div>
        <div class="rcv-row-score">
          <span class="rcv-num"
                :class="scoreClass(r)">{{ r.score?.toFixed(1) ?? '-' }}</span>
          <span class="rcv-pct" v-if="r.maxScore > 0">/ {{ r.maxScore }}</span>
        </div>
        <div class="rcv-row-issues">
          <span v-if="r.issueCount > 0" class="rcv-issue-chip">
            {{ r.issueCount }} 项问题
          </span>
          <span v-else class="rcv-clean">无问题</span>
        </div>
        <div class="rcv-row-status">
          <span class="rcv-status" :class="`s-${r.submissionStatus.toLowerCase()}`">
            {{ statusLabel(r.submissionStatus) }}
          </span>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getMySummary, getMyInspections,
  type ReceivedSummary, type ReceivedInspection,
} from '@/api/inspection/received'

const loading = ref(false)
const days = ref(30)
const summary = ref<ReceivedSummary | null>(null)
const inspections = ref<ReceivedInspection[]>([])

async function reload() {
  loading.value = true
  try {
    const [s, list] = await Promise.all([
      getMySummary(days.value),
      getMyInspections({ days: days.value }),
    ])
    summary.value = s
    inspections.value = list
  } catch (e: unknown) {
    console.error('加载失败', e)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function fmtDate(s: string): string {
  if (!s) return '-'
  const d = new Date(s)
  return `${d.getMonth() + 1}/${d.getDate()}`
}
function fmtTime(s: string): string {
  if (!s) return ''
  const d = new Date(s)
  return d.getHours().toString().padStart(2, '0') + ':' + d.getMinutes().toString().padStart(2, '0')
}
function scoreClass(r: ReceivedInspection): string {
  if (!r.maxScore) return ''
  const pct = r.score / r.maxScore
  if (pct >= 0.85) return 'sc-good'
  if (pct >= 0.6) return 'sc-mid'
  return 'sc-bad'
}
function statusLabel(s: string): string {
  const map: Record<string, string> = {
    PENDING: '待检', IN_PROGRESS: '检查中',
    COMPLETED: '已完成', SUBMITTED: '已提交',
    PUBLISHED: '已发布', SKIPPED: '跳过',
  }
  return map[s] || s
}

onMounted(reload)
</script>

<style scoped>
.rcv-page { padding: 18px 24px; max-width: 1500px; margin: 0 auto; }
.rcv-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px; margin-bottom: 14px;
  background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
}
.rcv-eyebrow { font-size: 11px; color: #9ca3af; letter-spacing: 0.05em; }
.rcv-title { font-size: 22px; font-weight: 700; color: #111827; margin: 4px 0 0; }
.rcv-kpi { display: flex; align-items: center; gap: 18px; }
.kpi { text-align: center; }
.kpi-num {
  display: block; font-size: 22px; font-weight: 700; color: #1f2937;
  font-family: ui-monospace, monospace;
}
.kpi-label { font-size: 11px; color: #6b7280; }
.kpi-sep { width: 1px; height: 28px; background: #e5e7eb; }

.rcv-toolbar {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 0; font-size: 13px; color: #4b5563;
}
.rcv-spacer { flex: 1; }
.rcv-meta { font-size: 12px; color: #9ca3af; }

.rcv-list { display: flex; flex-direction: column; gap: 6px; }
.rcv-empty {
  padding: 36px; text-align: center; color: #9ca3af;
  background: #fff; border: 1px dashed #e5e7eb; border-radius: 8px;
}

.rcv-row {
  display: grid;
  grid-template-columns: 80px 1fr 90px 120px 80px;
  gap: 12px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  align-items: center;
  font-size: 13px;
}
.rcv-row.has-issue {
  border-left: 3px solid #f59e0b;
  background: #fffbeb;
}
.rcv-row-date { text-align: right; }
.rcv-num { font-family: ui-monospace, monospace; font-weight: 600; color: #1f2937; font-size: 14px; }
.rcv-time { font-size: 11px; color: #9ca3af; }
.rcv-row-proj { color: #1f2937; font-weight: 500; }
.rcv-row-sub { font-size: 12px; color: #6b7280; margin-top: 2px; }
.rcv-target { color: #4b5563; }
.rcv-task { margin-left: 6px; font-family: ui-monospace, monospace; font-size: 11px; color: #9ca3af; }
.rcv-row-score { text-align: right; }
.rcv-pct { font-size: 11px; color: #9ca3af; margin-left: 2px; }
.sc-good { color: #047857; }
.sc-mid  { color: #b45309; }
.sc-bad  { color: #b91c1c; }
.rcv-issue-chip {
  display: inline-block;
  padding: 2px 8px; border-radius: 4px;
  background: #fef3c7; color: #92400e;
  font-size: 11px; font-weight: 500;
}
.rcv-clean { font-size: 11px; color: #9ca3af; }
.rcv-status {
  display: inline-block;
  padding: 1px 8px; border-radius: 3px;
  font-size: 11px; font-weight: 500;
}
.s-completed, .s-published { background: #dcfce7; color: #15803d; }
.s-submitted { background: #dbeafe; color: #1e40af; }
.s-pending, .s-in_progress { background: #fef3c7; color: #92400e; }
</style>
