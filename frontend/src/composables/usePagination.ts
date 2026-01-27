import { ref, computed, watch } from 'vue'

/**
 * 分页 Composable
 * 提供标准化的分页状态管理
 */
export interface PaginationOptions {
  defaultPageSize?: number
  pageSizes?: number[]
}

export function usePagination(options: PaginationOptions = {}) {
  const { defaultPageSize = 10, pageSizes = [10, 20, 50, 100] } = options

  const currentPage = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)

  // 总页数
  const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

  // 是否有上一页
  const hasPrevPage = computed(() => currentPage.value > 1)

  // 是否有下一页
  const hasNextPage = computed(() => currentPage.value < totalPages.value)

  // 重置到第一页
  const resetPage = () => {
    currentPage.value = 1
  }

  // 上一页
  const prevPage = () => {
    if (hasPrevPage.value) {
      currentPage.value--
    }
  }

  // 下一页
  const nextPage = () => {
    if (hasNextPage.value) {
      currentPage.value++
    }
  }

  // 跳转到指定页
  const goToPage = (page: number) => {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page
    }
  }

  // 设置总数
  const setTotal = (value: number) => {
    total.value = value
  }

  // 分页参数
  const paginationParams = computed(() => ({
    pageNum: currentPage.value,
    pageSize: pageSize.value
  }))

  // 当 pageSize 变化时，重置到第一页
  watch(pageSize, () => {
    currentPage.value = 1
  })

  return {
    currentPage,
    pageSize,
    total,
    totalPages,
    pageSizes,
    hasPrevPage,
    hasNextPage,
    paginationParams,
    resetPage,
    prevPage,
    nextPage,
    goToPage,
    setTotal
  }
}
