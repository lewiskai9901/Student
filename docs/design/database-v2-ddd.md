# V2 数据库设计文档 (DDD架构)

## 概述

V2数据库设计基于DDD（领域驱动设计）原则，将业务领域划分为清晰的边界上下文，每个领域拥有独立的数据模型。

## 数据库版本

- **V5.0.0**: 组织管理领域表结构
- **V5.0.1**: 数据迁移（departments → org_units）
- **V5.0.2**: 量化检查领域表结构
- **V5.0.3**: 权限管理领域表结构

---

## 组织管理领域 (Organization Domain)

### org_units - 组织单元表

统一管理学校、院系、部门等组织结构。

```sql
CREATE TABLE org_units (
    id BIGINT NOT NULL COMMENT '主键ID',
    unit_code VARCHAR(50) NOT NULL COMMENT '组织编码',
    unit_name VARCHAR(100) NOT NULL COMMENT '组织名称',
    unit_type ENUM('SCHOOL', 'COLLEGE', 'DEPARTMENT', 'TEACHING_GROUP') NOT NULL COMMENT '组织类型',
    parent_id BIGINT DEFAULT NULL COMMENT '父级ID',
    tree_path VARCHAR(500) DEFAULT NULL COMMENT '树路径 /1/5/12/',
    tree_level INT DEFAULT 1 COMMENT '树层级',
    leader_id BIGINT DEFAULT NULL COMMENT '负责人ID',
    deputy_leader_ids JSON DEFAULT NULL COMMENT '副负责人ID列表',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT DEFAULT NULL,
    updated_by BIGINT DEFAULT NULL,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_unit_code (unit_code),
    KEY idx_parent (parent_id),
    KEY idx_tree_path (tree_path(255)),
    KEY idx_type (unit_type)
);
```

**组织类型**:
| 类型 | 说明 |
|------|------|
| SCHOOL | 学校 |
| COLLEGE | 学院/院系 |
| DEPARTMENT | 系部/专业 |
| TEACHING_GROUP | 教研组 |

### academic_years - 学年表

管理学年周期和学期配置。

```sql
CREATE TABLE academic_years (
    id BIGINT NOT NULL COMMENT '主键ID',
    year_code VARCHAR(20) NOT NULL COMMENT '学年编码 如2024-2025',
    year_name VARCHAR(50) NOT NULL COMMENT '学年名称',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    semesters JSON DEFAULT NULL COMMENT '学期配置',
    is_current TINYINT DEFAULT 0 COMMENT '是否当前学年',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_year_code (year_code)
);
```

### teacher_assignments - 教师任职表

记录教师在班级中的任职信息。

```sql
CREATE TABLE teacher_assignments (
    id BIGINT NOT NULL COMMENT '主键ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    role_type ENUM('HEAD_TEACHER', 'DEPUTY_HEAD', 'SUBJECT_TEACHER', 'COUNSELOR') NOT NULL COMMENT '角色类型',
    subject_id BIGINT DEFAULT NULL COMMENT '学科ID',
    is_primary TINYINT DEFAULT 0 COMMENT '是否主要负责人',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE DEFAULT NULL COMMENT '结束日期',
    status ENUM('ACTIVE', 'TRANSFERRED', 'RESIGNED', 'EXPIRED') DEFAULT 'ACTIVE' COMMENT '任职状态',
    transfer_reason VARCHAR(500) DEFAULT NULL COMMENT '调动原因',
    handover_teacher_id BIGINT DEFAULT NULL COMMENT '交接教师ID',
    workload_hours DECIMAL(5,2) DEFAULT NULL COMMENT '工作量',
    remark TEXT DEFAULT NULL COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT DEFAULT NULL,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_class (class_id),
    KEY idx_teacher (teacher_id),
    KEY idx_active_assignment (class_id, role_type, status)
);
```

**任职角色**:
| 角色 | 说明 |
|------|------|
| HEAD_TEACHER | 班主任 |
| DEPUTY_HEAD | 副班主任 |
| SUBJECT_TEACHER | 学科教师 |
| COUNSELOR | 辅导员 |

### grade_directors - 年级主任表

