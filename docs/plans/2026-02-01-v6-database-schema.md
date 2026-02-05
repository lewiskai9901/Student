# V6 通用检查系统 - 数据库设计

> **版本**: 1.0
> **日期**: 2026-02-01
> **关联文档**: [v6-universal-inspection-system.md](./2026-02-01-v6-universal-inspection-system.md)

---

## 一、实体类型配置

### 1.1 实体类型表 (entity_types)

```sql
-- 实体类型配置表（系统级）
-- 定义系统中可用的组织、场所、用户类型
CREATE TABLE entity_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 分类：ORGANIZATION/SPACE/USER
    category ENUM('ORGANIZATION', 'SPACE', 'USER') NOT NULL,

    -- 类型代码和名称
    type_code VARCHAR(50) NOT NULL,           -- 如: DEPARTMENT, CLASS, DORMITORY, STUDENT
    type_name VARCHAR(100) NOT NULL,          -- 如: 系部, 班级, 宿舍, 学生

    -- 层级关系（用于组织类型）
    parent_type_code VARCHAR(50),             -- 父类型，如CLASS的父类型是DEPARTMENT
    is_leaf TINYINT DEFAULT 0,                -- 是否叶子节点（无子级）

    -- 显示配置
    icon VARCHAR(50),                         -- 图标
    color VARCHAR(20),                        -- 颜色

    -- 属性定义（JSON Schema格式）
    attributes_schema JSON,
    /*
      示例:
      {
        "member_count": {"type": "integer", "label": "人数"},
        "capacity": {"type": "integer", "label": "容量"},
        "enrollment_year": {"type": "integer", "label": "入学年份"}
      }
    */

    -- 加权属性（用于汇总时的权重计算）
    weight_attribute VARCHAR(50),             -- 如: member_count, capacity

    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    is_system TINYINT DEFAULT 0,              -- 系统预置

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_type_code (type_code),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实体类型配置表';

-- 预置数据：学校场景
INSERT INTO entity_types (category, type_code, type_name, parent_type_code, is_leaf, icon, weight_attribute, sort_order, is_system) VALUES
-- 组织类型
('ORGANIZATION', 'SCHOOL', '学校', NULL, 0, 'school', NULL, 1, 1),
('ORGANIZATION', 'DEPARTMENT', '系部', 'SCHOOL', 0, 'building', 'member_count', 2, 1),
('ORGANIZATION', 'CLASS', '班级', 'DEPARTMENT', 1, 'users', 'member_count', 3, 1),
-- 场所类型
('SPACE', 'BUILDING', '楼栋', NULL, 0, 'building', NULL, 10, 1),
('SPACE', 'DORMITORY', '宿舍', 'BUILDING', 1, 'home', 'capacity', 11, 1),
('SPACE', 'CLASSROOM', '教室', 'BUILDING', 1, 'book', 'capacity', 12, 1),
('SPACE', 'OFFICE', '办公室', 'BUILDING', 1, 'briefcase', 'area', 13, 1),
-- 用户类型
('USER', 'STUDENT', '学生', NULL, 1, 'user', NULL, 20, 1),
('USER', 'TEACHER', '教师', NULL, 1, 'user-tie', NULL, 21, 1);
```

### 1.2 动态分组表 (entity_groups)

