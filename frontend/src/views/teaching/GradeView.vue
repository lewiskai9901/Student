<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">成绩管理</h1>
        <p class="mt-0.5 text-sm text-gray-500">管理课程成绩批次、录入与统计</p>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="flex items-center gap-3 border-b border-gray-200 bg-white px-6 py-3">
      <el-select v-model="queryParams.semesterId" placeholder="选择学期" clearable class="w-44" @change="onFilterChange">
        <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.semesterName" />
      </el-select>
      <el-select v-model="queryParams.gradeType" placeholder="成绩类型" clearable class="w-32" @change="onFilterChange">
        <el-option :value="1" label="平时成绩" />
        <el-option :value="2" label="期中成绩" />
        <el-option :value="3" label="期末成绩" />
        <el-option :value="4" label="总评成绩" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="w-28" @change="onFilterChange">
        <el-option :value="0" label="草稿" />
        <el-option :value="1" label="已提交" />
        <el-option :value="2" label="已审核" />
        <el-option :value="3" label="已发布" />
      </el-select>
    </div>

    <!-- Content: master-detail -->
    <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6 space-y-4">
      <GradeBatchList
        :semester-id="queryParams.semesterId"
        :grade-type="queryParams.gradeType"
        :status="queryParams.status"
        @select="currentBatch = $event"
        @enter-grades="onEnterGrades"
        @view-statistics="onViewStatistics"
      />
      <GradeEntryPanel
        :batch="entryBatch"
        @close="entryBatch = undefined"
      />
      <GradeStatisticsPanel
        v-if="queryParams.semesterId"
        :semester-id="queryParams.semesterId"
        :batch-id="currentBatch?.id"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useSharedDataStore } from '@/stores/sharedData'
import type { GradeBatch } from '@/types/teaching'
import GradeBatchList from './grade/GradeBatchList.vue'
import GradeEntryPanel from './grade/GradeEntryPanel.vue'
import GradeStatisticsPanel from './grade/GradeStatisticsPanel.vue'

const sharedData = useSharedDataStore()
const semesters = ref<any[]>([])

const queryParams = reactive({
  semesterId: undefined as number | string | undefined,
  gradeType: undefined as number | undefined,
  status: undefined as number | undefined,
})

const currentBatch = ref<GradeBatch>()
const entryBatch = ref<GradeBatch>()
const statsBatch = ref<GradeBatch>()

function onFilterChange() {
  currentBatch.value = undefined
}

function onEnterGrades(batch: GradeBatch) {
  entryBatch.value = batch
}

function onViewStatistics(batch: GradeBatch) {
  statsBatch.value = batch
}

onMounted(async () => {
  semesters.value = await sharedData.getSemesters()
  const current = await sharedData.getCurrentSemester()
  if (current) queryParams.semesterId = current.id
})
</script>
