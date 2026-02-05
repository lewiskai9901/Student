-- =============================================
-- V8.4.0: V6检查系统 - 检查项目扩展
-- =============================================

-- 1. 创建检查项目表 (V6版本)
CREATE TABLE IF NOT EXISTS inspection_projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_code VARCHAR(50) NOT NULL UNIQUE COMMENT '项目编码',
    project_name VARCHAR(200) NOT NULL COMMENT '项目名称',
    description TEXT COMMENT '项目描述',

    -- 模板相关
    template_id BIGINT NOT NULL COMMENT '模板ID',
    template_snapshot JSON COMMENT '模板快照（发布时固化）',

    -- 检查范围配置
    scope_type VARCHAR(20) NOT NULL DEFAULT 'ORG' COMMENT '范围类型: ORG-组织, SPACE-场所, USER-用户',
    scope_config JSON COMMENT '范围配置（选中的组织/场所/用户ID列表）',

    -- 时间周期配置
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE COMMENT '结束日期（空表示长期）',
    cycle_type VARCHAR(20) NOT NULL DEFAULT 'DAILY' COMMENT '周期类型: DAILY-每日, WEEKLY-每周, MONTHLY-每月, CUSTOM-自定义',
    cycle_config JSON COMMENT '周期配置（星期几、每月几号等）',
    time_slots JSON COMMENT '时间段配置（如上午、下午、晚间）',
    skip_holidays TINYINT(1) DEFAULT 1 COMMENT '是否跳过节假日',
    excluded_dates JSON COMMENT '排除的日期列表',

    -- 共享场所策略
    shared_space_strategy VARCHAR(20) DEFAULT 'RATIO' COMMENT '共享场所策略: RATIO-按比例, AVERAGE-平均, FULL-全额, MAIN_ONLY-仅主归属',

    -- 分数分配模式
    score_distribution_mode VARCHAR(20) DEFAULT 'DIRECT' COMMENT '分数分配模式: DIRECT-直接到班级, WEIGHTED-加权汇总',

    -- 检查员配置
    inspector_assignment_mode VARCHAR(20) DEFAULT 'FREE' COMMENT '检查员模式: FREE-自由领取, ASSIGNED-指定分配, HYBRID-混合模式',
    default_inspectors JSON COMMENT '默认检查员列表',

    -- 项目状态
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT-草稿, ACTIVE-进行中, PAUSED-暂停, COMPLETED-已完成, ARCHIVED-已归档',
    published_at DATETIME COMMENT '发布时间',
    paused_at DATETIME COMMENT '暂停时间',
    completed_at DATETIME COMMENT '完成时间',

    -- 统计缓存
    total_tasks INT DEFAULT 0 COMMENT '总任务数',
    completed_tasks INT DEFAULT 0 COMMENT '已完成任务数',

    -- 审计字段
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,

    INDEX idx_project_code (project_code),
    INDEX idx_template_id (template_id),
    INDEX idx_status (status),
    INDEX idx_start_date (start_date),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V6检查项目表';

-- 2. 创建检查任务表
CREATE TABLE IF NOT EXISTS inspection_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_code VARCHAR(50) NOT NULL UNIQUE COMMENT '任务编码',
    project_id BIGINT NOT NULL COMMENT '所属项目ID',

    -- 任务时间
    task_date DATE NOT NULL COMMENT '任务日期',
    time_slot VARCHAR(50) COMMENT '时间段',

    -- 任务状态
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待执行, IN_PROGRESS-进行中, SUBMITTED-已提交, REVIEWED-已审核, PUBLISHED-已发布, CANCELLED-已取消',

    -- 检查员
    inspector_id BIGINT COMMENT '检查员ID',
    inspector_name VARCHAR(100) COMMENT '检查员姓名',
    claimed_at DATETIME COMMENT '领取时间',

    -- 执行信息
    started_at DATETIME COMMENT '开始执行时间',
    submitted_at DATETIME COMMENT '提交时间',
    reviewed_at DATETIME COMMENT '审核时间',
    published_at DATETIME COMMENT '发布时间',

    -- 统计信息
    total_targets INT DEFAULT 0 COMMENT '总检查目标数',
    completed_targets INT DEFAULT 0 COMMENT '已完成目标数',
    skipped_targets INT DEFAULT 0 COMMENT '跳过目标数',

    remarks TEXT COMMENT '备注',

    -- 审计字段
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,

    INDEX idx_task_code (task_code),
    INDEX idx_project_id (project_id),
    INDEX idx_task_date (task_date),
    INDEX idx_inspector_id (inspector_id),
    INDEX idx_status (status),

    FOREIGN KEY (project_id) REFERENCES inspection_projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V6检查任务表';

