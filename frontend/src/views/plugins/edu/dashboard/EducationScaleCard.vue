<template>
  <!-- 办学规模统计条 — 教育插件向总览看板贡献的 education 分区
       (后端 EducationDashboardContributor 产出, EDU 禁用时分区与本卡同时消失) -->
  <div class="rounded-lg border border-gray-200 bg-white px-5 py-4">
    <div class="mb-2 text-sm font-medium text-gray-500">办学规模</div>
    <div class="flex items-center gap-0 divide-x divide-gray-200 text-center">
      <div class="flex-1 cursor-pointer px-3 transition-colors hover:bg-gray-50" @click="goTo('/academic/majors')">
        <div class="text-2xl font-semibold text-gray-900">{{ stats.majorCount ?? 0 }}</div>
        <div class="mt-0.5 text-xs text-gray-500">专业</div>
      </div>
      <div class="flex-1 cursor-pointer px-3 transition-colors hover:bg-gray-50" @click="goTo('/student/classes')">
        <div class="text-2xl font-semibold text-gray-900">{{ stats.classCount ?? 0 }}</div>
        <div class="mt-0.5 text-xs text-gray-500">班级</div>
      </div>
      <div class="flex-1 cursor-pointer px-3 transition-colors hover:bg-gray-50" @click="goTo('/student/list')">
        <div class="text-2xl font-semibold text-gray-900">{{ stats.studentCount ?? 0 }}</div>
        <div class="mt-0.5 text-xs text-gray-500">学生</div>
      </div>
      <div class="flex-1 cursor-pointer px-3 transition-colors hover:bg-gray-50" @click="goTo('/system/teachers')">
        <div class="text-2xl font-semibold text-gray-900">{{ stats.teacherCount ?? 0 }}</div>
        <div class="mt-0.5 text-xs text-gray-500">教师</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

interface EducationStats {
  majorCount?: number
  classCount?: number
  studentCount?: number | string
  teacherCount?: number | string
}

const props = defineProps<{ data?: Record<string, unknown> }>()
const stats = computed<EducationStats>(() => (props.data ?? {}) as EducationStats)

const router = useRouter()
const goTo = (path: string) => router.push(path)
</script>
