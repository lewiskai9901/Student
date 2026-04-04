<template>
  <div class="flex gap-4">
    <!-- Left: Grade filter -->
    <div class="w-48 shrink-0 rounded-xl border border-gray-200 bg-white">
      <div class="border-b border-gray-200 px-4 py-3">
        <span class="text-sm font-semibold text-gray-900">年级筛选</span>
      </div>
      <div class="p-2">
        <div
          class="flex cursor-pointer items-center justify-between rounded-lg px-3 py-2 text-sm transition-colors"
          :class="selectedGrade === 'all' ? 'bg-blue-50 font-medium text-blue-700' : 'text-gray-700 hover:bg-gray-50'"
          @click="selectedGrade = 'all'"
        >
          <span>全部年级</span>
          <span class="text-xs text-gray-400">{{ gradeActivities.length }}</span>
        </div>
        <div
          v-for="grade in grades"
          :key="grade.id"
          class="flex cursor-pointer items-center justify-between rounded-lg px-3 py-2 text-sm transition-colors"
          :class="selectedGrade === grade.id ? 'bg-blue-50 font-medium text-blue-700' : 'text-gray-700 hover:bg-gray-50'"
          @click="selectedGrade = grade.id"
        >
          <span>{{ grade.name }}</span>
          <span class="text-xs text-gray-400">{{ getGradeActivityCount(grade.id) }}</span>
        </div>
      </div>
    </div>

    <!-- Right: Activity list -->
    <div class="min-w-0 flex-1 rounded-xl border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-5 py-3">
        <span class="text-sm font-semibold text-gray-900">年级活动列表</span>
        <div class="flex items-center gap-2">
          <el-select v-model="activityType" placeholder="活动类型" size="small" clearable class="!w-28">
            <el-option label="军训" :value="1" />
            <el-option label="入学教育" :value="2" />
            <el-option label="专业实习" :value="3" />
            <el-option label="社会实践" :value="4" />
            <el-option label="毕业实习" :value="5" />
            <el-option label="毕业设计" :value="6" />
            <el-option label="毕业答辩" :value="7" />
            <el-option label="其他" :value="10" />
          </el-select>
          <button
            class="inline-flex items-center gap-1 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700"
            @click="$emit('add')"
          >
            添加活动
          </button>
        </div>
      </div>
      <div class="p-4">
        <el-table :data="filteredActivities" border size="small">
          <el-table-column prop="gradeName" label="年级" width="100" />
          <el-table-column prop="activityName" label="活动名称" min-width="150" />
          <el-table-column label="活动类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ getActivityTypeName(row.activityType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="200">
            <template #default="{ row }">
              {{ row.startDate }} ~ {{ row.endDate || row.startDate }}
            </template>
          </el-table-column>
          <el-table-column label="周次" width="100">
            <template #default="{ row }">
              <span v-if="row.startWeek">第{{ row.startWeek }}-{{ row.endWeek || row.startWeek }}周</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="影响教学" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.affectTeaching ? 'warning' : 'info'" size="small">
                {{ row.affectTeaching ? '是' : '否' }}
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
        <el-empty v-if="!filteredActivities.length" description="暂无年级活动" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  grades: { id: number; name: string }[]
  gradeActivities: any[]
}>()

defineEmits<{
  add: []
  edit: [row: any]
  delete: [row: any]
}>()

const selectedGrade = ref<string | number>('all')
const activityType = ref<number | null>(null)

const getGradeActivityCount = (gradeId: number) =>
  props.gradeActivities.filter(a => a.gradeId === gradeId).length

const filteredActivities = computed(() => {
  let result = props.gradeActivities
  if (selectedGrade.value !== 'all') {
    result = result.filter(a => a.gradeId === selectedGrade.value)
  }
  if (activityType.value) {
    result = result.filter(a => a.activityType === activityType.value)
  }
  return result
})

const getActivityTypeName = (type: number) => {
  const map: Record<number, string> = {
    1: '军训', 2: '入学教育', 3: '专业实习', 4: '社会实践',
    5: '毕业实习', 6: '毕业设计', 7: '毕业答辩', 8: '毕业典礼',
    9: '就业指导', 10: '其他'
  }
  return map[type] || '其他'
}
</script>