-- 3. 创建检查目标表（每个任务的具体检查对象）
CREATE TABLE IF NOT EXISTS inspection_targets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '任务ID',

    -- 目标信息
    target_type VARCHAR(20) NOT NULL COMMENT '目标类型: ORG-组织, SPACE-场所, USER-用户',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    target_name VARCHAR(200) COMMENT '目标名称（冗余）',
    target_code VARCHAR(50) COMMENT '目标编码（冗余）',

    -- 归属信息（用于分数计算）
    org_unit_id BIGINT COMMENT '归属组织ID',
    org_unit_name VARCHAR(200) COMMENT '归属组织名称',
    class_id BIGINT COMMENT '归属班级ID',
    class_name VARCHAR(200) COMMENT '归属班级名称',

    -- 共享场所权重
    weight_ratio DECIMAL(5,2) DEFAULT 100.00 COMMENT '权重比例（共享场所用）',

    -- 检查状态
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待检查, LOCKED-锁定中, COMPLETED-已完成, SKIPPED-已跳过',
    locked_by BIGINT COMMENT '锁定者ID',
    locked_at DATETIME COMMENT '锁定时间',
    completed_at DATETIME COMMENT '完成时间',

    -- 分数信息
    base_score DECIMAL(6,2) DEFAULT 100.00 COMMENT '基础分',
    final_score DECIMAL(6,2) COMMENT '最终分数',
    deduction_total DECIMAL(6,2) DEFAULT 0.00 COMMENT '总扣分',
    bonus_total DECIMAL(6,2) DEFAULT 0.00 COMMENT '总加分',

    -- 快照信息
    snapshot JSON COMMENT '目标快照（检查时的状态）',

    skip_reason VARCHAR(500) COMMENT '跳过原因',
    remarks TEXT COMMENT '备注',

    -- 审计字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_task_id (task_id),
    INDEX idx_target_type_id (target_type, target_id),
    INDEX idx_org_unit_id (org_unit_id),
    INDEX idx_class_id (class_id),
    INDEX idx_status (status),

    FOREIGN KEY (task_id) REFERENCES inspection_tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V6检查目标表';

-- 4. 创建扣分记录表
CREATE TABLE IF NOT EXISTS inspection_deductions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_id BIGINT NOT NULL COMMENT '检查目标ID',

    -- 扣分项信息
    item_id BIGINT COMMENT '扣分项ID（从模板）',
    item_code VARCHAR(50) COMMENT '扣分项编码',
    item_name VARCHAR(200) NOT NULL COMMENT '扣分项名称',
    category_name VARCHAR(100) COMMENT '类别名称',

    -- 扣分详情
    deduction_score DECIMAL(6,2) NOT NULL COMMENT '扣分分值',
    quantity INT DEFAULT 1 COMMENT '数量',
    total_deduction DECIMAL(6,2) NOT NULL COMMENT '总扣分',

    -- 责任人信息
    responsible_user_id BIGINT COMMENT '责任人ID',
    responsible_user_name VARCHAR(100) COMMENT '责任人姓名',

    -- 证据
    evidence_description TEXT COMMENT '问题描述',
    evidence_attachments JSON COMMENT '证据附件列表',

    -- 申诉状态
    appeal_status VARCHAR(20) DEFAULT 'NONE' COMMENT '申诉状态: NONE-未申诉, PENDING-申诉中, APPROVED-申诉通过, REJECTED-申诉驳回',
    is_revoked TINYINT(1) DEFAULT 0 COMMENT '是否已撤销（申诉通过后）',

    remarks TEXT COMMENT '备注',

    -- 审计字段
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_target_id (target_id),
    INDEX idx_item_id (item_id),
    INDEX idx_responsible_user_id (responsible_user_id),
    INDEX idx_appeal_status (appeal_status),

    FOREIGN KEY (target_id) REFERENCES inspection_targets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V6扣分记录表';

