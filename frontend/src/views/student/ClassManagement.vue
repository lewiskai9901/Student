<template>
  <div class="tm-page">
    <div class="tm-header">
      <div>
        <h1 class="tm-title">班级管理</h1>
        <div class="tm-stats">
          <span>共 <b>{{ classes.length }}</b> 个班级</span>
          <span class="sep" />
          <span>在读 <b>{{ classes.filter(c => c.status === 1).length }}</b></span>
          <span class="sep" />
          <span>总人数 <b>{{ classes.reduce((s, c) => s + (c.studentCount || 0), 0) }}</b></span>
        </div>
      </div>
    </div>

    <div class="tm-filters">
      <div class="tm-search" style="flex: 0 0 200px;">
        <svg class="tm-search-icon" width="15" height="15" viewBox="0 0 15 15" fill="none"><circle cx="6.5" cy="6.5" r="5" stroke="currentColor" stroke-width="1.5"/><path d="m10 10 4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        <input v-model="keyword" class="tm-search-input" placeholder="搜索班级..." @keyup.enter="loadClasses" />
      </div>
      <select v-model="filterGrade" class="tm-select" @change="loadClasses">
        <option :value="undefined">全部年级</option>
        <option v-for="g in grades" :key="g.id" :value="g.id">{{ g.gradeName }}</option>
      </select>
      <span style="flex: 1;" />
      <span style="font-size: 12px; color: #9ca3af;">班级由组织管理创建，此处管理行业扩展属性</span>
    </div>

    <div class="tm-table-wrap">
      <table class="tm-table">
        <colgroup>
          <col /><col style="width: 100px" /><col style="width: 100px" />
          <col style="width: 100px" /><col style="width: 90px" /><col style="width: 70px" />
          <col style="width: 80px" /><col style="width: 80px" />
        </colgroup>
        <thead>
          <tr>
            <th class="text-left">班级名称</th>
            <th>所属年级</th>
            <th>专业</th>
            <th>班主任</th>
            <th>入学年份</th>
            <th>人数</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8" class="tm-empty"><span class="tm-spin" style="display:inline-block;width:16px;height:16px;border:2px solid #e5e7eb;border-top-color:#2563eb;border-radius:50%;" /> 加载中...</td>
          </tr>
          <tr v-else-if="filteredClasses.length === 0">
            <td colspan="8" class="tm-empty">暂无班级数据。请先在组织管理中创建类型为"班级"的节点。</td>
          </tr>
          <tr v-for="cls in filteredClasses" :key="cls.id">
            <td class="text-left" style="font-weight: 500;">
              {{ cls.className }}
              <span style="font-size: 11px; color: #9ca3af; margin-left: 4px;">{{ cls.classCode }}</span>
            </td>
            <td>{{ cls.gradeName || '-' }}</td>
            <td>{{ cls.majorName || '-' }}</td>
            <td>{{ cls.teacherName || '-' }}</td>
            <td class="tm-mono">{{ cls.enrollmentYear || '-' }}</td>
            <td class="tm-mono">{{ cls.studentCount || 0 }}</td>
            <td>
              <span :class="['tm-chip', cls.status === 1 ? 'tm-chip-green' : 'tm-chip-gray']">
                {{ cls.status === 1 ? '在读' : cls.status === 2 ? '毕业' : '未知' }}
              </span>
            </td>
            <td>
              <button class="tm-action" @click="showEditDrawer(cls)">编辑</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Edit Drawer -->
    <Transition name="tm-drawer">
      <div v-if="editVisible" class="tm-drawer-overlay" @click.self="editVisible = false">
        <div class="tm-drawer">
          <div class="tm-drawer-header">
            <h3 class="tm-drawer-title">编辑班级信息 — {{ editForm.className }}</h3>
            <button class="tm-drawer-close" @click="editVisible = false">&times;</button>
          </div>
          <div class="tm-drawer-body">
            <div class="tm-section">
              <h4 class="tm-section-title">组织信息（只读）</h4>
              <div class="tm-fields tm-cols-2">
                <div class="tm-field">
                  <label class="tm-label">班级名称</label>
                  <input :value="editForm.className" class="tm-input" disabled style="background: #f3f4f6;" />
                </div>
                <div class="tm-field">
                  <label class="tm-label">编码</label>
                  <input :value="editForm.classCode" class="tm-input" disabled style="background: #f3f4f6;" />
                </div>
              </div>
              <p style="font-size: 11px; color: #9ca3af; margin-top: 4px;">名称和编码在组织管理中修改</p>
            </div>

            <div class="tm-section">
              <h4 class="tm-section-title">行业扩展属性</h4>
              <div class="tm-fields tm-cols-2">
                <div class="tm-field">
                  <label class="tm-label">所属年级</label>
                  <select v-model="editForm.gradeId" class="tm-field-select">
                    <option :value="undefined">未关联</option>
                    <option v-for="g in grades" :key="g.id" :value="g.id">{{ g.gradeName }}</option>
                  </select>
                </div>
                <div class="tm-field">
                  <label class="tm-label">专业</label>
                  <select v-model="editForm.majorId" class="tm-field-select">
                    <option :value="undefined">未关联</option>
                    <option v-for="m in majors" :key="m.id" :value="m.id">{{ m.majorName }}</option>
                  </select>
                </div>
              </div>
              <div class="tm-fields tm-cols-2">
                <div class="tm-field">
                  <label class="tm-label">班主任</label>
                  <select v-model="editForm.teacherId" class="tm-field-select">
                    <option :value="undefined">未分配</option>
                    <option v-for="t in teachers" :key="t.id" :value="t.id">{{ t.name }}</option>
                  </select>
                </div>
                <div class="tm-field">
                  <label class="tm-label">入学年份</label>
                  <input v-model.number="editForm.enrollmentYear" type="number" min="2020" max="2030" class="tm-input" />
                </div>
              </div>
              <div class="tm-fields tm-cols-2">
                <div class="tm-field">
                  <label class="tm-label">学制（年）</label>
                  <input v-model.number="editForm.duration" type="number" min="1" max="6" class="tm-input" />
                </div>
                <div class="tm-field">
                  <label class="tm-label">班级类型</label>
                  <select v-model="editForm.classType" class="tm-field-select">
                    <option :value="1">普通班</option>
                    <option :value="2">重点班</option>
                    <option :value="3">实验班</option>
                  </select>
                </div>
              </div>
            </div>
          </div>
          <div class="tm-drawer-footer">
            <button class="tm-btn tm-btn-secondary" @click="editVisible = false">取消</button>
            <button class="tm-btn tm-btn-primary" :disabled="saving" @click="saveEdit">{{ saving ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'

const classes = ref<any[]>([])
const grades = ref<any[]>([])
const majors = ref<any[]>([])
const teachers = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')
const filterGrade = ref<number>()
const editVisible = ref(false)
const saving = ref(false)
const editForm = ref<any>({})

const filteredClasses = computed(() => {
  let list = classes.value
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(c => c.className?.toLowerCase().includes(kw) || c.classCode?.toLowerCase().includes(kw))
  }
  if (filterGrade.value) {
    list = list.filter(c => c.gradeId === filterGrade.value)
  }
  return list
})

async function loadClasses() {
  loading.value = true
  try {
    const res = await http.get('/students/classes')
    const data = (res as any).data || res
    classes.value = Array.isArray(data) ? data : data.records || []
  } catch { classes.value = [] } finally { loading.value = false }
}

async function loadGrades() {
  try {
    const res = await http.get('/students/grades')
    const data = (res as any).data || res
    grades.value = Array.isArray(data) ? data : data.records || []
  } catch { grades.value = [] }
}

async function loadMajors() {
  try {
    const res = await http.get('/academic/majors')
    const data = (res as any).data || res
    majors.value = Array.isArray(data) ? data : data.records || []
  } catch { majors.value = [] }
}

async function loadTeachers() {
  try {
    const res = await http.get('/users', { params: { role: 'TEACHER', pageSize: 500 } })
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    teachers.value = items.map((t: any) => ({ id: t.id, name: t.realName || t.username }))
  } catch { teachers.value = [] }
}

function showEditDrawer(cls: any) {
  editForm.value = { ...cls }
  editVisible.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    await http.put(`/students/classes/${editForm.value.id}`, {
      gradeId: editForm.value.gradeId,
      majorId: editForm.value.majorId,
      teacherId: editForm.value.teacherId,
      enrollmentYear: editForm.value.enrollmentYear,
      duration: editForm.value.duration,
      classType: editForm.value.classType,
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadClasses()
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

onMounted(() => { loadClasses(); loadGrades(); loadMajors(); loadTeachers() })
</script>

<style>
@import '@/styles/teaching-ui.css';
</style>
