<template>
  <div class="flex h-full flex-col bg-white">
    <!-- 搜索 -->
    <div class="border-b border-gray-200 p-3">
      <div class="relative">
        <Search class="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <input
          v-model="keyword"
          type="text"
          placeholder="搜角色名/编码"
          class="h-8 w-full rounded-md border border-gray-200 pl-8 pr-2 text-xs focus:border-blue-500 focus:outline-none"
        />
      </div>
      <div class="mt-2 flex items-center gap-1 text-xs">
        <label class="flex items-center gap-1 text-gray-500 cursor-pointer">
          <input type="checkbox" v-model="compareMode" class="h-3 w-3" />
          对比模式
        </label>
        <span v-if="compareMode" class="ml-auto text-[10px] text-blue-600">
          已选 {{ comparedIds.length }}
        </span>
      </div>
    </div>

    <!-- 角色分组列表 -->
    <div class="flex-1 overflow-y-auto">
      <div v-if="loading" class="flex h-40 items-center justify-center text-sm text-gray-400">
        <Loader2 class="h-5 w-5 animate-spin" />
      </div>
      <template v-else>
        <div
          v-for="group in groupedRoles"
          :key="group.industry"
          class="border-b border-gray-100 last:border-b-0"
        >
          <button
            class="flex w-full items-center gap-2 px-3 py-2 text-left text-xs font-medium text-gray-600 hover:bg-gray-50"
            @click="toggleGroup(group.industry)"
          >
            <ChevronDown
              class="h-3 w-3 transition-transform"
              :class="{ '-rotate-90': !isGroupExpanded(group.industry) }"
            />
            <span
              class="inline-block h-2 w-2 rounded-full"
              :style="{ background: industryColor(group.industry) }"
            ></span>
            <span>{{ industryLabel(group.industry) }}</span>
            <span class="ml-auto rounded bg-gray-100 px-1.5 py-0.5 text-[10px] text-gray-500">
              {{ group.roles.length }}
            </span>
          </button>
          <div v-if="isGroupExpanded(group.industry)" class="pb-1">
            <button
              v-for="role in group.roles"
              :key="role.id"
              class="group flex w-full items-center gap-2 border-l-2 px-3 py-2 text-left text-sm hover:bg-blue-50/50"
              :class="[
                String(role.id) === String(modelValue)
                  ? 'border-blue-500 bg-blue-50 text-blue-700'
                  : 'border-transparent text-gray-700',
                role.pluginEnabled === false ? 'opacity-50' : '',
              ]"
              @click="selectRole(role)"
            >
              <input
                v-if="compareMode"
                type="checkbox"
                :checked="comparedIds.some(id => String(id) === String(role.id))"
                class="h-3.5 w-3.5"
                @click.stop
                @change="toggleCompare(role)"
              />
              <Shield
                class="h-3.5 w-3.5 flex-shrink-0"
                :class="String(role.id) === String(modelValue) ? 'text-blue-500' : 'text-gray-400'"
              />
              <div class="min-w-0 flex-1 truncate">
                <div class="truncate text-xs font-medium">{{ role.roleName }}</div>
                <div class="truncate font-mono text-[10px] text-gray-400">{{ role.roleCode }}</div>
              </div>
              <span
                v-if="role.pluginEnabled === false"
                class="flex-shrink-0 rounded-full bg-orange-100 px-1 py-0.5 text-[9px] text-orange-600"
                title="插件已禁用"
              >
                禁
              </span>
            </button>
          </div>
        </div>
        <div v-if="groupedRoles.length === 0" class="py-10 text-center text-sm text-gray-400">
          暂无角色
        </div>
      </template>
    </div>

    <!-- 对比模式下的操作 -->
    <div v-if="compareMode && comparedIds.length >= 2" class="border-t border-gray-200 p-3">
      <button
        class="h-8 w-full rounded-md bg-blue-600 text-xs font-medium text-white hover:bg-blue-700"
        @click="$emit('compare', comparedIds)"
      >
        对比这 {{ comparedIds.length }} 个角色
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Search, Loader2, Shield, ChevronDown } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { getRolesPage, type RoleResponse } from '@/api/access'

interface Props {
  modelValue: number | string | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: number | string]
  compare: [ids: (number | string)[]]
}>()

