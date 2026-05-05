# 小程序端架构设计 — 真 A+ 完美版

> 日期: 2026-05-04
> 作者: Claude (架构对齐) / 用户 (决策)
> 目标: 把 miniprogram 端按后端真 A+ 同等标准建设,一次到位,不留架构债
> 关联: `2026-04-04-miniprogram-design.md` (UI 风格 + Phase 1 范围保留)
>       `2026-04-19-phase2-unified-spi.md` (后端 sealed Contribution 契约)
>       `2026-04-20-core-purification-to-aplus.md` (后端 core 纯净化)
>       `2026-04-19-phase4a-dynamic-route-registration.md` (H5 动态路由)

---

## 0. 背景与定位

**当前状态**
- 脚手架已建 (commit `40672b2b`): uni-app + Vue 3 + TS + wot-design-uni + Pinia + z-paging
- 现有 4 个空壳页: `login / index / message / mine`
- 业务代码尚未编写——**这是建立架构的唯一窗口**
- 后端: 真 A+ (PluginPackage + Contribution sealed(8) + ContributionDispatcher + Policy SPI + boundary tests)
- H5: B+ (动态路由 + 条件 import,无 sealed contribution / no capability SPI / no boundary lint)

**本次目标**
- 小程序端建成 **真 A+**: 与后端架构完全对偶,把 H5 端没做完的事在小程序端先做对
- 不追 S/S+: 不做运行时热加载、不做 module federation、不做插件签名沙箱(YAGNI,无商业触发)

**指导原则**
- **核心通用**: `core/` 不含任何业务、不认识任何插件、不写死任何菜单/路由
- **插件自治**: `plugins/<key>/` 持有自己的 manifest / pages / api / stores,通过 sealed Contribution 契约向 core 注册
- **构建期校验**: 编译期类型 + ESLint 边界 lint + manifest schema 校验,违规直接挂 CI
- **跨平台**: 通过 PlatformCapability SPI 隔离 `uni.*` API,未来可换支付宝/抖音/H5

---

## 1. 顶层目录结构

```
miniprogram/
├── scripts/
│   ├── build-pages-json.ts         # 构建期: 扫 manifest 合并 pages.json
│   ├── lint-plugin-boundaries.ts   # ESLint 自定义规则: 边界守护
│   └── verify-manifest.ts          # CI: 校验所有 manifest 合规
├── src/
│   ├── core/                       # 通用核心 (主包,不含业务)
│   │   ├── plugin/                 # 插件平台 SPI
│   │   │   ├── contribution.ts     # sealed Contribution 联合类型
│   │   │   ├── package.ts          # PluginPackage 契约
│   │   │   ├── dispatcher.ts       # ContributionDispatcher (单一收口)
│   │   │   ├── registry.ts         # 各 Contribution 子注册表
│   │   │   ├── event-bus.ts        # 跨插件事件总线
│   │   │   └── context.ts          # PluginContext (运行期状态)
│   │   ├── platform/               # 平台能力 SPI (六边形端口)
│   │   │   ├── capability.ts       # PlatformCapability 接口
│   │   │   ├── weixin.ts           # 微信小程序实现
│   │   │   ├── alipay.ts           # 支付宝实现 (预留)
│   │   │   └── h5.ts               # H5 实现 (预留)
│   │   ├── api/                    # 通用 API: auth / menu / message / user / file
│   │   ├── stores/                 # auth / plugin-registry / capability
│   │   ├── components/             # PluginEntry / PermGuard / GridMenu / AppHeader / ...
│   │   ├── utils/                  # request / perm / logger / error
│   │   ├── pages/                  # 主包页面: login / index / message / mine
│   │   └── types/                  # 通用类型
│   ├── plugins/
│   │   ├── inspection/             # 检查 + 整改插件 (业务最重)
│   │   │   ├── manifest.ts         # PluginPackage 实现
│   │   │   ├── pages/
│   │   │   ├── api/
│   │   │   ├── stores/
│   │   │   └── components/
│   │   ├── healthcare/             # 桩插件 (验证扩展性,2 页 + 1 事件)
│   │   │   ├── manifest.ts
│   │   │   └── pages/
│   │   └── demo/                   # 最小骨架插件 (1 页,自测用)
│   │       └── manifest.ts
│   ├── App.vue
│   ├── main.ts                     # 启动: capability 注入 + plugin bootstrap
│   ├── pages.json                  # 构建期由 scripts 生成,人勿改
│   ├── manifest.json
│   └── uni.scss
├── docs/
│   └── plugin-author-guide.md      # 给插件作者看的契约文档
└── package.json
```

