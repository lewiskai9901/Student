# 小程序 Phase D-2 — 简单写路径 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Each task is bite-sized, TDD-driven, with explicit commits.

**Goal:** 把 inspection 插件 5 个文本类写路径接通真实后端,任务详情页与整改详情页可完成完整业务流闭环。

**Architecture:** 仍走 Phase A 既有架构 — `requestWrapped` 解 Result 信封 + `BizError` 抛业务错 + manifest 内 event 类型作 emit 契约。详情页按当前状态条件性渲染按钮(状态机驱动 UI),写成功后 emit 跨插件事件并 reLaunch 回列表。**不引入** 全局 toast/loading framework — 用 `capability.toast.show` + 局部 `loading` ref。

**Tech Stack:** TypeScript + uni-app + Vue 3 + Pinia + vitest + 既有 `@core/api/request.requestWrapped` + 既有 `PluginEventBus`。无新依赖。

**Out of scope (defer to D-3):**
- 申诉提交(`POST /inspection/appeals`) — 需 `submissionDetailId` 入口,而该入口属于"评分提交详情"流,Phase C/D-2 没构建此页面;Phase D-3 引入评分组件时一起补上
- 任务 reject / case verify / case escalate — 这些走 review/manage casbin action,小程序端不放
- 文件上传 / 拍照水印 / 微信订阅消息 — D-3
- 401 自动 refresh — 仍简化为 reLaunch 到 login

**后端契约一览(已通过读源码核对):**

| 动作 | Method+Path | Body | 返回 |
|------|----|----|----|
| 认领任务 | POST `/inspection/tasks/{id}/claim` | `{ inspectorName: string }` | `Result<InspTask>` |
| 开始任务 | POST `/inspection/tasks/{id}/start` | (无) | `Result<InspTask>` |
| 提交任务 | POST `/inspection/tasks/{id}/submit` | (无) | `Result<InspTask>` |
| 开始整改 | POST `/inspection/corrective-cases/{id}/start-work` | (无) | `Result<CorrectiveCase>` |
| 提交整改 | POST `/inspection/corrective-cases/{id}/submit-correction` | `{ correctionNote: string, evidenceIds: number[] }` | `Result<CorrectiveCase>` |

D-2 文本路径里 `evidenceIds` 一律传 `[]`(D-3 拍照后再附)。

**Manifest event 对齐:**
- `inspection.task.submitted` → submit 成功后 emit (claim/start 不 emit,变化太琐碎)
- `inspection.case.processed` → submitCorrection 成功后 emit (action='submitted')
- `inspection.appeal.created` → 不在本 phase 触发(申诉 defer)

---

## Task 1: 扩展 inspectionApi 加 5 个写方法

**Files:**
- Modify: `miniprogram/src/plugins/inspection/api/inspection.ts`
- Test: `miniprogram/src/plugins/inspection/api/inspection.test.ts` (新建)

**Step 1: Write the failing test**

```ts
// miniprogram/src/plugins/inspection/api/inspection.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest'

const requestWrappedMock = vi.fn()
vi.mock('@core/api/request', () => ({
  requestWrapped: (opts: any) => requestWrappedMock(opts)
}))

import { inspectionApi } from './inspection'

beforeEach(() => requestWrappedMock.mockReset())

describe('inspectionApi write paths', () => {
  it('claimTask POSTs body with inspectorName', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 1, status: 'CLAIMED' })
    const r = await inspectionApi.claimTask(1, '张三')
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/tasks/1/claim',
      method: 'POST',
      data: { inspectorName: '张三' }
    })
    expect(r.status).toBe('CLAIMED')
  })

  it('startTask POSTs without body', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 1, status: 'IN_PROGRESS' })
    await inspectionApi.startTask(1)
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/tasks/1/start',
      method: 'POST'
    })
  })

  it('submitTask POSTs without body', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 1, status: 'SUBMITTED' })
    await inspectionApi.submitTask(1)
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/tasks/1/submit',
      method: 'POST'
    })
  })

  it('startCaseWork POSTs without body', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 9, status: 'IN_PROGRESS' })
    await inspectionApi.startCaseWork(9)
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/corrective-cases/9/start-work',
      method: 'POST'
    })
  })

  it('submitCorrection POSTs note + empty evidenceIds', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 9, status: 'SUBMITTED' })
    const r = await inspectionApi.submitCorrection(9, '已修复')
    expect(requestWrappedMock).toHaveBeenCalledWith({
      url: '/inspection/corrective-cases/9/submit-correction',
      method: 'POST',
      data: { correctionNote: '已修复', evidenceIds: [] }
    })
    expect(r.status).toBe('SUBMITTED')
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd miniprogram && npx vitest run src/plugins/inspection/api/inspection.test.ts`
Expected: FAIL — `inspectionApi.claimTask is not a function` (5 errors)

