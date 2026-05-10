/** @type {import('dependency-cruiser').IConfiguration} */
module.exports = {
  forbidden: [
    {
      // TODO(F4): router/index.ts 1212 行集中懒加载导致 api → utils/request → router → views → api 循环.
      // F4 把 router 拆 plugin manifest 后, 把这条规则升回 error.
      name: 'no-circular',
      severity: 'warn',
      comment: '循环依赖让重构变 NP-hard (本期降 warn, F4 router 拆分后升回 error)',
      from: {},
      to: { circular: true }
    },
    {
      name: 'no-orphans',
      severity: 'warn',
      comment: '孤儿模块说明可能是死代码',
      from: {
        orphan: true,
        pathNot: [
          '\\.d\\.ts$',
          '(^|/)tsconfig\\.[^/]+\\.json$',
          '(^|/)\\.eslintrc\\.[^/]+(\\.js|\\.cjs)?$',
          '(^|/)vite\\.config\\.ts$',
          '(^|/)vitest\\.config\\.ts$',
          '(^|/)playwright\\.config\\.[jt]s$',
          '(^|/)main\\.ts$',
          '(^|/)App\\.vue$',
          'src/__tests__/'
        ]
      },
      to: {}
    },
    {
      // TODO: utils/request.ts 通过 stores/auth 拿 token. 业界常见但确实违反方向.
      // 可在后续 refactor 中改成 inject token getter, 解耦 store. 本期降 warn.
      name: 'utils-no-business-import',
      severity: 'warn',
      comment: 'src/utils 不应反引业务 (request.ts → stores/auth 是已知豁免, TODO 后续解耦)',
      from: { path: '^src/utils/' },
      to: { path: '^src/(views|stores|api/(?!common))' }
    },
    {
      name: 'composables-no-views-import',
      severity: 'error',
      comment: 'composables 应被 views 用,不能反引 views',
      from: { path: '^src/composables/' },
      to: { path: '^src/views/' }
    },
    {
      // main.ts 引 element-plus/theme-chalk + es/locale 是官方推荐用法, 给 main.ts 例外.
      // src 其余位置仍禁 deep import.
      name: 'no-direct-element-plus-deep-import',
      severity: 'error',
      comment: 'Element Plus 用顶层 import { ElButton },不要 deep import 内部 (main.ts 主题 + locale 例外)',
      from: {
        path: '^src/',
        pathNot: '^src/main\\.ts$'
      },
      to: { path: 'element-plus/(es|lib|theme-chalk)/(?!index)' }
    },
    {
      name: 'views-cant-cross-import-stores',
      severity: 'warn',
      comment: '视图最好通过 composable 间接用 store, 不要直接 import 跨业务的 store',
      from: { path: '^src/views/([^/]+)/' },
      to: {
        path: '^src/stores/(?!\\1)',
        pathNot: [
          '^src/stores/auth\\.',
          '^src/stores/sidebar\\.',
          '^src/stores/theme\\.',
          '^src/stores/locale\\.'
        ]
      }
    }
  ],
  options: {
    doNotFollow: { path: 'node_modules' },
    tsPreCompilationDeps: true,
    tsConfig: { fileName: 'tsconfig.json' },
    enhancedResolveOptions: {
      exportsFields: ['exports'],
      conditionNames: ['import', 'require', 'node', 'default'],
      mainFields: ['module', 'main']
    }
  }
}
