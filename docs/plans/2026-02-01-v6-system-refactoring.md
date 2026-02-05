# V6 系统重构方案 - 组织/场所/用户管理改造

> **版本**: 1.0
> **日期**: 2026-02-01
> **关联文档**:
> - [v6-universal-inspection-system.md](./2026-02-01-v6-universal-inspection-system.md)
> - [v6-database-schema.md](./2026-02-01-v6-database-schema.md)

---

## 一、改造目标

将现有的组织管理、场所管理、用户管理改造为通用化设计，以支持：
- 多行业场景（学校、企业、物业、政府等）
- V6检查系统的三大检查主体（组织、场所、用户）
- 灵活的类型自定义
- 多归属关系（用户多组织、场所多组织共享）

---

## 二、组织管理改造

### 2.1 现有设计分析

```
当前设计：
├── OrgUnit (聚合根) - 统一的组织单元
├── OrgUnitType (枚举) - 硬编码的组织类型
│   ├── SCHOOL, COLLEGE, DEPARTMENT, TEACHING_GROUP (教学)
│   └── STUDENT_AFFAIRS, ACADEMIC_AFFAIRS... (职能)
└── UnitCategory (枚举) - ACADEMIC, FUNCTIONAL, ADMINISTRATIVE
```

**问题**：
1. 类型硬编码在枚举中，无法自定义
2. 层级关系固定，不同行业无法适配
3. 命名偏向学校场景

### 2.2 改造方案

#### 2.2.1 组织类型配置表（新增）

```sql
-- 组织类型配置表（替代硬编码的枚举）
CREATE TABLE org_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    type_code VARCHAR(50) NOT NULL,           -- 类型代码: SCHOOL, DEPARTMENT, CLASS, TEAM
    type_name VARCHAR(100) NOT NULL,          -- 类型名称: 学校, 部门, 班级, 团队

    -- 层级配置
    level INT NOT NULL,                       -- 层级深度（1=顶级）
    parent_type_codes JSON,                   -- 允许的父类型代码列表
    can_have_children TINYINT DEFAULT 1,      -- 是否可以有子级

    -- 关联配置
    can_have_spaces TINYINT DEFAULT 1,        -- 是否可以关联场所
    can_have_users TINYINT DEFAULT 1,         -- 是否可以关联用户

    -- 显示配置
    icon VARCHAR(50),
    color VARCHAR(20),

    -- 加权属性（V6检查汇总用）
    weight_attribute VARCHAR(50),             -- member_count, space_count等

    -- 预置场景
    scene_code VARCHAR(50),                   -- SCHOOL, COMPANY, PROPERTY, GOVERNMENT

    sort_order INT DEFAULT 0,
    is_system TINYINT DEFAULT 0,              -- 系统预置
    is_enabled TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_type_code (type_code),
    INDEX idx_scene (scene_code),
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织类型配置表';

-- 预置数据：学校场景
INSERT INTO org_types (type_code, type_name, level, parent_type_codes, can_have_children, icon, weight_attribute, scene_code, is_system) VALUES
('SCHOOL', '学校', 1, NULL, 1, 'school', NULL, 'SCHOOL', 1),
('COLLEGE', '系部/学院', 2, '["SCHOOL"]', 1, 'building', 'member_count', 'SCHOOL', 1),
('MAJOR', '专业', 3, '["COLLEGE"]', 1, 'book', 'member_count', 'SCHOOL', 1),
('CLASS', '班级', 4, '["COLLEGE","MAJOR"]', 0, 'users', 'member_count', 'SCHOOL', 1),
('ADMIN_DEPT', '行政部门', 2, '["SCHOOL"]', 1, 'briefcase', 'member_count', 'SCHOOL', 1);

-- 预置数据：企业场景
INSERT INTO org_types (type_code, type_name, level, parent_type_codes, can_have_children, icon, weight_attribute, scene_code, is_system) VALUES
('COMPANY', '公司', 1, NULL, 1, 'building', NULL, 'COMPANY', 1),
('DIVISION', '事业部', 2, '["COMPANY"]', 1, 'layers', 'member_count', 'COMPANY', 1),
('DEPARTMENT', '部门', 3, '["COMPANY","DIVISION"]', 1, 'folder', 'member_count', 'COMPANY', 1),
('TEAM', '团队', 4, '["DEPARTMENT"]', 0, 'users', 'member_count', 'COMPANY', 1);
```

