<template>
  <el-dialog
    :model-value="visible"
    title="合并组织"
    width="480px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
    @closed="handleClosed"
  >
    <div class="space-y-4">
      <div class="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3">
        <p class="text-sm text-amber-800">
          将「<span class="font-semibold">{{ source?.unitName }}</span>」的所有子组织和成员合并到目标组织，原组织将被标记为已合并。
        </p>
      </div>

      <!-- Target selection -->
      <div>
        <label class="mb-1.5 block text-sm font-medium text-gray-700">
          目标组织 <span class="text-red-500">*</span>
        </label>
        <el-tree-select
          v-model="targetId"
          :data="targetOptions"
          :props="treeSelectProps"
          placeholder="选择要合并到的目标组织"
          clearable
          check-strictly
          :render-after-expand="false"
          style="width: 100%"
        >
          <template #default="{ data }">
            <div class="flex items-center gap-2">
              <span
                class="flex h-5 w-5 items-center justify-center rounded"
                :style="{
                  backgroundColor: `${data.typeColor || '#6b7280'}18`,
                  color: data.typeColor || '#6b7280'
                }"
              >
                <Building2 class="h-3 w-3" />
              </span>
              <span>{{ data.unitName }}</span>
              <span class="text-xs text-gray-400">({{ data.unitCode }})</span>
            </div>
          </template>
        </el-tree-select>
      </div>

      <!-- Reason -->
      <div>
        <label class="mb-1.5 block text-sm font-medium text-gray-700">合并原因</label>
        <el-input
          v-model="reason"
          type="textarea"
          :rows="2"
          placeholder="可选，记录合并原因"
          maxlength="200"
          show-word-limit
        />
      </div>
    </div>

    <template #footer>
      <div class="flex items-center justify-end gap-3">
        <el-button @click="$emit('update:visible', false)">取消</el-button>
        <el-button
          type="warning"
          :loading="submitting"
          :disabled="!targetId"
          @click="handleSubmit"
        >
          确认合并
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Building2 } from 'lucide-vue-next'
import { mergeOrgUnit, type DepartmentResponse } from '@/api/organization'

interface Props {
  visible: boolean
  source: DepartmentResponse | null
  allDepartments: DepartmentResponse[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const targetId = ref<number | null>(null)
const reason = ref('')
const submitting = ref(false)

const treeSelectProps = {
  value: 'id',
  label: 'unitName',
  children: 'children'
}

// Exclude source and its descendants from target options
const targetOptions = computed(() => {
  if (!props.allDepartments.length || !props.source) return []

  const excludeIds = new Set<number>()
  const collectIds = (node: DepartmentResponse) => {
    excludeIds.add(node.id)
    node.children?.forEach(collectIds)
  }
  collectIds(props.source)

  const filterDepts = (items: DepartmentResponse[]): DepartmentResponse[] => {
    return items
      .filter(item => !excludeIds.has(item.id))
      .map(item => ({
        ...item,
        children: item.children ? filterDepts(item.children) : undefined
      }))
  }
  return filterDepts(props.allDepartments)
})

const handleSubmit = async () => {
  if (!targetId.value || !props.source) return

  const targetNode = findNode(props.allDepartments, targetId.value)
  const targetName = targetNode?.unitName || `#${targetId.value}`

  try {
    await ElMessageBox.confirm(
      `确定要将「${props.source.unitName}」合并到「${targetName}」吗？此操作不可撤销。`,
      '确认合并',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    await mergeOrgUnit(props.source.id, targetId.value, reason.value || undefined)
    ElMessage.success('合并成功')
    emit('update:visible', false)
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '合并失败')
  } finally {
    submitting.value = false
  }
}

const findNode = (nodes: DepartmentResponse[], id: number): DepartmentResponse | null => {
  for (const n of nodes) {
    if (n.id === id) return n
    if (n.children) {
      const found = findNode(n.children, id)
      if (found) return found
    }
  }
  return null
}

const handleClosed = () => {
  targetId.value = null
  reason.value = ''
}

watch(() => props.visible, (val) => {
  if (!val) handleClosed()
})
</script>
