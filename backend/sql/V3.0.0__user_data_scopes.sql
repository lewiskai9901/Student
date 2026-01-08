-- ============================================================================
-- 用户数据范围表 - 权限系统升级 V3.0.0
--
-- 设计理念：
-- 1. 角色 = 功能权限（能做什么操作）
-- 2. 数据范围 = 数据权限（能看什么数据）
-- 3. 两者正交分离，互不干扰
--
-- 替代原有设计：
-- - departments.leader_id
-- - grades.grade_director_id / grade_counselor_id
-- - classes.teacher_id / assistant_teacher_id
-- ============================================================================

-- 1. 创建用户数据范围表
CREATE TABLE IF NOT EXISTS user_data_scopes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    scope_type VARCHAR(20) NOT NULL COMMENT '范围类型: DEPARTMENT-部门, GRADE-年级, CLASS-班级',
    scope_id BIGINT NOT NULL COMMENT '范围ID（部门ID/年级ID/班级ID）',
    scope_name VARCHAR(100) COMMENT '范围名称（冗余字段，方便显示）',
    include_children TINYINT DEFAULT 1 COMMENT '是否包含下级: 1-是, 0-否（部门包含子部门和班级，年级包含班级）',

    -- 审计字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by BIGINT COMMENT '更新人ID',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',

    -- 唯一约束：同一用户对同一范围只能有一条记录
    UNIQUE KEY uk_user_scope (user_id, scope_type, scope_id, deleted),

    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_scope (scope_type, scope_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户数据范围表';

-- 2. 从现有表迁移数据

-- 2.1 迁移部门负责人
INSERT INTO user_data_scopes (user_id, scope_type, scope_id, scope_name, include_children, created_by)
SELECT
    d.leader_id,
    'DEPARTMENT',
    d.id,
    d.dept_name,
    1,
    1
FROM departments d
WHERE d.leader_id IS NOT NULL
  AND d.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM user_data_scopes uds
      WHERE uds.user_id = d.leader_id
        AND uds.scope_type = 'DEPARTMENT'
        AND uds.scope_id = d.id
        AND uds.deleted = 0
  );

-- 2.2 迁移年级主任
INSERT INTO user_data_scopes (user_id, scope_type, scope_id, scope_name, include_children, created_by)
SELECT
    g.grade_director_id,
    'GRADE',
    g.id,
    g.grade_name,
    1,
    1
FROM grades g
WHERE g.grade_director_id IS NOT NULL
  AND g.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM user_data_scopes uds
      WHERE uds.user_id = g.grade_director_id
        AND uds.scope_type = 'GRADE'
        AND uds.scope_id = g.id
        AND uds.deleted = 0
  );

-- 2.3 迁移年级辅导员（如果有的话）
INSERT INTO user_data_scopes (user_id, scope_type, scope_id, scope_name, include_children, created_by)
SELECT
    g.grade_counselor_id,
    'GRADE',
    g.id,
    g.grade_name,
    1,
    1
FROM grades g
WHERE g.grade_counselor_id IS NOT NULL
  AND g.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM user_data_scopes uds
      WHERE uds.user_id = g.grade_counselor_id
        AND uds.scope_type = 'GRADE'
        AND uds.scope_id = g.id
        AND uds.deleted = 0
  );

-- 2.4 迁移班主任
INSERT INTO user_data_scopes (user_id, scope_type, scope_id, scope_name, include_children, created_by)
SELECT
    c.teacher_id,
    'CLASS',
    c.id,
    c.class_name,
    0,
    1
FROM classes c
WHERE c.teacher_id IS NOT NULL
  AND c.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM user_data_scopes uds
      WHERE uds.user_id = c.teacher_id
        AND uds.scope_type = 'CLASS'
        AND uds.scope_id = c.id
        AND uds.deleted = 0
  );

-- 2.5 迁移副班主任
INSERT INTO user_data_scopes (user_id, scope_type, scope_id, scope_name, include_children, created_by)
SELECT
    c.assistant_teacher_id,
    'CLASS',
    c.id,
    c.class_name,
    0,
    1
FROM classes c
WHERE c.assistant_teacher_id IS NOT NULL
  AND c.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM user_data_scopes uds
      WHERE uds.user_id = c.assistant_teacher_id
        AND uds.scope_type = 'CLASS'
        AND uds.scope_id = c.id
        AND uds.deleted = 0
  );

-- 3. 添加注释说明旧字段将被废弃（暂不删除，保持兼容）
-- ALTER TABLE departments COMMENT = '部门表（注意：leader_id字段已废弃，请使用user_data_scopes表）';
-- ALTER TABLE grades COMMENT = '年级表（注意：grade_director_id/grade_counselor_id字段已废弃，请使用user_data_scopes表）';
-- ALTER TABLE classes COMMENT = '班级表（注意：teacher_id/assistant_teacher_id字段已废弃，请使用user_data_scopes表）';
