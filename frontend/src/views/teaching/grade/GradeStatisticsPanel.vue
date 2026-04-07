<template>
  <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; overflow: hidden;">
    <!-- Header with tabs -->
    <div style="display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #f3f4f6; padding: 10px 20px;">
      <div class="tm-tabs" style="border: none; padding: 0; background: none;">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="['tm-tab', { active: activeTab === tab.key }]"
          style="padding: 6px 0;"
          @click="activeTab = tab.key"
        >{{ tab.label }}</button>
      </div>
      <button
        class="tm-btn tm-btn-secondary"
        style="padding: 5px 12px; font-size: 12px;"
        :disabled="!semesterId || exporting"
        @click="doExport"
      >{{ exporting ? '导出中...' : '导出Excel' }}</button>
    </div>

    <!-- Loading -->
    <div v-if="loading" style="text-align: center; padding: 50px 0; color: #9ca3af; font-size: 13px;">
      <span class="tm-spin" style="display:inline-block;width:20px;height:20px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...
    </div>

    <!-- Statistics Summary Tab -->
    <template v-else-if="activeTab === 'summary'">
      <template v-if="statistics">
        <!-- Stats cards -->
        <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; padding: 16px 20px 0;">
          <div class="stat-card">
            <div class="stat-label">总人数</div>
            <div class="stat-value">{{ statistics.totalCount }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">及格人数</div>
            <div class="stat-value">{{ statistics.passCount }} <span class="stat-pct">({{ (statistics.passRate * 100).toFixed(1) }}%)</span></div>
          </div>
          <div class="stat-card">
            <div class="stat-label">优秀人数</div>
            <div class="stat-value">{{ statistics.excellentCount }} <span class="stat-pct">({{ (statistics.excellentRate * 100).toFixed(1) }}%)</span></div>
          </div>
          <div class="stat-card">
            <div class="stat-label">平均分</div>
            <div class="stat-value">{{ statistics.averageScore?.toFixed(1) }}</div>
          </div>
        </div>

        <!-- Distribution & Overview -->
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 16px 20px;">
          <!-- Distribution -->
          <div style="border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;">
            <div style="border-bottom: 1px solid #f3f4f6; padding: 10px 16px; font-size: 13px; font-weight: 600; color: #111827;">成绩分布</div>
            <div style="padding: 12px 16px;">
              <div v-for="item in statistics.distribution" :key="item.range" style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
                <span style="width: 50px; font-size: 11px; color: #6b7280;">{{ item.range }}</span>
                <div style="flex: 1; height: 18px; background: #f3f4f6; border-radius: 4px; overflow: hidden;">
                  <div style="height: 100%; background: #3b82f6; border-radius: 4px; transition: width 0.3s;" :style="{ width: `${item.percentage}%` }" />
                </div>
                <span style="width: 80px; text-align: right; font-size: 11px; color: #6b7280;">{{ item.count }}人 ({{ item.percentage.toFixed(1) }}%)</span>
              </div>
            </div>
          </div>

          <!-- Overview -->
          <div style="border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;">
            <div style="border-bottom: 1px solid #f3f4f6; padding: 10px 16px; font-size: 13px; font-weight: 600; color: #111827;">成绩概览</div>
            <div style="padding: 12px 16px;">
              <table class="tm-table" style="border: none; border-radius: 0;">
                <tbody>
                  <tr><td class="text-left" style="font-size: 12px; color: #6b7280;">最高分</td><td class="tm-mono" style="font-weight: 600;">{{ statistics.maxScore }}</td></tr>
                  <tr><td class="text-left" style="font-size: 12px; color: #6b7280;">最低分</td><td class="tm-mono" style="font-weight: 600;">{{ statistics.minScore }}</td></tr>
                  <tr><td class="text-left" style="font-size: 12px; color: #6b7280;">及格率</td><td class="tm-mono" style="font-weight: 600;">{{ (statistics.passRate * 100).toFixed(1) }}%</td></tr>
                  <tr><td class="text-left" style="font-size: 12px; color: #6b7280;">优秀率</td><td class="tm-mono" style="font-weight: 600;">{{ (statistics.excellentRate * 100).toFixed(1) }}%</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>
      <div v-else style="text-align: center; padding: 50px 0; color: #9ca3af; font-size: 13px;">暂无统计数据</div>
    </template>

    <!-- Ranking Tab -->
    <template v-else-if="activeTab === 'ranking'">
      <div v-if="rankingLoading" style="text-align: center; padding: 50px 0; color: #9ca3af; font-size: 13px;">
        <span class="tm-spin" style="display:inline-block;width:20px;height:20px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...
      </div>
      <div v-else-if="ranking.length > 0" style="padding: 12px 20px 16px;">
        <table class="tm-table">
          <colgroup>
            <col style="width: 70px" />
            <col />
            <col style="width: 100px" />
          </colgroup>
          <thead>
            <tr>
              <th>排名</th>
              <th class="text-left">姓名</th>
              <th>总分</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in ranking" :key="row.studentId">
              <td class="tm-mono" style="font-weight: 600;">{{ row.rank }}</td>
              <td class="text-left">{{ row.studentName }}</td>
              <td class="tm-mono" style="font-weight: 600;">{{ row.totalScore }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else style="text-align: center; padding: 50px 0; color: #9ca3af; font-size: 13px;">暂无排名数据</div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { gradeApi } from '@/api/teaching'
import type { GradeStatistics } from '@/types/teaching'

const props = defineProps<{
  semesterId?: number | string | null
  batchId?: number | string | null
}>()

const tabs = [
  { key: 'summary', label: '统计概览' },
  { key: 'ranking', label: '班级排名' },
] as const

type TabKey = (typeof tabs)[number]['key']
const activeTab = ref<TabKey>('summary')

const loading = ref(false)
const rankingLoading = ref(false)
const exporting = ref(false)
const statistics = ref<GradeStatistics>()
const ranking = ref<{ studentId: number | string; studentName: string; totalScore: number; rank: number }[]>([])

watch(() => [props.semesterId, props.batchId], () => {
  if (props.semesterId || props.batchId) loadStatistics()
  else statistics.value = undefined
}, { immediate: true })

watch(activeTab, (tab) => { if (tab === 'ranking' && props.semesterId) loadRanking() })

async function loadStatistics() {
  loading.value = true; statistics.value = undefined
  try {
    const params: Record<string, any> = {}
    if (props.batchId) params.batchId = props.batchId
    if (props.semesterId) params.semesterId = props.semesterId
    const res: any = await gradeApi.getStatistics(params)
    statistics.value = res.data || res
  } catch { statistics.value = undefined } finally { loading.value = false }
}

async function loadRanking() {
  if (!props.semesterId) return
  rankingLoading.value = true; ranking.value = []
  try {
    const res: any = await gradeApi.getRanking({ orgUnitId: 0, semesterId: props.semesterId })
    ranking.value = res.data || res || []
  } catch { ranking.value = [] } finally { rankingLoading.value = false }
}

async function doExport() {
  if (!props.semesterId) { ElMessage.warning('请先选择学期'); return }
  exporting.value = true
  try {
    const res: any = props.batchId ? await gradeApi.exportGrades(props.batchId) : await gradeApi.exportGradesByFilter({ semesterId: props.semesterId })
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = '成绩导出.xlsx'; document.body.appendChild(a); a.click(); document.body.removeChild(a); window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch { ElMessage.error('导出失败') } finally { exporting.value = false }
}
</script>

<style scoped>
.stat-card { border: 1px solid #e5e7eb; border-radius: 8px; background: #f9fafb; padding: 12px 16px; }
.stat-label { font-size: 11px; color: #6b7280; }
.stat-value { margin-top: 4px; font-size: 20px; font-weight: 600; color: #111827; }
.stat-pct { font-size: 11px; font-weight: 400; color: #9ca3af; margin-left: 4px; }
</style>

<style>
@import '@/styles/teaching-ui.css';
</style>
