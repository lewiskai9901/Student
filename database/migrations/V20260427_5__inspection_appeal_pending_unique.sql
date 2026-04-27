-- A: 申诉 PENDING 唯一性 — 防止并发提交绕过应用层 anyMatch 校验
-- 经典模式: 生成列存"当 PENDING 时锁定 detail_id, 否则 NULL", 利用 NULL 不参与 UNIQUE 的特性

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inspection_appeals'
      AND COLUMN_NAME = 'pending_lock_key'
);
SET @sql := IF(@col_exists = 0,
    "ALTER TABLE inspection_appeals ADD COLUMN pending_lock_key BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' AND deleted = 0 THEN submission_detail_id ELSE NULL END) STORED COMMENT '并发锁键: 同 detail 仅一条 PENDING — 借 UNIQUE+NULL 实现'",
    "SELECT 'pending_lock_key column already exists' AS msg");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inspection_appeals'
      AND INDEX_NAME = 'uk_appeal_pending_lock'
);
SET @sql := IF(@idx_exists = 0,
    "ALTER TABLE inspection_appeals ADD UNIQUE INDEX uk_appeal_pending_lock (pending_lock_key)",
    "SELECT 'uk_appeal_pending_lock index already exists' AS msg");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
