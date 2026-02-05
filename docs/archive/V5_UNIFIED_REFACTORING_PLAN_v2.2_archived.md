# V5 统一重构方案：权限系统 + 量化检查系统

> **版本**: 2.0 (统一重构版)
> **日期**: 2026-01-31
> **状态**: 设计方案
> **范围**: 权限系统优化 + V5量化检查系统重构

---

## 一、重构目标

### 1.1 本次重构解决的核心问题

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           重构目标                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  权限系统优化:                                                               │
│  ├── 功能权限与数据权限彻底分离                                              │
│  ├── 无硬编码：权限、模块、范围全部可配置                                     │
│  ├── 模块级数据权限：同一用户不同模块可有不同数据范围                         │
│  └── 支持复杂场景：跨部门、临时授权、自定义范围                               │
│                                                                             │
│  量化检查系统 V5:                                                            │
│  ├── Session-based 架构：项目→任务→记录三级结构                              │
│  ├── 5种录入模式：SPACE/PERSON/CLASS/ITEM/CHECKLIST                         │
│  ├── 混合宿舍分数分配                                                        │
│  ├── 完整申诉流程                                                            │
│  ├── 整改工单管理                                                            │
│  └── 与权限系统深度整合                                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 设计原则

| 原则 | 说明 |
|------|------|
| **部门不分配功能权限** | 业界主流做法，部门只用于组织归属和数据范围 |
| **角色 = 功能权限 + 数据权限** | 角色同时配置两个维度 |
| **无硬编码** | 所有配置项从数据库读取，支持动态扩展 |
| **模块级数据权限** | 同一角色在不同模块可有不同数据范围 |
| **快照模式** | 检查记录保存时固化关联信息 |

---

## 二、权限系统架构设计

### 2.1 核心模型

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           权限模型设计                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                    ┌─────────────┐                                          │
│                    │    用户     │                                          │
│                    │   (User)    │                                          │
│                    └──────┬──────┘                                          │
│                           │                                                 │
│            ┌──────────────┴──────────────┐                                  │
│            │                             │                                  │
│            ▼                             ▼                                  │
│     ┌─────────────┐              ┌─────────────┐                            │
│     │    部门     │              │    角色     │ ←── 用户可有多个角色         │
│     │ (OrgUnit)   │              │   (Role)    │                            │
│     └─────────────┘              └──────┬──────┘                            │
│            │                            │                                   │
│            │                    ┌───────┴───────┐                           │
│            ▼                    │               │                           │
│     ┌─────────────┐      ┌───────────┐  ┌─────────────────┐                │
│     │  数据归属   │      │ 功能权限  │  │    数据权限      │                │
│     │ (用于确定   │      │(Permission)│  │(RoleDataPermission)│             │
│     │  "本部门")  │      │           │  │                 │                │
│     └─────────────┘      │ 控制:     │  │ 控制:           │                │
│                          │"能不能用" │  │"能看/改多少"    │                │
│                          └───────────┘  └────────┬────────┘                │
│                                                  │                          │
│                                                  ▼                          │
│                                         ┌─────────────────┐                │
│                                         │  按模块配置      │                │
│                                         │  (DataModule)   │                │
│                                         │                 │                │
│                                         │ student: ALL    │                │
│                                         │ dormitory: DEPT │                │
│                                         │ inspection: SELF│                │
│                                         └─────────────────┘                │
│                                                                             │
│  关键点:                                                                    │
│  ✗ 部门不分配功能权限                                                       │
│  ✓ 角色同时配置功能权限和数据权限                                            │
│  ✓ 数据权限按模块独立配置                                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 权限判断流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              权限检查流程                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   用户请求: GET /api/inspection/records                                     │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ Step 1: 功能权限检查 (Spring Security)                               │   │
│   │                                                                     │   │
│   │ @PreAuthorize("hasAuthority('inspection:record:view')")             │   │
│   │                                                                     │   │
│   │ 1. 获取用户所有角色                                                  │   │
│   │ 2. 获取所有角色的功能权限 (并集)                                      │   │
│   │ 3. 判断是否包含 inspection:record:view                              │   │
│   │                                                                     │   │
│   │ 无权限 → 返回 403 Forbidden                                         │   │
│   │ 有权限 → 继续 Step 2                                                │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                     │                                       │
│                                     ▼                                       │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ Step 2: 数据权限处理 (MyBatis Interceptor)                           │   │
│   │                                                                     │   │
│   │ @DataPermission(module = "inspection_record")                       │   │
│   │                                                                     │   │
│   │ 1. 获取用户所有角色在 inspection_record 模块的数据权限配置            │   │
│   │ 2. 多角色取最大范围                                                  │   │
│   │ 3. 若 CUSTOM，合并所有自定义范围                                     │   │
│   │ 4. 生成数据过滤 SQL 条件                                            │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                     │                                       │
│                                     ▼                                       │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ Step 3: 执行查询 (自动过滤)                                          │   │
│   │                                                                     │   │
│   │ 原SQL:                                                              │   │
│   │   SELECT * FROM target_inspection_records WHERE task_id = ?         │   │
│   │                                                                     │   │
│   │ 过滤后 (DEPARTMENT_AND_BELOW):                                      │   │
│   │   SELECT * FROM target_inspection_records                           │   │
│   │   WHERE task_id = ?                                                 │   │
│   │   AND org_unit_id IN (1, 2, 3, 4)  -- 用户部门及下级                 │   │
│   │                                                                     │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 三、配置化设计（无硬编码）

### 3.1 配置表结构

