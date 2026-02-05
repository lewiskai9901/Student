# V6 检查系统完善计划

> **日期**: 2026-02-01
> **目标**: 移除V4，完善V6，实现设计文档中的全部功能

---

## 一、差距分析总结

### 设计 vs 实现对比

| 模块 | 设计状态 | 实现状态 | 差距 |
|------|---------|---------|------|
| 实体类型配置 | ✅ 完整设计 | ❌ 未实现 | 缺失 |
| 动态分组 | ✅ 完整设计 | ❌ 未实现 | 缺失 |
| 检查模板 | ✅ 完整设计 | ⚠️ 复用V4 | 需增强 |
| 检查项目 | ✅ 完整设计 | ⚠️ 部分实现 | 缺向导 |
| 检查任务 | ✅ 完整设计 | ✅ 基本实现 | 小调整 |
| **检查执行** | ✅ 完整设计 | ❌ 简化版 | **核心缺失** |
| 汇总统计 | ✅ 完整设计 | ✅ 基本实现 | 小调整 |
| 打分策略 | ✅ 完整设计 | ❌ 未实现 | 缺失 |

---

## 二、未完成功能清单

### Phase 1: 核心缺失 - 检查执行系统 (优先级: P0)

#### 1.1 后端 - 检查执行API (`/execution`)

```
缺失的API端点:
├── GET  /execution/tasks/:taskId/targets - 获取检查目标列表
├── GET  /execution/tasks/:taskId/targets/tree - 获取目标树形结构（递归部门）
├── POST /execution/targets/:targetId/lock - 锁定目标
├── POST /execution/targets/:targetId/unlock - 解锁目标
├── POST /execution/targets/:targetId/extend-lock - 延长锁定
├── POST /execution/targets/:targetId/details - 添加检查明细
├── POST /execution/targets/:targetId/details/batch - 批量添加明细
├── PUT  /execution/targets/:targetId/details/:detailId - 更新明细
├── DELETE /execution/targets/:targetId/details/:detailId - 删除明细
├── GET  /execution/targets/:targetId/details - 获取明细列表
├── POST /execution/targets/:targetId/complete - 完成检查
├── POST /execution/targets/:targetId/skip - 跳过检查
├── POST /execution/evidence/upload - 上传证据
├── DELETE /execution/evidence/:id - 删除证据
├── GET  /execution/quick-items - 常用检查项
└── POST /execution/targets/:targetId/quick-deduct - 快捷扣分
```

需要创建:
- [ ] `InspectionExecutionController.java`
- [ ] `InspectionExecutionApplicationService.java`
- [ ] `InspectionDetail.java` 领域实体
- [ ] `InspectionEvidence.java` 领域实体
- [ ] 对应的Repository和Mapper

#### 1.2 前端 - 检查执行界面 (PC端双栏布局)

**当前**: `TaskExecute.vue` 是简化版，缺少递归组织树和完整打分功能

**需要实现的设计**:
```
┌──────────────────────────┬──────────────────────────────────────────┐
│ 左侧：组织树（递归结构）    │ 右侧：打分区域                            │
├──────────────────────────┼──────────────────────────────────────────┤
│ 🔍 搜索...                │ 📍 经信系 › 软件1班 › 宿舍101             │
│                          │                                          │
│ ▼ 📁 经济与信息技术系     │ 🏠 宿舍101 (8人间)                        │
│ │   进度: 15/35          │ 当前: 93分 (基准100 - 扣7)                │
│ │                        │                                          │
│ │   ▼ 📚 软件1班         │ 📝 检查项                                 │
│ │   │   ├─ 101 ● 检查中  │ ┌────────────────────────────────────┐  │
│ │   │   ├─ 102 ✓ 95分    │ │ 卫生类 (权重40%)           扣5分    │  │
│ │   │   ├─ 103 ✓ 88分    │ │ ☑ 地面不清洁        -3分           │  │
│ │   │   └─ ...           │ │ ☑ 床铺不整洁(张三)  -2分/人        │  │
│ │   │                    │ └────────────────────────────────────┘  │
│ │   ▶ 📚 软件2班         │                                          │
│ ...                      │ 📷 证据上传  [+ 上传照片]                 │
└──────────────────────────┴──────────────────────────────────────────┘
```

需要创建/重写:
- [ ] `TaskExecute.vue` - 完整重写
- [ ] `components/inspection/TargetTree.vue` - 递归目标树组件
- [ ] `components/inspection/ScoringPanel.vue` - 打分面板组件
- [ ] `components/inspection/DeductionSelector.vue` - 扣分项选择器
- [ ] `components/inspection/IndividualPicker.vue` - 个体（学生）选择器
- [ ] `components/inspection/EvidenceUploader.vue` - 证据上传组件

