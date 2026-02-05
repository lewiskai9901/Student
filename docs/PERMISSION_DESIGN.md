# 权限管理系统设计方案

> **版本**: 1.0
> **日期**: 2026-01-31
> **目标**: 设计一套清晰、灵活、无硬编码的权限管理方案

---

## 一、问题分析

### 1.1 当前痛点

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           权限管理复杂度来源                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  维度1: 功能权限 (能不能用)                                              │
│  ├── 有的人能用学生管理，有的人不能                                       │
│  ├── 有的人能创建学生，有的人只能查看                                     │
│  └── 有的人能导出数据，有的人不能                                         │
│                                                                         │
│  维度2: 数据权限 (能看多少)                                              │
│  ├── 有的人能看所有学生，有的人只能看本部门                               │
│  ├── 有的人能管理所有宿舍，有的人只能管理指定楼栋                          │
│  └── 有的班主任只能看自己班级的数据                                       │
│                                                                         │
│  维度3: 模块差异                                                         │
│  ├── 同一用户在不同模块的数据范围可能不同                                  │
│  │   (如：学生模块看全部，宿舍模块只看本部门)                              │
│  └── 不同模块的数据归属字段不同                                           │
│       (学生按class_id/org_unit_id，宿舍按building_id)                    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 设计目标

1. **功能权限与数据权限分离**: 两个维度独立配置
2. **模块级数据权限**: 每个模块可独立配置数据范围
3. **无硬编码**: 权限编码、数据范围均可配置
4. **灵活组合**: 通过角色组合实现各种权限需求
5. **易于理解**: 管理员能直观理解和配置

---

## 二、权限模型设计

### 2.1 核心概念

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           权限模型核心概念                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────┐                                                        │
│  │    用户     │                                                        │
│  │   (User)    │                                                        │
│  └──────┬──────┘                                                        │
│         │ 1:N                                                           │
│         ▼                                                               │
│  ┌─────────────┐         ┌─────────────────────────────────────────┐   │
│  │    角色     │         │               权限配置                   │   │
│  │   (Role)    │────────→│  ┌─────────────┐  ┌─────────────────┐  │   │
│  └─────────────┘         │  │  功能权限    │  │    数据权限      │  │   │
│                          │  │ (Permission) │  │ (DataPermission) │  │   │
│                          │  │              │  │                  │  │   │
│                          │  │ 控制:        │  │ 控制:            │  │   │
│                          │  │ "能不能用"   │  │ "能看/改多少"    │  │   │
│                          │  └─────────────┘  └─────────────────┘  │   │
│                          └─────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 功能权限编码规范

```
格式: {module}:{resource}:{action}

module   - 模块编码 (student, dormitory, inspection, user...)
resource - 资源编码 (info, record, template, config...)
action   - 操作编码 (view, create, update, delete, export, import, manage...)
```

**权限树示例**:

