/**
 * P3 客户端错误捕获 + 上报
 *
 * 捕获三类错误:
 * 1. Vue componentError — app.config.errorHandler
 * 2. window.onerror — 全局 JS 异常
 * 3. unhandledrejection — Promise 未捕获 reject
 *
 * 上报到 POST /api/errors/log (匿名亦可)
 *
 * 防滥用 (前端侧):
 * - 同一指纹 60s 内仅上报 1 次
 * - 单 session 最多 50 条 (防止 JS 死循环刷爆)
 * - 不上报: AbortError, NetworkError (这些是用户操作/连接问题, 非代码 bug)
 */

const REPORT_ENDPOINT = '/api/errors/log'
const MAX_REPORTS_PER_SESSION = 50
const DEDUPE_WINDOW_MS = 60_000

const recentFingerprints = new Map<string, number>()
let reportCount = 0
let installed = false

const IGNORED_PATTERNS = [
  /AbortError/i,
  /Network ?Error/i,
  /NetworkError/i,
  /Loading chunk \d+ failed/i,  // 通常是部署期间用户旧 JS
  /ResizeObserver loop/i,        // 浏览器内核警告, 非真实 bug
  /Script error\.?$/,            // 跨域脚本错误, 信息无意义
]

function shouldIgnore(message: string): boolean {
  if (!message) return true
  return IGNORED_PATTERNS.some(p => p.test(message))
}

function fingerprint(message: string, url?: string): string {
  const base = (message || '').slice(0, 200) + '|' + (url || '').split('?')[0]
  let h = 0
  for (let i = 0; i < base.length; i++) h = ((h << 5) - h + base.charCodeAt(i)) | 0
  return Math.abs(h).toString(36)
}

interface ReportPayload {
  level?: 'ERROR' | 'WARN' | 'INFO'
  source?: 'JS' | 'VUE' | 'HTTP' | 'UNHANDLED'
  message: string
  stack?: string
  url?: string
  routePath?: string
  userAgent?: string
}

export function reportError(payload: ReportPayload): void {
  if (reportCount >= MAX_REPORTS_PER_SESSION) return
  if (shouldIgnore(payload.message)) return

  const fp = fingerprint(payload.message, payload.url)
  const now = Date.now()
  const lastSeen = recentFingerprints.get(fp)
  if (lastSeen && now - lastSeen < DEDUPE_WINDOW_MS) return
  recentFingerprints.set(fp, now)
  reportCount++

  // 异步上报, 失败静默 (避免错误循环)
  try {
    const tok = sessionStorage.getItem('access_token')
    fetch(REPORT_ENDPOINT, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(tok ? { Authorization: 'Bearer ' + tok } : {}),
      },
      body: JSON.stringify({
        level: payload.level || 'ERROR',
        source: payload.source || 'JS',
        message: payload.message,
        stack: payload.stack,
        url: payload.url || location.href,
        routePath: payload.routePath || location.pathname,
        userAgent: payload.userAgent || navigator.userAgent,
      }),
      // keepalive 让 unload 期间也能发出
      keepalive: true,
    }).catch(() => { /* 静默 */ })
  } catch { /* 静默 */ }
}

/**
 * 安装全局错误捕获. 只能调用一次 (idempotent).
 * 应在 main.ts 启动时调用.
 */
export function installErrorReporter(app: any): void {
  if (installed) return
  installed = true

  // 1. Vue 组件错误
  if (app && app.config) {
    app.config.errorHandler = (err: any, instance: any, info: string) => {
      console.error('[Vue Error]', err, info)
      reportError({
        source: 'VUE',
        message: String(err?.message || err || 'Vue error'),
        stack: err?.stack,
        url: location.href,
      })
    }
  }

  // 2. 全局 JS 错误 (window.onerror)
  window.addEventListener('error', (event) => {
    // 资源加载错误 (img / script) 单独处理
    if (event.target && (event.target as HTMLElement).tagName) {
      const tag = (event.target as HTMLElement).tagName
      if (['IMG', 'SCRIPT', 'LINK'].includes(tag)) {
        // 资源 404 不算严重错误, 不上报
        return
      }
    }
    reportError({
      source: 'JS',
      message: event.message,
      stack: event.error?.stack,
      url: event.filename,
    })
  })

  // 3. 未捕获 Promise reject
  window.addEventListener('unhandledrejection', (event) => {
    const reason = event.reason
    reportError({
      source: 'UNHANDLED',
      message: String(reason?.message || reason || 'Unhandled rejection'),
      stack: reason?.stack,
    })
  })

  console.log('[errorReporter] installed (P3 自建错误上报)')
}
