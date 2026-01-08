-- ════════════════════════════════════════════════════════════════════════════
-- 量化模块 V3.0 完整数据库架构
-- 创建时间: 2025-11-23
-- 版本: v3.0 Final
-- 说明: 包含15张新表 + 核心存储过程
-- ════════════════════════════════════════════════════════════════════════════

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ════════════════════════════════════════════════════════════════════════════
-- 第一部分: 字典表 (2张)
-- ════════════════════════════════════════════════════════════════════════════

-- 1. 检查类别字典表
DROP TABLE IF EXISTS check_categories;
CREATE TABLE check_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_code VARCHAR(50) UNIQUE NOT NULL COMMENT '类别编码: HYGIENE, DISCIPLINE',
    category_name VARCHAR(100) NOT NULL COMMENT '类别名称: 卫生检查, 纪律检查',
    category_type VARCHAR(20) COMMENT '类别类型: HYGIENE, DISCIPLINE, OTHER',
    default_max_score DECIMAL(10,2) COMMENT '默认满分: 100.00',
    description TEXT COMMENT '描述',
    icon VARCHAR(50) COMMENT '图标: 🏠, 📚',

    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 1=启用, 0=禁用',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    deleted TINYINT DEFAULT 0,

    INDEX idx_code (category_code),
    INDEX idx_type (category_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='检查类别字典表';


-- 2. 检查项字典表
DROP TABLE IF EXISTS check_items;
CREATE TABLE check_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL COMMENT 'FK → check_categories.id',
    item_code VARCHAR(50) UNIQUE NOT NULL COMMENT '检查项编码: FLOOR_CLEAN',
    item_name VARCHAR(200) NOT NULL COMMENT '检查项名称: 地面整洁',
    item_description TEXT COMMENT '检查说明',

    -- 扣分规则
    deduct_mode TINYINT DEFAULT 1 COMMENT '扣分模式: 1=按次, 2=按人',
    default_deduct_score DECIMAL(10,2) COMMENT '默认扣分: -5.00',
    min_deduct_score DECIMAL(10,2) COMMENT '最小扣分: -10.00',
    max_deduct_score DECIMAL(10,2) COMMENT '最大扣分: -1.00',

    -- 检查要点
    check_points TEXT COMMENT 'JSON: ["地面无垃圾", "无污渍"]',

    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    deleted TINYINT DEFAULT 0,

    INDEX idx_category (category_id),
    INDEX idx_code (item_code),
    CONSTRAINT fk_items_category
        FOREIGN KEY (category_id) REFERENCES check_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='检查项字典表';


-- ════════════════════════════════════════════════════════════════════════════
-- 第二部分: 模板表 (3张)
-- ════════════════════════════════════════════════════════════════════════════

-- 3. 检查模板表
DROP TABLE IF EXISTS check_templates;
CREATE TABLE check_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称: 综合检查模板',
    template_code VARCHAR(50) UNIQUE NOT NULL COMMENT '模板编码: COMPREHENSIVE_CHECK',
    template_type VARCHAR(20) COMMENT '模板类型: COMPREHENSIVE, SPECIAL',
    description TEXT,

    -- 默认配置
    default_weight_config_id BIGINT COMMENT '默认加权方案',
    default_appeal_config_id BIGINT COMMENT '默认申诉配置',

    -- 统计
    use_count INT DEFAULT 0 COMMENT '使用次数',
    last_used_at DATETIME COMMENT '最后使用时间',

    is_default TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    deleted TINYINT DEFAULT 0,

    INDEX idx_code (template_code),
    INDEX idx_type (template_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='检查模板表';


-- 4. 模板-类别关联表
DROP TABLE IF EXISTS template_categories;
CREATE TABLE template_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL COMMENT 'FK → check_templates.id',
    category_id BIGINT NOT NULL COMMENT 'FK → check_categories.id',

    -- 覆盖默认配置
    max_score DECIMAL(10,2) COMMENT '该模板中此类别的满分',
    weight DECIMAL(5,2) DEFAULT 1.0 COMMENT '类别权重',

    sort_order INT DEFAULT 0,
    is_required TINYINT DEFAULT 1 COMMENT '是否必填',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    UNIQUE INDEX uk_template_category (template_id, category_id),
    INDEX idx_template (template_id),
    INDEX idx_category (category_id),
    CONSTRAINT fk_tc_template
        FOREIGN KEY (template_id) REFERENCES check_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_tc_category
        FOREIGN KEY (category_id) REFERENCES check_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='模板-类别关联表';


-- 5. 模板-检查项关联表
DROP TABLE IF EXISTS template_items;
CREATE TABLE template_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL COMMENT 'FK → check_templates.id',
    category_id BIGINT NOT NULL COMMENT 'FK → check_categories.id',
    item_id BIGINT NOT NULL COMMENT 'FK → check_items.id',

    -- 覆盖默认配置
    deduct_score DECIMAL(10,2) COMMENT '该模板中此项的扣分',

    sort_order INT DEFAULT 0,
    is_required TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    UNIQUE INDEX uk_template_item (template_id, item_id),
    INDEX idx_template (template_id),
    INDEX idx_category (category_id),
    INDEX idx_item (item_id),
    CONSTRAINT fk_ti_template
        FOREIGN KEY (template_id) REFERENCES check_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_ti_category
        FOREIGN KEY (category_id) REFERENCES check_categories(id),
    CONSTRAINT fk_ti_item
        FOREIGN KEY (item_id) REFERENCES check_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='模板-检查项关联表';


-- ════════════════════════════════════════════════════════════════════════════
-- 第三部分: 评级模板表 (3张) - 关键: 绑定检查模板
-- ════════════════════════════════════════════════════════════════════════════

-- 6. 评级模板表
DROP TABLE IF EXISTS rating_templates;
CREATE TABLE rating_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_name VARCHAR(100) NOT NULL COMMENT '评级模板名称: 综合检查评级模板',
    template_code VARCHAR(50) UNIQUE NOT NULL COMMENT '评级模板编码: RATING_COMPREHENSIVE',
    description TEXT,

    -- 绑定检查模板 (关键!)
    check_template_id BIGINT NOT NULL COMMENT 'FK → check_templates.id',

    is_default TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    deleted TINYINT DEFAULT 0,

    INDEX idx_check_template (check_template_id),
    INDEX idx_code (template_code),
    CONSTRAINT fk_rating_check_template
        FOREIGN KEY (check_template_id) REFERENCES check_templates(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='评级模板表';


-- 7. 评级规则表
DROP TABLE IF EXISTS rating_rules;
CREATE TABLE rating_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rating_template_id BIGINT NOT NULL COMMENT 'FK → rating_templates.id',

    -- 评级信息
    rating_name VARCHAR(100) NOT NULL COMMENT '评级名称: 卫生优秀班级',
    rating_code VARCHAR(50) NOT NULL COMMENT '评级编码: HYGIENE_EXCELLENT',
    rating_description TEXT,

    -- 评级依据
    rating_basis VARCHAR(50) NOT NULL COMMENT '评级依据: SINGLE_CATEGORY, MULTI_CATEGORY, TOTAL',
    category_ids TEXT COMMENT '参与评级的类别ID: "1,2,3"',

    -- 计算方式
    score_type VARCHAR(20) NOT NULL COMMENT '分数类型: DEDUCTION(原始扣分), WEIGHTED(加权后)',

    -- 评级条件类型
    condition_type VARCHAR(20) NOT NULL COMMENT '条件类型: PERCENTAGE, ABSOLUTE, HYBRID',

    -- 显示
    icon VARCHAR(50) COMMENT '图标: 🏆, ⭐',
    color VARCHAR(20) COMMENT '颜色: #67C23A',

    -- 奖励
    reward_points INT DEFAULT 0,
    reward_description TEXT,

    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    INDEX idx_rating_template (rating_template_id),
    INDEX idx_code (rating_code),
    CONSTRAINT fk_rule_rating_template
        FOREIGN KEY (rating_template_id) REFERENCES rating_templates(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='评级规则表';


-- 8. 评级等级表
DROP TABLE IF EXISTS rating_levels;
CREATE TABLE rating_levels (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rating_rule_id BIGINT NOT NULL COMMENT 'FK → rating_rules.id',

    -- 等级信息
    level_name VARCHAR(50) NOT NULL COMMENT '等级名称: 优秀, 良好, 一般, 需改进',
    level_code VARCHAR(20) NOT NULL COMMENT '等级编码: EXCELLENT, GOOD, FAIR, POOR',
    level_order INT NOT NULL COMMENT '等级顺序: 1, 2, 3, 4',

    -- 百分比条件
    min_percent DECIMAL(5,2) COMMENT '最小百分比: 0.00 (前0%)',
    max_percent DECIMAL(5,2) COMMENT '最大百分比: 20.00 (前20%)',

    -- 绝对分数条件 (扣分范围,负数)
    min_score DECIMAL(10,2) COMMENT '最小分数: -10.00 (扣分10分以内)',
    max_score DECIMAL(10,2) COMMENT '最大分数: 0.00 (满分)',

    -- 显示
    level_color VARCHAR(20) COMMENT '颜色: #67C23A',
    level_icon VARCHAR(50) COMMENT '图标: ⭐⭐⭐⭐⭐',

    -- 奖励
    reward_points INT DEFAULT 0,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    INDEX idx_rating_rule (rating_rule_id),
    CONSTRAINT fk_level_rating_rule
        FOREIGN KEY (rating_rule_id) REFERENCES rating_rules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='评级等级表';


-- ════════════════════════════════════════════════════════════════════════════
-- 第四部分: 检查记录表 (1张) - 合并daily_checks和check_records_v3
-- ════════════════════════════════════════════════════════════════════════════

-- 9. 检查记录表 (统一版)
DROP TABLE IF EXISTS check_records;
CREATE TABLE check_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_code VARCHAR(50) UNIQUE NOT NULL COMMENT '记录编号: CR20251123001',

    -- 基本信息
    check_name VARCHAR(200) NOT NULL COMMENT '检查名称: 2025-11-23宿舍卫生检查',
    check_date DATE NOT NULL COMMENT '检查日期',
    check_type TINYINT DEFAULT 1 COMMENT '检查类型: 1=宿舍, 2=教室, 3=纪律',

    -- 关联模板
    template_id BIGINT COMMENT 'FK → check_templates.id',
    rating_template_id BIGINT COMMENT 'FK → rating_templates.id',

    -- 检查人
    checker_id BIGINT NOT NULL COMMENT 'FK → users.id',
    checker_name VARCHAR(100),

    -- 状态机 (关键!)
    status TINYINT DEFAULT 0 COMMENT '状态: 0=草稿, 1=已提交, 2=已审核, 3=已发布, 4=已归档',

    -- 时间戳
    drafted_at DATETIME COMMENT '创建草稿时间',
    submitted_at DATETIME COMMENT '提交时间',
    reviewed_at DATETIME COMMENT '审核时间',
    published_at DATETIME COMMENT '发布时间',
    archived_at DATETIME COMMENT '归档时间',

    -- 审核/发布人
    reviewer_id BIGINT,
    reviewer_name VARCHAR(50),
    publisher_id BIGINT,
    publisher_name VARCHAR(50),

    -- 加权和申诉配置
    default_weight_config_id BIGINT COMMENT '默认加权方案',
    appeal_config_id BIGINT COMMENT '申诉配置',

    -- 统计数据 (冗余,便于快速查询)
    total_classes INT DEFAULT 0 COMMENT '检查班级数',
    total_categories INT DEFAULT 0 COMMENT '检查类别数',
    total_items INT DEFAULT 0 COMMENT '扣分条数',

    -- 分数统计
    total_deduct DECIMAL(10,2) DEFAULT 0.00 COMMENT '总扣分',
    avg_deduct DECIMAL(10,2) DEFAULT 0.00 COMMENT '平均扣分',
    max_deduct DECIMAL(10,2) DEFAULT 0.00 COMMENT '最大扣分 (最差班级)',
    min_deduct DECIMAL(10,2) DEFAULT 0.00 COMMENT '最小扣分 (最好班级)',

    -- 快照 (重要!)
    template_snapshot JSON COMMENT '模板快照',
    weight_config_snapshot JSON COMMENT '加权配置快照',
    appeal_config_snapshot JSON COMMENT '申诉配置快照',

    -- 备注
    remark TEXT,
    description TEXT,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    deleted TINYINT DEFAULT 0,

    INDEX idx_date (check_date),
    INDEX idx_status (status),
    INDEX idx_template (template_id),
    INDEX idx_checker (checker_id),
    INDEX idx_published_at (published_at),
    CONSTRAINT fk_records_template
        FOREIGN KEY (template_id) REFERENCES check_templates(id),
    CONSTRAINT fk_records_rating_template
        FOREIGN KEY (rating_template_id) REFERENCES rating_templates(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='检查记录表(合并daily_checks和check_records_v3)';


-- ════════════════════════════════════════════════════════════════════════════
-- 第五部分: 加权配置表 (1张) - 支持继承+覆盖
-- ════════════════════════════════════════════════════════════════════════════

-- 10. 检查记录加权配置表
DROP TABLE IF EXISTS check_record_weight_configs;
CREATE TABLE check_record_weight_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL COMMENT 'FK → check_records.id',

    -- 配置层级
    config_level VARCHAR(20) NOT NULL COMMENT '配置层级: RECORD, CATEGORY, ITEM',
    parent_id BIGINT COMMENT '父级配置ID',

    -- 关联对象
    category_id BIGINT COMMENT '类别ID',
    item_id BIGINT COMMENT '检查项ID',
    target_name VARCHAR(200) COMMENT '名称(快照)',

    -- 加权配置
    weight_config_id BIGINT COMMENT 'FK → class_weight_configs.id',
    is_inherited TINYINT DEFAULT 1 COMMENT '是否继承: 1=继承, 0=覆盖',
    weight_enabled TINYINT DEFAULT 1 COMMENT '是否启用: 1=启用, 0=禁用',

    -- 快照
    weight_config_snapshot JSON,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    INDEX idx_record (record_id),
    INDEX idx_level (record_id, config_level),
    INDEX idx_category (record_id, category_id),
    INDEX idx_item (record_id, item_id),
    CONSTRAINT fk_rwc_record
        FOREIGN KEY (record_id) REFERENCES check_records(id) ON DELETE CASCADE,
    CONSTRAINT fk_rwc_weight_config
        FOREIGN KEY (weight_config_id) REFERENCES class_weight_configs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='检查记录加权配置表(支持继承+覆盖)';


-- ════════════════════════════════════════════════════════════════════════════
-- 第六部分: 班级统计表 (1张) - 精简版
-- ════════════════════════════════════════════════════════════════════════════

-- 11. 班级统计表
DROP TABLE IF EXISTS check_record_class_stats;
CREATE TABLE check_record_class_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL COMMENT 'FK → check_records.id',
    class_id BIGINT NOT NULL COMMENT 'FK → classes.id',

    -- 快照字段 (记录检查时的状态)
    class_name VARCHAR(100) NOT NULL COMMENT '班级名称快照',
    class_size INT COMMENT '班级人数快照',
    teacher_id BIGINT,
    teacher_name VARCHAR(50) COMMENT '班主任快照',

    -- 原始扣分
    original_deduct DECIMAL(10,2) DEFAULT 0.00 COMMENT '原始总扣分',

    -- 加权调整
    weight_config_id BIGINT COMMENT '使用的加权方案',
    weight_factor DECIMAL(5,2) DEFAULT 1.0 COMMENT '权重系数',
    weighted_deduct DECIMAL(10,2) DEFAULT 0.00 COMMENT '加权后扣分',

    -- 最终分数
    final_score DECIMAL(10,2) DEFAULT 0.00 COMMENT '最终得分 = 100 + weighted_deduct',

    -- 排名
    ranking INT COMMENT '全校排名',
    grade_ranking INT COMMENT '年级排名',

    -- 申诉统计
    appeal_count INT DEFAULT 0,
    appeal_passed INT DEFAULT 0,
    appeal_pending INT DEFAULT 0,

    -- 对比数据
    vs_avg_score DECIMAL(10,2) COMMENT '与平均分差值',
    vs_last_score DECIMAL(10,2) COMMENT '与上次差值',

    -- 版本控制 (用于申诉后重算)
    stat_version INT DEFAULT 1,
    is_latest TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE INDEX uk_record_class_latest (record_id, class_id, is_latest),
    INDEX idx_record (record_id),
    INDEX idx_class (class_id),
    INDEX idx_ranking (record_id, ranking),
    CONSTRAINT fk_stats_record
        FOREIGN KEY (record_id) REFERENCES check_records(id) ON DELETE CASCADE,
    CONSTRAINT fk_stats_class
        FOREIGN KEY (class_id) REFERENCES classes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='班级统计表(精简版)';


-- ════════════════════════════════════════════════════════════════════════════
-- 第七部分: 扣分明细表 (1张)
-- ════════════════════════════════════════════════════════════════════════════

-- 12. 扣分明细表
DROP TABLE IF EXISTS check_record_items;
CREATE TABLE check_record_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL COMMENT 'FK → check_records.id',
    class_stat_id BIGINT NOT NULL COMMENT 'FK → check_record_class_stats.id',

    -- 类别和检查项 (使用ID+快照)
    category_id BIGINT NOT NULL COMMENT 'FK → check_categories.id',
    category_name VARCHAR(100) COMMENT '类别名称快照',
    item_id BIGINT COMMENT 'FK → check_items.id (可为NULL,临时项)',
    item_name VARCHAR(200) NOT NULL COMMENT '检查项名称',

    -- 扣分信息
    deduct_mode TINYINT DEFAULT 1 COMMENT '扣分模式: 1=按次, 2=按人',
    deduct_score DECIMAL(10,2) NOT NULL COMMENT '扣分: -5.00',
    person_count INT COMMENT '涉及人数(按人扣分时)',

    -- 关联对象
    link_type TINYINT DEFAULT 0 COMMENT '关联类型: 0=无, 1=宿舍, 2=学生, 3=教室',
    link_id BIGINT COMMENT '关联ID',
    link_no VARCHAR(50) COMMENT '关联编号: 201宿舍',

    -- 证据
    photo_urls TEXT COMMENT '照片URLs (JSON数组)',
    video_urls TEXT COMMENT '视频URLs',
    location_detail VARCHAR(200) COMMENT '具体位置',

    -- 描述
    issue_description TEXT COMMENT '问题详细描述',
    remark TEXT COMMENT '备注',

    -- 申诉信息
    appeal_status TINYINT DEFAULT 0 COMMENT '申诉状态: 0=未申诉, 1=待处理, 2=已通过, 3=已驳回',
    appeal_id BIGINT COMMENT 'FK → check_item_appeals.id',

    -- 是否临时扣分项
    is_temporary TINYINT DEFAULT 0 COMMENT '是否临时项: 0=模板项, 1=临时项',
    temp_approved TINYINT COMMENT '临时项审批状态',
    temp_approver_id BIGINT,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,

    INDEX idx_record (record_id),
    INDEX idx_class_stat (class_stat_id),
    INDEX idx_category (category_id),
    INDEX idx_item (item_id),
    INDEX idx_appeal (appeal_status),
    CONSTRAINT fk_record_items_record
        FOREIGN KEY (record_id) REFERENCES check_records(id) ON DELETE CASCADE,
    CONSTRAINT fk_record_items_class_stat
        FOREIGN KEY (class_stat_id) REFERENCES check_record_class_stats(id) ON DELETE CASCADE,
    CONSTRAINT fk_record_items_category
        FOREIGN KEY (category_id) REFERENCES check_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='扣分明细表';


-- ════════════════════════════════════════════════════════════════════════════
-- 第八部分: 评级结果表 (1张)
-- ════════════════════════════════════════════════════════════════════════════

-- 13. 评级结果表
DROP TABLE IF EXISTS check_record_rating_results;
CREATE TABLE check_record_rating_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL COMMENT 'FK → check_records.id',
    class_stat_id BIGINT NOT NULL COMMENT 'FK → check_record_class_stats.id',
    class_id BIGINT NOT NULL,
    class_name VARCHAR(100),

    -- 评级信息
    rating_template_id BIGINT,
    rating_rule_id BIGINT NOT NULL COMMENT 'FK → rating_rules.id',
    rating_name VARCHAR(100) NOT NULL COMMENT '评级名称: 卫生优秀班级',
    rating_code VARCHAR(50),

    -- 等级信息
    level_id BIGINT NOT NULL COMMENT 'FK → rating_levels.id',
    level_name VARCHAR(50) NOT NULL COMMENT '等级名称: 优秀',
    level_code VARCHAR(20),
    level_order INT,

    -- 计算依据
    calculated_score DECIMAL(10,2) COMMENT '用于评级的分数',
    ranking INT COMMENT '在该评级规则下的排名',
    total_classes INT COMMENT '参与该评级的总班级数',
    percentage_rank DECIMAL(5,2) COMMENT '百分比排名',

    -- 奖励
    reward_points INT DEFAULT 0,

    -- 版本控制
    rating_version INT DEFAULT 1,
    is_latest TINYINT DEFAULT 1,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_record (record_id),
    INDEX idx_class_stat (class_stat_id),
    INDEX idx_class (class_id),
    INDEX idx_rule (rating_rule_id),
    INDEX idx_latest (record_id, class_id, rating_rule_id, is_latest),
    CONSTRAINT fk_results_record
        FOREIGN KEY (record_id) REFERENCES check_records(id) ON DELETE CASCADE,
    CONSTRAINT fk_results_class_stat
        FOREIGN KEY (class_stat_id) REFERENCES check_record_class_stats(id),
    CONSTRAINT fk_results_rule
        FOREIGN KEY (rating_rule_id) REFERENCES rating_rules(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='评级结果表';


-- ════════════════════════════════════════════════════════════════════════════
-- 第九部分: 申诉表 (1张)
-- ════════════════════════════════════════════════════════════════════════════

-- 14. 申诉表
DROP TABLE IF EXISTS check_item_appeals;
CREATE TABLE check_item_appeals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL COMMENT 'FK → check_record_items.id',
    record_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    class_name VARCHAR(100),

    -- 扣分信息
    category_name VARCHAR(100),
    item_name VARCHAR(200),
    original_deduct_score DECIMAL(10,2),

    -- 申诉信息
    appeal_reason TEXT NOT NULL COMMENT '申诉理由',
    appeal_photos TEXT COMMENT '申诉证据照片',
    appeal_videos TEXT,

    -- 申诉人
    appealer_id BIGINT NOT NULL,
    appealer_name VARCHAR(50),
    appeal_time DATETIME NOT NULL,

    -- 申诉状态
    appeal_status TINYINT DEFAULT 1 COMMENT '状态: 1=待处理, 2=已通过, 3=已驳回, 4=已撤销',

    -- 处理信息
    handler_id BIGINT,
    handler_name VARCHAR(50),
    handle_time DATETIME,
    handle_result TEXT COMMENT '处理结果说明',

    -- 撤销信息
    withdrawn_at DATETIME,
    withdraw_reason TEXT,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_item (item_id),
    INDEX idx_record (record_id),
    INDEX idx_class (class_id),
    INDEX idx_status (appeal_status),
    INDEX idx_appeal_time (appeal_time),
    CONSTRAINT fk_appeals_item
        FOREIGN KEY (item_id) REFERENCES check_record_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='申诉表';


-- ════════════════════════════════════════════════════════════════════════════
-- 第十部分: 审计日志表 (1张)
-- ════════════════════════════════════════════════════════════════════════════

-- 15. 审计日志表
DROP TABLE IF EXISTS check_audit_logs;
CREATE TABLE check_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 操作信息
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型: CREATE, UPDATE, DELETE, SUBMIT, PUBLISH',
    operation_module VARCHAR(50) COMMENT '操作模块: CHECK_RECORD, APPEAL, RATING',
    operation_desc VARCHAR(500) COMMENT '操作描述',

    -- 关联对象
    target_type VARCHAR(50) COMMENT '目标类型: CHECK_RECORD, CHECK_ITEM, APPEAL',
    target_id BIGINT,

    -- 操作人
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(50),
    operator_ip VARCHAR(50),

    -- 变更内容
    old_value JSON COMMENT '变更前的值',
    new_value JSON COMMENT '变更后的值',

    -- 额外信息
    extra_info JSON,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_operation (operation_type, operation_module),
    INDEX idx_target (target_type, target_id),
    INDEX idx_operator (operator_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='审计日志表';


-- ════════════════════════════════════════════════════════════════════════════
-- 第十一部分: 存储过程和函数
-- ════════════════════════════════════════════════════════════════════════════

DELIMITER $$

-- ────────────────────────────────────────────────────────────────────────────
-- 函数: 计算加权后的扣分 (核心算法!)
-- ────────────────────────────────────────────────────────────────────────────
DROP FUNCTION IF EXISTS calculate_weighted_deduct$$

CREATE FUNCTION calculate_weighted_deduct(
    p_class_stat_id BIGINT,
    p_class_id BIGINT
) RETURNS DECIMAL(10,2)
READS SQL DATA
COMMENT '计算班级的加权后总扣分'
BEGIN
    DECLARE v_total_weighted_deduct DECIMAL(10,2) DEFAULT 0;
    DECLARE v_item_deduct DECIMAL(10,2);
    DECLARE v_item_id BIGINT;
    DECLARE v_category_id BIGINT;
    DECLARE v_weight_config_id BIGINT;
    DECLARE v_weight_enabled TINYINT;
    DECLARE v_class_size INT;
    DECLARE v_standard_size INT;
    DECLARE v_weight_factor DECIMAL(5,2);
    DECLARE done INT DEFAULT 0;

    -- 获取班级人数
    SELECT class_size INTO v_class_size
    FROM check_record_class_stats
    WHERE id = p_class_stat_id;

    -- 游标: 遍历该班级的所有扣分项
    BEGIN
        DECLARE item_cursor CURSOR FOR
            SELECT deduct_score, item_id, category_id
            FROM check_record_items
            WHERE class_stat_id = p_class_stat_id
            AND appeal_status != 2;  -- 排除申诉通过的

        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

        OPEN item_cursor;

        read_loop: LOOP
            FETCH item_cursor INTO v_item_deduct, v_item_id, v_category_id;

            IF done THEN
                LEAVE read_loop;
            END IF;

            -- 获取该扣分项的有效加权配置
            CALL get_effective_weight_config(
                (SELECT record_id FROM check_record_class_stats WHERE id = p_class_stat_id),
                v_item_id,
                v_category_id,
                v_weight_config_id,
                v_weight_enabled
            );

            -- 如果启用加权
            IF v_weight_enabled = 1 AND v_weight_config_id IS NOT NULL THEN
                -- 获取标准人数
                SELECT standard_size INTO v_standard_size
                FROM class_weight_configs
                WHERE id = v_weight_config_id;

                -- 计算权重系数
                SET v_weight_factor = v_class_size / v_standard_size;

                -- 调整扣分 (核心公式!)
                -- 注意: 扣分是负数, 除以权重系数
                -- 人数多 → 权重>1 → 扣分变小(更好)
                -- 人数少 → 权重<1 → 扣分变大(更严)
                SET v_total_weighted_deduct = v_total_weighted_deduct + (v_item_deduct / v_weight_factor);
            ELSE
                -- 不加权,使用原始扣分
                SET v_total_weighted_deduct = v_total_weighted_deduct + v_item_deduct;
            END IF;
        END LOOP;

        CLOSE item_cursor;
    END;

    RETURN v_total_weighted_deduct;
END$$


-- ────────────────────────────────────────────────────────────────────────────
-- 存储过程: 获取有效的加权配置 (处理继承)
-- ────────────────────────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS get_effective_weight_config$$

CREATE PROCEDURE get_effective_weight_config(
    IN p_record_id BIGINT,
    IN p_item_id BIGINT,
    IN p_category_id BIGINT,
    OUT p_weight_config_id BIGINT,
    OUT p_weight_enabled TINYINT
)
COMMENT '获取某个检查项的有效加权配置(处理3级继承)'
BEGIN
    DECLARE v_is_inherited TINYINT DEFAULT 1;

    -- 1) 先查找检查项级别的配置
    SELECT weight_config_id, weight_enabled, is_inherited
    INTO p_weight_config_id, p_weight_enabled, v_is_inherited
    FROM check_record_weight_configs
    WHERE record_id = p_record_id
    AND config_level = 'ITEM'
    AND item_id = p_item_id
    AND deleted = 0
    LIMIT 1;

    -- 如果找到且不继承,直接结束
    IF p_weight_config_id IS NOT NULL AND v_is_inherited = 0 THEN
        -- 已找到,结束
        SET p_weight_config_id = p_weight_config_id;
    ELSE
        -- 2) 查找类别级别的配置
        SELECT weight_config_id, weight_enabled, is_inherited
        INTO p_weight_config_id, p_weight_enabled, v_is_inherited
        FROM check_record_weight_configs
        WHERE record_id = p_record_id
        AND config_level = 'CATEGORY'
        AND category_id = p_category_id
        AND deleted = 0
        LIMIT 1;

        -- 如果找到且不继承
        IF p_weight_config_id IS NOT NULL AND v_is_inherited = 0 THEN
            -- 已找到,结束
            SET p_weight_config_id = p_weight_config_id;
        ELSE
            -- 3) 使用记录级别的配置
            SELECT weight_config_id, weight_enabled
            INTO p_weight_config_id, p_weight_enabled
            FROM check_record_weight_configs
            WHERE record_id = p_record_id
            AND config_level = 'RECORD'
            AND deleted = 0
            LIMIT 1;
        END IF;
    END IF;
END$$


-- ────────────────────────────────────────────────────────────────────────────
-- 存储过程: 计算班级分数
-- ────────────────────────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS calculate_class_scores$$

CREATE PROCEDURE calculate_class_scores(IN p_record_id BIGINT)
COMMENT '计算某次检查的所有班级分数和排名'
BEGIN
    -- 为每个班级计算分数
    UPDATE check_record_class_stats s
    SET
        -- 1) 计算原始扣分
        original_deduct = (
            SELECT COALESCE(SUM(i.deduct_score), 0)
            FROM check_record_items i
            WHERE i.class_stat_id = s.id
            AND i.appeal_status != 2  -- 排除申诉通过的
        ),

        -- 2) 计算加权后扣分 (这是核心逻辑!)
        weighted_deduct = calculate_weighted_deduct(s.id, s.class_id),

        -- 3) 计算最终得分
        final_score = 100 + calculate_weighted_deduct(s.id, s.class_id),  -- 注意weighted_deduct是负数

        updated_at = NOW()
    WHERE s.record_id = p_record_id;

    -- 4) 计算排名 (扣分少的排名靠前)
    UPDATE check_record_class_stats s1
    SET ranking = (
        SELECT COUNT(*) + 1
        FROM check_record_class_stats s2
        WHERE s2.record_id = s1.record_id
        AND s2.weighted_deduct > s1.weighted_deduct  -- 注意是>而不是<,因为是负数
    )
    WHERE s1.record_id = p_record_id;
END$$


-- ────────────────────────────────────────────────────────────────────────────
-- 存储过程: 计算评级
-- ────────────────────────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS calculate_ratings$$

CREATE PROCEDURE calculate_ratings(
    IN p_record_id BIGINT
)
COMMENT '计算某次检查的所有评级'
BEGIN
    DECLARE v_rating_template_id BIGINT;

    -- 获取该检查记录使用的评级模板
    SELECT rating_template_id INTO v_rating_template_id
    FROM check_records
    WHERE id = p_record_id;

    -- 遍历评级规则
    CALL calculate_ratings_for_template(p_record_id, v_rating_template_id);
END$$


DROP PROCEDURE IF EXISTS calculate_ratings_for_template$$

CREATE PROCEDURE calculate_ratings_for_template(
    IN p_record_id BIGINT,
    IN p_rating_template_id BIGINT
)
COMMENT '根据评级模板计算所有规则'
BEGIN
    DECLARE v_rule_id BIGINT;
    DECLARE v_rating_basis VARCHAR(50);
    DECLARE v_category_ids TEXT;
    DECLARE v_score_type VARCHAR(20);
    DECLARE v_condition_type VARCHAR(20);
    DECLARE done INT DEFAULT 0;

    BEGIN
        DECLARE rule_cursor CURSOR FOR
            SELECT id, rating_basis, category_ids, score_type, condition_type
            FROM rating_rules
            WHERE rating_template_id = p_rating_template_id
            AND is_enabled = 1
            AND deleted = 0;

        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

        OPEN rule_cursor;

        read_loop: LOOP
            FETCH rule_cursor INTO v_rule_id, v_rating_basis, v_category_ids,
                                   v_score_type, v_condition_type;

            IF done THEN
                LEAVE read_loop;
            END IF;

            -- 为该规则计算评级
            CALL calculate_rating_for_rule(
                p_record_id,
                v_rule_id,
                v_rating_basis,
                v_category_ids,
                v_score_type,
                v_condition_type
            );
        END LOOP;

        CLOSE rule_cursor;
    END;
END$$


DROP PROCEDURE IF EXISTS calculate_rating_for_rule$$

CREATE PROCEDURE calculate_rating_for_rule(
    IN p_record_id BIGINT,
    IN p_rule_id BIGINT,
    IN p_rating_basis VARCHAR(50),
    IN p_category_ids TEXT,
    IN p_score_type VARCHAR(20),
    IN p_condition_type VARCHAR(20)
)
COMMENT '根据单个评级规则计算结果'
BEGIN
    -- 创建临时表存储每个班级的评级分数
    DROP TEMPORARY TABLE IF EXISTS temp_class_rating_scores;
    CREATE TEMPORARY TABLE temp_class_rating_scores (
        class_stat_id BIGINT,
        class_id BIGINT,
        class_name VARCHAR(100),
        calculated_score DECIMAL(10,2),
        ranking INT,
        percentage_rank DECIMAL(5,2)
    );

    -- 根据评级依据计算分数
    IF p_rating_basis = 'SINGLE_CATEGORY' THEN
        -- 单个类别的扣分
        INSERT INTO temp_class_rating_scores (class_stat_id, class_id, class_name, calculated_score)
        SELECT
            s.id,
            s.class_id,
            s.class_name,
            COALESCE((
                SELECT SUM(i.deduct_score)
                FROM check_record_items i
                WHERE i.class_stat_id = s.id
                AND i.category_id = CAST(p_category_ids AS UNSIGNED)
                AND i.appeal_status != 2
            ), 0) AS calculated_score
        FROM check_record_class_stats s
        WHERE s.record_id = p_record_id;

    ELSEIF p_rating_basis = 'MULTI_CATEGORY' THEN
        -- 多个类别的扣分总和
        INSERT INTO temp_class_rating_scores (class_stat_id, class_id, class_name, calculated_score)
        SELECT
            s.id,
            s.class_id,
            s.class_name,
            COALESCE((
                SELECT SUM(i.deduct_score)
                FROM check_record_items i
                WHERE i.class_stat_id = s.id
                AND FIND_IN_SET(i.category_id, p_category_ids)
                AND i.appeal_status != 2
            ), 0) AS calculated_score
        FROM check_record_class_stats s
        WHERE s.record_id = p_record_id;

    ELSEIF p_rating_basis = 'TOTAL' THEN
        -- 总分
        IF p_score_type = 'WEIGHTED' THEN
            -- 使用加权后的扣分
            INSERT INTO temp_class_rating_scores (class_stat_id, class_id, class_name, calculated_score)
            SELECT
                s.id,
                s.class_id,
                s.class_name,
                s.weighted_deduct
            FROM check_record_class_stats s
            WHERE s.record_id = p_record_id;
        ELSE
            -- 使用原始扣分
            INSERT INTO temp_class_rating_scores (class_stat_id, class_id, class_name, calculated_score)
            SELECT
                s.id,
                s.class_id,
                s.class_name,
                s.original_deduct
            FROM check_record_class_stats s
            WHERE s.record_id = p_record_id;
        END IF;
    END IF;

    -- 计算排名 (扣分少的排前面, 因为是负数,所以>的靠前)
    UPDATE temp_class_rating_scores t1
    SET ranking = (
        SELECT COUNT(*) + 1
        FROM temp_class_rating_scores t2
        WHERE t2.calculated_score > t1.calculated_score
    );

    -- 计算百分比排名
    UPDATE temp_class_rating_scores
    SET percentage_rank = ROUND((ranking / (SELECT COUNT(*) FROM temp_class_rating_scores)) * 100, 2);

    -- 匹配评级等级并插入结果
    INSERT INTO check_record_rating_results (
        record_id,
        class_stat_id,
        class_id,
        class_name,
        rating_template_id,
        rating_rule_id,
        rating_name,
        rating_code,
        level_id,
        level_name,
        level_code,
        level_order,
        calculated_score,
        ranking,
        total_classes,
        percentage_rank,
        reward_points,
        rating_version,
        is_latest
    )
    SELECT
        p_record_id,
        t.class_stat_id,
        t.class_id,
        t.class_name,
        (SELECT rating_template_id FROM rating_rules WHERE id = p_rule_id),
        p_rule_id,
        r.rating_name,
        r.rating_code,
        l.id,
        l.level_name,
        l.level_code,
        l.level_order,
        t.calculated_score,
        t.ranking,
        (SELECT COUNT(*) FROM temp_class_rating_scores),
        t.percentage_rank,
        l.reward_points,
        1,
        1
    FROM temp_class_rating_scores t
    CROSS JOIN rating_rules r
    LEFT JOIN rating_levels l ON l.rating_rule_id = r.id
    WHERE r.id = p_rule_id
    AND (
        -- 混合条件: 同时满足百分比和绝对分数
        (p_condition_type = 'HYBRID' AND
         t.percentage_rank >= l.min_percent AND t.percentage_rank < l.max_percent AND
         t.calculated_score >= l.min_score AND t.calculated_score < l.max_score)
        OR
        -- 仅百分比
        (p_condition_type = 'PERCENTAGE' AND
         t.percentage_rank >= l.min_percent AND t.percentage_rank < l.max_percent)
        OR
        -- 仅绝对分数
        (p_condition_type = 'ABSOLUTE' AND
         t.calculated_score >= l.min_score AND t.calculated_score < l.max_score)
    );

    DROP TEMPORARY TABLE IF EXISTS temp_class_rating_scores;
END$$

DELIMITER ;


-- ════════════════════════════════════════════════════════════════════════════
-- 完成!
-- ════════════════════════════════════════════════════════════════════════════

SET FOREIGN_KEY_CHECKS = 1;

-- ────────────────────────────────────────────────────────────────────────────
-- 使用说明:
-- ────────────────────────────────────────────────────────────────────────────
-- 1. 本脚本创建15张新表和4个存储过程/函数
-- 2. 核心算法: calculate_weighted_deduct() - 实现加权扣分计算
-- 3. 关键特性:
--    - 支持3级加权配置继承+覆盖 (RECORD → CATEGORY → ITEM)
--    - 评级模板绑定检查模板 (rating_templates.check_template_id)
--    - 状态机管理检查记录 (草稿→提交→审核→发布→归档)
--    - 版本控制支持申诉后重算 (stat_version, is_latest)
--    - 快照机制保证历史数据准确性
-- 4. 执行命令:
--    mysql -u root -p student_management < 005_create_v3_complete_schema.sql
-- ────────────────────────────────────────────────────────────────────────────
