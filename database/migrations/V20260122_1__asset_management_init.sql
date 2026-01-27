-- Asset Management Module Database Schema
-- Version: V20260122_1
-- Description: Initialize asset management tables

-- =====================================================
-- 1. Asset Category Table (资产分类表)
-- =====================================================
CREATE TABLE IF NOT EXISTS asset_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT DEFAULT NULL COMMENT '父分类ID',
    category_code VARCHAR(32) NOT NULL COMMENT '分类编码',
    category_name VARCHAR(64) NOT NULL COMMENT '分类名称',
    category_type TINYINT NOT NULL DEFAULT 1 COMMENT '类型: 1-固定资产 2-低值易耗品',
    depreciation_years INT COMMENT '折旧年限(固定资产)',
    unit VARCHAR(16) COMMENT '默认计量单位',
    sort_order INT DEFAULT 0 COMMENT '排序',
    remark VARCHAR(255) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_parent_id (parent_id),
    INDEX idx_category_code (category_code),
    UNIQUE KEY uk_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产分类表';

-- =====================================================
-- 2. Asset Table (资产主表)
-- =====================================================
CREATE TABLE IF NOT EXISTS asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_code VARCHAR(32) NOT NULL UNIQUE COMMENT '资产编号',
    asset_name VARCHAR(128) NOT NULL COMMENT '资产名称',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    brand VARCHAR(64) COMMENT '品牌',
    model VARCHAR(64) COMMENT '型号规格',
    unit VARCHAR(16) NOT NULL COMMENT '计量单位',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    original_value DECIMAL(12,2) COMMENT '原值(单价)',
    net_value DECIMAL(12,2) COMMENT '净值',
    purchase_date DATE COMMENT '购置日期',
    warranty_date DATE COMMENT '保修截止日期',
    supplier VARCHAR(128) COMMENT '供应商',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-在用 2-闲置 3-维修中 4-已报废',
    -- 位置信息
    location_type VARCHAR(32) COMMENT '位置类型: classroom/dormitory/office/warehouse',
    location_id BIGINT COMMENT '位置ID(教室ID/宿舍ID等)',
    location_name VARCHAR(128) COMMENT '位置名称(冗余存储便于查询)',
    -- 责任人
    responsible_user_id BIGINT COMMENT '责任人用户ID',
    responsible_user_name VARCHAR(64) COMMENT '责任人姓名',
    -- 其他
    remark VARCHAR(512) COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_location (location_type, location_id),
    INDEX idx_asset_code (asset_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产主表';

-- =====================================================
-- 3. Asset History Table (资产变更记录表)
-- =====================================================
CREATE TABLE IF NOT EXISTS asset_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_id BIGINT NOT NULL COMMENT '资产ID',
    change_type VARCHAR(32) NOT NULL COMMENT '变更类型: CREATE/UPDATE/TRANSFER/REPAIR/SCRAP',
    change_content TEXT COMMENT '变更内容(JSON格式记录字段变化)',
    old_location_type VARCHAR(32) COMMENT '原位置类型',
    old_location_id BIGINT COMMENT '原位置ID',
    old_location_name VARCHAR(128) COMMENT '原位置名称',
    new_location_type VARCHAR(32) COMMENT '新位置类型',
    new_location_id BIGINT COMMENT '新位置ID',
    new_location_name VARCHAR(128) COMMENT '新位置名称',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) COMMENT '操作人姓名',
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    remark VARCHAR(255) COMMENT '备注',
    INDEX idx_asset_id (asset_id),
    INDEX idx_change_type (change_type),
    INDEX idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产变更记录表';

