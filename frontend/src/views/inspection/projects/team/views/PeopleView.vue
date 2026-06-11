<script setup lang="ts">
/**
 * Phase D - 「按人」视图主战场.
 *
 * 每行一个检查员; 默认折叠态显示进度条 + 4 数字徽章;
 * 有待办的行默认展开 (展开后看 3 段任务列表).
 *
 * 排序由后端按 "逾期 → 待审核 → 进行中" 倒序返回, 负责人始终置顶.
 */
import { ref, computed } from 'vue'
import { ElInput } from 'element-plus'
import { Search } from 'lucide-vue-next'
import type { LongId } from '@/types/common'
import type { PersonRow, WorkbenchTaskRow } from '@/api/inspection/project'
import PersonRowCard from './PersonRowCard.vue'

const props = defineProps<{
  projectId: LongId
  people: PersonRow[]
  pendingAssignTasks: WorkbenchTaskRow[]
  isDraft?: boolean
}>()

const emit = defineEmits<{
  (e: 'reload'): void
}>()

const filterText = ref('')
const roleFilter = ref<'ALL' | 'LEAD' | 'REVIEWER' | 'INSPECTOR'>('ALL')

const filteredPeople = computed(() => {
  let result = props.people
  if (roleFilter.value !== 'ALL') {
    result = result.filter(p => p.roles.includes(roleFilter.value as any))
  }
  if (filterText.value.trim()) {
    const q = filterText.value.trim().toLowerCase()
    result = result.filter(p =>
      (p.userName || '').toLowerCase().includes(q) ||
      (p.orgUnitName || '').toLowerCase().includes(q)
    )
  }
  return result
})

const showFilter = computed(() => (props.people?.length ?? 0) > 4)
</script>

<template>
  <div class="pv">
    <!-- 顶部 chips 角色筛选 + 姓名搜索 -->
    <div v-if="showFilter" class="pv-filter">
      <div class="pv-chips">
        <button class="pv-chip" :class="{ active: roleFilter === 'ALL' }" @click="roleFilter = 'ALL'">
          全部 <span class="pv-chip-num">{{ people.length }}</span>
        </button>
        <button class="pv-chip pv-chip--lead" :class="{ active: roleFilter === 'LEAD' }" @click="roleFilter = 'LEAD'">
          负责人 <span class="pv-chip-num">{{ people.filter(p => p.roles.includes('LEAD')).length }}</span>
        </button>
        <button class="pv-chip" :class="{ active: roleFilter === 'REVIEWER' }" @click="roleFilter = 'REVIEWER'">
          审核员 <span class="pv-chip-num">{{ people.filter(p => p.roles.includes('REVIEWER')).length }}</span>
        </button>
        <button class="pv-chip" :class="{ active: roleFilter === 'INSPECTOR' }" @click="roleFilter = 'INSPECTOR'">
          检查员 <span class="pv-chip-num">{{ people.filter(p => p.roles.includes('INSPECTOR')).length }}</span>
        </button>
      </div>
      <ElInput v-model="filterText"
               placeholder="按姓名/部门搜索..."
               size="default"
               clearable
               style="max-width: 240px">
        <template #prefix><Search class="w-3.5 h-3.5" /></template>
      </ElInput>
    </div>

    <!-- 人员列表 -->
    <div v-if="filteredPeople.length === 0" class="pv-empty">
      <span v-if="people.length === 0">暂无项目成员, 点击右上「添加成员」</span>
      <span v-else>未找到匹配的成员</span>
    </div>
    <div v-else class="pv-list">
      <PersonRowCard
        v-for="p in filteredPeople"
        :key="p.userId"
        :project-id="projectId"
        :person="p"
        :pending-assign-tasks="pendingAssignTasks"
        :is-draft="isDraft"
        @reload="emit('reload')"
      />
    </div>
  </div>
</template>

<style scoped>
.pv-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.pv-chips { display: flex; gap: 6px; }
.pv-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  background: var(--insp-bg-surface, #fff);
  border: 1px solid var(--insp-border-default, #e5e7eb);
  border-radius: 16px;
  font-size: 12px;
  color: var(--insp-ink-secondary, #4b5563);
  cursor: pointer;
  transition: all 0.15s;
}
.pv-chip:hover { border-color: #9ca3af; }
.pv-chip.active { background: #eff6ff; border-color: #3b82f6; color: #1d4ed8; font-weight: 600; }
.pv-chip--lead.active { background: #fffbeb; border-color: #d97706; color: #92400e; }
.pv-chip-num { background: rgba(0,0,0,0.06); border-radius: 10px; padding: 1px 6px; font-size: 11px; }
.pv-chip.active .pv-chip-num { background: rgba(255,255,255,0.5); }

.pv-list { display: flex; flex-direction: column; gap: 8px; }
.pv-empty {
  text-align: center;
  padding: 48px 16px;
  color: var(--insp-ink-tertiary, #6b7280);
  font-size: 13px;
  background: var(--insp-bg-surface, #fff);
  border: 1px dashed var(--insp-border-default, #e5e7eb);
  border-radius: 8px;
}
</style>
