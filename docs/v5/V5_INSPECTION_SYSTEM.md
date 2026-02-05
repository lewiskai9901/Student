# V5 重构方案 - 检查系统设计

> **版本**: 3.0
> **日期**: 2026-01-31
> **关联文档**: [V5_ARCHITECTURE.md](./V5_ARCHITECTURE.md)

---

## 一、系统架构

### 1.1 三级结构

```
┌──────────────┐
│ 检查模板     │  ←── 定义扣分项、类别、规则
│ (Template)   │      可见性：私有/部门/公开
└──────┬───────┘
       │ 引用（复制快照）
       ▼
┌──────────────┐
│ 检查项目     │  ←── 配置检查范围、时间、权重
│ (Project)    │      5种录入模式
└──────┬───────┘
       │ 自动生成
       ▼
┌──────────────┐
│ 检查任务     │  ←── 每日/每周任务
│ (Task)       │      分配检查员
└──────┬───────┘
       │ 包含
       ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ 检查记录     │────▶│ 扣分明细     │────▶│ 证据附件     │
│ (Record)     │     │ (Deduction)  │     │ (Evidence)   │
└──────────────┘     └──────────────┘     └──────────────┘
```

### 1.2 核心流程

```
模板创建 → 项目配置 → 任务生成 → 检查执行 → 审核发布 → 汇总排名
                                    │
                                    ├── 申诉处理
                                    └── 整改管理
```

---

## 二、检查模板

### 2.1 模板结构

```yaml
Template:
  - template_code: 宿舍卫生检查
  - template_name: 宿舍卫生日常检查模板
  - target_type: DORMITORY  # 检查目标类型
  - base_score: 100         # 基准分
  - scoring_mode: DEDUCTION # 计分模式
  - visibility: DEPARTMENT  # 可见性

Categories:
  - category_code: HYGIENE
    category_name: 卫生
    weight: 0.6
    items:
      - item_code: FLOOR
        item_name: 地面卫生
        score: 2
        require_evidence: true
      - item_code: BED
        item_name: 床铺整理
        score: 1

  - category_code: SAFETY
    category_name: 安全
    weight: 0.4
    items:
      - item_code: ELECTRICAL
        item_name: 违规电器
        score: 5
        require_evidence: true
```

### 2.2 计分模式

| 模式 | 说明 | 计算公式 |
|------|------|----------|
| DEDUCTION | 扣分制 | 最终分 = 基准分 - 扣分 |
| ADDITION | 加分制 | 最终分 = 加分合计 |
| CHECKLIST | 清单制 | 最终分 = 完成项/总项 × 100 |
| BONUS_ONLY | 仅加分 | 最终分 = 基准分 + 加分 |

### 2.3 可见性设置

| 可见性 | 说明 |
|--------|------|
| PRIVATE | 仅创建者可见 |
| DEPARTMENT | 创建者所在部门可见 |
| PUBLIC | 全校可见（需审核） |

---

## 三、检查项目

### 3.1 项目配置

```yaml
Project:
  - project_code: 2024-FALL-DORM
  - project_name: 2024秋季学期宿舍检查
  - template_id: 1              # 关联模板
  - template_snapshot: {...}    # 模板快照（固化）

  # 检查范围
  - inspection_level: DEPARTMENT
  - target_org_unit_ids: [1, 2, 3]

  # 时间配置
  - start_date: 2024-09-01
  - end_date: 2025-01-15
  - semester_id: 20241

  # 录入模式
  - entry_mode: SPACE

  # 公平权重
  - fair_weight_enabled: true
  - fair_weight_mode: DIVIDE
  - benchmark_count: null

  # 混合宿舍策略
  - mixed_dormitory_strategy: RATIO
```

### 3.2 录入模式

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| SPACE | 按场所录入 | 宿舍逐间检查 |
| PERSON | 按人员录入 | 仪容仪表检查 |
| CLASS | 按班级录入 | 课堂纪律检查 |
| ITEM | 按项目录入 | 单项专项检查 |
| CHECKLIST | 清单打勾 | 安全检查清单 |

### 3.3 公平权重

解决不同班级被检查次数不同导致的不公平：

