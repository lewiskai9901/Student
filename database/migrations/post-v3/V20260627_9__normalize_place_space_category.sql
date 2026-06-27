-- 归一历史遗留 PLACE 类型分类 'SPACE' → 合法 BaseCategory (枚举无 SPACE; 早期未迁移种子)。
-- baseline_v3 已含本归一; 本迁移供已有 dev 库追赶。
UPDATE entity_type_configs SET category='BUILDING' WHERE entity_type='PLACE' AND category='SPACE' AND type_code IN ('TEACH_BUILDING','DORMITORY');
UPDATE entity_type_configs SET category='AREA' WHERE entity_type='PLACE' AND category='SPACE' AND type_code='PLAYGROUND';
UPDATE entity_type_configs SET category='ROOM' WHERE entity_type='PLACE' AND category='SPACE';