```sql
-- 动态分组表
-- 支持按属性条件创建虚拟分组
CREATE TABLE entity_groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    group_code VARCHAR(50) NOT NULL,
    group_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),

    -- 分组类型
    group_type ENUM('DYNAMIC', 'STATIC') DEFAULT 'DYNAMIC',
    -- DYNAMIC: 根据条件动态计算
    -- STATIC: 手动指定成员

    -- 目标实体类型
    entity_category ENUM('ORGANIZATION', 'SPACE', 'USER') NOT NULL,
    entity_type_code VARCHAR(50),             -- 可选，限定具体类型

    -- 筛选条件（DYNAMIC类型使用）
    filter_conditions JSON,
    /*
      示例:
      {
        "conditions": [
          {"field": "attributes.enrollment_year", "operator": "=", "value": 2024},
          {"field": "attributes.major", "operator": "=", "value": "软件工程"}
        ],
        "logic": "AND"
      }
    */

    -- 静态成员（STATIC类型使用）
    static_member_ids JSON,                   -- [1, 2, 3, ...]

    -- 缓存（DYNAMIC类型使用）
    cached_member_ids JSON,
    cached_member_count INT DEFAULT 0,
    cached_at DATETIME,

    -- 自动刷新配置
    auto_refresh TINYINT DEFAULT 1,
    refresh_interval ENUM('HOURLY', 'DAILY', 'WEEKLY') DEFAULT 'DAILY',

    -- 归属
    org_unit_id BIGINT,                       -- 创建者所属部门
    created_by BIGINT,

    is_enabled TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_group_code (group_code),
    INDEX idx_entity_category (entity_category),
    INDEX idx_org_unit (org_unit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态分组表';
```

---

## 二、检查模板

### 2.1 检查模板表 (inspection_templates)

```sql
-- 检查模板表
CREATE TABLE inspection_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    template_code VARCHAR(50) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),

    -- 适用的检查目标类型
    applicable_categories JSON NOT NULL,      -- ["ORGANIZATION", "SPACE", "USER"]
    applicable_types JSON,                    -- ["DORMITORY", "CLASSROOM"] 可选限定

    -- 默认打分配置
    default_scoring_mode ENUM('DEDUCTION', 'ADDITION', 'BASE_SCORE', 'RATING', 'GRADE', 'PASS_FAIL', 'CHECKLIST', 'HYBRID') DEFAULT 'BASE_SCORE',
    default_base_score DECIMAL(6,2) DEFAULT 100,
    default_min_score DECIMAL(6,2) DEFAULT 0,
    default_allow_negative TINYINT DEFAULT 0,

    -- 默认分数计算公式
    default_formula VARCHAR(500),             -- 如: base - SUM(deduction * weight)

    -- 可见性
    visibility ENUM('PRIVATE', 'DEPARTMENT', 'PUBLIC') DEFAULT 'PRIVATE',

    -- 版本控制
    version INT DEFAULT 1,
    is_latest TINYINT DEFAULT 1,
    parent_template_id BIGINT,                -- 基于哪个模板创建

    -- 统计
    use_count INT DEFAULT 0,

    -- 归属
    org_unit_id BIGINT,
    created_by BIGINT,

    is_enabled TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_template_code (template_code),
    INDEX idx_visibility (visibility),
    INDEX idx_org_unit (org_unit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查模板表';
```

### 2.2 检查类别表 (inspection_categories)

```sql
-- 检查类别表
CREATE TABLE inspection_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    template_id BIGINT NOT NULL,

    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),

    -- 权重（0.00-1.00，同一模板内所有类别权重之和应为1）
    weight DECIMAL(5,4) DEFAULT 0.25,

    -- 显示配置
    icon VARCHAR(50),
    color VARCHAR(20),

    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_template_category (template_id, category_code),
    INDEX idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查类别表';
```

### 2.3 检查项表 (inspection_score_items)

```sql
-- 检查项表
CREATE TABLE inspection_score_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    template_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,

    item_code VARCHAR(50) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),

    -- 适用目标（继承模板或单独指定）
    applicable_categories JSON,               -- ["ORGANIZATION", "SPACE", "USER"]
    applicable_types JSON,                    -- 具体类型

    -- 作用范围
    scope ENUM('WHOLE', 'INDIVIDUAL') DEFAULT 'WHOLE',
    -- WHOLE: 针对目标整体
    -- INDIVIDUAL: 需关联到具体个体

    individual_type VARCHAR(50),              -- 当scope=INDIVIDUAL时，关联的个体类型: USER/SPACE

    -- 打分配置
    scoring_mode ENUM('DEDUCTION', 'ADDITION', 'RATING', 'GRADE', 'CHECKLIST') DEFAULT 'DEDUCTION',
    score DECIMAL(6,2) NOT NULL,              -- 分值
    score_unit VARCHAR(20),                   -- 单位描述: 分, 分/人, 分/件

    -- 评级配置（当scoring_mode=GRADE时）
    grade_options JSON,
    /*
      示例:
      [
        {"code": "A", "name": "优秀", "score": 100},
        {"code": "B", "name": "良好", "score": 85},
        {"code": "C", "name": "合格", "score": 70},
        {"code": "D", "name": "不合格", "score": 50}
      ]
    */

    -- 证据要求
    require_evidence TINYINT DEFAULT 0,
    require_remark TINYINT DEFAULT 0,

    -- 类内权重（可选，用于更细粒度的权重控制）
    item_weight DECIMAL(5,4) DEFAULT 1.0,

    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_template_item (template_id, item_code),
    INDEX idx_template_id (template_id),
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查项表';
```