#### 1.3 数据库 - 检查明细表增强

当前 `inspection_deductions` 和 `inspection_bonuses` 是简化版，需要增强为支持多种打分模式的 `inspection_details` 表：

```sql
-- 需要增加的表
CREATE TABLE inspection_details (
    id BIGINT PRIMARY KEY,
    target_record_id BIGINT NOT NULL,

    -- 检查项信息
    category_id BIGINT,
    category_code VARCHAR(50),
    category_name VARCHAR(100),
    item_id BIGINT,
    item_code VARCHAR(50),
    item_name VARCHAR(100),

    -- 作用范围
    scope ENUM('WHOLE', 'INDIVIDUAL'),
    individual_type VARCHAR(50),
    individual_id BIGINT,
    individual_name VARCHAR(100),

    -- 打分（支持多种模式）
    scoring_mode VARCHAR(20) NOT NULL,  -- DEDUCTION/ADDITION/RATING/GRADE/CHECKLIST
    score DECIMAL(6,2),
    quantity INT DEFAULT 1,
    total_score DECIMAL(6,2),
    grade_code VARCHAR(10),
    grade_name VARCHAR(50),
    checklist_checked TINYINT,

    remark VARCHAR(500),
    evidence_urls JSON,
    ...
);

CREATE TABLE inspection_evidences (
    id BIGINT PRIMARY KEY,
    detail_id BIGINT,
    target_record_id BIGINT,
    file_name VARCHAR(200),
    file_path VARCHAR(500),
    file_url VARCHAR(500),
    file_size BIGINT,
    file_type VARCHAR(50),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    upload_by BIGINT,
    upload_time DATETIME,
    ...
);
```

---

### Phase 2: 实体类型与动态分组 (优先级: P1)

#### 2.1 后端 - 实体类型API

```
缺失的API端点:
├── GET  /entity-types - 获取实体类型列表
├── GET  /entity-types/:typeCode/instances - 获取实例列表
└── GET  /entity-types/:typeCode/tree - 获取层级树
```

需要创建:
- [ ] `EntityTypeController.java`
- [ ] `EntityTypeApplicationService.java`
- [ ] `entity_types` 数据库表

#### 2.2 后端 - 动态分组API

```
缺失的API端点:
├── GET  /entity-groups - 获取分组列表
├── POST /entity-groups - 创建分组
├── GET  /entity-groups/:id/members - 获取分组成员
└── POST /entity-groups/:id/refresh - 刷新分组缓存
```

需要创建:
- [ ] `EntityGroupController.java`
- [ ] `EntityGroupApplicationService.java`
- [ ] `entity_groups` 数据库表

---

### Phase 3: 项目创建向导 (优先级: P1)

#### 3.1 前端 - 6步向导

当前 `ProjectCreate.vue` 是简化版，需要实现完整的6步向导：

| 步骤 | 当前状态 | 需要 |
|------|---------|------|
| Step 1: 基本信息 | ⚠️ 部分 | 完善 |
| Step 2: 模板选择 | ⚠️ 简化 | 完善 |
| Step 3: 范围配置 | ❌ 缺失 | 新建 |
| Step 4: 打分配置 | ❌ 缺失 | 新建 |
| Step 5: 加权配置 | ❌ 缺失 | 新建 |
| Step 6: 确认创建 | ❌ 缺失 | 新建 |

需要创建:
- [ ] `ProjectWizard.vue` - 向导容器
- [ ] `WizardStep1Basic.vue` - 基本信息
- [ ] `WizardStep2Template.vue` - 模板选择
- [ ] `WizardStep3Scope.vue` - 范围配置（支持递归选择）
- [ ] `WizardStep4Scoring.vue` - 打分配置
- [ ] `WizardStep5Weight.vue` - 加权配置
- [ ] `WizardStep6Confirm.vue` - 确认创建

#### 3.2 后端 - 向导API

```
缺失的API端点:
├── GET  /projects/wizard/templates - 获取可用模板
├── GET  /projects/wizard/targets - 获取可选目标
├── POST /projects/wizard/preview-targets - 预览检查目标
└── GET  /projects/wizard/scoring-strategies - 获取打分策略
```

---

### Phase 4: 打分策略配置 (优先级: P2)

#### 4.1 数据库

```sql
CREATE TABLE scoring_strategies (
    id BIGINT PRIMARY KEY,
    strategy_code VARCHAR(50) NOT NULL,
    strategy_name VARCHAR(100) NOT NULL,
    strategy_type ENUM('DEDUCTION', 'ADDITION', 'BASE_SCORE', 'RATING', 'GRADE', 'PASS_FAIL', 'CHECKLIST'),
    config JSON,
    completion_rules JSON,
    result_format VARCHAR(100),
    is_system TINYINT,
    ...
);
```

