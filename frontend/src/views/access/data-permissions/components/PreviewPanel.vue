<template>
  <div class="flex h-full flex-col bg-gray-50">
    <div class="border-b border-gray-200 bg-white px-4 py-3">
      <h3 class="flex items-center gap-2 text-sm font-semibold text-gray-900">
        <Eye class="h-4 w-4 text-gray-400" />
        配置预览
      </h3>
      <p class="mt-1 text-[11px] text-gray-500">此角色用户实际能看到什么</p>
    </div>

    <div class="flex-1 overflow-y-auto p-4">
      <!-- 当前场景摘要 -->
      <div class="mb-4 rounded-lg border border-gray-200 bg-white p-3">
        <div class="mb-2 text-[11px] font-medium uppercase tracking-wide text-gray-500">当前场景</div>
        <div class="space-y-1.5 text-xs text-gray-700">
          <div class="flex items-start gap-2">
            <component :is="primaryIcon" class="mt-0.5 h-3.5 w-3.5 flex-shrink-0 text-blue-500" />
            <div>
              <div class="font-medium">{{ primaryLabel }}</div>
              <div class="text-[10px] text-gray-400">{{ primaryDesc }}</div>
            </div>
          </div>

          <div v-if="decision.studentScope" class="flex items-start gap-2">
            <GraduationCap class="mt-0.5 h-3.5 w-3.5 flex-shrink-0 text-orange-500" />
            <div>
              <div class="font-medium">学生: {{ studentLabel }}</div>
              <div class="text-[10px] text-gray-400">EDU 特化</div>
            </div>
          </div>

          <div class="flex items-start gap-2">
            <CheckCircle2 v-if="decision.bizAutoFollow" class="mt-0.5 h-3.5 w-3.5 flex-shrink-0 text-emerald-500" />
            <XCircle v-else class="mt-0.5 h-3.5 w-3.5 flex-shrink-0 text-gray-400" />
            <div class="font-medium">
              {{ decision.bizAutoFollow ? '业务模块自动跟随' : '业务模块独立配置 (默认 SELF)' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 能看到的数据 (可读摘要) -->
      <div class="mb-4 rounded-lg border border-gray-200 bg-white p-3">
        <div class="mb-2 text-[11px] font-medium uppercase tracking-wide text-gray-500">
          能看到的数据
        </div>
        <ul class="space-y-1.5 text-xs text-gray-700">
          <li v-for="(line, i) in summaryLines" :key="i" class="flex items-start gap-1.5">
            <ChevronRight class="mt-0.5 h-3 w-3 flex-shrink-0 text-gray-400" />
            <span>{{ line }}</span>
          </li>
        </ul>
      </div>

      <!-- 模块清单 -->
      <div class="rounded-lg border border-gray-200 bg-white p-3">
        <div class="mb-2 flex items-center justify-between">
          <span class="text-[11px] font-medium uppercase tracking-wide text-gray-500">模块覆盖</span>
          <span class="rounded bg-gray-100 px-1.5 py-0.5 text-[10px] text-gray-600">
            {{ modulePermissions.length }} / {{ totalModules }}
          </span>
        </div>
        <div class="max-h-64 overflow-y-auto">
          <div
            v-for="mp in displayModulePermissions"
            :key="mp.moduleCode"
            class="flex items-center justify-between border-b border-gray-50 py-1 last:border-0"
          >
            <span class="truncate text-xs text-gray-700">{{ moduleLabel(mp.moduleCode) }}</span>
            <span
              class="ml-2 flex-shrink-0 rounded px-1.5 py-0.5 text-[10px] font-medium"
              :class="scopeBadge(mp.scopeCode)"
            >
              {{ scopeName(mp.scopeCode) }}
            </span>
          </div>
          <div v-if="modulePermissions.length === 0" class="py-4 text-center text-xs text-gray-400">
            无模块配置
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Eye,
  Globe,
  Building2,
  User,
  Edit3,
  GraduationCap,
  CheckCircle2,
  XCircle,
  ChevronRight,
} from 'lucide-vue-next'
import type { ModulePermission, DataScopeOption } from '@/types/access'
import type { SceneDecision } from '../composables/useSceneTemplate'

interface Props {
  decision: SceneDecision
  modulePermissions: ModulePermission[]
  dataScopeOptions: DataScopeOption[]
  moduleNameMap: Record<string, string>
  totalModules: number
}

const props = defineProps<Props>()

const primaryIcon = computed(() => {
  switch (props.decision.primary) {
    case 'ALL':
      return Globe
    case 'DEPARTMENT_AND_BELOW':
    case 'DEPARTMENT':
      return Building2
    case 'SELF':
      return User
    case 'CUSTOM':
      return Edit3
    default:
      return Globe
  }
})

const primaryLabel = computed(() => {
  const m: Record<string, string> = {
    ALL: '全部数据',
    DEPARTMENT_AND_BELOW: '本部门及以下',
    DEPARTMENT: '仅本部门',
    SELF: '仅本人',
    CUSTOM: '自定义',
  }
  return m[props.decision.primary] || props.decision.primary
})

const primaryDesc = computed(() => {
  const m: Record<string, string> = {
    ALL: '所有组织的所有数据',
    DEPARTMENT_AND_BELOW: '含子部门数据',
    DEPARTMENT: '不含子部门',
    SELF: '只看自己创建的',
    CUSTOM: '手选组织/年级/班级',
  }
  return m[props.decision.primary] || ''
})

const studentLabel = computed(() => {
  const m: Record<string, string> = {
    ALL: '全部学生',
    BY_CLASS: '我带的班级',
    BY_GRADE: '我管的年级',
    BY_MAJOR: '我管的专业',
    SELF: '仅本人',
  }
  return m[props.decision.studentScope || 'ALL'] || ''
})

const summaryLines = computed<string[]>(() => {
  const lines: string[] = []
  const d = props.decision

  if (d.primary === 'ALL') {
    lines.push('能看所有组织的所有数据')
  } else if (d.primary === 'DEPARTMENT_AND_BELOW') {
    lines.push('能看自己部门及下属部门的所有数据')
  } else if (d.primary === 'DEPARTMENT') {
    lines.push('只能看自己部门的数据 (不含子部门)')
  } else if (d.primary === 'SELF') {
    lines.push('只能看自己创建/负责的数据')
  } else if (d.primary === 'CUSTOM') {
    const o = d.customOrgIds?.length || 0
    const g = d.customGradeIds?.length || 0
    const c = d.customClassIds?.length || 0
    if (o + g + c === 0) lines.push('自定义范围: 未选择任何项 (实际无权限)')
    else lines.push(`自定义范围: ${o} 个组织 + ${g} 个年级 + ${c} 个班级`)
  }

  if (d.studentScope === 'BY_CLASS') lines.push('学生数据按班级关系过滤')
  else if (d.studentScope === 'BY_GRADE') lines.push('学生数据按年级关系过滤')
  else if (d.studentScope === 'BY_MAJOR') lines.push('学生数据按专业关系过滤')
  else if (d.studentScope === 'SELF') lines.push('只能看自己绑定的学生')

  if (d.bizAutoFollow) lines.push('考勤/成绩/检查/任务 自动同步主决策')
  else lines.push('业务模块默认仅本人 — 需在高级里单独配')

  return lines
})

const displayModulePermissions = computed(() => {
  return [...props.modulePermissions].sort((a, b) =>
    (props.moduleNameMap[a.moduleCode] || a.moduleCode).localeCompare(
      props.moduleNameMap[b.moduleCode] || b.moduleCode,
      'zh-CN'
    )
  )
})

function moduleLabel(code: string): string {
  return props.moduleNameMap[code] || code
}

function scopeName(code: string): string {
  const found = props.dataScopeOptions.find(s => s.scopeCode === code)
  return found?.scopeName || code
}

function scopeBadge(code: string): string {
  switch (code) {
    case 'ALL':
      return 'bg-blue-100 text-blue-700'
    case 'DEPARTMENT_AND_BELOW':
    case 'DEPARTMENT':
      return 'bg-emerald-100 text-emerald-700'
    case 'SELF':
      return 'bg-gray-100 text-gray-600'
    case 'CUSTOM':
      return 'bg-purple-100 text-purple-700'
    default:
      return 'bg-gray-100 text-gray-600'
  }
}
</script>
