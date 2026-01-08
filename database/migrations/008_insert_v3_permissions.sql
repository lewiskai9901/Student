-- ==========================================
-- 量化模块 V3.0 权限数据初始化脚本
-- ==========================================
-- 功能: 插入量化模块V3.0的25个权限代码
-- 作者: Claude
-- 日期: 2025-11-24
-- 版本: V3.0
-- ==========================================

SET NAMES utf8mb4;
SET CHARACTER_SET_CLIENT = utf8mb4;

USE student_management;

-- ==========================================
-- 0. 清理已存在的量化权限数据（如果存在）
-- ==========================================

SET FOREIGN_KEY_CHECKS=0;
DELETE FROM role_permissions WHERE permission_id IN (
    SELECT id FROM permissions WHERE permission_code = 'quantification' OR permission_code LIKE 'quantification:%'
);
DELETE FROM permissions WHERE permission_code = 'quantification' OR permission_code LIKE 'quantification:%';
SET FOREIGN_KEY_CHECKS=1;

-- ==========================================
-- 1. 插入权限数据 (26个权限)
-- ==========================================

-- 一级模块: 量化管理
-- resource_type: 1=菜单/模块, 2=按钮/操作
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
VALUES ('quantification', '量化管理', 0, 1, '量化考核管理模块', 1, 1, NOW(), NOW(), 0);

-- 获取一级模块ID
SET @module_id = LAST_INSERT_ID();

-- ==========================================
-- 2.1 检查记录管理 (7个权限)
-- ==========================================

-- 二级功能: 检查记录管理
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
VALUES ('quantification:check-record', '检查记录管理', @module_id, 1, '检查记录管理功能', 1, 1, NOW(), NOW(), 0);

SET @check_record_id = LAST_INSERT_ID();

-- 三级操作权限
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
VALUES
('quantification:check-record:create', '创建检查记录', @check_record_id, 2, '创建新的检查记录', 1, 1, NOW(), NOW(), 0),
('quantification:check-record:update', '编辑检查记录', @check_record_id, 2, '编辑检查记录信息', 2, 1, NOW(), NOW(), 0),
('quantification:check-record:delete', '删除检查记录', @check_record_id, 2, '删除检查记录', 3, 1, NOW(), NOW(), 0),
('quantification:check-record:view', '查看检查记录', @check_record_id, 2, '查看检查记录详情', 4, 1, NOW(), NOW(), 0),
('quantification:check-record:submit', '提交检查记录', @check_record_id, 2, '提交检查记录进行审核', 5, 1, NOW(), NOW(), 0),
('quantification:check-record:review', '审核检查记录', @check_record_id, 2, '审核检查记录', 6, 1, NOW(), NOW(), 0),
('quantification:check-record:publish', '发布检查记录', @check_record_id, 2, '发布检查记录并计算得分', 7, 1, NOW(), NOW(), 0);

-- ==========================================
-- 2.2 扣分项管理 (5个权限)
-- ==========================================

-- 二级功能: 扣分项管理
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
VALUES ('quantification:check-item', '扣分项管理', @module_id, 1,'扣分项管理功能', 2, 1, NOW(), NOW(), 0);

SET @check_item_id = LAST_INSERT_ID();

-- 三级操作权限
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
VALUES
('quantification:check-item:add', '添加扣分项', @check_item_id, 2,'添加扣分项到检查记录', 1, 1, NOW(), NOW(), 0),
('quantification:check-item:update', '编辑扣分项', @check_item_id, 2,'编辑扣分项信息', 2, 1, NOW(), NOW(), 0),
('quantification:check-item:delete', '删除扣分项', @check_item_id, 2,'删除扣分项', 3, 1, NOW(), NOW(), 0),
('quantification:check-item:appeal', '提交申诉', @check_item_id, 2,'对扣分项提交申诉', 4, 1, NOW(), NOW(), 0),
('quantification:check-item:review-appeal', '审核申诉', @check_item_id, 2,'审核扣分项申诉', 5, 1, NOW(), NOW(), 0);

-- ==========================================
-- 2.3 加权配置管理 (3个权限)
-- ==========================================

-- 二级功能: 加权配置管理
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
VALUES ('quantification:weight-config', '加权配置管理', @module_id, 1,'加权配置管理功能', 3, 1, NOW(), NOW(), 0);

SET @weight_config_id = LAST_INSERT_ID();

-- 三级操作权限
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
VALUES
('quantification:weight-config:view', '查看加权配置', @weight_config_id, 2,'查看加权配置树和生效配置', 1, 1, NOW(), NOW(), 0),
('quantification:weight-config:set', '设置加权配置', @weight_config_id, 2,'设置记录级/类别级/检查项级加权配置', 2, 1, NOW(), NOW(), 0),
('quantification:weight-config:reset', '重置加权配置', @weight_config_id, 2,'重置为记录级配置', 3, 1, NOW(), NOW(), 0);

