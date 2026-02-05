# V6 开发追踪清单

> 使用说明：开发过程中实时更新此清单，确保无遗漏

---

## Phase 0: 命名统一改造 ✅

> 将"部门/Department"统一改为"组织/Organization"

### 0.1 数据库层改造 ✅
- [x] 创建迁移脚本 `V8.0.0__naming_unification.sql`
- [x] 更新权限代码 `system:department:*` → `system:org:*`
- [x] 更新菜单名称 `部门管理` → `组织管理`

### 0.2 后端代码改造 ✅
- [x] `OrgUnitPO.java` - 更新表映射和字段命名
- [x] `OrgUnitMapper.java/xml` - 更新SQL查询
- [x] `OrgUnitRepositoryImpl.java` - 更新转换方法
- [x] `OrgUnitController.java` - 更新权限注解
- [x] 创建 `OrgResultStats.java` (新)
- [x] 创建 `OrgRankingService.java` (新)
- [x] 创建 `OrgRankingServiceImpl.java` (新)
- [x] 创建 `OrgRankingQueryService.java` (新)
- [x] 创建 `OrgRankingController.java` (新)
- [x] `InspectionCacheService.java` - 添加新方法，废弃旧方法
- [x] 标记 `DepartmentRanking*` 文件为 @Deprecated

### 0.3 前端代码改造 ✅
- [x] 创建 `orgRanking.ts` API模块
- [x] 更新 `departmentRanking.ts` 添加废弃标记
- [x] 更新 `api/index.ts` 导出新模块
- [x] 更新 `types/access.ts` 数据范围标签
- [x] 更新 `types/inspectionSession.ts` 注释
- [x] 更新 `router/index.ts` 权限代码
- [x] 更新 `DepartmentsView.vue` UI文本
- [x] 更新 `DepartmentForm.vue` UI文本
- [x] 更新 `OrgStructureV2.vue` UI文本
- [x] 更新 `RolesView.vue` UI文本
- [x] 更新 `UsersView.vue` UI文本
- [x] 更新 `ClassList.vue` UI文本
- [x] 更新 `DataAnalyticsCenter.vue` UI文本
- [x] 更新 `DepartmentDormitoryView.vue` UI文本
- [x] 更新 `ProfileView.vue` UI文本
- [x] 更新 `ClassRankingTable.vue` 类型定义

### 0.4 验证测试
- [x] 前端构建成功 (`npm run build`)
- [ ] 后端编译验证 (需要本地Maven环境)
- [ ] 浏览器集成测试

### 遗留项目 (后续版本清理)
- 197个 "部门" 引用分散在38个文件中，主要是:
  - 旧版API兼容层 (departmentDormitory.ts等)
  - 测试文件
  - 组件内部变量名 (不影响用户界面)
  - 注释文本

---

## Phase 1: 类型配置系统

### 1.1 组织类型 (org_types)

#### 后端开发
- [ ] 数据库迁移脚本 `V7.1.0__create_org_types.sql`
- [ ] 领域模型 `OrgType.java`
- [ ] 仓储接口 `OrgTypeRepository.java`
- [ ] 仓储实现 `OrgTypeRepositoryImpl.java`
- [ ] 应用服务 `OrgTypeApplicationService.java`
- [ ] REST控制器 `OrgTypeController.java`
- [ ] 单元测试 `OrgTypeTest.java` (覆盖率 >= 80%)
- [ ] 集成测试 `OrgTypeIntegrationTest.java`

#### 前端开发
- [ ] API模块 `src/api/orgType.ts`
- [ ] 类型定义 `src/types/orgType.ts`
- [ ] 列表页面 `src/views/system/OrgTypeList.vue`
- [ ] 编辑对话框 `src/components/OrgTypeDialog.vue`

#### 验收测试
- [ ] 创建类型 - 正常
- [ ] 创建类型 - 重复代码校验
- [ ] 编辑类型
- [ ] 删除类型 - 未使用
- [ ] 删除类型 - 已使用（应阻止）
- [ ] 删除类型 - 系统预置（应阻止）
- [ ] 类型树形展示

#### 代码评审
- [ ] 后端代码评审通过
- [ ] 前端代码评审通过

---

### 1.2 场所类型 (space_types)

#### 后端开发
- [ ] 数据库迁移脚本 `V7.1.1__create_space_types.sql`
- [ ] 领域模型 `SpaceType.java`
- [ ] 仓储接口 `SpaceTypeRepository.java`
- [ ] 仓储实现 `SpaceTypeRepositoryImpl.java`
- [ ] 应用服务 `SpaceTypeApplicationService.java`
- [ ] REST控制器 `SpaceTypeController.java`
- [ ] 单元测试 (覆盖率 >= 80%)
- [ ] 集成测试

