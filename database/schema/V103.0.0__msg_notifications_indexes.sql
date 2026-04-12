-- =====================================================
-- 消息通知 / 订阅 / 模板 / 事件类型 索引优化
--
-- MySQL 不支持 CREATE INDEX IF NOT EXISTS，
-- 使用 information_schema 判断 + 动态 SQL 保证脚本可重入。
--
-- 表名映射：
--   msg_notifications       （站内消息）
--   msg_subscription_rules  （订阅规则）
--   msg_templates           （消息模板）
--   entity_event_types      （事件类型定义，注意非 event_types）
-- =====================================================

-- ---------- msg_notifications ----------
-- 用户未读查询（最热路径）：user_id + is_read + deleted + created_at
SET @i = (SELECT COUNT(*) FROM information_schema.statistics
          WHERE table_schema = DATABASE()
            AND table_name = 'msg_notifications'
            AND index_name = 'idx_msg_notif_user_read');
SET @s = IF(@i = 0,
    'CREATE INDEX idx_msg_notif_user_read ON msg_notifications (user_id, is_read, deleted, created_at)',
    'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 用户列表分页：user_id + deleted + created_at
SET @i = (SELECT COUNT(*) FROM information_schema.statistics
          WHERE table_schema = DATABASE()
            AND table_name = 'msg_notifications'
            AND index_name = 'idx_msg_notif_user_list');
SET @s = IF(@i = 0,
    'CREATE INDEX idx_msg_notif_user_list ON msg_notifications (user_id, deleted, created_at)',
    'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 按事件查找（统计 / 聚合）
SET @i = (SELECT COUNT(*) FROM information_schema.statistics
          WHERE table_schema = DATABASE()
            AND table_name = 'msg_notifications'
            AND index_name = 'idx_msg_notif_event');
SET @s = IF(@i = 0,
    'CREATE INDEX idx_msg_notif_event ON msg_notifications (event_id)',
    'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- msg_subscription_rules ----------
-- 事件类型 + 软删除：替换旧 idx_event 以支持软删过滤
SET @i = (SELECT COUNT(*) FROM information_schema.statistics
          WHERE table_schema = DATABASE()
            AND table_name = 'msg_subscription_rules'
            AND index_name = 'idx_msg_subscription_event');
SET @s = IF(@i = 0,
    'CREATE INDEX idx_msg_subscription_event ON msg_subscription_rules (event_category, event_type, deleted)',
    'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- msg_templates ----------
-- 按编码 + 软删除：findByCode 查询路径
SET @i = (SELECT COUNT(*) FROM information_schema.statistics
          WHERE table_schema = DATABASE()
            AND table_name = 'msg_templates'
            AND index_name = 'idx_msg_template_code');
SET @s = IF(@i = 0,
    'CREATE INDEX idx_msg_template_code ON msg_templates (template_code, deleted)',
    'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- entity_event_types ----------
-- 按 type_code + 软删除：配置查询
SET @i = (SELECT COUNT(*) FROM information_schema.statistics
          WHERE table_schema = DATABASE()
            AND table_name = 'entity_event_types'
            AND index_name = 'idx_entity_event_type_code');
SET @s = IF(@i = 0,
    'CREATE INDEX idx_entity_event_type_code ON entity_event_types (type_code, deleted)',
    'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