---

## 三、检查项目

### 3.1 检查项目表 (inspection_projects)

```sql
-- 检查项目表
CREATE TABLE inspection_projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    project_code VARCHAR(50) NOT NULL,
    project_name VARCHAR(200) NOT NULL,
    description TEXT,

    -- 关联模板
    template_id BIGINT NOT NULL,
    template_snapshot JSON,                   -- 模板快照（创建时复制，后续模板修改不影响）

    -- 有效期间
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    -- 检查频率
    frequency ENUM('DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM') DEFAULT 'WEEKLY',
    frequency_config JSON,
    /*
      WEEKLY示例: {"days": [1, 3, 5]}  // 周一、三、五
      MONTHLY示例: {"days": [1, 15]}   // 每月1日、15日
      CUSTOM示例: {"dates": ["2024-03-01", "2024-03-15"]}
    */

    -- 检查范围配置
    root_target_type ENUM('ORGANIZATION', 'SPACE', 'USER', 'GROUP') NOT NULL,
    root_target_ids JSON NOT NULL,            -- 根目标ID列表

    -- 检查深度配置（嵌套检查）
    inspection_depth JSON NOT NULL,
    /*
      示例:
      {
        "level_0": {
          "include_self": false,
          "include_spaces": false,
          "include_users": false,
          "include_children": true,
          "scoring_mode": null
        },
        "level_1": {
          "include_self": false,
          "include_spaces": true,
          "include_users": false,
          "include_children": false,
          "scoring_mode": "BASE_SCORE"
        },
        "max_depth": 2
      }
    */

    -- 打分配置（覆盖模板默认）
    scoring_mode ENUM('DEDUCTION', 'ADDITION', 'BASE_SCORE', 'RATING', 'GRADE', 'PASS_FAIL', 'CHECKLIST', 'HYBRID'),
    base_score DECIMAL(6,2),
    min_score DECIMAL(6,2),
    score_formula VARCHAR(500),

    -- 类别权重覆盖
    category_weights JSON,                    -- {"HYGIENE": 0.4, "SAFETY": 0.35, "ORDER": 0.25}

    -- 加权配置
    weight_enabled TINYINT DEFAULT 0,
    space_weight_mode ENUM('NONE', 'CAPACITY', 'OCCUPANCY', 'FIXED') DEFAULT 'NONE',
    org_weight_mode ENUM('NONE', 'MEMBER_COUNT', 'SPACE_COUNT', 'FIXED') DEFAULT 'NONE',
    user_weight_mode ENUM('NONE', 'FIXED') DEFAULT 'NONE',

    -- 公平权重
    fair_weight_enabled TINYINT DEFAULT 0,
    fair_weight_mode ENUM('NONE', 'NORMALIZE', 'BENCHMARK') DEFAULT 'NONE',
    fair_weight_benchmark INT DEFAULT 5,

    -- 状态
    status ENUM('DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED') DEFAULT 'DRAFT',

    -- 统计缓存
    total_tasks INT DEFAULT 0,
    completed_tasks INT DEFAULT 0,

    -- 归属
    org_unit_id BIGINT,
    created_by BIGINT,

    deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_project_code (project_code),
    INDEX idx_status (status),
    INDEX idx_date_range (start_date, end_date),
    INDEX idx_org_unit (org_unit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查项目表';
```

