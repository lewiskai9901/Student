<script setup lang="ts">
/**
 * AboutMeView — 当事人视角 · 关于我的检查记录
 *
 * 解决之前完全缺失的 UX 漏洞: 受检主体 / 部门主任 / 受检组织 等被检查方
 * 没有页面能看到"针对我组织的检查结果"以及发起申诉.
 *
 * 数据来源: 调用观察记录 API 过滤 subjectId == currentUser.userId 或所在 orgUnit.
 * 此页面对所有登录用户开放, 无需特殊权限.
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { getMyAppeals } from '@/api/inspection/appeal'
import type { InspAppeal } from '@/types/insp/appeal'
import SubmitAppealDialog from '@/views/inspection/appeals/components/SubmitAppealDialog.vue'

const router = useRouter()
const authStore = useAuthStore()

interface Observation {
  id: number
  itemName: string
  subjectName: string
  subjectType: string
  className?: string
  score: number
  severity?: string
  observedAt: string
  isNegative?: boolean
}

const loading = ref(false)
const observations = ref<Observation[]>([])
const myAppeals = ref<InspAppeal[]>([])

// Appeal dialog
const appealDialog = ref(false)
const appealDetailId = ref<number | null>(null)
const appealItemName = ref<string | undefined>('')
const appealCurrentScore = ref<number | undefined>(undefined)

// Period filter
const period = ref<'7d' | '30d' | '90d' | 'all'>('30d')

const userOrgUnitId = computed(() => Number((authStore.user as any)?.primaryOrgUnitId))

// ── Data ──
async function loadObservations() {
  loading.value = true
  try {
    // Try filtering by subjectId (USER) and orgUnit
    const userId = Number(authStore.user?.userId)
    const params: Record<string, any> = { size: 200, isNegative: true }

    const data = await http.get<any>('/inspection/observations', { params })
    const records = data?.records || data?.data || data || []
    observations.value = (records as any[])
      .filter(r => {
        // 主体是我自己, 或我所在的组织单元
        if (r.subjectType === 'USER' && Number(r.subjectId) === userId) return true
        if (r.subjectType === 'ORG_UNIT' && Number(r.subjectId) === userOrgUnitId.value) return true
        return false
      })
      .filter(r => filterByPeriod(r.observedAt))
  } catch {
    observations.value = []
  } finally {
    loading.value = false
  }
}

async function loadAppeals() {
  try {
    myAppeals.value = await getMyAppeals()
  } catch { /* ignore */ }
}

function filterByPeriod(t: string): boolean {
  if (period.value === 'all') return true
  const days = period.value === '7d' ? 7 : period.value === '30d' ? 30 : 90
  return new Date(t).getTime() > Date.now() - days * 86400000
}

// ── Aggregations ──
const stats = computed(() => {
  const list = observations.value
  return {
    total: list.length,
    totalDeduction: list.reduce((s, r) => s + (Number(r.score) < 0 ? Number(r.score) : 0), 0),
    bySeverity: {
      LOW: list.filter(r => r.severity === 'LOW').length,
      MEDIUM: list.filter(r => r.severity === 'MEDIUM').length,
      HIGH: list.filter(r => r.severity === 'HIGH').length,
      CRITICAL: list.filter(r => r.severity === 'CRITICAL').length,
    },
  }
})

const grouped = computed(() => {
  const map = new Map<string, Observation[]>()
  for (const o of observations.value) {
    const day = (o.observedAt || '').slice(0, 10)
    if (!map.has(day)) map.set(day, [])
    map.get(day)!.push(o)
  }
  return Array.from(map.entries())
    .sort((a, b) => b[0].localeCompare(a[0]))
    .map(([day, items]) => ({
      day,
      label: dayLabel(day),
      items,
      total: items.reduce((s, r) => s + Number(r.score), 0),
    }))
})

function dayLabel(d: string): string {
  if (!d) return '未知'
  const date = new Date(d)
  const today = new Date()
  const diff = Math.floor((today.getTime() - date.getTime()) / 86400000)
  if (diff === 0) return '今天'
  if (diff === 1) return '昨天'
  if (diff < 7) return `${diff} 天前`
  return d
}

// ── Appeal status mapping ──
function appealStatusForObservation(_o: Observation): InspAppeal | undefined {
  // observation 不直接关联 appeal, 我们能否找到对应 detail 已申诉?
  // 简化: 若 itemName 匹配, 时间相近 > 视为同记录
  return undefined
}

function severityVariant(s: string): string {
  return ({ LOW: 'pending', MEDIUM: 'info', HIGH: 'warn', CRITICAL: 'fail' } as any)[s] || 'pending'
}