```
system (系统管理)
├── system:user (用户管理)
│   ├── system:user:view      查看用户
│   ├── system:user:create    创建用户
│   ├── system:user:update    修改用户
│   ├── system:user:delete    删除用户
│   └── system:user:reset-pwd 重置密码
├── system:role (角色管理)
│   ├── system:role:view      查看角色
│   ├── system:role:create    创建角色
│   ├── system:role:update    修改角色
│   ├── system:role:delete    删除角色
│   └── system:role:assign    分配权限
└── system:config (系统配置)
    ├── system:config:view    查看配置
    └── system:config:update  修改配置

student (学生管理)
├── student:info (学生信息)
│   ├── student:info:view     查看学生
│   ├── student:info:create   创建学生
│   ├── student:info:update   修改学生
│   ├── student:info:delete   删除学生
│   ├── student:info:import   导入学生
│   └── student:info:export   导出学生
├── student:status (学籍状态)
│   ├── student:status:view   查看学籍
│   └── student:status:change 变更学籍
└── student:dormitory (学生住宿)
    ├── student:dormitory:view   查看住宿
    └── student:dormitory:assign 分配宿舍

dormitory (宿舍管理)
├── dormitory:building (楼栋管理)
│   ├── dormitory:building:view   查看楼栋
│   ├── dormitory:building:create 创建楼栋
│   └── dormitory:building:update 修改楼栋
├── dormitory:room (房间管理)
│   ├── dormitory:room:view       查看房间
│   ├── dormitory:room:create     创建房间
│   ├── dormitory:room:update     修改房间
│   └── dormitory:room:assign     分配房间
└── dormitory:checkin (入住管理)
    ├── dormitory:checkin:view    查看入住
    ├── dormitory:checkin:operate 办理入住/退宿
    └── dormitory:checkin:adjust  调整床位

inspection (量化检查)
├── inspection:template (检查模板)
│   ├── inspection:template:view   查看模板
│   ├── inspection:template:create 创建模板
│   ├── inspection:template:update 修改模板
│   └── inspection:template:delete 删除模板
├── inspection:project (检查项目)
│   ├── inspection:project:view    查看项目
│   ├── inspection:project:create  创建项目
│   ├── inspection:project:manage  管理项目
│   └── inspection:project:delete  删除项目
├── inspection:task (检查任务)
│   ├── inspection:task:view       查看任务
│   ├── inspection:task:execute    执行检查
│   ├── inspection:task:review     审核任务
│   └── inspection:task:publish    发布任务
├── inspection:record (检查记录)
│   ├── inspection:record:view     查看记录
│   ├── inspection:record:create   录入记录
│   ├── inspection:record:update   修改记录
│   └── inspection:record:delete   删除记录
├── inspection:appeal (申诉管理)
│   ├── inspection:appeal:view     查看申诉
│   ├── inspection:appeal:create   提交申诉
│   └── inspection:appeal:review   审核申诉
├── inspection:summary (汇总排名)
│   ├── inspection:summary:view    查看汇总
│   ├── inspection:summary:generate 生成汇总
│   └── inspection:summary:publish 发布排名
└── inspection:corrective (整改管理)
    ├── inspection:corrective:view    查看整改
    ├── inspection:corrective:create  下发整改
    ├── inspection:corrective:execute 执行整改
    └── inspection:corrective:verify  复查整改

academic (教务管理)
├── academic:class (班级管理)
│   ├── academic:class:view    查看班级
│   ├── academic:class:create  创建班级
│   ├── academic:class:update  修改班级
│   └── academic:class:delete  删除班级
├── academic:grade (年级管理)
│   ├── academic:grade:view    查看年级
│   ├── academic:grade:create  创建年级
│   └── academic:grade:update  修改年级
└── academic:major (专业管理)
    ├── academic:major:view    查看专业
    ├── academic:major:create  创建专业
    └── academic:major:update  修改专业
```

### 2.3 数据权限范围

```java
/**
 * 数据权限范围类型
 */
public enum DataScope {

    ALL(1, "全部数据",
        "可访问系统中所有数据，无任何限制"),

    DEPARTMENT_AND_BELOW(2, "本部门及下级",
        "可访问用户所属部门及其所有下级部门的数据"),

    DEPARTMENT(3, "仅本部门",
        "只能访问用户所属部门的数据"),

    SELF_DEPARTMENT_CUSTOM(4, "本部门+自定义",
        "可访问本部门数据，以及额外指定的部门/班级数据"),

    CUSTOM(5, "自定义范围",
        "只能访问明确指定的部门/班级/楼栋等范围的数据"),

    SELF(6, "仅本人",
        "只能访问自己创建或负责的数据");

    // 范围大小排序: ALL > DEPARTMENT_AND_BELOW > DEPARTMENT > CUSTOM > SELF
}
```

### 2.4 数据模块定义

```java
/**
 * 数据模块 - 用于数据权限配置
 */
public enum DataModule {

    // ===== 学生管理领域 =====
    STUDENT("student", "学生信息", "student",
            "class_id", "org_unit_id"),

    // ===== 宿舍管理领域 =====
    DORMITORY_BUILDING("dormitory_building", "楼栋管理", "dormitory",
            null, "org_unit_id"),
    DORMITORY_ROOM("dormitory_room", "房间管理", "dormitory",
            "building_id", "org_unit_id"),
    DORMITORY_CHECKIN("dormitory_checkin", "入住管理", "dormitory",
            "class_id", "org_unit_id"),

    // ===== 量化检查领域 =====
    INSPECTION_TEMPLATE("inspection_template", "检查模板", "inspection",
            null, "org_unit_id"),
    INSPECTION_PROJECT("inspection_project", "检查项目", "inspection",
            null, "org_unit_id"),
    INSPECTION_TASK("inspection_task", "检查任务", "inspection",
            null, "org_unit_id"),
    INSPECTION_RECORD("inspection_record", "检查记录", "inspection",
            "class_id", "org_unit_id"),
    INSPECTION_APPEAL("inspection_appeal", "申诉管理", "inspection",
            "class_id", "org_unit_id"),
    INSPECTION_SUMMARY("inspection_summary", "汇总排名", "inspection",
            "class_id", "org_unit_id"),

    // ===== 教务管理领域 =====
    ACADEMIC_CLASS("academic_class", "班级管理", "academic",
            null, "org_unit_id"),
    ACADEMIC_GRADE("academic_grade", "年级管理", "academic",
            null, "org_unit_id"),

    // ===== 系统管理领域 =====
    SYSTEM_USER("system_user", "用户管理", "system",
            null, "org_unit_id"),
    SYSTEM_ROLE("system_role", "角色管理", "system",
            null, null);  // 无数据权限控制

    private final String code;          // 模块编码
    private final String name;          // 模块名称
    private final String domain;        // 所属领域
    private final String classField;    // 班级关联字段
    private final String orgUnitField;  // 部门关联字段
}
```