| 模式 | 计算方式 |
|------|----------|
| DIVIDE | 权重 = 1 / 检查次数 |
| BENCHMARK | 权重 = 基准次数 / 实际次数 |

### 3.4 混合宿舍策略

当宿舍住多个班级学生时的分数分配：

| 策略 | 说明 |
|------|------|
| RATIO | 按班级人数比例分配扣分 |
| AVERAGE | 各班级平均分配扣分 |
| FULL | 每个班级都扣全额 |
| MAIN | 仅主班级扣分 |

---

## 四、检查任务

### 4.1 任务生成

```
┌────────────────────────────────────────────────────────────────┐
│                        任务生成流程                             │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│   项目配置                       任务列表                       │
│   ┌──────────┐                  ┌──────────┐                   │
│   │ 项目A    │   自动生成       │ 任务1    │ 2024-09-01       │
│   │          │ ───────────────▶ │ 任务2    │ 2024-09-02       │
│   │ 每日检查 │                  │ 任务3    │ 2024-09-03       │
│   └──────────┘                  │ ...      │                   │
│                                 └──────────┘                   │
│                                                                │
│   生成规则:                                                     │
│   - 按项目时间范围自动生成                                      │
│   - 支持手动补录                                               │
│   - 节假日可跳过                                               │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### 4.2 任务状态

```
DRAFT → IN_PROGRESS → SUBMITTED → REVIEWED → PUBLISHED
  │          │             │          │          │
  │          │             │          │          └── 发布（学生可见）
  │          │             │          └── 审核通过
  │          │             └── 检查员提交
  │          └── 开始检查
  └── 新建/待开始
```

---

## 五、检查记录

### 5.1 记录结构

```yaml
TargetInspectionRecord:
  - task_id: 1
  - target_type: DORMITORY
  - target_id: 101            # 宿舍ID

  # 快照信息（检查时刻状态）
  - target_snapshot: {...}
  - class_id: 201             # 归属班级
  - class_name: 2024级软件1班
  - org_unit_id: 10           # 归属部门
  - org_unit_name: 软件工程系

  # 分数
  - base_score: 100
  - raw_score: 95             # 原始分
  - weighted_score: 95        # 加权分
  - fair_adjusted_score: 95   # 公平调整分

  - total_deduction: 5
  - deduction_count: 2

  # 检查人
  - inspector_id: 12345
  - inspected_at: 2024-09-01 10:30:00
```

### 5.2 扣分明细

```yaml
DeductionRecord:
  - target_record_id: 1
  - category_id: 1            # 卫生类别
  - score_item_id: 3          # 地面卫生
  - score: 2
  - score_type: DEDUCTION
  - quantity: 1
  - student_id: null          # 关联学生（可选）
  - remark: 地面有杂物
```

---

## 六、申诉管理

### 6.1 申诉流程

```
提交申诉 → 一级审核 → 二级审核 → 申诉生效
    │          │          │          │
    │          │          │          └── 更新检查记录分数
    │          │          └── 最终审核（通过/驳回）
    │          └── 初审（通过→二级，驳回→结束）
    └── 学生/班主任提交
```

### 6.2 申诉状态

| 状态 | 说明 |
|------|------|
| PENDING | 待审核 |
| PENDING_LEVEL1_REVIEW | 一级审核中 |
| PENDING_LEVEL2_REVIEW | 二级审核中 |
| APPROVED | 已通过 |
| REJECTED | 已驳回 |
| WITHDRAWN | 已撤回 |

### 6.3 申诉时限

- 申诉窗口期：检查发布后 7 天内
- 一级审核期限：3 个工作日
- 二级审核期限：5 个工作日

---

## 七、整改管理

### 7.1 整改流程

```
下发整改 → 整改执行 → 提交整改 → 复查验收 → 整改完成
    │          │          │          │          │
    │          │          │          │          └── 关闭工单
    │          │          │          └── 验收通过/不通过（可多轮）
    │          │          └── 班主任提交整改材料
    │          └── 班主任收到通知
    └── 检查员发起
