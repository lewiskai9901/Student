/**
 * @hey-api/client-axios 运行时配置.
 *
 * 这是 codegen 生成的 SDK 共享 axios client 配置入口. hey-api 会用此配置
 * 创建一个 axios 实例; 我们让 baseURL 和 JWT 头复用 request.ts 的设置,
 * 这样 SDK 与手写 API 走同一个鉴权 / Result envelope 解构 / 错误处理管道.
 *
 * !! 重要: hey-api 的 axios client 默认不会解构 Result<T> envelope.
 * 我们通过 axios interceptors 让响应自动 .data 解包, 与现有手写 API 行为一致.
 */
import type { CreateClientConfig } from './api-generated/client.gen'
import service from '@/utils/request'

export const createClientConfig: CreateClientConfig = (config) => ({
  ...config,
  // 复用 request.ts 创建好的 axios 实例 (内置 JWT auth + Result envelope 解构 + 401 refresh)
  baseURL: (service.defaults?.baseURL as string | undefined) ?? '/api',
  // 让 hey-api 用我们的 axios 实例做请求, 共享 interceptor 链路
  axios: service,
})