-- 5. 创建加分记录表
CREATE TABLE IF NOT EXISTS inspection_bonuses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_id BIGINT NOT NULL COMMENT '检查目标ID',

    -- 加分项信息
    item_id BIGINT COMMENT '加分项ID',
    item_code VARCHAR(50) COMMENT '加分项编码',
    item_name VARCHAR(200) NOT NULL COMMENT '加分项名称',

    -- 加分详情
    bonus_score DECIMAL(6,2) NOT NULL COMMENT '加分分值',
    quantity INT DEFAULT 1 COMMENT '数量',
    total_bonus DECIMAL(6,2) NOT NULL COMMENT '总加分',

    -- 获得者信息
    awarded_user_id BIGINT COMMENT '获得者ID',
    awarded_user_name VARCHAR(100) COMMENT '获得者姓名',

    reason TEXT COMMENT '加分原因',
    evidence_attachments JSON COMMENT '证据附件',

    remarks TEXT COMMENT '备注',

    -- 审计字段
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_target_id (target_id),
    INDEX idx_item_id (item_id),
    INDEX idx_awarded_user_id (awarded_user_id),

    FOREIGN KEY (target_id) REFERENCES inspection_targets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V6加分记录表';

-- 6. 创建任务检查员分配表
CREATE TABLE IF NOT EXISTS task_inspector_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    inspector_id BIGINT NOT NULL COMMENT '检查员ID',
    inspector_name VARCHAR(100) COMMENT '检查员姓名',

    -- 分配范围
    scope_type VARCHAR(20) COMMENT '分配范围类型: ALL-全部, ORG-按组织, SPACE-按场所, CUSTOM-自定义',
    scope_ids JSON COMMENT '分配的目标ID列表',

    -- 状态
    status VARCHAR(20) DEFAULT 'ASSIGNED' COMMENT '状态: ASSIGNED-已分配, ACCEPTED-已接受, DECLINED-已拒绝',
    accepted_at DATETIME COMMENT '接受时间',

    -- 审计字段
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_task_id (task_id),
    INDEX idx_inspector_id (inspector_id),

    UNIQUE KEY uk_task_inspector (task_id, inspector_id),
    FOREIGN KEY (task_id) REFERENCES inspection_tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V6任务检查员分配表';

-- 7. 创建项目检查员配置表
CREATE TABLE IF NOT EXISTS project_inspector_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    inspector_id BIGINT NOT NULL COMMENT '检查员ID',
    inspector_name VARCHAR(100) COMMENT '检查员姓名',

    -- 配置
    is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认检查员',
    scope_type VARCHAR(20) COMMENT '负责范围类型',
    scope_ids JSON COMMENT '负责的目标ID列表',

    -- 排班配置
    available_days JSON COMMENT '可用日期（星期几）',
    available_time_slots JSON COMMENT '可用时间段',

    -- 审计字段
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_project_id (project_id),
    INDEX idx_inspector_id (inspector_id),

    UNIQUE KEY uk_project_inspector (project_id, inspector_id),
    FOREIGN KEY (project_id) REFERENCES inspection_projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V6项目检查员配置表';