#### 4.2 预置数据

```sql
INSERT INTO scoring_strategies VALUES
('PURE_DEDUCTION', '纯扣分制', 'DEDUCTION', ...),
('BASE_100', '百分制', 'BASE_SCORE', ...),
('RATING_5', '五分制', 'RATING', ...),
('GRADE_ABCD', '四级评级', 'GRADE', ...),
('PASS_FAIL', '通过制', 'PASS_FAIL', ...),
('CHECKLIST', '清单制', 'CHECKLIST', ...);
```

---

### Phase 5: 移除V4相关代码 (优先级: P2)

#### 5.1 后端需移除

```
domain/inspection/model/ (非v6目录下的)
├── InspectionSession.java
├── ClassInspectionRecord.java
├── InspectionDeduction.java (V4版)
├── InspectionBonus.java (V4版)
├── ChecklistResponse.java
├── Appeal.java
├── CorrectiveAction.java
├── StudentBehaviorRecord.java
└── ...

application/inspection/ (非v6目录下的)
├── InspectionSessionApplicationService.java
├── AppealApplicationService.java
├── CorrectiveActionService.java
└── ...

interfaces/rest/inspection/ (非v6目录下的)
├── InspectionSessionController.java
├── AppealController.java
└── ...
```

#### 5.2 前端需移除

```
views/inspection/ (非v6目录下的)
├── InspectionExecute.vue
├── InspectionPlanList.vue
├── AppealManagement.vue
├── CorrectiveActions.vue
├── StudentBehaviorProfile.vue
├── TeacherDashboard.vue
├── RankingResults.vue
├── DataAnalyticsCenter.vue
└── ...
```

#### 5.3 数据库需处理

需要评估V4表的数据迁移策略：
- `inspection_sessions`
- `class_inspection_records`
- `checklist_responses`
- `inspection_deductions` (V4版)
- `inspection_bonuses` (V4版)
- `appeals`
- `corrective_actions`
- `student_behavior_records`
- ...

---

### Phase 6: 从V4迁移功能到V6 (优先级: P2)

以下V4独有功能需要在V6中重新实现：

| 功能 | V4实现 | V6需要 |
|------|--------|--------|
| 检查清单模式 | ChecklistResponse | inspection_details.scoring_mode='CHECKLIST' |
| 整改工单 | CorrectiveAction | 新建corrective_actions_v6表 |
| 学生行为档案 | StudentBehaviorRecord | 新建student_behaviors表 |
| 行为预警 | AlertRule | 新建alert_rules表 |
| 申诉管理 | Appeal | 复用或新建appeals_v6表 |
| 班主任仪表盘 | TeacherDashboard.vue | 新建TeacherDashboard.vue (V6版) |

---

## 三、实施计划

### 阶段划分

| 阶段 | 内容 | 预计工作量 |
|------|------|-----------|
| **Phase 1** | 检查执行系统（核心） | 大 |
| **Phase 2** | 实体类型与动态分组 | 中 |
| **Phase 3** | 项目创建向导 | 中 |
| **Phase 4** | 打分策略配置 | 小 |
| **Phase 5** | 移除V4代码 | 小 |
| **Phase 6** | 迁移V4功能到V6 | 大 |

### 建议执行顺序

1. **先完成Phase 1** - 检查执行是核心功能，必须先完成
2. **再完成Phase 3** - 项目创建向导依赖Phase 2，但可以先简化实现
3. **然后Phase 2** - 实体类型和动态分组
4. **接着Phase 4** - 打分策略
5. **最后Phase 5+6** - 移除V4并迁移功能

---

## 四、检查清单

### Phase 1 完成标准

- [ ] 可以在PC端双栏界面执行检查
- [ ] 左侧递归显示组织树，支持展开/收起
- [ ] 右侧显示当前目标的打分面板
- [ ] 支持选择扣分项并关联到具体学生
- [ ] 支持上传证据照片
- [ ] 支持目标锁定/解锁
- [ ] 支持完成检查/跳过检查

### Phase 3 完成标准

- [ ] 6步向导可以正常流转
- [ ] 可以选择模板
- [ ] 可以配置检查范围（递归组织树）
- [ ] 可以配置打分模式
- [ ] 可以配置加权方案
- [ ] 可以预览检查目标数量

### Phase 5+6 完成标准

- [ ] V4相关代码已移除
- [ ] V4数据已迁移（如需要）
- [ ] 整改工单功能已在V6实现
- [ ] 申诉管理功能已在V6实现
- [ ] 学生行为档案功能已在V6实现

---

**文档版本**: 1.0
**最后更新**: 2026-02-01
