import { ref, computed, watch } from 'vue'
import { usePagination } from './usePagination'
import { useSimpleLoading } from './useLoading'

/**
 * 表格 Composable
 * 提供标准化的表格数据管理
 */
export interface UseTableOptions<T, P = Record<string, unknown>> {
  fetchData: (params: P & { pageNum: number; pageSize: number }) => Promise<{
    records: T[]
    total: number
  }>
  defaultParams?: Partial<P>
  immediate?: boolean
  defaultPageSize?: number
}

export function useTable<T, P = Record<string, unknown>>(options: UseTableOptions<T, P>) {
  const { fetchData, defaultParams = {}, immediate = true, defaultPageSize = 10 } = options

  // 数据
  const data = ref<T[]>([]) as { value: T[] }

  // 分页
  const pagination = usePagination({ defaultPageSize })

  // 加载状态
  const { loading, withLoading } = useSimpleLoading()

  // 查询参数
  const queryParams = ref<Partial<P>>({ ...defaultParams })

  // 选中项
  const selectedRows = ref<T[]>([]) as { value: T[] }
  const selectedIds = computed(() =>
    selectedRows.value.map((row: T & { id?: number }) => row.id).filter(Boolean) as number[]
  )

  // 是否有选中项
  const hasSelection = computed(() => selectedRows.value.length > 0)

  // 加载数据
  const loadData = async () => {
    await withLoading(async () => {
      try {
        const result = await fetchData({
          ...queryParams.value,
          ...pagination.paginationParams.value
        } as P & { pageNum: number; pageSize: number })

        data.value = result.records || []
        pagination.setTotal(result.total || 0)
      } catch (error) {
        console.error('加载数据失败:', error)
        data.value = []
        pagination.setTotal(0)
      }
    })
  }

  // 刷新数据（保持当前页）
  const refresh = () => loadData()

  // 重置并加载（回到第一页）
  const resetAndLoad = () => {
    pagination.resetPage()
    loadData()
  }

  // 搜索（回到第一页）
  const search = (params?: Partial<P>) => {
    if (params) {
      queryParams.value = { ...queryParams.value, ...params }
    }
    pagination.resetPage()
    loadData()
  }

  // 重置查询参数
  const resetQuery = () => {
    queryParams.value = { ...defaultParams }
    pagination.resetPage()
    loadData()
  }

  // 处理选择变化
  const handleSelectionChange = (rows: T[]) => {
    selectedRows.value = rows
  }

  // 清除选择
  const clearSelection = () => {
    selectedRows.value = []
  }

  // 监听分页变化
  watch(
    () => pagination.currentPage.value,
    () => loadData(),
    { immediate: false }
  )

  watch(
    () => pagination.pageSize.value,
    () => {
      pagination.resetPage()
      loadData()
    },
    { immediate: false }
  )

  // 立即加载
  if (immediate) {
    loadData()
  }

  return {
    // 数据
    data,
    loading,

    // 分页
    ...pagination,

    // 查询
    queryParams,
    search,
    resetQuery,

    // 选择
    selectedRows,
    selectedIds,
    hasSelection,
    handleSelectionChange,
    clearSelection,

    // 操作
    loadData,
    refresh,
    resetAndLoad
  }
}
