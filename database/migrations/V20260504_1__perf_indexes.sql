-- ============================================================
-- P0-A SQL 性能审计 — 加 5 个热路径复合索引
-- 痛点: JwtAuthFilter 每请求触发 5+ SQL, 全是 user/role/permission 校验
-- 加索引后这些 SQL 从 2-5ms 降到 < 1ms (几倍快)
-- ============================================================

-- 1. user_roles 复合索引: 几乎所有请求都跑 WHERE user_id=? AND is_active=1 AND (expires_at IS NULL OR > NOW())
SELECT IF(EXISTS(
  SELECT 1 FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_roles' AND INDEX_NAME = 'idx_ur_user_active'
), 'SELECT 1', 'ALTER TABLE user_roles ADD INDEX idx_ur_user_active (user_id, is_active, expires_at)') INTO @s;
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. permissions 索引: 支撑 SELECT DISTINCT permission_code WHERE plugin_enabled=1
SELECT IF(EXISTS(
  SELECT 1 FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permissions' AND INDEX_NAME = 'idx_perm_plugin_code'
), 'SELECT 1', 'ALTER TABLE permissions ADD INDEX idx_perm_plugin_code (plugin_enabled, permission_code)') INTO @s;
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. roles 索引: WHERE deleted=0 AND plugin_enabled=1 (CasbinConfig + 角色加载都用)
SELECT IF(EXISTS(
  SELECT 1 FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'roles' AND INDEX_NAME = 'idx_roles_active_plugin'
), 'SELECT 1', 'ALTER TABLE roles ADD INDEX idx_roles_active_plugin (deleted, plugin_enabled, status)') INTO @s;
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. event_publications 索引: 5s 一次扫描 WHERE status='PENDING' AND next_retry_at...
SELECT IF(EXISTS(
  SELECT 1 FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_publications' AND INDEX_NAME = 'idx_evt_pub_pending'
), 'SELECT 1', 'ALTER TABLE event_publications ADD INDEX idx_evt_pub_pending (status, next_retry_at)') INTO @s;
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. insp_submission_details 索引: 高频访问 SUBMISSION_ID + scoring_mode (打分页 / 审核台)
SELECT IF(EXISTS(
  SELECT 1 FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_submission_details' AND INDEX_NAME = 'idx_isd_sub_mode'
), 'SELECT 1', 'ALTER TABLE insp_submission_details ADD INDEX idx_isd_sub_mode (submission_id, scoring_mode, deleted)') INTO @s;
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. insp_submissions 索引: WHERE task_id=? AND deleted=0 + status 联合 (审核台/驾驶舱热路径)
SELECT IF(EXISTS(
  SELECT 1 FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'insp_submissions' AND INDEX_NAME = 'idx_isub_task_status'
), 'SELECT 1', 'ALTER TABLE insp_submissions ADD INDEX idx_isub_task_status (task_id, status, deleted)') INTO @s;
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