---

## 四、检查任务与记录

### 4.1 检查任务表 (inspection_tasks)

```sql
-- 检查任务表
-- 代表一次具体的检查（如：2024-03-01的宿舍检查）
CREATE TABLE inspection_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    project_id BIGINT NOT NULL,

    task_code VARCHAR(50) NOT NULL,
    task_name VARCHAR(200),

    -- 检查日期
    scheduled_date DATE NOT NULL,

    -- 检查时间窗口
    start_time TIME,
    end_time TIME,

    -- 状态
    status ENUM('SCHEDULED', 'IN_PROGRESS', 'SUBMITTED', 'REVIEWED', 'PUBLISHED') DEFAULT 'SCHEDULED',
    /*
      SCHEDULED: 已计划，待开始
      IN_PROGRESS: 进行中
      SUBMITTED: 已提交，待审核
      REVIEWED: 已审核
      PUBLISHED: 已发布（学生可见）
    */

    -- 检查员分配
    inspector_ids JSON,                       -- 分配的检查员ID列表

    -- 统计
    total_targets INT DEFAULT 0,
    completed_targets INT DEFAULT 0,
    skipped_targets INT DEFAULT 0,

    -- 时间记录
    started_at DATETIME,
    submitted_at DATETIME,
    reviewed_at DATETIME,
    published_at DATETIME,

    -- 审核
    reviewer_id BIGINT,
    review_remark VARCHAR(500),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_task_code (task_code),
    INDEX idx_project_id (project_id),
    INDEX idx_scheduled_date (scheduled_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查任务表';
```

### 4.2 检查目标表 (inspection_targets)

```sql
-- 检查目标表
-- 任务中每个待检查的目标
CREATE TABLE inspection_targets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    task_id BIGINT NOT NULL,

    -- 检查目标
    target_category ENUM('ORGANIZATION', 'SPACE', 'USER') NOT NULL,
    target_type_code VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    target_name VARCHAR(200),

    -- 层级路径（用于嵌套检查的导航和归属）
    target_path JSON,                         -- ["经信系", "软件1班", "宿舍101"]
    target_path_ids JSON,                     -- [1, 101, 1001]
    depth_level INT DEFAULT 0,                -- 在检查深度中的层级

    -- 归属组织（用于汇总）
    belong_org_id BIGINT,
    belong_org_name VARCHAR(200),
    belong_org_path JSON,

    -- 目标属性快照（用于加权计算）
    target_attributes JSON,                   -- {"capacity": 8, "occupancy": 6}
    target_weight DECIMAL(8,2) DEFAULT 1.0,   -- 计算出的权重

    -- 检查状态
    status ENUM('PENDING', 'LOCKED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED') DEFAULT 'PENDING',

    -- 锁定信息（防止并发）
    locked_by BIGINT,
    locked_at DATETIME,
    lock_expires_at DATETIME,

    -- 检查结果
    scoring_mode VARCHAR(20),                 -- 实际使用的打分模式
    result_data JSON,                         -- 检查结果数据
    /*
      BASE_SCORE模式:
      {
        "base_score": 100,
        "total_deduction": 7,
        "total_bonus": 0,
        "final_score": 93,
        "category_scores": {
          "HYGIENE": {"deduction": 5, "weighted_deduction": 2},
          "SAFETY": {"deduction": 0, "weighted_deduction": 0},
          "ORDER": {"deduction": 2, "weighted_deduction": 0.5}
        }
      }
    */

    raw_score DECIMAL(6,2),                   -- 原始得分
    weighted_score DECIMAL(6,2),              -- 加权后得分
    result_summary VARCHAR(100),              -- 结果摘要: "93分", "B(良好)"

    -- 检查人
    inspector_id BIGINT,
    inspector_name VARCHAR(50),

    -- 时间
    started_at DATETIME,
    completed_at DATETIME,

    -- 跳过原因
    skip_reason VARCHAR(200),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_task_target (task_id, target_category, target_id),
    INDEX idx_task_id (task_id),
    INDEX idx_target (target_category, target_id),
    INDEX idx_belong_org (belong_org_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查目标表';
```

