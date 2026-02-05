# V4 量化检查系统全面重构计划

> 创建日期: 2026-01-30
> 基于: quantification-v4-comprehensive-redesign.md
> 状态: 待执行

---

## 一、执行摘要

经过全面审查，V4系统的实现情况如下：

| 层级 | 完成度 | 关键问题 |
|------|--------|----------|
| **数据库** | 95% | 表结构完整，已有V4迁移脚本 |
| **后端API** | 85% | V4核心API完整，但有旧版JDBC控制器存在SQL注入风险 |
| **前端组件** | 70% | V4组件存在但大部分使用Mock数据，未连接后端API |
| **路由配置** | 50% | 路由仍指向V3组件（quantification/目录）|
| **数据流** | 40% | 前后端未打通，检查计划仍使用daily_checks而非inspection_sessions |

**核心问题**: V4后端已完成，但前端仍在使用V3组件和旧API。

---

## 二、详细差距分析

### 2.1 数据库层 (95% 完成)

**V4表结构完整清单** ✅

| 表名 | 用途 | 状态 |
|------|------|------|
| `inspection_sessions` | 检查会话聚合根 | ✅ 已创建 |
| `class_inspection_records` | 班级检查记录聚合根 | ✅ 已创建 |
| `inspection_deductions` | 扣分明细 | ✅ 已创建 |
| `checklist_responses` | 逐项核验响应 | ✅ 已创建 |
| `bonus_items` | 加分项配置 | ✅ 已创建 |
| `inspection_bonuses` | 加分明细 | ✅ 已创建 |
| `corrective_actions` | 整改工单 | ✅ 已创建 |
| `auto_action_rules` | 自动创建规则 | ✅ 已创建 |
| `student_behavior_records` | 学生行为记录 | ✅ 已创建 |
| `student_behavior_alerts` | 学生行为预警 | ✅ 已创建 |
| `schedule_policies` | 排班策略 | ✅ 已创建 |
| `schedule_executions` | 排班执行记录 | ✅ 已创建 |
| `analytics_snapshots` | 分析快照 | ✅ 已创建 |

**遗留旧表** (需要数据迁移后废弃)

| 旧表名 | 替代方案 |
|--------|----------|
| `daily_checks` | → `inspection_sessions` |
| `daily_check_categories` | → `inspection_categories` (模板快照) |
| `daily_check_targets` | → `class_inspection_records` |
| `check_record_class_stats` | → `class_inspection_records` |

---

### 2.2 后端API层 (85% 完成)

#### V4 DDD架构控制器 ✅ (推荐使用)

| 控制器 | 路由前缀 | 功能 | 状态 |
|--------|----------|------|------|
| `InspectionSessionController` | `/inspection/sessions` | 会话管理、录入扣分 | ✅ 完整 |
| `InspectionTemplateController` | `/inspection-templates` | 模板管理 | ✅ 完整 |
| `InspectionRecordController` | `/inspection-records` | 检查记录 | ✅ 完整 |
| `BonusItemController` | `/inspection/bonus-items` | 加分项管理 | ✅ 完整 |
| `AppealController` | `/appeals` | 申诉管理(9状态) | ✅ 完整 |
| `TeacherDashboardController` | `/teacher-dashboard` | 班主任工作台 | ✅ 完整 |
| `DepartmentRankingController` | `/inspection/department-ranking` | 系部排名 | ✅ 完整 |
| `ExportCenterController` | `/export-center` | 数据导出 | ✅ 完整 |
| `SpaceQueryController` | `/inspection/spaces` | 物理空间查询 | ✅ 完整 |

#### 旧版JDBC控制器 ⚠️ (存在安全问题)

