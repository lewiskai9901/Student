-- 资产审批表
CREATE TABLE IF NOT EXISTS asset_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    approval_no VARCHAR(32) NOT NULL COMMENT '审批单号',
    approval_type INT NOT NULL COMMENT '审批类型: 1-借用申请 2-采购申请 3-报废申请 4-调拨申请',
    business_id BIGINT COMMENT '关联业务ID',
    asset_id BIGINT COMMENT '关联资产ID',
    asset_name VARCHAR(200) COMMENT '资产名称',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    applicant_name VARCHAR(50) COMMENT '申请人姓名',
    applicant_dept VARCHAR(100) COMMENT '申请人部门',
    approver_id BIGINT COMMENT '审批人ID',
    approver_name VARCHAR(50) COMMENT '审批人姓名',
    status INT NOT NULL DEFAULT 0 COMMENT '审批状态: 0-待审批 1-已通过 2-已拒绝 3-已取消',
    apply_reason TEXT COMMENT '申请原因',
    apply_quantity INT COMMENT '申请数量',
    apply_amount DECIMAL(12,2) COMMENT '申请金额',
    approval_remark TEXT COMMENT '审批意见',
    apply_time DATETIME NOT NULL COMMENT '申请时间',
    approval_time DATETIME COMMENT '审批时间',
    expire_time DATETIME COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_approval_no (approval_no),
    INDEX idx_applicant_id (applicant_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_status (status),
    INDEX idx_approval_type (approval_type),
    INDEX idx_business_id (business_id),
    INDEX idx_asset_id (asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产审批表';

-- 资产预警表
CREATE TABLE IF NOT EXISTS asset_alert (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    alert_type INT NOT NULL COMMENT '预警类型: 1-借用逾期 2-即将逾期 3-保修到期 4-库存不足',
    asset_id BIGINT COMMENT '关联资产ID',
    asset_code VARCHAR(50) COMMENT '资产编码',
    asset_name VARCHAR(200) COMMENT '资产名称',
    borrow_id BIGINT COMMENT '关联借用记录ID',
    alert_content TEXT NOT NULL COMMENT '预警内容',
    alert_level INT NOT NULL DEFAULT 1 COMMENT '预警级别: 1-普通 2-重要 3-紧急',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读 1-已读',
    is_handled TINYINT NOT NULL DEFAULT 0 COMMENT '是否已处理: 0-未处理 1-已处理',
    handle_remark VARCHAR(500) COMMENT '处理备注',
    handle_time DATETIME COMMENT '处理时间',
    handler_id BIGINT COMMENT '处理人ID',
    handler_name VARCHAR(50) COMMENT '处理人姓名',
    notify_user_id BIGINT COMMENT '通知用户ID',
    notify_user_name VARCHAR(50) COMMENT '通知用户姓名',
    alert_time DATETIME NOT NULL COMMENT '预警时间',
    expire_time DATETIME COMMENT '预警过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_alert_type (alert_type),
    INDEX idx_asset_id (asset_id),
    INDEX idx_borrow_id (borrow_id),
    INDEX idx_is_read (is_read),
    INDEX idx_is_handled (is_handled),
    INDEX idx_notify_user_id (notify_user_id),
    INDEX idx_alert_time (alert_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产预警表';

-- 资产折旧记录表
CREATE TABLE IF NOT EXISTS asset_depreciation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    asset_id BIGINT NOT NULL COMMENT '资产ID',
    asset_code VARCHAR(50) COMMENT '资产编码',
    asset_name VARCHAR(200) COMMENT '资产名称',
    depreciation_method INT NOT NULL DEFAULT 1 COMMENT '折旧方法: 1-直线法 2-余额递减法 3-不计提',
    depreciation_year INT NOT NULL COMMENT '折旧年份',
    depreciation_month INT NOT NULL COMMENT '折旧月份',
    original_value DECIMAL(12,2) NOT NULL COMMENT '原值',
    residual_value DECIMAL(12,2) DEFAULT 0 COMMENT '残值',
    net_value_before DECIMAL(12,2) NOT NULL COMMENT '折旧前净值',
    depreciation_amount DECIMAL(12,2) NOT NULL COMMENT '本期折旧额',
    accumulated_depreciation DECIMAL(12,2) NOT NULL COMMENT '累计折旧',
    net_value_after DECIMAL(12,2) NOT NULL COMMENT '折旧后净值',
    depreciation_rate DECIMAL(8,4) COMMENT '折旧率',
    useful_life INT COMMENT '使用年限（月）',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_asset_id (asset_id),
    INDEX idx_year_month (depreciation_year, depreciation_month),
    UNIQUE KEY uk_asset_year_month (asset_id, depreciation_year, depreciation_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产折旧记录表';

-- 资产分类表添加折旧相关字段
ALTER TABLE asset_category
ADD COLUMN IF NOT EXISTS depreciation_method INT DEFAULT 1 COMMENT '折旧方法: 1-直线法 2-余额递减法 3-不计提',
ADD COLUMN IF NOT EXISTS residual_rate DECIMAL(5,2) DEFAULT 5.00 COMMENT '残值率(%)';

-- 资产表添加折旧相关字段
ALTER TABLE asset
ADD COLUMN IF NOT EXISTS depreciation_method INT COMMENT '折旧方法',
ADD COLUMN IF NOT EXISTS residual_value DECIMAL(12,2) COMMENT '残值',
ADD COLUMN IF NOT EXISTS accumulated_depreciation DECIMAL(12,2) DEFAULT 0 COMMENT '累计折旧',
ADD COLUMN IF NOT EXISTS useful_life INT COMMENT '使用年限（月）',
ADD COLUMN IF NOT EXISTS depreciation_start_date DATE COMMENT '开始折旧日期';

-- 资产表添加库存预警阈值
ALTER TABLE asset
ADD COLUMN IF NOT EXISTS stock_warning_threshold INT DEFAULT 10 COMMENT '库存预警阈值（批量管理适用）';

-- 资产分类表添加库存预警阈值
ALTER TABLE asset_category
ADD COLUMN IF NOT EXISTS default_stock_warning_threshold INT DEFAULT 10 COMMENT '默认库存预警阈值';
