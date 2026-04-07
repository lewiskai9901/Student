<template>
  <div class="tm-page">
    <!-- Header -->
    <div class="tm-header">
      <div>
        <h1 class="tm-title">考试管理</h1>
        <div class="tm-stats">
          <span>管理考试批次、考场安排与监考分配</span>
        </div>
      </div>
      <div style="display: flex; align-items: center; gap: 8px;">
        <button v-if="currentBatch" class="tm-btn tm-btn-workflow" @click="handleGenerateGradeBatch">生成成绩批次</button>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="tm-filters">
      <select v-model="queryParams.semesterId" class="tm-select" @change="onFilterChange">
        <option :value="undefined">全部学期</option>
        <option v-for="sem in semesters" :key="sem.id" :value="sem.id">{{ sem.semesterName }}</option>
      </select>
      <select v-model="queryParams.examType" class="tm-select" @change="onFilterChange">
        <option :value="undefined">全部类型</option>
        <option :value="1">期中考试</option>
        <option :value="2">期末考试</option>
        <option :value="3">补考</option>
        <option :value="4">重修考试</option>
      </select>
      <select v-model="queryParams.status" class="tm-select" @change="onFilterChange">
        <option :value="undefined">全部状态</option>
        <option :value="0">草稿</option>
        <option :value="1">已发布</option>
        <option :value="2">进行中</option>
        <option :value="3">已结束</option>
      </select>
    </div>

    <!-- Content: master-detail -->
    <div class="tm-table-wrap" style="display: flex; flex-direction: column; gap: 16px;">
      <ExamBatchList
        :semester-id="queryParams.semesterId"
        :exam-type="queryParams.examType"
        :status="queryParams.status"
        @select="onSelectBatch"
      />
      <ExamArrangementPanel
        v-if="currentBatch"
        :batch="currentBatch"
        @select-arrangement="onSelectArrangement"
        @close="currentBatch = undefined"
      />
      <ExamRoomPanel
        v-if="currentArrangement"
        :arrangement="currentArrangement"
        :batch="currentBatch"
        @close="currentArrangement = undefined"
        @saved="onRoomSaved"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSharedDataStore } from '@/stores/sharedData'
import { workflowApi } from '@/api/teaching'
import type { ExamBatch, ExamArrangement } from '@/types/teaching'
import ExamBatchList from './exam/ExamBatchList.vue'
import ExamArrangementPanel from './exam/ExamArrangementPanel.vue'
import ExamRoomPanel from './exam/ExamRoomPanel.vue'

const sharedData = useSharedDataStore()
const semesters = ref<any[]>([])

const queryParams = reactive({
  semesterId: undefined as number | string | undefined,
  examType: undefined as number | undefined,
  status: undefined as number | undefined,
})

const currentBatch = ref<ExamBatch>()
const currentArrangement = ref<ExamArrangement>()

function onFilterChange() {
  currentBatch.value = undefined
  currentArrangement.value = undefined
}

function onSelectBatch(batch: ExamBatch) {
  currentBatch.value = batch
  currentArrangement.value = undefined
}

function onSelectArrangement(arrangement: ExamArrangement) {
  currentArrangement.value = arrangement
}

async function handleGenerateGradeBatch() {
  if (!currentBatch.value) return
  try {
    await ElMessageBox.confirm(
      `将从考试批次"${currentBatch.value.name || currentBatch.value.id}"创建对应的成绩录入批次。`,
      '生成成绩批次',
      { type: 'info' }
    )
    const res = await workflowApi.generateGradeBatch(currentBatch.value.id)
    ElMessage.success(`成绩批次已创建 (ID: ${res.gradeBatchId})`)
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '生成失败')
  }
}

function onRoomSaved() {
  currentArrangement.value = undefined
  const batch = currentBatch.value
  currentBatch.value = undefined
  setTimeout(() => { currentBatch.value = batch }, 0)
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
