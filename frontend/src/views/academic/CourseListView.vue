<template>
  <div class="course-page">
    <!-- Header -->
    <header class="course-header">
      <div class="header-left">
        <h1 class="page-title">课程目录</h1>
        <div class="stats-row">
          <span class="stat">共 <b>{{ total }}</b> 门</span>
          <i class="stat-sep" />
          <span class="stat">必修 <b class="c-required">{{ natureCount.required }}</b></span>
          <i class="stat-sep" />
          <span class="stat">限选 <b class="c-limited">{{ natureCount.limited }}</b></span>
          <i class="stat-sep" />
          <span class="stat">任选 <b class="c-free">{{ natureCount.free }}</b></span>
          <i class="stat-sep" />
          <span class="stat">
            <em class="dot dot-active" /> 启用 {{ activeCount }}
          </span>
          <i class="stat-sep" />
          <span class="stat">
            <em class="dot dot-inactive" /> 停用 {{ inactiveCount }}
          </span>
        </div>
      </div>
      <button class="btn-create" @click="showDrawer()">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M7 1v12M1 7h12" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
        新建课程
      </button>
    </header>

    <!-- Filters -->
    <div class="filter-bar">
      <div class="search-box">
        <svg class="search-icon" width="15" height="15" viewBox="0 0 15 15" fill="none"><circle cx="6.5" cy="6.5" r="5" stroke="currentColor" stroke-width="1.5"/><path d="m10 10 4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        <input
          v-model="queryParams.keyword"
          placeholder="搜索课程名称或编码..."
          class="search-input"
          @keyup.enter="search"
        />
      </div>
      <div class="filter-group">
        <select v-model="queryParams.courseCategory" class="filter-select" @change="search">
          <option :value="undefined">全部类别</option>
          <option :value="1">公共基础</option>
          <option :value="2">专业核心</option>
          <option :value="3">专业方向</option>
          <option :value="4">选修课</option>
        </select>
        <select v-model="queryParams.courseType" class="filter-select" @change="search">
          <option :value="undefined">全部类型</option>
          <option :value="1">理论</option>
          <option :value="2">实践</option>
          <option :value="3">理论+实践</option>
        </select>
        <select v-model="queryParams.status" class="filter-select" @change="search">
          <option :value="undefined">全部状态</option>
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </select>
        <button v-if="hasFilters" class="btn-reset" @click="resetQuery">清除筛选</button>
      </div>
    </div>

    <!-- Course Table -->
    <div class="table-container" v-loading="loading">
      <table class="course-table">
        <colgroup>
          <col style="width: 110px" />
          <col />
          <col style="width: 90px" />
          <col style="width: 60px" />
          <col style="width: 65px" />
          <col style="width: 65px" />
          <col style="width: 95px" />
          <col style="width: 65px" />
          <col style="width: 60px" />
          <col style="width: 85px" />
          <col style="width: 110px" />
        </colgroup>
        <thead>
          <tr>
            <th>编码</th>
            <th>课程名称</th>
            <th>类别</th>
            <th>性质</th>
            <th>学分</th>
            <th>总学时</th>
            <th>理论/实践</th>
            <th>周学时</th>
            <th>考核</th>
            <th>状态</th>
            <th class="text-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in courses" :key="row.id" class="course-row" @click="showDrawer(row)">
            <td>
              <code class="code-badge">{{ row.courseCode }}</code>
            </td>
            <td class="cell-name">
              <span class="name-text">{{ row.courseName }}</span>
              <span v-if="row.courseNameEn" class="name-en">{{ row.courseNameEn }}</span>
            </td>
            <td>
              <span class="category-chip" :class="'cat-' + row.courseCategory">
                {{ getCourseCategoryName(row.courseCategory) }}
              </span>
            </td>
            <td>
              <span class="nature-label" :class="'nat-' + row.courseNature">
                {{ getCourseNatureName(row.courseNature) }}
              </span>
            </td>
            <td class="mono">{{ row.credits }}</td>
            <td class="mono">{{ row.totalHours }}</td>
            <td class="mono">
              <span class="hour-theory">{{ row.theoryHours ?? 0 }}</span>
              <span class="hour-sep">/</span>
              <span class="hour-practice">{{ row.practiceHours ?? 0 }}</span>
            </td>
            <td class="mono">{{ row.weeklyHours }}</td>
            <td>{{ row.examType === 1 ? '考试' : '考查' }}</td>
            <td @click.stop>
              <button
                class="status-toggle"
                :class="row.status === 1 ? 'is-active' : 'is-inactive'"
                @click="toggleStatus(row)"
              >
                <em class="toggle-dot" />
                {{ row.status === 1 ? '启用' : '停用' }}
              </button>
            </td>
            <td class="text-right" @click.stop>
              <button class="action-btn" @click="showDrawer(row)">编辑</button>
              <button class="action-btn action-danger" @click="handleDelete(row)">删除</button>
            </td>
          </tr>
          <tr v-if="!loading && courses.length === 0">
            <td colspan="11" class="empty-cell">暂无课程数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination-bar" v-if="total > 0">
      <span class="page-info">
        显示 {{ (queryParams.pageNum! - 1) * queryParams.pageSize! + 1 }}-{{ Math.min(queryParams.pageNum! * queryParams.pageSize!, total) }}，共 {{ total }} 条
      </span>
      <div class="page-controls">
        <select v-model="queryParams.pageSize" class="page-size-select" @change="search">
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
        <button class="page-btn" :disabled="queryParams.pageNum === 1" @click="queryParams.pageNum!--; loadCourses()">
          <svg width="12" height="12" viewBox="0 0 12 12"><path d="M7.5 2.5 4 6l3.5 3.5" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>
        <span class="page-current">{{ queryParams.pageNum }}</span>
        <button class="page-btn" :disabled="queryParams.pageNum! * queryParams.pageSize! >= total" @click="queryParams.pageNum!++; loadCourses()">
          <svg width="12" height="12" viewBox="0 0 12 12"><path d="M4.5 2.5 8 6l-3.5 3.5" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>
      </div>
    </div>

    <!-- Course Drawer (side panel) -->
    <Teleport to="body">
      <Transition name="drawer">
        <div v-if="drawerVisible" class="drawer-overlay" @click.self="drawerVisible = false">
          <div class="drawer-panel">
            <div class="drawer-header">
              <h2 class="drawer-title">{{ form.id ? '编辑课程' : '新建课程' }}</h2>
              <button class="drawer-close" @click="drawerVisible = false">
                <svg width="18" height="18" viewBox="0 0 18 18"><path d="M4.5 4.5l9 9M13.5 4.5l-9 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              </button>
            </div>

            <div class="drawer-body">
              <!-- Section 1: 基本信息 -->
              <section class="form-section">
                <h3 class="section-title">基本信息</h3>
                <div class="field-grid cols-2">
                  <div class="field" :class="{ 'has-error': errors.courseCode }">
                    <label class="field-label">课程编码 <span class="req">*</span></label>
                    <input v-model="form.courseCode" placeholder="如 CS101" class="field-input" />
                  </div>
                  <div class="field" :class="{ 'has-error': errors.courseName }">
                    <label class="field-label">课程名称 <span class="req">*</span></label>
                    <input v-model="form.courseName" placeholder="课程名称" class="field-input" />
                  </div>
                </div>
                <div class="field">
                  <label class="field-label">英文名称</label>
                  <input v-model="form.courseNameEn" placeholder="English Name (optional)" class="field-input" />
                </div>
                <div class="field-grid cols-3">
                  <div class="field" :class="{ 'has-error': errors.courseCategory }">
                    <label class="field-label">课程类别 <span class="req">*</span></label>
                    <select v-model="form.courseCategory" class="field-select">
                      <option :value="undefined" disabled>选择类别</option>
                      <option :value="1">公共基础课</option>
                      <option :value="2">专业核心课</option>
                      <option :value="3">专业方向课</option>
                      <option :value="4">选修课</option>
                    </select>
                  </div>
                  <div class="field" :class="{ 'has-error': errors.courseType }">
                    <label class="field-label">课程类型 <span class="req">*</span></label>
                    <select v-model="form.courseType" class="field-select">
                      <option :value="undefined" disabled>选择类型</option>
                      <option :value="1">理论</option>
                      <option :value="2">实践</option>
                      <option :value="3">理论+实践</option>
                    </select>
                  </div>
                  <div class="field" :class="{ 'has-error': errors.courseNature }">
                    <label class="field-label">课程性质 <span class="req">*</span></label>
                    <div class="radio-group">
                      <label class="radio-item" :class="{ active: form.courseNature === 1 }">
                        <input type="radio" :value="1" v-model="form.courseNature" /> 必修
                      </label>
                      <label class="radio-item" :class="{ active: form.courseNature === 2 }">
                        <input type="radio" :value="2" v-model="form.courseNature" /> 限选
                      </label>
                      <label class="radio-item" :class="{ active: form.courseNature === 3 }">
                        <input type="radio" :value="3" v-model="form.courseNature" /> 任选
                      </label>
                    </div>
                  </div>
                </div>
              </section>

              <!-- Section 2: 学时学分 -->
              <section class="form-section">
                <h3 class="section-title">学时与学分</h3>
                <div class="field-grid cols-4">
                  <div class="field" :class="{ 'has-error': errors.credits }">
                    <label class="field-label">学分 <span class="req">*</span></label>
                    <input v-model.number="form.credits" type="number" min="0.5" max="20" step="0.5" class="field-input mono" />
                  </div>
                  <div class="field" :class="{ 'has-error': errors.totalHours }">
                    <label class="field-label">总学时 <span class="req">*</span></label>
                    <input v-model.number="form.totalHours" type="number" min="1" max="500" class="field-input mono" />
                  </div>
                  <div class="field">
                    <label class="field-label">理论学时</label>
                    <input v-model.number="form.theoryHours" type="number" min="0" class="field-input mono" />
                  </div>
                  <div class="field">
                    <label class="field-label">实践学时</label>
                    <input v-model.number="form.practiceHours" type="number" min="0" class="field-input mono" />
                  </div>
                </div>
                <div class="field-grid cols-2">
                  <div class="field">
                    <label class="field-label">周学时</label>
                    <input v-model.number="form.weeklyHours" type="number" min="0" max="30" class="field-input mono" />
                  </div>
                  <div class="field">
                    <label class="field-label">考核方式</label>
                    <div class="radio-group">
                      <label class="radio-item" :class="{ active: form.examType === 1 }">
                        <input type="radio" :value="1" v-model="form.examType" /> 考试
                      </label>
                      <label class="radio-item" :class="{ active: form.examType === 2 }">
                        <input type="radio" :value="2" v-model="form.examType" /> 考查
                      </label>
                    </div>
                  </div>
                </div>
                <!-- Hours visual bar -->
                <div v-if="form.totalHours" class="hours-visual">
                  <div class="hours-bar">
                    <div class="hours-theory" :style="{ width: theoryPercent + '%' }">
                      <span v-if="theoryPercent > 15">理论 {{ form.theoryHours || 0 }}h</span>
                    </div>
                    <div class="hours-practice" :style="{ width: practicePercent + '%' }">
                      <span v-if="practicePercent > 15">实践 {{ form.practiceHours || 0 }}h</span>
                    </div>
                  </div>
                </div>
              </section>

              <!-- Section 3: 归属与描述 -->
              <section class="form-section">
                <h3 class="section-title">归属与描述</h3>
                <div class="field">
                  <label class="field-label">开课部门</label>
                  <el-tree-select
                    v-model="form.orgUnitId"
                    :data="orgTree"
                    :props="{ label: 'unitName', value: 'id', children: 'children' }"
                    placeholder="选择开课组织单元"
                    clearable
                    filterable
                    check-strictly
                    class="w-full"
                  />
                </div>
                <div class="field">
                  <label class="field-label">课程描述</label>
                  <textarea v-model="form.description" rows="4" placeholder="课程简介、教学目标、先修要求等..." class="field-textarea" />
                </div>
              </section>
            </div>

            <div class="drawer-footer">
              <button class="btn-cancel" @click="drawerVisible = false">取消</button>
              <button class="btn-save" :disabled="saving" @click="saveCourse">
                <svg v-if="saving" class="spin" width="14" height="14" viewBox="0 0 14 14"><circle cx="7" cy="7" r="5.5" stroke="currentColor" stroke-width="1.5" fill="none" stroke-dasharray="20 12" /></svg>
                {{ saving ? '保存中...' : '保存课程' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { courseApi } from '@/api/academic'
import { orgUnitApi } from '@/api/organization'
import type { Course, CourseQueryParams } from '@/types/academic'

const loading = ref(false)
const saving = ref(false)
const drawerVisible = ref(false)
const courses = ref<Course[]>([])
const total = ref(0)
const orgTree = ref<any[]>([])

const queryParams = reactive<CourseQueryParams>({
  keyword: '',
  courseCategory: undefined,
  courseType: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 20,
})

const form = ref<Partial<Course>>({})
const errors = reactive<Record<string, boolean>>({})

const activeCount = computed(() => courses.value.filter(c => c.status === 1).length)
const inactiveCount = computed(() => courses.value.filter(c => c.status === 0).length)
const natureCount = computed(() => ({
  required: courses.value.filter(c => c.courseNature === 1).length,
  limited: courses.value.filter(c => c.courseNature === 2).length,
  free: courses.value.filter(c => c.courseNature === 3).length,
}))
const hasFilters = computed(() =>
  queryParams.keyword || queryParams.courseCategory || queryParams.courseType || queryParams.status !== undefined
)
const theoryPercent = computed(() => {
  if (!form.value.totalHours) return 0
  return Math.round(((form.value.theoryHours || 0) / form.value.totalHours) * 100)
})
const practicePercent = computed(() => {
  if (!form.value.totalHours) return 0
  return Math.round(((form.value.practiceHours || 0) / form.value.totalHours) * 100)
})

const loadCourses = async () => {
  loading.value = true
  try {
    const res: any = await courseApi.list(queryParams)
    const data = res.data || res
    courses.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error('Failed to load courses:', error)
  } finally {
    loading.value = false
  }
}

const loadOrgTree = async () => {
  try {
    const res: any = await orgUnitApi.getTree()
    orgTree.value = (res as any)?.data || res || []
  } catch { /* ignore */ }
}

const search = () => { queryParams.pageNum = 1; loadCourses() }

const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.courseCategory = undefined
  queryParams.courseType = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  loadCourses()
}

const showDrawer = (course?: Course) => {
  Object.keys(errors).forEach(k => errors[k] = false)
  form.value = course
    ? { ...course }
    : {
        credits: 2, totalHours: 32, theoryHours: 24, practiceHours: 8,
        weeklyHours: 2, courseCategory: 1, courseType: 1, courseNature: 1,
        examType: 1, status: 1,
      }
  drawerVisible.value = true
}

const validate = () => {
  const f = form.value
  errors.courseCode = !f.courseCode?.trim()
  errors.courseName = !f.courseName?.trim()
  errors.courseCategory = !f.courseCategory
  errors.courseType = !f.courseType
  errors.courseNature = !f.courseNature
  errors.credits = !f.credits
  errors.totalHours = !f.totalHours
  return !Object.values(errors).some(Boolean)
}

const saveCourse = async () => {
  if (!validate()) return
  saving.value = true
  try {
    if (form.value.id) {
      await courseApi.update(form.value.id, form.value)
    } else {
      await courseApi.create(form.value)
    }
    ElMessage.success('保存成功')
    drawerVisible.value = false
    loadCourses()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (course: Course) => {
  const newStatus = course.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(`确定${action}课程"${course.courseName}"吗？`, '提示', { type: 'warning' })
  try {
    await courseApi.updateStatus(course.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadCourses()
  } catch { ElMessage.error(`${action}失败`) }
}

const handleDelete = async (course: Course) => {
  await ElMessageBox.confirm(`确定删除课程"${course.courseName}"吗？此操作不可恢复。`, '警告', {
    type: 'warning', confirmButtonText: '确定删除', confirmButtonClass: 'el-button--danger',
  })
  try {
    await courseApi.delete(course.id)
    ElMessage.success('删除成功')
    loadCourses()
  } catch { ElMessage.error('删除失败') }
}

const getCourseCategoryName = (c: number) => ({ 1: '公共基础', 2: '专业核心', 3: '专业方向', 4: '选修' }[c] || '未知')
const getCourseNatureName = (n: number) => ({ 1: '必修', 2: '限选', 3: '任选' }[n] || '未知')

onMounted(() => { loadCourses(); loadOrgTree() })
</script>

<style scoped>
/* ============================================
   Course Page — Editorial Academic Aesthetic
   ============================================ */

.course-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f8f9fb;
  font-family: 'DM Sans', sans-serif;
}

/* Header */
.course-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 24px 16px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
}
.page-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.025em;
  margin: 0;
}
.stats-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}
.stat { font-size: 12.5px; color: #6b7280; }
.stat b { font-weight: 600; }
.c-required { color: #dc2626; }
.c-limited { color: #d97706; }
.c-free { color: #2563eb; }
.stat-sep { display: block; width: 1px; height: 10px; background: #d1d5db; }
.dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 2px; vertical-align: middle; }
.dot-active { background: #10b981; }
.dot-inactive { background: #9ca3af; }

.btn-create {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #111827;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.btn-create:hover { background: #1f2937; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }

/* Filter Bar */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
}
.search-box {
  position: relative;
  flex: 0 0 240px;
}
.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  pointer-events: none;
}
.search-input {
  width: 100%;
  padding: 7px 10px 7px 32px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  font-size: 13px;
  font-family: inherit;
  color: #374151;
  background: #f9fafb;
  transition: all 0.15s;
  outline: none;
}
.search-input:focus { border-color: #2563eb; background: #fff; box-shadow: 0 0 0 3px rgba(37,99,235,0.08); }
.search-input::placeholder { color: #9ca3af; }

.filter-group { display: flex; gap: 8px; align-items: center; }
.filter-select {
  padding: 7px 28px 7px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  font-size: 13px;
  font-family: inherit;
  color: #374151;
  background: #f9fafb url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%239ca3af' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") right 10px center no-repeat;
  appearance: none;
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s;
}
.filter-select:focus { border-color: #2563eb; }

.btn-reset {
  padding: 7px 12px;
  font-size: 12px;
  color: #6b7280;
  background: none;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.btn-reset:hover { color: #374151; border-color: #9ca3af; }

/* Table */
.table-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}

.course-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: separate;
  border-spacing: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}
.course-table th,
.course-table td {
  padding: 10px 12px;
  vertical-align: middle;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.course-table th {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #6b7280;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  user-select: none;
}
.course-table td {
  font-size: 13px;
  color: #374151;
  border-bottom: 1px solid #f3f4f6;
}
.course-row {
  cursor: pointer;
  transition: background 0.1s;
}
.course-row:hover { background: #f0f5ff; }
.course-row:last-child td { border-bottom: none; }

/* Column widths & alignment — th and td inherit the same rules */
/* Alignment overrides for special columns */
.text-right { text-align: right !important; }

/* Name cell allows wrapping for subtitle */
.cell-name { white-space: normal !important; }
.mono { font-family: 'JetBrains Mono', monospace; font-size: 12.5px; }

.code-badge {
  display: inline-block;
  padding: 2px 7px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11.5px;
  color: #475569;
  letter-spacing: 0.02em;
}
.name-text { font-weight: 500; color: #111827; }
.name-en { display: block; font-size: 11px; color: #9ca3af; margin-top: 1px; }

.category-chip {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11.5px;
  font-weight: 500;
}
.cat-1 { background: #eff6ff; color: #1d4ed8; }
.cat-2 { background: #fef2f2; color: #dc2626; }
.cat-3 { background: #f5f3ff; color: #7c3aed; }
.cat-4 { background: #f0fdf4; color: #16a34a; }

.nature-label { font-size: 12px; font-weight: 600; }
.nat-1 { color: #dc2626; }
.nat-2 { color: #d97706; }
.nat-3 { color: #2563eb; }

.hour-theory { color: #374151; }
.hour-sep { color: #d1d5db; margin: 0 2px; }
.hour-practice { color: #6b7280; }

.status-toggle {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px 3px 6px;
  border: 1px solid;
  border-radius: 99px;
  font-size: 11.5px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s;
  background: #fff;
}
.status-toggle.is-active { border-color: #bbf7d0; color: #16a34a; }
.status-toggle.is-active .toggle-dot { background: #22c55e; }
.status-toggle.is-inactive { border-color: #e5e7eb; color: #6b7280; }
.status-toggle.is-inactive .toggle-dot { background: #9ca3af; }
.toggle-dot { width: 6px; height: 6px; border-radius: 50%; }

.action-btn {
  padding: 4px 8px;
  font-size: 12px;
  color: #4b5563;
  background: none;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.1s;
}
.action-btn:hover { background: #f3f4f6; color: #111827; }
.action-danger:hover { background: #fef2f2; color: #dc2626; }

.empty-cell { text-align: center; padding: 40px 0 !important; color: #9ca3af; font-size: 13px; }

/* Pagination */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 24px 16px;
}
.page-info { font-size: 12px; color: #6b7280; }
.page-controls { display: flex; align-items: center; gap: 4px; }
.page-size-select {
  padding: 5px 24px 5px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  font-family: inherit;
  background: #fff url("data:image/svg+xml,%3Csvg width='8' height='5' viewBox='0 0 8 5' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l3 3 3-3' stroke='%239ca3af' stroke-width='1.2' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") right 8px center no-repeat;
  appearance: none;
  cursor: pointer;
  outline: none;
  margin-right: 8px;
}
.page-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #374151;
  cursor: pointer;
  transition: all 0.1s;
}
.page-btn:hover:not(:disabled) { background: #f3f4f6; border-color: #d1d5db; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-current {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: #111827;
  color: #fff;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

/* ============================================
   Drawer
   ============================================ */
.drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(2px);
  display: flex;
  justify-content: flex-end;
}
.drawer-panel {
  width: 560px;
  max-width: 90vw;
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 32px rgba(0,0,0,0.12);
}
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e8eaed;
}
.drawer-title {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 17px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}
.drawer-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: #f3f4f6;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}
.drawer-close:hover { background: #e5e7eb; color: #111827; }

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

/* Form Sections */
.form-section {
  padding: 20px 24px;
  border-bottom: 1px solid #f3f4f6;
}
.form-section:last-child { border-bottom: none; }

.section-title {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #6b7280;
  margin: 0 0 14px 0;
}

.field-grid { display: grid; gap: 12px; margin-bottom: 12px; }
.field-grid:last-child { margin-bottom: 0; }
.cols-2 { grid-template-columns: 1fr 1fr; }
.cols-3 { grid-template-columns: 1fr 1fr 1fr; }
.cols-4 { grid-template-columns: 1fr 1fr 1fr 1fr; }

.field { margin-bottom: 12px; }
.field:last-child { margin-bottom: 0; }
.field-grid .field { margin-bottom: 0; }

.field-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 5px;
}
.req { color: #ef4444; }

.field-input, .field-select, .field-textarea {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  font-size: 13px;
  font-family: inherit;
  color: #111827;
  background: #fafafa;
  transition: all 0.15s;
  outline: none;
  box-sizing: border-box;
}
.field-input:focus, .field-select:focus, .field-textarea:focus {
  border-color: #2563eb;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(37,99,235,0.06);
}
.field-textarea { resize: vertical; min-height: 80px; }
.field-select { appearance: none; background-image: url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%239ca3af' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E"); background-position: right 10px center; background-repeat: no-repeat; padding-right: 28px; cursor: pointer; }

.has-error .field-input, .has-error .field-select {
  border-color: #fca5a5;
  background: #fef2f2;
}

/* Radio group (inline) */
.radio-group { display: flex; gap: 0; border: 1px solid #e5e7eb; border-radius: 7px; overflow: hidden; }
.radio-item {
  flex: 1;
  padding: 7px 0;
  text-align: center;
  font-size: 12.5px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
  border-right: 1px solid #e5e7eb;
  user-select: none;
}
.radio-item:last-child { border-right: none; }
.radio-item input { display: none; }
.radio-item.active { background: #111827; color: #fff; }
.radio-item:not(.active):hover { background: #f3f4f6; }

/* Hours visual bar */
.hours-visual { margin-top: 12px; }
.hours-bar {
  display: flex;
  height: 22px;
  border-radius: 5px;
  overflow: hidden;
  background: #f3f4f6;
}
.hours-theory {
  background: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: width 0.3s;
}
.hours-theory span { font-size: 10px; color: #fff; font-weight: 600; }
.hours-practice {
  background: #06b6d4;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: width 0.3s;
}
.hours-practice span { font-size: 10px; color: #fff; font-weight: 600; }

/* Drawer footer */
.drawer-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid #e8eaed;
  background: #fafafa;
}
.btn-cancel {
  padding: 8px 18px;
  border: 1px solid #d1d5db;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  background: #fff;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.btn-cancel:hover { background: #f3f4f6; }
.btn-save {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  background: #111827;
  color: #fff;
  border: none;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.btn-save:hover:not(:disabled) { background: #1f2937; }
.btn-save:disabled { opacity: 0.6; cursor: not-allowed; }

@keyframes spin { to { transform: rotate(360deg); } }
.spin { animation: spin 0.8s linear infinite; }

/* Drawer transitions */
.drawer-enter-active { transition: opacity 0.2s ease; }
.drawer-enter-active .drawer-panel { transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1); }
.drawer-leave-active { transition: opacity 0.15s ease 0.05s; }
.drawer-leave-active .drawer-panel { transition: transform 0.2s ease; }
.drawer-enter-from { opacity: 0; }
.drawer-enter-from .drawer-panel { transform: translateX(100%); }
.drawer-leave-to { opacity: 0; }
.drawer-leave-to .drawer-panel { transform: translateX(100%); }
</style>
