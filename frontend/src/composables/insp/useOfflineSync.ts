/**
 * V7 检查平台 - 离线同步 Composable
 *
 * 职责：
 * 1. IndexedDB 存储离线表单草稿
 * 2. 同步队列管理（待上传变更）
 * 3. 网络状态检测（在线/离线）
 * 4. 冲突检测与解决
 */
import { ref, readonly, onMounted, onUnmounted } from 'vue'
import { inspSyncApi } from '@/api/insp/sync'
import type { SyncPushItem, SyncPushResult } from '@/api/insp/sync'
import type { InspSubmission, SubmissionDetail } from '@/types/insp/project'

// ==================== IndexedDB Helpers ====================

const DB_NAME = 'insp_offline_v7'
const DB_VERSION = 1

interface OfflineDraft {
  submissionId: number
  formData: string
  clientSyncVersion: number
  savedAt: number // timestamp
}

interface SyncConflict {
  submissionId: number
  localFormData: string
  localSyncVersion: number
  serverFormData: string
  serverSyncVersion: number
  detectedAt: number
}

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = () => {
      const db = request.result
      if (!db.objectStoreNames.contains('drafts')) {
        db.createObjectStore('drafts', { keyPath: 'submissionId' })
      }
      if (!db.objectStoreNames.contains('submissions')) {
        db.createObjectStore('submissions', { keyPath: 'id' })
      }
      if (!db.objectStoreNames.contains('details')) {
        db.createObjectStore('details', { keyPath: 'id' })
      }
      if (!db.objectStoreNames.contains('conflicts')) {
        db.createObjectStore('conflicts', { keyPath: 'submissionId' })
      }
      if (!db.objectStoreNames.contains('meta')) {
        db.createObjectStore('meta', { keyPath: 'key' })
      }
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

async function dbPut<T>(storeName: string, data: T): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite')
    tx.objectStore(storeName).put(data)
    tx.oncomplete = () => { db.close(); resolve() }
    tx.onerror = () => { db.close(); reject(tx.error) }
  })
}

async function dbGet<T>(storeName: string, key: IDBValidKey): Promise<T | undefined> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readonly')
    const req = tx.objectStore(storeName).get(key)
    req.onsuccess = () => { db.close(); resolve(req.result as T | undefined) }
    req.onerror = () => { db.close(); reject(req.error) }
  })
}

async function dbGetAll<T>(storeName: string): Promise<T[]> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readonly')
    const req = tx.objectStore(storeName).getAll()
    req.onsuccess = () => { db.close(); resolve(req.result as T[]) }
    req.onerror = () => { db.close(); reject(req.error) }
  })
}

async function dbDelete(storeName: string, key: IDBValidKey): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite')
    tx.objectStore(storeName).delete(key)
    tx.oncomplete = () => { db.close(); resolve() }
    tx.onerror = () => { db.close(); reject(tx.error) }
  })
}

async function dbClearStore(storeName: string): Promise<void> {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite')
    tx.objectStore(storeName).clear()
    tx.oncomplete = () => { db.close(); resolve() }
    tx.onerror = () => { db.close(); reject(tx.error) }
  })
}

// ==================== Composable ====================

