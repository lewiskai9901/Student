# 小程序 Phase C 实施计划 — Inspection 真实业务接入(只读 MVP)

> **For Claude:** REQUIRED SUB-SKILL: superpowers:executing-plans / superpowers:subagent-driven-development

**Goal:** 把 inspection 业务作为第一个真实插件接入,MVP 只读浏览(我的任务 / 我的整改 / 我的申诉),验证 Phase A SPI + Phase B 扩展点在真实业务下不漏。**写路径(打分提交、整改证据上传、申诉提交)推迟到 Phase D**(写路径需要相机/水印/上传/签名,体量太大)。

**Architecture:** 完全复用 Phase A 平台。新增 `plugins/inspection/` 目录:manifest 注册 12 perm + 5 menu + 7 route + 1 scan resolver(`INSPECTION:TASK:`) + 3 message handlers + 5 events;API client 走 Phase A `core/api/request`,补 Result envelope unwrap 助手;6 个 Vue 页面(任务列表/详情、整改列表/详情、申诉列表、扫码 landing)。

**Tech Stack:** 同 Phase A/B,无新依赖。

**Branch:** `feature/miniprogram-phase-c` (worktree: `.worktrees/miniprogram-phase-c`)

**关联**:
- Phase A 设计 `docs/plans/2026-05-04-miniprogram-architecture-design.md`
- Phase B 报告 `miniprogram/docs/phase-b-extensibility-report.md`(7 条经验回填)
- 后端控制器:
  - `interfaces/rest/inspection/InspTaskController.java`
  - `interfaces/rest/inspection/CorrectiveCaseController.java`
  - `interfaces/rest/inspection/InspAppealController.java`

---

## API 表面(MVP 只读用到的端点)

| 端点 | 方法 | 返回 | 用途 |
|---|---|---|---|
| `/inspection/tasks/my-tasks` | GET | `Result<List<InspTask>>` | 我的任务列表 |
| `/inspection/tasks/{id}` | GET | `Result<InspTask>` | 任务详情(扫码后落地) |
| `/inspection/tasks/available` | GET | `Result<List<InspTask>>` | 可领取任务(MVP 选做) |
| `/inspection/corrective-cases/my-cases` | GET | `Result<List<CorrectiveCase>>` | 我的整改列表 |
| `/inspection/corrective-cases/{id}` | GET | `Result<CorrectiveCase>` | 整改详情 |
| `/inspection/appeals/my` | GET | `Result<List<InspAppeal>>` | 我的申诉列表 |
| `/inspection/appeals/{id}` | GET | `Result<InspAppeal>` | 申诉详情(选做) |

后端统一返回 `Result<T> = { code: number, message: string, data: T, timestamp: number }`。code === 200 视为成功;非 200 抛 `BizError(code, message)`。

---

## 全局约定

- 所有路径相对 worktree 根 `D:\学生管理系统\.worktrees\miniprogram-phase-c\`
- 工作目录基线:`cd miniprogram` 后执行
- TDD 强制:测试先写
- 每完成 task 提交一次,带 Co-Authored-By
- `npm run ci` 在每个 task 末尾必须 exit 0
- core/ 0 改动(V1 不变);**唯一例外**:Task 1 在 `core/api/request.ts` 加 `requestWrapped()` 助手,用于 Result 解包(改 core 是因为 Result envelope 是后端约定,非业务,属于核心通用基础设施)

---

## Task 1: Result envelope unwrap helper + inspection types

**Files:**
- Modify: `miniprogram/src/core/api/request.ts` — 加 `requestWrapped<T>()` + `BizError`
- Modify: `miniprogram/src/core/api/request.test.ts`(新增) — 测 unwrap 逻辑
- Create: `miniprogram/src/plugins/inspection/api/types.ts` — TS 类型对齐后端 DTOs

### 设计原则

`request<T>()` 已存在,返回 `r.data`(uni.request 的 success 回调 r.data,即 Result envelope 整体)。**对内 API 全部走包装**。

新增:

```ts
export interface ResultEnvelope<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export class BizError extends Error {
  constructor(public code: number, public bizMessage: string) {
    super(`[${code}] ${bizMessage}`)
  }
}

export async function requestWrapped<T>(opts: RequestOpts): Promise<T> {
  const r = await request<ResultEnvelope<T>>(opts)
  if (r?.code !== 200) {
    throw new BizError(r?.code ?? -1, r?.message ?? 'unknown error')
  }
  return r.data
}
```

### inspection types

```ts
// plugins/inspection/api/types.ts

