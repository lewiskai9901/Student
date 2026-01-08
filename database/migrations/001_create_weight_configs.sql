-- ====================================================================
-- 创建加权配置表 (weight_configs)
--
-- 目的: 修复check_records_v3表的外键约束问题，实现加权功能
-- 作者: Claude Code
-- 日期: 2025-11-23
-- 优先级: 🔴 最高 - 立即执行
-- ====================================================================

-- 1. 创建weight_configs表
CREATE TABLE IF NOT EXISTS `weight_configs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_name` VARCHAR(100) NOT NULL COMMENT '配置名称',
  `config_code` VARCHAR(50) NOT NULL COMMENT '配置编码(唯一)',
  `config_desc` VARCHAR(500) NULL COMMENT '配置说明',

  -- 加权模式配置
  `weight_mode` VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '加权模式: STANDARD(标准人数), PER_CAPITA(人均), NONE(不加权)',

  -- 标准人数配置
  `standard_size_mode` VARCHAR(20) NULL COMMENT '标准人数模式: FIXED(固定值), TARGET_AVERAGE(目标平均), DYNAMIC(动态计算)',
  `standard_size_value` INT NULL COMMENT '固定标准人数值(当mode=FIXED时)',

  -- 适用范围
  `apply_scope` VARCHAR(20) DEFAULT 'ALL' COMMENT '适用范围: ALL(全部), DEPARTMENT(按系), GRADE(按年级)',
  `scope_filter` JSON NULL COMMENT '范围过滤条件(JSON格式)',

  -- 状态和标记
  `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认配置: 0-否, 1-是',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',

  -- 审计字段
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_code` (`config_code`),
  KEY `idx_weight_mode` (`weight_mode`),
  KEY `idx_status` (`status`, `deleted`),
  KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='加权配置表';

-- 2. 插入默认配置数据
INSERT INTO `weight_configs`
(`config_name`, `config_code`, `config_desc`, `weight_mode`, `standard_size_mode`, `standard_size_value`, `is_default`, `status`, `sort_order`)
VALUES
(
  '标准人数加权(40人基准)',
  'STANDARD_40',
  '以40人为标准班级人数，实际人数多于标准则扣分减少，少于标准则扣分增加。适用于人数对检查结果有影响的场景（如卫生检查）。',
  'STANDARD',
  'FIXED',
  40,
  1,  -- 设为默认
  1,
  1
),
(
  '动态标准人数加权',
  'STANDARD_DYNAMIC',
  '自动计算同年级同系的平均人数作为标准，更符合实际情况。建议用于跨系或跨年级的检查。',
  'STANDARD',
  'TARGET_AVERAGE',
  NULL,
  0,
  1,
  2
),
(
  '人均扣分计算',
  'PER_CAPITA',
  '计算人均扣分（总扣分/班级人数），消除人数因素影响。适用于违纪、考勤等个人行为检查。',
  'PER_CAPITA',
  NULL,
  NULL,
  0,
  1,
  3
),
(
  '不加权（原始分数）',
  'NONE',
  '使用原始扣分，不进行任何调整。适用于不需要考虑人数因素的检查。',
  'NONE',
  NULL,
  NULL,
  0,
  1,
  4
);

-- 3. 验证数据插入
SELECT
  id,
  config_name,
  config_code,
  weight_mode,
  standard_size_mode,
  standard_size_value,
  is_default,
  status
FROM weight_configs
ORDER BY sort_order;

-- 4. 添加外键约束到check_records_v3表
-- 注意: 如果check_records_v3表中已有数据且weight_config_id不为NULL，需要先更新
-- UPDATE check_records_v3 SET weight_config_id = (SELECT id FROM weight_configs WHERE is_default = 1) WHERE weight_config_id IS NOT NULL;

-- ALTER TABLE check_records_v3
-- ADD CONSTRAINT fk_check_records_v3_weight_config
-- FOREIGN KEY (weight_config_id) REFERENCES weight_configs(id) ON DELETE SET NULL;

-- ====================================================================
-- 执行说明:
-- 1. 直接在MySQL客户端执行此脚本
-- 2. 执行完成后验证数据是否正确插入
-- 3. 如果需要添加外键约束，请先检查现有数据的weight_config_id值
-- 4. 外键约束部分已注释，需要根据实际情况决定是否启用
-- ====================================================================

-- 回滚脚本 (如果需要)
-- DROP TABLE IF EXISTS weight_configs;
