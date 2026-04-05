<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Header -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">开课管理</h1>
        <p class="mt-0.5 text-sm text-gray-500">学期开课、班级分配、教学班管理</p>
      </div>
      <div class="flex items-center gap-3">
        <el-select v-model="semesterId" placeholder="选择学期" class="w-48" @change="onSemesterChange">
          <el-option v-for="s in semesters" :key="s.id" :value="s.id" :label="s.semesterName" />
        </el-select>
        <button
          v-if="semesterId"
          class="inline-flex items-center gap-1.5 rounded-lg bg-green-600 px-3 py-2 text-sm font-medium text-white hover:bg-green-700"
          @click="handleImportFromPlan"
        >
          从培养方案导入
        </button>
      </div>
    </div>

    <!-- Tabs -->
    <div class="border-b border-gray-200 bg-white px-6">
      <nav class="-mb-px flex gap-6">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="border-b-2 px-1 py-3 text-sm font-medium transition-colors"
          :class="activeTab === tab.key ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto">
      <OfferingListTab v-if="activeTab === 'offerings'" :semester-id="semesterId" />
      <ClassAssignmentTab v-else-if="activeTab === 'assignments'" :semester-id="semesterId" />
      <TeachingClassTab v-else-if="activeTab === 'classes'" :semester-id="semesterId" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSharedDataStore } from '@/stores/sharedData'
import { workflowApi } from '@/api/teaching'
import OfferingListTab from './offering/OfferingListTab.vue'
import ClassAssignmentTab from './offering/ClassAssignmentTab.vue'
import TeachingClassTab from './offering/TeachingClassTab.vue'

const sharedData = useSharedDataStore()
const semesters = ref<any[]>([])
const semesterId = ref<number | string>('')
const activeTab = ref('offerings')

const tabs = [
  { key: 'offerings', label: '开课计划' },
  { key: 'assignments', label: '班级分配' },
  { key: 'classes', label: '教学班' },
]

const onSemesterChange = () => {}

const handleImportFromPlan = async () => {
  if (!semesterId.value) return
  try {
    await ElMessageBox.confirm(
      '将根据年级-学期映射，从培养方案自动导入本学期应开课程。已存在的开课记录不会重复创建。',
      '从培养方案导入',
      { type: 'info' }
    )
    // 先生成映射，再导入
    await workflowApi.generateMappings(semesterId.value)
    const res = await workflowApi.generateOfferings(semesterId.value)
    ElMessage.success(`成功导入 ${res.generated} 条开课记录`)
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '导入失败')
  }
}

onMounted(async () => {
  semesters.value = await sharedData.getSemesters()
  const current = await sharedData.getCurrentSemester()
  if (current) semesterId.value = current.id
})
</script>
