<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">考试管理</h1>
        <p class="mt-0.5 text-sm text-gray-500">管理考试批次、考场安排与监考分配</p>
      </div>
      <button
        v-if="currentBatch"
        class="inline-flex items-center gap-1.5 rounded-lg bg-green-600 px-3 py-2 text-sm font-medium text-white hover:bg-green-700"
        @click="handleGenerateGradeBatch"
      >
        生成成绩批次
      </button>
    </div>

    <!-- Filter Bar -->
    <div class="flex items-center gap-3 border-b border-gray-200 bg-white px-6 py-3">
      <el-select v-model="queryParams.semesterId" placeholder="选择学期" clearable class="w-48" @change="onFilterChange">
        <el-option v-for="sem in semesters" :key="sem.id" :value="sem.id" :label="sem.semesterName" />
      </el-select>
      <el-select v-model="queryParams.examType" placeholder="考试类型" clearable class="w-36" @change="onFilterChange">
        <el-option :value="1" label="期中考试" />
        <el-option :value="2" label="期末考试" />
        <el-option :value="3" label="补考" />
        <el-option :value="4" label="重修考试" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="w-28" @change="onFilterChange">
        <el-option :value="0" label="草稿" />
        <el-option :value="1" label="已发布" />
        <el-option :value="2" label="进行中" />
        <el-option :value="3" label="已结束" />
      </el-select>
    </div>

    <!-- Content: master-detail -->
    <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6 space-y-4">
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