```

### 7.2 整改状态

| 状态 | 说明 |
|------|------|
| PENDING | 待整改 |
| IN_PROGRESS | 整改中 |
| SUBMITTED | 已提交待验收 |
| VERIFIED | 验收通过 |
| REJECTED | 验收不通过 |
| COMPLETED | 已完成 |
| OVERDUE | 已逾期 |

### 7.3 整改配置

- 整改期限：默认 3 天，可配置
- 最大轮次：默认 3 轮
- 逾期处理：自动标记逾期，可选额外扣分

---

## 八、汇总与排名

### 8.1 汇总生成

```
┌────────────────────────────────────────────────────────────────┐
│                        汇总生成流程                             │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│   检查记录                       每日汇总                       │
│   ┌──────────┐                  ┌──────────┐                   │
│   │ 记录1    │   聚合计算       │ 软件1班  │ 95.5分           │
│   │ 记录2    │ ───────────────▶ │ 软件2班  │ 93.2分           │
│   │ 记录3    │                  │ 网络1班  │ 91.8分           │
│   └──────────┘                  └──────────┘                   │
│                                                                │
│   汇总维度:                                                     │
│   - 按班级（最常用）                                            │
│   - 按部门                                                      │
│   - 按年级                                                      │
│                                                                │
│   触发方式:                                                     │
│   - 定时任务（每日凌晨2点）                                     │
│   - 手动触发                                                    │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### 8.2 排名计算

使用标准竞赛排名（1224排名）：

| 班级 | 分数 | 排名 |
|------|------|------|
| 软件1班 | 98 | 1 |
| 软件2班 | 98 | 1 |
| 网络1班 | 95 | 3 |
| 网络2班 | 92 | 4 |

### 8.3 自动加分规则

| 规则类型 | 说明 | 示例 |
|----------|------|------|
| CONSECUTIVE_FULL_SCORE | 连续满分 | 连续7天满分加2分 |
| MONTHLY_EXCELLENT | 月度优秀 | 月排名前3加3分 |
| IMPROVEMENT | 进步奖 | 排名上升5名以上加1分 |
| CUSTOM | 自定义 | 按条件配置 |

---

## 九、数据权限配置

### 9.1 模块权限

| 模块代码 | 过滤字段 | 说明 |
|----------|----------|------|
| inspection_template | org_unit_id, created_by | 模板归属 |
| inspection_project | org_unit_id, created_by | 项目归属 |
| inspection_task | org_unit_id | 任务归属 |
| inspection_record | org_unit_id, class_id | 记录归属（目标班级） |
| inspection_personal | target_id | 个人记录（学生） |
| inspection_appeal | org_unit_id, class_id, created_by | 申诉归属 |
| inspection_summary | org_unit_id, class_id | 汇总归属 |
| inspection_corrective | org_unit_id, class_id | 整改归属 |

### 9.2 记录归属说明

检查记录的部门归属使用**快照模式**：

- 记录创建时，保存目标的班级和部门信息
- 后续目标调整班级/部门，不影响已有记录
- 数据权限过滤使用快照字段

```sql
-- 数据权限过滤示例
WHERE (
    snapshot_department_id IN (1, 2, 3)
    OR (snapshot_department_id IS NULL AND org_unit_id IN (1, 2, 3))
)
```

---

## 十、前端组件

### 10.1 组件列表

| 组件 | 路径 | 说明 |
|------|------|------|
| PersonalInspectionRecords | components/inspection/ | 学生个人检查记录 |
| CorrectiveOrderCard | components/inspection/ | 整改工单卡片 |
| ClassRankingTable | components/inspection/ | 班级排名表格 |
| DeductionItemPicker | components/inspection/ | 扣分项选择器 |

### 10.2 页面列表

| 页面 | 路由 | 说明 |
|------|------|------|
| InspectionConfig | /inspection/config | 检查配置（模板管理） |
| InspectionPlanList | /inspection/plans | 检查计划（项目管理） |
| InspectionExecute | /inspection/execute | 检查执行 |
| AppealManagement | /inspection/appeals | 申诉管理 |
| CorrectiveActions | /inspection/correctives | 整改管理 |
| RankingResults | /inspection/rankings | 排名结果 |
| DataAnalyticsCenter | /inspection/analytics | 数据分析 |
| StudentBehaviorProfile | /inspection/behavior | 学生行为档案 |
| TeacherDashboard | /inspection/dashboard | 教师工作台 |

---

**文档版本**: 3.0
**最后更新**: 2026-01-31