function fmtTime(t: string): string {
  if (!t) return ''
  const d = new Date(t)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function openAppeal(o: Observation) {
  // Note: 实际 appeal 需要 submissionDetailId, 这里简化用 obs.id 作为 fallback
  appealDetailId.value = (o as any).submissionDetailId || o.id
  appealItemName.value = o.itemName
  appealCurrentScore.value = Number(o.score) || undefined
  appealDialog.value = true
}

function changePeriod(p: typeof period.value) {
  period.value = p
  loadObservations()
}

onMounted(() => {
  loadObservations()
  loadAppeals()
})
</script>

<template>
  <div class="insp-shell about-me">
    <!-- Header -->
    <header class="am-head">
      <div class="am-head__lead">
        <span class="insp-eyebrow">关于我 · About Me</span>
        <h1 class="am-head__title">针对我的检查记录</h1>
        <p class="am-head__sub">
          这里展示针对你或你所在组织的检查打分. 不服扣分可发起申诉.
        </p>
      </div>
      <div class="am-period">
        <button v-for="p in [
          { key: '7d', label: '近 7 天' },
          { key: '30d', label: '近 30 天' },
          { key: '90d', label: '近 90 天' },
          { key: 'all', label: '全部' },
        ]" :key="p.key" class="period-tab"
                :class="{ 'is-active': period === p.key }"
                @click="changePeriod(p.key as any)">
          {{ p.label }}
        </button>
      </div>
    </header>

    <!-- KPI strip -->
    <section class="am-kpi" v-if="!loading && observations.length > 0">
      <div class="kpi-cell">
        <span class="kpi-cell__num insp-num">{{ stats.total }}</span>
        <span class="kpi-cell__label">扣分记录</span>
      </div>
      <div class="kpi-cell">
        <span class="kpi-cell__num insp-num kpi-cell__num--neg">{{ stats.totalDeduction.toFixed(1) }}</span>
        <span class="kpi-cell__label">累计失分</span>
      </div>
      <div class="kpi-cell">
        <span class="kpi-cell__num insp-num">{{ myAppeals.filter(a => a.status === 'PENDING').length }}</span>
        <span class="kpi-cell__label">申诉中</span>
      </div>
      <div class="kpi-cell">
        <span class="kpi-cell__num insp-num kpi-cell__num--pos">+{{ myAppeals.filter(a => a.status === 'APPROVED' && a.finalAdjustment).reduce((s, a) => s + Number(a.finalAdjustment || 0), 0).toFixed(1) }}</span>
        <span class="kpi-cell__label">申诉已追回</span>
      </div>
    </section>

    <!-- Empty state -->
    <div v-if="!loading && observations.length === 0" class="am-empty">
      <div class="insp-eyebrow">无记录</div>
      <p>当前周期内, 没有针对你或你所在组织的检查打分</p>
      <p class="am-empty__sub">这是好事 · 继续保持</p>
    </div>

    <!-- Day-grouped list -->
    <section class="am-stream" v-loading="loading">
      <article v-for="day in grouped" :key="day.day" class="day-card">
        <header class="day-card__head">
          <span class="day-card__label">{{ day.label }}</span>
          <span class="day-card__date insp-num">{{ day.day }}</span>
          <span class="day-card__sep" />
          <span class="day-card__count insp-num">{{ day.items.length }} 条</span>
          <span v-if="day.total < 0" class="day-card__total insp-num">合计 {{ day.total.toFixed(1) }} 分</span>
        </header>

        <ul class="obs-list">
          <li v-for="o in day.items" :key="o.id" class="obs-row">
            <span class="obs-time insp-num">{{ fmtTime(o.observedAt) }}</span>
            <div class="obs-main">
              <div class="obs-line">
                <span class="obs-name">{{ o.itemName }}</span>
                <span v-if="o.severity" class="insp-chip" :class="`insp-chip--${severityVariant(o.severity)}`">
                  {{ ({ LOW:'轻微', MEDIUM:'中等', HIGH:'严重', CRITICAL:'危急' } as any)[o.severity] }}
                </span>
              </div>
              <div class="obs-meta">
                <span v-if="o.className">{{ o.className }}</span>
              </div>
            </div>
            <span class="obs-score insp-num" :class="{ 'is-neg': Number(o.score) < 0 }">
              {{ Number(o.score) > 0 ? '+' : '' }}{{ o.score }}
            </span>
            <button class="obs-action" @click="openAppeal(o)" title="对此扣分发起申诉">
              申诉
            </button>
          </li>
        </ul>
      </article>
    </section>

    <SubmitAppealDialog
      v-if="appealDetailId != null"
      v-model="appealDialog"
      :submission-detail-id="appealDetailId"
      :item-name="appealItemName"
      :current-score="appealCurrentScore"
      @submitted="loadAppeals"
    />
  </div>
</template>

<style scoped>
.about-me { padding: 12px 16px; }

