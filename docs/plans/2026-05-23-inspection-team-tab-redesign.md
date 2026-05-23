# 检查项目「人员与任务」Tab 重构设计

**日期**: 2026-05-23
**作者**: brainstorming with Claude
**范围**: `ProjectDetailView.vue` 内 `activeTab === 'team'` 区域 (第 1104-1248 行) 的完整重构，配套后端 `InspectorRole` 语义化重塑

---

## 1. 背景与动机

### 1.1 现状

当前 `ProjectDetailView.vue:1104-1248` 把"人员与任务"实现为 3 张纵向卡片：

1. **待审核任务**（红徽章，仅在有数据时出现）
2. **待分配任务**（橙徽章，仅在有数据时出现）
3. **检查员管理**（添加搜索 + 名单 + 个人统计 + 负载条）

### 1.2 痛点

- **工作流割裂**：管理员的真实操作是"看谁忙、谁闲、谁有逾期，再决定指派"，但 UI 把任务/人切分开，需要在卡片之间来回滚动并心算
- **角色装饰化**：`InspectorRole` enum 有 `INSPECTOR / REVIEWER / LEAD` 三个值，但 grep 整个 backend 发现 **`LEAD` 没有任何业务消费方**，`REVIEWER` 同样没在审核逻辑里被校验——3 个标签都是装饰
- **创建者隐身**：`InspProject.createdBy` 不会自动写入 `project_inspector`，导致项目"负责人是谁"在人员页根本看不到
- **批量操作缺位**：当前只能逐条指派、逐条催办

### 1.3 三个事实澄清

- **检查角色 vs 系统 RBAC 完全独立**：`domain/inspection/InspectorRole` 与 `domain/access/Role` 零关联，一人可在 A 项目当负责人、B 项目当检查员
- **LEAD 当前几乎是装饰**：搜遍 backend 无业务消费
- **创建者目前不属于任何检查角色**：能管理项目纯因系统 RBAC 给了 admin/项目管理权限

---

## 2. 设计目标

1. 把"人员"和"任务"从切分卡片合并为**以人为中心的工作台**
2. 把 `LEAD` 从装饰枚举改造成**真实的"项目负责人"语义**，并自动绑定 `createdBy`
3. 把 `REVIEWER` 从装饰枚举改造成**审核动作的真授权检查**
4. 提供**批量指派 / 批量催办** 能力
5. 引入**角色矩阵视图**，让管理员一眼看清并快速调整角色归属

---

## 3. 信息架构

```
┌───────────────────────────────────────────────────────────┐
│ ① 顶部状态条（紧凑，单行）                                  │
│  共 N 人 · 待分配 X · 待审核 Y · 逾期 Z · 负责人 张三       │
├───────────────────────────────────────────────────────────┤
│ ② 视图切换（segmented，默认"按人"）                         │
│  [ 按人 ▾ ]  [ 按任务 ]  [ 角色矩阵 ]      [+ 添加成员]    │
├───────────────────────────────────────────────────────────┤
│ ③ 主区域 — 三视图之一                                       │
└───────────────────────────────────────────────────────────┘
```

**关键决定**：

- 取消"待分配/待审核独立卡片"的浮动逻辑——这些状态收编到顶部状态条 + 每个人的行里
- 默认「按人」视图，回应工作流割裂痛点；保留「按任务」视图作为兼容旧习惯的备选
- 「角色矩阵」视图供管理员快速调整角色归属
- 数字徽章可点击 → 自动切到对应视图 + 对应 tab

---

## 4. 「按人」视图（主战场）

### 4.1 折叠行（默认）

```
┌────────────────────────────────────────────────────────────────┐
│ 👤 张三  ⭐负责人  ·  教导处                                    │
│   ▮▮▮▮▮▮▮○○○  60%  (本周 9/15)                                │
│   待分配候选 3 │ 进行中 2 │ 待我审核 4 │ ⚠逾期 1               │
│   [分配] [查看任务] [...]                            [展开 ▾] │
└────────────────────────────────────────────────────────────────┘
```

### 4.2 展开后