**新规则 (与后端 ArchUnit 对偶)**
- `core/` 禁止 import `plugins/*`
- `plugins/A/` 禁止 import `plugins/B/*` (跨插件只能走 EventBus + Contribution)
- 所有 `uni.scanCode / uni.getLocation / uni.chooseImage / uni.requestSubscribeMessage` 必须通过 `capability`,禁止裸调
- 业务页面必须放在 `plugins/<key>/pages/`,禁止放 `core/pages/` 或裸 `pages/`

---

## 2. Plugin Platform SPI (核心契约)

### 2.1 Contribution sealed 联合类型

对偶后端 `Contribution sealed(8)`,前端用 TypeScript discriminated union 实现"编译期穷举校验":

```ts
// core/plugin/contribution.ts
export type Contribution =
  | MenuContribution
  | RouteContribution
  | PermissionContribution
  | MessageHandlerContribution
  | ScanResolverContribution
  | SubscribeMessageContribution
  | ConfigSchemaContribution
  | EventContribution

// 1. 菜单贡献 (首页宫格 + 自定义入口)
export interface MenuContribution {
  type: 'menu'
  key: string                           // 全局唯一: <plugin>.<menu>
  label: string
  icon: string                          // wot icon name 或 url
  path: string                          // 跳转路径
  perm?: string                         // 所需权限码
  order: number                         // 排序
  group?: 'home-grid' | 'mine-extra'    // 渲染槽位
  badge?: () => Promise<number>         // 红点数 (可选)
}

// 2. 路由贡献 (插件页面)
export interface RouteContribution {
  type: 'route'
  path: string                          // 完整路径: plugins/<key>/pages/xxx
  perm?: string
  meta?: { title?: string; navStyle?: 'custom' | 'default' }
  inSubPackage: true                    // 强制进分包,主包瘦身
}

// 3. 权限贡献 (声明本插件需要的权限码,与后端权限注册表对齐)
export interface PermissionContribution {
  type: 'permission'
  code: string                          // 如 'inspection:task:view'
  description: string
}

// 4. 消息处理器贡献 (消息中心按 category 分发)
export interface MessageHandlerContribution {
  type: 'message-handler'
  category: string                      // 后端消息 category 字段
  render: (msg: BizMessage) => MessageRendered  // 自定义图标/标题/跳转
}

// 5. 扫码解析器贡献 (扫码后路由分发)
export interface ScanResolverContribution {
  type: 'scan-resolver'
  prefix: string                        // 如 'INSPECTION:TASK:' / 'PATIENT:'
  resolve: (raw: string) => ScanAction  // { path, params } | { decline }
  priority: number
}

// 6. 微信订阅消息贡献 (插件声明需要的模板 ID)
export interface SubscribeMessageContribution {
  type: 'subscribe-template'
  templateId: string
  scenario: string                      // 'inspection.task.assigned' 等
  description: string
}

// 7. 配置 Schema 贡献 (插件运行期配置项,未来后台可视化)
export interface ConfigSchemaContribution {
  type: 'config-schema'
  schema: JsonSchema
}

// 8. 事件贡献 (声明本插件发布的事件类型,供其它插件订阅时获得类型提示)
export interface EventContribution {
  type: 'event'
  eventName: string                     // <plugin>.<entity>.<verb>
  payloadSchema: JsonSchema
}
```

### 2.2 PluginPackage 契约

```ts
// core/plugin/package.ts
export interface PluginPackage {
  readonly key: string                  // 全局唯一: 'inspection' / 'healthcare'
  readonly label: string
  readonly schemaVersion: 1             // 当前契约版本,未来升 v2 走迁移
  readonly minCoreVersion: string       // semver,加载时校验
  readonly contributions: readonly Contribution[]
  readonly enabled?: (ctx: PluginContext) => boolean  // 运行期开关
  readonly bootstrap?: (ctx: PluginContext) => void   // 启动钩子 (订阅事件等)
  readonly subPackage?: SubPackageConfig              // 分包路径 (构建期用)
}

export function definePlugin(pkg: PluginPackage): PluginPackage {
  // 编译期辅助 + 运行期 schema 校验
  return Object.freeze(pkg)
}
```