#### 2.2.2 组织单元表改造

```sql
-- 组织单元表（departments表改名为org_units）
ALTER TABLE departments RENAME TO org_units;

-- 新增/修改字段
ALTER TABLE org_units
    -- 类型改为引用配置表
    CHANGE COLUMN unit_type type_code VARCHAR(50) NOT NULL,

    -- 新增自定义属性
    ADD COLUMN attributes JSON COMMENT '扩展属性（人数、入学年份等）',

    -- 新增统计缓存
    ADD COLUMN member_count INT DEFAULT 0 COMMENT '成员数量',
    ADD COLUMN child_count INT DEFAULT 0 COMMENT '子组织数量',
    ADD COLUMN space_count INT DEFAULT 0 COMMENT '关联场所数量',

    -- 添加索引
    ADD INDEX idx_type_code (type_code);

-- 示例：组织属性JSON结构
/*
  班级属性:
  {
    "enrollment_year": 2024,
    "major_direction": "软件开发",
    "standard_size": 50,
    "class_teacher_id": 12345
  }

  部门属性:
  {
    "cost_center": "CC001",
    "location": "A栋3楼"
  }
*/
```

#### 2.2.3 用户-组织多归属关系表（新增）

```sql
-- 用户-组织关系表（支持一人多组织）
CREATE TABLE user_org_relations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    user_id BIGINT NOT NULL,
    org_id BIGINT NOT NULL,

    -- 关系类型
    relation_type ENUM('PRIMARY', 'SECONDARY', 'TEMPORARY') DEFAULT 'PRIMARY',
    -- PRIMARY: 主归属（每个用户只有一个）
    -- SECONDARY: 兼职/副职
    -- TEMPORARY: 临时借调

    -- 角色（在该组织中的角色）
    role_in_org VARCHAR(50),                  -- 如：班主任、组长、成员

    -- 有效期（临时借调用）
    start_date DATE,
    end_date DATE,

    -- 权重（用于分数分配）
    weight DECIMAL(5,4) DEFAULT 1.0,

    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_org (user_id, org_id),
    INDEX idx_org (org_id),
    INDEX idx_user_primary (user_id, relation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-组织关系表';
```

---

## 三、场所管理改造

### 3.1 现有设计分析

```
当前设计：
├── Space (聚合根) - 统一的场所管理
├── SpaceType (枚举) - CAMPUS, BUILDING, FLOOR, ROOM
├── RoomType (枚举) - DORMITORY, CLASSROOM, OFFICE等
└── 归属字段: orgUnitId, classId, responsibleUserId
```

**问题**：
1. 只支持单一组织归属，不支持多组织共享
2. 场所类型硬编码
3. classId和orgUnitId同时存在，语义不清

### 3.2 改造方案

#### 3.2.1 场所类型配置表（新增）