```
◆ 进行中 (2)
   · INSP-20260523-01  晨检·教学楼A  截止今天18:00  [催办][撤回]
◆ 待审核 (4)  — 此人作为审核员
   · INSP-20260522-07  检查员 王五 · 22:10提交  [✓通过][✗驳回]
◆ 待分配 (3 个候选任务)
   · INSP-20260523-09  晨检·实验楼   [立即指派给本人]
◆ 已逾期 (1)
   · INSP-20260520-04  超时 3 天     [重派他人][延期]
```

### 4.3 视觉与交互规则

- **角色徽章**：负责人 ⭐金色；审核员蓝色；检查员灰色（dot + label，不用大色块）
- **进度条**：仅展示"本周完成率"（比全期累计更有行动指引）
- **4 个数字徽章** (待分配/进行中/待审核/逾期)：任意 >0 才显示，全 0 显示"空闲"
- **逾期 >0 的行**：整体描红边
- **默认展开规则**：有待办的行默认展开；全空闲的行折叠
- **排序**：固定按"逾期 desc → 待审核 desc → 待分配 desc → 进行中 desc"，**忙的人/堵的人浮到顶部**
- **搜索**：>4 人才出现搜索框；支持按角色筛选 chips
- **负责人置顶**：即使没有待办，负责人始终在最顶

---

## 5. 「按任务」视图（兼容旧习惯 + 批量）

```
[待分配 12] [待审核 5] [进行中 28] [逾期 3]    ← tab 切换
□ 全选   [批量指派 ▾]  [批量催办]
┌────────────────────────────────────────────────┐
│ □ INSP-20260523-01  晨检·教学楼A  待分配         │
│   3 候选: 张三(忙) 李四(空闲) 王五(空闲)        │
│   [指派给李四 →]                                │
└────────────────────────────────────────────────┘
```

**关键增强**：

- 候选检查员旁标"忙/空闲"，根据 inspectorStats 计算（`assigned-completed > 3` = 忙），系统推荐空闲者
- 多选 + 批量指派 / 批量催办
- 顶部按状态切换 tab——本质是当前"待分配/待审核卡片"的归位

---

## 6. 「角色矩阵」视图

```
┌──────────────────────────────────────────────────────────┐
│ 姓名   部门     检查员  审核员  负责人  状态             │
│ 张三   教导处   ✓       ─      ⭐     启用 [移除]       │
│ 李四   后勤     ✓       ✓      ─      启用 [移除]       │
│ 王五   学工     ✓       ─      ─      禁用 [启用]       │
└──────────────────────────────────────────────────────────┘
```

**规则**：

- 同一人多角色 = 多行（保留 DB `(project_id, user_id, role)` 复合主键语义）
- 点击 ✓/─ 直接 toggle 增删行；移除负责人时校验"剩余 LEAD ≥ 1"
- 顶部 chips 筛选：[全部] [仅负责人] [仅审核员] [仅检查员] [已禁用]
- 不提供批量赋角色（避免误操作；逐行点击足够）

---

## 7. LEAD 改造为「项目负责人」

### 7.1 枚举语义

| code | 旧 label | 新 label | 新职责（强制） |
|---|---|---|---|
| `INSPECTOR` | 检查员 | 检查员 | 执行任务 |
| `REVIEWER` | 审核员 | 审核员 | 审核提交（**有此角色才能审**） |
| `LEAD` | 组长 | **项目负责人** | 项目管理（**有此角色或 admin 才能改设置**） |

**enum code 保持不变**（`LEAD`），仅改 label——避免 DB / 缓存 / API 兼容问题。

### 7.2 自动绑定规则

1. 创建项目时：自动把 `createdBy` 写入 `project_inspector`，`role=LEAD`, `isActive=true`
2. 强制不变量：每个项目至少 1 个 LEAD；移除最后一个时拒绝
3. 多 LEAD 允许：可追加更多负责人共同管理
4. 角色升降：admin 或现任 LEAD 可在"角色矩阵"里改动

### 7.3 功能性约束（新增 enforce）

