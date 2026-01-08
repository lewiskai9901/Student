-- =====================================================
-- 量化管理系统重构 - 数据库迁移脚本 V2.0
-- 创建时间: 2025-01-19
-- 说明: 重构量化管理系统,支持模板、多年级、场所关联、申诉、汇总等功能
-- =====================================================

-- =====================================================
-- 一、基础配置表
-- =====================================================

-- 1. 学期表
CREATE TABLE IF NOT EXISTS semesters (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    semester_name VARCHAR(50) NOT NULL COMMENT '学期名称',
    semester_code VARCHAR(20) NOT NULL COMMENT '学期编码(如:2024-2025-2)',
    start_date DATE NOT NULL COMMENT '学期开始日期',
    end_date DATE NOT NULL COMMENT '学期结束日期',
    is_current TINYINT DEFAULT 0 COMMENT '是否当前学期:1-是 0-否',
    status TINYINT DEFAULT 1 COMMENT '状态:1-正常 0-已结束',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记:1-已删除 0-未删除',
    UNIQUE KEY uk_semester_code (semester_code),
    INDEX idx_is_current (is_current),
    INDEX idx_dates (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期表';

-- 2. 教学周表
CREATE TABLE IF NOT EXISTS academic_weeks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    semester_name VARCHAR(50) COMMENT '学期名称(冗余)',
    week_number INT NOT NULL COMMENT '周次(第几周)',
    week_name VARCHAR(50) COMMENT '周名称(如:第1周)',
    start_date DATE NOT NULL COMMENT '开始日期(周一)',
    end_date DATE NOT NULL COMMENT '结束日期(周日)',
    is_current TINYINT DEFAULT 0 COMMENT '是否当前周:1-是 0-否',
    status TINYINT DEFAULT 1 COMMENT '状态:1-正常 0-停用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_semester (semester_id),
    INDEX idx_dates (start_date, end_date),
    INDEX idx_is_current (is_current),
    UNIQUE KEY uk_semester_week (semester_id, week_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学周表';

-- =====================================================
-- 二、量化模板相关表
-- =====================================================

-- 3. 量化模板表
CREATE TABLE IF NOT EXISTS quantification_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_code VARCHAR(50) NOT NULL COMMENT '模板编码',
    description TEXT COMMENT '模板描述',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认模板:1-是 0-否',
    status TINYINT DEFAULT 1 COMMENT '状态:1-启用 0-禁用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记:1-已删除 0-未删除',
    UNIQUE KEY uk_template_code (template_code),
    INDEX idx_is_default (is_default),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='量化模板表';

-- 4. 量化模板-量化项关联表
CREATE TABLE IF NOT EXISTS template_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    category_id BIGINT NOT NULL COMMENT '量化项ID',
    location_type TINYINT DEFAULT 0 COMMENT '关联场所类型:0-不关联 1-教室 2-宿舍 3-两者都关联',
    auto_bind_location TINYINT DEFAULT 0 COMMENT '是否自动关联本班场所:1-是 0-否',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_template_id (template_id),
    INDEX idx_category_id (category_id),
    UNIQUE KEY uk_template_category (template_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板-量化项关联表';

-- =====================================================
-- 三、修改现有的量化项和扣分项表
-- =====================================================

-- 5. 修改量化检查类别表(增加场所关联配置)
-- 先检查列是否存在,如果不存在则添加
SET @exist_location_type := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'student_management' AND TABLE_NAME = 'quantification_check_categories' AND COLUMN_NAME = 'location_type');
SET @sql_location_type = IF(@exist_location_type = 0,
    'ALTER TABLE quantification_check_categories ADD COLUMN location_type TINYINT DEFAULT 0 COMMENT ''关联场所类型:0-不关联 1-教室 2-宿舍 3-两者都关联'' AFTER description',
    'SELECT ''Column location_type already exists'' AS message');
PREPARE stmt FROM @sql_location_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist_auto_bind := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'student_management' AND TABLE_NAME = 'quantification_check_categories' AND COLUMN_NAME = 'auto_bind_location');
SET @sql_auto_bind = IF(@exist_auto_bind = 0,
    'ALTER TABLE quantification_check_categories ADD COLUMN auto_bind_location TINYINT DEFAULT 0 COMMENT ''是否自动关联本班场所:1-是 0-否'' AFTER location_type',
    'SELECT ''Column auto_bind_location already exists'' AS message');
PREPARE stmt FROM @sql_auto_bind;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. 修改量化扣分项表(支持三种扣分模式)
-- 修改deduction_mode列注释
ALTER TABLE quantification_deduction_items
MODIFY COLUMN deduction_mode TINYINT COMMENT '扣分模式:1-固定扣分 2-按人次扣分 3-区间扣分';

-- 添加新列
SET @exist_base := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'student_management' AND TABLE_NAME = 'quantification_deduction_items' AND COLUMN_NAME = 'base_score');
SET @sql_base = IF(@exist_base = 0,
    'ALTER TABLE quantification_deduction_items ADD COLUMN base_score DECIMAL(10,2) DEFAULT 0 COMMENT ''基础分值(人次模式的单次扣分)'' AFTER deduction_score',
    'SELECT ''Column base_score already exists'' AS message');
PREPARE stmt FROM @sql_base;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist_min := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'student_management' AND TABLE_NAME = 'quantification_deduction_items' AND COLUMN_NAME = 'min_score');
SET @sql_min = IF(@exist_min = 0,
    'ALTER TABLE quantification_deduction_items ADD COLUMN min_score DECIMAL(10,2) DEFAULT 0 COMMENT ''最低扣分(区间模式)'' AFTER base_score',
    'SELECT ''Column min_score already exists'' AS message');
