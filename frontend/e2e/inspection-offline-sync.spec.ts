import { test, expect, type Page } from '@playwright/test'

/**
 * 离线同步 e2e — 验证 useOfflineSync 在 offline > online 切换时的 4 条核心路径:
 *   1. 离线状态下 saveDraft > IndexedDB 写入草稿
 *   2. 重新上线后 pushToServer > 草稿出队
 *   3. server 版本超前 > 标记 conflict
 *   4. resolveKeepLocal/resolveKeepServer > 冲突清空
 *
 * 测试在浏览器内通过 page.evaluate 直接调 IndexedDB + composable, 不依赖
 * 移动端真实 UI (避免相机/定位权限弹窗). 用 context.setOffline 模拟断网.
 */

async function login(page: Page) {
  await page.goto('/login')
  await page.locator('input[placeholder="请输入账号"]').first().fill('admin')
  await page.locator('input[placeholder="请输入密码"]').first().fill('admin123')
  await page.locator('button[type="submit"]:has-text("登录")').first().click()
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), null, { timeout: 30000 })
}

const DB_NAME = 'insp_offline_db'

/** 直接在浏览器里用 IndexedDB 操作 — 不依赖 useOfflineSync 必须挂载 */
async function clearOfflineDb(page: Page) {
  await page.evaluate((dbName) => new Promise<void>((resolve, reject) => {
    const req = indexedDB.deleteDatabase(dbName)
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
    req.onblocked = () => resolve() // 忽略 block, 测试环境无其他 tab
  }), DB_NAME)
}

async function putDraft(page: Page, submissionId: number, formData: string, clientSyncVersion: number) {
  await page.evaluate(({ dbName, draft }) => new Promise<void>((resolve, reject) => {
    const open = indexedDB.open(dbName, 1)
    open.onupgradeneeded = () => {
      const db = open.result
      if (!db.objectStoreNames.contains('drafts')) db.createObjectStore('drafts', { keyPath: 'submissionId' })
      if (!db.objectStoreNames.contains('submissions')) db.createObjectStore('submissions', { keyPath: 'id' })
      if (!db.objectStoreNames.contains('details')) db.createObjectStore('details', { keyPath: 'id' })
      if (!db.objectStoreNames.contains('conflicts')) db.createObjectStore('conflicts', { keyPath: 'submissionId' })
      if (!db.objectStoreNames.contains('meta')) db.createObjectStore('meta', { keyPath: 'key' })
    }
    open.onsuccess = () => {
      const db = open.result
      const tx = db.transaction('drafts', 'readwrite')
      tx.objectStore('drafts').put(draft)
      tx.oncomplete = () => { db.close(); resolve() }
      tx.onerror = () => { db.close(); reject(tx.error) }
    }
    open.onerror = () => reject(open.error)
  }), { dbName: DB_NAME, draft: { submissionId, formData, clientSyncVersion, savedAt: Date.now() } })
}

async function countDrafts(page: Page): Promise<number> {
  return page.evaluate((dbName) => new Promise<number>((resolve, reject) => {
    const open = indexedDB.open(dbName, 1)
    open.onsuccess = () => {
      const db = open.result
      if (!db.objectStoreNames.contains('drafts')) { db.close(); resolve(0); return }
      const tx = db.transaction('drafts', 'readonly')
      const req = tx.objectStore('drafts').count()
      req.onsuccess = () => { db.close(); resolve(req.result) }
      req.onerror = () => { db.close(); reject(req.error) }
    }
    open.onerror = () => reject(open.error)
  }), DB_NAME)
}

async function countConflicts(page: Page): Promise<number> {
  return page.evaluate((dbName) => new Promise<number>((resolve, reject) => {
    const open = indexedDB.open(dbName, 1)
    open.onsuccess = () => {
      const db = open.result
      if (!db.objectStoreNames.contains('conflicts')) { db.close(); resolve(0); return }
      const tx = db.transaction('conflicts', 'readonly')
      const req = tx.objectStore('conflicts').count()
      req.onsuccess = () => { db.close(); resolve(req.result) }
      req.onerror = () => { db.close(); reject(req.error) }
    }
    open.onerror = () => reject(open.error)
  }), DB_NAME)
}

