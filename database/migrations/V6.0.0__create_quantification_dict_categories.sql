-- ============================================================================
-- V6.0.0: 创建量化类型字典表
-- 用于存储独立的检查类别定义（不依赖模板）
-- ============================================================================

CREATE TABLE IF NOT EXISTS `quantification_dict_categories` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `category_code` VARCHAR(50) NOT NULL COMMENT '类别编码',
    `category_name` VARCHAR(100) NOT NULL COMMENT '类别名称',
    `description` VARCHAR(500) COMMENT '描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1=启用, 0=禁用',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_code` (`category_code`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='量化类型字典表';

-- 插入初始测试数据
INSERT INTO `quantification_dict_categories` (`id`, `category_code`, `category_name`, `description`, `status`, `sort_order`) VALUES
(1, 'hygiene', '卫生检查', '宿舍/教室卫生情况检查', 1, 1),
(2, 'discipline', '纪律检查', '学生日常行为纪律检查', 1, 2),
(3, 'safety', '安全检查', '消防安全、用电安全等检查', 1, 3),
(4, 'attendance', '考勤检查', '出勤、迟到、早退等检查', 1, 4);
