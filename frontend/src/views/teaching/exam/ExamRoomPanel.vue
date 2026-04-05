<template>
  <el-dialog v-model="dialogVisible" title="分配考场" width="700px" @close="onDialogClose">
    <div v-if="arrangement" class="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3">
      <div class="flex items-center gap-4 text-sm">
        <span class="text-gray-500">课程 <span class="font-medium text-gray-900">{{ arrangement.courseName }}</span></span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-gray-500">时间 <span class="font-medium text-gray-900">{{ arrangement.examDate }} {{ arrangement.startTime }}-{{ arrangement.endTime }}</span></span>
      </div>
    </div>

    <div class="mt-4">
      <div class="flex items-center justify-between">
        <span class="text-sm font-medium text-gray-700">已分配考场</span>
        <button
          class="inline-flex items-center gap-1 rounded-md border border-gray-200 px-2.5 py-1 text-xs font-medium text-gray-600 hover:bg-gray-50"
          @click="addRoom"
        >
          <Plus class="h-3 w-3" />
          添加考场
        </button>
      </div>

      <el-table :data="assignedRooms" border class="mt-3">
        <el-table-column prop="classroomName" label="教室" />
        <el-table-column prop="capacity" label="容量" width="80" align="center" />
        <el-table-column prop="actualCount" label="实际人数" width="120" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.actualCount" :min="0" :max="row.capacity" size="small" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="监考教师" min-width="180">
          <template #default="{ row }">
            <el-select v-model="row.invigilatorIds" multiple filterable size="small" placeholder="选择监考" style="width: 100%">
              <el-option v-for="t in teacherOptions" :key="t.id" :value="t.id" :label="t.realName || t.username" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <button class="rounded px-2 py-1 text-xs font-medium text-red-500 hover:bg-red-50" @click="removeRoom($index)">移除</button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveRoomAssignment">保存</el-button>
    </template>
  </el-dialog>

  <!-- Add Room Dialog -->
  <el-dialog v-model="addRoomDialogVisible" title="选择考场" width="400px">
    <el-form :model="newRoomForm" label-width="80px">
      <el-form-item label="教室">
        <el-select v-model="newRoomForm.classroomId" filterable placeholder="搜索教室" style="width: 100%">
          <el-option
            v-for="c in classroomOptions"
            :key="c.id"
            :value="c.id"
            :label="c.placeName || c.name"
          >
            <div class="flex items-center justify-between">
              <span>{{ c.placeName || c.name }}</span>
              <span class="text-xs text-gray-400">容量: {{ c.capacity || c.attributes?.capacity || '-' }}</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="addRoomDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmAddRoom">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Plus } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { examApi } from '@/api/teaching'
import { http as request } from '@/utils/request'
import type { ExamArrangement, ExamBatch } from '@/types/teaching'

const props = defineProps<{
  arrangement: ExamArrangement | undefined
  batch: ExamBatch | undefined
}>()

const emit = defineEmits<{
  close: []
  saved: []
}>()

// Dialog visibility driven by arrangement prop
const dialogVisible = ref(false)

watch(() => props.arrangement, (val) => {
  if (val) {
    dialogVisible.value = true
    assignedRooms.value = (val.examRooms || []).map(room => ({
      ...room,
      invigilatorIds: room.invigilators?.map(i => i.teacherId) || [],
    }))
  } else {
    dialogVisible.value = false
  }
})

function onDialogClose() {
  emit('close')
}

// State
const saving = ref(false)
const assignedRooms = ref<any[]>([])
const addRoomDialogVisible = ref(false)
const newRoomForm = ref({ classroomId: undefined as number | undefined })

// Options (loaded once)
const teacherOptions = ref<{ id: number | string; username: string; realName?: string }[]>([])
const classroomOptions = ref<any[]>([])

async function loadTeacherOptions() {
  try {
    const res = await request.get('/users', { params: { pageSize: 500 } })
    const data = res.data || res
    teacherOptions.value = (Array.isArray(data) ? data : data.records || []).map((t: any) => ({
      id: t.id,
      username: t.username,
      realName: t.realName,
    }))
  } catch (error) {
    console.error('Failed to load teacher options:', error)
  }
}

async function loadClassroomOptions() {
  try {
    const res = await request.get('/v9/places', { params: { typeCode: 'CLASSROOM', pageSize: 500 } })
    const data = res.data || res
    classroomOptions.value = Array.isArray(data) ? data : data.records || []
  } catch (error) {
    console.error('Failed to load classroom options:', error)
  }
}

// Room operations
const addRoom = () => {
  newRoomForm.value = { classroomId: undefined }
  addRoomDialogVisible.value = true
}

const confirmAddRoom = () => {
  if (!newRoomForm.value.classroomId) {
    ElMessage.warning('请选择教室')
    return
  }
  const classroom = classroomOptions.value.find((c: any) => c.id === newRoomForm.value.classroomId)
  if (!classroom) return
  if (assignedRooms.value.some(r => r.classroomId === classroom.id)) {
    ElMessage.warning('该教室已添加')
    return
  }
  const capacity = classroom.capacity || classroom.attributes?.capacity || 0
  assignedRooms.value.push({
    classroomId: classroom.id,
    classroomName: classroom.placeName || classroom.name,
    capacity,
    actualCount: 0,
    invigilatorIds: [],
  })
  addRoomDialogVisible.value = false
}

const removeRoom = (index: number) => {
  assignedRooms.value.splice(index, 1)
}

const saveRoomAssignment = async () => {
  if (!props.arrangement) return
  saving.value = true
  try {
    // Save room assignments
    await examApi.assignRooms(
      props.arrangement.id,
      assignedRooms.value.map(r => ({
        classroomId: r.classroomId,
        capacity: r.capacity,
      }))
    )

    // Save invigilator assignments for each room that has invigilators
    for (const room of assignedRooms.value) {
      if (room.id && room.invigilatorIds?.length > 0) {
        const mainTeacherId = room.invigilatorIds[0]
        await examApi.assignInvigilators(room.id, room.invigilatorIds, mainTeacherId)
      }
    }

    ElMessage.success('保存成功')
    dialogVisible.value = false
    emit('saved')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// Load options on creation
loadTeacherOptions()
loadClassroomOptions()
</script>
