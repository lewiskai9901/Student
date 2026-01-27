import { ElMessageBox, ElMessage } from 'element-plus'

/**
 * 确认对话框 Composable
 * 提供标准化的确认和提示操作
 */
export interface ConfirmOptions {
  title?: string
  message: string
  type?: 'warning' | 'info' | 'error' | 'success'
  confirmText?: string
  cancelText?: string
  showCancelButton?: boolean
}

export function useConfirm() {
  /**
   * 确认对话框
   */
  const confirm = async (options: ConfirmOptions): Promise<boolean> => {
    const {
      title = '确认',
      message,
      type = 'warning',
      confirmText = '确定',
      cancelText = '取消',
      showCancelButton = true
    } = options

    try {
      await ElMessageBox.confirm(message, title, {
        confirmButtonText: confirmText,
        cancelButtonText: cancelText,
        type,
        showCancelButton
      })
      return true
    } catch {
      return false
    }
  }

  /**
   * 删除确认
   */
  const confirmDelete = async (itemName?: string): Promise<boolean> => {
    return confirm({
      title: '删除确认',
      message: itemName
        ? `确定要删除 "${itemName}" 吗？此操作不可恢复。`
        : '确定要删除吗？此操作不可恢复。',
      type: 'warning'
    })
  }

  /**
   * 批量删除确认
   */
  const confirmBatchDelete = async (count: number): Promise<boolean> => {
    return confirm({
      title: '批量删除确认',
      message: `确定要删除选中的 ${count} 项吗？此操作不可恢复。`,
      type: 'warning'
    })
  }

  /**
   * 操作确认
   */
  const confirmAction = async (action: string, itemName?: string): Promise<boolean> => {
    return confirm({
      title: '操作确认',
      message: itemName
        ? `确定要${action} "${itemName}" 吗？`
        : `确定要${action}吗？`,
      type: 'warning'
    })
  }

  /**
   * 成功提示
   */
  const showSuccess = (message: string) => {
    ElMessage.success(message)
  }

  /**
   * 错误提示
   */
  const showError = (message: string) => {
    ElMessage.error(message)
  }

  /**
   * 警告提示
   */
  const showWarning = (message: string) => {
    ElMessage.warning(message)
  }

  /**
   * 信息提示
   */
  const showInfo = (message: string) => {
    ElMessage.info(message)
  }

  return {
    confirm,
    confirmDelete,
    confirmBatchDelete,
    confirmAction,
    showSuccess,
    showError,
    showWarning,
    showInfo
  }
}