```sql
-- 场所类型配置表
CREATE TABLE space_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 层级类型
    hierarchy_type ENUM('CAMPUS', 'BUILDING', 'FLOOR', 'ROOM', 'AREA') NOT NULL,

    -- 具体类型
    type_code VARCHAR(50) NOT NULL,           -- DORMITORY, CLASSROOM, OFFICE, MEETING_ROOM
    type_name VARCHAR(100) NOT NULL,          -- 学生宿舍, 教室, 办公室, 会议室

    -- 父类型限制
    parent_hierarchy_types JSON,              -- 允许的父层级类型

    -- 特性配置
    has_occupancy TINYINT DEFAULT 0,          -- 是否有入住管理
    has_gender_limit TINYINT DEFAULT 0,       -- 是否区分性别
    has_capacity TINYINT DEFAULT 1,           -- 是否有容量限制

    -- 显示配置
    icon VARCHAR(50),
    color VARCHAR(20),

    -- 加权属性
    weight_attribute VARCHAR(50),             -- capacity, area, occupancy

    -- 场景
    scene_code VARCHAR(50),

    sort_order INT DEFAULT 0,
    is_system TINYINT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_type_code (type_code),
    INDEX idx_hierarchy (hierarchy_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场所类型配置表';

-- 预置数据
INSERT INTO space_types (hierarchy_type, type_code, type_name, has_occupancy, has_gender_limit, has_capacity, icon, weight_attribute, scene_code, is_system) VALUES
-- 宿舍类
('ROOM', 'DORMITORY', '学生宿舍', 1, 1, 1, 'home', 'capacity', 'SCHOOL', 1),
('ROOM', 'STAFF_DORM', '教职工宿舍', 1, 1, 1, 'home', 'capacity', 'SCHOOL', 1),
-- 教学类
('ROOM', 'CLASSROOM', '普通教室', 0, 0, 1, 'book', 'capacity', 'SCHOOL', 1),
('ROOM', 'LAB', '实验室', 0, 0, 1, 'flask', 'capacity', 'SCHOOL', 1),
('ROOM', 'COMPUTER_LAB', '机房', 0, 0, 1, 'computer', 'capacity', 'SCHOOL', 1),
-- 办公类
('ROOM', 'OFFICE', '办公室', 0, 0, 1, 'briefcase', 'area', 'COMMON', 1),
('ROOM', 'MEETING_ROOM', '会议室', 0, 0, 1, 'users', 'capacity', 'COMMON', 1);
```

#### 3.2.2 场所-组织关系表（新增，支持多组织共享）

```sql
-- 场所-组织关系表
CREATE TABLE space_org_relations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    space_id BIGINT NOT NULL,
    org_id BIGINT NOT NULL,

    -- 关系类型
    relation_type ENUM('OWNER', 'USER', 'MANAGER') DEFAULT 'USER',
    -- OWNER: 归属（如：宿舍归属班级）
    -- USER: 使用（如：教室被班级使用）
    -- MANAGER: 管理（如：楼管管理楼栋）

    -- 使用人数（用于按比例分配）
    member_count INT DEFAULT 0,

    -- 占比（自动计算或手动设置）
    ratio DECIMAL(5,4),

    -- 是否主归属（用于PRIMARY_ONLY策略）
    is_primary TINYINT DEFAULT 0,

    -- 有效期
    start_date DATE,
    end_date DATE,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_space_org (space_id, org_id),
    INDEX idx_org (org_id),
    INDEX idx_space_primary (space_id, is_primary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场所-组织关系表';
```

#### 3.2.3 场所表改造

```sql
-- 场所表改造
ALTER TABLE space
    -- 类型改为引用配置表
    CHANGE COLUMN room_type type_code VARCHAR(50),

    -- 移除单一归属字段（改用关系表）
    -- 保留 responsible_user_id 作为管理员

    -- 新增自定义属性
    ADD COLUMN attributes JSON COMMENT '扩展属性',

    -- 新增统计缓存
    ADD COLUMN org_count INT DEFAULT 0 COMMENT '关联组织数量',
    ADD COLUMN occupant_count INT DEFAULT 0 COMMENT '当前入住人数',

    ADD INDEX idx_type_code (type_code);
```

---

## 四、用户管理改造

### 4.1 现有设计分析

```
当前设计：
├── User (聚合根) - 统一用户
│   ├── userType: ADMIN, TEACHER, STUDENT
│   ├── orgUnitId: 单一组织归属
│   └── classId: 学生班级
└── Student (聚合根) - 学生扩展信息
```

**问题**：
1. 只支持单一组织归属
2. userType硬编码
3. 学生和教师的扩展信息设计不一致

### 4.2 改造方案

#### 4.2.1 用户类型配置表（新增）

