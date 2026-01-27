<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑评级配置' : '新建评级配置'"
    width="1000px"
    :close-on-click-modal="false"
    @closed="handleClosed"
    class="rating-config-dialog"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <!-- 基本信息卡片 -->
      <el-card shadow="never" class="info-card">
        <template #header>
          <div class="card-header">
            <el-icon class="header-icon"><Setting /></el-icon>
            <span>基本信息</span>
          </div>
        </template>

        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="评级周期" prop="ratingType">
              <el-segmented
                v-model="form.ratingType"
                :options="[
                  { label: '按日评级', value: 'DAILY', icon: markRaw(Calendar) },
                  { label: '按周评级', value: 'WEEKLY', icon: markRaw(Calendar) },
                  { label: '按月评级', value: 'MONTHLY', icon: markRaw(Calendar) }
                ]"
                :disabled="isEdit"
                block
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 评级分段配置 -->
      <el-card shadow="never" class="segments-card">
        <template #header>
          <div class="card-header">
            <el-icon class="header-icon"><Rank /></el-icon>
            <span>评级分段</span>
            <span class="header-tip">可添加多个评级段，如"前3名"、"后5名"、"其他"</span>
          </div>
        </template>

        <div class="segments-container">
          <div
            v-for="(segment, index) in form.segments"
            :key="index"
            class="segment-item"
          >
            <div class="segment-header">
              <div class="segment-index">段 {{ index + 1 }}</div>
              <el-button
                v-if="form.segments.length > 1"
                link
                type="danger"
                @click="removeSegment(index)"
              >
                <el-icon><Close /></el-icon>
                删除
              </el-button>
            </div>

            <div class="segment-content">
              <el-row :gutter="16">
                <el-col :span="10">
                  <el-form-item label="评级名称" :prop="`segments.${index}.ratingName`" :rules="[{ required: true, message: '请输入评级名称' }]">
                    <el-input
                      v-model="segment.ratingName"
                      placeholder="如：优秀班级"
                      maxlength="50"
                    >
                      <template #prefix>
                        <el-icon><Medal /></el-icon>
                      </template>
                    </el-input>
                  </el-form-item>
                </el-col>

                <el-col :span="6">
                  <el-form-item label="划分方式" :prop="`segments.${index}.divisionMethod`">
                    <el-select v-model="segment.divisionMethod" placeholder="选择">
                      <el-option label="前N名" value="TOP_N">
                        <el-icon><Top /></el-icon> 前N名
                      </el-option>
                      <el-option label="前N%" value="TOP_PERCENT">
                        <el-icon><Top /></el-icon> 前N%
                      </el-option>
                      <el-option label="后N名" value="BOTTOM_N">
                        <el-icon><Bottom /></el-icon> 后N名
                      </el-option>
                      <el-option label="后N%" value="BOTTOM_PERCENT">
                        <el-icon><Bottom /></el-icon> 后N%
                      </el-option>
                      <el-option label="其他" value="OTHER">
                        <el-icon><More /></el-icon> 其他（剩余）
                      </el-option>
                    </el-select>
                  </el-form-item>
                </el-col>

                <el-col :span="4" v-if="segment.divisionMethod !== 'OTHER'">
                  <el-form-item label="数值" :prop="`segments.${index}.divisionValue`">
                    <el-input-number
                      v-model="segment.divisionValue"
                      :min="0.01"
                      :precision="segment.divisionMethod?.includes('PERCENT') ? 2 : 0"
                      :placeholder="segment.divisionMethod?.includes('PERCENT') ? '10.00' : '3'"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>

                <el-col :span="2">
                  <el-form-item label="颜色">
                    <el-color-picker v-model="segment.color" />
                  </el-form-item>
                </el-col>

                <el-col :span="2">
                  <el-form-item label="优先级">
                    <el-input-number
                      v-model="segment.priority"
                      :min="0"
                      :max="99"
                      controls-position="right"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
            </div>
          </div>

          <el-button
            class="add-segment-btn"
            @click="addSegment"
          >
            <el-icon><Plus /></el-icon>
            添加评级段
          </el-button>
        </div>
      </el-card>

      <!-- 排名数据源卡片 -->
      <el-card shadow="never" class="sources-card">
        <template #header>
          <div class="card-header">
            <el-icon class="header-icon"><DataAnalysis /></el-icon>
            <span>排名数据源</span>
            <span class="header-tip">支持多数据源组合排名</span>
          </div>
        </template>

        <div class="sources-container">
          <div
            v-for="(source, index) in form.rankingSources"
            :key="index"
            class="source-item"
          >
            <div class="source-header">
              <div class="source-index">
                <el-icon><Collection /></el-icon>
                数据源 {{ index + 1 }}
              </div>
              <el-button
                v-if="form.rankingSources.length > 1"
                link
                type="danger"
                @click="removeSource(index)"
              >
                <el-icon><Close /></el-icon>
                移除
              </el-button>
            </div>

            <div class="source-content">
              <!-- 数据类型 -->
              <div class="form-row">
                <label class="field-label">数据类型</label>
                <el-segmented
                  v-model="source.sourceType"
                  :options="[
                    { label: '总分', value: 'TOTAL_SCORE' },
                    { label: '分类得分', value: 'CATEGORY' },
                    { label: '扣分项', value: 'DEDUCTION_ITEM' }
                  ]"
                  @change="handleSourceTypeChange(index, source.sourceType)"
                  block
                />
              </div>

              <!-- 分类选择 -->
              <div v-if="source.sourceType === 'CATEGORY'" class="form-row">
                <label class="field-label">选择类别</label>
                <el-empty v-if="categories.length === 0" description="暂无检查类别" :image-size="60" />
                <div v-else class="tag-group">
                  <el-check-tag
                    v-for="category in categories"
                    :key="category.categoryId"
                    :checked="source.sourceId === category.categoryId"
                    @change="source.sourceId = category.categoryId"
                  >
                    {{ category.categoryName }}
                  </el-check-tag>
                </div>
              </div>

              <!-- 扣分项选择 -->
              <div v-if="source.sourceType === 'DEDUCTION_ITEM'" class="form-row">
                <label class="field-label">选择扣分项</label>
                <el-empty v-if="deductionItems.length === 0" description="暂无扣分项" :image-size="60" />
                <div v-else>
                  <div
                    v-for="category in categoriesWithItems"
                    :key="category.categoryId"
                    class="deduction-category"
                  >
                    <div class="category-label">{{ category.categoryName }}</div>
                    <div class="tag-group">
                      <el-check-tag
                        v-for="item in category.items"
                        :key="item.itemId"
                        :checked="source.sourceId === item.itemId"
                        @change="source.sourceId = item.itemId"
                      >
                        {{ item.itemName }}
                      </el-check-tag>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 配置选项 -->
              <div class="options-row">
                <div class="option-item">
                  <label class="field-label">分数类型</label>
                  <el-radio-group v-model="source.useWeighted" size="small">
                    <el-radio-button :value="0">原始分</el-radio-button>
                    <el-radio-button :value="1">加权分</el-radio-button>
                  </el-radio-group>
                </div>

                <div class="option-item">
                  <label class="field-label">缺失数据</label>
                  <el-radio-group v-model="source.missingDataStrategy" size="small">
                    <el-radio-button label="ZERO">视为0分</el-radio-button>
                    <el-radio-button label="SKIP">跳过</el-radio-button>
                  </el-radio-group>
                </div>

                <div v-if="form.rankingSources.length > 1" class="option-item">
                  <label class="field-label">权重</label>
                  <el-input-number
                    v-model="source.weight"
                    :min="0"
                    :max="1"
                    :step="0.1"
                    :precision="2"
                    size="small"
                    style="width: 120px"
                  />
                </div>
              </div>
            </div>
          </div>

          <el-button
            v-if="form.rankingSources.length < 5"
            class="add-source-btn"
            @click="addSource"
          >
            <el-icon><Plus /></el-icon>
            添加数据源
          </el-button>

          <el-alert
            v-if="form.rankingSources.length > 1"
            :title="`权重总和：${totalWeight.toFixed(2)} ${totalWeight !== 1 ? '（需为1.0）' : ''}`"
            :type="totalWeight === 1 ? 'success' : 'warning'"
            :closable="false"
            show-icon
          />
        </div>
      </el-card>

      <!-- 其他设置卡片 -->
      <el-card shadow="never" class="settings-card">
        <template #header>
          <div class="card-header">
            <el-icon class="header-icon"><Tools /></el-icon>
            <span>其他设置</span>
          </div>
        </template>

        <el-row :gutter="20">
          <el-col :span="8">
            <div class="switch-item">
              <div class="switch-label">
                <el-icon><Check /></el-icon>
                需要审核
              </div>
              <el-switch v-model="requireApprovalSwitch" />
            </div>
          </el-col>

          <el-col :span="8">
            <div class="switch-item">
              <div class="switch-label">
                <el-icon><Promotion /></el-icon>
                自动发布
              </div>
              <el-switch
                v-model="autoPublishSwitch"
                :disabled="!requireApprovalSwitch"
              />
            </div>
          </el-col>
        </el-row>

        <el-form-item label="规则说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="选填，可说明评级规则的用途和注意事项"
          />
        </el-form-item>

        <el-form-item v-if="isEdit" label="变更说明">
          <el-input
            v-model="form.changeDescription"
            placeholder="选填，说明本次修改的内容"
            maxlength="200"
          />
        </el-form-item>
      </el-card>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, markRaw } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  Plus,
  Close,
  Setting,
  Rank,
  Medal,
  Top,
  Bottom,
  More,
  DataAnalysis,
  Collection,
  Tools,
  Check,
  Promotion,
  Calendar
} from '@element-plus/icons-vue'
import {
  createRatingConfig,
  updateRatingConfig,
  type RatingConfigVO,
  type RatingConfigCreateDTO,
  type RatingConfigUpdateDTO,
  type RatingRankingSourceDTO
} from '@/api/rating'
import {
  getCheckPlanDetail,
  parseTemplateSnapshot,
  type SnapshotCategory,
  type SnapshotDeductionItem
} from '@/api/quantification'

