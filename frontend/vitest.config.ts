import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  test: {
    // 使用 happy-dom 作为测试环境
    environment: 'happy-dom',

    // 测试文件匹配模式
    include: ['src/**/*.{test,spec}.{js,ts,jsx,tsx}'],

    // 排除文件
    exclude: ['node_modules', 'dist', '.idea', '.git', '.cache'],

    // 全局测试设置
    globals: true,

    // 测试设置文件
    setupFiles: ['./src/__tests__/setup.ts'],

    // 覆盖率配置
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      reportsDirectory: './coverage',
      include: ['src/**/*.{ts,vue}'],
      exclude: [
        'src/**/*.d.ts',
        'src/**/*.test.ts',
        'src/**/*.spec.ts',
        'src/main.ts',
        'src/vite-env.d.ts',
      ],
      // 覆盖率阈值
      thresholds: {
        lines: 20,
        functions: 20,
        branches: 20,
        statements: 20,
      },
    },

    // 测试超时
    testTimeout: 10000,

    // 钩子超时
    hookTimeout: 10000,

    // 并发测试
    pool: 'threads',
    poolOptions: {
      threads: {
        singleThread: true,
      },
    },
  },

  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
