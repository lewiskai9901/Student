<script setup lang="ts">
/**
 * Phase F - 「角色矩阵」视图.
 *
 * 行 = 人 / 列 = 3 种角色 + 状态; 点击 ✓/─ toggle 角色.
 * LEAD 不变量 (剩 1 LEAD 不让删) 由后端守护并以 409 返回,
 * 前端 totalLeads === 1 且当前为 LEAD 时禁用按钮 + tooltip 解释.
 */
import { ref, computed, toRef } from 'vue'
import { ElTooltip, ElMessage } from 'element-plus'
import { Check, Minus, Crown } from 'lucide-vue-next'
import type { LongId } from '@/types/common'
import type { PersonRow } from '@/api/inspection/project'
import { useInspectorRoles, type InspectorRole, RoleLabels } from '../composables/useInspectorRoles'

const props = defineProps<{
  projectId: LongId
  people: PersonRow[]
  totalLeads: number
}>()

const emit = defineEmits<{ (e: 'reload'): void }>()

const projectIdRef = toRef(props, 'projectId')
const { pending, toggleRole } = useInspectorRoles(projectIdRef)

const roleFilter = ref<'ALL' | InspectorRole | 'INACTIVE'>('ALL')

const filtered = computed(() => {
  if (roleFilter.value === 'ALL') return props.people
  if (roleFilter.value === 'INACTIVE') return props.people.filter(p => !p.isActive)
  return props.people.filter(p => p.roles.includes(roleFilter.value as InspectorRole))
})

function hasRole(p: PersonRow, role: InspectorRole): boolean {
  return p.roles.includes(role)
}

// 移除 LEAD 时, 若总数 = 1 且当前人是 LEAD, 禁用 (UI 提示)
function isLastLead(p: PersonRow, role: InspectorRole): boolean {
  return role === 'LEAD' && p.roles.includes('LEAD') && props.totalLeads <= 1
}

async function onToggle(p: PersonRow, role: InspectorRole) {
  if (isLastLead(p, role)) {
    ElMessage.warning('项目至少需要 1 个负责人, 请先指派其他负责人再移除当前')
    return
  }
  const ok = await toggleRole(p.userId, p.userName, role, hasRole(p, role))
  if (ok) emit('reload')
}
</script>

<template>
  <div class="rmv">
    <!-- 顶部 chips 筛选 -->
    <div class="rmv-chips">
      <button class="rmv-chip" :class="{ active: roleFilter === 'ALL' }" @click="roleFilter = 'ALL'">
        全部 <span class="rmv-chip-num">{{ people.length }}</span>
      </button>
      <button class="rmv-chip" :class="{ active: roleFilter === 'LEAD' }" @click="roleFilter = 'LEAD'">
        仅负责人 <span class="rmv-chip-num">{{ people.filter(p => p.roles.includes('LEAD')).length }}</span>
      </button>
      <button class="rmv-chip" :class="{ active: roleFilter === 'REVIEWER' }" @click="roleFilter = 'REVIEWER'">
        仅审核员 <span class="rmv-chip-num">{{ people.filter(p => p.roles.includes('REVIEWER')).length }}</span>
      </button>
      <button class="rmv-chip" :class="{ active: roleFilter === 'INSPECTOR' }" @click="roleFilter = 'INSPECTOR'">
        仅检查员 <span class="rmv-chip-num">{{ people.filter(p => p.roles.includes('INSPECTOR')).length }}</span>
      </button>
      <button class="rmv-chip" :class="{ active: roleFilter === 'INACTIVE' }" @click="roleFilter = 'INACTIVE'">
        已禁用 <span class="rmv-chip-num">{{ people.filter(p => !p.isActive).length }}</span>
      </button>
    </div>

    <!-- 矩阵表 -->
    <div v-if="filtered.length === 0" class="rmv-empty">
      暂无符合条件的成员
    </div>
    <div v-else class="rmv-table">
      <div class="rmv-row rmv-row--head">
        <div class="rmv-col-name">姓名</div>
        <div class="rmv-col-org">部门</div>
        <div class="rmv-col-role">检查员</div>
        <div class="rmv-col-role">审核员</div>
        <div class="rmv-col-role">负责人</div>
        <div class="rmv-col-status">状态</div>
      </div>

      <div v-for="p in filtered" :key="p.userId" class="rmv-row" :class="{ 'rmv-row--inactive': !p.isActive }">
        <div class="rmv-col-name">
          {{ p.userName }}
          <Crown v-if="p.roles.includes('LEAD')" class="rmv-crown" />
          <span v-if="p.isCreator" class="rmv-creator-badge" title="项目创建者">创建者</span>
        </div>
        <div class="rmv-col-org">{{ p.orgUnitName || '-' }}</div>

        <!-- 检查员 toggle -->
        <div class="rmv-col-role">
          <button class="rmv-toggle"
                  :class="{ 'rmv-toggle--checked': hasRole(p, 'INSPECTOR') }"
                  :disabled="pending"
                  @click="onToggle(p, 'INSPECTOR')">
            <Check v-if="hasRole(p, 'INSPECTOR')" class="w-3.5 h-3.5" />
            <Minus v-else class="w-3.5 h-3.5" />
          </button>
        </div>

        <!-- 审核员 toggle -->
        <div class="rmv-col-role">
          <button class="rmv-toggle"
                  :class="{ 'rmv-toggle--checked': hasRole(p, 'REVIEWER') }"
                  :disabled="pending"
                  @click="onToggle(p, 'REVIEWER')">
            <Check v-if="hasRole(p, 'REVIEWER')" class="w-3.5 h-3.5" />
            <Minus v-else class="w-3.5 h-3.5" />
          </button>
        </div>

        <!-- 负责人 toggle (LEAD 不变量保护) -->
        <div class="rmv-col-role">
          <ElTooltip v-if="isLastLead(p, 'LEAD')" content="项目至少需要 1 个负责人, 不能移除最后一个 LEAD" placement="top">
            <button class="rmv-toggle rmv-toggle--checked rmv-toggle--locked" :disabled="true">
              <Crown class="w-3.5 h-3.5" />
            </button>
          </ElTooltip>
          <button v-else
                  class="rmv-toggle"
                  :class="{ 'rmv-toggle--checked rmv-toggle--lead': hasRole(p, 'LEAD') }"
                  :disabled="pending"
                  @click="onToggle(p, 'LEAD')">
            <Crown v-if="hasRole(p, 'LEAD')" class="w-3.5 h-3.5" />
            <Minus v-else class="w-3.5 h-3.5" />
          </button>
        </div>

        <div class="rmv-col-status">
          <span :class="['rmv-status', p.isActive ? 'rmv-status--active' : 'rmv-status--inactive']">
            {{ p.isActive ? '启用' : '禁用' }}
          </span>
        </div>
      </div>
    </div>

    <!-- 说明 -->
    <div class="rmv-hint">
      <strong>提示:</strong> 同一人可同时担任多个角色 (例如检查员 + 审核员). 项目至少需要 1 个负责人.
    </div>
  </div>
