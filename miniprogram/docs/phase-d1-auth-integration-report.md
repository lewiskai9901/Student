# Phase D-1 Report — Auth 真实集成

> 日期: 2026-05-05
> 分支: `feature/miniprogram-phase-d`
> 状态: ✅ 完成

## 范围

修复 Phase A 留下的 auth 伪集成问题。后端 LoginResponse 加 `enabledPlugins` 字段;前端 auth.ts 适配真实嵌套响应结构。**写路径(Phase D-2/D-3)不在本期范围**。

## 改动总览

### 后端(2 文件,10 行新增)

| 文件 | 改动 |
|---|---|
| `LoginResponse.java` | 顶层加 `private List<String> enabledPlugins;` |
| `AuthController.java` | 注入 `TenantPluginService` + `buildLoginResponse` 填充字段 |

后端 `mvn compile -DskipTests` 通过。**用户重启后端后,真实 `/auth/login` 响应会带新字段。**

### 前端(3 文件)

| 文件 | 改动 |
|---|---|
| `core/api/auth.ts` | LoginResp 重构嵌套 userInfo + 加 `LoginUserInfo` + `toUserInfo()` 助手 + 改用 `requestWrapped` |
| `core/stores/auth.ts` | login action 适配嵌套结构 + 加 `tenantId` 状态 |
| `core/api/auth.test.ts` | 新建,5 单测覆盖 envelope 解包 / 嵌套结构 / toUserInfo flatten |

## Phase A 留下的伪集成

```ts
// Phase A 假设 (错):
LoginResp = { accessToken, refreshToken, user, permissions, enabledPlugins }

// 真实后端响应:
Result<LoginResponse> = {
  code: 200, message, timestamp,
  data: {
    accessToken, refreshToken, tokenType, expiresIn,
    enabledPlugins,                    // 顶层
    userInfo: {                        // 嵌套
      userId, username, realName, roles,
      permissions,                     // 在 userInfo 内
      tenantId, ...
    }
  }
}
```

差异:
- envelope 包裹 — Phase A 没 unwrap
- userInfo 嵌套 — 字段名 + 层级都不一样
- permissions 在 userInfo 内,不在顶层
- 缺 enabledPlugins(后端字段没有)

## 修复后的契约

### 前端 LoginResp(对齐后端 LoginResponse)

```ts
export interface LoginUserInfo {
  userId: number
  username: string
  realName: string
  phone?, email?, avatar?, status?
  roles: string[]
  permissions: string[]
  orgUnitId?, tenantId?, userTypeCode?
}

export interface LoginResp {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: LoginUserInfo
  enabledPlugins: string[]
}
```

### auth store

新增 `tenantId` 状态(后续切租户接口可能用);从 `r.userInfo.permissions` 取 permissions,从 `r.enabledPlugins` 取 plugins;`r.userInfo` 通过 `toUserInfo()` 拍平为 `PluginContext.UserInfo` 形态。

## 测试

- Phase C 完成: 91 测试
- D-1 新增: 5 测试 (auth envelope unwrap / 嵌套结构 / POST 请求形状 / toUserInfo flatten / roles 默认)
- **总: 96 测试通过,ci 全绿**

## 验收

- ✅ 后端 LoginResponse 含 `enabledPlugins` 顶层字段
- ✅ AuthController 从 TenantPluginService 取 enabledPlugins
- ✅ 前端 LoginResp 嵌套 userInfo,匹配真实响应
- ✅ auth store login 正确解析嵌套结构 + 写入存储
- ✅ tenantId 也存
- ✅ auth.test.ts 5 单测通过
- ✅ `npm run ci` exit 0
- ✅ 后端 `mvn compile -DskipTests` BUILD SUCCESS

## 仍未做(Phase D-2/D-3)

### D-2: 简单写路径(估 1-2 天)
- 任务执行最小提交:`POST /inspection/tasks/{id}/claim/start/submit`(无打分明细)
- 整改提交:`POST /inspection/corrective-cases/{id}/submit-correction`(只文本,无附件)
- 申诉提交:`POST /inspection/appeals`(只文本)

### D-3: 拍照 + 上传 + 订阅消息(估 2-3 天)
- 拍照水印:`canvas` 合成 时间+GPS+检查员名,`capability.takePhoto` 扩展
- 文件上传:`/inspection/files/upload` 后端 + `capability.uploadFile` 前端
- 评分组件:按 ScoringPolicy 类型渲染(5 分制 / GradeScheme / 利克特)
- 微信订阅消息:任务派发 + 复核结果 模板申请

### 其他遗留
- 401 自动刷新仍是简化版(reLaunch 到登录),Phase D-3 接 refresh token
- 微信登录(wechat-login + bind-wechat)— Phase D-3 或独立 Phase E

## 经验回填

1. **集成假设要尽早实测**:Phase A 写的 LoginResp 是从规范设计出来的,但没真跑过。Phase A 的"login 流程跑通"只是 type-check 绿,实际跑会立刻报错。架构骨架阶段难免有这类伪集成,Phase D-1 这种"接通"任务是必须的。

2. **TenantPluginService 是后端早就准备好的**:不是新写。后端 Phase 5 已经写好这个服务,只是没人接。说明跨工种集成最后一公里需要专门的 phase。

3. **Result envelope 解包成本接近 0**:Phase C 的 `requestWrapped` 助手让 D-1 接通几乎零成本——auth.ts 改一个 import + 一个函数名,结构就对了。架构投资在前期付清,后期接业务很轻松。

4. **TS 类型重构不破坏其他文件**:login page 没消费返回值,plugin-registry 不读 auth shape,所以 LoginResp 重构只动了 auth.ts/stores/auth.ts/auth.test.ts 三个文件。这是耦合做对了的好处。