const keyword = ref('')
const compareMode = ref(false)
const comparedIds = ref<(number | string)[]>([])
const loading = ref(false)
const roles = ref<RoleResponse[]>([])
const expandedGroups = ref<Set<string>>(new Set(['CORE', 'EDU', 'HEALTH', 'CARE', 'CUSTOM']))

const INDUSTRY_LABELS: Record<string, string> = {
  CORE: '通用核心',
  EDU: '教育行业',
  HEALTH: '医疗行业',
  CARE: '养老行业',
  CUSTOM: '自定义',
}
const INDUSTRY_ORDER = ['CORE', 'EDU', 'HEALTH', 'CARE', 'CUSTOM']

function industryLabel(code: string): string {
  return INDUSTRY_LABELS[code] || code
}
function industryColor(code: string): string {
  return (
    {
      CORE: '#2563eb',
      EDU: '#d97706',
      HEALTH: '#be185d',
      CARE: '#059669',
      CUSTOM: '#6b7280',
    } as Record<string, string>
  )[code] || '#6b7280'
}

function inferIndustry(role: RoleResponse): string {
  // 优先用后端 industry 字段 (如果有)
  const anyRole = role as any
  if (anyRole.industry) return anyRole.industry
  // 兜底: 按 roleCode 前缀推断
  const code = (role.roleCode || '').toUpperCase()
  if (code.startsWith('EDU_') || /TEACHER|STUDENT|GRADE|CLASS/i.test(code)) return 'EDU'
  if (code.startsWith('HEALTH_') || /DOCTOR|NURSE|PATIENT/i.test(code)) return 'HEALTH'
  if (code.startsWith('CARE_') || /ELDERLY|CAREGIVER/i.test(code)) return 'CARE'
  if (code.startsWith('CUSTOM_')) return 'CUSTOM'
  return 'CORE'
}

interface RoleGroup {
  industry: string
  roles: RoleResponse[]
}

const groupedRoles = computed<RoleGroup[]>(() => {
  const kw = keyword.value.trim().toLowerCase()
  const filtered = kw
    ? roles.value.filter(
        r =>
          r.roleName?.toLowerCase().includes(kw) ||
          r.roleCode?.toLowerCase().includes(kw)
      )
    : roles.value

  const byIndustry: Record<string, RoleResponse[]> = {}
  for (const r of filtered) {
    const ind = inferIndustry(r)
    if (!byIndustry[ind]) byIndustry[ind] = []
    byIndustry[ind].push(r)
  }

  const groups: RoleGroup[] = []
  for (const ind of INDUSTRY_ORDER) {
    if (byIndustry[ind]?.length) groups.push({ industry: ind, roles: byIndustry[ind] })
  }
  for (const [ind, list] of Object.entries(byIndustry)) {
    if (!INDUSTRY_ORDER.includes(ind)) groups.push({ industry: ind, roles: list })
  }
  return groups
})

function isGroupExpanded(industry: string): boolean {
  return expandedGroups.value.has(industry)
}

function toggleGroup(industry: string) {
  if (expandedGroups.value.has(industry)) expandedGroups.value.delete(industry)
  else expandedGroups.value.add(industry)
}

function selectRole(role: RoleResponse) {
  // 允许选中 pluginEnabled=false 的角色 — 管理员需要查看被禁角色配置.
  // 主区 PermissionConfigurator 会顶部显 "插件已禁用" banner + 禁用保存按钮.
  emit('update:modelValue', role.id)
}

function toggleCompare(role: RoleResponse) {
  const idx = comparedIds.value.findIndex(id => String(id) === String(role.id))
  if (idx > -1) comparedIds.value.splice(idx, 1)
  else {
    if (comparedIds.value.length >= 5) {
      ElMessage.warning('最多对比 5 个角色')
      return
    }
    comparedIds.value.push(role.id)
  }
}

watch(compareMode, (v) => {
  if (!v) comparedIds.value = []
})

async function loadRoles() {
  loading.value = true
  try {
    const res = await getRolesPage({
      pageNum: 1,
      pageSize: 500,
      includeDisabled: true,
    })
    roles.value = res.records
  } catch (e) {
    ElMessage.error('加载角色失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRoles()
})

defineExpose({ reload: loadRoles })
</script>