-- =====================================================
-- 4. Asset Maintenance Table (资产维修保养记录表)
-- =====================================================
CREATE TABLE IF NOT EXISTS asset_maintenance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_id BIGINT NOT NULL COMMENT '资产ID',
    maintenance_type TINYINT NOT NULL COMMENT '类型: 1-维修 2-保养',
    fault_desc VARCHAR(512) COMMENT '故障描述',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    cost DECIMAL(10,2) COMMENT '费用',
    maintainer VARCHAR(64) COMMENT '维修人/维修单位',
    result VARCHAR(255) COMMENT '维修结果',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-进行中 2-已完成',
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_asset_id (asset_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产维修保养记录表';

-- =====================================================
-- 5. Asset Inventory Table (资产盘点表)
-- =====================================================
CREATE TABLE IF NOT EXISTS asset_inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inventory_code VARCHAR(32) NOT NULL COMMENT '盘点单号',
    inventory_name VARCHAR(128) NOT NULL COMMENT '盘点名称',
    scope_type VARCHAR(32) COMMENT '盘点范围: all/category/location',
    scope_value VARCHAR(255) COMMENT '范围值(分类ID/位置)',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '截止日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-进行中 2-已完成 3-已取消',
    total_count INT COMMENT '应盘数量',
    checked_count INT DEFAULT 0 COMMENT '已盘数量',
    profit_count INT DEFAULT 0 COMMENT '盘盈数量',
    loss_count INT DEFAULT 0 COMMENT '盘亏数量',
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_inventory_code (inventory_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产盘点表';

-- =====================================================
-- 6. Asset Inventory Detail Table (资产盘点明细表)
-- =====================================================
CREATE TABLE IF NOT EXISTS asset_inventory_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inventory_id BIGINT NOT NULL COMMENT '盘点ID',
    asset_id BIGINT NOT NULL COMMENT '资产ID',
    expected_quantity INT NOT NULL COMMENT '账面数量',
    actual_quantity INT COMMENT '实盘数量',
    difference INT COMMENT '差异数量',
    result_type TINYINT COMMENT '结果: 1-正常 2-盘盈 3-盘亏',
    check_time DATETIME COMMENT '盘点时间',
    checker_id BIGINT COMMENT '盘点人ID',
    checker_name VARCHAR(64) COMMENT '盘点人姓名',
    remark VARCHAR(255) COMMENT '备注',
    INDEX idx_inventory_id (inventory_id),
    INDEX idx_asset_id (asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产盘点明细表';

-- =====================================================
-- 7. Initial Data - Asset Categories
-- =====================================================
INSERT INTO asset_category (category_code, category_name, category_type, unit, sort_order, parent_id) VALUES
-- 一级分类
('TEACH', '教学设备', 1, '台', 1, NULL),
('OFFICE', '办公设备', 1, '台', 2, NULL),
('DORM', '宿舍设施', 1, '件', 3, NULL),
('OTHER', '其他资产', 1, '件', 4, NULL);

-- 获取父分类ID并插入子分类
SET @teach_id = (SELECT id FROM asset_category WHERE category_code = 'TEACH');
SET @office_id = (SELECT id FROM asset_category WHERE category_code = 'OFFICE');
SET @dorm_id = (SELECT id FROM asset_category WHERE category_code = 'DORM');

-- 教学设备子分类
INSERT INTO asset_category (category_code, category_name, category_type, unit, sort_order, parent_id) VALUES
('TEACH-DESK', '课桌', 1, '张', 1, @teach_id),
('TEACH-CHAIR', '椅凳', 1, '把', 2, @teach_id),
('TEACH-PODIUM', '讲台', 1, '个', 3, @teach_id),
('TEACH-BOARD', '黑板/白板', 1, '块', 4, @teach_id),
('TEACH-MULTIMEDIA', '多媒体设备', 1, '套', 5, @teach_id),
('TEACH-LECTERN', '讲桌', 1, '张', 6, @teach_id),
('TEACH-PROJECTOR', '投影仪', 1, '台', 7, @teach_id),
('TEACH-AC', '空调', 1, '台', 8, @teach_id);

-- 办公设备子分类
INSERT INTO asset_category (category_code, category_name, category_type, unit, sort_order, parent_id) VALUES
('OFFICE-PC', '电脑', 1, '台', 1, @office_id),
('OFFICE-PRINT', '打印机', 1, '台', 2, @office_id),
('OFFICE-DESK', '办公桌', 1, '张', 3, @office_id),
('OFFICE-CHAIR', '办公椅', 1, '把', 4, @office_id),
('OFFICE-CABINET', '文件柜', 1, '个', 5, @office_id);

-- 宿舍设施子分类
INSERT INTO asset_category (category_code, category_name, category_type, unit, sort_order, parent_id) VALUES
('DORM-BED', '床铺', 1, '张', 1, @dorm_id),
('DORM-CABINET', '衣柜', 1, '个', 2, @dorm_id),
('DORM-DESK', '书桌', 1, '张', 3, @dorm_id),
('DORM-CHAIR', '椅子', 1, '把', 4, @dorm_id),
('DORM-FAN', '风扇', 1, '台', 5, @dorm_id),
('DORM-AC', '空调', 1, '台', 6, @dorm_id);

-- =====================================================
-- 8. Permissions for Asset Module
-- =====================================================
INSERT INTO permission (permission_code, permission_name, module, description, created_at) VALUES
-- 资产管理权限
('asset:list', '查看资产列表', 'asset', '查看资产台账列表', NOW()),
('asset:create', '创建资产', 'asset', '新增资产记录', NOW()),
('asset:update', '更新资产', 'asset', '编辑资产信息', NOW()),
('asset:delete', '删除资产', 'asset', '删除资产记录', NOW()),
('asset:transfer', '调拨资产', 'asset', '资产调拨转移', NOW()),
('asset:scrap', '报废资产', 'asset', '资产报废处理', NOW()),
-- 资产分类权限
('asset:category:list', '查看分类', 'asset', '查看资产分类列表', NOW()),
('asset:category:manage', '管理分类', 'asset', '新增/编辑/删除资产分类', NOW()),
-- 资产盘点权限
('asset:inventory:list', '查看盘点', 'asset', '查看资产盘点列表', NOW()),
('asset:inventory:create', '创建盘点', 'asset', '创建资产盘点任务', NOW()),
('asset:inventory:execute', '执行盘点', 'asset', '执行资产盘点操作', NOW()),
-- 资产维修权限
('asset:maintenance:list', '查看维修记录', 'asset', '查看资产维修记录', NOW()),
('asset:maintenance:create', '创建维修记录', 'asset', '新增资产维修记录', NOW())
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- Grant permissions to admin role (assuming role_id = 1 is admin)
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission WHERE module = 'asset'
ON DUPLICATE KEY UPDATE role_id = role_id;