```sql
-- 用户类型配置表
CREATE TABLE user_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    type_code VARCHAR(50) NOT NULL,           -- STUDENT, TEACHER, ADMIN, EMPLOYEE
    type_name VARCHAR(100) NOT NULL,          -- 学生, 教师, 管理员, 员工

    -- 特性配置
    can_be_inspector TINYINT DEFAULT 0,       -- 是否可以作为检查员
    can_be_inspected TINYINT DEFAULT 0,       -- 是否可以被检查
    requires_org TINYINT DEFAULT 1,           -- 是否必须归属组织
    allows_multi_org TINYINT DEFAULT 0,       -- 是否允许多组织

    -- 显示配置
    icon VARCHAR(50),
    color VARCHAR(20),

    -- 场景
    scene_code VARCHAR(50),

    sort_order INT DEFAULT 0,
    is_system TINYINT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户类型配置表';

-- 预置数据
INSERT INTO user_types (type_code, type_name, can_be_inspector, can_be_inspected, requires_org, allows_multi_org, icon, scene_code, is_system) VALUES
('STUDENT', '学生', 0, 1, 1, 0, 'user', 'SCHOOL', 1),
('TEACHER', '教师', 1, 1, 1, 1, 'user-tie', 'SCHOOL', 1),
('ADMIN', '管理员', 1, 0, 0, 1, 'shield', 'COMMON', 1),
('EMPLOYEE', '员工', 1, 1, 1, 1, 'user', 'COMPANY', 1);
```

#### 4.2.2 用户表改造

```sql
-- 用户表改造
ALTER TABLE users
    -- 类型改为引用配置表
    CHANGE COLUMN user_type type_code VARCHAR(50),

    -- 移除单一归属字段（改用关系表）
    DROP COLUMN org_unit_id,
    DROP COLUMN class_id,

    -- 新增主归属缓存（从关系表同步）
    ADD COLUMN primary_org_id BIGINT COMMENT '主归属组织ID（缓存）',
    ADD COLUMN primary_org_name VARCHAR(200) COMMENT '主归属组织名称（缓存）',

    -- 新增自定义属性
    ADD COLUMN attributes JSON COMMENT '扩展属性',

    ADD INDEX idx_type_code (type_code),
    ADD INDEX idx_primary_org (primary_org_id);
```

#### 4.2.3 用户-场所关系表（新增）

```sql
-- 用户-场所关系表（住宿、工位等）
CREATE TABLE user_space_relations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    user_id BIGINT NOT NULL,
    space_id BIGINT NOT NULL,

    -- 关系类型
    relation_type ENUM('RESIDENT', 'WORKSTATION', 'MANAGER') DEFAULT 'RESIDENT',
    -- RESIDENT: 入住（学生住宿舍）
    -- WORKSTATION: 工位（员工办公位）
    -- MANAGER: 管理（楼管管理楼栋）

    -- 细节
    position VARCHAR(50),                     -- 床位号、工位号

    -- 有效期
    start_date DATE,
    end_date DATE,

    is_current TINYINT DEFAULT 1,             -- 是否当前有效

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user (user_id),
    INDEX idx_space (space_id),
    INDEX idx_current (is_current)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-场所关系表';
```

---

## 五、V6检查系统补充设计

### 5.1 共享场所处理（inspection_projects表新增字段）

```sql
ALTER TABLE inspection_projects ADD COLUMN (
    -- 共享场所处理策略
    shared_space_strategy ENUM('SINGLE', 'MULTIPLE', 'PRIMARY_ONLY') DEFAULT 'SINGLE',
    -- SINGLE: 共享场所只检查一次，分数分配给各组织
    -- MULTIPLE: 为每个关联组织分别生成检查目标
    -- PRIMARY_ONLY: 只检查主归属组织

    -- 分数分配模式（当策略为SINGLE时生效）
    score_distribution_mode ENUM('RATIO', 'EQUAL', 'FULL') DEFAULT 'RATIO',
    -- RATIO: 按人数比例分配
    -- EQUAL: 平均分配
    -- FULL: 每个组织都扣全额

    -- 检查员分工模式
    inspector_assignment_mode ENUM('FREE', 'ASSIGNED', 'HYBRID') DEFAULT 'FREE',
    -- FREE: 自由领取
    -- ASSIGNED: 预分配
    -- HYBRID: 混合（支持预分配，也可自由领取未分配的）

    -- 节假日处理
    skip_holidays TINYINT DEFAULT 0,
    excluded_dates JSON                       -- 排除的日期列表: ["2024-10-01", "2024-10-02"]
);
```

### 5.2 检查目标表扩展（支持多组织归属）

