# 量化检查系统功能清单

## 系统概述

量化检查系统采用 **V6通用化项目制检查**架构，支持灵活的检查目标类型、评分策略和工作流程。

### 核心概念

- **实体类型 (EntityType)**: 定义可检查的目标类型，分为ORG(组织)、SPACE(场所)、USER(用户)三大类
- **检查项目 (Project)**: 检查活动的顶层配置，定义检查范围、周期、评分策略
- **检查任务 (Task)**: 项目生成的具体执行单元，分配给检查员执行
- **评分策略 (ScoringStrategy)**: 定义评分规则，支持扣分制、加分制、等级制等多种模式
- **整改工单 (CorrectiveAction)**: 问题发现后的整改跟踪机制

---

## 一、系统管理员 (Admin)

### 1. 实体类型管理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 实体类型列表 | `/inspection/v6/entity-types` | `inspection:entity-type:view` | 查看所有实体类型 |
| 创建实体类型 | - | `inspection:entity-type:create` | 创建新的实体类型（如：教室、宿舍、办公室） |
| 编辑实体类型 | - | `inspection:entity-type:update` | 编辑实体类型配置 |
| 删除实体类型 | - | `inspection:entity-type:delete` | 删除未使用的实体类型 |
| 实体分组管理 | - | `inspection:entity-group:manage` | 管理动态/静态实体分组 |

### 2. 检查配置管理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 量化配置中心 | `/inspection/config` | `quantification:config:view` | 检查模板、检查项的统一配置入口 |
| 检查模板管理 | - | `quantification:template:*` | 创建/编辑/删除检查模板，配置检查项分类 |
| 扣分项配置 | - | `quantification:deduction:*` | 配置扣分项名称、分值、累计规则 |
| 加分项配置 | - | `quantification:bonus:*` | 配置加分项（固定/递进/改善模式） |

### 3. 评分策略管理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 策略列表 | `/inspection/v6/scoring-strategies` | `inspection:scoring-strategy:view` | 查看所有评分策略 |
| 创建扣分制策略 | - | `inspection:scoring-strategy:create` | 创建扣分制评分策略 |
| 创建加分制策略 | - | `inspection:scoring-strategy:create` | 创建加分制评分策略 |
| 创建基础分制策略 | - | `inspection:scoring-strategy:create` | 创建基础分制评分策略 |
| 创建等级制策略 | - | `inspection:scoring-strategy:create` | 创建等级制评分策略（A/B/C/D） |
| 创建评级制策略 | - | `inspection:scoring-strategy:create` | 创建星级评级策略（1-5星） |
| 创建通过制策略 | - | `inspection:scoring-strategy:create` | 创建通过/不通过策略 |
| 创建清单制策略 | - | `inspection:scoring-strategy:create` | 创建检查清单策略 |
| 编辑策略 | - | `inspection:scoring-strategy:update` | 编辑评分策略配置 |
| 删除策略 | - | `inspection:scoring-strategy:delete` | 删除未使用的策略 |

### 4. 项目管理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 项目列表 | `/inspection/v6/projects` | `inspection:project:view` | 查看所有检查项目 |
| 创建项目 | `/inspection/v6/projects/create` | `inspection:project:create` | 5步向导式创建检查项目 |
| 项目配置 | `/inspection/v6/projects/:id/config` | `inspection:project:update` | 配置检查范围、周期、检查员 |
| 发布项目 | - | `inspection:project:publish` | 发布项目，开始自动生成任务 |
| 暂停项目 | - | `inspection:project:update` | 暂停项目任务生成 |
| 归档项目 | - | `inspection:project:update` | 归档已完成的项目 |
| 删除项目 | - | `inspection:project:delete` | 删除草稿状态的项目 |
| 手动生成任务 | - | `inspection:task:generate` | 手动触发任务生成 |
| 分配检查员 | - | `inspection:task:assign` | 为任务分配检查员 |
| 生成汇总 | - | `inspection:summary:generate` | 手动生成日/周/月汇总数据 |

### 5. 排班调度管理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 排班策略 | `/inspection/schedule` | `schedule:policy:view` | 查看排班策略列表 |
| 策略管理 | - | `schedule:policy:manage` | 创建/编辑排班策略（轮询/随机/负载均衡） |
| 检查员池 | - | - | 配置可用检查员名单 |
| 节假日排除 | - | - | 设置排除日期（节假日、特殊日期） |