### 4.3 检查明细表 (inspection_details)

```sql
-- 检查明细表
-- 记录每个检查项的具体扣分/加分情况
CREATE TABLE inspection_details (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    target_record_id BIGINT NOT NULL,         -- inspection_targets.id

    -- 检查项信息
    category_id BIGINT NOT NULL,
    category_code VARCHAR(50),
    category_name VARCHAR(100),

    item_id BIGINT NOT NULL,
    item_code VARCHAR(50),
    item_name VARCHAR(100),

    -- 作用范围
    scope ENUM('WHOLE', 'INDIVIDUAL') NOT NULL,

    -- 关联个体（当scope=INDIVIDUAL时）
    individual_type VARCHAR(50),
    individual_id BIGINT,
    individual_name VARCHAR(100),

    -- 打分
    scoring_mode VARCHAR(20) NOT NULL,
    score DECIMAL(6,2) NOT NULL,              -- 扣分为负，加分为正
    quantity INT DEFAULT 1,                   -- 数量（如：迟到3人次）
    total_score DECIMAL(6,2),                 -- score × quantity

    -- 评级结果（当scoring_mode=GRADE时）
    grade_code VARCHAR(10),
    grade_name VARCHAR(50),

    -- 清单结果（当scoring_mode=CHECKLIST时）
    checklist_checked TINYINT,

    -- 备注和证据
    remark VARCHAR(500),
    evidence_urls JSON,                       -- 证据图片URL列表

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_target_record (target_record_id),
    INDEX idx_item (item_id),
    INDEX idx_individual (individual_type, individual_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查明细表';
```

---

## 五、汇总与排名

### 5.1 检查汇总表 (inspection_summaries)

```sql
-- 检查汇总表
-- 按周期汇总检查结果
CREATE TABLE inspection_summaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    project_id BIGINT NOT NULL,

    -- 汇总周期
    period_type ENUM('DAILY', 'WEEKLY', 'MONTHLY', 'SEMESTER', 'YEARLY') NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    period_label VARCHAR(50),                 -- 如: "2024年第10周", "2024年3月"

    -- 汇总目标
    target_category ENUM('ORGANIZATION', 'SPACE', 'USER') NOT NULL,
    target_type_code VARCHAR(50),
    target_id BIGINT NOT NULL,
    target_name VARCHAR(200),
    target_path JSON,

    -- 上级组织（用于层级汇总）
    parent_org_id BIGINT,
    parent_org_name VARCHAR(200),

    -- 检查统计
    check_count INT DEFAULT 0,                -- 被检查次数
    completed_count INT DEFAULT 0,            -- 完成的检查次数
    skipped_count INT DEFAULT 0,              -- 跳过的次数

    -- 原始得分
    total_raw_score DECIMAL(10,2) DEFAULT 0,
    avg_raw_score DECIMAL(6,2),
    min_raw_score DECIMAL(6,2),
    max_raw_score DECIMAL(6,2),

    -- 加权得分
    target_weight DECIMAL(8,2) DEFAULT 1.0,   -- 目标权重
    fair_weight_factor DECIMAL(6,4) DEFAULT 1.0, -- 公平权重系数
    total_weighted_score DECIMAL(10,2) DEFAULT 0,
    avg_weighted_score DECIMAL(6,2),

    -- 类别得分明细
    category_scores JSON,
    /*
      {
        "HYGIENE": {"avg": 92.5, "weight": 0.4},
        "SAFETY": {"avg": 98.0, "weight": 0.35},
        "ORDER": {"avg": 88.0, "weight": 0.25}
      }
    */

    -- 排名
    ranking INT,
    ranking_total INT,                        -- 参与排名的总数
    ranking_change INT,                       -- 相比上期变化
    ranking_percentile DECIMAL(5,2),          -- 百分位 (前X%)

    -- 趋势
    score_change DECIMAL(6,2),                -- 相比上期分数变化
    trend ENUM('UP', 'DOWN', 'STABLE'),

    -- 计算时间
    calculated_at DATETIME,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_summary (project_id, period_type, period_start, target_category, target_id),
    INDEX idx_project_period (project_id, period_type, period_start),
    INDEX idx_target (target_category, target_id),
    INDEX idx_ranking (project_id, period_type, period_start, ranking)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查汇总表';
```