export type TaskStatus = 'PENDING' | 'CLAIMED' | 'IN_PROGRESS' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type CaseStatus = 'PENDING' | 'ASSIGNED' | 'IN_PROGRESS' | 'SUBMITTED' | 'VERIFIED' | 'REJECTED' | 'CLOSED' | 'ESCALATED'
export type AppealStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN'

// MVP 字段子集 — 后端有更多字段,此处只列 UI 渲染需要的。
// 严格上要从后端 DTO 反向生成,但 Phase C MVP 手维护;Phase D 可考虑 openapi-typescript。
export interface InspTask {
  id: number
  projectId: number
  projectName?: string
  templateId: number
  templateVersion?: number
  inspectorId?: number
  inspectorName?: string
  status: TaskStatus
  type?: string
  title?: string
  targetType?: string
  targetId?: number
  targetName?: string
  orgUnitId?: number
  orgUnitName?: string
  scheduledStartAt?: string
  scheduledEndAt?: string
  deadline?: string
  totalScore?: number
  createdAt?: string
  updatedAt?: string
}

export interface CorrectiveCase {
  id: number
  caseCode?: string
  projectId: number
  projectName?: string
  taskId?: number
  submissionId?: number
  itemName?: string
  observation?: string
  rootCause?: string
  status: CaseStatus
  assigneeId?: number
  assigneeName?: string
  deadline?: string
  rejectCount?: number
  escalationLevel?: number
  createdAt?: string
  submittedAt?: string
  verifiedAt?: string
}

export interface InspAppeal {
  id: number
  submissionDetailId: number
  submitterId: number
  submitterName?: string
  reason: string
  attachments?: string
  expectedAdjustment?: number
  finalAdjustment?: number
  status: AppealStatus
  reviewerId?: number
  reviewerName?: string
  reviewComment?: string
  createdAt?: string
  reviewedAt?: string
}
```

### 测试

`request.test.ts`:

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { requestWrapped, BizError } from './request'

const mockUni = {
  request: vi.fn(),
  reLaunch: vi.fn(),
  getStorageSync: vi.fn(() => undefined),
  setStorageSync: vi.fn(),
  removeStorageSync: vi.fn(),
  getSystemInfoSync: vi.fn(() => ({ platform: 'devtools', SDKVersion: '3.0.0' }))
}
;(globalThis as any).uni = mockUni

describe('requestWrapped (Result envelope)', () => {
  beforeEach(() => Object.values(mockUni).forEach(fn => 'mockReset' in fn && fn.mockReset()))

  it('unwraps Result.data on code=200', async () => {
    mockUni.request.mockImplementation((o: any) =>
      o.success({ statusCode: 200, data: { code: 200, message: 'ok', data: { id: 1, name: 'Demo' }, timestamp: 1 } })
    )
    const r = await requestWrapped<{ id: number; name: string }>({ url: '/x' })
    expect(r).toEqual({ id: 1, name: 'Demo' })
  })

  it('throws BizError on non-200 result code', async () => {
    mockUni.request.mockImplementation((o: any) =>
      o.success({ statusCode: 200, data: { code: 5001, message: '业务错', data: null, timestamp: 1 } })
    )
    await expect(requestWrapped({ url: '/x' })).rejects.toBeInstanceOf(BizError)
    await expect(requestWrapped({ url: '/x' })).rejects.toMatchObject({ code: 5001, bizMessage: '业务错' })
  })
})
```

提交:`feat(miniprogram/core): Result envelope unwrap + BizError + inspection types`

---

## Task 2: inspection plugin manifest

**Files:**
- Create: `miniprogram/src/plugins/inspection/manifest.ts`
- Create: `miniprogram/src/plugins/inspection/manifest.test.ts`
- Modify: `miniprogram/src/plugins/index.ts`(加 inspection)
- Modify: `miniprogram/src/plugins/manifests.json`(JSON 镜像)

### Contributions(对齐 §A)

- 12 PermissionContribution: `inspection:task:view / list / claim / start / submit`, `inspection:correction:view / list / process / submit`, `inspection:appeal:view / list / submit`(MVP 只用 view/list 系)
- 5 MenuContribution(home-grid):
  - `inspection.my-tasks` 我的任务 — `/plugins/inspection/pages/my-tasks` perm `inspection:task:list`
  - `inspection.scan` 扫码检查 — `/plugins/inspection/pages/scan-landing` perm `inspection:task:view`
  - `inspection.my-corrections` 我的整改 — `/plugins/inspection/pages/my-corrections` perm `inspection:correction:list`
  - `inspection.my-appeals` 我的申诉 — `/plugins/inspection/pages/my-appeals` perm `inspection:appeal:list`
  - `inspection.task-available` 可领任务 — `/plugins/inspection/pages/available-tasks` perm `inspection:task:list`(MVP 选做,可省)