### 2.3 ContributionDispatcher (单一收口)

对偶后端 `ContributionDispatcher @Order(60)`:

```ts
// core/plugin/dispatcher.ts
class ContributionDispatcher {
  private readonly registries = new Map<ContributionType, ContributionRegistry>()
  private readonly plugins = new Map<string, PluginPackage>()

  register(plugin: PluginPackage): void {
    this.verifyCompatibility(plugin)        // schemaVersion + minCoreVersion
    this.detectConflicts(plugin)            // menu key / route path / scan prefix 唯一性
    for (const c of plugin.contributions) {
      this.getRegistry(c.type).add(plugin.key, c)
    }
    this.plugins.set(plugin.key, plugin)
  }

  query<T extends Contribution>(type: T['type']): T[] {
    return this.getRegistry(type).all() as T[]
  }

  queryFiltered<T extends Contribution>(
    type: T['type'],
    filter: (c: T, ctx: PluginContext) => boolean
  ): T[] { ... }
}

export const dispatcher = new ContributionDispatcher()
```

**关键**: 业务系统(首页菜单、消息中心、扫码、路由守卫) **只问 dispatcher 要数据**,不直接读 manifest。新增 Contribution 类型时,只动 dispatcher,不动业务系统。

### 2.4 PluginContext

```ts
// core/plugin/context.ts
export interface PluginContext {
  readonly tenantPlugins: readonly string[]   // 租户启用的插件 key
  readonly permissions: readonly string[]     // 当前用户权限码
  readonly user: UserInfo
  readonly capability: PlatformCapability
  readonly bus: PluginEventBus
}
```

由 core 在登录后构建,传给所有 `enabled()` / `bootstrap()` 调用。

### 2.5 PluginEventBus (跨插件通信)

对偶后端 `domain_events`:

```ts
// core/plugin/event-bus.ts
export interface PluginEventBus {
  emit<T>(eventName: string, payload: T): void
  on<T>(eventName: string, handler: (payload: T) => void): Unsubscribe
  off(eventName: string, handler: Function): void
}
```

**约束**:
- 事件名必须以插件 key 开头: `inspection.task.submitted`
- 跨插件通信**只能**走 bus,直接 import 被 ESLint 拦截
- bus 不持久化,纯内存,同 tab 内有效

---

## 3. Platform Capability SPI (六边形端口)

```ts
// core/platform/capability.ts
export interface PlatformCapability {
  // 扫码
  scan(opts?: ScanOpts): Promise<ScanResult>

  // 定位
  getLocation(opts?: LocationOpts): Promise<Location>

  // 拍照 (带水印能力)
  takePhoto(opts: PhotoOpts): Promise<LocalFile>

  // 文件上传 (走 core/api/file)
  uploadFile(file: LocalFile, opts: UploadOpts): Promise<RemoteFile>

  // 订阅消息 (微信特有,其它平台 noop)
  requestSubscribeMessage(templateIds: string[]): Promise<SubscribeResult>

  // 存储
  storage: KVStorage

  // 系统信息
  systemInfo(): SystemInfo

  // 平台标识
  readonly platform: 'mp-weixin' | 'mp-alipay' | 'mp-toutiao' | 'h5' | 'app'
}

// core/platform/weixin.ts
export const weixinCapability: PlatformCapability = { ... }
```

**注入**: `main.ts` 根据编译时 `process.env.UNI_PLATFORM` 选择实现,挂到 PluginContext。

**禁令**: 插件代码 **禁止 import `uni`**, ESLint `no-restricted-imports` 拦截。

---

## 4. 构建期机制

### 4.1 pages.json 自动生成

