<template>
  <div class="analytics-center">
    <!-- Date Range Filter -->
    <el-card shadow="never" class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="8">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="loadAllData"
          />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadAllData">查询</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- Charts Grid -->
    <el-row :gutter="16">
      <!-- Class Ranking -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>班级排名 TOP 10</span>
          </template>
          <div v-loading="loading.ranking">
            <el-table :data="classRankingData" stripe size="small" max-height="360">
              <el-table-column type="index" label="排名" width="60" />
              <el-table-column prop="class_name" label="班级" min-width="120" />
              <el-table-column prop="avg_score" label="平均分" width="100">
                <template #default="{ row }">{{ Number(row.avg_score).toFixed(1) }}</template>
              </el-table-column>
              <el-table-column prop="check_count" label="检查次数" width="80" />
            </el-table>
          </div>
        </el-card>
      </el-col>

      <!-- Violation Distribution -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>违规分布 TOP 10</span>
          </template>
          <div v-loading="loading.violation">
            <el-table :data="violationData" stripe size="small" max-height="360">
              <el-table-column type="index" label="#" width="50" />
              <el-table-column prop="item_name" label="违规项" min-width="150" show-overflow-tooltip />
              <el-table-column prop="occurrence_count" label="次数" width="80" />
              <el-table-column prop="total_deduction" label="总扣分" width="80">
                <template #default="{ row }">{{ Number(row.total_deduction).toFixed(1) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>

      <!-- Inspector Workload -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>检查员工作量</span>
          </template>
          <div v-loading="loading.workload">
            <el-table :data="workloadData" stripe size="small" max-height="360">
              <el-table-column prop="inspector_name" label="检查员" min-width="120" />
              <el-table-column prop="session_count" label="检查次数" width="100" />
              <el-table-column prop="class_count" label="班级数" width="80" />
            </el-table>
          </div>
        </el-card>
      </el-col>

      <!-- Department Comparison -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>系部对比</span>
          </template>
          <div v-loading="loading.department">
            <el-table :data="departmentData" stripe size="small" max-height="360">
              <el-table-column prop="department_name" label="系部" min-width="120" />
              <el-table-column prop="avg_score" label="平均分" width="80">
                <template #default="{ row }">{{ Number(row.avg_score).toFixed(1) }}</template>
              </el-table-column>
              <el-table-column prop="class_count" label="班级数" width="80" />
              <el-table-column prop="record_count" label="记录数" width="80" />
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getClassRanking, getViolationDistribution, getInspectorWorkload, getDepartmentComparison } from '@/api/v2/inspectionAnalytics'

const today = new Date()
const thirtyDaysAgo = new Date(today)
thirtyDaysAgo.setDate(today.getDate() - 30)

const dateRange = ref<string[]>([
  thirtyDaysAgo.toISOString().slice(0, 10),
  today.toISOString().slice(0, 10)
])

const loading = reactive({
  ranking: false,
  violation: false,
  workload: false,
  department: false
})

const classRankingData = ref<Record<string, any>[]>([])
const violationData = ref<Record<string, any>[]>([])
const workloadData = ref<Record<string, any>[]>([])
const departmentData = ref<Record<string, any>[]>([])

async function loadAllData() {
  if (!dateRange.value || dateRange.value.length !== 2) return
  const params = { startDate: dateRange.value[0], endDate: dateRange.value[1] }

  loading.ranking = true
  loading.violation = true
  loading.workload = true
  loading.department = true

  try {
    const [ranking, violation, workload, department] = await Promise.allSettled([
      getClassRanking(params),
      getViolationDistribution(params),
      getInspectorWorkload(params),
      getDepartmentComparison(params)
    ])
    if (ranking.status === 'fulfilled') classRankingData.value = ranking.value.data || []
    if (violation.status === 'fulfilled') violationData.value = violation.value.data || []
    if (workload.status === 'fulfilled') workloadData.value = workload.value.data || []
    if (department.status === 'fulfilled') departmentData.value = department.value.data || []
  } catch (e: any) {
    ElMessage.error(e.message || '加载分析数据失败')
  } finally {
    loading.ranking = false
    loading.violation = false
    loading.workload = false
    loading.department = false
  }
}

onMounted(loadAllData)
</script>

<style scoped>
.analytics-center {
  padding: 0;
}
.filter-card {
  margin-bottom: 16px;
}
.chart-card {
  margin-bottom: 16px;
  min-height: 420px;
}
</style>