---

## 三、数据库设计

### 3.1 表结构

```sql
-- =====================================================
-- 1. 功能权限表 (树形结构)
-- =====================================================
CREATE TABLE permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 权限编码 (唯一)
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,

    -- 权限分解
    module VARCHAR(50) NOT NULL,           -- 模块: student, dormitory, inspection
    resource VARCHAR(50),                  -- 资源: info, room, template
    action VARCHAR(50),                    -- 操作: view, create, update, delete

    -- 树形结构
    parent_id BIGINT,
    tree_path VARCHAR(500),                -- 物化路径: /1/2/3/
    tree_level INT DEFAULT 0,              -- 层级深度

    -- 权限类型
    permission_type ENUM('MODULE', 'MENU', 'BUTTON', 'API') NOT NULL,

    -- 前端路由 (MENU类型)
    route_path VARCHAR(255),
    component_path VARCHAR(255),
    icon VARCHAR(100),

    -- 排序和状态
    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    description VARCHAR(500),

    -- 审计字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_permission_code (permission_code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能权限表';


-- =====================================================
-- 2. 角色表
-- =====================================================
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    role_code VARCHAR(50) NOT NULL,        -- 角色编码
    role_name VARCHAR(100) NOT NULL,       -- 角色名称
    role_type ENUM('SYSTEM', 'CUSTOM') DEFAULT 'CUSTOM',  -- 系统角色不可删除

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
-- 3. 角色-功能权限关联表
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
-- 4. 角色-数据权限配置表 (核心!)
-- =====================================================
CREATE TABLE role_data_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,

    -- 数据模块
    data_module VARCHAR(50) NOT NULL,      -- 模块编码: student, dormitory_room, inspection_record

    -- 数据范围
    data_scope ENUM('ALL', 'DEPARTMENT_AND_BELOW', 'DEPARTMENT',
                   'SELF_DEPARTMENT_CUSTOM', 'CUSTOM', 'SELF') NOT NULL,

    -- 说明
    description VARCHAR(200),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_role_module (role_id, data_module),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-数据权限配置表';


-- =====================================================
-- 5. 自定义数据范围表 (CUSTOM/SELF_DEPARTMENT_CUSTOM时使用)
-- =====================================================
CREATE TABLE role_data_scope_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_data_permission_id BIGINT NOT NULL,

    -- 范围类型和ID
    scope_type ENUM('ORG_UNIT', 'CLASS', 'GRADE', 'BUILDING', 'FLOOR') NOT NULL,
    scope_id BIGINT NOT NULL,
    scope_name VARCHAR(100),               -- 冗余，方便显示

    -- 是否包含下级 (仅ORG_UNIT类型有效)
    include_children TINYINT DEFAULT 0,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_permission_scope (role_data_permission_id, scope_type, scope_id),
    INDEX idx_role_data_permission_id (role_data_permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义数据范围明细表';


-- =====================================================
-- 6. 用户-角色关联表
-- =====================================================
CREATE TABLE user_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    -- 可选：角色在特定范围内生效
    scope_org_unit_id BIGINT,              -- 角色仅在此部门范围内生效

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,

    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';
```

### 3.2 关系图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              权限数据模型关系图                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│    ┌─────────┐                                                              │
│    │  users  │                                                              │
│    └────┬────┘                                                              │
│         │ 1:N                                                               │
│         ▼                                                                   │
│    ┌────────────┐        ┌─────────┐                                        │
│    │ user_roles │───────→│  roles  │                                        │
│    └────────────┘   N:1  └────┬────┘                                        │
│                               │                                             │
│              ┌────────────────┼────────────────┐                            │
│              │                │                │                            │
│              ▼ 1:N            ▼ 1:N            │                            │
│    ┌─────────────────┐  ┌──────────────────────┴─┐                          │
│    │ role_permissions │  │ role_data_permissions  │                          │
│    └────────┬────────┘  └───────────┬────────────┘                          │
│             │                       │                                       │
│             ▼ N:1                   ▼ 1:N                                   │
│    ┌─────────────────┐  ┌────────────────────────┐                          │
│    │   permissions   │  │  role_data_scope_items │                          │
│    │   (功能权限)     │  │     (自定义范围)        │                          │
│    └─────────────────┘  └────────────────────────┘                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 四、权限判断逻辑

