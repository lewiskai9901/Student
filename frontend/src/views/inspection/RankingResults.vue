<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Trophy, Medal, TrendingUp, TrendingDown, Minus, Download,
  FileSpreadsheet, Filter, ChevronDown, ChevronUp, ArrowUpRight,
  ArrowDownRight, Eye, Star, BarChart3
} from 'lucide-vue-next'

// ─── Types ───
interface ClassRanking {
  ranking: number
  classId: number
  className: string
  orgUnitName: string
  gradeName: string
  deductionTotal: number
  bonusTotal: number
  finalScore: number
  passRate: number
  scoreLevel: string
  trend: 'UP' | 'DOWN' | 'STABLE'
  trendChange: number
  categoryBreakdown: {
    hygiene: number
    discipline: number
    attendance: number
    dormitory: number
    other: number
  }
}

// ─── State ───
const selectedGrade = ref('')
const selectedDept = ref('')
const rankingDimension = ref('overall')
const scoreType = ref('final')
const expandedRow = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = 15

// ─── Mock Data ───
const summary = {
  planName: '2025-2026学年第二学期日常检查',
  checkDate: '2026-01-27',
  totalClasses: 42,
  avgDeduction: 12.3,
  excellentRate: 35.7,
  passRateAvg: 85.2,
  scoringMode: 'DUAL_TRACK' as const,
  baseScore: 100,
}

const rankings: ClassRanking[] = [
  { ranking: 1, classId: 3, className: '计算机3班', orgUnitName: '信息工程系', gradeName: '2024级', deductionTotal: 2.0, bonusTotal: 1.0, finalScore: 99.0, passRate: 98, scoreLevel: '优秀', trend: 'UP', trendChange: 2, categoryBreakdown: { hygiene: 0.5, discipline: 0.5, attendance: 0, dormitory: 1.0, other: 0 } },
  { ranking: 2, classId: 10, className: '电商1班', orgUnitName: '经济管理系', gradeName: '2024级', deductionTotal: 3.5, bonusTotal: 0, finalScore: 96.5, passRate: 95, scoreLevel: '优秀', trend: 'STABLE', trendChange: 0, categoryBreakdown: { hygiene: 1.5, discipline: 1.0, attendance: 0.5, dormitory: 0.5, other: 0 } },
  { ranking: 3, classId: 15, className: '会计2班', orgUnitName: '财经系', gradeName: '2024级', deductionTotal: 4.0, bonusTotal: 2.0, finalScore: 98.0, passRate: 94, scoreLevel: '优秀', trend: 'UP', trendChange: 3, categoryBreakdown: { hygiene: 2.0, discipline: 0.5, attendance: 0.5, dormitory: 0.5, other: 0.5 } },
  { ranking: 4, classId: 1, className: '计算机1班', orgUnitName: '信息工程系', gradeName: '2024级', deductionTotal: 5.5, bonusTotal: 1.0, finalScore: 95.5, passRate: 92, scoreLevel: '优秀', trend: 'UP', trendChange: 1, categoryBreakdown: { hygiene: 2.5, discipline: 1.0, attendance: 1.0, dormitory: 1.0, other: 0 } },
  { ranking: 5, classId: 2, className: '计算机2班', orgUnitName: '信息工程系', gradeName: '2024级', deductionTotal: 8.5, bonusTotal: 0.5, finalScore: 92.0, passRate: 88, scoreLevel: '良好', trend: 'UP', trendChange: 2, categoryBreakdown: { hygiene: 4.5, discipline: 2.0, attendance: 1.0, dormitory: 1.0, other: 0 } },
  { ranking: 6, classId: 11, className: '电商2班', orgUnitName: '经济管理系', gradeName: '2024级', deductionTotal: 9.0, bonusTotal: 0, finalScore: 91.0, passRate: 87, scoreLevel: '良好', trend: 'DOWN', trendChange: -1, categoryBreakdown: { hygiene: 3.0, discipline: 3.0, attendance: 1.5, dormitory: 1.0, other: 0.5 } },
  { ranking: 7, classId: 16, className: '会计1班', orgUnitName: '财经系', gradeName: '2024级', deductionTotal: 10.5, bonusTotal: 0, finalScore: 89.5, passRate: 85, scoreLevel: '良好', trend: 'STABLE', trendChange: 0, categoryBreakdown: { hygiene: 4.0, discipline: 2.5, attendance: 2.0, dormitory: 1.5, other: 0.5 } },
  { ranking: 8, classId: 4, className: '软件1班', orgUnitName: '信息工程系', gradeName: '2024级', deductionTotal: 11.0, bonusTotal: 0, finalScore: 89.0, passRate: 83, scoreLevel: '良好', trend: 'UP', trendChange: 1, categoryBreakdown: { hygiene: 3.5, discipline: 3.0, attendance: 2.5, dormitory: 1.5, other: 0.5 } },
  { ranking: 9, classId: 5, className: '软件2班', orgUnitName: '信息工程系', gradeName: '2024级', deductionTotal: 13.0, bonusTotal: 0.5, finalScore: 87.5, passRate: 80, scoreLevel: '良好', trend: 'DOWN', trendChange: -2, categoryBreakdown: { hygiene: 5.0, discipline: 3.0, attendance: 2.5, dormitory: 2.0, other: 0.5 } },
  { ranking: 10, classId: 20, className: '物流1班', orgUnitName: '经济管理系', gradeName: '2024级', deductionTotal: 15.0, bonusTotal: 0, finalScore: 85.0, passRate: 78, scoreLevel: '一般', trend: 'DOWN', trendChange: -3, categoryBreakdown: { hygiene: 5.5, discipline: 4.0, attendance: 2.5, dormitory: 2.0, other: 1.0 } },
  { ranking: 11, classId: 21, className: '物流2班', orgUnitName: '经济管理系', gradeName: '2024级', deductionTotal: 18.0, bonusTotal: 0, finalScore: 82.0, passRate: 72, scoreLevel: '一般', trend: 'STABLE', trendChange: 0, categoryBreakdown: { hygiene: 6.0, discipline: 5.0, attendance: 3.0, dormitory: 3.0, other: 1.0 } },
  { ranking: 12, classId: 22, className: '物流3班', orgUnitName: '经济管理系', gradeName: '2024级', deductionTotal: 28.5, bonusTotal: 0, finalScore: 71.5, passRate: 60, scoreLevel: '较差', trend: 'DOWN', trendChange: -5, categoryBreakdown: { hygiene: 10.0, discipline: 8.0, attendance: 5.0, dormitory: 4.0, other: 1.5 } },
]

