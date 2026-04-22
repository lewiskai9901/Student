<template>
  <div class="space-y-5">
    <!-- 决策 1: 数据可见范围 -->
    <div>
      <div class="mb-2 flex items-center gap-2">
        <span
          class="flex h-5 w-5 items-center justify-center rounded-full bg-blue-100 text-[10px] font-bold text-blue-700"
          >1</span
        >
        <h4 class="text-sm font-medium text-gray-900">数据可见范围</h4>
        <span class="text-xs text-gray-400">主决策</span>
      </div>
      <div class="grid grid-cols-2 gap-2 md:grid-cols-5">
        <label
          v-for="opt in primaryOptions"
          :key="opt.code"
          class="relative flex cursor-pointer flex-col rounded-lg border p-3 text-left transition hover:border-blue-400 hover:bg-blue-50/30"
          :class="
            decision.primary === opt.code
              ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500'
              : 'border-gray-200 bg-white'
          "
        >
          <input
            type="radio"
            class="sr-only"
            :value="opt.code"
            :checked="decision.primary === opt.code"
            @change="updatePrimary(opt.code)"
          />
          <div class="flex items-center gap-1.5">
            <component :is="opt.icon" class="h-4 w-4" :class="decision.primary === opt.code ? 'text-blue-600' : 'text-gray-400'" />
            <span class="text-sm font-medium text-gray-900">{{ opt.label }}</span>
          </div>
          <span class="mt-1 text-[11px] leading-snug text-gray-500">{{ opt.desc }}</span>
        </label>
      </div>
    </div>

    <!-- CUSTOM 时内联展开组织树 -->
    <div
      v-if="decision.primary === 'CUSTOM'"
      class="rounded-lg border border-blue-200 bg-blue-50/40 p-3"
    >
      <CustomScopeTreePicker
        :org-ids="decision.customOrgIds || []"
        :grade-ids="decision.customGradeIds || []"
        :class-ids="decision.customClassIds || []"
        @update:org-ids="v => updateField('customOrgIds', v)"
        @update:grade-ids="v => updateField('customGradeIds', v)"
        @update:class-ids="v => updateField('customClassIds', v)"
      />
    </div>

    <!-- 决策 2: 学生特化 (仅 EDU 启用时) -->
    <div v-if="eduEnabled">
      <div class="mb-2 flex items-center gap-2">
        <span
          class="flex h-5 w-5 items-center justify-center rounded-full bg-orange-100 text-[10px] font-bold text-orange-700"
          >2</span
        >
        <h4 class="text-sm font-medium text-gray-900">我的学生</h4>
        <span class="text-xs text-gray-400">仅 EDU 启用时显示</span>
      </div>
      <div class="grid grid-cols-2 gap-2 md:grid-cols-4">
        <label
          v-for="opt in studentOptions"
          :key="opt.code"
          class="relative flex cursor-pointer flex-col rounded-lg border p-2.5 text-left transition hover:border-orange-400 hover:bg-orange-50/30"
          :class="
            decision.studentScope === opt.code
              ? 'border-orange-500 bg-orange-50 ring-1 ring-orange-500'
              : 'border-gray-200 bg-white'
          "
        >
          <input
            type="radio"
            class="sr-only"
            :value="opt.code"
            :checked="decision.studentScope === opt.code"
            @change="updateStudent(opt.code)"
          />
          <div class="flex items-center gap-1.5">
            <component :is="opt.icon" class="h-3.5 w-3.5" :class="decision.studentScope === opt.code ? 'text-orange-600' : 'text-gray-400'" />
            <span class="text-xs font-medium text-gray-900">{{ opt.label }}</span>
          </div>
          <span class="mt-0.5 text-[10px] leading-snug text-gray-500">{{ opt.desc }}</span>
        </label>
      </div>
    </div>

    <!-- 决策 3: 业务数据自动跟随 -->
    <div>
      <div class="mb-2 flex items-center gap-2">
        <span
          class="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-100 text-[10px] font-bold text-emerald-700"
          >3</span
        >
        <h4 class="text-sm font-medium text-gray-900">业务数据自动跟随</h4>
        <span class="text-xs text-gray-400">考勤/成绩/检查/任务 自动用上方范围</span>
      </div>
      <label
        class="flex cursor-pointer items-center gap-2 rounded-lg border border-gray-200 bg-white p-3 hover:bg-gray-50"
      >
        <input
          type="checkbox"
          :checked="decision.bizAutoFollow"
          class="h-4 w-4"
          @change="updateField('bizAutoFollow', ($event.target as HTMLInputElement).checked)"
        />
        <span class="text-sm text-gray-700">
          开启: 所有业务模块的 scope 自动 = 主决策
          <span class="text-xs text-gray-400">(关闭则业务模块默认 SELF, 需在"高级"里逐个配)</span>
        </span>
      </label>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Globe, Building2, User, Edit3, GraduationCap, BookOpen, Award } from 'lucide-vue-next'
import CustomScopeTreePicker from './CustomScopeTreePicker.vue'
import type { SceneDecision, PrimaryScope, StudentScope } from '../composables/useSceneTemplate'

interface Props {
  decision: SceneDecision
  eduEnabled: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:decision': [value: SceneDecision]
}>()

const primaryOptions = [
  { code: 'ALL' as PrimaryScope, label: '全部数据', desc: '所有组织的所有数据', icon: Globe },
  { code: 'DEPARTMENT_AND_BELOW' as PrimaryScope, label: '本部门及以下', desc: '含子部门数据', icon: Building2 },
  { code: 'DEPARTMENT' as PrimaryScope, label: '仅本部门', desc: '不含子部门', icon: Building2 },
  { code: 'SELF' as PrimaryScope, label: '仅本人', desc: '只看自己创建的', icon: User },
  { code: 'CUSTOM' as PrimaryScope, label: '自定义', desc: '手选组织/年级/班级', icon: Edit3 },
]

const studentOptions = [
  { code: 'ALL' as StudentScope, label: '全部学生', desc: '所有学生数据', icon: Globe },
  { code: 'BY_CLASS' as StudentScope, label: '我带的班级', desc: '按班级关系', icon: BookOpen },
  { code: 'BY_GRADE' as StudentScope, label: '我管的年级', desc: '按年级关系', icon: GraduationCap },
  { code: 'BY_MAJOR' as StudentScope, label: '我管的专业', desc: '按专业关系', icon: Award },
]

function updatePrimary(code: PrimaryScope) {
  emit('update:decision', {
    ...props.decision,
    primary: code,
    // 切换离开 CUSTOM 时清空自定义项
    customOrgIds: code === 'CUSTOM' ? props.decision.customOrgIds : undefined,
    customGradeIds: code === 'CUSTOM' ? props.decision.customGradeIds : undefined,
    customClassIds: code === 'CUSTOM' ? props.decision.customClassIds : undefined,
  })
}

function updateStudent(code: StudentScope) {
  emit('update:decision', { ...props.decision, studentScope: code })
}

function updateField<K extends keyof SceneDecision>(key: K, value: SceneDecision[K]) {
  emit('update:decision', { ...props.decision, [key]: value })
}
</script>