```sql
ALTER TABLE inspection_targets ADD COLUMN (
    -- 多组织归属（用于共享场所）
    belong_orgs JSON,
    /*
      示例（203宿舍的归属）:
      [
        {"orgId": 1, "orgName": "软件1班", "ratio": 0.5, "isPrimary": true},
        {"orgId": 2, "orgName": "网络1班", "ratio": 0.5, "isPrimary": false}
      ]
    */

    -- 分配的检查员（预分配模式）
    assigned_inspector_id BIGINT,
    assigned_inspector_name VARCHAR(50)
);

CREATE INDEX idx_assigned_inspector ON inspection_targets(assigned_inspector_id);
```

### 5.3 检查明细表补充

```sql
ALTER TABLE inspection_details ADD COLUMN (
    -- 创建者信息
    created_by BIGINT,
    created_by_name VARCHAR(50)
);
```

### 5.4 检查员-任务分配表（新增，用于预分配模式）

```sql
-- 检查员任务分配表
CREATE TABLE inspection_task_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    task_id BIGINT NOT NULL,
    inspector_id BIGINT NOT NULL,
    inspector_name VARCHAR(50),

    -- 分配范围
    assignment_type ENUM('ORG', 'SPACE', 'CUSTOM') NOT NULL,
    -- ORG: 按组织分配（如：负责某系部）
    -- SPACE: 按场所分配（如：负责1号楼）
    -- CUSTOM: 自定义分配

    -- 分配的目标
    assigned_org_ids JSON,                    -- 分配的组织ID列表
    assigned_space_ids JSON,                  -- 分配的场所ID列表
    assigned_target_ids JSON,                 -- 直接分配的目标ID列表

    -- 统计
    total_targets INT DEFAULT 0,
    completed_targets INT DEFAULT 0,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_task_inspector (task_id, inspector_id),
    INDEX idx_inspector (inspector_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查员任务分配表';
```

---

## 六、API补充设计

### 6.1 组织类型管理API

```typescript
// GET /api/v6/org-types
// 获取组织类型列表

// POST /api/v6/org-types
// 创建自定义组织类型
{
  typeCode: string
  typeName: string
  level: number
  parentTypeCodes: string[]
  canHaveChildren: boolean
  canHaveSpaces: boolean
  canHaveUsers: boolean
  icon?: string
  color?: string
  weightAttribute?: string
}

// PUT /api/v6/org-types/:id
// DELETE /api/v6/org-types/:id（仅非系统类型可删除）
```

### 6.2 用户-组织关系API

```typescript
// GET /api/v6/users/:userId/orgs
// 获取用户的所有组织归属

// POST /api/v6/users/:userId/orgs
// 添加用户组织归属
{
  orgId: number
  relationType: 'PRIMARY' | 'SECONDARY' | 'TEMPORARY'
  roleInOrg?: string
  startDate?: string
  endDate?: string
}

// PUT /api/v6/users/:userId/orgs/:orgId
// DELETE /api/v6/users/:userId/orgs/:orgId

// GET /api/v6/orgs/:orgId/users
// 获取组织的所有用户
```

### 6.3 场所-组织关系API

```typescript
// GET /api/v6/spaces/:spaceId/orgs
// 获取场所的所有关联组织

// POST /api/v6/spaces/:spaceId/orgs
// 添加场所组织关联
{
  orgId: number
  relationType: 'OWNER' | 'USER' | 'MANAGER'
  memberCount?: number
  ratio?: number
  isPrimary: boolean
}

// PUT /api/v6/spaces/:spaceId/orgs/:orgId
// DELETE /api/v6/spaces/:spaceId/orgs/:orgId

// GET /api/v6/orgs/:orgId/spaces
// 获取组织关联的所有场所
```

### 6.4 检查员分配API

