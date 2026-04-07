<template>
  <div class="tm-page">
    <!-- Header -->
    <div class="tm-header">
      <div>
        <h1 class="tm-title">成绩管理</h1>
        <div class="tm-stats">
          <span>管理课程成绩批次、录入与统计</span>
        </div>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="tm-filters">
      <select v-model="queryParams.semesterId" class="tm-select" @change="onFilterChange">
        <option :value="undefined">全部学期</option>
        <option v-for="sem in semesters" :key="sem.id" :value="sem.id">{{ sem.semesterName }}</option>
      </select>
      <select v-model="queryParams.gradeType" class="tm-select" @change="onFilterChange">
        <option :value="undefined">全部类型</option>
        <option :value="1">平时成绩</option>
        <option :value="2">期中成绩</option>
        <option :value="3">期末成绩</option>
        <option :value="4">总评成绩</option>
      </select>
      <select v-model="queryParams.status" class="tm-select" @change="onFilterChange">
        <option :value="undefined">全部状态</option>
        <option :value="0">草稿</option>
        <option :value="1">已提交</option>
        <option :value="2">已审核</option>
        <option :value="3">已发布</option>
      </select>
    </div>

    <!-- Content: master-detail -->
    <div class="tm-table-wrap" style="display: flex; flex-direction: column; gap: 16px;">
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

function onFilterChange() {
  currentBatch.value = undefined
}

function onEnterGrades(batch: GradeBatch) {
  entryBatch.value = batch
}

function onViewStatistics(_batch: GradeBatch) {
  // statistics panel reacts via batch-id prop
}

onMounted(async () => {
  semesters.value = await sharedData.getSemesters()
  const current = await sharedData.getCurrentSemester()
  if (current) queryParams.semesterId = current.id
})
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
