-- ============================================================
-- 修复 V20260417_4 错误：把单字符串 JSON 数组拆成多元素数组
-- 原种子用逗号分隔裸字符串 (USER,PLACE)，V4 的 CONCAT 包裹把它们变成了
-- '["USER,PLACE"]' —— 这是 1 个带逗号的字符串，而不是 2 元素数组
-- 正确形式应为 '["USER","PLACE"]'
-- ============================================================

UPDATE entity_event_types
SET applicable_subjects = REPLACE(applicable_subjects, ',', '","')
WHERE applicable_subjects LIKE '[%,%]'
  AND applicable_subjects NOT LIKE '%","%'
  AND deleted = 0;