### 4.1 权限检查流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              权限检查流程                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   用户请求: GET /api/students?classId=1                                     │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ Step 1: 功能权限检查                                                 │   │
│   │                                                                     │   │
│   │ 检查用户是否有 student:info:view 权限                                │   │
│   │                                                                     │   │
│   │ ┌─────────────────────────────────────────────────────────────────┐│   │
│   │ │ 1. 获取用户所有角色                                              ││   │
│   │ │ 2. 获取所有角色的功能权限 (取并集)                                ││   │
│   │ │ 3. 判断是否包含 student:info:view                                ││   │
│   │ └─────────────────────────────────────────────────────────────────┘│   │
│   │                                                                     │   │
│   │ 无权限 → 返回 403 Forbidden                                         │   │
│   │ 有权限 → 继续 Step 2                                                │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                     │                                       │
│                                     ▼                                       │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ Step 2: 数据权限检查                                                 │   │
│   │                                                                     │   │
│   │ 获取用户在 student 模块的数据范围                                    │   │
│   │                                                                     │   │
│   │ ┌─────────────────────────────────────────────────────────────────┐│   │
│   │ │ 1. 获取用户所有角色在 student 模块的数据权限配置                  ││   │
│   │ │ 2. 多个角色取最大范围 (ALL > DEPT_AND_BELOW > DEPT > CUSTOM > SELF)││   │
│   │ │ 3. 如果是 CUSTOM，合并所有自定义范围                              ││   │
│   │ └─────────────────────────────────────────────────────────────────┘│   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                     │                                       │
│                                     ▼                                       │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ Step 3: 应用数据过滤                                                 │   │
│   │                                                                     │   │
│   │ 根据数据范围修改 SQL 查询条件                                        │   │
│   │                                                                     │   │
│   │ ALL:                                                                │   │
│   │   SELECT * FROM students WHERE ...                                  │   │
│   │                                                                     │   │
│   │ DEPARTMENT_AND_BELOW:                                               │   │
│   │   SELECT * FROM students WHERE org_unit_id IN (部门及下级ID列表)     │   │
│   │                                                                     │   │
│   │ DEPARTMENT:                                                         │   │
│   │   SELECT * FROM students WHERE org_unit_id = 用户部门ID             │   │
│   │                                                                     │   │
│   │ CUSTOM:                                                             │   │
│   │   SELECT * FROM students WHERE                                      │   │
│   │     (org_unit_id IN (自定义部门列表) OR class_id IN (自定义班级列表)) │   │
│   │                                                                     │   │
│   │ SELF:                                                               │   │
│   │   SELECT * FROM students WHERE created_by = 当前用户ID              │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 核心实现

