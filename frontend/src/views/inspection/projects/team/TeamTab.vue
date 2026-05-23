<script setup lang="ts">
/**
 * Phase C - 「人员与任务」Tab 容器.
 *
 * 替换 ProjectDetailView.vue 旧的 144 行内联实现 (1104-1248), 改成
 * 3 视图切换 (按人 / 按任务 / 角色矩阵) + 顶部状态条 + 添加成员按钮.
 *
 * 设计意图: 解决工作流割裂 — 不再把"待分配/待审核/检查员"切成 3 张独立卡片,
 * 而是以人为中心组织信息, 顶部统计条全局可见, 视图按任务/角色另开两个模式.
 */
import { ref, computed, watch, onMounted, toRef } from 'vue'
import { ElMessage, ElButton, ElSelect, ElOption } from 'element-plus'
import { Users, ClipboardList, Grid3X3, UserPlus } from 'lucide-vue-next'
import type { LongId } from '@/types/common'
import { usePeopleWorkbench } from './composables/usePeopleWorkbench'
import TeamStatusBar from './TeamStatusBar.vue'
import PeopleView from './views/PeopleView.vue'
import TasksView from './views/TasksView.vue'
import RoleMatrixView from './views/RoleMatrixView.vue'
import { addInspector } from '@/api/inspection/project'
import { getSimpleUserList } from '@/api/user'
import type { SimpleUser } from '@/types/user'

const props = defineProps<{
  projectId: LongId
  isDraft?: boolean
}>()

type ViewMode = 'people' | 'tasks' | 'matrix'
const activeView = ref<ViewMode>('people')
const tasksDefaultTab = ref<'pendingAssign' | 'pendingReview' | 'inProgress' | 'overdue'>('pendingAssign')

const projectIdRef = toRef(props, 'projectId')
const wb = usePeopleWorkbench(projectIdRef)

onMounted(() => {
  wb.reload()
})

watch(() => props.projectId, () => {
  wb.reload()
})

function onStatusBarNavigate(view: ViewMode, filter?: string) {
  activeView.value = view
  if (view === 'tasks' && filter) {
    tasksDefaultTab.value = filter as any
  }
}

// 添加成员
const showAddPanel = ref(false)
const addQuery = ref('')
const addResults = ref<SimpleUser[]>([])
const addLoading = ref(false)
const addRole = ref<'INSPECTOR' | 'REVIEWER' | 'LEAD'>('INSPECTOR')

async function searchUsers(q: string) {
  if (!q || q.length < 1) {
    addResults.value = []
    return
  }
  addLoading.value = true
  try {
    addResults.value = await getSimpleUserList(q)
  } catch (e) {
    addResults.value = []
  } finally {
    addLoading.value = false
  }
}

async function handleAddMember(userId: LongId) {
  const user = addResults.value.find(u => String(u.id) === String(userId))
  if (!user) return
  try {
    await addInspector(props.projectId, {
      userId,
      userName: user.realName || user.username || '',
      role: addRole.value as any,
    })
    ElMessage.success(`已添加 ${user.realName || user.username} 为${addRole.value === 'LEAD' ? '负责人' : addRole.value === 'REVIEWER' ? '审核员' : '检查员'}`)
    showAddPanel.value = false
    addQuery.value = ''
    addResults.value = []
    await wb.reload()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '添加失败')
  }
}

// 暴露 reload, 让上层可以在外部触发刷新 (审核/分配后)
defineExpose({ reload: wb.reload })
</script>

