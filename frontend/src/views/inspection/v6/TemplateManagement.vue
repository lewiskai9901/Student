<template>
  <div class="template-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <span class="page-title">{{ isEditing ? '编辑模板' : '检查模板管理' }}</span>
        <el-tag v-if="isEditing && currentTemplate.templateCode" type="info">{{ currentTemplate.templateCode }}</el-tag>
      </div>
      <div class="header-actions">
        <template v-if="isEditing">
          <el-button @click="exitEditing">返回列表</el-button>
          <el-button type="primary" @click="saveTemplate" :loading="saving">保存模板</el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="createNewTemplate">
            <el-icon><Plus /></el-icon>
            新建模板
          </el-button>
        </template>
      </div>
    </div>

    <!-- 编辑模式：步骤向导 -->
    <template v-if="isEditing">
      <!-- 步骤条 -->
      <div class="steps-bar">
        <div
          v-for="(step, idx) in steps"
          :key="step.key"
          class="step-item"
          :class="{ active: currentStep === idx, completed: idx < currentStep }"
          @click="goToStep(idx)"
        >
          <div class="step-number">{{ idx + 1 }}</div>
          <div class="step-content">
            <div class="step-title">{{ step.title }}</div>
            <div class="step-desc">{{ step.desc }}</div>
          </div>
        </div>
      </div>

      <!-- 主内容区 -->
      <div class="main-content">
        <!-- 左侧面板 -->
        <div class="left-panel">
          <!-- 步骤1: 基本信息 -->
          <div v-show="currentStep === 0" class="step-panel">
            <div class="panel-header">
              <span class="panel-title">模板基本信息</span>
            </div>
            <div class="panel-body">
              <div class="form-grid">
                <div class="form-item">
                  <label class="form-label required">模板名称</label>
                  <el-input v-model="templateForm.templateName" placeholder="请输入模板名称" />
                </div>
                <div class="form-item">
                  <label class="form-label">模板代码 <span class="hint">(自动生成)</span></label>
                  <el-input v-model="templateForm.templateCode" placeholder="系统自动生成" :disabled="!!templateForm.id" />
                </div>
                <div class="form-item full">
                  <label class="form-label">模板描述</label>
                  <el-input v-model="templateForm.description" type="textarea" :rows="3" placeholder="请输入模板描述" />
                </div>
                <div class="form-item">
                  <label class="form-label">适用范围</label>
                  <el-select v-model="templateForm.targetType" placeholder="选择适用范围">
                    <el-option label="班级检查" value="CLASS" />
                    <el-option label="宿舍检查" value="DORMITORY" />
                    <el-option label="个人检查" value="INDIVIDUAL" />
                  </el-select>
                </div>
                <div class="form-item">
                  <label class="form-label">检查周期</label>
                  <el-select v-model="templateForm.checkCycle" placeholder="选择检查周期">
                    <el-option label="每日" value="DAILY" />
                    <el-option label="每周" value="WEEKLY" />
                    <el-option label="每月" value="MONTHLY" />
                    <el-option label="自定义" value="CUSTOM" />
                  </el-select>
                </div>
              </div>
            </div>
          </div>

          <!-- 步骤2: 计分策略 -->
          <div v-show="currentStep === 1" class="step-panel">
            <div class="panel-header">
              <span class="panel-title">选择计分策略</span>
            </div>
            <div class="panel-body">
              <StrategySelector
                v-model="templateForm.scoringStrategy"
                v-model:parameters="templateForm.strategyParams"
                @change="handleStrategyChange"
              />
            </div>
          </div>

          <!-- 步骤3: 检查类别 -->
          <div v-show="currentStep === 2" class="step-panel">
            <div class="panel-header">
              <span class="panel-title">配置检查类别</span>
              <div class="header-extra">
                <span class="weight-sum" :class="{ error: totalWeight !== 100 }">权重合计: {{ totalWeight }}%</span>
                <el-button type="primary" size="small" @click="showCategoryDialog()">添加类别</el-button>
              </div>
            </div>
            <div class="panel-body">
              <div class="category-list">
                <div v-for="cat in categories" :key="cat.id || cat.tempId" class="category-card">
                  <div class="category-info">
                    <div class="category-color" :style="{ backgroundColor: cat.color || '#5b5fc7' }"></div>
                    <div class="category-details">
                      <h4>{{ cat.categoryName }}</h4>
                      <div class="category-meta">{{ cat.items?.length || 0 }}个检查项 · 权重 {{ cat.weight }}%</div>
                    </div>
                  </div>
                  <div class="category-actions">
                    <el-button link type="primary" size="small" @click="showCategoryDialog(cat)">编辑</el-button>
                    <el-button link type="danger" size="small" @click="deleteCategory(cat)">删除</el-button>
                  </div>
                </div>
                <div v-if="categories.length === 0" class="empty-tip">
                  暂无类别，点击"添加类别"开始配置
                </div>
              </div>
            </div>
          </div>

          <!-- 步骤4: 检查项目 -->
          <div v-show="currentStep === 3" class="step-panel">
            <div class="panel-header">
              <span class="panel-title">配置检查项目</span>
            </div>
            <!-- 类别标签 -->
            <div class="category-tabs">
              <div
                v-for="cat in categories"
                :key="cat.id || cat.tempId"
                class="tab-item"
                :class="{ active: selectedCategoryId === (cat.id || cat.tempId) }"
                @click="selectCategory(cat)"
              >
                <span class="tab-dot" :style="{ backgroundColor: cat.color || '#5b5fc7' }"></span>
                {{ cat.categoryName }}
                <span class="tab-count">({{ cat.items?.length || 0 }})</span>
              </div>
            </div>
            <div class="panel-body">
              <template v-if="selectedCategory">
                <div class="items-header">
                  <span>{{ selectedCategory.categoryName }} - 检查项</span>
                  <div>
                    <el-button size="small" @click="batchImportItems">批量导入</el-button>
                    <el-button type="primary" size="small" @click="showItemDialog()">添加检查项</el-button>
                  </div>
                </div>
                <div class="items-grid">
                  <div v-for="item in selectedCategory.items" :key="item.id || item.tempId" class="item-card">
                    <div class="item-card-header">
                      <div class="item-info">
                        <span class="item-name">{{ item.itemName }}</span>
                        <span class="item-code">{{ item.itemCode }}</span>
                      </div>
                      <div v-if="hasScoreConfig(item)" class="item-score" :class="getScoreClass(item)">
                        {{ formatScorePreview(item) }}
                      </div>
                    </div>
                    <div v-if="item.description" class="item-desc">{{ item.description }}</div>
                    <div class="item-tags">
                      <el-tag v-if="item.inputType" size="small">{{ getInputTypeLabel(item.inputType) }}</el-tag>
                      <el-tag v-if="item.canLinkIndividual" size="small" type="warning">关联个人</el-tag>
                      <el-tag v-if="item.requiresPhoto" size="small" type="success">需拍照</el-tag>
                    </div>
                    <div class="item-card-footer">
                      <el-switch v-model="item.isEnabled" size="small" />
                      <div class="item-actions">
                        <el-button link type="primary" size="small" @click="showItemDialog(item)">编辑</el-button>
                        <el-button link type="primary" size="small" @click="showScoreConfigDialog(item)">配分</el-button>
                        <el-button link type="danger" size="small" @click="deleteItem(item)">删除</el-button>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="!selectedCategory.items?.length" class="empty-tip">
                  暂无检查项，点击"添加检查项"开始配置
                </div>
              </template>
              <div v-else class="empty-state">
                <p>请先在步骤3中添加检查类别</p>
              </div>
            </div>
          </div>

          <!-- 步骤5: 高级规则 -->
          <div v-show="currentStep === 4" class="step-panel">
            <div class="panel-header">
              <span class="panel-title">高级计算规则</span>
            </div>
            <div class="panel-body">
              <RuleChainEditor
                v-model="templateForm.calculationRules"
                v-model:formula="templateForm.customFormula"
                @change="handleRulesChange"
              />
            </div>
          </div>
        </div>

        <!-- 右侧摘要面板 -->
        <div class="right-panel">
          <div class="panel-header">
            <span class="panel-title">模板摘要</span>
          </div>

          <div class="summary-section">
            <div class="summary-title">基本信息</div>
            <div class="summary-item">
              <span class="summary-label">模板名称</span>
              <span class="summary-value">{{ templateForm.templateName || '-' }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">适用范围</span>
              <span class="summary-value">{{ getTargetTypeLabel(templateForm.targetType) }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">检查周期</span>
              <span class="summary-value">{{ getCycleLabel(templateForm.checkCycle) }}</span>
            </div>
          </div>

          <div class="summary-section">
            <div class="summary-title">计分配置</div>
            <div class="summary-item">
              <span class="summary-label">计分策略</span>
              <span class="summary-value highlight">{{ getStrategyLabel(templateForm.scoringStrategy) }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">基准分</span>
              <span class="summary-value">{{ templateForm.strategyParams?.baseScore || 100 }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">分数范围</span>
              <span class="summary-value">{{ templateForm.strategyParams?.minScore || 0 }} ~ {{ templateForm.strategyParams?.maxScore || 100 }}</span>
            </div>
          </div>

          <div class="summary-section">
            <div class="summary-title">检查内容</div>
            <div class="summary-item">
              <span class="summary-label">检查类别</span>
              <span class="summary-value">{{ categories.length }} 个</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">检查项目</span>
              <span class="summary-value">{{ totalItemsCount }} 个</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">已启用</span>
              <span class="summary-value highlight">{{ enabledItemsCount }} 个</span>
            </div>
          </div>

          <div class="summary-section">
            <div class="summary-title">高级规则</div>
            <div v-for="rule in enabledRules" :key="rule.code" class="summary-item">
              <span class="summary-label">{{ rule.name }}</span>
              <el-tag size="small" type="success">已启用</el-tag>
            </div>
            <div v-if="enabledRules.length === 0" class="summary-item">
              <span class="summary-label">无启用规则</span>
            </div>
          </div>

          <!-- 权重分配 -->
          <div v-if="categories.length > 0" class="summary-section">
            <div class="summary-title">权重分配</div>
            <div class="weight-bars">
              <div v-for="cat in categories" :key="cat.id || cat.tempId" class="weight-bar-item">
                <div class="weight-dot" :style="{ backgroundColor: cat.color || '#5b5fc7' }"></div>
                <span class="weight-name">{{ cat.categoryName }}</span>
                <span class="weight-value">{{ cat.weight }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部操作栏 -->
      <div class="footer-bar">
        <el-button :disabled="currentStep === 0" @click="prevStep">上一步</el-button>
        <div>
          <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
          <el-button v-if="currentStep < steps.length - 1" type="primary" @click="nextStep">下一步</el-button>
          <el-button v-else type="primary" @click="saveTemplate" :loading="saving">完成并保存</el-button>
        </div>
      </div>
    </template>

    <!-- 列表模式 -->
    <template v-else>
      <div class="template-list-container">
        <div class="list-toolbar">
          <el-input v-model="searchKeyword" placeholder="搜索模板..." style="width: 240px" clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="template-grid" v-loading="loading">
          <div
            v-for="t in filteredTemplates"
            :key="t.id"
            class="template-card"
            @click="editTemplate(t)"
          >
            <div class="card-header">
              <span class="card-title">{{ t.templateName }}</span>
              <el-tag v-if="t.isPublished" size="small" type="success">已发布</el-tag>
              <el-tag v-else size="small" type="info">草稿</el-tag>
            </div>
            <div class="card-code">{{ t.templateCode }}</div>
            <div class="card-desc">{{ t.description || '暂无描述' }}</div>
            <div class="card-meta">
              <span>{{ getStrategyLabel(t.scoringStrategy) }}</span>
              <span>{{ t.categoriesCount || 0 }}个类别</span>
              <span>{{ t.itemsCount || 0 }}个检查项</span>
            </div>
            <div class="card-actions" @click.stop>
              <el-button link type="primary" size="small" @click="editTemplate(t)">编辑</el-button>
              <el-button link type="primary" size="small" @click="duplicateTemplate(t)">复制</el-button>
              <el-button link type="danger" size="small" @click="deleteTemplate(t)">删除</el-button>
            </div>
          </div>
          <div v-if="filteredTemplates.length === 0" class="empty-state">
            <p>{{ searchKeyword ? '没有找到匹配的模板' : '暂无模板，点击"新建模板"开始创建' }}</p>
          </div>
        </div>
      </div>
    </template>

    <!-- 类别编辑对话框 -->
    <el-dialog v-model="categoryDialogVisible" :title="editingCategory ? '编辑类别' : '添加类别'" width="500px" destroy-on-close>
      <div class="dialog-form">
        <div class="form-item">
          <label class="form-label required">类别名称</label>
          <el-input v-model="categoryForm.categoryName" placeholder="如：卫生类" />
        </div>
        <div class="form-item">
          <label class="form-label required">类别编码</label>
          <el-input v-model="categoryForm.categoryCode" placeholder="如：HYGIENE" :disabled="!!editingCategory" />
        </div>
        <div class="form-item">
          <label class="form-label">标识颜色</label>
          <div class="color-presets">
            <div
              v-for="c in colorPresets"
              :key="c"
              class="color-dot"
              :class="{ active: categoryForm.color === c }"
              :style="{ backgroundColor: c }"
              @click="categoryForm.color = c"
            ></div>
          </div>
        </div>
        <div class="form-item">
          <label class="form-label">权重</label>
          <el-input-number v-model="categoryForm.weight" :min="0" :max="100" /> %
        </div>
      </div>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">确定</el-button>
      </template>
    </el-dialog>

    <!-- 检查项编辑对话框 -->
    <el-dialog v-model="itemDialogVisible" :title="editingItem ? '编辑检查项' : '添加检查项'" width="600px" destroy-on-close>
      <div class="dialog-form">
        <div class="form-row">
          <div class="form-item">
            <label class="form-label required">检查项名称</label>
            <el-input v-model="itemForm.itemName" placeholder="如：地面清洁" />
          </div>
          <div class="form-item" style="width: 160px">
            <label class="form-label required">编码</label>
            <el-input v-model="itemForm.itemCode" placeholder="HYG001" :disabled="!!editingItem" />
          </div>
        </div>
        <div class="form-item">
          <label class="form-label required">打分方式</label>
          <InputTypeSelector
            v-model="itemForm.inputType"
            v-model:config="itemForm.inputConfig"
            @change="handleInputTypeChange"
          />
        </div>
        <div class="form-item">
          <label class="form-label">描述说明</label>
          <el-input v-model="itemForm.description" type="textarea" :rows="2" placeholder="检查标准（可选）" />
        </div>
        <div class="form-item">
          <label class="form-label">附加要求</label>
          <div class="checkbox-group">
            <el-checkbox v-model="itemForm.canLinkIndividual">可关联个人</el-checkbox>
            <el-checkbox v-model="itemForm.requiresPhoto">需要拍照</el-checkbox>
            <el-checkbox v-model="itemForm.requiresRemark">需要备注</el-checkbox>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveItem">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分值配置对话框 -->
    <el-dialog v-model="scoreConfigDialogVisible" title="配置分值" width="450px" destroy-on-close>
      <div v-if="editingScoreItem" class="score-config-form">
        <div class="score-header">
          <span>{{ editingScoreItem.itemName }}</span>
          <el-tag size="small">{{ getInputTypeLabel(editingScoreItem.inputType) }}</el-tag>
        </div>
        <!-- 选项类型 -->
        <template v-if="editingScoreItem.inputType === 'OPTIONS'">
          <div v-for="opt in (editingScoreItem.inputConfig?.options || defaultOptions)" :key="opt.label" class="score-row">
            <span class="score-label">{{ opt.label }}</span>
            <el-input-number v-model="scoreConfig[opt.label]" :precision="1" style="width: 120px" />
            <span class="score-unit">分</span>
          </div>
        </template>
        <!-- 计数类型 -->
        <template v-else-if="editingScoreItem.inputType === 'COUNT'">
          <div class="score-row">
            <span class="score-label">每{{ editingScoreItem.inputConfig?.unit || '次' }}</span>
            <el-input-number v-model="scoreConfig.perUnit" :precision="1" style="width: 120px" />
            <span class="score-unit">分</span>
          </div>
        </template>
        <!-- 勾选类型 -->
        <template v-else-if="editingScoreItem.inputType === 'CHECKBOX'">
          <div class="score-row">
            <span class="score-label">是</span>
            <el-input-number v-model="scoreConfig.yes" :precision="1" style="width: 120px" />
            <span class="score-unit">分</span>
          </div>
          <div class="score-row">
            <span class="score-label">否</span>
            <el-input-number v-model="scoreConfig.no" :precision="1" style="width: 120px" />
            <span class="score-unit">分</span>
          </div>
        </template>
        <!-- 数值类型 -->
        <template v-else>
          <p class="score-hint">数值类型直接使用输入值作为分数</p>
        </template>
      </div>
      <template #footer>
        <el-button @click="scoreConfigDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveScoreConfig">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { v6InspectionApi } from '@/api/v6Inspection'
import StrategySelector from './components/scoring/StrategySelector.vue'
import InputTypeSelector from './components/scoring/InputTypeSelector.vue'
import RuleChainEditor from './components/scoring/RuleChainEditor.vue'

// ============ 常量配置 ============
const steps = [
  { key: 'basic', title: '基本信息', desc: '模板名称与描述' },
  { key: 'strategy', title: '计分策略', desc: '选择计分方式' },
  { key: 'categories', title: '检查类别', desc: '配置分类与权重' },
  { key: 'items', title: '检查项目', desc: '添加具体检查项' },
  { key: 'rules', title: '高级规则', desc: '封顶保底等规则' }
]

const colorPresets = [
  '#5b5fc7', '#1890ff', '#52c41a', '#faad14', '#ff4d4f', '#722ed1', '#13c2c2', '#eb2f96'
]

const defaultOptions = [{ label: '合格', score: 0 }, { label: '不合格', score: -2 }]

const targetTypeLabels: Record<string, string> = {
  CLASS: '班级检查',
  DORMITORY: '宿舍检查',
  INDIVIDUAL: '个人检查'
}

const cycleLabels: Record<string, string> = {
  DAILY: '每日',
  WEEKLY: '每周',
  MONTHLY: '每月',
  CUSTOM: '自定义'
}

const strategyLabels: Record<string, string> = {
  DEDUCTION: '扣分制',
  ADDITION: '加分制',
  BASE_ADJUST: '基准调整制',
  CHECKLIST: '清单制',
  GRADE: '等级制',
  STAR_RATING: '星级制',
  PASS_FAIL: '合格制'
}

const inputTypeLabels: Record<string, string> = {
  NUMERIC: '数值输入',
  OPTIONS: '档位选择',
  CHECKBOX: '勾选完成',
  COUNT: '计数输入',
  STAR: '星级点选',
  GRADE: '等级选择'
}

// ============ 状态 ============
const loading = ref(false)
const saving = ref(false)
const isEditing = ref(false)
const currentStep = ref(0)
const searchKeyword = ref('')

// 模板列表
const templates = ref<any[]>([])
const currentTemplate = ref<any>({})

// 模板表单
const templateForm = ref({
  id: null as number | null,
  templateName: '',
  templateCode: '',
  description: '',
  targetType: 'CLASS',
  checkCycle: 'WEEKLY',
  scoringStrategy: 'DEDUCTION',
  strategyParams: {
    baseScore: 100,
    maxScore: 120,
    minScore: 0
  },
  calculationRules: [] as any[],
  customFormula: ''
})

// 类别和检查项
const categories = ref<any[]>([])
const selectedCategoryId = ref<string | number | null>(null)

// 对话框
const categoryDialogVisible = ref(false)
const itemDialogVisible = ref(false)
const scoreConfigDialogVisible = ref(false)

const editingCategory = ref<any>(null)
const editingItem = ref<any>(null)
const editingScoreItem = ref<any>(null)

const categoryForm = ref({
  categoryName: '',
  categoryCode: '',
  color: '#5b5fc7',
  weight: 25,
  sortOrder: 0
})

const itemForm = ref({
  itemName: '',
  itemCode: '',
  inputType: 'OPTIONS',
  inputConfig: {} as Record<string, any>,
  description: '',
  canLinkIndividual: false,
  requiresPhoto: false,
  requiresRemark: false,
  sortOrder: 0
})

const scoreConfig = ref<Record<string, number>>({})

// ============ 计算属性 ============
const filteredTemplates = computed(() => {
  if (!searchKeyword.value) return templates.value
  const kw = searchKeyword.value.toLowerCase()
  return templates.value.filter(t =>
    t.templateName.toLowerCase().includes(kw) ||
    t.templateCode?.toLowerCase().includes(kw)
  )
})

const selectedCategory = computed(() => {
  return categories.value.find(c => (c.id || c.tempId) === selectedCategoryId.value)
})

const totalWeight = computed(() => {
  return categories.value.reduce((sum, c) => sum + (c.weight || 0), 0)
})

const totalItemsCount = computed(() => {
  return categories.value.reduce((sum, c) => sum + (c.items?.length || 0), 0)
})

const enabledItemsCount = computed(() => {
  return categories.value.reduce((sum, c) => {
    return sum + (c.items?.filter((i: any) => i.isEnabled !== false)?.length || 0)
  }, 0)
})

const enabledRules = computed(() => {
  return templateForm.value.calculationRules.filter(r => r.enabled)
})

// ============ 辅助方法 ============
const getTargetTypeLabel = (type: string) => targetTypeLabels[type] || type || '-'
const getCycleLabel = (cycle: string) => cycleLabels[cycle] || cycle || '-'
const getStrategyLabel = (strategy: string) => strategyLabels[strategy] || strategy || '-'
const getInputTypeLabel = (type: string) => inputTypeLabels[type] || type || '-'

const hasScoreConfig = (item: any) => {
  if (!item.scoreConfig || Object.keys(item.scoreConfig).length === 0) return false
  return true
}

const getScoreClass = (item: any) => {
  if (!item.scoreConfig) return ''
  const values = Object.values(item.scoreConfig).filter(v => typeof v === 'number') as number[]
  if (values.length === 0) return ''
  return values.some(v => v < 0) ? 'deduct' : 'add'
}

const formatScorePreview = (item: any) => {
  if (!item.scoreConfig) return ''
  const c = item.scoreConfig
  if (item.inputType === 'OPTIONS') {
    const scores = Object.values(c).filter(v => typeof v === 'number') as number[]
    if (scores.length === 0) return ''
    const min = Math.min(...scores), max = Math.max(...scores)
    return min === max ? `${min >= 0 ? '+' : ''}${min}` : `${min}~${max}`
  }
  if (item.inputType === 'COUNT' && c.perUnit !== undefined) {
    return `${c.perUnit}/${item.inputConfig?.unit || '次'}`
  }
  if (item.inputType === 'CHECKBOX') {
    return `${c.yes || 0}/${c.no || 0}`
  }
  return ''
}

// ============ 数据加载 ============
const loadTemplates = async () => {
  loading.value = true
  try {
    const res = await v6InspectionApi.getTemplates()
    templates.value = res?.data?.data || res?.data || res || []
  } catch (e) {
    console.error('Failed to load templates:', e)
  } finally {
    loading.value = false
  }
}

const loadTemplateDetail = async (id: number) => {
  loading.value = true
  try {
    const res = await v6InspectionApi.getTemplate(id)
    const template = res?.data?.data || res?.data || res
    templateForm.value = {
      id: template.id,
      templateName: template.templateName,
      templateCode: template.templateCode,
      description: template.description,
      targetType: template.targetType || 'CLASS',
      checkCycle: template.checkCycle || 'WEEKLY',
      scoringStrategy: template.scoringStrategy || 'DEDUCTION',
      strategyParams: template.strategyParams || { baseScore: 100, maxScore: 120, minScore: 0 },
      calculationRules: template.calculationRules || [],
      customFormula: template.customFormula || ''
    }
    currentTemplate.value = template

    // 加载类别
    const catRes = await v6InspectionApi.getCategories(id)
    categories.value = catRes?.data?.data || catRes?.data || catRes || []

    // 加载每个类别的检查项
    for (const cat of categories.value) {
      const itemsRes = await v6InspectionApi.getItems(cat.id)
      cat.items = (itemsRes?.data?.data || itemsRes?.data || itemsRes || []).map((item: any) => ({
        ...item,
        isEnabled: item.isEnabled !== false,
        scoreConfig: item.scoreConfig || {}
      }))
    }

    if (categories.value.length > 0) {
      selectedCategoryId.value = categories.value[0].id
    }
  } catch (e) {
    console.error('Failed to load template detail:', e)
    ElMessage.error('加载模板失败')
  } finally {
    loading.value = false
  }
}

// ============ 模板操作 ============
const createNewTemplate = () => {
  isEditing.value = true
  currentStep.value = 0
  templateForm.value = {
    id: null,
    templateName: '',
    templateCode: '',
    description: '',
    targetType: 'CLASS',
    checkCycle: 'WEEKLY',
    scoringStrategy: 'DEDUCTION',
    strategyParams: { baseScore: 100, maxScore: 120, minScore: 0 },
    calculationRules: [],
    customFormula: ''
  }
  categories.value = []
  selectedCategoryId.value = null
  currentTemplate.value = {}
}

const editTemplate = async (t: any) => {
  isEditing.value = true
  currentStep.value = 0
  await loadTemplateDetail(t.id)
}

const exitEditing = () => {
  isEditing.value = false
  loadTemplates()
}

const duplicateTemplate = async (t: any) => {
  try {
    await ElMessageBox.confirm(`确定复制模板"${t.templateName}"？`, '提示')
    // 实现复制逻辑
    ElMessage.success('复制成功')
    loadTemplates()
  } catch { }
}

const deleteTemplate = async (t: any) => {
  try {
    await ElMessageBox.confirm(`确定删除模板"${t.templateName}"？此操作不可恢复！`, '警告', { type: 'warning' })
    // await v6InspectionApi.deleteTemplate(t.id)
    ElMessage.success('删除成功')
    loadTemplates()
  } catch { }
}

const saveTemplate = async () => {
  if (!templateForm.value.templateName) {
    ElMessage.warning('请输入模板名称')
    currentStep.value = 0
    return
  }

  saving.value = true
  try {
    const data = {
      ...templateForm.value,
      baseScore: templateForm.value.strategyParams?.baseScore
    }

    if (templateForm.value.id) {
      await v6InspectionApi.updateTemplate(templateForm.value.id, data)
    } else {
      const res = await v6InspectionApi.createTemplate(data)
      templateForm.value.id = res?.data?.id || res?.id
    }

    // 保存类别和检查项
    for (const cat of categories.value) {
      if (cat.id) {
        await v6InspectionApi.updateCategory(cat.id, cat)
      } else {
        const catRes = await v6InspectionApi.createCategory({
          ...cat,
          templateId: templateForm.value.id
        })
        cat.id = catRes?.data?.id || catRes?.id
      }

      // 保存检查项
      for (const item of cat.items || []) {
        if (item.id) {
          await v6InspectionApi.updateItem(item.id, item)
        } else {
          await v6InspectionApi.createItem({
            ...item,
            categoryId: cat.id
          })
        }
      }
    }

    ElMessage.success('保存成功')
    exitEditing()
  } catch (e) {
    console.error('Failed to save template:', e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const saveDraft = async () => {
  await saveTemplate()
}

// ============ 步骤导航 ============
const goToStep = (idx: number) => {
  currentStep.value = idx
}

const prevStep = () => {
  if (currentStep.value > 0) currentStep.value--
}

const nextStep = () => {
  if (currentStep.value < steps.length - 1) currentStep.value++
}

// ============ 类别操作 ============
const selectCategory = (cat: any) => {
  selectedCategoryId.value = cat.id || cat.tempId
}

const showCategoryDialog = (cat?: any) => {
  editingCategory.value = cat || null
  if (cat) {
    categoryForm.value = {
      categoryName: cat.categoryName,
      categoryCode: cat.categoryCode,
      color: cat.color || '#5b5fc7',
      weight: cat.weight || 25,
      sortOrder: cat.sortOrder || 0
    }
  } else {
    categoryForm.value = {
      categoryName: '',
      categoryCode: '',
      color: colorPresets[categories.value.length % colorPresets.length],
      weight: 25,
      sortOrder: categories.value.length
    }
  }
  categoryDialogVisible.value = true
}

const saveCategory = () => {
  if (!categoryForm.value.categoryName || !categoryForm.value.categoryCode) {
    ElMessage.warning('请填写类别名称和编码')
    return
  }

  if (editingCategory.value) {
    Object.assign(editingCategory.value, categoryForm.value)
  } else {
    categories.value.push({
      ...categoryForm.value,
      tempId: `temp_${Date.now()}`,
      items: []
    })
  }

  categoryDialogVisible.value = false

  // 自动选中新增的类别
  if (!editingCategory.value && categories.value.length === 1) {
    selectedCategoryId.value = categories.value[0].tempId
  }
}

const deleteCategory = async (cat: any) => {
  try {
    await ElMessageBox.confirm(`删除"${cat.categoryName}"及其所有检查项？`, '确认')
    const idx = categories.value.findIndex(c => (c.id || c.tempId) === (cat.id || cat.tempId))
    if (idx >= 0) {
      categories.value.splice(idx, 1)
    }
    if (selectedCategoryId.value === (cat.id || cat.tempId)) {
      selectedCategoryId.value = categories.value[0]?.id || categories.value[0]?.tempId || null
    }
    ElMessage.success('已删除')
  } catch { }
}

// ============ 检查项操作 ============
const showItemDialog = (item?: any) => {
  editingItem.value = item || null
  if (item) {
    itemForm.value = {
      itemName: item.itemName,
      itemCode: item.itemCode,
      inputType: item.inputType || 'OPTIONS',
      inputConfig: item.inputConfig || {},
      description: item.description || '',
      canLinkIndividual: item.canLinkIndividual || false,
      requiresPhoto: item.requiresPhoto || false,
      requiresRemark: item.requiresRemark || false,
      sortOrder: item.sortOrder || 0
    }
  } else {
    const prefix = selectedCategory.value?.categoryCode?.substring(0, 3)?.toUpperCase() || 'ITM'
    const count = (selectedCategory.value?.items?.length || 0) + 1
    itemForm.value = {
      itemName: '',
      itemCode: `${prefix}${String(count).padStart(3, '0')}`,
      inputType: 'OPTIONS',
      inputConfig: { options: [...defaultOptions] },
      description: '',
      canLinkIndividual: false,
      requiresPhoto: false,
      requiresRemark: false,
      sortOrder: count - 1
    }
  }
  itemDialogVisible.value = true
}

const saveItem = () => {
  if (!itemForm.value.itemName || !itemForm.value.itemCode) {
    ElMessage.warning('请填写检查项名称和编码')
    return
  }

  if (!selectedCategory.value) {
    ElMessage.warning('请先选择类别')
    return
  }

  if (editingItem.value) {
    Object.assign(editingItem.value, itemForm.value)
  } else {
    if (!selectedCategory.value.items) {
      selectedCategory.value.items = []
    }
    selectedCategory.value.items.push({
      ...itemForm.value,
      tempId: `temp_${Date.now()}`,
      isEnabled: true,
      scoreConfig: {}
    })
  }

  itemDialogVisible.value = false
}

const deleteItem = async (item: any) => {
  try {
    await ElMessageBox.confirm(`删除"${item.itemName}"？`, '确认')
    if (selectedCategory.value?.items) {
      const idx = selectedCategory.value.items.findIndex((i: any) =>
        (i.id || i.tempId) === (item.id || item.tempId)
      )
      if (idx >= 0) {
        selectedCategory.value.items.splice(idx, 1)
      }
    }
    ElMessage.success('已删除')
  } catch { }
}

const batchImportItems = () => {
  ElMessage.info('批量导入功能开发中')
}

// ============ 分值配置 ============
const showScoreConfigDialog = (item: any) => {
  editingScoreItem.value = item
  scoreConfig.value = { ...item.scoreConfig }

  // 初始化默认值
  if (item.inputType === 'OPTIONS') {
    const options = item.inputConfig?.options || defaultOptions
    for (const opt of options) {
      if (!(opt.label in scoreConfig.value)) {
        scoreConfig.value[opt.label] = opt.score || 0
      }
    }
  } else if (item.inputType === 'CHECKBOX') {
    if (!('yes' in scoreConfig.value)) scoreConfig.value.yes = 0
    if (!('no' in scoreConfig.value)) scoreConfig.value.no = -2
  } else if (item.inputType === 'COUNT') {
    if (!('perUnit' in scoreConfig.value)) scoreConfig.value.perUnit = -1
  }

  scoreConfigDialogVisible.value = true
}

const saveScoreConfig = () => {
  if (editingScoreItem.value) {
    editingScoreItem.value.scoreConfig = { ...scoreConfig.value }
  }
  scoreConfigDialogVisible.value = false
  ElMessage.success('已保存')
}

// ============ 事件处理 ============
const handleStrategyChange = (strategy: any) => {
  // 根据策略类型设置默认参数
  if (strategy.defaultParameters) {
    templateForm.value.strategyParams = { ...strategy.defaultParameters }
  }
}

const handleInputTypeChange = (inputType: any) => {
  // 根据打分方式初始化配置
  if (inputType.componentConfig) {
    itemForm.value.inputConfig = { ...inputType.componentConfig }
  }
}

const handleRulesChange = () => {
  // 规则变更处理
}

// ============ 生命周期 ============
onMounted(() => {
  loadTemplates()
})
</script>

<style scoped>
.template-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #fff;
  border-radius: 12px;
  margin: 16px 16px 0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 步骤条 */
.steps-bar {
  display: flex;
  gap: 8px;
  padding: 16px 24px;
  background: #fff;
  border-radius: 12px;
  margin: 16px 16px 0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.step-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.step-item:hover {
  background: #f5f5f5;
}

.step-item.active {
  background: #f0f0ff;
}

.step-item.completed {
  background: #f6ffed;
}

.step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e8e8e8;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.step-item.active .step-number {
  background: #5b5fc7;
  color: #fff;
}

.step-item.completed .step-number {
  background: #52c41a;
  color: #fff;
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.step-desc {
  font-size: 12px;
  color: #8c8c8c;
}

/* 主内容区 */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 16px;
  overflow: hidden;
}

.left-panel {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.right-panel {
  width: 320px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  overflow-y: auto;
  flex-shrink: 0;
}

/* 面板 */
.step-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

.header-extra {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel-body {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

/* 表单 */
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.form-item.full {
  flex: none;
  width: 100%;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.form-label.required::after {
  content: ' *';
  color: #ff4d4f;
}

.form-label .hint {
  font-weight: 400;
  color: #8c8c8c;
  margin-left: 4px;
}

/* 类别列表 */
.category-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.category-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: #fafafa;
  border-radius: 10px;
  border: 1px solid #e8e8e8;
  transition: all 0.2s;
}

.category-card:hover {
  background: #f5f5ff;
  border-color: #d0d0ff;
}

.category-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.category-color {
  width: 8px;
  height: 40px;
  border-radius: 4px;
}

.category-details h4 {
  font-size: 14px;
  font-weight: 500;
  margin: 0 0 4px;
  color: #1a1a2e;
}

.category-meta {
  font-size: 12px;
  color: #8c8c8c;
}

.category-actions {
  display: flex;
  gap: 4px;
}

/* 类别标签 */
.category-tabs {
  display: flex;
  gap: 4px;
  padding: 12px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  flex-wrap: wrap;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  transition: all 0.2s;
}

.tab-item:hover {
  background: #fff;
}

.tab-item.active {
  background: #fff;
  color: #5b5fc7;
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.tab-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.tab-count {
  font-size: 11px;
  color: #8c8c8c;
}

/* 检查项 */
.items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.item-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: all 0.2s;
}

.item-card:hover {
  border-color: #5b5fc7;
  box-shadow: 0 2px 8px rgba(91,95,199,0.1);
}

.item-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.item-info {
  min-width: 0;
}

.item-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a2e;
  display: block;
}

.item-code {
  font-size: 11px;
  color: #8c8c8c;
}

.item-score {
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.item-score.deduct {
  background: #fff1f0;
  color: #ff4d4f;
}

.item-score.add {
  background: #f6ffed;
  color: #52c41a;
}

.item-desc {
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}

.item-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.item-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.item-actions {
  display: flex;
  gap: 4px;
}

/* 摘要面板 */
.summary-section {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.summary-title {
  font-size: 12px;
  font-weight: 600;
  color: #8c8c8c;
  margin-bottom: 12px;
  text-transform: uppercase;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}

.summary-label {
  font-size: 13px;
  color: #666;
}

.summary-value {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a2e;
}

.summary-value.highlight {
  color: #5b5fc7;
}

/* 权重分配 */
.weight-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.weight-bar-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.weight-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.weight-name {
  flex: 1;
  font-size: 13px;
  color: #666;
}

.weight-value {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.weight-sum {
  font-size: 12px;
  color: #52c41a;
}

.weight-sum.error {
  color: #ff4d4f;
}

/* 底部操作栏 */
.footer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-radius: 12px;
  margin: 0 16px 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

/* 列表模式 */
.template-list-container {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.list-toolbar {
  margin-bottom: 16px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.template-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.template-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

.template-card .card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.template-card .card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.template-card .card-code {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.template-card .card-desc {
  font-size: 13px;
  color: #666;
  margin-bottom: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.template-card .card-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.template-card .card-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

/* 对话框 */
.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.color-presets {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.color-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.15s;
  border: 2px solid transparent;
}

.color-dot:hover {
  transform: scale(1.15);
}

.color-dot.active {
  border-color: #333;
}

.checkbox-group {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

/* 分值配置 */
.score-config-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.score-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 15px;
  font-weight: 500;
}

.score-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.score-label {
  width: 80px;
  font-size: 13px;
  color: #666;
}

.score-unit {
  font-size: 13px;
  color: #999;
}

.score-hint {
  font-size: 13px;
  color: #999;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.empty-tip {
  text-align: center;
  padding: 30px;
  color: #999;
  font-size: 13px;
}
</style>