```java
/**
 * 权限服务
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleDataPermissionRepository dataPermissionRepository;
    private final OrgUnitRepository orgUnitRepository;

    /**
     * 检查功能权限
     */
    public boolean hasPermission(Long userId, String permissionCode) {
        Set<String> permissions = getUserPermissions(userId);
        return permissions.contains(permissionCode);
    }

    /**
     * 获取用户所有功能权限 (缓存)
     */
    @Cacheable(value = "user_permissions", key = "#userId")
    public Set<String> getUserPermissions(Long userId) {
        // 1. 获取用户所有角色
        List<Long> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        // 2. 获取所有角色的权限编码 (并集)
        return rolePermissionRepository.findPermissionCodesByRoleIds(roleIds);
    }

    /**
     * 获取用户在指定模块的数据范围
     */
    public DataScopeResult getDataScope(Long userId, String dataModule) {
        // 1. 获取用户所有角色
        List<Long> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return DataScopeResult.none();
        }

        // 2. 获取所有角色在该模块的数据权限配置
        List<RoleDataPermission> permissions =
            dataPermissionRepository.findByRoleIdsAndModule(roleIds, dataModule);

        if (permissions.isEmpty()) {
            // 无配置，默认 SELF
            return DataScopeResult.self(userId);
        }

        // 3. 合并多个角色的数据权限 (取最大范围)
        return mergeDataScopes(userId, permissions);
    }

    /**
     * 合并多个数据权限配置
     */
    private DataScopeResult mergeDataScopes(Long userId, List<RoleDataPermission> permissions) {
        // 范围优先级: ALL > DEPARTMENT_AND_BELOW > DEPARTMENT > CUSTOM > SELF

        // 检查是否有 ALL
        if (permissions.stream().anyMatch(p -> p.getDataScope() == DataScope.ALL)) {
            return DataScopeResult.all();
        }

        // 检查是否有 DEPARTMENT_AND_BELOW
        if (permissions.stream().anyMatch(p -> p.getDataScope() == DataScope.DEPARTMENT_AND_BELOW)) {
            Long userOrgUnitId = getUserOrgUnitId(userId);
            List<Long> orgUnitIds = orgUnitRepository.findIdsByParentPath(userOrgUnitId);
            return DataScopeResult.orgUnits(orgUnitIds);
        }

        // 检查是否有 DEPARTMENT
        if (permissions.stream().anyMatch(p -> p.getDataScope() == DataScope.DEPARTMENT)) {
            Long userOrgUnitId = getUserOrgUnitId(userId);
            return DataScopeResult.orgUnits(List.of(userOrgUnitId));
        }

        // 合并所有 CUSTOM 范围
        Set<Long> orgUnitIds = new HashSet<>();
        Set<Long> classIds = new HashSet<>();
        Set<Long> buildingIds = new HashSet<>();

        for (RoleDataPermission permission : permissions) {
            if (permission.getDataScope() == DataScope.CUSTOM
                || permission.getDataScope() == DataScope.SELF_DEPARTMENT_CUSTOM) {

                // 如果是 SELF_DEPARTMENT_CUSTOM，先加入本部门
                if (permission.getDataScope() == DataScope.SELF_DEPARTMENT_CUSTOM) {
                    orgUnitIds.add(getUserOrgUnitId(userId));
                }

                // 加入自定义范围
                List<RoleDataScopeItem> items =
                    permission.getScopeItems();
                for (RoleDataScopeItem item : items) {
                    switch (item.getScopeType()) {
                        case ORG_UNIT -> {
                            orgUnitIds.add(item.getScopeId());
                            if (item.isIncludeChildren()) {
                                orgUnitIds.addAll(
                                    orgUnitRepository.findChildIds(item.getScopeId())
                                );
                            }
                        }
                        case CLASS -> classIds.add(item.getScopeId());
                        case BUILDING -> buildingIds.add(item.getScopeId());
                    }
                }
            }
        }

        if (!orgUnitIds.isEmpty() || !classIds.isEmpty() || !buildingIds.isEmpty()) {
            return DataScopeResult.custom(orgUnitIds, classIds, buildingIds);
        }

        // 默认 SELF
        return DataScopeResult.self(userId);
    }
}
```

### 4.3 数据权限注解

```java
/**
 * 数据权限注解 - 标记在 Controller 或 Service 方法上
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermission {

    /**
     * 数据模块
     */
    String module();

    /**
     * 是否启用 (可临时禁用)
     */
    boolean enabled() default true;
}

/**
 * 使用示例
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    @GetMapping
    @PreAuthorize("hasAuthority('student:info:view')")
    @DataPermission(module = "student")
    public Result<Page<StudentVO>> list(StudentQuery query) {
        // 数据权限会自动应用到查询
        return studentService.findPage(query);
    }
}
```

### 4.4 MyBatis 数据权限拦截器

