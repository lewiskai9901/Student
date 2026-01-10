<template>
  <div class="plan-analysis-tab">
    <!-- 顶部操作栏 -->
    <div class="tab-header">
      <div class="header-left">
        <span class="tab-title">统计分析配置</span>
        <span class="config-count">共 {{ configs.length }} 个配置</span>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="createConfig">
          <el-icon><Plus /></el-icon>
          新建配置
        </el-button>
      </div>
    </div>

    <!-- 配置列表 -->
    <div class="config-list" v-loading="loading">
      <!-- 空状态 -->
      <div v-if="!loading && configs.length === 0" class="empty-state">
        <el-empty description="暂无分析配置">
          <el-button type="primary" @click="createConfig">
            <el-icon><Plus /></el-icon>
            创建第一个配置
          </el-button>
        </el-empty>
      </div>

      <!-- 配置卡片列表 -->
      <div v-else class="config-cards">
        <div
          v-for="config in configs"
          :key="config.id"
          class="config-card"
          :class="{ 'is-default': config.isDefault, 'is-disabled': !config.isEnabled }"
        >
          <!-- 卡片头部 -->
          <div class="card-header">
            <div class="config-title">
              <el-tag v-if="config.isDefault" type="warning" size="small" effect="dark">默认</el-tag>
              <span class="config-name">{{ config.configName }}</span>
            </div>
            <el-switch
              v-model="config.isEnabled"
              size="small"
              @change="toggleEnabled(config)"
            />
          </div>

          <!-- 卡片内容 -->
          <div class="card-body">
            <p class="config-desc">{{ config.configDesc || '暂无描述' }}</p>
            <div class="config-meta">
              <div class="meta-item">
                <el-icon><Calendar /></el-icon>
                <span>{{ getScopeTypeLabel(config.scopeType) }}</span>
              </div>
              <div class="meta-item">
                <el-icon><Aim /></el-icon>
                <span>{{ getTargetTypeLabel(config.targetType) }}</span>
              </div>
              <div class="meta-item">
                <el-icon><Refresh /></el-icon>
                <span>{{ config.updateMode === 'dynamic' ? '动态更新' : '静态' }}</span>
              </div>
            </div>
            <div class="config-stats">
              <span class="stat-item">
                <strong>{{ config.metricCount || 0 }}</strong> 个指标
              </span>
              <span class="stat-item">
                <strong>{{ config.snapshotCount || 0 }}</strong> 个快照
              </span>
            </div>
          </div>

          <!-- 卡片操作 -->
          <div class="card-footer">
            <el-button type="primary" link @click="viewAnalysis(config)">
              <el-icon><DataAnalysis /></el-icon>
              查看分析
            </el-button>
            <el-button type="primary" link @click="editConfig(config)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="primary" link @click="copyConfig(config)">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, config)">
              <el-button type="primary" link>
                更多
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="setDefault" :disabled="config.isDefault">
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
          </div>
        </div>
      </div>
    </div>

    <!-- 配置向导对话框 -->
    <AnalysisConfigWizard
      v-model:visible="wizardVisible"
      :config-id="currentConfigId"
      :plan-id="props.planId"
      @success="onWizardSuccess"
    />

    <!-- 快照列表对话框 -->
    <el-dialog
      v-model="snapshotDialogVisible"
      title="历史快照"
      width="700px"
      destroy-on-close
    >
      <el-table :data="snapshots" stripe border>
        <el-table-column prop="generatedAt" label="生成时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.generatedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="dateRangeStart" label="数据范围" min-width="180">
          <template #default="{ row }">
            {{ row.dateRangeStart || '-' }} ~ {{ row.dateRangeEnd || '至今' }}
          </template>
        </el-table-column>
        <el-table-column prop="recordCount" label="记录数" width="90" align="center" />
        <el-table-column prop="classCount" label="班级数" width="90" align="center" />
        <el-table-column label="操作" width="100">
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
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Edit, Delete, CopyDocument, Star, Clock, DataAnalysis, ArrowDown,
  Calendar, Aim, Refresh
} from '@element-plus/icons-vue'
import {
  getConfigsByPlanId,
  deleteConfig,
  toggleConfigEnabled,
  setDefaultConfig,
  copyConfig as apiCopyConfig,
  getSnapshots,
  type AnalysisConfig,
  type AnalysisSnapshot
} from '@/api/v2/quantification-extra'
import { formatDateTime } from '@/utils/date'
import AnalysisConfigWizard from './analysis/AnalysisConfigWizard.vue'

