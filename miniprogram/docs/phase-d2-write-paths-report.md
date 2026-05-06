# Phase D-2 Report — Inspection 文本写路径接通

> 日期: 2026-05-06
> 分支: `feature/miniprogram-phase-d2`
> 状态: ✅ 完成

## 一句话总结

inspection 插件 5 个文本写 API 接通真实后端,任务详情页与整改详情页改造为状态机驱动,完成"看 → 做 → 提交"业务闭环(无附件、无评分组件,留 D-3)。

## 范围

✅ **完成:**
- 5 个写 API: `claimTask` / `startTask` / `submitTask` / `startCaseWork` / `submitCorrection`
- task-detail 状态机驱动: PENDING→认领 / CLAIMED→开始 / IN_PROGRESS→提交 / 其他→无按钮
- correction-detail 状态机驱动: PENDING+ASSIGNED→开始处理 / IN_PROGRESS+REJECTED→提交整改(textarea + 5-1000 字校验)
- BizError 自动 reload(状态漂移自愈)
- auth.user 缺失 fail-fast 守卫(不再有 `submitterId: 0` 哨兵)
- 提交成功后 emit 跨插件事件 + reLaunch 列表页
- 集成测试 3 项: API + bus 契约
- 修掉 `available-tasks.vue` 上"认领功能将在后续版本上线"的过期提示

❌ **不在本期(defer to D-3):**
- 申诉提交 (`POST /inspection/appeals`) — 无入口页(需先做评分提交详情)
- 任务 reject / case verify / case escalate(都属管理员 review 权限,小程序端不放)
- 拍照水印 / 文件上传 / 微信订阅消息
- 401 自动 refresh(仍简化为 reLaunch login)

## 5 commit 序列

| SHA | 说明 |
|---|---|
| `776a1475` | T1: 5 写 API + 5 单测 |
| `519bf9c4` | T2: task-detail 状态机首版 |
| `924034ca` | T2 fix: 漂移自愈 + auth guard 上提 + 过期提示清理 |
| `409f0515` | T3: correction-detail 状态机 + textarea |
| `c14624c2` | T4: 集成测试 (API + bus 契约,3 项) |

## 后端契约对照表

后端契约通过直接读 `InspTaskController.java` / `CorrectiveCaseController.java` 验证,**无猜测**:

| 动作 | Method+Path | Body | Casbin |
|------|----|----|----|
| 认领任务 | POST `/inspection/tasks/{id}/claim` | `{ inspectorName }` | `insp:task` `execute` |
| 开始任务 | POST `/inspection/tasks/{id}/start` | (无) | `insp:task` `execute` |
| 提交任务 | POST `/inspection/tasks/{id}/submit` | (无) | `insp:task` `execute` |
| 开始整改 | POST `/inspection/corrective-cases/{id}/start-work` | (无) | `insp:corrective` `execute` |
| 提交整改 | POST `/inspection/corrective-cases/{id}/submit-correction` | `{ correctionNote, evidenceIds }` | `insp:corrective` `execute` |

D-2 文本路径里 `evidenceIds` 一律传 `[]`,D-3 拍照后再附。

## Manifest event 触发对照

| 事件 | 触发位置 | Payload |
|---|---|---|
| `inspection.task.submitted` | task-detail 提交检查成功 | `{ taskId, submitterId }` |
| `inspection.case.processed` | correction-detail 提交整改成功 | `{ caseId, action: 'submitted' }` |
| `inspection.appeal.created` | 不在本期触发 | — |

## 测试基线

- 起点: 22 文件 / 101 测试 (Phase D-1 末)
- 终点: **23 文件 / 104 测试,0 失败**
- 增量: T1 加 5 测试到 `inspection.write.test.ts`;T4 加 3 测试到 `__integration__/inspection-write.test.ts`

```
 Test Files  23 passed (23)
      Tests  104 passed (104)
   Duration  ~50s
```

`npm run type-check` 0 errors。`npm run lint` 0 errors(346→356 warnings,均为 textarea/`v-if` 块的 inline-text/whitespace 样式 warning,与项目既有基线同模式)。

## 文件清单

### 修改 (4)
- `src/plugins/inspection/api/inspection.ts` — +5 写方法
- `src/plugins/inspection/pages/task-detail.vue` — 状态机 + 漂移自愈 + auth 守卫
- `src/plugins/inspection/pages/correction-detail.vue` — 状态机 + textarea + 校验
- `src/plugins/inspection/pages/available-tasks.vue` — 修掉过期提示