```sql
-- =====================================================
-- 1. 数据范围类型配置表 (代替枚举硬编码)
-- =====================================================
CREATE TABLE data_scope_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    scope_code VARCHAR(50) NOT NULL,           -- ALL, DEPARTMENT, CUSTOM 等
    scope_name VARCHAR(100) NOT NULL,          -- 显示名称
    scope_level INT DEFAULT 0,                 -- 优先级 (用于多角色合并时取最大)

    -- 计算逻辑类型
    calc_type VARCHAR(50) NOT NULL,            -- NONE/USER_ORG/USER_ORG_TREE/CUSTOM_CONFIG/CREATOR

    -- 说明
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


-- =====================================================
-- 2. 数据模块配置表 (代替枚举硬编码)
-- =====================================================
CREATE TABLE data_modules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    module_code VARCHAR(50) NOT NULL,          -- student, dormitory_room 等
    module_name VARCHAR(100) NOT NULL,         -- 显示名称
    domain_code VARCHAR(50),                   -- 所属领域: organization, inspection, access
    domain_name VARCHAR(100),                  -- 领域名称

    -- 数据过滤字段配置 (JSON)
    -- {"org_unit": "org_unit_id", "class": "class_id", "creator": "created_by"}
    filter_fields JSON,

    -- 主表名 (用于动态SQL)
    main_table VARCHAR(100),

    -- 是否启用数据权限控制
    enable_data_permission TINYINT DEFAULT 1,

    description VARCHAR(500),
    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_module_code (module_code),
    INDEX idx_domain_code (domain_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据模块配置表';

-- 初始数据 (按领域分组)
INSERT INTO data_modules (module_code, module_name, domain_code, domain_name, filter_fields, main_table, sort_order) VALUES
-- Organization 领域
('student', '学生信息', 'organization', '组织管理', '{"org_unit":"org_unit_id","class":"class_id","creator":"created_by"}', 'students', 101),
('school_class', '班级管理', 'organization', '组织管理', '{"org_unit":"org_unit_id"}', 'classes', 102),
('org_unit', '组织架构', 'organization', '组织管理', '{"org_unit":"id"}', 'org_units', 103),

-- Space 领域
('dormitory_building', '楼栋管理', 'space', '场所管理', '{"org_unit":"org_unit_id"}', 'space', 201),
('dormitory_room', '房间管理', 'space', '场所管理', '{"org_unit":"org_unit_id","building":"building_id"}', 'space', 202),
('dormitory_checkin', '入住管理', 'space', '场所管理', '{"org_unit":"org_unit_id","class":"class_id"}', 'space_occupants', 203),

-- Inspection 领域 (V5新增)
('inspection_template', '检查模板', 'inspection', '量化检查', '{"org_unit":"org_unit_id","creator":"created_by"}', 'inspection_templates', 301),
('inspection_project', '检查项目', 'inspection', '量化检查', '{"org_unit":"org_unit_id","creator":"created_by"}', 'inspection_projects', 302),
('inspection_task', '检查任务', 'inspection', '量化检查', '{"org_unit":"org_unit_id"}', 'inspection_tasks', 303),
('inspection_record', '检查记录', 'inspection', '量化检查', '{"org_unit":"org_unit_id","class":"class_id"}', 'target_inspection_records', 304),
('inspection_appeal', '申诉管理', 'inspection', '量化检查', '{"org_unit":"org_unit_id","class":"class_id","creator":"created_by"}', 'appeals', 305),
('inspection_summary', '检查汇总', 'inspection', '量化检查', '{"org_unit":"org_unit_id","class":"class_id"}', 'daily_summaries', 306),
('inspection_corrective', '整改管理', 'inspection', '量化检查', '{"org_unit":"org_unit_id","class":"class_id"}', 'corrective_orders', 307),

-- Access 领域
('system_user', '用户管理', 'access', '权限管理', '{"org_unit":"org_unit_id"}', 'users', 401),
('system_role', '角色管理', 'access', '权限管理', NULL, 'roles', 402);


-- =====================================================
-- 3. 自定义范围项类型配置表
-- =====================================================
CREATE TABLE scope_item_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    item_type_code VARCHAR(50) NOT NULL,       -- ORG_UNIT, CLASS, BUILDING 等
    item_type_name VARCHAR(100) NOT NULL,      -- 显示名称

    -- 关联的数据表和字段 (用于动态加载选项)
    ref_table VARCHAR(100),                    -- 引用表: org_units, classes, buildings
    ref_id_field VARCHAR(100),                 -- ID字段: id
    ref_name_field VARCHAR(100),               -- 名称字段: name
    ref_parent_field VARCHAR(100),             -- 父级字段 (用于树形): parent_id

    -- 适用的数据模块 (JSON数组)
    applicable_modules JSON,                   -- ["student", "inspection_record"]

    -- 对应的过滤字段KEY (与 data_modules.filter_fields 中的 key 对应)
    filter_field_key VARCHAR(50),              -- org_unit, class, building 等

    -- 是否支持包含下级
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

### 3.2 功能权限表优化

```sql
-- =====================================================
-- 功能权限表 (树形结构，支持配置)
-- =====================================================
CREATE TABLE permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 权限编码 (格式: module:resource:action)
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,

    -- 权限分解 (便于查询和分组)
    module_code VARCHAR(50) NOT NULL,          -- 模块: student, dormitory, inspection
    resource_code VARCHAR(50),                 -- 资源: info, room, template
    action_code VARCHAR(50),                   -- 操作: view, create, update, delete

    -- 树形结构
    parent_id BIGINT,
    tree_path VARCHAR(500),                    -- 物化路径: /1/2/3/
    tree_level INT DEFAULT 0,                  -- 层级深度

    -- 权限类型
    permission_type VARCHAR(20) NOT NULL,      -- MODULE(模块) / MENU(菜单) / BUTTON(按钮) / API(接口)

    -- 前端路由 (MENU类型)
    route_path VARCHAR(255),
    component_path VARCHAR(255),
    icon VARCHAR(100),

    -- 关联的后端API (API类型)
    api_path VARCHAR(255),
    api_method VARCHAR(10),

    -- 排序和状态
    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    is_system TINYINT DEFAULT 0,               -- 系统权限不可删除
    description VARCHAR(500),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_permission_code (permission_code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_module_code (module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能权限表';


-- =====================================================
-- 角色表 (优化)
-- =====================================================
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    role_code VARCHAR(50) NOT NULL,            -- 角色编码
    role_name VARCHAR(100) NOT NULL,           -- 角色名称

    -- 角色类型
    role_type VARCHAR(30) DEFAULT 'CUSTOM',    -- SYSTEM(系统)/CUSTOM(自定义)
    role_level INT DEFAULT 100,                -- 角色等级 (用于权限继承判断)

    description VARCHAR(500),
    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    deleted TINYINT DEFAULT 0,

    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';


-- =====================================================
-- 角色-功能权限关联表
-- =====================================================
CREATE TABLE role_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-功能权限关联表';


-- =====================================================
-- 角色-数据权限配置表 (核心! 模块级配置)
-- =====================================================
CREATE TABLE role_data_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,

    -- 数据模块 (关联 data_modules 表)
    module_code VARCHAR(50) NOT NULL,

    -- 数据范围 (关联 data_scope_types 表)
    scope_code VARCHAR(50) NOT NULL,

    -- 说明
    description VARCHAR(200),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_role_module (role_id, module_code),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-数据权限配置表';


-- =====================================================
-- 自定义数据范围明细表 (CUSTOM范围时使用)
-- =====================================================
CREATE TABLE role_data_scope_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_data_permission_id BIGINT NOT NULL,

    -- 范围类型 (关联 scope_item_types 表)
    item_type_code VARCHAR(50) NOT NULL,

    -- 范围ID和名称
    scope_id BIGINT NOT NULL,
    scope_name VARCHAR(100),                   -- 冗余，方便显示

    -- 是否包含下级 (仅部门类型有效)
    include_children TINYINT DEFAULT 0,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_permission_scope (role_data_permission_id, item_type_code, scope_id),
    INDEX idx_role_data_permission_id (role_data_permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义数据范围明细表';


-- =====================================================
-- 用户-角色关联表 (支持范围限定)
-- =====================================================
CREATE TABLE user_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    -- 可选：角色在特定范围内生效 (支持部门级角色分配)
    scope_org_unit_id BIGINT,                  -- 角色仅在此部门范围内生效

    -- 临时角色支持
    expires_at DATETIME,                       -- 过期时间

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,

    UNIQUE KEY uk_user_role (user_id, role_id, scope_org_unit_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';
```

### 3.3 与现有系统的对比和迁移

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     现有系统 vs 新设计 对比                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  现有系统:                           新设计:                                 │
│  ────────────────────────────────   ────────────────────────────────────    │
│                                                                             │
│  DataScope (Java枚举)               data_scope_types (配置表)               │
│  ├── ALL                            ├── 可动态新增                          │
│  ├── DEPARTMENT_AND_BELOW           ├── scope_level 用于合并                │
│  ├── DEPARTMENT                     └── calc_type 定义计算逻辑              │
│  ├── GRADE                                                                  │
│  ├── CLASS                          变化: 枚举 → 配置表                     │
│  ├── CUSTOM                         好处: 新增范围类型无需改代码             │
│  └── SELF                                                                   │
│                                                                             │
│  ──────────────────────────────────────────────────────────────────────────│
│                                                                             │
│  DataModule (Java枚举)              data_modules (配置表)                   │
│  ├── ORG_UNIT                       ├── filter_fields (JSON)               │
│  ├── STUDENT                        ├── main_table                         │
│  ├── DORMITORY                      └── 按 domain 分组                      │
│  ├── CLASSROOM                                                              │
│  ├── INSPECTION_TEMPLATE            变化: 枚举 → 配置表                     │
│  ├── INSPECTION_RECORD              好处: 新增模块无需改代码                 │
│  ├── APPEAL                               filter_fields 定义过滤字段        │
│  └── ...                                                                    │
│                                                                             │
│  ──────────────────────────────────────────────────────────────────────────│
│                                                                             │
│  role_data_permissions              role_data_permissions (优化)            │
│  ├── roleId                         ├── role_id                            │
│  ├── moduleCode                     ├── module_code (关联配置表)            │
│  ├── dataScope (Integer)            ├── scope_code (关联配置表)             │
│  ├── customDeptIds (逗号分隔)       └── 关联 role_data_scope_items          │
│  └── customClassIds (逗号分隔)                                              │
│                                     变化: 自定义范围独立到明细表             │
│                                     好处: 支持更多类型的自定义范围           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 四、检查系统权限配置

### 4.1 检查系统功能权限

```sql
-- 检查系统权限初始化数据
INSERT INTO permissions (permission_code, permission_name, module_code, resource_code, action_code, permission_type, parent_id, tree_level, sort_order) VALUES
-- 量化检查模块 (顶层)
('inspection', '量化检查', 'inspection', NULL, NULL, 'MODULE', NULL, 0, 300),

-- 检查模板
('inspection:template', '检查模板', 'inspection', 'template', NULL, 'MENU', (SELECT id FROM permissions WHERE permission_code = 'inspection'), 1, 301),
('inspection:template:view', '查看模板', 'inspection', 'template', 'view', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:template'), 2, 1),
('inspection:template:create', '创建模板', 'inspection', 'template', 'create', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:template'), 2, 2),
('inspection:template:update', '修改模板', 'inspection', 'template', 'update', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:template'), 2, 3),
('inspection:template:delete', '删除模板', 'inspection', 'template', 'delete', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:template'), 2, 4),

-- 检查项目
('inspection:project', '检查项目', 'inspection', 'project', NULL, 'MENU', (SELECT id FROM permissions WHERE permission_code = 'inspection'), 1, 302),
('inspection:project:view', '查看项目', 'inspection', 'project', 'view', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:project'), 2, 1),
('inspection:project:create', '创建项目', 'inspection', 'project', 'create', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:project'), 2, 2),
('inspection:project:manage', '管理项目', 'inspection', 'project', 'manage', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:project'), 2, 3),
('inspection:project:delete', '删除项目', 'inspection', 'project', 'delete', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:project'), 2, 4),

-- 检查任务
('inspection:task', '检查任务', 'inspection', 'task', NULL, 'MENU', (SELECT id FROM permissions WHERE permission_code = 'inspection'), 1, 303),
('inspection:task:view', '查看任务', 'inspection', 'task', 'view', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:task'), 2, 1),
('inspection:task:execute', '执行检查', 'inspection', 'task', 'execute', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:task'), 2, 2),
('inspection:task:review', '审核任务', 'inspection', 'task', 'review', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:task'), 2, 3),
('inspection:task:publish', '发布任务', 'inspection', 'task', 'publish', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:task'), 2, 4),

-- 检查记录
('inspection:record', '检查记录', 'inspection', 'record', NULL, 'MENU', (SELECT id FROM permissions WHERE permission_code = 'inspection'), 1, 304),
('inspection:record:view', '查看记录', 'inspection', 'record', 'view', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:record'), 2, 1),
('inspection:record:create', '录入记录', 'inspection', 'record', 'create', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:record'), 2, 2),
('inspection:record:update', '修改记录', 'inspection', 'record', 'update', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:record'), 2, 3),
('inspection:record:delete', '删除记录', 'inspection', 'record', 'delete', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:record'), 2, 4),

-- 申诉管理
('inspection:appeal', '申诉管理', 'inspection', 'appeal', NULL, 'MENU', (SELECT id FROM permissions WHERE permission_code = 'inspection'), 1, 305),
('inspection:appeal:view', '查看申诉', 'inspection', 'appeal', 'view', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:appeal'), 2, 1),
('inspection:appeal:create', '提交申诉', 'inspection', 'appeal', 'create', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:appeal'), 2, 2),
('inspection:appeal:review', '审核申诉', 'inspection', 'appeal', 'review', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:appeal'), 2, 3),

-- 整改管理
('inspection:corrective', '整改管理', 'inspection', 'corrective', NULL, 'MENU', (SELECT id FROM permissions WHERE permission_code = 'inspection'), 1, 306),
('inspection:corrective:view', '查看整改', 'inspection', 'corrective', 'view', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:corrective'), 2, 1),
('inspection:corrective:create', '下发整改', 'inspection', 'corrective', 'create', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:corrective'), 2, 2),
('inspection:corrective:execute', '执行整改', 'inspection', 'corrective', 'execute', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:corrective'), 2, 3),
('inspection:corrective:verify', '复查整改', 'inspection', 'corrective', 'verify', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:corrective'), 2, 4),

-- 汇总排名
('inspection:summary', '汇总排名', 'inspection', 'summary', NULL, 'MENU', (SELECT id FROM permissions WHERE permission_code = 'inspection'), 1, 307),
('inspection:summary:view', '查看汇总', 'inspection', 'summary', 'view', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:summary'), 2, 1),
('inspection:summary:generate', '生成汇总', 'inspection', 'summary', 'generate', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:summary'), 2, 2),
('inspection:summary:publish', '发布排名', 'inspection', 'summary', 'publish', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:summary'), 2, 3),

-- 学生个人 (学生端)
('inspection:personal', '个人检查', 'inspection', 'personal', NULL, 'MENU', (SELECT id FROM permissions WHERE permission_code = 'inspection'), 1, 310),
('inspection:personal:view', '查看个人记录', 'inspection', 'personal', 'view', 'BUTTON', (SELECT id FROM permissions WHERE permission_code = 'inspection:personal'), 2, 1);
```

### 4.2 典型角色配置

```yaml
# =====================================================
# 角色1: 超级管理员
# =====================================================
SUPER_ADMIN:
  role_name: 超级管理员
  role_type: SYSTEM
  role_level: 1

  功能权限: "*"  # 所有权限

  数据权限:
    所有模块: ALL

# =====================================================
# 角色2: 系部主任
# =====================================================
DEPT_DIRECTOR:
  role_name: 系部主任
  role_type: CUSTOM
  role_level: 20

  功能权限:
    - student:*                      # 学生管理全部
    - school_class:*                 # 班级管理全部
    - dormitory:*:view               # 宿舍查看
    - dormitory:checkin:*            # 入住管理全部
    - inspection:*                   # 量化检查全部

  数据权限:
    student: DEPARTMENT_AND_BELOW
    school_class: DEPARTMENT_AND_BELOW
    dormitory_room: DEPARTMENT_AND_BELOW
    dormitory_checkin: DEPARTMENT_AND_BELOW
    inspection_template: DEPARTMENT_AND_BELOW
    inspection_project: DEPARTMENT_AND_BELOW
    inspection_task: DEPARTMENT_AND_BELOW
    inspection_record: DEPARTMENT_AND_BELOW
    inspection_appeal: DEPARTMENT_AND_BELOW
    inspection_summary: DEPARTMENT_AND_BELOW
    inspection_corrective: DEPARTMENT_AND_BELOW

# =====================================================
# 角色3: 班主任
# =====================================================
CLASS_TEACHER:
  role_name: 班主任
  role_type: CUSTOM
  role_level: 40

  功能权限:
    - student:info:view
    - student:info:update
    - dormitory:room:view
    - dormitory:checkin:view
    - inspection:record:view
    - inspection:appeal:view
    - inspection:appeal:create          # 代学生提交申诉
    - inspection:summary:view
    - inspection:corrective:view
    - inspection:corrective:execute     # 执行整改

  数据权限:
    student: CUSTOM                      # 仅管理的班级
    dormitory_checkin: CUSTOM            # 仅管理的班级
    inspection_record: CUSTOM            # 仅管理的班级
    inspection_appeal: CUSTOM
    inspection_summary: CUSTOM
    inspection_corrective: CUSTOM

    # 自定义范围明细 (分配角色时配置)
    custom_scope:
      - type: CLASS
        id: {管理的班级ID}

# =====================================================
# 角色4: 宿管员
# =====================================================
DORM_MANAGER:
  role_name: 宿管员
  role_type: CUSTOM
  role_level: 50

  功能权限:
    - dormitory:building:view
    - dormitory:room:view
    - dormitory:room:update
    - dormitory:checkin:*
    - inspection:task:view
    - inspection:task:execute
    - inspection:record:view
    - inspection:record:create

  数据权限:
    dormitory_building: CUSTOM           # 仅负责的楼栋
    dormitory_room: CUSTOM
    dormitory_checkin: CUSTOM
    inspection_task: SELF                # 仅分配给自己的任务
    inspection_record: SELF              # 仅自己录入的记录

    custom_scope:
      - type: BUILDING
        id: {负责的楼栋ID}

# =====================================================
# 角色5: 检查员
# =====================================================
INSPECTOR:
  role_name: 检查员
  role_type: CUSTOM
  role_level: 50

  功能权限:
    - inspection:task:view
    - inspection:task:execute
    - inspection:record:view
    - inspection:record:create
    - inspection:record:update           # 未发布前可改

  数据权限:
    inspection_task: SELF                # 仅分配给自己的任务
    inspection_record: SELF              # 仅自己录入的记录

# =====================================================
# 角色6: 学生
# =====================================================
STUDENT:
  role_name: 学生
  role_type: SYSTEM
  role_level: 100

  功能权限:
    - inspection:personal:view           # 查看个人检查记录
    - inspection:appeal:view             # 查看申诉
    - inspection:appeal:create           # 提交申诉
    - profile:view
    - profile:password:change

  数据权限:
    inspection_record: SELF              # 仅本人记录
    inspection_appeal: SELF              # 仅本人申诉

# =====================================================
# 角色7: 模板审核员 (新增)
# =====================================================
TEMPLATE_REVIEWER:
  role_name: 模板审核员
  role_type: CUSTOM
  role_level: 30

  功能权限:
    - inspection:template:view
    - inspection:template:review         # 审核公开申请

  数据权限:
    inspection_template: ALL             # 可查看所有模板
```

---

## 五、后端实现设计

### 5.1 核心服务类

```java
/**
 * 权限服务 - 核心实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleDataPermissionRepository dataPermissionRepository;
    private final DataScopeTypeRepository scopeTypeRepository;
    private final DataModuleRepository moduleRepository;
    private final ScopeItemTypeRepository itemTypeRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final CacheManager cacheManager;

    /**
     * 检查功能权限
     */
    public boolean hasPermission(Long userId, String permissionCode) {
        Set<String> permissions = getUserPermissions(userId);

        // 支持通配符
        if (permissions.contains("*")) {
            return true;
        }

        // 精确匹配
        if (permissions.contains(permissionCode)) {
            return true;
        }

        // 模块级通配符匹配 (inspection:* 匹配 inspection:record:view)
        String[] parts = permissionCode.split(":");
        if (parts.length >= 2) {
            if (permissions.contains(parts[0] + ":*")) {
                return true;
            }
            if (parts.length >= 3 && permissions.contains(parts[0] + ":" + parts[1] + ":*")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取用户所有功能权限 (带缓存)
     */
    @Cacheable(value = "user_permissions", key = "#userId")
    public Set<String> getUserPermissions(Long userId) {
        List<Long> roleIds = getEffectiveRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        return rolePermissionRepository.findPermissionCodesByRoleIds(roleIds);
    }

    /**
     * 获取用户有效角色ID (过滤过期角色)
     */
    private List<Long> getEffectiveRoleIds(Long userId) {
        return userRoleRepository.findByUserId(userId).stream()
            .filter(ur -> ur.getExpiresAt() == null || ur.getExpiresAt().isAfter(LocalDateTime.now()))
            .map(UserRole::getRoleId)
            .distinct()
            .toList();
    }

    /**
     * 获取用户在指定模块的数据范围 (核心方法)
     */
    public DataScopeResult getDataScope(Long userId, String moduleCode) {
        // 1. 获取用户有效角色
        List<Long> roleIds = getEffectiveRoleIds(userId);
        if (roleIds.isEmpty()) {
            return DataScopeResult.none();
        }

        // 2. 获取所有角色在该模块的数据权限配置
        List<RoleDataPermission> permissions =
            dataPermissionRepository.findByRoleIdsAndModuleCode(roleIds, moduleCode);

        if (permissions.isEmpty()) {
            // 无配置，默认 SELF
            return DataScopeResult.self(userId);
        }

        // 3. 合并多个角色的数据权限 (取最大范围)
        return mergeDataScopes(userId, permissions);
    }

    /**
     * 合并多个数据权限配置 (取最大范围)
     */
    private DataScopeResult mergeDataScopes(Long userId, List<RoleDataPermission> permissions) {
        // 获取所有范围类型配置 (用于比较优先级)
        Map<String, DataScopeType> scopeTypes = scopeTypeRepository.findAllEnabled().stream()
            .collect(Collectors.toMap(DataScopeType::getScopeCode, Function.identity()));

        // 找出最大范围
        RoleDataPermission maxPermission = permissions.stream()
            .max(Comparator.comparing(p -> scopeTypes.getOrDefault(p.getScopeCode(),
                new DataScopeType()).getScopeLevel()))
            .orElse(null);

        if (maxPermission == null) {
            return DataScopeResult.self(userId);
        }

        String scopeCode = maxPermission.getScopeCode();
        DataScopeType scopeType = scopeTypes.get(scopeCode);

        // 根据 calc_type 计算数据范围
        return switch (scopeType.getCalcType()) {
            case "NONE" -> DataScopeResult.all();

            case "USER_ORG" -> {
                Long userOrgUnitId = getUserOrgUnitId(userId);
                yield DataScopeResult.orgUnits(List.of(userOrgUnitId));
            }

            case "USER_ORG_TREE" -> {
                Long userOrgUnitId = getUserOrgUnitId(userId);
                List<Long> orgUnitIds = orgUnitRepository.findIdsByParentPath(userOrgUnitId);
                yield DataScopeResult.orgUnits(orgUnitIds);
            }

            case "CUSTOM_CONFIG" -> {
                // 合并所有 CUSTOM 类型的自定义范围
                yield buildCustomScope(userId, permissions.stream()
                    .filter(p -> "CUSTOM".equals(p.getScopeCode()))
                    .toList());
            }

            case "CREATOR" -> DataScopeResult.self(userId);

            default -> DataScopeResult.none();
        };
    }

    /**
     * 构建自定义范围
     */
    private DataScopeResult buildCustomScope(Long userId, List<RoleDataPermission> customPermissions) {
        Set<Long> orgUnitIds = new HashSet<>();
        Set<Long> classIds = new HashSet<>();
        Set<Long> buildingIds = new HashSet<>();

        for (RoleDataPermission permission : customPermissions) {
            List<RoleDataScopeItem> items = permission.getScopeItems();

            for (RoleDataScopeItem item : items) {
                switch (item.getItemTypeCode()) {
                    case "ORG_UNIT" -> {
                        orgUnitIds.add(item.getScopeId());
                        if (item.isIncludeChildren()) {
                            orgUnitIds.addAll(orgUnitRepository.findChildIds(item.getScopeId()));
                        }
                    }
                    case "CLASS" -> classIds.add(item.getScopeId());
                    case "BUILDING" -> buildingIds.add(item.getScopeId());
                    // 其他类型...
                }
            }
        }

        return DataScopeResult.custom(orgUnitIds, classIds, buildingIds);
    }

    /**
     * 清除用户权限缓存
     */
    @CacheEvict(value = "user_permissions", key = "#userId")
    public void clearUserPermissionCache(Long userId) {
        log.debug("清除用户权限缓存, userId={}", userId);
    }

    /**
     * 清除角色相关用户的权限缓存
     */
    public void clearRolePermissionCache(Long roleId) {
        List<Long> userIds = userRoleRepository.findUserIdsByRoleId(roleId);
        userIds.forEach(this::clearUserPermissionCache);
    }
}
```

### 5.2 数据权限拦截器

```java
/**
 * MyBatis 数据权限拦截器
 */
@Intercepts({
    @Signature(type = Executor.class, method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
@Component
@RequiredArgsConstructor
@Slf4j
public class DataPermissionInterceptor implements Interceptor {

    private final PermissionService permissionService;
    private final DataModuleRepository moduleRepository;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 获取当前数据权限上下文
        DataPermissionContext context = DataPermissionHolder.getContext();
        if (context == null || !context.isEnabled()) {
            return invocation.proceed();
        }

        // 2. 获取当前用户
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return invocation.proceed();
        }

        // 3. 获取用户的数据范围
        DataScopeResult scope = permissionService.getDataScope(userId, context.getModuleCode());

        // 4. 如果是 ALL，不做限制
        if (scope.isAll()) {
            DataPermissionHolder.clear();
            return invocation.proceed();
        }

        // 5. 获取模块配置
        DataModule module = moduleRepository.findByCode(context.getModuleCode());
        if (module == null || module.getFilterFields() == null) {
            DataPermissionHolder.clear();
            return invocation.proceed();
        }

        // 6. 修改 SQL
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        BoundSql boundSql = ms.getBoundSql(parameter);
        String originalSql = boundSql.getSql();

        String filterSql = buildFilterSql(scope, module.getFilterFields());
        if (!filterSql.isEmpty()) {
            String newSql = injectWhereClause(originalSql, filterSql);

            // 重新构建 BoundSql
            MappedStatement newMs = copyMappedStatement(ms, new BoundSqlSqlSource(
                ms.getConfiguration(), newSql, boundSql.getParameterMappings(), parameter
            ));
            invocation.getArgs()[0] = newMs;
        }

        DataPermissionHolder.clear();
        return invocation.proceed();
    }

    /**
     * 根据数据范围和模块配置构建过滤SQL
     */
    private String buildFilterSql(DataScopeResult scope, Map<String, String> filterFields) {
        List<String> conditions = new ArrayList<>();

        // 部门过滤
        if (!scope.getOrgUnitIds().isEmpty() && filterFields.containsKey("org_unit")) {
            String field = filterFields.get("org_unit");
            conditions.add(String.format("%s IN (%s)", field, joinIds(scope.getOrgUnitIds())));
        }

        // 班级过滤
        if (!scope.getClassIds().isEmpty() && filterFields.containsKey("class")) {
            String field = filterFields.get("class");
            conditions.add(String.format("%s IN (%s)", field, joinIds(scope.getClassIds())));
        }

        // 楼栋过滤
        if (!scope.getBuildingIds().isEmpty() && filterFields.containsKey("building")) {
            String field = filterFields.get("building");
            conditions.add(String.format("%s IN (%s)", field, joinIds(scope.getBuildingIds())));
        }

        // 创建者过滤 (SELF)
        if (scope.isSelf() && filterFields.containsKey("creator")) {
            String field = filterFields.get("creator");
            conditions.add(String.format("%s = %d", field, scope.getUserId()));
        }

        if (conditions.isEmpty()) {
            return "";
        }

        // 多条件用 OR 连接 (同一范围内用 IN，不同范围类型用 OR)
        return conditions.size() == 1
            ? conditions.get(0)
            : "(" + String.join(" OR ", conditions) + ")";
    }

    private String joinIds(Collection<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
```

### 5.3 数据权限注解

```java
/**
 * 数据权限注解 - 标记在 Controller 或 Service 方法上
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermission {

    /**
     * 数据模块编码 (对应 data_modules.module_code)
     */
    String module();

    /**
     * 是否启用 (可临时禁用)
     */
    boolean enabled() default true;
}

/**
 * AOP 切面 - 设置数据权限上下文
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataPermissionAspect {

    @Around("@annotation(dataPermission)")
    public Object around(ProceedingJoinPoint point, DataPermission dataPermission) throws Throwable {
        try {
            if (dataPermission.enabled()) {
                DataPermissionHolder.setContext(new DataPermissionContext(
                    dataPermission.module(),
                    true
                ));
            }
            return point.proceed();
        } finally {
            DataPermissionHolder.clear();
        }
    }
}
```

### 5.4 Controller 使用示例

```java
@RestController
@RequestMapping("/api/inspection/records")
@RequiredArgsConstructor
@Tag(name = "检查记录", description = "检查记录管理接口")
public class InspectionRecordController {

    private final InspectionRecordApplicationService recordService;

    @GetMapping
    @PreAuthorize("hasAuthority('inspection:record:view')")
    @DataPermission(module = "inspection_record")
    @Operation(summary = "查询检查记录列表")
    public Result<Page<InspectionRecordVO>> list(InspectionRecordQuery query) {
        // 数据权限自动应用，无需手动过滤
        return Result.success(recordService.queryPage(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    @DataPermission(module = "inspection_record")
    @Operation(summary = "获取检查记录详情")
    public Result<InspectionRecordVO> getById(@PathVariable Long id) {
        return Result.success(recordService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inspection:record:create')")
    @Operation(summary = "创建检查记录")
    public Result<Long> create(@RequestBody @Valid CreateRecordCommand cmd) {
        return Result.success(recordService.create(cmd));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inspection:record:update')")
    @DataPermission(module = "inspection_record")
    @Operation(summary = "更新检查记录")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UpdateRecordCommand cmd) {
        recordService.update(id, cmd);
        return Result.success();
    }
}
```

---

## 六、前端实现设计

### 6.1 权限配置界面

```vue
<!-- RoleDataPermissionConfig.vue - 角色数据权限配置 -->
<template>
  <el-dialog title="数据权限配置" v-model="visible" width="800px">
    <div class="role-info">
      <span>角色：{{ role.roleName }}</span>
    </div>

    <!-- 按领域分组的模块列表 -->
    <div v-for="domain in groupedModules" :key="domain.code" class="domain-group">
      <div class="domain-header">{{ domain.name }}</div>

      <el-table :data="domain.modules" border>
        <el-table-column prop="moduleName" label="数据模块" width="150" />

        <el-table-column label="数据范围">
          <template #default="{ row }">
            <el-select
              v-model="row.scopeCode"
              placeholder="选择数据范围"
              @change="onScopeChange(row)"
            >
              <el-option
                v-for="scope in scopeTypes"
                :key="scope.scopeCode"
                :label="scope.scopeName"
                :value="scope.scopeCode"
              />
            </el-select>
          </template>
        </el-table-column>

        <!-- CUSTOM 时显示配置按钮 -->
        <el-table-column label="自定义范围" width="150">
          <template #default="{ row }">
            <el-button
              v-if="row.scopeCode === 'CUSTOM'"
              type="primary"
              link
              @click="openScopeItemDialog(row)"
            >
              配置范围 ({{ row.scopeItems?.length || 0 }})
            </el-button>
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>

    <!-- 自定义范围配置对话框 -->
    <CustomScopeDialog
      v-model="scopeDialogVisible"
      :module="currentModule"
      :items="currentScopeItems"
      @save="onScopeItemsSave"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { roleDataPermissionApi } from '@/api/access'

const props = defineProps<{
  roleId: number
}>()

const visible = defineModel<boolean>()
const role = ref({})
const groupedModules = ref([])
const scopeTypes = ref([])
const modulePermissions = ref<Record<string, { scopeCode: string; scopeItems: any[] }>>({})

// 加载数据
onMounted(async () => {
  const [modulesRes, scopesRes, configRes] = await Promise.all([
    roleDataPermissionApi.getModules(),
    roleDataPermissionApi.getScopes(),
    roleDataPermissionApi.getConfig(props.roleId)
  ])

  groupedModules.value = modulesRes
  scopeTypes.value = scopesRes

  // 初始化配置
  configRes.modulePermissions.forEach(mp => {
    modulePermissions.value[mp.moduleCode] = {
      scopeCode: mp.scopeCode,
      scopeItems: mp.scopeItems || []
    }
  })
})

// 保存配置
async function handleSave() {
  const config = {
    roleId: props.roleId,
    modulePermissions: Object.entries(modulePermissions.value).map(([moduleCode, config]) => ({
      moduleCode,
      scopeCode: config.scopeCode,
      scopeItems: config.scopeItems
    }))
  }

  await roleDataPermissionApi.saveConfig(props.roleId, config)
  ElMessage.success('保存成功')
  visible.value = false
}
</script>
```

### 6.2 API 定义

```typescript
// api/access.ts - 数据权限 API

export interface DataScopeType {
  scopeCode: string
  scopeName: string
  scopeLevel: number
  calcType: string
  description: string
}

export interface DataModule {
  moduleCode: string
  moduleName: string
  domainCode: string
  domainName: string
}

export interface GroupedModules {
  code: string
  name: string
  modules: DataModule[]
}

export interface ScopeItem {
  itemTypeCode: string
  scopeId: number
  scopeName: string
  includeChildren: boolean
}

export interface ModulePermission {
  moduleCode: string
  scopeCode: string
  scopeItems?: ScopeItem[]
}

export interface RolePermissionConfig {
  roleId: number
  roleName: string
  modulePermissions: ModulePermission[]
}

export const roleDataPermissionApi = {
  // 获取所有数据模块 (按领域分组)
  getModules(): Promise<GroupedModules[]> {
    return http.get('/roles/data-permissions/modules')
  },

  // 获取所有数据范围类型
  getScopes(): Promise<DataScopeType[]> {
    return http.get('/roles/data-permissions/scopes')
  },

  // 获取角色数据权限配置
  getConfig(roleId: number): Promise<RolePermissionConfig> {
    return http.get(`/roles/${roleId}/data-permissions`)
  },

  // 保存角色数据权限配置
  saveConfig(roleId: number, config: RolePermissionConfig): Promise<void> {
    return http.put(`/roles/${roleId}/data-permissions`, config)
  },

  // 获取自定义范围可选项 (根据类型动态加载)
  getScopeItemOptions(itemTypeCode: string, search?: string): Promise<{ id: number; name: string }[]> {
    return http.get('/roles/data-permissions/scope-items', { params: { itemTypeCode, search } })
  }
}
```

---

## 七、量化检查系统数据库设计

### 7.1 核心表结构

```sql
-- =====================================================
-- 检查模板表
-- =====================================================
CREATE TABLE inspection_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    template_code VARCHAR(50) NOT NULL,
    template_name VARCHAR(100) NOT NULL,

    -- 可见性
    visibility ENUM('PRIVATE', 'DEPARTMENT', 'PUBLIC') DEFAULT 'PRIVATE',

    -- 归属
    created_by BIGINT NOT NULL,
    org_unit_id BIGINT,

    -- 配置
    base_score DECIMAL(10,2) DEFAULT 100.00,
    scoring_mode ENUM('DEDUCTION', 'ADDITION', 'CHECKLIST', 'BONUS_ONLY') DEFAULT 'DEDUCTION',
    target_type ENUM('STUDENT', 'CLASS', 'DORMITORY', 'CLASSROOM') NOT NULL,

    -- 状态
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


-- =====================================================
-- 模板类别表
-- =====================================================
CREATE TABLE template_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,

    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(100) NOT NULL,

    -- 权重配置
    weight DECIMAL(5,2) DEFAULT 1.00,
    max_score DECIMAL(10,2),

    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    description VARCHAR(500),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_template_category (template_id, category_code),
    INDEX idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板类别表';


-- =====================================================
-- 类别扣分项表
-- =====================================================
CREATE TABLE category_score_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,

    item_code VARCHAR(50) NOT NULL,
    item_name VARCHAR(200) NOT NULL,

    -- 分值
    score DECIMAL(10,2) NOT NULL,
    score_type ENUM('DEDUCTION', 'BONUS') DEFAULT 'DEDUCTION',

    -- 是否需要证据
    require_evidence TINYINT DEFAULT 0,

    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    description VARCHAR(500),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='类别扣分项表';


-- =====================================================
-- 检查项目表
-- =====================================================
CREATE TABLE inspection_projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    project_code VARCHAR(50) NOT NULL,
    project_name VARCHAR(100) NOT NULL,

    -- 模板关联 (复制模板快照)
    template_id BIGINT,
    template_snapshot JSON,

    -- 检查范围
    inspection_level ENUM('CLASS', 'GRADE', 'DEPARTMENT') DEFAULT 'CLASS',
    target_org_unit_ids JSON,

    -- 时间配置
    start_date DATE NOT NULL,
    end_date DATE,
    semester_id BIGINT,

    -- 录入模式
    entry_mode ENUM('SPACE', 'PERSON', 'CLASS', 'ITEM', 'CHECKLIST') DEFAULT 'SPACE',

    -- 公平权重配置
    fair_weight_enabled TINYINT DEFAULT 0,
    fair_weight_mode ENUM('DIVIDE', 'BENCHMARK'),
    benchmark_count INT,

    -- 混合宿舍策略
    mixed_dormitory_strategy ENUM('RATIO', 'AVERAGE', 'FULL', 'MAIN') DEFAULT 'RATIO',

    -- 汇总配置
    auto_generate_summary TINYINT DEFAULT 1,
    summary_cron VARCHAR(50) DEFAULT '0 0 2 * * ?',

    -- 状态
    status ENUM('DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED') DEFAULT 'DRAFT',

    -- 归属
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


-- =====================================================
-- 检查任务表
-- =====================================================
CREATE TABLE inspection_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    task_code VARCHAR(50) NOT NULL,
    project_id BIGINT NOT NULL,

    -- 检查日期
    inspection_date DATE NOT NULL,

    -- 检查人员
    inspector_ids JSON,

    -- 状态
    status ENUM('DRAFT', 'IN_PROGRESS', 'SUBMITTED', 'REVIEWED', 'PUBLISHED') DEFAULT 'DRAFT',

    -- 时间戳
    submitted_at DATETIME,
    submitted_by BIGINT,
    reviewed_at DATETIME,
    reviewed_by BIGINT,
    published_at DATETIME,
    published_by BIGINT,

    -- 归属
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


-- =====================================================
-- 目标检查记录表 (核心记录表)
-- =====================================================
CREATE TABLE target_inspection_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    task_id BIGINT NOT NULL,

    -- 检查目标 (多态)
    target_type ENUM('STUDENT', 'CLASS', 'DORMITORY', 'CLASSROOM') NOT NULL,
    target_id BIGINT NOT NULL,

    -- 目标信息快照 (检查时刻的状态)
    target_snapshot JSON,

    -- 班级归属快照 (所有检查最终归属班级)
    class_id BIGINT NOT NULL,
    class_name VARCHAR(100),
    class_code VARCHAR(50),

    -- 组织归属快照 (用于数据权限)
    org_unit_id BIGINT NOT NULL,
    org_unit_name VARCHAR(100),

    -- 分数
    base_score DECIMAL(10,2) NOT NULL,
    raw_score DECIMAL(10,2) NOT NULL,
    weighted_score DECIMAL(10,2),
    fair_adjusted_score DECIMAL(10,2),

    -- 扣分/加分统计
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
    INDEX idx_org_unit_id (org_unit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目标检查记录表';


-- =====================================================
-- 扣分/加分明细记录表
-- =====================================================
CREATE TABLE deduction_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    target_record_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    score_item_id BIGINT,

    -- 分值
    score DECIMAL(10,2) NOT NULL,
    score_type ENUM('DEDUCTION', 'BONUS') NOT NULL,

    -- 数量 (同一项目多次扣分)
    quantity INT DEFAULT 1,

    -- 关联学生 (按学生录入时)
    student_id BIGINT,
    student_name VARCHAR(50),
    student_no VARCHAR(50),

    -- 备注
    remark VARCHAR(500),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,

    INDEX idx_target_record_id (target_record_id),
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扣分/加分明细记录表';


-- =====================================================
-- 检查证据表
-- =====================================================
CREATE TABLE inspection_evidences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    deduction_record_id BIGINT NOT NULL,

    -- 文件信息
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,

    -- 缩略图
    thumbnail_path VARCHAR(500),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,

    INDEX idx_deduction_record_id (deduction_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查证据表';


-- =====================================================
-- 申诉表
-- =====================================================
CREATE TABLE appeals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    appeal_code VARCHAR(50) NOT NULL,
    deduction_record_id BIGINT NOT NULL,
    target_record_id BIGINT NOT NULL,

    -- 申诉人
    appellant_id BIGINT NOT NULL,
    appellant_type ENUM('STUDENT', 'TEACHER') NOT NULL,

    -- 申诉内容
    reason TEXT NOT NULL,
    evidence_ids JSON,

    -- 状态流转
    status ENUM('PENDING', 'REVIEWING', 'APPROVED', 'REJECTED', 'CANCELLED') DEFAULT 'PENDING',

    -- 审核信息
    reviewer_id BIGINT,
    review_comment TEXT,
    reviewed_at DATETIME,

    -- 分数调整
    score_adjustment DECIMAL(10,2),

    -- 归属 (用于数据权限)
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


-- =====================================================
-- 整改工单表
-- =====================================================
CREATE TABLE corrective_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    order_code VARCHAR(50) NOT NULL,
    target_record_id BIGINT NOT NULL,

    -- 整改要求
    issue_description TEXT NOT NULL,
    requirement TEXT,

    -- 责任人
    responsible_id BIGINT NOT NULL,
    responsible_type ENUM('CLASS_TEACHER', 'DEPT_ADMIN') NOT NULL,

    -- 期限配置
    deadline DATE NOT NULL,
    max_rounds INT DEFAULT 3,
    current_round INT DEFAULT 1,

    -- 是否影响扣分
    affect_score TINYINT DEFAULT 0,
    score_adjustment DECIMAL(10,2),

    -- 状态
    status ENUM('PENDING', 'IN_PROGRESS', 'SUBMITTED', 'VERIFIED', 'REJECTED', 'COMPLETED', 'OVERDUE') DEFAULT 'PENDING',

    -- 整改提交
    corrective_description TEXT,
    corrective_evidence_ids JSON,
    submitted_at DATETIME,

    -- 复查
    verifier_id BIGINT,
    verify_comment TEXT,
    verified_at DATETIME,

    -- 归属
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


-- =====================================================
-- 每日汇总表
-- =====================================================
CREATE TABLE daily_summaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    project_id BIGINT NOT NULL,
    summary_date DATE NOT NULL,

    -- 汇总维度
    class_id BIGINT NOT NULL,
    class_name VARCHAR(100),
    org_unit_id BIGINT NOT NULL,
    org_unit_name VARCHAR(100),

    -- 分数汇总
    total_base_score DECIMAL(10,2),
    total_raw_score DECIMAL(10,2),
    total_weighted_score DECIMAL(10,2),
    total_fair_score DECIMAL(10,2),

    final_score DECIMAL(10,2),

    -- 统计
    total_deduction DECIMAL(10,2),
    total_bonus DECIMAL(10,2),
    record_count INT,

    -- 评级
    rating VARCHAR(20),
    rating_color VARCHAR(20),

    -- 排名
    rank_in_grade INT,
    rank_in_department INT,
    rank_in_school INT,

    -- 状态
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


-- =====================================================
-- 自动加分规则表
-- =====================================================
CREATE TABLE auto_bonus_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    project_id BIGINT NOT NULL,

    rule_code VARCHAR(50) NOT NULL,
    rule_name VARCHAR(100) NOT NULL,

    -- 规则类型
    rule_type ENUM('CONSECUTIVE_FULL_SCORE', 'MONTHLY_EXCELLENT', 'IMPROVEMENT', 'CUSTOM') NOT NULL,

    -- 触发条件 (JSON)
    trigger_condition JSON NOT NULL,

    -- 加分值
    bonus_score DECIMAL(10,2) NOT NULL,

    is_enabled TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,

    UNIQUE KEY uk_project_rule (project_id, rule_code),
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动加分规则表';
```

---

## 八、实施计划

### 8.1 分阶段实施

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           实施路线图                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Phase 1: 权限系统优化                                                       │
│  ────────────────────                                                       │
│  ├── 创建配置表 (data_scope_types, data_modules, scope_item_types)          │
│  ├── 迁移现有权限数据                                                        │
│  ├── 实现 PermissionService 优化版                                          │
│  ├── 实现 DataPermissionInterceptor                                        │
│  ├── 前端权限配置界面优化                                                    │
│  └── 测试验证                                                               │
│                                                                             │
│  Phase 2: 检查系统基础                                                       │
│  ────────────────────                                                       │
│  ├── 创建检查系统数据库表                                                    │
│  ├── 实现 Template、Category、ScoreItem 领域模型                            │
│  ├── 实现模板管理 API                                                       │
│  ├── 前端模板管理页面                                                        │
│  └── 权限配置 (inspection_template 模块)                                    │
│                                                                             │
│  Phase 3: 项目与任务                                                         │
│  ────────────────────                                                       │
│  ├── 实现 Project 领域模型                                                  │
│  ├── 实现 Task 领域模型                                                     │
│  ├── 项目配置 (权重、录入模式、公平权重)                                      │
│  ├── 任务生成与分配                                                          │
│  ├── 前端项目/任务管理页面                                                   │
│  └── 权限配置 (inspection_project, inspection_task 模块)                    │
│                                                                             │
│  Phase 4: 检查执行                                                           │
│  ────────────────────                                                       │
│  ├── 实现多种录入模式 (SPACE/PERSON/CLASS/ITEM/CHECKLIST)                   │
│  ├── 混合宿舍分数分配                                                        │
│  ├── 公平权重计算                                                            │
│  ├── 证据上传                                                               │
│  ├── 前端检查执行页面                                                        │
│  └── 权限配置 (inspection_record 模块)                                      │
│                                                                             │
│  Phase 5: 申诉与整改                                                         │
│  ────────────────────                                                       │
│  ├── 实现 Appeal 领域模型                                                   │
│  ├── 实现 CorrectiveOrder 领域模型                                          │
│  ├── 整改责任人自动分配                                                      │
│  ├── 前端申诉/整改页面                                                       │
│  └── 权限配置 (inspection_appeal, inspection_corrective 模块)               │
│                                                                             │
│  Phase 6: 汇总与排名                                                         │
│  ────────────────────                                                       │
│  ├── 实现汇总生成 (定时 + 手动)                                             │
│  ├── 排名计算 (标准竞赛排名)                                                │
│  ├── 自动加分规则                                                            │
│  ├── Saga 异步编排                                                          │
│  ├── 前端汇总/排名页面                                                       │
│  └── 权限配置 (inspection_summary 模块)                                     │
│                                                                             │
│  Phase 7: 测试与上线                                                         │
│  ────────────────────                                                       │
│  ├── 功能测试                                                               │
│  ├── 性能测试                                                               │
│  ├── 用户验收测试                                                            │
│  ├── 数据迁移 (如有V4数据)                                                  │
│  └── 正式上线                                                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 8.2 关键里程碑

| 阶段 | 交付物 | 验收标准 |
|------|--------|----------|
| Phase 1 | 权限系统优化 | 无硬编码，配置化管理，数据权限自动过滤 |
| Phase 2 | 模板管理 | 可创建/编辑/删除模板，支持可见性设置 |
| Phase 3 | 项目任务管理 | 可创建项目，自动生成任务，分配检查员 |
| Phase 4 | 检查执行 | 5种录入模式正常工作，混合宿舍分配正确 |
| Phase 5 | 申诉整改 | 完整流程闭环，责任人自动分配 |
| Phase 6 | 汇总排名 | 自动生成汇总，排名发布，自动加分 |
| Phase 7 | 正式上线 | 全功能可用，性能达标，用户验收通过 |

---

## 九、建议与风险

### 9.1 设计建议

1. **权限缓存策略**
   - 用户权限缓存到 Redis，设置合理过期时间
   - 角色变更时主动清除相关用户缓存
   - 考虑使用事件驱动的缓存刷新机制

2. **数据权限性能**
   - 数据量大时，IN 子句可能影响性能
   - 考虑使用临时表或 JOIN 优化
   - 为过滤字段建立索引

3. **模板版本控制**
   - 项目创建时复制模板快照
   - 原模板修改不影响已创建项目
   - 考虑模板版本历史

4. **异步处理**
   - 汇总生成使用异步任务
   - 大量数据导出使用后台任务
   - 通知发送使用消息队列

### 9.2 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 权限配置复杂 | 管理员配置困难 | 提供预设角色模板，简化配置界面 |
| 数据权限性能 | 查询变慢 | 优化SQL，增加索引，考虑缓存 |
| 数据迁移 | 历史数据丢失 | 制定详细迁移计划，保留备份 |
| 并发问题 | 数据不一致 | 使用乐观锁，事务隔离 |

---

## 十、附录

### A. 与现有系统的兼容性

本方案与现有系统完全兼容：

1. **权限系统**: 扩展现有 DDD 架构，保留 Role、Permission 核心模型
2. **数据模块**: 扩展 DataModule 配置，新增检查相关模块
3. **API 风格**: 保持 RESTful 风格，与现有 API 一致
4. **前端架构**: 复用 Vue3 + Element Plus + Pinia 技术栈

### B. 参考文档

- `V5_FINAL_REFACTORING_PLAN.md` - V5设计决策记录
- `V5_COMPLETE_INSPECTION_SYSTEM.md` - 完整V5设计方案
- `PERMISSION_DESIGN.md` - 权限系统设计方案
- `CLAUDE.md` - 项目开发指南

---

---

## 十一、补充决策记录

### 决策18：多角色数据权限合并策略 ✅ 已确认

**问题**：用户同时拥有多个角色，且在同一模块有不同数据范围时如何合并？

**决策**：自定义范围和仅本人取**并集**，而非只取最大范围

**合并规则**：
```java
public DataScopeResult mergeDataScopes(Long userId, List<RoleDataPermission> permissions) {
    // 1. 有 ALL → 直接返回 ALL
    if (hasScope(permissions, "ALL")) {
        return DataScopeResult.all();
    }

    // 2. 收集所有范围（并集）
    Set<Long> orgUnitIds = new HashSet<>();
    Set<Long> classIds = new HashSet<>();
    boolean includeSelf = false;

    for (RoleDataPermission p : permissions) {
        switch (p.getScopeCode()) {
            case "DEPARTMENT_AND_BELOW" -> {
                orgUnitIds.addAll(getOrgUnitTree(userId));
            }
            case "DEPARTMENT" -> {
                orgUnitIds.add(getUserOrgUnitId(userId));
            }
            case "CUSTOM" -> {
                // 合并所有自定义范围
                orgUnitIds.addAll(p.getCustomOrgUnitIds());
                classIds.addAll(p.getCustomClassIds());
            }
            case "SELF" -> {
                includeSelf = true;
            }
        }
    }

    // 3. 返回并集结果
    return DataScopeResult.union(orgUnitIds, classIds, includeSelf ? userId : null);
}
```

**SQL生成**（多条件用 OR 连接）：
```sql
-- 张老师: 班主任(1班) + 检查员(仅本人)
WHERE (
    class_id IN (101)           -- 班主任的自定义范围
    OR created_by = 12345       -- 检查员的仅本人
)
```

### 决策19：班主任管理多个班级 ✅ 已确认

**问题**：一个老师同时当多个班的班主任，怎么配置？

**决策**：采用**方案A** —— 一个角色配置多个班级范围

**配置示例**：
```yaml
用户: 张老师
角色: 班主任
数据权限:
  student: CUSTOM
  inspection_record: CUSTOM

  自定义范围:
    - 类型: CLASS, ID: 101, 名称: 2024级软件1班
    - 类型: CLASS, ID: 102, 名称: 2024级软件2班
```

**界面设计**：
```
┌─────────────────────────────────────────────────────────────┐
│ 数据权限配置 - 班主任                                        │
├─────────────────────────────────────────────────────────────┤
│ 学生信息: [自定义范围 ▼]                                     │
│                                                             │
│ 自定义范围:                                                 │
│ ┌─────────────────────────────────────────────────────────┐│
│ │ + 添加班级                                               ││
│ │                                                         ││
│ │ ☑ 2024级软件1班                          [删除]         ││
│ │ ☑ 2024级软件2班                          [删除]         ││
│ └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 决策20：检查记录的部门归属 ✅ 已确认

**问题**：检查记录的 `org_unit_id` 应该填什么？

**决策**：归属到**被检查对象的部门**，而不是检查项目创建者的部门

**规则**：
```java
public Long determineRecordOrgUnitId(TargetInspectionRecord record) {
    return switch (record.getTargetType()) {
        case STUDENT -> {
            Student student = studentRepository.findById(record.getTargetId());
            SchoolClass clazz = classRepository.findById(student.getClassId());
            yield clazz.getOrgUnitId();  // 学生所在班级的部门
        }
        case CLASS -> {
            SchoolClass clazz = classRepository.findById(record.getTargetId());
            yield clazz.getOrgUnitId();  // 班级的部门
        }
        case DORMITORY, CLASSROOM -> {
            Space space = spaceRepository.findById(record.getTargetId());
            if (space.getClassId() != null) {
                // 场所分配给了班级，取班级的部门
                SchoolClass clazz = classRepository.findById(space.getClassId());
                yield clazz.getOrgUnitId();
            }
            yield space.getOrgUnitId();  // 场所自身的部门
        }
    };
}
```

**好处**：
- 系部主任能看到本系学生的所有检查记录，无论检查项目由谁创建
- 数据权限过滤更符合业务逻辑

### 决策21：学生查看个人扣分记录 ✅ 已确认

**问题**：学生使用"仅本人"范围，但检查记录是检查员创建的

**决策**：学生模块使用专门的过滤字段 `student_id` 或 `target_id`

**数据模块配置**：
```sql
-- 检查记录模块 (普通用户)
INSERT INTO data_modules (module_code, filter_fields, ...) VALUES
('inspection_record', '{
    "org_unit": "org_unit_id",
    "class": "class_id",
    "creator": "created_by"
}', ...);

-- 学生个人检查记录模块 (学生专用)
INSERT INTO data_modules (module_code, filter_fields, ...) VALUES
('inspection_personal', '{
    "student": "target_id"
}', ...);
```

**学生角色数据权限**：
```yaml
STUDENT:
  数据权限:
    inspection_personal: SELF  # 使用 target_id = 当前用户关联的学生ID
```

**实现**：
```java
// 学生查看个人记录时
@GetMapping("/my-records")
@PreAuthorize("hasAuthority('inspection:personal:view')")
@DataPermission(module = "inspection_personal")
public Result<List<RecordVO>> getMyRecords() {
    // 数据权限拦截器会添加: WHERE target_id = 当前学生ID
    return recordService.findMyRecords();
}

// 特殊处理: SELF 范围在 inspection_personal 模块的处理
private String buildFilterSql(DataScopeResult scope, String moduleCode) {
    if ("inspection_personal".equals(moduleCode) && scope.isSelf()) {
        // 学生ID = 当前用户关联的学生
        Long studentId = studentRepository.findByUserId(scope.getUserId()).getId();
        return String.format("target_id = %d AND target_type = 'STUDENT'", studentId);
    }
    // 其他模块正常处理...
}
```

### 决策22：数据权限缓存失效策略 ✅ 已确认

**问题**：当角色的数据权限配置发生变更时，如何处理已登录用户的权限缓存？

**决策**：采用**实时失效**策略

**实现方案**：
```java
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CHANNEL = "permission:cache:invalidate";

    /**
     * 角色权限变更时，发布缓存失效消息
     */
    public void invalidateRolePermissionCache(Long roleId) {
        // 1. 获取该角色下所有用户ID
        List<Long> userIds = userRoleRepository.findUserIdsByRoleId(roleId);

        // 2. 发布失效消息（所有实例都会收到）
        InvalidateMessage msg = new InvalidateMessage("ROLE", roleId, userIds);
        stringRedisTemplate.convertAndSend(CHANNEL, JSON.toJSONString(msg));
    }

    /**
     * 监听缓存失效消息
     */
    @RedisListener(channel = CHANNEL)
    public void onInvalidateMessage(String message) {
        InvalidateMessage msg = JSON.parseObject(message, InvalidateMessage.class);

        // 清除本地缓存和Redis缓存
        for (Long userId : msg.getUserIds()) {
            String cacheKey = "user:permission:" + userId;
            redisTemplate.delete(cacheKey);
            localCache.invalidate(userId);
        }
    }
}
```

**好处**：
- 权限变更立即生效，安全性高
- 分布式环境下所有实例同步失效
- 不会出现权限延迟问题

---

### 决策23：无角色用户的默认行为 ✅ 已确认

**问题**：如果用户没有分配任何角色，其数据权限应该如何处理？

**决策**：**可登录但无业务数据访问权限**

**实现**：
```java
public DataScopeResult getDataScope(Long userId, String moduleCode) {
    // 1. 获取用户有效角色
    List<Long> roleIds = getEffectiveRoleIds(userId);

    if (roleIds.isEmpty()) {
        // 无角色 → 无数据访问权限
        log.warn("用户[{}]没有分配任何角色，拒绝数据访问", userId);
        return DataScopeResult.none();
    }

    // 2. 获取数据权限配置...
}
```

**前端处理**：
```typescript
// 无角色用户登录后，显示友好提示
if (user.roles.length === 0) {
  ElMessage.warning('您尚未分配角色，请联系管理员')
  // 可访问个人信息页面，但业务菜单不显示
}
```

**行为说明**：
- 可以登录系统
- 可以查看个人信息、修改密码等基础功能
- 业务模块菜单不显示
- 访问业务API返回空数据

---

### 决策24：检查记录历史数据迁移 ✅ 已确认

**问题**：现有检查记录可能没有 `snapshot_class_id` 和 `snapshot_department_id` 字段

**决策**：**保持空值，查询时兼容处理**

**数据库修改**：
```sql
-- 新增快照字段（允许为空）
ALTER TABLE target_inspection_records
ADD COLUMN snapshot_class_id BIGINT COMMENT '检查时刻的班级ID快照',
ADD COLUMN snapshot_class_name VARCHAR(100) COMMENT '检查时刻的班级名称快照',
ADD COLUMN snapshot_department_id BIGINT COMMENT '检查时刻的部门ID快照',
ADD COLUMN snapshot_department_name VARCHAR(100) COMMENT '检查时刻的部门名称快照';
```

**查询兼容处理**：
```java
// 数据权限SQL生成时做兼容
private String buildOrgUnitFilter(List<Long> orgUnitIds) {
    String idList = joinIds(orgUnitIds);
    // 优先使用快照字段，为空时回退到关联查询
    return String.format(
        "(snapshot_department_id IN (%s) OR " +
        "(snapshot_department_id IS NULL AND org_unit_id IN (%s)))",
        idList, idList
    );
}
```

**新数据处理**：
```java
// 新建检查记录时，自动填充快照
public void createRecord(CreateRecordCommand cmd) {
    TargetInspectionRecord record = new TargetInspectionRecord();
    // ... 其他字段

    // 填充快照信息
    Target target = resolveTarget(cmd.getTargetType(), cmd.getTargetId());
    record.setSnapshotClassId(target.getCurrentClassId());
    record.setSnapshotClassName(target.getCurrentClassName());
    record.setSnapshotDepartmentId(target.getCurrentDepartmentId());
    record.setSnapshotDepartmentName(target.getCurrentDepartmentName());
}
```

---

### 决策25：API兼容性处理 ✅ 已确认

**问题**：V2数据权限API是否需要保持与V1的兼容？

**决策**：**直接替换**，前端同步更新

**API变更清单**：

| V1 API | V2 API | 变更说明 |
|--------|--------|----------|
| `GET /roles/{id}/data-permissions` | 保持 | 响应结构调整 |
| `PUT /roles/{id}/data-permissions` | 保持 | 请求结构调整 |
| - | `GET /roles/data-permissions/modules` | 新增 |
| - | `GET /roles/data-permissions/scopes` | 新增 |
| - | `GET /roles/data-permissions/scope-items` | 新增 |
| - | `GET /roles/data-permissions/scope-item-types` | 新增 |

**迁移步骤**：
1. 后端实现V2 API
2. 前端同步更新API调用和类型定义
3. 删除V1代码（无并行期）

**理由**：
- 前端代码我们可控，可以同步修改
- 减少维护两套API的成本
- 避免新旧API混用导致的问题

---

### 决策26：批量操作的权限校验 ✅ 已确认

**问题**：批量操作（如批量删除、批量导出）时如何处理数据权限校验？

**决策**：**部分成功模式**，返回跳过的记录列表

**实现**：
```java
@Data
public class BatchOperationResult<T> {
    private int successCount;
    private int failedCount;
    private List<T> successItems;
    private List<FailedItem<T>> failedItems;

    @Data
    public static class FailedItem<T> {
        private T item;
        private String reason;  // "NO_PERMISSION" | "NOT_FOUND" | "OTHER"
    }
}

@Service
public class StudentService {

    @Transactional
    public BatchOperationResult<Long> batchDelete(List<Long> ids) {
        BatchOperationResult<Long> result = new BatchOperationResult<>();

        // 1. 查询用户有权限的学生ID列表
        Set<Long> permittedIds = getPermittedStudentIds(ids);

        // 2. 执行删除
        for (Long id : ids) {
            if (permittedIds.contains(id)) {
                studentRepository.deleteById(id);
                result.addSuccess(id);
            } else {
                result.addFailed(id, "NO_PERMISSION");
            }
        }

        return result;
    }
}
```

**前端处理**：
```typescript
async function handleBatchDelete(ids: number[]) {
  const result = await studentApi.batchDelete(ids)

  if (result.failedCount > 0) {
    const noPermissionCount = result.failedItems
      .filter(f => f.reason === 'NO_PERMISSION').length

    ElMessage.warning(
      `成功删除 ${result.successCount} 条，` +
      `${noPermissionCount} 条因权限不足被跳过`
    )
  } else {
    ElMessage.success(`成功删除 ${result.successCount} 条`)
  }

  refreshList()
}
```

---

### 决策27：功能权限与数据权限的关系 ✅ 已确认

**问题**：功能权限和数据权限是什么关系？

**决策**：**AND关系**，必须同时满足

**执行顺序**：
```
用户请求
    ↓
功能权限检查（@PreAuthorize）
    ↓ 通过
数据权限过滤（DataPermissionInterceptor）
    ↓
返回结果
```

**示例**：
```java
@GetMapping("/students")
@PreAuthorize("hasAuthority('student:view')")  // 1. 功能权限：能不能查
@DataPermission(module = "student")            // 2. 数据权限：能查哪些
public Result<Page<StudentVO>> list(Query query) {
    return studentService.findPage(query);
}
```

**场景说明**：

| 功能权限 | 数据权限 | 结果 |
|---------|---------|------|
| ✅ `student:view` | ALL | 可查看所有学生 |
| ✅ `student:view` | DEPARTMENT | 只能看本部门学生 |
| ✅ `student:view` | SELF | 只能看自己（如果是学生） |
| ❌ 无 | 任意 | 403 禁止访问 |

---

### 决策28：前端路由守卫与数据权限 ✅ 已确认

**问题**：前端路由守卫是否需要考虑数据权限？

**决策**：**路由只检查功能权限**，数据权限在页面内处理

**实现**：
```typescript
// router/guards.ts - 路由守卫只检查功能权限
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 检查功能权限
  if (to.meta.permission) {
    if (!authStore.hasPermission(to.meta.permission)) {
      return next('/403')
    }
  }

  next()
})

// views/StudentList.vue - 页面内处理数据权限
const { data, loading } = useQuery({
  queryFn: () => studentApi.list(query)
})

// 空数据提示
<template>
  <div v-if="!loading && data.length === 0" class="empty-state">
    <el-empty description="暂无可访问的数据">
      <template #description>
        <p>您当前的数据权限范围内没有学生数据</p>
        <p class="text-gray-400 text-sm">如需查看更多数据，请联系管理员调整权限</p>
      </template>
    </el-empty>
  </div>
</template>
```

**好处**：
- 路由守卫逻辑简单，只关注功能权限
- 数据权限的空状态可以给用户更友好的提示
- 避免路由守卫需要异步查询数据权限配置

---

**文档版本**: 2.2
**最后更新**: 2026-01-31
**作者**: Claude (基于系统分析和业界最佳实践)