export function useOfflineSync(taskId: number) {
  const isOnline = ref(navigator.onLine)
  const pendingCount = ref(0)
  const conflicts = ref<SyncConflict[]>([])
  const isSyncing = ref(false)
  const lastSyncAt = ref<string | null>(null)

  // Cached data for offline viewing
  const cachedSubmissions = ref<InspSubmission[]>([])
  const cachedDetails = ref<SubmissionDetail[]>([])

  // Network status listeners
  function handleOnline() { isOnline.value = true }
  function handleOffline() { isOnline.value = false }

  onMounted(async () => {
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)
    await loadCachedData()
    await loadPendingCount()
    await loadConflicts()
    await loadLastSyncAt()
  })

  onUnmounted(() => {
    window.removeEventListener('online', handleOnline)
    window.removeEventListener('offline', handleOffline)
  })

  // ========== Cache Management ==========

  async function loadCachedData() {
    cachedSubmissions.value = await dbGetAll<InspSubmission>('submissions')
    cachedDetails.value = await dbGetAll<SubmissionDetail>('details')
  }

  async function loadPendingCount() {
    const drafts = await dbGetAll<OfflineDraft>('drafts')
    pendingCount.value = drafts.length
  }

  async function loadConflicts() {
    conflicts.value = await dbGetAll<SyncConflict>('conflicts')
  }

  async function loadLastSyncAt() {
    const meta = await dbGet<{ key: string; value: string }>('meta', `lastSync_${taskId}`)
    lastSyncAt.value = meta?.value ?? null
  }

  // ========== Offline Draft Operations ==========

  /** Save form data locally (offline-first) */
  async function saveDraft(submissionId: number, formData: string, syncVersion: number) {
    const draft: OfflineDraft = {
      submissionId,
      formData,
      clientSyncVersion: syncVersion,
      savedAt: Date.now(),
    }
    await dbPut('drafts', draft)
    await loadPendingCount()
  }

  /** Get a draft from local storage */
  async function getDraft(submissionId: number): Promise<OfflineDraft | undefined> {
    return dbGet<OfflineDraft>('drafts', submissionId)
  }

  /** Remove a draft after successful sync */
  async function removeDraft(submissionId: number) {
    await dbDelete('drafts', submissionId)
    await loadPendingCount()
  }

  // ========== Sync Operations ==========

  /** Pull latest data from server and cache locally */
  async function pullFromServer() {
    if (!isOnline.value) return

    isSyncing.value = true
    try {
      const response = await inspSyncApi.syncPull({
        taskId,
        lastSyncAt: lastSyncAt.value,
      })

      // Cache submissions
      for (const sub of response.submissions) {
        await dbPut('submissions', sub)
      }
      // Cache details
      for (const detail of response.details) {
        await dbPut('details', detail)
      }

      // Update last sync time
      await dbPut('meta', { key: `lastSync_${taskId}`, value: response.serverTime })
      lastSyncAt.value = response.serverTime

      await loadCachedData()
    } finally {
      isSyncing.value = false
    }
  }

  /** Push all pending drafts to server */
  async function pushToServer(): Promise<SyncPushResult[]> {
    if (!isOnline.value) return []

    const drafts = await dbGetAll<OfflineDraft>('drafts')
    if (drafts.length === 0) return []

    isSyncing.value = true
    try {
      const items: SyncPushItem[] = drafts.map(d => ({
        submissionId: d.submissionId,
        formData: d.formData,
        clientSyncVersion: d.clientSyncVersion,
      }))

      const response = await inspSyncApi.syncPush(items)

      // Process results
      for (const result of response.results) {
        if (result.status === 'SYNCED') {
          await removeDraft(result.submissionId)
        } else if (result.status === 'CONFLICT') {
          const draft = drafts.find(d => d.submissionId === result.submissionId)
          if (draft) {
            const conflict: SyncConflict = {
              submissionId: result.submissionId,
              localFormData: draft.formData,
              localSyncVersion: draft.clientSyncVersion,
              serverFormData: result.serverFormData ?? '',
              serverSyncVersion: result.serverSyncVersion ?? 0,
              detectedAt: Date.now(),
            }
            await dbPut('conflicts', conflict)
          }
        }
        // NOT_FOUND, REJECTED — remove from queue (data is stale)
        if (result.status === 'NOT_FOUND' || result.status === 'REJECTED') {
          await removeDraft(result.submissionId)
        }
      }

      await loadPendingCount()
      await loadConflicts()
      await dbPut('meta', { key: `lastSync_${taskId}`, value: response.serverTime })
      lastSyncAt.value = response.serverTime

      return response.results
    } finally {
      isSyncing.value = false
    }
  }

  /** Full sync: push then pull */
  async function fullSync(): Promise<SyncPushResult[]> {
    const results = await pushToServer()
    await pullFromServer()
    return results
  }

  // ========== Conflict Resolution ==========

  /** Resolve a conflict by keeping local version */
  async function resolveKeepLocal(submissionId: number) {
    const conflict = await dbGet<SyncConflict>('conflicts', submissionId)
    if (!conflict) return

    // Re-queue the draft with server's syncVersion (force overwrite on next push)
    await saveDraft(submissionId, conflict.localFormData, conflict.serverSyncVersion)
    await dbDelete('conflicts', submissionId)
    await loadConflicts()
  }

  /** Resolve a conflict by keeping server version */
  async function resolveKeepServer(submissionId: number) {
    await removeDraft(submissionId)
    await dbDelete('conflicts', submissionId)
    await loadConflicts()
  }

  // ========== Cleanup ==========

  async function clearAllOfflineData() {
    await dbClearStore('drafts')
    await dbClearStore('submissions')
    await dbClearStore('details')
    await dbClearStore('conflicts')
    await dbClearStore('meta')
    pendingCount.value = 0
    conflicts.value = []
    lastSyncAt.value = null
    cachedSubmissions.value = []
    cachedDetails.value = []
  }

  return {
    // State
    isOnline: readonly(isOnline),
    pendingCount: readonly(pendingCount),
    conflicts: readonly(conflicts),
    isSyncing: readonly(isSyncing),
    lastSyncAt: readonly(lastSyncAt),
    cachedSubmissions: readonly(cachedSubmissions),
    cachedDetails: readonly(cachedDetails),

    // Draft operations
    saveDraft,
    getDraft,
    removeDraft,

    // Sync operations
    pullFromServer,
    pushToServer,
    fullSync,

    // Conflict resolution
    resolveKeepLocal,
    resolveKeepServer,

    // Cleanup
    clearAllOfflineData,
  }
}
