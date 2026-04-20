-- V104: 删除 users.employee_no 列
--
-- 按"通用核心 + 行业插件"原则, "工号"是 TEACHER / STAFF 特有概念,
-- 不应放在通用核心 User 聚合上. user_teacher.employee_no (V89 创建) 承接该字段.
--
-- 迁移策略:
--   1. 将 users.employee_no 已有数据回填到 user_teacher.employee_no (仅当 teacher 已存在档案且档案未设工号).
--   2. DROP users.employee_no 列.
--
-- 注意: 本项目开发阶段不考虑旧数据兼容; 若 users 有 employee_no 但没有 user_teacher 档案,
-- 该字段值会丢失 (符合"彻底删除"原则).

-- 1. 把已有工号迁移到教师档案 (仅当 teacher 档案存在且尚未设置工号)
UPDATE user_teacher ut
INNER JOIN users u ON ut.user_id = u.id
SET ut.employee_no = u.employee_no
WHERE u.employee_no IS NOT NULL
  AND u.employee_no <> ''
  AND (ut.employee_no IS NULL OR ut.employee_no = '');

-- 2. 从 users 表删除 employee_no 列
ALTER TABLE users DROP COLUMN employee_no;