// ─── Computed ───
const topThree = computed(() => {
  const sorted = [...rankings].sort((a, b) => a.ranking - b.ranking)
  return {
    second: sorted[1],
    first: sorted[0],
    third: sorted[2],
  }
})

const filteredRankings = computed(() => {
  let result = [...rankings]
  if (selectedGrade.value) result = result.filter(r => r.gradeName === selectedGrade.value)
  if (selectedDept.value) result = result.filter(r => r.orgUnitName === selectedDept.value)
  return result
})

const paginatedRankings = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredRankings.value.slice(start, start + pageSize)
})

// ─── Helpers ───
function getLevelStyle(level: string) {
  const map: Record<string, string> = {
    '优秀': 'bg-emerald-100 text-emerald-700 border border-emerald-200',
    '良好': 'bg-blue-100 text-blue-700 border border-blue-200',
    '一般': 'bg-amber-100 text-amber-700 border border-amber-200',
    '较差': 'bg-red-100 text-red-700 border border-red-200',
  }
  return map[level] || ''
}

function getTrendIcon(trend: string) {
  if (trend === 'UP') return ArrowUpRight
  if (trend === 'DOWN') return ArrowDownRight
  return Minus
}

function getTrendColor(trend: string) {
  if (trend === 'UP') return 'text-emerald-500'
  if (trend === 'DOWN') return 'text-red-500'
  return 'text-gray-400'
}

function getMedalEmoji(rank: number) {
  if (rank === 1) return '🥇'
  if (rank === 2) return '🥈'
  if (rank === 3) return '🥉'
  return ''
}

function toggleExpand(classId: number) {
  expandedRow.value = expandedRow.value === classId ? null : classId
}

function getPodiumHeight(rank: number) {
  if (rank === 1) return 'h-32'
  if (rank === 2) return 'h-24'
  return 'h-20'
}

