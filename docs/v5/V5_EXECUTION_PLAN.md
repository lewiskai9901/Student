# V5 重构执行计划

> **版本**: 1.0
> **日期**: 2026-01-31
> **状态**: 执行中

---

## 执行原则

1. **后端先行**：每个阶段先完成后端，再进行前端
2. **增量验证**：每完成一个模块立即测试验证
3. **保持可运行**：任何时刻系统都应能正常启动
4. **及时提交**：每完成一个任务点就提交代码

---

## Phase 1: 权限系统优化

### 1.1 数据库层

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P1-DB-01 | 创建 `data_scope_types` 表 | ⬜ 待开始 | 表存在，初始数据5条 |
| P1-DB-02 | 创建 `data_modules` 表 | ⬜ 待开始 | 表存在，初始数据15+条 |
| P1-DB-03 | 创建 `scope_item_types` 表 | ⬜ 待开始 | 表存在，初始数据5条 |
| P1-DB-04 | 创建 `role_data_permissions` 表 | ⬜ 待开始 | 表存在 |
| P1-DB-05 | 创建 `role_data_scope_items` 表 | ⬜ 待开始 | 表存在 |
| P1-DB-06 | 执行迁移脚本并验证 | ⬜ 待开始 | 所有表创建成功，初始数据正确 |

### 1.2 后端领域层

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P1-BE-01 | 创建 `DataScopeType` 枚举 | ⬜ 待开始 | 5种范围类型 |
| P1-BE-02 | 创建 `DataModule` 枚举 | ⬜ 待开始 | 15+个模块 |
| P1-BE-03 | 创建 `ScopeItemType` 枚举 | ⬜ 待开始 | 5种范围项类型 |
| P1-BE-04 | 创建 `RoleDataPermission` 实体 | ⬜ 待开始 | 包含roleId, moduleCode, scopeCode |
| P1-BE-05 | 创建 `DataScopeItem` 实体 | ⬜ 待开始 | 包含permissionId, itemType, scopeId |
| P1-BE-06 | 创建 `MergedDataScope` 值对象 | ⬜ 待开始 | 用于多角色合并 |

### 1.3 后端基础设施层

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P1-BE-07 | 创建 `@DataPermission` 注解 | ⬜ 待开始 | 支持module, tableAlias, enabled |
| P1-BE-08 | 创建 `UserContext` 和 `UserContextHolder` | ⬜ 待开始 | 线程安全，RequestScope |
| P1-BE-09 | 创建 `DataModuleRegistry` | ⬜ 待开始 | 从数据库加载模块配置 |
| P1-BE-10 | 实现 `DataPermissionInterceptor` | ⬜ 待开始 | SQL自动注入过滤条件 |
| P1-BE-11 | 实现多角色权限合并逻辑 | ⬜ 待开始 | ALL优先，CUSTOM/SELF用OR |
| P1-BE-12 | 集成到 MyBatis | ⬜ 待开始 | 拦截器生效 |

### 1.4 后端应用层

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P1-BE-13 | 创建 `DataPermissionRepository` | ⬜ 待开始 | CRUD方法 |
| P1-BE-14 | 实现 `DataPermissionApplicationService` | ⬜ 待开始 | 角色权限配置服务 |
| P1-BE-15 | 实现 Redis 缓存策略 | ⬜ 待开始 | 权限缓存，失效通知 |

### 1.5 后端接口层

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P1-BE-16 | `GET /roles/data-permissions/modules` | ⬜ 待开始 | 返回所有模块（按领域分组） |
| P1-BE-17 | `GET /roles/data-permissions/scopes` | ⬜ 待开始 | 返回所有范围类型 |
| P1-BE-18 | `GET /roles/data-permissions/scope-item-types` | ⬜ 待开始 | 返回范围项类型 |
| P1-BE-19 | `GET /roles/data-permissions/scope-items` | ⬜ 待开始 | 搜索自定义范围可选项 |
| P1-BE-20 | `GET /roles/{id}/data-permissions` | ⬜ 待开始 | 获取角色配置 |
| P1-BE-21 | `PUT /roles/{id}/data-permissions` | ⬜ 待开始 | 保存角色配置 |
| P1-BE-22 | Swagger文档验证 | ⬜ 待开始 | 所有API可访问 |

