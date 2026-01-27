import { ref, computed } from 'vue'

/**
 * 对话框 Composable
 * 提供标准化的对话框状态管理
 */
export interface UseDialogOptions {
  onOpen?: () => void
  onClose?: () => void
}

export function useDialog(options: UseDialogOptions = {}) {
  const { onOpen, onClose } = options

  const visible = ref(false)
  const loading = ref(false)

  // 打开对话框
  const open = () => {
    visible.value = true
    onOpen?.()
  }

  // 关闭对话框
  const close = () => {
    visible.value = false
    loading.value = false
    onClose?.()
  }

  // 切换显示状态
  const toggle = () => {
    if (visible.value) {
      close()
    } else {
      open()
    }
  }

  // 开始加载
  const startLoading = () => {
    loading.value = true
  }

  // 结束加载
  const stopLoading = () => {
    loading.value = false
  }

  // 带加载的操作
  const withLoading = async <T>(fn: () => Promise<T>): Promise<T> => {
    startLoading()
    try {
      return await fn()
    } finally {
      stopLoading()
    }
  }

  return {
    visible,
    loading,
    open,
    close,
    toggle,
    startLoading,
    stopLoading,
    withLoading
  }
}

/**
 * 编辑对话框 Composable
 * 用于新增/编辑场景的对话框
 */
export interface UseEditDialogOptions<T> extends UseDialogOptions {
  defaultData?: Partial<T>
}

export function useEditDialog<T extends { id?: number }>(
  options: UseEditDialogOptions<T> = {}
) {
  const { defaultData = {}, ...dialogOptions } = options

  const dialog = useDialog(dialogOptions)

  // 当前编辑的数据
  const editData = ref<Partial<T>>({ ...defaultData })

  // 当前编辑的 ID
  const editingId = computed(() => editData.value.id)

  // 是否编辑模式
  const isEditing = computed(() => editingId.value !== undefined && editingId.value !== null)

  // 对话框标题
  const title = computed(() => (isEditing.value ? '编辑' : '新增'))

  // 打开新增对话框
  const openCreate = (initialData?: Partial<T>) => {
    editData.value = { ...defaultData, ...initialData }
    dialog.open()
  }

  // 打开编辑对话框
  const openEdit = (data: T) => {
    editData.value = { ...data }
    dialog.open()
  }

  // 关闭并重置
  const closeAndReset = () => {
    dialog.close()
    editData.value = { ...defaultData }
  }

  return {
    ...dialog,
    editData,
    editingId,
    isEditing,
    title,
    openCreate,
    openEdit,
    closeAndReset
  }
}

/**
 * 详情对话框 Composable
 * 用于查看详情场景的对话框
 */
export function useDetailDialog<T>() {
  const dialog = useDialog()

  // 详情数据
  const detailData = ref<T | null>(null)

  // 是否有数据
  const hasData = computed(() => detailData.value !== null)

  // 打开详情对话框
  const openDetail = (data: T) => {
    detailData.value = data
    dialog.open()
  }

  // 关闭并清除
  const closeAndClear = () => {
    dialog.close()
    detailData.value = null
  }

  return {
    ...dialog,
    detailData,
    hasData,
    openDetail,
    closeAndClear
  }
}