### 6. 评级与通报管理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 评级统计中心 | `/inspection/rating-statistics` | `quantification:rating:view` | 查看评级频次和趋势 |
| 荣誉徽章管理 | `/inspection/badge-management` | `quantification:badge:manage` | 创建/编辑徽章配置 |
| 授予徽章 | - | `quantification:badge:grant` | 手动授予班级荣誉徽章 |
| 撤销徽章 | - | `quantification:badge:revoke` | 撤销已授予的徽章 |
| 生成通报 | - | `quantification:notification:generate` | 生成荣誉通报/预警通报 |
| 生成证书 | - | `quantification:notification:certificate` | 批量生成荣誉证书 |
| 发布通报 | - | `quantification:notification:publish` | 发布或撤销通报 |

### 7. 整改工单管理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 工单列表 | `/inspection/v6/corrective-actions` | `corrective:action:view` | 查看所有整改工单 |
| 工单统计 | - | - | 查看整改统计数据（待整改/已提交/已验收/已驳回） |
| 逾期工单 | - | - | 查看逾期未完成的整改工单 |
| 创建工单 | - | `corrective:action:create` | 手动创建整改工单 |
| 自动规则 | - | `corrective:rule:manage` | 配置自动创建工单规则 |
| 工单验收 | - | `corrective:action:verify` | 验收整改结果（通过/驳回） |
| 取消工单 | - | `corrective:action:cancel` | 取消整改工单 |

### 8. 数据分析中心

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 数据分析 | `/inspection/data-analytics` | `analytics:view` | 多维度数据分析和可视化 |
| 班级排名 | - | - | 班级得分排名趋势 |
| 部门统计 | - | - | 部门检查数据汇总 |
| 违规分布 | - | - | 违规类型分布分析 |
| 检查员工作量 | - | - | 检查员工作量统计 |

### 9. 导出中心

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 导出中心 | `/inspection/export-center` | `inspection:export:view` | 查看导出任务列表 |
| 创建导出 | - | `inspection:export:create` | 创建新的数据导出任务 |
| 下载文件 | - | - | 下载已生成的导出文件 |

### 10. 权限与角色

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 角色管理 | `/access/roles` | `system:role:view` | 管理检查相关角色 |
| 权限分配 | - | `system:role:edit` | 为角色分配检查权限 |
| 数据权限 | - | `system:role:data` | 配置角色的数据访问范围 |

---

## 二、检查管理员 (Inspection Admin)

### 1. 项目管理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 项目列表 | `/inspection/v6/projects` | `inspection:project:view` | 查看负责的检查项目 |
| 项目详情 | `/inspection/v6/projects/:id` | `inspection:project:view` | 查看项目详情和统计数据 |
| 项目配置 | - | `inspection:project:update` | 调整检查范围和配置 |

### 2. 任务管理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 任务列表 | `/inspection/v6/tasks` | `inspection:task:view` | 查看所有检查任务 |
| 任务详情 | `/inspection/v6/tasks/:id` | `inspection:task:view` | 查看任务详情和目标列表 |
| 审核任务 | - | `inspection:task:review` | 审核已提交的检查任务 |
| 发布任务 | - | `inspection:task:publish` | 发布检查任务结果 |
| 退回任务 | - | `inspection:task:review` | 退回需修改的任务 |

### 3. 申诉管理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 申诉列表 | `/inspection/appeals` | `inspection:appeal:view` | 查看所有申诉记录 |
| 申诉审核 | - | `inspection:appeal:review` | 审核申诉（通过/驳回） |
| 申诉统计 | - | - | 查看申诉统计数据 |

### 4. 整改监管

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 工单监管 | `/inspection/v6/corrective-actions` | `corrective:action:view` | 查看整改工单进度 |
| 工单验收 | - | `corrective:action:verify` | 验收整改结果 |
| 工单驳回 | - | `corrective:action:verify` | 驳回不合格整改 |

### 5. 统计分析

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 汇总统计 | `/inspection/v6/summary` | `inspection:summary:view` | 汇总统计仪表盘 |
| 排名榜 | `/inspection/v6/ranking` | `inspection:summary:view` | 每日排名榜 |
| 评级频次 | - | `quantification:rating:view` | 评级频次统计 |
| 排名结果 | `/inspection/ranking-results` | `inspection:record:view` | 查看排名结果 |

