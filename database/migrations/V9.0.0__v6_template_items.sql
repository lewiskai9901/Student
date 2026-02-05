-- ============================================================================
-- V6 模板扣分项管理表
-- 用于模板管理功能，TaskExecute动态加载扣分项
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 1. 模板类别表 (关联模板)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `template_score_items`;
DROP TABLE IF EXISTS `template_categories`;

CREATE TABLE `template_categories` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_id` BIGINT NOT NULL COMMENT '模板ID',
    `category_code` VARCHAR(50) NOT NULL COMMENT '类别编码',
    `category_name` VARCHAR(100) NOT NULL COMMENT '类别名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '类别描述',
    `icon` VARCHAR(50) DEFAULT 'Folder' COMMENT '图标',
    `color` VARCHAR(20) DEFAULT '#409EFF' COMMENT '颜色',
    `weight` DECIMAL(5,2) DEFAULT 1.00 COMMENT '类别权重',
    `max_score` DECIMAL(6,2) DEFAULT 100.00 COMMENT '满分',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_category` (`template_id`, `category_code`),
    KEY `idx_template` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模板类别表';

-- ---------------------------------------------------------------------------
-- 2. 模板扣分项表
-- ---------------------------------------------------------------------------
CREATE TABLE `template_score_items` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `category_id` BIGINT NOT NULL COMMENT '类别ID',
    `item_code` VARCHAR(50) NOT NULL COMMENT '扣分项编码',
    `item_name` VARCHAR(200) NOT NULL COMMENT '扣分项名称',
    `description` TEXT DEFAULT NULL COMMENT '详细描述',
    `scoring_mode` ENUM('DEDUCTION', 'ADDITION', 'FIXED', 'PER_PERSON', 'RANGE') DEFAULT 'DEDUCTION' COMMENT '计分模式',
    `score` DECIMAL(6,2) NOT NULL DEFAULT 1.00 COMMENT '分值',
    `min_score` DECIMAL(6,2) DEFAULT 0 COMMENT '最小分值',
    `max_score` DECIMAL(6,2) DEFAULT NULL COMMENT '最大分值',
    `per_person_score` DECIMAL(6,2) DEFAULT NULL COMMENT '每人额外扣分',
    `can_link_individual` TINYINT DEFAULT 0 COMMENT '是否可关联个人',
    `requires_photo` TINYINT DEFAULT 0 COMMENT '是否必须拍照',
    `requires_remark` TINYINT DEFAULT 0 COMMENT '是否必须备注',
    `check_points` JSON DEFAULT NULL COMMENT '检查要点',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_item` (`category_id`, `item_code`),
    KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模板扣分项表';

-- ---------------------------------------------------------------------------
-- 3. 初始化数据 - 日常检查模板 (template_id=1)
-- ---------------------------------------------------------------------------
INSERT INTO `template_categories` (`id`, `template_id`, `category_code`, `category_name`, `description`, `icon`, `color`, `weight`, `sort_order`) VALUES
(1, 1, 'HYGIENE', '卫生类', '卫生检查相关扣分项', 'Brush', '#67C23A', 40, 1),
(2, 1, 'SAFETY', '安全类', '安全检查相关扣分项', 'Warning', '#E6A23C', 35, 2),
(3, 1, 'ORDER', '秩序类', '秩序检查相关扣分项', 'List', '#409EFF', 25, 3);

-- 卫生类扣分项 (category_id=1)
INSERT INTO `template_score_items` (`category_id`, `item_code`, `item_name`, `scoring_mode`, `score`, `can_link_individual`, `sort_order`) VALUES
(1, 'HYG001', '地面不清洁', 'DEDUCTION', 3.00, 0, 1),
(1, 'HYG002', '床铺不整洁', 'DEDUCTION', 2.00, 1, 2),
(1, 'HYG003', '阳台杂物', 'DEDUCTION', 2.00, 0, 3),
(1, 'HYG004', '卫生间不洁', 'DEDUCTION', 3.00, 0, 4),
(1, 'HYG005', '垃圾未清理', 'DEDUCTION', 2.00, 0, 5);

-- 安全类扣分项 (category_id=2)
INSERT INTO `template_score_items` (`category_id`, `item_code`, `item_name`, `scoring_mode`, `score`, `can_link_individual`, `sort_order`) VALUES
(2, 'SAF001', '违规电器', 'DEDUCTION', 5.00, 1, 1),
(2, 'SAF002', '私拉电线', 'DEDUCTION', 3.00, 0, 2),
(2, 'SAF003', '消防通道堵塞', 'DEDUCTION', 5.00, 0, 3),
(2, 'SAF004', '门窗损坏', 'DEDUCTION', 2.00, 0, 4);

-- 秩序类扣分项 (category_id=3)
INSERT INTO `template_score_items` (`category_id`, `item_code`, `item_name`, `scoring_mode`, `score`, `can_link_individual`, `sort_order`) VALUES
(3, 'ORD001', '物品摆放混乱', 'DEDUCTION', 2.00, 0, 1),
(3, 'ORD002', '私自张贴', 'DEDUCTION', 1.00, 0, 2),
(3, 'ORD003', '饲养宠物', 'DEDUCTION', 5.00, 1, 3);

-- ---------------------------------------------------------------------------
-- 4. 宿舍检查模板 (template_id=3)
-- ---------------------------------------------------------------------------
INSERT INTO `template_categories` (`id`, `template_id`, `category_code`, `category_name`, `description`, `icon`, `color`, `weight`, `sort_order`) VALUES
(4, 3, 'DORM_HYGIENE', '宿舍卫生', '宿舍卫生检查', 'House', '#67C23A', 50, 1),
(5, 3, 'DORM_SAFETY', '宿舍安全', '宿舍安全检查', 'Warning', '#F56C6C', 30, 2),
(6, 3, 'DORM_DISCIPLINE', '宿舍纪律', '宿舍纪律检查', 'Document', '#909399', 20, 3);

-- 宿舍卫生扣分项 (category_id=4)
INSERT INTO `template_score_items` (`category_id`, `item_code`, `item_name`, `scoring_mode`, `score`, `can_link_individual`, `sort_order`) VALUES
(4, 'DH001', '地面有明显垃圾', 'DEDUCTION', 2.00, 0, 1),
(4, 'DH002', '床铺未整理', 'DEDUCTION', 2.00, 1, 2),
(4, 'DH003', '桌面物品杂乱', 'DEDUCTION', 1.00, 1, 3),
(4, 'DH004', '阳台/窗台脏乱', 'DEDUCTION', 2.00, 0, 4),
(4, 'DH005', '卫生间未清洁', 'DEDUCTION', 3.00, 0, 5);

-- 宿舍安全扣分项 (category_id=5)
INSERT INTO `template_score_items` (`category_id`, `item_code`, `item_name`, `scoring_mode`, `score`, `can_link_individual`, `sort_order`) VALUES
(5, 'DS001', '使用违规电器', 'DEDUCTION', 10.00, 1, 1),
(5, 'DS002', '私接电源/插排', 'DEDUCTION', 5.00, 0, 2),
(5, 'DS003', '易燃物品存放', 'DEDUCTION', 5.00, 0, 3),
(5, 'DS004', '门窗未锁', 'DEDUCTION', 2.00, 0, 4);

-- 宿舍纪律扣分项 (category_id=6)
INSERT INTO `template_score_items` (`category_id`, `item_code`, `item_name`, `scoring_mode`, `score`, `can_link_individual`, `sort_order`) VALUES
(6, 'DD001', '晚归', 'DEDUCTION', 3.00, 1, 1),
(6, 'DD002', '留宿外人', 'DEDUCTION', 10.00, 1, 2),
(6, 'DD003', '大声喧哗', 'DEDUCTION', 2.00, 0, 3);

SET FOREIGN_KEY_CHECKS = 1;
