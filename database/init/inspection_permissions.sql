-- =============================================
-- 量化检查系统完整权限初始化
-- 包含: inspection, corrective, schedule, analytics, export 权限
-- =============================================

-- 1. 插入所有检查相关权限（如已存在则忽略）
INSERT IGNORE INTO permissions (permission_name, permission_code, resource_type, parent_id, sort_order, status, created_at) VALUES
-- 检查记录权限
('检查记录查看', 'inspection:record:view', 2, 0, 0, 1, NOW()),
('检查记录创建', 'inspection:record:create', 2, 0, 0, 1, NOW()),
('检查记录编辑', 'inspection:record:edit', 2, 0, 0, 1, NOW()),
('检查记录提交', 'inspection:record:submit', 2, 0, 0, 1, NOW()),
('检查记录审核', 'inspection:record:review', 2, 0, 0, 1, NOW()),
('检查记录发布', 'inspection:record:publish', 2, 0, 0, 1, NOW()),
-- 检查模板权限
('检查模板查看', 'inspection:template:view', 2, 0, 0, 1, NOW()),
('检查模板编辑', 'inspection:template:edit', 2, 0, 0, 1, NOW()),
-- 量化模板权限 (V1名称兼容)
('量化模板添加', 'quantification:template:add', 2, 0, 0, 1, NOW()),
('量化模板查看', 'quantification:template:view', 2, 0, 0, 1, NOW()),
('量化模板编辑', 'quantification:template:edit', 2, 0, 0, 1, NOW()),
-- 申诉权限
('申诉创建', 'inspection:appeal:create', 2, 0, 0, 1, NOW()),
('申诉查看', 'inspection:appeal:view', 2, 0, 0, 1, NOW()),
('申诉审核', 'inspection:appeal:review', 2, 0, 0, 1, NOW()),
('申诉终审', 'inspection:appeal:final-review', 2, 0, 0, 1, NOW()),
-- 导出权限
('检查导出', 'inspection:export', 2, 0, 0, 1, NOW()),
('检查导出查看', 'inspection:export:view', 1, 0, 0, 1, NOW()),
('创建导出', 'inspection:export:create', 2, 0, 0, 1, NOW()),
-- 纠正行动权限
('纠正行动创建', 'corrective:action:create', 2, 0, 0, 1, NOW()),
('纠正行动查看', 'corrective:action:view', 2, 0, 0, 1, NOW()),
('纠正行动处理', 'corrective:action:process', 2, 0, 0, 1, NOW()),
('纠正行动验证', 'corrective:action:verify', 2, 0, 0, 1, NOW()),
-- 排班权限
('排班管理', 'schedule:policy:view', 1, 0, 0, 1, NOW()),
('排班策略管理', 'schedule:policy:manage', 2, 0, 0, 1, NOW()),
-- 数据分析权限
('数据分析', 'analytics:view', 1, 0, 0, 1, NOW());

-- 2. 将所有检查相关权限分配给超级管理员角色 (role_id=1)
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT 1, id, NOW() FROM permissions
WHERE permission_code IN (
    'inspection:record:view', 'inspection:record:create', 'inspection:record:edit',
    'inspection:record:submit', 'inspection:record:review', 'inspection:record:publish',
    'inspection:template:view', 'inspection:template:edit',
    'quantification:template:add', 'quantification:template:view', 'quantification:template:edit',
    'inspection:appeal:create', 'inspection:appeal:view', 'inspection:appeal:review', 'inspection:appeal:final-review',
    'inspection:export', 'inspection:export:view', 'inspection:export:create',
    'corrective:action:create', 'corrective:action:view', 'corrective:action:process', 'corrective:action:verify',
    'schedule:policy:view', 'schedule:policy:manage',
    'analytics:view'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 1 AND rp.permission_id = permissions.id
);

-- 验证
SELECT COUNT(*) AS total_inspection_permissions FROM permissions
WHERE permission_code LIKE 'inspection:%'
   OR permission_code LIKE 'corrective:%'
   OR permission_code LIKE 'schedule:%'
   OR permission_code LIKE 'analytics:%'
   OR permission_code LIKE 'quantification:%';
