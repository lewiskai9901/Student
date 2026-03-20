<template>
  <el-dialog
    :model-value="visible"
    title="拆分组织"
    width="600px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
    @closed="handleClosed"
  >
    <div class="space-y-4">
      <div class="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3">
        <p class="text-sm text-blue-800">
          将「<span class="font-semibold">{{ source?.unitName }}</span>」拆分为多个新组织。
          原组织将被标记为已解散，子组织可分配到新创建的组织中。
        </p>
      </div>

      <!-- Reason -->
      <div>
        <label class="mb-1.5 block text-sm font-medium text-gray-700">
          拆分原因 <span class="text-red-500">*</span>
        </label>
        <el-input
          v-model="reason"
          type="textarea"
          :rows="2"
          placeholder="请输入拆分原因"
          maxlength="200"
          show-word-limit
        />
      </div>

      <!-- New units -->
      <div>
        <div class="mb-2 flex items-center justify-between">
          <label class="text-sm font-medium text-gray-700">
            新组织 <span class="text-red-500">*</span>
            <span class="ml-1 text-xs font-normal text-gray-400">(至少2个)</span>
          </label>
          <button
            type="button"
            class="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-2.5 py-1 text-xs font-medium text-gray-600 hover:bg-gray-50"
            @click="addSplit"
          >
            <Plus class="h-3 w-3" />
            添加
          </button>
        </div>

        <div class="space-y-3">
          <div
            v-for="(split, index) in splits"
            :key="index"
            class="rounded-lg border border-gray-200 bg-gray-50/50 p-3"
          >
            <div class="mb-2 flex items-center justify-between">
              <span class="text-xs font-medium text-gray-500">新组织 {{ index + 1 }}</span>
              <button
                v-if="splits.length > 2"
                type="button"
                class="rounded p-0.5 text-gray-400 hover:bg-red-50 hover:text-red-500"
                @click="removeSplit(index)"
              >
                <X class="h-3.5 w-3.5" />
              </button>
            </div>
            <div class="grid grid-cols-2 gap-2">
              <div>
                <el-input
                  v-model="split.unitName"
                  placeholder="组织名称"
                  size="small"
                />
              </div>
              <div>
                <el-input
                  v-model="split.unitCode"
                  placeholder="组织编码"
                  size="small"
                />
              </div>
            </div>

            <!-- Child assignment (optional) -->
            <div v-if="sourceChildren.length > 0" class="mt-2">
              <label class="mb-1 block text-[11px] text-gray-500">分配子组织（可选）</label>
              <el-select
                v-model="split.childIds"
                multiple
                placeholder="选择要分配到此组织的子组织"
                size="small"
                style="width: 100%"
                clearable
              >
                <el-option
                  v-for="child in availableChildrenFor(index)"
                  :key="child.id"
                  :label="child.unitName"
                  :value="child.id"
                />
              </el-select>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex items-center justify-end gap-3">
        <el-button @click="$emit('update:visible', false)">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="!isValid"
          @click="handleSubmit"
        >
          确认拆分
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, X } from 'lucide-vue-next'
import { splitOrgUnit, type DepartmentResponse } from '@/api/organization'

interface SplitItem {
  unitName: string
  unitCode: string
  childIds: number[]
}

interface Props {
  visible: boolean
  source: DepartmentResponse | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const reason = ref('')
const submitting = ref(false)
const splits = ref<SplitItem[]>([
  { unitName: '', unitCode: '', childIds: [] },
  { unitName: '', unitCode: '', childIds: [] }
])

const sourceChildren = computed(() => props.source?.children || [])

// Available children for a given split (exclude children already assigned to other splits)
const availableChildrenFor = (splitIndex: number) => {
  const assignedElsewhere = new Set<number>()
  splits.value.forEach((s, i) => {
    if (i !== splitIndex) {
      s.childIds.forEach(id => assignedElsewhere.add(id))
    }
  })
  return sourceChildren.value.filter(c => !assignedElsewhere.has(c.id))
}

const isValid = computed(() => {
  if (!reason.value.trim()) return false
  if (splits.value.length < 2) return false
  return splits.value.every(s => s.unitName.trim() && s.unitCode.trim())
})

const addSplit = () => {
  splits.value.push({ unitName: '', unitCode: '', childIds: [] })
}

const removeSplit = (index: number) => {
  splits.value.splice(index, 1)
}

const handleSubmit = async () => {
  if (!isValid.value || !props.source) return

  try {
    await ElMessageBox.confirm(
      `确定要将「${props.source.unitName}」拆分为 ${splits.value.length} 个新组织吗？原组织将被解散。`,
      '确认拆分',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    await splitOrgUnit(props.source.id, {
      reason: reason.value,
      splits: splits.value.map(s => ({
        unitCode: s.unitCode,
        unitName: s.unitName,
        childIds: s.childIds.length > 0 ? s.childIds : undefined
      }))
    })
    ElMessage.success('拆分成功')
    emit('update:visible', false)
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '拆分失败')
  } finally {
    submitting.value = false
  }
}

const handleClosed = () => {
  reason.value = ''
  splits.value = [
    { unitName: '', unitCode: '', childIds: [] },
    { unitName: '', unitCode: '', childIds: [] }
  ]
}

watch(() => props.visible, (val) => {
  if (!val) handleClosed()
})
</script>