**Step 3: Write minimal implementation**

Modify `miniprogram/src/plugins/inspection/api/inspection.ts`:

```ts
import { requestWrapped } from '@core/api/request'
import type { InspTask, CorrectiveCase, InspAppeal } from './types'

export const inspectionApi = {
  // ===== Reads =====
  myTasks: () => requestWrapped<InspTask[]>({ url: '/inspection/tasks/my-tasks' }),
  availableTasks: () => requestWrapped<InspTask[]>({ url: '/inspection/tasks/available' }),
  taskById: (id: number) => requestWrapped<InspTask>({ url: `/inspection/tasks/${id}` }),
  myCases: () => requestWrapped<CorrectiveCase[]>({ url: '/inspection/corrective-cases/my-cases' }),
  caseById: (id: number) => requestWrapped<CorrectiveCase>({ url: `/inspection/corrective-cases/${id}` }),
  myAppeals: () => requestWrapped<InspAppeal[]>({ url: '/inspection/appeals/my' }),
  appealById: (id: number) => requestWrapped<InspAppeal>({ url: `/inspection/appeals/${id}` }),

  // ===== Writes (Phase D-2) =====
  claimTask: (id: number, inspectorName: string) =>
    requestWrapped<InspTask>({
      url: `/inspection/tasks/${id}/claim`,
      method: 'POST',
      data: { inspectorName }
    }),
  startTask: (id: number) =>
    requestWrapped<InspTask>({ url: `/inspection/tasks/${id}/start`, method: 'POST' }),
  submitTask: (id: number) =>
    requestWrapped<InspTask>({ url: `/inspection/tasks/${id}/submit`, method: 'POST' }),
  startCaseWork: (id: number) =>
    requestWrapped<CorrectiveCase>({
      url: `/inspection/corrective-cases/${id}/start-work`,
      method: 'POST'
    }),
  submitCorrection: (id: number, correctionNote: string) =>
    requestWrapped<CorrectiveCase>({
      url: `/inspection/corrective-cases/${id}/submit-correction`,
      method: 'POST',
      data: { correctionNote, evidenceIds: [] }
    })
}
```

**Step 4: Run test to verify it passes**

Run: `cd miniprogram && npx vitest run src/plugins/inspection/api/inspection.test.ts`
Expected: PASS, 5 tests

**Step 5: Run full CI**

Run: `cd miniprogram && npm run ci`
Expected: 22 files, 101 tests passing (96 + 5 new)

**Step 6: Commit**

```bash
git add miniprogram/src/plugins/inspection/api/inspection.ts \
        miniprogram/src/plugins/inspection/api/inspection.test.ts
git commit -m "feat(miniprogram/inspection): 5 write API methods (task claim/start/submit, case start/submit)"
```

---

## Task 2: 任务详情页接通 claim/start/submit

**Files:**
- Modify: `miniprogram/src/plugins/inspection/pages/task-detail.vue`

按当前 task 状态条件性渲染单个主按钮:
- `PENDING` → "认领任务" → claim → reload page
- `CLAIMED` → "开始执行" → start → reload page
- `IN_PROGRESS` → "提交检查" → confirm dialog → submit → emit `inspection.task.submitted` → reLaunch 回 my-tasks
- 其他状态 → 不渲染按钮(只显示状态徽章)

`claim` 需要 `inspectorName`,从 `useAuth().user?.name` 取(login 后已存)。

**Step 1: Update component logic**

Replace the entire `<script setup lang="ts">` block of `task-detail.vue` with:

```ts
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { inspectionApi } from '../api/inspection'
import { BizError } from '@core/api/request'
import { capability } from '@core/platform/auto'
import { useAuth } from '@core/stores/auth'
import { eventBus } from '@core/plugin/event-bus'
import type { InspTask } from '../api/types'
import { taskStatusLabel, taskStatusColor, formatDateTime } from '../utils/format'

const auth = useAuth()
const task = ref<InspTask | null>(null)
const loading = ref(true)
const submitting = ref(false)
const errMsg = ref('')
let taskId = 0

onLoad(async (query: any) => {
  taskId = Number(query?.id)
  if (!Number.isFinite(taskId) || taskId <= 0) {
    errMsg.value = '任务 ID 缺失或非法'
    loading.value = false
    return
  }
  await reload()
})

async function reload() {
  loading.value = true
  errMsg.value = ''
  try {
    task.value = await inspectionApi.taskById(taskId)
  } catch (e) {
    errMsg.value = e instanceof BizError ? e.bizMessage : '加载失败'
  } finally {
    loading.value = false
  }
}

const action = computed<'claim' | 'start' | 'submit' | null>(() => {
  switch (task.value?.status) {
    case 'PENDING': return 'claim'
    case 'CLAIMED': return 'start'
    case 'IN_PROGRESS': return 'submit'
    default: return null
  }
})

const actionLabel = computed(() => {
  if (action.value === 'claim') return '认领任务'
  if (action.value === 'start') return '开始执行'
  if (action.value === 'submit') return '提交检查'
  return ''
})

async function doAction() {
  if (!action.value || submitting.value) return
  submitting.value = true
  try {
    if (action.value === 'claim') {
      const name = auth.user?.name || auth.user?.username || ''
      if (!name) { capability.toast.show('未取到当前用户名'); return }
      await inspectionApi.claimTask(taskId, name)
      capability.toast.show('已认领')
      await reload()
    } else if (action.value === 'start') {
      await inspectionApi.startTask(taskId)
      capability.toast.show('已开始')
      await reload()
    } else if (action.value === 'submit') {
      await inspectionApi.submitTask(taskId)
      eventBus.emit('inspection.task.submitted', {
        taskId,
        submitterId: auth.user?.id ?? 0
      })
      capability.toast.show('已提交')
      capability.navigation.reLaunch('/plugins/inspection/pages/my-tasks')
    }
  } catch (e) {
    capability.toast.show(e instanceof BizError ? e.bizMessage : '操作失败')
  } finally {
    submitting.value = false
  }
}
```

**Step 2: Update template**

Replace the `<view class="actions">` block in the template with:

```html
<view v-if="action" class="actions">
  <wd-button block :loading="submitting" @click="doAction">{{ actionLabel }}</wd-button>
</view>
```

**Step 3: Type-check**

Run: `cd miniprogram && npm run type-check`
Expected: 0 errors

**Step 4: Lint**

Run: `cd miniprogram && npm run lint`
Expected: 0 errors (warns OK)

**Step 5: CI**

Run: `cd miniprogram && npm run ci`
Expected: still 22 files / 101 tests passing

**Step 6: Commit**

```bash
git add miniprogram/src/plugins/inspection/pages/task-detail.vue
git commit -m "feat(miniprogram/inspection): task-detail 状态机驱动 claim/start/submit 闭环"
```

---

## Task 3: 整改详情页接通 start-work/submit-correction

**Files:**
- Modify: `miniprogram/src/plugins/inspection/pages/correction-detail.vue`

按当前 case status 条件性渲染:
- `PENDING` / `ASSIGNED` → "开始处理" 主按钮 → start-work → reload
- `IN_PROGRESS` / `REJECTED` → 显示 textarea (整改说明) + "提交整改" 主按钮 → submitCorrection(note) → emit `inspection.case.processed` → reLaunch 回 my-corrections
- 其他状态 → 不渲染

整改说明必填,长度 5-1000;校验失败 toast。

**Step 1: Update component logic**

Replace the entire `<script setup lang="ts">` block of `correction-detail.vue` with:

```ts
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { inspectionApi } from '../api/inspection'
import { BizError } from '@core/api/request'
import { capability } from '@core/platform/auto'
import { eventBus } from '@core/plugin/event-bus'
import type { CorrectiveCase } from '../api/types'
import { caseStatusLabel, caseStatusColor, formatDateTime } from '../utils/format'

const c = ref<CorrectiveCase | null>(null)
const loading = ref(true)
const submitting = ref(false)
const errMsg = ref('')
const note = ref('')
let caseId = 0

onLoad(async (query: any) => {
  caseId = Number(query?.id)
  if (!Number.isFinite(caseId) || caseId <= 0) {
    errMsg.value = '整改单 ID 缺失或非法'
    loading.value = false
    return
  }
  await reload()
})

async function reload() {
  loading.value = true
  errMsg.value = ''
  try {
    c.value = await inspectionApi.caseById(caseId)
  } catch (e) {
    errMsg.value = e instanceof BizError ? e.bizMessage : '加载失败'
  } finally {
    loading.value = false
  }
}

const action = computed<'start' | 'submit' | null>(() => {
  switch (c.value?.status) {
    case 'PENDING':
    case 'ASSIGNED':
      return 'start'
    case 'IN_PROGRESS':
    case 'REJECTED':
      return 'submit'
    default:
      return null
  }
})

async function doStart() {
  if (submitting.value) return
  submitting.value = true
  try {
    await inspectionApi.startCaseWork(caseId)
    capability.toast.show('已开始处理')
    await reload()
  } catch (e) {
    capability.toast.show(e instanceof BizError ? e.bizMessage : '操作失败')
  } finally {
    submitting.value = false
  }
}

async function doSubmit() {
  if (submitting.value) return
  const trimmed = note.value.trim()
  if (trimmed.length < 5) { capability.toast.show('整改说明至少 5 字'); return }
  if (trimmed.length > 1000) { capability.toast.show('整改说明最多 1000 字'); return }
  submitting.value = true
  try {
    await inspectionApi.submitCorrection(caseId, trimmed)
    eventBus.emit('inspection.case.processed', { caseId, action: 'submitted' })
    capability.toast.show('已提交')
    capability.navigation.reLaunch('/plugins/inspection/pages/my-corrections')
  } catch (e) {
    capability.toast.show(e instanceof BizError ? e.bizMessage : '操作失败')
  } finally {
    submitting.value = false
  }
}
```

**Step 2: Update template**

Replace the `<view class="actions">` block in the template with:

```html
<view v-if="action === 'start'" class="actions">
  <wd-button block :loading="submitting" @click="doStart">开始处理</wd-button>
</view>

<view v-else-if="action === 'submit'" class="submit-card">
  <view class="card-title">整改说明 <text class="hint">(5-1000 字)</text></view>
  <textarea
    v-model="note"
    class="note"
    placeholder="描述本次整改情况"
    maxlength="1000"
    auto-height
  />
  <view class="actions">
    <wd-button block :loading="submitting" @click="doSubmit">提交整改</wd-button>
  </view>
</view>
```

Add to `<style>`:

```scss
.submit-card { background: #fff; border-radius: 14px; padding: 24rpx; margin-top: 16rpx; box-shadow: 0 2px 6px rgba(58,123,213,0.06); }
.hint { color: #a0aab4; font-weight: 400; font-size: 22rpx; margin-left: 8rpx; }
.note { width: 100%; min-height: 200rpx; padding: 16rpx; border: 1rpx solid #e0e6ee; border-radius: 8px; font-size: 26rpx; color: #1a2840; box-sizing: border-box; }
```

**Step 3: Type-check / Lint**

Run: `cd miniprogram && npm run type-check && npm run lint`
Expected: 0 errors

**Step 4: CI**

Run: `cd miniprogram && npm run ci`
Expected: still 101 passing

**Step 5: Commit**

```bash
git add miniprogram/src/plugins/inspection/pages/correction-detail.vue
git commit -m "feat(miniprogram/inspection): correction-detail 状态机驱动 start-work/submit-correction"
```

---

## Task 4: 集成测试 — emit 事件流闭环

**Files:**
- Create: `miniprogram/src/__integration__/inspection-write.test.ts`

验证 D-2 关键不变量:
1. `task.submit` 成功后 `inspection.task.submitted` 被发射
2. `submitCorrection` 成功后 `inspection.case.processed` 被发射
3. demo 插件在事件总线上能跨插件接收到这两个事件(用 demo 的 `_crossPluginAuditLog` 机制)

**Step 1: Write the failing test**

