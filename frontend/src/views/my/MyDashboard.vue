<template>
  <div class="my-dash">
    <!-- Header: 问候 + 紧凑统计条 -->
    <header class="my-header">
      <h1 class="my-title">{{ greeting }}，{{ displayName }}</h1>
      <span class="my-sub">{{ todayLabel }}</span>
      <span class="my-sep">|</span>
      <span class="my-stat">今日 <b>{{ summary?.todayLessons ?? '-' }}</b> 节课</span>
      <span class="my-stat">本周课时 <b>{{ summary?.weeklyHoursCurrent ?? '-' }}/{{ summary?.weeklyHoursTotal ?? '-' }}</b></span>
      <span class="my-stat">代课待办 <b>{{ summary?.substituteRequests ?? '-' }}</b></span>
    </header>

    <!-- 非 TEACHER 友好空态 -->
    <div v-if="!isTeacher" class="my-non-teacher">
      <div class="my-non-teacher-title">工作台面向教师</div>
      <div class="my-non-teacher-desc">
        当前账户类型：{{ userTypeLabel }}。工作台视图仅针对教师角色启用。
      </div>
      <router-link to="/dashboard" class="tm-btn tm-btn-secondary">前往系统首页</router-link>
    </div>

    <!-- TEACHER 视图 -->
    <div v-else class="my-grid">
      <!-- 今日课表（占满顶部整行） -->
      <section class="my-card my-card-wide">
        <header class="my-card-head">
          <span class="my-card-title">今日课表</span>
          <span class="my-card-meta">{{ scheduleMeta }}</span>
        </header>
        <div v-if="scheduleState === 'loading'" class="my-state-loading">加载中…</div>
        <div v-else-if="scheduleState === 'error'" class="my-state-error">
          加载失败
          <button class="my-link" @click="loadSchedule">重试</button>
        </div>
        <div v-else-if="!todaySchedule.length" class="my-state-empty">今天没有课</div>
        <ul v-else class="my-list">
          <li v-for="lesson in todaySchedule" :key="lesson.instanceId" class="my-row">
            <span class="my-row-dot" :class="lessonDotClass(lesson)"></span>
            <span class="my-row-time">{{ slotLabel(lesson.startSlot, lesson.endSlot) }}</span>
            <span class="my-row-main">{{ lesson.courseName || '未命名课程' }}</span>
            <span class="my-row-aux">{{ lesson.className || '' }}</span>
            <span class="my-row-aux">{{ lesson.classroomName || '' }}</span>
            <span class="my-row-tag" v-if="lesson.status === 4">代课</span>
            <span class="my-row-tag" v-else-if="lesson.status === 3">补课</span>
          </li>
        </ul>
      </section>

      <!-- 我的班级 -->
      <section class="my-card">
        <header class="my-card-head">
          <span class="my-card-title">我的班级</span>
          <router-link to="/my-class" class="my-card-link">查看</router-link>
        </header>
        <div v-if="classesState === 'loading'" class="my-state-loading">加载中…</div>
        <div v-else-if="classesState === 'error'" class="my-state-error">
          加载失败
          <button class="my-link" @click="loadClasses">重试</button>
        </div>
        <div v-else-if="!myClasses.length" class="my-state-empty">尚未被分配班级 · 联系教务</div>
        <ul v-else class="my-list">
          <li v-for="c in myClasses" :key="c.classId" class="my-row">
            <span class="my-row-main">{{ c.className || '—' }}</span>
            <span class="my-row-aux">{{ c.studentCount }} 人</span>
            <span v-if="c.isHeadTeacher" class="my-row-tag">班主任</span>
            <span v-else-if="c.subjects?.length" class="my-row-aux">{{ c.subjects.join('/') }}</span>
          </li>
        </ul>
      </section>

      <!-- 代课待办 -->
      <section class="my-card">
        <header class="my-card-head">
          <span class="my-card-title">代课待办</span>
        </header>
        <div v-if="tasksState === 'loading'" class="my-state-loading">加载中…</div>
        <div v-else-if="tasksState === 'error'" class="my-state-error">
          加载失败
          <button class="my-link" @click="loadTasks">重试</button>
        </div>
        <div v-else-if="!substituteTasks.length" class="my-state-empty">暂无代课请求</div>
        <ul v-else class="my-list">
          <li v-for="t in substituteTasks" :key="t.taskId" class="my-row my-row-col">
            <div class="my-row-line">
              <span class="my-row-main">{{ t.courseName || '代课' }}</span>
              <span class="my-row-aux">{{ formatScheduled(t) }}</span>
            </div>
            <div class="my-row-line">
              <span class="my-row-aux">{{ t.requesterName || '申请人' }} · {{ relTime(t.requestedAt) }}</span>
            </div>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getMyDashboardSummary, getMyTodaySchedule, getMyClasses, getMySubstituteTasks } from '@/api/my'