### 1.6 前端权限模块

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P1-FE-01 | 创建 `api/permission/dataPermission.ts` | ⬜ 待开始 | 所有API封装 |
| P1-FE-02 | 创建 `types/permission.ts` 类型定义 | ⬜ 待开始 | 完整类型 |
| P1-FE-03 | 完善 `RoleDataPermissionDialog.vue` | ⬜ 待开始 | 模块分组展示，范围选择 |
| P1-FE-04 | 完善 `CustomScopeItemDialog.vue` | ⬜ 待开始 | 搜索选择，支持多选 |
| P1-FE-05 | 集成到角色管理页面 | ⬜ 待开始 | 可配置数据权限 |
| P1-FE-06 | E2E测试验证 | ⬜ 待开始 | 配置保存后生效 |

### Phase 1 验收检查点

- [ ] 数据库表创建完成，初始数据正确
- [ ] 后端API全部可用，Swagger测试通过
- [ ] 前端权限配置界面可用
- [ ] 配置一个测试角色，验证数据过滤生效

---

## Phase 2: 检查系统基础（模板管理）

### 2.1 数据库层

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P2-DB-01 | 创建 `inspection_templates` 表 | ⬜ 待开始 | 包含visibility, scoringMode |
| P2-DB-02 | 创建 `score_categories` 表 | ⬜ 待开始 | 关联模板，包含weight |
| P2-DB-03 | 创建 `score_items` 表 | ⬜ 待开始 | 关联类别，包含requireEvidence |
| P2-DB-04 | 创建 `bonus_items` 表 | ⬜ 待开始 | 加分项配置 |
| P2-DB-05 | 执行迁移脚本 | ⬜ 待开始 | 所有表创建成功 |

### 2.2 后端实现

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P2-BE-01 | 创建 `InspectionTemplate` 聚合根 | ⬜ 待开始 | 包含categories集合 |
| P2-BE-02 | 创建 `ScoreCategory` 实体 | ⬜ 待开始 | 包含items集合 |
| P2-BE-03 | 创建 `ScoreItem` 实体 | ⬜ 待开始 | 扣分项/加分项 |
| P2-BE-04 | 创建 `TemplateVisibility` 枚举 | ⬜ 待开始 | PRIVATE/DEPARTMENT/PUBLIC |
| P2-BE-05 | 创建 `ScoringMode` 枚举 | ⬜ 待开始 | 4种计分模式 |
| P2-BE-06 | 实现 `TemplateRepository` | ⬜ 待开始 | CRUD + 可见性过滤 |
| P2-BE-07 | 实现 `TemplateApplicationService` | ⬜ 待开始 | 创建/发布/归档 |
| P2-BE-08 | 实现模板 Controller | ⬜ 待开始 | RESTful API |
| P2-BE-09 | 添加 `@DataPermission` 注解 | ⬜ 待开始 | 模板数据权限生效 |

### 2.3 前端实现

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P2-FE-01 | 重构 `api/inspection/template.ts` | ⬜ 待开始 | 适配新字段 |
| P2-FE-02 | 更新 `types/inspection.ts` | ⬜ 待开始 | 添加新类型 |
| P2-FE-03 | 重构 `InspectionConfig.vue` | ⬜ 待开始 | 支持可见性、计分模式 |
| P2-FE-04 | 功能测试 | ⬜ 待开始 | 模板CRUD正常 |

### Phase 2 验收检查点

- [ ] 模板CRUD功能正常
- [ ] 模板可见性控制生效
- [ ] 类别和扣分项管理正常
- [ ] 数据权限过滤生效

---

## Phase 3: 项目与任务

### 3.1 数据库层

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P3-DB-01 | 创建 `inspection_projects` 表 | ⬜ 待开始 | 包含templateSnapshot JSON |
| P3-DB-02 | 创建 `inspection_tasks` 表 | ⬜ 待开始 | 包含状态流转字段 |
| P3-DB-03 | 创建 `task_inspectors` 关联表 | ⬜ 待开始 | 任务-检查员多对多 |
| P3-DB-04 | 执行迁移脚本 | ⬜ 待开始 | 所有表创建成功 |