- 审核任务校验 `reviewerId` ∈ `project_inspector(project_id, user_id, role)` 且 `role ∈ {REVIEWER, LEAD}` 或 admin
- 修改项目设置接口前置 `InspProjectAuthorizationGuard.assertCanEditSettings(projectId, userId)`：admin 直通，否则必须是 LEAD
- 移除 LEAD 角色校验剩余 LEAD ≥ 1

### 7.4 admin 后门

系统 `admin` 始终绕过 LEAD 约束（能编辑任何项目、审核任何任务），避免锁死。

### 7.5 数据迁移

`database/migrations/V20260524_1__inspection_lead_backfill.sql`：

```sql
INSERT INTO project_inspector (project_id, user_id, role, is_active, created_at, updated_at, org_unit_id)
SELECT p.id, p.created_by, 'LEAD', 1, NOW(), NOW(), p.org_unit_id
FROM insp_projects p
WHERE p.created_by IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM project_inspector pi
    WHERE pi.project_id = p.id
      AND pi.user_id = p.created_by
      AND pi.role = 'LEAD'
  );
```

迁移幂等可重复执行（`NOT EXISTS` 守护）。

---

## 8. 后端改动清单

### 8.1 Domain 层

- `ProjectInspector`：新增静态方法 `ensureLastLeadInvariant(remainingLeads, role)` — 抛 `LastLeadRemovalException`
- `InspProject.create()`：发布 `ProjectCreatedEvent`，由 listener 异步写入 LEAD
- 新事件 listener `AutoEnrollCreatorAsLeadHandler`，标 `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`（依据 memory 中的"跨 service 边界监听器约定"）

### 8.2 Application 层

- `InspProjectApplicationService.removeInspector()` 调 invariant 守护
- `InspTaskApplicationService.startReview()` 前置 `assertReviewerIsAuthorized(projectId, reviewerId)`
- 新增 `InspProjectAuthorizationGuard.assertCanEditSettings(projectId, userId)`，挂在所有 `PUT /inspection/projects/{id}/*` 切面

### 8.3 Infrastructure / DB

- 迁移 `V20260524_1__inspection_lead_backfill.sql`
- 校验 `project_inspector` 复合唯一索引 `(project_id, user_id, role)`，缺则补

### 8.4 新增 / 调整 API

- `POST /inspection/projects/{id}/inspectors/batch-assign` — 批量指派
- `POST /inspection/projects/{id}/inspectors/{userId}/roles/{role}` — 单独切换某角色（角色矩阵）
- `DELETE /inspection/projects/{id}/inspectors/{userId}/roles/{role}`
- `GET /inspection/projects/{id}/people-workbench` — 一次聚合返回 inspector + stats + 4 段任务分组，避免前端 N+1

---

## 9. 前端组件拆分

替换 `ProjectDetailView.vue:1104-1248`（144 行）：

```
views/inspection/projects/team/
├── TeamTab.vue                    # 容器 + 视图切换
├── TeamStatusBar.vue              # 顶部状态条
├── views/
│   ├── PeopleView.vue             # 「按人」主视图（折叠列表）
│   │   ├── PersonRow.vue          # 单行（折叠态）
│   │   └── PersonExpandedPanel.vue # 展开后的 4 段任务列表
│   ├── TasksView.vue              # 「按任务」主视图（tab+批量）
│   └── RoleMatrixView.vue         # 「角色矩阵」表格
└── composables/
    ├── usePeopleWorkbench.ts      # 聚合数据 + 排序
    └── useInspectorRoles.ts       # 角色 toggle + invariant 校验
```

`ProjectDetailView.vue` 第 1104-1248 行整段替换为 `<TeamTab :project-id="projectId" />`，主文件减重约 140 行。

---

## 10. 测试策略

### 10.1 后端

- `InspProjectAutoLeadEnrollmentTest` — 创建项目 → assert 1 LEAD
- `LastLeadRemovalGuardTest` — 移除最后 LEAD 抛异常
- `ReviewerAuthorizationTest` — 非 REVIEWER/LEAD 审核被拒
- `V20260524BackfillTest` — 迁移幂等 + 全项目至少 1 LEAD
- `InspProjectAuthorizationGuardTest` — admin 直通 / LEAD 直通 / 其他拒绝

