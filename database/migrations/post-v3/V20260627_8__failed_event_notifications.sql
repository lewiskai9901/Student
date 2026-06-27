-- 区块3-B: 事件通知分发失败死信队列。
-- baseline_v3 已含本表 (fresh init 无需本文件); 本迁移仅供已有、落后于 baseline 的 dev 库追赶。
CREATE TABLE `failed_event_notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL COMMENT '待重试的 entity_events.id',
  `error_message` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `retry_count` int NOT NULL DEFAULT '0',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RESOLVED/EXHAUSTED',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `last_retry_at` datetime DEFAULT NULL,
  `tenant_id` bigint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_status_retry` (`status`,`retry_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件通知分发失败死信队列';