- 7 RouteContribution:my-tasks, task-detail, my-corrections, correction-detail, my-appeals, scan-landing, available-tasks
- 1 ScanResolverContribution:`INSPECTION:TASK:` → 跳 task-detail(`/plugins/inspection/pages/task-detail?id=...`)
- 3 EventContribution(声明,Phase D 真用):`inspection.task.submitted / inspection.case.processed / inspection.appeal.created`
- enabled gate: `(ctx) => ctx.tenantPlugins.includes('inspection')`

测试:5 cases 覆盖注册不冲突 / scan prefix / contribution 计数 / enabled gate true/false。

提交:`feat(miniprogram/plugins): inspection manifest — 12 perm / 5 menu / 7 route / 1 scan / 3 events`

---

## Task 3: inspection API client

**Files:**
- Create: `miniprogram/src/plugins/inspection/api/inspection.ts`
- Create: `miniprogram/src/plugins/inspection/api/inspection.test.ts`

```ts
import { requestWrapped } from '@core/api/request'
import type { InspTask, CorrectiveCase, InspAppeal } from './types'

export const inspectionApi = {
  myTasks: () => requestWrapped<InspTask[]>({ url: '/inspection/tasks/my-tasks' }),
  availableTasks: () => requestWrapped<InspTask[]>({ url: '/inspection/tasks/available' }),
  taskById: (id: number) => requestWrapped<InspTask>({ url: `/inspection/tasks/${id}` }),
  myCases: () => requestWrapped<CorrectiveCase[]>({ url: '/inspection/corrective-cases/my-cases' }),
  caseById: (id: number) => requestWrapped<CorrectiveCase>({ url: `/inspection/corrective-cases/${id}` }),
  myAppeals: () => requestWrapped<InspAppeal[]>({ url: '/inspection/appeals/my' }),
  appealById: (id: number) => requestWrapped<InspAppeal>({ url: `/inspection/appeals/${id}` })
}
```

测试 mock uni.request 验证 URL/方法/responseShape 正确。3-4 个 case 即可。

提交:`feat(miniprogram/plugins): inspection API client (7 read endpoints)`

---

## Task 4: 任务相关页面(my-tasks + task-detail + scan-landing + available-tasks)

**Files:**
- Create: `miniprogram/src/plugins/inspection/pages/my-tasks.vue`
- Create: `miniprogram/src/plugins/inspection/pages/task-detail.vue`
- Create: `miniprogram/src/plugins/inspection/pages/scan-landing.vue`
- Create: `miniprogram/src/plugins/inspection/pages/available-tasks.vue`
- Create: `miniprogram/src/plugins/inspection/utils/format.ts`(状态/时间格式化)

### my-tasks.vue 关键

- `onShow` 调 `inspectionApi.myTasks()`
- 状态筛选 tab(全部/PENDING/IN_PROGRESS/SUBMITTED)
- 列表项:`title || projectName` / status 徽章 / deadline / orgUnitName
- 点项:`uni.navigateTo({ url: '/plugins/inspection/pages/task-detail?id=' + id })`
- 加载/错误/空态分别处理
- 用 `wd-skeleton` 骨架屏

### task-detail.vue 关键

- 从 `onLoad(query)` 拿 id
- 调 `inspectionApi.taskById(id)`
- 顶部:title / status / 下一步动作提示(MVP 不真做动作,只展示)
- 信息块:project / template version / target / org unit / scheduled / deadline / inspector
- 底部按钮区灰显("提交检查"等动作 disabled,Phase D 启用)

### scan-landing.vue 关键

- 调 `capability.scan()`
- 解析码 → 用 dispatcher.query<ScanResolverContribution>('scan-resolver') 找 prefix 匹配的 resolver
- resolver 返回 path,navigateTo 过去
- INSPECTION:TASK:123 → task-detail

### available-tasks.vue:

- 简化版 my-tasks,调 `inspectionApi.availableTasks()`
- MVP 不做"领取"动作,只展示 + 按钮 disabled

### format.ts