---

## 六、其他支撑表

### 6.1 打分策略配置表 (scoring_strategies)

```sql
-- 打分策略配置表
CREATE TABLE scoring_strategies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    strategy_code VARCHAR(50) NOT NULL,
    strategy_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),

    -- 策略类型
    strategy_type ENUM('DEDUCTION', 'ADDITION', 'BASE_SCORE', 'RATING', 'GRADE', 'PASS_FAIL', 'CHECKLIST') NOT NULL,

    -- 配置
    config JSON,
    /*
      BASE_SCORE: {"base": 100, "min": 0, "allow_negative": false}
      RATING: {"min": 1, "max": 10, "step": 1}
      GRADE: {"grades": [{"code": "A", "name": "优秀", "min_score": 90}, ...]}
    */

    -- 完成条件
    completion_rules JSON,
    /*
      {
        "require_all_items": false,
        "require_confirm_if_empty": true,
        "min_items_required": 0
      }
    */

    -- 结果格式
    result_format VARCHAR(100),               -- 如: "{score}分", "{grade}({grade_name})"

    is_system TINYINT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_strategy_code (strategy_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打分策略配置表';

-- 预置数据
INSERT INTO scoring_strategies (strategy_code, strategy_name, strategy_type, config, result_format, is_system) VALUES
('PURE_DEDUCTION', '纯扣分制', 'DEDUCTION', '{"accumulate": true}', '累计扣{total}分', 1),
('BASE_100', '百分制', 'BASE_SCORE', '{"base": 100, "min": 0}', '{score}分', 1),
('RATING_5', '五分制', 'RATING', '{"min": 1, "max": 5}', '{score}/5分', 1),
('RATING_10', '十分制', 'RATING', '{"min": 1, "max": 10}', '{score}/10分', 1),
('GRADE_ABCD', '四级评级', 'GRADE', '{"grades": [{"code":"A","name":"优秀","min":90},{"code":"B","name":"良好","min":75},{"code":"C","name":"合格","min":60},{"code":"D","name":"不合格","min":0}]}', '{grade}({grade_name})', 1),
('PASS_FAIL', '通过制', 'PASS_FAIL', '{}', '{result}', 1),
('CHECKLIST', '清单制', 'CHECKLIST', '{}', '{checked}/{total}项({rate}%)', 1);
```

### 6.2 证据附件表 (inspection_evidences)

```sql
-- 证据附件表
CREATE TABLE inspection_evidences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 关联
    detail_id BIGINT,                         -- inspection_details.id
    target_record_id BIGINT,                  -- inspection_targets.id（整体证据）

    -- 文件信息
    file_name VARCHAR(200) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(50),                    -- image/jpeg, image/png, video/mp4

    -- 元数据
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    upload_by BIGINT,

    -- GPS信息（移动端拍照）
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_detail (detail_id),
    INDEX idx_target_record (target_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='证据附件表';
```

---

## 七、索引优化建议

```sql
-- 针对常见查询场景的复合索引

-- 1. 查询某项目某日期的任务
CREATE INDEX idx_project_date ON inspection_tasks(project_id, scheduled_date);

-- 2. 查询某组织的检查记录
CREATE INDEX idx_belong_org_date ON inspection_targets(belong_org_id, created_at);

-- 3. 查询某用户的扣分记录
CREATE INDEX idx_individual ON inspection_details(individual_type, individual_id, created_at);

-- 4. 汇总排名查询
CREATE INDEX idx_summary_ranking ON inspection_summaries(project_id, period_type, period_start, target_category, ranking);
```

---

**文档版本**: 1.0
**最后更新**: 2026-02-01