```ts
// scripts/build-pages-json.ts
async function build() {
  const corePages = readCorePages()          // core/pages/* 主包
  const pluginManifests = await scanPlugins() // plugins/*/manifest.ts

  const subPackages = pluginManifests
    .filter(p => p.subPackage)
    .map(p => ({
      root: `plugins/${p.key}`,
      pages: extractPagePaths(p.contributions.filter(c => c.type === 'route'))
    }))

  const tabBar = buildTabBar(corePages)      // 主 tabbar 仍 core 提供
  const preloadRule = buildPreloadRule(pluginManifests)  // 分包预下载策略

  writeFile('src/pages.json', { pages: corePages, subPackages, tabBar, preloadRule, ... })
}
```

- 在 `vite.config.ts` 的 `buildStart` 钩子里跑
- 增量缓存: manifest 未变不重写
- 主包目标 < 1.5MB,留 0.5MB 给 core 演进

### 4.2 边界 Lint (ESLint 自定义规则)

```ts
// scripts/lint-plugin-boundaries.ts (eslint-plugin-mp-boundary)
rules:
  no-core-importing-plugin     // core/** 禁止 import plugins/**
  no-cross-plugin-import       // plugins/A/** 禁止 import plugins/B/**
  no-bare-uni-api              // 禁止裸调 uni.scanCode/getLocation/chooseImage/...
  manifest-must-define-plugin  // manifest.ts 必须 export default definePlugin(...)
  page-must-belong-to-plugin   // 业务页面必须在 plugins/<key>/pages/ 下
  no-business-in-core          // core/ 不得出现行业字面量 (黑名单: STUDENT/PATIENT/...)
```

CI 跑 `pnpm lint`,违规挂。这是后端 `NoIndustryTypeLiteralInCoreTest` / `NoBypassAuthServiceTest` 的前端对偶。

### 4.3 Manifest Schema 校验

```ts
// scripts/verify-manifest.ts
- 全局唯一: plugin.key / menu.key / route.path / scan.prefix
- schemaVersion 和 minCoreVersion 兼容
- 引用的 perm code 必须有对应 PermissionContribution 声明
- subscribe-template 的 scenario 必须有对应 EventContribution 声明
```

CI 跑,违规挂。

---

## 5. 运行期机制

### 5.1 启动顺序

```ts
// main.ts
import { createSSRApp } from 'vue'
import { capability } from './core/platform/auto'
import { dispatcher } from './core/plugin/dispatcher'
import { allPlugins } from './plugins/index'   // 由构建期生成的索引

export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())

  // 1. 平台能力注入
  app.provide('capability', capability)

  // 2. 插件注册 (此时尚未登录,enabled 检查推迟)
  for (const p of allPlugins) {
    dispatcher.register(p)
  }

  return { app }
}
```

### 5.2 登录后激活

```ts
// core/stores/plugin-registry.ts
async function activate(user, permissions, tenantPlugins) {
  const ctx: PluginContext = { user, permissions, tenantPlugins, capability, bus }

  // 过滤启用的插件
  const active = dispatcher.allPlugins().filter(p =>
    tenantPlugins.includes(p.key) && (p.enabled?.(ctx) ?? true)
  )

  // 调 bootstrap (订阅事件等)
  for (const p of active) await p.bootstrap?.(ctx)

  this.activePlugins = active
}
```

### 5.3 首页菜单渲染

```ts
// core/pages/index/index.vue
const menus = computed(() =>
  dispatcher.query<MenuContribution>('menu')
    .filter(m => activePluginKeys.has(m.key.split('.')[0]))
    .filter(m => !m.perm || hasPerm(m.perm))
    .filter(m => m.group === 'home-grid')
    .sort((a, b) => a.order - b.order)
)
```

**core 不认识任何具体菜单**,只按 dispatcher 数据渲染。

### 5.4 消息中心分发

```ts
// core/pages/message/index.vue
function renderMessage(msg: BizMessage) {
  const handler = dispatcher.query<MessageHandlerContribution>('message-handler')
    .find(h => h.category === msg.category)
  return handler ? handler.render(msg) : defaultRender(msg)
}
```

**core 不认识 inspection 消息长什么样**,inspection 插件自己说。

### 5.5 扫码分发

```ts
// core/utils/scan.ts (插件公共调)
export async function scan() {
  const result = await capability.scan()
  const resolvers = dispatcher.query<ScanResolverContribution>('scan-resolver')
    .sort((a, b) => b.priority - a.priority)
  for (const r of resolvers) {
    if (result.code.startsWith(r.prefix)) {
      const action = r.resolve(result.code)
      if (action) return uni.navigateTo({ url: action.path })
    }
  }
  uni.showToast({ title: '无法识别的二维码', icon: 'none' })
}
```

