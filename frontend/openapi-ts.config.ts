import { defineConfig } from '@hey-api/openapi-ts'

/**
 * @hey-api/openapi-ts 配置: 把 backend/openapi.json → frontend/src/api-generated/
 *
 * 工作流:
 *   1. backend 改完接口后跑 `bash backend/scripts/export-openapi.sh` 更新 spec
 *   2. 前端跑 `npm run openapi:generate` 重生 SDK
 *   3. commit 两者
 *   4. CI 跑 drift 检测保证两者同步
 *
 * LongId 映射: backend 的 LongAsStringModelConverter 给所有 Long 字段加
 * `x-typescript-type: LongId` 扩展. 本配置的 customClient 转换会把这些
 * 字段的类型从 `string` 替换为 `LongId` (from @/types/common).
 */
export default defineConfig({
  input: '../backend/openapi.json',
  output: {
    path: 'src/api-generated',
    format: 'prettier',
    lint: false,
    clean: true,
  },
  plugins: [
    // axios client — 接现有 axios 实例 (在 client.ts 配置 baseURL/interceptor)
    {
      name: '@hey-api/client-axios',
      runtimeConfigPath: './src/api-generated-client.ts',
    },
    // 类型 + SDK
    '@hey-api/typescript',
    {
      name: '@hey-api/sdk',
      // 把所有 SDK function 收到一个 namespace
      asClass: false,
    },
  ],
})
