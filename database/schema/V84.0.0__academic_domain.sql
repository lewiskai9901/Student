-- =====================================================
-- V84.0.0 Academic Domain: Major Categories + Enhanced Majors/Directions
-- =====================================================

-- 1. major_categories: 专业大类表 (技工院校14大类)
CREATE TABLE IF NOT EXISTS major_categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(10)  NOT NULL COMMENT '大类编码',
    name        VARCHAR(100) NOT NULL COMMENT '大类名称',
    sort_order  INT          NOT NULL DEFAULT 0,
    enabled     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业大类';

-- Seed: 14 vocational school major categories
INSERT IGNORE INTO major_categories (code, name, sort_order) VALUES
('01', '机械类', 1),
('02', '电工电子类', 2),
('03', '信息类', 3),
('04', '交通类', 4),
('05', '服务类', 5),
('06', '财经商贸类', 6),
('07', '文化艺术类', 7),
('08', '医药卫生类', 8),
('09', '化工类', 9),
('10', '轻工类', 10),
('11', '建筑类', 11),
('12', '农业类', 12),
('13', '服装类', 13),
('14', '其他', 14);

-- 2. ALTER majors: add vocational-school fields
-- major_category_code
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'majors' AND COLUMN_NAME = 'major_category_code');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE majors ADD COLUMN major_category_code VARCHAR(10) NULL COMMENT ''所属专业大类编码'' AFTER major_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- enrollment_target
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'majors' AND COLUMN_NAME = 'enrollment_target');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE majors ADD COLUMN enrollment_target VARCHAR(50) NULL COMMENT ''招生对象(初中毕业生/高中毕业生/等)'' AFTER description',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- education_form
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'majors' AND COLUMN_NAME = 'education_form');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE majors ADD COLUMN education_form VARCHAR(50) NULL COMMENT ''办学形式(全日制/非全日制)'' AFTER enrollment_target',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- lead_teacher_id
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'majors' AND COLUMN_NAME = 'lead_teacher_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE majors ADD COLUMN lead_teacher_id BIGINT NULL COMMENT ''专业带头人用户ID'' AFTER education_form',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- lead_teacher_name
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'majors' AND COLUMN_NAME = 'lead_teacher_name');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE majors ADD COLUMN lead_teacher_name VARCHAR(100) NULL COMMENT ''专业带头人姓名(冗余)'' AFTER lead_teacher_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- approval_year
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'majors' AND COLUMN_NAME = 'approval_year');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE majors ADD COLUMN approval_year INT NULL COMMENT ''批准设置年份'' AFTER lead_teacher_name',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- major_status
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'majors' AND COLUMN_NAME = 'major_status');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE majors ADD COLUMN major_status VARCHAR(20) NOT NULL DEFAULT ''ENROLLING'' COMMENT ''专业状态(PREPARING/ENROLLING/SUSPENDED/REVOKED)'' AFTER approval_year',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sort_order (may already exist in some setups)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'majors' AND COLUMN_NAME = 'sort_order');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE majors ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT ''排序号'' AFTER major_status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. ALTER major_directions: add vocational-school fields

-- enrollment_target
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_directions' AND COLUMN_NAME = 'enrollment_target');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE major_directions ADD COLUMN enrollment_target VARCHAR(50) NULL COMMENT ''招生对象'' AFTER remarks',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- education_form
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_directions' AND COLUMN_NAME = 'education_form');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE major_directions ADD COLUMN education_form VARCHAR(50) NULL COMMENT ''办学形式'' AFTER enrollment_target',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- certificate_names (JSON array)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_directions' AND COLUMN_NAME = 'certificate_names');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE major_directions ADD COLUMN certificate_names JSON NULL COMMENT ''对应职业资格证书(JSON数组)'' AFTER education_form',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- training_standard
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_directions' AND COLUMN_NAME = 'training_standard');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE major_directions ADD COLUMN training_standard VARCHAR(200) NULL COMMENT ''培养标准/教学大纲'' AFTER certificate_names',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- cooperation_enterprise
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_directions' AND COLUMN_NAME = 'cooperation_enterprise');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE major_directions ADD COLUMN cooperation_enterprise VARCHAR(200) NULL COMMENT ''合作企业'' AFTER training_standard',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- max_enrollment
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_directions' AND COLUMN_NAME = 'max_enrollment');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE major_directions ADD COLUMN max_enrollment INT NULL COMMENT ''最大招生人数'' AFTER cooperation_enterprise',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sort_order
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_directions' AND COLUMN_NAME = 'sort_order');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE major_directions ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT ''排序号'' AFTER max_enrollment',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- enabled
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_directions' AND COLUMN_NAME = 'enabled');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE major_directions ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''是否启用'' AFTER sort_order',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