| 控制器 | 路由前缀 | 问题 | 建议 |
|--------|----------|------|------|
| `CheckPlanController` | `/check-plans` | SQL注入风险 | 重写或废弃 |
| `DailyCheckController` | `/quantification/daily-checks` | SQL注入风险 | 废弃，改用InspectionSession |
| `CheckRecordController` | `/check-records` | SQL注入风险 | 废弃，改用InspectionRecord |
| `DeductionItemController` | `/deduction-items` | 混合架构 | 迁移到DDD |

#### 缺失的API ❌

| API | V4设计 | 当前状态 |
|-----|--------|----------|
| 检查计划与会话关联 | `POST /inspection/sessions?planId=xxx` | ❌ 无planId支持 |
| 我的检查任务 | `/my-check-tasks` | ❌ 空实现 |
| 传统按班级录入 | ORG_FIRST模式 | ❌ 未实现 |

---

### 2.3 前端组件层 (70% 完成)

#### inspection/ 目录V4组件状态

| 组件 | 功能 | UI完成度 | API集成 | 问题 |
|------|------|----------|---------|------|
| `InspectionConfig.vue` | 配置中心5Tab | 95% | ❌ Mock数据 | 需要集成后端API |
| `InspectionExecute.vue` | 4种录入模式 | 85% | ✅ 部分 | ORG_FIRST未实现 |
| `InspectionPlanList.vue` | 计划列表 | 90% | ❌ Mock数据 | 需要集成后端API |
| `TeacherDashboard.vue` | 班主任工作台 | 80% | ✅ 部分 | classId硬编码为0 |
| `CorrectiveActionListView.vue` | 整改工单 | 85% | ✅ 完整 | 基本可用 |
| `BehaviorRecordListView.vue` | 学生行为 | 85% | ✅ 完整 | 基本可用 |
| `ScheduleManagementView.vue` | 排班策略 | 80% | ✅ 完整 | 基本可用 |
| `DataAnalyticsCenterView.vue` | 数据分析 | 50% | ✅ 集成 | 缺少图表库 |
| `RankingResults.vue` | 排名结果 | 70% | ❌ 无路由 | 需添加路由 |
| `AppealManagement.vue` | 申诉管理 | 70% | ❌ Mock数据 | 路由指向V3组件 |

#### 路由指向问题 (核心问题)

**当前路由配置** (`router/index.ts`):

```typescript
// 这些路由指向V3组件，需要修改
'/inspection/check-plan'     → quantification/CheckPlanListView.vue    // 应改为 inspection/InspectionPlanList.vue
'/inspection/check-plan/:id' → quantification/CheckPlanDetailView.vue  // 应新建 inspection/InspectionPlanDetail.vue
'/inspection/check-record-scoring' → quantification/CheckRecordScoring.vue // 应改为 inspection/InspectionExecute.vue
'/inspection/appeals-v3'     → quantification/AppealManagement.vue      // 应改为 inspection/AppealManagement.vue
```

---

### 2.4 数据流断裂分析

**V4设计的数据流**:
```
检查计划 → 创建检查会话(inspection_session) → 4种录入模式执行 → 提交/发布 → 生成结果
         POST /inspection/sessions
```

**当前实际数据流**:
```
检查计划 → 创建日常检查(daily_check) → 旧版打分页面 → 提交
         POST /quantification/daily-checks
```

**断裂点**:
1. `CheckPlanDetailView.vue` 调用 `/quantification/daily-checks` 而非 `/inspection/sessions`
2. "打分"按钮导航到 `/quantification/check-record-scoring` 而非 `/inspection/execute/:sessionId`
3. `InspectionExecute.vue` 虽然存在但未被主流程使用

---

## 三、完整重构任务清单

### Phase 1: 安全修复 (紧急 - 本周完成)

#### 1.1 修复SQL注入漏洞

| 任务 | 文件 | 优先级 |
|------|------|--------|
| 修复CheckPlanController | `CheckPlanController.java` | P0 |
| 修复DailyCheckController | `DailyCheckController.java` | P0 |
| 修复CheckRecordController | `CheckRecordController.java` | P0 |

