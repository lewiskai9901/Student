-- =====================================================
-- 量化管理系统重构 - 创建表结构(不含数据)
-- =====================================================

USE student_management;
SET NAMES utf8mb4;

-- 1. 学期表
CREATE TABLE IF NOT EXISTS semesters (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    semester_name VARCHAR(50) NOT NULL,
    semester_code VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_semester_code (semester_code),
    INDEX idx_is_current (is_current),
    INDEX idx_dates (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 教学周表
CREATE TABLE IF NOT EXISTS academic_weeks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    semester_name VARCHAR(50),
    week_number INT NOT NULL,
    week_name VARCHAR(50),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_semester (semester_id),
    INDEX idx_dates (start_date, end_date),
    INDEX idx_is_current (is_current),
    UNIQUE KEY uk_semester_week (semester_id, week_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 量化模板表
CREATE TABLE IF NOT EXISTS quantification_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_name VARCHAR(100) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    description TEXT,
    is_default TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_by BIGINT,
    updated_by BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_template_code (template_code),
    INDEX idx_is_default (is_default),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 量化模板-量化项关联表
CREATE TABLE IF NOT EXISTS template_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    location_type TINYINT DEFAULT 0,
    auto_bind_location TINYINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_template_id (template_id),
    INDEX idx_category_id (category_id),
    UNIQUE KEY uk_template_category (template_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 量化任务表
CREATE TABLE IF NOT EXISTS quantification_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_code VARCHAR(50) NOT NULL,
    task_name VARCHAR(100) NOT NULL,
    template_id BIGINT,
    template_name VARCHAR(100),
    check_date DATE NOT NULL,
    semester_id BIGINT,
    week_number INT,
    grade_ids JSON NOT NULL,
    grade_names VARCHAR(500),
    checker_id BIGINT,
    checker_name VARCHAR(50),
    status TINYINT DEFAULT 1,
    remarks TEXT,
    archived TINYINT DEFAULT 0,
    archived_at DATETIME,
    created_by BIGINT,
    updated_by BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_task_code (task_code),
    INDEX idx_check_date (check_date),
    INDEX idx_status (status),
    INDEX idx_archived (archived),
    INDEX idx_semester_week (semester_id, week_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 量化任务-量化项配置表
CREATE TABLE IF NOT EXISTS task_category_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    category_name VARCHAR(50),
    location_type TINYINT DEFAULT 0,
    auto_bind_location TINYINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_category_id (category_id),
    UNIQUE KEY uk_task_category (task_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 检查场所关联表
CREATE TABLE IF NOT EXISTS quantification_location_bindings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    class_name VARCHAR(100),
    location_type TINYINT NOT NULL,
    location_id BIGINT NOT NULL,
    location_name VARCHAR(100),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_category (task_id, category_id),
    INDEX idx_class_id (class_id),
    INDEX idx_location (location_type, location_id),
    UNIQUE KEY uk_task_category_location (task_id, category_id, location_type, location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. 量化检查记录表
CREATE TABLE IF NOT EXISTS quantification_check_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    class_name VARCHAR(100),
    category_id BIGINT NOT NULL,
    category_name VARCHAR(50),
    deduction_item_id BIGINT,
    deduction_item_name VARCHAR(100),
    deduction_mode TINYINT,
    location_type TINYINT,
    location_id BIGINT,
    location_name VARCHAR(100),
    deduction_score DECIMAL(10,2) NOT NULL DEFAULT 0,
    person_count INT DEFAULT 0,
    base_score DECIMAL(10,2) DEFAULT 0,
    original_score DECIMAL(10,2),
    remarks TEXT,
    photos JSON,
    checker_id BIGINT,
    checker_name VARCHAR(50),
    check_time DATETIME NOT NULL,
    appeal_status TINYINT DEFAULT 0,
    archived TINYINT DEFAULT 0,
    archived_at DATETIME,
    created_by BIGINT,
    updated_by BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_task_id (task_id),
    INDEX idx_class_id (class_id),
    INDEX idx_category_id (category_id),
    INDEX idx_location (location_type, location_id),
    INDEX idx_check_time (check_time),
    INDEX idx_appeal_status (appeal_status),
    INDEX idx_archived (archived)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. 量化汇总配置表
CREATE TABLE IF NOT EXISTS quantification_summary_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_name VARCHAR(100) NOT NULL,
    summary_type TINYINT NOT NULL,
    semester_id BIGINT,
    semester_name VARCHAR(50),
    week_number INT,
    start_date DATE,
    end_date DATE,
    year INT,
    month INT,
    grade_ids JSON,
    grade_names VARCHAR(500),
    rating_rule_type TINYINT DEFAULT 1,
    auto_publish TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    calculated_at DATETIME,
    published_at DATETIME,
    archived TINYINT DEFAULT 0,
    archived_at DATETIME,
    created_by BIGINT,
    updated_by BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_summary_type (summary_type),
    INDEX idx_semester_week (semester_id, week_number),
    INDEX idx_dates (start_date, end_date),
    INDEX idx_status (status),
    INDEX idx_archived (archived)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. 量化汇总结果表
CREATE TABLE IF NOT EXISTS quantification_summary_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    class_name VARCHAR(100),
    grade_id BIGINT,
    grade_name VARCHAR(50),
    total_deduction DECIMAL(10,2) DEFAULT 0,
    check_count INT DEFAULT 0,
    average_deduction DECIMAL(10,2) DEFAULT 0,
    rank_in_grade INT,
    rank_in_school INT,
    rank_percent DECIMAL(5,2),
    rating_level_id BIGINT,
    rating_level_name VARCHAR(50),
    category_details JSON,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_id (config_id),
    INDEX idx_class_id (class_id),
    INDEX idx_grade_id (grade_id),
    INDEX idx_rank_in_grade (rank_in_grade),
    INDEX idx_rating_level (rating_level_id),
    UNIQUE KEY uk_config_class (config_id, class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. 量化评级等级表
CREATE TABLE IF NOT EXISTS quantification_rating_levels (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    level_name VARCHAR(50) NOT NULL,
    level_code VARCHAR(20) NOT NULL,
    level_color VARCHAR(20),
    level_icon VARCHAR(50),
    rule_type TINYINT NOT NULL,
    percent_threshold DECIMAL(5,2),
    score_min DECIMAL(10,2),
    score_max DECIMAL(10,2),
    sort_order INT DEFAULT 0,
    description TEXT,
    status TINYINT DEFAULT 1,
    created_by BIGINT,
    updated_by BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_rule_type (rule_type),
    INDEX idx_status (status),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. 量化公告表
CREATE TABLE IF NOT EXISTS quantification_announcements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    summary_config_id BIGINT NOT NULL,
    announcement_title VARCHAR(200) NOT NULL,
    announcement_content TEXT,
    cover_image VARCHAR(500),
    publish_scope TINYINT DEFAULT 1,
    scope_grade_ids JSON,
    scope_grade_names VARCHAR(500),
    is_published TINYINT DEFAULT 0,
    published_at DATETIME,
    view_count INT DEFAULT 0,
    publisher_id BIGINT,
    publisher_name VARCHAR(50),
    created_by BIGINT,
    updated_by BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_summary_config (summary_config_id),
    INDEX idx_published (is_published, published_at),
    INDEX idx_publish_scope (publish_scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. 班级-宿舍关联表
CREATE TABLE IF NOT EXISTS class_dormitory_bindings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    dormitory_id BIGINT NOT NULL,
    student_count INT DEFAULT 0,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_class_id (class_id),
    INDEX idx_dormitory_id (dormitory_id),
    UNIQUE KEY uk_class_dormitory (class_id, dormitory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. 归档规则配置表
CREATE TABLE IF NOT EXISTS archive_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_name VARCHAR(100) NOT NULL,
    table_name VARCHAR(50) NOT NULL,
    archive_type TINYINT NOT NULL,
    archive_days INT,
    archive_semesters INT,
    is_enabled TINYINT DEFAULT 1,
    last_executed_at DATETIME,
    created_by BIGINT,
    updated_by BIGINT,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_table_name (table_name),
    INDEX idx_is_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '所有表创建完成!' AS message;
