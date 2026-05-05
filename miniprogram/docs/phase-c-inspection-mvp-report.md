# Phase C Inspection MVP Report

> 日期: 2026-05-05
> 分支: `feature/miniprogram-phase-c`
> 状态: 全部完成,只读 MVP 闭环

## 范围

第一个真实业务插件接入,**只读浏览** MVP。写路径(打分提交、整改提交、申诉提交、上传)推迟到 Phase D。

## 接入清单

### API(7 read endpoints)

| 端点 | 方法 | 用途 |
|---|---|---|
| `/inspection/tasks/my-tasks` | GET | 我的任务 |
| `/inspection/tasks/{id}` | GET | 任务详情 |
| `/inspection/tasks/available` | GET | 可领任务 |
| `/inspection/corrective-cases/my-cases` | GET | 我的整改 |
| `/inspection/corrective-cases/{id}` | GET | 整改详情 |
| `/inspection/appeals/my` | GET | 我的申诉 |
| `/inspection/appeals/{id}` | GET | 申诉详情 |

### Pages(7)

| 页面 | 路径 | 主要交互 |
|---|---|---|
| 我的任务 | `pages/my-tasks` | 列表 + 状态筛选 chip |
| 任务详情 | `pages/task-detail` | 信息卡片 + Phase D 提交按钮占位 |
| 扫码检查 | `pages/scan-landing` | 调 capability.scan + dispatcher 路由分发 |
| 可领任务 | `pages/available-tasks` | 列表只读 + Phase D 认领按钮占位 |
| 我的整改 | `pages/my-corrections` | 列表 + 状态筛选 + 驳回/升级警示 |
| 整改详情 | `pages/correction-detail` | 问题描述 + 根因 + 时间线 + Phase D 提交占位 |
| 我的申诉 | `pages/my-appeals` | 列表 + 审核状态 + 审核意见高亮 |

### Contributions(总 28)

- **12 PermissionContribution**:`inspection:task:{view,list,claim,start,submit}` + `inspection:correction:{view,list,process,submit}` + `inspection:appeal:{view,list,submit}`
- **5 MenuContribution**(home-grid):我的任务 / 扫码 / 可领任务 / 我的整改 / 我的申诉
- **7 RouteContribution**:对应 7 个 page 路径
- **1 ScanResolverContribution**:`INSPECTION:TASK:` → task-detail
- **3 EventContribution**:`inspection.task.submitted / case.processed / appeal.created`(Phase D 真用)

### Core 改动

```
miniprogram/src/core/api/request.test.ts | 52 ++++
miniprogram/src/core/api/request.ts      | 25 ++
2 files changed, 77 insertions
```

**唯一的 core 改动**:`request.ts` 加了 `requestWrapped<T>()` + `BizError` + `ResultEnvelope<T>` 类型。这是后端 Result envelope 传输约定的封装,不是业务代码,属于核心通用基础设施。

V1 (新插件零侵入) **不严格成立**——本期为后端约定加了基础设施。但意图边界清晰:`requestWrapped` 是任何后端集成都会用到的助手,不是 inspection 业务专属。Phase D 写路径仍 0 改动 core。

## 测试统计

| 阶段 | 测试数 | 增量 |
|---|---|---|
| Phase B 完成 | 59 | - |
| Task 1: requestWrapped + types | 63 | +4 (request envelope) |
| Task 2: manifest | 69 | +6 (manifest contracts) |
| Task 3: API client | 77 | +8 (URL/解包/BizError) |
| Task 4: 4 task pages | 82 | +5 (format helpers) |
| Task 5: 3 corr/appeal pages | 82 | +0 (无新测试) |
| Task 6: 集成测试 | **91** | +9 (扫码 5 + 激活 4) |

`npm run ci` 全程绿(lint + type-check + verify-manifest + 91/91 test)。

## Commits(6 个)

```
255faca3 test(miniprogram): inspection 扫码 + 激活 集成验证
a3e211d9 feat(miniprogram/plugins): inspection 整改+申诉 3 页面
fc28629e feat(miniprogram/plugins): inspection 任务相关 4 页面 + 状态格式化
23441f5d feat(miniprogram/plugins): inspection API client (7 read endpoints)
8b06fa66 feat(miniprogram/plugins): inspection manifest — 12 perm / 5 menu / 7 route / 1 scan / 3 events
b3bc7519 feat(miniprogram/core): Result envelope unwrap + BizError + inspection types
```

## 遇到的问题与解决

1. **Result envelope 解包**:Phase A 的 `request<T>` 直返 `r.data`(整个 envelope),不剥离内层。Phase C 决定不动 `request<T>`(向后兼容),新增 `requestWrapped<T>` 在外面套一层,自动拿 `r.data` 当 envelope 解开;Phase A 的 `auth.ts` 应在 Phase D 一并迁移到 `requestWrapped`。

