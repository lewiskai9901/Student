<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑导出模板' : '创建导出模板'"
    width="90%"
    top="3vh"
    @close="handleClose"
    destroy-on-close
    class="template-editor-dialog"
    :close-on-click-modal="false"
  >
    <!-- 步骤导航 -->
    <div class="steps-nav">
      <div
        v-for="(step, index) in steps"
        :key="step.key"
        class="step-item"
        :class="{ active: currentStep === index, completed: currentStep > index }"
        @click="currentStep = index"
      >
        <span class="step-number">{{ index + 1 }}</span>
        <span class="step-label">{{ step.label }}</span>
      </div>
    </div>

    <!-- 步骤内容 -->
    <div class="step-content">
      <!-- 步骤1: 基本信息 -->
      <div v-show="currentStep === 0" class="step-panel">
        <div class="form-group">
          <label class="form-label required">模板名称</label>
          <el-input
            v-model="formData.templateName"
            placeholder="如：早操迟到通报、宿舍卫生检查"
            size="large"
          />
        </div>

        <div class="form-group">
          <label class="form-label">模板描述</label>
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="2"
            placeholder="简要说明此模板的用途"
          />
        </div>
      </div>

      <!-- 步骤2: 表格配置（重构后） -->
      <div v-show="currentStep === 1" class="step-panel table-config-panel">
        <!-- 多表格切换标签 -->
        <div class="table-tabs-wrapper">
          <div class="table-tabs">
            <div
              v-for="(table, idx) in formData.tables"
              :key="idx"
              class="table-tab"
              :class="{ active: currentTableIndex === idx }"
              @click="currentTableIndex = idx"
            >
              <span class="tab-label">{{ table.title || `表格 ${idx + 1}` }}</span>
              <X
                v-if="formData.tables.length > 1"
                :size="12"
                class="tab-close"
                @click.stop="removeTable(idx)"
              />
            </div>
            <div class="table-tab add-tab" @click="addTable">
              <Plus :size="14" />
            </div>
          </div>
        </div>

        <div class="table-config-layout">
          <!-- 左侧：配置区域 -->
          <div class="config-section">
            <!-- 表格标题配置 -->
            <div class="config-card table-title-card">
              <div class="title-row">
                <el-checkbox v-model="tableConfig.showTitle">显示表格标题</el-checkbox>
                <el-input
                  v-if="tableConfig.showTitle"
                  v-model="tableConfig.title"
                  placeholder="输入表格标题"
                  size="small"
                  class="title-input"
                />
              </div>
            </div>

            <!-- 表格列选择 -->
            <div class="config-card">
              <div class="config-card-header">
                <TableProperties :size="16" />
                <span>选择显示字段</span>
              </div>
              <div class="field-selector">
                <div class="field-grid">
                  <label
                    v-for="field in availableFields"
                    :key="field.field"
                    class="field-checkbox"
                    :class="{ checked: isFieldSelected(field.field) }"
                  >
                    <input
                      type="checkbox"
                      :checked="isFieldSelected(field.field)"
                      @change="toggleField(field)"
                    />
                    <span class="field-name">{{ field.label }}</span>
                  </label>
                </div>
                <!-- 添加组合字段按钮 -->
                <div class="add-composite-btn" @click="showCompositeDialog = true">
                  <Plus :size="14" />
                  <span>添加组合字段</span>
                </div>
              </div>
            </div>

            <!-- 已选字段配置（含合并设置） -->
            <div v-if="tableConfig.columns?.length" class="config-card">
              <div class="config-card-header">
                <Layers :size="16" />
                <span>列配置</span>
                <span class="header-hint">拖动调整顺序</span>
              </div>
              <div class="column-config-list">
                <draggable
                  v-model="tableConfig.columns"
                  item-key="field"
                  ghost-class="ghost"
                  handle=".drag-handle"
                  class="column-list"
                >
                  <template #item="{ element, index }">
                    <div class="column-item-wrapper">
                      <div class="column-item-compact">
                        <GripVertical :size="14" class="drag-handle" />
                        <span class="column-index">{{ index + 1 }}</span>
                        <span class="column-name">
                          {{ element.label }}
                          <span v-if="element.mergeType === 'group'" class="group-order-badge">#{{ getGroupOrder(element) }}</span>
                        </span>

                        <!-- 合并方式：滑动tab -->
                        <div class="merge-tabs">
                          <button
                            class="merge-tab"
                            :class="{ active: !element.mergeType || element.mergeType === '' }"
                            @click="setMergeType(element, '')"
                          >默认</button>
                          <button
                            class="merge-tab"
                            :class="{ active: element.mergeType === 'group' }"
                            @click="setMergeType(element, 'group')"
                          >分组</button>
                          <button
                            class="merge-tab"
                            :class="{ active: element.mergeType === 'concat', disabled: !canSetConcat(element) }"
                            :disabled="!canSetConcat(element)"
                            @click="setMergeType(element, 'concat')"
                            :title="!canSetConcat(element) ? '需要先设置分组列' : '同组数据拼接显示'"
                          >数据</button>
                        </div>

                        <X :size="14" class="remove-btn" @click="removeField(index)" />
                      </div>

                      <!-- 别名配置行 -->
                      <div class="alias-config-row">
                        <span class="alias-config-label">列名</span>
                        <el-input
                          v-model="element.alias"
                          size="small"
                          :placeholder="element.label"
                          class="alias-input"
                          clearable
                        />
                      </div>

                      <!-- 数据合并配置：第二行显示 -->
                      <div v-if="element.mergeType === 'concat'" class="concat-config-row">
                        <span class="concat-config-label">按</span>
                        <el-select
                          v-model="element.groupByField"
                          size="small"
                          class="concat-groupby-select"
                        >
                          <el-option
                            v-for="gc in getGroupColumns()"
                            :key="gc.field"
                            :value="gc.field"
                            :label="getColumnDisplayName(gc)"
                          />
                        </el-select>
                        <span class="concat-config-label">合并，分隔符</span>
                        <el-select
                          v-model="element.separator"
                          size="small"
                          class="concat-separator-select"
                        >
                          <el-option value="、" label="顿号 、" />
                          <el-option value="，" label="逗号 ，" />
                          <el-option value="\n" label="换行 ↵" />
                          <el-option value=" " label="空格" />
                        </el-select>
                      </div>
                    </div>
                  </template>
                </draggable>
              </div>

              <!-- 提示信息 -->
              <div v-if="!hasGroupColumns()" class="merge-tip warning">
                <Info :size="14" />
                <span>提示：设置"分组"列后，才能使用"数据"合并</span>
              </div>
              <div v-else class="merge-tip info">
                <Info :size="14" />
                <span>分组列：相同内容合并单元格 | 数据列：同组数据拼接显示</span>
              </div>

              <!-- 分组层级顺序（仅当有分组时显示） -->
              <div v-if="getGroupColumns().length > 0" class="hierarchy-order-section">
                <div class="hierarchy-order-header">
                  <span>分组优先级（拖动调整顺序）</span>
                </div>
                <draggable
                  :modelValue="getGroupColumns()"
                  @update:modelValue="updateGroupOrder"
                  item-key="field"
                  ghost-class="ghost"
                  class="hierarchy-order-list"
                >
                  <template #item="{ element, index }">
                    <div class="hierarchy-order-tag">
                      <span class="order-badge">{{ index + 1 }}</span>
                      <span>{{ getColumnDisplayName(element) }}</span>
                    </div>
                  </template>
                </draggable>
              </div>
            </div>

            <!-- 表格样式（折叠面板） -->
            <el-collapse class="style-collapse">
              <el-collapse-item title="边框样式" name="border">
                <div class="style-grid">
                  <div class="style-item">
                    <label>边框颜色</label>
                    <el-color-picker v-model="tableConfig.borderColor" size="small" />
                  </div>
                  <div class="style-item">
                    <label>边框宽度</label>
                    <el-select v-model="tableConfig.borderWidth" size="small" style="width: 100px;">
                      <el-option label="1px" value="1px" />
                      <el-option label="2px" value="2px" />
                      <el-option label="3px" value="3px" />
                    </el-select>
                  </div>
                  <div class="style-item">
                    <label>边框样式</label>
                    <el-select v-model="tableConfig.borderStyle" size="small" style="width: 100px;">
                      <el-option label="实线" value="solid" />
                      <el-option label="虚线" value="dashed" />
                      <el-option label="点线" value="dotted" />
                      <el-option label="双线" value="double" />
                    </el-select>
                  </div>
                </div>
              </el-collapse-item>
              <el-collapse-item title="表头样式" name="header">
                <div class="style-grid">
                  <div class="style-item">
                    <label>背景颜色</label>
                    <el-color-picker v-model="tableConfig.headerBgColor" size="small" />
                  </div>
                  <div class="style-item">
                    <label>文字颜色</label>
                    <el-color-picker v-model="tableConfig.headerTextColor" size="small" />
                  </div>
                  <div class="style-item">
                    <label>字体粗细</label>
                    <el-select v-model="tableConfig.headerFontWeight" size="small" style="width: 100px;">
                      <el-option label="正常" value="normal" />
                      <el-option label="加粗" value="bold" />
                      <el-option label="更粗" value="800" />
                    </el-select>
                  </div>
                  <div class="style-item">
                    <label>字体大小</label>
                    <el-select v-model="tableConfig.headerFontSize" size="small" style="width: 100px;">
                      <el-option label="12px" value="12px" />
                      <el-option label="14px" value="14px" />
                      <el-option label="16px" value="16px" />
                      <el-option label="18px" value="18px" />
                    </el-select>
                  </div>
                </div>
              </el-collapse-item>
              <el-collapse-item title="单元格样式" name="cell">
                <div class="style-grid">
                  <div class="style-item">
                    <label>内边距</label>
                    <el-select v-model="tableConfig.cellPadding" size="small" style="width: 100px;">
                      <el-option label="紧凑 (4px)" value="4px" />
                      <el-option label="标准 (8px)" value="8px" />
                      <el-option label="宽松 (12px)" value="12px" />
                      <el-option label="超宽 (16px)" value="16px" />
                    </el-select>
                  </div>
                  <div class="style-item">
                    <label>字体大小</label>
                    <el-select v-model="tableConfig.fontSize" size="small" style="width: 100px;">
                      <el-option label="12px" value="12px" />
                      <el-option label="14px" value="14px" />
                      <el-option label="16px" value="16px" />
                      <el-option label="18px" value="18px" />
                    </el-select>
                  </div>
                  <div class="style-item">
                    <label>水平对齐</label>
                    <el-select v-model="tableConfig.textAlign" size="small" style="width: 100px;">
                      <el-option label="左对齐" value="left" />
                      <el-option label="居中" value="center" />
                      <el-option label="右对齐" value="right" />
                    </el-select>
                  </div>
                  <div class="style-item">
                    <label>垂直对齐</label>
                    <el-select v-model="tableConfig.verticalAlign" size="small" style="width: 100px;">
                      <el-option label="顶部" value="top" />
                      <el-option label="居中" value="middle" />
                      <el-option label="底部" value="bottom" />
                    </el-select>
                  </div>
                  <div class="style-item">
                    <label>行高</label>
                    <el-select v-model="tableConfig.lineHeight" size="small" style="width: 100px;">
                      <el-option label="紧凑 (1.2)" value="1.2" />
                      <el-option label="标准 (1.5)" value="1.5" />
                      <el-option label="宽松 (1.8)" value="1.8" />
                      <el-option label="超宽 (2)" value="2" />
                    </el-select>
                  </div>
                </div>
              </el-collapse-item>
              <el-collapse-item title="行样式" name="row">
                <div class="style-grid">
                  <div class="style-item full-width">
                    <label class="checkbox-inline">
                      <input type="checkbox" v-model="tableConfig.zebraStripes" />
                      <span>启用斑马纹（交替行背景色）</span>
                    </label>
                  </div>
                  <div class="style-item" v-if="tableConfig.zebraStripes">
                    <label>斑马纹颜色</label>
                    <el-color-picker v-model="tableConfig.zebraColor" size="small" />
                  </div>
                  <div class="style-item full-width">
                    <label class="checkbox-inline">
                      <input type="checkbox" v-model="tableConfig.hoverHighlight" />
                      <span>鼠标悬停高亮（仅预览有效）</span>
                    </label>
                  </div>
                  <div class="style-item" v-if="tableConfig.hoverHighlight">
                    <label>悬停颜色</label>
                    <el-color-picker v-model="tableConfig.hoverColor" size="small" />
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>

          <!-- 右侧：预览区域 -->
          <div class="preview-section">
            <div class="preview-header">
              <Eye :size="18" />
              <span>表格预览</span>
            </div>
            <div class="preview-container">
              <div class="table-preview" v-html="tablePreviewHtml"></div>
            </div>
            <div class="preview-note">
              <Info :size="14" />
              <span>预览使用示例数据，实际导出时将使用真实数据</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 步骤3: 文档模板（左右布局） -->
      <div v-show="currentStep === 2" class="step-panel editor-panel">
        <div class="editor-layout">
          <!-- 左侧：工具面板 -->
          <div class="tools-panel">
            <div class="tools-section">
              <div class="tools-title">
                <Variable :size="16" />
                <span>插入变量</span>
              </div>
              <div class="variable-list">
                <button
                  v-for="v in systemVariables"
                  :key="v.code"
                  class="variable-btn"
                  @click="insertVariable(v.code)"
                >
                  <span class="var-code">{{ v.code.replace(/\{\{|\}\}/g, '') }}</span>
                  <span class="var-label">{{ v.label }}</span>
                </button>
              </div>
            </div>
            <div class="tools-section">
              <div class="tools-title">
                <Table2 :size="16" />
                <span>插入表格</span>
              </div>
              <!-- 多表格选择 -->
              <div class="table-insert-list">
                <button
                  v-if="formData.tables.length === 1"
                  class="insert-table-btn"
                  @click="insertVariable('{{TABLE}}')"
                >
                  <Table2 :size="18" />
                  <span>插入数据表格</span>
                </button>
                <template v-else>
                  <button
                    class="insert-table-btn insert-all"
                    @click="insertVariable('{{TABLE_ALL}}')"
                  >
                    <Table2 :size="16" />
                    <span>插入全部表格</span>
                  </button>
                  <button
                    v-for="(table, idx) in formData.tables"
                    :key="idx"
                    class="insert-table-btn insert-single"
                    @click="insertVariable(`{{TABLE_${idx + 1}}}`)"
                  >
                    <Table2 :size="14" />
                    <span>{{ table.title || `表格 ${idx + 1}` }}</span>
                  </button>
                </template>
              </div>
              <p class="tools-hint">表格将按照上一步的配置生成</p>
            </div>
            <div class="tools-section">
              <div class="tools-title">
                <FileText :size="16" />
                <span>模板说明</span>
              </div>
              <ul class="tips-list">
                <li>变量用 <code v-pre>{{变量名}}</code> 表示</li>
                <li>导出时变量自动替换为实际值</li>
                <li v-if="formData.tables.length === 1"><code v-pre>{{TABLE}}</code> 表示表格位置</li>
                <li v-else><code v-pre>{{TABLE_1}}</code>...<code v-pre>{{TABLE_N}}</code> 表示各表格</li>
                <li>可自由编辑文档内容和样式</li>
              </ul>
            </div>
          </div>
          <!-- 右侧：编辑器 -->
          <div class="editor-main">
            <div class="editor-wrapper">
              <!-- TinyMCE 编辑器已暂时替换为简单 textarea -->
              <el-input
                v-model="formData.documentTemplate"
                type="textarea"
                :rows="20"
                placeholder="请输入文档模板内容（支持 HTML 格式）"
                class="template-textarea"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作 -->
    <template #footer>
      <div class="dialog-footer">
        <div class="footer-left">
          <el-button v-if="currentStep > 0" @click="currentStep--">
            <ChevronLeft :size="16" />
            上一步
          </el-button>
        </div>
        <div class="footer-right">
          <el-button @click="handleClose">取消</el-button>
          <el-button
            v-if="currentStep < steps.length - 1"
            type="primary"
            @click="nextStep"
          >
            下一步
            <ChevronRight :size="16" />
          </el-button>
          <el-button
            v-else
            type="primary"
            :loading="loading"
            @click="handleSubmit"
          >
            {{ isEdit ? '保存模板' : '创建模板' }}
          </el-button>
        </div>
      </div>
    </template>

  <!-- 组合字段对话框 -->
  <el-dialog
    v-model="showCompositeDialog"
    title="添加组合字段"
    width="500px"
    @close="resetCompositeForm"
  >
    <div class="composite-form">
      <div class="form-item">
        <label>字段名称</label>
        <el-input v-model="compositeForm.label" placeholder="如：姓名学号" />
      </div>
      <div class="form-item">
        <label>选择要组合的字段（按顺序选择）</label>
        <div class="composite-field-list">
          <div
            v-for="field in availableFields"
            :key="field.field"
            class="composite-field-item"
            :class="{ selected: compositeForm.fields.includes(field.field) }"
            @click="toggleCompositeField(field.field)"
          >
            <span class="field-order" v-if="compositeForm.fields.includes(field.field)">
              {{ compositeForm.fields.indexOf(field.field) + 1 }}
            </span>
            <span>{{ field.label }}</span>
          </div>
        </div>
      </div>
      <div class="form-item">
        <label>分隔符</label>
        <div class="separator-options">
          <label v-for="sep in separatorOptions" :key="sep.value" class="separator-option"
            :class="{ selected: compositeForm.separator === sep.value }">
            <input type="radio" v-model="compositeForm.separator" :value="sep.value" hidden />
            <span>{{ sep.label }}</span>
          </label>
          <el-input
            v-if="compositeForm.separator === 'custom'"
            v-model="compositeForm.customSeparator"
            placeholder="自定义"
            size="small"
            style="width: 80px; margin-left: 8px;"
          />
        </div>
      </div>
      <div class="form-item" v-if="compositeForm.fields.length >= 2">
        <label>预览</label>
        <div class="composite-preview">{{ compositePreview }}</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="showCompositeDialog = false">取消</el-button>
      <el-button type="primary" @click="addCompositeField" :disabled="!canAddComposite">添加</el-button>
    </template>
  </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import DOMPurify from 'dompurify'
