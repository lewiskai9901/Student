# V5 量化检查系统完整设计方案

> **版本**: 3.0 Final
> **日期**: 2026-01-30
> **状态**: 最终方案

---

## 一、核心概念定义

### 1.1 概念总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           V5 量化检查系统概念模型                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐     │
│  │  检查模板        │      │  检查项目        │      │  检查任务        │     │
│  │  (Template)     │ ──→  │  (Project)      │ ──→  │  (Task)         │     │
│  │                 │      │                 │      │                 │     │
│  │ 定义"怎么检查"   │      │ 定义"谁来检查"   │      │ 定义"这次检查"   │     │
│  │ - 检查类别       │      │ - 负责人         │      │ - 具体日期       │     │
│  │ - 扣分项/加分项  │      │ - 检查频率       │      │ - 实际执行       │     │
│  │ - 打分规则       │      │ - 权重配置       │      │ - 检查记录       │     │
│  └─────────────────┘      │ - 包含哪些类别   │      └─────────────────┘     │
│                           └─────────────────┘                               │
│                                    │                                        │
│                                    ▼                                        │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                           汇总系统 (Summary)                          │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐             │  │
│  │  │ 每日汇总  │  │ 每周汇总  │  │ 每月汇总  │  │ 学期汇总  │             │  │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘             │  │
│  │                                                                      │  │
│  │  汇总维度: 按项目 | 按模板 | 按类别 | 全校                            │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 核心概念详解

#### 1.2.1 检查模板 (InspectionTemplate)

**定义**: 检查的结构定义，描述"检查什么、怎么打分"

```
检查模板
├── 基本信息
│   ├── 模板名称: "宿舍卫生检查模板"
│   ├── 模板描述
│   └── 适用范围: 全校 / 指定部门
│
├── 检查类别 (1:N)
│   ├── 类别1: 地面卫生
│   │   ├── 扣分项: 地面有垃圾 (-3分)
│   │   ├── 扣分项: 地面未拖 (-2分)
│   │   └── 加分项: 地面特别整洁 (+2分)
│   │
│   └── 类别2: 物品摆放
│       ├── 扣分项: 被子未叠 (-2分)
│       └── 扣分项: 物品杂乱 (-3分)
│
└── 默认配置
    ├── 默认打分模式
    └── 默认基准分
```

#### 1.2.2 检查项目 (InspectionProject)

**定义**: 检查的组织单元，描述"谁负责、怎么组织"

```
检查项目
├── 基本信息
│   ├── 项目名称: "2024学年宿舍日常检查"
│   ├── 负责人: 张某
│   ├── 参与人员: [检查员A, 检查员B]
│   └── 有效期: 2024-09-01 ~ 2025-06-30
│
├── 包含的检查类别 (从模板选择或自定义)
│   ├── 宿舍卫生 (来自模板A)
│   ├── 宿舍纪律 (来自模板B)
│   └── 自定义类别...
│
├── 检查频率配置
│   ├── 频率类型: 每天 / 每周N次 / 自定义
│   ├── 检查时段: 上午 / 下午 / 晚上
│   └── 自动生成任务: 是/否
│
├── 权重配置 (必须配置)
│   ├── 类别权重: 卫生50% + 纪律50%
│   ├── 公平权重模式: 按人数比例 / 基准人数法 / 关闭
│   ├── 混合宿舍策略: 按人数比例 / 平均分配 / 全额 / 主责
│   └── 缺失项处理: 不计入 / 用满分
│
├── 打分配置 (必须配置)
│   ├── 打分模式: 百分制 / 纯扣分制 / 等级制 / 达标制
│   ├── 基准分: 100 (百分制时)
│   └── 等级映射: A=95, B=85... (等级制时)
│
└── 汇总配置
    ├── 汇总周期: 日 / 周 / 月 / 学期
    ├── 公布方式: 自动 / 手动审核
    └── 公布时间: 每天22:00
```

#### 1.2.3 检查任务 (InspectionTask)

**定义**: 每次具体的检查执行，描述"这一次检查"

```
检查任务
├── 基本信息
│   ├── 任务编号: TASK-20260130-001
│   ├── 所属项目: 2024学年宿舍日常检查
│   ├── 检查日期: 2026-01-30
│   └── 状态: 草稿 → 进行中 → 已提交 → 已发布
│
├── 检查目标 (本次检查的对象)
│   ├── 目标类型: 宿舍 / 班级 / 学生
│   └── 目标范围: 全部 / 指定部门 / 随机抽查
│
├── 执行信息
│   ├── 检查人员: [张三, 李四]
│   ├── 开始时间
│   ├── 提交时间
│   └── 发布时间
│
├── 检查记录 (1:N)
│   ├── 宿舍101记录
│   │   ├── 扣分: 地面有垃圾 -3分
│   │   ├── 扣分: 被子未叠 -2分 (学生: 王五)
│   │   └── 证据: [照片1, 照片2]
│   │
│   └── 宿舍102记录...
│
└── 统计信息
    ├── 检查数量
    ├── 平均分
    └── 扣分总计
```

#### 1.2.4 检查目标 (InspectionTarget)

**定义**: 被检查的对象

```
检查目标类型
├── 学生 (Student)
│   ├── 直接对学生个人打分/扣分
│   └── 自动归属到: 班级 → 部门
│
├── 班级 (Class)
│   ├── 对班级整体打分/扣分
│   └── 自动归属到: 部门
│
├── 场所 (Space)
│   ├── 宿舍 (Dormitory)
│   │   ├── 对宿舍打分/扣分
│   │   └── 可选关联: 学生 → 班级 → 部门
│   │
│   └── 教室 (Classroom)
│       ├── 对教室打分/扣分
│       └── 可选关联: 班级 → 部门
│
└── 最终汇总链路
    场所/学生 → 班级 → 部门 → 全校
```

---

## 二、数据模型设计

### 2.1 实体关系图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              实体关系图 (ERD)                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  InspectionTemplate ──────┐                                                 │
│  (检查模板)                │ 1:N                                            │
│       │                   ▼                                                 │
│       │            TemplateCategory ──────┐                                 │
│       │            (模板类别)              │ 1:N                            │
│       │                   │               ▼                                 │
│       │                   │         ScoreItem                               │
│       │                   │         (扣分项/加分项)                          │
│       │                   │                                                 │
│       │ 引用              │ 引用                                            │
│       ▼                   ▼                                                 │
│  InspectionProject ◄───── ProjectCategory                                   │
│  (检查项目)         1:N   (项目检查类别)                                     │
│       │                        │                                            │
│       │ 1:N                    │ 配置                                       │
│       ▼                        ▼                                            │
│  InspectionTask          CategoryWeightConfig                               │
│  (检查任务)              (类别权重配置)                                      │
│       │                                                                     │
│       │ 1:N                                                                 │
│       ▼                                                                     │
│  TargetRecord ─────────────────────────────────────────┐                    │
│  (目标记录: 宿舍/班级/学生)                              │ 1:N              │
│       │                                                ▼                    │
│       │ 1:N                                      ScoreDetail                │
│       ▼                                          (扣分/加分明细)             │
│  ClassRecord                                          │                     │
│  (班级汇总记录)                                        │ 1:N               │
│       │                                               ▼                     │
│       │                                          Evidence                   │
│       │                                          (证据: 照片等)             │
│       │ N:1                                                                 │
│       ▼                                                                     │
│  DailySummary ──► WeeklySummary ──► MonthlySummary ──► SemesterSummary     │
│  (每日汇总)       (每周汇总)        (每月汇总)          (学期汇总)           │
│                                                                             │
│  ════════════════════════════════════════════════════════════════════════  │
│                                                                             │
│  Appeal (申诉) ◄─────── ScoreDetail                                         │
│       │                                                                     │
│       │ 1:N                                                                 │
│       ▼                                                                     │
│  AppealApproval (审批记录)                                                  │
│                                                                             │
│  ════════════════════════════════════════════════════════════════════════  │
│                                                                             │
│  CorrectiveAction (整改工单) ◄─────── ScoreDetail                           │
│       │                                                                     │
│       │ 1:N                                                                 │
│       ▼                                                                     │
│  CorrectiveRecord (整改记录/复查)                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 核心实体定义

#### 2.2.1 检查模板 (InspectionTemplate)

