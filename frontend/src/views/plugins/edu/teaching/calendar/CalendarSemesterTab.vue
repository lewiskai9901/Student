<template>
  <div class="flex gap-4">
    <!-- Left: Semester list -->
    <div class="w-72 shrink-0 rounded-xl border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <span class="text-sm font-semibold text-gray-900">学期列表</span>
        <button
          class="inline-flex items-center gap-1 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700"
          @click="$emit('show-dialog')"
        >
          新建学期
        </button>
      </div>
      <div class="max-h-[calc(100vh-340px)] overflow-y-auto p-2">
        <div
          v-for="sem in semesters"
          :key="sem.id"
          class="mb-1 cursor-pointer rounded-lg border px-3 py-2.5 transition-all"
          :class="[
            selectedSemester?.id === sem.id
              ? 'border-blue-200 bg-blue-50'
              : 'border-transparent hover:bg-gray-50',
            sem.isCurrent ? 'ring-1 ring-emerald-200' : ''
          ]"
          @click="$emit('select', sem)"
        >
          <div class="flex items-center gap-2">
            <span
              class="rounded px-1.5 py-0.5 text-xs font-medium"
              :class="{
                'bg-amber-50 text-amber-700': sem.semesterType === 1,
                'bg-emerald-50 text-emerald-700': sem.semesterType === 2,
                'bg-sky-50 text-sky-700': sem.semesterType === 3,
              }"
            >
              {{ sem.semesterType === 1 ? '秋季' : sem.semesterType === 2 ? '春季' : '夏季' }}
            </span>
            <span class="text-sm font-medium text-gray-900">{{ sem.semesterName }}</span>
            <el-tag v-if="sem.isCurrent" size="small" type="success">当前</el-tag>
          </div>
          <div class="mt-1 text-xs text-gray-400">
            <span>{{ sem.startDate }} ~ {{ sem.endDate }}</span>
          </div>
        </div>
        <el-empty v-if="!semesters.length" description="暂无学期，请先创建" :image-size="60" />
      </div>
    </div>

    <!-- Right: Semester detail + teaching weeks -->
    <div class="min-w-0 flex-1">
      <template v-if="selectedSemester">
        <div class="rounded-xl border border-gray-200 bg-white">
          <div class="flex items-center justify-between border-b border-gray-200 px-5 py-3">
            <span class="text-sm font-semibold text-gray-900">{{ selectedSemester.semesterName }} - 教学周管理</span>
            <div class="flex items-center gap-2">
              <el-button size="small" @click="$emit('show-dialog', selectedSemester)">编辑学期</el-button>
              <el-button type="primary" size="small" @click="$emit('generate-weeks')">自动生成周次</el-button>
            </div>
          </div>

          <!-- Semester info -->
          <div class="border-b border-gray-100 px-5 py-3">
            <div class="flex flex-wrap gap-6">
              <div class="text-sm">
                <span class="text-gray-400">开始日期</span>
                <span class="ml-2 font-medium text-gray-900">{{ selectedSemester.startDate }}</span>
              </div>
              <div class="text-sm">
                <span class="text-gray-400">结束日期</span>
                <span class="ml-2 font-medium text-gray-900">{{ selectedSemester.endDate }}</span>
              </div>
              <div class="text-sm">
                <span class="text-gray-400">教学周数</span>
                <span class="ml-2 font-medium text-gray-900">{{ teachingWeeks.length || '-' }}周</span>
              </div>
              <div class="text-sm">
                <span class="text-gray-400">状态</span>
                <el-tag class="ml-2" :type="selectedSemester.isCurrent ? 'success' : 'info'" size="small">
                  {{ selectedSemester.isCurrent ? '当前学期' : '非当前' }}
                </el-tag>
              </div>
            </div>
            <el-button
              v-if="!selectedSemester.isCurrent"
              type="primary"
              text
              size="small"
              class="mt-2"
              @click="$emit('set-current', selectedSemester.id)"
            >
              设为当前学期
            </el-button>
          </div>

          <!-- Teaching weeks table -->
          <div class="p-4">
            <el-table :data="teachingWeeks" border size="small" max-height="400">
              <el-table-column prop="weekNumber" label="周次" width="80" align="center">
                <template #default="{ row }">
                  <span class="font-medium text-gray-700">第{{ row.weekNumber }}周</span>
                </template>
              </el-table-column>
              <el-table-column prop="startDate" label="开始日期" width="110" align="center" />
              <el-table-column prop="endDate" label="结束日期" width="110" align="center" />
              <el-table-column prop="weekType" label="周类型" width="100" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="getWeekTypeTag(row.weekType) as any">
                    {{ getWeekTypeName(row.weekType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注/标签" min-width="120">
                <template #default="{ row }">
                  <span class="text-gray-400">{{ row.remark || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" align="center">
                <template #default="{ row }">
                  <el-button text size="small" @click="$emit('edit-week', row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </template>

      <div v-else class="flex h-64 items-center justify-center rounded-xl border border-gray-200 bg-white">
        <el-empty description="请在左侧选择一个学期查看详情" :image-size="80" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Semester, TeachingWeek } from '@/types/teaching'

defineProps<{
  semesters: Semester[]
  selectedSemester: Semester | null
  teachingWeeks: TeachingWeek[]
}>()

defineEmits<{
  select: [semester: Semester]
  'show-dialog': [semester?: Semester]
  'generate-weeks': []
  'set-current': [id: number | string]
  'edit-week': [week: TeachingWeek]
}>()

const getWeekTypeName = (type: number) => {
  const map: Record<number, string> = { 1: '教学周', 2: '考试周', 3: '假期', 4: '机动周' }
  return map[type] || '未知'
}

const getWeekTypeTag = (type: number) => {
  const map: Record<number, string> = { 1: 'success', 2: 'warning', 3: 'info', 4: '' }
  return map[type] || ''
}
</script>
