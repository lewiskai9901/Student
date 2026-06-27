-- Tier2 收官: membershipSubjectColumn 从 @DataPermission 注解迁进 resource_relations 注册表。
-- 加 subject_column 列承载 owner_org SUBJECT_GRAPH 行的 subject 列名 (主表充当 ar.subject_id 的列)。
-- 行值由 ResourceRelationUpserter 在启动时按 contribution 写入 (contribution 驱动, 非 seed):
-- student owner_org 经 EducationManifest 的 withSubjectColumn("user_id") 声明 → 启动写 subject_column='user_id'。
-- 仅需该列存在 (DDL); dev 库追赶用, baseline_v3 已含同列。

ALTER TABLE `resource_relations`
  ADD COLUMN `subject_column` varchar(64) DEFAULT NULL
  COMMENT 'Tier2: owner_org SUBJECT_GRAPH 主表充当 ar.subject_id 的列 (空=默认 id; user_student=user_id)'
  AFTER `resolver_bean`;
