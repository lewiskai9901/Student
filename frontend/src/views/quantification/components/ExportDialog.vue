<template>
  <el-dialog
    v-model="visible"
    title="导出数据"
    width="900px"
    @close="handleClose"
    destroy-on-close
  >
    <div class="export-dialog">
      <!-- 步骤条 -->
      <el-steps :active="currentStep" finish-status="success" align-center class="step-bar">
        <el-step title="选择模板" />
        <el-step title="筛选班级" />
        <el-step title="预览导出" />
      </el-steps>

      <!-- 步骤1: 选择模板 -->
      <div v-show="currentStep === 0" class="step-content">
        <div v-if="templates.length === 0" class="empty-state">
          <el-empty description="暂无导出模板">
            <el-button type="primary" @click="emit('createTemplate')">
              创建模板
            </el-button>
          </el-empty>
        </div>
        <div v-else class="template-list">
          <div
            v-for="template in templates"
            :key="template.id"
            class="template-card"
            :class="{ selected: selectedTemplateId === template.id }"
            @click="selectTemplate(template)"
          >
            <div class="template-info">
              <div class="template-name">{{ template.templateName }}</div>
              <div class="template-desc">{{ template.description || '暂无描述' }}</div>
            </div>
            <div class="template-format">
              <el-tag :type="getFormatType(template.outputFormat)" size="small">
                {{ template.outputFormat }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 步骤2: 筛选数据 -->
      <div v-show="currentStep === 1" class="step-content">
        <div class="filter-section">
          <!-- 轮次筛选 -->
          <div class="filter-block">
            <div class="filter-title">
              <span>检查轮次</span>
              <el-button type="primary" link size="small" @click="selectAllRounds">全选</el-button>
              <el-button type="primary" link size="small" @click="clearRoundSelection">清空</el-button>
            </div>
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="filter-alert"
            >
              选择要导出的检查轮次。如果不选择，将导出全部轮次的数据。
            </el-alert>
            <div class="round-list" v-if="availableRounds.length > 0">
              <el-checkbox-group v-model="selectedRounds">
                <el-checkbox
                  v-for="round in availableRounds"
                  :key="round.roundNumber"
                  :label="round.roundNumber"
                  :value="round.roundNumber"
                  class="round-checkbox"
                >
                  {{ round.roundName }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
            <div v-else class="empty-rounds">
              <span>暂无轮次信息</span>
            </div>
          </div>

          <!-- 班级筛选 -->
          <div class="filter-block">
            <div class="filter-title">
              <span>班级筛选</span>
              <el-button type="primary" link size="small" @click="selectAllVisible">全选当前</el-button>
              <el-button type="primary" link size="small" @click="clearSelection">清空</el-button>
            </div>
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="filter-alert"
            >
              选择要导出的班级。如果不选择，将导出全部班级的数据。
            </el-alert>

            <el-form inline class="class-filter-form">
              <el-form-item label="院系">
                <el-select
                  v-model="filterDepartmentId"
                  clearable
                  placeholder="全部院系"
                  @change="handleDepartmentChange"
                >
                  <el-option
                    v-for="dept in departments"
                    :key="dept.id"
                    :label="dept.deptName"
                    :value="dept.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="年级">
                <el-select
                  v-model="filterGradeId"
                  clearable
                  placeholder="全部年级"
                  @change="handleGradeChange"
                >
                  <el-option
                    v-for="grade in grades"
                    :key="grade.id"
                    :label="grade.gradeName"
                    :value="grade.id"
                  />
                </el-select>
              </el-form-item>
            </el-form>

            <div class="class-list">
              <el-checkbox-group v-model="selectedClassIds">
                <el-checkbox
                  v-for="cls in filteredClasses"
                  :key="cls.id"
                  :label="cls.id"
                  :value="cls.id"
                  class="class-checkbox"
                >
                  {{ cls.className }}
                  <span class="class-dept">{{ cls.orgUnitName }} / {{ cls.gradeName }}</span>
                </el-checkbox>
              </el-checkbox-group>
            </div>

            <div class="selection-summary">
              已选择 <strong>{{ selectedClassIds.length }}</strong> 个班级
            </div>
          </div>
        </div>
      </div>

      <!-- 步骤3: 预览导出 -->
      <div v-show="currentStep === 2" class="step-content">
        <div v-if="previewLoading" class="loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在生成预览...</span>
        </div>
        <div v-else-if="previewData" class="preview-area">
          <!-- 统计信息 -->
          <div class="preview-stats">
            <div class="stat-item">
              <div class="stat-value">{{ previewData.totalCount }}</div>
              <div class="stat-label">违纪学生</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ previewData.classCount }}</div>
              <div class="stat-label">涉及班级</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ previewData.departmentCount }}</div>
              <div class="stat-label">涉及院系</div>
            </div>
          </div>

          <!-- 输出格式选择 -->
          <div class="format-selector">
            <span>导出格式：</span>
            <el-radio-group v-model="exportFormat">
              <el-radio-button label="PDF">PDF</el-radio-button>
              <el-radio-button label="WORD">Word</el-radio-button>
              <el-radio-button label="EXCEL">Excel</el-radio-button>
            </el-radio-group>
          </div>

          <!-- HTML预览 -->
          <div class="html-preview" v-if="previewData.renderedHtml">
            <div class="preview-header">
              <span>文档预览</span>
              <el-button type="primary" link @click="toggleFullPreview">
                {{ fullPreview ? '收起' : '展开' }}
              </el-button>
            </div>
            <div
              class="preview-content"
              :class="{ expanded: fullPreview }"
              v-html="sanitizedPreviewHtml"
            />
          </div>
        </div>
        <div v-else class="empty-state">
          <el-empty description="无法生成预览" />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
        <el-button
          v-if="currentStep < 2"
          type="primary"
          :disabled="!canNext"
          @click="nextStep"
        >
          下一步
        </el-button>
        <el-button
          v-if="currentStep === 2"
          type="primary"
          :loading="exportLoading"
          @click="handleExport"
        >
          导出下载
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import DOMPurify from 'dompurify'
import {
  getExportTemplatesForCheck,
  getExportPreview,
  exportFile,
  type ExportTemplateDTO,
  type ExportPreviewDTO
} from '@/api/v2/exportTemplate'
import { getAllEnabledDepartments } from '@/api/v2/organization'
import { getAllGrades } from '@/api/v2/organization'
import { getClassList } from '@/api/v2/organization'
import { getDailyCheckById } from '@/api/v2/quantification'
import { getCheckPlanDetail } from '@/api/v2/quantification'

