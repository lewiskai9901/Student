# 小程序插件作者指南

> Phase A,schemaVersion = 1。本文档配合 `docs/plans/2026-05-04-miniprogram-architecture-design.md` 阅读。

## 5 分钟新建插件

### 1. 建目录

```
src/plugins/<your-key>/
├── manifest.ts
└── pages/
    └── main.vue
```

### 2. 写 manifest

```ts
// src/plugins/<your-key>/manifest.ts
import { definePlugin } from '@core/plugin/package'

export default definePlugin({
  key: '<your-key>',
  label: '你的插件',
  schemaVersion: 1,
  minCoreVersion: '0.1.0',
  contributions: [
    { type: 'permission', code: '<your-key>:main:view', description: '主页访问' },
    {
      type: 'menu',
      key: '<your-key>.main',
      label: '主入口',
      icon: 'm',
      path: '/plugins/<your-key>/pages/main',
      perm: '<your-key>:main:view',
      order: 1,
      group: 'home-grid'
    },
    {
      type: 'route',
      path: 'plugins/<your-key>/pages/main',
      inSubPackage: true,
      perm: '<your-key>:main:view'
    }
  ],
  subPackage: { root: 'plugins/<your-key>', pages: ['pages/main'] }
})
```

### 3. 注册

`src/plugins/index.ts`:

```ts
import myPlugin from './<your-key>/manifest'
export const allPlugins = [demo, myPlugin]
```

`src/plugins/manifests.json` 加一份等价 JSON(参考 `demo` 块复制改字段)。

### 4. 验证

```bash
npm run ci
```

绿了就能在微信开发者工具看到入口。

---

## 8 种 Contribution 速查

| Type | 用途 | 必填字段 |
|---|---|---|
| `menu` | 首页宫格 / 我的页扩展入口 | key, label, icon, path, order |
| `route` | 插件页面声明 | path, inSubPackage |
| `permission` | 声明本插件需要的权限码 | code, description |
| `message-handler` | 消息中心按 category 分发渲染 | category, render |
| `scan-resolver` | 扫码后按前缀路由 | prefix, resolve, priority |
| `subscribe-template` | 微信订阅消息模板 | templateId, scenario, description |
| `config-schema` | 插件运行期配置项(后台可视化) | schema |
| `event` | 声明本插件发布的事件类型 | eventName, payloadSchema |

类型源码:`src/core/plugin/contribution.ts`。**8 种是 sealed**;新增需扩 sealed union。

---

## 跨插件通信

**不要 import 别的插件代码**(ESLint 会报错)。用 EventBus:

```ts
// 你的插件订阅
const r = usePluginRegistry()
r.bus.on('inspection.task.submitted', (payload) => {
  // 处理
})

// 你的插件发布
r.bus.emit('<your-key>.entity.verb', { ... })
```

事件名强制 `<plugin>.<entity>.<verb>` 三段格式。Bus 是单例,所有插件共享。

声明事件契约:

```ts
{ type: 'event', eventName: '<your-key>.entity.verb', payloadSchema: {
  type: 'object',
  properties: { id: { type: 'number' } },
  required: ['id']
}}
```

---

## 调用平台能力

**不要直接 `uni.scanCode/getLocation/chooseImage/uploadFile/...`**(ESLint 会报错)。

```ts
// ❌ 错
uni.scanCode({ success: r => ... })

// ✅ 对
import { capability } from '@core/platform/auto'
const r = await capability.scan()
```

提供能力:

| 方法 | 说明 |
|---|---|
| `capability.scan(opts?)` | 扫码 |
| `capability.getLocation(opts?)` | 定位 |
| `capability.takePhoto(opts)` | 拍照(camera/album) |
| `capability.uploadFile(file, opts)` | 上传 |
| `capability.requestSubscribeMessage(ids)` | 订阅消息授权 |
| `capability.storage.{get,set,remove}` | 持久化存储 |
| `capability.systemInfo()` | 系统信息 |

完整签名:`src/core/platform/capability.ts`。

---

## 权限驱动菜单

`MenuContribution.perm` 字段引用一个权限码。该权限必须由本插件的 `PermissionContribution` 声明,否则 `verify-manifest` 报错。

登录后端返回的 `permissions[]` 决定菜单可见性:权限缺失则菜单不渲染,即便插件已激活。

---

## 插件激活模型

1. 启动时 `main.ts` 把 `allPlugins` 注册到 dispatcher
2. 用户登录后,`useAuth().enabledPlugins` 决定哪些插件激活
3. `usePluginRegistry().activate()` 调每个 active 插件的 `bootstrap()`
4. 业务系统(首页菜单 / 消息中心 / 扫码 / 路由)通过 `dispatcher.query<T>(type)` 拿数据

`enabled?(ctx)` 可加运行期门:返回 false 则该插件即便在 enabledPlugins 里也不激活。

---

## CI 守护

提交前必跑 `npm run ci`,违规规则:

| 规则 | 说明 |
|---|---|
| `mp-boundary/no-core-importing-plugin` | core 不得 import plugins |
| `mp-boundary/no-cross-plugin-import` | 插件 A 不得 import 插件 B |
| `mp-boundary/no-bare-uni-api` | 插件不得裸调受限 uni.* |
| `mp-boundary/no-business-in-core` | core 不得出现行业字面量 |

---

## 常见问题

**Q: 如何调试单个插件?**
A: 改 `enabledPlugins` 数组(可在登录响应里 mock,或修改 `useAuth` 默认值)。

**Q: subPackage 的 pages 路径怎么写?**
A: 相对 `subPackage.root`。例如 `subPackage = { root: 'plugins/demo', pages: ['pages/hello'] }`,实际页面文件在 `src/plugins/demo/pages/hello.vue`,uni-app 跳转用 `uni.navigateTo({ url: '/plugins/demo/pages/hello' })`。

**Q: 我的插件需要新 Contribution 类型怎么办?**
A: Phase A schemaVersion=1 锁定 8 种。新增需要扩 sealed union(改 contribution.ts + dispatcher.ts + verify-manifest.js),并升 schemaVersion 到 2。请提架构 RFC。

**Q: 主包超 2MB 怎么办?**
A: 主包只能放 core/。所有插件代码都进 subPackage。core 自己超 2MB 时需要拆分(Phase B 议题)。
