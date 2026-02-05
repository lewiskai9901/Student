# V5 重构方案 - 数据库表结构

> **版本**: 3.0
> **日期**: 2026-01-31
> **关联文档**: [V5_ARCHITECTURE.md](./V5_ARCHITECTURE.md)

---

## 一、权限系统表

### 1.1 数据范围类型配置表

```sql
CREATE TABLE data_scope_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scope_code VARCHAR(50) NOT NULL,
    scope_name VARCHAR(100) NOT NULL,
    scope_level INT DEFAULT 0,
    calc_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    is_enabled TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_scope_code (scope_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据范围类型配置表';

-- 初始数据
INSERT INTO data_scope_types (scope_code, scope_name, scope_level, calc_type, description, sort_order) VALUES
('ALL', '全部数据', 100, 'NONE', '可访问系统中所有数据', 1),
('DEPARTMENT_AND_BELOW', '本部门及下级', 80, 'USER_ORG_TREE', '可访问用户所属部门及下级部门的数据', 2),
('DEPARTMENT', '仅本部门', 60, 'USER_ORG', '只能访问用户所属部门的数据', 3),
('CUSTOM', '自定义范围', 40, 'CUSTOM_CONFIG', '管理员配置的指定范围', 4),
('SELF', '仅本人', 20, 'CREATOR', '只能访问自己创建或负责的数据', 5);
```

### 1.2 数据模块配置表

```sql
CREATE TABLE data_modules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_code VARCHAR(50) NOT NULL,
    module_name VARCHAR(100) NOT NULL,
    domain_code VARCHAR(50),
    domain_name VARCHAR(100),
    filter_fields JSON,
    main_table VARCHAR(100),
    enable_data_permission TINYINT DEFAULT 1,
    description VARCHAR(500),
    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_module_code (module_code),
    INDEX idx_domain_code (domain_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据模块配置表';

-- 初始数据
INSERT INTO data_modules (module_code, module_name, domain_code, domain_name, filter_fields, main_table, sort_order) VALUES
-- Organization 领域
('student', '学生信息', 'organization', '组织管理', '{"org_unit":"org_unit_id","class":"class_id","creator":"created_by"}', 'students', 101),
('school_class', '班级管理', 'organization', '组织管理', '{"org_unit":"org_unit_id"}', 'classes', 102),
('org_unit', '组织架构', 'organization', '组织管理', '{"org_unit":"id"}', 'org_units', 103),
-- Space 领域
('dormitory_building', '楼栋管理', 'space', '场所管理', '{"org_unit":"org_unit_id"}', 'space', 201),
('dormitory_room', '房间管理', 'space', '场所管理', '{"org_unit":"org_unit_id","building":"building_id"}', 'space', 202),
('dormitory_checkin', '入住管理', 'space', '场所管理', '{"org_unit":"org_unit_id","class":"class_id"}', 'space_occupants', 203),
-- Inspection 领域
('inspection_template', '检查模板', 'inspection', '量化检查', '{"org_unit":"org_unit_id","creator":"created_by"}', 'inspection_templates', 301),
('inspection_project', '检查项目', 'inspection', '量化检查', '{"org_unit":"org_unit_id","creator":"created_by"}', 'inspection_projects', 302),
('inspection_task', '检查任务', 'inspection', '量化检查', '{"org_unit":"org_unit_id"}', 'inspection_tasks', 303),
('inspection_record', '检查记录', 'inspection', '量化检查', '{"org_unit":"org_unit_id","class":"class_id"}', 'target_inspection_records', 304),
('inspection_personal', '个人检查记录', 'inspection', '量化检查', '{"student":"target_id"}', 'target_inspection_records', 305),
('inspection_appeal', '申诉管理', 'inspection', '量化检查', '{"org_unit":"org_unit_id","class":"class_id","creator":"created_by"}', 'appeals', 306),
('inspection_summary', '检查汇总', 'inspection', '量化检查', '{"org_unit":"org_unit_id","class":"class_id"}', 'daily_summaries', 307),
('inspection_corrective', '整改管理', 'inspection', '量化检查', '{"org_unit":"org_unit_id","class":"class_id"}', 'corrective_orders', 308),
-- Access 领域
('system_user', '用户管理', 'access', '权限管理', '{"org_unit":"org_unit_id"}', 'users', 401),
('system_role', '角色管理', 'access', '权限管理', NULL, 'roles', 402);
```

