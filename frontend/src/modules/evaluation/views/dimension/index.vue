<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 页面标题 -->
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">综测维度权重配置</h1>
        <p class="mt-1 text-sm text-gray-500">配置各维度权重，总权重应为100%</p>
      </div>
      <div class="flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2">
        <span class="text-sm text-gray-500">总权重:</span>
        <span :class="['text-lg font-semibold', totalWeight === 100 ? 'text-green-600' : 'text-red-600']">
          {{ totalWeight }}%
        </span>
      </div>
    </div>

    <!-- 警告提示 -->
    <div v-if="totalWeight !== 100" class="mb-6 rounded-lg border border-amber-200 bg-amber-50 p-4">
      <div class="flex items-center gap-2">
        <AlertTriangle class="h-5 w-5 text-amber-600" />
        <span class="text-sm text-amber-700">所有维度权重之和应为100%，当前总权重为{{ totalWeight }}%</span>
      </div>
    </div>

    <!-- 表格区域 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50">
            <tr>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">维度编码</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">维度名称</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">权重(%)</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">基础分</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">奖励分上限</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">分数范围</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-medium text-gray-700">计算公式说明</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">排序</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">状态</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-medium text-gray-700">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-if="loading">
              <td colspan="10" class="px-4 py-8 text-center text-sm text-gray-500">加载中...</td>
            </tr>
            <tr v-else-if="dimensionList.length === 0">
              <td colspan="10" class="px-4 py-8 text-center text-sm text-gray-500">暂无数据</td>
            </tr>
            <tr v-for="row in dimensionList" :key="row.dimensionCode" class="hover:bg-gray-50">
              <td class="px-4 py-3">
                <span :class="['inline-flex rounded px-2 py-0.5 text-xs font-medium', getDimensionClass(row.dimensionCode)]">
                  {{ row.dimensionCode }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-900">{{ row.dimensionName }}</td>
              <td class="px-4 py-3 text-center">
                <span class="text-sm font-semibold text-blue-600">{{ row.weight }}%</span>
              </td>
              <td class="px-4 py-3 text-center text-sm text-gray-900">{{ row.baseScore }}</td>
              <td class="px-4 py-3 text-center text-sm text-gray-900">{{ row.maxBonusScore }}</td>
              <td class="px-4 py-3 text-center text-sm text-gray-900">{{ row.minTotalScore }} ~ {{ row.maxTotalScore }}</td>
              <td class="max-w-xs truncate px-4 py-3 text-sm text-gray-500" :title="row.calculationFormula">
                {{ row.calculationFormula || '-' }}
              </td>
              <td class="px-4 py-3 text-center text-sm text-gray-900">{{ row.sortOrder }}</td>
              <td class="px-4 py-3 text-center">
                <span :class="['inline-flex rounded-full px-2 py-0.5 text-xs', row.status === 1 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700']">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="px-4 py-3 text-center">
                <button class="text-sm text-blue-600 hover:text-blue-700" @click="handleEdit(row)">编辑</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 权重可视化 -->
    <div class="mt-6 rounded-lg border border-gray-200 bg-white p-6">
      <h3 class="mb-4 text-sm font-medium text-gray-900">维度权重分布</h3>
      <div class="space-y-3">
        <div v-for="dim in dimensionList" :key="dim.dimensionCode" class="flex items-center gap-3">
          <span class="w-16 text-right text-sm text-gray-600">{{ dim.dimensionName }}</span>
          <div class="flex-1 h-6 bg-gray-100 rounded overflow-hidden">
            <div
              class="h-full rounded flex items-center justify-end px-2 transition-all duration-300"
              :style="{ width: dim.weight + '%', backgroundColor: getDimensionColor(dim.dimensionCode) }"
            >
              <span class="text-xs font-medium text-white">{{ dim.weight }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50" @click="dialogVisible = false"></div>
        <div class="relative w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
          <div class="mb-6 flex items-center justify-between">
            <h3 class="text-lg font-medium text-gray-900">编辑维度配置</h3>
            <button class="text-gray-400 hover:text-gray-500" @click="dialogVisible = false">
              <X class="h-5 w-5" />
            </button>
          </div>
          <form @submit.prevent="handleSubmit" class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="mb-1 block text-sm text-gray-700">维度编码</label>
                <input type="text" :value="formData.dimensionCode" disabled class="h-9 w-full rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-700">维度名称</label>
                <input type="text" :value="formData.dimensionName" disabled class="h-9 w-full rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="mb-1 block text-sm text-gray-700">权重(%)<span class="text-red-500">*</span></label>
                <input type="number" v-model.number="formData.weight" min="0" max="100" step="0.01" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-700">基础分<span class="text-red-500">*</span></label>
                <input type="number" v-model.number="formData.baseScore" min="0" max="100" step="0.01" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="mb-1 block text-sm text-gray-700">奖励分上限<span class="text-red-500">*</span></label>
                <input type="number" v-model.number="formData.maxBonusScore" min="0" max="100" step="0.01" required class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-700">排序</label>
                <input type="number" v-model.number="formData.sortOrder" min="0" max="99" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="mb-1 block text-sm text-gray-700">最低分</label>
                <input type="number" v-model.number="formData.minTotalScore" min="0" max="100" step="0.01" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-700">最高分</label>
                <input type="number" v-model.number="formData.maxTotalScore" min="0" max="100" step="0.01" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
              </div>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-700">计算公式说明</label>
              <textarea v-model="formData.calculationFormula" rows="3" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none" placeholder="请输入计算公式说明"></textarea>
            </div>
            <div class="flex justify-end gap-3 pt-4">
              <button type="button" class="h-9 rounded-lg border border-gray-200 px-4 text-sm text-gray-700 hover:bg-gray-50" @click="dialogVisible = false">取消</button>
              <button type="submit" :disabled="submitLoading" class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700 disabled:opacity-50">
                {{ submitLoading ? '保存中...' : '确定' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { X, AlertTriangle } from 'lucide-vue-next'
import { listAllDimensions, updateDimension, type EvaluationDimension } from '@/api/v2/evaluation'

const dimensionList = ref<EvaluationDimension[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)

const formData = reactive<EvaluationDimension>({
  dimensionCode: '', dimensionName: '', weight: 0, baseScore: 60, maxBonusScore: 40,
  minTotalScore: 0, maxTotalScore: 100, calculationFormula: '', sortOrder: 0
})

const totalWeight = computed(() => dimensionList.value.reduce((sum, dim) => sum + (dim.weight || 0), 0))

const getDimensionClass = (code: string) => {
  const map: Record<string, string> = {
    MORAL: 'bg-red-100 text-red-700', INTELLECTUAL: 'bg-blue-100 text-blue-700',
    PHYSICAL: 'bg-green-100 text-green-700', AESTHETIC: 'bg-amber-100 text-amber-700',
    LABOR: 'bg-gray-100 text-gray-700', DEVELOPMENT: 'bg-purple-100 text-purple-700'
  }
  return map[code] || 'bg-gray-100 text-gray-700'
}

const getDimensionColor = (code: string) => {
  const map: Record<string, string> = {
    MORAL: '#ef4444', INTELLECTUAL: '#3b82f6', PHYSICAL: '#22c55e',
    AESTHETIC: '#f59e0b', LABOR: '#6b7280', DEVELOPMENT: '#a855f7'
  }
  return map[code] || '#6b7280'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await listAllDimensions()
    dimensionList.value = res.data || []
  } catch (error) {
    console.error('加载维度配置失败', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = (row: EvaluationDimension) => {
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    submitLoading.value = true
    await updateDimension(formData.id!, formData)
    ElMessage.success('更新成功')
    dialogVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '提交失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => loadData())
</script>
