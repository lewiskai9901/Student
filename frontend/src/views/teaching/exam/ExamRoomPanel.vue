<template>
  <!-- Room Assignment Drawer -->
  <Transition name="tm-drawer">
    <div v-if="arrangement" class="tm-drawer-overlay" @click.self="emit('close')">
      <div class="tm-drawer" style="width: 640px;">
        <div class="tm-drawer-header">
          <h3 class="tm-drawer-title">分配考场</h3>
          <button class="tm-drawer-close" @click="emit('close')">&times;</button>
        </div>
        <div class="tm-drawer-body">
          <!-- Arrangement Info -->
          <div class="tm-section" style="background: #f9fafb;">
            <div class="tm-stats" style="margin-top: 0;">
              <span>课程 <b>{{ arrangement.courseName }}</b></span>
              <span class="sep" />
              <span>时间 <b>{{ arrangement.examDate }} {{ arrangement.startTime }}-{{ arrangement.endTime }}</b></span>
            </div>
          </div>

          <!-- Assigned Rooms -->
          <div class="tm-section">
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
              <h4 class="tm-section-title" style="margin-bottom: 0;">已分配考场</h4>
              <button class="tm-btn tm-btn-secondary" style="padding: 5px 10px; font-size: 12px;" @click="showAddRoom">添加考场</button>
            </div>

            <table class="tm-table">
              <colgroup>
                <col />
                <col style="width: 70px" />
                <col style="width: 90px" />
                <col style="width: 160px" />
                <col style="width: 60px" />
              </colgroup>
              <thead>
                <tr>
                  <th class="text-left">教室</th>
                  <th>容量</th>
                  <th>实际人数</th>
                  <th class="text-left">监考教师</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="assignedRooms.length === 0">
                  <td colspan="5" class="tm-empty">尚未分配考场</td>
                </tr>
                <tr v-for="(room, idx) in assignedRooms" :key="idx">
                  <td class="text-left">{{ room.classroomName }}</td>
                  <td class="tm-mono">{{ room.capacity }}</td>
                  <td>
                    <input v-model.number="room.actualCount" type="number" min="0" :max="room.capacity" class="tm-input" style="width: 70px; padding: 4px 6px; text-align: center;" />
                  </td>
                  <td class="text-left">
                    <select v-model="room.invigilatorIds" class="tm-field-select" multiple style="min-height: 50px; font-size: 12px;">
                      <option v-for="t in teacherOptions" :key="t.id" :value="t.id">{{ t.realName || t.username }}</option>
                    </select>
                  </td>
                  <td>
                    <button class="tm-action tm-action-danger" @click="removeRoom(idx)">移除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="tm-drawer-footer">
          <button class="tm-btn tm-btn-secondary" @click="emit('close')">取消</button>
          <button class="tm-btn tm-btn-primary" :disabled="saving" @click="saveRoomAssignment">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Add Room Drawer -->
  <Transition name="tm-drawer">
    <div v-if="addRoomDialogVisible" class="tm-drawer-overlay" @click.self="addRoomDialogVisible = false">
      <div class="tm-drawer" style="width: 380px;">
        <div class="tm-drawer-header">
          <h3 class="tm-drawer-title">选择考场</h3>
          <button class="tm-drawer-close" @click="addRoomDialogVisible = false">&times;</button>
        </div>
        <div class="tm-drawer-body">
          <div class="tm-section">
            <div class="tm-field">
              <label class="tm-label">教室</label>
              <select v-model="newRoomForm.classroomId" class="tm-field-select">
                <option :value="undefined" disabled>选择教室</option>
                <option v-for="c in classroomOptions" :key="c.id" :value="c.id">
                  {{ c.placeName || c.name }} (容量: {{ c.capacity || c.attributes?.capacity || '-' }})
                </option>
              </select>
            </div>
          </div>
        </div>
        <div class="tm-drawer-footer">
          <button class="tm-btn tm-btn-secondary" @click="addRoomDialogVisible = false">取消</button>
          <button class="tm-btn tm-btn-primary" @click="confirmAddRoom">确定</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
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

watch(() => props.arrangement, (val) => {
  if (val) {
    assignedRooms.value = (val.examRooms || []).map(room => ({
      ...room,
      invigilatorIds: room.invigilators?.map((i: any) => i.teacherId) || [],
    }))
  }
})

// State
const saving = ref(false)
const assignedRooms = ref<any[]>([])
const addRoomDialogVisible = ref(false)
const newRoomForm = ref({ classroomId: undefined as number | undefined })

const teacherOptions = ref<{ id: number | string; username: string; realName?: string }[]>([])
const classroomOptions = ref<any[]>([])

async function loadTeacherOptions() {
  try {
    const res = await request.get('/users', { params: { pageSize: 500 } })
    const data = res.data || res
    teacherOptions.value = (Array.isArray(data) ? data : data.records || []).map((t: any) => ({ id: t.id, username: t.username, realName: t.realName }))
  } catch { /* */ }
}

async function loadClassroomOptions() {
  try {
    const res = await request.get('/places', { params: { roomType: 'CLASSROOM', pageSize: 500 } })
    const data = res.data || res
    classroomOptions.value = Array.isArray(data) ? data : data.records || []
  } catch { /* */ }
}

const showAddRoom = () => {
  newRoomForm.value = { classroomId: undefined }
  addRoomDialogVisible.value = true
}

const confirmAddRoom = () => {
  if (!newRoomForm.value.classroomId) { ElMessage.warning('请选择教室'); return }
  const classroom = classroomOptions.value.find((c: any) => c.id === newRoomForm.value.classroomId)
  if (!classroom) return
  if (assignedRooms.value.some(r => r.classroomId === classroom.id)) { ElMessage.warning('该教室已添加'); return }
  const capacity = classroom.capacity || classroom.attributes?.capacity || 0
  assignedRooms.value.push({ classroomId: classroom.id, classroomName: classroom.placeName || classroom.name, capacity, actualCount: 0, invigilatorIds: [] })
  addRoomDialogVisible.value = false
}

const removeRoom = (index: number) => { assignedRooms.value.splice(index, 1) }

const saveRoomAssignment = async () => {
  if (!props.arrangement) return
  saving.value = true
  try {
    await examApi.assignRooms(props.arrangement.id, assignedRooms.value.map(r => ({ classroomId: r.classroomId, capacity: r.capacity })))
    for (const room of assignedRooms.value) {
      if (room.id && room.invigilatorIds?.length > 0) {
        await examApi.assignInvigilators(room.id, room.invigilatorIds, room.invigilatorIds[0])
      }
    }
    ElMessage.success('保存成功')
    emit('saved')
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

loadTeacherOptions()
loadClassroomOptions()
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