**示例修复**:
```java
// 危险代码:
sql.append(" AND plan_name LIKE '%").append(keyword).append("%'");

// 修复为:
sql.append(" AND plan_name LIKE ?");
jdbcTemplate.queryForList(sql.toString(), "%" + keyword + "%");
```

---

### Phase 2: 前端核心页面连接V4 API (1周)

#### 2.1 新建检查计划详情页V4版本

**新建文件**: `frontend/src/views/inspection/InspectionPlanDetail.vue`

**功能需求**:
- [ ] 显示计划基本信息（名称、模板、时间范围、状态）
- [ ] 显示关联的inspection_sessions列表（而非daily_checks）
- [ ] "新建检查"按钮调用 `POST /inspection/sessions?planId=xxx`
- [ ] "打分"按钮导航到 `/inspection/execute/:sessionId`
- [ ] 统计数据从V4表获取

**API调用修改**:
```typescript
// 旧代码 (需要替换):
await http.post('/quantification/daily-checks', {...})

// 新代码:
await http.post('/inspection/sessions', {
  templateId: planDetail.value?.templateId,
  planId: planId.value,  // 需要后端支持
  inspectionDate: form.checkDate,
  inputMode: 'SPACE_FIRST',
  scoringMode: 'DEDUCTION_ONLY',
  ...
})
```

#### 2.2 完善InspectionPlanList API集成

**文件**: `frontend/src/views/inspection/InspectionPlanList.vue`

**任务**:
- [ ] 替换Mock数据为真实API调用
- [ ] 调用 `GET /check-plans` (或新建V4版CheckPlan API)
- [ ] 添加新建计划功能
- [ ] 添加删除/编辑功能

#### 2.3 完善InspectionConfig API集成

**文件**: `frontend/src/views/inspection/InspectionConfig.vue`

**任务**:
- [ ] 模板管理Tab: 调用 `GET/POST/PUT /inspection-templates`
- [ ] 权重方案Tab: 调用权重配置API
- [ ] 评级规则Tab: 调用评级规则API
- [ ] 调度策略Tab: 调用 `GET/POST /schedule/policies`
- [ ] 整改规则Tab: 调用 `GET/POST /auto-action-rules`

#### 2.4 修改路由配置

**文件**: `frontend/src/router/index.ts`

```typescript
// 修改1: 检查计划列表 → V4组件
{
  path: '/inspection/check-plan',
  component: () => import('@/views/inspection/InspectionPlanList.vue'),  // 改
}

// 修改2: 检查计划详情 → V4组件
{
  path: '/inspection/check-plan/:id',
  component: () => import('@/views/inspection/InspectionPlanDetail.vue'), // 新建
}

// 修改3: 打分 → 重定向到execute
{
  path: '/inspection/check-record-scoring',
  redirect: to => `/inspection/execute/${to.query.checkId}`,  // 或删除
}

// 修改4: 申诉管理 → V4组件
{
  path: '/inspection/appeals',
  component: () => import('@/views/inspection/AppealManagement.vue'),  // 改
}

// 新增: 排名结果页
{
  path: '/inspection/ranking/:sessionId',
  component: () => import('@/views/inspection/RankingResults.vue'),
}
```

---

### Phase 3: 后端API完善 (1周)

#### 3.1 添加planId支持到InspectionSessionController

**文件**: `InspectionSessionController.java`

```java
// 修改CreateSessionCommand，添加planId
@PostMapping
public Result<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
    CreateSessionCommand command = CreateSessionCommand.builder()
        .planId(request.getPlanId())  // 新增
        .templateId(request.getTemplateId())
        // ...
        .build();
}

// 新增：按计划查询会话
@GetMapping
public Result<List<SessionResponse>> listSessions(
    @RequestParam(required = false) Long planId,  // 新增
    @RequestParam @DateTimeFormat LocalDate startDate,
    @RequestParam @DateTimeFormat LocalDate endDate) {
    // ...
}
```

