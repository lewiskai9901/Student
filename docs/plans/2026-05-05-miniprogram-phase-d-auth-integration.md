# 小程序 Phase D-1 实施计划 — 后端补 enabledPlugins + 前端 auth 接通

> 重新定义的 Phase D 范围(原计划过大,拆分):
> - **D-1**(本计划,~半天):后端补字段 + 前端 auth 真实接通,login 流程对齐真实后端
> - **D-2**(后续):任务/整改/申诉简单写路径(无上传)
> - **D-3**(后续):拍照水印 + 文件上传 + 订阅消息

**Goal:** 让 Phase A 当年没认真做的 login 集成真正走通——后端 LoginResponse 加 `enabledPlugins`(从 TenantPluginService 取),前端 auth.ts 改为消费真实响应结构(`Result<LoginResponse>`,LoginResponse 嵌套 userInfo)。

**Branch:** `feature/miniprogram-phase-d` (worktree `.worktrees/miniprogram-phase-d`)

---

## 背景:Phase A 留下的伪集成

Phase A 写的 `core/api/auth.ts`:

```ts
export interface LoginResp {
  accessToken: string
  refreshToken: string
  user: UserInfo
  permissions: string[]
  enabledPlugins: string[]
}

export const authApi = {
  login: (u, p) => request<LoginResp>({ url: '/auth/login', ... })  // 直接 request,不 unwrap envelope
}
```

真实后端 `POST /auth/login` 返回(Result envelope):

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userInfo": {
      "userId": 1,
      "username": "admin",
      "realName": "管理员",
      "permissions": ["..."],
      "tenantId": 1,
      "roles": ["..."],
      ...
    }
  },
  "timestamp": ...
}
```

差异:
1. **包了 envelope** — Phase A 没 unwrap
2. **userInfo 嵌套** — Phase A 期望顶层 `user`
3. **没 enabledPlugins** — 后端 LoginResponse 还没这个字段

## 目标改动

### 后端(2 文件)

1. `LoginResponse.java`:顶层加 `private List<String> enabledPlugins;`
2. `AuthController.java`:
   - 注入 `TenantPluginService`
   - `buildLoginResponse(...)` 拿到 userDetails.tenantId 后调 `tenantPluginService.enabledPlugins(tenantId)`,写到 LoginResponse.enabledPlugins
   - refresh token 路径同步处理

### 前端(2 文件 + 测试)

1. `core/api/auth.ts`:
   - `LoginResp` 类型重构匹配真实结构(嵌套 userInfo)
   - `authApi.login` 改用 `requestWrapped<LoginRespData>()` 自动 unwrap envelope
2. `core/stores/auth.ts`:
   - `login` action 适配嵌套结构 — `r.userInfo.permissions`, `r.userInfo` → user
   - 顶层 `r.enabledPlugins` 取出
3. 集成单测验证嵌套响应正确解析

---

## Task 1: 后端补 enabledPlugins

**Files:**
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/access/dto/LoginResponse.java`
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/access/AuthController.java`

### LoginResponse.java 改动

在顶层(同 accessToken 同级)加:

```java
private List<String> enabledPlugins;
```

### AuthController.java 改动

1. 注入:`private final TenantPluginService tenantPluginService;`
2. `buildLoginResponse(...)` 把 `enabledPlugins` 填上:

```java
private LoginResponse buildLoginResponse(String accessToken, String refreshToken,
                                          CustomUserDetails userDetails, UserPO user) {
    Set<String> enabledPlugins = userDetails.getTenantId() != null
        ? tenantPluginService.enabledPlugins(userDetails.getTenantId())
        : Set.of();
    return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(7200L)
            .enabledPlugins(new ArrayList<>(enabledPlugins))
            .userInfo(buildUserInfo(userDetails, user))
            .build();
}
```

后端测试:无新单测(MVP 范围内,改动小且无业务规则);对应集成测试由前端覆盖。

后端代码动后需要重启,参考 CLAUDE.md "自动重启"约定。**注意:这一步用户可能不希望自动重启后端;改动只在 worktree,不会影响他在跑的进程。提示用户后端改动需要他重启验证。**

---

## Task 2: 前端 auth 类型重构 + 测试

**Files:**
- Modify: `miniprogram/src/core/api/auth.ts`
- Modify: `miniprogram/src/core/stores/auth.ts`
- Create: `miniprogram/src/core/api/auth.test.ts`
- Modify: `miniprogram/src/core/stores/plugin-registry.test.ts`(若该文件用旧 LoginResp 结构 — 检查)
- Modify: `miniprogram/src/core/pages/login/index.vue`(若需要适配)

### auth.ts 新结构

```ts
import { requestWrapped } from './request'
import type { UserInfo } from '../plugin/context'

// 后端 LoginResponse.UserInfo 完整字段(子集 — UI 用得到)
export interface LoginUserInfo {
  userId: number
  username: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  status?: number
  roles: string[]
  permissions: string[]
  orgUnitId?: number
  tenantId?: number
  userTypeCode?: string
}

export interface LoginResp {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: LoginUserInfo
  enabledPlugins: string[]
}

export const authApi = {
  login: (username: string, password: string) =>
    requestWrapped<LoginResp>({
      url: '/auth/login', method: 'POST',
      data: { username, password },
      skipAuth: true
    })
}

