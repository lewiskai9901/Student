-- V105: MsgNotification 接收人多态化 (Road to S - S-1)
-- 当前 user_id 单字段限制接收人只能是 USER. 引入 receiver_type + receiver_id 支持
-- USER / GROUP / ROLE 三类接收人. user_id 保留向后兼容 (USER 类型时与 receiver_id 同步).

-- 检查列存在再加 (information_schema 条件化, 可重复执行)
SET @schema_name = (SELECT DATABASE());

-- receiver_type
SET @col_check = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'msg_notifications' AND COLUMN_NAME = 'receiver_type'
);
SET @sql = IF(@col_check = 0,
  'ALTER TABLE msg_notifications ADD COLUMN receiver_type VARCHAR(32) NOT NULL DEFAULT ''USER'' COMMENT ''接收人类型 USER/GROUP/ROLE'' AFTER tenant_id',
  'SELECT ''receiver_type already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- receiver_id
SET @col_check = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'msg_notifications' AND COLUMN_NAME = 'receiver_id'
);
SET @sql = IF(@col_check = 0,
  'ALTER TABLE msg_notifications ADD COLUMN receiver_id BIGINT NOT NULL DEFAULT 0 COMMENT ''接收人 ID (USER=user_id, GROUP=group_id, ROLE=role_id)'' AFTER receiver_type',
  'SELECT ''receiver_id already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 数据迁移: 现有记录 user_id → receiver_id, receiver_type 默认已是 USER
UPDATE msg_notifications
SET receiver_id = user_id
WHERE receiver_id = 0 AND user_id IS NOT NULL;

-- 索引: 按接收人查询 (取代旧 idx_user_id 时仍保留 user_id 索引向后兼容)
SET @idx_check = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'msg_notifications' AND INDEX_NAME = 'idx_receiver'
);
SET @sql = IF(@idx_check = 0,
  'CREATE INDEX idx_receiver ON msg_notifications (receiver_type, receiver_id, is_read, created_at DESC)',
  'SELECT ''idx_receiver already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