### 新建 (2)
- `src/plugins/inspection/api/inspection.write.test.ts` — 5 写 API 单测
- `src/__integration__/inspection-write.test.ts` — 3 集成测试

## Plan 偏差日志(用于后续 plan 复盘)

Phase D-2 plan 写时假设了不存在的 API 表面;实施时已用真实模式替换。未来写 plan 前应先验证 API 真存在。

| Plan 假设 | 真实情况 | 处理 |
|----|----|----|
| `capability.toast.show()` | `PlatformCapability` 不含 `toast`。`uni.showToast` 不在 `mp-boundary/no-bare-uni-api` 黑名单 | 用 `uni.showToast({ title, icon: 'none' })`,与 healthcare/demo 已有页面一致 |
| `capability.navigation.reLaunch()` | 同上,`uni.reLaunch` 也不在黑名单 | 用 `uni.reLaunch({ url })` |
| `eventBus` 命名导出 from `@core/plugin/event-bus` | 该模块只导出 `createEventBus()` 工厂,共享单例在 `usePluginRegistry().bus` (`plugin-registry.ts:9` 的 `sharedBus`) | 用 `usePluginRegistry().bus.emit(...)`,与 healthcare/demo 已有页面一致 |
| 测试文件应"new" `inspection.test.ts` | 该文件已存在(9 个 read 测试,占 96 baseline) | 写到 `inspection.write.test.ts` 兄弟文件,保持基线增长 +5 |

Code review 还指出 2 个 Important 当场修了(commit `924034ca`):
1. **状态漂移自愈** — BizError 后没 reload 会让用户对着已变质的 UI 点同样按钮反复失败 → 现在 BizError 后强制 reload
2. **auth guard 上提 & 删 `?? 0` 哨兵** — `submitterId: auth.user?.id ?? 0` 在 user 为 null 时发 sentinel 0,污染下游;改成 `doAction()` 顶部 fail-fast,删 fallback

跳过的 Minor:
- API 写测试只 stub `requestWrapped`,不走 envelope 深路径(reads 已覆盖,writes 是 1 行 pass-through,加深测试是重复)
- `evidenceIds: []` 没加 `// TODO(D-3)` 注释(报告里说明已足)
- `let caseId = 0` 模块级可变(单例页面,改 ref 是无收益重写)
- `auth.user` 守卫不窄化(reactive store getter,目前 body 不再解引用 user.id 后无影响)

## 已知限制 / 真机验证清单

代码层 type-check + lint + 测试都绿,但页面行为只有真机能验:

- [ ] 真机重启后端,登录 inspectorName 真实用户
- [ ] 找一个状态为 `PENDING` 的任务,点"认领任务" → toast "已认领" + 状态切到 CLAIMED → 按钮变"开始执行"
- [ ] 点"开始执行" → toast + 状态切 IN_PROGRESS → 按钮变"提交检查"
- [ ] 点"提交检查" → toast "已提交" + 自动跳回 my-tasks 列表
- [ ] 同时两台设备打开同一 PENDING 任务,A 先认领,B 再点 → B 看到 BizError toast 后页面自动 reload,按钮消失或变成"开始执行"(状态机自愈)
- [ ] 找一个 PENDING/ASSIGNED 整改单,点"开始处理" → 状态切 IN_PROGRESS → 出现 textarea
- [ ] textarea 输 < 5 字 → toast "整改说明至少 5 字"(无 API 调用)
- [ ] 输 ≥ 5 字 → 点"提交整改" → toast + 跳 my-corrections 列表
- [ ] 整改单状态为 REJECTED 时,提交流程同 IN_PROGRESS

## Next Steps (D-3)

D-2 处理"文本路径"。D-3 接"重路径":
1. 拍照 + canvas 加水印 (timestamp + GPS) — 走 `capability.takePhoto`
2. 文件上传 → 后端 `/files/upload`(走 `capability.uploadFile`),返回 `evidenceId`
3. submitCorrection 真实带 `evidenceIds`
4. 评分组件按 ScoringPolicy(单选 / 多选 / 数值 / 文本)
5. 申诉提交入口(从评分详情进)
6. 微信订阅消息 — 走 `capability.requestSubscribeMessage`,后端 `subscribe-template` Contribution 提供 templateIds

预估 2-3 天。

---

**P.S.** 本期由 5 个 commit 组成,full subagent-driven-development(每 task 独立 implementer + spec/quality reviewer + 修复循环)。Plan 偏差全部出在写 plan 阶段对 API 表面的错误假设;后续 plan 写之前应先扫一眼真实模块导出。
