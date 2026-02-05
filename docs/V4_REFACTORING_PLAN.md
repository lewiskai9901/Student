# V4 量化检查系统重构计划

> 创建日期: 2026-01-30
> 状态: 待执行

---

## 一、问题概述

当前系统存在 **V3 与 V4 并行** 的问题，导致：
1. 路由配置使用 V3 组件（`quantification/` 目录）
2. V4 组件已实现但未被使用（`inspection/` 目录）
3. 后端存在两套 API：`DailyCheckController`（旧）vs `InspectionSessionController`（V4）
4. 数据流断裂：V4 的 `inspection_sessions` 表未被前端使用

---

## 二、当前状态对比

### 2.1 前端组件对比

| V4 设计要求 | V4 已实现组件 | 路由当前使用的组件 | 状态 |
|------------|--------------|------------------|------|
| 配置中心 | `inspection/InspectionConfig.vue` | `inspection/InspectionConfig.vue` | ✅ 已对齐 |
| 检查计划列表 | 需新建 | `quantification/CheckPlanListView.vue` | ❌ 用V3 |
| 检查计划详情 | 需新建 | `quantification/CheckPlanDetailView.vue` | ❌ 用V3 |
| 创建检查计划 | 需新建 | `quantification/CheckPlanCreateView.vue` | ❌ 用V3 |
| 检查执行(4模式) | `inspection/InspectionExecute.vue` | `quantification/CheckRecordScoring.vue` | ❌ 用V3 |
| 班主任工作台 | `inspection/TeacherDashboard.vue` | `inspection/TeacherDashboard.vue` | ✅ 已对齐 |
| 整改工单 | `inspection/CorrectiveActionListView.vue` | `inspection/CorrectiveActionListView.vue` | ✅ 已对齐 |
| 学生行为 | `inspection/BehaviorRecordListView.vue` | `inspection/BehaviorRecordListView.vue` | ✅ 已对齐 |
| 自动排班 | `inspection/ScheduleManagementView.vue` | `inspection/ScheduleManagementView.vue` | ✅ 已对齐 |
| 数据分析 | `inspection/DataAnalyticsCenterView.vue` | `inspection/DataAnalyticsCenterView.vue` | ✅ 已对齐 |
| 导出中心 | `inspection/ExportCenterView.vue` | `inspection/ExportCenterView.vue` | ✅ 已对齐 |
| 排名结果 | `inspection/RankingResults.vue` | 无对应路由 | ❌ 未使用 |
| 申诉管理 | `inspection/AppealManagement.vue` | `quantification/AppealManagement.vue` | ❌ 用V3 |

### 2.2 后端API对比

| 功能 | V4 设计API | 当前使用API | 状态 |
|------|-----------|-------------|------|
| 创建检查会话 | `POST /inspection/sessions` | `POST /quantification/daily-checks` | ❌ 用旧API |
| 获取检查会话 | `GET /inspection/sessions/{id}` | `GET /quantification/daily-checks/{id}` | ❌ 用旧API |
| 空间扣分 | `POST /inspection/sessions/{id}/space-deductions` | 无 | ❌ 未使用 |
| 人员扣分 | `POST /inspection/sessions/{id}/person-deductions` | 无 | ❌ 未使用 |
| 逐项核验 | `POST /inspection/sessions/{id}/checklist-responses` | 无 | ❌ 未使用 |
| 提交/发布 | `PATCH /inspection/sessions/{id}/status` | 无 | ❌ 未使用 |

### 2.3 数据表对比

| V4 设计表 | 当前使用表 | 状态 |
|----------|-----------|------|
| `inspection_sessions` | `daily_checks` | ❌ 未对齐 |
| `class_inspection_records` | `check_record_class_stats` | ❌ 未对齐 |
| `checklist_responses` | 无 | ❌ 未使用 |
| `bonus_records` | 无 | ❌ 未使用 |

---

## 三、重构任务清单

### Phase 1: 前端核心页面重构 (优先级最高)

#### 1.1 检查计划列表页 - 新建 V4 版本
**文件**: `frontend/src/views/inspection/InspectionPlanList.vue` (已存在，需完善)

**当前问题**:
- 路由 `/inspection/check-plan` 指向 `quantification/CheckPlanListView.vue`

**重构内容**:
```
□ 修改路由指向 inspection/InspectionPlanList.vue
□ 确保 InspectionPlanList.vue 调用 V4 API
□ 支持创建/查看/删除检查计划
□ 显示关联的 inspection_sessions 数量
```

#### 1.2 检查计划详情页 - 新建 V4 版本
**文件**: 新建 `frontend/src/views/inspection/InspectionPlanDetail.vue`

**当前问题**:
- 路由指向 `quantification/CheckPlanDetailView.vue` (6000+ 行的巨型组件)
- 创建"日常检查"调用旧 API `/quantification/daily-checks`
- "打分"按钮导航到 `/quantification/check-record-scoring`

**重构内容**:
```
□ 新建 InspectionPlanDetail.vue (V4 架构)
□ 显示计划基本信息
□ 显示关联的 inspection_sessions 列表
□ "新建检查" 调用 POST /inspection/sessions
□ "打分" 导航到 /inspection/execute/:sessionId
□ 支持查看会话详情和进度
□ 统计分析使用 V4 数据源
```

#### 1.3 检查执行页 - 连接到主流程
**文件**: `frontend/src/views/inspection/InspectionExecute.vue` (已存在)

**当前问题**:
- 组件存在但未被主流程使用
- 检查计划详情页的"打分"导航到旧版 CheckRecordScoring.vue