// Props
const props = defineProps<{
  modelValue: boolean
  configData: RatingConfigVO | null
  checkPlanId: string | number
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

// 响应式数据
const formRef = ref<FormInstance>()
const visible = ref(false)
const submitting = ref(false)
const loading = ref(false)

const isEdit = computed(() => !!props.configData)

// 检查类别和扣分项数据
const categories = ref<SnapshotCategory[]>([])
const deductionItems = ref<SnapshotDeductionItem[]>([])

// 按类别分组的扣分项
const categoriesWithItems = computed(() => {
  return categories.value
    .map((category) => ({
      categoryId: category.categoryId,
      categoryName: category.categoryName,
      items: category.deductionItems || []
    }))
    .filter((group) => group.items.length > 0)
})

// Form数据结构（支持多评级段）
const form = reactive<any>({
  ratingType: 'DAILY',
  segments: [
    {
      ratingName: '',
      divisionMethod: 'TOP_N',
      divisionValue: 3,
      color: '#67C23A',
      priority: 0
    }
  ],
  rankingSources: [
    {
      sourceType: 'TOTAL_SCORE',
      sourceId: undefined,
      useWeighted: 0,
      weight: 1.0,
      missingDataStrategy: 'ZERO',
      sortOrder: 0
    }
  ],
  description: '',
  changeDescription: ''
})

const requireApprovalSwitch = ref(false)
const autoPublishSwitch = ref(false)

// 表单验证规则
const rules: FormRules = {
  ratingType: [{ required: true, message: '请选择评级周期', trigger: 'change' }]
}

// 计算属性
const totalWeight = computed(() => {
  if (form.rankingSources.length === 1) return 1
  return form.rankingSources.reduce((sum: number, s: any) => sum + (s.weight || 0), 0)
})

// 评级段方法
const addSegment = () => {
  const colors = ['#67C23A', '#E6A23C', '#F56C6C', '#909399', '#409EFF']
  const nextPriority = form.segments.length
  const nextColor = colors[form.segments.length % colors.length]

  form.segments.push({
    ratingName: '',
    divisionMethod: 'OTHER',
    divisionValue: null,
    color: nextColor,
    priority: nextPriority
  })
}

const removeSegment = (index: number) => {
  form.segments.splice(index, 1)
}

// 加载检查计划数据
const loadCheckPlanData = async () => {
  try {
    loading.value = true
    const res = await getCheckPlanDetail(props.checkPlanId)

    // 解析模板快照
    if (res.templateSnapshot) {
      const snapshot = parseTemplateSnapshot(res.templateSnapshot)
      if (snapshot) {
        categories.value = snapshot.categories || []

        // 提取所有扣分项
        const allItems: SnapshotDeductionItem[] = []
        snapshot.categories.forEach((category) => {
          if (category.deductionItems) {
            allItems.push(...category.deductionItems)
          }
        })
        deductionItems.value = allItems
      }
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载检查计划数据失败')
  } finally {
    loading.value = false
  }
}

// 监听visible变化
watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val) {
      // 加载检查计划数据
      loadCheckPlanData()

      if (props.configData) {
        // 编辑模式，回显数据（只显示单个段）
        form.ratingType = props.configData.ratingType
        form.segments = [
          {
            ratingName: props.configData.ratingName,
            divisionMethod: props.configData.divisionMethod,
            divisionValue: props.configData.divisionValue,
            color: props.configData.color || '#67C23A',
            priority: props.configData.priority
          }
        ]
        form.rankingSources = props.configData.rankingSources.map((s) => ({
          sourceType: s.sourceType,
          sourceId: s.sourceId,
          useWeighted: s.useWeighted,
          weight: s.weight || 1.0,
          missingDataStrategy: s.missingDataStrategy,
          sortOrder: s.sortOrder
        }))
        form.description = props.configData.description || ''
        form.changeDescription = ''
        requireApprovalSwitch.value = props.configData.requireApproval === 1
        autoPublishSwitch.value = props.configData.autoPublish === 1
      }
    }
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 数据源类型变化
const handleSourceTypeChange = (index: number, type: string) => {
  form.rankingSources[index].sourceType = type
  form.rankingSources[index].sourceId = undefined
}

// 添加数据源
const addSource = () => {
  form.rankingSources.push({
    sourceType: 'TOTAL_SCORE',
    sourceId: undefined,
    useWeighted: 0,
    weight: 0,
    missingDataStrategy: 'ZERO',
    sortOrder: form.rankingSources.length
  })
}

// 删除数据源
const removeSource = (index: number) => {
  form.rankingSources.splice(index, 1)
}

// 提交（支持多段评级）
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    // 验证评级段
    if (form.segments.length === 0) {
      ElMessage.warning('请至少添加一个评级段')
      return
    }

    for (let i = 0; i < form.segments.length; i++) {
      const segment = form.segments[i]
      if (!segment.ratingName?.trim()) {
        ElMessage.warning(`评级段 ${i + 1} 未填写评级名称`)
        return
      }
      if (segment.divisionMethod !== 'OTHER' && !segment.divisionValue) {
        ElMessage.warning(`评级段 ${i + 1} 未填写划分值`)
        return
      }
    }

    // 验证权重总和
    if (form.rankingSources.length > 1 && totalWeight.value !== 1) {
      ElMessage.warning('组合排名时，权重总和必须为1.0')
      return
    }

    // 验证数据源配置
    for (let i = 0; i < form.rankingSources.length; i++) {
      const source = form.rankingSources[i]
      if (source.sourceType !== 'TOTAL_SCORE' && !source.sourceId) {
        ElMessage.warning(`数据源 ${i + 1} 未选择具体项目`)
        return
      }
    }

    submitting.value = true

    // 编辑模式：只能编辑单个配置
    if (isEdit.value) {
      const segment = form.segments[0]
      const data: any = {
        id: props.configData!.id,
        ratingName: segment.ratingName,
        ratingType: form.ratingType,
        divisionMethod: segment.divisionMethod,
        divisionValue: segment.divisionMethod === 'OTHER' ? null : segment.divisionValue,
        color: segment.color,
        priority: segment.priority,
        rankingSources: form.rankingSources,
        description: form.description,
        changeDescription: form.changeDescription,
        checkPlanId: props.checkPlanId,
        requireApproval: requireApprovalSwitch.value ? 1 : 0,
        autoPublish: autoPublishSwitch.value ? 1 : 0,
        enabled: 1
      }
      await updateRatingConfig(data as RatingConfigUpdateDTO)
      ElMessage.success('更新成功')
    } else {
      // 新建模式：为每个段创建一个评级配置
      for (let i = 0; i < form.segments.length; i++) {
        const segment = form.segments[i]
        const data: any = {
          ratingName: segment.ratingName,
          ratingType: form.ratingType,
          divisionMethod: segment.divisionMethod,
          divisionValue: segment.divisionMethod === 'OTHER' ? null : segment.divisionValue,
          color: segment.color,
          priority: segment.priority,
          rankingSources: form.rankingSources,
          description: form.description,
          checkPlanId: props.checkPlanId,
          requireApproval: requireApprovalSwitch.value ? 1 : 0,
          autoPublish: autoPublishSwitch.value ? 1 : 0,
          enabled: 1
        }
        await createRatingConfig(data as RatingConfigCreateDTO)
      }
      ElMessage.success(`成功创建 ${form.segments.length} 个评级配置`)
    }

    emit('success')
    visible.value = false
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

// 关闭回调
const handleClosed = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    ratingType: 'DAILY',
    segments: [
      {
        ratingName: '',
        divisionMethod: 'TOP_N',
        divisionValue: 3,
        color: '#67C23A',
        priority: 0
      }
    ],
    rankingSources: [
      {
        sourceType: 'TOTAL_SCORE',
        sourceId: undefined,
        useWeighted: 0,
        weight: 1.0,
        missingDataStrategy: 'ZERO',
        sortOrder: 0
      }
    ],
    description: '',
    changeDescription: ''
  })
  requireApprovalSwitch.value = false
  autoPublishSwitch.value = false
  categories.value = []
  deductionItems.value = []
}
</script>

