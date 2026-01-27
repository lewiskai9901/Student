import { ref, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'

/**
 * 搜索 Composable
 * 提供标准化的搜索功能
 */
export interface UseSearchOptions<P> {
  defaultParams?: Partial<P>
  debounceMs?: number
  onSearch?: (params: P) => void
}

export function useSearch<P extends Record<string, unknown>>(
  options: UseSearchOptions<P> = {}
) {
  const { defaultParams = {}, debounceMs = 300, onSearch } = options

  // 搜索参数
  const searchParams = ref<Partial<P>>({ ...defaultParams }) as { value: Partial<P> }

  // 关键字（快捷访问）
  const keyword = ref('')

  // 搜索回调
  const doSearch = () => {
    onSearch?.(searchParams.value as P)
  }

  // 防抖搜索
  const debouncedSearch = useDebounceFn(doSearch, debounceMs)

  // 立即搜索
  const search = () => {
    doSearch()
  }

  // 设置参数并搜索
  const setParamsAndSearch = (params: Partial<P>) => {
    searchParams.value = { ...searchParams.value, ...params }
    search()
  }

  // 重置参数
  const resetParams = () => {
    searchParams.value = { ...defaultParams }
    keyword.value = ''
  }

  // 重置并搜索
  const resetAndSearch = () => {
    resetParams()
    search()
  }

  // 更新单个参数
  const setParam = <K extends keyof P>(key: K, value: P[K]) => {
    (searchParams.value as P)[key] = value
  }

  // 监听关键字变化进行防抖搜索
  watch(keyword, (newValue) => {
    if ('keyword' in searchParams.value || 'name' in searchParams.value) {
      if ('keyword' in searchParams.value) {
        (searchParams.value as Record<string, unknown>).keyword = newValue
      }
      if ('name' in searchParams.value) {
        (searchParams.value as Record<string, unknown>).name = newValue
      }
      debouncedSearch()
    }
  })

  return {
    searchParams,
    keyword,
    search,
    debouncedSearch,
    setParamsAndSearch,
    resetParams,
    resetAndSearch,
    setParam
  }
}

/**
 * 高级搜索 Composable
 * 支持展开/收起的高级搜索
 */
export interface UseAdvancedSearchOptions<P> extends UseSearchOptions<P> {
  basicFields?: (keyof P)[]
}

export function useAdvancedSearch<P extends Record<string, unknown>>(
  options: UseAdvancedSearchOptions<P> = {}
) {
  const { basicFields = [], ...searchOptions } = options

  const search = useSearch(searchOptions)

  // 是否展开高级搜索
  const isExpanded = ref(false)

  // 切换展开状态
  const toggleExpand = () => {
    isExpanded.value = !isExpanded.value
  }

  // 展开
  const expand = () => {
    isExpanded.value = true
  }

  // 收起
  const collapse = () => {
    isExpanded.value = false
  }

  // 收起并清除高级参数
  const collapseAndReset = () => {
    // 只保留基础字段
    const basicParams: Partial<P> = {}
    basicFields.forEach((field) => {
      if (field in search.searchParams.value) {
        (basicParams as Record<string, unknown>)[field as string] =
          (search.searchParams.value as Record<string, unknown>)[field as string]
      }
    })
    search.searchParams.value = basicParams
    collapse()
    search.search()
  }

  return {
    ...search,
    isExpanded,
    toggleExpand,
    expand,
    collapse,
    collapseAndReset
  }
}
