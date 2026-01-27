<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center">
        <!-- Backdrop -->
        <div class="fixed inset-0 bg-black/50 backdrop-blur-sm" @click="handleClose"></div>

        <!-- Dialog -->
        <div class="relative z-10 w-full max-w-2xl rounded-xl bg-white shadow-2xl">
          <!-- Header -->
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h3 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
              <School class="h-5 w-5 text-cyan-600" />
              {{ isEdit ? '编辑教室' : '新增教室' }}
            </h3>
            <button @click="handleClose" class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
              <X class="h-5 w-5" />
            </button>
          </div>

          <!-- Form -->
          <div class="max-h-[70vh] overflow-y-auto p-6">
            <div class="grid grid-cols-2 gap-4">
              <!-- Building (Read-only if provided) -->
              <div class="col-span-2">
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  所属教学楼 <span class="text-red-500">*</span>
                </label>
                <div v-if="buildingName" class="flex items-center gap-2 rounded-lg bg-gray-50 px-3 py-2 text-sm text-gray-600">
                  <Building2 class="h-4 w-4 text-gray-400" />
                  {{ buildingName }}
                </div>
                <input v-else v-model="form.buildingId" type="hidden" />
              </div>

              <!-- Classroom Name -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  教室名称 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="form.classroomName"
                  type="text"
                  placeholder="如: 多媒体教室A101"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                />
              </div>

              <!-- Classroom Code -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  教室编号 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="form.classroomCode"
                  type="text"
                  placeholder="如: JXL-A101"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 font-mono text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                />
              </div>

              <!-- Floor -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  楼层 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model.number="form.floor"
                  type="number"
                  min="1"
                  max="50"
                  placeholder="请输入楼层"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                />
              </div>

              <!-- Room Number -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  房间号 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="form.roomNumber"
                  type="text"
                  placeholder="如: 101, A101"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                />
              </div>

              <!-- Capacity -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">
                  容纳人数 <span class="text-red-500">*</span>
                </label>
                <input
                  v-model.number="form.capacity"
                  type="number"
                  min="1"
                  max="500"
                  placeholder="请输入容纳人数"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                />
              </div>

              <!-- Classroom Type -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">教室类型</label>
                <select
                  v-model="form.classroomType"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                >
                  <option value="普通教室">普通教室</option>
                  <option value="多媒体教室">多媒体教室</option>
                  <option value="实验室">实验室</option>
                </select>
              </div>

              <!-- Status -->
              <div>
                <label class="mb-1.5 block text-sm font-medium text-gray-700">状态</label>
                <div class="flex items-center gap-6 py-2">
                  <label class="flex cursor-pointer items-center gap-2">
                    <input
                      v-model="form.status"
                      type="radio"
                      :value="1"
                      class="h-4 w-4 border-gray-300 text-cyan-600 focus:ring-cyan-500"
                    />
                    <span class="text-sm text-gray-700">启用</span>
                  </label>
                  <label class="flex cursor-pointer items-center gap-2">
                    <input
                      v-model="form.status"
                      type="radio"
                      :value="0"
                      class="h-4 w-4 border-gray-300 text-cyan-600 focus:ring-cyan-500"
                    />
                    <span class="text-sm text-gray-700">停用</span>
                  </label>
                </div>
              </div>

              <!-- Facilities -->
              <div class="col-span-2">
                <label class="mb-1.5 block text-sm font-medium text-gray-700">设施设备</label>
                <textarea
                  v-model="form.facilities"
                  rows="3"
                  placeholder="请描述教室的设施设备，如：投影仪、音响、空调等"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                ></textarea>
              </div>
            </div>
          </div>

          <!-- Footer -->
          <div class="flex justify-end gap-3 border-t border-gray-100 px-6 py-4">
            <button
              @click="handleClose"
              class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              取消
            </button>
            <button
              @click="handleSubmit"
              :disabled="submitting"
              class="flex items-center gap-2 rounded-lg bg-cyan-600 px-4 py-2 text-sm font-medium text-white transition-all hover:bg-cyan-700 disabled:opacity-50"
            >
              <Loader2 v-if="submitting" class="h-4 w-4 animate-spin" />
              {{ isEdit ? '保存修改' : '创建教室' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { School, Building2, X, Loader2 } from 'lucide-vue-next'
import { createClassroom, updateClassroom, type ClassroomWithDetails } from '@/api/teaching'
import type { Classroom } from '@/types/teaching'

const props = defineProps<{
  visible: boolean
  classroom?: ClassroomWithDetails | null
  buildingId?: number | string | null
  buildingName?: string | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  close: []
  success: []
}>()

const isEdit = computed(() => !!props.classroom?.id)
const submitting = ref(false)

const defaultForm = (): Partial<Classroom> => ({
  buildingId: props.buildingId || '',
  classroomName: '',
  classroomCode: '',
  floor: 1,
  roomNumber: '',
  capacity: 50,
  classroomType: '普通教室',
  facilities: '',
  status: 1
})

const form = reactive(defaultForm())

// Watch for classroom prop changes
watch(
  () => props.classroom,
  (newVal) => {
    if (newVal) {
      Object.assign(form, {
        id: newVal.id,
        buildingId: newVal.buildingId || props.buildingId,
        classroomName: newVal.classroomName,
        classroomCode: newVal.classroomCode,
        floor: newVal.floor,
        roomNumber: newVal.roomNumber,
        capacity: newVal.capacity,
        classroomType: newVal.classroomType || '普通教室',
        facilities: newVal.facilities || '',
        status: newVal.status
      })
    } else {
      Object.assign(form, defaultForm())
    }
  },
  { immediate: true }
)

// Watch for buildingId prop changes
watch(
  () => props.buildingId,
  (newVal) => {
    if (!props.classroom && newVal) {
      form.buildingId = newVal
    }
  }
)

// Watch for dialog visibility
watch(
  () => props.visible,
  (newVal) => {
    if (newVal && !props.classroom) {
      Object.assign(form, defaultForm())
    }
  }
)

const validate = (): boolean => {
  if (!form.classroomName?.trim()) {
    ElMessage.error('请输入教室名称')
    return false
  }
  if (!form.classroomCode?.trim()) {
    ElMessage.error('请输入教室编号')
    return false
  }
  if (!form.floor || form.floor < 1) {
    ElMessage.error('请输入有效楼层')
    return false
  }
  if (!form.roomNumber?.trim()) {
    ElMessage.error('请输入房间号')
    return false
  }
  if (!form.capacity || form.capacity < 1) {
    ElMessage.error('请输入有效容纳人数')
    return false
  }
  return true
}

const handleSubmit = async () => {
  if (!validate()) return

  submitting.value = true
  try {
    const data = {
      ...form,
      buildingId: props.buildingId || form.buildingId
    }

    if (isEdit.value) {
      await updateClassroom(props.classroom!.id!, data)
      ElMessage.success('教室更新成功')
    } else {
      await createClassroom(data)
      ElMessage.success('教室创建成功')
    }
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  emit('update:visible', false)
  emit('close')
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.95);
}
</style>