</template>

<style scoped>
.rmv-chips {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.rmv-chip {
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
}
.rmv-chip:hover { border-color: #9ca3af; }
.rmv-chip.active { background: #eff6ff; border-color: #3b82f6; color: #1d4ed8; font-weight: 600; }
.rmv-chip-num { background: rgba(0,0,0,0.06); border-radius: 10px; padding: 1px 6px; font-size: 11px; }
.rmv-chip.active .rmv-chip-num { background: rgba(255,255,255,0.5); }

.rmv-table {
  background: var(--insp-bg-surface, #fff);
  border: 1px solid var(--insp-border-default, #e5e7eb);
  border-radius: 8px;
  overflow: hidden;
}
.rmv-row {
  display: grid;
  grid-template-columns: 1.6fr 1.2fr 1fr 1fr 1fr 0.8fr;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid var(--insp-border-default, #e5e7eb);
  font-size: 13px;
}
.rmv-row:last-child { border-bottom: none; }
.rmv-row--head {
  background: var(--insp-bg-subtle, #f9fafb);
  font-weight: 600;
  font-size: 12px;
  color: var(--insp-ink-secondary, #4b5563);
  text-transform: none;
}
.rmv-row--inactive { opacity: 0.6; background: rgba(0,0,0,0.02); }

.rmv-col-name { display: flex; align-items: center; gap: 6px; color: var(--insp-ink-primary, #111827); font-weight: 500; }
.rmv-crown { width: 14px; height: 14px; color: #d97706; }
.rmv-creator-badge { background: #ede9fe; color: #6d28d9; font-size: 10px; padding: 1px 6px; border-radius: 8px; font-weight: 500; }
.rmv-col-org { color: var(--insp-ink-tertiary, #6b7280); font-size: 12px; }
.rmv-col-role { text-align: center; }
.rmv-col-status { text-align: center; }

.rmv-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: var(--insp-bg-subtle, #f9fafb);
  border: 1px solid var(--insp-border-default, #e5e7eb);
  border-radius: 50%;
  color: var(--insp-ink-tertiary, #6b7280);
  cursor: pointer;
  transition: all 0.15s;
}
.rmv-toggle:hover:not(:disabled) { background: #eff6ff; border-color: #3b82f6; color: #2563eb; }
.rmv-toggle:disabled { cursor: not-allowed; opacity: 0.6; }
.rmv-toggle--checked { background: #dbeafe; border-color: #3b82f6; color: #1d4ed8; }
.rmv-toggle--checked.rmv-toggle--lead { background: #fef3c7; border-color: #d97706; color: #92400e; }
.rmv-toggle--locked { background: #fef3c7; border-color: #d97706; color: #92400e; cursor: not-allowed; }

.rmv-status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 11.5px;
  font-weight: 500;
}
.rmv-status--active { background: #dcfce7; color: #166534; }
.rmv-status--inactive { background: #f3f4f6; color: #6b7280; }

.rmv-empty {
  text-align: center;
  padding: 48px 16px;
  color: var(--insp-ink-tertiary, #6b7280);
  font-size: 13px;
  background: var(--insp-bg-surface, #fff);
  border: 1px dashed var(--insp-border-default, #e5e7eb);
  border-radius: 8px;
}

.rmv-hint {
  margin-top: 12px;
  padding: 10px 14px;
  background: var(--insp-bg-subtle, #f9fafb);
  border-radius: 6px;
  font-size: 12px;
  color: var(--insp-ink-tertiary, #6b7280);
}
.rmv-hint strong { color: var(--insp-ink-secondary, #4b5563); }
</style>