### 3.2 后端实现

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P3-BE-01 | 创建 `InspectionProject` 聚合根 | ⬜ 待开始 | 包含模板快照 |
| P3-BE-02 | 创建 `InspectionTask` 实体 | ⬜ 待开始 | 状态机 |
| P3-BE-03 | 创建 `EntryMode` 枚举 | ⬜ 待开始 | 5种录入模式 |
| P3-BE-04 | 创建 `ProjectStatus` 枚举 | ⬜ 待开始 | DRAFT/ACTIVE/PAUSED/COMPLETED/ARCHIVED |
| P3-BE-05 | 创建 `TaskStatus` 枚举 | ⬜ 待开始 | DRAFT/IN_PROGRESS/SUBMITTED/REVIEWED/PUBLISHED |
| P3-BE-06 | 实现 `ProjectRepository` | ⬜ 待开始 | CRUD |
| P3-BE-07 | 实现 `TaskRepository` | ⬜ 待开始 | CRUD + 日期范围查询 |
| P3-BE-08 | 实现 `ProjectApplicationService` | ⬜ 待开始 | 创建项目时复制模板快照 |
| P3-BE-09 | 实现 `TaskApplicationService` | ⬜ 待开始 | 任务生成、分配检查员 |
| P3-BE-10 | 实现任务批量生成逻辑 | ⬜ 待开始 | 按日期范围生成 |
| P3-BE-11 | 实现 Project Controller | ⬜ 待开始 | RESTful API |
| P3-BE-12 | 实现 Task Controller | ⬜ 待开始 | RESTful API |
| P3-BE-13 | 添加 `@DataPermission` 注解 | ⬜ 待开始 | 项目/任务数据权限 |

