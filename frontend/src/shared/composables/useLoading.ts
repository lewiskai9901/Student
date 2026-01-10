import { ref, computed } from 'vue'

/**
 * 加载状态 Composable
 * 提供多个加载状态的管理
 */
export function useLoading() {
  const loadingMap = ref<Record<string, boolean>>({})
  const defaultKey = 'default'

  // 检查是否有任何加载中
  const isAnyLoading = computed(() => Object.values(loadingMap.value).some(v => v))

  // 获取指定 key 的加载状态
  const isLoading = (key: string = defaultKey) => computed(() => loadingMap.value[key] || false)

  // 开始加载
  const startLoading = (key: string = defaultKey) => {
    loadingMap.value[key] = true
  }

  // 结束加载
  const stopLoading = (key: string = defaultKey) => {
    loadingMap.value[key] = false
  }

  // 包装异步函数
  const withLoading = async <T>(
    fn: () => Promise<T>,
    key: string = defaultKey
  ): Promise<T> => {
    startLoading(key)
    try {
      return await fn()
    } finally {
      stopLoading(key)
    }
  }

  // 重置所有加载状态
  const resetLoading = () => {
    loadingMap.value = {}
  }

  return {
    loadingMap,
    isAnyLoading,
    isLoading,
    startLoading,
    stopLoading,
    withLoading,
    resetLoading
  }
}

/**
 * 简单加载状态 Composable
 */
export function useSimpleLoading(initialValue = false) {
  const loading = ref(initialValue)

  const startLoading = () => {
    loading.value = true
  }

  const stopLoading = () => {
    loading.value = false
  }

  const withLoading = async <T>(fn: () => Promise<T>): Promise<T> => {
    startLoading()
    try {
      return await fn()
    } finally {
      stopLoading()
    }
  }

  return {
    loading,
    startLoading,
    stopLoading,
    withLoading
  }
}