import {
  X, GripVertical, ChevronLeft, ChevronRight,
  FileText, Table2, Info, Layers,
  GitBranch, TableProperties, Eye, Variable, Plus
} from 'lucide-vue-next'
import draggable from 'vuedraggable'

// ========== TinyMCE 已移除，暂时注释以解除依赖问题 ==========
// TODO: 如需富文本编辑器，请使用 @wangeditor/editor 或重新安装 tinymce
// // TinyMCE 核心 - 必须在组件之前导入
// import tinymce from 'tinymce/tinymce'
// import 'tinymce/themes/silver/theme'
// import 'tinymce/icons/default/icons'
// import 'tinymce/models/dom/model'
//
// // 在导入插件之前设置全局默认配置（重要！）
// tinymce.overrideDefaults({
//   license_key: 'gpl',
//   promotion: false,
//   branding: false
// })
//
// // TinyMCE 插件
// import 'tinymce/plugins/accordion'
// import 'tinymce/plugins/advlist'
// import 'tinymce/plugins/anchor'
// import 'tinymce/plugins/autolink'
// import 'tinymce/plugins/autosave'
// import 'tinymce/plugins/charmap'
// import 'tinymce/plugins/code'
// import 'tinymce/plugins/codesample'
// import 'tinymce/plugins/directionality'
// import 'tinymce/plugins/emoticons'
// import 'tinymce/plugins/emoticons/js/emojis'
// import 'tinymce/plugins/fullscreen'
// import 'tinymce/plugins/help'
// import 'tinymce/plugins/image'
// import 'tinymce/plugins/importcss'
// import 'tinymce/plugins/insertdatetime'
// import 'tinymce/plugins/link'
// import 'tinymce/plugins/lists'
// import 'tinymce/plugins/media'
// import 'tinymce/plugins/nonbreaking'
// import 'tinymce/plugins/pagebreak'
// import 'tinymce/plugins/preview'
// import 'tinymce/plugins/quickbars'
// import 'tinymce/plugins/save'
// import 'tinymce/plugins/searchreplace'
// import 'tinymce/plugins/table'
// import 'tinymce/plugins/visualblocks'
// import 'tinymce/plugins/visualchars'
// import 'tinymce/plugins/wordcount'
// // TinyMCE 皮肤
// import 'tinymce/skins/ui/oxide/skin.min.css'
// import contentCss from 'tinymce/skins/content/default/content.min.css?raw'
// import contentUiCss from 'tinymce/skins/ui/oxide/content.min.css?raw'
// // TinyMCE Vue 组件
// import TinymceEditor from '@tinymce/tinymce-vue'
import {
  createExportTemplate,
  updateExportTemplate,
  AVAILABLE_FIELDS,
  type ExportTemplateDTO,
  type ExportTemplateRequest,
  type ColumnConfig
} from '@/api/exportTemplate'

