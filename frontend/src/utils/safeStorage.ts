/**
 * Safari 隐私模式 / 配额耗尽兼容的 localStorage / sessionStorage 封装 (K1).
 *
 * <p>原生 localStorage 在以下情况会抛错:
 * <ul>
 *   <li>Safari 隐私浏览模式 — getItem 返回 null, setItem throw QuotaExceededError</li>
 *   <li>磁盘配额耗尽 — setItem throw</li>
 *   <li>SSR (本项目无, 但留作以后 Nuxt 兼容)</li>
 * </ul>
 *
 * <p>未包 try/catch 的 setup-time 调用会让整个组件 / store / app 崩溃.
 *
 * <p>用法:
 * <pre>
 *   safeLocalStorage.getItem('foo')        // 返回 null 兜底
 *   safeLocalStorage.setItem('foo', 'bar') // 返回 false 表示失败 (但不抛)
 *   safeLocalStorage.removeItem('foo')
 *   safeLocalStorage.getJSON<MyType>('key') // 反序列化 + 容错
 *   safeLocalStorage.setJSON('key', value)
 * </pre>
 */

/** 通用 Storage 兼容封装 — localStorage / sessionStorage 同接口. */
export interface SafeStorage {
    getItem(key: string): string | null
    setItem(key: string, value: string): boolean
    removeItem(key: string): boolean
    getJSON<T>(key: string, fallback?: T | null): T | null
    setJSON(key: string, value: unknown): boolean
}

function buildSafeStorage(storage: () => Storage): SafeStorage {
    return {
        getItem(key: string): string | null {
            try {
                return storage().getItem(key)
            } catch {
                return null
            }
        },

        setItem(key: string, value: string): boolean {
            try {
                storage().setItem(key, value)
                return true
            } catch {
                return false
            }
        },

        removeItem(key: string): boolean {
            try {
                storage().removeItem(key)
                return true
            } catch {
                return false
            }
        },

        getJSON<T>(key: string, fallback: T | null = null): T | null {
            try {
                const raw = storage().getItem(key)
                if (raw === null) return fallback
                return JSON.parse(raw) as T
            } catch {
                return fallback
            }
        },

        setJSON(key: string, value: unknown): boolean {
            try {
                storage().setItem(key, JSON.stringify(value))
                return true
            } catch {
                return false
            }
        },
    }
}

export const safeLocalStorage: SafeStorage = buildSafeStorage(() => localStorage)
export const safeSessionStorage: SafeStorage = buildSafeStorage(() => sessionStorage)