#### 3.2 实现MyCheckTaskController

**文件**: `MyCheckTaskController.java` (当前空实现)

```java
// 需要实现:
GET /my-check-tasks                 - 我的检查任务列表
GET /my-check-tasks/pending-count   - 待完成任务数
GET /my-check-tasks/{id}            - 任务详情
POST /my-check-tasks/{id}/start     - 开始任务
```

#### 3.3 实现ORG_FIRST录入模式

**文件**: `InspectionSessionController.java`

```java
// 新增：传统按班级录入
@PostMapping("/{id}/org-deductions")
public Result<ClassRecordResponse> recordOrgDeduction(
    @PathVariable Long id,
    @Valid @RequestBody OrgDeductionRequest request) {
    // 直接指定classId，不需要解析
}
```

---

### Phase 4: InspectionExecute完善 (3天)

#### 4.1 完成ORG_FIRST模式

**文件**: `frontend/src/views/inspection/InspectionExecute.vue`

**当前状态**: 仅有占位符 "传统按班级模式开发中"

**需要实现**:
- [ ] 班级选择下拉框
- [ ] 选择班级后显示其关联宿舍/教室
- [ ] 扣分录入面板
- [ ] 调用 `POST /inspection/sessions/{id}/org-deductions`

#### 4.2 修复学生搜索

**当前问题**: 使用Mock数据

**修复**:
```typescript
// 替换Mock搜索为真实API
const searchStudents = async (keyword: string) => {
  const response = await http.get('/students/quick-search', {
    params: { keyword, limit: 10 }
  })
  return response.data
}
```

#### 4.3 完善拍照功能

**当前问题**: 仅有占位符

**需要实现**:
- [ ] 调用摄像头拍照或选择文件
- [ ] 上传到文件服务器
- [ ] 返回URL存入evidence_urls

---

### Phase 5: 数据分析升级 (3天)

#### 5.1 引入图表库

**文件**: `DataAnalyticsCenterView.vue`

**任务**:
- [ ] 安装ECharts: `npm install echarts vue-echarts`
- [ ] 班级排名 → 横向柱状图
- [ ] 违规分布 → 饼图
- [ ] 检查员工作量 → 堆叠柱状图
- [ ] 系部对比 → 雷达图

#### 5.2 完善TeacherDashboard

**文件**: `TeacherDashboard.vue`

**任务**:
- [ ] 修复classId获取（从用户上下文或路由参数）
- [ ] 趋势图改用ECharts
- [ ] 添加数据导出功能

---

### Phase 6: 清理与迁移 (1周)

#### 6.1 数据迁移脚本

```sql
-- 将daily_checks数据迁移到inspection_sessions
INSERT INTO inspection_sessions (id, session_code, template_id, inspection_date, ...)
SELECT id, CONCAT('SES-', DATE_FORMAT(check_date, '%Y%m%d'), '-', id),
       template_id, check_date, ...
FROM daily_checks WHERE deleted = 0;

-- 迁移扣分数据
INSERT INTO inspection_deductions (...)
SELECT ... FROM deduction_details WHERE ...;
```

#### 6.2 废弃旧组件

**待废弃文件列表**:

```
前端 (迁移完成后删除):
├── views/quantification/CheckPlanDetailView.vue      # 6000+行V3巨型组件
├── views/quantification/CheckPlanListView.vue
├── views/quantification/CheckPlanCreateView.vue
├── views/quantification/CheckRecordScoring.vue
├── views/quantification/DailyCheckView.vue
├── views/quantification/AppealManagement.vue
└── views/quantification/QuantificationUnifiedView.vue

后端 (标记@Deprecated后删除):
├── DailyCheckController.java
├── CheckPlanController.java (重写或废弃)
└── CheckRecordController.java
```

---

## 四、优先级排序

### P0 - 本周必须完成 (安全问题)
1. ✅ 修复SQL注入漏洞

