# 通用组织管理平台 - 小程序

插件化小程序端,对偶后端真 A+ 架构。

## 技术栈

- uni-app + Vue 3 + TypeScript + Vite
- Pinia 状态管理
- wot-design-uni UI 组件库
- z-paging 分页组件
- Vitest 单元测试
- ESLint + 自定义 mp-boundary 4 规则

## 快速开始

```bash
npm install
npm run ci              # 跑全套校验 (lint + type-check + verify-manifest + test)
npm run dev:mp-weixin   # 微信小程序开发 (会先跑 build-pages-json)
```

构建产物在 `dist/dev/mp-weixin/`,用微信开发者工具导入。

## 架构

详见 [`../docs/plans/2026-05-04-miniprogram-architecture-design.md`](../docs/plans/2026-05-04-miniprogram-architecture-design.md)。

## 核心规则

- `core/` 不得 import `plugins/*`(违反则 lint error)
- 插件之间不得直接 import,跨插件通信走 `bus` 或 Contribution
- 插件代码不得裸调 `uni.scanCode/getLocation/...`,必须走 `capability`
- `core/` 不得出现行业字面量(`'STUDENT' / 'PATIENT' / 'CLASS' / ...`)
- 业务页面必须放在 `plugins/<key>/pages/`

这 4 条由 `scripts/eslint-rules/` 自定义 ESLint 规则强制守护。

## 增加新插件

1. 在 `src/plugins/` 下建目录 `<your-key>/`
2. 写 `manifest.ts`,导出 `definePlugin({...})`
3. 在 `pages/` 下写 Vue 页面
4. 在 `src/plugins/index.ts` 加入 `allPlugins` 数组
5. 在 `src/plugins/manifests.json` 加 JSON 镜像 (供构建脚本读)
6. 跑 `npm run ci` 校验

详见 [`docs/plugin-author-guide.md`](docs/plugin-author-guide.md)。

## 目录结构

```
miniprogram/
├── scripts/
│   ├── build-pages-json.js     # 构建期生成 src/pages.json
│   ├── verify-manifest.js      # CI 校验插件 manifest 唯一性 + 完整性
│   └── eslint-rules/           # mp-boundary 自定义 ESLint 规则
├── src/
│   ├── core/                   # 通用核心,不含业务
│   │   ├── plugin/             # 插件平台 SPI (Contribution/Package/Dispatcher/EventBus)
│   │   ├── platform/           # 六边形端口 (PlatformCapability + 微信实现)
│   │   ├── api/                # 通用 API (request/auth)
│   │   ├── stores/             # Pinia stores (auth, plugin-registry)
│   │   └── pages/              # 主包页面 (login/index/message/mine)
│   └── plugins/
│       ├── index.ts            # allPlugins 注册表
│       ├── manifests.json      # JSON 镜像(供 Node 脚本)
│       └── demo/               # 演示插件
└── docs/
    └── plugin-author-guide.md
```

## CI 流水线

`npm run ci` 顺序执行:

1. `lint` — ESLint + mp-boundary 边界守护
2. `type-check` — vue-tsc 严格 TS 检查
3. `verify-manifest` — 插件 manifest 唯一性/完整性
4. `test` — Vitest 单测

任何一步失败整体 CI 失败。`npm run dev:mp-weixin` 不会触发 CI,但提交前必须 ci 全绿。