### 1.3 自定义范围项类型配置表

```sql
CREATE TABLE scope_item_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_type_code VARCHAR(50) NOT NULL,
    item_type_name VARCHAR(100) NOT NULL,
    ref_table VARCHAR(100),
    ref_id_field VARCHAR(100),
    ref_name_field VARCHAR(100),
    ref_parent_field VARCHAR(100),
    applicable_modules JSON,
    filter_field_key VARCHAR(50),
    support_children TINYINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_item_type_code (item_type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义范围项类型配置表';

-- 初始数据
INSERT INTO scope_item_types (item_type_code, item_type_name, ref_table, ref_id_field, ref_name_field, ref_parent_field, applicable_modules, filter_field_key, support_children, sort_order) VALUES
('ORG_UNIT', '部门', 'org_units', 'id', 'name', 'parent_id', '["student","school_class","inspection_record","inspection_template","system_user"]', 'org_unit', 1, 1),
('CLASS', '班级', 'classes', 'id', 'class_name', NULL, '["student","inspection_record","dormitory_checkin"]', 'class', 0, 2),
('GRADE', '年级', 'grades', 'id', 'name', NULL, '["student","school_class"]', 'grade', 0, 3),
('BUILDING', '楼栋', 'space', 'id', 'space_name', NULL, '["dormitory_room","dormitory_checkin"]', 'building', 0, 4),
('MAJOR', '专业', 'majors', 'id', 'name', NULL, '["student","school_class"]', 'major', 0, 5);
```

### 1.4 功能权限表

```sql
CREATE TABLE permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    module_code VARCHAR(50) NOT NULL,
    resource_code VARCHAR(50),
    action_code VARCHAR(50),
    parent_id BIGINT,
    tree_path VARCHAR(500),
    tree_level INT DEFAULT 0,
    permission_type VARCHAR(20) NOT NULL,
    route_path VARCHAR(255),
    component_path VARCHAR(255),
    icon VARCHAR(100),
    api_path VARCHAR(255),
    api_method VARCHAR(10),
    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    is_system TINYINT DEFAULT 0,
    description VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_permission_code (permission_code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_module_code (module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能权限表';
```

### 1.5 角色表

```sql
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    role_type VARCHAR(30) DEFAULT 'CUSTOM',
    role_level INT DEFAULT 100,
    description VARCHAR(500),
    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';
```

### 1.6 角色-功能权限关联表

```sql
CREATE TABLE role_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-功能权限关联表';
```

### 1.7 角色-数据权限配置表

```sql
CREATE TABLE role_data_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    module_code VARCHAR(50) NOT NULL,
    scope_code VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_module (role_id, module_code),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-数据权限配置表';
```

### 1.8 自定义数据范围明细表

```sql
CREATE TABLE role_data_scope_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_data_permission_id BIGINT NOT NULL,
    item_type_code VARCHAR(50) NOT NULL,
    scope_id BIGINT NOT NULL,
    scope_name VARCHAR(100),
    include_children TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_permission_scope (role_data_permission_id, item_type_code, scope_id),
    INDEX idx_role_data_permission_id (role_data_permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义数据范围明细表';
```

### 1.9 用户-角色关联表

```sql
CREATE TABLE user_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    scope_org_unit_id BIGINT,
    expires_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    UNIQUE KEY uk_user_role (user_id, role_id, scope_org_unit_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';
```

---

## 二、检查系统表

### 2.1 检查模板表

```sql
CREATE TABLE inspection_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_code VARCHAR(50) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    visibility ENUM('PRIVATE', 'DEPARTMENT', 'PUBLIC') DEFAULT 'PRIVATE',
    created_by BIGINT NOT NULL,
    org_unit_id BIGINT,
    base_score DECIMAL(10,2) DEFAULT 100.00,
    scoring_mode ENUM('DEDUCTION', 'ADDITION', 'CHECKLIST', 'BONUS_ONLY') DEFAULT 'DEDUCTION',
    target_type ENUM('STUDENT', 'CLASS', 'DORMITORY', 'CLASSROOM') NOT NULL,
    status ENUM('DRAFT', 'ACTIVE', 'ARCHIVED') DEFAULT 'DRAFT',
    version INT DEFAULT 1,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_template_code (template_code),
    INDEX idx_created_by (created_by),
    INDEX idx_org_unit_id (org_unit_id),
    INDEX idx_visibility (visibility)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查模板表';
```

