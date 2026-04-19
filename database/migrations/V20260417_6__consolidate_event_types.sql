-- ============================================================
-- 事件类型规范化：
--   1. 主体只保留 USER/ORG_UNIT/PLACE 三种（学生是 USER 子类型，不单列）
--      → 从 applicable_subjects 中移除 STUDENT
--   2. 教务流程事件从 NOTIFICATION 归入 TEACHING 分类
--      → GRADE_SUBMITTED / GRADE_APPROVED / GRADE_PUBLISHED / EXAM_PUBLISHED
--      → 同步更新：entity_event_types / msg_subscription_rules
--               / entity_events / msg_notifications
--   3. NOTIFICATION 分类内部 polarity 统一为 NEUTRAL（消除分组查询 GROUP BY 分裂）
-- ============================================================

-- ==========================================
-- Part 1: 清除 applicable_subjects 中的 STUDENT
-- 幂等 —— 目前 DB 里无 STUDENT，但 V20260417_4 可能被他处执行过
-- ==========================================
UPDATE entity_event_types
SET applicable_subjects = REPLACE(REPLACE(REPLACE(
      applicable_subjects,
      ',"STUDENT"', ''),
      '"STUDENT",', ''),
      '"STUDENT"', '')
WHERE applicable_subjects LIKE '%STUDENT%' AND deleted = 0;

-- ==========================================
-- Part 2: 把 4 个教务事件从 NOTIFICATION 迁到 TEACHING
-- ==========================================

-- 2a. entity_event_types: 改分类 + 统一 polarity=NEUTRAL
UPDATE entity_event_types
SET category_code   = 'TEACHING',
    category_name   = '教务流程',
    category_polarity = 'NEUTRAL',
    sort_order      = CASE type_code
                        WHEN 'GRADE_SUBMITTED'  THEN 10
                        WHEN 'GRADE_APPROVED'   THEN 11
                        WHEN 'GRADE_PUBLISHED'  THEN 12
                        WHEN 'EXAM_PUBLISHED'   THEN 13
                        ELSE sort_order
                      END
WHERE type_code IN ('GRADE_SUBMITTED','GRADE_APPROVED','GRADE_PUBLISHED','EXAM_PUBLISHED')
  AND category_code = 'NOTIFICATION'
  AND deleted = 0;

-- 2b. msg_subscription_rules: 同步 event_category
UPDATE msg_subscription_rules
SET event_category = 'TEACHING'
WHERE event_category = 'NOTIFICATION'
  AND event_type IN ('GRADE_SUBMITTED','GRADE_APPROVED','GRADE_PUBLISHED','EXAM_PUBLISHED');

-- 2c. entity_events: 历史事件记录同步
UPDATE entity_events
SET event_category = 'TEACHING'
WHERE event_category = 'NOTIFICATION'
  AND event_type IN ('GRADE_SUBMITTED','GRADE_APPROVED','GRADE_PUBLISHED','EXAM_PUBLISHED');

-- 2d. msg_notifications: 站内消息的 event_category
UPDATE msg_notifications
SET event_category = 'TEACHING'
WHERE event_category = 'NOTIFICATION'
  AND source_event_type IN ('GRADE_SUBMITTED','GRADE_APPROVED','GRADE_PUBLISHED','EXAM_PUBLISHED');

-- ==========================================
-- Part 3: NOTIFICATION 剩余类型 polarity 全部归 NEUTRAL
-- 之前 V20260417_1 误把 GRADE_APPROVED 设为 POSITIVE 已在 Part 2 修正
-- 此处为防御性兜底：把 NOTIFICATION 分类所有行 polarity 统一为 NEUTRAL
-- ==========================================
UPDATE entity_event_types
SET category_polarity = 'NEUTRAL'
WHERE category_code = 'NOTIFICATION'
  AND category_polarity <> 'NEUTRAL'
  AND deleted = 0;
