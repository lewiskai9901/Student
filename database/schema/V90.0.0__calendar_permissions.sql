-- =====================================================
-- V90.0.0 - Extract calendar permissions from teaching
-- Date: 2026-04-03
-- Description: Creates standalone calendar permissions
--   (replacing teaching:calendar) since calendar is now
--   a shared resource used by all domains.
-- =====================================================

SET NAMES utf8mb4;

-- 1. Create calendar top-level permission node
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES (9001, 'calendar', '校历管理', 0, 1, 2, 1, 0);

-- 2. Calendar children: view / edit
INSERT IGNORE INTO permissions (id, permission_code, permission_name, parent_id, permission_type, sort_order, status, deleted)
VALUES
(9002, 'calendar:view', '查看校历', 9001, 3, 1, 1, 0),
(9003, 'calendar:edit', '编辑校历', 9001, 3, 2, 1, 0);

-- 3. Grant calendar permissions to admin role (role_id = 1)
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at, tenant_id)
SELECT 1, id, NOW(), 1 FROM permissions
WHERE permission_code IN ('calendar', 'calendar:view', 'calendar:edit') AND deleted = 0
  AND id NOT IN (SELECT permission_id FROM role_permissions WHERE role_id = 1);

-- 4. Also add casbin policies so @CasbinAccess works
-- Anyone who had teaching:calendar now gets calendar
INSERT IGNORE INTO casbin_rule (ptype, v0, v1, v2)
SELECT 'p', v0, 'calendar', v2
FROM casbin_rule
WHERE ptype = 'p' AND v1 = 'teaching:calendar'
  AND NOT EXISTS (
    SELECT 1 FROM casbin_rule cr2
    WHERE cr2.ptype = 'p' AND cr2.v0 = casbin_rule.v0
      AND cr2.v1 = 'calendar' AND cr2.v2 = casbin_rule.v2
  );
