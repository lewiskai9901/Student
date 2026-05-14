/**
 * 小程序核心共享类型.
 *
 * 与 frontend/src/types/common.ts 保持一致的 LongId 契约.
 */

/**
 * 后端 Long 字段的小程序类型.
 *
 * 后端 `JacksonConfig.java:37-38` 把所有 Long 序列化为 JSON string,
 * 防止 JS 53-bit 大数精度丢失 (雪花 ID 超 Number.MAX_SAFE_INTEGER).
 * 所有 `id / xxxId` 字段实际收到的是 string, 不是 number.
 *
 * 用法:
 *   import type { LongId } from '@core/types'
 *   interface Foo { id: LongId; projectId: LongId; count: number }
 *
 * 真 number 字段不要用 LongId: count / score / weight / total / expiresIn.
 */
export type LongId = string