### 6. 学生行为管理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 行为记录 | `/inspection/behavior-records` | `behavior:record:view` | 查看学生行为记录 |
| 行为统计 | - | - | 学生行为数据统计 |
| 行为预警 | - | - | 查看行为预警列表 |

---

## 三、检查员 (Inspector)

### 1. 任务领取与执行

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 我的任务 | `/inspection/v6/my-tasks` | `inspection:task:execute` | 查看待办检查任务 |
| 可领取任务 | - | `inspection:task:execute` | 查看可领取的任务（FREE模式） |
| 领取任务 | - | `inspection:task:execute` | 领取可用的检查任务 |
| 开始任务 | - | `inspection:task:execute` | 开始执行检查任务 |

### 2. 检查执行

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 执行检查 | `/inspection/v6/tasks/:id/execute` | `inspection:task:execute` | 检查执行界面 |
| 记录扣分 | - | - | 记录检查扣分明细 |
| 记录加分 | - | - | 记录检查加分明细 |
| 评分/评级 | - | - | 根据策略进行评分或评级 |
| 拍照取证 | - | - | 上传检查证据照片 |
| 添加备注 | - | - | 添加检查备注说明 |
| 创建整改 | - | `corrective:action:create` | 发现问题时创建整改工单 |

### 3. 任务提交

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 提交任务 | - | `inspection:task:execute` | 提交检查任务 |
| 保存草稿 | - | - | 保存检查进度 |

### 4. 查看记录

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 任务详情 | `/inspection/v6/tasks/:id` | `inspection:task:view` | 查看任务详情 |
| 历史任务 | - | - | 查看历史执行记录 |

---

## 四、班主任 (Class Teacher)

### 1. 班级检查数据

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 班主任工作台 | `/inspection/teacher-dashboard` | - | 班级检查数据概览 |
| 本班检查详情 | - | - | 查看本班检查详情和扣分明细 |
| 班级荣誉 | `/inspection/class-honor/:classId?` | `quantification:rating:view` | 班级荣誉展示 |
| 班级排名 | - | - | 查看班级在各项检查中的排名 |

### 2. 申诉管理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 发起申诉 | - | `inspection:appeal:create` | 对检查结果提出申诉 |
| 我的申诉 | - | - | 查看本班申诉记录 |
| 申诉跟踪 | - | - | 跟踪申诉处理进度 |

### 3. 整改处理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 待办工单 | - | `corrective:action:handle` | 查看待处理整改工单 |
| 提交整改 | - | - | 上传整改证明和说明 |
| 整改历史 | - | - | 查看班级整改历史 |

### 4. 学生管理

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 学生行为 | - | `behavior:record:view` | 查看本班学生行为记录 |
| 学生画像 | `/inspection/student-profile/:studentId?` | - | 查看学生行为画像 |
| 行为预警 | - | - | 处理学生行为预警 |

---

## 五、普通教师 (Teacher)

### 1. 数据查看

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 排名结果 | `/inspection/ranking-results` | `inspection:record:view` | 查看公开的排名结果 |
| 汇总统计 | `/inspection/v6/summary` | `inspection:summary:view` | 查看汇总统计 |
| 排名榜 | `/inspection/v6/ranking` | `inspection:summary:view` | 查看排名榜 |

### 2. 通报查看

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 查看通报 | `/notification/view/:id` | - | 查看公开通报（无需登录） |
| 通报历史 | - | `quantification:notification:view` | 查看历史通报 |

---

## 六、学生/家长 (Student/Parent)

### 1. 公开查看

| 功能 | 路由 | 权限 | 描述 |
|------|------|------|------|
| 查看通报 | `/notification/view/:id` | 无需登录 | 查看公开发布的通报 |
| 班级荣誉 | `/inspection/class-honor/:classId` | - | 查看班级荣誉展示 |

### 2. 微信小程序

| 功能 | 描述 |
|------|------|
| 检查结果推送 | 接收检查结果微信通知 |
| 行为记录查看 | 查看学生行为记录 |
| 整改进度 | 跟踪班级整改进度 |

---

## 七、功能模块汇总

### 按模块分类

