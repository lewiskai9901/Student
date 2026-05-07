# Phase D-3b Report — 检查表填写 + 申诉提交

> 日期: 2026-05-07
> 分支: `feature/miniprogram-phase-d3b1`(单 worktree 容纳 D-3b-1 + D-3b-2)
> 状态: ✅ 完成

## 一句话总结

inspection 任务从 D-2 的"直接提交"升级为"逐项填表 → 提交"完整流;补回 D-2 跳过的申诉提交;`inspection.appeal.created` 事件首次真触发。

## 范围

✅ **完成:**
- D-3b-1 检查表填写
  - 4 新 API:`submissionsByTask` / `submissionDetails` / `updateDetailResponse` / `completeSubmission`
  - 新页 `submission-detail.vue`:逐项填表 + 2 简评分模式控件(PASS_FAIL toggle + DIRECT 数字输入)+ "提交检查表" finalize
  - 其他 11 种 ScoringMode 显示"请用 web 端填写"
  - task-detail IN_PROGRESS 状态拆成 2 个按钮:**填写检查表** + **提交任务**
  - `scoringModeLabel(m)` helper(13 mode 中文标签)
- D-3b-2 申诉提交
  - 1 新 API:`submitAppeal`
  - 新页 `submit-appeal.vue`:申诉理由 5-500 字 + emit `inspection.appeal.created`
  - submission-detail 单项加"申诉此项"链接(score 已填后才显示)

❌ **不在本期(留 D-3c / 后续):**
- 11 种复杂 scoring mode UI(LEVEL/SCORE_TABLE/CUMULATIVE/TIERED_DEDUCTION/RATING_SCALE/WEIGHTED_MULTI/RISK_MATRIX/THRESHOLD/FORMULA/DEDUCTION/ADDITION)
- 申诉附件上传(reuse D-3a 的 evidenceIds 通道,需扩 InspAppeal 字段)
- 微信订阅消息(D-3c)
- submission 状态机展示(IN_PROGRESS / COMPLETED / LOCKED 全状态可视化)

## 2 commits 序列

| SHA | 说明 |
|---|---|
| `7df0ced4` | D-3b-1: 检查表填写 + 4 API + 5 测试(submission-detail.vue 新页 / task-detail 双按钮) |
| `b9bb49c0` | D-3b-2: 申诉提交 + 1 API + 1 测试(submit-appeal.vue 新页 / submission-detail 申诉入口) |

## 后端契约对照表

D-3b 新接通的 5 个端点(全部已存在,前端首次消费):

| 端点 | 用途 | 关键字段 |
|---|---|---|
| `GET /inspection/submissions?taskId=X` | 列任务的 submission(取 [0]) | id, taskId, status, totalScore, itemCount, completedCount |
| `GET /inspection/submissions/{id}/details` | 列 submission 所有 item | id, itemCode, itemName, scoringMode, scoringConfig, responseValue, score |
| `PUT /inspection/submissions/details/{id}/response` | 写单项响应 | body `{responseValue, scoringMode, score, dimensions}` |
| `POST /inspection/submissions/{id}/complete` | finalize submission | (无 body)后端计算总分 |
| `POST /inspection/appeals` | 提交申诉 | body `{submissionDetailId, submitterName, reason, attachments?}` |

## Manifest 变化

- 路由:7 → 9(+ submission-detail / submit-appeal)
- subPackage pages 同步加 2
- 现在所有 manifest 声明的事件都被真触发了:
  - `inspection.task.submitted` ✅ task-detail submit
  - `inspection.case.processed` ✅ correction-detail submit
  - `inspection.appeal.created` ✅ submit-appeal submit(D-3b-2 新)
- manifest.test.ts route 计数 7→9

## 测试基线

- 起点:28 文件 / 126 测试(Phase E 末)
- 终点:**28 文件 / 131 测试,0 失败**
- 增量:5 测试(D-3b-1 + 4, D-3b-2 + 1)— 全部加在既有 `inspection.write.test.ts`
- type-check + lint 全绿

## 文件清单

### 修改 (6 文件)
- `inspection/api/types.ts` — +ScoringMode +SubmissionDetail +InspSubmission +SubmissionStatus
- `inspection/api/inspection.ts` — +5 方法
- `inspection/api/inspection.write.test.ts` — +5 测试
- `inspection/utils/format.ts` — +scoringModeLabel
- `inspection/manifest.ts` — +2 路由 + 2 subPackage
- `inspection/manifest.test.ts` — 路由计数 7→9
- `inspection/pages/task-detail.vue` — submit 分支拆 2 按钮

