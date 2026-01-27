# 校历管理专业设计方案

> 参考：正方教务、金智教育、清华大学教务系统、教育部《高等学校学籍学历信息管理办法》

## 一、业界标准架构

### 1.1 核心概念层级

```
┌─────────────────────────────────────────────────────────────────┐
│                         学校（School）                           │
└─────────────────────────────────────────────────────────────────┘
                                │
                ┌───────────────┴───────────────┐
                ▼                               ▼
┌───────────────────────────┐     ┌───────────────────────────────┐
│       校历体系             │     │         组织体系               │
│   (Calendar System)       │     │   (Organization System)       │
└───────────────────────────┘     └───────────────────────────────┘
        │                                     │
        ▼                                     ▼
┌───────────────────┐             ┌─────────────────────────────┐
│ • 学年 (Year)      │             │ • 院系 (Department)          │
│ • 学期 (Semester)  │             │ • 专业 (Major)               │
│ • 教学周 (Week)    │             │ • 年级 (Grade)               │
│ • 节次 (Period)    │             │ • 班级 (Class)               │
│ • 校历事件 (Event) │             │ • 学生 (Student)             │
└───────────────────┘             └─────────────────────────────┘
                │                               │
                └───────────────┬───────────────┘
                                ▼
                ┌───────────────────────────────┐
                │         教学体系               │
                │   (Teaching System)           │
                │ • 培养方案 (Curriculum)        │
                │ • 教学任务 (Task)              │
                │ • 课程表 (Schedule)            │
                │ • 考试安排 (Exam)              │
                └───────────────────────────────┘
```

### 1.2 校历与年级的标准关系

```
┌─────────────────────────────────────────────────────────────────┐
│                     全校校历（唯一基准）                          │
│  Academic Calendar - 定义时间框架，全校统一                       │
├─────────────────────────────────────────────────────────────────┤
│  学年 → 学期 → 教学周 → 日期                                     │
│  2025-2026学年 → 第一学期 → 第1周 → 2025-09-01 ~ 2025-09-07     │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │ 引用（Reference）
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     培养方案（按专业+年级）                        │
│  Curriculum Plan - 定义"学什么"                                  │
├─────────────────────────────────────────────────────────────────┤
│  计算机科学2024级培养方案：                                       │
│  - 第1学期：高等数学、程序设计、...                               │
│  - 第2学期：数据结构、离散数学、...                               │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │ 实例化（Instantiate）
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     教学执行计划（按学期）                         │
│  Teaching Execution Plan - 定义"何时学"                          │
├─────────────────────────────────────────────────────────────────┤
│  2025-2026第一学期 计算机1班：                                    │
│  - 高等数学：第1-16周，周一1-2节                                  │
│  - 程序设计：第1-16周，周三3-4节                                  │
└─────────────────────────────────────────────────────────────────┘
                                │
                                │ 执行过程中
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     年级活动安排（补充）                          │
│  Grade Activity - 年级特有活动，不改变校历                        │
├─────────────────────────────────────────────────────────────────┤
│  2024级活动：                                                    │
│  - 军训：第1-2周（教学任务从第3周开始）                           │
│  - 入学教育：第1周周末                                           │
│  2022级活动：                                                    │
│  - 毕业答辩：第15-16周                                           │
│  - 毕业典礼：6月20日                                             │
└─────────────────────────────────────────────────────────────────┘
```

## 二、完整数据模型

### 2.1 校历核心表

