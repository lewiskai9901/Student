<template>
  <div class="tm-page" style="flex-direction: row;">
    <DeptTree :semester-id="semesterId" @select="onTreeSelect" />
    <div style="flex: 1; display: flex; flex-direction: column; overflow: hidden;">
      <!-- Header -->
      <div class="tm-header">
        <div>
          <h1 class="tm-title">开课管理</h1>
          <div class="tm-stats">
            <span>学期开课、教学任务落实</span>
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
        <OfferingListTab v-if="activeTab === 'offerings'" :semester-id="semesterId" :selected-org="selectedOrg" @all-confirmed="activeTab = 'fulfillment'" />
        <TaskFulfillmentTab v-else-if="activeTab === 'fulfillment'" :semester-id="semesterId" :selected-org="selectedOrg" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSharedDataStore } from '@/stores/sharedData'
import { workflowApi } from '@/api/teaching'
import DeptTree from '@/components/teaching/DeptTree.vue'
import OfferingListTab from './offering/OfferingListTab.vue'
import TaskFulfillmentTab from './offering/TaskFulfillmentTab.vue'

const route = useRoute()
const sharedData = useSharedDataStore()
const semesters = ref<any[]>([])
const semesterId = ref<number | string>('')
const activeTab = ref((route.query.tab as string) || 'offerings')

watch(() => route.query.tab, (tab) => {
  if (tab && typeof tab === 'string') activeTab.value = tab
})

const selectedOrg = ref<{ type: string; id: number | string; name: string; classIds?: (number | string)[] }>({ type: '', id: '', name: '' })

function onTreeSelect(node: { type: string; id: number | string; name: string; classIds?: (number | string)[] }) {
  selectedOrg.value = node
}

const tabs = [
  { key: 'offerings', label: '开课计划' },
  { key: 'fulfillment', label: '任务落实' },
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
