<template>
  <el-dialog
    v-model="visible"
    title="办理入住"
    width="480px"
    :close-on-click-modal="false"
  >
    <!-- 房间信息 -->
    <div class="bg-gray-50 rounded-lg p-4 mb-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-blue-100 flex items-center justify-center">
            <el-icon class="text-blue-600 text-lg"><House /></el-icon>
          </div>
          <div>
            <div class="font-medium text-gray-900">{{ spaceName }}</div>
            <div class="text-xs text-gray-500 mt-0.5">
              容量 {{ capacity }} · 已住 {{ currentOccupancy }} · 剩余 {{ capacity - currentOccupancy }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="space-y-4"
    >
      <!-- 入住类型 -->
      <el-form-item label="入住类型">
        <el-radio-group v-model="form.occupantType" @change="occupantOptions = []" class="w-full">
          <el-radio-button
            v-for="t in occupantTypes"
            :key="t.value"
            :value="t.value"
            class="flex-1"
          >
            {{ t.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- 入住人员 -->
      <el-form-item label="入住人员" prop="occupantId">
        <el-select
          v-model="form.occupantId"
          placeholder="输入姓名或学号搜索"
          filterable
          remote
          :remote-method="searchOccupant"
          :loading="searching"
          class="w-full"
        >
          <el-option
            v-for="item in occupantOptions"
            :key="item.id"
            :label="`${item.name} (${item.no})`"
            :value="item.id"
          >
            <div class="flex justify-between items-center">
              <span>{{ item.name }}</span>
              <span class="text-xs text-gray-400">{{ item.no }}</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <!-- 床位与备注 -->
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="床位号">
            <el-input-number
              v-model="form.positionNo"
              :min="1"
              :max="capacity"
              placeholder="可选"
              class="w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="备注">
            <el-input
              v-model="form.remark"
              placeholder="可选"
              clearable
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确认入住
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { House } from '@element-plus/icons-vue'
import { checkIn } from '@/api/v2/space'
import type { OccupantType, CheckInRequest } from '@/types/v2/space'
import request from '@/utils/request'

const props = defineProps<{
  visible: boolean
  spaceId: number
  spaceName: string
  capacity: number
  currentOccupancy: number
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// 入住类型选项
const occupantTypes = [
  { value: 'STUDENT', label: '学生' },
  { value: 'TEACHER', label: '教师' },
  { value: 'STAFF', label: '职工' }
]

// 表单
const formRef = ref<FormInstance>()
const form = ref<{
  occupantType: OccupantType | ''
  occupantId: number | undefined
  positionNo: number | undefined
  remark: string
}>({
  occupantType: 'STUDENT',
  occupantId: undefined,
  positionNo: undefined,
  remark: ''
})

const rules: FormRules = {
  occupantId: [{ required: true, message: '请选择入住人员', trigger: 'change' }]
}

// 重置表单
watch(
  () => props.visible,
  (val) => {
    if (!val) {
      form.value = {
        occupantType: 'STUDENT',
        occupantId: undefined,
        positionNo: undefined,
        remark: ''
      }
      occupantOptions.value = []
      formRef.value?.resetFields()
    }
  }
)

// 人员搜索
const searching = ref(false)
const occupantOptions = ref<{ id: number; name: string; no: string }[]>([])

async function searchOccupant(query: string) {
  if (!query || query.length < 2) {
    occupantOptions.value = []
    return
  }

  searching.value = true
  try {
    let url = ''
    if (form.value.occupantType === 'STUDENT') {
      url = `/students/search?keyword=${encodeURIComponent(query)}&pageSize=20`
    } else {
      url = `/users/search?keyword=${encodeURIComponent(query)}&pageSize=20`
    }

    const res = await request.get<any>(url)
    const data = res?.list || res || []

    occupantOptions.value = data.map((item: any) => ({
      id: item.id,
      name: item.name || item.realName || item.username,
      no: item.studentNo || item.employeeNo || item.username
    }))
  } catch (error) {
    console.error('搜索人员失败:', error)
    occupantOptions.value = []
  } finally {
    searching.value = false
  }
}

// 提交
const submitting = ref(false)

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (!form.value.occupantType || !form.value.occupantId) {
    ElMessage.warning('请完善入住信息')
    return
  }

  submitting.value = true
  try {
    const request: CheckInRequest = {
      occupantType: form.value.occupantType,
      occupantId: form.value.occupantId,
      positionNo: form.value.positionNo,
      remark: form.value.remark || undefined
    }

    await checkIn(props.spaceId, request)
    ElMessage.success('入住成功')
    visible.value = false
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '入住失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
:deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  font-size: 13px;
}

:deep(.el-radio-button__inner) {
  width: 100%;
}

.w-full {
  width: 100%;
}
</style>
