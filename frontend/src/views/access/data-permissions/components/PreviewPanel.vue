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

      <!-- Fallback 提示 (场景模板应用时某些模块不支持目标 scope, 自动降级) -->
      <div
        v-if="fallbacks && fallbacks.length"
        class="mb-4 rounded-lg border border-amber-200 bg-amber-50 p-3"
      >
        <div class="mb-1.5 flex items-center gap-1.5 text-[11px] font-medium text-amber-800">
          <AlertTriangle class="h-3.5 w-3.5" />
          {{ fallbacks.length }} 个模块已自动降级
        </div>
        <ul class="space-y-0.5 text-[10.5px] text-amber-700">
          <li v-for="f in fallbacks.slice(0, 6)" :key="f.moduleCode" class="flex items-center gap-1">
            <span class="truncate">{{ f.moduleName }}</span>
            <span class="text-amber-500">:</span>
            <code class="rounded bg-amber-100 px-1 text-[10px]">{{ scopeName(f.from) }}</code>
            <span class="text-amber-500">></span>
            <code class="rounded bg-amber-100 px-1 text-[10px]">{{ scopeName(f.to) }}</code>
          </li>
          <li v-if="fallbacks.length > 6" class="text-[10px] text-amber-600">
            … 共 {{ fallbacks.length }} 条
          </li>
        </ul>
        <p class="mt-1.5 text-[10px] text-amber-600">
          这些模块的 scope 配置集不含所选主范围, 系统按 FALLBACK 阶梯自动选择最接近的范围.
        </p>
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

      <!-- 模拟用户预览 -->
      <div class="mb-4 rounded-lg border border-blue-200 bg-blue-50 p-3">
        <div class="mb-2 flex items-center justify-between">
          <span class="flex items-center gap-1 text-[11px] font-medium text-blue-700">
            <User class="h-3 w-3" />
            模拟用户
          </span>
          <button
            v-if="simulateResults.length"
            class="text-[10px] text-blue-500 hover:underline"
            @click="resetSimulation"
          >
            清空
          </button>
        </div>

        <div class="mb-2 flex items-center gap-2">
          <input
            v-model.number="simulateUserId"
            type="number"
            placeholder="输入用户 ID (如 1)"
            class="h-7 flex-1 rounded border border-gray-300 px-2 text-[11px] outline-none focus:border-blue-500"
            @keyup.enter="runSimulate"
          />
          <button
            class="h-7 rounded bg-blue-600 px-3 text-[11px] font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            :disabled="!simulateUserId || simulating || !modulePermissions.length"
            @click="runSimulate"
          >
            <Loader2 v-if="simulating" class="inline h-2.5 w-2.5 animate-spin" />
            <span v-else>> 模拟</span>
          </button>
        </div>

        <div v-if="simulateResults.length" class="space-y-1">
          <div
            v-for="r in topResults"
            :key="r.moduleCode"
            class="flex items-start gap-1 text-[10.5px]"
          >
            <span class="w-20 flex-shrink-0 truncate font-medium text-blue-900">
              {{ moduleLabel(r.moduleCode) }}
            </span>
            <span class="flex-1">
              <span v-if="r.accessibleCount >= 0" class="font-semibold text-blue-800">
                {{ r.accessibleCount }} 条
              </span>
              <span v-else class="italic text-amber-600">
                {{ r.note || '未支持' }}
              </span>
              <span v-if="r.samples?.length" class="ml-1 text-[10px] text-gray-500">
                · {{ r.samples.map(s => s.name || s.id).join(', ') }}
              </span>
            </span>
          </div>
          <button
            v-if="simulateResults.length > 5"
            class="mt-1 w-full text-[10px] text-blue-500 hover:underline"
            @click="expandAll = !expandAll"
          >
            {{ expandAll ? '收起' : `展开全部 ${simulateResults.length} 个模块` }}
          </button>
        </div>

        <div v-else class="text-[10px] text-blue-600">
          输入用户 ID 预览此角色下该用户实际能访问的数据
        </div>
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
import type { LongId } from '@/types/common'
import { computed, ref } from 'vue'
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
  AlertTriangle,
  Loader2,
} from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import type { ModulePermission, DataScopeOption } from '@/types/access'
import type { SceneDecision, ScopeFallbackInfo } from '../composables/useSceneTemplate'
import { dataPermissionSimulateApi, type SimulateResult } from '@/api/access'

interface Props {
  decision: SceneDecision
  modulePermissions: ModulePermission[]
  dataScopeOptions: DataScopeOption[]
  moduleNameMap: Record<string, string>
  totalModules: number
  fallbacks?: ScopeFallbackInfo[]
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

// ==================== 模拟用户预览 ====================
const simulateUserId = ref<LongId | null>(null)
const simulating = ref(false)
const simulateResults = ref<SimulateResult[]>([])
const expandAll = ref(false)

const topResults = computed(() =>
  expandAll.value ? simulateResults.value : simulateResults.value.slice(0, 5)
)

function resetSimulation() {
  simulateResults.value = []
  expandAll.value = false
}

async function runSimulate() {
  if (!simulateUserId.value || !props.modulePermissions.length) return
  simulating.value = true
  try {
    const res = await dataPermissionSimulateApi.simulate({
      userId: simulateUserId.value,
      modulePermissions: props.modulePermissions.map(mp => ({
        moduleCode: mp.moduleCode,
        scopeCode: mp.scopeCode,
        scopeItems: mp.scopeItems as any,
      })),
    })
    simulateResults.value = (res?.results as SimulateResult[]) || []
    if (!simulateResults.value.length) {
      ElMessage.warning('未配置任何模块, 无法模拟')
    }
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || String(e)
    ElMessage.error('模拟失败: ' + msg)
  } finally {
    simulating.value = false
  }
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