<style scoped lang="scss">
// 对话框整体样式
.rating-config-dialog {
  :deep(.el-dialog__body) {
    padding: 20px;
    max-height: 75vh;
    overflow-y: auto;
  }
}

// 卡片通用样式
.info-card,
.segments-card,
.sources-card,
.settings-card {
  margin-bottom: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;

  :deep(.el-card__header) {
    padding: 14px 20px;
    background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
    border-bottom: 1px solid #e4e7ed;
  }

  :deep(.el-card__body) {
    padding: 20px;
  }
}

// 卡片标题样式
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;

  .header-icon {
    font-size: 18px;
    color: #409eff;
  }

  .header-tip {
    margin-left: auto;
    font-size: 12px;
    font-weight: 400;
    color: #909399;
  }
}

// 评级分段容器
.segments-container {
  .segment-item {
    margin-bottom: 16px;
    padding: 16px;
    background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    transition: all 0.3s ease;

    &:hover {
      border-color: #409eff;
      box-shadow: 0 2px 12px rgba(64, 158, 255, 0.08);
    }

    .segment-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px dashed #e4e7ed;

      .segment-index {
        display: inline-flex;
        align-items: center;
        padding: 4px 12px;
        font-size: 13px;
        font-weight: 600;
        color: #409eff;
        background: rgba(64, 158, 255, 0.1);
        border-radius: 4px;
      }
    }

    .segment-content {
      .el-form-item {
        margin-bottom: 0;
      }
    }
  }

  .add-segment-btn {
    width: 100%;
    height: 44px;
    margin-top: 8px;
    border: 2px dashed #dcdfe6;
    color: #606266;
    background: white;
    font-weight: 500;
    border-radius: 8px;
    transition: all 0.3s ease;

    &:hover {
      border-color: #409eff;
      color: #409eff;
      background: rgba(64, 158, 255, 0.05);
    }
  }
}

