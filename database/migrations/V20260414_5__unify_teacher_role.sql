-- V20260414_5: 合并教师权限角色 (V4 设计修正)
--
-- V4 建了 SUBJECT_TEACHER / CLASS_TEACHER / COUNSELOR 三个空壳权限角色,
-- 意图为不同职务的教师配不同 API 权限。但设计复盘发现:
--
-- 1. 职务已由 teacher_assignments.role_type 精确表达
--    (HEAD_TEACHER/DEPUTY_HEAD_TEACHER/COUNSELOR/SUBJECT_TEACHER 4 种),
--    MyDashboardQueryService / @DataPermission / 前端 UI 都从那取
-- 2. 权限角色(user_roles)职责是"能调哪些 API",职务(teacher_assignments)是
--    "负责哪些班/哪些学生" — 两者无需同构
-- 3. V4 的 CLASS_TEACHER 与领域模型的 HEAD_TEACHER 命名错位,
--    且缺 DEPUTY_HEAD_TEACHER
-- 4. 目前产品需求里没有"副班主任不能改成绩"类的 API 差异化
--
-- 因此 "最完美" 方案是压缩而非扩展权限角色:
--   ROLE_D64522C2 → 重命名为 TEACHER (对所有教师统一)
--   SUBJECT_TEACHER / CLASS_TEACHER / COUNSELOR → 软删 (空壳)
--   default_role_codes → ["TEACHER"]
--
-- 未来真有 API 差异化需求时,加 HEAD_TEACHER_EXTRA 之类叠加角色即可.

-- ========== A. 重命名 ROLE_D64522C2 → TEACHER ==========
UPDATE roles
SET role_code = 'TEACHER',
    role_name = '教师',
    role_desc = '教师统一权限角色;职务分类见 teacher_assignments.role_type'
WHERE role_code = 'ROLE_D64522C2' AND deleted = 0;

-- ========== B. 软删 V4 新建的空壳教师角色 ==========
UPDATE roles
SET deleted = 1
WHERE role_code IN ('SUBJECT_TEACHER', 'CLASS_TEACHER', 'COUNSELOR')
  AND deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur WHERE ur.role_id = roles.id AND ur.is_active = 1
  );

-- ========== C. user_types.default_role_codes → ["TEACHER"] ==========
UPDATE user_types
SET default_role_codes = '["TEACHER"]'
WHERE type_code = 'TEACHER'
  AND default_role_codes IN ('["SUBJECT_TEACHER"]', 'SUBJECT_TEACHER');
