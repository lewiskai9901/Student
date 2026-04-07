<template>
  <div>
    <!-- Controls -->
    <div style="border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 14px 20px; margin-bottom: 16px;">
      <div style="display: flex; flex-wrap: wrap; align-items: center; gap: 10px;">
        <div class="tm-radios" style="width: auto;">
          <label :class="['tm-radio', { active: viewType === 'class' }]" @click="viewType = 'class'; targetId = undefined"><input type="radio" />班级</label>
          <label :class="['tm-radio', { active: viewType === 'teacher' }]" @click="viewType = 'teacher'; targetId = undefined"><input type="radio" />教师</label>
          <label :class="['tm-radio', { active: viewType === 'classroom' }]" @click="viewType = 'classroom'; targetId = undefined"><input type="radio" />场所</label>
        </div>
        <select v-model="targetId" class="tm-select" @change="loadInstances">
          <option :value="undefined" disabled>{{ viewType === 'class' ? '选择班级' : viewType === 'teacher' ? '选择教师' : '选择场所' }}</option>
          <option v-for="o in targetOptions" :key="o.id" :value="o.id">{{ o.name }}</option>
        </select>
        <i style="display: inline-block; width: 1px; height: 18px; background: #d1d5db;" />
        <select v-model="weekNumber" class="tm-select" @change="loadInstances">
          <option :value="undefined">全部周次</option>
          <option v-for="w in 20" :key="w" :value="w">第{{ w }}周</option>
        </select>
      </div>
    </div>

    <!-- Legend -->
    <div style="display: flex; gap: 16px; margin-bottom: 12px; font-size: 12px; color: #6b7280;">
      <span><span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:#dcfce7;border:1px solid #bbf7d0;margin-right:4px;" />正常</span>
      <span><span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:#fecaca;border:1px solid #fca5a5;margin-right:4px;" />已取消</span>
      <span><span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:#bbf7d0;border:1px solid #86efac;margin-right:4px;" />补课</span>
      <span><span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:#fed7aa;border:1px solid #fdba74;margin-right:4px;" />代课</span>
      <span><span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:#e5e7eb;border:1px solid #d1d5db;margin-right:4px;" />已调走</span>
    </div>

    <!-- Instances Table -->
    <div v-if="loading" style="text-align: center; padding: 40px; color: #9ca3af;">加载中...</div>
    <div v-else-if="instances.length === 0" style="text-align: center; padding: 40px; color: #9ca3af;">
      {{ targetId ? '暂无实况数据（请先在排课设置中生成实况课表）' : '请选择查看对象' }}
    </div>
    <table v-else class="tm-table">
      <colgroup>
        <col style="width: 100px" />
        <col style="width: 70px" />
        <col />
        <col style="width: 100px" />
        <col style="width: 90px" />
        <col style="width: 90px" />
        <col style="width: 90px" />
        <col style="width: 80px" />
        <col />
        <col style="width: 100px" />
      </colgroup>
      <thead>
        <tr>
          <th>日期</th>
          <th>周次</th>
          <th class="text-left">课程</th>
          <th>班级</th>
          <th>节次</th>
          <th>教室</th>
          <th>教师</th>
          <th>状态</th>
          <th class="text-left">备注</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="inst in instances" :key="inst.id" :style="{ background: statusBg(inst.status) }">
          <td style="font-size: 12px;">{{ inst.actualDate }}</td>
          <td class="tm-mono">{{ inst.weekNumber }}</td>
          <td class="text-left" style="font-weight: 500;">{{ inst.courseName }}</td>
          <td>{{ inst.className }}</td>
          <td class="tm-mono">{{ inst.startSlot }}-{{ inst.endSlot }}节</td>
          <td style="font-size: 12px;">{{ inst.classroomName || '-' }}</td>
          <td>{{ inst.teacherName || '-' }}</td>
          <td><span :class="['tm-chip', statusChip(inst.status)]">{{ statusName(inst.status) }}</span></td>
          <td class="text-left" style="font-size: 12px; color: #6b7280;">{{ inst.cancelReason || '' }}</td>
          <td>
            <template v-if="inst.status === 0">
              <button class="tm-action" style="color: #d97706;" @click="showSubstitute(inst)">代课</button>
              <button class="tm-action tm-action-danger" @click="handleCancel(inst)">取消</button>
            </template>
            <template v-else-if="inst.status === 1 || inst.status === 4">
              <button class="tm-action" style="color: #2563eb;" @click="handleRestore(inst)">恢复</button>
            </template>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Stats -->
    <div v-if="instances.length > 0" class="tm-stats" style="margin-top: 12px;">
      <span>共 <b>{{ instances.length }}</b> 条</span>
      <span class="sep" />
      <span>正常 <b>{{ instances.filter(i => i.status === 0).length }}</b></span>
      <span class="sep" />
      <span>取消 <b>{{ instances.filter(i => i.status === 1).length }}</b></span>
      <span class="sep" />
      <span>补课 <b>{{ instances.filter(i => i.status === 3).length }}</b></span>
      <span class="sep" />
      <span>代课 <b>{{ instances.filter(i => i.status === 4).length }}</b></span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http as request } from '@/utils/request'
