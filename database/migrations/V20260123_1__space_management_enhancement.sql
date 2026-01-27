-- =====================================================
-- 场所管理增强 - 支持三级管理体系
-- 学院 → 部门(系部) → 班级
-- =====================================================

-- 1. 扩展 space 表，添加班级归属和性别类型字段（org_unit_id已存在）
ALTER TABLE space
ADD COLUMN IF NOT EXISTS class_id BIGINT NULL COMMENT '归属班级ID（班主任管理）' AFTER org_unit_id,
ADD COLUMN IF NOT EXISTS gender_type TINYINT DEFAULT 0 COMMENT '性别类型：0-不限/混合，1-男，2-女' AFTER class_id;

-- 添加索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_space_class ON space(class_id);
CREATE INDEX IF NOT EXISTS idx_space_gender_type ON space(gender_type);

-- 2. 创建场所-班级分配表（支持一个场所分配给多个班级的场景）
CREATE TABLE IF NOT EXISTS space_class_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL COMMENT '场所ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    org_unit_id BIGINT NOT NULL COMMENT '所属部门ID（冗余，便于权限控制）',
    assigned_beds INT DEFAULT NULL COMMENT '分配床位数（NULL表示不限）',
    priority INT DEFAULT 0 COMMENT '优先级（数值越大越优先）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    assigned_by BIGINT DEFAULT NULL COMMENT '分配人ID',
    assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_space_class (space_id, class_id),
    KEY idx_class_id (class_id),
    KEY idx_org_unit_id (org_unit_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场所-班级分配表（部门分配给班级）';

-- 3. 创建视图：场所详情视图（包含部门、班级、班主任信息）
DROP VIEW IF EXISTS v_space_detail;
CREATE VIEW v_space_detail AS
SELECT
    s.*,
    -- 部门信息
    ou.unit_name AS org_unit_name,
    -- 班级信息（直接分配）
    c.class_name AS assigned_class_name,
    c.teacher_id AS class_teacher_id,
    -- 班主任信息
    t.real_name AS teacher_name,
    t.phone AS teacher_phone
FROM space s
LEFT JOIN org_units ou ON s.org_unit_id = ou.id
LEFT JOIN classes c ON s.class_id = c.id
LEFT JOIN users t ON c.teacher_id = t.id
WHERE s.deleted = 0;

-- 4. 创建视图：场所班级分配详情视图
DROP VIEW IF EXISTS v_space_class_assignment_detail;
CREATE VIEW v_space_class_assignment_detail AS
SELECT
    sca.*,
    s.space_name,
    s.space_code,
    s.space_type,
    s.capacity,
    c.class_name,
    c.class_code,
    c.teacher_id,
    t.real_name AS teacher_name,
    t.phone AS teacher_phone,
    ou.unit_name AS org_unit_name
FROM space_class_assignment sca
JOIN space s ON sca.space_id = s.id
JOIN classes c ON sca.class_id = c.id
LEFT JOIN users t ON c.teacher_id = t.id
LEFT JOIN org_units ou ON sca.org_unit_id = ou.id;

SELECT 'Space management enhancement migration completed.' AS result;
