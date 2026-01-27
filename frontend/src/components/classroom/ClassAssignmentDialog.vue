<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center">
        <!-- Backdrop -->
        <div class="fixed inset-0 bg-black/50 backdrop-blur-sm" @click="handleClose"></div>

        <!-- Dialog -->
        <div class="relative z-10 w-full max-w-lg rounded-xl bg-white shadow-2xl">
          <!-- Header -->
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h3 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
              <Link2 class="h-5 w-5 text-cyan-600" />
              关联班级
            </h3>
            <button @click="handleClose" class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
              <X class="h-5 w-5" />
            </button>
          </div>

          <!-- Content -->
          <div class="p-6">
            <!-- Current Classroom Info -->
            <div class="mb-4 rounded-lg bg-gray-50 p-3">
              <div class="flex items-center gap-3">
                <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-cyan-100">
                  <School class="h-5 w-5 text-cyan-600" />
                </div>
                <div>
                  <p class="text-sm font-medium text-gray-900">{{ classroom?.classroomName }}</p>
                  <p class="text-xs text-gray-500">
                    {{ classroom?.buildingName }} · {{ classroom?.floor }}层 · 容量{{ classroom?.capacity }}人
                  </p>
                </div>
              </div>
            </div>

            <!-- Search -->
            <div class="mb-4">
              <label class="mb-1.5 block text-sm font-medium text-gray-700">选择班级</label>
              <div class="relative">
                <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <input
                  v-model="searchQuery"
                  type="text"
                  placeholder="搜索班级..."
                  class="w-full rounded-lg border border-gray-300 py-2 pl-9 pr-3 text-sm focus:border-cyan-500 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                />
              </div>
            </div>

            <!-- Loading -->
            <div v-if="loading" class="flex items-center justify-center py-8">
              <Loader2 class="h-6 w-6 animate-spin text-cyan-500" />
            </div>

            <!-- Class List -->
            <div v-else class="max-h-64 overflow-y-auto">
              <div v-if="filteredClasses.length === 0" class="py-8 text-center text-gray-400">
                <GraduationCap class="mx-auto h-8 w-8" />
                <p class="mt-2 text-sm">暂无可选班级</p>
              </div>
              <div v-else class="space-y-2">
                <div
                  v-for="cls in filteredClasses"
                  :key="cls.id"
                  @click="selectedClassId = cls.id"
                  class="flex cursor-pointer items-center justify-between rounded-lg border-2 p-3 transition-all"
                  :class="selectedClassId === cls.id ? 'border-cyan-500 bg-cyan-50' : 'border-gray-200 hover:border-cyan-300'"
                >
                  <div class="flex items-center gap-3">
                    <div
                      class="flex h-9 w-9 items-center justify-center rounded-lg text-sm font-bold"
                      :class="selectedClassId === cls.id ? 'bg-cyan-600 text-white' : 'bg-gray-100 text-gray-600'"
                    >
                      {{ getClassInitial(cls) }}
                    </div>
                    <div>
                      <p class="text-sm font-medium text-gray-900">{{ cls.className }}</p>
                      <p class="text-xs text-gray-500">
                        {{ cls.studentCount || 0 }}人 · {{ cls.headTeacherName || '无班主任' }}
                      </p>
                    </div>
                  </div>
                  <div v-if="selectedClassId === cls.id" class="flex h-5 w-5 items-center justify-center rounded-full bg-cyan-600">
                    <Check class="h-3 w-3 text-white" />
                  </div>
                </div>
              </div>
            </div>

            <!-- Warning for existing classroom -->
            <div v-if="selectedClass?.classroomName" class="mt-3 rounded-lg bg-amber-50 p-3">
              <div class="flex items-start gap-2">
                <AlertTriangle class="mt-0.5 h-4 w-4 flex-shrink-0 text-amber-500" />
                <div class="text-xs text-amber-700">
                  该班级当前已关联教室 "{{ selectedClass.classroomName }}"，
                  继续操作将解除原有关联。
                </div>
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
              @click="handleConfirm"
              :disabled="!selectedClassId || submitting"
              class="flex items-center gap-2 rounded-lg bg-cyan-600 px-4 py-2 text-sm font-medium text-white transition-all hover:bg-cyan-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Loader2 v-if="submitting" class="h-4 w-4 animate-spin" />
              确认关联
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Link2,
  X,
  School,
  Search,
  Loader2,
  GraduationCap,
  Check,
  AlertTriangle
} from 'lucide-vue-next'
import { getAllClasses, type SchoolClass } from '@/api/organization'
import { assignClassToClassroom, type ClassroomWithDetails } from '@/api/teaching'

const props = defineProps<{
  visible: boolean
  classroom?: ClassroomWithDetails | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  close: []
  success: []
}>()

const loading = ref(false)
const submitting = ref(false)
const classList = ref<SchoolClass[]>([])
const selectedClassId = ref<number | null>(null)
const searchQuery = ref('')

// Computed
const filteredClasses = computed(() => {
  if (!searchQuery.value) return classList.value
  const query = searchQuery.value.toLowerCase()
  return classList.value.filter(cls =>
    cls.className?.toLowerCase().includes(query) ||
    cls.headTeacherName?.toLowerCase().includes(query)
  )
})

const selectedClass = computed(() => {
  return classList.value.find(cls => cls.id === selectedClassId.value)
})

// Watch for dialog visibility
watch(
  () => props.visible,
  async (newVal) => {
    if (newVal) {
      selectedClassId.value = props.classroom?.classId as number || null
      searchQuery.value = ''
      await loadClasses()
    }
  }
)

// Methods
const loadClasses = async () => {
  loading.value = true
  try {
    classList.value = await getAllClasses()
  } catch (error: any) {
    ElMessage.error(error.message || '加载班级列表失败')
    classList.value = []
  } finally {
    loading.value = false
  }
}

const getClassInitial = (cls: SchoolClass) => {
  return cls.className?.charAt(0) || 'C'
}

const handleConfirm = async () => {
  if (!props.classroom?.id || !selectedClassId.value) return

  submitting.value = true
  try {
    await assignClassToClassroom(props.classroom.id, selectedClassId.value)
    ElMessage.success('班级关联成功')
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '关联失败')
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
