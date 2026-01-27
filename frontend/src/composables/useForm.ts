import { ref, reactive, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

/**
 * 表单 Composable
 * 提供标准化的表单状态管理
 */
export interface UseFormOptions<T extends Record<string, unknown>> {
  initialValues: T
  rules?: FormRules
}

export function useForm<T extends Record<string, unknown>>(options: UseFormOptions<T>) {
  const { initialValues, rules = {} } = options

  // 表单引用
  const formRef = ref<FormInstance>()

  // 表单数据
  const formData = reactive<T>({ ...initialValues }) as T

  // 表单规则
  const formRules = ref<FormRules>(rules)

  // 是否修改过
  const isDirty = ref(false)

  // 提交中
  const submitting = ref(false)

  // 标记为已修改
  const markDirty = () => {
    isDirty.value = true
  }

  // 重置表单
  const resetForm = () => {
    Object.assign(formData, initialValues)
    formRef.value?.resetFields()
    isDirty.value = false
  }

  // 设置表单值
  const setValues = (values: Partial<T>) => {
    Object.assign(formData, values)
  }

  // 获取表单值
  const getValues = (): T => {
    return { ...formData } as T
  }

  // 验证表单
  const validate = async (): Promise<boolean> => {
    if (!formRef.value) return true
    try {
      await formRef.value.validate()
      return true
    } catch {
      return false
    }
  }

  // 验证指定字段
  const validateField = async (field: string): Promise<boolean> => {
    if (!formRef.value) return true
    try {
      await formRef.value.validateField(field)
      return true
    } catch {
      return false
    }
  }

  // 清除验证
  const clearValidate = (fields?: string | string[]) => {
    formRef.value?.clearValidate(fields)
  }

  // 提交表单
  const submit = async <R>(
    submitFn: (data: T) => Promise<R>
  ): Promise<{ success: boolean; data?: R; error?: unknown }> => {
    const valid = await validate()
    if (!valid) {
      return { success: false }
    }

    submitting.value = true
    try {
      const result = await submitFn(getValues())
      isDirty.value = false
      return { success: true, data: result }
    } catch (error) {
      return { success: false, error }
    } finally {
      submitting.value = false
    }
  }

  return {
    formRef,
    formData,
    formRules,
    isDirty,
    submitting,
    markDirty,
    resetForm,
    setValues,
    getValues,
    validate,
    validateField,
    clearValidate,
    submit
  }
}

/**
 * 编辑表单 Composable
 * 用于新增/编辑场景
 */
export interface UseEditFormOptions<T extends Record<string, unknown>> extends UseFormOptions<T> {
  createFn: (data: T) => Promise<unknown>
  updateFn: (id: number, data: T) => Promise<unknown>
}

export function useEditForm<T extends Record<string, unknown>>(options: UseEditFormOptions<T>) {
  const { createFn, updateFn, ...formOptions } = options

  const form = useForm(formOptions)

  // 当前编辑的 ID
  const editingId = ref<number | null>(null)

  // 是否编辑模式
  const isEditing = computed(() => editingId.value !== null)

  // 模式标题
  const modeTitle = computed(() => (isEditing.value ? '编辑' : '新增'))

  // 开始新增
  const startCreate = () => {
    editingId.value = null
    form.resetForm()
  }

  // 开始编辑
  const startEdit = (id: number, data: Partial<T>) => {
    editingId.value = id
    form.resetForm()
    form.setValues(data)
  }

  // 保存
  const save = async (): Promise<{ success: boolean; error?: unknown }> => {
    const valid = await form.validate()
    if (!valid) {
      return { success: false }
    }

    form.submitting.value = true
    try {
      const data = form.getValues()
      if (isEditing.value && editingId.value !== null) {
        await updateFn(editingId.value, data)
      } else {
        await createFn(data)
      }
      return { success: true }
    } catch (error) {
      return { success: false, error }
    } finally {
      form.submitting.value = false
    }
  }

  return {
    ...form,
    editingId,
    isEditing,
    modeTitle,
    startCreate,
    startEdit,
    save
  }
}
