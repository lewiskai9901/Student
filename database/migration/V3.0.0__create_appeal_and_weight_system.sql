-- ============================================================================
-- 学生管理系统 - 申诉管理与人数加权系统 V3.0.0
-- 创建时间: 2024-12-29
-- 描述:
--   1. 创建年级管理表(grades)
--   2. 创建统一申诉管理表(check_item_appeals)
--   3. 创建班级人数加权系统相关表
--   4. 创建申诉审批流配置表
--   5. 修改现有表结构
--   6. 删除旧申诉表
--   7. 创建索引和约束
-- ============================================================================

USE student_management;

-- ============================================================================
-- 第一部分: 年级管理
-- ============================================================================

-- 1. 创建年级表
DROP TABLE IF EXISTS grades;
CREATE TABLE grades (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    -- 基础信息
    grade_name VARCHAR(50) NOT NULL COMMENT '年级名称(如:2024级高一)',
    grade_code VARCHAR(20) NOT NULL UNIQUE COMMENT '年级编码(如:GRADE_2024_10)',
    grade_level INT NOT NULL COMMENT '年级等级(10=高一,11=高二,12=高三)',
    enrollment_year YEAR NOT NULL COMMENT '入学年份',

    -- 所属部门
    department_id BIGINT NOT NULL COMMENT '所属院系ID',

    -- 年级管理人员
    grade_director_id BIGINT COMMENT '年级主任ID',
    grade_counselor_id BIGINT COMMENT '年级辅导员ID',

    -- 统计信息
    total_classes INT DEFAULT 0 COMMENT '班级总数',
    total_students INT DEFAULT 0 COMMENT '学生总数',
    standard_class_size INT COMMENT '标准班级人数(用于加权计算)',

    -- 状态信息
    status TINYINT DEFAULT 1 COMMENT '状态(1=在读,2=已毕业,3=停招)',
    graduation_year YEAR COMMENT '预计毕业年份',

    -- 系统字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记(0=正常,1=已删除)',

    -- 索引
    INDEX idx_department (department_id),
    INDEX idx_level (grade_level),
    INDEX idx_year (enrollment_year),
    INDEX idx_director (grade_director_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted),
    UNIQUE KEY uk_dept_level_year (department_id, grade_level, enrollment_year, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年级表';

-- ============================================================================
-- 第二部分: 班级人数加权系统
-- ============================================================================

-- 2. 班级人数标准表(固定标准)
DROP TABLE IF EXISTS class_size_standards;
CREATE TABLE class_size_standards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    semester_id BIGINT NOT NULL COMMENT '学期ID',
    department_id BIGINT NOT NULL COMMENT '院系ID',
    grade_level INT NOT NULL COMMENT '年级等级',

    standard_size INT NOT NULL COMMENT '标准班级人数',
    calculation_method VARCHAR(20) DEFAULT 'MANUAL' COMMENT '计算方式(MANUAL=手动设置,AUTO=自动计算)',
    calculation_date DATE COMMENT '计算日期',
    calculation_base_count INT COMMENT '计算基准(用于计算的班级数)',

    locked TINYINT DEFAULT 1 COMMENT '是否锁定(1=锁定使用固定值,0=解锁使用实时计算)',
    locked_at DATETIME COMMENT '锁定时间',
    locked_by BIGINT COMMENT '锁定人ID',

    remarks TEXT COMMENT '备注说明',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_semester_dept_grade (semester_id, department_id, grade_level),
    INDEX idx_semester (semester_id),
    INDEX idx_locked (locked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级人数标准表';

-- 3. 班级人数快照表(历史快照)
DROP TABLE IF EXISTS class_size_snapshots;
CREATE TABLE class_size_snapshots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    class_id BIGINT NOT NULL COMMENT '班级ID',
    snapshot_date DATE NOT NULL COMMENT '快照日期',

    student_count INT NOT NULL COMMENT '学生总数',
    active_count INT COMMENT '在校人数(排除休学/请假)',
    male_count INT COMMENT '男生人数',
    female_count INT COMMENT '女生人数',

    snapshot_source VARCHAR(20) DEFAULT 'AUTO' COMMENT '快照来源(AUTO=自动,MANUAL=手动,PUBLISH=发布检查时)',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_class_date (class_id, snapshot_date),
    INDEX idx_snapshot_date (snapshot_date),
    INDEX idx_class_id (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级人数快照表';

-- 4. 加权配置表
DROP TABLE IF EXISTS class_weight_configs;
CREATE TABLE class_weight_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    config_code VARCHAR(50) NOT NULL UNIQUE COMMENT '配置编码',

    -- 基础配置
    weight_mode VARCHAR(20) DEFAULT 'STANDARD' COMMENT '加权模式(STANDARD=标准折算,PER_CAPITA=人均,SEGMENT=分段,NONE=不加权)',
    enable_weight TINYINT DEFAULT 1 COMMENT '是否启用加权(0=禁用,1=启用)',

    -- 标准人数配置
    use_fixed_standard TINYINT DEFAULT 1 COMMENT '是否使用固定标准(1=使用class_size_standards表,0=实时计算)',

    -- 权重上下限
    min_weight DECIMAL(5,2) DEFAULT 0.50 COMMENT '最小权重系数',
    max_weight DECIMAL(5,2) DEFAULT 2.00 COMMENT '最大权重系数',
    enable_weight_limit TINYINT DEFAULT 1 COMMENT '是否启用权重上下限(0=不限制,1=限制)',

    -- 分段加权配置(JSON格式)
    segment_rules JSON COMMENT '分段规则 [{"minSize":1,"maxSize":20,"weight":1.5},...]',

    -- 应用范围
    apply_to_all TINYINT DEFAULT 1 COMMENT '应用到所有检查(1=是,0=否)',
    effective_date DATE COMMENT '生效日期',
    expire_date DATE COMMENT '失效日期',

    is_default TINYINT DEFAULT 0 COMMENT '是否默认配置',
    status TINYINT DEFAULT 1 COMMENT '状态(0=禁用,1=启用)',

    description TEXT COMMENT '配置说明',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted TINYINT DEFAULT 0,

    INDEX idx_status (status),
    INDEX idx_default (is_default),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级人数加权配置表';

-- 5. 类别加权规则表(支持不同类别使用不同加权策略)
DROP TABLE IF EXISTS category_weight_rules;
CREATE TABLE category_weight_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    weight_config_id BIGINT NOT NULL COMMENT '加权配置ID',
    category_id BIGINT NOT NULL COMMENT '量化类型ID(quantification_types)',

    enable_weight TINYINT DEFAULT 1 COMMENT '该类别是否启用加权',
    weight_mode VARCHAR(20) COMMENT '该类别的加权模式(覆盖全局配置)',

    remarks VARCHAR(200) COMMENT '备注',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_config_category (weight_config_id, category_id),
    INDEX idx_config (weight_config_id),
    INDEX idx_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='类别加权规则表';

-- ============================================================================
-- 第三部分: 申诉管理系统
-- ============================================================================

-- 6. 统一申诉表(基于check_record_items_v3)
DROP TABLE IF EXISTS check_item_appeals;
CREATE TABLE check_item_appeals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    appeal_code VARCHAR(50) NOT NULL UNIQUE COMMENT '申诉编号(格式:APPEAL-YYYYMMDD-XXX)',

    -- 关联检查记录V3
    record_id BIGINT NOT NULL COMMENT '检查记录ID(check_records_v3)',
    record_code VARCHAR(50) COMMENT '检查记录编号(冗余)',
    class_stat_id BIGINT NOT NULL COMMENT '班级统计ID(check_record_class_stats)',
    item_id BIGINT NOT NULL COMMENT '扣分明细ID(check_record_items_v3)',

    -- 班级信息(冗余,便于查询)
    class_id BIGINT NOT NULL COMMENT '班级ID',
    class_name VARCHAR(100) COMMENT '班级名称',
    grade_id BIGINT COMMENT '年级ID',

    -- 扣分信息(冗余,便于展示)
    category_id BIGINT COMMENT '类别ID',
    category_name VARCHAR(100) COMMENT '类别名称',
    item_name VARCHAR(200) COMMENT '扣分项名称',
    original_score DECIMAL(10,2) NOT NULL COMMENT '原始扣分',
    link_info VARCHAR(200) COMMENT '关联信息(宿舍/教室编号)',
    original_photo_urls TEXT COMMENT '原始照片URLs(JSON数组)',
    original_remark TEXT COMMENT '原始备注',

    -- 申诉信息
    appeal_type TINYINT DEFAULT 1 COMMENT '申诉类型(1=分数异议,2=事实异议,3=程序异议)',
    appeal_reason TEXT NOT NULL COMMENT '申诉理由',
    evidence_urls TEXT COMMENT '申诉证据URLs(JSON数组)',

    appellant_id BIGINT NOT NULL COMMENT '申诉人ID(班主任)',
    appellant_name VARCHAR(50) NOT NULL COMMENT '申诉人姓名',
    appellant_role VARCHAR(20) COMMENT '申诉人角色',

    appeal_time DATETIME NOT NULL COMMENT '申诉时间',
    appeal_deadline DATETIME COMMENT '申诉截止时间',

    -- 审核信息
    status TINYINT DEFAULT 1 COMMENT '状态(1=待审核,2=审核通过,3=审核驳回,4=已撤销,5=已过期,6=公示中,7=已生效)',
    current_step INT DEFAULT 1 COMMENT '当前审批步骤',

    -- 审批结果
    final_reviewer_id BIGINT COMMENT '最终审核人ID',
    final_reviewer_name VARCHAR(50) COMMENT '最终审核人姓名',
    final_review_time DATETIME COMMENT '最终审核时间',
    final_review_opinion TEXT COMMENT '最终审核意见',

    -- 调整结果
    adjusted_score DECIMAL(10,2) COMMENT '调整后扣分',
    score_change DECIMAL(10,2) COMMENT '分数变化(原始分-调整后分)',

    -- 公示期
    publicity_start_time DATETIME COMMENT '公示开始时间',
    publicity_end_time DATETIME COMMENT '公示结束时间',
    publicity_days INT DEFAULT 3 COMMENT '公示天数',

    -- 生效时间
    effective_time DATETIME COMMENT '实际生效时间',

    -- 审批流配置快照(JSON)
    approval_flow_snapshot JSON COMMENT '审批流配置快照',

    -- 系统字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    -- 索引
    INDEX idx_record_id (record_id),
    INDEX idx_class_id (class_id),
    INDEX idx_grade_id (grade_id),
    INDEX idx_item_id (item_id),
    INDEX idx_appellant (appellant_id),
    INDEX idx_status (status),
    INDEX idx_appeal_time (appeal_time),
    INDEX idx_current_step (current_step),
    INDEX idx_publicity_end (publicity_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查项目申诉表';

-- 7. 申诉审批记录表(每个审批步骤一条记录)
DROP TABLE IF EXISTS appeal_approval_records;
CREATE TABLE appeal_approval_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    appeal_id BIGINT NOT NULL COMMENT '申诉ID',
    step_order INT NOT NULL COMMENT '审批步骤序号',

    -- 审批人信息
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approver_name VARCHAR(50) COMMENT '审批人姓名',
    approver_role VARCHAR(50) COMMENT '审批人角色',

    -- 审批结果
    approval_status TINYINT COMMENT '审批结果(1=待审批,2=同意,3=驳回,4=转交)',
    approval_opinion TEXT COMMENT '审批意见',
    approval_time DATETIME COMMENT '审批时间',

    -- 调整分数
    suggested_score DECIMAL(10,2) COMMENT '建议调整分数',

    -- 超时控制
    received_time DATETIME COMMENT '接收时间',
    timeout_hours INT COMMENT '超时小时数',
    is_timeout TINYINT DEFAULT 0 COMMENT '是否超时',

    -- 转交信息
    transferred_to BIGINT COMMENT '转交给谁',
    transfer_reason VARCHAR(200) COMMENT '转交原因',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_appeal (appeal_id),
    INDEX idx_approver (approver_id),
    INDEX idx_status (approval_status),
    INDEX idx_step (step_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申诉审批记录表';

-- 8. 申诉审计日志表
DROP TABLE IF EXISTS appeal_audit_logs;
CREATE TABLE appeal_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    appeal_id BIGINT NOT NULL COMMENT '申诉ID',

    action_type TINYINT NOT NULL COMMENT '操作类型(1=提交,2=审核,3=撤销,4=修改,5=公示,6=生效)',
    action_user_id BIGINT NOT NULL COMMENT '操作人ID',
    action_user_name VARCHAR(50) COMMENT '操作人姓名',
    action_time DATETIME NOT NULL COMMENT '操作时间',

    before_status TINYINT COMMENT '操作前状态',
    after_status TINYINT COMMENT '操作后状态',
    before_score DECIMAL(10,2) COMMENT '操作前分数',
    after_score DECIMAL(10,2) COMMENT '操作后分数',

    comment TEXT COMMENT '操作说明',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(200) COMMENT '用户代理',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_appeal (appeal_id),
    INDEX idx_action_time (action_time),
    INDEX idx_action_type (action_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申诉审计日志表';

-- ============================================================================
-- 第四部分: 申诉配置表
-- ============================================================================

-- 9. 申诉配置表
DROP TABLE IF EXISTS appeal_configs;
CREATE TABLE appeal_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    config_code VARCHAR(50) NOT NULL UNIQUE COMMENT '配置编码',

    -- 申诉期限
    appeal_deadline_days INT DEFAULT 7 COMMENT '申诉期限(天)',
    appeal_deadline_mode VARCHAR(20) DEFAULT 'FIXED' COMMENT '期限模式(FIXED=固定天数,UNTIL_DATE=截止日期)',

    -- 申诉限制
    max_appeals_per_check INT COMMENT '每次检查最多申诉数(NULL=不限制)',
    max_appeals_per_class INT COMMENT '每个班级最多申诉数(NULL=不限制)',
    allow_withdraw TINYINT DEFAULT 1 COMMENT '是否允许撤销(1=允许,0=不允许)',
    withdraw_deadline_hours INT DEFAULT 24 COMMENT '撤销期限(小时)',

    -- 公示期配置
    enable_publicity TINYINT DEFAULT 1 COMMENT '是否启用公示期',
    publicity_days INT DEFAULT 3 COMMENT '公示天数',
    publicity_view_mode VARCHAR(20) DEFAULT 'LIMITED' COMMENT '公示查看模式(FULL=完全公开,LIMITED=限制查看,NONE=不公开)',

    -- 证据要求
    require_evidence TINYINT DEFAULT 1 COMMENT '是否必须提供证据',
    min_evidence_count INT DEFAULT 0 COMMENT '最少证据数量',
    max_evidence_count INT DEFAULT 5 COMMENT '最多证据数量',

    -- 自动处理
    auto_recalculate TINYINT DEFAULT 1 COMMENT '申诉通过后自动重算排名',

    -- 应用范围
    is_default TINYINT DEFAULT 0 COMMENT '是否默认配置',
    status TINYINT DEFAULT 1 COMMENT '状态',

    description TEXT COMMENT '配置说明',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted TINYINT DEFAULT 0,

    INDEX idx_status (status),
    INDEX idx_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申诉配置表';

-- 10. 申诉审批流配置表
DROP TABLE IF EXISTS appeal_approval_configs;
CREATE TABLE appeal_approval_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    appeal_config_id BIGINT NOT NULL COMMENT '申诉配置ID',
    step_order INT NOT NULL COMMENT '审批步骤序号',
    step_name VARCHAR(100) NOT NULL COMMENT '步骤名称',

    -- 审批人类型
    approver_type VARCHAR(30) NOT NULL COMMENT '审批人类型(GRADE_DIRECTOR=年级主任,ROLE=角色,USER=指定用户,DEPT_LEADER=部门负责人)',
    approver_role_code VARCHAR(50) COMMENT '角色编码(当type=ROLE时)',
    approver_user_id BIGINT COMMENT '指定用户ID(当type=USER时)',
    approver_dept_code VARCHAR(50) COMMENT '部门编码(当type=DEPT_LEADER时)',

    -- 审批模式
    sign_mode VARCHAR(10) DEFAULT 'AND' COMMENT '会签模式(AND=会签都同意,OR=或签任意一人)',
    is_required TINYINT DEFAULT 1 COMMENT '是否必须(1=必须,0=可选)',

    -- 触发条件(JSON格式)
    trigger_condition JSON COMMENT '触发条件(如: {"scoreChange": {">": 5}})',

    -- 超时配置
    timeout_hours INT COMMENT '超时小时数',
    timeout_action VARCHAR(20) DEFAULT 'REMIND' COMMENT '超时动作(REMIND=提醒,ESCALATE=升级,AUTO_PASS=自动通过,AUTO_REJECT=自动驳回)',
    remind_interval_hours INT DEFAULT 24 COMMENT '提醒间隔(小时)',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_config_step (appeal_config_id, step_order),
    INDEX idx_config (appeal_config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申诉审批流配置表';

-- ============================================================================
-- 第五部分: 修改现有表结构
-- ============================================================================

-- 11. 修改classes表,增加grade_id字段
ALTER TABLE classes
ADD COLUMN grade_id BIGINT COMMENT '年级ID' AFTER department_id,
ADD INDEX idx_grade_id (grade_id);

-- 12. 修改students表,增加grade_id字段(可选,便于查询)
ALTER TABLE students
ADD COLUMN grade_id BIGINT COMMENT '年级ID(冗余字段)' AFTER class_id,
ADD INDEX idx_grade_id (grade_id);

-- 13. 修改semesters表,增加配置字段
ALTER TABLE semesters
ADD COLUMN default_weight_config_id BIGINT COMMENT '默认加权配置ID' AFTER is_current,
ADD COLUMN default_appeal_config_id BIGINT COMMENT '默认申诉配置ID' AFTER default_weight_config_id;

-- 14. 修改check_records_v3表,增加加权和申诉配置
ALTER TABLE check_records_v3
ADD COLUMN weight_config_id BIGINT COMMENT '加权配置ID' AFTER daily_check_id,
ADD COLUMN weight_enabled TINYINT DEFAULT 1 COMMENT '是否启用加权' AFTER weight_config_id,
ADD COLUMN appeal_config_id BIGINT COMMENT '申诉配置ID' AFTER weight_enabled,
ADD INDEX idx_weight_config (weight_config_id),
ADD INDEX idx_appeal_config (appeal_config_id);

-- 15. 修改check_record_class_stats表,增加人数快照和加权字段
ALTER TABLE check_record_class_stats
ADD COLUMN grade_id BIGINT COMMENT '年级ID' AFTER class_id,
ADD COLUMN student_count_snapshot INT COMMENT '人数快照(检查时的班级人数)' AFTER teacher_name,
ADD COLUMN raw_total_score DECIMAL(10,2) COMMENT '原始总扣分(未加权)' AFTER total_score,
ADD COLUMN weight_factor DECIMAL(5,4) COMMENT '权重系数' AFTER raw_total_score,
ADD COLUMN adjusted_total_score DECIMAL(10,2) COMMENT '调整后总扣分(已加权)' AFTER weight_factor,
ADD INDEX idx_grade (grade_id);

-- 注意: total_score字段保留,作为最终用于排名的分数
-- 如果启用加权: total_score = adjusted_total_score
-- 如果不启用: total_score = raw_total_score

-- ============================================================================
-- 第六部分: 删除旧申诉表
-- ============================================================================

-- 16. 删除旧申诉表(用户选择直接删除)
DROP TABLE IF EXISTS daily_check_appeals;
DROP TABLE IF EXISTS quantification_appeals;
DROP TABLE IF EXISTS check_appeals;
DROP TABLE IF EXISTS check_item_appeals;

-- 注意: 如果有历史数据需要保留,请先备份:
-- CREATE TABLE daily_check_appeals_backup AS SELECT * FROM daily_check_appeals;

-- ============================================================================
-- 第七部分: 创建归档表
-- ============================================================================

-- 17. 检查记录归档表(结构与check_records_v3相同)
DROP TABLE IF EXISTS check_records_v3_archive;
CREATE TABLE check_records_v3_archive LIKE check_records_v3;
ALTER TABLE check_records_v3_archive
ADD COLUMN archive_time DATETIME COMMENT '归档时间',
ADD COLUMN archive_reason VARCHAR(200) COMMENT '归档原因',
ADD COLUMN archive_by BIGINT COMMENT '归档人ID';

-- 18. 班级统计归档表
DROP TABLE IF EXISTS check_record_class_stats_archive;
CREATE TABLE check_record_class_stats_archive LIKE check_record_class_stats;
ALTER TABLE check_record_class_stats_archive
ADD COLUMN archive_time DATETIME COMMENT '归档时间';

-- 19. 扣分明细归档表
DROP TABLE IF EXISTS check_record_items_v3_archive;
CREATE TABLE check_record_items_v3_archive LIKE check_record_items_v3;
ALTER TABLE check_record_items_v3_archive
ADD COLUMN archive_time DATETIME COMMENT '归档时间';

-- 20. 申诉归档表
DROP TABLE IF EXISTS check_item_appeals_archive;
CREATE TABLE check_item_appeals_archive LIKE check_item_appeals;
ALTER TABLE check_item_appeals_archive
ADD COLUMN archive_time DATETIME COMMENT '归档时间';

-- ============================================================================
-- 第八部分: 创建视图(便于查询)
-- ============================================================================

-- 21. 年级统计视图
CREATE OR REPLACE VIEW v_grade_statistics AS
SELECT
    g.id as grade_id,
    g.grade_name,
    g.grade_code,
    g.grade_level,
    g.enrollment_year,
    g.department_id,
    d.dept_name as department_name,
    g.grade_director_id,
    u.real_name as director_name,
    COUNT(DISTINCT c.id) as total_classes,
    SUM(c.student_count) as total_students,
    ROUND(AVG(c.student_count), 2) as avg_students_per_class,
    g.status
FROM grades g
LEFT JOIN departments d ON g.department_id = d.id
LEFT JOIN users u ON g.grade_director_id = u.id
LEFT JOIN classes c ON c.grade_id = g.id AND c.deleted = 0
WHERE g.deleted = 0
GROUP BY g.id;

-- 22. 申诉统计视图
CREATE OR REPLACE VIEW v_appeal_statistics AS
SELECT
    a.record_id,
    r.record_code,
    r.check_name,
    r.check_date,
    COUNT(*) as total_appeals,
    SUM(CASE WHEN a.status = 1 THEN 1 ELSE 0 END) as pending_appeals,
    SUM(CASE WHEN a.status = 2 THEN 1 ELSE 0 END) as approved_appeals,
    SUM(CASE WHEN a.status = 3 THEN 1 ELSE 0 END) as rejected_appeals,
    SUM(CASE WHEN a.status = 6 THEN 1 ELSE 0 END) as publicity_appeals,
    SUM(CASE WHEN a.status = 7 THEN 1 ELSE 0 END) as effective_appeals,
    ROUND(AVG(ABS(a.score_change)), 2) as avg_score_change,
    ROUND(SUM(ABS(a.score_change)), 2) as total_score_change
FROM check_item_appeals a
JOIN check_records_v3 r ON a.record_id = r.id
WHERE a.deleted = 0
GROUP BY a.record_id;

-- ============================================================================
-- 第九部分: 创建触发器(可选,用于自动维护统计数据)
-- ============================================================================

-- 23. 定时修正班级人数的存储过程
DELIMITER //

DROP PROCEDURE IF EXISTS sync_class_student_count //
CREATE PROCEDURE sync_class_student_count()
BEGIN
    -- 更新所有班级的student_count字段
    UPDATE classes c
    SET student_count = (
        SELECT COUNT(*)
        FROM students s
        WHERE s.class_id = c.id
        AND s.deleted = 0
        AND s.student_status IN (0, 1) -- 0=在读,1=休学(算在内)
    )
    WHERE c.deleted = 0;

    -- 更新年级统计
    UPDATE grades g
    SET
        total_classes = (SELECT COUNT(*) FROM classes WHERE grade_id = g.id AND deleted = 0),
        total_students = (SELECT COALESCE(SUM(student_count), 0) FROM classes WHERE grade_id = g.id AND deleted = 0)
    WHERE g.deleted = 0;
END //

DELIMITER ;

-- ============================================================================
-- 第十部分: 插入初始数据
-- ============================================================================

-- 24. 插入默认加权配置
INSERT INTO class_weight_configs (
    config_name, config_code, weight_mode, enable_weight,
    use_fixed_standard, min_weight, max_weight, enable_weight_limit,
    apply_to_all, is_default, status, description
) VALUES (
    '标准人数加权(默认)', 'DEFAULT_STANDARD_WEIGHT', 'STANDARD', 1,
    1, 0.50, 2.00, 1,
    1, 1, 1, '按标准人数进行加权计算,权重上下限0.5-2.0'
);

-- 25. 插入不加权配置(用于特殊检查)
INSERT INTO class_weight_configs (
    config_name, config_code, weight_mode, enable_weight,
    apply_to_all, is_default, status, description
) VALUES (
    '不使用人数加权', 'NO_WEIGHT', 'NONE', 0,
    0, 0, 1, '不进行人数加权,直接使用原始扣分'
);

-- 26. 插入默认申诉配置
INSERT INTO appeal_configs (
    config_name, config_code,
    appeal_deadline_days, max_appeals_per_check,
    allow_withdraw, withdraw_deadline_hours,
    enable_publicity, publicity_days, publicity_view_mode,
    require_evidence, min_evidence_count, max_evidence_count,
    auto_recalculate, is_default, status, description
) VALUES (
    '标准申诉流程(默认)', 'DEFAULT_APPEAL',
    7, NULL,
    1, 24,
    1, 3, 'LIMITED',
    1, 0, 5,
    1, 1, 1, '发布后7天内可申诉,3天公示期,自动重算排名'
);

-- 27. 插入默认审批流配置(两级审批:年级主任→教务处)
INSERT INTO appeal_approval_configs (
    appeal_config_id, step_order, step_name,
    approver_type, is_required, sign_mode,
    timeout_hours, timeout_action, remind_interval_hours
) VALUES
(1, 1, '年级主任审批', 'GRADE_DIRECTOR', 1, 'AND', 72, 'REMIND', 24),
(1, 2, '教务处审批', 'ROLE', 1, 'AND', 72, 'REMIND', 24);

-- 注意: 第2步的role_code需要后续配置
UPDATE appeal_approval_configs
SET approver_role_code = 'academic_office'
WHERE id = 2;

-- ============================================================================
-- 完成
-- ============================================================================

-- 执行数据修正存储过程
CALL sync_class_student_count();

-- 提交事务
COMMIT;

SELECT '✅ 数据库结构升级完成! V3.0.0 - 申诉管理与人数加权系统已创建' as message;