PREPARE stmt FROM @sql_min;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist_max := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'student_management' AND TABLE_NAME = 'quantification_deduction_items' AND COLUMN_NAME = 'max_score');
SET @sql_max = IF(@exist_max = 0,
    'ALTER TABLE quantification_deduction_items ADD COLUMN max_score DECIMAL(10,2) DEFAULT 0 COMMENT ''最高扣分(区间模式)'' AFTER min_score',
    'SELECT ''Column max_score already exists'' AS message');
PREPARE stmt FROM @sql_max;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist_step := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'student_management' AND TABLE_NAME = 'quantification_deduction_items' AND COLUMN_NAME = 'score_step');
SET @sql_step = IF(@exist_step = 0,
    'ALTER TABLE quantification_deduction_items ADD COLUMN score_step DECIMAL(10,2) DEFAULT 0.5 COMMENT ''扣分步进值(区间模式)'' AFTER max_score',
    'SELECT ''Column score_step already exists'' AS message');
PREPARE stmt FROM @sql_step;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 修改deduction_score列注释
ALTER TABLE quantification_deduction_items
MODIFY COLUMN deduction_score DECIMAL(10,2) COMMENT '标准扣分值(固定模式使用)';

-- 更新现有数据:将旧的deductionScore值填充到对应字段
UPDATE quantification_deduction_items
SET base_score = deduction_score
WHERE deduction_mode = 2 AND base_score = 0;

UPDATE quantification_deduction_items
SET min_score = 0, max_score = deduction_score
WHERE deduction_mode = 3 AND max_score = 0;

-- =====================================================
-- 四、量化任务相关表
-- =====================================================