-- ==========================================
-- 2.4 评级管理 (6个权限)
-- ==========================================

-- 二级功能: 评级管理
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
VALUES ('quantification:rating', '评级管理', @module_id, 1,'评级管理功能', 4, 1, NOW(), NOW(), 0);

SET @rating_id = LAST_INSERT_ID();

-- 三级操作权限
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
VALUES
('quantification:rating:template', '管理评级模板', @rating_id, 2,'创建/编辑/删除评级模板', 1, 1, NOW(), NOW(), 0),
('quantification:rating:rule', '管理评级规则', @rating_id, 2,'创建/编辑/删除评级规则和等级', 2, 1, NOW(), NOW(), 0),
('quantification:rating:calculate', '计算评级', @rating_id, 2,'触发评级计算', 3, 1, NOW(), NOW(), 0),
('quantification:rating:view', '查看评级结果', @rating_id, 2,'查看班级评级结果', 4, 1, NOW(), NOW(), 0);

-- ==========================================
-- 2.5 字典管理 (4个权限)
-- ==========================================

-- 二级功能: 字典管理
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
VALUES ('quantification:dictionary', '字典管理', @module_id, 1,'检查字典管理功能', 5, 1, NOW(), NOW(), 0);

SET @dictionary_id = LAST_INSERT_ID();

-- 三级操作权限
INSERT INTO permissions (permission_code, permission_name, parent_id, resource_type, permission_desc, sort_order, status, created_at, updated_at, deleted)
VALUES
('quantification:dictionary:category', '管理检查类别', @dictionary_id, 2,'创建/编辑/删除检查类别', 1, 1, NOW(), NOW(), 0),
('quantification:dictionary:item', '管理检查项', @dictionary_id, 2,'创建/编辑/删除检查项', 2, 1, NOW(), NOW(), 0);

-- ==========================================
-- 3. 为超级管理员角色分配所有权限
-- ==========================================

-- 假设超级管理员角色ID为1
SET @admin_role_id = 1;

-- 插入角色权限关联 (一级模块权限)
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT @admin_role_id, id, NOW()
FROM permissions
WHERE permission_code = 'quantification' AND deleted = 0;

-- 插入角色权限关联 (所有二级和三级权限)
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT @admin_role_id, id, NOW()
FROM permissions
WHERE permission_code LIKE 'quantification:%' AND deleted = 0;

-- ==========================================
-- 4. 验证插入结果
-- ==========================================

-- 统计插入的权限数量
SELECT
    '权限插入统计' AS 'Category',
    (SELECT COUNT(*) FROM permissions WHERE permission_code = 'quantification' AND deleted = 0) AS '一级模块',
    (SELECT COUNT(*) FROM permissions WHERE permission_code LIKE 'quantification:%' AND permission_code NOT LIKE 'quantification:%:%' AND deleted = 0) AS '二级功能',
    (SELECT COUNT(*) FROM permissions WHERE permission_code LIKE 'quantification:%:%' AND deleted = 0) AS '三级操作',
    (SELECT COUNT(*) FROM permissions WHERE (permission_code = 'quantification' OR permission_code LIKE 'quantification:%') AND deleted = 0) AS '总权限数'
FROM DUAL;

-- 显示权限树结构
SELECT
    p1.permission_code AS '一级模块',
    p2.permission_code AS '二级功能',
    p3.permission_code AS '三级操作',
    p3.permission_name AS '权限名称',
    p3.resource_type AS '资源类型'
FROM permissions p1
LEFT JOIN permissions p2 ON p2.parent_id = p1.id AND p2.deleted = 0
LEFT JOIN permissions p3 ON p3.parent_id = p2.id AND p3.deleted = 0
WHERE p1.permission_code = 'quantification' AND p1.deleted = 0
ORDER BY p2.sort_order, p3.sort_order;

-- 验证角色权限分配
SELECT
    r.role_name AS '角色名称',
    COUNT(rp.permission_id) AS '分配的量化权限数量'
FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE r.id = @admin_role_id
  AND (p.permission_code = 'quantification' OR p.permission_code LIKE 'quantification:%')
  AND p.deleted = 0
GROUP BY r.id, r.role_name;

-- ==========================================
-- 执行完成提示
-- ==========================================

SELECT '✅ 量化模块V3.0权限数据初始化完成！' AS 'Status',
       '共插入26个权限（1个模块 + 5个功能 + 20个操作）' AS 'Details',
       '已为超级管理员角色分配所有权限' AS 'Role_Assignment';