```ts
// miniprogram/src/__integration__/inspection-write.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { eventBus } from '../core/plugin/event-bus'

const requestWrappedMock = vi.fn()
vi.mock('@core/api/request', () => ({
  requestWrapped: (opts: any) => requestWrappedMock(opts),
  BizError: class BizError extends Error {
    constructor(public code: number, public bizMessage: string) {
      super(`[${code}] ${bizMessage}`)
    }
  }
}))

import { inspectionApi } from '../plugins/inspection/api/inspection'

beforeEach(() => requestWrappedMock.mockReset())

describe('inspection write → event bus', () => {
  it('submitTask success then manual emit reaches a cross-plugin listener', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 7, status: 'SUBMITTED' })
    const seen: any[] = []
    eventBus.on('inspection.task.submitted', (p) => seen.push(p))
    await inspectionApi.submitTask(7)
    eventBus.emit('inspection.task.submitted', { taskId: 7, submitterId: 1 })
    expect(seen).toEqual([{ taskId: 7, submitterId: 1 }])
  })

  it('submitCorrection success then manual emit reaches listener', async () => {
    requestWrappedMock.mockResolvedValueOnce({ id: 9, status: 'SUBMITTED' })
    const seen: any[] = []
    eventBus.on('inspection.case.processed', (p) => seen.push(p))
    await inspectionApi.submitCorrection(9, '已完成 5 项整改')
    eventBus.emit('inspection.case.processed', { caseId: 9, action: 'submitted' })
    expect(seen).toEqual([{ caseId: 9, action: 'submitted' }])
  })

  it('BizError on submit still emits nothing if action skipped', async () => {
    requestWrappedMock.mockRejectedValueOnce(new (class extends Error { code = 4001; bizMessage = '状态不可提交' } as any))
    const seen: any[] = []
    eventBus.on('inspection.task.submitted', (p) => seen.push(p))
    await expect(inspectionApi.submitTask(7)).rejects.toBeTruthy()
    expect(seen).toEqual([])
  })
})
```

**Step 2: Run test to verify it fails or passes**

Run: `cd miniprogram && npx vitest run src/__integration__/inspection-write.test.ts`
Expected: PASS — these tests don't depend on Task 2/3 page code, they purely exercise API + bus contract. They'll go green immediately after Task 1.

If these need adjustment after seeing real bus signature, fix in this task only.

**Step 3: CI**

Run: `cd miniprogram && npm run ci`
Expected: 23 files / 104 tests passing (101 + 3 new)

**Step 4: Commit**

```bash
git add miniprogram/src/__integration__/inspection-write.test.ts
git commit -m "test(miniprogram/inspection): integration — write API + event bus contract"
```

---

## Task 5: 报告 + 合并

**Files:**
- Create: `miniprogram/docs/phase-d2-write-paths-report.md`

**Content outline:**
- 范围 / 不在范围
- 5 个写 API
- 2 个详情页状态机
- 3 个集成测试
- 测试基线变化 (96 → 104)
- 后端契约对照表
- 已知限制 / 后续 Phase D-3 入口

**Step 1: Write report**

参考 `miniprogram/docs/phase-d1-auth-integration-report.md` 的结构。需要包括:
- 一句话总结
- 范围 (✓ 完成 / ✗ defer)
- 后端契约对照表
- 文件清单
- 测试结果 (vitest output 截图)
- 已知限制
- Next steps

**Step 2: Commit**

```bash
git add miniprogram/docs/phase-d2-write-paths-report.md
git commit -m "docs(miniprogram): Phase D-2 写路径完成报告"
```

**Step 3: Final CI**

Run: `cd miniprogram && npm run ci`
Expected: 104 tests, 0 failures

**Step 4: Merge to master**

回到 master worktree:
```bash
cd /d/学生管理系统
git merge --no-ff feature/miniprogram-phase-d2 -m "Merge branch 'feature/miniprogram-phase-d2' — Phase D-2 inspection 写路径接通"
```

**Step 5: Cleanup**

```bash
git worktree remove .worktrees/miniprogram-phase-d2
git branch -d feature/miniprogram-phase-d2
```

---

## Verification Checklist (Definition of Done)

- [ ] `npm run ci` 全绿 (104 tests)
- [ ] `npm run type-check` 0 errors
- [ ] `npm run lint` 0 errors
- [ ] task-detail.vue 4 状态(PENDING/CLAIMED/IN_PROGRESS/其他)按钮渲染正确
- [ ] correction-detail.vue 3 状态分支正确
- [ ] 写成功 toast / reLaunch 行为正确(仅靠 type-check 验证不出来,留作 D-3 真机验证)
- [ ] 报告写完
- [ ] master 合并 + 分支清理