### 5.6 路由守卫

```ts
// core/utils/route-guard.ts (uni-app interceptor)
uni.addInterceptor('navigateTo', {
  invoke({ url }) {
    const route = dispatcher.query<RouteContribution>('route').find(r => url.startsWith(r.path))
    if (route?.perm && !hasPerm(route.perm)) {
      uni.showToast({ title: '无权限', icon: 'none' })
      return false
    }
    return true
  }
})
```

---

## 6. 后端协议小口子 (估 1-2 天)

### 6.1 登录响应增强

```diff
POST /api/auth/login → {
  accessToken, refreshToken, user,
  permissions: ['inspection:task:view', ...],
+ enabledPlugins: ['inspection', 'message'],     // 租户启用的插件 key
+ pluginConfig: { inspection: {...} }             // 插件配置 (来自 ConfigSchemaContribution)
}
```

字段从 **租户配置表** 读取(后端已具备 manifest/PluginPackage 概念,补一个 `tenant_plugins` 表即可)。

### 6.2 微信登录 (MVP 可选)

```
POST /api/auth/wechat-login   { code }                  → 已绑定: tokens / 未绑定: bindToken
POST /api/auth/bind-wechat    { bindToken, username, password } → tokens
```

MVP 第一版用账号密码即可,微信登录 Phase 2。

### 6.3 业务消息按 category 下发

后端 `notifications` 表已有 `category` 字段(见 messaging-to-aplus),小程序消息中心直接按 category 分发到 `MessageHandlerContribution`。

### 6.4 检查/整改 mobile 端 API

复用现有:
- `GET /api/inspection/tasks?assignee=me` (已有)
- `GET /api/inspection/cases?assignee=me` (已有)
- `POST /api/inspection/tasks/{id}/submit` (已有)
- 不需要专门搞 mobile 网关

---

## 7. MVP 实施分期

### Phase A: 平台骨架 (3-4 天) — 必须一次到位

**目标**: 跑通 core + demo 插件,所有 SPI 落地,守护规则上线

- [ ] `core/plugin/*`: contribution / package / dispatcher / registry / event-bus / context
- [ ] `core/platform/*`: capability 接口 + weixin 实现
- [ ] `core/api/auth.ts`: 登录 + 401 自动刷新
- [ ] `core/stores/auth.ts` + `core/stores/plugin-registry.ts`
- [ ] `core/components/`: PluginEntry / PermGuard / GridMenu / AppHeader
- [ ] `core/pages/`: login / index (空槽位) / message (空槽位) / mine
- [ ] `scripts/build-pages-json.ts` + vite 集成
- [ ] `scripts/lint-plugin-boundaries.ts` (ESLint plugin) + CI 接入
- [ ] `scripts/verify-manifest.ts` + CI 接入
- [ ] `plugins/demo/manifest.ts`: 1 个菜单 + 1 个页面 (自测插件骨架)
- [ ] **架构守护测试**: 写 5 个反例 (core import plugin / 跨插件 import / 裸调 uni / 行业字面量 / 业务页放 core),全部应被 lint 拦截

### Phase B: 扩展性验证 (1-2 天)

**目标**: 第二个插件证明扩展点工作,事件 + 分包 + capability 全跑过

- [ ] `plugins/healthcare/manifest.ts`: 2 页 + 1 事件 + 1 扫码 prefix `PATIENT:`
- [ ] 扫码触发跨插件事件: healthcare 收到 `PATIENT:` → emit `healthcare.patient.scanned` → demo 插件订阅打 log
- [ ] 验证分包按需加载,主包不含 healthcare 代码
- [ ] 验证 PluginContext.enabled() 关掉 healthcare 后菜单/路由全消失

### Phase C: Inspection 插件 (1-2 周)

**目标**: 真实业务接入,验证 SPI 在压力下不漏

