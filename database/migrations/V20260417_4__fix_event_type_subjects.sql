-- ============================================================
-- 修复 entity_event_types.applicable_subjects 格式与枚举一致性
-- 背景：
--   1. V64 种子数据使用旧枚举 'ORG'，与前端/Java SubjectType 使用的 'ORG_UNIT' 不一致
--   2. V20260417_1 种子错误地把单值写成裸字符串 'USER' / 'ORG_UNIT'，
--      而非 JSON 数组 '["USER"]'，导致前端 JSON.parse 抛错 → UI 显示空
--   3. 违规/表扬事件缺少 STUDENT 主体（业务最常见场景）
-- ============================================================

-- 1. V64 旧枚举 ORG → ORG_UNIT（只影响 JSON 内的 "ORG" token，不误伤 "ORG_UNIT"）
UPDATE entity_event_types
SET applicable_subjects = REPLACE(applicable_subjects, '"ORG"', '"ORG_UNIT"')
WHERE applicable_subjects LIKE '%"ORG"%'
  AND deleted = 0;

-- 2. V20260417_1 裸字符串 → JSON 数组
UPDATE entity_event_types
SET applicable_subjects = CONCAT('["', applicable_subjects, '"]')
WHERE applicable_subjects IS NOT NULL
  AND applicable_subjects <> ''
  AND applicable_subjects NOT LIKE '[%'
  AND deleted = 0;

-- (已废弃) 之前尝试为违规/表扬添加 STUDENT 主体
-- 已由 V20260417_6 撤销：主体只保留 USER/ORG_UNIT/PLACE，学生属 USER 子类型