| 模块 | 功能数量 | 主要权限前缀 |
|------|----------|--------------|
| 实体类型管理 | 5 | `inspection:entity-type:*`, `inspection:entity-group:*` |
| 检查配置 | 4 | `quantification:config:*`, `quantification:template:*` |
| 评分策略 | 9 | `inspection:scoring-strategy:*` |
| 项目管理 | 10 | `inspection:project:*` |
| 任务管理 | 8 | `inspection:task:*` |
| 申诉管理 | 4 | `inspection:appeal:*` |
| 整改工单 | 7 | `corrective:action:*`, `corrective:rule:*` |
| 评级系统 | 8 | `quantification:rating:*`, `quantification:badge:*` |
| 通报管理 | 4 | `quantification:notification:*` |
| 学生行为 | 3 | `behavior:record:*` |
| 排班调度 | 2 | `schedule:policy:*` |
| 数据分析 | 3 | `analytics:view` |
| 导出中心 | 2 | `inspection:export:*` |

### 权限代码清单

```
# 实体类型权限
inspection:entity-type:view          # 查看实体类型
inspection:entity-type:create        # 创建实体类型
inspection:entity-type:update        # 更新实体类型
inspection:entity-type:delete        # 删除实体类型
inspection:entity-group:manage       # 管理实体分组

# 检查配置权限
quantification:config:view           # 查看量化配置
quantification:template:view         # 查看检查模板
quantification:template:add          # 添加检查模板
quantification:template:edit         # 编辑检查模板
quantification:template:delete       # 删除检查模板

# 评分策略权限
inspection:scoring-strategy:view     # 查看评分策略
inspection:scoring-strategy:create   # 创建评分策略
inspection:scoring-strategy:update   # 更新评分策略
inspection:scoring-strategy:delete   # 删除评分策略

# 项目管理权限
inspection:project:view              # 查看检查项目
inspection:project:create            # 创建检查项目
inspection:project:update            # 更新检查项目
inspection:project:delete            # 删除检查项目
inspection:project:publish           # 发布检查项目

# 任务管理权限
inspection:task:view                 # 查看检查任务
inspection:task:generate             # 生成检查任务
inspection:task:assign               # 分配检查任务
inspection:task:execute              # 执行检查任务
inspection:task:review               # 审核检查任务
inspection:task:publish              # 发布检查任务

# 汇总统计权限
inspection:summary:view              # 查看汇总统计
inspection:summary:generate          # 生成汇总统计

# 检查记录权限
inspection:record:view               # 查看检查记录

# 申诉权限
inspection:appeal:view               # 查看申诉
inspection:appeal:create             # 创建申诉
inspection:appeal:review             # 审核申诉

# 整改工单权限
corrective:action:view               # 查看整改工单
corrective:action:create             # 创建整改工单
corrective:action:handle             # 处理整改工单
corrective:action:verify             # 验收整改结果
corrective:action:cancel             # 取消整改工单
corrective:rule:manage               # 管理自动规则

# 评级权限
quantification:rating:view           # 查看评级统计
quantification:rating:approve        # 评级审核
quantification:rating:export         # 导出评级报表
quantification:rating:refresh        # 刷新统计数据

# 徽章权限
quantification:badge:manage          # 管理徽章配置
quantification:badge:grant           # 授予徽章
quantification:badge:revoke          # 撤销徽章
quantification:badge:view            # 查看徽章记录

# 通报权限
quantification:notification:generate     # 生成通报
quantification:notification:certificate  # 生成证书
quantification:notification:publish      # 发布通报
quantification:notification:view         # 查看通报历史

# 学生行为权限
behavior:record:view                 # 查看行为记录
behavior:record:create               # 创建行为记录
behavior:alert:handle                # 处理行为预警

# 排班调度权限
schedule:policy:view                 # 查看排班策略
schedule:policy:manage               # 管理排班策略

# 数据分析权限
analytics:view                       # 查看数据分析

# 导出权限
inspection:export:view               # 查看导出中心
inspection:export:create             # 创建导出任务
```

---

## 八、V6系统特性

### 1. 实体类型体系

- **三大类别**: ORG(组织)、SPACE(场所)、USER(用户)
- **层级结构**: 支持父子类型关系（如：场所→教室、宿舍、办公室）
- **动态分组**: 基于规则的动态实体分组
- **静态分组**: 手动维护的固定实体集合
- **默认类型**: 系统内置班级、宿舍、学生等常用类型

### 2. 评分策略体系