```ts
export const TASK_STATUS_LABEL: Record<string, string> = {
  PENDING: '待处理', CLAIMED: '已认领', IN_PROGRESS: '进行中',
  SUBMITTED: '已提交', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已取消'
}

export const TASK_STATUS_COLOR: Record<string, string> = {
  PENDING: '#5a6a7a', CLAIMED: '#3a7bd5', IN_PROGRESS: '#3a7bd5',
  SUBMITTED: '#9a5cc6', APPROVED: '#15a87e', REJECTED: '#e0592a', CANCELLED: '#a0aab4'
}

export function formatDeadline(d?: string): string {
  if (!d) return '-'
  // 简单:截掉时区秒,显示 yyyy-MM-dd HH:mm
  return d.slice(0, 16).replace('T', ' ')
}
```

提交:`feat(miniprogram/plugins): inspection 任务相关 4 页面 + 状态格式化`

---

## Task 5: 整改 + 申诉 页面(my-corrections + correction-detail + my-appeals)

**Files:**
- Create: `miniprogram/src/plugins/inspection/pages/my-corrections.vue`
- Create: `miniprogram/src/plugins/inspection/pages/correction-detail.vue`
- Create: `miniprogram/src/plugins/inspection/pages/my-appeals.vue`
- Modify: `miniprogram/src/plugins/inspection/utils/format.ts`(加 case/appeal 格式化)

模式同 Task 4,只读列表 + 详情。

提交:`feat(miniprogram/plugins): inspection 整改+申诉 3 页面`

---

## Task 6: 集成测试 + pages.json 重生成 + ci

**Files:**
- Create: `miniprogram/src/__integration__/inspection-scan.test.ts` — 扫码 INSPECTION:TASK:123 → resolver → 路径正确
- Create: `miniprogram/src/__integration__/inspection-activate.test.ts` — tenantPlugins 含 inspection 时激活;不含时不激活
- 更新 `miniprogram/src/plugins/manifests.json` 加 inspection 块
- 跑 `node scripts/build-pages-json.js` 重生成 pages.json,验证 inspection subPackage 加入
- `npm run ci` exit 0

提交:`test(miniprogram): inspection 扫码 + 激活集成验证`

---

## Task 7: Phase C 报告

**Files:**
- Create: `miniprogram/docs/phase-c-inspection-mvp-report.md`

含:
- 业务接入清单(API 表 + Page 表 + Contribution 表)
- 测试统计(预期 59 → 80+)
- core 改动原因(`requestWrapped` 是核心通用基础设施)
- 遇到的问题与解决方案
- Phase D 写路径预案(打分提交、相机水印、上传、订阅消息)

提交:`docs(miniprogram): Phase C inspection MVP 完成报告`

---

## Phase C 验收标准

- [ ] inspection 插件激活后,首页出现 5 个菜单
- [ ] my-tasks / task-detail / my-corrections / correction-detail / my-appeals / scan-landing / available-tasks 共 7 页加载不报错
- [ ] requestWrapped Result envelope 正确解包,BizError 在非 200 时抛出
- [ ] 扫 `INSPECTION:TASK:123` → scan-resolver 正确路由
- [ ] tenantPlugins 不含 inspection 时,菜单/路由全消失
- [ ] `git diff master..HEAD --stat -- 'miniprogram/src/core/**'` 仅显示 `request.ts` + `request.test.ts` 改动(为 Result envelope 加 helper)
- [ ] `npm run ci` exit 0
- [ ] 测试数:59 → ≥80

---

## YAGNI(本期主动不做)

- ❌ 任务领取/开始/提交动作(写路径,Phase D)
- ❌ 检查项打分 + 拍照水印(写路径,Phase D)
- ❌ 整改提交 + 证据上传(写路径,Phase D)
- ❌ 申诉提交对话框(写路径,Phase D)
- ❌ 微信订阅消息申请(Phase D)
- ❌ 离线缓存(Phase E)
- ❌ 任务/整改的页内编辑/搜索/排序(MVP 只读取列表展示)
- ❌ openapi-typescript 自动生成 TS 类型(Phase E,先手维护)

---

## 预计工时

- Task 1: 1 小时
- Task 2: 1 小时
- Task 3: 0.5 小时
- Task 4: 2 小时
- Task 5: 1 小时
- Task 6: 1 小时
- Task 7: 0.5 小时

合计 **~7 小时**,约 1 个工作日。

完成后:Phase C 合 master → 进 Phase D 写路径(估 3-5 天)。