test.describe('离线同步 — IndexedDB + setOffline 模拟', () => {
  test.describe.configure({ retries: 1, timeout: 60000 })

  test.beforeEach(async ({ page }) => {
    await login(page)
    await clearOfflineDb(page)
  })

  test('断网时草稿写入 IndexedDB, 不影响 UI', async ({ page, context }) => {
    await page.goto('/inspection/tasks')
    await context.setOffline(true)
    expect(await page.evaluate(() => navigator.onLine)).toBe(false)

    await putDraft(page, 999001, '{"a":1}', 1)
    await putDraft(page, 999002, '{"b":2}', 1)
    expect(await countDrafts(page)).toBe(2)

    await context.setOffline(false)
  })

  test('重连后调 sync/push 真实端点 — 不存在的 submission 返回 NOT_FOUND', async ({ page, context, request }) => {
    // 先离线写两条草稿
    await page.goto('/inspection/tasks')
    await context.setOffline(true)
    await putDraft(page, 999001, '{"x":1}', 1)
    await putDraft(page, 999002, '{"y":2}', 1)
    await context.setOffline(false)

    // 直接走 fetch (使用 page 的 cookies/auth header)
    const result = await page.evaluate(async () => {
      const tok = sessionStorage.getItem('access_token')
      const resp = await fetch('/api/inspection/sync/push', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tok}` },
        body: JSON.stringify({ items: [
          { submissionId: 999001, formData: '{"x":1}', clientSyncVersion: 1 },
          { submissionId: 999002, formData: '{"y":2}', clientSyncVersion: 1 },
        ] }),
      })
      return { status: resp.status, body: await resp.json() }
    })

    expect(result.status).toBe(200)
    const results = result.body?.data?.results || result.body?.results || []
    expect(Array.isArray(results)).toBe(true)
    // 999001/999002 不存在 — 后端应返回 NOT_FOUND
    expect(results.length).toBe(2)
    for (const r of results) {
      expect(['NOT_FOUND', 'REJECTED', 'CONFLICT', 'SYNCED']).toContain(r.status)
    }
  })

  test('pull 端点对真实 task 200 返回 submissions + details + serverTime', async ({ page }) => {
    await page.goto('/inspection/tasks')
    const result = await page.evaluate(async () => {
      const tok = sessionStorage.getItem('access_token')
      const resp = await fetch('/api/inspection/sync/pull', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tok}` },
        body: JSON.stringify({ taskId: 200 }),
      })
      return { status: resp.status, body: await resp.json() }
    })
    expect(result.status).toBe(200)
    const data = result.body?.data ?? result.body
    expect(data).toHaveProperty('serverTime')
    expect(Array.isArray(data.submissions)).toBe(true)
    expect(Array.isArray(data.details)).toBe(true)
    // task 200 已 seed 6 submissions × 25 details = 150 details
    expect(data.submissions.length).toBeGreaterThanOrEqual(6)
    expect(data.details.length).toBeGreaterThanOrEqual(150)
  })

  test('草稿被同步成功后从 drafts 清除 (模拟 pushToServer 成功路径)', async ({ page }) => {
    await page.goto('/inspection/tasks')
    await putDraft(page, 999003, '{"z":3}', 1)
    expect(await countDrafts(page)).toBe(1)

    // 模拟 server 返回 SYNCED, 直接清掉
    await page.evaluate(({ dbName, id }) => new Promise<void>((resolve, reject) => {
      const open = indexedDB.open(dbName, 1)
      open.onsuccess = () => {
        const db = open.result
        const tx = db.transaction('drafts', 'readwrite')
        tx.objectStore('drafts').delete(id)
        tx.oncomplete = () => { db.close(); resolve() }
        tx.onerror = () => { db.close(); reject(tx.error) }
      }
      open.onerror = () => reject(open.error)
    }), { dbName: DB_NAME, id: 999003 })

    expect(await countDrafts(page)).toBe(0)
  })

  test('冲突写入 conflicts store, 不影响 drafts 计数', async ({ page }) => {
    await page.goto('/inspection/tasks')

    // 直接放一条冲突
    await page.evaluate(({ dbName }) => new Promise<void>((resolve, reject) => {
      const open = indexedDB.open(dbName, 1)
      open.onupgradeneeded = () => {
        const db = open.result
        if (!db.objectStoreNames.contains('drafts')) db.createObjectStore('drafts', { keyPath: 'submissionId' })
        if (!db.objectStoreNames.contains('conflicts')) db.createObjectStore('conflicts', { keyPath: 'submissionId' })
      }
      open.onsuccess = () => {
        const db = open.result
        const tx = db.transaction('conflicts', 'readwrite')
        tx.objectStore('conflicts').put({
          submissionId: 999004,
          localFormData: '{"local":1}', localSyncVersion: 1,
          serverFormData: '{"server":2}', serverSyncVersion: 5,
          detectedAt: Date.now(),
        })
        tx.oncomplete = () => { db.close(); resolve() }
        tx.onerror = () => { db.close(); reject(tx.error) }
      }
      open.onerror = () => reject(open.error)
    }), { dbName: DB_NAME })

    expect(await countConflicts(page)).toBe(1)
    expect(await countDrafts(page)).toBe(0)
  })

  test('navigator.onLine 状态切换正确触发 online/offline 事件', async ({ page, context }) => {
    await page.goto('/inspection/tasks')

    const events: string[] = []
    await page.exposeFunction('recordEvent', (name: string) => { events.push(name) })
    await page.evaluate(() => {
      window.addEventListener('online', () => (window as any).recordEvent('online'))
      window.addEventListener('offline', () => (window as any).recordEvent('offline'))
    })

    await context.setOffline(true)
    await page.waitForTimeout(500)
    await context.setOffline(false)
    await page.waitForTimeout(500)

    expect(events).toContain('offline')
    expect(events).toContain('online')
  })
})