| 策略类型 | 代码 | 描述 |
|---------|------|------|
| 扣分制 | DEDUCTION | 从满分开始扣分，扣完为止 |
| 加分制 | ADDITION | 从0分开始加分，上限可配置 |
| 基础分制 | BASE_SCORE | 基础分+扣分+加分的综合模式 |
| 等级制 | GRADE | A/B/C/D等级评定 |
| 评级制 | RATING | 1-5星评级 |
| 通过制 | PASS_FAIL | 通过/不通过二元判定 |
| 清单制 | CHECKLIST | 逐项检查完成情况 |

### 3. 项目周期配置

- **四种周期类型**: DAILY(每日)、WEEKLY(每周)、MONTHLY(每月)、CUSTOM(自定义)
- **自动任务生成**: 每日凌晨2点自动生成
- **自动汇总统计**: 每日凌晨3点自动生成
- **周期灵活配置**: 支持指定星期几、每月几号等

### 4. 任务分配模式

- **FREE(自由领取)**: 检查员自主领取任务
- **ASSIGNED(指定分配)**: 管理员指定检查员
- **HYBRID(混合模式)**: 部分指定+部分自由

### 5. 检查范围选择

- **树形选择**: 从组织架构树选择检查范围
- **分组选择**: 选择预定义的实体分组
- **手动选择**: 手动勾选具体检查目标

### 6. 整改工单流程

```
PENDING(待整改) → SUBMITTED(已提交) → VERIFIED(已验收)
                                    ↘ REJECTED(已驳回) → SUBMITTED

PENDING/SUBMITTED → CANCELLED(已取消)
```

- **逾期检测**: 自动标记超期未完成的工单
- **自动规则**: 支持配置自动创建工单的条件
- **验收流程**: 整改提交后需管理员验收确认

### 7. 排班调度特性

- **三种轮询算法**: ROUND_ROBIN(轮询)、RANDOM(随机)、LOAD_BALANCED(负载均衡)
- **节假日排除**: 支持配置排除日期
- **自动执行**: 按策略自动分配检查员

### 8. 学生行为特性

- **行为分类**: VIOLATION(违规)、COMMENDATION(表扬)
- **来源追踪**: INSPECTION(检查)、TEACHER_REPORT(教师上报)、SELF_REPORT(自报)
- **预警机制**: 频次/严重度/趋势三种预警类型
- **学生画像**: 综合展示学生行为数据

---

## 九、路由清单

### V6检查路由

| 路由 | 组件 | 描述 |
|------|------|------|
| `/inspection/v6/entity-types` | EntityTypeManagement.vue | 实体类型管理 |
| `/inspection/v6/scoring-strategies` | ScoringStrategyManagement.vue | 评分策略管理 |
| `/inspection/v6/projects` | ProjectList.vue | 项目列表 |
| `/inspection/v6/projects/create` | ProjectCreate.vue | 创建项目向导 |
| `/inspection/v6/projects/:id` | ProjectDetail.vue | 项目详情 |
| `/inspection/v6/tasks` | TaskList.vue | 任务列表 |
| `/inspection/v6/tasks/:id` | TaskDetail.vue | 任务详情 |
| `/inspection/v6/tasks/:id/execute` | TaskExecute.vue | 任务执行 |
| `/inspection/v6/my-tasks` | MyTasks.vue | 我的任务 |
| `/inspection/v6/corrective-actions` | CorrectiveActionManagement.vue | 整改管理 |
| `/inspection/v6/summary` | SummaryDashboard.vue | 汇总统计 |
| `/inspection/v6/ranking` | DailyRanking.vue | 每日排名 |

### 其他检查路由

| 路由 | 组件 | 描述 |
|------|------|------|
| `/inspection/config` | InspectionConfig.vue | 量化配置中心 |
| `/inspection/appeals` | AppealManagement.vue | 申诉管理 |
| `/inspection/teacher-dashboard` | TeacherDashboard.vue | 班主任工作台 |
| `/inspection/data-analytics` | DataAnalyticsCenter.vue | 数据分析中心 |
| `/inspection/ranking-results` | RankingResults.vue | 排名结果 |
| `/inspection/export-center` | ExportCenter.vue | 导出中心 |
| `/inspection/student-profile/:studentId?` | StudentBehaviorProfile.vue | 学生行为画像 |
