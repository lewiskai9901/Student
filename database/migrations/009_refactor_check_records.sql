-- =====================================================
-- 检查记录系统彻底重构
-- 版本：V4.0
-- 日期：2024-11-30
-- 说明：彻底重构检查记录系统，实现完整快照存档功能
-- =====================================================

-- 删除旧的检查记录相关表（如果存在）
DROP TABLE IF EXISTS check_record_appeals_new;
DROP TABLE IF EXISTS check_record_deductions_new;
DROP TABLE IF EXISTS check_record_category_stats_new;
DROP TABLE IF EXISTS check_record_class_stats_new;
DROP TABLE IF EXISTS check_records_new;

-- =====================================================
-- 1. 检查记录主表（检查级别的完整快照）
-- =====================================================
CREATE TABLE check_records_new (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    -- 基本信息
    record_code VARCHAR(50) NOT NULL COMMENT '记录编号，格式：CR + 日期 + 序号',
    daily_check_id BIGINT NOT NULL COMMENT '关联的日常检查ID',
    check_name VARCHAR(200) NOT NULL COMMENT '检查名称',
    check_date DATE NOT NULL COMMENT '检查日期',
    check_type TINYINT NOT NULL DEFAULT 1 COMMENT '检查类型：1=日常检查 2=专项检查',

    -- 检查人快照
    checker_id BIGINT NOT NULL COMMENT '检查人ID',
    checker_name VARCHAR(50) NOT NULL COMMENT '检查人姓名（快照）',

    -- 模板快照
    template_id BIGINT COMMENT '使用的模板ID',
    template_name VARCHAR(100) COMMENT '模板名称（快照）',
    template_snapshot JSON COMMENT '模板配置完整快照（JSON格式）',

    -- 统计快照
    total_classes INT NOT NULL DEFAULT 0 COMMENT '涉及班级总数',
    total_deduction_count INT NOT NULL DEFAULT 0 COMMENT '扣分条数',
    total_deduction_score DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总扣分',
    avg_score DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '平均扣分',
    max_score DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '最高扣分',
    min_score DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '最低扣分',

    -- 状态
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=已发布 2=已归档',
    publish_time DATETIME COMMENT '发布时间',
    archive_time DATETIME COMMENT '归档时间',

    -- 申诉统计
    total_appeal_count INT NOT NULL DEFAULT 0 COMMENT '申诉总数',
    appeal_pending_count INT NOT NULL DEFAULT 0 COMMENT '待处理申诉',
    appeal_approved_count INT NOT NULL DEFAULT 0 COMMENT '通过的申诉',
    appeal_rejected_count INT NOT NULL DEFAULT 0 COMMENT '驳回的申诉',

    -- 快照元数据
    snapshot_version INT NOT NULL DEFAULT 1 COMMENT '快照版本（重算时递增）',
    snapshot_created_at DATETIME NOT NULL COMMENT '快照创建时间',
    last_recalc_at DATETIME COMMENT '最后重算时间',
    recalc_reason TEXT COMMENT '重算原因',

    -- 备注
    description TEXT COMMENT '检查说明',
    remarks TEXT COMMENT '备注',

    -- 审计字段
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',

    UNIQUE KEY uk_record_code (record_code),
    UNIQUE KEY uk_daily_check_id (daily_check_id),
    INDEX idx_check_date (check_date),
    INDEX idx_status (status),
    INDEX idx_checker_id (checker_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录主表';

-- =====================================================
-- 2. 班级检查统计表（班级级别的聚合快照）
-- =====================================================
CREATE TABLE check_record_class_stats_new (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '检查记录ID',

    -- 班级信息快照
    class_id BIGINT NOT NULL COMMENT '班级ID',
    class_name VARCHAR(100) NOT NULL COMMENT '班级名称（快照）',
    grade_id BIGINT COMMENT '年级ID',
    grade_name VARCHAR(50) COMMENT '年级名称（快照）',
    department_id BIGINT COMMENT '院系ID',
    department_name VARCHAR(100) COMMENT '院系名称（快照）',

    -- 班主任信息快照
    teacher_id BIGINT COMMENT '班主任ID',
    teacher_name VARCHAR(50) COMMENT '班主任姓名（快照）',
    teacher_phone VARCHAR(20) COMMENT '班主任电话（快照）',

    -- 班级规模快照
    class_size INT COMMENT '班级人数（快照）',

    -- 扣分统计
    deduction_count INT NOT NULL DEFAULT 0 COMMENT '扣分项数量',
    total_score DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总扣分',

    -- 分类扣分统计
    hygiene_score DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '卫生类扣分',
    discipline_score DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '纪律类扣分',
    attendance_score DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '考勤类扣分',
    dormitory_score DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '宿舍类扣分',
    other_score DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '其他类扣分',

    -- 排名
    overall_ranking INT COMMENT '全校排名',
    grade_ranking INT COMMENT '年级排名',
    department_ranking INT COMMENT '院系排名',

    -- 评级
    score_level VARCHAR(20) COMMENT '评分等级：优秀/良好/一般/较差',

    -- 申诉统计
    appeal_count INT NOT NULL DEFAULT 0 COMMENT '申诉数量',
    appeal_approved INT NOT NULL DEFAULT 0 COMMENT '通过的申诉',
    appeal_pending INT NOT NULL DEFAULT 0 COMMENT '待处理申诉',

    -- 对比分析
    vs_avg_diff DECIMAL(10,2) COMMENT '与平均分差值',
    vs_last_diff DECIMAL(10,2) COMMENT '与上次检查差值',
    trend VARCHAR(20) COMMENT '趋势：UP/DOWN/STABLE',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_record_id (record_id),
    INDEX idx_class_id (class_id),
    INDEX idx_grade_id (grade_id),
    INDEX idx_overall_ranking (record_id, overall_ranking),
    INDEX idx_grade_ranking (record_id, grade_id, grade_ranking),
    CONSTRAINT fk_class_stats_record FOREIGN KEY (record_id) REFERENCES check_records_new(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级检查统计表';

-- =====================================================
-- 3. 类别统计表（按检查类别聚合）
-- =====================================================
CREATE TABLE check_record_category_stats_new (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '检查记录ID',
    class_stat_id BIGINT NOT NULL COMMENT '班级统计ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',

    -- 类别信息快照
    category_id BIGINT NOT NULL COMMENT '检查类别ID',
    category_code VARCHAR(50) COMMENT '类别编码（快照）',
    category_name VARCHAR(100) NOT NULL COMMENT '类别名称（快照）',
    category_type VARCHAR(30) COMMENT '类别类型：HYGIENE/DISCIPLINE/ATTENDANCE/DORMITORY/OTHER',

    -- 检查轮次
    check_round INT NOT NULL DEFAULT 1 COMMENT '检查轮次',

    -- 统计
    deduction_count INT NOT NULL DEFAULT 0 COMMENT '扣分项数量',
    total_score DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '该类别总扣分',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_record_id (record_id),
    INDEX idx_class_stat_id (class_stat_id),
    INDEX idx_class_id (class_id),
    INDEX idx_category_id (category_id),
    CONSTRAINT fk_category_stats_record FOREIGN KEY (record_id) REFERENCES check_records_new(id) ON DELETE CASCADE,
    CONSTRAINT fk_category_stats_class FOREIGN KEY (class_stat_id) REFERENCES check_record_class_stats_new(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='类别检查统计表';

-- =====================================================
-- 4. 扣分明细表（最细粒度的快照）
-- =====================================================
CREATE TABLE check_record_deductions_new (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '检查记录ID',
    class_stat_id BIGINT NOT NULL COMMENT '班级统计ID',
    category_stat_id BIGINT COMMENT '类别统计ID',

    -- 来源追溯
    daily_check_detail_id BIGINT COMMENT '原日常检查明细ID',

    -- 班级信息（快照）
    class_id BIGINT NOT NULL COMMENT '班级ID',
    class_name VARCHAR(100) NOT NULL COMMENT '班级名称（快照）',

    -- 检查类别快照
    category_id BIGINT NOT NULL COMMENT '检查类别ID',
    category_name VARCHAR(100) NOT NULL COMMENT '类别名称（快照）',
    check_round INT NOT NULL DEFAULT 1 COMMENT '检查轮次',

    -- 扣分项快照
    deduction_item_id BIGINT COMMENT '扣分项ID',
    deduction_item_code VARCHAR(50) COMMENT '扣分项编码（快照）',
    deduction_item_name VARCHAR(200) NOT NULL COMMENT '扣分项名称（快照）',

    -- 扣分模式与计算
    deduct_mode TINYINT NOT NULL DEFAULT 1 COMMENT '扣分模式：1=固定扣分 2=按人数扣分 3=区间扣分',
    base_score DECIMAL(10,2) COMMENT '基础分（模式2使用）',
    per_person_score DECIMAL(10,2) COMMENT '每人扣分（模式2使用）',
    min_score DECIMAL(10,2) COMMENT '最小扣分（模式3使用）',
    max_score_limit DECIMAL(10,2) COMMENT '最大扣分（模式3使用）',
    actual_score DECIMAL(10,2) NOT NULL COMMENT '实际扣分',

    -- 涉及人员
    person_count INT NOT NULL DEFAULT 0 COMMENT '违规人数',
    student_ids TEXT COMMENT '涉及学生ID列表（逗号分隔）',
    student_names TEXT COMMENT '涉及学生姓名列表（快照）',

    -- 关联对象（宿舍/教室）
    link_type TINYINT NOT NULL DEFAULT 0 COMMENT '关联类型：0=无 1=宿舍 2=教室',
    link_id BIGINT COMMENT '关联对象ID',
    link_code VARCHAR(50) COMMENT '关联对象编号',
    link_name VARCHAR(100) COMMENT '关联对象名称（快照）',

    -- 证据材料
    photo_urls JSON COMMENT '照片URL列表',
    photo_count INT NOT NULL DEFAULT 0 COMMENT '照片数量',

    -- 备注说明
    remark TEXT COMMENT '扣分备注/说明',

    -- 检查人信息
    checker_id BIGINT COMMENT '实际打分人ID',
    checker_name VARCHAR(50) COMMENT '实际打分人姓名（快照）',
    check_time DATETIME COMMENT '打分时间',

    -- 申诉状态
    appeal_status TINYINT NOT NULL DEFAULT 0 COMMENT '申诉状态：0=未申诉 1=申诉中 2=已通过 3=已驳回',
    appeal_id BIGINT COMMENT '关联的申诉记录ID',
    appeal_result_remark TEXT COMMENT '申诉结果说明',

    -- 修订信息
    is_revised TINYINT NOT NULL DEFAULT 0 COMMENT '是否已修订：0=否 1=是',
    original_score DECIMAL(10,2) COMMENT '原始扣分（修订前）',
    revised_score DECIMAL(10,2) COMMENT '修订后扣分',
    revision_time DATETIME COMMENT '修订时间',
    revision_reason TEXT COMMENT '修订原因',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_record_id (record_id),
    INDEX idx_class_stat_id (class_stat_id),
    INDEX idx_class_id (class_id),
    INDEX idx_category_id (category_id),
    INDEX idx_appeal_status (appeal_status),
    INDEX idx_link (link_type, link_id),
    INDEX idx_deduction_item_id (deduction_item_id),
    CONSTRAINT fk_deductions_record FOREIGN KEY (record_id) REFERENCES check_records_new(id) ON DELETE CASCADE,
    CONSTRAINT fk_deductions_class_stat FOREIGN KEY (class_stat_id) REFERENCES check_record_class_stats_new(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扣分明细表';

-- =====================================================
-- 5. 检查记录申诉表
-- =====================================================
CREATE TABLE check_record_appeals_new (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    -- 关联
    record_id BIGINT NOT NULL COMMENT '检查记录ID',
    deduction_id BIGINT NOT NULL COMMENT '扣分明细ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',

    -- 申诉人信息
    appellant_id BIGINT NOT NULL COMMENT '申诉人ID',
    appellant_name VARCHAR(50) NOT NULL COMMENT '申诉人姓名',
    appellant_role VARCHAR(30) COMMENT '申诉人角色：TEACHER/STUDENT',

    -- 申诉内容
    appeal_reason TEXT NOT NULL COMMENT '申诉理由',
    appeal_evidence JSON COMMENT '申诉证据（图片URLs等）',

    -- 处理信息
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=待处理 1=处理中 2=已通过 3=已驳回',
    handler_id BIGINT COMMENT '处理人ID',
    handler_name VARCHAR(50) COMMENT '处理人姓名',
    handle_time DATETIME COMMENT '处理时间',
    handle_result TEXT COMMENT '处理结果说明',

    -- 扣分调整
    original_score DECIMAL(10,2) COMMENT '原扣分',
    adjusted_score DECIMAL(10,2) COMMENT '调整后扣分',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_record_id (record_id),
    INDEX idx_deduction_id (deduction_id),
    INDEX idx_class_id (class_id),
    INDEX idx_status (status),
    INDEX idx_appellant_id (appellant_id),
    CONSTRAINT fk_appeals_record FOREIGN KEY (record_id) REFERENCES check_records_new(id) ON DELETE CASCADE,
    CONSTRAINT fk_appeals_deduction FOREIGN KEY (deduction_id) REFERENCES check_record_deductions_new(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录申诉表';

-- =====================================================
-- 更新日常检查表：添加关联字段（使用存储过程避免重复添加报错）
-- =====================================================
DELIMITER //

-- 添加record_generated列
DROP PROCEDURE IF EXISTS add_record_generated_column//
CREATE PROCEDURE add_record_generated_column()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM information_schema.columns
        WHERE table_schema = DATABASE()
        AND table_name = 'daily_checks'
        AND column_name = 'record_generated'
    ) THEN
        ALTER TABLE daily_checks ADD COLUMN record_generated TINYINT NOT NULL DEFAULT 0 COMMENT '是否已生成记录：0=否 1=是' AFTER status;
    END IF;
END//

CALL add_record_generated_column()//
DROP PROCEDURE IF EXISTS add_record_generated_column//

-- 添加record_id列
DROP PROCEDURE IF EXISTS add_record_id_column//
CREATE PROCEDURE add_record_id_column()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM information_schema.columns
        WHERE table_schema = DATABASE()
        AND table_name = 'daily_checks'
        AND column_name = 'record_id'
    ) THEN
        ALTER TABLE daily_checks ADD COLUMN record_id BIGINT COMMENT '生成的检查记录ID' AFTER record_generated;
    END IF;
END//

CALL add_record_id_column()//
DROP PROCEDURE IF EXISTS add_record_id_column//

DELIMITER ;
