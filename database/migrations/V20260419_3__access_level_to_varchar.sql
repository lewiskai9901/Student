-- access_relations.access_level 从 TINYINT 改为 VARCHAR 语义化
-- 映射: 1 -> 'FULL', 2 -> 'OWNER', 0 -> 'READ_ONLY'

ALTER TABLE access_relations
  MODIFY COLUMN access_level VARCHAR(20) NOT NULL DEFAULT 'FULL';

-- 历史数据中 1 全部表示"有权限"(老系统没细分),统一为 'FULL'
UPDATE access_relations SET access_level = 'FULL' WHERE access_level IN ('1', '2', '0');

-- history 表同步
ALTER TABLE access_relations_history
  MODIFY COLUMN access_level VARCHAR(20);