**重构内容**:
```
□ InspectionPlanDetail 的"打分"导航到此页面
□ 接收 sessionId 参数
□ 4种录入模式调用 V4 API:
  - 物理空间: POST /inspection/sessions/{id}/space-deductions
  - 人员搜索: POST /inspection/sessions/{id}/person-deductions
  - 逐项核验: POST /inspection/sessions/{id}/checklist-responses
  - 按班级: POST /inspection/sessions/{id}/space-deductions (classId)
□ 提交功能: PATCH /inspection/sessions/{id}/status?action=submit
□ 发布功能: PATCH /inspection/sessions/{id}/status?action=publish
```

#### 1.4 排名结果页 - 新建路由
**文件**: `frontend/src/views/inspection/RankingResults.vue` (已存在)

**当前问题**:
- 组件存在但无路由配置

**重构内容**:
```
□ 添加路由 /inspection/ranking/:sessionId
□ 显示会话的排名结果
□ 支持导出
```

### Phase 2: 路由配置修改

**文件**: `frontend/src/router/index.ts`

```typescript
// 需要修改的路由:

// 1. 检查计划列表 - 改用V4组件
{
  path: '/inspection/check-plan',
  component: () => import('@/views/inspection/InspectionPlanList.vue'), // 改
}

// 2. 检查计划详情 - 改用V4组件
{
  path: '/inspection/check-plan/:id',
  component: () => import('@/views/inspection/InspectionPlanDetail.vue'), // 新建
}

// 3. 删除或隐藏旧版打分路由
{
  path: '/inspection/check-record-scoring',
  // 删除或重定向到 /inspection/execute
}

// 4. 添加排名结果路由
{
  path: '/inspection/ranking/:sessionId',
  component: () => import('@/views/inspection/RankingResults.vue'),
}

// 5. 申诉管理 - 改用V4组件
{
  path: '/inspection/appeals',
  component: () => import('@/views/inspection/AppealManagement.vue'), // 改
}
```

### Phase 3: 后端API完善

#### 3.1 检查计划与会话的关联
**文件**: `InspectionSessionController.java`

**重构内容**:
```
□ 添加 planId 参数支持
□ POST /inspection/sessions 接收 planId
□ GET /inspection/sessions?planId=xxx 按计划筛选
□ 会话创建时关联 check_plans 表
```

#### 3.2 废弃旧版 DailyCheckController
**文件**: `DailyCheckController.java`

**重构内容**:
```
□ 标记为 @Deprecated
□ 前端迁移完成后删除
□ 数据迁移脚本: daily_checks → inspection_sessions
```

### Phase 4: 可废弃的 V3 组件清单

以下组件在 V4 完成后可删除：

```
frontend/src/views/quantification/
├── CheckPlanListView.vue        → 替换为 inspection/InspectionPlanList.vue
├── CheckPlanDetailView.vue      → 替换为 inspection/InspectionPlanDetail.vue
├── CheckPlanCreateView.vue      → 整合到 InspectionPlanList 的弹窗
├── CheckRecordScoring.vue       → 替换为 inspection/InspectionExecute.vue
├── DailyCheckView.vue           → 废弃，功能整合到计划详情
├── CheckRecordListView.vue      → 整合到计划详情
├── CheckRecordDetailView.vue    → 保留或重构
├── AppealManagement.vue         → 替换为 inspection/AppealManagement.vue
└── QuantificationUnifiedView.vue → 已废弃
```

---

## 四、实施路线图

### Week 1: 核心流程打通
1. 新建 `InspectionPlanDetail.vue` (简化版)
2. 修改路由指向新组件
3. "新建检查" 调用 V4 API
4. "打分" 导航到 InspectionExecute.vue

### Week 2: InspectionExecute 完善
1. 确保4种录入模式都调用正确的V4 API
2. 实现提交/发布功能
3. 测试完整流程

### Week 3: 统计与排名
1. 连接 RankingResults.vue
2. 完善数据分析页面数据源
3. 班主任工作台数据对接

### Week 4: 清理与迁移
1. 数据迁移脚本
2. 废弃旧组件
3. 文档更新

---

## 五、风险与注意事项

1. **数据兼容性**: 已有的 `daily_checks` 数据需要迁移到 `inspection_sessions`
2. **权限配置**: 新 API 需要配置相应权限
3. **渐进式迁移**: 可以保留旧路由一段时间，通过功能开关切换
4. **测试覆盖**: 每个阶段需要完整的端到端测试

---

## 六、快速开始

如果要立即开始重构，建议按以下顺序：

```bash
# Step 1: 新建V4计划详情页
frontend/src/views/inspection/InspectionPlanDetail.vue

# Step 2: 修改路由
frontend/src/router/index.ts
  - /inspection/check-plan/:id → InspectionPlanDetail.vue

# Step 3: 测试流程
检查计划列表 → 点击计划 → 新建检查(V4 API) → 打分(InspectionExecute) → 提交发布
```

---

## 附录: 需要新建/修改的文件清单

### 新建文件
- [ ] `frontend/src/views/inspection/InspectionPlanDetail.vue`

### 修改文件
- [ ] `frontend/src/router/index.ts` - 路由配置
- [ ] `frontend/src/views/inspection/InspectionPlanList.vue` - 完善功能
- [ ] `frontend/src/views/inspection/InspectionExecute.vue` - API对接
- [ ] `backend/.../InspectionSessionController.java` - 添加planId支持

### 废弃文件 (后续删除)
- [ ] `frontend/src/views/quantification/CheckPlanDetailView.vue`
- [ ] `frontend/src/views/quantification/CheckRecordScoring.vue`
- [ ] `frontend/src/views/quantification/DailyCheckView.vue`
- [ ] `backend/.../DailyCheckController.java`