// 助手:把 LoginUserInfo 拍平为 PluginContext.UserInfo 形态
export function toUserInfo(u: LoginUserInfo): UserInfo {
  return {
    id: u.userId,
    username: u.username,
    name: u.realName,
    avatar: u.avatar,
    roles: u.roles ?? []
  }
}
```

### auth.ts store 适配

```ts
import { defineStore } from 'pinia'
import { authApi, toUserInfo } from '../api/auth'
import { capability } from '../platform/auto'
import type { UserInfo } from '../plugin/context'

interface State {
  user: UserInfo | null
  permissions: string[]
  enabledPlugins: string[]
  tenantId: number | null
}

export const useAuth = defineStore('auth', {
  state: (): State => ({
    user: capability.storage.get<UserInfo>('user') ?? null,
    permissions: capability.storage.get<string[]>('permissions') ?? [],
    enabledPlugins: capability.storage.get<string[]>('enabledPlugins') ?? [],
    tenantId: capability.storage.get<number>('tenantId') ?? null
  }),
  getters: {
    loggedIn: (s) => s.user !== null,
    hasPerm: (s) => (code: string) => s.permissions.includes(code)
  },
  actions: {
    async login(username: string, password: string) {
      const r = await authApi.login(username, password)
      const u = toUserInfo(r.userInfo)
      capability.storage.set('accessToken', r.accessToken)
      capability.storage.set('refreshToken', r.refreshToken)
      capability.storage.set('user', u)
      capability.storage.set('permissions', r.userInfo.permissions ?? [])
      capability.storage.set('enabledPlugins', r.enabledPlugins ?? [])
      capability.storage.set('tenantId', r.userInfo.tenantId ?? null)
      this.user = u
      this.permissions = r.userInfo.permissions ?? []
      this.enabledPlugins = r.enabledPlugins ?? []
      this.tenantId = r.userInfo.tenantId ?? null
      return r
    },
    logout() {
      capability.storage.remove('accessToken')
      capability.storage.remove('refreshToken')
      capability.storage.remove('user')
      capability.storage.remove('permissions')
      capability.storage.remove('enabledPlugins')
      capability.storage.remove('tenantId')
      this.user = null
      this.permissions = []
      this.enabledPlugins = []
      this.tenantId = null
    }
  }
})
```

### 测试 auth.test.ts

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { authApi, toUserInfo } from './auth'

const mockUni = { request: vi.fn(), getStorageSync: vi.fn(() => undefined),
  setStorageSync: vi.fn(), removeStorageSync: vi.fn(), reLaunch: vi.fn(),
  getSystemInfoSync: vi.fn(() => ({ platform: 'devtools', SDKVersion: '3.0.0' })) }
;(globalThis as any).uni = mockUni

describe('authApi.login', () => {
  beforeEach(() => Object.values(mockUni).forEach((fn: any) => 'mockReset' in fn && fn.mockReset()))

  it('unwraps Result envelope and returns LoginResp', async () => {
    mockUni.request.mockImplementation((o: any) => o.success({
      statusCode: 200,
      data: { code: 200, message: 'ok', timestamp: 1, data: {
        accessToken: 'a', refreshToken: 'r', tokenType: 'Bearer', expiresIn: 7200,
        enabledPlugins: ['inspection'],
        userInfo: { userId: 1, username: 'admin', realName: '管理员',
          roles: ['admin'], permissions: ['inspection:task:list'], tenantId: 1 }
      } }
    }))
    const r = await authApi.login('admin', 'p')
    expect(r.accessToken).toBe('a')
    expect(r.userInfo.userId).toBe(1)
    expect(r.userInfo.permissions).toContain('inspection:task:list')
    expect(r.enabledPlugins).toEqual(['inspection'])
  })
})

describe('toUserInfo', () => {
  it('flattens LoginUserInfo into PluginContext.UserInfo', () => {
    const u = toUserInfo({
      userId: 1, username: 'u', realName: 'U', avatar: 'av',
      roles: ['admin'], permissions: ['p:a'], tenantId: 1
    } as any)
    expect(u).toEqual({ id: 1, username: 'u', name: 'U', avatar: 'av', roles: ['admin'] })
  })
})
```

---

## Task 3: 验收 + 报告

`npm run ci` exit 0,测试 91 → 93+(新增 auth 单测)。

Report `phase-d1-auth-integration-report.md` 含:
- 后端补字段 diff
- 前端类型修复 before/after
- 仍未做的:401 自动 refresh / 微信登录 / 写路径
- D-2 / D-3 预案

---

## 验收

- [ ] 后端 LoginResponse 含 `enabledPlugins`
- [ ] AuthController 注入 TenantPluginService 并填字段
- [ ] 前端 LoginResp 类型嵌套 userInfo
- [ ] auth store login 解析嵌套结构,permissions/enabledPlugins 都正确写入
- [ ] tenantId 也存(便于将来调切租户接口)
- [ ] auth.test.ts 2 个单测通过
- [ ] `npm run ci` exit 0(前端)
- [ ] 后端编译通过(用户手工验证或 mvn compile -DskipTests)

完成后:Phase D-1 合 master,Phase D-2/3 留待后续。
