/**
 * @hey-api/client-axios 运行时配置.
 *
 * 用 sdkAxios (sister 实例) — 与 service 共享 baseURL/JWT, 但 **不撸 Result envelope**.
 * 因为 hey-api 客户端期望标准 axios 响应形状 ({data, status, ...}) 自己消费 envelope;
 * 如果用 service (有 envelope 解构 interceptor), hey-api 会拿到数组而非对象,
 * 导致 `let {data} = response` 解构出 undefined → 渲染空列表.
 */
import type { CreateClientConfig } from './api-generated/client.gen'
import { sdkAxios } from '@/utils/request'

export const createClientConfig: CreateClientConfig = (config) => ({
  ...config,
  // sdkAxios 已经做了 /api 前缀注入 (interceptor) + JWT — hey-api 会硬覆 baseURL='', 这里设啥都行
  baseURL: (sdkAxios.defaults?.baseURL as string | undefined) ?? '/api',
  axios: sdkAxios,
})
