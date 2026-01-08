-- ============================================
-- 检查记录 V3 数据库设计
-- 创建日期: 2025-11-01
-- 说明: 支持120班级规模，完整统计功能，数据脱敏
-- ============================================

-- 1. 检查记录主表
DROP TABLE IF EXISTS check_records_v3;
CREATE TABLE check_records_v3 (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    daily_check_id BIGINT UNIQUE NOT NULL COMMENT '关联日常检查ID',
    record_code VARCHAR(50) UNIQUE NOT NULL COMMENT '记录编号 REC-20251101-001',

    -- 基本信息
    check_name VARCHAR(200) NOT NULL COMMENT '检查名称',
    check_date DATE NOT NULL COMMENT '检查日期',
    check_type TINYINT DEFAULT 1 COMMENT '检查类型(1日常2专项)',

    -- 检查人信息
    checker_id BIGINT NOT NULL COMMENT '检查人ID',
    checker_name VARCHAR(50) NOT NULL COMMENT '检查人姓名',

    -- 统计信息
    total_classes INT DEFAULT 0 COMMENT '涉及班级数',
    total_categories INT DEFAULT 0 COMMENT '检查类别数',
    total_items INT DEFAULT 0 COMMENT '扣分项总数',
    total_score DECIMAL(10,2) DEFAULT 0.00 COMMENT '总扣分',
    avg_score DECIMAL(10,2) DEFAULT 0.00 COMMENT '平均扣分',
    max_score DECIMAL(10,2) DEFAULT 0.00 COMMENT '最高扣分',
    min_score DECIMAL(10,2) DEFAULT 0.00 COMMENT '最低扣分',

    -- 状态信息
    status TINYINT DEFAULT 1 COMMENT '状态(1已发布2已归档)',
    publish_time DATETIME COMMENT '发布时间',
    remarks TEXT COMMENT '备注说明',

    -- 系统字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志',

    -- 索引
    INDEX idx_daily_check (daily_check_id),
    INDEX idx_date (check_date),
    INDEX idx_checker (checker_id),
    INDEX idx_status (status),
    INDEX idx_record_code (record_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录V3-主表';

-- 2. 班级统计表
DROP TABLE IF EXISTS check_record_class_stats;
CREATE TABLE check_record_class_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '检查记录ID',

    -- 班级信息
    class_id BIGINT NOT NULL COMMENT '班级ID',
    class_name VARCHAR(100) NOT NULL COMMENT '班级名称',
    grade_id BIGINT COMMENT '年级ID',
    grade_name VARCHAR(50) COMMENT '年级名称',

    -- 班主任信息
    teacher_id BIGINT COMMENT '班主任ID',
    teacher_name VARCHAR(50) COMMENT '班主任姓名',

    -- 扣分统计
    total_items INT DEFAULT 0 COMMENT '扣分项数量',
    total_score DECIMAL(10,2) DEFAULT 0.00 COMMENT '总扣分',

    -- 分类扣分统计
    hygiene_score DECIMAL(10,2) DEFAULT 0.00 COMMENT '卫生类扣分',
    discipline_score DECIMAL(10,2) DEFAULT 0.00 COMMENT '纪律类扣分',
    attendance_score DECIMAL(10,2) DEFAULT 0.00 COMMENT '考勤类扣分',
    other_score DECIMAL(10,2) DEFAULT 0.00 COMMENT '其他类扣分',

    -- 排名信息
    ranking INT COMMENT '全校排名(本次检查)',
    grade_ranking INT COMMENT '年级内排名',
    score_level VARCHAR(20) COMMENT '等级(优秀/良好/一般/较差)',

    -- 申诉统计
    appeal_count INT DEFAULT 0 COMMENT '申诉数量',
    appeal_passed INT DEFAULT 0 COMMENT '通过的申诉数',
    appeal_pending INT DEFAULT 0 COMMENT '待处理申诉数',

    -- 对比数据
    vs_avg_score DECIMAL(10,2) COMMENT '与平均分差值',
    vs_last_score DECIMAL(10,2) COMMENT '与上次差值',

    -- 系统字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    INDEX idx_record (record_id),
    INDEX idx_class (class_id),
    INDEX idx_teacher (teacher_id),
    INDEX idx_ranking (ranking),
    INDEX idx_grade_ranking (grade_id, grade_ranking),
    UNIQUE KEY uk_record_class (record_id, class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录V3-班级统计';

-- 3. 类别统计表
DROP TABLE IF EXISTS check_record_category_stats;
CREATE TABLE check_record_category_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '检查记录ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',

    -- 类别信息
    category_id BIGINT NOT NULL COMMENT '检查类别ID',
    category_name VARCHAR(100) NOT NULL COMMENT '类别名称',
    category_type VARCHAR(20) COMMENT '类别类型(hygiene/discipline/attendance/other)',

    -- 统计信息
    item_count INT DEFAULT 0 COMMENT '扣分项数量',
    total_score DECIMAL(10,2) DEFAULT 0.00 COMMENT '该类别总扣分',

    -- 系统字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引
    INDEX idx_record (record_id),
    INDEX idx_class (class_id),
    INDEX idx_category (category_id),
    UNIQUE KEY uk_record_class_category (record_id, class_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录V3-类别统计';

-- 4. 扣分明细表
DROP TABLE IF EXISTS check_record_items_v3;
CREATE TABLE check_record_items_v3 (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '检查记录ID',
    class_stat_id BIGINT NOT NULL COMMENT '班级统计ID',

    -- 扣分项信息
    category_id BIGINT NOT NULL COMMENT '检查类别ID',
    category_name VARCHAR(100) COMMENT '类别名称',
    item_id BIGINT NOT NULL COMMENT '扣分项ID',
    item_name VARCHAR(200) NOT NULL COMMENT '扣分项名称',

    -- 扣分信息
    deduct_mode TINYINT COMMENT '扣分模式(1固定2按人数3区间)',
    deduct_score DECIMAL(10,2) NOT NULL COMMENT '扣分',
    person_count INT COMMENT '涉及人数',

    -- 关联信息(宿舍/教室)
    link_type TINYINT DEFAULT 0 COMMENT '关联类型(0无1宿舍2教室)',
    link_id BIGINT COMMENT '关联对象ID',
    link_no VARCHAR(50) COMMENT '关联对象编号',

    -- 详细信息
    photo_urls TEXT COMMENT '照片URLs(JSON数组)',
    remark TEXT COMMENT '备注说明',

    -- 申诉状态
    appeal_status TINYINT DEFAULT 0 COMMENT '申诉状态(0未申诉1申诉中2已通过3已驳回)',
    appeal_id BIGINT COMMENT '申诉记录ID',

    -- 系统字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引
    INDEX idx_record (record_id),
    INDEX idx_class_stat (class_stat_id),
    INDEX idx_category (category_id),
    INDEX idx_appeal (appeal_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录V3-扣分明细';

-- 5. 数据可见性控制表(用于数据脱敏)
DROP TABLE IF EXISTS check_record_visibility;
CREATE TABLE check_record_visibility (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '检查记录ID',

    -- 可见性配置
    show_other_classes TINYINT DEFAULT 1 COMMENT '是否显示其他班级(1是0否)',
    show_class_names TINYINT DEFAULT 1 COMMENT '是否显示班级名称(1是0否,脱敏时显示班级A)',
    show_scores TINYINT DEFAULT 1 COMMENT '是否显示具体分数(1是0否)',
    show_details TINYINT DEFAULT 0 COMMENT '是否显示扣分明细(1是0否)',
    show_photos TINYINT DEFAULT 0 COMMENT '是否显示照片(1是0否)',

    -- 排名可见范围
    ranking_visible_range INT DEFAULT 10 COMMENT '排名可见范围(前后N名)',

    -- 系统字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    UNIQUE KEY uk_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录V3-可见性配置';

-- 6. 创建视图：班级历史统计
CREATE OR REPLACE VIEW v_class_history_stats AS
SELECT
    cs.class_id,
    cs.class_name,
    COUNT(DISTINCT cs.record_id) as check_count,
    AVG(cs.total_score) as avg_score,
    MIN(cs.total_score) as min_score,
    MAX(cs.total_score) as max_score,
    AVG(cs.ranking) as avg_ranking,
    SUM(cs.appeal_count) as total_appeals,
    SUM(cs.appeal_passed) as total_appeals_passed
FROM check_record_class_stats cs
JOIN check_records_v3 r ON cs.record_id = r.id
WHERE r.deleted = 0
GROUP BY cs.class_id, cs.class_name;

-- 7. 创建视图：年级统计
CREATE OR REPLACE VIEW v_grade_stats AS
SELECT
    cs.record_id,
    cs.grade_id,
    cs.grade_name,
    COUNT(cs.id) as class_count,
    SUM(cs.total_score) as total_score,
    AVG(cs.total_score) as avg_score,
    MIN(cs.total_score) as min_score,
    MAX(cs.total_score) as max_score
FROM check_record_class_stats cs
GROUP BY cs.record_id, cs.grade_id, cs.grade_name;

-- 插入默认的可见性配置(用于班主任查看)
-- 默认配置：可以看排名，但数据脱敏
INSERT INTO check_record_visibility (record_id, show_other_classes, show_class_names, show_scores, show_details, show_photos, ranking_visible_range)
VALUES (0, 1, 0, 0, 0, 0, 10)
ON DUPLICATE KEY UPDATE id=id;

COMMIT;