import type { DashboardSummary, TodayLesson, MyClass, SubstituteTask } from '@/types/my'

type LoadState = 'loading' | 'ready' | 'error'

const authStore = useAuthStore()
const displayName = computed(() => authStore.user?.realName || authStore.user?.username || '老师')
const userTypeCode = computed(() => authStore.user?.userTypeCode || '')
const isTeacher = computed(() => userTypeCode.value === 'TEACHER')
const userTypeLabel = computed(() => userTypeCode.value || '未指定')

const summary = ref<DashboardSummary | null>(null)
const todaySchedule = ref<TodayLesson[]>([])
const myClasses = ref<MyClass[]>([])
const substituteTasks = ref<SubstituteTask[]>([])

const scheduleState = ref<LoadState>('loading')
const classesState = ref<LoadState>('loading')
const tasksState = ref<LoadState>('loading')

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const todayLabel = computed(() => {
  const d = new Date()
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${d.getMonth() + 1}月${d.getDate()}日 · ${weekdays[d.getDay()]}`
})

const scheduleMeta = computed(() => {
  const n = todaySchedule.value.length
  return n ? `${n} 节` : ''
})

function slotLabel(start: number | null, end: number | null): string {
  if (!start) return '—'
  if (!end || end === start) return `第${start}节`
  return `第${start}-${end}节`
}

function lessonDotClass(l: TodayLesson): string {
  if (l.status === 4) return 'dot-violet'      // 代课
  if (l.status === 3) return 'dot-amber'        // 补课
  const now = new Date()
  const startSlotMinutes = slotToMinutes(l.startSlot ?? 0)
  const endSlotMinutes = slotToMinutes((l.endSlot ?? l.startSlot ?? 0) + 1)
  const cur = now.getHours() * 60 + now.getMinutes()
  if (cur >= startSlotMinutes && cur < endSlotMinutes) return 'dot-solid dot-green'
  if (cur >= endSlotMinutes) return 'dot-gray'
  return 'dot-green'
}

// 简易节次→分钟映射 (8:00 第1节 起，每节 45 分钟 + 10 分钟间隔)
function slotToMinutes(slot: number): number {
  if (slot < 1) return 0
  return 8 * 60 + (slot - 1) * 55
}

function formatScheduled(t: SubstituteTask): string {
  if (!t.scheduledDate) return ''
  const d = t.scheduledDate
  const slot = t.startSlot ? `第${t.startSlot}节` : ''
  return [d, slot].filter(Boolean).join(' · ')
}

function relTime(iso: string | null): string {
  if (!iso) return ''
  const t = new Date(iso).getTime()
  if (!t) return ''
  const diff = (Date.now() - t) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  return `${Math.floor(diff / 86400)}天前`
}

async function loadSummary() {
  try {
    summary.value = await getMyDashboardSummary()
  } catch {
    summary.value = null
  }
}

async function loadSchedule() {
  scheduleState.value = 'loading'
  try {
    todaySchedule.value = await getMyTodaySchedule()
    scheduleState.value = 'ready'
  } catch {
    scheduleState.value = 'error'
  }
}

async function loadClasses() {
  classesState.value = 'loading'
  try {
    myClasses.value = await getMyClasses()
    classesState.value = 'ready'
  } catch {
    classesState.value = 'error'
  }
}

async function loadTasks() {
  tasksState.value = 'loading'
  try {
    substituteTasks.value = await getMySubstituteTasks()
    tasksState.value = 'ready'
  } catch {
    tasksState.value = 'error'
  }
}

onMounted(() => {
  if (isTeacher.value) {
    loadSummary()
    loadSchedule()
    loadClasses()
    loadTasks()
  }
})
</script>

<style scoped>
.my-dash {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f8f9fb;
}

/* 紧凑头部 — 与 OrgStructure 对齐 */
.my-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;
}
.my-title { font-size: 15px; font-weight: 700; color: #111827; margin: 0; }
.my-sub { font-size: 12px; color: #9ca3af; }
.my-sep { color: #d1d5db; font-size: 12px; }
.my-stat { font-size: 12px; color: #6b7280; }
.my-stat b { font-weight: 600; color: #111827; margin: 0 1px; }

/* 非教师空态 */
.my-non-teacher {
  margin: 40px auto;
  padding: 32px 48px;
  text-align: center;
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 6px;
  max-width: 420px;
}
.my-non-teacher-title { font-size: 15px; font-weight: 600; color: #111827; margin-bottom: 6px; }
.my-non-teacher-desc { font-size: 13px; color: #6b7280; margin-bottom: 16px; line-height: 1.6; }

/* 卡片网格 */
.my-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-areas:
    "schedule schedule"
    "classes  tasks";
  gap: 12px;
  padding: 20px;
  flex: 1;
  overflow: auto;
}
.my-card {
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 6px;
  padding: 12px 16px 8px;
  display: flex;
  flex-direction: column;
  min-height: 180px;
}
.my-card-wide { grid-area: schedule; }
.my-card:nth-of-type(2) { grid-area: classes; }
.my-card:nth-of-type(3) { grid-area: tasks; }

.my-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f3f5;
  margin-bottom: 4px;
}
.my-card-title { font-size: 13px; font-weight: 600; color: #111827; }
.my-card-meta { font-size: 12px; color: #9ca3af; }
.my-card-link {
  margin-left: auto;
  font-size: 12px;
  color: #4f46e5;
  text-decoration: none;
}

/* 行列表 */
.my-list { list-style: none; margin: 0; padding: 0; }
.my-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
  font-size: 12.5px;
  color: #374151;
  border-bottom: 1px solid #f8f9fb;
}
.my-row:last-child { border-bottom: none; }
.my-row-col { flex-direction: column; align-items: stretch; gap: 2px; }
.my-row-line { display: flex; align-items: center; gap: 8px; }
.my-row-dot {
  width: 7px; height: 7px; border-radius: 50%;
  border: 1.5px solid #9ca3af; flex-shrink: 0;
}
.my-row-dot.dot-solid { border: none; }
.my-row-dot.dot-green { border-color: #10b981; background: #fff; }
.my-row-dot.dot-solid.dot-green { background: #10b981; }
.my-row-dot.dot-gray { border-color: #d1d5db; background: #f3f4f6; }
.my-row-dot.dot-violet { border-color: #8b5cf6; }
.my-row-dot.dot-amber { border-color: #f59e0b; }
.my-row-time { color: #6b7280; min-width: 90px; font-variant-numeric: tabular-nums; }
.my-row-main { color: #111827; font-weight: 500; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.my-row-aux { color: #6b7280; }
.my-row-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 3px;
  background: #f3f4f6;
  color: #4b5563;
}

/* 空/加载/错误态 */
.my-state-loading, .my-state-empty, .my-state-error {
  padding: 28px 0;
  text-align: center;
  font-size: 12.5px;
  color: #9ca3af;
}
.my-state-error { color: #dc2626; }
.my-link {
  background: none; border: none; color: #4f46e5;
  font-size: 12.5px; cursor: pointer; margin-left: 6px;
}
.my-link:hover { text-decoration: underline; }

/* 响应式 — 窄屏单列 */
@media (max-width: 900px) {
  .my-grid {
    grid-template-columns: 1fr;
    grid-template-areas: "schedule" "classes" "tasks";
  }
}
</style>