### 2.2 模板类别表

```sql
CREATE TABLE template_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    weight DECIMAL(5,2) DEFAULT 1.00,
    max_score DECIMAL(10,2),
    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    description VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_template_category (template_id, category_code),
    INDEX idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板类别表';
```

### 2.3 类别扣分项表

```sql
CREATE TABLE category_score_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    item_code VARCHAR(50) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    score DECIMAL(10,2) NOT NULL,
    score_type ENUM('DEDUCTION', 'BONUS') DEFAULT 'DEDUCTION',
    require_evidence TINYINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    description VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='类别扣分项表';
```

### 2.4 检查项目表

```sql
CREATE TABLE inspection_projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_code VARCHAR(50) NOT NULL,
    project_name VARCHAR(100) NOT NULL,
    template_id BIGINT,
    template_snapshot JSON,
    inspection_level ENUM('CLASS', 'GRADE', 'DEPARTMENT') DEFAULT 'CLASS',
    target_org_unit_ids JSON,
    start_date DATE NOT NULL,
    end_date DATE,
    semester_id BIGINT,
    entry_mode ENUM('SPACE', 'PERSON', 'CLASS', 'ITEM', 'CHECKLIST') DEFAULT 'SPACE',
    fair_weight_enabled TINYINT DEFAULT 0,
    fair_weight_mode ENUM('DIVIDE', 'BENCHMARK'),
    benchmark_count INT,
    mixed_dormitory_strategy ENUM('RATIO', 'AVERAGE', 'FULL', 'MAIN') DEFAULT 'RATIO',
    auto_generate_summary TINYINT DEFAULT 1,
    summary_cron VARCHAR(50) DEFAULT '0 0 2 * * ?',
    status ENUM('DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED') DEFAULT 'DRAFT',
    org_unit_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_project_code (project_code),
    INDEX idx_org_unit_id (org_unit_id),
    INDEX idx_status (status),
    INDEX idx_semester_id (semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查项目表';
```

### 2.5 检查任务表

```sql
CREATE TABLE inspection_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_code VARCHAR(50) NOT NULL,
    project_id BIGINT NOT NULL,
    inspection_date DATE NOT NULL,
    inspector_ids JSON,
    status ENUM('DRAFT', 'IN_PROGRESS', 'SUBMITTED', 'REVIEWED', 'PUBLISHED') DEFAULT 'DRAFT',
    submitted_at DATETIME,
    submitted_by BIGINT,
    reviewed_at DATETIME,
    reviewed_by BIGINT,
    published_at DATETIME,
    published_by BIGINT,
    org_unit_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_task_code (task_code),
    UNIQUE KEY uk_project_date (project_id, inspection_date),
    INDEX idx_project_id (project_id),
    INDEX idx_inspection_date (inspection_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查任务表';
```

### 2.6 目标检查记录表

