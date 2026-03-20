-- V7 Inspection Platform Permissions
-- 为 V7 检查平台模块添加权限记录，确保 SUPER_ADMIN 可以访问所有 V7 功能

-- Template BC permissions
INSERT INTO permissions (id, permission_name, permission_code, permission_desc, resource_type, parent_id, sort_order, status, tenant_id, created_at, updated_at, deleted)
VALUES
(900001, 'V7模板查看', 'insp:template:view', 'V7检查平台-查看模板列表', 2, NULL, 1, 1, 1, NOW(), NOW(), 0),
(900002, 'V7模板创建', 'insp:template:create', 'V7检查平台-创建模板', 2, NULL, 2, 1, 1, NOW(), NOW(), 0),
(900003, 'V7模板编辑', 'insp:template:edit', 'V7检查平台-编辑模板', 2, NULL, 3, 1, 1, NOW(), NOW(), 0),
(900004, 'V7模板删除', 'insp:template:delete', 'V7检查平台-删除模板', 2, NULL, 4, 1, 1, NOW(), NOW(), 0),
(900005, 'V7模板发布', 'insp:template:publish', 'V7检查平台-发布模板', 2, NULL, 5, 1, 1, NOW(), NOW(), 0),
(900006, 'V7分类查看', 'insp:catalog:view', 'V7检查平台-查看模板分类', 2, NULL, 6, 1, 1, NOW(), NOW(), 0),
(900007, 'V7分类管理', 'insp:catalog:manage', 'V7检查平台-管理模板分类', 2, NULL, 7, 1, 1, NOW(), NOW(), 0),
(900008, 'V7选项集查看', 'insp:response-set:view', 'V7检查平台-查看选项集', 2, NULL, 8, 1, 1, NOW(), NOW(), 0),
(900009, 'V7选项集管理', 'insp:response-set:manage', 'V7检查平台-管理选项集', 2, NULL, 9, 1, 1, NOW(), NOW(), 0),

-- Scoring BC permissions
(900010, 'V7评分配置查看', 'insp:scoring-profile:view', 'V7检查平台-查看评分配置', 2, NULL, 10, 1, 1, NOW(), NOW(), 0),
(900011, 'V7评分配置编辑', 'insp:scoring-profile:edit', 'V7检查平台-编辑评分配置', 2, NULL, 11, 1, 1, NOW(), NOW(), 0),

-- Execution BC permissions
(900020, 'V7项目查看', 'insp:project:view', 'V7检查平台-查看检查项目', 2, NULL, 20, 1, 1, NOW(), NOW(), 0),
(900021, 'V7项目创建', 'insp:project:create', 'V7检查平台-创建检查项目', 2, NULL, 21, 1, 1, NOW(), NOW(), 0),
(900022, 'V7项目编辑', 'insp:project:edit', 'V7检查平台-编辑检查项目', 2, NULL, 22, 1, 1, NOW(), NOW(), 0),
(900023, 'V7项目管理', 'insp:project:manage', 'V7检查平台-管理检查项目(发布/暂停等)', 2, NULL, 23, 1, 1, NOW(), NOW(), 0),
(900030, 'V7任务查看', 'insp:task:view', 'V7检查平台-查看检查任务', 2, NULL, 30, 1, 1, NOW(), NOW(), 0),
(900031, 'V7任务执行', 'insp:task:execute', 'V7检查平台-执行检查任务', 2, NULL, 31, 1, 1, NOW(), NOW(), 0),
(900032, 'V7任务审核', 'insp:task:review', 'V7检查平台-审核检查任务', 2, NULL, 32, 1, 1, NOW(), NOW(), 0),

-- Corrective BC permissions
(900040, 'V7整改查看', 'insp:corrective:view', 'V7检查平台-查看整改案例', 2, NULL, 40, 1, 1, NOW(), NOW(), 0),
(900041, 'V7整改管理', 'insp:corrective:manage', 'V7检查平台-管理整改案例', 2, NULL, 41, 1, 1, NOW(), NOW(), 0),

-- Analytics BC permissions
(900050, 'V7分析查看', 'insp:analytics:view', 'V7检查平台-查看分析报表', 2, NULL, 50, 1, 1, NOW(), NOW(), 0),
(900051, 'V7分析导出', 'insp:analytics:export', 'V7检查平台-导出分析数据', 2, NULL, 51, 1, 1, NOW(), NOW(), 0),

-- Rating Integration permissions
(900060, 'V7评级链接查看', 'insp:rating-link:view', 'V7检查平台-查看评级链接', 2, NULL, 60, 1, 1, NOW(), NOW(), 0),
(900061, 'V7评级链接管理', 'insp:rating-link:manage', 'V7检查平台-管理评级链接', 2, NULL, 61, 1, 1, NOW(), NOW(), 0),

-- Sync permissions
(900070, 'V7离线同步', 'insp:sync:access', 'V7检查平台-离线同步数据', 2, NULL, 70, 1, 1, NOW(), NOW(), 0);