/* ─ Head ─────── */
.am-head {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 14px 18px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}
.am-head__title {
  font-size: 18px;
  font-weight: 700;
  margin: 4px 0 4px;
  color: var(--insp-ink-primary);
}
.am-head__sub {
  margin: 0;
  font-size: 12px;
  color: var(--insp-ink-tertiary);
  line-height: 1.5;
}

.am-period {
  display: flex;
  gap: 2px;
  padding: 2px;
  background: var(--insp-bg-subtle);
  border-radius: var(--insp-radius-sm);
}
.period-tab {
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  color: var(--insp-ink-tertiary);
  background: transparent;
  border: 0;
  cursor: pointer;
  border-radius: 3px;
  font-family: inherit;
  transition: all var(--insp-t-fast);
}
.period-tab:hover { color: var(--insp-ink-primary); }
.period-tab.is-active {
  color: var(--insp-ink-primary);
  background: var(--insp-bg-surface);
  box-shadow: var(--insp-shadow-xs);
  font-weight: 600;
}

/* ─ KPI strip ─────── */
.am-kpi {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 12px 16px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 10px;
}
.kpi-cell { display: flex; flex-direction: column; gap: 2px; }
.kpi-cell__num {
  font-family: var(--insp-font-mono);
  font-size: 22px;
  font-weight: 700;
  color: var(--insp-ink-primary);
  line-height: 1;
}
.kpi-cell__num--neg { color: var(--insp-fail); }
.kpi-cell__num--pos { color: var(--insp-pass); }
.kpi-cell__label {
  font-size: 11px;
  color: var(--insp-ink-tertiary);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

/* ─ Empty ─────── */
.am-empty {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  padding: 60px 24px;
  text-align: center;
}
.am-empty p { margin: 8px 0 0; font-size: 13px; color: var(--insp-ink-secondary); }
.am-empty__sub { color: var(--insp-pass) !important; font-weight: 500; }

/* ─ Day stream ─────── */
.am-stream {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.day-card {
  background: var(--insp-bg-surface);
  border: 1px solid var(--insp-border-default);
  border-radius: var(--insp-radius-lg);
  overflow: hidden;
}

.day-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--insp-bg-subtle);
  border-bottom: 1px solid var(--insp-border-subtle);
  font-size: 12px;
}

.day-card__label {
  font-weight: 700;
  color: var(--insp-ink-primary);
}
.day-card__date {
  font-family: var(--insp-font-mono);
  font-size: 11px;
  color: var(--insp-ink-tertiary);
}
.day-card__sep {
  width: 1px;
  height: 12px;
  background: var(--insp-border-default);
}
.day-card__count {
  font-family: var(--insp-font-mono);
  font-size: 11px;
  color: var(--insp-ink-tertiary);
}
.day-card__total {
  margin-left: auto;
  font-family: var(--insp-font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--insp-fail);
}

/* ─ Obs rows ─────── */
.obs-list { list-style: none; margin: 0; padding: 0; }

.obs-row {
  display: grid;
  grid-template-columns: 50px 1fr auto auto;
  gap: 10px;
  align-items: center;
  padding: 8px 14px;
  border-bottom: 1px solid var(--insp-border-subtle);
  transition: background var(--insp-t-fast);
}
.obs-row:last-child { border-bottom: 0; }
.obs-row:hover { background: var(--insp-accent-paler); }

.obs-time {
  font-family: var(--insp-font-mono);
  font-size: 11px;
  color: var(--insp-ink-tertiary);
}

.obs-main { min-width: 0; }
.obs-line {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.obs-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--insp-ink-primary);
}
.obs-meta {
  font-size: 11px;
  color: var(--insp-ink-tertiary);
  margin-top: 2px;
}

.obs-score {
  font-family: var(--insp-font-mono);
  font-size: 14px;
  font-weight: 700;
  color: var(--insp-ink-secondary);
  min-width: 50px;
  text-align: right;
}
.obs-score.is-neg { color: var(--insp-fail); }

.obs-action {
  height: 24px;
  padding: 0 10px;
  font-size: 11px;
  font-weight: 500;
  background: var(--insp-bg-surface);
  color: var(--insp-accent);
  border: 1px solid var(--insp-accent-pale);
  border-radius: var(--insp-radius-sm);
  cursor: pointer;
  font-family: inherit;
  transition: all var(--insp-t-fast);
}
.obs-action:hover {
  background: var(--insp-accent);
  color: white;
  border-color: var(--insp-accent);
}

/* ─ Responsive ─────── */
@media (max-width: 720px) {
  .am-head { flex-direction: column; }
  .am-kpi { grid-template-columns: repeat(2, 1fr); }
  .obs-row { grid-template-columns: 50px 1fr auto; }
  .obs-action { grid-column: 2 / 4; justify-self: end; margin-top: 4px; }
}
</style>