```java
/**
 * 数据权限 SQL 拦截器
 */
@Intercepts({
    @Signature(type = Executor.class, method = "query",
               args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
@Component
public class DataPermissionInterceptor implements Interceptor {

    @Autowired
    private PermissionService permissionService;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 获取当前数据权限上下文
        DataPermissionContext context = DataPermissionHolder.getContext();
        if (context == null || !context.isEnabled()) {
            return invocation.proceed();
        }

        // 2. 获取当前用户的数据范围
        Long userId = SecurityUtils.getCurrentUserId();
        DataScopeResult scope = permissionService.getDataScope(userId, context.getModule());

        // 3. 如果是 ALL，不做限制
        if (scope.isAll()) {
            return invocation.proceed();
        }

        // 4. 修改 SQL，添加数据过滤条件
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        BoundSql boundSql = ms.getBoundSql(parameter);
        String originalSql = boundSql.getSql();

        String newSql = addDataScopeFilter(originalSql, scope, context);

        // 5. 重新构建 BoundSql
        BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), newSql,
            boundSql.getParameterMappings(), parameter);

        // ... 执行修改后的 SQL
        return invocation.proceed();
    }

    private String addDataScopeFilter(String sql, DataScopeResult scope, DataPermissionContext context) {
        // 根据模块配置获取过滤字段
        DataModule module = DataModule.valueOf(context.getModule().toUpperCase());

        StringBuilder filter = new StringBuilder();

        if (!scope.getOrgUnitIds().isEmpty() && module.getOrgUnitField() != null) {
            filter.append(String.format(" AND %s IN (%s)",
                module.getOrgUnitField(),
                scope.getOrgUnitIds().stream().map(String::valueOf).collect(Collectors.joining(","))
            ));
        }

        if (!scope.getClassIds().isEmpty() && module.getClassField() != null) {
            filter.append(String.format(" AND %s IN (%s)",
                module.getClassField(),
                scope.getClassIds().stream().map(String::valueOf).collect(Collectors.joining(","))
            ));
        }

        if (scope.isSelf()) {
            filter.append(String.format(" AND created_by = %d", scope.getUserId()));
        }

        // 将过滤条件添加到 WHERE 子句
        return injectWhereClause(sql, filter.toString());
    }
}
```

---

## 五、典型角色配置示例

### 5.1 超级管理员

```yaml
角色编码: SUPER_ADMIN
角色名称: 超级管理员
角色类型: SYSTEM

功能权限:
  - 所有权限 (*)

数据权限:
  - 所有模块: ALL
```

### 5.2 系部主任

```yaml
角色编码: DEPT_DIRECTOR
角色名称: 系部主任
角色类型: CUSTOM

功能权限:
  - student:*                    # 学生管理全部权限
  - dormitory:*:view             # 宿舍管理查看权限
  - dormitory:checkin:*          # 入住管理全部权限
  - inspection:*                 # 量化检查全部权限
  - academic:class:*             # 班级管理全部权限

数据权限:
  - student: DEPARTMENT_AND_BELOW
  - dormitory_room: DEPARTMENT_AND_BELOW
  - dormitory_checkin: DEPARTMENT_AND_BELOW
  - inspection_record: DEPARTMENT_AND_BELOW
  - inspection_summary: DEPARTMENT_AND_BELOW
  - academic_class: DEPARTMENT_AND_BELOW
```

### 5.3 班主任

```yaml
角色编码: CLASS_TEACHER
角色名称: 班主任
角色类型: CUSTOM

功能权限:
  - student:info:view            # 查看学生
  - student:info:update          # 修改学生
  - student:status:view          # 查看学籍
  - student:dormitory:view       # 查看住宿
  - dormitory:room:view          # 查看房间
  - dormitory:checkin:view       # 查看入住
  - inspection:record:view       # 查看检查记录
  - inspection:appeal:view       # 查看申诉
  - inspection:appeal:create     # 提交申诉 (代学生)
  - inspection:summary:view      # 查看汇总
  - inspection:corrective:view   # 查看整改
  - inspection:corrective:execute # 执行整改

数据权限:
  - student: CUSTOM
    scope_items:
      - type: CLASS, id: {管理的班级ID}
  - dormitory_checkin: CUSTOM
    scope_items:
      - type: CLASS, id: {管理的班级ID}
  - inspection_record: CUSTOM
    scope_items:
      - type: CLASS, id: {管理的班级ID}
  - inspection_summary: CUSTOM
    scope_items:
      - type: CLASS, id: {管理的班级ID}
```

### 5.4 宿管员

```yaml
角色编码: DORM_MANAGER
角色名称: 宿管员
角色类型: CUSTOM

功能权限:
  - dormitory:building:view      # 查看楼栋
  - dormitory:room:view          # 查看房间
  - dormitory:room:update        # 修改房间
  - dormitory:checkin:*          # 入住管理全部
  - inspection:task:view         # 查看检查任务
  - inspection:task:execute      # 执行检查
  - inspection:record:view       # 查看记录
  - inspection:record:create     # 录入记录

数据权限:
  - dormitory_building: CUSTOM
    scope_items:
      - type: BUILDING, id: {负责的楼栋ID}
  - dormitory_room: CUSTOM
    scope_items:
      - type: BUILDING, id: {负责的楼栋ID}
  - dormitory_checkin: CUSTOM
    scope_items:
      - type: BUILDING, id: {负责的楼栋ID}
  - inspection_record: CUSTOM
    scope_items:
      - type: BUILDING, id: {负责的楼栋ID}
```

