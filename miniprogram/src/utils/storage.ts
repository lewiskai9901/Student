/**
 * 本地存储工具
 */

/**
 * 存储数据
 */
export function setStorage(key: string, value: any): void {
  try {
    const data = typeof value === 'object' ? JSON.stringify(value) : value
    uni.setStorageSync(key, data)
  } catch (e) {
    console.error('Storage setItem error:', e)
  }
}

/**
 * 获取数据
 */
export function getStorage<T = any>(key: string, defaultValue?: T): T | null {
  try {
    const value = uni.getStorageSync(key)
    if (!value) return defaultValue ?? null

    try {
      return JSON.parse(value) as T
    } catch {
      return value as T
    }
  } catch (e) {
    console.error('Storage getItem error:', e)
    return defaultValue ?? null
  }
}

/**
 * 删除数据
 */
export function removeStorage(key: string): void {
  try {
    uni.removeStorageSync(key)
  } catch (e) {
    console.error('Storage removeItem error:', e)
  }
}

/**
 * 清空所有数据
 */
export function clearStorage(): void {
  try {
    uni.clearStorageSync()
  } catch (e) {
    console.error('Storage clear error:', e)
  }
}

/**
 * 存储信息类型
 */
interface StorageInfo {
  keys: string[]
  currentSize: number
  limitSize: number
}

/**
 * 获取存储信息
 */
export function getStorageInfo(): StorageInfo | null {
  try {
    return uni.getStorageInfoSync()
  } catch (e) {
    console.error('Storage getInfo error:', e)
    return null
  }
}