### 新建 (2 页面)
- `inspection/pages/submission-detail.vue` — 检查表填写主页(~205 行)
- `inspection/pages/submit-appeal.vue` — 申诉提交(~75 行)

## 关键设计决策

### 为什么只支持 PASS_FAIL + DIRECT 两种 scoring mode

13 种 mode 全做要 4-5 天,且不少 mode(SCORE_TABLE / WEIGHTED_MULTI / RISK_MATRIX / FORMULA)需要复杂表单(矩阵/嵌套 inputs/动态依赖)。小程序屏幕窄,这些 mode 在 web 端体验更好。简单 mode(PASS_FAIL/DIRECT)覆盖最常见检查项("有/没有""扣多少分"),够 80% 场景。其他 mode 给"请用 web 端"提示,引导用户。

### 为什么 task-detail 拆 2 按钮("填写"/"提交")

D-2 的设计:IN_PROGRESS 直接给"提交检查"按钮,但实际后端要求所有 item 已填才能 submitTask 成功。用户不知道流程会卡住。改成 2 按钮明确:先填表,再提交。即使后端不强制,UX 也更清晰。

### 为什么"申诉"在 submission-detail 而不是独立菜单

申诉的 entry context 是"对某个评分项不满"— 必须知道是哪一项。从 submission-detail 单项进入是最自然路径(detail 已 score 后才显示)。D-2 设计申诉 menu(my-appeals 列表)只读已存在,这次补的是"创建路径"。

### 为什么不实装附件上传

D-3a 的 evidenceIds 是给整改用,backend `InspAppeal.attachments` 字段是 `String`(JSON 字符串数组)— 不直接复用 evidenceIds 表。需要单独路径或后端扩展。留给后续。

## 已知限制 / 真机验证清单

代码层全绿,但需真机验:

- [ ] 真账号登录,IN_PROGRESS 任务 → "填写检查表"按钮显示
- [ ] 进入 submission-detail → 列出所有 item,按 sectionName 分组
- [ ] 找一个 PASS_FAIL item → 点"通过"/"不通过"切换 → 后端确认 responseValue + score 写入
- [ ] 找一个 DIRECT item → 输数字 → blur 时保存 → toast "已保存"
- [ ] 找一个其他 mode item → 显示"请用 web 端填写"灰态
- [ ] 全部 item 填完 → "提交检查表" 按钮可点
- [ ] 漏填 item → "提交检查表"按钮 disabled
- [ ] complete submission 后 → 跳回 task-detail,显示 SUBMITTED 状态(submission 状态)
- [ ] 任意已 score item → "申诉此项"链接显示
- [ ] 点击进入 submit-appeal → 输入 ≥5 字 → 提交 → toast "已提交申诉" + 跳回 submission-detail
- [ ] 我的申诉(my-appeals 页)— 列表里看到刚提交的申诉

## Inspection 模块累计状态

合并 D-3b 后,inspection 插件几乎闭环:

| 功能 | Phase | 状态 |
|---|---|---|
| 任务列表 / 可领任务 | C | ✅ |
| 任务详情 | C+D-2+D-3b | ✅ 完整状态机(认领/开始/填表/提交) |
| 整改列表 / 详情 | C+D-2+D-3a | ✅ 文本 + 附件 |
| 检查表填写 | D-3b-1 | ✅ 2 简模式 |
| 申诉列表 | C | ✅ |
| 申诉提交 | D-3b-2 | ✅ |
| 扫码 | C | ✅ scan-resolver |
| **现存缺口** | | |
| 11 种复杂 scoring mode | — | ❌ 引导 web |
| 申诉附件 | — | ❌ 后端字段 String,需扩 |
| 微信订阅消息 | D-3c | ❌ 后端缺一整套 |

## Next Steps

候选:
- **真机验证** — 累积 D-1+D-2+D-3a+E+D-3b 都没跑过真后端
- **D-3c 微信订阅消息** — 后端缺一整套(模板/触发/发送/回执),工作量大
- **education 行业插件** — 进入行业垂直层(StudentPlugin / TeacherPlugin / ClassPlugin 桩页面)
- **Phase F 监控/ADR/作者文档** — 收尾工程

强烈建议先做**真机验证**。

---

**P.S.** D-3b 共 2 commit,subagent-driven。中途没有 plan 偏差,后端契约假设全对(D-3a 留下的"先扫真接口再写 plan"教训生效)。
