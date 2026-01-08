-- =====================================================
-- V4.0.0 统一申诉表字段命名
-- 移除旧版字段，保留规范命名的字段
-- =====================================================

-- 1. 迁移旧字段数据到新字段（如果新字段为空）
UPDATE check_item_appeals
SET original_score = original_deduct_score
WHERE original_score IS NULL AND original_deduct_score IS NOT NULL;

UPDATE check_item_appeals
SET appellant_id = appealer_id
WHERE appellant_id IS NULL AND appealer_id IS NOT NULL;

UPDATE check_item_appeals
SET appellant_name = appealer_name
WHERE appellant_name IS NULL AND appealer_name IS NOT NULL;

-- 2. 删除旧版字段
ALTER TABLE check_item_appeals DROP COLUMN IF EXISTS original_deduct_score;
ALTER TABLE check_item_appeals DROP COLUMN IF EXISTS appealer_id;
ALTER TABLE check_item_appeals DROP COLUMN IF EXISTS appealer_name;
ALTER TABLE check_item_appeals DROP COLUMN IF EXISTS appeal_status;
ALTER TABLE check_item_appeals DROP COLUMN IF EXISTS appeal_photos;
ALTER TABLE check_item_appeals DROP COLUMN IF EXISTS appeal_videos;

-- 3. 为新字段添加索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_appellant_id ON check_item_appeals(appellant_id);

-- 4. 添加字段注释
ALTER TABLE check_item_appeals
    MODIFY COLUMN original_score DECIMAL(10,2) COMMENT '原始扣分',
    MODIFY COLUMN appellant_id BIGINT COMMENT '申诉人ID',
    MODIFY COLUMN appellant_name VARCHAR(50) COMMENT '申诉人姓名',
    MODIFY COLUMN status INT DEFAULT 1 COMMENT '申诉状态:1=待审核,2=审核通过,3=审核驳回,4=已撤销,5=已过期,6=公示中,7=已生效';

-- =====================================================
-- 说明：
-- 字段统一命名规范：
-- - original_score: 原始扣分（简洁明了）
-- - appellant_id/appellant_name: 申诉人（标准英语）
-- - status: 统一状态字段（取代appeal_status）
-- =====================================================
