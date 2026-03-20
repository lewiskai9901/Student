-- V25.0.0 统一访问关系表 (Zanzibar Simplified)
-- 合并 place_org_relations, user_org_relations, user_place_relations 为单表

CREATE TABLE IF NOT EXISTS access_relations (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_type     VARCHAR(50)  NOT NULL COMMENT '资源类型: place, org_unit, student, class...',
    resource_id       BIGINT       NOT NULL COMMENT '资源ID',
    relation          VARCHAR(30)  NOT NULL COMMENT '关系: owner, manager, user, member, viewer, responsible, occupant',
    subject_type      VARCHAR(30)  NOT NULL COMMENT '主体类型: org_unit, user',
    subject_id        BIGINT       NOT NULL COMMENT '主体ID',
    include_children  TINYINT(1)   DEFAULT 0 COMMENT '是否包含主体的子级组织',
    access_level      TINYINT      DEFAULT 1 COMMENT '1=只读, 2=读写',
    metadata          JSON         DEFAULT NULL COMMENT '扩展字段（isPrimary, weightRatio, positionTitle 等）',
    remark            VARCHAR(500) DEFAULT NULL,
    created_by        BIGINT       DEFAULT NULL,
    created_at        DATETIME     DEFAULT NOW(),
    updated_at        DATETIME     DEFAULT NOW() ON UPDATE NOW(),
    deleted           TINYINT(1)   DEFAULT 0,

    INDEX idx_resource (resource_type, resource_id, deleted),
    INDEX idx_subject (subject_type, subject_id, deleted),
    INDEX idx_lookup (resource_type, relation, subject_type, subject_id, deleted),
    UNIQUE INDEX uk_relation (resource_type, resource_id, relation, subject_type, subject_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一访问关系表 (Zanzibar Simplified)';

-- 从 user_org_relations 迁移数据
INSERT INTO access_relations (resource_type, resource_id, relation, subject_type, subject_id, include_children, access_level, metadata, remark, created_by, created_at, updated_at, deleted)
SELECT
    'org_unit',
    uor.org_unit_id,
    CASE uor.relation_type
        WHEN 'PRIMARY' THEN 'member'
        WHEN 'SECONDARY' THEN 'member'
        WHEN 'TEMPORARY' THEN 'viewer'
        WHEN 'SUPERVISING' THEN 'manager'
        ELSE 'member'
    END,
    'user',
    uor.user_id,
    0,
    CASE WHEN uor.can_manage = 1 THEN 2 ELSE 1 END,
    JSON_OBJECT(
        'isPrimary', CAST(uor.is_primary AS JSON),
        'isLeader', CAST(uor.is_leader AS JSON),
        'canManage', CAST(uor.can_manage AS JSON),
        'canApprove', CAST(uor.can_approve AS JSON),
        'positionTitle', uor.position_title,
        'positionLevel', uor.position_level,
        'relationType', uor.relation_type,
        'weightRatio', uor.weight_ratio,
        'startDate', DATE_FORMAT(uor.start_date, '%Y-%m-%d'),
        'endDate', DATE_FORMAT(uor.end_date, '%Y-%m-%d'),
        'sortOrder', uor.sort_order
    ),
    uor.remark,
    uor.created_by,
    uor.created_at,
    uor.updated_at,
    uor.deleted
FROM user_org_relations uor;

-- 从 place_org_relations 迁移数据
INSERT INTO access_relations (resource_type, resource_id, relation, subject_type, subject_id, include_children, access_level, metadata, remark, created_by, created_at, updated_at, deleted)
SELECT
    'place',
    por.place_id,
    CASE por.relation_type
        WHEN 'PRIMARY' THEN 'owner'
        WHEN 'SHARED' THEN 'user'
        WHEN 'MANAGED' THEN 'manager'
        ELSE 'user'
    END,
    'org_unit',
    por.org_unit_id,
    0,
    CASE WHEN por.can_manage = 1 THEN 2 ELSE 1 END,
    JSON_OBJECT(
        'isPrimary', CAST(por.is_primary AS JSON),
        'canUse', CAST(por.can_use AS JSON),
        'canManage', CAST(por.can_manage AS JSON),
        'canAssign', CAST(por.can_assign AS JSON),
        'canInspect', CAST(por.can_inspect AS JSON),
        'relationType', por.relation_type,
        'allocatedCapacity', por.allocated_capacity,
        'weightRatio', por.weight_ratio,
        'startDate', DATE_FORMAT(por.start_date, '%Y-%m-%d'),
        'endDate', DATE_FORMAT(por.end_date, '%Y-%m-%d'),
        'sortOrder', por.sort_order
    ),
    por.remark,
    por.created_by,
    por.created_at,
    por.updated_at,
    por.deleted
FROM place_org_relations por;

-- 从 user_place_relations 迁移数据
INSERT INTO access_relations (resource_type, resource_id, relation, subject_type, subject_id, include_children, access_level, metadata, remark, created_by, created_at, updated_at, deleted)
SELECT
    'place',
    upr.place_id,
    CASE upr.relation_type
        WHEN 'ASSIGNED' THEN 'occupant'
        WHEN 'MANAGED' THEN 'responsible'
        WHEN 'TEMPORARY' THEN 'viewer'
        ELSE 'occupant'
    END,
    'user',
    upr.user_id,
    0,
    CASE WHEN upr.can_manage = 1 THEN 2 ELSE 1 END,
    JSON_OBJECT(
        'isPrimary', CAST(upr.is_primary AS JSON),
        'canUse', CAST(upr.can_use AS JSON),
        'canManage', CAST(upr.can_manage AS JSON),
        'positionCode', upr.position_code,
        'positionName', upr.position_name,
        'relationType', upr.relation_type,
        'feeAmount', upr.fee_amount,
        'feePaid', CAST(upr.fee_paid AS JSON),
        'startDate', DATE_FORMAT(upr.start_date, '%Y-%m-%d'),
        'endDate', DATE_FORMAT(upr.end_date, '%Y-%m-%d'),
        'sortOrder', upr.sort_order
    ),
    upr.remark,
    upr.created_by,
    upr.created_at,
    upr.updated_at,
    upr.deleted
FROM user_place_relations upr;
