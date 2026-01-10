/**
 * V2 Composables
 * 基于 DDD 架构的可复用组合式函数
 */

// 分页
export { usePagination } from './usePagination'
export type { PaginationOptions } from './usePagination'

// 加载状态
export { useLoading, useSimpleLoading } from './useLoading'

// 确认对话框
export { useConfirm } from './useConfirm'
export type { ConfirmOptions } from './useConfirm'

// 表格
export { useTable } from './useTable'
export type { UseTableOptions } from './useTable'

// 表单
export { useForm, useEditForm } from './useForm'
export type { UseFormOptions, UseEditFormOptions } from './useForm'

// 对话框
export { useDialog, useEditDialog, useDetailDialog } from './useDialog'
export type { UseDialogOptions, UseEditDialogOptions } from './useDialog'

// 选择
export { useSelection, useSingleSelection } from './useSelection'
export type { UseSelectionOptions } from './useSelection'

// 搜索
export { useSearch, useAdvancedSearch } from './useSearch'
export type { UseSearchOptions, UseAdvancedSearchOptions } from './useSearch'