**核心页面**
- [ ] 我的检查任务列表 + 筛选 (z-paging)
- [ ] 任务执行: 项目列表 → 打分 (评分套件按模板 schema 渲染) → 拍照水印
- [ ] 任务提交 + 提交后跳转
- [ ] 我的整改单列表 + 筛选
- [ ] 整改处理详情 + 上传证据 + 提交复核
- [ ] 申诉提交对话框 + 我的申诉列表

**Contribution 注册**
- [ ] 4 MenuContribution: 我的任务 / 我的整改 / 扫码 / 我的申诉
- [ ] 8+ RouteContribution
- [ ] 12+ PermissionContribution
- [ ] 2 ScanResolverContribution: `INSPECTION:TASK:` / `INSPECTION:OBJECT:`
- [ ] 3 MessageHandlerContribution: 任务派发 / 整改提醒 / 申诉结果
- [ ] 3 SubscribeMessageContribution: 任务派发 / 整改到期 / 复核结果
- [ ] 5 EventContribution: task.submitted / case.processed / appeal.created 等

### Phase D: 微信登录 + 订阅消息 (2-3 天,可推迟)

- [ ] 后端 `wechat-login / bind-wechat`
- [ ] 前端微信授权流程 + 绑定页
- [ ] 订阅消息申请时机 (任务提交后申请下次任务派发模板)

### Phase E: 文档 + 守护 + ADR (1 天)

- [ ] `docs/plugin-author-guide.md`: 给插件作者看的契约文档 (照后端 plugin-extension-catalog 风格)
- [ ] ADR: `docs/adr/adr-0XX-miniprogram-plugin-architecture.md`
- [ ] CI 跑通: type-check + boundary lint + manifest verify

---

## 8. YAGNI 清单 (主动不做)

- ❌ **运行时插件热加载** — 微信小程序不允许动态 import 远程包,做不了,且无业务触发
- ❌ **Module Federation** — H5 端 POC 已验证 ROI 负 (见 `poc-module-federation-findings.md`),小程序更不可行
- ❌ **插件签名 / 沙箱** — 单一团队,无第三方插件 marketplace
- ❌ **多租户构建期裁剪出多个包** — 运行期 enabledPlugins[] 已够,免得 N 个租户 N 个包
- ❌ **跨插件直接调用 API** — 必须经 EventBus,即便慢一拍也不破边界
- ❌ **Plugin DSL** — 用 TypeScript 直接写 manifest,不发明 YAML/JSON DSL

触发条件 (出现任一则启动): 第三方开发者生态 / 独立分发 / 插件商店 / 多租户独立部署。

---

## 9. 与后端架构的对偶关系

| 后端 | 小程序 | 说明 |
|------|--------|------|
| `PluginPackage` | `PluginPackage` | 1:1 |
| `Contribution sealed(8)` | `Contribution` discriminated union | 8 种,前端略不同 (前端有 ScanResolver/SubscribeMessage,后端有 TriggerPoint/EventType) |
| `ContributionDispatcher @Order(60)` | `dispatcher.ts` | 1:1,单一收口 |
| `Policy<T>` SPI | (暂不需要,前端无业务规则评估) | 如未来要前端校验业务规则再加 |
| `domain_events` | `PluginEventBus` | 内存版 |
| `NoIndustryTypeLiteralInCoreTest` | `no-business-in-core` ESLint | 1:1 |
| `NoBypassAuthServiceTest` | `no-bare-uni-api` ESLint | 形似,目的都是封装边界 |
| 六边形端口适配器 | `PlatformCapability` | 1:1 |
| `schemaVersion` SPI | `PluginPackage.schemaVersion` | 1:1 |
| `plugins/healthcare/` 桩验证 | `plugins/healthcare/` 桩验证 | 同等做法 |

---

## 10. 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| pages.json 构建期生成,IDE/HMR 兼容 | dev 体验下降 | 在 `vite.config.ts` 的 `configResolved` + 文件监听里跑,改 manifest 即重生 |
| 主包超 2MB | 不能上传 | core 严格管控依赖,wot-design-uni 按需 import,大资源拆分包 |
| 分包加载延迟 | 首次进入慢 | `preloadRule` 配置预下载,登录后预拉常用插件 |
| ESLint 自定义规则维护成本 | 团队学习曲线 | 写在 `scripts/eslint-rules/` 内联,不发独立包;PR 模板加 checklist |
| 后端 enabledPlugins 字段未就绪 | 运行期开关失效 | Phase A 默认全开,Phase D 接后端字段 |
| 跨插件事件类型不安全 | 运行时漏洞 | EventContribution 声明 payloadSchema,bus.emit 编译期类型推断 + 运行期 schema 校验 |

