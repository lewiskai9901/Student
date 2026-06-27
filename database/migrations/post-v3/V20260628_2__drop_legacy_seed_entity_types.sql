-- 清除未随插件模型迁移的遗留种子实体类型 (is_plugin_registered=0, 零使用)。
-- 多为插件类型的孤儿影子 (VISITOR≈GUEST / GRADUATE·UNDERGRADUATE≈STUDENT / LIBRARY·GYM 等 0 实例旧 place 类型)。
-- 保留 VISITOR: 金标准测试用户 dpt_ct 在用。baseline_v3 已含此清理; 本迁移供 dev 库追赶。
DELETE FROM entity_type_configs WHERE entity_type='USER'     AND is_plugin_registered=0 AND type_code<>'VISITOR' AND deleted=0;
DELETE FROM entity_type_configs WHERE entity_type='ORG_UNIT' AND is_plugin_registered=0 AND deleted=0;
DELETE FROM entity_type_configs WHERE entity_type='PLACE'    AND is_plugin_registered=0 AND deleted=0;