```typescript
// GET /api/v6/inspection/tasks/:taskId/assignments
// 获取任务的检查员分配情况

// POST /api/v6/inspection/tasks/:taskId/assignments
// 创建检查员分配
{
  inspectorId: number
  assignmentType: 'ORG' | 'SPACE' | 'CUSTOM'
  assignedOrgIds?: number[]
  assignedSpaceIds?: number[]
  assignedTargetIds?: number[]
}

// PUT /api/v6/inspection/tasks/:taskId/assignments/:id
// DELETE /api/v6/inspection/tasks/:taskId/assignments/:id

// POST /api/v6/inspection/tasks/:taskId/auto-assign
// 自动分配（按规则均分）
{
  strategy: 'BY_ORG' | 'BY_SPACE' | 'EVEN'
}
```

### 6.5 我的任务/记录API

```typescript
// GET /api/v6/inspection/my/tasks
// 获取当前用户（检查员）的任务列表
{
  status?: string
  startDate?: string
  endDate?: string
}

// GET /api/v6/inspection/my/targets
// 获取当前用户分配/可领取的检查目标

// GET /api/v6/inspection/records/by-org/:orgId
// 获取某组织的历史检查记录

// GET /api/v6/inspection/records/by-space/:spaceId
// 获取某场所的历史检查记录

// GET /api/v6/inspection/records/by-user/:userId
// 获取某用户的历史检查记录（扣分记录）
```

---

## 七、数据迁移方案

### 7.1 组织数据迁移

```sql
-- 1. 创建org_types表并插入预置数据（见上文）

-- 2. 迁移departments表到org_units
-- 已通过ALTER RENAME完成

-- 3. 迁移类型数据
UPDATE org_units SET type_code =
    CASE unit_type
        WHEN 'SCHOOL' THEN 'SCHOOL'
        WHEN 'COLLEGE' THEN 'COLLEGE'
        WHEN 'TEACHING_GROUP' THEN 'CLASS'
        -- ... 其他映射
    END;

-- 4. 迁移user_org关系
INSERT INTO user_org_relations (user_id, org_id, relation_type)
SELECT id, org_unit_id, 'PRIMARY'
FROM users
WHERE org_unit_id IS NOT NULL;
```

### 7.2 场所数据迁移

```sql
-- 1. 创建space_types表并插入预置数据（见上文）

-- 2. 迁移space表类型字段
UPDATE space SET type_code = room_type;

-- 3. 迁移space_org关系
INSERT INTO space_org_relations (space_id, org_id, relation_type, is_primary)
SELECT id, org_unit_id, 'OWNER', 1
FROM space
WHERE org_unit_id IS NOT NULL;

INSERT INTO space_org_relations (space_id, org_id, relation_type, is_primary)
SELECT id, class_id, 'USER', 1
FROM space
WHERE class_id IS NOT NULL AND class_id != org_unit_id;
```

### 7.3 用户数据迁移

```sql
-- 1. 创建user_types表并插入预置数据（见上文）

-- 2. 迁移users表类型字段
UPDATE users SET type_code =
    CASE user_type
        WHEN 1 THEN 'ADMIN'
        WHEN 2 THEN 'TEACHER'
        WHEN 3 THEN 'STUDENT'
    END;

-- 3. 迁移user_space关系（学生住宿）
INSERT INTO user_space_relations (user_id, space_id, relation_type, position, is_current)
SELECT s.user_id, s.dormitory_id, 'RESIDENT', s.bed_number, 1
FROM students s
WHERE s.dormitory_id IS NOT NULL;
```

---

## 八、改造影响范围

### 8.1 需要修改的模块

| 模块 | 改动内容 | 优先级 |
|------|---------|--------|
| OrgUnit领域 | 使用type_code替代OrgUnitType枚举 | P0 |
| Space领域 | 使用type_code替代RoomType枚举 | P0 |
| User领域 | 使用type_code替代UserType枚举 | P0 |
| 组织管理前端 | 支持类型配置、多用户归属 | P1 |
| 场所管理前端 | 支持类型配置、多组织关联 | P1 |
| 用户管理前端 | 支持多组织归属管理 | P1 |
| V6检查系统 | 集成新的关系表 | P0 |

### 8.2 兼容性处理

1. **枚举兼容层**：保留原枚举类，添加fromTypeCode方法
2. **API兼容层**：旧接口返回兼容格式
3. **数据迁移**：分批次迁移，支持回滚

---

**文档版本**: 1.0
**最后更新**: 2026-02-01
