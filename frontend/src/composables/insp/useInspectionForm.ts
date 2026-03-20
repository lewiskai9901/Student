/**
 * V7 检查平台 - 动态表单渲染 + 校验 Composable
 *
 * 职责：
 * 1. 根据 submission 加载表单结构 (sections + details)
 * 2. 字段值更新 + 脏标记
 * 3. 条件逻辑计算（字段可见性）
 * 4. 校验规则执行
 * 5. 自动保存（30s 间隔）
 */
import { ref, watch, onUnmounted, type Ref } from 'vue'
import type { SubmissionDetail } from '@/types/insp/project'
import type { TemplateSection } from '@/types/insp/template'
import {
  getDetails,
  updateDetailResponse,
  saveFormData,
} from '@/api/insp/submission'
import { getSections } from '@/api/insp/template'

interface ConditionRule {
  field: string
  operator: 'eq' | 'neq' | 'gt' | 'gte' | 'lt' | 'lte' | 'in' | 'not_in' | 'empty' | 'not_empty'
  value?: any
  logic?: 'AND' | 'OR'
  children?: ConditionRule[]
}

interface ValidationRule {
  type: 'required' | 'min' | 'max' | 'minLength' | 'maxLength' | 'pattern' | 'custom'
  value?: any
  message: string
}

const AUTO_SAVE_INTERVAL = 30_000 // 30 seconds

