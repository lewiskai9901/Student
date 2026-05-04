/* eslint-env node */
require('@rushstack/eslint-patch/modern-module-resolution')

module.exports = {
  root: true,
  extends: [
    'plugin:vue/vue3-essential',
    'eslint:recommended',
    '@vue/eslint-config-typescript',
    '@vue/eslint-config-prettier/skip-formatting',
    './.eslintrc-auto-import.json'
  ],
  plugins: ['unused-imports'],
  parserOptions: {
    ecmaVersion: 'latest'
  },
  rules: {
    'vue/no-unused-vars': 'warn',
    'vue/multi-word-component-names': 'warn',
    'vue/valid-v-slot': 'warn',
    '@typescript-eslint/no-unused-vars': 'off',
    'unused-imports/no-unused-imports': 'warn',
    'unused-imports/no-unused-vars': ['warn', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
    'no-empty': ['warn', { allowEmptyCatch: true }],
    'no-inner-declarations': 'warn',
    'no-case-declarations': 'warn',
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off'
  },
  overrides: [
    {
      files: ['*.config.js', '*.config.cjs', '.eslintrc.cjs', 'postcss.config.*'],
      env: { node: true }
    },
    // ─── 插件化架构守护 ───
    // P1.3: core 视图禁止 import 任何 plugins/* 视图
    {
      files: [
        'src/views/inspection/**/*.{ts,vue}',
        'src/views/access/**/*.{ts,vue}',
        'src/views/asset/**/*.{ts,vue}',
        'src/views/event/**/*.{ts,vue}',
        'src/views/message/**/*.{ts,vue}',
        'src/views/my/**/*.{ts,vue}',
        'src/views/organization/**/*.{ts,vue}',
        'src/views/place/**/*.{ts,vue}',
        'src/views/profile/**/*.{ts,vue}',
        'src/views/settings/**/*.{ts,vue}',
        'src/views/system/**/*.{ts,vue}',
      ],
      rules: {
        'no-restricted-imports': ['error', {
          patterns: [
            {
              group: ['@/views/plugins/*', '../plugins/*', '../../plugins/*'],
              message: '通用 (core) 视图不得 import 行业插件 (plugins/*) — 违反插件化边界. 如需共享, 抽到 src/components 或 src/composables.',
            },
          ],
        }],
      },
    },
    // 跨插件禁止: edu 不得 import health, 反之亦然
    {
      files: ['src/views/plugins/edu/**/*.{ts,vue}'],
      rules: {
        'no-restricted-imports': ['error', {
          patterns: [{
            group: ['@/views/plugins/health/*', '../health/*', '../../health/*'],
            message: 'EDU 插件视图不得 import HEALTH 插件 — 跨插件边界违规.',
          }],
        }],
      },
    },
    {
      files: ['src/views/plugins/health/**/*.{ts,vue}'],
      rules: {
        'no-restricted-imports': ['error', {
          patterns: [{
            group: ['@/views/plugins/edu/*', '../edu/*', '../../edu/*'],
            message: 'HEALTH 插件视图不得 import EDU 插件 — 跨插件边界违规.',
          }],
        }],
      },
    },
  ]
}