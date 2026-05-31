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

-- 2. 删除旧版字段 — MySQL 8.0 不支持 DROP COLUMN IF EXISTS, 用 information_schema 条件化
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='check_item_appeals' AND COLUMN_NAME='original_deduct_score');
SET @s := IF(@c>0, "ALTER TABLE check_item_appeals DROP COLUMN original_deduct_score", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='check_item_appeals' AND COLUMN_NAME='appealer_id');
SET @s := IF(@c>0, "ALTER TABLE check_item_appeals DROP COLUMN appealer_id", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='check_item_appeals' AND COLUMN_NAME='appealer_name');
SET @s := IF(@c>0, "ALTER TABLE check_item_appeals DROP COLUMN appealer_name", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='check_item_appeals' AND COLUMN_NAME='appeal_status');
SET @s := IF(@c>0, "ALTER TABLE check_item_appeals DROP COLUMN appeal_status", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='check_item_appeals' AND COLUMN_NAME='appeal_photos');
SET @s := IF(@c>0, "ALTER TABLE check_item_appeals DROP COLUMN appeal_photos", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='check_item_appeals' AND COLUMN_NAME='appeal_videos');
SET @s := IF(@c>0, "ALTER TABLE check_item_appeals DROP COLUMN appeal_videos", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 3. 为新字段添加索引（如果不存在）
SET @x := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='check_item_appeals' AND INDEX_NAME='idx_appellant_id');
SET @s := IF(@x=0, "CREATE INDEX idx_appellant_id ON check_item_appeals(appellant_id)", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

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
