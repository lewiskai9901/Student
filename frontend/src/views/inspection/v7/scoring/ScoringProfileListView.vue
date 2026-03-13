<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Eye, Trash2 } from 'lucide-vue-next'
import { useInspScoringStore } from '@/stores/insp/inspScoringStore'
import type { ScoringProfile } from '@/types/insp/scoring'
import InspEmptyState from '../shared/InspEmptyState.vue'

const router = useRouter()
const store = useInspScoringStore()

const loading = ref(false)
const profiles = ref<ScoringProfile[]>([])

async function loadData() {
  loading.value = true
  try {
    await store.loadProfiles()
    profiles.value = store.profiles
  } catch (e: any) {
    ElMessage.error(e.message || '加载评分配置列表失败')
  } finally {
    loading.value = false
  }
}

function goEdit(profile: ScoringProfile) {
  router.push(`/inspection/v7/scoring/${profile.id}`)
}

async function handleDelete(profile: ScoringProfile) {
  try {
    await ElMessageBox.confirm(
      `确认删除该评分配置？此操作不可恢复。`,
      '确认删除',
      { type: 'warning' }
    )
    await store.deleteProfile(profile.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="p-5 space-y-4">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">评分配置列表</h2>
    </div>

    <!-- Table -->
    <el-table :data="profiles" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="templateId" label="关联模板" width="120" />
      <el-table-column prop="maxScore" label="最高分" width="100" align="center" />
      <el-table-column prop="minScore" label="最低分" width="100" align="center" />
      <el-table-column prop="precisionDigits" label="精度" width="70" align="center" />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <div class="flex items-center gap-1">
            <el-button link type="primary" size="small" @click="goEdit(row)">
              <Eye class="w-3.5 h-3.5" />
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              <Trash2 class="w-3.5 h-3.5" />
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <InspEmptyState v-if="!loading && profiles.length === 0" message="暂无评分配置" />
  </div>
</template>
