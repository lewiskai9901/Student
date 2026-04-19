-- ============================================================
-- Phase 5.1  多租户插件启用表
-- 日期: 2026-04-22
--
-- 目的: 让每个 tenant 独立控制插件启用状态, 而非全局 plugin_packages.enabled.
--
-- 架构:
--   plugin_packages.enabled         — 全局是否安装 (运维级)
--   tenant_plugin_enablement.enabled — 每个租户自己的开关 (租户级)
--
-- 判定"租户 T 能看到插件 P 的数据"需满足:
--   plugin_packages.enabled = 1 (全局已装)
--   AND (
--        tenant_plugin_enablement 有对应 row AND enabled=1
--     OR tenant_plugin_enablement 无该行 AND 插件默认启用 (CORE 默认对所有租户启用)
--   )
--
-- 幂等: IF NOT EXISTS + ON DUPLICATE 保护, 多次执行安全
-- ============================================================

CREATE TABLE IF NOT EXISTS `tenant_plugin_enablement` (
  `tenant_id`    BIGINT       NOT NULL COMMENT '租户 ID',
  `plugin_code`  VARCHAR(20)  NOT NULL COMMENT '插件码, 对应 plugin_packages.industry_code',
  `enabled`      TINYINT      NOT NULL DEFAULT 1 COMMENT '此租户是否启用 (1=启用/0=禁用)',
  `config_json`  JSON         NULL COMMENT '租户级插件配置 (如 EDU 插件的学校代码)',
  `enabled_by`   BIGINT       NULL COMMENT '启用操作者 user_id',
  `enabled_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '最近变更时间',
  `notes`        VARCHAR(500) NULL COMMENT '备注',
  PRIMARY KEY (`tenant_id`, `plugin_code`),
  KEY `idx_plugin_code` (`plugin_code`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='租户级插件启用表';

-- 默认数据: 现有唯一租户 (tenant_id=1) 启用所有已安装插件
INSERT IGNORE INTO `tenant_plugin_enablement`
  (`tenant_id`, `plugin_code`, `enabled`, `enabled_at`, `notes`)
SELECT 1, `industry_code`, 1, NOW(), '默认租户初始化'
  FROM `plugin_packages`
 WHERE `enabled` = 1;

-- 验证: 应该产生 2 行 (CORE + EDU)
SELECT tenant_id, plugin_code, enabled, enabled_at
  FROM tenant_plugin_enablement
 ORDER BY tenant_id, plugin_code;