const props = defineProps<{
  modelValue: boolean
  planId: string | number
  template?: ExportTemplateDTO | null
  deductionItems?: Array<{ id: string | number; itemName: string }>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const isEdit = computed(() => !!props.template?.id)
const loading = ref(false)
const currentStep = ref(0)

// 步骤定义
const steps = [
  { key: 'basic', label: '基本信息' },
  { key: 'table', label: '表格配置' },
  { key: 'template', label: '文档模板' }
]


// 系统变量
const systemVariables = [
  { code: '{{checkDate}}', label: '检查日期' },
  { code: '{{checkName}}', label: '检查名称' },
  { code: '{{totalCount}}', label: '总人数' },
  { code: '{{exportDate}}', label: '导出日期' }
]

// 可用字段
const availableFields = AVAILABLE_FIELDS

// ========== 组合字段功能 ==========
const showCompositeDialog = ref(false)
const compositeForm = ref({
  label: '',
  fields: [] as string[],
  separator: ' ',
  customSeparator: ''
})

const separatorOptions = [
  { value: ' ', label: '空格' },
  { value: '-', label: '横杠 -' },
  { value: '/', label: '斜杠 /' },
  { value: '|', label: '竖线 |' },
  { value: '_', label: '下划线 _' },
  { value: '', label: '无' },
  { value: 'custom', label: '自定义' }
]

const compositePreview = computed(() => {
  if (compositeForm.value.fields.length < 2) return ''
  const sep = compositeForm.value.separator === 'custom' 
    ? compositeForm.value.customSeparator 
    : compositeForm.value.separator
  const sampleValues: Record<string, string> = {
    studentNo: '2023001',
    studentName: '张三',
    gender: '男',
    className: '计算机1班',
    gradeName: '23级',
    departmentName: '经信系',
    headTeacher: '李老师',
    // 宿舍/教室共用字段
    buildingName: 'A栋',
    roomNo: '101',
    // 检查相关
    categoryName: '考勤',
    relationType: '无',
    deductionItemName: '迟到',
    deductMode: '按人数扣分',
    personCount: '1',
    deductScore: '-2',
    originalTotalScore: '-2',
    weightedDeductScore: '-1.6',
    totalWeightedDeductScore: '-1.6',
    checkerName: '王检查员',
    remark: ''
  }
  return compositeForm.value.fields
    .map(f => sampleValues[f] || f)
    .join(sep)
})

const canAddComposite = computed(() => {
  return compositeForm.value.label.trim() && 
         compositeForm.value.fields.length >= 2
})

function toggleCompositeField(field: string) {
  const index = compositeForm.value.fields.indexOf(field)
  if (index >= 0) {
    compositeForm.value.fields.splice(index, 1)
  } else {
    compositeForm.value.fields.push(field)
  }
}

function resetCompositeForm() {
  compositeForm.value = {
    label: '',
    fields: [],
    separator: ' ',
    customSeparator: ''
  }
}

function addCompositeField() {
  if (!canAddComposite.value) return
  
  const sep = compositeForm.value.separator === 'custom' 
    ? compositeForm.value.customSeparator 
    : compositeForm.value.separator
  
  const compositeField: ColumnConfig = {
    field: 'composite_' + new Date().getTime(),
    label: compositeForm.value.label,
    width: 120,
    align: 'center',
    groupable: false,
    isComposite: true,
    compositeFields: [...compositeForm.value.fields],
    compositeSeparator: sep
  }
  
  if (!tableConfig.value.columns) {
    tableConfig.value.columns = []
  }
  tableConfig.value.columns.push(compositeField)
  
  showCompositeDialog.value = false
  resetCompositeForm()
}


// 获取层级标签的映射
const fieldLabelMap = computed(() => {
  const map: Record<string, string> = {}
  AVAILABLE_FIELDS.forEach(f => { map[f.field] = f.label })
  return map
})

// 获取层级标签
const getLevelLabel = (value: string) => {
  return fieldLabelMap.value[value] || value
}

// TinyMCE 编辑器
const editorRef = shallowRef<any>(null)

// ========== TinyMCE 配置已注释，使用简单 textarea 替代 ==========
// const tinymceConfig = {
//   license_key: 'gpl',
//   language: 'zh_CN',
//   language_url: '/tinymce/langs/zh_CN.js',
//   height: '100%',
//   min_height: 500,
//   promotion: false,
//   branding: false,
//   skin: false,
//   content_css: false,
//   content_style: [contentCss, contentUiCss, `
//     body { font-family: 'Microsoft YaHei', SimSun, serif; font-size: 14px; line-height: 1.8; padding: 20px 25px; }
//     p { margin: 0.5em 0; }
//     table { border-collapse: collapse; width: 100%; margin: 10px 0; }
//     table td, table th { border: 1px solid #333; padding: 8px; }
//     table th { background: #f0f0f0; font-weight: bold; }
//     img { max-width: 100%; }
//     .pagebreak { page-break-before: always; border-top: 1px dashed #999; margin: 20px 0; }
//   `].join('\n'),
//   setup: (editor: any) => {
//     editor.on('init', () => {
//       if (editor.mode.get() === 'readonly') {
//         editor.mode.set('design')
//       }
//     })
//   },
//   menubar: 'file edit view insert format table tools help',
//   plugins: [
//     'accordion', 'advlist', 'anchor', 'autolink', 'autosave', 'charmap', 'code',
//     'codesample', 'directionality', 'emoticons', 'fullscreen', 'help', 'image',
//     'importcss', 'insertdatetime', 'link', 'lists', 'media', 'nonbreaking',
//     'pagebreak', 'preview', 'quickbars', 'save', 'searchreplace', 'table',
//     'visualblocks', 'visualchars', 'wordcount'
//   ],
//   toolbar: [
//     'undo redo | blocks fontfamily fontsize lineheight | bold italic underline strikethrough | subscript superscript removeformat',
//     'alignleft aligncenter alignright alignjustify | outdent indent | bullist numlist | ltr rtl',
//     'forecolor backcolor | hr pagebreak nonbreaking | accordion table image media link anchor codesample',
//     'charmap emoticons insertdatetime | searchreplace | visualblocks visualchars code fullscreen preview print help'
//   ].join(' | '),
//   quickbars_selection_toolbar: 'bold italic underline strikethrough | quicklink h2 h3 blockquote | forecolor backcolor',
//   quickbars_insert_toolbar: 'quickimage quicktable hr pagebreak',
//   quickbars_image_toolbar: 'alignleft aligncenter alignright | rotateleft rotateright | imageoptions',
//   font_family_formats: '微软雅黑=Microsoft YaHei;宋体=SimSun;黑体=SimHei;楷体=KaiTi;仿宋=FangSong;' +
//     'Arial=arial,helvetica,sans-serif;Times New Roman=times new roman,times,serif;' +
//     'Courier New=courier new,courier,monospace;Georgia=georgia,palatino',
//   font_size_formats: '8px 10px 12px 14px 16px 18px 20px 24px 28px 32px 36px 48px 72px',
//   line_height_formats: '1 1.2 1.4 1.5 1.6 1.8 2 2.5 3',
//   resize: true,
//   statusbar: true,
//   image_advtab: true,
//   image_caption: true,
//   automatic_uploads: false,
//   file_picker_types: 'image',
//   table_advtab: true,
//   table_cell_advtab: true,
//   table_row_advtab: true,
//   table_default_styles: { 'border-collapse': 'collapse', 'width': '100%' },
//   table_default_attributes: { 'border': '1' },
//   link_default_target: '_blank',
//   link_assume_external_targets: true,
//   paste_data_images: true,
//   browser_spellcheck: true,
//   contextmenu: 'link image table'
// }

// const handleEditorInit = (e: any) => {
//   const editor = e.target
//   editorRef.value = editor
//   if (editor && editor.mode && editor.mode.get() !== 'design') {
//     editor.mode.set('design')
//   }
// }

// 默认模板
const defaultTemplate = `
<div style="max-width: 680px; margin: 0 auto; padding: 40px 60px; font-family: SimSun, serif;">
<div style="text-align: center; margin-bottom: 20px;">
<h1 style="color: #c00000; font-size: 32px; font-weight: bold; font-family: SimHei, sans-serif; letter-spacing: 8px; margin: 0px;">经济与信息技术系学生管理办公室</h1>
</div>
<hr style="border: none; border-top: 2px solid #C00000; margin: 20px 0;">
<h2 style="text-align: center; font-size: 22px; font-weight: bold; font-family: 'SimHei', sans-serif; margin: 30px 0; line-height: 1.5;">关于{{checkDate}}学生违纪情况的通报</h2>
<p style="font-size: 16px; line-height: 2; margin-bottom: 15px;">各院系、各班级：</p>
<p style="text-indent: 2em; font-size: 16px; line-height: 2; margin-bottom: 15px;">为进一步加强学生日常行为规范管理，维护正常的教学秩序，根据{{checkDate}}检查结果，共有 <strong>{{totalCount}}</strong> 名学生存在违纪行为，现将有关情况通报如下：</p>
<div style="margin: 25px 0;">{{TABLE}}</div>
<p style="text-indent: 2em; font-size: 16px; line-height: 2; margin-bottom: 15px;">以上违纪学生的行为已违反了《学生手册》相关规定，根据学校有关规章制度，现予以通报批评，并扣除相应量化考核分数。</p>
<p style="text-indent: 2em; font-size: 16px; line-height: 2; margin-bottom: 15px;">希望以上同学认真反思，引以为戒，严格遵守学校各项规章制度。各班级要以此为鉴，进一步加强学生教育管理工作，杜绝类似事件再次发生。</p>
<div style="margin-top: 50px; text-align: right; padding-right: 80px;">
<p style="font-size: 16px; line-height: 2; margin: 0;">经济与信息技术系学生管理办公室</p>
<p style="font-size: 16px; line-height: 2; margin: 0;">{{exportDate}}&nbsp; &nbsp;&nbsp;</p>
</div>
</div>
`.trim()

// 默认表格配置
const createDefaultTableConfig = () => ({
  title: '',  // 表格标题
  showTitle: false,  // 是否显示标题
  columns: [] as ColumnConfig[],
  showDepartmentHeader: false,
  showGradeHeader: false,
  showClassHeader: true,
  // 边框样式
  borderColor: '#000000',
  borderWidth: '1px',
  borderStyle: 'solid',
  // 表头样式
  headerBgColor: '#ffffff',
  headerTextColor: '#000000',
  headerFontWeight: 'bold',
  headerFontSize: '14px',
  // 单元格样式
  cellPadding: '8px',
  fontSize: '14px',
  textAlign: 'center',
  verticalAlign: 'middle',
  lineHeight: '1.5',
  // 行样式
  rowHeight: 'auto',
  zebraStripes: false,
  zebraColor: '#f9f9f9',
  hoverHighlight: false,
  hoverColor: '#e6f7ff',
  // 合并配置
  mergeConfig: {
    enabled: false,
    hierarchyLevels: [] as string[],
    concatDataRows: false,
    separator: '、'
  }
})

// 当前选中的表格索引
const currentTableIndex = ref(0)

// 表单数据
const formData = ref({
  templateName: '',
  description: '',
  outputFormat: 'PDF',
  filterConfig: {
    deductionItemIds: [] as number[],
    checkRounds: [] as number[],
    categoryIds: [] as number[],
    includeAllItems: true
  },
  // 多表格配置
  tables: [createDefaultTableConfig()],
  // 保留 tableConfig 作为计算属性的后备
  documentTemplate: defaultTemplate
})

// 当前表格配置（计算属性，方便访问）
const tableConfig = computed({
  get: () => formData.value.tables[currentTableIndex.value] || formData.value.tables[0],
  set: (val) => {
    if (formData.value.tables[currentTableIndex.value]) {
      formData.value.tables[currentTableIndex.value] = val
    }
  }
})

// 添加新表格
const addTable = () => {
  formData.value.tables.push(createDefaultTableConfig())
  currentTableIndex.value = formData.value.tables.length - 1
}

// 删除表格
const removeTable = (index: number) => {
  if (formData.value.tables.length <= 1) return
  formData.value.tables.splice(index, 1)
  if (currentTableIndex.value >= formData.value.tables.length) {
    currentTableIndex.value = formData.value.tables.length - 1
  }
}

// 复制表格
const duplicateTable = (index: number) => {
  const copy = JSON.parse(JSON.stringify(formData.value.tables[index]))
  copy.title = copy.title ? `${copy.title} (副本)` : '表格副本'
  formData.value.tables.splice(index + 1, 0, copy)
  currentTableIndex.value = index + 1
}

// 获取所有分组合并的列
const getGroupColumns = () => {
  return tableConfig.value.columns
    .filter((col: any) => col.mergeType === 'group')
    .sort((a: any, b: any) => (a.groupOrder || 999) - (b.groupOrder || 999))
}

// 获取列的分组优先级
const getGroupOrder = (col: any) => {
  const groupCols = getGroupColumns()
  const index = groupCols.findIndex((c: any) => c.field === col.field)
  return index >= 0 ? index + 1 : '-'
}

// 获取列的显示名称（优先使用别名）
const getColumnDisplayName = (col: any) => {
  return col.alias || col.label || col.field
}

// 更新分组顺序
const updateGroupOrder = (newOrder: any[]) => {
  newOrder.forEach((col, index) => {
    const target = tableConfig.value.columns.find((c: any) => c.field === col.field)
    if (target) {
      target.groupOrder = index + 1
    }
  })
}

// 设置合并类型
const setMergeType = (col: any, type: string) => {
  if (type === 'concat' && !canSetConcat(col)) {
    return // 没有分组列时不能设置数据合并
  }
  col.mergeType = type
  onMergeTypeChange(col)
}

// 检查列是否可以设置数据合并
const canSetConcat = (col: any) => {
  // 必须存在至少一个分组列（且不是自己）
  return tableConfig.value.columns.some(
    (c: any) => c.mergeType === 'group' && c.field !== col.field
  )
}

// 合并类型改变时的处理
const onMergeTypeChange = (col: any) => {
  if (col.mergeType === 'group') {
    // 设置分组优先级
    const groupCols = tableConfig.value.columns.filter((c: any) => c.mergeType === 'group')
    col.groupOrder = groupCols.length
    col.separator = undefined
    col.groupByField = undefined
  } else if (col.mergeType === 'concat') {
    // 设置默认分隔符
    if (!col.separator) {
      col.separator = '、'
    }
    // 设置默认的参考分组列（取最后一个分组列，通常是最细粒度的）
    const groupCols = getGroupColumns()
    if (groupCols.length > 0 && !col.groupByField) {
      col.groupByField = groupCols[groupCols.length - 1].field
    }
    col.groupOrder = undefined
  } else {
    col.groupOrder = undefined
    col.separator = undefined
    col.groupByField = undefined
  }

  // 检查：如果取消了分组列，需要检查数据合并列是否还有效
  if (col.mergeType !== 'group') {
    const hasOtherGroup = tableConfig.value.columns.some(
      (c: any) => c.mergeType === 'group' && c.field !== col.field
    )
    if (!hasOtherGroup) {
      // 没有分组列了，清除所有数据合并设置
      tableConfig.value.columns.forEach((c: any) => {
        if (c.mergeType === 'concat') {
          c.mergeType = ''
          c.separator = undefined
          c.groupByField = undefined
        }
      })
    }
  }
}

// 检查是否有分组列（用于兼容旧逻辑）
const hasGroupColumns = () => {
  return tableConfig.value.columns.some((col: any) => col.mergeType === 'group')
}

// 将旧的 tableConfig 转换为新的表格配置格式
const parseTableConfig = (tc: any) => ({
  title: tc.title || '',
  showTitle: tc.showTitle || false,
  columns: tc.columns || [],
  showDepartmentHeader: tc.showDepartmentHeader || false,
  showGradeHeader: tc.showGradeHeader || false,
  showClassHeader: tc.showClassHeader ?? true,
  borderColor: tc.borderColor || '#000000',
  borderWidth: tc.borderWidth || '1px',
  borderStyle: tc.borderStyle || 'solid',
  headerBgColor: tc.headerBgColor || '#ffffff',
  headerTextColor: tc.headerTextColor || '#000000',
  headerFontWeight: tc.headerFontWeight || 'bold',
  headerFontSize: tc.headerFontSize || '14px',
  cellPadding: tc.cellPadding || '8px',
  fontSize: tc.fontSize || '14px',
  textAlign: tc.textAlign || 'center',
  verticalAlign: tc.verticalAlign || 'middle',
  lineHeight: tc.lineHeight || '1.5',
  rowHeight: tc.rowHeight || 'auto',
  zebraStripes: tc.zebraStripes || false,
  zebraColor: tc.zebraColor || '#f9f9f9',
  hoverHighlight: tc.hoverHighlight || false,
  hoverColor: tc.hoverColor || '#e6f7ff',
  mergeConfig: {
    enabled: tc.mergeConfig?.enabled || false,
    hierarchyLevels: tc.mergeConfig?.hierarchyLevels || [],
    concatDataRows: tc.mergeConfig?.concatDataRows || false,
    separator: tc.mergeConfig?.separator || '、'
  }
})

// 初始化数据
watch(
  () => props.template,
  (template) => {
    if (template) {
      // 兼容旧格式：tableConfig 转为 tables 数组
      let tables: any[] = []
      if (template.tables && Array.isArray(template.tables)) {
        tables = template.tables.map((t: any) => parseTableConfig(t))
      } else if (template.tableConfig) {
        tables = [parseTableConfig(template.tableConfig)]
      } else {
        tables = [createDefaultTableConfig()]
      }

      formData.value = {
        templateName: template.templateName || '',
        description: template.description || '',
        outputFormat: template.outputFormat || 'PDF',
        filterConfig: template.filterConfig || {
          deductionItemIds: [],
          checkRounds: [],
          categoryIds: [],
          includeAllItems: true
        },
        tables,
        documentTemplate: template.documentTemplate || defaultTemplate
      }
      currentTableIndex.value = 0
    } else {
      // 创建新模板时，重置所有配置为默认值
      currentStep.value = 0
      currentTableIndex.value = 0
      formData.value.templateName = ''
      formData.value.description = ''
      formData.value.outputFormat = 'PDF'
      formData.value.tables = [createDefaultTableConfig()]
      formData.value.documentTemplate = defaultTemplate
    }
  },
  { immediate: true }
)

// 检查字段是否已选中
const isFieldSelected = (field: string) => {
  return tableConfig.value.columns?.some(c => c.field === field)
}

// 切换字段
const toggleField = (field: ColumnConfig) => {
  if (!tableConfig.value.columns) {
    tableConfig.value.columns = []
  }
  const index = tableConfig.value.columns.findIndex(c => c.field === field.field)
  if (index >= 0) {
    tableConfig.value.columns.splice(index, 1)
  } else {
    // 添加新列，初始化合并配置
    tableConfig.value.columns.push({
      ...field,
      mergeType: '',  // 默认不合并
      separator: undefined,
      groupOrder: undefined
    })
  }
}

// 移除字段
const removeField = (index: number) => {
  tableConfig.value.columns?.splice(index, 1)
}

// 表格预览HTML
const tablePreviewHtml = computed(() => {
  const config = tableConfig.value
  const columns = config.columns || []
  if (columns.length === 0) {
    return '<div style="color: #999; text-align: center; padding: 40px;">请先选择表格字段</div>'
  }

  // 边框样式
  const borderColor = config.borderColor || '#000'
  const borderWidth = config.borderWidth || '1px'
  const borderStyle = config.borderStyle || 'solid'
  const border = `${borderWidth} ${borderStyle} ${borderColor}`
  // 表头样式
  const headerBg = config.headerBgColor || '#fff'
  const headerText = config.headerTextColor || '#000'
  const headerFontWeight = config.headerFontWeight || 'bold'
  const headerFontSize = config.headerFontSize || '14px'
  // 单元格样式
  const padding = config.cellPadding || '8px'
  const fontSize = config.fontSize || '14px'
  const textAlign = config.textAlign || 'center'
  const verticalAlign = config.verticalAlign || 'middle'
  const lineHeight = config.lineHeight || '1.5'
  // 行样式
  const zebra = config.zebraStripes
  const zebraColor = config.zebraColor || '#f9f9f9'
  const mergeConfig = config.mergeConfig

  // ========== 示例数据设计思想 ==========
  // 以扣分项为核心，每扣一次分生成一条记录：
  // - 固定扣分/区间扣分：生成1条记录，不关联学生（学号、姓名为空），personCount = 1
  // - 按人数扣分：N个学生 = N条记录，每条记录关联1个学生，personCount = 1
  //
  // 分数计算规则：
  // - originalTotalScore = deductScore * personCount（每条记录 personCount=1，所以等于 deductScore）
  // - categoryOriginalScore = 同一班级同一检查类别所有记录的 originalTotalScore 之和
  // - classOriginalScore = 同一班级所有记录的 originalTotalScore 之和
  //
  // buildingName 和 roomNo 为宿舍/教室共用字段，根据 relationType 区分
  const sampleData = [
    // ========== 经信系 - 23级 - 计算机1班 ==========
    // 班级原总扣分 = -13 (考勤-8 + 宿舍卫生-5), 班级加权总扣分 = -10.4
    //
    // 考勤检查（按人数扣分）- 检查类别原总扣分 = -8, 检查类别加权总扣分 = -6.4
    // 第1轮检查 - 迟到：3名学生，每人扣2分，生成3条记录，每条记录的 personCount = 3
    { index: '1', studentNo: '2023001', studentName: '张三', gender: '男', className: '计算机1班', headTeacher: '李老师', gradeName: '23级', departmentName: '经信系', categoryName: '考勤', checkRound: '1', relationType: '无', deductionItemName: '迟到', deductMode: '按人数扣分', personCount: '3', deductScore: '-2', originalTotalScore: '-6', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-4.8', categoryOriginalScore: '-8', categoryWeightedScore: '-6.4', classOriginalScore: '-13', classWeightedScore: '-10.4', buildingName: '', roomNo: '', remark: '', checkerName: '王检查员' },
    { index: '2', studentNo: '2023002', studentName: '李四', gender: '男', className: '计算机1班', headTeacher: '李老师', gradeName: '23级', departmentName: '经信系', categoryName: '考勤', checkRound: '1', relationType: '无', deductionItemName: '迟到', deductMode: '按人数扣分', personCount: '3', deductScore: '-2', originalTotalScore: '-6', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-4.8', categoryOriginalScore: '-8', categoryWeightedScore: '-6.4', classOriginalScore: '-13', classWeightedScore: '-10.4', buildingName: '', roomNo: '', remark: '', checkerName: '王检查员' },
    { index: '3', studentNo: '2023003', studentName: '王五', gender: '女', className: '计算机1班', headTeacher: '李老师', gradeName: '23级', departmentName: '经信系', categoryName: '考勤', checkRound: '1', relationType: '无', deductionItemName: '迟到', deductMode: '按人数扣分', personCount: '3', deductScore: '-2', originalTotalScore: '-6', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-4.8', categoryOriginalScore: '-8', categoryWeightedScore: '-6.4', classOriginalScore: '-13', classWeightedScore: '-10.4', buildingName: '', roomNo: '', remark: '', checkerName: '王检查员' },
    // 第2轮检查 - 迟到：1名学生迟到，personCount = 1
    { index: '4', studentNo: '2023004', studentName: '赵六', gender: '男', className: '计算机1班', headTeacher: '李老师', gradeName: '23级', departmentName: '经信系', categoryName: '考勤', checkRound: '2', relationType: '无', deductionItemName: '迟到', deductMode: '按人数扣分', personCount: '1', deductScore: '-2', originalTotalScore: '-2', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-1.6', categoryOriginalScore: '-8', categoryWeightedScore: '-6.4', classOriginalScore: '-13', classWeightedScore: '-10.4', buildingName: '', roomNo: '', remark: '', checkerName: '王检查员' },
    //
    // 宿舍卫生检查（固定扣分，关联宿舍）- 检查类别原总扣分 = -5, 检查类别加权总扣分 = -4
    // 第1轮检查 - 101宿舍卫生不合格，固定扣3分
    { index: '5', studentNo: '', studentName: '', gender: '', className: '计算机1班', headTeacher: '李老师', gradeName: '23级', departmentName: '经信系', categoryName: '宿舍卫生', checkRound: '1', relationType: '宿舍', deductionItemName: '卫生不合格', deductMode: '固定扣分', personCount: '1', deductScore: '-3', originalTotalScore: '-3', weightedDeductScore: '-2.4', totalWeightedDeductScore: '-2.4', categoryOriginalScore: '-5', categoryWeightedScore: '-4', classOriginalScore: '-13', classWeightedScore: '-10.4', buildingName: 'A栋', roomNo: '101', remark: '', checkerName: '刘检查员' },
    // 第1轮检查 - 102宿舍物品乱放，固定扣2分
    { index: '6', studentNo: '', studentName: '', gender: '', className: '计算机1班', headTeacher: '李老师', gradeName: '23级', departmentName: '经信系', categoryName: '宿舍卫生', checkRound: '1', relationType: '宿舍', deductionItemName: '物品乱放', deductMode: '固定扣分', personCount: '1', deductScore: '-2', originalTotalScore: '-2', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-1.6', categoryOriginalScore: '-5', categoryWeightedScore: '-4', classOriginalScore: '-13', classWeightedScore: '-10.4', buildingName: 'A栋', roomNo: '102', remark: '', checkerName: '刘检查员' },

    // ========== 经信系 - 24级 - 计算机2班 ==========
    // 班级原总扣分 = -7 (教室卫生-3 + 考勤-4), 班级加权总扣分 = -5.6
    //
    // 教室卫生检查（固定/区间扣分，关联教室）- 检查类别原总扣分 = -3, 检查类别加权总扣分 = -2.4
    { index: '7', studentNo: '', studentName: '', gender: '', className: '计算机2班', headTeacher: '王老师', gradeName: '24级', departmentName: '经信系', categoryName: '教室卫生', checkRound: '1', relationType: '教室', deductionItemName: '地面有垃圾', deductMode: '区间扣分', personCount: '1', deductScore: '-2', originalTotalScore: '-2', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-1.6', categoryOriginalScore: '-3', categoryWeightedScore: '-2.4', classOriginalScore: '-7', classWeightedScore: '-5.6', buildingName: '1号教学楼', roomNo: '301', remark: '', checkerName: '陈检查员' },
    { index: '8', studentNo: '', studentName: '', gender: '', className: '计算机2班', headTeacher: '王老师', gradeName: '24级', departmentName: '经信系', categoryName: '教室卫生', checkRound: '1', relationType: '教室', deductionItemName: '桌椅未摆放', deductMode: '固定扣分', personCount: '1', deductScore: '-1', originalTotalScore: '-1', weightedDeductScore: '-0.8', totalWeightedDeductScore: '-0.8', categoryOriginalScore: '-3', categoryWeightedScore: '-2.4', classOriginalScore: '-7', classWeightedScore: '-5.6', buildingName: '1号教学楼', roomNo: '301', remark: '', checkerName: '陈检查员' },
    //
    // 考勤检查（按人数扣分）- 检查类别原总扣分 = -4, 检查类别加权总扣分 = -3.2
    // 第1轮检查 - 迟到：2名学生，personCount = 2
    { index: '9', studentNo: '2024001', studentName: '周七', gender: '男', className: '计算机2班', headTeacher: '王老师', gradeName: '24级', departmentName: '经信系', categoryName: '考勤', checkRound: '1', relationType: '无', deductionItemName: '迟到', deductMode: '按人数扣分', personCount: '2', deductScore: '-2', originalTotalScore: '-4', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-3.2', categoryOriginalScore: '-4', categoryWeightedScore: '-3.2', classOriginalScore: '-7', classWeightedScore: '-5.6', buildingName: '', roomNo: '', remark: '', checkerName: '王检查员' },
    { index: '10', studentNo: '2024002', studentName: '吴八', gender: '男', className: '计算机2班', headTeacher: '王老师', gradeName: '24级', departmentName: '经信系', categoryName: '考勤', checkRound: '1', relationType: '无', deductionItemName: '迟到', deductMode: '按人数扣分', personCount: '2', deductScore: '-2', originalTotalScore: '-4', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-3.2', categoryOriginalScore: '-4', categoryWeightedScore: '-3.2', classOriginalScore: '-7', classWeightedScore: '-5.6', buildingName: '', roomNo: '', remark: '', checkerName: '王检查员' },

    // ========== 文传系 - 23级 - 新闻1班 ==========
    // 班级原总扣分 = -15 (考勤-5 + 宿舍检查-10), 班级加权总扣分 = -12
    //
    // 考勤检查（按人数扣分）- 检查类别原总扣分 = -5, 检查类别加权总扣分 = -4
    // 第1轮检查 - 迟到：2名学生，personCount = 2
    { index: '11', studentNo: '2023101', studentName: '郑九', gender: '男', className: '新闻1班', headTeacher: '陈老师', gradeName: '23级', departmentName: '文传系', categoryName: '考勤', checkRound: '1', relationType: '无', deductionItemName: '迟到', deductMode: '按人数扣分', personCount: '2', deductScore: '-2', originalTotalScore: '-4', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-3.2', categoryOriginalScore: '-5', categoryWeightedScore: '-4', classOriginalScore: '-15', classWeightedScore: '-12', buildingName: '', roomNo: '', remark: '', checkerName: '王检查员' },
    { index: '12', studentNo: '2023102', studentName: '冯十', gender: '女', className: '新闻1班', headTeacher: '陈老师', gradeName: '23级', departmentName: '文传系', categoryName: '考勤', checkRound: '1', relationType: '无', deductionItemName: '迟到', deductMode: '按人数扣分', personCount: '2', deductScore: '-2', originalTotalScore: '-4', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-3.2', categoryOriginalScore: '-5', categoryWeightedScore: '-4', classOriginalScore: '-15', classWeightedScore: '-12', buildingName: '', roomNo: '', remark: '', checkerName: '王检查员' },
    // 第1轮检查 - 早退：1名学生，personCount = 1
    { index: '13', studentNo: '2023103', studentName: '陈十一', gender: '男', className: '新闻1班', headTeacher: '陈老师', gradeName: '23级', departmentName: '文传系', categoryName: '考勤', checkRound: '1', relationType: '无', deductionItemName: '早退', deductMode: '按人数扣分', personCount: '1', deductScore: '-1', originalTotalScore: '-1', weightedDeductScore: '-0.8', totalWeightedDeductScore: '-0.8', categoryOriginalScore: '-5', categoryWeightedScore: '-4', classOriginalScore: '-15', classWeightedScore: '-12', buildingName: '', roomNo: '', remark: '', checkerName: '王检查员' },
    //
    // ========== 宿舍检查（检查类别关联宿舍，包含固定扣分和按人数扣分两种扣分项）==========
    // 检查类别原总扣分 = -10, 检查类别加权总扣分 = -8
    // 场景说明：3个宿舍，扣分项有"卫生不合格"(固定扣分)和"晚归"(按人数扣分)
    // 第1轮检查 - 宿舍201: 扣卫生 → 1条记录
    { index: '14', studentNo: '', studentName: '', gender: '', className: '新闻1班', headTeacher: '陈老师', gradeName: '23级', departmentName: '文传系', categoryName: '宿舍检查', checkRound: '1', relationType: '宿舍', deductionItemName: '卫生不合格', deductMode: '固定扣分', personCount: '1', deductScore: '-2', originalTotalScore: '-2', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-1.6', categoryOriginalScore: '-10', categoryWeightedScore: '-8', classOriginalScore: '-15', classWeightedScore: '-12', buildingName: 'B栋', roomNo: '201', remark: '', checkerName: '刘检查员' },
    // 第1轮检查 - 宿舍202: 扣卫生 + 扣晚归(2学生) → 3条记录
    { index: '15', studentNo: '', studentName: '', gender: '', className: '新闻1班', headTeacher: '陈老师', gradeName: '23级', departmentName: '文传系', categoryName: '宿舍检查', checkRound: '1', relationType: '宿舍', deductionItemName: '卫生不合格', deductMode: '固定扣分', personCount: '1', deductScore: '-2', originalTotalScore: '-2', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-1.6', categoryOriginalScore: '-10', categoryWeightedScore: '-8', classOriginalScore: '-15', classWeightedScore: '-12', buildingName: 'B栋', roomNo: '202', remark: '', checkerName: '刘检查员' },
    // 第1轮检查 - 晚归：2名学生，personCount = 2
    { index: '16', studentNo: '2023104', studentName: '褚十二', gender: '男', className: '新闻1班', headTeacher: '陈老师', gradeName: '23级', departmentName: '文传系', categoryName: '宿舍检查', checkRound: '1', relationType: '宿舍', deductionItemName: '晚归', deductMode: '按人数扣分', personCount: '2', deductScore: '-2', originalTotalScore: '-4', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-3.2', categoryOriginalScore: '-10', categoryWeightedScore: '-8', classOriginalScore: '-15', classWeightedScore: '-12', buildingName: 'B栋', roomNo: '202', remark: '', checkerName: '刘检查员' },
    { index: '17', studentNo: '2023105', studentName: '卫十三', gender: '男', className: '新闻1班', headTeacher: '陈老师', gradeName: '23级', departmentName: '文传系', categoryName: '宿舍检查', checkRound: '1', relationType: '宿舍', deductionItemName: '晚归', deductMode: '按人数扣分', personCount: '2', deductScore: '-2', originalTotalScore: '-4', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-3.2', categoryOriginalScore: '-10', categoryWeightedScore: '-8', classOriginalScore: '-15', classWeightedScore: '-12', buildingName: 'B栋', roomNo: '202', remark: '', checkerName: '刘检查员' },
    // 第1轮检查 - 宿舍203: 扣晚归(1学生) → 1条记录
    { index: '18', studentNo: '2023106', studentName: '蒋十四', gender: '女', className: '新闻1班', headTeacher: '陈老师', gradeName: '23级', departmentName: '文传系', categoryName: '宿舍检查', checkRound: '1', relationType: '宿舍', deductionItemName: '晚归', deductMode: '按人数扣分', personCount: '1', deductScore: '-2', originalTotalScore: '-2', weightedDeductScore: '-1.6', totalWeightedDeductScore: '-1.6', categoryOriginalScore: '-10', categoryWeightedScore: '-8', classOriginalScore: '-15', classWeightedScore: '-12', buildingName: 'B栋', roomNo: '203', remark: '', checkerName: '刘检查员' }
    // 总计：宿舍检查生成5条记录（1 + 3 + 1 = 5）
  ]

  let html = `<table style="width:100%;border-collapse:collapse;font-size:${fontSize};line-height:${lineHeight};">`

  // 获取分组列（按 groupOrder 排序）
  const groupColumns = columns
    .filter((col: any) => col.mergeType === 'group')
    .sort((a: any, b: any) => (a.groupOrder || 999) - (b.groupOrder || 999))
  const groupFields = groupColumns.map((c: any) => c.field)

  // 获取数据合并列
  const concatColumns = columns.filter((col: any) => col.mergeType === 'concat')
  const hasMerge = groupFields.length > 0 || concatColumns.length > 0

  // 表头（使用别名 alias，如果没有则使用默认 label）
  html += '<thead><tr>'
  columns.forEach((col: any) => {
    const headerLabel = col.alias || col.label
    html += `<th style="border:${border};padding:${padding};background:${headerBg};color:${headerText};font-weight:${headerFontWeight};font-size:${headerFontSize};text-align:center;vertical-align:${verticalAlign};">${headerLabel}</th>`
  })
  html += '</tr></thead><tbody>'

  if (hasMerge && (groupFields.length > 0 || concatColumns.length > 0)) {
    // ========== 有分组列或数据合并列时的合并模式 ==========
    // 收集所有需要排序的字段（分组字段 + 数据合并的groupByField）
    const sortFields = [...groupFields]
    concatColumns.forEach((col: any) => {
      const groupByField = col.groupByField || groupFields[groupFields.length - 1]
      if (groupByField && !sortFields.includes(groupByField)) {
        sortFields.push(groupByField)
      }
    })

    // 先按排序字段对数据进行排序，确保相同值的行是连续的
    sampleData.sort((a: any, b: any) => {
      for (const field of sortFields) {
        const v1 = (a[field] || '').toString()
        const v2 = (b[field] || '').toString()
        if (v1 < v2) return -1
        if (v1 > v2) return 1
      }
      return 0
    })

    // 层级分组：后面的分组基于前面分组的范围
    const cellInfo: Record<string, { rowspan: number; concatValue?: string }>[] = sampleData.map(() => ({}))

    // 检查两行在指定层级及之前的所有分组列值是否相同
    const isSameGroup = (row1: number, row2: number, upToLevel: number): boolean => {
      for (let lvl = 0; lvl <= upToLevel; lvl++) {
        const field = groupFields[lvl]
        const v1 = (sampleData[row1] as any)[field] || ''
        const v2 = (sampleData[row2] as any)[field] || ''
        if (v1 !== v2) return false
      }
      return true
    }

    // 对每个分组列计算rowspan（层级嵌套）
    groupFields.forEach((field, levelIndex) => {
      let i = 0
      while (i < sampleData.length) {
        let span = 1
        let j = i + 1
        while (j < sampleData.length) {
          // 检查当前分组列的值相同，且前面所有层级的值也相同
          if (isSameGroup(i, j, levelIndex)) {
            cellInfo[j][field] = { rowspan: 0 }
            span++
            j++
          } else {
            break
          }
        }
        cellInfo[i][field] = { rowspan: span }
        i = j
      }
    })

    concatColumns.forEach((col: any) => {
      const groupByField = col.groupByField || groupFields[groupFields.length - 1]
      // 找到 groupByField 在 groupFields 中的位置，以确定需要检查哪些上层分组
      const groupByIndex = groupFields.indexOf(groupByField)

      // 检查两行是否在同一个合并组内（需要检查 groupByField 及其所有上层分组字段）
      const isSameConcatGroup = (row1: number, row2: number): boolean => {
        // 如果 groupByField 不在 groupFields 中，只检查 groupByField 本身
        if (groupByIndex === -1) {
          const v1 = (sampleData[row1] as any)[groupByField] || ''
          const v2 = (sampleData[row2] as any)[groupByField] || ''
          return v1 === v2
        }
        // 检查 groupByField 及其所有上层分组字段是否相同
        for (let lvl = 0; lvl <= groupByIndex; lvl++) {
          const field = groupFields[lvl]
          const v1 = (sampleData[row1] as any)[field] || ''
          const v2 = (sampleData[row2] as any)[field] || ''
          if (v1 !== v2) return false
        }
        return true
      }

      let i = 0
      while (i < sampleData.length) {
        const values: string[] = [(sampleData[i] as any)[col.field] || '']
        let span = 1
        let j = i + 1
        while (j < sampleData.length) {
          // 检查是否在同一个合并组内（包括上层分组）
          if (isSameConcatGroup(i, j)) {
            values.push((sampleData[j] as any)[col.field] || '')
            cellInfo[j][col.field] = { rowspan: 0 }
            span++
            j++
          } else {
            break
          }
        }
        // 拼接值（去重）
        const separator = col.separator === '\\n' ? '<br/>' : (col.separator || '、')
        const uniqueValues = [...new Set(values.filter(v => v))]
        cellInfo[i][col.field] = { rowspan: span, concatValue: uniqueValues.join(separator) || '-' }
        i = j
      }
    })

    // 渲染每行数据
    sampleData.forEach((row, rowIdx) => {
      const rowBg = zebra && rowIdx % 2 === 1 ? zebraColor : '#fff'
      html += `<tr style="background:${rowBg};">`

      columns.forEach((col: any) => {
        const info = cellInfo[rowIdx][col.field]
        if (col.mergeType === 'group') {
          // 分组合并列
          if (info && info.rowspan > 0) {
            html += `<td rowspan="${info.rowspan}" style="border:${border};padding:${padding};text-align:${textAlign};vertical-align:${verticalAlign};background:#fafafa;font-weight:500;">${(row as any)[col.field] || '-'}</td>`
          }
        } else if (col.mergeType === 'concat') {
          // 数据合并列
          if (info && info.rowspan > 0) {
            html += `<td rowspan="${info.rowspan}" style="border:${border};padding:${padding};text-align:${textAlign};vertical-align:${verticalAlign};">${info.concatValue}</td>`
          }
        } else {
          // 普通列或组合字段 - 每行都正常显示，不跟随concat列合并
          let value = '-'
          if (col.isComposite && col.compositeFields) {
            value = col.compositeFields.map((f: string) => (row as any)[f] || '').filter(Boolean).join(col.compositeSeparator || ' ') || '-'
          } else {
            // personCount 直接使用后端返回的值（已包含同一次扣分的总人数）
            value = (row as any)[col.field] || '-'
          }

          // 普通列和组合字段每行都独立显示，不进行合并
          html += `<td style="border:${border};padding:${padding};text-align:${textAlign};vertical-align:${verticalAlign};">${value}</td>`
        }
      })

      html += '</tr>'
    })
  } else {
    // ========== 简单模式：无分组列 ==========
    sampleData.forEach((row, idx) => {
      const rowBg = zebra && idx % 2 === 1 ? zebraColor : '#fff'
      html += `<tr style="background:${rowBg};">`
      columns.forEach((col: any) => {
        let value = '-'; if (col.isComposite && col.compositeFields) { value = col.compositeFields.map((f: string) => (row as any)[f] || '').filter(Boolean).join(col.compositeSeparator || ' ') || '-' } else { value = (row as any)[col.field] || '-' }
        html += `<td style="border:${border};padding:${padding};text-align:${textAlign};vertical-align:${verticalAlign};">${value}</td>`
      })
      html += '</tr>'
    })
  }

  html += '</tbody></table>'
  // XSS防护：对生成的HTML进行消毒
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['table', 'thead', 'tbody', 'tr', 'th', 'td', 'div', 'span', 'br', 'style'],
    ALLOWED_ATTR: ['style', 'rowspan', 'colspan'],
    ALLOW_DATA_ATTR: false
  })
})

// 插入变量
const insertVariable = (variable: string) => {
  const editor = editorRef.value
  if (editor) {
    const html = `<span style="color: #3b82f6; background: #eff6ff; padding: 2px 6px; border-radius: 4px; font-family: monospace;">${variable}</span>&nbsp;`
    editor.insertContent(html)
  }
}

// 下一步
const nextStep = () => {
  if (currentStep.value === 0 && !formData.value.templateName.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  currentStep.value++
}

// 关闭
const handleClose = () => {
  visible.value = false
}

// 提交
const handleSubmit = async () => {
  if (!formData.value.templateName.trim()) {
    ElMessage.warning('请输入模板名称')
    currentStep.value = 0
    return
  }

  loading.value = true
  try {
    const request: ExportTemplateRequest = {
      templateName: formData.value.templateName,
      description: formData.value.description,
      outputFormat: formData.value.outputFormat,
      filterConfig: formData.value.filterConfig,
      // 多表格配置
      tables: formData.value.tables,
      // 向后兼容：tableConfig 使用第一个表格配置
      tableConfig: formData.value.tables[0],
      documentTemplate: formData.value.documentTemplate,
      status: 1
    }

    if (isEdit.value && props.template?.id) {
      await updateExportTemplate(props.template.id, request)
      ElMessage.success('模板更新成功')
    } else {
      await createExportTemplate(props.planId, request)
      ElMessage.success('模板创建成功')
    }
    emit('success')
    handleClose()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.template-editor-dialog {
  :deep(.el-dialog) {
    max-width: 1400px;
    display: flex;
    flex-direction: column;
    max-height: 94vh;
  }

  :deep(.el-dialog__body) {
    padding: 0;
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  :deep(.el-dialog__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #e5e7eb;
    margin: 0;
  }

  :deep(.el-dialog__footer) {
    padding: 12px 20px;
    border-top: 1px solid #e5e7eb;
  }
}

// 修复 TinyMCE 下拉菜单和对话框被 Dialog 遮挡 - 已注释，TinyMCE 已移除
// :global(.tox-tinymce-aux) { z-index: 10000 !important; }
// :global(.tox-menu) { z-index: 10001 !important; }
// :global(.tox-dialog-wrap) { z-index: 10002 !important; }
// :global(.tox-dialog-wrap__backdrop) {
//   z-index: 10001 !important;
//   background-color: rgba(0, 0, 0, 0.3) !important;
// }
// :global(.tox-dialog) {
//   z-index: 10003 !important;
// }
// :global(.tox-dialog__header) {
//   z-index: 10004 !important;
// }
// :global(.tox-dialog__body) {
//   z-index: 10004 !important;
// }
// :global(.tox-dialog__footer) {
//   z-index: 10004 !important;
// }

.steps-nav {
  display: flex;
  padding: 16px 24px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  transition: all 0.2s;

  &:not(:last-child)::after {
    content: '';
    width: 40px;
    height: 1px;
    background: #e5e7eb;
    margin-left: 16px;
  }

  .step-number {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: #e5e7eb;
    color: #6b7280;
    font-size: 13px;
    font-weight: 600;
  }

  .step-label {
    font-size: 14px;
    color: #6b7280;
  }

  &.active {
    .step-number { background: #3b82f6; color: #fff; }
    .step-label { color: #1f2937; font-weight: 500; }
  }

  &.completed {
    .step-number { background: #10b981; color: #fff; }
    .step-label { color: #6b7280; }
  }
}

.step-content {
  flex: 1;
  overflow: auto;
}

.step-panel {
  height: 100%;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateX(10px); }
  to { opacity: 1; transform: translateX(0); }
}

// 步骤1样式
.step-panel:not(.table-config-panel):not(.editor-panel) {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-row {
  display: flex;
  gap: 20px;
}

.flex-1 { flex: 1; }

.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;

  &.required::after {
    content: '*';
    color: #ef4444;
    margin-left: 4px;
  }
}

.format-options {
  display: flex;
  gap: 12px;
}

.format-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;

  input { display: none; }

  &:hover { border-color: #93c5fd; }

  &.selected {
    border-color: #3b82f6;
    background: #eff6ff;
  }

  .format-name { font-weight: 600; color: #1f2937; }
  .format-desc { font-size: 12px; color: #6b7280; }
}

// 步骤2：表格配置
.table-config-panel {
  display: flex;
  flex-direction: column;
}

// 多表格切换标签
.table-tabs-wrapper {
  padding: 8px 12px;
  background: #f5f5f5;
  border-bottom: 1px solid #e5e7eb;
}

.table-tabs {
  display: flex;
  gap: 4px;
  align-items: center;
}

.table-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #409eff;
    color: #409eff;
  }

  &.active {
    background: #409eff;
    border-color: #409eff;
    color: #fff;

    .tab-close:hover {
      background: rgba(255, 255, 255, 0.2);
    }
  }

  &.add-tab {
    padding: 6px 10px;
    background: transparent;
    border-style: dashed;

    &:hover {
      background: #fff;
    }
  }

  .tab-label {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .tab-close {
    padding: 2px;
    border-radius: 2px;
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: rgba(0, 0, 0, 0.1);
    }
  }
}

// 表格标题配置卡片
.table-title-card {
  padding: 10px 12px !important;
  margin-bottom: 8px !important;

  .title-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .title-input {
    flex: 1;
    max-width: 200px;
  }
}

.table-config-layout {
  display: flex;
  flex: 1;
  min-height: 500px;
  overflow: hidden;
}

.config-section {
  width: 360px;
  flex-shrink: 0;
  padding: 12px;
  overflow-y: auto;
  border-right: 1px solid #e5e7eb;
  background: #fafafa;
}

.preview-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  font-weight: 600;
  color: #374151;
}

.preview-container {
  flex: 1;
  padding: 20px;
  overflow: auto;
  background: #fff;
}

.table-preview {
  min-height: 200px;

  table {
    margin: 0 auto;
  }
}

.preview-note {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #fffbeb;
  color: #92400e;
  font-size: 12px;
  border-top: 1px solid #fde68a;
}

// 配置卡片（紧凑版）
.config-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 10px;
}

.config-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  font-weight: 600;
  font-size: 13px;
  color: #374151;

  .header-hint {
    font-weight: 400;
    font-size: 11px;
    color: #9ca3af;
    margin-left: auto;
  }
}

// 列配置列表
.column-config-list {
  padding: 8px;
}

.column-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

// 列配置项包装器
.column-item-wrapper {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 6px;
  transition: all 0.15s;

  &:hover {
    border-color: #d1d5db;
    box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  }
}

// 紧凑版列配置项（左右布局）
.column-item-compact {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;

  .drag-handle {
    cursor: grab;
    color: #9ca3af;
    flex-shrink: 0;
    &:hover { color: #6b7280; }
  }

  .column-index {
    width: 18px;
    height: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #e5e7eb;
    border-radius: 4px;
    font-size: 10px;
    font-weight: 600;
    color: #6b7280;
    flex-shrink: 0;
  }

  .column-name {
    flex: 1;
    font-size: 13px;
    font-weight: 500;
    color: #374151;
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 4px;

    .group-order-badge {
      font-size: 10px;
      color: #fff;
      background: #3b82f6;
      padding: 1px 5px;
      border-radius: 8px;
      font-weight: 600;
      flex-shrink: 0;
    }
  }

  .merge-tabs {
    display: flex;
    background: #f3f4f6;
    border-radius: 4px;
    padding: 2px;
    flex-shrink: 0;
  }

  .merge-tab {
    padding: 3px 8px;
    font-size: 11px;
    border: none;
    background: transparent;
    color: #6b7280;
    cursor: pointer;
    border-radius: 3px;
    transition: all 0.15s;
    white-space: nowrap;

    &:hover:not(.disabled):not(.active) {
      color: #374151;
      background: rgba(255,255,255,0.5);
    }

    &.active {
      background: #fff;
      color: #3b82f6;
      font-weight: 500;
      box-shadow: 0 1px 2px rgba(0,0,0,0.1);
    }

    &.disabled {
      color: #d1d5db;
      cursor: not-allowed;
    }
  }

  .remove-btn {
    color: #9ca3af;
    cursor: pointer;
    flex-shrink: 0;
    &:hover { color: #ef4444; }
  }
}

// 数据合并配置行
.alias-config-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px 6px 36px;
  background: #fafafa;
  border-top: 1px dashed #e5e7eb;
  font-size: 12px;

  .alias-config-label {
    color: #6b7280;
    white-space: nowrap;
    font-size: 12px;
  }

  .alias-input {
    flex: 1;
    max-width: 200px;
    :deep(.el-input__inner) {
      font-size: 12px;
      height: 26px;
    }
  }
}

.concat-config-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px 8px 36px;
  background: #f8fafc;
  border-top: 1px dashed #e5e7eb;
  font-size: 12px;

  .concat-config-label {
    color: #6b7280;
    white-space: nowrap;
  }

  .concat-groupby-select {
    width: 80px;
    :deep(.el-input__inner) {
      font-size: 12px;
    }
  }

  .concat-separator-select {
    width: 90px;
    :deep(.el-input__inner) {
      font-size: 12px;
    }
  }
}

// 合并提示信息
.merge-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 11px;
  border-radius: 0 0 6px 6px;
  margin-top: -1px;

  &.warning {
    background: #fef3c7;
    color: #92400e;
  }

  &.info {
    background: #f0f9ff;
    color: #0369a1;
  }
}

.column-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  transition: all 0.15s;

  &:hover {
    border-color: #d1d5db;
  }

  &.is-hierarchy {
    background: #eff6ff;
    border-color: #93c5fd;
  }

  .drag-handle {
    cursor: grab;
    color: #9ca3af;
    flex-shrink: 0;
    &:hover { color: #6b7280; }
  }

  .column-index {
    width: 18px;
    height: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f3f4f6;
    border-radius: 50%;
    font-size: 11px;
    font-weight: 500;
    color: #6b7280;
    flex-shrink: 0;
  }

  .column-name {
    flex: 1;
    font-size: 13px;
    color: #374151;
  }

  .field-type-tag {
    font-size: 10px;
    padding: 1px 4px;
    border-radius: 3px;
    flex-shrink: 0;

    &.data {
      background: #fef3c7;
      color: #92400e;
    }
  }

  .hierarchy-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    width: 28px;
    height: 24px;
    border: 1px solid #e5e7eb;
    border-radius: 4px;
    background: #fff;
    color: #9ca3af;
    cursor: pointer;
    transition: all 0.15s;
    flex-shrink: 0;

    &:hover {
      border-color: #3b82f6;
      color: #3b82f6;
    }

    &.active {
      background: #3b82f6;
      border-color: #3b82f6;
      color: #fff;
    }

    .hierarchy-order {
      position: absolute;
      top: -6px;
      right: -6px;
      width: 14px;
      height: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #ef4444;
      color: #fff;
      border-radius: 50%;
      font-size: 10px;
      font-weight: 600;
    }
  }

  .remove-btn {
    color: #9ca3af;
    cursor: pointer;
    flex-shrink: 0;
    &:hover { color: #ef4444; }
  }
}

// 分组层级顺序
.hierarchy-order-section {
  padding: 8px;
  border-top: 1px solid #e5e7eb;
}

.hierarchy-order-header {
  font-size: 11px;
  color: #6b7280;
  margin-bottom: 6px;
}

.hierarchy-order-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.hierarchy-order-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: #dbeafe;
  border: 1px solid #93c5fd;
  border-radius: 4px;
  font-size: 12px;
  color: #1e40af;
  cursor: grab;

  .order-badge {
    width: 16px;
    height: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #3b82f6;
    color: #fff;
    border-radius: 50%;
    font-size: 10px;
    font-weight: 600;
  }
}

// 合并模式设置
.merge-mode-section {
  padding: 10px;
  border-top: 1px solid #e5e7eb;
}

.merge-info-box {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 6px;
  margin-bottom: 10px;

  .info-icon {
    flex-shrink: 0;
    font-size: 14px;
  }

  .info-text {
    font-size: 12px;
    color: #0369a1;
    line-height: 1.5;

    strong {
      color: #0c4a6e;
    }
  }
}

.concat-option {
  padding: 8px 0;

  :deep(.el-checkbox) {
    font-weight: 500;
  }

  .option-hint {
    font-size: 11px;
    color: #9ca3af;
    margin-top: 4px;
    margin-left: 24px;
  }
}

.merge-mode-header {
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 8px;
}

.merge-mode-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.merge-mode-option {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;

  input { display: none; }

  &:hover {
    border-color: #93c5fd;
  }

  &.active {
    border-color: #3b82f6;
    background: #eff6ff;
  }

  .option-content {
    flex: 1;
  }

  .option-title {
    font-size: 13px;
    font-weight: 500;
    color: #374151;
  }

  .option-desc {
    font-size: 11px;
    color: #9ca3af;
    margin-top: 2px;
  }
}

.separator-setting {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e5e7eb;
  display: flex;
  align-items: center;
  gap: 8px;

  .setting-label {
    font-size: 12px;
    color: #6b7280;
    white-space: nowrap;
  }
}

// 字段选择器（紧凑版）
.field-selector {
  padding: 10px 12px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.field-checkbox {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;

  input { display: none; }

  &:hover { border-color: #93c5fd; }

  &.checked {
    border-color: #3b82f6;
    background: #eff6ff;

    .field-name { color: #1e40af; }
  }

  .field-name {
    font-size: 12px;
    color: #374151;
  }
}


// 样式折叠面板（紧凑版）
.style-collapse {
  margin-top: 0;

  :deep(.el-collapse-item__header) {
    padding: 0 12px;
    height: 36px;
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    font-size: 13px;
    font-weight: 500;
  }

  :deep(.el-collapse-item__wrap) {
    border: 1px solid #e5e7eb;
    border-top: none;
    border-radius: 0 0 6px 6px;
  }

  :deep(.el-collapse-item__content) {
    padding: 10px 12px;
  }
}

.style-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px 12px;
}

.style-item {
  display: flex;
  flex-direction: column;
  gap: 4px;

  > label {
    font-size: 11px;
    color: #6b7280;
  }

  &.full-width {
    grid-column: 1 / -1;
  }
}

.checkbox-inline {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;

  input[type="checkbox"] {
    width: 16px;
    height: 16px;
  }

  span {
    font-size: 13px;
    color: #374151;
  }
}

// 步骤3：编辑器布局
.editor-panel {
  display: flex;
  flex-direction: column;
}

.editor-layout {
  display: flex;
  height: 100%;
  min-height: 500px;
}

.tools-panel {
  width: 240px;
  flex-shrink: 0;
  border-right: 1px solid #e5e7eb;
  background: #fafafa;
  overflow-y: auto;
}

.tools-section {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;

  &:last-child { border-bottom: none; }
}

.tools-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.variable-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.variable-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #3b82f6;
    background: #eff6ff;
  }

  .var-code {
    font-family: monospace;
    font-size: 12px;
    color: #3b82f6;
  }

  .var-label {
    font-size: 12px;
    color: #6b7280;
  }
}

.table-insert-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.insert-table-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 14px;
  border: 2px dashed #3b82f6;
  border-radius: 8px;
  background: #eff6ff;
  color: #3b82f6;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #dbeafe;
  }

  &.insert-all {
    padding: 10px;
    font-size: 13px;
  }

  &.insert-single {
    padding: 8px 12px;
    font-size: 12px;
    font-weight: 500;
    border-width: 1px;
    border-style: solid;
    background: #f8fafc;
    justify-content: flex-start;

    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &:hover {
      background: #eff6ff;
    }
  }
}

.tools-hint {
  margin-top: 8px;
  font-size: 11px;
  color: #9ca3af;
  text-align: center;
}

.tips-list {
  margin: 0;
  padding: 0 0 0 16px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.8;

  code {
    background: #f3f4f6;
    padding: 1px 4px;
    border-radius: 3px;
    font-size: 11px;
  }
}

.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.editor-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;

  // TinyMCE 样式已注释
  // :deep(.tox-tinymce) {
  //   flex: 1;
  //   border: none;
  //   border-radius: 0;
  // }

  // :deep(.tox-editor-header) {
  //   border-bottom: 1px solid #e5e7eb;
  // }

  // 替换为 textarea 的样式
  .template-textarea {
    height: 100%;
    :deep(textarea) {
      font-family: 'Courier New', monospace;
      font-size: 13px;
    }
  }
}

// 底部
.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-left,
.footer-right {
  display: flex;
  gap: 8px;
}

// 拖拽动画
.ghost {
  opacity: 0.4;
}

.flip-list-move {
  transition: transform 0.3s;
}

// 添加组合字段按钮
.add-composite-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  margin-top: 10px;
  background: #f0f9ff;
  border: 1px dashed #3b82f6;
  border-radius: 6px;
  color: #3b82f6;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #dbeafe;
    border-style: solid;
  }
}

// 组合字段表单
.composite-form {
  .form-item {
    margin-bottom: 20px;

    > label {
      display: block;
      margin-bottom: 8px;
      font-size: 14px;
      font-weight: 500;
      color: #374151;
    }
  }
}

.composite-field-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
  padding: 10px;
  background: #f9fafb;
  border-radius: 8px;
}

.composite-field-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #93c5fd;
  }

  &.selected {
    background: #eff6ff;
    border-color: #3b82f6;
    color: #1e40af;
  }

  .field-order {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    background: #3b82f6;
    color: #fff;
    border-radius: 50%;
    font-size: 11px;
    font-weight: 600;
  }
}

.separator-options {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.separator-option {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #93c5fd;
  }

  &.selected {
    background: #eff6ff;
    border-color: #3b82f6;
    color: #1e40af;
  }
}

.composite-preview {
  padding: 12px 16px;
  background: #f0fdf4;
  border: 1px solid #86efac;
  border-radius: 8px;
  font-size: 14px;
  color: #166534;
  font-weight: 500;
}
</style>