```sql
CREATE TABLE inspection_templates (
    id BIGINT PRIMARY KEY,

    -- 基本信息
    template_code VARCHAR(32) NOT NULL UNIQUE,
    template_name VARCHAR(200) NOT NULL,
    description TEXT,

    -- 适用范围
    scope ENUM('GLOBAL', 'DEPARTMENT', 'CUSTOM') NOT NULL DEFAULT 'GLOBAL',
    scope_org_ids JSON,  -- 指定部门时的部门ID列表

    -- 版本控制
    version INT NOT NULL DEFAULT 1,
    status ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT',

    -- 默认配置
    default_scoring_mode ENUM('PERCENTAGE', 'DEDUCTION_ONLY', 'BONUS_ONLY', 'GRADE', 'PASS_FAIL') DEFAULT 'PERCENTAGE',
    /*
      PERCENTAGE: 百分制 - 从基准分扣减/加分
      DEDUCTION_ONLY: 纯扣分制 - 只扣分不加分
      BONUS_ONLY: 纯加分制 - 只加分不扣分 (如评优检查)
      GRADE: 等级制 - A/B/C/D等级评定
      PASS_FAIL: 合格制 - 合格/不合格
    */
    default_base_score INT DEFAULT 100,

    -- 审计
    creator_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    deleted TINYINT DEFAULT 0,

    INDEX idx_status (status),
    INDEX idx_scope (scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.2.2 模板类别 (TemplateCategory)

```sql
CREATE TABLE template_categories (
    id BIGINT PRIMARY KEY,
    template_id BIGINT NOT NULL,

    -- 类别信息
    category_code VARCHAR(32) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INT DEFAULT 0,

    -- 检查目标类型
    target_type ENUM('STUDENT', 'CLASS', 'DORMITORY', 'CLASSROOM') NOT NULL,

    -- 是否关联到班级 (场所检查时)
    link_to_class TINYINT DEFAULT 1,

    -- 默认配置
    default_base_score INT DEFAULT 100,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE INDEX uk_template_code (template_id, category_code),
    FOREIGN KEY (template_id) REFERENCES inspection_templates(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.2.3 扣分项/加分项 (ScoreItem)

```sql
CREATE TABLE score_items (
    id BIGINT PRIMARY KEY,
    category_id BIGINT NOT NULL,

    -- 基本信息
    item_code VARCHAR(32) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    description TEXT,
    item_type ENUM('DEDUCTION', 'BONUS') NOT NULL,  -- 扣分项 / 加分项
    sort_order INT DEFAULT 0,

    -- 多模式打分配置 (JSON)
    scoring_configs JSON NOT NULL,
    /*
    {
      "PERCENTAGE": {
        "score": 5,           -- 扣/加分值
        "min": 1,             -- 最小值
        "max": 10,            -- 最大值
        "perPerson": false,   -- 是否按人数计算
        "perPersonScore": 1   -- 每人扣/加分
      },
      "DEDUCTION_ONLY": {
        "score": 5
      },
      "GRADE": {
        "levelChange": 1      -- 等级变化
      },
      "PASS_FAIL": {
        "result": "FAIL"      -- 不通过
      }
    }
    */

    -- 权重配置
    weight_config JSON,
    /*
    {
      "enableFairWeight": true,        -- 启用公平权重
      "fairWeightMode": "DIVIDE",      -- DIVIDE(除以人数) / BENCHMARK(基准人数法)
      "benchmarkCount": 40,            -- 基准人数
      "customWeight": 1.0              -- 自定义权重系数
    }
    */

    -- 是否需要关联人员
    require_person TINYINT DEFAULT 0,
    -- 是否需要证据
    require_evidence TINYINT DEFAULT 0,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE INDEX uk_category_code (category_id, item_code),
    FOREIGN KEY (category_id) REFERENCES template_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.2.4 检查项目 (InspectionProject)

```sql
CREATE TABLE inspection_projects (
    id BIGINT PRIMARY KEY,

    -- 基本信息
    project_code VARCHAR(32) NOT NULL UNIQUE,
    project_name VARCHAR(200) NOT NULL,
    description TEXT,

    -- 负责人与参与者
    owner_id BIGINT NOT NULL,           -- 项目负责人
    participant_ids JSON,               -- 参与人员ID列表

    -- 有效期
    start_date DATE NOT NULL,
    end_date DATE,

    -- 检查频率配置
    frequency_config JSON NOT NULL,
    /*
    {
      "type": "DAILY",                  -- DAILY / WEEKLY / CUSTOM
      "timesPerWeek": 3,                -- 每周次数 (WEEKLY时)
      "weekdays": [1, 3, 5],            -- 星期几 (WEEKLY时)
      "timeSlots": ["MORNING", "AFTERNOON"],  -- 时段
      "autoGenerate": true              -- 自动生成任务
    }
    */

    -- 打分配置 (必须配置)
    scoring_config JSON NOT NULL,
    /*
    {
      "mode": "PERCENTAGE",             -- 打分模式
      "baseScore": 100,                 -- 基准分
      "gradeMapping": {                 -- 等级映射 (等级制时)
        "A": 95, "B": 85, "C": 75, "D": 60
      },
      "allowBonus": true,               -- 允许加分
      "maxBonus": 10,                   -- 最大加分
      "minScore": 0,                    -- 最低分
      "maxScore": 100                   -- 最高分 (加分后)
    }
    */

    -- 权重配置 (必须配置)
    weight_config JSON NOT NULL,
    /*
    {
      "fairWeight": {
        "enabled": true,
        "mode": "DIVIDE",               -- DIVIDE / BENCHMARK / DISABLED
        "benchmarkCount": 40
      },
      "mixedDormitory": {
        "strategy": "RATIO"             -- RATIO(按比例) / AVERAGE / FULL / MAIN
      },
      "missingCategory": {
        "strategy": "EXCLUDE"           -- EXCLUDE(不计入) / FULL_SCORE(满分)
      }
    }
    */

    -- 汇总配置
    summary_config JSON NOT NULL,
    /*
    {
      "periods": ["DAILY", "WEEKLY", "MONTHLY", "SEMESTER"],
      "publishMode": "MANUAL",          -- AUTO / MANUAL
      "autoPublishTime": "22:00",       -- 自动发布时间
      "weeklyDay": 7,                   -- 周汇总在周几
      "monthlyDay": 1                   -- 月汇总在几号
    }
    */

    -- 检查级别
    inspection_level ENUM('CLASS', 'GRADE', 'DEPARTMENT') NOT NULL DEFAULT 'CLASS',
    /*
      CLASS: 班级级别 - 检查结果直接归属到班级
      GRADE: 年级级别 - 检查结果可在年级内对比
      DEPARTMENT: 部门级别 - 检查结果可在部门内对比
    */

    -- 允许的录入模式 (可多选)
    allowed_input_modes JSON DEFAULT '["SPACE", "PERSON", "CLASS", "ITEM", "CHECKLIST", "FREE"]',
    -- 默认录入模式
    default_input_mode ENUM('SPACE', 'PERSON', 'CLASS', 'ITEM', 'CHECKLIST', 'FREE') DEFAULT 'SPACE',

    -- 状态
    status ENUM('DRAFT', 'ACTIVE', 'PAUSED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT',

    -- 审计
    creator_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    deleted TINYINT DEFAULT 0,

    INDEX idx_owner (owner_id),
    INDEX idx_status (status),
    INDEX idx_date_range (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.2.5 项目检查类别 (ProjectCategory)

```sql
CREATE TABLE project_categories (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,

    -- 来源
    source_type ENUM('TEMPLATE', 'CUSTOM') NOT NULL,
    template_category_id BIGINT,        -- 来自模板时

    -- 类别信息 (自定义时填写，模板时可覆盖)
    category_name VARCHAR(100) NOT NULL,
    target_type ENUM('STUDENT', 'CLASS', 'DORMITORY', 'CLASSROOM') NOT NULL,
    link_to_class TINYINT DEFAULT 1,

    -- 权重 (必须配置)
    weight_percentage DECIMAL(5,2) NOT NULL,  -- 占比权重，如 40.00 表示 40%

    -- 排序
    sort_order INT DEFAULT 0,

    -- 状态
    enabled TINYINT DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_project (project_id),
    FOREIGN KEY (project_id) REFERENCES inspection_projects(id),
    FOREIGN KEY (template_category_id) REFERENCES template_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.2.6 检查任务 (InspectionTask)

```sql
CREATE TABLE inspection_tasks (
    id BIGINT PRIMARY KEY,

    -- 基本信息
    task_code VARCHAR(32) NOT NULL UNIQUE,
    project_id BIGINT NOT NULL,

    -- 检查时间
    inspection_date DATE NOT NULL,
    time_slot ENUM('MORNING', 'AFTERNOON', 'EVENING', 'ALL_DAY'),

    -- 任务标题 (可自定义，默认自动生成)
    title VARCHAR(200),

    -- 检查范围
    target_scope_config JSON,
    /*
    {
      "type": "ALL",                    -- ALL / SPECIFIED / RANDOM
      "orgUnitIds": [1, 2],             -- 指定部门
      "randomCount": 10,                -- 随机数量
      "randomSeed": "xxx"               -- 随机种子 (可复现)
    }
    */

    -- 执行人员
    inspector_ids JSON,                  -- 检查人员ID列表

    -- 状态
    status ENUM('DRAFT', 'IN_PROGRESS', 'SUBMITTED', 'PUBLISHED', 'CANCELLED')
           NOT NULL DEFAULT 'DRAFT',

    -- 时间线
    started_at DATETIME,
    submitted_at DATETIME,
    submitted_by BIGINT,
    published_at DATETIME,
    published_by BIGINT,

    -- 统计快照
    stats JSON,
    /*
    {
      "totalTargets": 50,               -- 检查目标数
      "completedTargets": 48,           -- 完成数
      "totalDeductions": 120,           -- 总扣分次数
      "totalDeductionScore": 350.5,     -- 总扣分分值
      "totalBonuses": 10,               -- 总加分次数
      "totalBonusScore": 25.0,          -- 总加分分值
      "averageScore": 85.5              -- 平均分
    }
    */

    -- 审计
    creator_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,

    INDEX idx_project (project_id),
    INDEX idx_date (inspection_date),
    INDEX idx_status (status),
    UNIQUE INDEX uk_project_date_slot (project_id, inspection_date, time_slot),
    FOREIGN KEY (project_id) REFERENCES inspection_projects(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.2.7 目标记录 (TargetRecord)

```sql
-- 检查目标记录 (宿舍/教室/班级/学生 的检查记录)
CREATE TABLE target_records (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    project_category_id BIGINT NOT NULL,

    -- 目标信息
    target_type ENUM('STUDENT', 'CLASS', 'DORMITORY', 'CLASSROOM') NOT NULL,
    target_id BIGINT NOT NULL,          -- 目标ID (学生ID/班级ID/宿舍ID/教室ID)
    target_name VARCHAR(100),           -- 目标名称快照
    target_code VARCHAR(50),            -- 目标编码快照 (如宿舍号)

    -- 关联的班级 (场所检查时可选关联)
    linked_class_id BIGINT,
    linked_class_name VARCHAR(100),
    linked_org_unit_id BIGINT,          -- 部门ID
    linked_org_unit_name VARCHAR(100),

    -- 打分模式 (继承自项目配置)
    scoring_mode ENUM('PERCENTAGE', 'DEDUCTION_ONLY', 'BONUS_ONLY', 'GRADE', 'PASS_FAIL') NOT NULL,

    -- 分数计算
    base_score DECIMAL(10,2) DEFAULT 100,     -- 基准分
    raw_deduction DECIMAL(10,2) DEFAULT 0,    -- 原始扣分 (未加权)
    weighted_deduction DECIMAL(10,2) DEFAULT 0, -- 加权后扣分
    raw_bonus DECIMAL(10,2) DEFAULT 0,        -- 原始加分
    weighted_bonus DECIMAL(10,2) DEFAULT 0,   -- 加权后加分
    final_score DECIMAL(10,2),                -- 最终得分
    grade VARCHAR(10),                        -- 等级 (等级制时)
    pass_status TINYINT,                      -- 是否通过 (达标制时)

    -- 状态
    status ENUM('PENDING', 'RECORDING', 'COMPLETED') DEFAULT 'PENDING',

    -- 审计
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,

    INDEX idx_task (task_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_class (linked_class_id),
    INDEX idx_org_unit (linked_org_unit_id),
    UNIQUE INDEX uk_task_category_target (task_id, project_category_id, target_id),
    FOREIGN KEY (task_id) REFERENCES inspection_tasks(id),
    FOREIGN KEY (project_category_id) REFERENCES project_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.2.8 扣分/加分明细 (ScoreDetail)

```sql
CREATE TABLE score_details (
    id BIGINT PRIMARY KEY,
    target_record_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,            -- 冗余，便于查询

    -- 扣分项/加分项信息
    score_item_id BIGINT,               -- 关联的扣分项 (可为空，自由扣分时)
    item_type ENUM('DEDUCTION', 'BONUS') NOT NULL,
    item_name VARCHAR(200) NOT NULL,    -- 项目名称快照
    category_name VARCHAR(100),         -- 类别名称快照

    -- 录入方式
    input_mode ENUM('SPACE', 'PERSON', 'CLASS', 'ITEM', 'CHECKLIST', 'FREE') NOT NULL,
    /*
      SPACE: 按空间(宿舍/教室)录入 - 选择宿舍/教室，然后选择扣分项
      PERSON: 按人员(学生)录入 - 选择学生，然后选择扣分项
      CLASS: 按班级录入 - 直接对班级整体扣分
      ITEM: 按扣分项录入 - 选择扣分项，然后批量选择对象
      CHECKLIST: 清单模式 - 按YES/NO/NA逐项检查
      FREE: 自由录入 - 灵活组合，不受模式限制
    */

    -- 关联的人员 (PERSON模式或需要关联人员时)
    student_ids JSON,                   -- 涉及的学生ID列表
    student_names JSON,                 -- 学生姓名列表
    person_count INT DEFAULT 0,         -- 涉及人数

    -- 关联的空间 (SPACE模式时)
    space_type ENUM('DORMITORY', 'CLASSROOM'),
    space_id BIGINT,
    space_name VARCHAR(100),

    -- 分数
    raw_score DECIMAL(10,2) NOT NULL,   -- 原始分数 (扣分为正数)
    weighted_score DECIMAL(10,2),       -- 加权后分数

    -- 权重计算详情
    weight_calculation JSON,
    /*
    {
      "fairWeightApplied": true,
      "fairWeightMode": "DIVIDE",
      "originalScore": 5,
      "personCount": 50,
      "weightedScore": 0.1,             -- 5/50 = 0.1
      "customWeight": 1.0
    }
    */

    -- 备注
    remark TEXT,

    -- 检查人
    inspector_id BIGINT NOT NULL,
    inspector_name VARCHAR(50),

    -- 时间
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_target_record (target_record_id),
    INDEX idx_task (task_id),
    INDEX idx_item (score_item_id),
    INDEX idx_type (item_type),
    FOREIGN KEY (target_record_id) REFERENCES target_records(id),
    FOREIGN KEY (task_id) REFERENCES inspection_tasks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.2.9 证据 (Evidence)

```sql
CREATE TABLE evidences (
    id BIGINT PRIMARY KEY,
    score_detail_id BIGINT NOT NULL,

    -- 证据类型
    evidence_type ENUM('PHOTO', 'VIDEO', 'AUDIO', 'DOCUMENT') NOT NULL,

    -- 文件信息
    file_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(200),
    file_size BIGINT,                   -- 字节
    mime_type VARCHAR(100),

    -- 水印信息
    watermark_config JSON,
    /*
    {
      "enabled": true,
      "content": {
        "time": "2026-01-30 10:30:00",
        "location": "1号宿舍楼 101室",
        "inspector": "张三",
        "taskCode": "TASK-20260130-001"
      },
      "position": "BOTTOM_RIGHT"
    }
    */

    -- 元信息
    metadata JSON,
    /*
    {
      "width": 1920,
      "height": 1080,
      "duration": 30,                   -- 视频/音频时长(秒)
      "gps": {
        "latitude": 39.9042,
        "longitude": 116.4074
      },
      "capturedAt": "2026-01-30T10:30:00"
    }
    */

    -- 上传信息
    uploader_id BIGINT NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_score_detail (score_detail_id),
    INDEX idx_type (evidence_type),
    FOREIGN KEY (score_detail_id) REFERENCES score_details(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.2.10 班级汇总记录 (ClassRecord)

```sql
-- 班级维度的汇总记录 (所有检查最终汇总到班级)
CREATE TABLE class_records (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,

    -- 班级信息
    class_id BIGINT NOT NULL,
    class_name VARCHAR(100),
    class_size INT,                     -- 班级人数 (用于公平权重)
    org_unit_id BIGINT NOT NULL,
    org_unit_name VARCHAR(100),

    -- 各类别得分明细 (JSON)
    category_scores JSON,
    /*
    {
      "categories": [
        {
          "categoryId": 1,
          "categoryName": "宿舍卫生",
          "weight": 0.5,
          "rawScore": 85,
          "weightedScore": 42.5,
          "deductionCount": 3,
          "bonusCount": 1
        },
        {
          "categoryId": 2,
          "categoryName": "纪律检查",
          "weight": 0.5,
          "rawScore": 90,
          "weightedScore": 45,
          "deductionCount": 1,
          "bonusCount": 0
        }
      ],
      "totalWeightedScore": 87.5
    }
    */

    -- 汇总分数
    total_raw_deduction DECIMAL(10,2) DEFAULT 0,
    total_weighted_deduction DECIMAL(10,2) DEFAULT 0,
    total_raw_bonus DECIMAL(10,2) DEFAULT 0,
    total_weighted_bonus DECIMAL(10,2) DEFAULT 0,
    final_score DECIMAL(10,2),

    -- 公平权重调整后分数 (用于排名)
    fair_adjusted_score DECIMAL(10,2),

    -- 排名
    rank_in_org_unit INT,               -- 部门内排名
    rank_in_school INT,                 -- 全校排名

    -- 状态
    status ENUM('PENDING', 'CALCULATED', 'PUBLISHED') DEFAULT 'PENDING',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,

    INDEX idx_task (task_id),
    INDEX idx_class (class_id),
    INDEX idx_org_unit (org_unit_id),
    INDEX idx_score (final_score),
    UNIQUE INDEX uk_task_class (task_id, class_id),
    FOREIGN KEY (task_id) REFERENCES inspection_tasks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.2.11 混合宿舍分配记录 (MixedDormitoryAllocation)

```sql
-- 记录混合宿舍的扣分分配
CREATE TABLE mixed_dormitory_allocations (
    id BIGINT PRIMARY KEY,
    score_detail_id BIGINT NOT NULL,    -- 原始扣分记录

    -- 宿舍信息
    dormitory_id BIGINT NOT NULL,
    dormitory_name VARCHAR(100),

    -- 分配到的班级
    class_id BIGINT NOT NULL,
    class_name VARCHAR(100),

    -- 该班级在宿舍的人数
    student_count_in_dorm INT NOT NULL,
    -- 宿舍总人数
    total_dorm_count INT NOT NULL,

    -- 分配策略
    allocation_strategy ENUM('RATIO', 'AVERAGE', 'FULL', 'MAIN') NOT NULL,

    -- 分配比例
    allocation_ratio DECIMAL(5,4),      -- 如 0.6667 表示 66.67%

    -- 分配的分数
    original_score DECIMAL(10,2) NOT NULL,
    allocated_score DECIMAL(10,2) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_score_detail (score_detail_id),
    INDEX idx_dormitory (dormitory_id),
    INDEX idx_class (class_id),
    FOREIGN KEY (score_detail_id) REFERENCES score_details(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.2.12 检查清单响应 (ChecklistResponse)

```sql
-- 清单模式检查响应记录
CREATE TABLE checklist_responses (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    target_record_id BIGINT NOT NULL,   -- 关联的目标记录

    -- 清单项信息
    score_item_id BIGINT NOT NULL,      -- 关联的扣分项/加分项
    item_name VARCHAR(200) NOT NULL,    -- 项目名称快照
    category_id BIGINT NOT NULL,
    category_name VARCHAR(100),         -- 类别名称快照

    -- 响应
    response ENUM('YES', 'NO', 'NA') NOT NULL,
    /*
      YES: 符合/通过 (不扣分)
      NO: 不符合/不通过 (扣分)
      NA: 不适用 (跳过，不计入评分)
    */

    -- YES时可能的加分
    bonus_score DECIMAL(10,2) DEFAULT 0,
    -- NO时的扣分 (从扣分项获取)
    deduction_score DECIMAL(10,2) DEFAULT 0,
    -- 实际应用的分数
    applied_score DECIMAL(10,2) DEFAULT 0,

    -- 备注
    remark TEXT,

    -- 检查人
    inspector_id BIGINT NOT NULL,
    inspector_name VARCHAR(50),
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_task (task_id),
    INDEX idx_target (target_record_id),
    INDEX idx_item (score_item_id),
    FOREIGN KEY (task_id) REFERENCES inspection_tasks(id),
    FOREIGN KEY (target_record_id) REFERENCES target_records(id),
    FOREIGN KEY (score_item_id) REFERENCES score_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.2.13 独立加分项 (BonusItem)

```sql
-- 独立的奖励/加分项，不与扣分检查关联
CREATE TABLE bonus_items (
    id BIGINT PRIMARY KEY,

    -- 关联信息
    project_id BIGINT,                  -- 所属项目 (可为空表示全局)
    task_id BIGINT,                     -- 所属任务 (可为空)

    -- 加分对象
    target_type ENUM('STUDENT', 'CLASS', 'DORMITORY') NOT NULL,
    target_id BIGINT NOT NULL,
    target_name VARCHAR(100),

    -- 关联班级/部门
    class_id BIGINT,
    class_name VARCHAR(100),
    org_unit_id BIGINT,
    org_unit_name VARCHAR(100),

    -- 加分信息
    bonus_type VARCHAR(100) NOT NULL,   -- 加分类型 (如: 文明寝室、优秀班级)
    bonus_reason TEXT,                  -- 加分原因
    bonus_score DECIMAL(10,2) NOT NULL, -- 加分分值

    -- 加分来源
    source_type ENUM('MANUAL', 'AUTO', 'IMPORT') NOT NULL DEFAULT 'MANUAL',
    /*
      MANUAL: 手动录入
      AUTO: 系统自动 (如连续满分自动加分)
      IMPORT: 批量导入
    */

    -- 有效期
    effective_date DATE NOT NULL,       -- 生效日期
    expiry_date DATE,                   -- 失效日期 (可为空)

    -- 是否计入汇总
    include_in_summary TINYINT DEFAULT 1,

    -- 证据
    evidence_urls JSON,                 -- 证据图片URL列表

    -- 审计
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    deleted TINYINT DEFAULT 0,

    INDEX idx_project (project_id),
    INDEX idx_task (task_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_class (class_id),
    INDEX idx_effective (effective_date),
    FOREIGN KEY (project_id) REFERENCES inspection_projects(id),
    FOREIGN KEY (task_id) REFERENCES inspection_tasks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.3 汇总系统数据模型

#### 2.3.1 每日汇总 (DailySummary)

```sql
CREATE TABLE daily_summaries (
    id BIGINT PRIMARY KEY,

    -- 汇总维度
    summary_date DATE NOT NULL,
    summary_scope ENUM('PROJECT', 'TEMPLATE', 'CATEGORY', 'SCHOOL') NOT NULL,
    scope_id BIGINT,                    -- 项目ID/模板ID/类别ID (SCHOOL时为空)
    scope_name VARCHAR(200),

    -- 汇总对象类型
    target_type ENUM('CLASS', 'ORG_UNIT') NOT NULL,  -- 按班级汇总或按部门汇总
    target_id BIGINT NOT NULL,
    target_name VARCHAR(100),

    -- 关联部门 (班级汇总时)
    org_unit_id BIGINT,
    org_unit_name VARCHAR(100),

    -- 包含的检查任务
    included_task_ids JSON,             -- [task_id1, task_id2, ...]
    included_task_count INT DEFAULT 0,

    -- 各类别得分
    category_scores JSON,
    /*
    {
      "categories": [
        {"categoryId": 1, "categoryName": "宿舍卫生", "weight": 0.4, "score": 85, "weightedScore": 34},
        {"categoryId": 2, "categoryName": "纪律检查", "weight": 0.3, "score": 90, "weightedScore": 27},
        {"categoryId": 3, "categoryName": "安全检查", "weight": 0.3, "score": null, "status": "MISSING"}
      ],
      "missingStrategy": "EXCLUDE",     -- 缺失处理策略
      "effectiveWeight": 0.7,           -- 实际有效权重 (排除缺失项后)
      "normalizedScore": 87.14          -- 归一化后得分
    }
    */

    -- 汇总分数
    raw_score DECIMAL(10,2),            -- 原始得分 (加权前)
    weighted_score DECIMAL(10,2),       -- 加权得分
    fair_adjusted_score DECIMAL(10,2),  -- 公平权重调整后得分

    -- 统计信息
    deduction_count INT DEFAULT 0,
    bonus_count INT DEFAULT 0,
    total_deduction_score DECIMAL(10,2) DEFAULT 0,
    total_bonus_score DECIMAL(10,2) DEFAULT 0,

    -- 排名
    rank_in_scope INT,                  -- 范围内排名
    rank_change INT,                    -- 排名变化 (与上次比较)

    -- 公布状态
    publish_status ENUM('DRAFT', 'PENDING_REVIEW', 'PUBLISHED') DEFAULT 'DRAFT',
    reviewed_by BIGINT,
    reviewed_at DATETIME,
    published_at DATETIME,

    -- 审计
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,

    INDEX idx_date (summary_date),
    INDEX idx_scope (summary_scope, scope_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_org_unit (org_unit_id),
    INDEX idx_publish_status (publish_status),
    UNIQUE INDEX uk_date_scope_target (summary_date, summary_scope, scope_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.3.2 周期汇总 (PeriodSummary)

```sql
-- 周/月/学期汇总 (通用结构)
CREATE TABLE period_summaries (
    id BIGINT PRIMARY KEY,

    -- 汇总周期
    period_type ENUM('WEEKLY', 'MONTHLY', 'SEMESTER', 'CUSTOM') NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    period_name VARCHAR(100),           -- 如 "2026年第5周", "2026年1月", "2025-2026学年第一学期"

    -- 汇总维度 (与DailySummary相同)
    summary_scope ENUM('PROJECT', 'TEMPLATE', 'CATEGORY', 'SCHOOL') NOT NULL,
    scope_id BIGINT,
    scope_name VARCHAR(200),

    -- 汇总对象
    target_type ENUM('CLASS', 'ORG_UNIT') NOT NULL,
    target_id BIGINT NOT NULL,
    target_name VARCHAR(100),
    org_unit_id BIGINT,
    org_unit_name VARCHAR(100),

    -- 包含的每日汇总
    included_daily_summary_ids JSON,
    included_days INT DEFAULT 0,        -- 实际有数据的天数

    -- 汇总分数
    average_score DECIMAL(10,2),        -- 平均分
    total_score DECIMAL(10,2),          -- 总分
    highest_score DECIMAL(10,2),        -- 最高分
    lowest_score DECIMAL(10,2),         -- 最低分
    score_std_dev DECIMAL(10,2),        -- 标准差

    -- 公平权重调整
    fair_adjusted_average DECIMAL(10,2),

    -- 趋势分析
    trend_data JSON,
    /*
    {
      "dailyScores": [
        {"date": "2026-01-24", "score": 85},
        {"date": "2026-01-25", "score": 87},
        ...
      ],
      "trend": "UP",                    -- UP / DOWN / STABLE
      "trendPercentage": 2.5            -- 上升/下降百分比
    }
    */

    -- 统计信息
    total_deduction_count INT DEFAULT 0,
    total_bonus_count INT DEFAULT 0,
    total_inspection_count INT DEFAULT 0,

    -- 排名
    rank_in_scope INT,
    rank_change INT,                    -- 与上一周期比较

    -- 公布状态
    publish_status ENUM('DRAFT', 'PENDING_REVIEW', 'PUBLISHED') DEFAULT 'DRAFT',
    published_at DATETIME,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,

    INDEX idx_period (period_type, period_start_date, period_end_date),
    INDEX idx_scope (summary_scope, scope_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_publish_status (publish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.3.3 排名快照 (RankingSnapshot)

```sql
-- 排名快照 (用于公布和历史查询)
CREATE TABLE ranking_snapshots (
    id BIGINT PRIMARY KEY,

    -- 排名类型
    ranking_type ENUM('DAILY', 'WEEKLY', 'MONTHLY', 'SEMESTER') NOT NULL,
    ranking_date DATE NOT NULL,         -- 排名日期
    period_start DATE,                  -- 周期开始 (周/月/学期)
    period_end DATE,                    -- 周期结束

    -- 排名维度
    ranking_scope ENUM('PROJECT', 'TEMPLATE', 'SCHOOL') NOT NULL,
    scope_id BIGINT,
    scope_name VARCHAR(200),

    -- 排名对象类型
    target_type ENUM('CLASS', 'ORG_UNIT') NOT NULL,

    -- 排名数据
    ranking_data JSON NOT NULL,
    /*
    {
      "rankings": [
        {
          "rank": 1,
          "targetId": 101,
          "targetName": "计算机1班",
          "orgUnitId": 1,
          "orgUnitName": "信息工程系",
          "score": 95.5,
          "fairAdjustedScore": 94.8,
          "deductionCount": 2,
          "bonusCount": 1,
          "previousRank": 2,
          "rankChange": 1
        },
        ...
      ],
      "totalCount": 50,
      "averageScore": 85.2,
      "highestScore": 95.5,
      "lowestScore": 72.3
    }
    */

    -- 公布信息
    published_by BIGINT,
    published_at DATETIME,

    -- 备注
    remark TEXT,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_type_date (ranking_type, ranking_date),
    INDEX idx_scope (ranking_scope, scope_id),
    INDEX idx_published (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.4 申诉系统数据模型

#### 2.4.1 申诉 (Appeal)

```sql
CREATE TABLE appeals (
    id BIGINT PRIMARY KEY,
    appeal_code VARCHAR(32) NOT NULL UNIQUE,

    -- 关联信息
    score_detail_id BIGINT NOT NULL,    -- 申诉的扣分记录
    task_id BIGINT NOT NULL,
    target_record_id BIGINT NOT NULL,

    -- 申诉类型
    appeal_type ENUM('SCORE_ERROR', 'EVIDENCE_DISPUTE', 'PROCEDURE_ISSUE', 'OTHER') NOT NULL,

    -- 申诉人
    appellant_id BIGINT NOT NULL,
    appellant_name VARCHAR(50),
    appellant_type ENUM('STUDENT', 'CLASS_TEACHER', 'OTHER') NOT NULL,
    class_id BIGINT,
    class_name VARCHAR(100),

    -- 申诉内容
    original_score DECIMAL(10,2) NOT NULL,  -- 原扣分
    requested_score DECIMAL(10,2) NOT NULL, -- 申请调整为
    reason TEXT NOT NULL,                    -- 申诉理由
    evidence_urls JSON,                      -- 申诉证据

    -- 审批结果
    approved_score DECIMAL(10,2),           -- 审批后分数
    final_adjustment DECIMAL(10,2),         -- 最终调整量

    -- 状态
    status ENUM('PENDING', 'REVIEWING', 'APPROVED', 'REJECTED', 'WITHDRAWN') NOT NULL DEFAULT 'PENDING',

    -- 时间线
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME,
    resolved_at DATETIME,

    -- 审计
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,

    INDEX idx_score_detail (score_detail_id),
    INDEX idx_task (task_id),
    INDEX idx_appellant (appellant_id),
    INDEX idx_class (class_id),
    INDEX idx_status (status),
    INDEX idx_submitted (submitted_at),
    FOREIGN KEY (score_detail_id) REFERENCES score_details(id),
    FOREIGN KEY (task_id) REFERENCES inspection_tasks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.4.2 申诉审批记录 (AppealApproval)

```sql
CREATE TABLE appeal_approvals (
    id BIGINT PRIMARY KEY,
    appeal_id BIGINT NOT NULL,

    -- 审批信息
    approval_level INT NOT NULL,        -- 审批级别 (1=初审, 2=复审...)
    action ENUM('APPROVE', 'REJECT', 'RETURN', 'FORWARD') NOT NULL,

    -- 审批人
    approver_id BIGINT NOT NULL,
    approver_name VARCHAR(50),
    approver_role VARCHAR(50),          -- 审批人角色

    -- 审批内容
    approved_score DECIMAL(10,2),       -- 批准调整后的分数
    comment TEXT,                       -- 审批意见

    -- 时间
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_appeal (appeal_id),
    INDEX idx_approver (approver_id),
    FOREIGN KEY (appeal_id) REFERENCES appeals(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.5 整改系统数据模型

#### 2.5.1 整改工单 (CorrectiveAction)

```sql
CREATE TABLE corrective_actions (
    id BIGINT PRIMARY KEY,
    action_code VARCHAR(32) NOT NULL UNIQUE,

    -- 关联信息
    score_detail_id BIGINT NOT NULL,    -- 关联的扣分记录
    task_id BIGINT NOT NULL,
    target_record_id BIGINT NOT NULL,

    -- 责任方
    responsible_type ENUM('CLASS', 'STUDENT', 'DORMITORY') NOT NULL,
    responsible_id BIGINT NOT NULL,
    responsible_name VARCHAR(100),
    class_id BIGINT,
    class_name VARCHAR(100),
    org_unit_id BIGINT,
    org_unit_name VARCHAR(100),

    -- 整改内容
    issue_description TEXT NOT NULL,    -- 问题描述
    issue_category VARCHAR(100),        -- 问题类别
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,

    -- 整改要求
    required_action TEXT NOT NULL,      -- 整改要求
    deadline DATETIME NOT NULL,         -- 整改期限
    assigned_to_id BIGINT,              -- 负责人
    assigned_to_name VARCHAR(50),

    -- 整改状态
    status ENUM('PENDING', 'IN_PROGRESS', 'SUBMITTED', 'VERIFIED', 'CLOSED', 'OVERDUE')
           NOT NULL DEFAULT 'PENDING',

    -- 复查信息
    verification_required TINYINT DEFAULT 1,  -- 是否需要复查
    verified_by BIGINT,
    verified_at DATETIME,
    verification_result ENUM('PASS', 'FAIL'),
    verification_comment TEXT,

    -- 时间线
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME,
    submitted_at DATETIME,
    closed_at DATETIME,

    INDEX idx_score_detail (score_detail_id),
    INDEX idx_task (task_id),
    INDEX idx_responsible (responsible_type, responsible_id),
    INDEX idx_class (class_id),
    INDEX idx_status (status),
    INDEX idx_deadline (deadline),
    FOREIGN KEY (score_detail_id) REFERENCES score_details(id),
    FOREIGN KEY (task_id) REFERENCES inspection_tasks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2.5.2 整改记录 (CorrectiveRecord)

```sql
CREATE TABLE corrective_records (
    id BIGINT PRIMARY KEY,
    corrective_action_id BIGINT NOT NULL,

    -- 记录类型
    record_type ENUM('PROGRESS', 'SUBMISSION', 'VERIFICATION', 'COMMENT') NOT NULL,

    -- 记录内容
    content TEXT NOT NULL,
    evidence_urls JSON,                 -- 整改证据

    -- 记录人
    recorder_id BIGINT NOT NULL,
    recorder_name VARCHAR(50),

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_action (corrective_action_id),
    INDEX idx_type (record_type),
    FOREIGN KEY (corrective_action_id) REFERENCES corrective_actions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 三、权重计算引擎

### 3.1 权重体系总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              权重计算体系                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. 类别占比权重 (Category Weight)                                          │
│     ├── 不同检查类别的占比不同                                               │
│     ├── 如: 卫生40% + 纪律30% + 安全30%                                     │
│     └── 在项目级别配置                                                       │
│                                                                             │
│  2. 扣分项权重 (Item Weight)                                                │
│     ├── 不同扣分项的严重程度不同                                             │
│     ├── 如: 严重违纪×2.0, 轻微违纪×1.0                                      │
│     └── 在扣分项级别配置                                                     │
│                                                                             │
│  3. 公平权重 (Fair Weight) ★重点★                                           │
│     ├── 解决班级人数不同导致的不公平问题                                      │
│     ├── 方式A: 扣分÷班级人数                                                │
│     ├── 方式B: 基准人数法 (扣分×基准人数/实际人数)                           │
│     └── 在项目级别配置，扣分项级别可覆盖                                     │
│                                                                             │
│  4. 混合宿舍分配权重 (Mixed Dormitory Weight)                               │
│     ├── 解决混合宿舍扣分如何分配到各班级                                      │
│     ├── 策略A: 按人数比例分配                                               │
│     ├── 策略B: 平均分配                                                     │
│     ├── 策略C: 全额分配给每个班级                                           │
│     ├── 策略D: 只分配给主责班级                                             │
│     └── 在项目级别配置                                                       │
│                                                                             │
│  5. 缺失项处理 (Missing Category)                                           │
│     ├── 某类别当天没有检查时如何处理                                         │
│     ├── 策略A: 不计入汇总 (重新计算权重)                                     │
│     ├── 策略B: 用满分填充                                                   │
│     └── 在项目级别配置                                                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 计算流程

```
                                原始扣分
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │  Step 1: 扣分项权重计算       │
                    │  weighted = raw × itemWeight │
                    └─────────────────────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │  Step 2: 公平权重计算         │
                    │  (如果启用)                   │
                    │  DIVIDE: score / classSize   │
                    │  BENCHMARK: score × (base/size) │
                    └─────────────────────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │  Step 3: 混合宿舍分配         │
                    │  (如果是混合宿舍)            │
                    │  按配置的策略分配到各班级     │
                    └─────────────────────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │  Step 4: 班级得分计算         │
                    │  score = base - deductions   │
                    │          + bonuses           │
                    └─────────────────────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │  Step 5: 类别加权汇总         │
                    │  total = Σ(score × weight)  │
                    └─────────────────────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │  Step 6: 缺失项处理          │
                    │  归一化或用满分填充           │
                    └─────────────────────────────┘
                                   │
                                   ▼
                              最终得分
```

### 3.3 计算引擎实现

```java
/**
 * 权重计算引擎
 */
@Service
public class WeightCalculationEngine {

    /**
     * 计算单个扣分的加权分数
     */
    public WeightedScore calculateDeductionScore(
            ScoreDetail detail,
            ScoreItem item,
            ProjectWeightConfig projectConfig,
            ClassInfo classInfo
    ) {
        BigDecimal rawScore = detail.getRawScore();
        WeightedScore result = new WeightedScore();
        result.setRawScore(rawScore);

        // Step 1: 应用扣分项权重
        BigDecimal itemWeight = getItemWeight(item);
        BigDecimal afterItemWeight = rawScore.multiply(itemWeight);
        result.setAfterItemWeight(afterItemWeight);

        // Step 2: 应用公平权重
        BigDecimal afterFairWeight = afterItemWeight;
        if (shouldApplyFairWeight(item, projectConfig)) {
            afterFairWeight = applyFairWeight(
                afterItemWeight,
                projectConfig.getFairWeight(),
                classInfo.getClassSize()
            );
        }
        result.setAfterFairWeight(afterFairWeight);
        result.setFinalScore(afterFairWeight);

        // 记录计算过程
        result.setCalculationDetail(buildCalculationDetail(
            rawScore, itemWeight, afterItemWeight, afterFairWeight,
            projectConfig, classInfo
        ));

        return result;
    }

    /**
     * 应用公平权重
     */
    private BigDecimal applyFairWeight(
            BigDecimal score,
            FairWeightConfig config,
            int classSize
    ) {
        if (!config.isEnabled() || classSize <= 0) {
            return score;
        }

        switch (config.getMode()) {
            case DIVIDE:
                // 方式A: 扣分 ÷ 班级人数
                return score.divide(
                    BigDecimal.valueOf(classSize),
                    4, RoundingMode.HALF_UP
                );

            case BENCHMARK:
                // 方式B: 扣分 × (基准人数 / 实际人数)
                int benchmark = config.getBenchmarkCount();
                return score.multiply(BigDecimal.valueOf(benchmark))
                    .divide(BigDecimal.valueOf(classSize), 4, RoundingMode.HALF_UP);

            default:
                return score;
        }
    }

    /**
     * 混合宿舍扣分分配
     */
    public List<MixedDormitoryAllocation> allocateMixedDormitoryScore(
            ScoreDetail detail,
            List<DormitoryClassInfo> classesInDorm,
            MixedDormitoryConfig config
    ) {
        BigDecimal originalScore = detail.getWeightedScore();
        int totalCount = classesInDorm.stream()
            .mapToInt(DormitoryClassInfo::getStudentCount)
            .sum();

        List<MixedDormitoryAllocation> allocations = new ArrayList<>();

        switch (config.getStrategy()) {
            case RATIO:
                // 按人数比例分配
                for (DormitoryClassInfo classInfo : classesInDorm) {
                    BigDecimal ratio = BigDecimal.valueOf(classInfo.getStudentCount())
                        .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP);
                    BigDecimal allocatedScore = originalScore.multiply(ratio);

                    allocations.add(MixedDormitoryAllocation.builder()
                        .scoreDetailId(detail.getId())
                        .dormitoryId(detail.getSpaceId())
                        .classId(classInfo.getClassId())
                        .className(classInfo.getClassName())
                        .studentCountInDorm(classInfo.getStudentCount())
                        .totalDormCount(totalCount)
                        .allocationStrategy(AllocationStrategy.RATIO)
                        .allocationRatio(ratio)
                        .originalScore(originalScore)
                        .allocatedScore(allocatedScore)
                        .build());
                }
                break;

            case AVERAGE:
                // 平均分配
                int classCount = classesInDorm.size();
                BigDecimal avgScore = originalScore.divide(
                    BigDecimal.valueOf(classCount), 4, RoundingMode.HALF_UP);

                for (DormitoryClassInfo classInfo : classesInDorm) {
                    allocations.add(MixedDormitoryAllocation.builder()
                        .scoreDetailId(detail.getId())
                        .dormitoryId(detail.getSpaceId())
                        .classId(classInfo.getClassId())
                        .className(classInfo.getClassName())
                        .studentCountInDorm(classInfo.getStudentCount())
                        .totalDormCount(totalCount)
                        .allocationStrategy(AllocationStrategy.AVERAGE)
                        .allocationRatio(BigDecimal.valueOf(1.0 / classCount))
                        .originalScore(originalScore)
                        .allocatedScore(avgScore)
                        .build());
                }
                break;

            case FULL:
                // 全额分配给每个班级
                for (DormitoryClassInfo classInfo : classesInDorm) {
                    allocations.add(MixedDormitoryAllocation.builder()
                        .scoreDetailId(detail.getId())
                        .dormitoryId(detail.getSpaceId())
                        .classId(classInfo.getClassId())
                        .className(classInfo.getClassName())
                        .studentCountInDorm(classInfo.getStudentCount())
                        .totalDormCount(totalCount)
                        .allocationStrategy(AllocationStrategy.FULL)
                        .allocationRatio(BigDecimal.ONE)
                        .originalScore(originalScore)
                        .allocatedScore(originalScore)
                        .build());
                }
                break;

            case MAIN:
                // 只分配给人数最多的班级
                DormitoryClassInfo mainClass = classesInDorm.stream()
                    .max(Comparator.comparingInt(DormitoryClassInfo::getStudentCount))
                    .orElseThrow();

                allocations.add(MixedDormitoryAllocation.builder()
                    .scoreDetailId(detail.getId())
                    .dormitoryId(detail.getSpaceId())
                    .classId(mainClass.getClassId())
                    .className(mainClass.getClassName())
                    .studentCountInDorm(mainClass.getStudentCount())
                    .totalDormCount(totalCount)
                    .allocationStrategy(AllocationStrategy.MAIN)
                    .allocationRatio(BigDecimal.ONE)
                    .originalScore(originalScore)
                    .allocatedScore(originalScore)
                    .build());
                break;
        }

        return allocations;
    }

    /**
     * 计算班级在单次检查任务中的得分
     */
    public ClassTaskScore calculateClassTaskScore(
            Long classId,
            InspectionTask task,
            List<TargetRecord> records,
            ProjectWeightConfig config
    ) {
        // 1. 按类别分组计算得分
        Map<Long, CategoryScore> categoryScores = new HashMap<>();
        BigDecimal baseScore = BigDecimal.valueOf(config.getScoring().getBaseScore());

        for (TargetRecord record : records) {
            Long categoryId = record.getProjectCategoryId();
            ProjectCategory category = getCategoryById(categoryId);

            // 计算该类别的得分
            BigDecimal categoryRawScore = baseScore
                .subtract(record.getWeightedDeduction())
                .add(record.getWeightedBonus());

            // 限制分数范围
            categoryRawScore = categoryRawScore.max(BigDecimal.ZERO);
            if (config.getScoring().getMaxScore() != null) {
                categoryRawScore = categoryRawScore.min(
                    BigDecimal.valueOf(config.getScoring().getMaxScore()));
            }

            categoryScores.put(categoryId, CategoryScore.builder()
                .categoryId(categoryId)
                .categoryName(category.getCategoryName())
                .weight(category.getWeightPercentage())
                .rawScore(categoryRawScore)
                .weightedScore(categoryRawScore.multiply(
                    category.getWeightPercentage().divide(BigDecimal.valueOf(100))))
                .deductionCount(record.getDeductionCount())
                .bonusCount(record.getBonusCount())
                .build());
        }

        // 2. 处理缺失类别
        List<ProjectCategory> allCategories = getProjectCategories(task.getProjectId());
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalWeightedScore = BigDecimal.ZERO;

        for (ProjectCategory category : allCategories) {
            CategoryScore score = categoryScores.get(category.getId());

            if (score != null) {
                totalWeight = totalWeight.add(category.getWeightPercentage());
                totalWeightedScore = totalWeightedScore.add(score.getWeightedScore());
            } else {
                // 缺失类别处理
                if (config.getMissingCategory().getStrategy() == MissingStrategy.FULL_SCORE) {
                    // 用满分填充
                    BigDecimal fullScore = baseScore.multiply(
                        category.getWeightPercentage().divide(BigDecimal.valueOf(100)));
                    totalWeight = totalWeight.add(category.getWeightPercentage());
                    totalWeightedScore = totalWeightedScore.add(fullScore);

                    categoryScores.put(category.getId(), CategoryScore.builder()
                        .categoryId(category.getId())
                        .categoryName(category.getCategoryName())
                        .weight(category.getWeightPercentage())
                        .rawScore(baseScore)
                        .weightedScore(fullScore)
                        .status(CategoryScoreStatus.FULL_SCORE_FILLED)
                        .build());
                }
                // EXCLUDE 策略: 不计入，权重不累加
            }
        }

        // 3. 归一化得分 (如果有缺失项且使用EXCLUDE策略)
        BigDecimal finalScore;
        if (totalWeight.compareTo(BigDecimal.valueOf(100)) < 0 && totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            // 归一化: 按有效权重比例计算
            finalScore = totalWeightedScore
                .multiply(BigDecimal.valueOf(100))
                .divide(totalWeight, 2, RoundingMode.HALF_UP);
        } else {
            finalScore = totalWeightedScore;
        }

        return ClassTaskScore.builder()
            .classId(classId)
            .taskId(task.getId())
            .categoryScores(categoryScores)
            .totalWeight(totalWeight)
            .totalWeightedScore(totalWeightedScore)
            .finalScore(finalScore)
            .build();
    }
}
```

### 3.4 打分模式转换

```java
/**
 * 打分模式转换器
 * 将不同打分模式统一转换为标准分 (0-100)
 */
@Service
public class ScoringModeConverter {

    /**
     * 转换为标准分
     */
    public BigDecimal convertToStandardScore(
            TargetRecord record,
            ScoringConfig config
    ) {
        switch (config.getMode()) {
            case PERCENTAGE:
                // 百分制: 直接使用
                return record.getFinalScore();

            case DEDUCTION_ONLY:
                // 纯扣分制: 100 - 扣分
                // 考虑加分: 100 - 扣分 + 加分
                return BigDecimal.valueOf(100)
                    .subtract(record.getWeightedDeduction())
                    .add(record.getWeightedBonus())
                    .max(BigDecimal.ZERO)
                    .min(BigDecimal.valueOf(config.getMaxScore()));

            case GRADE:
                // 等级制: 根据映射转换
                return convertGradeToScore(record.getGrade(), config.getGradeMapping());

            case PASS_FAIL:
                // 达标制: 通过=配置分数(默认100), 不通过=0
                return record.getPassStatus() == 1
                    ? BigDecimal.valueOf(config.getPassScore())
                    : BigDecimal.ZERO;

            default:
                throw new IllegalArgumentException("Unknown scoring mode: " + config.getMode());
        }
    }

    /**
     * 等级转分数
     */
    private BigDecimal convertGradeToScore(String grade, Map<String, Integer> mapping) {
        Integer score = mapping.get(grade);
        if (score == null) {
            throw new IllegalArgumentException("Unknown grade: " + grade);
        }
        return BigDecimal.valueOf(score);
    }

    /**
     * 分数转等级
     */
    public String convertScoreToGrade(BigDecimal score, Map<String, Integer> mapping) {
        // 按分数从高到低找到对应等级
        return mapping.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .filter(e -> score.compareTo(BigDecimal.valueOf(e.getValue())) >= 0)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse("D");  // 默认最低等级
    }
}
```

### 3.5 权重配置示例

```json
{
  "projectId": 1,
  "projectName": "2024学年日常检查",

  "scoringConfig": {
    "mode": "PERCENTAGE",
    "baseScore": 100,
    "minScore": 0,
    "maxScore": 110,
    "allowBonus": true,
    "maxBonus": 10,
    "gradeMapping": {
      "A": 95, "B": 85, "C": 75, "D": 60, "E": 0
    }
  },

  "weightConfig": {
    "fairWeight": {
      "enabled": true,
      "mode": "BENCHMARK",
      "benchmarkCount": 40
    },
    "mixedDormitory": {
      "strategy": "RATIO"
    },
    "missingCategory": {
      "strategy": "EXCLUDE"
    }
  },

  "categoryWeights": [
    {
      "categoryId": 1,
      "categoryName": "宿舍卫生",
      "weight": 40.00,
      "itemWeights": [
        {"itemId": 101, "itemName": "地面脏乱", "weight": 1.0},
        {"itemId": 102, "itemName": "被子未叠", "weight": 1.0},
        {"itemId": 103, "itemName": "违规电器", "weight": 2.0, "fairWeightOverride": {"enabled": false}}
      ]
    },
    {
      "categoryId": 2,
      "categoryName": "纪律检查",
      "weight": 35.00,
      "itemWeights": [
        {"itemId": 201, "itemName": "迟到", "weight": 1.0, "fairWeightOverride": {"enabled": true, "mode": "DIVIDE"}},
        {"itemId": 202, "itemName": "旷课", "weight": 2.0, "fairWeightOverride": {"enabled": true, "mode": "DIVIDE"}},
        {"itemId": 203, "itemName": "严重违纪", "weight": 3.0}
      ]
    },
    {
      "categoryId": 3,
      "categoryName": "安全检查",
      "weight": 25.00
    }
  ]
}
```

---

## 四、汇总系统设计

### 4.1 汇总流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              每日汇总流程                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  触发时机:                                                                  │
│  ├── 定时任务: 每天 22:00 自动触发                                          │
│  ├── 手动触发: 管理员点击"生成汇总"                                         │
│  └── 任务发布时: 某项目的所有任务发布后自动触发                              │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 1: 收集当天数据                                                │   │
│  │  ├── 查询当天所有已发布的检查任务                                     │   │
│  │  ├── 按项目分组                                                      │   │
│  │  └── 获取各任务的班级记录                                            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 2: 按项目汇总                                                  │   │
│  │  ├── 合并同一项目下的所有任务数据                                     │   │
│  │  ├── 按类别计算加权得分                                              │   │
│  │  ├── 处理缺失类别                                                    │   │
│  │  └── 计算最终得分                                                    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 3: 计算排名                                                    │   │
│  │  ├── 按项目内排名                                                    │   │
│  │  ├── 按部门内排名                                                    │   │
│  │  ├── 全校排名                                                        │   │
│  │  └── 计算排名变化                                                    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 4: 生成汇总记录                                                │   │
│  │  ├── 创建 DailySummary 记录                                          │   │
│  │  ├── 状态: DRAFT                                                     │   │
│  │  └── 等待审核或自动发布                                              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 5: 审核与发布                                                  │   │
│  │  ├── 手动模式: 等待管理员审核后发布                                   │   │
│  │  └── 自动模式: 到达配置时间后自动发布                                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 6: 生成排名快照                                                │   │
│  │  ├── 创建 RankingSnapshot                                            │   │
│  │  └── 用于历史查询和公示                                              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 汇总服务实现

```java
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final InspectionTaskRepository taskRepository;
    private final ClassRecordRepository classRecordRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final PeriodSummaryRepository periodSummaryRepository;
    private final RankingSnapshotRepository rankingSnapshotRepository;
    private final WeightCalculationEngine weightEngine;

    /**
     * 生成每日汇总
     */
    @Transactional
    public List<DailySummary> generateDailySummary(LocalDate date, SummaryScope scope, Long scopeId) {
        List<DailySummary> summaries = new ArrayList<>();

        // 1. 查询当天已发布的任务
        List<InspectionTask> tasks = taskRepository.findByDateAndStatus(
            date, TaskStatus.PUBLISHED);

        if (scope == SummaryScope.PROJECT && scopeId != null) {
            tasks = tasks.stream()
                .filter(t -> t.getProjectId().equals(scopeId))
                .collect(Collectors.toList());
        }

        // 2. 按项目分组
        Map<Long, List<InspectionTask>> tasksByProject = tasks.stream()
            .collect(Collectors.groupingBy(InspectionTask::getProjectId));

        // 3. 为每个项目生成汇总
        for (Map.Entry<Long, List<InspectionTask>> entry : tasksByProject.entrySet()) {
            Long projectId = entry.getKey();
            List<InspectionTask> projectTasks = entry.getValue();

            // 获取项目配置
            InspectionProject project = projectRepository.findById(projectId).orElseThrow();
            ProjectWeightConfig weightConfig = parseWeightConfig(project.getWeightConfig());

            // 汇总每个班级的数据
            Map<Long, ClassDailySummaryData> classSummaries = new HashMap<>();

            for (InspectionTask task : projectTasks) {
                List<ClassRecord> records = classRecordRepository.findByTaskId(task.getId());
                for (ClassRecord record : records) {
                    classSummaries.compute(record.getClassId(), (classId, existing) -> {
                        if (existing == null) {
                            existing = new ClassDailySummaryData(record);
                        } else {
                            existing.merge(record);
                        }
                        return existing;
                    });
                }
            }

            // 4. 计算最终得分和排名
            List<ClassDailySummaryData> sortedSummaries = new ArrayList<>(classSummaries.values());

            // 应用公平权重
            for (ClassDailySummaryData data : sortedSummaries) {
                data.setFairAdjustedScore(
                    weightEngine.applyFairWeightToFinalScore(
                        data.getWeightedScore(),
                        weightConfig.getFairWeight(),
                        data.getClassSize()
                    )
                );
            }

            // 排序计算排名
            sortedSummaries.sort((a, b) ->
                b.getFairAdjustedScore().compareTo(a.getFairAdjustedScore()));

            int rank = 1;
            for (ClassDailySummaryData data : sortedSummaries) {
                data.setRank(rank++);

                // 计算排名变化
                DailySummary yesterday = dailySummaryRepository.findByDateAndScopeAndTarget(
                    date.minusDays(1), SummaryScope.PROJECT, projectId,
                    TargetType.CLASS, data.getClassId()
                );
                if (yesterday != null) {
                    data.setRankChange(yesterday.getRankInScope() - data.getRank());
                }
            }

            // 5. 创建汇总记录
            for (ClassDailySummaryData data : sortedSummaries) {
                DailySummary summary = DailySummary.builder()
                    .summaryDate(date)
                    .summaryScope(SummaryScope.PROJECT)
                    .scopeId(projectId)
                    .scopeName(project.getProjectName())
                    .targetType(TargetType.CLASS)
                    .targetId(data.getClassId())
                    .targetName(data.getClassName())
                    .orgUnitId(data.getOrgUnitId())
                    .orgUnitName(data.getOrgUnitName())
                    .includedTaskIds(projectTasks.stream().map(InspectionTask::getId).collect(Collectors.toList()))
                    .includedTaskCount(projectTasks.size())
                    .categoryScores(data.getCategoryScoresJson())
                    .rawScore(data.getRawScore())
                    .weightedScore(data.getWeightedScore())
                    .fairAdjustedScore(data.getFairAdjustedScore())
                    .deductionCount(data.getDeductionCount())
                    .bonusCount(data.getBonusCount())
                    .totalDeductionScore(data.getTotalDeductionScore())
                    .totalBonusScore(data.getTotalBonusScore())
                    .rankInScope(data.getRank())
                    .rankChange(data.getRankChange())
                    .publishStatus(PublishStatus.DRAFT)
                    .build();

                summaries.add(dailySummaryRepository.save(summary));
            }
        }

        return summaries;
    }

    /**
     * 生成周汇总
     */
    @Transactional
    public List<PeriodSummary> generateWeeklySummary(LocalDate weekEndDate, SummaryScope scope, Long scopeId) {
        LocalDate weekStartDate = weekEndDate.minusDays(6);
        String periodName = String.format("%d年第%d周",
            weekEndDate.getYear(), weekEndDate.get(WeekFields.ISO.weekOfYear()));

        return generatePeriodSummary(
            PeriodType.WEEKLY, periodName, weekStartDate, weekEndDate, scope, scopeId);
    }

    /**
     * 生成月汇总
     */
    @Transactional
    public List<PeriodSummary> generateMonthlySummary(YearMonth month, SummaryScope scope, Long scopeId) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        String periodName = String.format("%d年%d月", month.getYear(), month.getMonthValue());

        return generatePeriodSummary(
            PeriodType.MONTHLY, periodName, monthStart, monthEnd, scope, scopeId);
    }

    /**
     * 生成学期汇总
     */
    @Transactional
    public List<PeriodSummary> generateSemesterSummary(
            String semesterName, LocalDate startDate, LocalDate endDate,
            SummaryScope scope, Long scopeId) {
        return generatePeriodSummary(
            PeriodType.SEMESTER, semesterName, startDate, endDate, scope, scopeId);
    }

    /**
     * 通用周期汇总
     */
    private List<PeriodSummary> generatePeriodSummary(
            PeriodType periodType, String periodName,
            LocalDate startDate, LocalDate endDate,
            SummaryScope scope, Long scopeId) {

        List<PeriodSummary> summaries = new ArrayList<>();

        // 1. 查询周期内的每日汇总
        List<DailySummary> dailySummaries = dailySummaryRepository.findByDateRangeAndScope(
            startDate, endDate, scope, scopeId, PublishStatus.PUBLISHED);

        // 2. 按目标分组
        Map<Long, List<DailySummary>> summariesByTarget = dailySummaries.stream()
            .collect(Collectors.groupingBy(DailySummary::getTargetId));

        // 3. 计算每个目标的周期汇总
        for (Map.Entry<Long, List<DailySummary>> entry : summariesByTarget.entrySet()) {
            Long targetId = entry.getKey();
            List<DailySummary> targetDailies = entry.getValue();

            // 统计数据
            DoubleSummaryStatistics stats = targetDailies.stream()
                .mapToDouble(d -> d.getFairAdjustedScore().doubleValue())
                .summaryStatistics();

            // 趋势数据
            List<Map<String, Object>> dailyScores = targetDailies.stream()
                .sorted(Comparator.comparing(DailySummary::getSummaryDate))
                .map(d -> Map.<String, Object>of(
                    "date", d.getSummaryDate().toString(),
                    "score", d.getFairAdjustedScore()
                ))
                .collect(Collectors.toList());

            // 计算趋势
            String trend = calculateTrend(targetDailies);
            double trendPercentage = calculateTrendPercentage(targetDailies);

            DailySummary firstDaily = targetDailies.get(0);

            PeriodSummary periodSummary = PeriodSummary.builder()
                .periodType(periodType)
                .periodStartDate(startDate)
                .periodEndDate(endDate)
                .periodName(periodName)
                .summaryScope(scope)
                .scopeId(scopeId)
                .scopeName(firstDaily.getScopeName())
                .targetType(firstDaily.getTargetType())
                .targetId(targetId)
                .targetName(firstDaily.getTargetName())
                .orgUnitId(firstDaily.getOrgUnitId())
                .orgUnitName(firstDaily.getOrgUnitName())
                .includedDailySummaryIds(targetDailies.stream().map(DailySummary::getId).collect(Collectors.toList()))
                .includedDays(targetDailies.size())
                .averageScore(BigDecimal.valueOf(stats.getAverage()))
                .highestScore(BigDecimal.valueOf(stats.getMax()))
                .lowestScore(BigDecimal.valueOf(stats.getMin()))
                .fairAdjustedAverage(BigDecimal.valueOf(stats.getAverage()))
                .trendData(buildTrendData(dailyScores, trend, trendPercentage))
                .totalDeductionCount(targetDailies.stream().mapToInt(DailySummary::getDeductionCount).sum())
                .totalBonusCount(targetDailies.stream().mapToInt(DailySummary::getBonusCount).sum())
                .totalInspectionCount(targetDailies.stream().mapToInt(DailySummary::getIncludedTaskCount).sum())
                .publishStatus(PublishStatus.DRAFT)
                .build();

            summaries.add(periodSummaryRepository.save(periodSummary));
        }

        // 4. 计算排名
        summaries.sort((a, b) -> b.getFairAdjustedAverage().compareTo(a.getFairAdjustedAverage()));
        int rank = 1;
        for (PeriodSummary summary : summaries) {
            summary.setRankInScope(rank++);
            // 与上一周期比较
            PeriodSummary previous = findPreviousPeriodSummary(summary, periodType);
            if (previous != null) {
                summary.setRankChange(previous.getRankInScope() - summary.getRankInScope());
            }
        }

        return summaries;
    }

    /**
     * 发布汇总并生成排名快照
     */
    @Transactional
    public RankingSnapshot publishDailySummary(LocalDate date, SummaryScope scope, Long scopeId, Long publisherId) {
        // 1. 更新汇总状态
        List<DailySummary> summaries = dailySummaryRepository.findByDateAndScope(date, scope, scopeId);
        for (DailySummary summary : summaries) {
            summary.setPublishStatus(PublishStatus.PUBLISHED);
            summary.setPublishedAt(LocalDateTime.now());
        }
        dailySummaryRepository.saveAll(summaries);

        // 2. 生成排名快照
        RankingSnapshot snapshot = RankingSnapshot.builder()
            .rankingType(RankingType.DAILY)
            .rankingDate(date)
            .rankingScope(scope)
            .scopeId(scopeId)
            .scopeName(summaries.isEmpty() ? "" : summaries.get(0).getScopeName())
            .targetType(TargetType.CLASS)
            .rankingData(buildRankingData(summaries))
            .publishedBy(publisherId)
            .publishedAt(LocalDateTime.now())
            .build();

        return rankingSnapshotRepository.save(snapshot);
    }
}
```

### 4.3 定时任务配置

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SummaryScheduler {

    private final SummaryService summaryService;
    private final InspectionProjectRepository projectRepository;

    /**
     * 每日汇总任务 - 每天22:00执行
     */
    @Scheduled(cron = "0 0 22 * * ?")
    public void dailySummaryTask() {
        log.info("开始执行每日汇总任务");
        LocalDate today = LocalDate.now();

        // 获取所有活跃项目
        List<InspectionProject> activeProjects = projectRepository.findByStatus(ProjectStatus.ACTIVE);

        for (InspectionProject project : activeProjects) {
            try {
                // 检查项目配置是否需要汇总
                SummaryConfig config = parseSummaryConfig(project.getSummaryConfig());
                if (!config.getPeriods().contains(PeriodType.DAILY)) {
                    continue;
                }

                // 生成汇总
                List<DailySummary> summaries = summaryService.generateDailySummary(
                    today, SummaryScope.PROJECT, project.getId());

                // 自动发布模式
                if (config.getPublishMode() == PublishMode.AUTO) {
                    summaryService.publishDailySummary(
                        today, SummaryScope.PROJECT, project.getId(), null);
                    log.info("项目 {} 今日汇总已自动发布，共 {} 条记录",
                        project.getProjectName(), summaries.size());
                } else {
                    log.info("项目 {} 今日汇总已生成，待审核，共 {} 条记录",
                        project.getProjectName(), summaries.size());
                }
            } catch (Exception e) {
                log.error("项目 {} 每日汇总失败: {}", project.getProjectName(), e.getMessage(), e);
            }
        }
    }

    /**
     * 每周汇总任务 - 每周日23:00执行
     */
    @Scheduled(cron = "0 0 23 ? * SUN")
    public void weeklySummaryTask() {
        log.info("开始执行每周汇总任务");
        LocalDate today = LocalDate.now();

        List<InspectionProject> activeProjects = projectRepository.findByStatus(ProjectStatus.ACTIVE);

        for (InspectionProject project : activeProjects) {
            try {
                SummaryConfig config = parseSummaryConfig(project.getSummaryConfig());
                if (!config.getPeriods().contains(PeriodType.WEEKLY)) {
                    continue;
                }

                List<PeriodSummary> summaries = summaryService.generateWeeklySummary(
                    today, SummaryScope.PROJECT, project.getId());

                log.info("项目 {} 周汇总已生成，共 {} 条记录",
                    project.getProjectName(), summaries.size());
            } catch (Exception e) {
                log.error("项目 {} 周汇总失败: {}", project.getProjectName(), e.getMessage(), e);
            }
        }
    }

    /**
     * 每月汇总任务 - 每月1日00:30执行
     */
    @Scheduled(cron = "0 30 0 1 * ?")
    public void monthlySummaryTask() {
        log.info("开始执行每月汇总任务");
        YearMonth lastMonth = YearMonth.now().minusMonths(1);

        List<InspectionProject> activeProjects = projectRepository.findByStatus(ProjectStatus.ACTIVE);

        for (InspectionProject project : activeProjects) {
            try {
                SummaryConfig config = parseSummaryConfig(project.getSummaryConfig());
                if (!config.getPeriods().contains(PeriodType.MONTHLY)) {
                    continue;
                }

                List<PeriodSummary> summaries = summaryService.generateMonthlySummary(
                    lastMonth, SummaryScope.PROJECT, project.getId());

                log.info("项目 {} 月汇总已生成，共 {} 条记录",
                    project.getProjectName(), summaries.size());
            } catch (Exception e) {
                log.error("项目 {} 月汇总失败: {}", project.getProjectName(), e.getMessage(), e);
            }
        }
    }
}
```

### 4.4 Saga异步编排

当检查任务发布时，需要触发一系列异步操作，使用Saga模式编排：

```java
/**
 * 检查完成Saga - 编排任务发布后的异步工作流
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InspectionCompletionSaga {

    private final ClassInspectionRecordRepository recordRepository;
    private final RatingCalculationService ratingService;
    private final NotificationService notificationService;
    private final SummaryService summaryService;
    private final BonusItemService bonusItemService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 处理任务发布事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskPublished(TaskPublishedEvent event) {
        log.info("开始执行检查完成Saga, taskId={}", event.getTaskId());

        SagaContext context = new SagaContext(event.getTaskId());

        try {
            // Step 1: 加载所有班级检查记录
            executeStep1LoadRecords(context);

            // Step 2: 计算班级评级
            executeStep2CalculateRatings(context);

            // Step 3: 检查是否触发自动加分
            executeStep3CheckAutoBonus(context);

            // Step 4: 生成每日汇总 (如果配置了自动汇总)
            executeStep4GenerateSummary(context);

            // Step 5: 发送通知
            executeStep5SendNotifications(context);

            log.info("检查完成Saga执行成功, taskId={}", event.getTaskId());

        } catch (Exception e) {
            log.error("检查完成Saga执行失败, taskId={}, error={}",
                event.getTaskId(), e.getMessage(), e);
            // 记录失败，便于后续重试
            recordSagaFailure(context, e);
        }
    }

    /**
     * Step 1: 加载所有班级检查记录
     */
    private void executeStep1LoadRecords(SagaContext context) {
        log.debug("Saga Step 1: 加载班级检查记录");
        List<ClassRecord> records = recordRepository.findByTaskId(context.getTaskId());
        context.setClassRecords(records);
        log.debug("加载到 {} 条班级记录", records.size());
    }

    /**
     * Step 2: 计算班级评级
     */
    private void executeStep2CalculateRatings(SagaContext context) {
        log.debug("Saga Step 2: 计算班级评级");

        for (ClassRecord record : context.getClassRecords()) {
            // 根据最终得分计算评级
            String rating = ratingService.calculateRating(record.getFinalScore());
            record.setRating(rating);

            // 判断是否需要整改
            if (ratingService.requiresCorrective(rating)) {
                record.setRequiresCorrective(true);
            }
        }

        // 批量更新评级
        recordRepository.batchUpdateRatings(context.getClassRecords());
    }

    /**
     * Step 3: 检查自动加分规则
     */
    private void executeStep3CheckAutoBonus(SagaContext context) {
        log.debug("Saga Step 3: 检查自动加分规则");

        for (ClassRecord record : context.getClassRecords()) {
            // 检查是否满足自动加分条件 (如连续满分)
            List<AutoBonusRule> matchedRules = bonusItemService.checkAutoBonusRules(
                record.getClassId(),
                context.getTaskId()
            );

            for (AutoBonusRule rule : matchedRules) {
                BonusItem bonus = BonusItem.builder()
                    .taskId(context.getTaskId())
                    .targetType(TargetType.CLASS)
                    .targetId(record.getClassId())
                    .targetName(record.getClassName())
                    .classId(record.getClassId())
                    .className(record.getClassName())
                    .bonusType(rule.getBonusType())
                    .bonusReason(rule.getDescription())
                    .bonusScore(rule.getBonusScore())
                    .sourceType(BonusSourceType.AUTO)
                    .effectiveDate(LocalDate.now())
                    .build();

                bonusItemService.create(bonus);
                log.info("自动加分: class={}, type={}, score={}",
                    record.getClassName(), rule.getBonusType(), rule.getBonusScore());
            }
        }
    }

    /**
     * Step 4: 生成每日汇总
     */
    private void executeStep4GenerateSummary(SagaContext context) {
        log.debug("Saga Step 4: 检查是否需要生成汇总");

        InspectionTask task = taskRepository.findById(context.getTaskId()).orElseThrow();
        InspectionProject project = projectRepository.findById(task.getProjectId()).orElseThrow();

        SummaryConfig config = parseSummaryConfig(project.getSummaryConfig());

        // 检查是否配置了自动汇总
        if (config.getPublishMode() == PublishMode.AUTO
            && config.getPeriods().contains(PeriodType.DAILY)) {

            summaryService.generateDailySummary(
                task.getInspectionDate(),
                SummaryScope.PROJECT,
                project.getId()
            );

            log.info("已自动生成每日汇总, date={}, projectId={}",
                task.getInspectionDate(), project.getId());
        }
    }

    /**
     * Step 5: 发送通知
     */
    private void executeStep5SendNotifications(SagaContext context) {
        log.debug("Saga Step 5: 发送通知");

        for (ClassRecord record : context.getClassRecords()) {
            // 查找班主任
            User classTeacher = userService.getClassTeacher(record.getClassId());
            if (classTeacher != null) {
                NotificationRequest notification = NotificationRequest.builder()
                    .recipientId(classTeacher.getId())
                    .type(NotificationType.INSPECTION_RESULT)
                    .title("检查结果通知")
                    .content(String.format(
                        "班级 %s 今日检查得分: %.2f, 评级: %s",
                        record.getClassName(),
                        record.getFinalScore(),
                        record.getRating()
                    ))
                    .relatedType("INSPECTION_TASK")
                    .relatedId(context.getTaskId())
                    .build();

                notificationService.send(notification);
            }

            // 如果需要整改，发送整改通知
            if (record.isRequiresCorrective()) {
                eventPublisher.publishEvent(new CorrectiveRequiredEvent(
                    record.getClassId(),
                    context.getTaskId(),
                    record.getRating()
                ));
            }
        }
    }

    /**
     * Saga执行上下文
     */
    @Data
    private static class SagaContext {
        private final Long taskId;
        private List<ClassRecord> classRecords;
        private int currentStep = 0;
        private Map<String, Object> data = new HashMap<>();
    }
}
```

#### 4.4.1 领域事件定义

```java
/**
 * 任务发布事件
 */
@Getter
public class TaskPublishedEvent extends ApplicationEvent {
    private final Long taskId;
    private final Long projectId;
    private final LocalDate inspectionDate;
    private final String timeSlot;
    private final Long publisherId;

    public TaskPublishedEvent(Object source, Long taskId, Long projectId,
                              LocalDate inspectionDate, String timeSlot, Long publisherId) {
        super(source);
        this.taskId = taskId;
        this.projectId = projectId;
        this.inspectionDate = inspectionDate;
        this.timeSlot = timeSlot;
        this.publisherId = publisherId;
    }
}

/**
 * 需要整改事件
 */
@Getter
public class CorrectiveRequiredEvent extends ApplicationEvent {
    private final Long classId;
    private final Long taskId;
    private final String rating;

    public CorrectiveRequiredEvent(Object source, Long classId, Long taskId, String rating) {
        super(source);
        this.classId = classId;
        this.taskId = taskId;
        this.rating = rating;
    }
}

/**
 * 申诉状态变化事件
 */
@Getter
public class AppealStatusChangedEvent extends ApplicationEvent {
    private final Long appealId;
    private final AppealStatus oldStatus;
    private final AppealStatus newStatus;
    private final Long operatorId;

    public AppealStatusChangedEvent(Object source, Long appealId,
                                     AppealStatus oldStatus, AppealStatus newStatus,
                                     Long operatorId) {
        super(source);
        this.appealId = appealId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.operatorId = operatorId;
    }
}
```

#### 4.4.2 评级计算服务

```java
@Service
@RequiredArgsConstructor
public class RatingCalculationService {

    private final InspectionConfigService configService;

    /**
     * 根据得分计算评级
     */
    public String calculateRating(BigDecimal score) {
        List<RatingRule> rules = configService.getActiveRatingRules();

        for (RatingRule rule : rules) {
            for (RatingLevel level : rule.getLevels()) {
                if (score.compareTo(BigDecimal.valueOf(level.getMin())) >= 0
                    && score.compareTo(BigDecimal.valueOf(level.getMax())) <= 0) {
                    return level.getName();
                }
            }
        }

        return "未评级";
    }

    /**
     * 判断评级是否需要整改
     */
    public boolean requiresCorrective(String rating) {
        // 不及格和差需要整改
        return "不及格".equals(rating) || "差".equals(rating);
    }

    /**
     * 获取评级颜色
     */
    public String getRatingColor(String rating) {
        return switch (rating) {
            case "优秀" -> "#10b981";
            case "良好" -> "#3b82f6";
            case "中等" -> "#f59e0b";
            case "及格" -> "#f97316";
            case "不及格", "差" -> "#ef4444";
            default -> "#6b7280";
        };
    }
}
```

#### 4.4.3 自动加分规则

```java
/**
 * 自动加分规则检查
 */
@Service
@RequiredArgsConstructor
public class AutoBonusRuleService {

    private final ClassRecordRepository classRecordRepository;
    private final BonusRuleRepository bonusRuleRepository;

    /**
     * 检查班级是否满足自动加分条件
     */
    public List<AutoBonusRule> checkAutoBonusRules(Long classId, Long currentTaskId) {
        List<AutoBonusRule> matchedRules = new ArrayList<>();

        // 获取所有激活的自动加分规则
        List<AutoBonusRule> activeRules = bonusRuleRepository.findByStatus(RuleStatus.ACTIVE);

        for (AutoBonusRule rule : activeRules) {
            if (checkRule(classId, currentTaskId, rule)) {
                matchedRules.add(rule);
            }
        }

        return matchedRules;
    }

    private boolean checkRule(Long classId, Long currentTaskId, AutoBonusRule rule) {
        switch (rule.getRuleType()) {
            case CONSECUTIVE_FULL_SCORE:
                // 连续满分加分
                return checkConsecutiveFullScore(classId, rule.getConsecutiveDays());

            case MONTHLY_EXCELLENT:
                // 月度优秀加分
                return checkMonthlyExcellent(classId, rule.getExcellentThreshold());

            case IMPROVEMENT:
                // 进步加分
                return checkImprovement(classId, rule.getImprovementPercentage());

            default:
                return false;
        }
    }

    /**
     * 检查是否连续满分
     */
    private boolean checkConsecutiveFullScore(Long classId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<ClassRecord> records = classRecordRepository.findByClassIdAndDateRange(
            classId, startDate, endDate);

        if (records.size() < days) {
            return false;
        }

        return records.stream()
            .allMatch(r -> r.getFinalScore().compareTo(BigDecimal.valueOf(100)) >= 0);
    }
}
```

---

## 五、API设计

### 5.1 API概览

```yaml
# ============================================================
# V5 量化检查系统 API 设计
# ============================================================

# ---------- 检查模板 ----------
POST   /api/v5/templates                      # 创建模板
GET    /api/v5/templates                      # 模板列表
GET    /api/v5/templates/{id}                 # 模板详情
PUT    /api/v5/templates/{id}                 # 更新模板
DELETE /api/v5/templates/{id}                 # 删除模板
POST   /api/v5/templates/{id}/publish         # 发布模板
POST   /api/v5/templates/{id}/copy            # 复制模板

# 模板类别
POST   /api/v5/templates/{id}/categories      # 添加类别
PUT    /api/v5/templates/{id}/categories/{categoryId}
DELETE /api/v5/templates/{id}/categories/{categoryId}

# 扣分项/加分项
POST   /api/v5/categories/{categoryId}/items  # 添加扣分项
PUT    /api/v5/items/{itemId}                 # 更新扣分项
DELETE /api/v5/items/{itemId}                 # 删除扣分项

# ---------- 检查项目 ----------
POST   /api/v5/projects                       # 创建项目
GET    /api/v5/projects                       # 项目列表 (支持权限过滤)
GET    /api/v5/projects/{id}                  # 项目详情
PUT    /api/v5/projects/{id}                  # 更新项目
DELETE /api/v5/projects/{id}                  # 删除项目
POST   /api/v5/projects/{id}/activate         # 激活项目
POST   /api/v5/projects/{id}/pause            # 暂停项目
POST   /api/v5/projects/{id}/archive          # 归档项目

# 项目类别配置
GET    /api/v5/projects/{id}/categories       # 获取项目类别
PUT    /api/v5/projects/{id}/categories       # 更新项目类别配置
PUT    /api/v5/projects/{id}/weight-config    # 更新权重配置
PUT    /api/v5/projects/{id}/scoring-config   # 更新打分配置

# ---------- 检查任务 ----------
POST   /api/v5/tasks                          # 创建任务
GET    /api/v5/tasks                          # 任务列表
GET    /api/v5/tasks/{id}                     # 任务详情
PUT    /api/v5/tasks/{id}                     # 更新任务
DELETE /api/v5/tasks/{id}                     # 删除任务 (仅草稿)

# 任务状态操作
POST   /api/v5/tasks/{id}/start               # 开始检查
POST   /api/v5/tasks/{id}/submit              # 提交任务
POST   /api/v5/tasks/{id}/publish             # 发布任务
POST   /api/v5/tasks/{id}/cancel              # 取消任务

# 快速开始
POST   /api/v5/tasks/quick-start              # 快速创建并开始任务

# ---------- 检查录入 ----------
# 按宿舍录入
POST   /api/v5/tasks/{id}/records/dormitory   # 宿舍扣分/加分
# 按教室录入
POST   /api/v5/tasks/{id}/records/classroom   # 教室扣分/加分
# 按学生录入
POST   /api/v5/tasks/{id}/records/student     # 学生扣分/加分
# 按班级录入
POST   /api/v5/tasks/{id}/records/class       # 班级扣分/加分
# 按扣分项录入
POST   /api/v5/tasks/{id}/records/item        # 扣分项批量录入

# 记录管理
GET    /api/v5/tasks/{id}/records             # 获取任务所有记录
GET    /api/v5/tasks/{id}/records/summary     # 获取任务汇总
DELETE /api/v5/records/{recordId}             # 删除扣分/加分记录

# 证据管理
POST   /api/v5/records/{recordId}/evidences   # 上传证据
DELETE /api/v5/evidences/{evidenceId}         # 删除证据

# ---------- 汇总与排名 ----------
# 每日汇总
POST   /api/v5/summaries/daily/generate       # 生成每日汇总
GET    /api/v5/summaries/daily                # 查询每日汇总
POST   /api/v5/summaries/daily/publish        # 发布每日汇总

# 周期汇总
POST   /api/v5/summaries/weekly/generate      # 生成周汇总
POST   /api/v5/summaries/monthly/generate     # 生成月汇总
POST   /api/v5/summaries/semester/generate    # 生成学期汇总
GET    /api/v5/summaries/period               # 查询周期汇总

# 排名
GET    /api/v5/rankings/daily                 # 每日排名
GET    /api/v5/rankings/weekly                # 每周排名
GET    /api/v5/rankings/monthly               # 每月排名
GET    /api/v5/rankings/semester              # 学期排名
GET    /api/v5/rankings/snapshots             # 排名快照历史

# ---------- 申诉 ----------
POST   /api/v5/appeals                        # 提交申诉
GET    /api/v5/appeals                        # 申诉列表
GET    /api/v5/appeals/{id}                   # 申诉详情
POST   /api/v5/appeals/{id}/review            # 开始审核
POST   /api/v5/appeals/{id}/approve           # 批准申诉
POST   /api/v5/appeals/{id}/reject            # 驳回申诉
POST   /api/v5/appeals/{id}/withdraw          # 撤回申诉

# ---------- 整改 ----------
POST   /api/v5/corrective-actions             # 创建整改工单
GET    /api/v5/corrective-actions             # 整改工单列表
GET    /api/v5/corrective-actions/{id}        # 工单详情
PUT    /api/v5/corrective-actions/{id}        # 更新工单
POST   /api/v5/corrective-actions/{id}/start  # 开始整改
POST   /api/v5/corrective-actions/{id}/submit # 提交整改
POST   /api/v5/corrective-actions/{id}/verify # 复查
POST   /api/v5/corrective-actions/{id}/close  # 关闭工单

# 整改记录
POST   /api/v5/corrective-actions/{id}/records # 添加整改记录

# ---------- 统计分析 ----------
GET    /api/v5/statistics/overview            # 总览统计
GET    /api/v5/statistics/trend               # 趋势分析
GET    /api/v5/statistics/distribution        # 问题分布
GET    /api/v5/statistics/comparison          # 对比分析
GET    /api/v5/statistics/workload            # 检查人工作量

# ---------- 导出 ----------
POST   /api/v5/export/rankings                # 导出排名
POST   /api/v5/export/records                 # 导出检查记录
POST   /api/v5/export/summaries               # 导出汇总报表
```

### 5.2 核心DTO设计

```java
// ==================== 检查项目 ====================

@Data
public class CreateProjectRequest {
    @NotBlank
    private String projectName;
    private String description;

    @NotNull
    private LocalDate startDate;
    private LocalDate endDate;

    @NotNull
    private FrequencyConfig frequencyConfig;

    @NotNull
    private ScoringConfig scoringConfig;

    @NotNull
    private WeightConfig weightConfig;

    @NotNull
    private SummaryConfig summaryConfig;

    @NotEmpty
    private List<ProjectCategoryConfig> categories;
}

@Data
public class FrequencyConfig {
    private FrequencyType type;           // DAILY, WEEKLY, CUSTOM
    private Integer timesPerWeek;         // 每周次数
    private List<Integer> weekdays;       // 星期几 (1-7)
    private List<String> timeSlots;       // MORNING, AFTERNOON, EVENING
    private Boolean autoGenerate;         // 自动生成任务
}

@Data
public class ScoringConfig {
    private ScoringMode mode;             // PERCENTAGE, DEDUCTION_ONLY, BONUS_ONLY, GRADE, PASS_FAIL
    private Integer baseScore;            // 基准分 (百分制/纯扣分时使用)
    private Integer minScore;             // 最低分
    private Integer maxScore;             // 最高分
    private Boolean allowBonus;           // 允许加分 (百分制时)
    private Integer maxBonus;             // 最大加分
    private Boolean allowDeduction;       // 允许扣分 (纯加分时为false)
    private Integer startScore;           // 起始分 (纯加分制时，如从0开始加分)
    private Map<String, Integer> gradeMapping;  // 等级映射
    private Integer passThreshold;        // 合格线 (合格制时)
}

@Data
public class WeightConfig {
    private FairWeightConfig fairWeight;
    private MixedDormitoryConfig mixedDormitory;
    private MissingCategoryConfig missingCategory;
}

@Data
public class FairWeightConfig {
    private Boolean enabled;
    private FairWeightMode mode;          // DIVIDE, BENCHMARK
    private Integer benchmarkCount;       // 基准人数
}

@Data
public class MixedDormitoryConfig {
    private AllocationStrategy strategy;  // RATIO, AVERAGE, FULL, MAIN
}

@Data
public class MissingCategoryConfig {
    private MissingStrategy strategy;     // EXCLUDE, FULL_SCORE
}

// ==================== 检查录入 ====================

@Data
public class DormitoryRecordRequest {
    @NotNull
    private Long dormitoryId;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long scoreItemId;

    @NotNull
    private BigDecimal score;             // 扣分为正数，加分为正数

    private List<Long> studentIds;        // 关联的学生
    private String remark;
    private List<String> evidenceUrls;

    private Boolean linkToClass;          // 是否关联到班级
}

@Data
public class StudentRecordRequest {
    @NotNull
    private List<Long> studentIds;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long scoreItemId;

    @NotNull
    private BigDecimal score;

    private String remark;
    private List<String> evidenceUrls;
}

@Data
public class ClassRecordRequest {
    @NotNull
    private Long classId;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long scoreItemId;

    @NotNull
    private BigDecimal score;

    private List<Long> studentIds;
    private String remark;
    private List<String> evidenceUrls;
}

// ==================== 汇总 ====================

@Data
public class GenerateSummaryRequest {
    @NotNull
    private LocalDate date;

    @NotNull
    private SummaryScope scope;           // PROJECT, TEMPLATE, SCHOOL

    private Long scopeId;                 // 项目ID或模板ID
}

@Data
public class DailySummaryVO {
    private Long id;
    private LocalDate summaryDate;
    private String scopeName;

    private Long targetId;
    private String targetName;
    private String orgUnitName;

    private BigDecimal rawScore;
    private BigDecimal weightedScore;
    private BigDecimal fairAdjustedScore;

    private Integer deductionCount;
    private Integer bonusCount;

    private Integer rank;
    private Integer rankChange;

    private String publishStatus;
    private LocalDateTime publishedAt;

    private List<CategoryScoreVO> categoryScores;
}

@Data
public class CategoryScoreVO {
    private Long categoryId;
    private String categoryName;
    private BigDecimal weight;
    private BigDecimal rawScore;
    private BigDecimal weightedScore;
    private Integer deductionCount;
    private Integer bonusCount;
    private String status;                // NORMAL, MISSING, FULL_SCORE_FILLED
}

@Data
public class RankingVO {
    private Integer rank;
    private Long targetId;
    private String targetName;
    private String orgUnitName;
    private BigDecimal score;
    private BigDecimal fairAdjustedScore;
    private Integer deductionCount;
    private Integer bonusCount;
    private Integer previousRank;
    private Integer rankChange;
}
```

---

## 六、前端页面设计

### 6.1 页面结构规划

```
/inspection (量化检查模块)
├── /templates                        # 检查模板管理
│   ├── TemplateListView.vue         # 模板列表
│   ├── TemplateDetailView.vue       # 模板详情/编辑
│   └── TemplatePreviewView.vue      # 模板预览
│
├── /projects                         # 检查项目管理
│   ├── ProjectListView.vue          # 项目列表
│   ├── ProjectDetailView.vue        # 项目详情
│   ├── ProjectConfigView.vue        # 项目配置
│   └── ProjectWizardView.vue        # 项目创建向导
│
├── /tasks                            # 检查任务执行
│   ├── TaskListView.vue             # 任务列表
│   ├── TaskExecuteView.vue          # 检查执行
│   ├── TaskReviewView.vue           # 任务审核
│   └── QuickCheckView.vue           # 快速检查
│
├── /summaries                        # 汇总与排名
│   ├── DailySummaryView.vue         # 每日汇总
│   ├── PeriodSummaryView.vue        # 周期汇总
│   ├── RankingView.vue              # 排名展示
│   └── RankingPublishView.vue       # 排名发布
│
├── /appeals                          # 申诉管理
│   ├── AppealListView.vue           # 申诉列表
│   ├── AppealDetailView.vue         # 申诉详情
│   ├── MyAppealsView.vue            # 我的申诉
│   └── AppealReviewView.vue         # 申诉审核
│
├── /corrective                       # 整改管理
│   ├── CorrectiveListView.vue       # 整改工单列表
│   ├── CorrectiveDetailView.vue     # 工单详情
│   ├── CorrectiveExecuteView.vue    # 整改执行
│   └── CorrectiveVerifyView.vue     # 整改复查
│
├── /statistics                       # 统计分析
│   ├── OverviewDashboard.vue        # 总览仪表盘
│   ├── TrendAnalysisView.vue        # 趋势分析
│   ├── DistributionView.vue         # 问题分布
│   └── ComparisonView.vue           # 对比分析
│
└── /config                           # 系统配置
    ├── WeightConfigView.vue         # 权重配置
    ├── ScoringConfigView.vue        # 打分配置
    └── GlobalSettingsView.vue       # 全局设置
```

### 6.2 核心页面设计

#### 6.2.1 项目创建向导 (ProjectWizardView)

```vue
<template>
  <div class="project-wizard">
    <el-steps :active="currentStep" finish-status="success">
      <el-step title="基本信息" />
      <el-step title="选择模板" />
      <el-step title="配置类别" />
      <el-step title="权重设置" />
      <el-step title="打分规则" />
      <el-step title="汇总配置" />
      <el-step title="确认创建" />
    </el-steps>

    <!-- Step 1: 基本信息 -->
    <div v-if="currentStep === 0" class="step-content">
      <el-form :model="form" label-width="120px">
        <el-form-item label="项目名称" required>
          <el-input v-model="form.projectName" placeholder="如：2024年秋季宿舍卫生检查" />
        </el-form-item>
        <el-form-item label="检查周期" required>
          <el-date-picker
            v-model="form.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item label="检查频率" required>
          <el-select v-model="form.frequencyConfig.type">
            <el-option label="每日检查" value="DAILY" />
            <el-option label="每周检查" value="WEEKLY" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.frequencyConfig.type === 'WEEKLY'" label="检查日期">
          <el-checkbox-group v-model="form.frequencyConfig.weekdays">
            <el-checkbox :label="1">周一</el-checkbox>
            <el-checkbox :label="2">周二</el-checkbox>
            <el-checkbox :label="3">周三</el-checkbox>
            <el-checkbox :label="4">周四</el-checkbox>
            <el-checkbox :label="5">周五</el-checkbox>
            <el-checkbox :label="6">周六</el-checkbox>
            <el-checkbox :label="7">周日</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="时间段">
          <el-checkbox-group v-model="form.frequencyConfig.timeSlots">
            <el-checkbox label="MORNING">上午</el-checkbox>
            <el-checkbox label="AFTERNOON">下午</el-checkbox>
            <el-checkbox label="EVENING">晚间</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="自动生成任务">
          <el-switch v-model="form.frequencyConfig.autoGenerate" />
        </el-form-item>
      </el-form>
    </div>

    <!-- Step 2: 选择模板 -->
    <div v-if="currentStep === 1" class="step-content">
      <div class="template-selector">
        <el-alert type="info" :closable="false">
          请选择要使用的检查模板，可以选择多个模板的类别组合成一个项目
        </el-alert>
        <div class="template-list">
          <el-card
            v-for="template in availableTemplates"
            :key="template.id"
            :class="{ selected: selectedTemplates.includes(template.id) }"
            @click="toggleTemplate(template.id)"
          >
            <div class="template-card">
              <el-checkbox :model-value="selectedTemplates.includes(template.id)" />
              <div class="template-info">
                <h4>{{ template.name }}</h4>
                <p>{{ template.description }}</p>
                <el-tag size="small">{{ template.categoryCount }} 个类别</el-tag>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- Step 3: 配置类别 -->
    <div v-if="currentStep === 2" class="step-content">
      <div class="category-config">
        <el-alert type="info" :closable="false">
          选择要包含的检查类别，并配置各类别的权重占比
        </el-alert>
        <el-table :data="categoriesFromTemplates" stripe>
          <el-table-column type="selection" width="55" />
          <el-table-column prop="name" label="类别名称" />
          <el-table-column prop="templateName" label="来源模板" />
          <el-table-column prop="itemCount" label="检查项数" width="100" />
          <el-table-column label="权重占比" width="200">
            <template #default="{ row }">
              <el-input-number
                v-model="row.weightPercentage"
                :min="0"
                :max="100"
                :precision="1"
              >
                <template #suffix>%</template>
              </el-input-number>
            </template>
          </el-table-column>
        </el-table>
        <div class="weight-summary">
          <el-alert
            :type="totalWeight === 100 ? 'success' : 'warning'"
            :closable="false"
          >
            当前权重总和: {{ totalWeight }}% (必须等于100%)
          </el-alert>
        </div>
      </div>
    </div>

    <!-- Step 4: 权重设置 -->
    <div v-if="currentStep === 3" class="step-content">
      <el-form :model="form.weightConfig" label-width="160px">
        <!-- 公平权重 -->
        <el-divider content-position="left">公平权重</el-divider>
        <el-form-item label="启用公平权重">
          <el-switch v-model="form.weightConfig.fairWeight.enabled" />
          <span class="form-tip">根据班级/宿舍人数调整分数，避免人数差异导致的不公平</span>
        </el-form-item>
        <template v-if="form.weightConfig.fairWeight.enabled">
          <el-form-item label="计算方式" required>
            <el-radio-group v-model="form.weightConfig.fairWeight.mode">
              <el-radio label="DIVIDE">
                除以人数法
                <el-tooltip content="扣分 ÷ 班级人数，人多的班级扣分影响更小">
                  <el-icon><QuestionFilled /></el-icon>
                </el-tooltip>
              </el-radio>
              <el-radio label="BENCHMARK">
                基准人数法
                <el-tooltip content="按基准人数折算，更公平但需设置基准值">
                  <el-icon><QuestionFilled /></el-icon>
                </el-tooltip>
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item
            v-if="form.weightConfig.fairWeight.mode === 'BENCHMARK'"
            label="基准人数"
            required
          >
            <el-input-number
              v-model="form.weightConfig.fairWeight.benchmarkCount"
              :min="1"
              :max="100"
            />
            <span class="form-tip">通常设置为年级平均班级人数</span>
          </el-form-item>
        </template>

        <!-- 混合宿舍分配 -->
        <el-divider content-position="left">混合宿舍分配</el-divider>
        <el-form-item label="分配策略" required>
          <el-select v-model="form.weightConfig.mixedDormitory.strategy" style="width: 300px;">
            <el-option label="按人数比例分配 (推荐)" value="RATIO">
              <div class="option-with-desc">
                <span>按人数比例分配</span>
                <span class="desc">扣分按各班人数比例分摊，最公平</span>
              </div>
            </el-option>
            <el-option label="平均分配" value="AVERAGE">
              <div class="option-with-desc">
                <span>平均分配</span>
                <span class="desc">扣分平均分给各班，简单但可能不公平</span>
              </div>
            </el-option>
            <el-option label="全额分配" value="FULL">
              <div class="option-with-desc">
                <span>全额分配</span>
                <span class="desc">所有班级都扣全额，较严格</span>
              </div>
            </el-option>
            <el-option label="主班级承担" value="MAIN">
              <div class="option-with-desc">
                <span>主班级承担</span>
                <span class="desc">人数最多的班级承担全部扣分</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 缺失类别处理 -->
        <el-divider content-position="left">缺失类别处理</el-divider>
        <el-form-item label="处理策略" required>
          <el-radio-group v-model="form.weightConfig.missingCategory.strategy">
            <el-radio label="EXCLUDE">
              排除计算
              <el-tooltip content="该类别不参与总分计算，权重重新分配">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </el-radio>
            <el-radio label="FULL_SCORE">
              按满分计
              <el-tooltip content="未检查的类别按满分计入总分">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </div>

    <!-- Step 5: 打分规则 -->
    <div v-if="currentStep === 4" class="step-content">
      <el-form :model="form.scoringConfig" label-width="140px">
        <el-form-item label="打分模式" required>
          <el-radio-group v-model="form.scoringConfig.mode" @change="onScoringModeChange">
            <el-radio-button label="PERCENTAGE">百分制</el-radio-button>
            <el-radio-button label="DEDUCTION_ONLY">纯扣分</el-radio-button>
            <el-radio-button label="BONUS_ONLY">纯加分</el-radio-button>
            <el-radio-button label="GRADE">等级制</el-radio-button>
            <el-radio-button label="PASS_FAIL">合格制</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 百分制配置 -->
        <template v-if="form.scoringConfig.mode === 'PERCENTAGE'">
          <el-form-item label="基准分" required>
            <el-input-number v-model="form.scoringConfig.baseScore" :min="0" :max="200" />
            <span class="form-tip">通常为100分</span>
          </el-form-item>
          <el-form-item label="分数范围" required>
            <el-input-number v-model="form.scoringConfig.minScore" :min="0" placeholder="最低" />
            <span style="margin: 0 10px;">-</span>
            <el-input-number v-model="form.scoringConfig.maxScore" :min="0" placeholder="最高" />
          </el-form-item>
          <el-form-item label="允许加分">
            <el-switch v-model="form.scoringConfig.allowBonus" />
          </el-form-item>
          <el-form-item v-if="form.scoringConfig.allowBonus" label="最大加分">
            <el-input-number v-model="form.scoringConfig.maxBonus" :min="0" />
          </el-form-item>
        </template>

        <!-- 纯扣分配置 -->
        <template v-if="form.scoringConfig.mode === 'DEDUCTION_ONLY'">
          <el-alert type="info" :closable="false">
            纯扣分模式：从基准分开始只扣不加，最终得分 = 基准分 - 总扣分
          </el-alert>
          <el-form-item label="基准分" required>
            <el-input-number v-model="form.scoringConfig.baseScore" :min="0" />
          </el-form-item>
          <el-form-item label="最低分" required>
            <el-input-number v-model="form.scoringConfig.minScore" :min="0" />
            <span class="form-tip">扣完为止还是允许负分</span>
          </el-form-item>
        </template>

        <!-- 纯加分配置 -->
        <template v-if="form.scoringConfig.mode === 'BONUS_ONLY'">
          <el-alert type="info" :closable="false">
            纯加分模式：从起始分开始只加不扣，适用于评优检查等场景
          </el-alert>
          <el-form-item label="起始分" required>
            <el-input-number v-model="form.scoringConfig.startScore" :min="0" />
            <span class="form-tip">通常为0，从零开始累加</span>
          </el-form-item>
          <el-form-item label="最高分" required>
            <el-input-number v-model="form.scoringConfig.maxScore" :min="0" />
            <span class="form-tip">加分上限</span>
          </el-form-item>
        </template>

        <!-- 等级制配置 -->
        <template v-if="form.scoringConfig.mode === 'GRADE'">
          <el-form-item label="等级定义" required>
            <div class="grade-config">
              <div v-for="(grade, index) in form.scoringConfig.grades" :key="index" class="grade-item">
                <el-input v-model="grade.name" placeholder="等级名称" style="width: 100px;" />
                <el-input-number v-model="grade.value" placeholder="对应分值" :min="0" />
                <el-color-picker v-model="grade.color" />
                <el-button type="danger" :icon="Delete" circle @click="removeGrade(index)" />
              </div>
              <el-button type="primary" :icon="Plus" @click="addGrade">添加等级</el-button>
            </div>
          </el-form-item>
        </template>

        <!-- 合格制配置 -->
        <template v-if="form.scoringConfig.mode === 'PASS_FAIL'">
          <el-form-item label="合格标准" required>
            <el-input-number v-model="form.scoringConfig.passThreshold" :min="0" :max="100" />
            <span class="form-tip">达到此分数线视为合格</span>
          </el-form-item>
        </template>
      </el-form>
    </div>

    <!-- Step 6: 汇总配置 -->
    <div v-if="currentStep === 5" class="step-content">
      <el-form :model="form.summaryConfig" label-width="140px">
        <el-form-item label="汇总周期" required>
          <el-checkbox-group v-model="form.summaryConfig.periods">
            <el-checkbox label="DAILY">每日汇总</el-checkbox>
            <el-checkbox label="WEEKLY">每周汇总</el-checkbox>
            <el-checkbox label="MONTHLY">每月汇总</el-checkbox>
            <el-checkbox label="SEMESTER">学期汇总</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="发布模式" required>
          <el-radio-group v-model="form.summaryConfig.publishMode">
            <el-radio label="AUTO">自动发布</el-radio>
            <el-radio label="MANUAL">手动审核后发布</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排名可见性" required>
          <el-radio-group v-model="form.summaryConfig.rankingVisibility">
            <el-radio label="ALL">全校可见</el-radio>
            <el-radio label="DEPARTMENT">仅本部门可见</el-radio>
            <el-radio label="ADMIN">仅管理员可见</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="汇总维度" required>
          <el-checkbox-group v-model="form.summaryConfig.dimensions">
            <el-checkbox label="PROJECT">按项目</el-checkbox>
            <el-checkbox label="TEMPLATE">按模板</el-checkbox>
            <el-checkbox label="SCHOOL">全校统一</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
    </div>

    <!-- Step 7: 确认创建 -->
    <div v-if="currentStep === 6" class="step-content">
      <div class="project-preview">
        <el-descriptions title="项目信息" :column="2" border>
          <el-descriptions-item label="项目名称">{{ form.projectName }}</el-descriptions-item>
          <el-descriptions-item label="检查周期">
            {{ form.dateRange[0] }} ~ {{ form.dateRange[1] }}
          </el-descriptions-item>
          <el-descriptions-item label="检查频率">{{ frequencyLabel }}</el-descriptions-item>
          <el-descriptions-item label="打分模式">{{ scoringModeLabel }}</el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <h4>类别权重配置</h4>
        <el-table :data="selectedCategories" stripe size="small">
          <el-table-column prop="name" label="类别" />
          <el-table-column prop="weightPercentage" label="权重" width="100">
            <template #default="{ row }">{{ row.weightPercentage }}%</template>
          </el-table-column>
        </el-table>

        <el-divider />

        <h4>权重配置</h4>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="公平权重">
            {{ form.weightConfig.fairWeight.enabled ? fairWeightLabel : '未启用' }}
          </el-descriptions-item>
          <el-descriptions-item label="混合宿舍">
            {{ mixedDormitoryLabel }}
          </el-descriptions-item>
          <el-descriptions-item label="缺失类别">
            {{ missingCategoryLabel }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <div class="wizard-actions">
      <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
      <el-button v-if="currentStep < 6" type="primary" @click="nextStep" :disabled="!canNext">
        下一步
      </el-button>
      <el-button v-if="currentStep === 6" type="success" @click="createProject" :loading="creating">
        创建项目
      </el-button>
    </div>
  </div>
</template>
```

#### 6.2.2 检查执行页面 (TaskExecuteView)

```vue
<template>
  <div class="task-execute">
    <!-- 任务信息头部 -->
    <div class="task-header">
      <div class="task-info">
        <h2>{{ task.taskName }}</h2>
        <el-tag :type="taskStatusType">{{ task.status }}</el-tag>
      </div>
      <div class="task-meta">
        <span>检查日期: {{ task.inspectionDate }}</span>
        <span>时间段: {{ task.timeSlot }}</span>
        <span>已录入: {{ recordCount }} 条</span>
      </div>
    </div>

    <!-- 录入模式选择 -->
    <div class="input-mode-selector">
      <el-radio-group v-model="inputMode" size="large">
        <el-radio-button label="SPACE">
          <el-icon><Location /></el-icon>
          按空间录入
        </el-radio-button>
        <el-radio-button label="PERSON">
          <el-icon><User /></el-icon>
          按学生录入
        </el-radio-button>
        <el-radio-button label="CLASS">
          <el-icon><School /></el-icon>
          按班级录入
        </el-radio-button>
        <el-radio-button label="ITEM">
          <el-icon><Document /></el-icon>
          按扣分项录入
        </el-radio-button>
        <el-radio-button label="CHECKLIST">
          <el-icon><Checked /></el-icon>
          清单模式
        </el-radio-button>
        <el-radio-button label="FREE">
          <el-icon><Edit /></el-icon>
          自由录入
        </el-radio-button>
      </el-radio-group>
    </div>

    <el-divider />

    <!-- 按空间录入模式 -->
    <div v-if="inputMode === 'SPACE'" class="space-input-mode">
      <div class="left-panel">
        <!-- 空间选择器 (宿舍楼 -> 楼层 -> 宿舍) -->
        <div class="space-tree">
          <el-input v-model="spaceSearch" placeholder="搜索宿舍号..." clearable />
          <el-tree
            ref="spaceTree"
            :data="spaceTreeData"
            :props="{ label: 'name', children: 'children' }"
            highlight-current
            @node-click="onSpaceSelect"
          >
            <template #default="{ node, data }">
              <span class="space-node">
                <span>{{ node.label }}</span>
                <el-badge
                  v-if="data.deductionCount"
                  :value="data.deductionCount"
                  type="danger"
                />
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <div class="right-panel">
        <div v-if="selectedSpace" class="space-detail">
          <div class="space-info">
            <h3>{{ selectedSpace.name }}</h3>
            <div class="students">
              <span v-for="student in selectedSpace.students" :key="student.id" class="student-tag">
                {{ student.name }} ({{ student.className }})
              </span>
            </div>
          </div>

          <!-- 快速扣分面板 -->
          <div class="quick-score-panel">
            <el-tabs v-model="activeCategory">
              <el-tab-pane
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :name="category.id.toString()"
              >
                <div class="score-items">
                  <div
                    v-for="item in category.items"
                    :key="item.id"
                    class="score-item"
                    :class="{ deduction: item.scoreType === 'DEDUCTION', bonus: item.scoreType === 'BONUS' }"
                    @click="quickAddRecord(item)"
                  >
                    <span class="item-name">{{ item.name }}</span>
                    <span class="item-score">
                      {{ item.scoreType === 'DEDUCTION' ? '-' : '+' }}{{ item.defaultScore }}
                    </span>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>

          <!-- 已录入记录 -->
          <div class="current-records">
            <h4>本次已录入 ({{ currentSpaceRecords.length }})</h4>
            <el-table :data="currentSpaceRecords" size="small">
              <el-table-column prop="categoryName" label="类别" width="100" />
              <el-table-column prop="itemName" label="扣分项" />
              <el-table-column prop="score" label="分值" width="80">
                <template #default="{ row }">
                  <span :class="row.score < 0 ? 'text-danger' : 'text-success'">
                    {{ row.score }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="证据" width="80">
                <template #default="{ row }">
                  <el-button
                    v-if="row.evidences?.length"
                    type="primary"
                    link
                    @click="previewEvidences(row.evidences)"
                  >
                    {{ row.evidences.length }}张
                  </el-button>
                  <el-button v-else type="primary" link @click="uploadEvidence(row)">
                    上传
                  </el-button>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button type="danger" link @click="deleteRecord(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <div v-else class="empty-tip">
          <el-empty description="请从左侧选择要检查的空间" />
        </div>
      </div>
    </div>

    <!-- 按学生录入模式 -->
    <div v-if="inputMode === 'PERSON'" class="person-input-mode">
      <div class="student-search">
        <el-autocomplete
          v-model="studentSearch"
          :fetch-suggestions="searchStudents"
          placeholder="输入学号或姓名搜索学生..."
          style="width: 400px;"
          @select="onStudentSelect"
        >
          <template #default="{ item }">
            <div class="student-option">
              <span class="name">{{ item.name }}</span>
              <span class="code">{{ item.studentCode }}</span>
              <span class="class">{{ item.className }}</span>
            </div>
          </template>
        </el-autocomplete>
      </div>

      <div v-if="selectedStudents.length" class="selected-students">
        <h4>已选学生</h4>
        <div class="student-tags">
          <el-tag
            v-for="student in selectedStudents"
            :key="student.id"
            closable
            @close="removeStudent(student)"
          >
            {{ student.name }} ({{ student.className }})
          </el-tag>
        </div>
      </div>

      <div v-if="selectedStudents.length" class="score-form">
        <el-form :model="personRecordForm" label-width="100px">
          <el-form-item label="检查类别" required>
            <el-select v-model="personRecordForm.categoryId" @change="onCategoryChange">
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="扣分/加分项" required>
            <el-select v-model="personRecordForm.scoreItemId">
              <el-option
                v-for="item in currentCategoryItems"
                :key="item.id"
                :label="`${item.name} (${item.scoreType === 'DEDUCTION' ? '-' : '+'}${item.defaultScore})`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="分值">
            <el-input-number v-model="personRecordForm.score" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="personRecordForm.remark" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="证据照片">
            <el-upload
              v-model:file-list="personRecordForm.evidences"
              list-type="picture-card"
              :action="uploadUrl"
              :headers="uploadHeaders"
              accept="image/*"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitPersonRecord">确认录入</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="task-actions">
      <el-button @click="saveDraft">保存草稿</el-button>
      <el-button type="primary" @click="submitTask" :disabled="recordCount === 0">
        提交检查 ({{ recordCount }}条记录)
      </el-button>
    </div>

    <!-- 证据上传对话框 -->
    <el-dialog v-model="evidenceDialogVisible" title="上传证据" width="500px">
      <el-upload
        v-model:file-list="evidenceFileList"
        list-type="picture-card"
        :action="uploadUrl"
        :headers="uploadHeaders"
        accept="image/*"
        :on-success="onEvidenceUploadSuccess"
      >
        <el-icon><Plus /></el-icon>
      </el-upload>
      <div class="watermark-config">
        <el-checkbox v-model="addWatermark">添加水印</el-checkbox>
        <template v-if="addWatermark">
          <el-checkbox-group v-model="watermarkContent">
            <el-checkbox label="TIME">时间</el-checkbox>
            <el-checkbox label="LOCATION">位置</el-checkbox>
            <el-checkbox label="INSPECTOR">检查人</el-checkbox>
          </el-checkbox-group>
        </template>
      </div>
      <template #footer>
        <el-button @click="evidenceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEvidenceUpload">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>
```

#### 6.2.3 排名展示页面 (RankingView)

```vue
<template>
  <div class="ranking-view">
    <!-- 筛选条件 -->
    <div class="ranking-filters">
      <el-form :inline="true">
        <el-form-item label="排名类型">
          <el-radio-group v-model="rankingType">
            <el-radio-button label="DAILY">每日</el-radio-button>
            <el-radio-button label="WEEKLY">每周</el-radio-button>
            <el-radio-button label="MONTHLY">每月</el-radio-button>
            <el-radio-button label="SEMESTER">学期</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="汇总维度">
          <el-select v-model="summaryScope">
            <el-option label="按项目" value="PROJECT" />
            <el-option label="按模板" value="TEMPLATE" />
            <el-option label="全校统一" value="SCHOOL" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="summaryScope === 'PROJECT'" label="检查项目">
          <el-select v-model="scopeId" placeholder="选择项目">
            <el-option
              v-for="project in projects"
              :key="project.id"
              :label="project.name"
              :value="project.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-if="rankingType === 'DAILY'"
            v-model="rankingDate"
            type="date"
            placeholder="选择日期"
          />
          <el-date-picker
            v-else-if="rankingType === 'WEEKLY'"
            v-model="rankingWeek"
            type="week"
            format="YYYY 第 ww 周"
            placeholder="选择周"
          />
          <el-date-picker
            v-else-if="rankingType === 'MONTHLY'"
            v-model="rankingMonth"
            type="month"
            placeholder="选择月份"
          />
          <el-select v-else v-model="rankingSemester" placeholder="选择学期">
            <el-option label="2024-2025学年第一学期" value="2024-1" />
            <el-option label="2024-2025学年第二学期" value="2024-2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadRanking">查询</el-button>
          <el-button @click="exportRanking">导出</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 排名统计卡片 -->
    <div class="ranking-summary">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-statistic title="参与班级" :value="rankingData.totalClasses" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="平均分" :value="rankingData.averageScore" :precision="2" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="最高分" :value="rankingData.highestScore" :precision="2" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="最低分" :value="rankingData.lowestScore" :precision="2" />
        </el-col>
      </el-row>
    </div>

    <!-- 排名表格 -->
    <div class="ranking-table">
      <el-table
        :data="rankingList"
        stripe
        :row-class-name="tableRowClassName"
      >
        <el-table-column label="排名" width="100" align="center">
          <template #default="{ row }">
            <div class="rank-cell">
              <span v-if="row.rank <= 3" class="rank-medal" :class="`rank-${row.rank}`">
                {{ row.rank }}
              </span>
              <span v-else>{{ row.rank }}</span>
              <span v-if="row.rankChange !== 0" class="rank-change" :class="row.rankChange > 0 ? 'up' : 'down'">
                <el-icon v-if="row.rankChange > 0"><CaretTop /></el-icon>
                <el-icon v-else><CaretBottom /></el-icon>
                {{ Math.abs(row.rankChange) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="orgUnitName" label="部门" width="120" />
        <el-table-column prop="targetName" label="班级" width="150" />
        <el-table-column label="得分" width="120" align="right">
          <template #default="{ row }">
            <span class="score" :class="getScoreClass(row.fairAdjustedScore)">
              {{ row.fairAdjustedScore.toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="扣分" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="danger" size="small">-{{ row.totalDeduction }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="加分" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.totalBonus > 0" type="success" size="small">+{{ row.totalBonus }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="类别得分" min-width="300">
          <template #default="{ row }">
            <div class="category-scores">
              <el-tooltip
                v-for="cat in row.categoryScores"
                :key="cat.categoryId"
                :content="`${cat.categoryName}: ${cat.weightedScore.toFixed(2)}分 (占比${cat.weight}%)`"
              >
                <el-progress
                  :percentage="cat.percentage"
                  :stroke-width="16"
                  :color="getCategoryColor(cat)"
                  style="width: 80px; margin-right: 8px;"
                >
                  <span>{{ cat.weightedScore.toFixed(1) }}</span>
                </el-progress>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 趋势图表 -->
    <div class="ranking-trend">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>得分趋势</span>
            <el-select v-model="trendClassIds" multiple placeholder="选择班级对比" style="width: 300px;">
              <el-option
                v-for="item in rankingList"
                :key="item.targetId"
                :label="item.targetName"
                :value="item.targetId"
              />
            </el-select>
          </div>
        </template>
        <v-chart :option="trendChartOption" style="height: 300px;" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import * as rankingApi from '@/api/ranking'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

// ... 组件逻辑
</script>

<style scoped lang="scss">
.ranking-view {
  padding: 20px;
}

.rank-medal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-weight: bold;
  color: white;

  &.rank-1 {
    background: linear-gradient(135deg, #ffd700, #ffb347);
  }
  &.rank-2 {
    background: linear-gradient(135deg, #c0c0c0, #a8a8a8);
  }
  &.rank-3 {
    background: linear-gradient(135deg, #cd7f32, #b8860b);
  }
}

.rank-change {
  font-size: 12px;
  margin-left: 4px;

  &.up {
    color: #67c23a;
  }
  &.down {
    color: #f56c6c;
  }
}

.score {
  font-weight: bold;
  font-size: 16px;

  &.excellent {
    color: #67c23a;
  }
  &.good {
    color: #409eff;
  }
  &.medium {
    color: #e6a23c;
  }
  &.poor {
    color: #f56c6c;
  }
}
</style>
```

### 6.3 移动端适配设计

```vue
<!-- 移动端检查执行页面 (MobileCheckView.vue) -->
<template>
  <div class="mobile-check">
    <!-- 顶部任务信息 -->
    <van-nav-bar
      :title="task.taskName"
      left-arrow
      @click-left="goBack"
    >
      <template #right>
        <van-badge :content="recordCount">
          <van-icon name="records" size="20" />
        </van-badge>
      </template>
    </van-nav-bar>

    <!-- 空间选择 (下拉面板) -->
    <van-dropdown-menu>
      <van-dropdown-item v-model="selectedBuilding" :options="buildings" title="选择楼栋" />
      <van-dropdown-item v-model="selectedFloor" :options="floors" title="选择楼层" />
    </van-dropdown-menu>

    <!-- 宿舍列表 -->
    <van-list v-model:loading="loading" :finished="finished" @load="loadDormitories">
      <van-cell-group inset>
        <van-cell
          v-for="dorm in dormitories"
          :key="dorm.id"
          :title="dorm.name"
          :label="`${dorm.studentCount}人 | ${dorm.classes.join('、')}`"
          is-link
          @click="selectDormitory(dorm)"
        >
          <template #right-icon>
            <van-badge v-if="dorm.recordCount" :content="dorm.recordCount" type="danger" />
          </template>
        </van-cell>
      </van-cell-group>
    </van-list>

    <!-- 扣分快捷面板 (底部弹出) -->
    <van-action-sheet v-model:show="showScorePanel" title="扣分/加分" :round="false">
      <div class="score-panel">
        <van-tabs v-model:active="activeCategory" swipeable>
          <van-tab v-for="cat in categories" :key="cat.id" :title="cat.name">
            <div class="score-items">
              <van-grid :column-num="2" :border="false">
                <van-grid-item
                  v-for="item in cat.items"
                  :key="item.id"
                  @click="quickScore(item)"
                >
                  <div class="score-item-card" :class="item.scoreType.toLowerCase()">
                    <span class="item-name">{{ item.name }}</span>
                    <span class="item-score">
                      {{ item.scoreType === 'DEDUCTION' ? '-' : '+' }}{{ item.defaultScore }}
                    </span>
                  </div>
                </van-grid-item>
              </van-grid>
            </div>
          </van-tab>
        </van-tabs>
      </div>
    </van-action-sheet>

    <!-- 拍照证据 -->
    <van-popup v-model:show="showCamera" position="bottom" :style="{ height: '80%' }">
      <div class="camera-panel">
        <van-uploader
          v-model="evidenceFiles"
          :max-count="5"
          :after-read="afterRead"
          capture="camera"
        >
          <van-button icon="photograph" type="primary" block>拍照取证</van-button>
        </van-uploader>
        <van-checkbox-group v-model="watermarkOptions" direction="horizontal">
          <van-checkbox name="TIME">时间水印</van-checkbox>
          <van-checkbox name="LOCATION">位置水印</van-checkbox>
        </van-checkbox-group>
      </div>
    </van-popup>

    <!-- 底部操作 -->
    <van-submit-bar
      :price="recordCount * 100"
      :button-text="`提交检查 (${recordCount}条)`"
      @submit="submitCheck"
    >
      <template #tip>
        当前宿舍: {{ currentDorm?.name || '未选择' }}
      </template>
    </van-submit-bar>
  </div>
</template>
```

---

## 七、V4到V5迁移策略

### 7.1 迁移概述

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           V4 → V5 迁移路线图                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  阶段1: 基础设施准备                                                        │
│  ├── 创建V5数据库表结构                                                     │
│  ├── 部署V5后端API (与V4并行运行)                                           │
│  └── 准备数据迁移脚本                                                       │
│                                                                             │
│  阶段2: 数据迁移                                                            │
│  ├── 迁移检查模板 (V4 templates → V5 templates)                             │
│  ├── 迁移历史检查数据 (V4 plans/sessions → V5 projects/tasks)               │
│  └── 迁移申诉和整改记录                                                     │
│                                                                             │
│  阶段3: 功能切换                                                            │
│  ├── 新建项目使用V5系统                                                     │
│  ├── V4历史数据只读访问                                                     │
│  └── 前端逐步切换到V5页面                                                   │
│                                                                             │
│  阶段4: 完全迁移                                                            │
│  ├── 停止V4系统写入                                                         │
│  ├── 归档V4数据                                                             │
│  └── 下线V4 API                                                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.2 数据映射关系

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         V4 → V5 数据映射                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  V4 表                           V5 表                                      │
│  ──────────────────────────────────────────────────────────────────────    │
│                                                                             │
│  inspection_templates    ──────► inspection_templates                       │
│  template_categories     ──────► template_categories                        │
│  template_items          ──────► score_items                                │
│                                                                             │
│  inspection_plans        ──────► inspection_projects                        │
│  (多个plan可合并为一个project，根据实际使用场景)                            │
│                                                                             │
│  inspection_sessions     ──────► inspection_tasks                           │
│  (session与task一一对应，但结构更清晰)                                      │
│                                                                             │
│  session_class_records   ──────► class_records                              │
│  (聚合结果，不需要重新计算)                                                 │
│                                                                             │
│  class_deductions        ──────► target_records + score_details             │
│  (原扣分记录拆分为两层结构)                                                 │
│                                                                             │
│  appeals                 ──────► appeals (结构基本兼容)                     │
│  appeal_reviews          ──────► appeal_approvals                           │
│                                                                             │
│  corrective_actions      ──────► corrective_actions (新增更多状态)          │
│  corrective_records      ──────► corrective_records                         │
│                                                                             │
│  新增表:                                                                    │
│  - daily_summaries (每日汇总)                                               │
│  - period_summaries (周期汇总)                                              │
│  - ranking_snapshots (排名快照)                                             │
│  - evidences (证据管理)                                                     │
│  - mixed_dormitory_allocations (混合宿舍分配)                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.3 迁移脚本

```sql
-- ============================================================
-- V4 → V5 数据迁移脚本
-- ============================================================

-- 1. 迁移检查模板
INSERT INTO v5_inspection_templates (
    id, template_name, template_code, description,
    target_type, scoring_mode, base_score,
    status, version, created_by, created_at
)
SELECT
    t.id,
    t.name AS template_name,
    CONCAT('TPL_', LPAD(t.id, 6, '0')) AS template_code,
    t.description,
    CASE t.check_type
        WHEN 'DORMITORY' THEN 'DORMITORY'
        WHEN 'CLASSROOM' THEN 'CLASSROOM'
        ELSE 'CLASS'
    END AS target_type,
    'PERCENTAGE' AS scoring_mode,
    100 AS base_score,
    CASE t.status WHEN 1 THEN 'PUBLISHED' ELSE 'DRAFT' END AS status,
    1 AS version,
    t.created_by,
    t.created_at
FROM inspection_templates t
WHERE t.deleted = 0;

-- 2. 迁移模板类别
INSERT INTO v5_template_categories (
    id, template_id, category_name, category_code,
    description, max_deduction, weight_percentage, sort_order
)
SELECT
    c.id,
    c.template_id,
    c.name AS category_name,
    CONCAT('CAT_', LPAD(c.id, 6, '0')) AS category_code,
    c.description,
    COALESCE(c.max_score, 100) AS max_deduction,
    -- 平均分配权重，后续可手动调整
    ROUND(100.0 / (SELECT COUNT(*) FROM template_categories WHERE template_id = c.template_id), 1) AS weight_percentage,
    c.sort_order
FROM template_categories c
WHERE c.deleted = 0;

-- 3. 迁移扣分项
INSERT INTO v5_score_items (
    id, category_id, item_name, item_code,
    score_type, default_score, min_score, max_score,
    allow_multiple, max_count,
    scoring_configs, description, sort_order
)
SELECT
    i.id,
    i.category_id,
    i.name AS item_name,
    CONCAT('ITEM_', LPAD(i.id, 6, '0')) AS item_code,
    CASE WHEN i.score > 0 THEN 'DEDUCTION' ELSE 'BONUS' END AS score_type,
    ABS(i.score) AS default_score,
    0 AS min_score,
    ABS(i.score) * 2 AS max_score,
    1 AS allow_multiple,
    10 AS max_count,
    JSON_OBJECT(
        'PERCENTAGE', JSON_OBJECT('value', ABS(i.score)),
        'DEDUCTION_ONLY', JSON_OBJECT('value', ABS(i.score))
    ) AS scoring_configs,
    i.description,
    i.sort_order
FROM template_items i
WHERE i.deleted = 0;

-- 4. 迁移检查计划到项目
-- 注意：每个V4 plan创建一个V5 project，如有需要可手动合并
INSERT INTO v5_inspection_projects (
    id, project_name, description, series_id,
    start_date, end_date,
    frequency_config, scoring_config, weight_config, summary_config,
    target_scope, status, created_by, created_at
)
SELECT
    p.id,
    p.name AS project_name,
    p.description,
    NULL AS series_id,
    p.start_date,
    p.end_date,
    JSON_OBJECT(
        'type', CASE p.frequency WHEN 'DAILY' THEN 'DAILY' WHEN 'WEEKLY' THEN 'WEEKLY' ELSE 'CUSTOM' END,
        'autoGenerate', TRUE
    ) AS frequency_config,
    JSON_OBJECT(
        'mode', 'PERCENTAGE',
        'baseScore', 100,
        'minScore', 0,
        'maxScore', 100,
        'allowBonus', TRUE,
        'maxBonus', 20
    ) AS scoring_config,
    JSON_OBJECT(
        'fairWeight', JSON_OBJECT('enabled', FALSE, 'mode', 'DIVIDE'),
        'mixedDormitory', JSON_OBJECT('strategy', 'RATIO'),
        'missingCategory', JSON_OBJECT('strategy', 'FULL_SCORE')
    ) AS weight_config,
    JSON_OBJECT(
        'periods', JSON_ARRAY('DAILY', 'WEEKLY', 'MONTHLY'),
        'publishMode', 'MANUAL',
        'rankingVisibility', 'ALL',
        'dimensions', JSON_ARRAY('PROJECT')
    ) AS summary_config,
    JSON_OBJECT('type', 'ALL') AS target_scope,
    CASE p.status WHEN 1 THEN 'ACTIVE' ELSE 'DRAFT' END AS status,
    p.created_by,
    p.created_at
FROM inspection_plans p
WHERE p.deleted = 0;

-- 5. 迁移项目类别关联
INSERT INTO v5_project_categories (
    project_id, template_id, category_id, weight_percentage, sort_order
)
SELECT DISTINCT
    p.id AS project_id,
    t.id AS template_id,
    c.id AS category_id,
    ROUND(100.0 / (SELECT COUNT(DISTINCT c2.id)
        FROM inspection_sessions s2
        JOIN session_class_records scr2 ON s2.id = scr2.session_id
        JOIN class_deductions cd2 ON scr2.id = cd2.class_record_id
        JOIN template_categories c2 ON cd2.category_id = c2.id
        WHERE s2.plan_id = p.id), 1) AS weight_percentage,
    c.sort_order
FROM inspection_plans p
JOIN inspection_sessions s ON p.id = s.plan_id
JOIN session_class_records scr ON s.id = scr.session_id
JOIN class_deductions cd ON scr.id = cd.class_record_id
JOIN template_categories c ON cd.category_id = c.id
JOIN inspection_templates t ON c.template_id = t.id
WHERE p.deleted = 0;

-- 6. 迁移检查任务
INSERT INTO v5_inspection_tasks (
    id, project_id, task_name, task_code,
    inspection_date, time_slot,
    inspector_id, inspector_name,
    status, published_at,
    created_by, created_at
)
SELECT
    s.id,
    s.plan_id AS project_id,
    CONCAT(DATE_FORMAT(s.check_date, '%Y-%m-%d'), ' ',
        CASE s.time_slot WHEN 'MORNING' THEN '上午' WHEN 'AFTERNOON' THEN '下午' ELSE '晚间' END,
        '检查') AS task_name,
    CONCAT('TASK_', LPAD(s.id, 8, '0')) AS task_code,
    s.check_date AS inspection_date,
    s.time_slot,
    s.inspector_id,
    u.name AS inspector_name,
    CASE s.status
        WHEN 'DRAFT' THEN 'DRAFT'
        WHEN 'IN_PROGRESS' THEN 'IN_PROGRESS'
        WHEN 'SUBMITTED' THEN 'SUBMITTED'
        WHEN 'PUBLISHED' THEN 'PUBLISHED'
        ELSE 'DRAFT'
    END AS status,
    CASE WHEN s.status = 'PUBLISHED' THEN s.updated_at END AS published_at,
    s.created_by,
    s.created_at
FROM inspection_sessions s
LEFT JOIN users u ON s.inspector_id = u.id
WHERE s.deleted = 0;

-- 7. 迁移班级记录
INSERT INTO v5_class_records (
    id, task_id, class_id, class_name, org_unit_id, org_unit_name,
    class_size, category_scores,
    raw_score, weighted_score, fair_adjusted_score,
    total_deduction, total_bonus, deduction_count, bonus_count
)
SELECT
    scr.id,
    scr.session_id AS task_id,
    scr.class_id,
    c.name AS class_name,
    c.org_unit_id,
    ou.name AS org_unit_name,
    (SELECT COUNT(*) FROM students WHERE class_id = scr.class_id AND deleted = 0) AS class_size,
    -- 类别得分需要从明细计算
    (
        SELECT JSON_OBJECTAGG(
            cat.id,
            JSON_OBJECT(
                'categoryId', cat.id,
                'categoryName', cat.name,
                'rawScore', COALESCE(SUM(CASE WHEN cd.score > 0 THEN cd.score ELSE 0 END), 0),
                'weightedScore', COALESCE(SUM(CASE WHEN cd.score > 0 THEN cd.score ELSE 0 END), 0),
                'deductionCount', COUNT(CASE WHEN cd.score > 0 THEN 1 END),
                'bonusCount', COUNT(CASE WHEN cd.score < 0 THEN 1 END)
            )
        )
        FROM template_categories cat
        LEFT JOIN class_deductions cd ON cat.id = cd.category_id AND cd.class_record_id = scr.id
        WHERE cat.template_id = (SELECT template_id FROM inspection_plans WHERE id = scr.session_id)
        GROUP BY cat.id
    ) AS category_scores,
    COALESCE(scr.total_score, 100) AS raw_score,
    COALESCE(scr.total_score, 100) AS weighted_score,
    COALESCE(scr.total_score, 100) AS fair_adjusted_score,
    COALESCE(scr.deduction_total, 0) AS total_deduction,
    COALESCE(scr.bonus_total, 0) AS total_bonus,
    COALESCE(scr.deduction_count, 0) AS deduction_count,
    COALESCE(scr.bonus_count, 0) AS bonus_count
FROM session_class_records scr
JOIN school_classes c ON scr.class_id = c.id
JOIN org_units ou ON c.org_unit_id = ou.id
WHERE scr.deleted = 0;

-- 8. 迁移扣分明细到target_records和score_details
-- 8.1 创建target_records (按宿舍/教室/学生分组)
INSERT INTO v5_target_records (
    id, task_id, category_id, target_type, target_id, target_name,
    related_class_id, raw_score, is_mixed_target, remark
)
SELECT
    cd.id,
    s.id AS task_id,
    cd.category_id,
    CASE
        WHEN cd.dormitory_id IS NOT NULL THEN 'DORMITORY'
        WHEN cd.classroom_id IS NOT NULL THEN 'CLASSROOM'
        WHEN cd.student_id IS NOT NULL THEN 'STUDENT'
        ELSE 'CLASS'
    END AS target_type,
    COALESCE(cd.dormitory_id, cd.classroom_id, cd.student_id, scr.class_id) AS target_id,
    COALESCE(d.room_number, cr.room_number, st.name, c.name) AS target_name,
    scr.class_id AS related_class_id,
    cd.score AS raw_score,
    CASE WHEN cd.dormitory_id IS NOT NULL AND
        (SELECT COUNT(DISTINCT class_id) FROM students WHERE dormitory_id = cd.dormitory_id) > 1
        THEN 1 ELSE 0 END AS is_mixed_target,
    cd.remark
FROM class_deductions cd
JOIN session_class_records scr ON cd.class_record_id = scr.id
JOIN inspection_sessions s ON scr.session_id = s.id
JOIN school_classes c ON scr.class_id = c.id
LEFT JOIN dormitories d ON cd.dormitory_id = d.id
LEFT JOIN classrooms cr ON cd.classroom_id = cr.id
LEFT JOIN students st ON cd.student_id = st.id
WHERE cd.deleted = 0;

-- 8.2 创建score_details
INSERT INTO v5_score_details (
    id, target_record_id, score_item_id, score_type, score, original_score,
    student_ids, remark, recorded_by, recorded_at
)
SELECT
    cd.id,
    cd.id AS target_record_id,
    cd.item_id AS score_item_id,
    CASE WHEN cd.score > 0 THEN 'DEDUCTION' ELSE 'BONUS' END AS score_type,
    ABS(cd.score) AS score,
    ABS(cd.score) AS original_score,
    CASE WHEN cd.student_id IS NOT NULL THEN JSON_ARRAY(cd.student_id) ELSE NULL END AS student_ids,
    cd.remark,
    cd.created_by AS recorded_by,
    cd.created_at AS recorded_at
FROM class_deductions cd
WHERE cd.deleted = 0;

-- 9. 迁移证据 (如果V4有图片数据)
INSERT INTO v5_evidences (
    score_detail_id, file_url, file_name, file_size, file_type,
    watermark_config, uploaded_by, uploaded_at
)
SELECT
    cd.id AS score_detail_id,
    cd.image_url AS file_url,
    SUBSTRING_INDEX(cd.image_url, '/', -1) AS file_name,
    0 AS file_size,
    'image/jpeg' AS file_type,
    NULL AS watermark_config,
    cd.created_by AS uploaded_by,
    cd.created_at AS uploaded_at
FROM class_deductions cd
WHERE cd.deleted = 0 AND cd.image_url IS NOT NULL AND cd.image_url != '';

-- 10. 迁移申诉
INSERT INTO v5_appeals (
    id, score_detail_id, task_id, appellant_id, appellant_name, appellant_type,
    appeal_type, appeal_reason, expected_score, evidence_urls,
    status, result, final_score,
    created_at, updated_at
)
SELECT
    a.id,
    a.deduction_id AS score_detail_id,
    s.id AS task_id,
    a.created_by AS appellant_id,
    u.name AS appellant_name,
    'STUDENT' AS appellant_type,
    a.type AS appeal_type,
    a.reason AS appeal_reason,
    a.expected_score,
    CASE WHEN a.evidence_url IS NOT NULL THEN JSON_ARRAY(a.evidence_url) ELSE NULL END AS evidence_urls,
    CASE a.status
        WHEN 'PENDING' THEN 'PENDING'
        WHEN 'REVIEWING' THEN 'REVIEWING'
        WHEN 'APPROVED' THEN 'APPROVED'
        WHEN 'REJECTED' THEN 'REJECTED'
        ELSE 'PENDING'
    END AS status,
    a.result,
    a.final_score,
    a.created_at,
    a.updated_at
FROM appeals a
JOIN class_deductions cd ON a.deduction_id = cd.id
JOIN session_class_records scr ON cd.class_record_id = scr.id
JOIN inspection_sessions s ON scr.session_id = s.id
LEFT JOIN users u ON a.created_by = u.id
WHERE a.deleted = 0;

-- 11. 生成历史每日汇总 (基于已有数据)
INSERT INTO v5_daily_summaries (
    summary_date, summary_scope, scope_id, scope_name,
    target_type, target_id, target_name, org_unit_id, org_unit_name,
    included_task_ids, included_task_count, category_scores,
    raw_score, weighted_score, fair_adjusted_score,
    deduction_count, bonus_count, total_deduction_score, total_bonus_score,
    rank_in_scope, rank_change, publish_status, published_at
)
SELECT
    t.inspection_date AS summary_date,
    'PROJECT' AS summary_scope,
    t.project_id AS scope_id,
    p.project_name AS scope_name,
    'CLASS' AS target_type,
    cr.class_id AS target_id,
    cr.class_name AS target_name,
    cr.org_unit_id,
    cr.org_unit_name,
    JSON_ARRAY(t.id) AS included_task_ids,
    1 AS included_task_count,
    cr.category_scores,
    cr.raw_score,
    cr.weighted_score,
    cr.fair_adjusted_score,
    cr.deduction_count,
    cr.bonus_count,
    cr.total_deduction AS total_deduction_score,
    cr.total_bonus AS total_bonus_score,
    (SELECT COUNT(*) + 1
     FROM v5_class_records cr2
     WHERE cr2.task_id = t.id AND cr2.fair_adjusted_score > cr.fair_adjusted_score) AS rank_in_scope,
    0 AS rank_change,
    CASE WHEN t.status = 'PUBLISHED' THEN 'PUBLISHED' ELSE 'DRAFT' END AS publish_status,
    t.published_at
FROM v5_inspection_tasks t
JOIN v5_inspection_projects p ON t.project_id = p.id
JOIN v5_class_records cr ON t.id = cr.task_id;
```

### 7.4 迁移验证脚本

```sql
-- ============================================================
-- V4 → V5 迁移验证脚本
-- ============================================================

-- 1. 检查模板数量
SELECT 'templates' AS table_name,
    (SELECT COUNT(*) FROM inspection_templates WHERE deleted = 0) AS v4_count,
    (SELECT COUNT(*) FROM v5_inspection_templates) AS v5_count,
    CASE WHEN (SELECT COUNT(*) FROM inspection_templates WHERE deleted = 0) =
              (SELECT COUNT(*) FROM v5_inspection_templates)
         THEN 'PASS' ELSE 'FAIL' END AS status;

-- 2. 检查类别数量
SELECT 'categories' AS table_name,
    (SELECT COUNT(*) FROM template_categories WHERE deleted = 0) AS v4_count,
    (SELECT COUNT(*) FROM v5_template_categories) AS v5_count,
    CASE WHEN (SELECT COUNT(*) FROM template_categories WHERE deleted = 0) =
              (SELECT COUNT(*) FROM v5_template_categories)
         THEN 'PASS' ELSE 'FAIL' END AS status;

-- 3. 检查扣分项数量
SELECT 'score_items' AS table_name,
    (SELECT COUNT(*) FROM template_items WHERE deleted = 0) AS v4_count,
    (SELECT COUNT(*) FROM v5_score_items) AS v5_count,
    CASE WHEN (SELECT COUNT(*) FROM template_items WHERE deleted = 0) =
              (SELECT COUNT(*) FROM v5_score_items)
         THEN 'PASS' ELSE 'FAIL' END AS status;

-- 4. 检查项目数量
SELECT 'projects' AS table_name,
    (SELECT COUNT(*) FROM inspection_plans WHERE deleted = 0) AS v4_count,
    (SELECT COUNT(*) FROM v5_inspection_projects) AS v5_count,
    CASE WHEN (SELECT COUNT(*) FROM inspection_plans WHERE deleted = 0) =
              (SELECT COUNT(*) FROM v5_inspection_projects)
         THEN 'PASS' ELSE 'FAIL' END AS status;

-- 5. 检查任务数量
SELECT 'tasks' AS table_name,
    (SELECT COUNT(*) FROM inspection_sessions WHERE deleted = 0) AS v4_count,
    (SELECT COUNT(*) FROM v5_inspection_tasks) AS v5_count,
    CASE WHEN (SELECT COUNT(*) FROM inspection_sessions WHERE deleted = 0) =
              (SELECT COUNT(*) FROM v5_inspection_tasks)
         THEN 'PASS' ELSE 'FAIL' END AS status;

-- 6. 检查班级记录数量
SELECT 'class_records' AS table_name,
    (SELECT COUNT(*) FROM session_class_records WHERE deleted = 0) AS v4_count,
    (SELECT COUNT(*) FROM v5_class_records) AS v5_count,
    CASE WHEN (SELECT COUNT(*) FROM session_class_records WHERE deleted = 0) =
              (SELECT COUNT(*) FROM v5_class_records)
         THEN 'PASS' ELSE 'FAIL' END AS status;

-- 7. 检查扣分明细数量
SELECT 'deductions' AS table_name,
    (SELECT COUNT(*) FROM class_deductions WHERE deleted = 0) AS v4_count,
    (SELECT COUNT(*) FROM v5_score_details) AS v5_count,
    CASE WHEN (SELECT COUNT(*) FROM class_deductions WHERE deleted = 0) =
              (SELECT COUNT(*) FROM v5_score_details)
         THEN 'PASS' ELSE 'FAIL' END AS status;

-- 8. 检查申诉数量
SELECT 'appeals' AS table_name,
    (SELECT COUNT(*) FROM appeals WHERE deleted = 0) AS v4_count,
    (SELECT COUNT(*) FROM v5_appeals) AS v5_count,
    CASE WHEN (SELECT COUNT(*) FROM appeals WHERE deleted = 0) =
              (SELECT COUNT(*) FROM v5_appeals)
         THEN 'PASS' ELSE 'FAIL' END AS status;

-- 9. 抽样验证班级得分一致性
SELECT
    'score_consistency' AS check_type,
    scr.id AS v4_record_id,
    scr.total_score AS v4_score,
    cr.raw_score AS v5_score,
    CASE WHEN ABS(COALESCE(scr.total_score, 100) - cr.raw_score) < 0.01
         THEN 'PASS' ELSE 'FAIL' END AS status
FROM session_class_records scr
JOIN v5_class_records cr ON scr.id = cr.id
WHERE scr.deleted = 0
LIMIT 100;
```

### 7.5 回滚方案

```sql
-- ============================================================
-- V5 回滚方案 (紧急情况使用)
-- ============================================================

-- 如果迁移出现问题，执行以下脚本回滚

-- 1. 删除V5数据 (按依赖顺序)
DELETE FROM v5_ranking_snapshots;
DELETE FROM v5_period_summaries;
DELETE FROM v5_daily_summaries;
DELETE FROM v5_appeal_approvals;
DELETE FROM v5_appeals;
DELETE FROM v5_corrective_records;
DELETE FROM v5_corrective_actions;
DELETE FROM v5_mixed_dormitory_allocations;
DELETE FROM v5_evidences;
DELETE FROM v5_score_details;
DELETE FROM v5_target_records;
DELETE FROM v5_class_records;
DELETE FROM v5_inspection_tasks;
DELETE FROM v5_project_categories;
DELETE FROM v5_inspection_projects;
DELETE FROM v5_score_items;
DELETE FROM v5_template_categories;
DELETE FROM v5_inspection_templates;
DELETE FROM v5_inspection_series;

-- 2. 重置自增ID (可选)
-- ALTER TABLE v5_xxx AUTO_INCREMENT = 1;

-- 3. 前端回滚
-- 将前端路由切换回V4页面
-- /inspection/* → V4 组件
```

---

## 八、实施计划与总结

### 8.1 实施阶段

| 阶段 | 内容 | 产出物 | 依赖 |
|------|------|--------|------|
| **Phase 1: 基础架构** | 创建V5数据库表、领域模型、仓储接口 | 数据库DDL、领域层代码 | 无 |
| **Phase 2: 核心服务** | 权重计算引擎、汇总服务、定时任务 | 应用服务层代码 | Phase 1 |
| **Phase 3: API层** | REST控制器、DTO、权限配置 | API文档、接口代码 | Phase 2 |
| **Phase 4: 前端PC端** | 项目向导、检查执行、排名展示等页面 | Vue组件 | Phase 3 |
| **Phase 5: 前端移动端** | Vant适配的移动端检查页面 | 移动端组件 | Phase 3 |
| **Phase 6: 数据迁移** | 执行迁移脚本、验证数据完整性 | 迁移报告 | Phase 1-5 |
| **Phase 7: 测试验收** | 功能测试、性能测试、用户验收 | 测试报告 | Phase 6 |
| **Phase 8: 上线部署** | 正式切换、监控、运维 | 上线文档 | Phase 7 |

### 8.2 核心设计亮点

1. **灵活的权重系统**
   - 类别占比权重：支持不同类别配置不同权重
   - 公平权重：DIVIDE (除以人数) 和 BENCHMARK (基准人数) 两种模式
   - 混合宿舍分配：RATIO/AVERAGE/FULL/MAIN四种策略

2. **多维度汇总体系**
   - 汇总维度：按项目、按模板、全校统一
   - 汇总周期：每日/每周/每月/学期
   - 排名快照：历史排名可追溯
   - 检查级别：CLASS/GRADE/DEPARTMENT三级对比范围

3. **完全可配置化**
   - 无系统默认值，所有配置项必须明确选择
   - 打分模式：百分制、纯扣分、纯加分、等级制、合格制
   - 缺失类别处理：排除计算或按满分计
   - 项目级别可配置允许的录入模式

4. **多种录入模式**
   - 按空间录入 (SPACE)：宿舍/教室检查场景
   - 按学生录入 (PERSON)：个人行为记录
   - 按班级录入 (CLASS)：直接班级扣分
   - 按扣分项录入 (ITEM)：批量同类问题
   - 清单模式 (CHECKLIST)：YES/NO/NA逐项检查
   - 自由录入 (FREE)：灵活组合

5. **完整的业务闭环**
   - 检查录入 → 汇总计算 → 排名发布
   - 申诉流程 → 审核处理 → 分数调整
   - 整改工单 → 执行记录 → 复查验收
   - Saga异步编排：自动评级、自动加分、通知推送

6. **加分项独立管理**
   - 独立bonus_items表：手动/自动/导入多来源
   - 自动加分规则：连续满分奖励、月度优秀、进步奖励
   - 有效期管理：加分项可设置生效和失效日期
   - 汇总可控：可配置是否纳入最终汇总计算

7. **纯加分模式 (BONUS_ONLY)**
   - 适用场景：评优检查、好人好事表彰
   - 从起始分(通常为0)开始累加
   - 只加不扣，适合正向激励场景

### 8.3 技术选型

| 领域 | 技术选型 | 说明 |
|------|----------|------|
| 后端框架 | Spring Boot 3.2 | 主流企业级框架 |
| 数据库 | MySQL 8.0 | 支持JSON字段 |
| 缓存 | Redis | 排名缓存、分布式锁 |
| 定时任务 | Spring Scheduler | 汇总生成、定时统计 |
| 前端框架 | Vue 3 + TypeScript | Composition API |
| UI组件 | Element Plus | PC端 |
| 移动端UI | Vant 4 | 移动端检查 |
| 图表 | ECharts | 趋势分析、统计图表 |

### 8.4 文档版本记录

| 版本 | 日期 | 作者 | 说明 |
|------|------|------|------|
| V5.0.0 | 2024-XX-XX | Claude | 初始完整版本 |

---

**文档结束**
