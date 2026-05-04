<template>
  <div class="trd-page">
    <header class="trd-head">
      <div>
        <div class="rcv-eyebrow">受检主体 · Trends</div>
        <h1 class="rcv-title">检查趋势</h1>
        <p class="trd-sub">过去 {{ weeks }} 周的检查得分率与问题数变化</p>
      </div>
      <div class="trd-toolbar">
        <span>时间范围:</span>
        <el-radio-group v-model="weeks" size="small" @change="reload">
          <el-radio-button :value="4">4 周</el-radio-button>
          <el-radio-button :value="12">12 周</el-radio-button>
          <el-radio-button :value="26">26 周</el-radio-button>
        </el-radio-group>
      </div>
    </header>

    <div v-loading="loading" class="trd-body">
      <div v-if="!loading && points.length === 0" class="rcv-empty">
        过去 {{ weeks }} 周暂无检查数据
      </div>

      <!-- 简易柱状图 -->
      <div v-else class="chart-wrap">
        <h3 class="chart-title">每周得分率</h3>
        <div class="chart-bars">
          <div
            v-for="p in points" :key="p.isoWeek"
            class="bar-col"
            :title="`${p.weekStart}\n得分率 ${p.avgPct ?? '-'}%\n检查 ${p.submissionCount} 次\n问题 ${p.totalIssues} 个`"
          >
            <div class="bar-track">
              <div
                class="bar-fill"
                :class="barClass(p.avgPct)"
                :style="{ height: barHeight(p.avgPct) }"
              />
            </div>
            <div class="bar-pct">{{ p.avgPct != null ? p.avgPct + '%' : '-' }}</div>
            <div class="bar-week">{{ shortWeek(p.weekStart) }}</div>
          </div>
        </div>

        <h3 class="chart-title">每周问题数</h3>
        <div class="chart-bars">
          <div
            v-for="p in points" :key="`i-${p.isoWeek}`"
            class="bar-col"
            :title="`${p.weekStart}: ${p.totalIssues} 个问题`"
          >
            <div class="bar-track">
              <div
                class="bar-fill bar-issue"
                :style="{ height: issueHeight(p.totalIssues) }"
              />
            </div>
            <div class="bar-pct">{{ p.totalIssues }}</div>
            <div class="bar-week">{{ shortWeek(p.weekStart) }}</div>
          </div>
        </div>

        <!-- 数据表 -->
        <h3 class="chart-title">明细</h3>
        <table class="trd-table">
          <thead>
            <tr>
              <th>周</th>
              <th>检查次数</th>
              <th>平均分</th>
              <th>得分率</th>
              <th>问题数</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in [...points].reverse()" :key="`t-${p.isoWeek}`">
              <td>{{ p.weekStart }}</td>
              <td class="num">{{ p.submissionCount }}</td>
              <td class="num">{{ p.avgScore ?? '-' }}</td>
              <td class="num" :class="barClass(p.avgPct)">{{ p.avgPct != null ? p.avgPct + '%' : '-' }}</td>
              <td class="num" :class="{ 'has-issue': p.totalIssues > 0 }">{{ p.totalIssues }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyTrends, type ReceivedTrendPoint } from '@/api/inspection/received'

const loading = ref(false)
const weeks = ref(4)
const points = ref<ReceivedTrendPoint[]>([])

const maxIssues = computed(() => Math.max(1, ...points.value.map(p => p.totalIssues || 0)))

async function reload() {
  loading.value = true
  try {
    points.value = await getMyTrends(weeks.value)
  } catch (e: unknown) {
    console.error(e); ElMessage.error('加载趋势失败')
  } finally {
    loading.value = false
  }
}

function shortWeek(s: string): string {
  if (!s) return ''
  const d = new Date(s)
  return `${d.getMonth() + 1}/${d.getDate()}`
}
function barHeight(pct: number | null): string {
  if (pct == null) return '0%'
  return Math.max(6, Math.min(100, pct)) + '%'
}
function barClass(pct: number | null): string {
  if (pct == null) return ''
  if (pct >= 85) return 'sc-good'
  if (pct >= 60) return 'sc-mid'
  return 'sc-bad'
}
function issueHeight(n: number): string {
  if (!n) return '4%'
  return Math.max(8, Math.min(100, (n / maxIssues.value) * 100)) + '%'
}

onMounted(reload)
</script>

<style scoped>
.trd-page { padding: 18px 24px; max-width: 1500px; margin: 0 auto; }
.trd-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px; margin-bottom: 16px;
  background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
}
.rcv-eyebrow { font-size: 11px; color: #9ca3af; letter-spacing: 0.05em; }
.rcv-title { font-size: 22px; font-weight: 700; color: #111827; margin: 4px 0 0; }
.trd-sub { color: #6b7280; font-size: 13px; margin: 4px 0 0; }
.trd-toolbar { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #6b7280; }
.rcv-empty { padding: 60px; text-align: center; color: #9ca3af; }

.chart-wrap {
  background: #fff; border: 1px solid #e5e7eb; border-radius: 8px;
  padding: 18px 24px;
}
.chart-title {
  font-size: 13px; color: #374151; font-weight: 600;
  margin: 14px 0 10px;
}
.chart-title:first-child { margin-top: 0; }

.chart-bars {
  display: flex; align-items: flex-end; gap: 4px;
  height: 180px;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 26px;
  position: relative;
}
.bar-col {
  flex: 1; min-width: 26px;
  display: flex; flex-direction: column;
  align-items: center; justify-content: flex-end;
  height: 100%;
  cursor: help;
}
.bar-track {
  width: 70%;
  height: 100%;
  display: flex; flex-direction: column; justify-content: flex-end;
  background: #f3f4f6; border-radius: 3px 3px 0 0;
}
.bar-fill {
  width: 100%; border-radius: 3px 3px 0 0;
  transition: filter 0.15s;
}
.bar-fill.sc-good { background: linear-gradient(180deg, #34d399, #047857); }
.bar-fill.sc-mid  { background: linear-gradient(180deg, #fbbf24, #b45309); }
.bar-fill.sc-bad  { background: linear-gradient(180deg, #f87171, #b91c1c); }
.bar-fill.bar-issue { background: linear-gradient(180deg, #93c5fd, #1d4ed8); }
.bar-col:hover .bar-fill { filter: brightness(1.05); }
.bar-pct { font-size: 11px; color: #1f2937; font-family: ui-monospace, monospace; margin-top: 4px; }
.bar-week { font-size: 10px; color: #9ca3af; margin-top: 1px; }

.trd-table {
  width: 100%; border-collapse: collapse; margin-top: 4px; font-size: 13px;
}
.trd-table th, .trd-table td {
  padding: 8px 12px; border: 1px solid #f3f4f6; text-align: left;
}
.trd-table th { background: #f9fafb; color: #6b7280; font-weight: 500; }
.trd-table td.num { font-family: ui-monospace, monospace; }
.trd-table td.sc-good { color: #047857; font-weight: 600; }
.trd-table td.sc-mid  { color: #b45309; }
.trd-table td.sc-bad  { color: #b91c1c; font-weight: 600; }
.trd-table td.has-issue { color: #b45309; font-weight: 600; }
</style>
