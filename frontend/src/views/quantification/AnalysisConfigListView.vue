<template>
  <div class="analysis-config-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <el-page-header @back="goBack">
          <template #content>
            <span class="page-title">统计分析配置</span>
          </template>
        </el-page-header>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="createConfig">
          <el-icon><Plus /></el-icon>
          新建配置
        </el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="检查计划">
          <el-select
            v-model="filters.planId"
            placeholder="选择检查计划"
            clearable
            filterable
            style="width: 200px"
            @change="loadConfigs"
          >
            <el-option
              v-for="plan in checkPlans"
              :key="plan.id"
              :label="plan.planName"
              :value="plan.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="配置名称">
          <el-input
            v-model="filters.configName"
            placeholder="搜索配置名称"
            clearable
            style="width: 200px"
            @clear="loadConfigs"
            @keyup.enter="loadConfigs"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="filters.isEnabled"
            placeholder="全部状态"
            clearable
            style="width: 120px"
            @change="loadConfigs"
          >
            <el-option label="已启用" :value="true" />
            <el-option label="已禁用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadConfigs">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 配置列表 -->
    <el-card class="list-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="configs"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="configName" label="配置名称" min-width="180">
          <template #default="{ row }">
            <div class="config-name">
              <el-tag v-if="row.isDefault" type="warning" size="small" class="default-tag">默认</el-tag>
              <span>{{ row.configName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="planName" label="检查计划" min-width="150" />
        <el-table-column prop="scopeType" label="范围类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getScopeTypeTag(row.scopeType)" size="small">
              {{ getScopeTypeLabel(row.scopeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateMode" label="更新模式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.updateMode === 'dynamic' ? 'success' : 'info'" size="small">
              {{ row.updateMode === 'dynamic' ? '动态' : '静态' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="missingStrategy" label="缺检策略" width="120">
          <template #default="{ row }">
            {{ getMissingStrategyLabel(row.missingStrategy) }}
          </template>
        </el-table-column>
        <el-table-column prop="metricCount" label="指标数" width="80" align="center" />
        <el-table-column prop="snapshotCount" label="快照数" width="80" align="center" />
        <el-table-column prop="isEnabled" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.isEnabled"
              size="small"
              @change="toggleEnabled(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewAnalysis(row)">
              <el-icon><DataAnalysis /></el-icon>
              分析
            </el-button>
            <el-button type="primary" link size="small" @click="editConfig(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="primary" link size="small" @click="copyConfig(row)">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
            <el-dropdown trigger="click" @command="handleCommand($event, row)">
              <el-button type="primary" link size="small">
                更多
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="setDefault" :disabled="row.isDefault">
                    <el-icon><Star /></el-icon>
                    设为默认
                  </el-dropdown-item>
                  <el-dropdown-item command="viewSnapshots">
                    <el-icon><Clock /></el-icon>
                    历史快照
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <el-icon><Delete /></el-icon>
                    <span style="color: #f56c6c">删除</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadConfigs"
          @current-change="loadConfigs"
        />
      </div>
    </el-card>

    <!-- 配置向导对话框 -->
    <AnalysisConfigWizard
      v-model:visible="wizardVisible"
      :config-id="currentConfigId"
      :plan-id="filters.planId"
      @success="onWizardSuccess"
    />

    <!-- 快照列表对话框 -->
    <el-dialog
      v-model="snapshotDialogVisible"
      title="历史快照"
      width="800px"
      destroy-on-close
    >
      <el-table :data="snapshots" stripe border>
        <el-table-column prop="generatedAt" label="生成时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.generatedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="dateRangeStart" label="数据范围" min-width="200">
          <template #default="{ row }">
            {{ row.dateRangeStart || '-' }} ~ {{ row.dateRangeEnd || '至今' }}
          </template>
        </el-table-column>
        <el-table-column prop="recordCount" label="记录数" width="100" align="center" />
        <el-table-column prop="classCount" label="班级数" width="100" align="center" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewSnapshot(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Search, Edit, Delete, CopyDocument, Star, Clock, DataAnalysis, ArrowDown
} from '@element-plus/icons-vue'
import { getCheckPlanPage, type CheckPlan } from '@/api/quantification'
import {
  getConfigPage,
  deleteConfig,
  toggleConfigEnabled,
  setDefaultConfig,
  copyConfig as apiCopyConfig,
  getSnapshots,
  type AnalysisConfig,
  type AnalysisSnapshot
} from '@/api/quantification-extra'
import { formatDateTime } from '@/utils/date'
import AnalysisConfigWizard from './components/analysis/AnalysisConfigWizard.vue'

const router = useRouter()

// 状态
const loading = ref(false)
const configs = ref<AnalysisConfig[]>([])
const checkPlans = ref<CheckPlan[]>([])
const wizardVisible = ref(false)
const currentConfigId = ref<number | string | null>(null)
const snapshotDialogVisible = ref(false)
const snapshots = ref<AnalysisSnapshot[]>([])

// 筛选条件
const filters = reactive({
  planId: undefined as number | undefined,
  configName: '',
  isEnabled: undefined as boolean | undefined
})

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 加载检查计划列表
async function loadCheckPlans() {
  try {
    const res = await getCheckPlanPage({ pageNum: 1, pageSize: 100, status: 1 })
    checkPlans.value = res.data?.records || []
  } catch (error) {
    console.error('加载检查计划失败:', error)
  }
}

// 加载配置列表
async function loadConfigs() {
  loading.value = true
  try {
    const res = await getConfigPage({
      ...filters,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    configs.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error('加载配置列表失败:', error)
    ElMessage.error('加载配置列表失败')
  } finally {
    loading.value = false
  }
}

// 重置筛选
function resetFilters() {
  filters.planId = undefined
  filters.configName = ''
  filters.isEnabled = undefined
  pagination.pageNum = 1
  loadConfigs()
}

// 新建配置
function createConfig() {
  currentConfigId.value = null
  wizardVisible.value = true
}

// 编辑配置
function editConfig(config: AnalysisConfig) {
  currentConfigId.value = config.id!
  wizardVisible.value = true
}

// 查看分析结果
function viewAnalysis(config: AnalysisConfig) {
  router.push({
    name: 'AnalysisResult',
    params: { configId: config.id }
  })
}

// 复制配置
async function copyConfig(config: AnalysisConfig) {
  try {
    await ElMessageBox.prompt('请输入新配置名称', '复制配置', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: `${config.configName} - 副本`,
      inputPattern: /^.{1,100}$/,
      inputErrorMessage: '配置名称不能为空且不超过100字符'
    }).then(async ({ value }) => {
      await apiCopyConfig(config.id!, value)
      ElMessage.success('复制成功')
      loadConfigs()
    })
  } catch {
    // 取消操作
  }
}

// 切换启用状态
async function toggleEnabled(config: AnalysisConfig) {
  try {
    await toggleConfigEnabled(config.id!, config.isEnabled!)
    ElMessage.success(config.isEnabled ? '已启用' : '已禁用')
  } catch (error) {
    // 恢复状态
    config.isEnabled = !config.isEnabled
    ElMessage.error('操作失败')
  }
}

// 处理下拉菜单命令
async function handleCommand(command: string, config: AnalysisConfig) {
  switch (command) {
    case 'setDefault':
      await setDefault(config)
      break
    case 'viewSnapshots':
      await viewSnapshots(config)
      break
    case 'delete':
      await handleDelete(config)
      break
  }
}

// 设为默认
async function setDefault(config: AnalysisConfig) {
  try {
    await setDefaultConfig(config.id!)
    ElMessage.success('设置成功')
    loadConfigs()
  } catch (error) {
    ElMessage.error('设置失败')
  }
}

// 查看快照
async function viewSnapshots(config: AnalysisConfig) {
  try {
    const res = await getSnapshots(config.id!)
    snapshots.value = res.data || []
    snapshotDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载快照失败')
  }
}

// 查看快照详情
function viewSnapshot(snapshot: AnalysisSnapshot) {
  router.push({
    name: 'AnalysisSnapshot',
    params: { snapshotId: snapshot.id }
  })
}

// 删除配置
async function handleDelete(config: AnalysisConfig) {
  try {
    await ElMessageBox.confirm(
      `确定要删除配置"${config.configName}"吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteConfig(config.id!)
    ElMessage.success('删除成功')
    loadConfigs()
  } catch {
    // 取消操作
  }
}

// 向导成功回调
function onWizardSuccess() {
  wizardVisible.value = false
  loadConfigs()
}

// 返回
function goBack() {
  router.back()
}

// 辅助方法
function getScopeTypeLabel(type: string) {
  const labels: Record<string, string> = {
    time: '时间范围',
    record: '记录选择',
    mixed: '混合模式'
  }
  return labels[type] || type
}

function getScopeTypeTag(type: string) {
  const tags: Record<string, string> = {
    time: '',
    record: 'success',
    mixed: 'warning'
  }
  return tags[type] || ''
}

function getMissingStrategyLabel(strategy: string) {
  const labels: Record<string, string> = {
    avg: '平均扣分',
    weighted: '加权平均',
    full_only: '仅全覆盖',
    penalty: '缺检惩罚',
    exempt: '缺检豁免'
  }
  return labels[strategy] || strategy
}

// 初始化
onMounted(() => {
  loadCheckPlans()
  loadConfigs()
})
</script>

<style scoped lang="scss">
.analysis-config-list {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .page-title {
      font-size: 18px;
      font-weight: 600;
    }
  }

  .filter-card {
    margin-bottom: 20px;

    .filter-form {
      .el-form-item {
        margin-bottom: 0;
      }
    }
  }

  .list-card {
    .config-name {
      display: flex;
      align-items: center;
      gap: 8px;

      .default-tag {
        flex-shrink: 0;
      }
    }

    .pagination-container {
      display: flex;
      justify-content: flex-end;
      padding-top: 20px;
    }
  }
}
</style>
