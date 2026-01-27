<template>
  <div class="my-class-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>我的班级</h2>
      <p class="subtitle">管理您负责的班级，查看学生和数据统计</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 空状态 -->
    <el-empty
      v-else-if="classes.length === 0"
      description="您暂未分配任何班级"
    >
      <template #image>
        <el-icon :size="64" color="#909399"><School /></el-icon>
      </template>
    </el-empty>

    <!-- 班级卡片网格 -->
    <div v-else class="class-grid">
      <div
        v-for="item in classes"
        :key="item.id"
        class="class-card"
        @click="goToClassDetail(item.id)"
      >
        <div class="card-header">
          <div class="class-info">
            <span class="class-name">{{ item.className }}</span>
            <el-tag size="small" :type="getStatusType(item.status)">
              {{ getStatusLabel(item.status) }}
            </el-tag>
          </div>
          <el-tag size="small" :color="getRoleColor(item.myRole)" effect="dark">
            {{ getRoleLabel(item.myRole) }}
          </el-tag>
        </div>

        <div class="card-stats">
          <div class="stat-item">
            <el-icon><User /></el-icon>
            <span>{{ item.currentSize }}人</span>
          </div>
          <div v-if="item.weeklyRank" class="stat-item">
            <el-icon><Trophy /></el-icon>
            <span :class="getRankClass(item.weeklyRank, item.totalClasses)">
              第{{ item.weeklyRank }}/{{ item.totalClasses }}名
            </span>
          </div>
          <div v-if="item.weeklyScore" class="stat-item">
            <el-icon><DataAnalysis /></el-icon>
            <span>{{ item.weeklyScore?.toFixed(1) }}分</span>
          </div>
        </div>

        <!-- 趋势迷你图 -->
        <div v-if="item.scoreTrend?.length" class="trend-mini">
          <MiniTrendChart :data="item.scoreTrend" />
        </div>

        <div class="card-footer">
          <span class="enrollment">{{ item.enrollmentYear }}级</span>
          <el-button type="primary" text>
            进入 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Trophy, DataAnalysis, ArrowRight, School } from '@element-plus/icons-vue'
import { getMyClasses } from '@/api/myClass'
import type { MyClassItem } from '@/types/myClass'
import MiniTrendChart from './components/MiniTrendChart.vue'

const router = useRouter()
const loading = ref(true)
const classes = ref<MyClassItem[]>([])

const fetchClasses = async () => {
  try {
    loading.value = true
    classes.value = await getMyClasses()

    // 如果只有一个班级，直接跳转详情页
    if (classes.value.length === 1) {
      goToClassDetail(classes.value[0].id)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取班级列表失败')
  } finally {
    loading.value = false
  }
}

const goToClassDetail = (classId: string | number) => {
  router.push(`/my-class/${classId}`)
}

const getStatusType = (status: string) => {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    PREPARING: 'info',
    ACTIVE: 'success',
    GRADUATED: 'warning',
    DISSOLVED: 'danger'
  }
  return map[status] || 'info'
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    PREPARING: '筹建中',
    ACTIVE: '在读中',
    GRADUATED: '已毕业',
    DISSOLVED: '已撤销'
  }
  return map[status] || status
}

const getRoleColor = (role: string) => {
  const map: Record<string, string> = {
    HEAD_TEACHER: '#409EFF',
    DEPUTY_HEAD_TEACHER: '#67C23A',
    SUBJECT_TEACHER: '#909399',
    COUNSELOR: '#E6A23C'
  }
  return map[role] || '#909399'
}

const getRoleLabel = (role: string) => {
  const map: Record<string, string> = {
    HEAD_TEACHER: '班主任',
    DEPUTY_HEAD_TEACHER: '副班主任',
    SUBJECT_TEACHER: '任课教师',
    COUNSELOR: '辅导员'
  }
  return map[role] || role
}

const getRankClass = (rank: number, total: number | undefined) => {
  if (rank <= 3) return 'rank-top'
  if (total && rank > total * 0.7) return 'rank-warning'
  return ''
}

onMounted(fetchClasses)
</script>

<style scoped lang="scss">
.my-class-list {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;

  h2 {
    margin: 0 0 8px 0;
    font-size: 24px;
    font-weight: 600;
  }

  .subtitle {
    margin: 0;
    color: #909399;
  }
}

.loading-container {
  padding: 40px;
}

.class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.class-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;

  .class-info {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .class-name {
    font-size: 18px;
    font-weight: 600;
  }
}

.card-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;

  .stat-item {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #606266;
    font-size: 14px;

    .el-icon {
      color: #909399;
    }
  }

  .rank-top {
    color: #E6A23C;
    font-weight: 600;
  }

  .rank-warning {
    color: #F56C6C;
  }
}

.trend-mini {
  height: 40px;
  margin-bottom: 16px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #EBEEF5;

  .enrollment {
    color: #909399;
    font-size: 14px;
  }
}
</style>
