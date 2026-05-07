# Phase E Report — 核心模块浏览(我的资料 + 组织架构 + 通讯录 + 场所)

> 日期: 2026-05-07
> 分支: `feature/miniprogram-phase-e1`(单 worktree 容纳 E-1~E-4)
> 状态: ✅ 完成

## 一句话总结

补齐小程序端**核心层(平台基础)**的 4 个浏览模块,让一个没装任何插件的纯净租户也有可用功能;同时验证 D-1 auth 在多个 GET 端点上跑通。

## 范围

✅ **完成:**
- E-1: 我的资料 — `GET /auth/me` 拉真实数据,显示 phone/email/orgUnitId/tenantId/userTypeCode/roles
- E-2: 组织架构浏览 — `GET /org-units/tree`,递归 flatten + 折叠树
- E-3: 通讯录 — `GET /users/by-org-unit/{id}`,同事列表 + 客户端搜索 + inline 详情
- E-4: 场所浏览 — `GET /v9/places/tree`,场所树 + 容量进度条
- 首页"核心"区 — 3 个 grid item 入口(组织/通讯录/场所)
- auth store 加 `orgUnitId` 字段(为通讯录提供入参)

❌ **不在本期:**
- 用户详情页(详情走 inline 展开)
- 场所详情页 / 居住人/历史
- 组织详情页
- 跨组织搜索通讯录
- 数据权限配置 UI

## 6 commit 序列

| SHA | 说明 |
|---|---|
| `db1a3f5e` | E-1: authApi.me + mine 页拉真实资料 |
| `48609577` | E-2: 组织架构浏览页 + orgApi.tree |
| `a8a492e1` | E-3: 通讯录页 + userApi.byOrgUnit + auth.orgUnitId |
| `457fca45` | E-4: 场所浏览页 + placeApi.tree(含容量进度条) |
| (本次) | E-finalize: 首页核心入口 + 报告 |

## 后端契约对照表

D-3a 已经验过 mvn compile + 单测,现期没新加后端代码,纯前端消费已存在端点:

| 端点 | 用途 | 关键字段 |
|---|---|---|
| `GET /auth/me` | 当前用户详情 | userId, realName, phone, email, avatar, roles, orgUnitId, tenantId, userTypeCode |
| `GET /org-units/tree` | 组织树(全量) | id*, parentId*, unitName, unitType, status, headcount, children |
| `GET /users/by-org-unit/{id}` | 同组织用户列表 | id, realName, phone, email, userType, orgUnitName, roleNames |
| `GET /v9/places/tree?maxDepth=0` | 场所树(全深度) | id, placeName, typeCode, hasCapacity, capacity, currentOccupancy, children |

`*` 标星字段后端用 `@JsonSerialize(using = ToStringSerializer.class)` 序列化为 STRING(防 JS BigInt 精度丢失)— 前端 TS 类型对应用 `string`。其他 id 字段是普通 Long → JS number。

## 测试基线

- 起点:25 文件 / 118 测试(D-3a 收尾)
- 终点:**28 文件 / 126 测试,0 失败**
- 增量:
  - E-1 +1(authApi.me 测试)
  - E-2 +1 文件(orgApi)/ +2 测试
  - E-3 +1 文件(userApi)/ +2 测试
  - E-4 +1 文件(placeApi)/ +3 测试
- type-check + lint 全绿

## 文件清单

### 修改 (3 文件)
- `miniprogram/src/core/api/auth.ts` — `authApi.me()`
- `miniprogram/src/core/api/auth.test.ts` — +1 测试
- `miniprogram/src/core/stores/auth.ts` — 加 orgUnitId 字段(login/logout/state)
- `miniprogram/src/core/pages/mine/index.vue` — 重写,onShow 拉 /auth/me
- `miniprogram/src/core/pages/index/index.vue` — 加"核心" section 3 个入口

### 新建 (8 文件)
- `miniprogram/src/core/api/org.ts` + `org.test.ts`
- `miniprogram/src/core/api/user.ts` + `user.test.ts`
- `miniprogram/src/core/api/place.ts` + `place.test.ts`
- `miniprogram/src/core/pages/org/index.vue`
- `miniprogram/src/core/pages/directory/index.vue`
- `miniprogram/src/core/pages/place/index.vue`

## 设计决策