```sql
-- 学年表
CREATE TABLE academic_year (
    id BIGINT PRIMARY KEY,
    year_name VARCHAR(50) NOT NULL,           -- 2025-2026学年
    year_code VARCHAR(20) NOT NULL UNIQUE,    -- 2025-2026
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,                 -- 1:正常 0:停用
    remark VARCHAR(500),
    created_at DATETIME,
    updated_at DATETIME
);

-- 学期表
CREATE TABLE semester (
    id BIGINT PRIMARY KEY,
    year_id BIGINT NOT NULL,
    semester_name VARCHAR(50) NOT NULL,       -- 2025-2026学年第一学期
    semester_code VARCHAR(20) NOT NULL,       -- 2025-2026-1
    term_type TINYINT NOT NULL,               -- 1:秋季学期 2:春季学期 3:夏季短学期
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    teaching_week_count INT NOT NULL,         -- 教学周数
    exam_week_count INT DEFAULT 1,            -- 考试周数
    is_current TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    remark VARCHAR(500),
    FOREIGN KEY (year_id) REFERENCES academic_year(id)
);

-- 教学周表
CREATE TABLE teaching_week (
    id BIGINT PRIMARY KEY,
    semester_id BIGINT NOT NULL,
    week_number INT NOT NULL,                 -- 周次：1, 2, 3...
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    week_type TINYINT NOT NULL,               -- 1:教学周 2:考试周 3:假期 4:机动周
    week_label VARCHAR(50),                   -- 显示标签：第1周、考试周、国庆假期
    is_current TINYINT DEFAULT 0,
    remark VARCHAR(200),
    FOREIGN KEY (semester_id) REFERENCES semester(id),
    UNIQUE KEY uk_semester_week (semester_id, week_number)
);

-- 作息时间表（节次）
CREATE TABLE period_config (
    id BIGINT PRIMARY KEY,
    semester_id BIGINT,                       -- NULL表示全校默认
    period_number INT NOT NULL,               -- 节次：1, 2, 3...
    period_name VARCHAR(20) NOT NULL,         -- 第一节、第二节
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    period_type TINYINT DEFAULT 1,            -- 1:上午 2:下午 3:晚上
    FOREIGN KEY (semester_id) REFERENCES semester(id)
);

-- 校历事件表（全校级）
CREATE TABLE calendar_event (
    id BIGINT PRIMARY KEY,
    year_id BIGINT,
    semester_id BIGINT,
    event_name VARCHAR(100) NOT NULL,
    event_type TINYINT NOT NULL,              -- 见下方枚举
    event_level TINYINT DEFAULT 1,            -- 1:全校 2:院系 3:年级
    start_date DATE NOT NULL,
    end_date DATE,
    start_time TIME,
    end_time TIME,
    all_day TINYINT DEFAULT 1,
    location VARCHAR(200),
    description TEXT,
    affect_teaching TINYINT DEFAULT 0,        -- 是否影响教学
    status TINYINT DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME,
    FOREIGN KEY (year_id) REFERENCES academic_year(id),
    FOREIGN KEY (semester_id) REFERENCES semester(id)
);

-- 事件类型枚举
-- 1:开学典礼 2:放假 3:调休上课 4:期中考试 5:期末考试
-- 6:补考 7:毕业典礼 8:运动会 9:校庆 10:其他
```

### 2.2 年级活动表（独立于校历）

```sql
-- 年级活动安排表
CREATE TABLE grade_activity (
    id BIGINT PRIMARY KEY,
    grade_id BIGINT NOT NULL,                 -- 关联年级
    semester_id BIGINT NOT NULL,              -- 关联学期
    activity_name VARCHAR(100) NOT NULL,
    activity_type TINYINT NOT NULL,           -- 见下方枚举
    start_date DATE NOT NULL,
    end_date DATE,
    start_week INT,                           -- 开始周次
    end_week INT,                             -- 结束周次
    affect_teaching TINYINT DEFAULT 0,        -- 是否影响正常教学
    teaching_adjustment TEXT,                 -- 教学调整说明
    location VARCHAR(200),
    description TEXT,
    status TINYINT DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME,
    FOREIGN KEY (grade_id) REFERENCES grade(id),
    FOREIGN KEY (semester_id) REFERENCES semester(id)
);

-- 年级活动类型枚举
-- 1:军训 2:入学教育 3:专业实习 4:社会实践 5:毕业实习
-- 6:毕业设计 7:毕业答辩 8:毕业典礼 9:就业指导 10:其他
```

### 2.3 教学计划相关表

```sql
-- 年级教学周期配置（可选覆盖）
CREATE TABLE grade_teaching_period (
    id BIGINT PRIMARY KEY,
    grade_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    teaching_start_week INT NOT NULL,         -- 该年级教学开始周
    teaching_end_week INT NOT NULL,           -- 该年级教学结束周
    reason VARCHAR(200),                      -- 调整原因：如"军训占用1-2周"
    FOREIGN KEY (grade_id) REFERENCES grade(id),
    FOREIGN KEY (semester_id) REFERENCES semester(id),
    UNIQUE KEY uk_grade_semester (grade_id, semester_id)
);
```

## 三、模块功能设计

### 3.1 校历管理模块（当前页面）

```
校历管理
├── 学年管理
│   ├── 学年列表（卡片展示）
│   ├── 新建/编辑学年
│   └── 设置当前学年
│
├── 学期管理（进入学年后）
│   ├── 学期列表
│   ├── 新建/编辑学期
│   ├── 教学周管理
│   │   ├── 自动生成教学周
│   │   ├── 手动调整周类型
│   │   └── 设置周标签（如"国庆假期"）
│   └── 作息时间配置
│
├── 校历事件管理
│   ├── 事件日历视图
│   ├── 事件列表视图
│   ├── 添加/编辑事件
│   └── 事件导入/导出
│
└── 校历总览
    ├── 学期日历视图（月/学期）
    ├── 周次表格视图
    └── 校历打印/导出
```