### 5.5 检查员

```yaml
角色编码: INSPECTOR
角色名称: 检查员
角色类型: CUSTOM

功能权限:
  - inspection:task:view         # 查看任务
  - inspection:task:execute      # 执行检查
  - inspection:record:view       # 查看记录
  - inspection:record:create     # 录入记录
  - inspection:record:update     # 修改记录 (未发布时)

数据权限:
  - inspection_task: SELF        # 只能看分配给自己的任务
  - inspection_record: SELF      # 只能看自己录入的记录
```

### 5.6 学生

```yaml
角色编码: STUDENT
角色名称: 学生
角色类型: SYSTEM

功能权限:
  - inspection:record:view       # 查看检查记录 (个人)
  - inspection:appeal:view       # 查看申诉
  - inspection:appeal:create     # 提交申诉
  - profile:view                 # 查看个人信息
  - profile:password:change      # 修改密码

数据权限:
  - inspection_record: SELF      # 只能看自己的记录
  - inspection_appeal: SELF      # 只能看自己的申诉
```

---

## 六、权限管理界面设计

### 6.1 角色权限配置界面

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         角色权限配置 - 系部主任                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  基本信息                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 角色编码: DEPT_DIRECTOR                                              │   │
│  │ 角色名称: 系部主任                                                    │   │
│  │ 角色描述: 管理本系部及下属的学生、宿舍、检查等事务                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Tab: [功能权限]  [数据权限]                                           │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  功能权限 (树形勾选)                                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ □ 系统管理                                                           │   │
│  │   ├─ □ 用户管理                                                      │   │
│  │   ├─ □ 角色管理                                                      │   │
│  │   └─ □ 系统配置                                                      │   │
│  │                                                                       │   │
│  │ ☑ 学生管理                                                           │   │
│  │   ├─ ☑ 学生信息                                                      │   │
│  │   │    ├─ ☑ 查看学生                                                 │   │
│  │   │    ├─ ☑ 创建学生                                                 │   │
│  │   │    ├─ ☑ 修改学生                                                 │   │
│  │   │    ├─ ☑ 删除学生                                                 │   │
│  │   │    ├─ ☑ 导入学生                                                 │   │
│  │   │    └─ ☑ 导出学生                                                 │   │
│  │   └─ ☑ 学籍状态                                                      │   │
│  │                                                                       │   │
│  │ ☑ 宿舍管理                                                           │   │
│  │   ├─ ☑ 楼栋管理 (仅查看)                                              │   │
│  │   ├─ ☑ 房间管理 (仅查看)                                              │   │
│  │   └─ ☑ 入住管理                                                      │   │
│  │                                                                       │   │
│  │ ☑ 量化检查                                                           │   │
│  │   ├─ ☑ 检查模板                                                      │   │
│  │   ├─ ☑ 检查项目                                                      │   │
│  │   ├─ ☑ 检查任务                                                      │   │
│  │   └─ ...                                                             │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 数据权限配置界面

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         数据权限配置 - 系部主任                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  为每个数据模块配置数据访问范围                                              │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 学生信息                                                             │   │
│  │ ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │ │ 数据范围: ○全部  ●本部门及下级  ○仅本部门  ○自定义  ○仅本人       │ │   │
│  │ └─────────────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 宿舍房间                                                             │   │
│  │ ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │ │ 数据范围: ○全部  ●本部门及下级  ○仅本部门  ○自定义  ○仅本人       │ │   │
│  │ └─────────────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 检查记录                                                             │   │
│  │ ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │ │ 数据范围: ○全部  ●本部门及下级  ○仅本部门  ○自定义  ○仅本人       │ │   │
│  │ └─────────────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 检查任务 (班主任特殊配置示例)                                          │   │
│  │ ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │ │ 数据范围: ○全部  ○本部门及下级  ○仅本部门  ●自定义  ○仅本人       │ │   │
│  │ │                                                                   │ │   │
│  │ │ 自定义范围:                                                       │ │   │
│  │ │ ┌───────────────────────────────────────────────────────────┐   │ │   │
│  │ │ │ + 添加范围                                                  │   │ │   │
│  │ │ │                                                             │   │ │   │
│  │ │ │ ┌──────────┬─────────────────────┬────────────┬─────────┐ │   │ │   │
│  │ │ │ │ 范围类型  │ 范围名称             │ 包含下级    │ 操作    │ │   │ │   │
│  │ │ │ ├──────────┼─────────────────────┼────────────┼─────────┤ │   │ │   │
│  │ │ │ │ 班级     │ 2024级软件工程1班    │ -          │ [删除]  │ │   │ │   │
│  │ │ │ │ 班级     │ 2024级软件工程2班    │ -          │ [删除]  │ │   │ │   │
│  │ │ │ └──────────┴─────────────────────┴────────────┴─────────┘ │   │ │   │
│  │ │ └───────────────────────────────────────────────────────────┘   │ │   │
│  │ └─────────────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│                                              [取消]  [保存]                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 七、常见场景解决方案

