<template>
  <div class="rounded-xl border border-gray-200 bg-white">
    <div class="flex items-center justify-between border-b border-gray-200 px-5 py-3">
      <span class="text-sm font-semibold text-gray-900">节次时间配置</span>
      <div class="flex items-center gap-2">
        <el-select v-model="localSemesterId" placeholder="选择学期" size="small" class="!w-44">
          <el-option label="全校默认" :value="0" />
          <el-option v-for="sem in semesters" :key="sem.id" :label="sem.name" :value="sem.id" />
        </el-select>
        <button
          class="inline-flex items-center gap-1 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700"
          @click="$emit('add')"
        >
          添加节次
        </button>
      </div>
    </div>
    <div class="p-4">
      <el-table :data="periodConfigs" border size="small">
        <el-table-column prop="period" label="序号" width="80" align="center" />
        <el-table-column prop="name" label="名称" width="120" align="center" />
        <el-table-column prop="startTime" label="开始时间" width="120" align="center" />
        <el-table-column prop="endTime" label="结束时间" width="120" align="center" />
        <el-table-column label="时段" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.period <= 4 ? 'success' : row.period <= 8 ? 'warning' : 'info'">
              {{ row.period <= 4 ? '上午' : row.period <= 8 ? '下午' : '晚上' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button text size="small" @click="$emit('edit', row)">编辑</el-button>
            <el-button text size="small" type="danger" @click="$emit('delete', row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="mt-3 flex items-center gap-1.5 text-xs text-gray-400">
        <el-icon><InfoFilled /></el-icon>
        <span>作息时间是排课的基础配置，可以为不同学期设置不同的作息时间（如夏季/冬季作息）</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import type { Semester } from '@/types/teaching'

const props = defineProps<{
  semesters: Semester[]
  periodConfigs: any[]
  periodConfigSemester: number
}>()

const emit = defineEmits<{
  'update:periodConfigSemester': [value: number]
  add: []
  edit: [row: any]
  delete: [row: any]
}>()

const localSemesterId = ref(props.periodConfigSemester)
watch(localSemesterId, (v) => emit('update:periodConfigSemester', v))
watch(() => props.periodConfigSemester, (v) => { localSemesterId.value = v })
</script>