// 数据源容器
.sources-container {
  .source-item {
    margin-bottom: 16px;
    padding: 16px;
    background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    transition: all 0.3s ease;

    &:hover {
      border-color: #67c23a;
      box-shadow: 0 2px 12px rgba(103, 194, 58, 0.08);
    }

    .source-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px dashed #e4e7ed;

      .source-index {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 4px 12px;
        font-size: 13px;
        font-weight: 600;
        color: #67c23a;
        background: rgba(103, 194, 58, 0.1);
        border-radius: 4px;
      }
    }

    .source-content {
      .form-row {
        margin-bottom: 16px;

        &:last-child {
          margin-bottom: 0;
        }

        .field-label {
          display: block;
          margin-bottom: 10px;
          font-size: 13px;
          font-weight: 500;
          color: #606266;
        }
      }

      .tag-group {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
      }

      .deduction-category {
        margin-bottom: 16px;

        &:last-child {
          margin-bottom: 0;
        }

        .category-label {
          margin-bottom: 10px;
          padding-left: 10px;
          font-size: 13px;
          font-weight: 600;
          color: #303133;
          border-left: 3px solid #409eff;
        }
      }

      .options-row {
        display: flex;
        gap: 24px;
        margin-top: 16px;
        padding-top: 16px;
        border-top: 1px dashed #e4e7ed;

        .option-item {
          display: flex;
          flex-direction: column;
          gap: 8px;

          .field-label {
            font-size: 12px;
            font-weight: 500;
            color: #606266;
          }
        }
      }
    }
  }

  .add-source-btn {
    width: 100%;
    height: 44px;
    margin-top: 8px;
    margin-bottom: 12px;
    border: 2px dashed #dcdfe6;
    color: #606266;
    background: white;
    font-weight: 500;
    border-radius: 8px;
    transition: all 0.3s ease;

    &:hover {
      border-color: #67c23a;
      color: #67c23a;
      background: rgba(103, 194, 58, 0.05);
    }
  }
}

// 开关设置项
.switch-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 6px;
  transition: all 0.3s ease;

  &:hover {
    background: #ecf5ff;
  }

  .switch-label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 500;
    color: #303133;

    .el-icon {
      font-size: 16px;
      color: #409eff;
    }
  }
}

// Element Plus 样式覆盖
:deep(.el-segmented) {
  --el-border-radius-base: 6px;
  --el-border-color: #e4e7ed;
}

:deep(.el-segmented__item) {
  font-weight: 500;
}

:deep(.el-check-tag) {
  border-radius: 6px;
  font-size: 13px;
  padding: 6px 14px;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-1px);
  }
}

:deep(.el-alert) {
  border-radius: 6px;
  margin-top: 12px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  border-radius: 6px;
}

:deep(.el-select),
:deep(.el-input-number) {
  width: 100%;
}

:deep(.el-radio-button__inner) {
  border-radius: 4px;
  font-weight: 500;
}
</style>
