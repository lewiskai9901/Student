-- Asset Borrow Module Database Schema
-- Version: V20260125_1
-- Description: Add asset borrow/lend management tables

-- =====================================================
-- 1. Asset Borrow Table (资产借用记录表)
-- =====================================================
CREATE TABLE IF NOT EXISTS asset_borrow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    borrow_no VARCHAR(32) NOT NULL UNIQUE COMMENT '借用单号',
    borrow_type TINYINT NOT NULL COMMENT '类型: 1-领用(无需归还) 2-借用(需归还)',

    -- 资产信息
    asset_id BIGINT NOT NULL COMMENT '资产ID',
    asset_code VARCHAR(50) NOT NULL COMMENT '资产编号',
    asset_name VARCHAR(200) NOT NULL COMMENT '资产名称',
    quantity INT NOT NULL DEFAULT 1 COMMENT '借用数量',

    -- 借用人信息
    borrower_id BIGINT NOT NULL COMMENT '借用人ID',
    borrower_name VARCHAR(64) NOT NULL COMMENT '借用人姓名',
    borrower_dept VARCHAR(128) COMMENT '借用人部门',
    borrower_phone VARCHAR(32) COMMENT '借用人电话',

    -- 时间信息
    borrow_date DATETIME NOT NULL COMMENT '借出日期',
    expected_return_date DATE COMMENT '预计归还日期(借用必填)',
    actual_return_date DATETIME COMMENT '实际归还日期',

    -- 归还信息
    return_condition VARCHAR(200) COMMENT '归还状况: good-完好/damaged-损坏/lost-丢失',
    return_remark VARCHAR(500) COMMENT '归还备注',
    returner_id BIGINT COMMENT '归还经办人ID',
    returner_name VARCHAR(64) COMMENT '归还经办人姓名',

    -- 申请信息
    purpose VARCHAR(500) COMMENT '借用原因/用途',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-借出中 2-已归还 3-已逾期 4-已取消',

    -- 操作信息
    operator_id BIGINT NOT NULL COMMENT '借出经办人ID',
    operator_name VARCHAR(64) NOT NULL COMMENT '借出经办人姓名',

    -- 通用字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    INDEX idx_borrow_no (borrow_no),
    INDEX idx_asset_id (asset_id),
    INDEX idx_borrower_id (borrower_id),
    INDEX idx_status (status),
    INDEX idx_borrow_type (borrow_type),
    INDEX idx_expected_return (expected_return_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产借用记录表';

-- =====================================================
-- 2. Permissions for Asset Borrow Module
-- =====================================================
INSERT INTO permission (permission_code, permission_name, module, description, created_at) VALUES
('asset:borrow:list', '查看借用记录', 'asset', '查看资产借用记录列表', NOW()),
('asset:borrow:create', '创建借用', 'asset', '创建资产借用/领用申请', NOW()),
('asset:borrow:return', '归还资产', 'asset', '办理资产归还', NOW()),
('asset:borrow:cancel', '取消借用', 'asset', '取消借用记录', NOW())
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- Grant permissions to admin role
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE permission_code LIKE 'asset:borrow:%'
ON DUPLICATE KEY UPDATE role_id = role_id;
