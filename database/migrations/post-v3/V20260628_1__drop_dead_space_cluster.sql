-- 清理 Space→Place 重命名(2026-02-08)遗留的死表簇: 18 张 space_*/spaces/place_categories 表 +
-- 3 个建在死 space 表上的视图。全部 0 Java 消费方, 仅 spaces_v10↔place_categories 互引 FK。
-- baseline_v3 已移除这些块; 本迁移供已有 dev 库追赶。
SET FOREIGN_KEY_CHECKS=0;
DROP VIEW IF EXISTS v_buildings, v_classrooms, v_space_detail;
DROP TABLE IF EXISTS
  space, spaces, spaces_v10, space_assignments, space_categories, space_class_assignment,
  space_classroom_ext, space_dormitory_ext, space_lab_ext, space_occupant, space_occupants,
  space_office_ext, space_org_assignment, space_org_relation_history, space_org_relations,
  space_type_config, space_types, place_categories;
SET FOREIGN_KEY_CHECKS=1;
