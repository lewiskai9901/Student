-- V28.0.0 Organization Lifecycle Support
-- 组织生命周期：状态机 + 合并/拆分

-- ============================================================
-- 1. org_units 状态从 TINYINT(0/1) 改为 VARCHAR 支持多状态
-- ============================================================
ALTER TABLE org_units ADD COLUMN status_new VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'DRAFT/ACTIVE/FROZEN/MERGING/DISSOLVED';

-- 迁移旧数据
UPDATE org_units SET status_new = CASE
    WHEN status = 1 THEN 'ACTIVE'
    WHEN status = 0 THEN 'FROZEN'
    ELSE 'ACTIVE'
END;

ALTER TABLE org_units DROP COLUMN status;
ALTER TABLE org_units CHANGE COLUMN status_new status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'DRAFT/ACTIVE/FROZEN/MERGING/DISSOLVED';

-- ============================================================
-- 2. 合并/拆分/撤销 支持字段
-- ============================================================
ALTER TABLE org_units ADD COLUMN merged_into_id BIGINT DEFAULT NULL COMMENT '合并到哪个组织';
ALTER TABLE org_units ADD COLUMN split_from_id BIGINT DEFAULT NULL COMMENT '从哪个组织拆分';
ALTER TABLE org_units ADD COLUMN dissolved_at DATETIME DEFAULT NULL COMMENT '撤销时间';
ALTER TABLE org_units ADD COLUMN dissolved_reason VARCHAR(500) DEFAULT NULL COMMENT '撤销原因';

-- 索引
ALTER TABLE org_units ADD INDEX idx_merged_into (merged_into_id);
ALTER TABLE org_units ADD INDEX idx_split_from (split_from_id);
ALTER TABLE org_units ADD INDEX idx_status (status);