### P1 - 下周完成 (核心流程打通)
2. 新建InspectionPlanDetail.vue
3. 修改路由指向V4组件
4. "新建检查"调用V4 API
5. "打分"导航到InspectionExecute

### P2 - 2周内完成 (功能完善)
6. InspectionConfig API集成
7. InspectionPlanList API集成
8. ORG_FIRST模式实现
9. 学生搜索API集成

### P3 - 3周内完成 (用户体验)
10. 数据分析图表升级
11. TeacherDashboard完善
12. 拍照功能实现

### P4 - 1个月内完成 (清理)
13. 数据迁移
14. 废弃旧组件
15. 文档更新

---

## 五、验收标准

### 核心流程验收

1. **检查计划 → 创建会话 → 执行 → 发布**
   - [ ] 从计划列表进入计划详情
   - [ ] 点击"新建检查"创建inspection_session
   - [ ] 进入InspectionExecute页面
   - [ ] 使用4种模式中的任意一种录入扣分
   - [ ] 提交检查会话
   - [ ] 发布检查结果

2. **配置中心**
   - [ ] 5个Tab页都能正常加载数据
   - [ ] 模板CRUD功能正常
   - [ ] 权重方案/评级规则/调度策略/整改规则可配置

3. **数据分析**
   - [ ] 班级排名显示图表
   - [ ] 违规分布显示饼图
   - [ ] 数据可导出

4. **安全验收**
   - [ ] 无SQL注入漏洞
   - [ ] 所有API有权限检查

---

## 六、风险与依赖

### 风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| V3数据与V4表不兼容 | 历史数据丢失 | 编写详细迁移脚本并备份 |
| 前端改动范围大 | 引入新bug | 充分测试，渐进式迁移 |
| 用户习惯改变 | 用户困惑 | 保留旧路由一段时间，添加引导 |

### 依赖

- 后端planId支持需要先完成，前端才能连接
- 数据迁移需要停机维护窗口
- ECharts安装需要前端构建

---

## 七、快速开始

如果要立即开始重构，按以下顺序执行：

```bash
# Step 1: 修复安全问题 (今天)
# 修改 CheckPlanController.java 中的SQL拼接

# Step 2: 新建V4计划详情页 (明天)
# 创建 frontend/src/views/inspection/InspectionPlanDetail.vue

# Step 3: 修改路由 (后天)
# 修改 frontend/src/router/index.ts

# Step 4: 测试流程 (本周)
# 检查计划 → 新建检查(V4) → 打分(InspectionExecute) → 提交发布
```

---

## 附录：文件修改清单

### 需要新建的文件
- [ ] `frontend/src/views/inspection/InspectionPlanDetail.vue`

### 需要修改的文件
- [ ] `frontend/src/router/index.ts` - 路由配置
- [ ] `frontend/src/views/inspection/InspectionPlanList.vue` - API集成
- [ ] `frontend/src/views/inspection/InspectionConfig.vue` - API集成
- [ ] `frontend/src/views/inspection/InspectionExecute.vue` - ORG_FIRST模式
- [ ] `frontend/src/views/inspection/TeacherDashboard.vue` - classId修复
- [ ] `frontend/src/views/inspection/DataAnalyticsCenterView.vue` - 图表升级
- [ ] `backend/.../InspectionSessionController.java` - planId支持
- [ ] `backend/.../MyCheckTaskController.java` - 完整实现
- [ ] `backend/.../CheckPlanController.java` - SQL注入修复

### 需要废弃的文件 (后期删除)
- [ ] `frontend/src/views/quantification/CheckPlanDetailView.vue`
- [ ] `frontend/src/views/quantification/CheckRecordScoring.vue`
- [ ] `frontend/src/views/quantification/DailyCheckView.vue`
- [ ] `backend/.../DailyCheckController.java`

---

*本文档将随重构进展持续更新*
