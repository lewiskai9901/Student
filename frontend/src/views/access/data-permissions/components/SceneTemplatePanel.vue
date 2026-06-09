<template>
  <div class="space-y-5">
    <!-- 决策 1: 数据可见范围 (核心 scope, label 来自后端字典) -->
    <div>
      <div class="mb-2 flex items-center gap-2">
        <span class="flex h-5 w-5 items-center justify-center rounded-full bg-blue-100 text-[10px] font-bold text-blue-700">1</span>
        <h4 class="text-sm font-medium text-gray-900">数据可见范围</h4>
        <span class="text-xs text-gray-400">主决策</span>
      </div>
      <div class="grid grid-cols-2 gap-2 md:grid-cols-5">
        <label
          v-for="opt in primaryOptions"
          :key="opt.code"
          class="relative flex cursor-pointer flex-col rounded-lg border p-3 text-left transition hover:border-blue-400 hover:bg-blue-50/30"
          :class="decision.primary === opt.code ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500' : 'border-gray-200 bg-white'"
        >
          <input type="radio" class="sr-only" :value="opt.code" :checked="decision.primary === opt.code" @change="updatePrimary(opt.code)" />
          <div class="flex items-center gap-1.5">
            <component :is="opt.icon" class="h-4 w-4" :class="decision.primary === opt.code ? 'text-blue-600' : 'text-gray-400'" />
            <span class="text-sm font-medium text-gray-900">{{ opt.label }}</span>
          </div>
          <span class="mt-1 text-[11px] leading-snug text-gray-500">{{ opt.desc }}</span>
        </label>
      </div>
    </div>

    <!-- CUSTOM 时内联展开组织树 -->
    <div v-if="decision.primary === 'CUSTOM'" class="rounded-lg border border-blue-200 bg-blue-50/40 p-3">
      <CustomScopeTreePicker
        :org-ids="decision.customOrgIds || []"
        @update:org-ids="v => updateField('customOrgIds', v)"
      />
    </div>

    <!-- 决策 2..N: 行业特化维度 (插件贡献, 仅对应插件启用时出现; 核心零行业概念) -->
    <div v-for="spec in specializations" :key="spec.groupCode">
      <div class="mb-2 flex items-center gap-2">
        <span class="flex h-5 w-5 items-center justify-center rounded-full bg-orange-100 text-[10px] font-bold text-orange-700">2</span>
        <h4 class="text-sm font-medium text-gray-900">{{ spec.title }}</h4>
        <span class="text-xs text-gray-400">{{ spec.pluginCode }} 插件特化</span>
      </div>
      <div class="grid grid-cols-2 gap-2 md:grid-cols-4">
        <label
          v-for="opt in spec.options"
          :key="opt.code"
          class="relative flex cursor-pointer flex-col rounded-lg border p-2.5 text-left transition hover:border-orange-400 hover:bg-orange-50/30"
          :class="(decision.specializations?.[spec.groupCode]) === opt.code ? 'border-orange-500 bg-orange-50 ring-1 ring-orange-500' : 'border-gray-200 bg-white'"
        >
          <input type="radio" class="sr-only" :value="opt.code" :checked="(decision.specializations?.[spec.groupCode]) === opt.code" @change="updateSpecialization(spec.groupCode, opt.code)" />
          <div class="flex items-center gap-1.5">
            <SlidersHorizontal class="h-3.5 w-3.5" :class="(decision.specializations?.[spec.groupCode]) === opt.code ? 'text-orange-600' : 'text-gray-400'" />
            <span class="text-xs font-medium text-gray-900">{{ opt.label }}</span>
          </div>
          <span class="mt-0.5 text-[10px] leading-snug text-gray-500">{{ opt.desc }}</span>
        </label>
      </div>
    </div>

    <!-- 决策 3: 业务数据自动跟随 -->
    <div>
      <div class="mb-2 flex items-center gap-2">
        <span class="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-100 text-[10px] font-bold text-emerald-700">3</span>
        <h4 class="text-sm font-medium text-gray-900">业务数据自动跟随</h4>
        <span class="text-xs text-gray-400">其它业务模块自动用上方范围</span>
      </div>
      <label class="flex cursor-pointer items-center gap-2 rounded-lg border border-gray-200 bg-white p-3 hover:bg-gray-50">
        <input type="checkbox" :checked="decision.bizAutoFollow" class="h-4 w-4" @change="updateField('bizAutoFollow', ($event.target as HTMLInputElement).checked)" />
        <span class="text-sm text-gray-700">
          开启: 所有业务模块的 scope 自动 = 主决策
          <span class="text-xs text-gray-400">(关闭则业务模块默认 SELF, 需在"高级"里逐个配)</span>
        </span>
      </label>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Globe, Building2, User, Edit3, SlidersHorizontal } from 'lucide-vue-next'
import CustomScopeTreePicker from './CustomScopeTreePicker.vue'
import type { SceneDecision, PrimaryScope } from '../composables/useSceneTemplate'
import type { DataScopeOption } from '@/types/access'
import { usePluginsStore } from '@/stores/plugins'
import { enabledScopeSpecializations } from '../dataScopeSpecializations'

interface Props {
  decision: SceneDecision
  /** 后端 scope 字典 (核心 + 插件维度, 带中文 label) */
  scopeOptions: DataScopeOption[]
}

const props = defineProps<Props>()
const emit = defineEmits<{ 'update:decision': [value: SceneDecision] }>()

const pluginsStore = usePluginsStore()

/** 核心 scope 的图标 (纯展示) */
const PRIMARY_ICON: Record<string, any> = {
  ALL: Globe,
  DEPARTMENT_AND_BELOW: Building2,
  DEPARTMENT: Building2,
  SELF: User,
  CUSTOM: Edit3,
}
const PRIMARY_ORDER = ['ALL', 'DEPARTMENT_AND_BELOW', 'DEPARTMENT', 'SELF', 'CUSTOM']

/** 主决策选项 = 后端字典里 source=CORE 的 scope (label/desc 来自后端, 不再前端写死) */
const primaryOptions = computed(() => {
  const core = (props.scopeOptions || []).filter(s => s.source === 'CORE' && PRIMARY_ICON[s.scopeCode])
  const list = core.length
    ? core.map(s => ({ code: s.scopeCode as PrimaryScope, label: s.scopeName, desc: s.description || '', icon: PRIMARY_ICON[s.scopeCode] }))
    : PRIMARY_ORDER.map(c => ({ code: c as PrimaryScope, label: c, desc: '', icon: PRIMARY_ICON[c] }))
  return list.sort((a, b) => PRIMARY_ORDER.indexOf(a.code) - PRIMARY_ORDER.indexOf(b.code))
})

/** 行业特化维度 = 已启用插件贡献的 specs (EDU 关掉则为空, 核心向导自动无该分区) */
const specializations = computed(() => enabledScopeSpecializations(pluginsStore.codes))

function updatePrimary(code: PrimaryScope) {
  emit('update:decision', {
    ...props.decision,
    primary: code,
    customOrgIds: code === 'CUSTOM' ? props.decision.customOrgIds : undefined,
  })
}

function updateSpecialization(groupCode: string, scopeCode: string) {
  emit('update:decision', {
    ...props.decision,
    specializations: { ...(props.decision.specializations || {}), [groupCode]: scopeCode },
  })
}

function updateField<K extends keyof SceneDecision>(key: K, value: SceneDecision[K]) {
  emit('update:decision', { ...props.decision, [key]: value })
}
</script>