const props = defineProps<{
  planId: number | string
}>()

const router = useRouter()

// 状态
const loading = ref(false)
const configs = ref<AnalysisConfig[]>([])
const wizardVisible = ref(false)
const currentConfigId = ref<number | string | null>(null)
const snapshotDialogVisible = ref(false)
const snapshots = ref<AnalysisSnapshot[]>([])

// 加载配置列表
async function loadConfigs() {
  if (!props.planId) return

  loading.value = true
  try {
    const res = await getConfigsByPlanId(props.planId)
    configs.value = res.data || []
  } catch (error) {
    console.error('加载配置列表失败:', error)
    ElMessage.error('加载配置列表失败')
  } finally {
    loading.value = false
  }
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

// 辅助方法
function getScopeTypeLabel(type: string) {
  const labels: Record<string, string> = {
    time: '时间范围',
    record: '记录选择',
    mixed: '混合模式'
  }
  return labels[type] || type
}

function getTargetTypeLabel(type: string) {
  const labels: Record<string, string> = {
    all: '全部班级',
    department: '按院系',
    grade: '按年级',
    custom: '自定义'
  }
  return labels[type] || type
}

// 监听planId变化
watch(() => props.planId, () => {
  loadConfigs()
}, { immediate: true })

// 暴露刷新方法
defineExpose({
  refresh: loadConfigs
})
</script>

<style scoped lang="scss">
.plan-analysis-tab {
  .tab-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid var(--el-border-color-light);

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .tab-title {
        font-size: 16px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }

      .config-count {
        font-size: 13px;
        color: var(--el-text-color-secondary);
      }
    }
  }

  .config-list {
    min-height: 200px;

    .empty-state {
      padding: 60px 0;
    }

    .config-cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
      gap: 16px;
    }
  }

  .config-card {
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-light);
    border-radius: 8px;
    padding: 16px;
    transition: all 0.2s;

    &:hover {
      border-color: var(--el-color-primary-light-5);
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    }

    &.is-default {
      border-color: var(--el-color-warning-light-5);
      background: linear-gradient(135deg, rgba(var(--el-color-warning-rgb), 0.02), transparent);
    }

    &.is-disabled {
      opacity: 0.7;
      background: var(--el-fill-color-lighter);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .config-title {
        display: flex;
        align-items: center;
        gap: 8px;

        .config-name {
          font-size: 15px;
          font-weight: 600;
          color: var(--el-text-color-primary);
        }
      }
    }

    .card-body {
      .config-desc {
        font-size: 13px;
        color: var(--el-text-color-secondary);
        margin-bottom: 12px;
        line-height: 1.5;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .config-meta {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
        margin-bottom: 12px;

        .meta-item {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 12px;
          color: var(--el-text-color-regular);

          .el-icon {
            font-size: 14px;
            color: var(--el-text-color-secondary);
          }
        }
      }

      .config-stats {
        display: flex;
        gap: 16px;

        .stat-item {
          font-size: 12px;
          color: var(--el-text-color-secondary);

          strong {
            color: var(--el-color-primary);
            font-size: 14px;
          }
        }
      }
    }

    .card-footer {
      display: flex;
      align-items: center;
      gap: 4px;
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid var(--el-border-color-lighter);

      .el-button {
        padding: 4px 8px;
      }
    }
  }
}
</style>