---

## 11. 验收标准

**A 级 (必达)**
- [ ] `core/` 0 个 import 指向 `plugins/*` (lint 校验)
- [ ] 任意插件可由 `enabledPlugins[]` 关闭,关闭后菜单/路由/消息处理器全部隐藏
- [ ] healthcare 桩插件证明: 添加新插件不动 core 任何文件
- [ ] type-check + boundary lint + manifest verify 全绿,纳入 CI
- [ ] 主包 < 1.5MB
- [ ] `docs/plugin-author-guide.md` 写完,新插件作者照文档可独立开发

**B 级 (尽量)**
- [ ] inspection 插件 8+ 页面跑通,提交 + 拍照 + 扫码全闭环
- [ ] 微信订阅消息申请 + 后端推送 + 跳转闭环
- [ ] 跨插件事件演示: 扫码 → inspection task 启动,记录 audit log

**C 级 (可后置)**
- [ ] 微信账号绑定流程
- [ ] healthcare 实际业务页 (本期只是桩)

---

## 12. 决策点 (执行前需用户确认)

1. **Phase A 一次性投入 3-4 天搭骨架,值不值?**
   - 推荐: 值。否则后期补不回来,业务越上越改不动。

2. **healthcare 桩插件保留还是删?**
   - 推荐: **保留在仓库,但 enabled=false**。是活的扩展性证明,删了等于没做。

3. **微信登录 MVP 是否做?**
   - 推荐: **不做**。账号密码先上,微信 Phase D。

4. **首版业务范围是否就 inspection?**
   - 推荐: **是**。业务最熟、整改闭环刚收官、扫码场景最契合,投入 ROI 最高。

5. **要不要在 worktree 里做?**
   - 推荐: **要**。骨架 + 业务总变更面大,主分支保护。

---

## 附: 关键文件清单 (Phase A 完成后)

```
miniprogram/
├── scripts/
│   ├── build-pages-json.ts
│   ├── verify-manifest.ts
│   └── eslint-rules/
│       ├── no-core-importing-plugin.js
│       ├── no-cross-plugin-import.js
│       ├── no-bare-uni-api.js
│       ├── manifest-must-define-plugin.js
│       └── no-business-in-core.js
├── src/core/plugin/
│   ├── contribution.ts          (8 sealed types, ~150 行)
│   ├── package.ts               (PluginPackage + definePlugin, ~50 行)
│   ├── dispatcher.ts            (ContributionDispatcher, ~120 行)
│   ├── registry.ts              (各 sub registry, ~80 行)
│   ├── event-bus.ts             (mitt 风格 + schema 校验, ~60 行)
│   └── context.ts               (~30 行)
├── src/core/platform/
│   ├── capability.ts            (接口, ~80 行)
│   ├── weixin.ts                (实现, ~150 行)
│   └── auto.ts                  (按编译时平台选实现, ~20 行)
├── src/plugins/demo/manifest.ts (Phase A 自测)
└── src/plugins/healthcare/manifest.ts (Phase B 验证)
```

总计约 **800 行新增 core 代码** + **~300 行脚本** + **~150 行 ESLint 规则**,3-4 个开发日。

---

## 总结

这是把后端真 A+ 标准 1:1 搬到小程序端的设计:
- Sealed Contribution 契约 + ContributionDispatcher 单一收口
- PlatformCapability 六边形端口隔离 uni.* API
- PluginEventBus 替代 import 实现跨插件通信
- 构建期 lint + manifest verify 守护边界
- healthcare 桩插件证明扩展性
- 与后端 ArchUnit / Policy / Phase 2-4 完全对偶

未来 5 年内,加新业务插件不用动 core,换平台不用动插件,这就是"完美"的工程含义。

下一步:
1. 用户确认 §12 五个决策点
2. 执行 `superpowers:using-git-worktrees` 开 worktree
3. 执行 `superpowers:writing-plans` 把 Phase A 拆成可执行 task list