#### 前端开发
- [ ] API模块 `src/api/spaceType.ts`
- [ ] 类型定义 `src/types/spaceType.ts`
- [ ] 配置页面

#### 验收测试
- [ ] CRUD功能测试
- [ ] 层级类型校验
- [ ] 特性配置（入住管理、性别限制等）

#### 代码评审
- [ ] 后端代码评审通过
- [ ] 前端代码评审通过

---

### 1.3 用户类型 (user_types)

#### 后端开发
- [ ] 数据库迁移脚本 `V7.1.2__create_user_types.sql`
- [ ] 领域模型 `UserType.java`
- [ ] 仓储接口 `UserTypeRepository.java`
- [ ] 仓储实现 `UserTypeRepositoryImpl.java`
- [ ] 应用服务 `UserTypeApplicationService.java`
- [ ] REST控制器 `UserTypeController.java`
- [ ] 单元测试 (覆盖率 >= 80%)
- [ ] 集成测试

#### 前端开发
- [ ] API模块 `src/api/userType.ts`
- [ ] 类型定义 `src/types/userType.ts`
- [ ] 配置页面

#### 验收测试
- [ ] CRUD功能测试
- [ ] 特性配置（可检查、可被检查等）

#### 代码评审
- [ ] 后端代码评审通过
- [ ] 前端代码评审通过

---

## Phase 2: 多归属关系系统

### 2.1 用户-组织关系 (user_org_relations)

#### 后端开发
- [ ] 数据库迁移脚本 `V7.2.0__create_user_org_relations.sql`
- [ ] 领域模型 `UserOrgRelation.java`
- [ ] 仓储接口与实现
- [ ] 应用服务
- [ ] REST控制器 `/api/v6/users/:userId/orgs`
- [ ] 单元测试 (覆盖率 >= 80%)
- [ ] 集成测试

#### 前端开发
- [ ] API模块
- [ ] 用户组织分配组件
- [ ] 集成到用户管理页面

#### 验收测试
- [ ] 添加主归属
- [ ] 添加副职归属
- [ ] 添加临时归属（含有效期）
- [ ] 主归属唯一性校验
- [ ] 删除归属
- [ ] 归属历史查询

#### 数据迁移
- [ ] 迁移脚本编写
- [ ] 迁移脚本测试
- [ ] 回滚脚本准备

#### 代码评审
- [ ] 后端代码评审通过
- [ ] 前端代码评审通过

---

### 2.2 场所-组织关系 (space_org_relations)

#### 后端开发
- [ ] 数据库迁移脚本 `V7.2.1__create_space_org_relations.sql`
- [ ] 领域模型 `SpaceOrgRelation.java`
- [ ] 仓储接口与实现
- [ ] 应用服务
- [ ] REST控制器 `/api/v6/spaces/:spaceId/orgs`
- [ ] 单元测试 (覆盖率 >= 80%)
- [ ] 集成测试

#### 前端开发
- [ ] API模块
- [ ] 场所组织关联组件
- [ ] 比例设置组件
- [ ] 集成到场所管理页面

#### 验收测试
- [ ] 添加所有者关系
- [ ] 添加使用者关系
- [ ] 添加管理者关系
- [ ] 设置主归属
- [ ] 自动计算比例
- [ ] 手动设置比例

#### 数据迁移
- [ ] 迁移脚本编写
- [ ] 迁移脚本测试
- [ ] 回滚脚本准备

#### 代码评审
- [ ] 后端代码评审通过
- [ ] 前端代码评审通过

---

### 2.3 用户-场所关系 (user_space_relations)

#### 后端开发
- [ ] 数据库迁移脚本 `V7.2.2__create_user_space_relations.sql`
- [ ] 领域模型 `UserSpaceRelation.java`
- [ ] 仓储接口与实现
- [ ] 应用服务
- [ ] REST控制器 `/api/v6/users/:userId/spaces`
- [ ] 单元测试 (覆盖率 >= 80%)
- [ ] 集成测试

#### 前端开发
- [ ] API模块
- [ ] 用户场所分配组件（住宿、工位）

#### 验收测试
- [ ] 入住分配
- [ ] 工位分配
- [ ] 历史记录

#### 数据迁移
- [ ] 从students表迁移住宿数据
- [ ] 迁移脚本测试

#### 代码评审
- [ ] 后端代码评审通过
- [ ] 前端代码评审通过

---

## Phase 3: 现有模块改造

### 3.1 组织管理改造

- [ ] OrgUnit实体改用type_code
- [ ] 移除OrgUnitType硬编码枚举
- [ ] 添加attributes JSON字段
- [ ] 添加统计缓存字段
- [ ] 修改前端组织树组件
- [ ] 兼容层实现
- [ ] 回归测试

### 3.2 场所管理改造

