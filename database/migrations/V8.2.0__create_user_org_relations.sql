-- V8.2.0 用户-组织关系表
-- 支持用户多组织归属：主归属、副职、临时归属等

CREATE TABLE IF NOT EXISTS user_org_relations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',

    -- 关系类型
    relation_type VARCHAR(30) NOT NULL COMMENT '关系类型: PRIMARY(主归属), SECONDARY(副职), TEMPORARY(临时), SUPERVISING(分管)',
    position_title VARCHAR(100) DEFAULT NULL COMMENT '职务名称',
    position_level INT DEFAULT NULL COMMENT '职务级别(用于排序)',

    -- 归属属性
    is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主归属(每用户仅一个)',
    is_leader TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否领导(部门负责人)',
    can_manage TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否有管理权限',
    can_approve TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否有审批权限',

    -- 有效期(临时归属使用)
    start_date DATE DEFAULT NULL COMMENT '开始日期',
    end_date DATE DEFAULT NULL COMMENT '结束日期(临时归属)',

    -- 权重(用于成绩分配等)
    weight_ratio DECIMAL(5,2) DEFAULT 100.00 COMMENT '权重比例(百分比)',

    -- 排序
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',

    -- 备注
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',

    -- 系统字段
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',

    -- 索引
    KEY idx_user_id (user_id),
    KEY idx_org_unit_id (org_unit_id),
    KEY idx_relation_type (relation_type),
    KEY idx_is_primary (is_primary),
    KEY idx_is_leader (is_leader),
    KEY idx_start_date (start_date),
    KEY idx_end_date (end_date),

    -- 唯一约束：同一用户在同一组织只能有一种主归属关系
    UNIQUE KEY uk_user_org_primary (user_id, org_unit_id, relation_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-组织关系表';

-- 用户组织关系历史表(审计跟踪)
CREATE TABLE IF NOT EXISTS user_org_relation_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    relation_id BIGINT NOT NULL COMMENT '关系ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',
    relation_type VARCHAR(30) NOT NULL COMMENT '关系类型',
    position_title VARCHAR(100) DEFAULT NULL COMMENT '职务名称',
    is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主归属',
    is_leader TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否领导',

    -- 变更信息
    action VARCHAR(20) NOT NULL COMMENT '操作: CREATE, UPDATE, DELETE',
    change_reason VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
    changed_by BIGINT DEFAULT NULL COMMENT '操作人',
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    -- 索引
    KEY idx_relation_id (relation_id),
    KEY idx_user_id (user_id),
    KEY idx_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-组织关系历史表';

-- 关系类型枚举值说明
-- PRIMARY: 主归属，用户的主要所属部门，每个用户只能有一个
-- SECONDARY: 副职/兼职，用户在其他部门的兼任职务
-- TEMPORARY: 临时借调，有明确的开始和结束日期
-- SUPERVISING: 分管，领导分管某部门但不属于该部门

-- 从现有sys_user表迁移主归属数据
INSERT INTO user_org_relations (user_id, org_unit_id, relation_type, is_primary, sort_order, created_at)
SELECT
    u.id,
    u.department_id,
    'PRIMARY',
    1,
    0,
    NOW()
FROM sys_user u
WHERE u.department_id IS NOT NULL
  AND u.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM user_org_relations r
      WHERE r.user_id = u.id AND r.org_unit_id = u.department_id AND r.deleted = 0
  );