2. **manifests.json 不能放函数**:`scan-resolver.resolve` 和 `enabled` 在 JSON 镜像里只能 omit,verify-manifest 只校验非函数字段(`prefix`、`priority`)。这是 Phase B 已发现的边界,Phase C 沿用。

3. **Vue 页面单测**:Phase C MVP 不为 Vue 页面写单测——happy-dom + Vue Test Utils + uni-app shim 的搭建成本远超 7 个简单只读页的价值;集成覆盖通过 `__integration__/` 测试 manifest+dispatcher 行为来保证。Phase D 写路径(表单、上传、相机)涉及更复杂逻辑,届时再评估是否引入页面级单测。

4. **lint warnings 增量**:plugins/inspection/ 引入 ~120 条 vue 风格 warning(`vue/singleline-html-element-content-newline` 等),全是 wot-design-uni 模板换行偏好。设计上不想 `--fix` 重写所有页(可能会破坏自动格式与设计一致性)。Phase E 可考虑统一 prettier + eslint --fix 一遍。

## Phase D 预案(写路径)

写路径需要的能力:

### 任务执行

- **打分页面**(替换 task-detail 的 disabled 按钮):
  - 按 template item 渲染评分组件(根据 ScoringPolicy 类型决定:5 分制 / GradeScheme / 利克特等)
  - 每个 item 支持上传照片 + 写文字观察
  - 提交触发 `inspection.task.submitted` 事件 + 调 `/inspection/tasks/{id}/submit`

### 拍照水印

- 在 `core/platform/capability.takePhoto()` 上叠加水印:
  - 时间 + 地点(GPS via capability.getLocation)+ 检查员名
  - 用 canvas 合成,uploadFile 上传
- 这是 capability 层的扩展,不是 inspection 专属(其他插件如 healthcare 也可能用),应放 core/platform。

### 整改提交

- correction-detail 启用提交按钮 → 表单(整改说明 + 上传证据)
- 调 `/inspection/corrective-cases/{id}/submit-correction`
- 触发 `inspection.case.processed` 事件

### 申诉提交

- 在 task-detail 的某个 submission detail 上加"申诉"按钮(MVP 暂不做)
- 申诉表单 + 撤回按钮
- 调 `POST /inspection/appeals` 和 `POST /inspection/appeals/{id}/withdraw`

### 微信订阅消息

- 任务提交后申请下次任务派发模板
- 整改提交后申请复核结果模板
- 调 `capability.requestSubscribeMessage(templateIds)`
- 后端补 `subscribe-template-ids` 配置端点

### 离线缓存(Phase E)

- 弱网现场作业必备
- 用 `capability.storage` 缓存任务详情 + 草稿
- 重连后批量提交

## 经验回填

1. **plugin 改 core 时的判断**:不是"改了 core 就违反 V1"。改 core 是否合理,看是不是**通用约定**。Result envelope 是后端传输标准,所有 API 集成都用,改在 core 合理。如果 inspection 写自己的 axios interceptor 在 plugins/ 里,反而是过度封装。

2. **manifest 元数据完整性的隐藏成本**:JSON 镜像与 TS manifest 必须双写,容易漂移。Phase E 可写一个生成脚本:从 TS 编译输出反推 JSON 镜像,作为 verify-manifest 的输入。

3. **页面级单测 vs 集成测试**:对只读列表/详情页,集成测试(manifest + dispatcher 行为)比页面级单测信息密度更高。形态固定的页面,代码 review 比单测更便宜。Phase D 写路径(状态机、并发、上传重试)再考虑页面级测试。

4. **scan-resolver 优先级**:多插件场景下不同插件可能注册同一前缀(例如 `INSPECTION:` 和 `INSPECTION:TASK:`)。当前 `dispatcher.detectConflicts` 只查重前缀字符串完全一致,不查前缀重叠。Phase D 真有冲突时再加,目前 YAGNI。

5. **`available-tasks` MVP 价值有限**:只读不能领,实际作用是"占位 Phase D 入口"。可以先在 manifest 里隐藏(`enabled` 闭包不展示该菜单),Phase D 启用领取功能后再开。当前为了完整性留着。

## Phase C 验收

- ✅ inspection 插件激活后,首页出现 5 个菜单
- ✅ 7 页面骨架完整,可加载、空态/错误态友好
- ✅ requestWrapped Result envelope 正确解包,BizError 在非 200 时抛出
- ✅ 扫 `INSPECTION:TASK:123` 通过 dispatcher 路由到 task-detail
- ✅ tenantPlugins 不含 inspection 时,菜单/路由全消失
- ✅ core 改动仅 `request.ts` + `request.test.ts`(基础设施 +77 行)
- ✅ `npm run ci` exit 0
- ✅ 测试数 59 → 91(增 32)

下一步:Phase C 合 master → Phase D 写路径(估 3-5 天)。