import { instanceApi } from '@/api/teaching'

const props = defineProps<{ semesterId: number | string | undefined }>()

const viewType = ref<'class' | 'teacher' | 'classroom'>('class')
const targetId = ref<number | string>()
const weekNumber = ref<number>()
const instances = ref<any[]>([])
const loading = ref(false)
const classList = ref<{ id: number; name: string }[]>([])
const teacherList = ref<{ id: number; name: string }[]>([])
const classroomList = ref<{ id: number; name: string }[]>([])

import { computed } from 'vue'
const targetOptions = computed(() => viewType.value === 'class' ? classList.value : viewType.value === 'teacher' ? teacherList.value : classroomList.value)

async function loadInstances() {
  if (!props.semesterId || !targetId.value) { instances.value = []; return }
  loading.value = true
  try {
    const params: any = { semesterId: props.semesterId }
    if (weekNumber.value) params.weekNumber = weekNumber.value
    if (viewType.value === 'class') params.classId = targetId.value
    else if (viewType.value === 'teacher') params.teacherId = targetId.value
    else params.classroomId = targetId.value
    const res = await instanceApi.list(params)
    instances.value = (res as any).data || res || []
  } catch { instances.value = [] } finally { loading.value = false }
}

function statusName(s: number) { return ({ 0: '正常', 1: '取消', 2: '调走', 3: '补课', 4: '代课' } as any)[s] || '?' }
function statusChip(s: number) { return ({ 0: 'tm-chip-green', 1: 'tm-chip-red', 2: 'tm-chip-gray', 3: 'tm-chip-blue', 4: 'tm-chip-amber' } as any)[s] || 'tm-chip-gray' }
function statusBg(s: number) { return ({ 1: '#fef2f2', 2: '#f9fafb', 3: '#f0fdf4', 4: '#fffbeb' } as any)[s] || '' }

async function showSubstitute(inst: any) {
  // Simple prompt for substitute teacher selection
  const teacherName = await ElMessageBox.prompt('请输入代课教师ID', '代课', { inputPlaceholder: '教师ID' }).catch(() => null)
  if (!teacherName?.value) return
  try {
    await instanceApi.substitute(inst.id, Number(teacherName.value), '代课')
    ElMessage.success('代课设置成功'); loadInstances()
  } catch { ElMessage.error('设置失败') }
}

async function handleCancel(inst: any) {
  const res = await ElMessageBox.prompt('请填写取消原因', '取消课程', { inputPlaceholder: '如: 教师请假' }).catch(() => null)
  if (!res?.value) return
  try {
    await instanceApi.cancel(inst.id, res.value)
    ElMessage.success('已取消'); loadInstances()
  } catch { ElMessage.error('操作失败') }
}

async function handleRestore(inst: any) {
  await ElMessageBox.confirm('确定恢复此课程为正常状态？', '确认')
  try {
    await instanceApi.restore(inst.id)
    ElMessage.success('已恢复'); loadInstances()
  } catch { ElMessage.error('操作失败') }
}

async function loadOptions() {
  try { const r = await request.get('/students/classes'); const d = (r as any).data || r; classList.value = (Array.isArray(d) ? d : d.records || []).map((c: any) => ({ id: c.id, name: c.className || c.name })) } catch { classList.value = [] }
  try { const r = await request.get('/users', { params: { role: 'TEACHER' } }); const d = (r as any).data || r; teacherList.value = (Array.isArray(d) ? d : d.records || []).map((t: any) => ({ id: t.id, name: t.realName || t.username })) } catch { teacherList.value = [] }
  try { const r = await request.get('/places', { params: { roomType: 'CLASSROOM' } }); const d = (r as any).data || r; classroomList.value = (Array.isArray(d) ? d : d.records || []).map((p: any) => ({ id: p.id, name: p.placeName || p.name })) } catch { classroomList.value = [] }
}

onMounted(loadOptions)
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