interface RoundInfo {
  roundNumber: number
  roundName: string
}

const props = defineProps<{
  modelValue: boolean
  checkId: string | number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'createTemplate'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 步骤控制
const currentStep = ref(0)

// 模板相关
const templates = ref<ExportTemplateDTO[]>([])
const selectedTemplateId = ref<string | number | null>(null)
const selectedTemplate = computed(() =>
  templates.value.find(t => t.id === selectedTemplateId.value)
)

// 轮次筛选
const availableRounds = ref<RoundInfo[]>([])
const selectedRounds = ref<number[]>([])

// 班级筛选
const departments = ref<Array<{ id: string | number; deptName: string }>>([])
const grades = ref<Array<{ id: string | number; gradeName: string }>>([])
const classes = ref<Array<{
  id: string | number
  className: string
  orgUnitId?: string | number
  orgUnitName?: string
  gradeId?: string | number
  gradeName?: string
}>>([])
const filterDepartmentId = ref<string | number | null>(null)
const filterGradeId = ref<string | number | null>(null)
const selectedClassIds = ref<(string | number)[]>([])

// 过滤后的班级
const filteredClasses = computed(() => {
  return classes.value.filter(cls => {
    if (filterDepartmentId.value && cls.orgUnitId !== filterDepartmentId.value) {
      return false
    }
    if (filterGradeId.value && cls.gradeId !== filterGradeId.value) {
      return false
    }
    return true
  })
})

// 预览相关
const previewLoading = ref(false)
const previewData = ref<ExportPreviewDTO | null>(null)
const exportFormat = ref('PDF')
const fullPreview = ref(false)

// XSS防护：对预览HTML进行消毒
const sanitizedPreviewHtml = computed(() => {
  if (!previewData.value?.renderedHtml) return ''
  return DOMPurify.sanitize(previewData.value.renderedHtml, {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'u', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
                   'table', 'thead', 'tbody', 'tr', 'th', 'td', 'ul', 'ol', 'li',
                   'span', 'div', 'a', 'img', 'style'],
    ALLOWED_ATTR: ['class', 'style', 'href', 'src', 'alt', 'title', 'colspan', 'rowspan', 'width', 'height'],
    ALLOW_DATA_ATTR: false
  })
})