const podiumGradients: Record<number, string> = {
  1: 'from-amber-400 via-yellow-300 to-amber-400',
  2: 'from-gray-300 via-gray-200 to-gray-300',
  3: 'from-orange-400 via-orange-300 to-orange-400',
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- Header -->
    <div class="mb-6 flex items-start justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">检查结果 - 排名总览</h1>
        <p class="mt-1 text-sm text-gray-500">
          {{ summary.planName }} | 检查日期: {{ summary.checkDate }}
        </p>
      </div>
      <div class="flex items-center gap-2">
        <button class="flex items-center gap-1.5 rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50">
          <FileSpreadsheet class="h-4 w-4" />
          导出Excel
        </button>
        <button class="flex items-center gap-1.5 rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50">
          <Download class="h-4 w-4" />
          导出PDF
        </button>
      </div>
    </div>

    <!-- Filters -->
    <div class="mb-6 flex flex-wrap items-center gap-3">
      <div class="flex items-center gap-2">
        <Filter class="h-4 w-4 text-gray-400" />
        <span class="text-sm text-gray-500">筛选:</span>
      </div>
      <select v-model="selectedGrade" class="h-9 rounded-lg border border-gray-300 px-3 text-sm text-gray-700 focus:border-blue-500 focus:outline-none">
        <option value="">全部年级</option>
        <option>2024级</option>
        <option>2023级</option>
      </select>
      <select v-model="selectedDept" class="h-9 rounded-lg border border-gray-300 px-3 text-sm text-gray-700 focus:border-blue-500 focus:outline-none">
        <option value="">全部系部</option>
        <option>信息工程系</option>
        <option>经济管理系</option>
        <option>财经系</option>
      </select>
      <select v-model="rankingDimension" class="h-9 rounded-lg border border-gray-300 px-3 text-sm text-gray-700 focus:border-blue-500 focus:outline-none">
        <option value="overall">总排名</option>
        <option value="grade">年级排名</option>
        <option value="dept">系部排名</option>
      </select>
      <select v-model="scoreType" class="h-9 rounded-lg border border-gray-300 px-3 text-sm text-gray-700 focus:border-blue-500 focus:outline-none">
        <option value="final">最终得分</option>
        <option value="original">原始扣分</option>
        <option value="weighted">加权得分</option>
      </select>
    </div>

    <!-- Top 3 Podium -->
    <div class="mb-8 flex items-end justify-center gap-4">
      <!-- 2nd place -->
      <div v-if="topThree.second" class="flex flex-col items-center">
        <div class="mb-3 w-48 rounded-xl border border-gray-200 bg-white p-4 text-center shadow-md transition-transform hover:-translate-y-1 hover:shadow-lg">
          <div class="mb-1 text-3xl">🥈</div>
          <h3 class="text-sm font-bold text-gray-900">{{ topThree.second.className }}</h3>
          <p class="text-xs text-gray-500">{{ topThree.second.orgUnitName }}</p>
          <div class="mt-2 text-2xl font-black text-gray-800">{{ topThree.second.finalScore }}</div>
          <p class="text-xs text-gray-400">扣分: -{{ topThree.second.deductionTotal }}</p>
          <span :class="['mt-2 inline-block rounded-full px-2.5 py-0.5 text-[10px] font-semibold', getLevelStyle(topThree.second.scoreLevel)]">
            {{ topThree.second.scoreLevel }}
          </span>
        </div>
        <div :class="['w-48 rounded-t-lg bg-gradient-to-r', podiumGradients[2], 'h-20 flex items-center justify-center']">
          <span class="text-3xl font-black text-white/80">2</span>
        </div>
      </div>

      <!-- 1st place -->
      <div v-if="topThree.first" class="flex flex-col items-center">
        <div class="mb-3 w-52 rounded-xl border-2 border-amber-200 bg-white p-5 text-center shadow-lg transition-transform hover:-translate-y-1 hover:shadow-xl">
          <div class="mb-1 text-4xl">🥇</div>
          <h3 class="text-base font-bold text-gray-900">{{ topThree.first.className }}</h3>
          <p class="text-xs text-gray-500">{{ topThree.first.orgUnitName }}</p>
          <div class="mt-2 text-3xl font-black text-amber-600">{{ topThree.first.finalScore }}</div>
          <p class="text-xs text-gray-400">扣分: -{{ topThree.first.deductionTotal }} | 加分: +{{ topThree.first.bonusTotal }}</p>
          <span :class="['mt-2 inline-block rounded-full px-3 py-1 text-xs font-semibold', getLevelStyle(topThree.first.scoreLevel)]">
            {{ topThree.first.scoreLevel }}
          </span>
        </div>
        <div :class="['w-52 rounded-t-lg bg-gradient-to-r', podiumGradients[1], 'h-28 flex items-center justify-center']">
          <span class="text-4xl font-black text-white/80">1</span>
        </div>
      </div>

      <!-- 3rd place -->
      <div v-if="topThree.third" class="flex flex-col items-center">
        <div class="mb-3 w-48 rounded-xl border border-gray-200 bg-white p-4 text-center shadow-md transition-transform hover:-translate-y-1 hover:shadow-lg">
          <div class="mb-1 text-3xl">🥉</div>
          <h3 class="text-sm font-bold text-gray-900">{{ topThree.third.className }}</h3>
          <p class="text-xs text-gray-500">{{ topThree.third.orgUnitName }}</p>
          <div class="mt-2 text-2xl font-black text-gray-800">{{ topThree.third.finalScore }}</div>
          <p class="text-xs text-gray-400">扣分: -{{ topThree.third.deductionTotal }} | 加分: +{{ topThree.third.bonusTotal }}</p>
          <span :class="['mt-2 inline-block rounded-full px-2.5 py-0.5 text-[10px] font-semibold', getLevelStyle(topThree.third.scoreLevel)]">
            {{ topThree.third.scoreLevel }}
          </span>
        </div>
        <div :class="['w-48 rounded-t-lg bg-gradient-to-r', podiumGradients[3], 'h-16 flex items-center justify-center']">
          <span class="text-3xl font-black text-white/80">3</span>
        </div>
      </div>
    </div>

    <!-- Summary stats bar -->
    <div class="mb-6 flex items-center justify-center gap-8 rounded-xl border border-gray-200 bg-white px-6 py-3 shadow-sm">
      <div class="text-center">
        <span class="text-xs text-gray-500">参与班级</span>
        <p class="text-lg font-bold text-gray-900">{{ summary.totalClasses }}</p>
      </div>
      <div class="h-8 w-px bg-gray-200"></div>
      <div class="text-center">
        <span class="text-xs text-gray-500">平均扣分</span>
        <p class="text-lg font-bold text-gray-900">{{ summary.avgDeduction }}</p>
      </div>
      <div class="h-8 w-px bg-gray-200"></div>
      <div class="text-center">
        <span class="text-xs text-gray-500">优秀率</span>
        <p class="text-lg font-bold text-emerald-600">{{ summary.excellentRate }}%</p>
      </div>
      <div class="h-8 w-px bg-gray-200"></div>
      <div class="text-center">
        <span class="text-xs text-gray-500">通过率</span>
        <p class="text-lg font-bold text-blue-600">{{ summary.passRateAvg }}%</p>
      </div>
    </div>

    <!-- Main ranking table -->
    <div class="rounded-xl border border-gray-200 bg-white shadow-sm">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-200 bg-gray-50/80">
              <th class="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">排名</th>
              <th class="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">班级</th>
              <th class="px-5 py-3.5 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">系部</th>
              <th class="px-5 py-3.5 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">扣分</th>
              <th v-if="summary.scoringMode === 'DUAL_TRACK'" class="px-5 py-3.5 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">加分</th>
              <th class="px-5 py-3.5 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">最终得分</th>
              <th class="px-5 py-3.5 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">通过率</th>
              <th class="px-5 py-3.5 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">评级</th>
              <th class="px-5 py-3.5 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">趋势</th>
              <th class="px-5 py-3.5 text-center text-xs font-semibold uppercase tracking-wider text-gray-500">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <template v-for="item in paginatedRankings" :key="item.classId">
              <tr
                :class="[
                  'cursor-pointer transition-colors hover:bg-gray-50',
                  item.ranking <= 3 ? 'bg-amber-50/30' : '',
                  expandedRow === item.classId ? 'bg-blue-50/50' : ''
                ]"
                @click="toggleExpand(item.classId)"
              >
                <td class="px-5 py-4">
                  <div class="flex items-center gap-1.5">
                    <span v-if="item.ranking <= 3" class="text-lg">{{ getMedalEmoji(item.ranking) }}</span>
                    <span v-else class="text-sm font-bold text-gray-400">{{ item.ranking }}</span>
                  </div>
                </td>
                <td class="px-5 py-4">
                  <span class="text-sm font-semibold text-gray-900">{{ item.className }}</span>
                  <span class="ml-1.5 text-xs text-gray-400">{{ item.gradeName }}</span>
                </td>
                <td class="px-5 py-4 text-sm text-gray-600">{{ item.orgUnitName }}</td>
                <td class="px-5 py-4 text-center text-sm font-semibold text-red-600">-{{ item.deductionTotal }}</td>
                <td v-if="summary.scoringMode === 'DUAL_TRACK'" class="px-5 py-4 text-center text-sm font-semibold text-emerald-600">
                  {{ item.bonusTotal > 0 ? `+${item.bonusTotal}` : '-' }}
                </td>
                <td class="px-5 py-4 text-center">
                  <span class="text-base font-bold text-gray-900">{{ item.finalScore }}</span>
                </td>
                <td class="px-5 py-4">
                  <div class="mx-auto flex w-20 items-center gap-1.5">
                    <div class="h-1.5 flex-1 overflow-hidden rounded-full bg-gray-200">
                      <div
                        class="h-full rounded-full transition-all"
                        :class="item.passRate >= 90 ? 'bg-emerald-500' : item.passRate >= 70 ? 'bg-blue-500' : 'bg-red-500'"
                        :style="{ width: `${item.passRate}%` }"
                      ></div>
                    </div>
                    <span class="text-xs text-gray-500">{{ item.passRate }}%</span>
                  </div>
                </td>
                <td class="px-5 py-4 text-center">
                  <span :class="['inline-block rounded-full px-2.5 py-1 text-[10px] font-bold', getLevelStyle(item.scoreLevel)]">
                    {{ item.scoreLevel }}
                  </span>
                </td>
                <td class="px-5 py-4 text-center">
                  <div class="flex items-center justify-center gap-1">
                    <component :is="getTrendIcon(item.trend)" :class="['h-4 w-4', getTrendColor(item.trend)]" />
                    <span v-if="item.trendChange !== 0" :class="['text-xs font-semibold', getTrendColor(item.trend)]">
                      {{ item.trendChange > 0 ? `+${item.trendChange}` : item.trendChange }}
                    </span>
                  </div>
                </td>
                <td class="px-5 py-4 text-center">
                  <button class="rounded-lg p-1.5 text-gray-400 hover:bg-blue-50 hover:text-blue-600" @click.stop>
                    <Eye class="h-4 w-4" />
                  </button>
                </td>
              </tr>
              <!-- Expanded row: Category breakdown -->
              <tr v-if="expandedRow === item.classId">
                <td :colspan="summary.scoringMode === 'DUAL_TRACK' ? 10 : 9" class="bg-gray-50/80 px-5 py-4">
                  <div class="flex items-center gap-6 pl-12">
                    <span class="text-xs font-semibold text-gray-500">扣分明细:</span>
                    <div class="flex gap-4">
                      <div v-for="(value, key) in item.categoryBreakdown" :key="key" class="flex items-center gap-1.5">
                        <span class="text-xs text-gray-500">{{ { hygiene: '卫生', discipline: '纪律', attendance: '考勤', dormitory: '宿舍', other: '其他' }[key] }}:</span>
                        <span :class="['text-xs font-semibold', value > 0 ? 'text-red-600' : 'text-gray-400']">
                          {{ value > 0 ? `-${value}` : '0' }}
                        </span>
                      </div>
                    </div>
                    <button class="ml-auto flex items-center gap-1 rounded-lg border border-blue-200 bg-blue-50 px-3 py-1.5 text-xs font-medium text-blue-700 hover:bg-blue-100">
                      <BarChart3 class="h-3 w-3" /> 查看详情
                    </button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
      <!-- Pagination -->
      <div class="flex items-center justify-between border-t border-gray-200 px-5 py-3">
        <span class="text-sm text-gray-500">
          共 {{ filteredRankings.length }} 个班级
        </span>
        <div class="flex items-center gap-1">
          <button
            v-for="page in Math.ceil(filteredRankings.length / pageSize)"
            :key="page"
            :class="[
              'h-8 min-w-[32px] rounded-lg px-2.5 text-sm font-medium transition-colors',
              currentPage === page ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100'
            ]"
            @click="currentPage = page"
          >
            {{ page }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
