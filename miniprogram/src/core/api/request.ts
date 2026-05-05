import { capability } from '../platform/auto'

export interface RequestOpts {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: unknown
  header?: Record<string, string>
  skipAuth?: boolean
}

const BASE_URL = 'http://localhost:8080/api'

export class HttpError extends Error {
  constructor(public statusCode: number, public body: unknown, message?: string) {
    super(message ?? `HTTP ${statusCode}`)
  }
}

declare const uni: any

export async function request<T = unknown>(opts: RequestOpts): Promise<T> {
  const token = capability.storage.get<string>('accessToken')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...opts.header
  }
  if (token && !opts.skipAuth) headers.Authorization = `Bearer ${token}`

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + opts.url,
      method: opts.method || 'GET',
      data: opts.data,
      header: headers,
      success: (r: any) => {
        if (r.statusCode >= 200 && r.statusCode < 300) {
          resolve(r.data as T)
        } else if (r.statusCode === 401) {
          // Phase D 接 refresh token;Phase A 简化为清登录态 + 跳登录
          capability.storage.remove('accessToken')
          capability.storage.remove('refreshToken')
          uni.reLaunch({ url: '/core/pages/login/index' })
          reject(new HttpError(401, r.data, 'Unauthorized'))
        } else {
          reject(new HttpError(r.statusCode, r.data))
        }
      },
      fail: (e: any) => reject(new HttpError(0, e, e?.errMsg ?? 'network error'))
    })
  })
}

export interface ResultEnvelope<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export class BizError extends Error {
  constructor(public code: number, public bizMessage: string) {
    super(`[${code}] ${bizMessage}`)
    this.name = 'BizError'
  }
}

export async function requestWrapped<T>(opts: RequestOpts): Promise<T> {
  const r = await request<ResultEnvelope<T>>(opts)
  if (!r || typeof r.code !== 'number') {
    throw new BizError(-1, 'malformed response envelope')
  }
  if (r.code !== 200) {
    throw new BizError(r.code, r.message ?? 'unknown error')
  }
  return r.data
}