### 7.1 场景：同一用户在不同模块有不同权限

**需求**: 张老师是计算机系的班主任，同时也是学校的宿管员

**解决方案**: 为用户分配多个角色

```
张老师的角色:
1. 班主任角色
   - 学生模块: CUSTOM (仅管理的班级)
   - 检查模块: CUSTOM (仅管理的班级)

2. 宿管员角色
   - 宿舍模块: CUSTOM (负责的楼栋)
   - 检查模块: CUSTOM (负责的楼栋)

合并结果:
- 学生模块: CUSTOM (仅管理的班级)
- 宿舍模块: CUSTOM (负责的楼栋)
- 检查模块: CUSTOM (管理的班级 + 负责的楼栋)
```

### 7.2 场景：临时授予额外权限

**需求**: 李老师临时需要查看其他班级的学生信息

**解决方案**: 创建临时角色或扩展自定义范围

```
方案1: 创建临时角色
- 为李老师分配一个临时角色，包含额外班级的数据权限
- 过期后移除角色

方案2: 扩展自定义范围
- 在李老师现有角色的数据权限中，添加额外班级到自定义范围
```

### 7.3 场景：部门调整时的权限变更

**需求**: 王老师从A系调到B系

**解决方案**: 数据权限自动跟随用户部门

```
使用 DEPARTMENT / DEPARTMENT_AND_BELOW 范围:
- 用户部门字段更新后，数据权限自动生效
- 无需手动修改角色配置

使用 CUSTOM 范围:
- 需要更新角色的自定义范围配置
- 或重新分配角色
```

### 7.4 场景：跨部门协作

**需求**: 教务处需要查看所有部门的学生数据，但不能修改

**解决方案**: 创建只读全局角色

```yaml
角色编码: ACADEMIC_VIEWER
角色名称: 教务查看员

功能权限:
  - student:info:view            # 只有查看权限
  - academic:class:view

数据权限:
  - student: ALL                 # 可看所有数据
  - academic_class: ALL
```

---

## 八、实施建议

### 8.1 迁移步骤

```
Phase 1: 数据库准备
├── 创建新的权限相关表
├── 初始化功能权限数据 (permission树)
└── 初始化系统角色 (SUPER_ADMIN, STUDENT等)

Phase 2: 后端改造
├── 实现 PermissionService
├── 实现数据权限拦截器
├── 改造现有 Controller 添加权限注解
└── 单元测试

Phase 3: 前端改造
├── 角色管理界面
├── 功能权限配置界面
├── 数据权限配置界面
└── 按钮级权限控制

Phase 4: 数据迁移
├── 迁移现有角色数据
├── 迁移现有权限配置
└── 验证权限正确性

Phase 5: 测试验收
├── 各角色场景测试
├── 边界情况测试
└── 性能测试
```

### 8.2 注意事项

1. **缓存策略**: 用户权限需要缓存，角色变更时清除相关用户缓存
2. **性能优化**: 数据权限SQL拼接需要注意索引使用
3. **日志审计**: 权限变更需要记录审计日志
4. **前端联动**: 前端根据功能权限控制菜单和按钮显示

---

## 九、总结

本方案通过 **功能权限 + 数据权限** 双维度设计，实现了：

1. **功能权限**: 通过树形权限结构，灵活控制用户能使用哪些功能
2. **数据权限**: 通过模块级配置，精确控制用户能看到哪些数据范围
3. **无硬编码**: 所有权限编码、数据模块、数据范围均可配置
4. **灵活组合**: 通过角色组合实现各种复杂权限需求
5. **易于理解**: 管理界面直观，配置简单

**核心表**:
- `permissions` - 功能权限（树形）
- `roles` - 角色
- `role_permissions` - 角色-功能权限
- `role_data_permissions` - 角色-数据权限（模块级）
- `role_data_scope_items` - 自定义数据范围明细

**核心类**:
- `PermissionService` - 权限检查服务
- `DataPermissionInterceptor` - 数据权限SQL拦截器
- `@DataPermission` - 数据权限注解
