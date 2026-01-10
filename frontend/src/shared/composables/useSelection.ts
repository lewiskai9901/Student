import { ref, computed } from 'vue'

/**
 * 选择 Composable
 * 提供标准化的选择状态管理
 */
export interface UseSelectionOptions<T> {
  idKey?: keyof T
  maxSelection?: number
}

export function useSelection<T extends Record<string, unknown>>(
  options: UseSelectionOptions<T> = {}
) {
  const { idKey = 'id' as keyof T, maxSelection } = options

  // 选中的行
  const selectedRows = ref<T[]>([]) as { value: T[] }

  // 选中的 ID 列表
  const selectedIds = computed(() =>
    selectedRows.value.map((row) => row[idKey]).filter(Boolean)
  )

  // 是否有选中项
  const hasSelection = computed(() => selectedRows.value.length > 0)

  // 选中数量
  const selectionCount = computed(() => selectedRows.value.length)

  // 是否达到最大选择数
  const isMaxSelection = computed(() =>
    maxSelection !== undefined && selectedRows.value.length >= maxSelection
  )

  // 是否选中指定项
  const isSelected = (row: T): boolean => {
    const id = row[idKey]
    return selectedRows.value.some((r) => r[idKey] === id)
  }

  // 选择单个项
  const select = (row: T) => {
    if (!isSelected(row)) {
      if (maxSelection !== undefined && selectedRows.value.length >= maxSelection) {
        return false
      }
      selectedRows.value.push(row)
      return true
    }
    return false
  }

  // 取消选择单个项
  const deselect = (row: T) => {
    const id = row[idKey]
    const index = selectedRows.value.findIndex((r) => r[idKey] === id)
    if (index > -1) {
      selectedRows.value.splice(index, 1)
      return true
    }
    return false
  }

  // 切换选择状态
  const toggleSelect = (row: T) => {
    if (isSelected(row)) {
      deselect(row)
    } else {
      select(row)
    }
  }

  // 全选
  const selectAll = (rows: T[]) => {
    const rowsToSelect = maxSelection !== undefined
      ? rows.slice(0, maxSelection)
      : rows
    selectedRows.value = [...rowsToSelect]
  }

  // 清除选择
  const clearSelection = () => {
    selectedRows.value = []
  }

  // 处理选择变化（用于 el-table）
  const handleSelectionChange = (rows: T[]) => {
    selectedRows.value = rows
  }

  // 设置选中项
  const setSelection = (rows: T[]) => {
    selectedRows.value = [...rows]
  }

  return {
    selectedRows,
    selectedIds,
    hasSelection,
    selectionCount,
    isMaxSelection,
    isSelected,
    select,
    deselect,
    toggleSelect,
    selectAll,
    clearSelection,
    handleSelectionChange,
    setSelection
  }
}

/**
 * 单选 Composable
 */
export function useSingleSelection<T extends Record<string, unknown>>(
  options: Omit<UseSelectionOptions<T>, 'maxSelection'> = {}
) {
  const { idKey = 'id' as keyof T } = options

  // 选中的项
  const selectedItem = ref<T | null>(null) as { value: T | null }

  // 选中的 ID
  const selectedId = computed(() =>
    selectedItem.value ? selectedItem.value[idKey] : null
  )

  // 是否有选中项
  const hasSelection = computed(() => selectedItem.value !== null)

  // 是否选中指定项
  const isSelected = (row: T): boolean => {
    if (!selectedItem.value) return false
    return selectedItem.value[idKey] === row[idKey]
  }

  // 选择
  const select = (row: T) => {
    selectedItem.value = row
  }

  // 取消选择
  const deselect = () => {
    selectedItem.value = null
  }

  // 切换选择
  const toggleSelect = (row: T) => {
    if (isSelected(row)) {
      deselect()
    } else {
      select(row)
    }
  }

  // 清除选择
  const clearSelection = () => {
    selectedItem.value = null
  }

  return {
    selectedItem,
    selectedId,
    hasSelection,
    isSelected,
    select,
    deselect,
    toggleSelect,
    clearSelection
  }
}