// 导出相关
const exportLoading = ref(false)

// 是否可以进入下一步
const canNext = computed(() => {
  if (currentStep.value === 0) {
    return !!selectedTemplateId.value
  }
  return true
})

// 加载模板列表
const loadTemplates = async () => {
  try {
    templates.value = await getExportTemplatesForCheck(props.checkId)
  } catch (error) {
    console.error('加载模板失败', error)
  }
}

// 加载轮次信息
const loadRounds = async () => {
  try {
    // 先获取日常检查详情以获取planId
    const dailyCheck = await getDailyCheckById(Number(props.checkId))
    if (!dailyCheck?.planId) {
      // 没有关联计划，使用默认轮次
      availableRounds.value = [
        { roundNumber: 1, roundName: '第1轮' },
        { roundNumber: 2, roundName: '第2轮' },
        { roundNumber: 3, roundName: '第3轮' },
        { roundNumber: 4, roundName: '第4轮' },
        { roundNumber: 5, roundName: '第5轮' }
      ]
      return
    }

    // 获取检查计划详情以获取模板快照
    const checkPlan = await getCheckPlanDetail(dailyCheck.planId)
    if (checkPlan?.templateSnapshot) {
      try {
        const snapshot = typeof checkPlan.templateSnapshot === 'string'
          ? JSON.parse(checkPlan.templateSnapshot)
          : checkPlan.templateSnapshot

        // 从模板快照中获取轮次配置
        if (snapshot?.roundConfigs && Array.isArray(snapshot.roundConfigs)) {
          availableRounds.value = snapshot.roundConfigs.map((rc: any, index: number) => ({
            roundNumber: rc.roundNumber || (index + 1),
            roundName: rc.roundName || `第${index + 1}轮`
          }))
        } else if (snapshot?.roundCount) {
          // 如果没有轮次配置，根据轮次数量生成
          const count = snapshot.roundCount
          availableRounds.value = Array.from({ length: count }, (_, i) => ({
            roundNumber: i + 1,
            roundName: `第${i + 1}轮`
          }))
        } else {
          // 默认5轮
          setDefaultRounds()
        }
      } catch (e) {
        console.error('解析模板快照失败', e)
        setDefaultRounds()
      }
    } else {
      setDefaultRounds()
    }
  } catch (error) {
    console.error('加载轮次信息失败', error)
    setDefaultRounds()
  }
}

// 设置默认轮次
const setDefaultRounds = () => {
  availableRounds.value = [
    { roundNumber: 1, roundName: '第1轮' },
    { roundNumber: 2, roundName: '第2轮' },
    { roundNumber: 3, roundName: '第3轮' },
    { roundNumber: 4, roundName: '第4轮' },
    { roundNumber: 5, roundName: '第5轮' }
  ]
}

// 加载基础数据
const loadBaseData = async () => {
  try {
    const [deptRes, gradeRes, classRes] = await Promise.all([
      getAllEnabledDepartments(),
      getAllGrades(),
      getClassList({ pageSize: 1000 })
    ])
    departments.value = deptRes || []
    grades.value = gradeRes || []
    classes.value = (classRes?.list || classRes || []).map((c: any) => ({
      id: c.id,
      className: c.className,
      orgUnitId: c.orgUnitId,
      orgUnitName: c.orgUnitName,
      gradeId: c.gradeId,
      gradeName: c.gradeName
    }))
  } catch (error) {
    console.error('加载基础数据失败', error)
  }
}

// 全选轮次
const selectAllRounds = () => {
  selectedRounds.value = availableRounds.value.map(r => r.roundNumber)
}

// 清空轮次选择
const clearRoundSelection = () => {
  selectedRounds.value = []
}

// 选择模板
const selectTemplate = (template: ExportTemplateDTO) => {
  selectedTemplateId.value = template.id || null
  exportFormat.value = template.outputFormat || 'PDF'
}

// 获取格式标签类型
const getFormatType = (format?: string) => {
  const types: Record<string, string> = {
    PDF: 'danger',
    WORD: 'primary',
    EXCEL: 'success'
  }
  return types[format || 'PDF'] || 'info'
}

// 部门变化
const handleDepartmentChange = () => {
  filterGradeId.value = null
}

// 年级变化
const handleGradeChange = () => {
  // 保持当前选择
}

// 全选当前可见班级
const selectAllVisible = () => {
  const visibleIds = filteredClasses.value.map(c => c.id)
  selectedClassIds.value = [...new Set([...selectedClassIds.value, ...visibleIds])]
}