```sql
CREATE TABLE target_inspection_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    target_type ENUM('STUDENT', 'CLASS', 'DORMITORY', 'CLASSROOM') NOT NULL,
    target_id BIGINT NOT NULL,
    target_snapshot JSON,
    -- 快照字段
    class_id BIGINT NOT NULL,
    class_name VARCHAR(100),
    class_code VARCHAR(50),
    org_unit_id BIGINT NOT NULL,
    org_unit_name VARCHAR(100),
    snapshot_class_id BIGINT COMMENT '检查时刻的班级ID快照',
    snapshot_class_name VARCHAR(100) COMMENT '检查时刻的班级名称快照',
    snapshot_department_id BIGINT COMMENT '检查时刻的部门ID快照',
    snapshot_department_name VARCHAR(100) COMMENT '检查时刻的部门名称快照',
    -- 分数
    base_score DECIMAL(10,2) NOT NULL,
    raw_score DECIMAL(10,2) NOT NULL,
    weighted_score DECIMAL(10,2),
    fair_adjusted_score DECIMAL(10,2),
    total_deduction DECIMAL(10,2) DEFAULT 0,
    total_bonus DECIMAL(10,2) DEFAULT 0,
    deduction_count INT DEFAULT 0,
    bonus_count INT DEFAULT 0,
    -- 评级
    rating VARCHAR(20),
    rating_color VARCHAR(20),
    -- 状态
    status ENUM('DRAFT', 'SUBMITTED', 'REVIEWED', 'PUBLISHED') DEFAULT 'DRAFT',
    -- 检查人
    inspector_id BIGINT,
    inspected_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_target (task_id, target_type, target_id),
    INDEX idx_task_id (task_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_class_id (class_id),
    INDEX idx_org_unit_id (org_unit_id),
    INDEX idx_snapshot_department_id (snapshot_department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目标检查记录表';
```

### 2.7 扣分/加分明细记录表

```sql
CREATE TABLE deduction_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_record_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    score_item_id BIGINT,
    score DECIMAL(10,2) NOT NULL,
    score_type ENUM('DEDUCTION', 'BONUS') NOT NULL,
    quantity INT DEFAULT 1,
    student_id BIGINT,
    student_name VARCHAR(50),
    student_no VARCHAR(50),
    remark VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    INDEX idx_target_record_id (target_record_id),
    INDEX idx_category_id (category_id),
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扣分/加分明细记录表';
```

### 2.8 检查证据表

```sql
CREATE TABLE inspection_evidences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    deduction_record_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    thumbnail_path VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    INDEX idx_deduction_record_id (deduction_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查证据表';
```

### 2.9 申诉表

```sql
CREATE TABLE appeals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    appeal_code VARCHAR(50) NOT NULL,
    deduction_record_id BIGINT NOT NULL,
    target_record_id BIGINT NOT NULL,
    appellant_id BIGINT NOT NULL,
    appellant_type ENUM('STUDENT', 'TEACHER') NOT NULL,
    reason TEXT NOT NULL,
    evidence_ids JSON,
    status ENUM('PENDING', 'REVIEWING', 'APPROVED', 'REJECTED', 'CANCELLED') DEFAULT 'PENDING',
    reviewer_id BIGINT,
    review_comment TEXT,
    reviewed_at DATETIME,
    score_adjustment DECIMAL(10,2),
    class_id BIGINT NOT NULL,
    org_unit_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_appeal_code (appeal_code),
    INDEX idx_deduction_record_id (deduction_record_id),
    INDEX idx_appellant_id (appellant_id),
    INDEX idx_status (status),
    INDEX idx_class_id (class_id),
    INDEX idx_org_unit_id (org_unit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申诉表';
```

### 2.10 整改工单表

```sql
CREATE TABLE corrective_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_code VARCHAR(50) NOT NULL,
    target_record_id BIGINT NOT NULL,
    issue_description TEXT NOT NULL,
    requirement TEXT,
    responsible_id BIGINT NOT NULL,
    responsible_type ENUM('CLASS_TEACHER', 'DEPT_ADMIN') NOT NULL,
    deadline DATE NOT NULL,
    max_rounds INT DEFAULT 3,
    current_round INT DEFAULT 1,
    affect_score TINYINT DEFAULT 0,
    score_adjustment DECIMAL(10,2),
    status ENUM('PENDING', 'IN_PROGRESS', 'SUBMITTED', 'VERIFIED', 'REJECTED', 'COMPLETED', 'OVERDUE') DEFAULT 'PENDING',
    corrective_description TEXT,
    corrective_evidence_ids JSON,
    submitted_at DATETIME,
    verifier_id BIGINT,
    verify_comment TEXT,
    verified_at DATETIME,
    class_id BIGINT NOT NULL,
    org_unit_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_code (order_code),
    INDEX idx_target_record_id (target_record_id),
    INDEX idx_responsible_id (responsible_id),
    INDEX idx_status (status),
    INDEX idx_deadline (deadline),
    INDEX idx_class_id (class_id),
    INDEX idx_org_unit_id (org_unit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='整改工单表';
```

