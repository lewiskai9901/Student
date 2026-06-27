-- 完美重构 P4: 删共享数据权限码 inspection_record 的残留行。
-- P1-P3 已把原共用该码的 5 个 mapper 迁到 inspection_submission / inspection_task / inspection_evidence /
-- inspection_submission_detail / inspection_project_inspector 各自的码 → inspection_record 已无 @DataPermission 引用。
-- contribution 是 upsert-only(不删已移除的声明), 故手动清理已建库的残留 (含 P1 删的 inspected PROVIDER、
-- P2 删的 reviewer RECORD_RELATION, 它们都是 inspection_record 的 resource_relations 行)。
-- ⚠ inspection_record 作为 casbin 功能权限码 (inspection_record:view, 见 ObservationController/EducationManifest)
-- 是另一命名空间, 不在此删。本迁移只动数据权限三表。
-- 行为不变: CLASS_TEACHER 原 {creator,SELF} 配置删除后, 拆分后各码无配置 → 拦截器默认 SELF = created_by=me, 等价。

DELETE FROM resource_relations WHERE resource_code = 'inspection_record';
DELETE FROM data_resources     WHERE resource_code = 'inspection_record';
DELETE FROM role_data_scopes   WHERE resource_code = 'inspection_record';
