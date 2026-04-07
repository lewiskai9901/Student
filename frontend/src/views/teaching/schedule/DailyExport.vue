<template>
  <div>
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 20px;">
      <h3 style="font-size: 14px; font-weight: 600; color: #111827; margin: 0 0 16px;">导出课表</h3>
      <div class="tm-field">
        <label class="tm-label">导出维度</label>
        <div class="tm-radios" style="width: 240px;">
          <label :class="['tm-radio', { active: dimension === 'class' }]" @click="dimension = 'class'; targetId = ''"><input type="radio" />班级课表</label>
          <label :class="['tm-radio', { active: dimension === 'teacher' }]" @click="dimension = 'teacher'; targetId = ''"><input type="radio" />教师课表</label>
        </div>
      </div>
      <div class="tm-field">
        <label class="tm-label">选择对象</label>
        <select v-model="targetId" class="tm-field-select" style="width: 260px;">
          <option :value="''" disabled>请选择</option>
          <option v-for="item in targetList" :key="item.id" :value="item.id">{{ item.name }}</option>
        </select>
      </div>
      <button class="tm-btn tm-btn-primary" :disabled="!targetId || exporting" @click="doExport">
        {{ exporting ? '导出中...' : '下载 Excel 课表' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http as request } from '@/utils/request'
import { scheduleApi } from '@/api/teaching'

const props = defineProps<{ semesterId: number | string | undefined }>()

const dimension = ref<'class' | 'teacher'>('class')
const targetId = ref<number | string>('')
const exporting = ref(false)
const classList = ref<{ id: number; name: string }[]>([])
const teacherList = ref<{ id: number; name: string }[]>([])

const targetList = computed(() => dimension.value === 'class' ? classList.value : teacherList.value)

async function loadLists() {
  try {
    const res = await request.get('/students/classes')
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    classList.value = items.map((c: any) => ({ id: c.id, name: c.className || c.name }))
  } catch { classList.value = [] }
  try {
    const res = await request.get('/users', { params: { role: 'TEACHER' } })
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    teacherList.value = items.map((t: any) => ({ id: t.id, name: t.realName || t.username || t.name }))
  } catch { teacherList.value = [] }
}

async function doExport() {
  if (!targetId.value || !props.semesterId) return
  exporting.value = true
  try {
    const blob = dimension.value === 'class'
      ? await scheduleApi.exportClassSchedule(props.semesterId, targetId.value)
      : await scheduleApi.exportTeacherSchedule(props.semesterId, targetId.value)
    const url = window.URL.createObjectURL(new Blob([blob as any]))
    const a = document.createElement('a'); a.href = url; a.download = `${dimension.value}_schedule.xlsx`
    document.body.appendChild(a); a.click(); document.body.removeChild(a); window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch { ElMessage.error('导出失败') } finally { exporting.value = false }
}

onMounted(loadLists)
</script>