-- 7. 量化任务表(重构)
CREATE TABLE IF NOT EXISTS quantification_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_code VARCHAR(50) NOT NULL COMMENT '任务编号(如:QT20250119-001)',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    template_id BIGINT COMMENT '使用的模板ID',
    template_name VARCHAR(100) COMMENT '模板名称(冗余)',
    check_date DATE NOT NULL COMMENT '检查日期',
    semester_id BIGINT COMMENT '学期ID',
    week_number INT COMMENT '周次',
    grade_ids JSON NOT NULL COMMENT '年级ID数组:[1,2,3]',
    grade_names VARCHAR(500) COMMENT '年级名称(冗余,逗号分隔)',
    checker_id BIGINT COMMENT '检查人ID',
    checker_name VARCHAR(50) COMMENT '检查人姓名(冗余)',
    status TINYINT DEFAULT 1 COMMENT '状态:1-进行中 2-已完成 3-已取消',
    remarks TEXT COMMENT '备注',
    archived TINYINT DEFAULT 0 COMMENT '是否已归档:1-是 0-否',
    archived_at DATETIME COMMENT '归档时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记:1-已删除 0-未删除',
    UNIQUE KEY uk_task_code (task_code),
    INDEX idx_check_date (check_date),
    INDEX idx_status (status),
    INDEX idx_archived (archived),
    INDEX idx_semester_week (semester_id, week_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='量化任务表';

-- 8. 量化任务-量化项配置表
CREATE TABLE IF NOT EXISTS task_category_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    category_id BIGINT NOT NULL COMMENT '量化项ID',
    category_name VARCHAR(50) COMMENT '量化项名称(冗余)',
    location_type TINYINT DEFAULT 0 COMMENT '关联场所类型:0-不关联 1-教室 2-宿舍 3-两者都关联',
    auto_bind_location TINYINT DEFAULT 0 COMMENT '是否自动关联本班场所:1-是 0-否',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_id (task_id),
    INDEX idx_category_id (category_id),
    UNIQUE KEY uk_task_category (task_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务-量化项配置表';

-- 9. 检查场所关联表
CREATE TABLE IF NOT EXISTS quantification_location_bindings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '量化任务ID',
    category_id BIGINT NOT NULL COMMENT '量化项ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    class_name VARCHAR(100) COMMENT '班级名称(冗余)',
    location_type TINYINT NOT NULL COMMENT '场所类型:1-教室 2-宿舍',
    location_id BIGINT NOT NULL COMMENT '场所ID(教室ID或宿舍ID)',
    location_name VARCHAR(100) COMMENT '场所名称(冗余,如:101教室,301宿舍)',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_category (task_id, category_id),
    INDEX idx_class_id (class_id),
    INDEX idx_location (location_type, location_id),
    UNIQUE KEY uk_task_category_location (task_id, category_id, location_type, location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查场所关联表';

-- =====================================================
-- 五、检查记录相关表
-- =====================================================

-- 10. 量化检查记录表(重构)
CREATE TABLE IF NOT EXISTS quantification_check_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    class_name VARCHAR(100) COMMENT '班级名称(冗余)',
    category_id BIGINT NOT NULL COMMENT '量化项ID',
    category_name VARCHAR(50) COMMENT '量化项名称(冗余)',
    deduction_item_id BIGINT COMMENT '扣分项ID',
    deduction_item_name VARCHAR(100) COMMENT '扣分项名称(冗余)',
    deduction_mode TINYINT COMMENT '扣分模式(冗余):1-固定 2-人次 3-区间',
    location_type TINYINT COMMENT '检查场所类型:1-教室 2-宿舍',
    location_id BIGINT COMMENT '场所ID',
    location_name VARCHAR(100) COMMENT '场所名称(如:101教室,301宿舍)',
    deduction_score DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '实际扣分',
    person_count INT DEFAULT 0 COMMENT '人次数(人次模式使用)',
    base_score DECIMAL(10,2) DEFAULT 0 COMMENT '基础分值(人次模式)',
    original_score DECIMAL(10,2) COMMENT '原始扣分(申诉修改前)',
    remarks TEXT COMMENT '备注',
    photos JSON COMMENT '照片URL数组',
    checker_id BIGINT COMMENT '检查人ID',
    checker_name VARCHAR(50) COMMENT '检查人姓名(冗余)',
    check_time DATETIME NOT NULL COMMENT '检查时间',
    appeal_status TINYINT DEFAULT 0 COMMENT '申诉状态:0-未申诉 1-申诉中 2-申诉通过 3-申诉驳回',
    archived TINYINT DEFAULT 0 COMMENT '是否已归档:1-是 0-否',
    archived_at DATETIME COMMENT '归档时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记:1-已删除 0-未删除',
    INDEX idx_task_id (task_id),
    INDEX idx_class_id (class_id),
    INDEX idx_category_id (category_id),
    INDEX idx_location (location_type, location_id),
    INDEX idx_check_time (check_time),
    INDEX idx_appeal_status (appeal_status),
    INDEX idx_archived (archived)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='量化检查记录表';

-- =====================================================
-- 六、申诉相关表
-- =====================================================

-- 11. 修改量化申诉表
ALTER TABLE quantification_appeals
ADD COLUMN IF NOT EXISTS task_id BIGINT COMMENT '任务ID' AFTER id,
ADD COLUMN IF NOT EXISTS record_ids JSON NOT NULL COMMENT '申诉的记录ID数组(支持批量申诉)' AFTER record_id,
ADD COLUMN IF NOT EXISTS appeal_type TINYINT DEFAULT 1 COMMENT '申诉类型:1-扣分错误 2-场所错误 3-其他' AFTER record_ids,
ADD COLUMN IF NOT EXISTS class_id BIGINT COMMENT '班级ID' AFTER appeal_type,
ADD COLUMN IF NOT EXISTS class_name VARCHAR(100) COMMENT '班级名称(冗余)' AFTER class_id,
ADD COLUMN IF NOT EXISTS original_score DECIMAL(10,2) COMMENT '原扣分' AFTER appeal_evidence,
ADD COLUMN IF NOT EXISTS adjusted_score DECIMAL(10,2) COMMENT '调整后扣分' AFTER original_score,
MODIFY COLUMN status TINYINT DEFAULT 1 COMMENT '状态:1-待审核 2-审核通过 3-审核驳回 4-已撤销',
ADD INDEX idx_task_id (task_id),
ADD INDEX idx_class_id (class_id),
ADD INDEX idx_appeal_type (appeal_type);

-- =====================================================
-- 七、汇总统计相关表
-- =====================================================

-- 12. 量化汇总配置表
CREATE TABLE IF NOT EXISTS quantification_summary_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    summary_type TINYINT NOT NULL COMMENT '汇总类型:1-按天 2-按周 3-按月 4-自定义',
    semester_id BIGINT COMMENT '学期ID',
    semester_name VARCHAR(50) COMMENT '学期名称(冗余)',
    week_number INT COMMENT '周次(按周汇总时使用)',
    start_date DATE COMMENT '开始日期(按天/自定义)',
    end_date DATE COMMENT '结束日期(按天/自定义)',
    year INT COMMENT '年份(按月汇总)',
    month INT COMMENT '月份(按月汇总)',
    grade_ids JSON COMMENT '汇总年级范围(空表示全部年级)',
    grade_names VARCHAR(500) COMMENT '年级名称(冗余)',
    rating_rule_type TINYINT DEFAULT 1 COMMENT '评级规则类型:1-按比例 2-按分数',
    auto_publish TINYINT DEFAULT 0 COMMENT '自动公布:1-是 0-否',
    status TINYINT DEFAULT 1 COMMENT '状态:1-草稿 2-已计算 3-已公布',
    calculated_at DATETIME COMMENT '计算时间',
    published_at DATETIME COMMENT '公布时间',
    archived TINYINT DEFAULT 0 COMMENT '是否已归档:1-是 0-否',
    archived_at DATETIME COMMENT '归档时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记:1-已删除 0-未删除',
    INDEX idx_summary_type (summary_type),
    INDEX idx_semester_week (semester_id, week_number),
    INDEX idx_dates (start_date, end_date),
    INDEX idx_status (status),
    INDEX idx_archived (archived)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='量化汇总配置表';

-- 13. 量化汇总结果表
CREATE TABLE IF NOT EXISTS quantification_summary_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_id BIGINT NOT NULL COMMENT '汇总配置ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    class_name VARCHAR(100) COMMENT '班级名称(冗余)',
    grade_id BIGINT COMMENT '年级ID',
    grade_name VARCHAR(50) COMMENT '年级名称(冗余)',
    total_deduction DECIMAL(10,2) DEFAULT 0 COMMENT '总扣分',
    check_count INT DEFAULT 0 COMMENT '检查次数',
    average_deduction DECIMAL(10,2) DEFAULT 0 COMMENT '平均扣分',
    rank_in_grade INT COMMENT '年级排名',
    rank_in_school INT COMMENT '全校排名',
    rank_percent DECIMAL(5,2) COMMENT '排名百分比',
    rating_level_id BIGINT COMMENT '评级等级ID',
    rating_level_name VARCHAR(50) COMMENT '评级等级名称(冗余)',
    category_details JSON COMMENT '各量化项明细统计',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_id (config_id),
    INDEX idx_class_id (class_id),
    INDEX idx_grade_id (grade_id),
    INDEX idx_rank_in_grade (rank_in_grade),
    INDEX idx_rating_level (rating_level_id),
    UNIQUE KEY uk_config_class (config_id, class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='量化汇总结果表';

-- =====================================================
-- 八、评级相关表
-- =====================================================

-- 14. 量化评级等级表
CREATE TABLE IF NOT EXISTS quantification_rating_levels (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    level_name VARCHAR(50) NOT NULL COMMENT '等级名称(如:优秀/良好/合格/待改进)',
    level_code VARCHAR(20) NOT NULL COMMENT '等级编码(如:EXCELLENT/GOOD/PASS/POOR)',
    level_color VARCHAR(20) COMMENT '等级颜色(如:#67C23A)',
    level_icon VARCHAR(50) COMMENT '等级图标',
    rule_type TINYINT NOT NULL COMMENT '规则类型:1-按比例 2-按分数',
    percent_threshold DECIMAL(5,2) COMMENT '百分比阈值(按比例模式,如前10%则填10.00)',
    score_min DECIMAL(10,2) COMMENT '最低分数(按分数模式,含)',
    score_max DECIMAL(10,2) COMMENT '最高分数(按分数模式,含)',
    sort_order INT DEFAULT 0 COMMENT '排序(数字越小等级越高)',
    description TEXT COMMENT '等级描述',
    status TINYINT DEFAULT 1 COMMENT '状态:1-启用 0-禁用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记:1-已删除 0-未删除',
    INDEX idx_rule_type (rule_type),
    INDEX idx_status (status),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='量化评级等级表';

-- =====================================================
-- 九、公告相关表
-- =====================================================

-- 15. 量化公告表
CREATE TABLE IF NOT EXISTS quantification_announcements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    summary_config_id BIGINT NOT NULL COMMENT '汇总配置ID',
    announcement_title VARCHAR(200) NOT NULL COMMENT '公告标题',
    announcement_content TEXT COMMENT '公告内容(富文本HTML)',
    cover_image VARCHAR(500) COMMENT '封面图片URL',
    publish_scope TINYINT DEFAULT 1 COMMENT '公布范围:1-全校 2-指定年级',
    scope_grade_ids JSON COMMENT '指定年级ID数组',
    scope_grade_names VARCHAR(500) COMMENT '指定年级名称(冗余)',
    is_published TINYINT DEFAULT 0 COMMENT '是否已发布:1-是 0-否',
    published_at DATETIME COMMENT '发布时间',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    publisher_id BIGINT COMMENT '发布人ID',
    publisher_name VARCHAR(50) COMMENT '发布人姓名(冗余)',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记:1-已删除 0-未删除',
    INDEX idx_summary_config (summary_config_id),
    INDEX idx_published (is_published, published_at),
    INDEX idx_publish_scope (publish_scope)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='量化公告表';

-- =====================================================
-- 十、班级-场所关联表
-- =====================================================

-- 16. 修改班级表(增加默认教室字段)
ALTER TABLE classes
ADD COLUMN IF NOT EXISTS default_classroom_id BIGINT COMMENT '默认教室ID' AFTER grade_id,
ADD INDEX idx_default_classroom (default_classroom_id);

-- 17. 班级-宿舍关联表
CREATE TABLE IF NOT EXISTS class_dormitory_bindings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    dormitory_id BIGINT NOT NULL COMMENT '宿舍ID',
    student_count INT DEFAULT 0 COMMENT '该宿舍该班学生数',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_class_id (class_id),
    INDEX idx_dormitory_id (dormitory_id),
    UNIQUE KEY uk_class_dormitory (class_id, dormitory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级-宿舍关联表';

-- =====================================================
-- 十一、数据归档相关表
-- =====================================================

-- 18. 归档规则配置表
CREATE TABLE IF NOT EXISTS archive_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    table_name VARCHAR(50) NOT NULL COMMENT '表名',
    archive_type TINYINT NOT NULL COMMENT '归档类型:1-按时间 2-按学期',
    archive_days INT COMMENT '归档天数(如180天前的数据)',
    archive_semesters INT COMMENT '归档学期数(如2个学期前的数据)',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用:1-是 0-否',
    last_executed_at DATETIME COMMENT '上次执行时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记:1-已删除 0-未删除',
    INDEX idx_table_name (table_name),
    INDEX idx_is_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档规则配置表';

-- =====================================================
-- 十二、插入初始数据
-- =====================================================

-- 插入默认评级等级(按比例)
INSERT INTO quantification_rating_levels (level_name, level_code, level_color, rule_type, percent_threshold, sort_order, description, status) VALUES
('优秀', 'EXCELLENT', '#67C23A', 1, 10.00, 1, '前10%的班级', 1),
('良好', 'GOOD', '#409EFF', 1, 30.00, 2, '前11%-30%的班级', 1),
('合格', 'PASS', '#E6A23C', 1, 70.00, 3, '前31%-70%的班级', 1),
('待改进', 'POOR', '#F56C6C', 1, 100.00, 4, '后30%的班级', 1);

-- 插入默认评级等级(按分数)
INSERT INTO quantification_rating_levels (level_name, level_code, level_color, rule_type, score_min, score_max, sort_order, description, status) VALUES
('优秀', 'EXCELLENT', '#67C23A', 2, 0, 5, 1, '总扣分0-5分', 1),
('良好', 'GOOD', '#409EFF', 2, 5.01, 10, 2, '总扣分5.01-10分', 1),
('合格', 'PASS', '#E6A23C', 2, 10.01, 20, 3, '总扣分10.01-20分', 1),
('待改进', 'POOR', '#F56C6C', 2, 20.01, 999, 4, '总扣分20分以上', 1);

-- 插入默认归档规则
INSERT INTO archive_rules (rule_name, table_name, archive_type, archive_days, is_enabled) VALUES
('量化任务归档', 'quantification_tasks', 1, 180, 1),
('检查记录归档', 'quantification_check_records', 1, 180, 1),
('汇总配置归档', 'quantification_summary_configs', 1, 365, 1);

-- 插入默认量化模板
INSERT INTO quantification_templates (template_name, template_code, description, is_default, status, created_time) VALUES
('常规检查模板', 'REGULAR_CHECK', '包含纪律、卫生、宿舍等常规检查项', 1, 1, NOW());

-- =====================================================
-- 十三、数据迁移(将旧数据迁移到新表)
-- =====================================================

-- 注意: 如果有旧的quantification_records数据,需要迁移到新的quantification_check_records表
-- 这里假设旧表结构与新表兼容,实际迁移时需要根据具体情况调整

-- 示例迁移SQL(根据实际情况调整):
-- INSERT INTO quantification_check_records
-- (task_id, class_id, category_id, deduction_item_id, deduction_score, check_time, created_time)
-- SELECT id, class_id, type_id, item_id, score, check_time, created_at
-- FROM quantification_records
-- WHERE deleted = 0;

-- =====================================================
-- 脚本执行完毕
-- =====================================================

SELECT '量化管理系统数据库重构完成!' AS message;
