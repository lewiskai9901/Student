<template>
  <!-- Stats bar -->
  <div class="tm-filters">
    <span style="font-size: 12.5px; color: #6b7280;">教学班总数 <b>{{ teachingClasses.length }}</b></span>
    <i class="tm-sep" />
    <span style="font-size: 12.5px; color: #6b7280;">普通 <b>{{ tcNormalCount }}</b></span>
    <i class="tm-sep" />
    <span style="font-size: 12.5px; color: #6b7280;">合堂 <b>{{ tcCombinedCount }}</b></span>
    <i class="tm-sep" />
    <span style="font-size: 12.5px; color: #6b7280;">走班 <b>{{ tcWalkingCount }}</b></span>
  </div>

  <div class="tm-table-wrap">
    <!-- Action bar -->
    <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
      <button class="tm-btn tm-btn-secondary" @click="autoGenerateTeachingClasses">自动生成教学班</button>
      <button class="tm-btn tm-btn-primary" @click="showTeachingClassDialog()">手动创建</button>
    </div>

    <!-- Table -->
    <table class="tm-table">
      <colgroup>
        <col />
        <col style="width: 80px" />
        <col style="width: 130px" />
        <col />
        <col style="width: 80px" />
        <col style="width: 70px" />
        <col style="width: 150px" />
      </colgroup>
      <thead>
        <tr>
          <th class="text-left">教学班名称</th>
          <th>类型</th>
          <th>课程</th>
          <th class="text-left">包含班级</th>
          <th>学生数</th>
          <th>周课时</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="tcLoading">
          <td colspan="7" class="tm-empty">
            <span class="tm-spin" style="display:inline-block;width:16px;height:16px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...
          </td>
        </tr>
        <tr v-else-if="teachingClasses.length === 0">
          <td colspan="7" class="tm-empty">暂无教学班，可点击"自动生成教学班"快速创建</td>
        </tr>
        <tr v-for="row in teachingClasses" :key="row.id">
          <td class="text-left">{{ row.className }}</td>
          <td>
            <span :class="['tm-chip', { 1: 'tm-chip-gray', 2: 'tm-chip-amber', 3: 'tm-chip-green' }[row.classType] || 'tm-chip-gray']">
              {{ getClassTypeName(row.classType) }}
            </span>
          </td>
          <td>{{ row.courseName }}</td>
          <td class="text-left" style="white-space: normal !important;">
            <template v-if="row.members?.length">
              {{ row.members.filter((m: any) => m.memberType === 1).map((m: any) => m.adminClassName).join(', ') || '-' }}
            </template>
            <span v-else style="color: #9ca3af; font-size: 12px;">-</span>
          </td>
          <td class="tm-mono">{{ row.studentCount }}</td>
          <td class="tm-mono">{{ row.weeklyHours }}</td>
          <td>
            <button class="tm-action" @click="showTeachingClassDialog(row)">编辑</button>
            <button class="tm-action" @click="showMembersDialog(row)">成员</button>
            <button class="tm-action tm-action-danger" @click="deleteTeachingClass(row)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- Teaching Class Create/Edit Drawer -->
  <Transition name="tm-drawer">
    <div v-if="tcDialogVisible" class="tm-drawer-overlay" @click.self="tcDialogVisible = false">
      <div class="tm-drawer">
        <div class="tm-drawer-header">
          <h3 class="tm-drawer-title">{{ editingTC ? '编辑教学班' : '创建教学班' }}</h3>
          <button class="tm-drawer-close" @click="tcDialogVisible = false">&times;</button>
        </div>
        <div class="tm-drawer-body">
          <div class="tm-section">
            <h4 class="tm-section-title">基本信息</h4>
            <div class="tm-field" :class="{ 'tm-error': tcErrors.className }">
              <label class="tm-label">教学班名称 <span class="req">*</span></label>
              <input v-model="tcForm.className" class="tm-input" placeholder="如: 高一(1,2)班数学" />
            </div>
            <div class="tm-field" :class="{ 'tm-error': tcErrors.courseId }">
              <label class="tm-label">课程 <span class="req">*</span></label>
              <select v-model="tcForm.courseId" class="tm-field-select">
                <option :value="undefined" disabled>选择课程</option>
                <option v-for="c in allCourses" :key="c.id" :value="c.id">{{ c.courseCode }} - {{ c.courseName }}</option>
              </select>
            </div>
            <div class="tm-field">
              <label class="tm-label">教学班类型 <span class="req">*</span></label>
              <div class="tm-radios">
                <label :class="['tm-radio', { active: tcForm.classType === 1 }]" @click="tcForm.classType = 1"><input type="radio" />普通</label>
                <label :class="['tm-radio', { active: tcForm.classType === 2 }]" @click="tcForm.classType = 2"><input type="radio" />合堂</label>
                <label :class="['tm-radio', { active: tcForm.classType === 3 }]" @click="tcForm.classType = 3"><input type="radio" />走班</label>
              </div>
            </div>
            <div class="tm-fields tm-cols-3">
              <div class="tm-field">
                <label class="tm-label">周课时 <span class="req">*</span></label>
                <input v-model.number="tcForm.weeklyHours" type="number" min="1" max="20" class="tm-input" />
              </div>
              <div class="tm-field">
                <label class="tm-label">起始周 <span class="req">*</span></label>
                <input v-model.number="tcForm.startWeek" type="number" min="1" max="30" class="tm-input" />
              </div>
              <div class="tm-field">
                <label class="tm-label">结束周</label>
                <input v-model.number="tcForm.endWeek" type="number" :min="tcForm.startWeek" max="30" class="tm-input" />
              </div>
            </div>
            <div class="tm-field">
              <label class="tm-label">教室类型</label>
              <input v-model="tcForm.requiredRoomType" class="tm-input" placeholder="如: 多媒体教室、实验室" />
            </div>
            <div class="tm-field">
              <label class="tm-label">备注</label>
              <textarea v-model="tcForm.remark" class="tm-textarea" rows="2"></textarea>
            </div>
          </div>
        </div>
        <div class="tm-drawer-footer">
          <button class="tm-btn tm-btn-secondary" @click="tcDialogVisible = false">取消</button>
          <button class="tm-btn tm-btn-primary" :disabled="tcSaving" @click="saveTeachingClass">
            {{ tcSaving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Members Drawer -->
  <Transition name="tm-drawer">
    <div v-if="membersDialogVisible" class="tm-drawer-overlay" @click.self="membersDialogVisible = false">
      <div class="tm-drawer" style="width: 580px;">
        <div class="tm-drawer-header">
          <h3 class="tm-drawer-title">教学班成员</h3>
          <button class="tm-drawer-close" @click="membersDialogVisible = false">&times;</button>
        </div>
        <div class="tm-drawer-body">
          <div class="tm-section">
            <div v-if="selectedTC" style="font-size: 13px; color: #6b7280; margin-bottom: 12px;">
              {{ selectedTC.className }} - {{ selectedTC.courseName }}
              <span style="margin-left: 8px;">( {{ selectedTC.studentCount }} 人 )</span>
            </div>

            <!-- Add member -->
            <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 16px;">
              <select v-model="newMemberClassId" class="tm-field-select" style="flex: 1;">
                <option :value="''" disabled>选择行政班级</option>
                <option v-for="c in classes" :key="c.id" :value="c.id">{{ c.className }}</option>
              </select>
              <button class="tm-btn tm-btn-primary" style="padding: 8px 12px;" :disabled="!newMemberClassId" @click="addClassMember">添加整班</button>
            </div>

            <!-- Members table -->
            <table class="tm-table">
              <colgroup>
                <col style="width: 80px" />
                <col />
                <col style="width: 70px" />
              </colgroup>
              <thead>
                <tr>
                  <th>类型</th>
                  <th class="text-left">名称</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="membersLoading">
                  <td colspan="3" class="tm-empty">加载中...</td>
                </tr>
                <tr v-else-if="currentMembers.length === 0">
                  <td colspan="3" class="tm-empty">暂无成员</td>
                </tr>
                <tr v-for="row in currentMembers" :key="row.id">
                  <td>
                    <span :class="['tm-chip', row.memberType === 1 ? 'tm-chip-blue' : 'tm-chip-green']">
                      {{ row.memberType === 1 ? '整班' : '个人' }}
                    </span>
                  </td>
                  <td class="text-left">{{ row.memberType === 1 ? row.adminClassName : row.studentName }}</td>
                  <td>
                    <button class="tm-action tm-action-danger" @click="removeMember(row)">移除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { teachingClassApi } from '@/api/teaching'
import { courseApi } from '@/api/academic'
import { schoolClassApi } from '@/api/organization'
import type { TeachingClass, TeachingClassMember, Course } from '@/types/teaching'
import type { SchoolClass } from '@/types/organization'

const props = defineProps<{
  semesterId: number | string | null
}>()

// Data
const teachingClasses = ref<TeachingClass[]>([])
const tcLoading = ref(false)
const allCourses = ref<Course[]>([])
const classes = ref<SchoolClass[]>([])

// Computed
const tcNormalCount = computed(() => teachingClasses.value.filter(t => t.classType === 1).length)
const tcCombinedCount = computed(() => teachingClasses.value.filter(t => t.classType === 2).length)
const tcWalkingCount = computed(() => teachingClasses.value.filter(t => t.classType === 3).length)

// Dialog state
const tcDialogVisible = ref(false)
const tcSaving = ref(false)
const editingTC = ref<TeachingClass | null>(null)
const tcErrors = reactive({ className: false, courseId: false })

const tcForm = ref({
  className: '',
  courseId: undefined as number | undefined,
  classType: 1 as 1 | 2 | 3,
  weeklyHours: 2,
  startWeek: 1,
  endWeek: undefined as number | undefined,
  requiredRoomType: '',
  remark: '',
})

// Members dialog
const membersDialogVisible = ref(false)
const membersLoading = ref(false)
const selectedTC = ref<TeachingClass | null>(null)
const currentMembers = ref<TeachingClassMember[]>([])
const newMemberClassId = ref<number | string>('')

// Helpers
function getClassTypeName(type: number) {
  const map: Record<number, string> = { 1: '普通', 2: '合堂', 3: '走班' }
  return map[type] || '-'
}

// Data loading
async function loadCourses() {
  try { allCourses.value = await courseApi.listAll() } catch { /* */ }
}

async function loadClasses() {
  try { classes.value = await schoolClassApi.getAll() } catch { /* */ }
}

async function loadTeachingClasses() {
  if (!props.semesterId) { teachingClasses.value = []; return }
  tcLoading.value = true
  try {
    teachingClasses.value = await teachingClassApi.list(props.semesterId)
  } catch {
    ElMessage.error('加载教学班列表失败')
    teachingClasses.value = []
  } finally {
    tcLoading.value = false
  }
}

// Teaching Class CRUD
function validateTC() {
  tcErrors.className = !tcForm.value.className?.trim()
  tcErrors.courseId = !tcForm.value.courseId
  return !Object.values(tcErrors).some(Boolean)
}

function showTeachingClassDialog(row?: TeachingClass) {
  Object.keys(tcErrors).forEach(k => (tcErrors as any)[k] = false)
  editingTC.value = row || null
  tcForm.value = row
    ? { className: row.className, courseId: row.courseId, classType: row.classType, weeklyHours: row.weeklyHours, startWeek: row.startWeek, endWeek: row.endWeek, requiredRoomType: row.requiredRoomType || '', remark: row.remark || '' }
    : { className: '', courseId: undefined, classType: 1, weeklyHours: 2, startWeek: 1, endWeek: undefined, requiredRoomType: '', remark: '' }
  tcDialogVisible.value = true
}

async function saveTeachingClass() {
  if (!validateTC()) return
  tcSaving.value = true
  try {
    const payload: Partial<TeachingClass> = {
      semesterId: props.semesterId, className: tcForm.value.className, courseId: tcForm.value.courseId,
      classType: tcForm.value.classType, weeklyHours: tcForm.value.weeklyHours, startWeek: tcForm.value.startWeek,
      endWeek: tcForm.value.endWeek, requiredRoomType: tcForm.value.requiredRoomType || undefined, remark: tcForm.value.remark || undefined,
    }
    if (editingTC.value) {
      await teachingClassApi.update(editingTC.value.id, payload); ElMessage.success('更新成功')
    } else {
      await teachingClassApi.create(payload); ElMessage.success('创建成功')
    }
    tcDialogVisible.value = false
    loadTeachingClasses()
  } catch { ElMessage.error('保存失败') } finally { tcSaving.value = false }
}

async function deleteTeachingClass(row: TeachingClass) {
  try {
    await ElMessageBox.confirm('确定删除该教学班?', '删除确认', { type: 'warning' })
    await teachingClassApi.delete(row.id); ElMessage.success('已删除'); loadTeachingClasses()
  } catch { /* */ }
}

async function autoGenerateTeachingClasses() {
  if (!props.semesterId) { ElMessage.warning('请先选择学期'); return }
  try {
    await ElMessageBox.confirm('将根据已确认的班级课程分配自动生成教学班。合堂课程会自动合并，走班课程会单独建班。是否继续?', '自动生成', { type: 'info' })
    tcLoading.value = true
    await teachingClassApi.autoGenerate(props.semesterId); ElMessage.success('自动生成完成'); loadTeachingClasses()
  } catch { /* */ } finally { tcLoading.value = false }
}

// Members
async function showMembersDialog(row: TeachingClass) {
  selectedTC.value = row; membersDialogVisible.value = true; newMemberClassId.value = ''; await loadMembers(row.id)
}

async function loadMembers(tcId: number) {
  membersLoading.value = true
  try { currentMembers.value = await teachingClassApi.getMembers(tcId) } catch { currentMembers.value = [] } finally { membersLoading.value = false }
}

async function addClassMember() {
  if (!selectedTC.value || !newMemberClassId.value) return
  try {
    await teachingClassApi.addMembers(selectedTC.value.id, [{ teachingClassId: selectedTC.value.id, memberType: 1, adminClassId: Number(newMemberClassId.value) }])
    ElMessage.success('已添加'); newMemberClassId.value = ''; loadMembers(selectedTC.value.id); loadTeachingClasses()
  } catch { ElMessage.error('添加失败') }
}

async function removeMember(row: TeachingClassMember) {
  if (!selectedTC.value) return
  try {
    await ElMessageBox.confirm('确定移除该成员?', '确认', { type: 'warning' })
    await teachingClassApi.removeMembers(selectedTC.value.id, [row.id]); ElMessage.success('已移除'); loadMembers(selectedTC.value.id); loadTeachingClasses()
  } catch { /* */ }
}

watch(() => props.semesterId, () => { loadTeachingClasses() })
onMounted(() => { loadCourses(); loadClasses(); loadTeachingClasses() })
</script>

<style>
@import '@/styles/teaching-ui.css';
.tm-sep { display: inline-block; width: 1px; height: 10px; background: #d1d5db; vertical-align: middle; margin: 0 4px; }
</style>
