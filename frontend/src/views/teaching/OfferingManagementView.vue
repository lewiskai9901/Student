<template>
  <div class="tm-page">
    <!-- Header -->
    <div class="tm-header">
      <div>
        <h1 class="tm-title">开课管理</h1>
        <div class="tm-stats">
          <span>学期开课、班级分配、教学班管理</span>
        </div>
      </div>
      <div style="display: flex; align-items: center; gap: 10px;">
        <select v-model="semesterId" class="tm-select" @change="onSemesterChange">
          <option :value="''" disabled>选择学期</option>
          <option v-for="s in semesters" :key="s.id" :value="s.id">{{ s.semesterName }}</option>
        </select>
        <button v-if="semesterId" class="tm-btn tm-btn-workflow" @click="handleImportFromPlan">从培养方案导入</button>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tm-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tm-tab', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Content -->
    <div style="flex: 1; overflow-y: auto;">
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

<style>
@import '@/styles/teaching-ui.css';
</style>