### 2.11 每日汇总表

```sql
CREATE TABLE daily_summaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    summary_date DATE NOT NULL,
    class_id BIGINT NOT NULL,
    class_name VARCHAR(100),
    org_unit_id BIGINT NOT NULL,
    org_unit_name VARCHAR(100),
    total_base_score DECIMAL(10,2),
    total_raw_score DECIMAL(10,2),
    total_weighted_score DECIMAL(10,2),
    total_fair_score DECIMAL(10,2),
    final_score DECIMAL(10,2),
    total_deduction DECIMAL(10,2),
    total_bonus DECIMAL(10,2),
    record_count INT,
    rating VARCHAR(20),
    rating_color VARCHAR(20),
    rank_in_grade INT,
    rank_in_department INT,
    rank_in_school INT,
    status ENUM('DRAFT', 'PUBLISHED') DEFAULT 'DRAFT',
    published_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_date_class (project_id, summary_date, class_id),
    INDEX idx_project_id (project_id),
    INDEX idx_summary_date (summary_date),
    INDEX idx_class_id (class_id),
    INDEX idx_org_unit_id (org_unit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日汇总表';
```

### 2.12 自动加分规则表

```sql
CREATE TABLE auto_bonus_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    rule_code VARCHAR(50) NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    rule_type ENUM('CONSECUTIVE_FULL_SCORE', 'MONTHLY_EXCELLENT', 'IMPROVEMENT', 'CUSTOM') NOT NULL,
    trigger_condition JSON NOT NULL,
    bonus_score DECIMAL(10,2) NOT NULL,
    is_enabled TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    UNIQUE KEY uk_project_rule (project_id, rule_code),
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动加分规则表';
```

---

## 三、ER 图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             权限系统 ER                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────┐    ┌───────────────┐    ┌─────────┐                           │
│  │  users  │───▶│  user_roles   │◀───│  roles  │                           │
│  └─────────┘    └───────────────┘    └────┬────┘                           │
│                                           │                                 │
│              ┌────────────────────────────┼────────────────────────┐       │
│              ▼                            ▼                        ▼       │
│  ┌────────────────────┐    ┌─────────────────────────┐   ┌────────────┐   │
│  │  role_permissions  │    │ role_data_permissions   │   │ permissions│   │
│  └────────────────────┘    └───────────┬─────────────┘   └────────────┘   │
│                                        │                                    │
│                                        ▼                                    │
│                           ┌────────────────────────┐                       │
│                           │ role_data_scope_items  │                       │
│                           └────────────────────────┘                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                             检查系统 ER                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────┐                                                    │
│  │ inspection_templates│                                                    │
│  └──────────┬──────────┘                                                    │
│             │                                                               │
│             ▼                                                               │
│  ┌──────────────────┐    ┌─────────────────────┐                           │
│  │template_categories│───▶│ category_score_items│                           │
│  └──────────────────┘    └─────────────────────┘                           │
│                                                                             │
│  ┌─────────────────────┐                                                    │
│  │ inspection_projects │                                                    │
│  └──────────┬──────────┘                                                    │
│             │                                                               │
│             ▼                                                               │
│  ┌──────────────────┐                                                       │
│  │ inspection_tasks │                                                       │
│  └──────────┬───────┘                                                       │
│             │                                                               │
│             ▼                                                               │
│  ┌──────────────────────────┐    ┌────────────────────┐                    │
│  │target_inspection_records │───▶│ deduction_records  │                    │
│  └───────────┬──────────────┘    └─────────┬──────────┘                    │
│              │                             │                                │
│    ┌─────────┼─────────────┐              ▼                                │
│    ▼         ▼             ▼    ┌─────────────────────┐                    │
│ ┌───────┐ ┌───────────────┐ ┌──│inspection_evidences │                    │
│ │appeals│ │corrective_    │ │  └─────────────────────┘                    │
│ └───────┘ │orders         │ │                                              │
│           └───────────────┘ │                                              │
│                             ▼                                              │
│                     ┌─────────────────┐                                    │
│                     │ daily_summaries │                                    │
│                     └─────────────────┘                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

**文档版本**: 3.0
**最后更新**: 2026-01-31
