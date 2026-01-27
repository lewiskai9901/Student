<template>
  <div class="rating-rule-tab">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建评级规则
      </el-button>
      <el-button @click="loadRules">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 规则列表 -->
    <el-table :data="rules" v-loading="loading" stripe>
      <el-table-column prop="ruleName" label="规则名称" min-width="150">
        <template #default="{ row }">
          <span class="rule-name">{{ row.ruleName }}</span>
          <el-tag v-if="!row.enabled" type="info" size="small" class="ml-2">已禁用</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ruleType" label="评级类型" width="130">
        <template #default="{ row }">
          <el-tag :type="row.ruleType === 'DAILY' ? 'primary' : 'success'">
            {{ RULE_TYPE_LABELS[row.ruleType] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="scoreSource" label="评分来源" width="100">
        <template #default="{ row }">
          {{ SCORE_SOURCE_LABELS[row.scoreSource] }}
          <span v-if="row.categoryName" class="text-gray-500">({{ row.categoryName }})</span>
        </template>
      </el-table-column>
      <el-table-column prop="divisionMethod" label="划分方式" width="100">
        <template #default="{ row }">
          {{ DIVISION_METHOD_LABELS[row.divisionMethod] }}
        </template>
      </el-table-column>
      <el-table-column label="等级配置" width="200">
        <template #default="{ row }">
          <div class="level-preview">
            <el-tag
              v-for="level in row.levels.slice(0, 4)"
              :key="level.id"
              :color="level.levelColor"
              size="small"
              class="level-tag"
              effect="dark"
            >
              {{ level.levelName }}
            </el-tag>
            <span v-if="row.levels.length > 4" class="more-levels">+{{ row.levels.length - 4 }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link :type="row.enabled ? 'warning' : 'success'" @click="handleToggle(row)">
            {{ row.enabled ? '禁用' : '启用' }}
          </el-button>
          <el-popconfirm
            title="确定要删除这个规则吗？"
            @confirm="handleDelete(row)"
          >
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <el-empty v-if="!loading && rules.length === 0" description="暂无评级规则，点击上方按钮创建" />

    <!-- 创建/编辑对话框 -->
    <RatingRuleDialog
      v-model:visible="dialogVisible"
      :check-plan-id="checkPlanId"
      :rule-data="currentRule"
      :categories="categories"
      @success="handleDialogSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getRatingRulesByPlanId,
  deleteRatingRule,
  toggleRatingRuleEnabled,
  RULE_TYPE_LABELS,
  SCORE_SOURCE_LABELS,
  DIVISION_METHOD_LABELS,
  type RatingRuleVO
} from '@/api/rating'
import RatingRuleDialog from './RatingRuleDialog.vue'

const props = defineProps<{
  checkPlanId: string | number
  categories: Array<{ id: string | number; categoryName: string }>
}>()

const loading = ref(false)
const rules = ref<RatingRuleVO[]>([])
const dialogVisible = ref(false)
const currentRule = ref<RatingRuleVO | null>(null)

// 加载规则列表
const loadRules = async () => {
  loading.value = true
  try {
    const res = await getRatingRulesByPlanId(props.checkPlanId)
    rules.value = res || []
  } catch (error) {
    console.error('加载评级规则失败:', error)
  } finally {
    loading.value = false
  }
}

// 创建规则
const handleCreate = () => {
  currentRule.value = null
  dialogVisible.value = true
}

// 编辑规则
const handleEdit = (rule: RatingRuleVO) => {
  currentRule.value = rule
  dialogVisible.value = true
}

// 切换启用状态
const handleToggle = async (rule: RatingRuleVO) => {
  try {
    await toggleRatingRuleEnabled(rule.id, !rule.enabled)
    ElMessage.success(rule.enabled ? '已禁用' : '已启用')
    loadRules()
  } catch (error) {
    console.error('切换状态失败:', error)
  }
}

// 删除规则
const handleDelete = async (rule: RatingRuleVO) => {
  try {
    await deleteRatingRule(rule.id)
    ElMessage.success('删除成功')
    loadRules()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

// 对话框成功回调
const handleDialogSuccess = () => {
  loadRules()
}

onMounted(() => {
  loadRules()
})

// 暴露方法供父组件调用
defineExpose({
  loadRules
})
</script>

<style scoped>
.rating-rule-tab {
  padding: 16px 0;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
}

.rule-name {
  font-weight: 500;
}

.ml-2 {
  margin-left: 8px;
}

.level-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.level-tag {
  border: none;
}

.more-levels {
  font-size: 12px;
  color: #909399;
}

.text-gray-500 {
  color: #909399;
  font-size: 12px;
}
</style>