### 为什么 E-2/E-4 用 flatten + 折叠 而不是递归组件

uni-app + Vue 3 + Vite 自递归组件偶有问题(命名 + script setup 冲突)。flatten 成 list 有几个好处:
1. 可靠 — 列表渲染是最基本的 Vue 操作
2. 性能 — virtual scrolling 未来易接(z-paging 有现成轮子)
3. 可预测 — 调试时能看清节点状态

代价:折叠/展开逻辑要手写(parentChain.every(pid => expanded.has(pid)))。可接受。

### 为什么通讯录用客户端搜索而不是后端 keyword 参数

后端 `/users/by-org-unit/{id}` 的 keyword 参数有 TODO 注释("待 UserApplicationService 扩展支持"),实际后端没用。客户端 filter 在用户数 <500 时无感知,组织通常 <200 人,够用。后端补 keyword 后再无缝切换到服务端搜索。

### 为什么 orgUnitId 进 auth store 而不是 mine 页 local

通讯录(E-3)需要当前用户的 orgUnitId 作为 API 入参。而 mine 页 onShow 才填充这个字段 — 如果用户首次登录后直接进通讯录,orgUnitId 还是 null。所以放 auth store + login 时也填一份(从 D-1 已有的 LoginUserInfo.orgUnitId 取),保证任何路径都能用。

### 首页"核心"区为什么硬编码而不是走 menu 机制

dispatcher 的 menu 机制是给**插件**用的(Contribution registration)。core 入口是平台固定模块,不需要可拔插。硬编码 3 项简单清楚,改动小。如果未来 core 入口超过 5 个或要按权限/角色动态显示,再考虑引入 "core menu" 概念。

## 已知限制 / 真机验证清单

代码层全绿,但以下需真机:

- [ ] 后端启动 + 真实账号登录
- [ ] mine 页:头像 / 姓名 / 手机 / 邮箱 / 组织 ID / 租户 / 角色都正确显示
- [ ] mine 页:切到其他 tab 再回来 → onShow 重新拉 /auth/me 验证
- [ ] 首页:"核心" section 3 个 grid 显示
- [ ] 点"组织架构" → 树渲染,顶层 root 自动展开,点节点 caret 正确折叠
- [ ] 点"通讯录" → 同事列表,点行展开 inline 详情(邮箱/角色),搜索框输入实时过滤
- [ ] 点"场所" → 场所树,有 hasCapacity 的节点显示进度条
- [ ] 网络断开 → toast "加载失败"
- [ ] 401 → reLaunch 登录页(request.ts 的现成行为)

## 现状全景

合并 Phase E 后,小程序端所有已合 master 的工作:

| 模块 | 类型 | 进度 |
|---|---|---|
| 平台骨架(Phase A+B) | 框架 | ✅ 8 sealed contributions / dispatcher / capability / 4 lint |
| auth 登录 | 核心 | ✅ D-1 接通 |
| 我的资料 / 组织 / 通讯录 / 场所 | 核心浏览 | ✅ E-1~4 接通 |
| inspection 任务 / 整改文本 | 通用插件 | ✅ C+D-2 接通 |
| inspection 整改附件(拍照水印上传) | 通用插件 | ✅ D-3a 接通 |
| inspection 评分 / 申诉 | 通用插件 | ❌ 留 D-3b |
| 微信订阅消息 | 通用插件 | ❌ 留 D-3c(后端缺一整套) |
| education 学校行业 | 行业插件 | ❌ 未启动 |

## Next Steps

候选:
- **真机验证** — 累积 5 期未跑过真后端(D-1+D-2+D-3a+E-1~4),建议先做一次回归
- **D-3b 评分组件 + 申诉提交** — 补齐 inspection 闭环
- **education 学校插件** — 开始行业插件(StudentPlugin / TeacherPlugin / ClassPlugin 桩页面)
- **Phase F 监控/ADR/作者文档** — 对偶后端 Phase 8(可选,不挡 demo)

---

**P.S.** Phase E 共 5 个 commit(E-1~E-4 + finalize),全 subagent-driven-development。中途发现 mp-boundary 规则禁 core 出现行业字面量(`'TEACHER'` etc),test fixtures 改用 `'STAFF'`/`'MANAGER'` 通过 — 守护规则在帮我们守住核心层不被行业污染,这是 Phase A 留下的好底盘。