### 3.2 年级活动模块（新增或移至年级管理）

```
年级活动管理
├── 年级活动列表
│   ├── 按年级筛选
│   ├── 按学期筛选
│   └── 按活动类型筛选
│
├── 活动安排
│   ├── 新建活动
│   ├── 设置影响教学
│   └── 关联教学调整
│
└── 活动日历
    ├── 年级活动视图
    ├── 与校历叠加显示
    └── 导出年级日程
```

## 四、页面设计调整方案

### 4.1 当前页面问题

| 问题 | 说明 |
|-----|------|
| "年级校历"概念混淆 | 年级没有独立校历，应该是"年级活动" |
| 右侧面板定位不清 | 年级活动应该是补充信息，非核心 |
| 缺少周次管理入口 | 教学周是校历核心，应有明确入口 |
| 作息时间未体现 | 节次配置是排课基础 |

### 4.2 调整后页面结构

```
┌─────────────────────────────────────────────────────────────────┐
│ 校历管理                                      [新建学年] [导出]  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │2025-2026 │ │2024-2025 │ │2023-2024 │ │ + 新建   │          │
│  │  学年    │ │  学年    │ │  学年    │ │   学年   │          │
│  │ 当前     │ │          │ │ 已归档   │ │          │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                              │ 点击进入
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ ← 返回  2025-2026学年                    [编辑学年] [导出校历]  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─ Tab导航 ─────────────────────────────────────────────────┐ │
│  │ [学期管理]  [校历事件]  [作息时间]  [年级活动]             │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌─ 学期管理 ────────────────────────────────────────────────┐ │
│  │                                                           │ │
│  │  学期卡片区                        │  学期详情/周历        │ │
│  │  ┌────────────┐ ┌────────────┐    │                      │ │
│  │  │ 第一学期   │ │ 第二学期   │    │  选中学期的详细信息   │ │
│  │  │ 9/1-1/15   │ │ 2/17-7/10  │    │  • 基本信息          │ │
│  │  │ 18教学周   │ │ 20教学周   │    │  • 教学周列表        │ │
│  │  │ [当前]     │ │            │    │  • 快速操作          │ │
│  │  └────────────┘ └────────────┘    │                      │ │
│  │  [+新建学期]                       │                      │ │
│  │                                                           │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌─ 校历总览（下方） ────────────────────────────────────────┐ │
│  │  [月视图] [学期视图] [周次表格]                           │ │
│  │                                                           │ │
│  │  ┌─────────────────────────────────────────────────────┐ │ │
│  │  │              日历/周次表格视图                       │ │ │
│  │  │                                                     │ │ │
│  │  └─────────────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.3 Tab页详细设计

#### Tab 1: 学期管理
- 左侧：学期卡片列表
- 右侧：选中学期详情
  - 基本信息编辑
  - 教学周管理（表格形式）
  - 自动生成/手动调整周次

#### Tab 2: 校历事件
- 日历视图 + 事件列表
- 支持拖拽添加事件
- 事件分类筛选

#### Tab 3: 作息时间
- 节次配置表格
- 支持按学期覆盖
- 预设模板（夏季/冬季作息）

#### Tab 4: 年级活动
- 年级活动列表
- 按年级/类型筛选
- 活动与校历叠加显示

## 五、实施计划

### 阶段一：重构页面结构
1. 保持学年列表入口
2. 进入学年后改为Tab布局
3. 将"年级校历"改为"年级活动"

### 阶段二：完善学期管理
1. 添加教学周管理功能
2. 支持周类型调整
3. 支持周标签设置

### 阶段三：添加作息时间
1. 新建作息时间配置表
2. 添加节次管理界面
3. 支持学期覆盖

### 阶段四：年级活动功能
1. 新建年级活动表
2. 实现年级活动管理界面
3. 与校历日历叠加显示

## 六、与其他模块的关系

```
校历管理
    │
    ├──→ 排课管理：引用学期、教学周、节次
    │
    ├──→ 考试管理：引用学期、考试周
    │
    ├──→ 成绩管理：引用学期
    │
    └──→ 教学任务：引用学期、教学周范围

年级活动
    │
    ├──→ 教学任务：年级教学周期覆盖
    │
    └──→ 学生管理：年级日程查询
```

## 七、总结

| 项目 | 原设计 | 调整后 |
|-----|-------|-------|
| 核心定位 | 模糊 | 校历=全校时间框架 |
| 年级关系 | "年级校历" | "年级活动"（补充） |
| 功能完整性 | 缺少周次、作息 | 完整的校历管理 |
| 页面布局 | 三栏平铺 | 学年入口 + Tab详情 |
| 扩展性 | 一般 | 良好，符合业界标准 |

---

**确认后，我将按此方案进行实施。**
