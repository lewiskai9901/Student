<template>
  <div class="rating-config-management">
    <el-card shadow="never" class="header-card">
      <div class="header-actions">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新建评级
        </el-button>
      </div>
    </el-card>

    <!-- 评级配置列表 -->
    <el-card shadow="never" class="list-card">
      <el-table :data="configList" v-loading="loading" stripe>
        <el-table-column type="index" label="#" width="60" />

        <el-table-column label="评级名称" min-width="150">
          <template #default="{ row }">
            <div class="rating-name-cell">
              <el-icon v-if="row.icon" :style="{ color: row.color || '#409eff' }">
                <component :is="row.icon" />
              </el-icon>
              <span>{{ row.ratingName }}</span>
              <el-tag v-if="row.enabled === 0" type="info" size="small">已禁用</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="周期类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getRatingTypeTag(row.ratingType)">
              {{ row.ratingTypeText }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="划分规则" min-width="150">
          <template #default="{ row }">
            <span>{{ row.divisionMethodText }} {{ row.divisionValue }}</span>
          </template>
        </el-table-column>

        <el-table-column label="排名数据源" min-width="200">
          <template #default="{ row }">
            <div class="sources-cell">
              <el-tag
                v-for="source in row.rankingSources"
                :key="source.id"
                size="small"
                class="source-tag"
              >
                {{ source.sourceTypeText }}
                <span v-if="source.sourceName">: {{ source.sourceName }}</span>
                <span v-if="row.rankingSources.length > 1">
                  ({{ (source.weight * 100).toFixed(0) }}%)
                </span>
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="审核设置" width="120">
          <template #default="{ row }">
            <div class="approval-cell">
              <el-tag v-if="row.requireApproval === 1" type="warning" size="small">
                需审核
              </el-tag>
              <el-tag v-if="row.autoPublish === 1" type="success" size="small">
                自动发布
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="优先级" width="80" align="center">
          <template #default="{ row }">
            {{ row.priority }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.enabled === 1 ? 'warning' : 'success'"
              @click="handleToggleEnabled(row)"
            >
              {{ row.enabled === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && configList.length === 0" description="暂无评级配置" />
    </el-card>

    <!-- 配置对话框 -->
    <RatingConfigDialog
      v-model="dialogVisible"
      :config-data="currentConfig"
      :check-plan-id="checkPlanId"
      @success="handleSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getRatingConfigsByPlan,
  deleteRatingConfig,
  toggleRatingConfigEnabled,
  type RatingConfigVO
} from '@/api/checkPlanRatingConfig'
import RatingConfigDialog from './RatingConfigDialog.vue'

// Props
const props = defineProps<{
  checkPlanId: string | number
}>()

// 响应式数据
const loading = ref(false)
const configList = ref<RatingConfigVO[]>([])
const dialogVisible = ref(false)
const currentConfig = ref<RatingConfigVO | null>(null)

// 加载配置列表
const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await getRatingConfigsByPlan(props.checkPlanId)
    configList.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 获取评级类型标签
const getRatingTypeTag = (type: string) => {
  const map: Record<string, string> = {
    DAILY: 'success',
    WEEKLY: 'primary',
    MONTHLY: 'warning'
  }
  return map[type] || ''
}

// 新建
const handleCreate = () => {
  currentConfig.value = null
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: RatingConfigVO) => {
  currentConfig.value = row
  dialogVisible.value = true
}

// 启用/禁用
const handleToggleEnabled = async (row: RatingConfigVO) => {
  const action = row.enabled === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}评级"${row.ratingName}"吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await toggleRatingConfigEnabled(row.id, row.enabled === 0)
    ElMessage.success(`${action}成功`)
    await loadConfigs()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || `${action}失败`)
    }
  }
}

// 删除
const handleDelete = async (row: RatingConfigVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除评级"${row.ratingName}"吗？删除后将无法恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteRatingConfig(row.id)
    ElMessage.success('删除成功')
    await loadConfigs()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 操作成功回调
const handleSuccess = () => {
  dialogVisible.value = false
  loadConfigs()
}

// 初始化
onMounted(() => {
  loadConfigs()
})
</script>

<style scoped lang="scss">
.rating-config-management {
  .header-card {
    margin-bottom: 16px;

    .header-actions {
      display: flex;
      justify-content: flex-end;
    }
  }

  .list-card {
    .rating-name-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .el-icon {
        font-size: 18px;
      }
    }

    .sources-cell {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;

      .source-tag {
        margin: 0;
      }
    }

    .approval-cell {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
  }
}
</style>
