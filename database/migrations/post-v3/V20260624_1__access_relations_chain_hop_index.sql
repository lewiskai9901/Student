-- 多级关系链 P5(性能): access_relations 链遍历(subject→resource 方向)针对性索引。
-- 链中间跳查询: WHERE subject_type=? AND subject_id IN(...) AND relation IN(...) AND resource_type=? AND deleted=0
-- 既有 idx_subject (subject_type,subject_id,deleted) 仅覆盖主体前缀; 补 relation+resource_type 入索引,
-- 使链跳遍历走覆盖索引 (access_relations 增大后受益; 终端成员图反向走已有 idx_expand)。
CREATE INDEX `idx_chain_hop` ON `access_relations`
  (`subject_type`, `subject_id`, `relation`, `resource_type`, `deleted`);