### 10.2 前端

- `TeamTab.spec.ts` — 三视图切换
- `PersonRow.spec.ts` — 排序 / 徽章 / 默认展开规则
- `RoleMatrixView.spec.ts` — toggle / LEAD invariant 拦截
- Playwright e2e — "创建项目 → 自动 LEAD → 添加检查员 → 按人视图分配 → 切换角色矩阵 → 尝试移除最后 LEAD 失败"

### 10.3 守护闸 (ratchet)

- 新增 ArchUnit `InspectionRoleAuthorizationGuardTest` — 禁止 `application/inspection/**` 在审核/编辑动作里绕过 `InspProjectAuthorizationGuard`
- `type-check:ceiling` 不退化
- `id-number-check` baseline 0 不退化

---

## 11. 风险与取舍

| 风险 | 应对 |
|---|---|
| 存量项目 createdBy 已离职 → 回填的 LEAD 是离职用户 | 迁移后跑一次"清单 + 告警"脚本输出离职 LEAD 项目，由 admin 重指派 |
| `AFTER_COMMIT` listener 失败导致项目无 LEAD | `fallbackExecution=true` + 应用层补偿 `ensureProjectHasLead(projectId)` 在每次进入团队页时校验 |
| REVIEWER 授权变严打断现有审核流 | 灰度：先发 warning 日志一周观察被拒频次，再切硬拒绝 |
| 多 LEAD 互删风险 | UI 上 LEAD 行的"移除"按钮在 `leadCount === 1` 时禁用并 tooltip |
| `(project_id, user_id, role)` 复合主键如不存在需建索引 | 迁移前先 `SHOW CREATE TABLE project_inspector` 确认，缺则在 `V20260524_2` 补建 |

---

## 12. 实施顺序（粗）

1. **Phase A — 后端 LEAD 语义化** (DB 迁移 + Domain + Application + Authorization Guard + 测试)
2. **Phase B — 后端新 API** (people-workbench / batch-assign / role toggle)
3. **Phase C — 前端 TeamTab 容器 + 顶部状态条**
4. **Phase D — 「按人」视图** (主战场，含 PersonRow / PersonExpandedPanel)
5. **Phase E — 「按任务」视图** (含批量操作)
6. **Phase F — 「角色矩阵」视图**
7. **Phase G — e2e + ArchUnit 守护闸 + ratchet 验收**

每个 Phase 独立 commit，可单独 review。

---

## 13. 决策记录

| 决策 | 选项 | 选定 | 理由 |
|---|---|---|---|
| 重构方向 | A紧凑 / B以人为中心 / C换皮 / D角色管理 | **B + 部分 D** | 工作流割裂是真痛点，纯换皮浪费窗口 |
| LEAD 处理 | A保留 / B改造 / C删除 | **B 改造** | 迁移成本最低且解决"创建者隐身"真痛点 |
| 视图数量 | 2 个 / 3 个 | **3 个**（按人/按任务/角色矩阵） | 各自服务不同心智模型 |
| 默认展开 | 全折叠 / 有待办展开 | **有待办展开** | 紧凑性与行动性兼顾 |
| 进度周期 | 周 / 月 | **周** | 反馈更准 |
| 排序 | 手动 / 固定 | **固定按忙碌度倒序** | 减少决策成本 |
| admin 后门 | 守约束 / 后门 | **admin 后门** | 避免锁死 |
| 同人多角色 | 多行 / 单行多字段 | **多行** | 沿用现有复合主键，语义清晰 |
| 角色矩阵批量 | 支持 / 不支持 | **不支持** | 避免误操作，逐行足够 |

---

## 14. 不在本设计范围

- 检查员**跨项目**的负载视图（属于全局人力分析，另起设计）
- 自动调度算法（"系统自动指派给最闲的人"）— 当前只做手动指派 + 候选标签
- 通知推送（催办按钮当前依赖既有事件触发器系统，不在本设计扩展）