// 清空选择
const clearSelection = () => {
  selectedClassIds.value = []
}

// 上一步
const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

// 下一步
const nextStep = async () => {
  if (currentStep.value < 2) {
    currentStep.value++

    // 进入预览步骤时加载预览数据
    if (currentStep.value === 2) {
      await loadPreview()
    }
  }
}

// 加载预览
const loadPreview = async () => {
  if (!selectedTemplateId.value) return

  previewLoading.value = true
  try {
    previewData.value = await getExportPreview(props.checkId, {
      templateId: selectedTemplateId.value,
      classIds: selectedClassIds.value.length > 0 ? selectedClassIds.value : undefined,
      checkRounds: selectedRounds.value.length > 0 ? selectedRounds.value : undefined
    })
  } catch (error: any) {
    ElMessage.error(error.message || '加载预览失败')
    previewData.value = null
  } finally {
    previewLoading.value = false
  }
}

// 切换完整预览
const toggleFullPreview = () => {
  fullPreview.value = !fullPreview.value
}

// 导出下载
const handleExport = async () => {
  if (!selectedTemplateId.value) return

  exportLoading.value = true
  try {
    const blob = await exportFile(props.checkId, {
      templateId: selectedTemplateId.value,
      classIds: selectedClassIds.value.length > 0 ? selectedClassIds.value : undefined,
      checkRounds: selectedRounds.value.length > 0 ? selectedRounds.value : undefined,
      format: exportFormat.value
    })

    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url

    // 设置文件名
    const ext = exportFormat.value === 'WORD' ? '.docx' :
                exportFormat.value === 'EXCEL' ? '.xlsx' : '.pdf'
    link.download = `${selectedTemplate.value?.templateName || '导出文件'}_${new Date().toISOString().slice(0, 10)}${ext}`

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
    handleClose()
  } catch (error: any) {
    ElMessage.error(error.message || '导出失败')
  } finally {
    exportLoading.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
  // 重置状态
  currentStep.value = 0
  selectedTemplateId.value = null
  selectedClassIds.value = []
  selectedRounds.value = []
  previewData.value = null
  fullPreview.value = false
}

// 监听打开
watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      loadTemplates()
      loadBaseData()
      loadRounds()
    }
  }
)
</script>

<style scoped lang="scss">
.export-dialog {
  min-height: 400px;
}

.step-bar {
  margin-bottom: 24px;
}

.step-content {
  min-height: 300px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 250px;
}

.template-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.template-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border: 2px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #409eff;
  }

  &.selected {
    border-color: #409eff;
    background: #ecf5ff;
  }

  .template-info {
    flex: 1;
  }

  .template-name {
    font-weight: 600;
    font-size: 15px;
    margin-bottom: 4px;
  }

  .template-desc {
    font-size: 13px;
    color: #909399;
  }
}

.filter-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-block {
  .filter-title {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    font-weight: 600;
    font-size: 15px;
    color: #303133;
  }

  .filter-alert {
    margin-bottom: 12px;
  }
}

.round-list {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;

  .round-checkbox {
    margin-right: 20px;
    margin-bottom: 8px;
  }
}

.empty-rounds {
  padding: 20px;
  text-align: center;
  color: #909399;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}

.class-filter-form {
  margin-bottom: 12px;
}

.class-list {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
}

.class-checkbox {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  width: 100%;

  .class-dept {
    margin-left: 8px;
    font-size: 12px;
    color: #909399;
  }
}

.selection-summary {
  margin-top: 12px;
  text-align: right;
  color: #606266;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: #909399;

  .is-loading {
    font-size: 32px;
    margin-bottom: 12px;
  }
}

.preview-area {
  .preview-stats {
    display: flex;
    gap: 40px;
    justify-content: center;
    margin-bottom: 20px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 8px;

    .stat-item {
      text-align: center;

      .stat-value {
        font-size: 28px;
        font-weight: 600;
        color: #409eff;
      }

      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .format-selector {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
  }

  .html-preview {
    border: 1px solid #ebeef5;
    border-radius: 4px;

    .preview-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 16px;
      background: #f5f7fa;
      border-bottom: 1px solid #ebeef5;
      font-weight: 600;
    }

    .preview-content {
      max-height: 300px;
      overflow-y: auto;
      padding: 20px;

      &.expanded {
        max-height: none;
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