管理年级负责人配置。

```sql
CREATE TABLE grade_directors (
    id BIGINT NOT NULL COMMENT '主键ID',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',
    enrollment_year INT NOT NULL COMMENT '入学年份',
    director_id BIGINT NOT NULL COMMENT '年级主任ID',
    deputy_director_ids JSON DEFAULT NULL COMMENT '副主任ID列表',
    counselor_ids JSON DEFAULT NULL COMMENT '辅导员ID列表',
    max_class_count INT DEFAULT NULL COMMENT '最大班级数',
    enrollment_quota INT DEFAULT NULL COMMENT '招生名额',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_year (org_unit_id, enrollment_year)
);
```

---

## 班级表 (classes)

V2 API使用现有的 `classes` 表，通过Repository层映射到 SchoolClass 领域模型。

**关键字段映射**:
| 数据库字段 | 领域模型属性 | 说明 |
|------------|--------------|------|
| id | id | 班级ID |
| class_code | classCode | 班级编码 |
| class_name | className | 班级名称 |
| grade_id | orgUnitId | 所属组织单元ID |
| grade_level | gradeLevel | 年级(1-10) |
| enrollment_year | enrollmentYear | 入学年份 |
| graduation_year | expectedGraduationYear | 预计毕业年份 |
| max_students | maxStudents | 最大学生数 |
| status | status | 状态 |

**班级状态枚举**:
| 值 | 状态 | 说明 |
|----|------|------|
| 0 | PREPARING | 筹备中 |
| 1 | ACTIVE | 在读 |
| 2 | GRADUATED | 已毕业 |
| 3 | DISSOLVED | 已撤销 |

---

## 权限管理领域 (Access Domain)

V2权限API使用现有的 `permissions` 和 `roles` 表，通过Repository层映射到领域模型。

### permissions 表关键字段
- `id`: 权限ID
- `permission_code`: 权限编码
- `permission_name`: 权限名称
- `parent_id`: 父权限ID
- `permission_type`: 类型 (MENU/BUTTON/API)

### roles 表关键字段
- `id`: 角色ID
- `role_code`: 角色编码
- `role_name`: 角色名称
- `status`: 状态

---

## 量化检查领域 (Inspection Domain)

V2检查模板API使用现有的 `v3_check_templates` 表。

### v3_check_templates 表关键字段
- `id`: 模板ID
- `name`: 模板名称
- `description`: 描述
- `check_type`: 检查类型
- `total_score`: 总分
- `status`: 状态

---

## 数据迁移说明

### departments → org_units 迁移

V5.0.1 迁移脚本将现有 `departments` 表数据迁移到新的 `org_units` 表：

```sql
-- 迁移数据
INSERT INTO org_units (id, unit_code, unit_name, unit_type, parent_id, ...)
SELECT
    id,
    code,
    name,
    CASE level
        WHEN 0 THEN 'SCHOOL'
        WHEN 1 THEN 'COLLEGE'
        WHEN 2 THEN 'DEPARTMENT'
        ELSE 'TEACHING_GROUP'
    END,
    parent_id,
    ...
FROM departments
WHERE deleted = 0;
```

---

## 索引设计

### 组织单元表索引
- `uk_unit_code`: 唯一索引，确保组织编码唯一
- `idx_parent`: 支持快速查询子组织
- `idx_tree_path`: 支持路径查询（前缀匹配）
- `idx_type`: 支持按类型筛选

### 教师任职表索引
- `idx_class`: 支持按班级查询教师
- `idx_teacher`: 支持按教师查询任职班级
- `idx_active_assignment`: 复合索引，支持快速查询当前有效任职

---

## 与V1表的关系

| V1表 | V2表/领域模型 | 说明 |
|------|---------------|------|
| departments | org_units | 组织结构统一管理 |
| classes | classes (SchoolClass) | 领域模型映射 |
| permissions | permissions (Permission) | 领域模型映射 |
| roles | roles (Role) | 领域模型映射 |
| v3_check_templates | v3_check_templates (InspectionTemplate) | 领域模型映射 |

---

*文档更新时间: 2026年1月2日*
