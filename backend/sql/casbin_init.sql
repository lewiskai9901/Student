-- ============================================================
-- Casbin 数据权限系统初始化脚本
-- 创建日期: 2026-01-02
-- 版本: 1.0.0
-- ============================================================

-- 使用事务确保原子性
START TRANSACTION;

-- ============================================================
-- 1. Casbin 规则表 (由 Casbin JDBC Adapter 自动管理)
-- ============================================================
CREATE TABLE IF NOT EXISTS casbin_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ptype VARCHAR(100) NOT NULL COMMENT '策略类型: p=policy, g=grouping, g2=scope grouping',
    v0 VARCHAR(100) DEFAULT '' COMMENT '第1个参数',
    v1 VARCHAR(100) DEFAULT '' COMMENT '第2个参数',
    v2 VARCHAR(100) DEFAULT '' COMMENT '第3个参数',
    v3 VARCHAR(100) DEFAULT '' COMMENT '第4个参数',
    v4 VARCHAR(100) DEFAULT '' COMMENT '第5个参数',
    v5 VARCHAR(100) DEFAULT '' COMMENT '第6个参数',
    INDEX idx_ptype (ptype),
    INDEX idx_v0 (v0),
    INDEX idx_v1 (v1),
    INDEX idx_v0_v1 (v0, v1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Casbin规则表';

-- ============================================================
-- 2. 范围元数据表 (用于显示友好名称和管理)
-- ============================================================
CREATE TABLE IF NOT EXISTS scope_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scope_expression VARCHAR(200) NOT NULL COMMENT '范围表达式，如 scope:dept:1',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称，如 汽车系',
    scope_type VARCHAR(50) NOT NULL COMMENT '范围类型: ALL/DEPT/GRADE/DEPT_GRADE/CLASS/SELF',
    ref_id BIGINT COMMENT '关联的实体ID（部门ID/年级ID/班级ID）',
    ref_type VARCHAR(50) COMMENT '关联实体类型: DEPARTMENT/GRADE/CLASS',
    parent_scope VARCHAR(200) COMMENT '父范围表达式',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_scope_expression (scope_expression),
    INDEX idx_scope_type (scope_type),
    INDEX idx_ref_id (ref_id),
    INDEX idx_parent_scope (parent_scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='范围元数据表';

-- ============================================================
-- 3. 用户范围分配表 (业务视图，便于管理和查询)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_scope_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    scope_expression VARCHAR(200) NOT NULL COMMENT '范围表达式',
    scope_type VARCHAR(50) NOT NULL COMMENT '范围类型',
    display_name VARCHAR(100) COMMENT '显示名称',
    assigned_by BIGINT COMMENT '分配人ID',
    assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
    expires_at DATETIME COMMENT '过期时间（可选）',
    remark VARCHAR(500) COMMENT '备注',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_scope_type (scope_type),
    INDEX idx_deleted (deleted),
    UNIQUE KEY uk_user_scope (user_id, scope_expression, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户范围分配表';

-- ============================================================
-- 4. 权限审计日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS permission_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action_type VARCHAR(50) NOT NULL COMMENT '操作类型: GRANT/REVOKE/MODIFY',
    target_type VARCHAR(50) NOT NULL COMMENT '目标类型: ROLE/USER/SCOPE/POLICY',
    target_id VARCHAR(100) NOT NULL COMMENT '目标ID',
    old_value TEXT COMMENT '变更前的值（JSON格式）',
    new_value TEXT COMMENT '变更后的值（JSON格式）',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) COMMENT '操作人姓名',
    operation_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    INDEX idx_target (target_type, target_id),
    INDEX idx_operator (operator_id),
    INDEX idx_time (operation_time),
    INDEX idx_action_type (action_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限审计日志表';

-- ============================================================
-- 5. 角色权限模板表 (快速配置助手)
-- ============================================================
CREATE TABLE IF NOT EXISTS role_permission_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    default_scope_type VARCHAR(50) NOT NULL COMMENT '默认范围类型',
    scope_pattern VARCHAR(200) COMMENT '范围模式，支持占位符如 {deptId}',
    permissions JSON COMMENT '权限列表（JSON数组）',
    description VARCHAR(500) COMMENT '模板描述',
    is_default TINYINT DEFAULT 0 COMMENT '是否为默认模板: 0-否, 1-是',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role_code (role_code),
    INDEX idx_is_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限模板表';

-- ============================================================
-- 初始数据: 范围元数据
-- ============================================================
INSERT INTO scope_metadata (scope_expression, display_name, scope_type, ref_id, ref_type, sort_order) VALUES
('scope:*', '全部数据', 'ALL', NULL, NULL, 0),
('scope:self', '仅本人数据', 'SELF', NULL, NULL, 100),
('scope:self_class', '本人所在班级', 'SELF', NULL, NULL, 101),
('scope:self_dept', '本人所在部门', 'SELF', NULL, NULL, 102);

-- 注意: 部门、年级、班级的范围元数据应在创建实体时自动同步插入
-- 以下为示例数据，实际使用时应通过同步服务自动生成
-- INSERT INTO scope_metadata (scope_expression, display_name, scope_type, ref_id, ref_type, sort_order) VALUES
-- ('scope:dept:1', '汽车系', 'DEPT', 1, 'DEPARTMENT', 10),
-- ('scope:dept:2', '经信系', 'DEPT', 2, 'DEPARTMENT', 11),
-- ('scope:dept:3', '康养系', 'DEPT', 3, 'DEPARTMENT', 12),
-- ('scope:grade:24', '24级', 'GRADE', 24, 'GRADE', 20),
-- ('scope:grade:25', '25级', 'GRADE', 25, 'GRADE', 21);

-- ============================================================
-- 初始数据: 角色权限模板
-- ============================================================
INSERT INTO role_permission_templates (role_code, template_name, default_scope_type, scope_pattern, permissions, description, is_default, sort_order) VALUES
('ADMIN', '系统管理员', 'ALL', 'scope:*',
 '["*:*:*"]',
 '拥有系统全部数据的访问权限', 1, 0),

('DEPT_MANAGER', '系部负责人', 'DEPT', 'scope:dept:{deptId}',
 '["student:*:*", "class:*:*", "dormitory:*:*", "check:*:*"]',
 '可访问指定部门下的所有数据', 1, 10),

('GRADE_DIRECTOR', '年级主任', 'DEPT_GRADE', 'scope:dept_grade:{deptId}:{gradeId}',
 '["student:*:*", "class:*:*", "dormitory:*:*", "check:*:*"]',
 '可访问指定部门和年级交叉的所有班级数据', 1, 20),

('CLASS_TEACHER', '班主任', 'CLASS', 'scope:class:{classId}',
 '["student:*:read", "student:*:update", "class:*:read", "dormitory:*:read", "check:*:*"]',
 '可访问指定班级的学生、宿舍、检查等数据', 1, 30),

('TEACHER', '普通教师', 'SELF', 'scope:self',
 '["student:*:read", "class:*:read"]',
 '仅可查看本人相关的数据', 1, 40),

('INSPECTOR', '检查员', 'SELF', 'scope:self',
 '["check:*:*"]',
 '可进行检查操作，仅查看本人创建的检查记录', 1, 50);

-- ============================================================
-- 初始Casbin策略 (示例)
-- ============================================================
-- 注意: 这些策略应根据实际需求调整

-- 角色定义策略 (p = role, scope_pattern, resource, action, effect)
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3, v4) VALUES
-- 管理员: 全部数据的全部权限
('p', 'admin', 'scope:*', '*', '*', 'allow'),

-- 系部负责人: 部门范围内的管理权限
('p', 'dept_manager', 'scope:dept:*', 'student', '*', 'allow'),
('p', 'dept_manager', 'scope:dept:*', 'class', '*', 'allow'),
('p', 'dept_manager', 'scope:dept:*', 'dormitory', '*', 'allow'),
('p', 'dept_manager', 'scope:dept:*', 'check', '*', 'allow'),

-- 年级主任: 部门+年级范围内的管理权限
('p', 'grade_director', 'scope:dept_grade:*', 'student', '*', 'allow'),
('p', 'grade_director', 'scope:dept_grade:*', 'class', '*', 'allow'),
('p', 'grade_director', 'scope:dept_grade:*', 'check', '*', 'allow'),

-- 班主任: 班级范围内的权限
('p', 'class_teacher', 'scope:class:*', 'student', 'read', 'allow'),
('p', 'class_teacher', 'scope:class:*', 'student', 'update', 'allow'),
('p', 'class_teacher', 'scope:class:*', 'class', 'read', 'allow'),
('p', 'class_teacher', 'scope:class:*', 'dormitory', 'read', 'allow'),
('p', 'class_teacher', 'scope:class:*', 'check', '*', 'allow'),

-- 普通教师: 只读权限
('p', 'teacher', 'scope:self', 'student', 'read', 'allow'),
('p', 'teacher', 'scope:self', 'class', 'read', 'allow');

COMMIT;

-- ============================================================
-- 数据迁移脚本 (从旧表迁移到新表)
-- 执行此部分前请确保已备份数据
-- ============================================================

-- 步骤1: 检查是否存在旧表
-- SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'user_data_scopes';

-- 步骤2: 迁移 user_data_scopes 到新系统 (取消注释执行)
/*
INSERT INTO user_scope_assignments (user_id, scope_expression, scope_type, display_name, assigned_at, deleted)
SELECT
    uds.user_id,
    CASE uds.scope_type
        WHEN 'DEPARTMENT' THEN CONCAT('scope:dept:', uds.scope_id)
        WHEN 'GRADE' THEN CONCAT('scope:grade:', uds.scope_id)
        WHEN 'CLASS' THEN CONCAT('scope:class:', uds.scope_id)
        ELSE 'scope:self'
    END as scope_expression,
    CASE uds.scope_type
        WHEN 'DEPARTMENT' THEN 'DEPT'
        WHEN 'GRADE' THEN 'GRADE'
        WHEN 'CLASS' THEN 'CLASS'
        ELSE 'SELF'
    END as scope_type,
    uds.scope_name as display_name,
    uds.created_at as assigned_at,
    uds.deleted
FROM user_data_scopes uds
WHERE NOT EXISTS (
    SELECT 1 FROM user_scope_assignments usa
    WHERE usa.user_id = uds.user_id
    AND usa.scope_expression = CASE uds.scope_type
        WHEN 'DEPARTMENT' THEN CONCAT('scope:dept:', uds.scope_id)
        WHEN 'GRADE' THEN CONCAT('scope:grade:', uds.scope_id)
        WHEN 'CLASS' THEN CONCAT('scope:class:', uds.scope_id)
        ELSE 'scope:self'
    END
);
*/

-- 步骤3: 同步到 Casbin 规则表 (g2 关系) (取消注释执行)
/*
INSERT INTO casbin_rule (ptype, v0, v1)
SELECT DISTINCT
    'g2',
    CONCAT('user:', usa.user_id),
    usa.scope_expression
FROM user_scope_assignments usa
WHERE usa.deleted = 0
AND NOT EXISTS (
    SELECT 1 FROM casbin_rule cr
    WHERE cr.ptype = 'g2'
    AND cr.v0 = CONCAT('user:', usa.user_id)
    AND cr.v1 = usa.scope_expression
);
*/

-- 步骤4: 同步范围元数据 (取消注释执行)
/*
-- 同步部门
INSERT INTO scope_metadata (scope_expression, display_name, scope_type, ref_id, ref_type, sort_order)
SELECT
    CONCAT('scope:dept:', d.id),
    d.name,
    'DEPT',
    d.id,
    'DEPARTMENT',
    10 + d.id
FROM departments d
WHERE d.deleted = 0
AND NOT EXISTS (
    SELECT 1 FROM scope_metadata sm WHERE sm.scope_expression = CONCAT('scope:dept:', d.id)
);

-- 同步年级
INSERT INTO scope_metadata (scope_expression, display_name, scope_type, ref_id, ref_type, sort_order)
SELECT
    CONCAT('scope:grade:', g.id),
    g.name,
    'GRADE',
    g.id,
    'GRADE',
    20 + g.id
FROM grades g
WHERE g.deleted = 0
AND NOT EXISTS (
    SELECT 1 FROM scope_metadata sm WHERE sm.scope_expression = CONCAT('scope:grade:', g.id)
);

-- 同步班级
INSERT INTO scope_metadata (scope_expression, display_name, scope_type, ref_id, ref_type, parent_scope, sort_order)
SELECT
    CONCAT('scope:class:', c.id),
    c.name,
    'CLASS',
    c.id,
    'CLASS',
    CONCAT('scope:dept_grade:', c.department_id, ':', c.grade_id),
    30 + c.id
FROM classes c
WHERE c.deleted = 0
AND NOT EXISTS (
    SELECT 1 FROM scope_metadata sm WHERE sm.scope_expression = CONCAT('scope:class:', c.id)
);

-- 同步部门+年级交叉范围
INSERT INTO scope_metadata (scope_expression, display_name, scope_type, ref_id, ref_type, parent_scope, sort_order)
SELECT DISTINCT
    CONCAT('scope:dept_grade:', c.department_id, ':', c.grade_id),
    CONCAT(d.name, ' - ', g.name),
    'DEPT_GRADE',
    NULL,
    NULL,
    CONCAT('scope:dept:', c.department_id),
    25
FROM classes c
JOIN departments d ON c.department_id = d.id
JOIN grades g ON c.grade_id = g.id
WHERE c.deleted = 0
AND NOT EXISTS (
    SELECT 1 FROM scope_metadata sm
    WHERE sm.scope_expression = CONCAT('scope:dept_grade:', c.department_id, ':', c.grade_id)
);

-- 添加 g2 层级关系 (班级 -> 部门+年级, 年级, 部门)
INSERT INTO casbin_rule (ptype, v0, v1)
SELECT DISTINCT
    'g2',
    CONCAT('scope:class:', c.id),
    CONCAT('scope:dept_grade:', c.department_id, ':', c.grade_id)
FROM classes c
WHERE c.deleted = 0
AND NOT EXISTS (
    SELECT 1 FROM casbin_rule cr
    WHERE cr.ptype = 'g2'
    AND cr.v0 = CONCAT('scope:class:', c.id)
    AND cr.v1 = CONCAT('scope:dept_grade:', c.department_id, ':', c.grade_id)
);

INSERT INTO casbin_rule (ptype, v0, v1)
SELECT DISTINCT
    'g2',
    CONCAT('scope:class:', c.id),
    CONCAT('scope:grade:', c.grade_id)
FROM classes c
WHERE c.deleted = 0
AND NOT EXISTS (
    SELECT 1 FROM casbin_rule cr
    WHERE cr.ptype = 'g2'
    AND cr.v0 = CONCAT('scope:class:', c.id)
    AND cr.v1 = CONCAT('scope:grade:', c.grade_id)
);

INSERT INTO casbin_rule (ptype, v0, v1)
SELECT DISTINCT
    'g2',
    CONCAT('scope:class:', c.id),
    CONCAT('scope:dept:', c.department_id)
FROM classes c
WHERE c.deleted = 0
AND NOT EXISTS (
    SELECT 1 FROM casbin_rule cr
    WHERE cr.ptype = 'g2'
    AND cr.v0 = CONCAT('scope:class:', c.id)
    AND cr.v1 = CONCAT('scope:dept:', c.department_id)
);
*/

-- ============================================================
-- 清理脚本 (迁移完成后可选执行)
-- ============================================================

/*
-- 重命名旧表为备份
RENAME TABLE user_data_scopes TO user_data_scopes_backup;
RENAME TABLE role_data_permissions TO role_data_permissions_backup;

-- 或者删除旧表 (谨慎操作!)
-- DROP TABLE user_data_scopes_backup;
-- DROP TABLE role_data_permissions_backup;
*/