export function useInspectionForm(submissionId: Ref<number | null>) {
  // ==================== State ====================

  const formData = ref<Record<string, any>>({})
  const details = ref<SubmissionDetail[]>([])
  const sections = ref<TemplateSection[]>([])
  const visibilityMap = ref<Record<string, boolean>>({})
  const validationErrors = ref<Record<string, string>>({})
  const isDirty = ref(false)
  const isAutoSaving = ref(false)
  const isLoading = ref(false)

  let autoSaveTimer: ReturnType<typeof setInterval> | null = null

  // ==================== Load Form ====================

  /** Load form structure and detail data for a submission */
  async function loadForm(id: number) {
    isLoading.value = true
    try {
      // Load submission details (the filled-in data)
      const detailList = await getDetails(id)
      details.value = detailList

      // Build formData from existing response values
      const data: Record<string, any> = {}
      for (const detail of detailList) {
        data[detail.itemCode] = detail.responseValue
          ? tryParseJSON(detail.responseValue)
          : null
      }
      formData.value = data

      // Extract unique sectionIds and load sections for context
      const sectionIds = new Set<number>()
      for (const detail of detailList) {
        if (detail.sectionId != null) sectionIds.add(detail.sectionId)
      }
      if (sectionIds.size > 0) {
        // Load sections from the template (the first detail's section has templateId context)
        // Sections are loaded based on the first available sectionId
        const firstDetail = detailList.find(d => d.sectionId != null)
        if (firstDetail && firstDetail.sectionId) {
          try {
            // Sections are fetched at the template level; we derive templateId from the section
            // For now, we populate sections from the detail's sectionName grouping
            const sectionMap = new Map<number, TemplateSection>()
            for (const d of detailList) {
              if (d.sectionId != null && !sectionMap.has(d.sectionId)) {
                sectionMap.set(d.sectionId, {
                  id: d.sectionId,
                  templateId: 0, // not available from detail
                  sectionCode: '',
                  sectionName: d.sectionName || '',
                  sortOrder: sectionMap.size,
                  weight: 1,
                  isRepeatable: false,
                  conditionLogic: null,
                  createdAt: '',
                  updatedAt: '',
                })
              }
            }
            sections.value = Array.from(sectionMap.values())
          } catch {
            // Section loading is best-effort; form can still function without it
            sections.value = []
          }
        }
      }

      // Evaluate initial visibility
      evaluateConditions()

      isDirty.value = false
      validationErrors.value = {}
    } finally {
      isLoading.value = false
    }
  }

  // Watch submissionId for auto-loading
  watch(submissionId, (newId) => {
    if (newId) {
      loadForm(newId)
    } else {
      formData.value = {}
      details.value = []
      sections.value = []
      visibilityMap.value = {}
      validationErrors.value = {}
      isDirty.value = false
    }
  }, { immediate: true })

  // ==================== Field Update ====================

  /** Update a field value by detail id */
  function updateField(detailId: number, value: any) {
    const detail = details.value.find(d => d.id === detailId)
    if (!detail) return

    formData.value[detail.itemCode] = value
    isDirty.value = true

    // Clear validation error for this field
    delete validationErrors.value[detail.itemCode]

    // Re-evaluate conditions since a field value changed
    evaluateConditions()
  }

  // ==================== Condition Logic ====================

  /** Evaluate condition logic for all details to determine visibility */
  function evaluateConditions() {
    const map: Record<string, boolean> = {}
    for (const detail of details.value) {
      if (!detail.conditionLogic) {
        map[detail.itemCode] = true
        continue
      }
      try {
        const rules = JSON.parse(detail.conditionLogic) as ConditionRule | ConditionRule[]
        const ruleArray = Array.isArray(rules) ? rules : [rules]
        map[detail.itemCode] = evaluateRuleGroup(ruleArray, 'AND')
      } catch {
        // If condition parsing fails, show the field by default
        map[detail.itemCode] = true
      }
    }
    visibilityMap.value = map
  }

  function evaluateRuleGroup(rules: ConditionRule[], defaultLogic: 'AND' | 'OR'): boolean {
    if (rules.length === 0) return true
    const logic = rules[0].logic || defaultLogic

    if (logic === 'AND') {
      return rules.every(r => evaluateSingleRule(r))
    } else {
      return rules.some(r => evaluateSingleRule(r))
    }
  }

  function evaluateSingleRule(rule: ConditionRule): boolean {
    // Handle nested children
    if (rule.children && rule.children.length > 0) {
      return evaluateRuleGroup(rule.children, rule.logic || 'AND')
    }

    const fieldValue = formData.value[rule.field]

    switch (rule.operator) {
      case 'eq':
        return fieldValue == rule.value // loose equality for type coercion
      case 'neq':
        return fieldValue != rule.value
      case 'gt':
        return Number(fieldValue) > Number(rule.value)
      case 'gte':
        return Number(fieldValue) >= Number(rule.value)
      case 'lt':
        return Number(fieldValue) < Number(rule.value)
      case 'lte':
        return Number(fieldValue) <= Number(rule.value)
      case 'in':
        return Array.isArray(rule.value) && rule.value.includes(fieldValue)
      case 'not_in':
        return Array.isArray(rule.value) && !rule.value.includes(fieldValue)
      case 'empty':
        return fieldValue == null || fieldValue === '' || (Array.isArray(fieldValue) && fieldValue.length === 0)
      case 'not_empty':
        return fieldValue != null && fieldValue !== '' && !(Array.isArray(fieldValue) && fieldValue.length === 0)
      default:
        return true
    }
  }

  // ==================== Validation ====================

  /** Validate all visible fields. Returns true if all valid. */
  function validate(): boolean {
    const errors: Record<string, string> = {}

    for (const detail of details.value) {
      // Skip invisible fields
      if (!visibilityMap.value[detail.itemCode]) continue

      const value = formData.value[detail.itemCode]
      const rules = parseValidationRules(detail.validationRules)

      for (const rule of rules) {
        const error = runValidationRule(rule, value)
        if (error) {
          errors[detail.itemCode] = error
          break // only first error per field
        }
      }
    }

    validationErrors.value = errors
    return Object.keys(errors).length === 0
  }

  function parseValidationRules(rulesJson: string | null): ValidationRule[] {
    if (!rulesJson) return []
    try {
      const parsed = JSON.parse(rulesJson)
      return Array.isArray(parsed) ? parsed : [parsed]
    } catch {
      return []
    }
  }

  function runValidationRule(rule: ValidationRule, value: any): string | null {
    switch (rule.type) {
      case 'required':
        if (value == null || value === '' || (Array.isArray(value) && value.length === 0)) {
          return rule.message || '此字段为必填项'
        }
        break
      case 'min':
        if (value != null && Number(value) < Number(rule.value)) {
          return rule.message || `最小值为 ${rule.value}`
        }
        break
      case 'max':
        if (value != null && Number(value) > Number(rule.value)) {
          return rule.message || `最大值为 ${rule.value}`
        }
        break
      case 'minLength':
        if (typeof value === 'string' && value.length < Number(rule.value)) {
          return rule.message || `最少输入 ${rule.value} 个字符`
        }
        break
      case 'maxLength':
        if (typeof value === 'string' && value.length > Number(rule.value)) {
          return rule.message || `最多输入 ${rule.value} 个字符`
        }
        break
      case 'pattern':
        if (typeof value === 'string' && rule.value) {
          const regex = new RegExp(rule.value)
          if (!regex.test(value)) {
            return rule.message || '格式不正确'
          }
        }
        break
      case 'custom':
        // Custom validation would need a formula evaluator; skip for now
        break
    }
    return null
  }

  // ==================== Auto-Save ====================

  /** Start auto-saving form data every 30 seconds when dirty */
  function startAutoSave() {
    stopAutoSave()
    autoSaveTimer = setInterval(async () => {
      if (!isDirty.value || !submissionId.value) return
      isAutoSaving.value = true
      try {
        const serialized = JSON.stringify(formData.value)
        await saveFormData(submissionId.value, { formData: serialized })
        isDirty.value = false
      } catch {
        // Auto-save failures are silent; user can manually save
      } finally {
        isAutoSaving.value = false
      }
    }, AUTO_SAVE_INTERVAL)
  }

  /** Stop the auto-save timer */
  function stopAutoSave() {
    if (autoSaveTimer) {
      clearInterval(autoSaveTimer)
      autoSaveTimer = null
    }
  }

  // ==================== Helpers ====================

  function tryParseJSON(str: string): any {
    try {
      return JSON.parse(str)
    } catch {
      return str
    }
  }

  // Cleanup on unmount
  onUnmounted(() => stopAutoSave())

  return {
    // State
    formData,
    details,
    sections,
    visibilityMap,
    validationErrors,
    isDirty,
    isAutoSaving,
    isLoading,
    // Actions
    loadForm,
    updateField,
    evaluateConditions,
    validate,
    startAutoSave,
    stopAutoSave,
  }
}