- [ ] Space实体改用type_code
- [ ] 移除RoomType硬编码枚举
- [ ] 添加多组织关联支持
- [ ] 修改前端场所管理页面
- [ ] 兼容层实现
- [ ] 回归测试

### 3.3 用户管理改造

- [ ] User实体改用type_code
- [ ] 移除UserType硬编码枚举
- [ ] 添加多组织归属支持
- [ ] 修改前端用户管理页面
- [ ] 兼容层实现
- [ ] 回归测试

---

## Phase 4: V6检查系统核心

### 4.1 检查项目创建向导

#### 后端
- [ ] InspectionProject扩展字段
  - [ ] shared_space_strategy
  - [ ] score_distribution_mode
  - [ ] inspector_assignment_mode
  - [ ] skip_holidays
  - [ ] excluded_dates
- [ ] 项目创建API
- [ ] 项目草稿保存
- [ ] 单元测试
- [ ] 集成测试

#### 前端 - 6步向导
- [ ] Step1: 基本信息
- [ ] Step2: 检查范围（组织/场所/用户选择）
- [ ] Step3: 检查模板
- [ ] Step4: 检查周期
- [ ] Step5: 检查员配置
- [ ] Step6: 确认与发布
- [ ] 草稿保存/恢复

#### 验收测试
- [ ] 完整流程测试
- [ ] 每步数据保存
- [ ] 上一步/下一步
- [ ] 草稿保存恢复
- [ ] 项目发布

---

### 4.2 任务生成引擎

#### 后端
- [ ] TaskGenerationService
- [ ] 目标生成逻辑
  - [ ] 组织目标生成
  - [ ] 场所目标生成（含共享处理）
  - [ ] 用户目标生成（含多归属处理）
- [ ] 检查员分配逻辑
  - [ ] FREE模式
  - [ ] ASSIGNED模式
  - [ ] HYBRID模式
- [ ] 自动分配算法
- [ ] 单元测试
- [ ] 集成测试

#### 验收测试
- [ ] 按日生成任务
- [ ] 按周生成任务
- [ ] 按月生成任务
- [ ] 共享场所处理
- [ ] 多归属用户处理
- [ ] 检查员自动分配

---

### 4.3 检查执行模块

#### 后端
- [ ] 任务领取API
- [ ] 目标锁定API
- [ ] 检查提交API
- [ ] 跳过处理API
- [ ] 批量操作API
- [ ] 单元测试
- [ ] 集成测试

#### 前端 - PC端
- [ ] 任务列表页
- [ ] 检查执行页
- [ ] 目标详情页
- [ ] 扣分录入
- [ ] 批量操作

#### 前端 - 移动端适配
- [ ] 响应式布局
- [ ] 触摸优化
- [ ] 离线支持（可选）

#### 验收测试
- [ ] 任务领取
- [ ] 目标锁定
- [ ] 检查提交
- [ ] 跳过处理
- [ ] 并发控制
- [ ] 超时处理

---

### 4.4 检查员分配管理

#### 后端
- [ ] InspectionTaskAssignment实体
- [ ] 分配CRUD API
- [ ] 自动分配API
- [ ] 单元测试
- [ ] 集成测试

#### 前端
- [ ] 分配管理页面
- [ ] 拖拽分配
- [ ] 自动均分

#### 验收测试
- [ ] 按组织分配
- [ ] 按场所分配
- [ ] 自定义分配
- [ ] 自动均分

---

### 4.5 数据统计与汇总

#### 后端
- [ ] InspectionSummary计算服务
- [ ] 汇总API
- [ ] 排名API
- [ ] 导出API
- [ ] 单元测试

#### 前端
- [ ] 统计仪表盘
- [ ] 排名展示
- [ ] 趋势图表
- [ ] 导出功能

#### 验收测试
- [ ] 按日/周/月汇总
- [ ] 组织排名
- [ ] 场所排名
- [ ] 用户排名
- [ ] 数据导出

---

## Phase 5: 发布准备

### 5.1 全量测试

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] E2E测试关键路径通过
- [ ] 回归测试通过
- [ ] 性能测试通过
- [ ] 安全测试通过

### 5.2 文档完善

- [ ] API文档更新
- [ ] 数据库文档更新
- [ ] 用户操作手册
- [ ] 发布说明

### 5.3 数据迁移

- [ ] 迁移脚本最终测试
- [ ] 生产数据备份
- [ ] 迁移执行计划
- [ ] 回滚方案确认

### 5.4 发布

- [ ] 灰度发布计划
- [ ] 监控配置
- [ ] 告警配置
- [ ] 值班安排
- [ ] 回滚演练

---

## 问题跟踪

| ID | 描述 | 优先级 | 状态 | 负责人 | 备注 |
|----|-----|-------|------|-------|------|
| | | | | | |

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|-----|---------|-------|
| 2026-02-01 | 初始版本 | |