### 3.3 前端实现

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P3-FE-01 | 创建 `api/inspection/project.ts` | ⬜ 待开始 | 完整API |
| P3-FE-02 | 创建 `api/inspection/task.ts` | ⬜ 待开始 | 完整API |
| P3-FE-03 | 更新 `types/inspection.ts` | ⬜ 待开始 | Project/Task类型 |
| P3-FE-04 | 创建 `stores/inspection/project.ts` | ⬜ 待开始 | 项目状态管理 |
| P3-FE-05 | 创建 `stores/inspection/task.ts` | ⬜ 待开始 | 任务状态管理 |
| P3-FE-06 | 重构 `InspectionPlanList.vue` → `ProjectList.vue` | ⬜ 待开始 | 项目列表 |
| P3-FE-07 | 创建 `ProjectForm.vue` | ⬜ 待开始 | 项目创建/编辑 |
| P3-FE-08 | 重构 `InspectionPlanDetail.vue` → `ProjectDetail.vue` | ⬜ 待开始 | 项目详情 + 任务列表 |
| P3-FE-09 | 创建 `TaskList.vue` 组件 | ⬜ 待开始 | 任务列表组件 |
| P3-FE-10 | 创建 `TaskCalendar.vue` 组件 | ⬜ 待开始 | 日历视图 |
| P3-FE-11 | 创建 `InspectorAssignDialog.vue` | ⬜ 待开始 | 检查员分配 |
| P3-FE-12 | 更新路由配置 | ⬜ 待开始 | /inspection/projects/* |
| P3-FE-13 | 功能测试 | ⬜ 待开始 | 项目/任务CRUD正常 |

### Phase 3 验收检查点

- [ ] 项目创建时正确保存模板快照
- [ ] 任务批量生成正常
- [ ] 检查员分配正常
- [ ] 任务状态流转正常
- [ ] 数据权限过滤生效

---

## Phase 4: 检查执行

### 4.1 数据库层

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P4-DB-01 | 创建 `target_inspection_records` 表 | ⬜ 待开始 | 包含快照字段 |
| P4-DB-02 | 创建 `deduction_records` 表 | ⬜ 待开始 | 扣分明细 |
| P4-DB-03 | 创建 `evidence_attachments` 表 | ⬜ 待开始 | 证据附件 |
| P4-DB-04 | 执行迁移脚本 | ⬜ 待开始 | 所有表创建成功 |

### 4.2 后端实现

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P4-BE-01 | 创建 `TargetInspectionRecord` 聚合根 | ⬜ 待开始 | 包含快照、分数计算 |
| P4-BE-02 | 创建 `DeductionRecord` 实体 | ⬜ 待开始 | 扣分明细 |
| P4-BE-03 | 创建 `TargetType` 枚举 | ⬜ 待开始 | DORMITORY/CLASSROOM/STUDENT |
| P4-BE-04 | 实现分数计算逻辑 | ⬜ 待开始 | 4种计分模式 |
| P4-BE-05 | 实现公平权重计算 | ⬜ 待开始 | DIVIDE/BENCHMARK |
| P4-BE-06 | 实现混合宿舍分数分配 | ⬜ 待开始 | RATIO/AVERAGE/FULL/MAIN |
| P4-BE-07 | 实现 `RecordRepository` | ⬜ 待开始 | CRUD + 任务关联查询 |
| P4-BE-08 | 实现 `RecordApplicationService` | ⬜ 待开始 | 创建记录、添加扣分 |
| P4-BE-09 | 实现 Record Controller | ⬜ 待开始 | RESTful API |
| P4-BE-10 | 实现5种录入模式适配 | ⬜ 待开始 | SPACE/PERSON/CLASS/ITEM/CHECKLIST |
| P4-BE-11 | 添加 `@DataPermission` 注解 | ⬜ 待开始 | 记录数据权限 |

### 4.3 前端实现

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P4-FE-01 | 更新 `api/inspection/task.ts` | ⬜ 待开始 | 添加记录相关API |
| P4-FE-02 | 更新 `types/inspection.ts` | ⬜ 待开始 | Record/Deduction类型 |
| P4-FE-03 | 重构 `InspectionExecute.vue` → `TaskExecute.vue` | ⬜ 待开始 | 检查执行页面 |
| P4-FE-04 | 创建 `RecordCard.vue` | ⬜ 待开始 | 检查记录卡片 |
| P4-FE-05 | 重构 `DeductionItemPicker.vue` | ⬜ 待开始 | 适配模板快照 |
| P4-FE-06 | 创建 `EvidenceUploader.vue` | ⬜ 待开始 | 证据上传 |
| P4-FE-07 | 实现SPACE录入模式 | ⬜ 待开始 | 按场所录入 |
| P4-FE-08 | 实现PERSON录入模式 | ⬜ 待开始 | 按人员录入 |
| P4-FE-09 | 实现CLASS录入模式 | ⬜ 待开始 | 按班级录入 |
| P4-FE-10 | 实现ITEM录入模式 | ⬜ 待开始 | 按项目录入 |
| P4-FE-11 | 实现CHECKLIST录入模式 | ⬜ 待开始 | 清单打勾 |
| P4-FE-12 | 功能测试 | ⬜ 待开始 | 各模式录入正常 |

### Phase 4 验收检查点

- [ ] 5种录入模式全部可用
- [ ] 分数计算正确
- [ ] 公平权重生效
- [ ] 混合宿舍分数分配正确
- [ ] 证据上传正常
- [ ] 数据权限过滤生效

---

## Phase 5: 申诉与整改

### 5.1 数据库层

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P5-DB-01 | 创建 `appeals` 表 | ⬜ 待开始 | 两级审核字段 |
| P5-DB-02 | 创建 `corrective_orders` 表 | ⬜ 待开始 | 多轮验收 |
| P5-DB-03 | 创建 `corrective_submissions` 表 | ⬜ 待开始 | 整改提交记录 |
| P5-DB-04 | 执行迁移脚本 | ⬜ 待开始 | 所有表创建成功 |

### 5.2 后端实现

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P5-BE-01 | 创建 `Appeal` 聚合根 | ⬜ 待开始 | 状态机、两级审核 |
| P5-BE-02 | 创建 `AppealStatus` 枚举 | ⬜ 待开始 | 6种状态 |
| P5-BE-03 | 创建 `CorrectiveOrder` 聚合根 | ⬜ 待开始 | 多轮验收 |
| P5-BE-04 | 创建 `CorrectiveStatus` 枚举 | ⬜ 待开始 | 7种状态 |
| P5-BE-05 | 实现 `AppealRepository` | ⬜ 待开始 | CRUD |
| P5-BE-06 | 实现 `CorrectiveRepository` | ⬜ 待开始 | CRUD |
| P5-BE-07 | 实现 `AppealApplicationService` | ⬜ 待开始 | 提交、一级审核、二级审核 |
| P5-BE-08 | 实现申诉通过后更新记录分数 | ⬜ 待开始 | 标记扣分撤销，重算分数 |
| P5-BE-09 | 实现 `CorrectiveApplicationService` | ⬜ 待开始 | 下发、提交、验收 |
| P5-BE-10 | 实现 Appeal Controller | ⬜ 待开始 | RESTful API |
| P5-BE-11 | 实现 Corrective Controller | ⬜ 待开始 | RESTful API |
| P5-BE-12 | 添加 `@DataPermission` 注解 | ⬜ 待开始 | 申诉/整改数据权限 |

### 5.3 前端实现

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P5-FE-01 | 重构 `api/inspection/appeal.ts` | ⬜ 待开始 | 两级审核API |
| P5-FE-02 | 重构 `api/inspection/corrective.ts` | ⬜ 待开始 | 多轮验收API |
| P5-FE-03 | 更新 `types/inspection.ts` | ⬜ 待开始 | Appeal/Corrective类型 |
| P5-FE-04 | 重构 `AppealManagement.vue` | ⬜ 待开始 | 两级审核流程 |
| P5-FE-05 | 重构 `CorrectiveActions.vue` | ⬜ 待开始 | 多轮验收流程 |
| P5-FE-06 | 重构 `CorrectiveOrderCard.vue` | ⬜ 待开始 | 显示多轮状态 |
| P5-FE-07 | 删除 `CorrectiveActionListView.vue` | ⬜ 待开始 | 清理重复组件 |
| P5-FE-08 | 功能测试 | ⬜ 待开始 | 申诉/整改流程正常 |

### Phase 5 验收检查点

- [ ] 申诉提交、一级审核、二级审核流程正常
- [ ] 申诉通过后分数自动更新
- [ ] 整改下发、提交、验收流程正常
- [ ] 多轮验收正常
- [ ] 逾期标记正常
- [ ] 数据权限过滤生效

---

## Phase 6: 汇总与排名

### 6.1 数据库层

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P6-DB-01 | 创建 `daily_summaries` 表 | ⬜ 待开始 | 每日班级汇总 |
| P6-DB-02 | 创建 `auto_bonus_rules` 表 | ⬜ 待开始 | 自动加分规则 |
| P6-DB-03 | 创建 `auto_bonus_records` 表 | ⬜ 待开始 | 自动加分记录 |
| P6-DB-04 | 执行迁移脚本 | ⬜ 待开始 | 所有表创建成功 |

### 6.2 后端实现

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P6-BE-01 | 创建 `DailySummary` 实体 | ⬜ 待开始 | 班级日汇总 |
| P6-BE-02 | 创建 `AutoBonusRule` 实体 | ⬜ 待开始 | 自动加分规则 |
| P6-BE-03 | 实现 `SummaryRepository` | ⬜ 待开始 | CRUD + 范围查询 |
| P6-BE-04 | 实现 `SummaryApplicationService` | ⬜ 待开始 | 生成汇总、计算排名 |
| P6-BE-05 | 实现1224排名算法 | ⬜ 待开始 | 相同分数相同排名 |
| P6-BE-06 | 实现自动加分规则引擎 | ⬜ 待开始 | 连续满分、月度优秀等 |
| P6-BE-07 | 实现定时任务（汇总生成） | ⬜ 待开始 | 每日凌晨2点 |
| P6-BE-08 | 实现 Summary Controller | ⬜ 待开始 | RESTful API |
| P6-BE-09 | 添加 `@DataPermission` 注解 | ⬜ 待开始 | 汇总数据权限 |

### 6.3 前端实现

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P6-FE-01 | 创建 `api/inspection/summary.ts` | ⬜ 待开始 | 汇总API |
| P6-FE-02 | 更新 `types/inspection.ts` | ⬜ 待开始 | Summary类型 |
| P6-FE-03 | 重构 `RankingResults.vue` | ⬜ 待开始 | 适配新API |
| P6-FE-04 | 重构 `ClassRankingTable.vue` | ⬜ 待开始 | 适配ClassSummary |
| P6-FE-05 | 创建 `SummaryChart.vue` | ⬜ 待开始 | 汇总图表 |
| P6-FE-06 | 创建 `TrendChart.vue` | ⬜ 待开始 | 趋势图表 |
| P6-FE-07 | 创建 `RankChangeIndicator.vue` | ⬜ 待开始 | 排名变化指示 |
| P6-FE-08 | 重构 `DataAnalyticsCenter.vue` | ⬜ 待开始 | 适配新统计API |
| P6-FE-09 | 删除 `DataAnalyticsCenterView.vue` | ⬜ 待开始 | 清理重复组件 |
| P6-FE-10 | 重构 `TeacherDashboard.vue` | ⬜ 待开始 | 适配新数据结构 |
| P6-FE-11 | 功能测试 | ⬜ 待开始 | 汇总/排名正常 |

### Phase 6 验收检查点

- [ ] 汇总自动生成正常
- [ ] 手动触发汇总正常
- [ ] 排名计算正确（1224排名）
- [ ] 自动加分规则生效
- [ ] 图表展示正常
- [ ] 数据权限过滤生效

---

## Phase 7: 辅助功能与收尾

### 7.1 辅助API适配

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P7-BE-01 | 重构快捷录入API | ⬜ 待开始 | `/inspection/tasks/{id}/quick-entry` |
| P7-BE-02 | 重构排班API | ⬜ 待开始 | 绑定到项目 |
| P7-BE-03 | 重构导出API | ⬜ 待开始 | 适配新数据结构 |
| P7-BE-04 | 重构行为记录API | ⬜ 待开始 | 关联任务 |
| P7-FE-01 | 重构 `quickEntry.ts` | ⬜ 待开始 | 适配新API |
| P7-FE-02 | 重构 `schedule.ts` | ⬜ 待开始 | 适配项目级 |
| P7-FE-03 | 重构 `ExportCenterView.vue` | ⬜ 待开始 | 适配新API |
| P7-FE-04 | 重构 `ScheduleManagementView.vue` | ⬜ 待开始 | 项目级排班 |
| P7-FE-05 | 重构 `BehaviorRecordListView.vue` | ⬜ 待开始 | 关联任务 |
| P7-FE-06 | 重构 `StudentBehaviorProfile.vue` | ⬜ 待开始 | 适配新结构 |
| P7-FE-07 | 重构 `PersonalInspectionRecords.vue` | ⬜ 待开始 | 适配新结构 |

### 7.2 代码清理

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P7-CL-01 | 删除废弃API文件 | ⬜ 待开始 | quantification.ts等 |
| P7-CL-02 | 删除废弃组件 | ⬜ 待开始 | 重复的View文件 |
| P7-CL-03 | 删除废弃类型 | ⬜ 待开始 | 清理types目录 |
| P7-CL-04 | 更新路由配置 | ⬜ 待开始 | 清理旧路由 |
| P7-CL-05 | 更新导航菜单 | ⬜ 待开始 | 适配新结构 |
| P7-CL-06 | 重构 `stores/inspection.ts` | ⬜ 待开始 | 拆分为多个store |

### 7.3 测试与文档

| 任务ID | 任务 | 状态 | 验收标准 |
|--------|------|------|----------|
| P7-QA-01 | 单元测试 | ⬜ 待开始 | 核心服务覆盖 |
| P7-QA-02 | 集成测试 | ⬜ 待开始 | API端到端 |
| P7-QA-03 | 权限功能测试 | ⬜ 待开始 | 各角色验证 |
| P7-QA-04 | 性能测试 | ⬜ 待开始 | 大数据量查询 |
| P7-QA-05 | 回归测试 | ⬜ 待开始 | 完整流程 |
| P7-DOC-01 | 更新API文档 | ⬜ 待开始 | Swagger完整 |
| P7-DOC-02 | 更新README | ⬜ 待开始 | 反映新架构 |
| P7-DOC-03 | 归档V4代码 | ⬜ 待开始 | 保留备份分支 |

### Phase 7 验收检查点

- [ ] 所有辅助功能正常
- [ ] 无废弃代码残留
- [ ] 测试全部通过
- [ ] 文档更新完成
- [ ] 系统可正常部署运行

---

## 进度汇总

| Phase | 任务总数 | 完成数 | 进度 |
|-------|---------|--------|------|
| Phase 1: 权限系统 | 28 | 0 | 0% |
| Phase 2: 模板管理 | 14 | 0 | 0% |
| Phase 3: 项目任务 | 26 | 0 | 0% |
| Phase 4: 检查执行 | 23 | 0 | 0% |
| Phase 5: 申诉整改 | 20 | 0 | 0% |
| Phase 6: 汇总排名 | 22 | 0 | 0% |
| Phase 7: 收尾工作 | 24 | 0 | 0% |
| **总计** | **157** | **0** | **0%** |

---

**文档版本**: 1.0
**最后更新**: 2026-01-31
