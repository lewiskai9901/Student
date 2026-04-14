-- V20260414_4: Casbin 清理 + 补齐教师角色生态
--
-- 问题:
--   1. casbin_rule 中 16 条历史数据 (v1='scope:*' 等) 与当前 matcher (r.dom==p.dom=tenantId) 不兼容,永不匹配
--   2. user_types.default_role_codes 引用的 SUBJECT_TEACHER/CLASS_TEACHER/COUNSELOR 角色不存在,新建教师无法绑默认角色
--   3. D2.4 应急种入的 3 条 my:* p-rule + 22 条 ROLE_D64522C2 g-rule,在 PermissionScope.SELF 改为放行后已冗余
--
-- 本迁移:
--   A. 删死 casbin_rule (scope:* 域 + g2 测试数据)
--   B. 删 D2.4 应急种子 (my:* p-rule + ROLE_D64522C2 g-rule)
--   C. 补建 SUBJECT_TEACHER / CLASS_TEACHER / COUNSELOR 三个教师类角色
--   D. 修 user_types.default_role_codes 的 JSON 格式 (裸字符串 → JSON 数组)

-- ========== A. 清理死 casbin_rule ==========
DELETE FROM casbin_rule
WHERE (ptype = 'p' AND (v1 LIKE 'scope:%' OR v1 = 'scope:*'))
   OR ptype = 'g2';

-- ========== B. 清理 D2.4 应急种子 ==========
DELETE FROM casbin_rule
WHERE ptype = 'p'
  AND v0 = 'ROLE_D64522C2'
  AND v1 = '1'
  AND v2 IN ('my:schedule', 'my:students', 'my:substitute')
  AND v3 = 'view';

DELETE FROM casbin_rule
WHERE ptype = 'g'
  AND v1 = 'ROLE_D64522C2'
  AND v2 = '1'
  AND v0 IN (SELECT CAST(u.id AS CHAR) FROM users u WHERE u.user_type_code = 'TEACHER' AND u.deleted = 0);

-- ========== C. 补建教师类角色 ==========
INSERT IGNORE INTO roles (role_name, role_code, role_desc, role_type, sort_order, status, tenant_id, deleted)
VALUES
  ('任课教师', 'SUBJECT_TEACHER', '授课教师,仅教学数据访问', 'SYSTEM', 10, 1, 1, 0),
  ('班主任',   'CLASS_TEACHER',   '班级管理+授课,本班数据访问', 'SYSTEM', 11, 1, 1, 0),
  ('辅导员',   'COUNSELOR',       '年级辅导员,年级数据访问',   'SYSTEM', 12, 1, 1, 0);

-- ========== D. 修 user_types.default_role_codes JSON 格式 ==========
-- TEACHER: "SUBJECT_TEACHER" (裸字符串) → ["SUBJECT_TEACHER"]
-- STUDENT: "STUDENT" (裸字符串) → ["STUDENT"] (但 STUDENT 角色也不存在,本次只修 TEACHER)
UPDATE user_types
SET default_role_codes = '["SUBJECT_TEACHER"]'
WHERE type_code = 'TEACHER' AND default_role_codes = 'SUBJECT_TEACHER';
