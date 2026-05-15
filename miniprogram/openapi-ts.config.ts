import { defineConfig } from '@hey-api/openapi-ts'

/**
 * 小程序 OpenAPI 类型生成 — types only (无 client 生成)
 *
 * 小程序用 uni.request, 不接 axios. 这里只生成数据类型 (interface),
 * 由现有 `src/plugins/inspection/api/inspection.ts` 等手写 wrapper 引用.
 *
 * 工作流: backend 改接口 → openapi.json 更新 → 跑 `npm run openapi:generate`
 * → miniprogram/src/api-generated/types.gen.ts 同步.
 */
export default defineConfig({
  input: '../backend/openapi.json',
  output: {
    path: 'src/api-generated',
    lint: false,
    clean: true,
  },
  // 只生成 typescript 类型, 不生成 SDK client / 不绑定 axios
  plugins: ['@hey-api/typescript'],
})
