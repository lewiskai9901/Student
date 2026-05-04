module.exports = {
  root: true,
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 2020,
    sourceType: 'module',
    extraFileExtensions: ['.vue']
  },
  plugins: ['@typescript-eslint', 'mp-boundary'],
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:vue/vue3-recommended'
  ],
  rules: {
    '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    // any 在测试 mock 与 uni 回调适配处不可避免;架构守护交给 mp-boundary
    '@typescript-eslint/no-explicit-any': 'warn',
    'vue/multi-word-component-names': 'off',
    'mp-boundary/no-core-importing-plugin': 'error',
    'mp-boundary/no-cross-plugin-import': 'error',
    'mp-boundary/no-bare-uni-api': 'error',
    'mp-boundary/no-business-in-core': 'error'
  },
  overrides: [
    {
      // env.d.ts 用 Vue 3 官方模板的 DefineComponent<{}, {}, any> 声明
      files: ['src/env.d.ts'],
      rules: {
        '@typescript-eslint/ban-types': 'off',
        '@typescript-eslint/no-explicit-any': 'off'
      }
    },
    {
      // 测试文件:partial mock 的 `as any` 是惯用法
      files: ['**/*.test.ts', '**/*.spec.ts'],
      rules: {
        '@typescript-eslint/no-explicit-any': 'off',
        '@typescript-eslint/no-unused-vars': 'off'
      }
    },
    {
      // 平台适配层:uni 回调天然 untyped,逐个标 unknown 收益负
      files: ['src/core/platform/**/*.ts'],
      rules: {
        '@typescript-eslint/no-explicit-any': 'off'
      }
    }
  ]
}
