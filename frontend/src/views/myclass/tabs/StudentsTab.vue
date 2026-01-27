<template>
  <div class="students-tab">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索学号、姓名..."
          :prefix-icon="Search"
          clearable
          class="search-input"
        />
        <el-select
          v-model="statusFilter"
          placeholder="学籍状态"
          clearable
          class="status-select"
        >
          <el-option label="在读" value="ENROLLED" />
          <el-option label="休学" value="SUSPENDED" />
          <el-option label="退学" value="DROPPED" />
          <el-option label="已毕业" value="GRADUATED" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button :icon="Download" @click="handleExport">
          导出名单
        </el-button>
      </div>
    </div>

    <!-- 学生列表 -->
    <el-table
      v-loading="loading"
      :data="filteredStudents"
      stripe
      class="students-table"
      empty-text="暂无学生数据"
    >
      <el-table-column label="状态" width="60" align="center">
        <template #default="{ row }">
          <span
            class="status-dot"
            :class="getStatusDotClass(row.status)"
            :title="StudentStatusConfig[row.status]?.label || row.status"
          />
        </template>
      </el-table-column>

      <el-table-column prop="studentNo" label="学号" width="140">
        <template #default="{ row }">
          <span class="student-no">{{ row.studentNo }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="name" label="姓名" width="100">
        <template #default="{ row }">
          <span class="student-name">{{ row.name }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="gender" label="性别" width="80" align="center">
        <template #default="{ row }">
          <span :class="row.gender === '男' ? 'gender-male' : 'gender-female'">
            {{ row.gender }}
          </span>
        </template>
      </el-table-column>

      <el-table-column prop="dormitoryName" label="宿舍" min-width="140">
        <template #default="{ row }">
          <span v-if="row.dormitoryName">
            {{ row.dormitoryName }}
            <span v-if="row.bedNo" class="bed-no">{{ row.bedNo }}床</span>
          </span>
          <span v-else class="no-data">未分配</span>
        </template>
      </el-table-column>

      <el-table-column prop="phone" label="联系电话" width="140">
        <template #default="{ row }">
          <span v-if="row.phone" class="phone-number">{{ row.phone }}</span>
          <span v-else class="no-data">-</span>
        </template>
      </el-table-column>

      <el-table-column prop="status" label="学籍状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag
            :type="StudentStatusConfig[row.status]?.type || 'info'"
            size="small"
          >
            {{ StudentStatusConfig[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleViewStudent(row)">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 统计信息 -->
    <div class="stats-footer">
      <div class="stats-item">
        <span class="stats-label">共</span>
        <span class="stats-value">{{ students.length }}</span>
        <span class="stats-label">人</span>
      </div>
      <div class="stats-divider" />
      <div class="stats-item">
        <span class="status-dot enrolled" />
        <span class="stats-label">在读</span>
        <span class="stats-value">{{ statusCounts.enrolled }}</span>
      </div>
      <div class="stats-item">
        <span class="status-dot suspended" />
        <span class="stats-label">休学</span>
        <span class="stats-value">{{ statusCounts.suspended }}</span>
      </div>
      <div class="stats-item">
        <span class="status-dot dropped" />
        <span class="stats-label">退学</span>
        <span class="stats-value">{{ statusCounts.dropped }}</span>
      </div>
      <div class="stats-item">
        <span class="status-dot graduated" />
        <span class="stats-label">毕业</span>
        <span class="stats-value">{{ statusCounts.graduated }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'
import { useDebounceFn } from '@vueuse/core'
import { getClassStudents } from '@/api/v2/myClass'
import type { MyClassStudent } from '@/types/v2/myClass'
import { StudentStatusConfig } from '@/types/v2/myClass'

const props = defineProps<{
  classId: string | number
}>()

// 状态
const loading = ref(false)
const students = ref<MyClassStudent[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')

// 加载学生列表
const loadStudents = async () => {
  loading.value = true
  try {
    const params: { keyword?: string; status?: string } = {}
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    if (statusFilter.value) {
      params.status = statusFilter.value
    }
    const response = await getClassStudents(props.classId, params)
    students.value = response || []
  } catch (error: any) {
    console.error('加载学生列表失败:', error)
    ElMessage.error(error.response?.data?.message || '加载学生列表失败')
  } finally {
    loading.value = false
  }
}

// 防抖搜索
const debouncedSearch = useDebounceFn(() => {
  loadStudents()
}, 300)

// 过滤后的学生列表（前端筛选备用）
const filteredStudents = computed(() => {
  return students.value
})

// 状态统计
const statusCounts = computed(() => {
  const counts = {
    enrolled: 0,
    suspended: 0,
    dropped: 0,
    graduated: 0
  }
  students.value.forEach(student => {
    switch (student.status) {
      case 'ENROLLED':
        counts.enrolled++
        break
      case 'SUSPENDED':
        counts.suspended++
        break
      case 'DROPPED':
        counts.dropped++
        break
      case 'GRADUATED':
        counts.graduated++
        break
    }
  })
  return counts
})

// 状态点样式
const getStatusDotClass = (status: string): string => {
  switch (status) {
    case 'ENROLLED':
      return 'enrolled'
    case 'SUSPENDED':
      return 'suspended'
    case 'DROPPED':
      return 'dropped'
    case 'GRADUATED':
      return 'graduated'
    default:
      return ''
  }
}

// 导出名单
const handleExport = () => {
  ElMessage.info('导出功能开发中...')
}

// 查看学生详情
const handleViewStudent = (student: MyClassStudent) => {
  ElMessage.info(`查看学生 ${student.name} 详情功能开发中...`)
}

// 监听搜索关键词变化
watch(searchKeyword, () => {
  debouncedSearch()
})

// 监听状态筛选变化
watch(statusFilter, () => {
  loadStudents()
})

// 监听 classId 变化
watch(() => props.classId, () => {
  loadStudents()
})

onMounted(() => {
  loadStudents()
})
</script>

<style lang="scss" scoped>
.students-tab {
  padding: 0;
}

// 工具栏
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;

  @media (max-width: 768px) {
    flex-direction: column;
    align-items: stretch;
  }
}

.toolbar-left {
  display: flex;
  gap: 12px;
  flex: 1;

  @media (max-width: 768px) {
    flex-direction: column;
  }
}

.search-input {
  width: 240px;

  @media (max-width: 768px) {
    width: 100%;
  }
}

.status-select {
  width: 140px;

  @media (max-width: 768px) {
    width: 100%;
  }
}

.toolbar-right {
  display: flex;
  gap: 12px;
}

// 学生列表
.students-table {
  background: white;
  border-radius: 12px;
  overflow: hidden;
}

.student-no {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 13px;
  color: #606266;
}

.student-name {
  font-weight: 500;
  color: #303133;
}

.gender-male {
  color: #409eff;
}

.gender-female {
  color: #f56c6c;
}

.bed-no {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

.phone-number {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 13px;
  color: #606266;
}

.no-data {
  color: #c0c4cc;
  font-size: 12px;
}

// 状态点
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #909399;

  &.enrolled {
    background: #67c23a;
  }

  &.suspended {
    background: #e6a23c;
  }

  &.dropped {
    background: #f56c6c;
  }

  &.graduated {
    background: #909399;
  }
}

// 统计信息
.stats-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: white;
  border-radius: 12px;
  margin-top: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebeef5;

  @media (max-width: 768px) {
    flex-wrap: wrap;
  }
}

.stats-item {
  display: flex;
  align-items: center;
  gap: 6px;

  .status-dot {
    width: 6px;
    height: 6px;
  }
}

.stats-label {
  font-size: 13px;
  color: #909399;
}

.stats-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.stats-divider {
  width: 1px;
  height: 20px;
  background: #dcdfe6;
}
</style>
