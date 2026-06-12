-- ============================================================================
-- V20260612_1: data_resources 陈旧列配置清洗 + DROP 死表 data_modules
--
-- 背景: data_resources.org_unit_field / creator_field 是**活配置** —
-- DataPermissionInterceptor 里 moduleConfig.getOrgUnitField()/getCreatorField()
-- 优先于 @DataPermission 注解值。以下行的配置指向已删除/从不存在的列, 当前
-- 因 viaMembership 短路或模块无 mapper 消费而休眠, 但任何代码改动让它们变活
-- 即是 "Unknown column" 500 (2026-06-12 拦截器 plugin-dim 降级分支事故同源)。
--
--   student      org_unit_id/created_by → user_student 两列均已 DROP (V20260531),
--                归属走 access_relations member (viaMembership) → 双 NULL
--   attendance   class_id → attendance_records 无此列, 真实 org 列是 org_unit_id
--   exam         created_by → exams 表不存在 (实际表 exam_arrangements), 模块当前
--                无 @DataPermission mapper 消费 → NULL, 待接入时按真实表显式配置
--   notification user_id → notifications 表不存在, 同上 → NULL
--
-- data_modules: v3 单轨 data_resources 后的死表 — 代码零消费 (无 mapper,
-- DataModulePO 仅作内存 DTO 由 data_resources 行构造), 表里还躺着同一批
-- 陈旧列配置 (student org_unit_id/attendance class_id), 彻底 DROP。
--
-- 幂等: UPDATE 重复执行无副作用; DROP TABLE IF EXISTS 天然幂等。
-- ============================================================================

UPDATE data_resources SET org_unit_field = NULL, creator_field = NULL
WHERE resource_code = 'student';

UPDATE data_resources SET org_unit_field = 'org_unit_id'
WHERE resource_code = 'attendance' AND org_unit_field = 'class_id';

UPDATE data_resources SET creator_field = NULL
WHERE resource_code = 'exam' AND creator_field = 'created_by';

UPDATE data_resources SET creator_field = NULL
WHERE resource_code = 'notification' AND creator_field = 'user_id';

DROP TABLE IF EXISTS data_modules;