<template>
  <div class="team-tab">
    <!-- 顶部状态条 -->
    <TeamStatusBar :summary="wb.summary.value" @navigate="onStatusBarNavigate" />

    <!-- 视图切换 + 添加成员 -->
    <div class="team-toolbar">
      <div class="team-tabs">
        <button class="team-tab-btn" :class="{ active: activeView === 'people' }"
                @click="activeView = 'people'">
          <Users class="w-3.5 h-3.5" />按人
        </button>
        <button class="team-tab-btn" :class="{ active: activeView === 'tasks' }"
                @click="activeView = 'tasks'">
          <ClipboardList class="w-3.5 h-3.5" />按任务
        </button>
        <button class="team-tab-btn" :class="{ active: activeView === 'matrix' }"
                @click="activeView = 'matrix'">
          <Grid3X3 class="w-3.5 h-3.5" />角色矩阵
        </button>
      </div>
      <div class="team-toolbar-actions">
        <ElButton type="primary" size="small" @click="showAddPanel = !showAddPanel">
          <UserPlus class="w-3.5 h-3.5 mr-0.5" />添加成员
        </ElButton>
      </div>
    </div>

    <!-- 添加成员浮层 -->
    <div v-if="showAddPanel" class="team-add-panel">
      <div class="team-add-row">
        <label class="team-add-label">姓名</label>
        <ElSelect
          v-model="addQuery"
          filterable
          remote
          reserve-keyword
          :remote-method="searchUsers"
          :loading="addLoading"
          placeholder="输入姓名搜索..."
          style="flex:1; min-width: 240px"
          size="default"
          clearable
          @change="handleAddMember"
        >
          <ElOption
            v-for="u in addResults"
            :key="u.id"
            :label="(u.realName || u.username) + (u.orgUnitName ? ` (${u.orgUnitName})` : '')"
            :value="u.id"
          />
        </ElSelect>
        <label class="team-add-label">角色</label>
        <ElSelect v-model="addRole" size="default" style="width: 130px">
          <ElOption label="检查员" value="INSPECTOR" />
          <ElOption label="审核员" value="REVIEWER" />
          <ElOption label="项目负责人" value="LEAD" />
        </ElSelect>
        <ElButton size="small" @click="showAddPanel = false">取消</ElButton>
      </div>
    </div>

    <!-- 加载/错误 -->
    <div v-if="wb.loading.value && !wb.data.value" class="team-loading">
      加载中...
    </div>
    <div v-else-if="wb.error.value" class="team-error">
      {{ wb.error.value }}
      <ElButton size="small" link type="primary" @click="wb.reload()">重试</ElButton>
    </div>

    <!-- 主区域 -->
    <div v-else class="team-body">
      <PeopleView v-if="activeView === 'people'"
                  :project-id="projectId"
                  :people="wb.people.value"
                  :pending-assign-tasks="wb.pendingAssignTasks.value"
                  :is-draft="isDraft"
                  @reload="wb.reload" />
      <TasksView v-else-if="activeView === 'tasks'"
                 :project-id="projectId"
                 :people="wb.people.value"
                 :pending-assign-tasks="wb.pendingAssignTasks.value"
                 :default-tab="tasksDefaultTab"
                 @reload="wb.reload" />
      <RoleMatrixView v-else-if="activeView === 'matrix'"
                      :project-id="projectId"
                      :people="wb.people.value"
                      :total-leads="wb.summary.value?.totalLeads ?? 0"
                      @reload="wb.reload" />
    </div>
  </div>
</template>

<style scoped>
.team-tab { padding-top: 4px; }

.team-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 12px;
}
.team-tabs { display: inline-flex; background: var(--insp-bg-subtle, #f3f4f6); border-radius: 8px; padding: 3px; gap: 2px; }
.team-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: none;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  color: var(--insp-ink-secondary, #4b5563);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.team-tab-btn:hover { color: var(--insp-ink-primary, #111827); }
.team-tab-btn.active {
  background: var(--insp-bg-surface, #fff);
  color: var(--insp-ink-primary, #111827);
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}

.team-add-panel {
  background: var(--insp-bg-surface, #fff);
  border: 1px solid var(--insp-border-default, #e5e7eb);
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
}
.team-add-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.team-add-label { font-size: 12px; color: var(--insp-ink-tertiary, #6b7280); white-space: nowrap; }

.team-loading, .team-error {
  text-align: center;
  padding: 32px;
  color: var(--insp-ink-tertiary, #6b7280);
  font-size: 13px;
}
.team-error { color: #dc2626; }

.team-body { min-height: 200px; }
</style>
